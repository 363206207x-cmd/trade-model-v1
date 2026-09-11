package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.images.builder.Transferable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@org.junit.jupiter.api.Tag("core-regression")
class V24AssetCardLiveSignalMigrationContractTest {
    @org.junit.jupiter.api.io.TempDir Path archiveDirectory;
    private static final String OLD_SHA256="b9e49308b0ce84e3d2033a07a571d009d8d1483b5770010fd7c74555dba0105b";
    private static final List<String> CARD_TABLES=List.of("tm_asset_card_snapshot","tm_asset_card_spot_bar","tm_asset_card_feature_history");

    @Test
    void independentDisposableWatchdogReclaimsCommittedCreateAfterControllerIsKilledWithoutFinally() throws Exception {
        requireDocker();
        try(var database=new PostgreSQLContainer<>("postgres:16-alpine")) {
            database.start();
            execute(database,"CREATE ROLE rine_migrator NOLOGIN NOSUPERUSER; REVOKE CREATE ON SCHEMA public FROM PUBLIC; REVOKE CREATE ON SCHEMA public FROM rine_migrator");
            assertCreateReclaimed(database);
            CountDownLatch controllerGone=new CountDownLatch(1), armed=new CountDownLatch(1);
            AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread watchdog=new Thread(()-> {
                armed.countDown();
                try {
                    // Isolated external-watchdog analogue: no shared controller connection/finally.
                    // A timeout also revokes, even if the failed controller never sends the latch.
                    controllerGone.await(10,TimeUnit.SECONDS);
                    execute(database,"REVOKE CREATE ON SCHEMA public FROM rine_migrator");
                    assertCreateReclaimed(database); // A separate, fresh connection after COMMIT.
                } catch(Throwable problem) { failure.set(problem); }
            },"disposable-independent-create-watchdog");
            watchdog.start();
            try {
                assertThat(armed.await(5,TimeUnit.SECONDS)).isTrue();
                var controller=database.execInContainer("sh","-c","psql -X -v ON_ERROR_STOP=1 -U test -d test -c 'GRANT CREATE ON SCHEMA public TO rine_migrator' && kill -KILL $$");
                assertThat(controller.getExitCode()).isEqualTo(137);
                assertThat(scalar(database,"SELECT has_schema_privilege('rine_migrator','public','CREATE')")).isEqualTo("t");
                controllerGone.countDown(); watchdog.join(15_000);
                assertThat(watchdog.isAlive()).isFalse(); assertThat(failure.get()).isNull();
                assertCreateReclaimed(database);
                System.out.println("V24_CONTROLLER_SIGKILL_WATCHDOG=PASS_ISOLATED_SIMULATION\nREAL_SYSTEMD_WATCHDOG=NOT_EXECUTED\nHOST_POWER_LOSS_RECOVERY=NOT_EXECUTED");
            } finally { controllerGone.countDown(); watchdog.join(15_000); }
        }
    }

