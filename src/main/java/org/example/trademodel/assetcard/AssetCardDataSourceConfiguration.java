package org.example.trademodel.assetcard;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Card-only holder: deliberately NOT a root DataSource/JdbcOperations bean. */
@Configuration(proxyBeanMethods = false)
public class AssetCardDataSourceConfiguration {
    public static final String WRITER_ROLE = "rine_asset_card_writer";
    private static final String SYSTEMD_CREDENTIAL_DIRECTORY = "/run/credentials/rine-logic.service";
    private static final String SYSTEMD_CREDENTIAL_PATH = "/run/credentials/rine-logic.service/asset-card-db-password";

    /* Linux systemd credentials use a root-owned read-only tmpfs and an exact named-user ACL.
       This helper reads metadata only, never the credential bytes. Missing prerequisites fail closed. */
    static final String SYSTEMD_CREDENTIAL_METADATA_CHECK = """
        import errno,os,stat,struct,sys
        def check():
            if len(sys.argv)!=3 or sys.platform!='linux': return False
            text,path=sys.argv[1:]
            if not text.isascii() or not text.isdecimal() or len(text)>10: return False
            uid=int(text)
            directory='/run/credentials/rine-logic.service'
            if uid<=0 or uid>2147483647 or os.getuid()!=uid or os.geteuid()!=uid: return False
            if path!=directory+'/asset-card-db-password' or os.environ.get('CREDENTIALS_DIRECTORY')!=directory: return False
            for ancestor in ('/','/run','/run/credentials',directory):
                info=os.lstat(ancestor)
                if not stat.S_ISDIR(info.st_mode) or info.st_uid!=0 or info.st_gid!=0 or info.st_mode&0o022: return False
            d=os.lstat(directory); f=os.lstat(path)
            if stat.S_IMODE(d.st_mode)!=0o550 or stat.S_IMODE(f.st_mode)!=0o440: return False
            if not stat.S_ISREG(f.st_mode) or f.st_uid!=0 or f.st_gid!=0 or not 1<=f.st_size<=4096: return False
            def acl(target,permission):
                value=os.getxattr(target,'system.posix_acl_access',follow_symlinks=False)
                if len(value)!=44 or struct.unpack_from('<I',value)[0]!=2: return False
                actual=[struct.unpack_from('<HHI',value,offset) for offset in range(4,len(value),8)]
                expected=[(1,permission,0xffffffff),(2,permission,uid),(4,0,0xffffffff),(16,permission,0xffffffff),(32,0,0xffffffff)]
                return actual==expected
            if not acl(directory,5) or not acl(path,4): return False
            try:
                os.getxattr(directory,'system.posix_acl_default',follow_symlinks=False)
                return False
            except OSError as missing:
                if missing.errno!=errno.ENODATA: return False
            with open('/proc/self/mountinfo','rb') as stream: data=stream.read(1048577)
            if not data or len(data)>1048576: return False
            def decode_mount(value):
                for escaped,literal in (('\\\\040',' '),('\\\\011','\\t'),('\\\\012','\\n'),('\\\\134','\\\\')):
                    value=value.replace(escaped,literal)
                return value
            covering=[]
            for line in data.decode('ascii').splitlines():
                left,right=line.split(' - ',1); fields=left.split(); filesystem=right.split()
                mount=decode_mount(fields[4]); options=set(fields[5].split(','))
                if path==mount or path.startswith(mount.rstrip('/')+'/'):
                    covering.append((len(mount),mount,options,filesystem[0]))
            if not covering: return False
            longest=max(entry[0] for entry in covering)
            matches=[entry for entry in covering if entry[0]==longest]
            if len(matches)!=1: return False
            _,mount,options,kind=matches[0]
            if mount!=directory or kind!='tmpfs' or 'rw' in options: return False
            if not {'ro','nosuid','nodev','noexec','nosymfollow'}.issubset(options): return False
            return bool(os.statvfs(path).f_flag&os.ST_RDONLY)
        try: allowed=check()
        except BaseException: allowed=False
        print('ASSET_CARD_SYSTEMD_CREDENTIAL_METADATA='+('PASS' if allowed else 'FAIL'))
        sys.exit(0 if allowed else 2)
        """;

    @Bean(name = "assetCardWriter", destroyMethod = "close")
    public AssetCardWriter assetCardWriter(AssetCardProperties properties) { return new AssetCardWriter(properties); }

    public record Verification(boolean allowed, String reason) {
        static Verification denied(String reason) { return new Verification(false, reason); }
    }

