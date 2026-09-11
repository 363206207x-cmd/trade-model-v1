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