    @Test
    void v24TemporaryCreateIsReclaimedAfterFailureInterruptionAndSuccessAndOnlyNewTableAclsConverge() throws Exception {
        requireDocker();
        try(var database=new PostgreSQLContainer<>("postgres:16-alpine")) {
            database.start();
            String password="LOCAL_ONLY_"+UUID.randomUUID().toString().replace("-","");
            initializeStagingAclFixture(database,password);
            String baseline=legacyIdentity(database), defaults=defaultAcls(database);
            try(var blocker=DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword());
                var statement=blocker.createStatement()) {
                blocker.setAutoCommit(false);
                statement.execute("LOCK TABLE public.flyway_schema_history IN ACCESS EXCLUSIVE MODE");
                try {
                    assertThatThrownBy(()->createWindow(database,()->Flyway.configure()
                            .dataSource(database.getJdbcUrl(),"rine_migrator",password)
                            .locations("classpath:db/migration").target("24")
                            .initSql("SET statement_timeout='2s'; SET lock_timeout='1s'").load().migrate()))
                            .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
                } finally { blocker.rollback(); } // Releases only this disposable blocking fixture transaction.
            }
            assertCreateReclaimed(database);
            assertThat(scalar(database,"SELECT count(*) FROM pg_class WHERE relname LIKE 'tm_asset_card_%'")).isEqualTo("0");

            CountDownLatch entered=new CountDownLatch(1);
            AtomicReference<Throwable> interrupted=new AtomicReference<>();
            Thread worker=new Thread(()-> {
                try { createWindow(database,()-> { entered.countDown(); new CountDownLatch(1).await(); }); }
                catch(Throwable expected) { interrupted.set(expected); }
            },"disposable-v24-interrupted-window");
            worker.start();
            try {
                assertThat(entered.await(10,TimeUnit.SECONDS)).isTrue();
                worker.interrupt(); worker.join(10_000);
                assertThat(worker.isAlive()).isFalse();
                assertThat(interrupted.get()).isInstanceOf(InterruptedException.class);
                assertCreateReclaimed(database);
            } finally { if(worker.isAlive()) { worker.interrupt(); worker.join(10_000); } }

            createWindow(database,()->migrations(database,password,"24").migrate());
            assertCreateReclaimed(database);
            assertThat(scalar(database,"SELECT count(*) FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname LIKE 'tm_asset_card_%'")).isEqualTo("3");
            assertThat(scalar(database,"SELECT string_agg(i.relname,',' ORDER BY i.relname) FROM pg_index x JOIN pg_class i ON i.oid=x.indexrelid JOIN pg_class t ON t.oid=x.indrelid WHERE t.relnamespace='public'::regnamespace AND t.relname LIKE 'tm_asset_card_%' AND NOT x.indisprimary"))
                    .isEqualTo("idx_asset_card_feature_available,idx_asset_card_spot_available");
            for(String table:CARD_TABLES) for(String privilege:List.of("SELECT","INSERT","UPDATE","DELETE"))
                assertThat(scalar(database,"SELECT has_table_privilege('rine_app','public."+table+"','"+privilege+"')")).isEqualTo("t");
            bootstrapNewTables(database,false);
            // Default bootstrap cannot silently opt into changing the new tables' legacy-app ACL.
            for(String table:CARD_TABLES)
                assertThat(scalar(database,"SELECT has_table_privilege('rine_app','public."+table+"','DELETE')")).isEqualTo("t");
            convergeNewTables(database);
            for(String table:CARD_TABLES)
                assertThat(scalar(database,"SELECT has_table_privilege('rine_app','public."+table+"','SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')")).isEqualTo("f");
            execute(database,"ALTER ROLE rine_asset_card_writer PASSWORD '"+password+"'");
            var poolConfig=new com.zaxxer.hikari.HikariConfig();
            poolConfig.setJdbcUrl(database.getJdbcUrl()); poolConfig.setUsername("rine_asset_card_writer");
            poolConfig.setPassword(password); poolConfig.setMaximumPoolSize(1); poolConfig.setMinimumIdle(0);
            poolConfig.setConnectionTimeout(2000); poolConfig.setPoolName("disposable-v24-retention-permission-probe");
            try(var pool=new com.zaxxer.hikari.HikariDataSource(poolConfig)) {
                var jdbc=new org.springframework.jdbc.core.JdbcTemplate(pool);
                var mapper=new org.example.trademodel.mapper.AssetCardMapper(jdbc);
                assertThat(mapper.inspectWriterPermissions().cleanupAllowed()).isTrue();
                var usage=mapper.storageUsage();
                assertThat(usage.postgres()).isTrue();
                assertThat(usage.totalRows()).isZero();
                assertThat(usage.databaseBytes()).isPositive(); assertThat(usage.walBytes()).isPositive();
                assertThat(mapper.withRetentionLock("TESTRETENTIONUSDT",()->jdbc.queryForObject("SELECT current_user",String.class)))
                        .isEqualTo("rine_asset_card_writer");
                var now=java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
                var old=now.minusSeconds(43_200);
                mapper.saveFeatureHistory("TESTRETENTIONUSDT",old,old,"{\"isolatedArchive\":true}");
                var rows=mapper.selectHistory("TESTRETENTIONUSDT",org.example.trademodel.mapper.AssetCardMapper.HistoryKind.FEATURE,old,old,now,10);
                assertThat(rows).hasSize(1);
                byte[] archiveBytes=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
                        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .writeValueAsBytes(new org.example.trademodel.mapper.AssetCardMapper.VerifiedArchive(1,"ASSET_CARD_HISTORY_ARCHIVE_V1","TESTRETENTIONUSDT",1,0,rows,List.of()));
                String archiveSha=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(archiveBytes));
                Path archiveFile=archiveDirectory.resolve(archiveSha+".json"); Files.write(archiveFile,archiveBytes);
                var confirmation=new org.example.trademodel.mapper.AssetCardMapper.ArchiveConfirmation("TESTRETENTIONUSDT",
                        org.example.trademodel.mapper.AssetCardMapper.HistoryKind.FEATURE,rows.stream().map(org.example.trademodel.mapper.AssetCardMapper.TypedHistory::recordKey).toList(),
                        old,now.minusSeconds(21_600),now,archiveSha,now,archiveFile);
                // One pool connection must suffice for lock, exact effective ACL verification and deletion.
                int deleted=mapper.withRetentionLock("TESTRETENTIONUSDT",()->mapper.pruneArchivedHistory(confirmation,1));
                assertThat(deleted).isEqualTo(1);
                assertThat(mapper.storageUsage().historyRows()).isZero();
                assertThat(org.example.trademodel.mapper.AssetCardMapper.readVerifiedArchive(archiveFile,archiveSha,"TESTRETENTIONUSDT").history()).containsExactlyElementsOf(rows);
            }
            assertThat(legacyIdentity(database)).isEqualTo(baseline);
            assertThat(defaultAcls(database)).isEqualTo(defaults);
            assertThat(scalar(database,"SELECT count(*) FROM public.flyway_schema_history WHERE version='24' AND success")).isEqualTo("1");
            System.out.println("V24_DEFAULT_ACL_REHEARSAL=PASS\nV24_CREATE_SUCCESS_FAILURE_INTERRUPT_RECLAIMED=YES\nV24_FRESH_CONNECTION_PERMISSION_VERIFICATION=PASS\nNEW_CARD_TABLE_ACL_CONVERGENCE=PASS\nLEGACY_DATA_AND_DEFAULT_ACLS_UNCHANGED=YES\nSINGLE_CONNECTION_RETENTION_AND_STORAGE_PROBE=PASS");
        }
    }

    @Test
    void exactRetainedOldJarStartsWithItsUnchangedFlywayDefaultsAfterV24AndPreservesCardData() throws Exception {
        String supplied=System.getProperty("assetCard.rollbackJar");
        assumeTrue(supplied!=null,"ACTUAL_OLD_JAR=NOT_EXECUTED: provide the retained exact b9e49308 artifact; never substitute rebuilt or current code");
        Path jar=Path.of(supplied).toAbsolutePath().normalize();
        assertThat(jar).isRegularFile(); assertThat(Files.isSymbolicLink(jar)).isFalse();
        assertThat(hash(jar)).isEqualTo(OLD_SHA256);
        try(var archive=new java.util.jar.JarFile(jar.toFile())) {
            assertThat(archive.getJarEntry("BOOT-INF/classes/db/migration/V23__coinglass_runtime_snapshot.sql")).isNotNull();
            assertThat(archive.getJarEntry("BOOT-INF/classes/db/migration/V24__asset_card_live_signal.sql")).isNull();
            assertThat(archive.getManifest().getMainAttributes().getValue("Start-Class")).isEqualTo("org.example.trademodel.TradeModelApplication");
            int historicalMigrations=0;
            var entries=archive.entries();
            while(entries.hasMoreElements()) {
                var entry=entries.nextElement();
                if(!entry.getName().matches("BOOT-INF/classes/db/migration/V[0-9]+__[^/]+\\.sql")) continue;
                try(var input=archive.getInputStream(entry)) {
                    assertThat(input.readAllBytes()).isEqualTo(Files.readAllBytes(Path.of("src/main/resources",entry.getName().substring("BOOT-INF/classes/".length()))));
                }
                historicalMigrations++;
            }
            assertThat(historicalMigrations).isEqualTo(23);
        }
        requireDocker();
        try(var network=Network.builder().createNetworkCmdModifier(c->c.withInternal(true)).build();
            var database=new PostgreSQLContainer<>("postgres:16-alpine")) {
            database.start();
            // The test harness reaches PostgreSQL through its ordinary loopback mapping;
            // the old app is attached ONLY to the additional no-egress test network.
            DockerClientFactory.instance().client().connectToNetworkCmd().withNetworkId(network.getId())
                    .withContainerId(database.getContainerId()).withContainerNetwork(
                            new com.github.dockerjava.api.model.ContainerNetwork().withAliases("rollback-postgres")).exec();
            String password="LOCAL_ONLY_"+UUID.randomUUID().toString().replace("-","");
            initializeStagingAclFixture(database,password);
            createWindow(database,()->migrations(database,password,"24").migrate());
            convergeNewTables(database);
            execute(database,"INSERT INTO public.tm_asset_card_snapshot(symbol,version_counter,snapshot_version,snapshot_json) VALUES ('TESTROLLBACKUSDT',1,1,'{\"localFixture\":true}')");
            execute(database,"INSERT INTO public.tm_asset_card_feature_history(symbol,record_kind,record_key,signal_as_of,available_at,payload_json) VALUES ('TESTROLLBACKUSDT','INFERENCE','local-rollback',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'{\"pendingLabel\":true}')");
            String before=legacyIdentity(database), defaults=defaultAcls(database);
            String history=scalar(database,"SELECT md5(string_agg(row_to_json(f)::text,'' ORDER BY installed_rank)) FROM public.flyway_schema_history f");
            String cardRows=scalar(database,"SELECT md5(string_agg(row_to_json(f)::text,'' ORDER BY symbol,record_key)) FROM public.tm_asset_card_feature_history f");
            // Existing prod Flyway locations/validation remain unchanged. The isolated network
            // has no external route; test-only credentials are carried in a protected file.
            String config="""
                    spring.profiles.active=prod
                    spring.datasource.url=jdbc:postgresql://rollback-postgres:5432/test
                    spring.datasource.username=rine_app
                    spring.datasource.password=%s
                    spring.flyway.user=rine_migrator
                    spring.flyway.password=%s
                    binance.api.key=LOCAL_DISABLED_PROVIDER_FIXTURE
                    binance.api.secret=LOCAL_DISABLED_PROVIDER_FIXTURE
                    server.address=0.0.0.0
                    server.port=8081
                    trade-model.production.allow-public-bind=true
                    trade-model.production.scheduler-policy=EXPLICIT_OPT_IN
                    trade-model.production.scheduler-approval.push-recheck=PROD_BLOCKED
                    trade-model.production.scheduler-approval.position-sync=PROD_BLOCKED
                    trade-model.production.scheduler-approval.position-monitor=PROD_BLOCKED
                    trade-model.production.scheduler-approval.market-data=PROD_BLOCKED
                    trade-model.production.scheduler-approval.ohlcv-ingestion=PROD_BLOCKED
                    trade-model.production.scheduler-approval.watchlist=PROD_BLOCKED
                    trade-model.production.scheduler-approval.analysis=PROD_BLOCKED
                    trade-model.production.scheduler-approval.provider-scan=PROD_BLOCKED
                    trade-model.schedulers.enabled=false
                    trade-model.schedulers.push-recheck.enabled=false
                    trade-model.schedulers.position-sync.enabled=false
                    trade-model.schedulers.position-monitor.enabled=false
                    trade-model.schedulers.market-data.enabled=false
                    trade-model.schedulers.ohlcv-ingestion.enabled=false
                    trade-model.schedulers.watchlist.enabled=false
                    trade-model.analysis.scheduler.enabled=false
                    trade-model.ai.enabled=false
                    trade-model.ai.background-execution.enabled=false
                    trade-model.telegram.enabled=false
                    trade-model.telegram.external-calls-enabled=false
                    trade-model.provider-call.enabled=false
                    trade-model.provider-call.scheduler-enabled=false
                    trade-model.provider-call.external-calls-enabled=false
                    trade-model.providers.coinglass.enabled=false
                    trade-model.providers.coinglass.external-calls-enabled=false
                    trade-model.home-live.enabled=false
                    spring.main.banner-mode=off
                    logging.level.org.example.trademodel.mapper=OFF
                    """.formatted(password,password);
            assertThat(config).doesNotContain("validate-on-migrate", "ignore-migration", "baseline-on-migrate");
            String rollbackArchitecture;
            try(var app=new GenericContainer<>("eclipse-temurin:17-jre-jammy")
                    .withNetwork(network)
                    .withCopyFileToContainer(MountableFile.forHostPath(jar),"/fixture/old.jar")
                    .withCopyToContainer(Transferable.of(config,0400),"/fixture/rollback.properties")
                    .withCommand("java","-jar","/fixture/old.jar","--spring.config.additional-location=file:/fixture/rollback.properties")
                    .waitingFor(Wait.forLogMessage(".*Started TradeModelApplication.*",1).withStartupTimeout(Duration.ofSeconds(120)))) {
                app.start();
                var readiness=app.execInContainer("bash","-c","exec 3<>/dev/tcp/127.0.0.1/8081; printf 'GET /actuator/health/readiness HTTP/1.0\\r\\nHost: localhost\\r\\n\\r\\n' >&3; cat <&3");
                assertThat(readiness.getExitCode()).isZero();
                assertThat(readiness.getStdout()).withFailMessage("Readiness did not pass; sanitized old-JAR logs: %s",app.getLogs().replace(password,"[REDACTED]")).startsWith("HTTP/1.1 200");
                String log=app.getLogs();
                assertThat(log.contains(password)).as("Credential bytes must never enter old-JAR logs").isFalse();
                String safeLog=log.replace(password,"[REDACTED]");
                assertThat(safeLog).contains("Started TradeModelApplication", "Successfully validated", "Schema \"public\" is up to date");
                assertThat(safeLog).doesNotContain("Validate failed", "Migration failed");
                assertThat(safeLog).contains("24");
                var architecture=app.execInContainer("uname","-m");
                assertThat(architecture.getExitCode()).isZero();
                rollbackArchitecture=architecture.getStdout().trim();
            }
            assertCreateReclaimed(database);
            assertThat(legacyIdentity(database)).isEqualTo(before);
            assertThat(defaultAcls(database)).isEqualTo(defaults);
            assertThat(scalar(database,"SELECT md5(string_agg(row_to_json(f)::text,'' ORDER BY installed_rank)) FROM public.flyway_schema_history f")).isEqualTo(history);
            assertThat(scalar(database,"SELECT md5(string_agg(row_to_json(f)::text,'' ORDER BY symbol,record_key)) FROM public.tm_asset_card_feature_history f")).isEqualTo(cardRows);
            assertThat(scalar(database,"SELECT snapshot_json FROM public.tm_asset_card_snapshot WHERE symbol='TESTROLLBACKUSDT'")).isEqualTo("{\"localFixture\":true}");
            assertThat(hash(jar)).isEqualTo(OLD_SHA256);
            System.out.println("OLD_JAR_ROLLBACK=PASS\nOLD_JAR_SHA256="+OLD_SHA256+"\nOLD_JAR_SOURCE_SHA=094b70a8ed31891999da0814fae5add09e2c4e08\nOLD_FLYWAY_VALIDATION_UNCHANGED=YES\nLEGACY_MIGRATIONS_BYTE_MATCH=23/23\nV24_TABLES_AND_ROWS_RETAINED=YES\nROLLBACK_READINESS=200\nROLLBACK_CONTAINER_ARCH="+rollbackArchitecture+"\nEXECUTION_ENVIRONMENT=DISPOSABLE_LOCAL_POSTGRESQL");
        }
    }

    private static void requireDocker() {
        boolean available;
        try { available=DockerClientFactory.instance().isDockerAvailable(); } catch(RuntimeException absent) { available=false; }
        assumeTrue(available,"Disposable PostgreSQL unavailable; no external database fallback");
    }
    private static Flyway migrations(PostgreSQLContainer<?> database,String password,String target) {
        return Flyway.configure().dataSource(database.getJdbcUrl(),"rine_migrator",password)
                .locations("classpath:db/migration").target(target).load();
    }
    private static void initializeStagingAclFixture(PostgreSQLContainer<?> database,String password) throws Exception {
        assertThat(database.getDatabaseName()).isEqualTo("test");
        assertThat(database.getJdbcUrl()).startsWith("jdbc:postgresql://").doesNotContain("staging","production");
        execute(database,"CREATE ROLE rine_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'; CREATE ROLE rine_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '"+password+"'; ALTER SCHEMA public OWNER TO rine_migrator; REVOKE ALL ON DATABASE test FROM PUBLIC; GRANT CONNECT ON DATABASE test TO rine_migrator,rine_app; REVOKE ALL ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO rine_app; REVOKE CREATE ON SCHEMA public FROM rine_migrator; ALTER DEFAULT PRIVILEGES FOR ROLE rine_migrator IN SCHEMA public GRANT SELECT,INSERT,UPDATE,DELETE ON TABLES TO rine_app; ALTER DEFAULT PRIVILEGES FOR ROLE rine_migrator IN SCHEMA public GRANT SELECT,UPDATE,USAGE ON SEQUENCES TO rine_app");
        createWindow(database,()->migrations(database,password,"23").migrate());
        execute(database,"INSERT INTO public.tm_rule_config(rule_id,rule_key,rule_value) VALUES ('local-rollback-fixture','local-rollback-fixture','UNCHANGED')");
        execute(database,"INSERT INTO public.tm_user(username,password_hash,created_at) VALUES ('fixture_"+UUID.randomUUID().toString().replace("-","").substring(0,12)+"','TEST_ONLY_UNUSABLE_HASH',CURRENT_TIMESTAMP)");
    }
    @FunctionalInterface private interface MigrationAction { void run() throws Exception; }
    private static void createWindow(PostgreSQLContainer<?> database,MigrationAction action) throws Exception {
        assertCreateReclaimed(database);
        try { execute(database,"GRANT CREATE ON SCHEMA public TO rine_migrator"); action.run(); }
        finally {
            boolean interrupted=Thread.interrupted();
            try { execute(database,"REVOKE CREATE ON SCHEMA public FROM rine_migrator"); assertCreateReclaimed(database); }
            finally { if(interrupted) Thread.currentThread().interrupt(); }
        }
    }
    private static void assertCreateReclaimed(PostgreSQLContainer<?> database) throws Exception {
        // scalar opens another fresh connection after REVOKE's independent commit.
        assertThat(scalar(database,"SELECT has_schema_privilege('rine_migrator','public','CREATE') OR (SELECT rolsuper FROM pg_roles WHERE rolname='rine_migrator')")).isEqualTo("f");
    }
    private static void convergeNewTables(PostgreSQLContainer<?> database) throws Exception {
        bootstrapNewTables(database,true);
    }
    private static void bootstrapNewTables(PostgreSQLContainer<?> database,boolean reconcile) throws Exception {
        for(String name:List.of("asset-card-role-bootstrap.sql","asset-card-role-verify.sql"))
            database.copyFileToContainer(MountableFile.forHostPath(Path.of("deploy/native-staging",name)),"/tmp/"+name);
        var result=database.execInContainer("psql","-X","-v","ON_ERROR_STOP=1","-v","card_reconcile_new_table_acl="+reconcile,"-U",database.getUsername(),"-d","test","-f","/tmp/asset-card-role-bootstrap.sql");
        assertThat(result.getExitCode()).withFailMessage(result.getStderr()).isZero();
        var verify=database.execInContainer("psql","-X","-v","ON_ERROR_STOP=1","-U",database.getUsername(),"-d","test","-f","/tmp/asset-card-role-verify.sql");
        assertThat(verify.getExitCode()).withFailMessage(verify.getStderr()).isZero();
    }
    private static void execute(PostgreSQLContainer<?> database,String sql) throws Exception {
        try(var c=DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword()); var s=c.createStatement()) {
            s.execute("SET statement_timeout='15s'; SET lock_timeout='2s'"); s.execute(sql);
        }
    }
    private static String scalar(PostgreSQLContainer<?> database,String sql) throws Exception {
        try(var c=DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword()); var s=c.createStatement()) {
            c.setReadOnly(true); s.execute("SET statement_timeout='10s'");
            try(var r=s.executeQuery(sql)) { assertThat(r.next()).isTrue(); return r.getString(1); }
        }
    }
    private static String legacyIdentity(PostgreSQLContainer<?> database) throws Exception {
        StringBuilder identity=new StringBuilder(scalar(database,"SELECT md5(string_agg(relname||COALESCE(relacl::text,''),'' ORDER BY relname)) FROM pg_class WHERE relnamespace='public'::regnamespace AND relname NOT LIKE '%asset_card%'"));
        try(var c=DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword()); var s=c.createStatement()) {
            c.setReadOnly(true); s.execute("SET statement_timeout='10s'");
            try(var rows=s.executeQuery("SELECT relname FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND relname NOT LIKE 'tm_asset_card_%' AND relname<>'flyway_schema_history' ORDER BY relname")) {
                while(rows.next()) {
                    String table=rows.getString(1);
                    assertThat(table).matches("[A-Za-z_][A-Za-z_0-9]*");
                    identity.append(table).append(':').append(scalar(database,"SELECT count(*)||':'||COALESCE(md5(string_agg(row_to_json(r)::text,'' ORDER BY row_to_json(r)::text)),'EMPTY') FROM public.\""+table+"\" r"));
                }
            }
        }
        return identity.toString();
    }
    private static String defaultAcls(PostgreSQLContainer<?> database) throws Exception {
        return scalar(database,"SELECT md5(string_agg(row_to_json(a)::text,'' ORDER BY oid)) FROM pg_default_acl a");
    }
    private static String hash(Path path) throws Exception {
        try(var input=Files.newInputStream(path)) {
            var digest=java.security.MessageDigest.getInstance("SHA-256"); byte[] buffer=new byte[65536]; int n;
            while((n=input.read(buffer))!=-1) digest.update(buffer,0,n);
            return java.util.HexFormat.of().formatHex(digest.digest());
        }
    }
    @Test
    void addsOnlyThreeIsolatedCardTablesWithUtcAndNoOwnerMutation() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V24__asset_card_live_signal.sql"));
        String upper = sql.toUpperCase(Locale.ROOT);
        assertThat(upper).contains("TM_ASSET_CARD_SNAPSHOT", "TM_ASSET_CARD_SPOT_BAR",
                "TM_ASSET_CARD_FEATURE_HISTORY", "VERSION_COUNTER", "SNAPSHOT_VERSION",
                "TIMESTAMP WITH TIME ZONE", "AVAILABLE_AT", "PRIMARY KEY (SYMBOL, INTERVAL_CODE, OPEN_TIME)",
                "RECORD_KIND", "RECORD_KEY", "PRIMARY KEY (SYMBOL, RECORD_KIND, RECORD_KEY)",
                "'FEATURE'", "'INFERENCE'", "'TRADE'", "'LABEL'");
        assertThat(upper).doesNotContain("DELETE", "DROP ", "TRUNCATE", "ALTER ", "UPDATE ", "INSERT ",
                "TM_USER_POSITION", "GRANT ", "REVOKE ", "SUPERUSER", "NEXTVAL", "CREATE SEQUENCE", "CREATE FUNCTION", "CREATE TRIGGER");
        assertThat(upper.split("CREATE TABLE IF NOT EXISTS", -1)).hasSize(4);
        assertThat(upper.split("CREATE INDEX IF NOT EXISTS", -1)).hasSize(3);
        assertThat(Files.readString(Path.of("src/main/resources/schema.sql")))
                .endsWith(sql);
    }

    @Test
    void postgresMigrationPreservesLegacyRowsAndDoesNotChangePrivileges() throws Exception {
        boolean available;
        try { available = DockerClientFactory.instance().isDockerAvailable(); }
        catch (RuntimeException unavailable) { available = false; }
        assumeTrue(available, "Disposable Docker PostgreSQL unavailable; no external database fallback");
        try (var postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target("23").load().migrate();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                assertThat(connection.getMetaData().getURL()).isEqualTo(postgres.getJdbcUrl());
                String before;
                try (var result = statement.executeQuery("SELECT count(*)::text FROM tm_user_position")) {
                    result.next(); before = result.getString(1);
                }
                String beforeAcl;
                try (var result = statement.executeQuery("""
                        SELECT md5(string_agg(relname || COALESCE(relacl::text,''),'' ORDER BY relname))
                        FROM pg_class WHERE relnamespace='public'::regnamespace AND relname NOT LIKE '%asset_card%'
                        """)) { result.next(); beforeAcl = result.getString(1); }
                Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                        .locations("classpath:db/migration").load().migrate();
                try (var result = statement.executeQuery("SELECT count(*)::text FROM tm_user_position")) {
                    result.next(); assertThat(result.getString(1)).isEqualTo(before);
                }
                for (String table : new String[]{"tm_asset_card_snapshot", "tm_asset_card_spot_bar", "tm_asset_card_feature_history"}) {
                    try (var result = statement.executeQuery("SELECT to_regclass('public." + table + "') IS NOT NULL")) {
                        result.next(); assertThat(result.getBoolean(1)).isTrue();
                    }
                }
                try (var result = statement.executeQuery("SELECT relacl IS NULL FROM pg_class WHERE relname='tm_asset_card_snapshot'")) {
                    result.next(); assertThat(result.getBoolean(1)).isTrue();
                }
                try (var result = statement.executeQuery("""
                        SELECT md5(string_agg(relname || COALESCE(relacl::text,''),'' ORDER BY relname))
                        FROM pg_class WHERE relnamespace='public'::regnamespace AND relname NOT LIKE '%asset_card%'
                        """)) { result.next(); assertThat(result.getString(1)).isEqualTo(beforeAcl); }

                var mapper = new org.example.trademodel.mapper.AssetCardMapper(new org.springframework.jdbc.core.JdbcTemplate(
                        new org.springframework.jdbc.datasource.DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())));
                String symbol = "T" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
                var asOf = java.time.Instant.parse("2026-09-10T12:00:00Z");
                long first = mapper.nextSnapshotVersion(symbol), second = mapper.nextSnapshotVersion(symbol);
                assertThat(second).isGreaterThan(first);
                assertThat(mapper.saveSnapshot(symbol, 0, second, "{\"version\":2}", asOf)).isEqualTo(1);
                assertThat(mapper.saveSnapshot(symbol, 0, first, "{\"version\":1}", asOf)).isZero();
                assertThat(mapper.selectSnapshotJson(symbol)).isEqualTo("{\"version\":2}");
                var bar = new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotBar(symbol, "5m",
                        asOf.minusSeconds(300), asOf.minusMillis(1), java.math.BigDecimal.ONE, java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, null, null, asOf);
                assertThat(mapper.upsertClosedBar(bar)).isEqualTo(1);
                assertThat(mapper.upsertClosedBar(bar)).isZero();
                assertThat(mapper.selectClosedBars(symbol, "5m", asOf, 10)).hasSize(1);
                assertThat(mapper.saveFeatureHistory(symbol, asOf, asOf, "{\"evidence\":1}")).isEqualTo(1);
                assertThat(mapper.saveFeatureHistory(symbol, asOf, asOf, "{\"evidence\":2}")).isZero();
                assertThat(mapper.selectFeatureHistory(symbol, asOf, asOf, asOf, 10))
                        .extracting(org.example.trademodel.mapper.AssetCardMapper.FeatureHistory::payloadJson)
                        .containsExactly("{\"evidence\":1}");
                // Migration's disposable administrator can validate SQL, but is never a least-privilege app writer.
                assertThat(mapper.inspectWriterPermissions().writable()).isFalse();
                assertThat(mapper.inspectWriterPermissions().cleanupAllowed()).isFalse();
                assertThat(mapper.saveInference(symbol, asOf, asOf, "{\"status\":\"TIMEOUT\"}")).isEqualTo(1);
                assertThat(mapper.saveInference(symbol, asOf.minusMillis(1), asOf.plusSeconds(1), "{\"status\":\"LATER\"}")).isZero();
                assertThat(mapper.selectInference(symbol, asOf, asOf).orElseThrow().payloadJson())
                        .isEqualTo("{\"status\":\"TIMEOUT\"}");

                // PostgreSQL columns round to microseconds; original JSON clocks must still gate label eligibility.
                var horizon = asOf.plusSeconds(14400);
                var originalAvailableAt = horizon.plusNanos(1);
                var observation = new org.example.trademodel.assetcard.AssetCardFeatureService.Observation(
                        10.0, "BINANCE_SPOT_AGG_TRADE", horizon.minusSeconds(1), originalAvailableAt,
                        "BINANCE:SPOT:NONE:BTC/USDT", "BINANCE_SPOT_PUBLIC_V1", "QUOTE_CURRENCY", horizon.plusSeconds(10), "42");
                var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
                        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                String payload = json.writeValueAsString(java.util.Map.of("observation", observation));
                var trade = new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote("BTCUSDT",
                        java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, 42, observation.observedAt(), originalAvailableAt);
                assertThat(mapper.saveTradeObservation(trade, observation.instrument(), observation.sourceVersion(), payload)).isEqualTo(1);
                var candidate = mapper.selectHorizonTrade("BTCUSDT", horizon.minusSeconds(2), horizon).orElseThrow();
                assertThat(candidate.availableAt()).isEqualTo(horizon);
                assertThat(candidate.payloadJson()).isEqualTo(payload);
                var restored = json.treeToValue(json.readTree(candidate.payloadJson()).path("observation"),
                        org.example.trademodel.assetcard.AssetCardFeatureService.Observation.class);
                assertThat(restored.availableAt()).isEqualTo(originalAvailableAt).isAfter(horizon);
                assertThat(org.example.trademodel.assetcard.AssetCardFeatureService.usableObservation(
                        "BTCUSDT", "spotPrice", restored, horizon)).isFalse();
                assertThat(org.example.trademodel.assetcard.AssetCardFeatureService.usableObservation(
                        "BTCUSDT", "spotPrice", restored, originalAvailableAt)).isTrue();
            }
        }
    }
}