    /* Effective privileges include PUBLIC, column ACLs and membership. Only catalog reads occur here. */
    static final String PERMISSION_SQL = """
        WITH writer AS (SELECT * FROM pg_roles WHERE rolname=current_user),
        card_names(name) AS (VALUES ('tm_asset_card_snapshot'),('tm_asset_card_spot_bar'),('tm_asset_card_feature_history')),
        objects AS (
          SELECT c.oid,c.relowner,c.relacl,c.relkind,c.relname,n.nspname FROM pg_class c
          JOIN pg_namespace n ON n.oid=c.relnamespace
          WHERE n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%'
          AND c.relkind IN ('r','p','v','m','f','S')
        ), matrix AS (
          SELECT o.*,p.privilege,
            o.nspname='public' AND ((o.relname='tm_asset_card_snapshot' AND p.privilege IN ('SELECT','INSERT','UPDATE'))
            OR (o.relname IN ('tm_asset_card_spot_bar','tm_asset_card_feature_history') AND p.privilege IN ('SELECT','INSERT','DELETE'))) AS expected
          FROM objects o CROSS JOIN (VALUES ('SELECT'),('INSERT'),('UPDATE'),('DELETE'),('TRUNCATE'),('REFERENCES'),('TRIGGER')) p(privilege)
          WHERE o.relkind<>'S'
        )
        SELECT CASE
          WHEN current_database()<>? OR session_user<>'rine_asset_card_writer' OR current_user<>session_user THEN 'CARD_CONNECTION_IDENTITY_INVALID'
          WHEN current_setting('transaction_read_only')<>'off' THEN 'CARD_CONNECTION_READ_ONLY'
          WHEN EXISTS(SELECT 1 FROM writer r WHERE NOT r.rolcanlogin OR r.rolsuper OR r.rolcreatedb OR r.rolcreaterole
            OR r.rolinherit OR r.rolreplication OR r.rolbypassrls OR r.rolconfig IS NOT NULL)
            OR EXISTS(SELECT 1 FROM pg_auth_members m,writer r WHERE m.member=r.oid OR m.roleid=r.oid)
            OR EXISTS(SELECT 1 FROM pg_db_role_setting s,writer r WHERE s.setrole=r.oid)
            THEN 'CARD_ROLE_ATTRIBUTES_OR_MEMBERSHIP_INVALID'
          WHEN EXISTS(SELECT 1 FROM pg_database d,writer r WHERE d.datdba=r.oid)
            OR EXISTS(SELECT 1 FROM pg_namespace n,writer r WHERE n.nspowner=r.oid)
            OR EXISTS(SELECT 1 FROM pg_class c,writer r WHERE c.relowner=r.oid) THEN 'CARD_ROLE_OWNERSHIP_FORBIDDEN'
          WHEN has_database_privilege(current_user,current_database(),'CREATE')
            OR has_database_privilege(current_user,current_database(),'TEMPORARY')
            OR NOT has_database_privilege(current_user,current_database(),'CONNECT')
            OR NOT has_schema_privilege(current_user,'public','USAGE')
            OR EXISTS(SELECT 1 FROM pg_namespace WHERE has_schema_privilege(current_user,oid,'CREATE')) THEN 'CARD_DATABASE_OR_SCHEMA_PRIVILEGE_INVALID'
          WHEN EXISTS(SELECT 1 FROM card_names names WHERE NOT EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
            WHERE n.nspname='public' AND c.relname=names.name AND c.relkind='r'))
            OR EXISTS(SELECT 1 FROM card_names WHERE to_regclass(name) IS DISTINCT FROM to_regclass('public.'||name)) THEN 'CARD_TABLE_IDENTITY_INVALID'
          WHEN EXISTS(SELECT 1 FROM objects WHERE relkind='S' AND has_sequence_privilege(current_user,oid,'USAGE,SELECT,UPDATE'))
            THEN 'CARD_SEQUENCE_PRIVILEGE_FORBIDDEN'
          WHEN EXISTS(SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
            WHERE p.prosecdef AND n.nspname NOT IN ('pg_catalog','information_schema')
            AND has_function_privilege(current_user,p.oid,'EXECUTE')) THEN 'CARD_SECURITY_DEFINER_EXECUTE_FORBIDDEN'
          WHEN EXISTS(SELECT 1 FROM matrix WHERE expected<>has_table_privilege(current_user,oid,privilege))
            OR EXISTS(SELECT 1 FROM matrix m JOIN pg_attribute a ON a.attrelid=m.oid AND a.attnum>0 AND NOT a.attisdropped
              WHERE NOT m.expected AND m.privilege IN ('SELECT','INSERT','UPDATE','REFERENCES')
              AND has_column_privilege(current_user,m.oid,a.attnum,m.privilege)) THEN 'CARD_EFFECTIVE_PRIVILEGES_EXCESSIVE_OR_MISSING'
          WHEN EXISTS(SELECT 1 FROM pg_database d CROSS JOIN LATERAL aclexplode(d.datacl) a,writer r
            WHERE a.grantee IN (0,r.oid) AND a.is_grantable)
            OR EXISTS(SELECT 1 FROM pg_namespace n CROSS JOIN LATERAL aclexplode(n.nspacl) a,writer r
              WHERE a.grantee IN (0,r.oid) AND a.is_grantable)
            OR EXISTS(SELECT 1 FROM pg_class c CROSS JOIN LATERAL aclexplode(c.relacl) a,writer r
              WHERE a.grantee IN (0,r.oid) AND a.is_grantable)
            OR EXISTS(SELECT 1 FROM pg_attribute c CROSS JOIN LATERAL aclexplode(c.attacl) a,writer r
              WHERE a.grantee IN (0,r.oid) AND a.is_grantable) THEN 'CARD_GRANT_OPTION_FORBIDDEN'
          ELSE 'PASS' END
        """;

