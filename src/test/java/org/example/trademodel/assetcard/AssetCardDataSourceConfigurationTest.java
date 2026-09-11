package org.example.trademodel.assetcard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardDataSourceConfigurationTest {
    @TempDir Path temporary;

    @Test
    void disabledDedicatedNamedChildDoesNotDisplaceBootDefaultDataSourceOrJdbc() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class))
                .withUserConfiguration(AssetCardDataSourceConfiguration.class)
                .withBean(AssetCardProperties.class)
                .withPropertyValues("spring.datasource.url=jdbc:h2:mem:default_" + UUID.randomUUID(),
                        "spring.datasource.username=sa", "spring.datasource.password=")
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class).hasSingleBean(JdbcTemplate.class);
                    var defaults = context.getBean(DataSource.class);
                    assertThat(context.getBean(JdbcTemplate.class).getDataSource()).isSameAs(defaults);
                    var writer = context.getBean(AssetCardDataSourceConfiguration.AssetCardWriter.class);
                    assertThat(writer.childContext().getBean("assetCardDataSource")).isNotSameAs(defaults);
                    assertThat(writer.childContext().getBean("assetCardJdbcTemplate")).isSameAs(writer.jdbcTemplate());
                    assertThatThrownBy(() -> writer.jdbcTemplate().queryForObject("SELECT 1", Integer.class))
                            .hasStackTraceContaining("ASSET_CARD_WRITER_DISABLED");
                    assertThat(new JdbcTemplate(defaults).queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
                });
    }

    @Test
    void credentialMetadataRejectsSymlinkBroadModeWrongOwnerAndEmptyWithoutEchoingContents() throws Exception {
        Path file = temporary.toRealPath().resolve("writer.credential");
        Files.writeString(file, "synthetic-local-password");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        String owner = Files.getOwner(file).getName();
        assertThat(AssetCardDataSourceConfiguration.readCredential(file, owner)).isEqualTo("synthetic-local-password".toCharArray());
        Path link = file.resolveSibling("symlink"); Files.createSymbolicLink(link, file);
        assertThatThrownBy(() -> AssetCardDataSourceConfiguration.readCredential(link, owner))
                .hasMessage("ASSET_CARD_CREDENTIAL_INVALID").hasNoCause();
        assertThatThrownBy(() -> AssetCardDataSourceConfiguration.readCredential(file, "nonexistent-test-owner"))
                .hasMessage("ASSET_CARD_CREDENTIAL_INVALID").hasNoCause();
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        assertThatThrownBy(() -> AssetCardDataSourceConfiguration.readCredential(file, owner)).hasMessage("ASSET_CARD_CREDENTIAL_INVALID");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")); Files.writeString(file, "");
        assertThatThrownBy(() -> AssetCardDataSourceConfiguration.readCredential(file, owner)).hasMessage("ASSET_CARD_CREDENTIAL_INVALID");
    }

    @Test
    void systemdCredentialValidationHasAnExactRuntimeBoundaryWithoutRelaxingProtectedSources() throws Exception {
        String source = Files.readString(Path.of("src/main/java/org/example/trademodel/assetcard/AssetCardDataSourceConfiguration.java"));
        assertThat(source).contains("/run/credentials/rine-logic.service/asset-card-db-password",
                "SYSTEMD_CREDENTIAL_METADATA_CHECK", "CREDENTIALS_DIRECTORY", "system.posix_acl_access",
                "nosymfollow", "mountBefore", "mountAfter");
        assertThat(source).contains("\"/usr/bin/python3\", \"-I\", \"-S\", \"-B\"", "environment().clear()",
                "getOutputStream().close()", "waitFor(2", "readNBytes(4097)");
        assertThat(source).doesNotContain("getenv(\"PASSWORD\")", "getenv(\"ASSET_CARD_PASSWORD\")");
        // Existing 0400/0600 source files remain usable with no Python dependency or group-read allowance.
        Path sourceFile = temporary.toRealPath().resolve("protected-source");
        Files.writeString(sourceFile, "isolated-protected-source");
        Files.setPosixFilePermissions(sourceFile, PosixFilePermissions.fromString("r--------"));
        char[] read = AssetCardDataSourceConfiguration.readCredential(sourceFile, Files.getOwner(sourceFile).getName());
        try { assertThat(read.length).isPositive(); } finally { java.util.Arrays.fill(read, '\0'); }
        Files.setPosixFilePermissions(sourceFile, PosixFilePermissions.fromString("r--r-----"));
        assertThatThrownBy(() -> AssetCardDataSourceConfiguration.readCredential(sourceFile, Files.getOwner(sourceFile).getName()))
                .hasMessage("ASSET_CARD_CREDENTIAL_INVALID").hasNoCause();
    }

    @Test
    void realLinuxSystemdAclAndReadOnlyMountPermitOnlyTheExactServiceIdentity() throws Exception {
        // Read the exact production helper, never a test replacement. No real credentials are used.
        var helperField = AssetCardDataSourceConfiguration.class.getDeclaredField("SYSTEMD_CREDENTIAL_METADATA_CHECK");
        helperField.setAccessible(true);
        String helper = (String) helperField.get(null);
        boolean docker;
        try { docker = org.testcontainers.DockerClientFactory.instance().isDockerAvailable(); }
        catch (RuntimeException absent) { docker = false; }
        boolean ci = "true".equalsIgnoreCase(System.getenv("CI"));
        if (ci) assertThat(docker).as("CI must execute the real Linux credential test").isTrue();
        org.junit.jupiter.api.Assumptions.assumeTrue(docker, "Disposable Linux runtime unavailable; no server fallback");
        String directory = "/run/credentials/rine-logic.service";
        var image = new org.testcontainers.images.builder.ImageFromDockerfile("v42-credential-local-" + UUID.randomUUID(), true)
                .withFileFromString("Dockerfile", """
                    FROM eclipse-temurin:17-jre-jammy AS java
                    FROM python:3.12-slim
                    COPY --from=java /opt/java/openjdk /opt/java/openjdk
                    RUN ln -s /usr/local/bin/python3 /usr/bin/python3
                    """);
        // No host mounts, socket or network. SYS_ADMIN is only for this disposable namespace's tmpfs.
        // Docker's local-volume option parser does not support MS_NOSYMFOLLOW, so use the Linux syscall.
        // docker-default AppArmor denies mount even with SYS_ADMIN. Only this no-network,
        // no-host-mount synthetic fixture opts out; no host or deployment policy is changed.
        try (var runtime = new org.testcontainers.containers.GenericContainer<>(image)
                .withNetworkMode("none")
                .withCreateContainerCmdModifier(command -> command.getHostConfig()
                        .withCapAdd(com.github.dockerjava.api.model.Capability.SYS_ADMIN)
                        .withSecurityOpts(java.util.List.of("apparmor=unconfined")))
                .withCommand("/usr/bin/python3", "-I", "-S", "-B", "-c", "import time; print('READY',flush=True); time.sleep(180)")
                .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forLogMessage(".*READY.*", 1))) {
                copyCredentialProbe(runtime);
                runtime.start();
                var kernel = runtime.execInContainer("/usr/bin/python3", "-I", "-S", "-B", "-c", """
                    import gzip,os
                    p='/proc/config.gz'
                    missing=os.path.isfile(p) and '# CONFIG_TMPFS_POSIX_ACL is not set' in gzip.open(p,'rt').read().splitlines()
                    print('LINUXKIT_WITHOUT_TMPFS_ACL' if 'linuxkit' in os.uname().release and missing else 'RUN_REQUIRED')
                    """);
                assertThat(kernel.getExitCode()).isZero();
                boolean unsupportedLocalKernel = "LINUXKIT_WITHOUT_TMPFS_ACL\n".equals(kernel.getStdout());
                if (ci) assertThat(unsupportedLocalKernel).as("Required Linux CI may not skip real tmpfs ACL verification").isFalse();
                if (!ci && System.getProperty("os.name").startsWith("Mac"))
                    org.junit.jupiter.api.Assumptions.assumeFalse(unsupportedLocalKernel,
                            "LOCAL_KERNEL_TMPFS_POSIX_ACL_UNAVAILABLE; required exact-head Linux CI must execute, not skip");
                var seeded = runtime.execInContainer("/usr/bin/python3", "-I", "-S", "-B", "-c", """
                    import os,struct,ctypes
                    d='/run/credentials/rine-logic.service'; p=d+'/asset-card-db-password'
                    os.makedirs(d,mode=0o550)
                    libc=ctypes.CDLL(None,use_errno=True)
                    assert libc.mount(b'tmpfs',d.encode(),b'tmpfs',2|4|8|256,b'size=1m,mode=0550')==0,ctypes.get_errno()
                    with open(p,'xb') as f: f.write(b'isolated-test-credential-never-reported')
                    os.chown(d,0,0); os.chown(p,0,0)
                    for path,perm in ((d,5),(p,4)):
                        os.chmod(path,0o550 if path==d else 0o440)
                        entries=((1,perm,0xffffffff),(2,perm,999),(4,0,0xffffffff),(16,perm,0xffffffff),(32,0,0xffffffff))
                        os.setxattr(path,'system.posix_acl_access',struct.pack('<I',2)+b''.join(struct.pack('<HHI',*x) for x in entries))
                    print('SYNTHETIC_METADATA_READY')
                    """);
                assertThat(seeded.getExitCode()).as("Synthetic mount/ACL fixture: %s", seeded.getStderr()).isZero();
                assertThat(seeded.getStdout()).isEqualTo("SYNTHETIC_METADATA_READY\n");
                    remountCredential(runtime, true);
                    assertMetadata(runtime, helper, "999", directory, directory + "/asset-card-db-password", true);
                    assertMetadata(runtime, helper, "998", directory, directory + "/asset-card-db-password", false);
                    assertMetadata(runtime, helper, "999", "/run/credentials/other.service", directory + "/asset-card-db-password", false);
                    assertMetadata(runtime, helper, "999", directory, directory + "/different-secret", false);
                    assertJavaCredential(runtime, true);
                    // An extra named ACL entry is forbidden even when it grants zero access.
                    remountCredential(runtime, false);
                    var extra = runtime.execInContainer("/usr/bin/python3", "-I", "-S", "-B", "-c", """
                        import os,struct
                        p='/run/credentials/rine-logic.service/asset-card-db-password'
                        entries=((1,4,0xffffffff),(2,4,999),(2,0,1001),(4,0,0xffffffff),(16,4,0xffffffff),(32,0,0xffffffff))
                        os.setxattr(p,'system.posix_acl_access',struct.pack('<I',2)+b''.join(struct.pack('<HHI',*x) for x in entries))
                        """);
                    assertThat(extra.getExitCode()).isZero();
                    remountCredential(runtime, true);
                    assertMetadata(runtime, helper, "999", directory, directory + "/asset-card-db-password", false);
                    assertJavaCredential(runtime, false);
                    // Restore the exact ACL, proving the next negative case fails for its own reason.
                    remountCredential(runtime, false);
                    var restored = runtime.execInContainer("/usr/bin/python3", "-I", "-S", "-B", "-c", """
                        import os,struct
                        p='/run/credentials/rine-logic.service/asset-card-db-password'
                        entries=((1,4,0xffffffff),(2,4,999),(4,0,0xffffffff),(16,4,0xffffffff),(32,0,0xffffffff))
                        os.setxattr(p,'system.posix_acl_access',struct.pack('<I',2)+b''.join(struct.pack('<HHI',*x) for x in entries))
                        """);
                    assertThat(restored.getExitCode()).isZero();
                    remountCredential(runtime, true);
                    assertMetadata(runtime, helper, "999", directory, directory + "/asset-card-db-password", true);
                    assertJavaCredential(runtime, true);
                // Identical UID, content and valid ACL: only the writable mount is changed.
                remountCredential(runtime, false);
                assertMetadata(runtime, helper, "999", directory, directory + "/asset-card-db-password", false);
                assertJavaCredential(runtime, false);
                remountCredential(runtime, true);
                assertJavaCredential(runtime, true);
        }
    }

    private static void remountCredential(org.testcontainers.containers.GenericContainer<?> runtime, boolean readOnly) throws Exception {
        var result = runtime.execInContainer("/usr/bin/python3", "-I", "-S", "-B", "-c", """
                import ctypes,sys
                libc=ctypes.CDLL(None,use_errno=True)
                flags=32|2|4|8|256|(1 if sys.argv[1]=='ro' else 0)
                assert libc.mount(None,b'/run/credentials/rine-logic.service',None,flags,None)==0,ctypes.get_errno()
                """, readOnly ? "ro" : "rw");
        assertThat(result.getExitCode()).isZero();
        assertThat(result.getStderr()).isEmpty();
    }

    private static final String DROP_TEST_IDENTITY = """
            import os,sys
            os.setgroups([]); os.setgid(988); os.setuid(999)
            assert os.geteuid()==999
            with open('/proc/self/status') as f:
                assert all(int(line.split()[1],16)==0 for line in f if line.startswith(('CapEff:','CapPrm:','CapAmb:')))
            os.execv(sys.argv[1],sys.argv[1:])
            """;

    private static void assertMetadata(org.testcontainers.containers.GenericContainer<?> runtime, String helper,
            String uid, String directory, String path, boolean allowed) throws Exception {
        var result = runtime.execInContainer("/usr/bin/env", "-i", "LC_ALL=C", "CREDENTIALS_DIRECTORY=" + directory,
                "/usr/bin/python3", "-I", "-S", "-B", "-c", DROP_TEST_IDENTITY,
                "/usr/bin/python3", "-I", "-S", "-B", "-c", helper, uid, path);
        assertThat(result.getExitCode()).isEqualTo(allowed ? 0 : 2);
        assertThat(result.getStdout()).isEqualTo("ASSET_CARD_SYSTEMD_CREDENTIAL_METADATA=" + (allowed ? "PASS\n" : "FAIL\n"));
        assertThat(result.getStderr()).isEmpty();
    }

    private static void assertJavaCredential(org.testcontainers.containers.GenericContainer<?> runtime, boolean allowed) throws Exception {
        var result = runtime.execInContainer("/usr/bin/env", "-i", "LC_ALL=C", "CREDENTIALS_DIRECTORY=/run/credentials/rine-logic.service",
                "/usr/bin/python3", "-I", "-S", "-B", "-c", DROP_TEST_IDENTITY,
                "/opt/java/openjdk/bin/java", "-cp", "/fixture/classes:/fixture/test-classes:/fixture/lib/*",
                LinuxCredentialProbe.class.getName(), "/run/credentials/rine-logic.service/asset-card-db-password", "999");
        assertThat(result.getExitCode()).isEqualTo(allowed ? 0 : 2);
        assertThat(result.getStdout()).isEqualTo(allowed ? "JAVA_CREDENTIAL_READ=PASS\n"
                : "JAVA_CREDENTIAL_READ=FAIL:IllegalArgumentException\n");
        assertThat(result.getStderr()).isEmpty();
    }

    private static void copyCredentialProbe(org.testcontainers.containers.GenericContainer<?> runtime) throws Exception {
        String packagePath = "org/example/trademodel/assetcard/";
        try (var files = Files.list(Path.of("target/classes/" + packagePath))) {
            for (Path file : files.filter(p -> p.getFileName().toString().startsWith("AssetCardDataSourceConfiguration")
                    || p.getFileName().toString().startsWith("AssetCardProperties")).toList())
                runtime.withCopyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(file),
                        "/fixture/classes/" + packagePath + file.getFileName());
        }
        runtime.withCopyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(
                Path.of("target/test-classes/" + LinuxCredentialProbe.class.getName().replace('.', '/') + ".class")),
                "/fixture/test-classes/" + LinuxCredentialProbe.class.getName().replace('.', '/') + ".class");
        for (String path : System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")).split(java.io.File.pathSeparator)) {
            String name = Path.of(path).getFileName().toString();
            if (name.startsWith("spring-") || name.startsWith("HikariCP-") || name.startsWith("slf4j-api-"))
                runtime.withCopyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(path), "/fixture/lib/" + name);
        }
    }

    public static final class LinuxCredentialProbe {
        public static void main(String[] arguments) {
            char[] password = null;
            try {
                password = AssetCardDataSourceConfiguration.readCredential(Path.of(arguments[0]), arguments[1]);
                if (password.length < 1) throw new IllegalStateException();
                System.out.println("JAVA_CREDENTIAL_READ=PASS");
            } catch (Throwable failure) {
                System.out.println("JAVA_CREDENTIAL_READ=FAIL:" + failure.getClass().getSimpleName());
                System.exit(2);
            } finally { if (password != null) java.util.Arrays.fill(password, '\0'); }
        }
    }

    @Test
    void candidateAclFailurePreservesOldPoolAndSuccessfulRotationDrainsLeasedOldConnection() throws Exception {
        var properties = properties();
        var old = pool("PASS");
        var rejected = pool("CARD_EFFECTIVE_PRIVILEGES_EXCESSIVE");
        var replacement = pool("PASS");
        var factory = mock(AssetCardDataSourceConfiguration.PoolFactory.class);
        when(factory.create(any(), any())).thenReturn(old, rejected, replacement);
        try (var writer = new AssetCardDataSourceConfiguration.AssetCardWriter(properties, factory)) {
            assertThat(writer.readiness().allowed()).isTrue();
            Connection lease = writer.jdbcTemplate().getDataSource().getConnection();
            assertThat(writer.rotate().allowed()).isFalse();
            verify(rejected).close(); verify(old, never()).close();
            assertThat(writer.readiness().allowed()).isTrue();
            assertThat(writer.rotate().allowed()).isTrue();
            verify(old, never()).close();
            lease.close(); verify(old).close();
        }
        verify(replacement).close();
    }

    @Test
    void neitherConstructionNorReadsInitializePoolBeforeBackgroundReadiness() throws Exception {
        var p = properties();
        var candidate = pool("PASS");
        var factory = mock(AssetCardDataSourceConfiguration.PoolFactory.class);
        when(factory.create(any(), any())).thenReturn(candidate);
        try (var writer = new AssetCardDataSourceConfiguration.AssetCardWriter(p, factory)) {
            verifyNoInteractions(factory);
            assertThatThrownBy(() -> writer.jdbcTemplate().getDataSource().getConnection()).hasMessage("ASSET_CARD_WRITER_UNAVAILABLE");
            verifyNoInteractions(factory);
            assertThat(writer.readiness().allowed()).isTrue();
            verify(factory, times(1)).create(any(), any());
            assertThat(writer.readiness().allowed()).isTrue();
            verify(factory, times(1)).create(any(), any());
        }
        verify(candidate).close();
    }

    @Test
    void invalidConfigurationNeverAttemptsConnectionOrFallsBackAndPoolBoundsAreExplicit() {
        var p = new AssetCardProperties(); p.setWriterEnabled(true);
        var factory = mock(AssetCardDataSourceConfiguration.PoolFactory.class);
        try (var writer = new AssetCardDataSourceConfiguration.AssetCardWriter(p, factory)) {
            assertThat(writer.readiness().allowed()).isFalse();
            verifyNoInteractions(factory);
            assertThatThrownBy(() -> writer.jdbcTemplate().getDataSource().getConnection()).hasMessage("ASSET_CARD_WRITER_UNAVAILABLE");
        }
        assertThatThrownBy(() -> p.getWriter().setMaximumPoolSize(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.getWriter().setMaximumPoolSize(5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.getWriter().setConnectionTimeout(java.time.Duration.ofSeconds(31))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifierFailsClosedForNonPostgresAndUsesNoPermissionMutationSql() throws Exception {
        Connection connection = mock(Connection.class); DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata); when(metadata.getDatabaseProductName()).thenReturn("H2");
        assertThat(AssetCardDataSourceConfiguration.verify(connection, "isolated").allowed()).isFalse();
        verify(connection, never()).createStatement();
        String sql = AssetCardDataSourceConfiguration.PERMISSION_SQL;
        assertThat(sql).contains("has_column_privilege", "pg_auth_members", "has_sequence_privilege", "TEMPORARY",
                "tm_asset_card_snapshot", "tm_asset_card_spot_bar", "tm_asset_card_feature_history");
        assertThat(sql.toUpperCase(java.util.Locale.ROOT)).doesNotContain("GRANT ", "REVOKE ", "ALTER ", "SET ROLE");
    }

    @Test
    void realSpringPostgresUsesExactRoleWhileDefaultConnectionRetainsOnlyCanonicalReadAccess() throws Exception {
        try (var fixture = WriterFixture.open(temporary)) {
            var context = fixture.context();
            assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
            assertThat(context.getBeansOfType(JdbcTemplate.class)).hasSize(1);
            JdbcTemplate defaults = context.getBean(JdbcTemplate.class);
            assertThat(defaults).isNotSameAs(fixture.writer().jdbcTemplate());
            assertThat(fixture.mapper().inspectWriterPermissions().writable()).isTrue();
            assertThat(fixture.mapper().inspectWriterPermissions().cleanupAllowed()).isTrue();
            assertThat(defaults.queryForObject("SELECT current_user", String.class)).startsWith("card_reader_");
            assertThat(fixture.writer().jdbcTemplate().queryForObject("SELECT current_user", String.class))
                    .isEqualTo(AssetCardDataSourceConfiguration.WRITER_ROLE);
            String canonicalSymbol = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(java.util.Locale.ROOT);
            var cutoff = java.time.Instant.parse("2026-01-02T03:05:00Z");
            var availableAt = java.time.LocalDateTime.ofInstant(cutoff, java.time.ZoneOffset.UTC);
            fixture.admin().update("""
                    INSERT INTO tm_persisted_ohlcv_bar(symbol,timeframe,open_time_ms,close_time_ms,
                      open_price,high_price,low_price,close_price,volume,is_closed,provider,provider_market_type,
                      source_endpoint,source_batch_id,source_trace_id,source_version,ingested_at,quality_status,
                      fetch_time,source_status,provenance_version)
                    VALUES (?,'5m',?,?,100,102,99,101,10,TRUE,'BINANCE_PUBLIC','SPOT',
                      'public-spot-kline','isolated','isolated',1,?,'OK',?,'READY','v1')
                    """, canonicalSymbol, cutoff.minusSeconds(300).toEpochMilli(), cutoff.minusMillis(1).toEpochMilli(),
                    availableAt, availableAt);
            assertThat(fixture.mapper().selectExistingSpotBars(canonicalSymbol, "5m", cutoff, 10)).singleElement().satisfies(bar -> {
                assertThat(bar.symbol()).isEqualTo(canonicalSymbol);
                assertThat(bar.close()).isEqualByComparingTo("101");
                assertThat(bar.availableAt()).isEqualTo(cutoff);
            });
            long version = fixture.mapper().nextSnapshotVersion("BTCUSDT");
            assertThat(fixture.mapper().saveSnapshot("BTCUSDT", 0, version, "{}", java.time.Instant.now())).isEqualTo(1);
            assertThatThrownBy(() -> defaults.queryForObject("SELECT count(*) FROM tm_asset_card_snapshot", Integer.class))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThatThrownBy(() -> fixture.writer().jdbcTemplate().queryForObject("SELECT count(*) FROM tm_persisted_ohlcv_bar", Integer.class))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThatThrownBy(() -> fixture.writer().jdbcTemplate().update("DELETE FROM tm_asset_card_snapshot"))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThatThrownBy(() -> fixture.writer().jdbcTemplate().update("UPDATE tm_asset_card_spot_bar SET trade_count=1"))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
        }
    }

    @Test
    void realCatalogVerificationRejectsExcessAclColumnGrantGrantOptionMembershipAndSecurityDefiner() throws Exception {
        try (var fixture = WriterFixture.open(temporary)) {
            for (String[] mutation : new String[][] {
                    {"GRANT DELETE ON tm_asset_card_snapshot TO rine_asset_card_writer", "REVOKE DELETE ON tm_asset_card_snapshot FROM rine_asset_card_writer"},
                    {"GRANT UPDATE ON tm_asset_card_spot_bar TO rine_asset_card_writer", "REVOKE UPDATE ON tm_asset_card_spot_bar FROM rine_asset_card_writer"},
                    {"GRANT UPDATE ON tm_asset_card_feature_history TO rine_asset_card_writer", "REVOKE UPDATE ON tm_asset_card_feature_history FROM rine_asset_card_writer"},
                    {"GRANT SELECT(value) ON existing_private_business TO rine_asset_card_writer", "REVOKE SELECT(value) ON existing_private_business FROM rine_asset_card_writer"},
                    {"GRANT SELECT ON tm_asset_card_snapshot TO rine_asset_card_writer WITH GRANT OPTION", "REVOKE GRANT OPTION FOR SELECT ON tm_asset_card_snapshot FROM rine_asset_card_writer"},
                    {"GRANT USAGE ON existing_private_sequence TO rine_asset_card_writer", "REVOKE USAGE ON existing_private_sequence FROM rine_asset_card_writer"}
            }) {
                fixture.admin().execute(mutation[0]);
                try { assertThat(fixture.writer().readiness().allowed()).isFalse(); }
                finally { fixture.admin().execute(mutation[1]); }
                assertThat(fixture.writer().readiness().allowed()).isTrue();
            }
            fixture.admin().execute("CREATE FUNCTION public.private_reader() RETURNS int LANGUAGE sql SECURITY DEFINER AS 'SELECT value FROM existing_private_business LIMIT 1'");
            assertThat(fixture.writer().readiness().allowed()).isFalse();
            assertThat(fixture.freshVerification().reason()).isEqualTo("CARD_SECURITY_DEFINER_EXECUTE_FORBIDDEN");
            fixture.admin().execute("DROP FUNCTION public.private_reader()");
            fixture.admin().execute("CREATE ROLE test_parent NOLOGIN");
            fixture.admin().execute("GRANT test_parent TO rine_asset_card_writer");
            assertThat(fixture.writer().readiness().allowed()).isFalse();
            fixture.admin().execute("REVOKE test_parent FROM rine_asset_card_writer");
            assertThat(fixture.writer().readiness().allowed()).isTrue();
        }
    }

    @Test
    void realRequiredPermissionsCannotBeMissingAndPublicOrSchemaPrivilegesCannotWidenWriter() throws Exception {
        try (var fixture = WriterFixture.open(temporary)) {
            for (String table : java.util.List.of("tm_asset_card_snapshot", "tm_asset_card_spot_bar", "tm_asset_card_feature_history")) {
                for (String permission : java.util.List.of("SELECT", "INSERT", table.endsWith("snapshot") ? "UPDATE" : "DELETE")) {
                    fixture.admin().execute("REVOKE " + permission + " ON " + table + " FROM rine_asset_card_writer");
                    try {
                        assertThat(fixture.writer().readiness().allowed()).as(table + " missing " + permission).isFalse();
                        assertThatThrownBy(() -> fixture.mapper().nextSnapshotVersion("BTCUSDT"))
                                .isInstanceOf(org.springframework.dao.DataAccessException.class);
                    } finally { fixture.admin().execute("GRANT " + permission + " ON " + table + " TO rine_asset_card_writer"); }
                    assertThat(fixture.writer().readiness().allowed()).isTrue();
                }
            }
            for (String[] mutation : new String[][] {
                    {"GRANT SELECT ON existing_private_business TO PUBLIC", "REVOKE SELECT ON existing_private_business FROM PUBLIC"},
                    {"GRANT CREATE ON SCHEMA public TO rine_asset_card_writer", "REVOKE CREATE ON SCHEMA public FROM rine_asset_card_writer"},
                    {"GRANT TEMP ON DATABASE " + fixture.database.getDatabaseName() + " TO rine_asset_card_writer",
                            "REVOKE TEMP ON DATABASE " + fixture.database.getDatabaseName() + " FROM rine_asset_card_writer"}
            }) {
                fixture.admin().execute(mutation[0]);
                try { assertThat(fixture.writer().readiness().allowed()).isFalse(); }
                finally { fixture.admin().execute(mutation[1]); }
                assertThat(fixture.writer().readiness().allowed()).isTrue();
            }
            assertThat(fixture.admin().queryForObject("SELECT count(*) FROM tm_asset_card_snapshot", Integer.class)).isZero();
        }
    }

    @Test
    void actualWriterConnectionLossDoesNotFallbackAndCanRecoverAfterCredentialRoleRestoration() throws Exception {
        try (var fixture = WriterFixture.open(temporary)) {
            fixture.admin().execute("ALTER ROLE rine_asset_card_writer NOLOGIN");
            fixture.admin().queryForList("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE usename='rine_asset_card_writer'");
            try {
                assertThat(fixture.writer().readiness().allowed()).isFalse();
                assertThatThrownBy(() -> fixture.mapper().nextSnapshotVersion("BTCUSDT"))
                        .isInstanceOf(org.springframework.dao.DataAccessException.class);
                assertThat(fixture.context().getBean(JdbcTemplate.class).queryForObject("SELECT count(*) FROM tm_persisted_ohlcv_bar", Integer.class)).isZero();
                assertThat(fixture.admin().queryForObject("SELECT count(*) FROM tm_asset_card_snapshot", Integer.class)).isZero();
            } finally { fixture.admin().execute("ALTER ROLE rine_asset_card_writer LOGIN"); }
            assertThat(fixture.writer().rotate().allowed()).isTrue();
            assertThat(fixture.writer().readiness().allowed()).isTrue();
        }
    }

    @Test
    void realAuthenticationFailureAndRotationNeverFallbackAndOldLeaseClosesAfterSuccessfulReplacement() throws Exception {
        try (var fixture = WriterFixture.open(temporary)) {
            var writer = fixture.writer();
            try (Connection oldLease = writer.jdbcTemplate().getDataSource().getConnection()) {
                Files.writeString(fixture.credential(), "deliberately-wrong-local-test-password");
                assertThat(writer.rotate().allowed()).isFalse();
                assertThat(writer.readiness().allowed()).isTrue();
                assertThat(fixture.admin().queryForObject("SELECT count(*) FROM tm_asset_card_snapshot", Integer.class)).isZero();
                String replacement = "test_" + UUID.randomUUID().toString().replace("-", "");
                fixture.admin().execute("ALTER ROLE rine_asset_card_writer PASSWORD '" + replacement + "'");
                Files.writeString(fixture.credential(), replacement);
                assertThat(writer.rotate().allowed()).isTrue();
                assertThat(oldLease.isClosed()).isFalse();
                assertThat(writer.jdbcTemplate().queryForObject("SELECT current_user", String.class)).isEqualTo("rine_asset_card_writer");
            }
            writer.close();
            assertThatThrownBy(() -> writer.jdbcTemplate().getDataSource().getConnection()).hasMessage("ASSET_CARD_WRITER_DISABLED");
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(fixture.admin().queryForObject("SELECT count(*) FROM pg_stat_activity WHERE usename='rine_asset_card_writer'", Integer.class)).isZero());
            assertThat(fixture.context().getBean(JdbcTemplate.class).queryForObject("SELECT count(*) FROM tm_persisted_ohlcv_bar", Integer.class)).isZero();
        }
    }

    @Test
    void realWrongPasswordAtFirstBackgroundInitializationReturnsUnavailableWithoutTouchingDefaultPool() throws Exception {
        try (var fixture = WriterFixture.open(temporary)) {
            Files.writeString(fixture.credential(), "wrong-local-initial-password");
            try (var rejected = new AssetCardDataSourceConfiguration.AssetCardWriter(fixture.properties())) {
                assertThat(rejected.readiness().allowed()).isFalse();
                assertThatThrownBy(() -> rejected.jdbcTemplate().queryForObject("SELECT 1", Integer.class))
                        .isInstanceOf(org.springframework.dao.DataAccessException.class);
                assertThat(fixture.context().getBean(JdbcTemplate.class).queryForObject("SELECT count(*) FROM tm_persisted_ohlcv_bar", Integer.class)).isZero();
                assertThat(fixture.admin().queryForObject("SELECT count(*) FROM tm_asset_card_snapshot", Integer.class)).isZero();
                Files.writeString(fixture.credential(), fixture.password);
                assertThat(rejected.readiness().reason()).isEqualTo("CARD_WRITER_RETRY_PENDING");
                // Simulate the next background cadence without sleeping or weakening the real authentication check.
                org.springframework.test.util.ReflectionTestUtils.setField(rejected, "nextAutomaticAttempt", 0L);
                assertThat(rejected.readiness().allowed()).isTrue();
            }
        }
    }

    /** Shared with the SHADOW pipeline test: actual Boot beans, Hikari, SCRAM and exact-role PostgreSQL. */
    static final class WriterFixture implements AutoCloseable {
        private final org.testcontainers.containers.PostgreSQLContainer<?> database;
        private final org.springframework.context.annotation.AnnotationConfigApplicationContext context;
        private final AssetCardProperties properties;
        private final JdbcTemplate admin;
        private final Path credential;
        private final String password;
        private final java.util.List<java.util.Map<String,Object>> originalPrivateRows;
        private final java.util.List<java.util.Map<String,Object>> originalLegacyAcl;
        private WriterFixture(org.testcontainers.containers.PostgreSQLContainer<?> database,
                org.springframework.context.annotation.AnnotationConfigApplicationContext context,
                AssetCardProperties properties, JdbcTemplate admin, Path credential, String password) {
            this.database=database; this.context=context; this.properties=properties; this.admin=admin; this.credential=credential; this.password=password;
            originalPrivateRows = admin.queryForList("SELECT * FROM existing_private_business");
            originalLegacyAcl = legacyAcl();
        }
        static WriterFixture open(Path temporary) throws Exception {
            var database = new org.testcontainers.containers.PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("card_" + UUID.randomUUID().toString().replace("-", ""))
                    .withEnv("POSTGRES_INITDB_ARGS", "--auth-host=scram-sha-256");
            org.springframework.context.annotation.AnnotationConfigApplicationContext context = null;
            try {
                database.start();
                JdbcTemplate admin = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        database.getJdbcUrl(), database.getUsername(), database.getPassword()));
                try (Connection connection = admin.getDataSource().getConnection()) {
                    assertThat(connection.getMetaData().getURL()).isEqualTo(database.getJdbcUrl());
                }
                for (String sql : Files.readString(Path.of("src/main/resources/db/migration/V24__asset_card_live_signal.sql")).split(";"))
                    if (!sql.isBlank()) admin.execute(sql);
                String schema = Files.readString(Path.of("src/main/resources/db/migration/V1__baseline_schema_tables.sql"));
                int start = schema.indexOf("CREATE TABLE IF NOT EXISTS tm_persisted_ohlcv_bar (");
                admin.execute(schema.substring(start, schema.indexOf(";", start) + 1));
                for (String sql : Files.readString(Path.of("src/main/resources/db/migration/V4__ohlcv_ingestion_provenance.sql")).split(";"))
                    if (!sql.isBlank()) admin.execute(sql);
                admin.execute("CREATE TABLE existing_private_business(value int)");
                admin.update("INSERT INTO existing_private_business(value) VALUES (?)", UUID.randomUUID().hashCode());
                admin.execute("CREATE SEQUENCE existing_private_sequence");
                admin.execute("REVOKE TEMP ON DATABASE " + database.getDatabaseName() + " FROM PUBLIC");
                for (String file : java.util.List.of("asset-card-role-bootstrap.sql", "asset-card-role-verify.sql"))
                    database.copyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(Path.of("deploy/native-staging", file)), "/tmp/" + file);
                var result = database.execInContainer("psql", "-X", "-v", "ON_ERROR_STOP=1", "-U", database.getUsername(), "-d", database.getDatabaseName(),
                        "-f", "/tmp/asset-card-role-bootstrap.sql");
                assertThat(result.getExitCode()).as("disposable exact-role bootstrap").isZero();
                String password = "local_" + UUID.randomUUID().toString().replace("-", "");
                admin.execute("ALTER ROLE rine_asset_card_writer PASSWORD '" + password + "'");
                String reader = "card_reader_" + UUID.randomUUID().toString().replace("-", "");
                String readerPassword = "local_" + UUID.randomUUID().toString().replace("-", "");
                admin.execute("CREATE ROLE " + reader + " LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT PASSWORD '" + readerPassword + "'");
                admin.execute("GRANT USAGE ON SCHEMA public TO " + reader);
                admin.execute("GRANT SELECT ON tm_persisted_ohlcv_bar TO " + reader);
                Path credential = temporary.toRealPath().resolve("asset-card-db-password-" + UUID.randomUUID());
                Files.writeString(credential, password); Files.setPosixFilePermissions(credential, PosixFilePermissions.fromString("rw-------"));
                var properties = new AssetCardProperties(); properties.setEnabled(true); properties.setWriterEnabled(true);
                properties.setBarRetention(java.time.Duration.ofHours(6)); properties.setFeatureRetention(java.time.Duration.ofHours(6));
                properties.setTradeRetention(java.time.Duration.ofHours(6)); properties.setLabelRetention(java.time.Duration.ofHours(6));
                String writerUrl = "jdbc:postgresql://" + database.getHost() + ":" + database.getMappedPort(5432) + "/" + database.getDatabaseName();
                properties.getWriter().setJdbcUrl(writerUrl); properties.getWriter().setExpectedDatabase(database.getDatabaseName());
                properties.getWriter().setCredentialFile(credential); properties.getWriter().setCredentialOwner(Files.getAttribute(credential,"unix:uid").toString());
                properties.getWriter().setConnectionTimeout(java.time.Duration.ofMillis(300));
                context = new org.springframework.context.annotation.AnnotationConfigApplicationContext();
                context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("isolated-reader", java.util.Map.of(
                        "spring.datasource.url", database.getJdbcUrl(), "spring.datasource.username", reader,
                        "spring.datasource.password", readerPassword, "spring.datasource.hikari.read-only", "true",
                        "spring.datasource.hikari.maximum-pool-size", "2")));
                context.registerBean(AssetCardProperties.class, () -> properties);
                context.register(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
                        AssetCardDataSourceConfiguration.class, org.example.trademodel.mapper.AssetCardMapper.class);
                context.refresh();
                var fixture = new WriterFixture(database, context, properties, admin, credential, password);
                var readiness = fixture.writer().readiness();
                assertThat(readiness.allowed()).as("actual fresh writer connection and ACL: %s", readiness.reason()).isTrue();
                return fixture;
            } catch (Exception | AssertionError failure) { if (context != null) context.close(); database.close(); throw failure; }
        }
        AssetCardProperties properties() { return properties; }
        org.example.trademodel.mapper.AssetCardMapper mapper() { return context.getBean(org.example.trademodel.mapper.AssetCardMapper.class); }
        org.springframework.context.annotation.AnnotationConfigApplicationContext context() { return context; }
        JdbcTemplate admin() { return admin; }
        AssetCardDataSourceConfiguration.AssetCardWriter writer() { return context.getBean(AssetCardDataSourceConfiguration.AssetCardWriter.class); }
        Path credential() { return credential; }
        AssetCardDataSourceConfiguration.Verification freshVerification() throws Exception {
            try (Connection connection = java.sql.DriverManager.getConnection(database.getJdbcUrl(), "rine_asset_card_writer", password)) {
                return AssetCardDataSourceConfiguration.verify(connection, database.getDatabaseName());
            }
        }
        public void close() {
            try {
                assertThat(admin.queryForList("SELECT * FROM existing_private_business")).isEqualTo(originalPrivateRows);
                assertThat(legacyAcl()).isEqualTo(originalLegacyAcl);
            } finally {
                context.close(); database.close();
                try { Files.deleteIfExists(credential); } catch (java.io.IOException failure) { throw new IllegalStateException("Test credential cleanup failed"); }
            }
        }
        private java.util.List<java.util.Map<String,Object>> legacyAcl() {
            return admin.queryForList("""
                    SELECT c.oid,c.relname,c.relowner,COALESCE(c.relacl,acldefault(CASE WHEN c.relkind='S' THEN 's'::\"char\" ELSE 'r'::\"char\" END,c.relowner))::text AS effective_acl
                    FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                    WHERE n.nspname='public' AND c.relkind IN ('r','S') AND c.relname NOT LIKE 'tm_asset_card_%'
                    ORDER BY c.oid
                    """);
        }
    }

    private AssetCardProperties properties() throws Exception {
        Path credential = temporary.toRealPath().resolve("writer-" + UUID.randomUUID());
        Files.writeString(credential, "synthetic-rotation-password");
        Files.setPosixFilePermissions(credential, PosixFilePermissions.fromString("rw-------"));
        var p = new AssetCardProperties(); p.setWriterEnabled(true);
        p.getWriter().setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/isolated");
        p.getWriter().setExpectedDatabase("isolated"); p.getWriter().setCredentialFile(credential);
        p.getWriter().setCredentialOwner(Files.getOwner(credential).getName());
        return p;
    }

    private AssetCardDataSourceConfiguration.CandidatePool pool(String reason) throws Exception {
        var pool = mock(AssetCardDataSourceConfiguration.CandidatePool.class);
        when(pool.connection()).thenAnswer(invocation -> {
            Connection c = mock(Connection.class); DatabaseMetaData m = mock(DatabaseMetaData.class);
            when(c.getMetaData()).thenReturn(m); when(m.getDatabaseProductName()).thenReturn("PostgreSQL");
            var statement = mock(java.sql.PreparedStatement.class); var result = mock(ResultSet.class);
            when(c.prepareStatement(anyString())).thenReturn(statement); when(statement.executeQuery()).thenReturn(result);
            when(result.next()).thenReturn(true, false); when(result.getString(1)).thenReturn(reason);
            return c;
        });
        return pool;
    }
}