    /** Does not issue GRANT/REVOKE, mutate data, switch role, or expose driver error text. */
    public static Verification verify(Connection connection, String expectedDatabase) {
        try {
            if (connection == null || expectedDatabase == null || !expectedDatabase.matches("[A-Za-z0-9_]+")
                    || !"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()))
                return Verification.denied("CARD_POSTGRESQL_IDENTITY_REQUIRED");
            if (connection.isReadOnly()) return Verification.denied("CARD_CONNECTION_READ_ONLY");
            try (var query = connection.prepareStatement(PERMISSION_SQL)) {
                query.setQueryTimeout(5); query.setString(1, expectedDatabase);
                try (var result = query.executeQuery()) {
                    if (!result.next()) return Verification.denied("CARD_PERMISSION_CHECK_FAILED");
                    String reason = result.getString(1);
                    return "PASS".equals(reason) ? new Verification(true, "CARD_TABLE_WRITE_PERMISSIONS_VERIFIED")
                            : Verification.denied(reason != null && reason.matches("CARD_[A-Z_]+") ? reason : "CARD_PERMISSION_CHECK_FAILED");
                }
            }
        } catch (SQLException | RuntimeException failure) { return Verification.denied("CARD_PERMISSION_CHECK_FAILED"); }
    }

    static char[] readCredential(Path path, String expectedOwner) {
        byte[] bytes = null;
        try {
            if (path == null || !path.isAbsolute() || !path.normalize().equals(path) || expectedOwner == null || expectedOwner.isBlank())
                throw new IllegalArgumentException();
            boolean systemdRuntime = SYSTEMD_CREDENTIAL_PATH.equals(path.toString());
            byte[] mountBefore = systemdRuntime ? systemdMountIdentity() : null;
            Path cursor = path.getRoot();
            for (Path component : path) {
                cursor = cursor.resolve(component);
                if (Files.isSymbolicLink(cursor)) throw new IllegalArgumentException();
                if (!cursor.equals(path)) {
                    var parent = Files.readAttributes(cursor, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    long uid = ((Number) Files.getAttribute(cursor, "unix:uid", LinkOption.NOFOLLOW_LINKS)).longValue();
                    int mode = ((Number) Files.getAttribute(cursor, "unix:mode", LinkOption.NOFOLLOW_LINKS)).intValue();
                    boolean trustedOwner = uid == 0 || (expectedOwner.matches("[0-9]+")
                            ? expectedOwner.equals(Long.toString(uid)) : expectedOwner.equals(parent.owner().getName()));
                    // A root-owned sticky temporary parent cannot replace another user's protected child directory.
                    if (!parent.isDirectory() || !trustedOwner || (mode & 0022) != 0 && !(uid == 0 && (mode & 01000) != 0))
                        throw new IllegalArgumentException();
                }
            }
            PosixFileAttributes before = credentialAttributes(path, expectedOwner, systemdRuntime);
            try (var channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.allocate(4097);
                while (buffer.hasRemaining() && channel.read(buffer) > 0) { }
                if (buffer.position() < 1 || buffer.position() > 4096) throw new IllegalArgumentException();
                bytes = Arrays.copyOf(buffer.array(), buffer.position()); Arrays.fill(buffer.array(), (byte)0);
            }
            PosixFileAttributes after = credentialAttributes(path, expectedOwner, systemdRuntime);
            byte[] mountAfter = systemdRuntime ? systemdMountIdentity() : null;
            if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
                    || !before.lastModifiedTime().equals(after.lastModifiedTime())
                    || !Arrays.equals(mountBefore, mountAfter)) throw new IllegalArgumentException();
            var decoded = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            char[] password = new char[decoded.remaining()]; decoded.get(password);
            if (password.length > 0 && password[password.length - 1] == '\n') {
                char[] trimmed = Arrays.copyOf(password, password.length - 1); Arrays.fill(password, '\0'); password = trimmed;
            }
            if (password.length == 0) throw new IllegalArgumentException();
            for (char value : password) if (value == '\0' || value == '\n' || value == '\r') {
                Arrays.fill(password, '\0'); throw new IllegalArgumentException();
            }
            return password;
        } catch (Exception failure) { throw new IllegalArgumentException("ASSET_CARD_CREDENTIAL_INVALID"); }
        finally { if (bytes != null) Arrays.fill(bytes, (byte)0); }
    }

    private static PosixFileAttributes credentialAttributes(Path path, String owner, boolean systemdRuntime) throws Exception {
        if (systemdRuntime) {
            verifySystemdCredentialMetadata(path, owner);
            return Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        PosixFileAttributes attributes = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        boolean ownerMatches = owner.matches("[0-9]+")
                ? owner.equals(Files.getAttribute(path, "unix:uid", LinkOption.NOFOLLOW_LINKS).toString())
                : owner.equals(attributes.owner().getName());
        Set<PosixFilePermission> mode = attributes.permissions();
        if (!attributes.isRegularFile() || !ownerMatches || attributes.size() < 1 || attributes.size() > 4096
                || (((Number) Files.getAttribute(path, "unix:mode", LinkOption.NOFOLLOW_LINKS)).intValue() & 07000) != 0
                || !(mode.equals(Set.of(PosixFilePermission.OWNER_READ))
                || mode.equals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))))
            throw new IllegalArgumentException();
        return attributes;
    }

    private static byte[] systemdMountIdentity() throws Exception {
        try (var stream = Files.newInputStream(Path.of("/proc/self/mountinfo"))) {
            byte[] metadata = stream.readNBytes(1_048_577);
            if (metadata.length == 0 || metadata.length > 1_048_576) throw new IllegalArgumentException();
            return metadata;
        }
    }

    private static void verifySystemdCredentialMetadata(Path path, String owner) throws Exception {
        if (!SYSTEMD_CREDENTIAL_DIRECTORY.equals(System.getenv("CREDENTIALS_DIRECTORY"))
                || !owner.matches("[1-9][0-9]{0,9}")) throw new IllegalArgumentException();
        ProcessBuilder builder = new ProcessBuilder("/usr/bin/python3", "-I", "-S", "-B", "-c",
                SYSTEMD_CREDENTIAL_METADATA_CHECK, owner, path.toString());
        builder.environment().clear();
        builder.environment().put("LC_ALL", "C");
        builder.environment().put("CREDENTIALS_DIRECTORY", SYSTEMD_CREDENTIAL_DIRECTORY);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try {
            process.getOutputStream().close();
            if (!process.waitFor(2, TimeUnit.SECONDS)) throw new IllegalArgumentException();
            byte[] output = process.getInputStream().readNBytes(4097);
            if (process.exitValue() != 0 || !Arrays.equals(output,
                    "ASSET_CARD_SYSTEMD_CREDENTIAL_METADATA=PASS\n".getBytes(StandardCharsets.US_ASCII)))
                throw new IllegalArgumentException();
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            process.getInputStream().close();
            process.getErrorStream().close();
        }
    }

    private static void validateSettings(AssetCardProperties.Writer settings) {
        if (settings == null || settings.getJdbcUrl() == null || settings.getExpectedDatabase() == null
                || !settings.getExpectedDatabase().matches("[A-Za-z0-9_]+")
                || !settings.getJdbcUrl().matches("jdbc:postgresql://[A-Za-z0-9.:-]+/[A-Za-z0-9_]+")
                || !settings.getJdbcUrl().endsWith("/" + settings.getExpectedDatabase())
                || settings.getCredentialFile() == null || settings.getCredentialOwner() == null)
            throw new IllegalArgumentException("ASSET_CARD_WRITER_CONFIGURATION_INVALID");
    }

    interface CandidatePool extends AutoCloseable { Connection connection() throws SQLException; @Override void close(); }
    interface PoolFactory { CandidatePool create(AssetCardProperties.Writer settings, char[] password); }

    private static CandidatePool hikari(AssetCardProperties.Writer settings, char[] password) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("assetCardDataSource"); config.setJdbcUrl(settings.getJdbcUrl());
        config.setUsername(WRITER_ROLE); config.setPassword(new String(password));
        config.setMaximumPoolSize(settings.getMaximumPoolSize()); config.setMinimumIdle(0);
        config.setConnectionTimeout(settings.getConnectionTimeout().toMillis()); config.setValidationTimeout(1000);
        config.setIdleTimeout(30_000); config.setMaxLifetime(300_000); config.setInitializationFailTimeout(-1);
        config.addDataSourceProperty("connectTimeout", 5); config.addDataSourceProperty("socketTimeout", 10);
        config.addDataSourceProperty("options", "-c search_path=pg_catalog,public -c statement_timeout=10000");
        HikariDataSource pool = new HikariDataSource(config);
        return new CandidatePool() {
            public Connection connection() throws SQLException { return pool.getConnection(); }
            public void close() { pool.close(); }
        };
    }

    /** A stable facade prevents consumers retaining a replaced credential generation. */
    public static final class AssetCardWriter implements AutoCloseable {
        private final AssetCardProperties properties;
        private final PoolFactory factory;
        private final StaticApplicationContext child = new StaticApplicationContext();
        private final JdbcTemplate jdbc;
        private Generation active, retired;
        private boolean closed;
        private long nextAutomaticAttempt;

        private static final class Generation {
            final CandidatePool pool; final String database; int leases;
            Generation(CandidatePool pool, String database) { this.pool = pool; this.database = database; }
        }

        public AssetCardWriter(AssetCardProperties properties) { this(properties, AssetCardDataSourceConfiguration::hikari); }
        AssetCardWriter(AssetCardProperties properties, PoolFactory factory) {
            this.properties = Objects.requireNonNull(properties); this.factory = Objects.requireNonNull(factory);
            var datasource = new AbstractDataSource() {
                @Override public Connection getConnection() throws SQLException { return borrow(); }
                @Override public Connection getConnection(String username, String password) throws SQLException {
                    throw new SQLException("ASSET_CARD_EXPLICIT_CREDENTIALS_FORBIDDEN");
                }
            };
            jdbc = new JdbcTemplate(datasource);
            child.getBeanFactory().registerSingleton("assetCardDataSource", datasource);
            child.getBeanFactory().registerSingleton("assetCardJdbcTemplate", jdbc);
            child.refresh();
        }
        public StaticApplicationContext childContext() { return child; }
        public JdbcTemplate jdbcTemplate() { return jdbc; }

        /** Explicit local lifecycle operation, never called by a page GET. Invalid candidates leave old credentials active. */
        public synchronized Verification rotate() {
            if (closed || !properties.isWriterEnabled()) return Verification.denied("ASSET_CARD_WRITER_DISABLED");
            if (retired != null) return Verification.denied("CARD_PREVIOUS_POOL_DRAINING");
            CandidatePool candidate = null; char[] password = null;
            try {
                var settings = properties.getWriter(); validateSettings(settings);
                password = readCredential(settings.getCredentialFile(), settings.getCredentialOwner());
                candidate = factory.create(settings, password);
                Verification verified;
                try (Connection connection = candidate.connection()) { verified = verify(connection, settings.getExpectedDatabase()); }
                if (!verified.allowed()) return verified;
                Generation previous = active; active = new Generation(candidate, settings.getExpectedDatabase()); candidate = null;
                if (previous != null) { if (previous.leases == 0) previous.pool.close(); else retired = previous; }
                return verified;
            } catch (RuntimeException | SQLException failure) { return Verification.denied("ASSET_CARD_WRITER_UNAVAILABLE"); }
            finally { if (password != null) Arrays.fill(password, '\0'); if (candidate != null) candidate.close(); }
        }

        public Verification readiness() {
            synchronized (this) {
                if (active == null && !closed && properties.isWriterEnabled()) {
                    long now = System.nanoTime();
                    if (now < nextAutomaticAttempt) return Verification.denied("CARD_WRITER_RETRY_PENDING");
                    nextAutomaticAttempt = now + java.time.Duration.ofSeconds(60).toNanos();
                    Verification initialized = rotate();
                    if (!initialized.allowed()) return initialized;
                }
            }
            try (Connection connection = borrow()) { return new Verification(true, "CARD_TABLE_WRITE_PERMISSIONS_VERIFIED"); }
            catch (SQLException | RuntimeException failure) { return Verification.denied("ASSET_CARD_WRITER_UNAVAILABLE"); }
        }

        private Connection borrow() throws SQLException {
            Generation generation;
            synchronized (this) {
                if (closed || !properties.isWriterEnabled()) throw new SQLException("ASSET_CARD_WRITER_DISABLED");
                if (active == null) throw new SQLException("ASSET_CARD_WRITER_UNAVAILABLE");
                generation = active; generation.leases++;
            }
            Connection connection = null;
            try {
                connection = generation.pool.connection();
                if (!verify(connection, generation.database).allowed()) throw new SQLException("ASSET_CARD_WRITER_PERMISSION_DENIED");
                Connection delegate = connection; boolean[] returned = {false};
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        synchronized (returned) { if (!returned[0]) { returned[0] = true; try { delegate.close(); } finally { release(generation); } } }
                        return null;
                    }
                    if (method.getName().equals("unwrap")) throw new SQLException("ASSET_CARD_CONNECTION_UNWRAP_FORBIDDEN");
                    if (method.getName().equals("isWrapperFor")) return false;
                    if (method.getName().equals("toString")) return "AssetCardWriterConnection";
                    try { return method.invoke(delegate, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
            } catch (SQLException | RuntimeException failure) {
                if (connection != null) try { connection.close(); } catch (SQLException ignored) { }
                release(generation); throw new SQLException("ASSET_CARD_WRITER_UNAVAILABLE");
            }
        }

        private synchronized void release(Generation generation) {
            generation.leases--;
            if (generation == retired && generation.leases == 0) { retired = null; generation.pool.close(); }
        }
        @Override public synchronized void close() {
            if (closed) return; closed = true;
            if (active != null) active.pool.close(); if (retired != null) retired.pool.close();
            active = null; retired = null; child.close();
        }
    }

    /** Independent non-Web CLI: only explicit file-based credentials and read-only catalog verification. */
    public static void main(String[] args) {
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        int code = run(args, System.out); System.exit(code);
    }
    static int run(String[] args, PrintStream output) {
        Verification result = Verification.denied("ASSET_CARD_VERIFIER_ARGUMENT_INVALID"); char[] password = null;
        try {
            if (args == null || args.length != 9 || !"verify".equals(args[0])) throw new IllegalArgumentException();
            Map<String,String> options = new HashMap<>();
            Set<String> names = Set.of("--jdbc-url", "--expected-database", "--credential-file", "--credential-owner");
            for (int i = 1; i < args.length; i += 2)
                if (!names.contains(args[i]) || options.putIfAbsent(args[i], args[i + 1]) != null) throw new IllegalArgumentException();
            var settings = new AssetCardProperties.Writer(); settings.setJdbcUrl(options.get("--jdbc-url"));
            settings.setExpectedDatabase(options.get("--expected-database"));
            settings.setCredentialFile(Path.of(options.get("--credential-file"))); settings.setCredentialOwner(options.get("--credential-owner"));
            validateSettings(settings); password = readCredential(settings.getCredentialFile(), settings.getCredentialOwner());
            Properties connectionProperties = new Properties(); connectionProperties.setProperty("user", WRITER_ROLE);
            connectionProperties.setProperty("password", new String(password));
            connectionProperties.setProperty("connectTimeout", "5"); connectionProperties.setProperty("socketTimeout", "10");
            connectionProperties.setProperty("options", "-c search_path=pg_catalog,public -c statement_timeout=10000");
            try (Connection connection = DriverManager.getConnection(settings.getJdbcUrl(), connectionProperties)) {
                result = verify(connection, settings.getExpectedDatabase());
            } finally { connectionProperties.clear(); }
        } catch (Exception failure) { result = Verification.denied("ASSET_CARD_FRESH_CONNECTION_VERIFICATION_FAILED"); }
        finally { if (password != null) Arrays.fill(password, '\0'); }
        output.println("ASSET_CARD_WRITER_VERIFY=" + (result.allowed() ? "PASS" : "FAIL"));
        output.println("REASON_CODE=" + result.reason()); return result.allowed() ? 0 : 2;
    }
}
