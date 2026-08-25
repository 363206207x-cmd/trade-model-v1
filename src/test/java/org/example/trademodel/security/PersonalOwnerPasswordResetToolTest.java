package org.example.trademodel.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.example.trademodel.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalOwnerPasswordResetToolTest {
    private static final char[] VALID = "orbit meadow quartz harbor 47!".toCharArray();

    @TempDir
    Path tempDir;

    @Test
    void resetsOnlyCanonicalOwnerPreservesOtherUsersAndInvalidatesOwnerSessions() throws Exception {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        try (Connection connection = database("success")) {
            String ownerHash = encoder.encode("old-secret-47!");
            String userHash = encoder.encode("user-secret-48!");
            insert(connection, 1L, "xuchao", ownerHash, "OWNER", true, 7L, 1);
            insert(connection, 2L, "ordinary", userHash, "USER", true, 3L, null);

            PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    connection, "xuchao", VALID.clone(), VALID.clone(), encoder);

            assertThat(count(connection)).isEqualTo(2);
            assertThat(ownerCount(connection)).isEqualTo(1);
            assertThat(value(connection, 1L, "id", Long.class)).isEqualTo(1L);
            assertThat(value(connection, 1L, "username", String.class)).isEqualTo("xuchao");
            assertThat(value(connection, 1L, "role", String.class)).isEqualTo("OWNER");
            assertThat(value(connection, 1L, "session_version", Long.class)).isEqualTo(8L);
            assertThat(encoder.matches(new String(VALID), value(connection, 1L, "password_hash", String.class)))
                    .isTrue();
            assertThat(value(connection, 2L, "password_hash", String.class)).isEqualTo(userHash);
            assertThat(value(connection, 2L, "role", String.class)).isEqualTo("USER");
            assertThat(value(connection, 2L, "session_version", Long.class)).isEqualTo(3L);
        }
    }

    @Test
    void rejectsMismatchMissingOwnerDuplicateOwnerAndWrongConfiguredIdentity() throws Exception {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        try (Connection mismatch = database("mismatch")) {
            insertOwner(mismatch, 1L, "xuchao", encoder.encode("old-secret-47!"));
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    mismatch, "xuchao", VALID.clone(), "different meadow quartz harbor 47!".toCharArray(), encoder))
                    .hasMessage("PASSWORD_MISMATCH");
        }
        try (Connection missing = database("missing")) {
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    missing, "xuchao", VALID.clone(), VALID.clone(), encoder))
                    .hasMessage("OWNER_MISSING");
        }
        try (Connection duplicate = database("duplicate")) {
            insertOwner(duplicate, 1L, "xuchao", encoder.encode("old-secret-47!"));
            insertOwner(duplicate, 2L, "other", encoder.encode("old-secret-48!"));
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    duplicate, "xuchao", VALID.clone(), VALID.clone(), encoder))
                    .hasMessage("MULTIPLE_OWNERS_REJECTED");
        }
        try (Connection wrong = database("wrong")) {
            insertOwner(wrong, 1L, "owner", encoder.encode("old-secret-47!"));
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    wrong, "xuchao", VALID.clone(), VALID.clone(), encoder))
                    .hasMessage("CONFIGURED_OWNER_NOT_FOUND");
        }
        try (Connection wrongId = database("wrong-id")) {
            insertOwner(wrongId, 9L, "xuchao", encoder.encode("old-secret-47!"));
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    wrongId, "xuchao", VALID.clone(), VALID.clone(), encoder))
                    .hasMessage("CANONICAL_OWNER_INVALID");
        }
    }

    @Test
    void rejectsWeakOrOwnerDerivedPassphrases() throws Exception {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        try (Connection connection = database("policy")) {
            insertOwner(connection, 1L, "xuchao", encoder.encode("old-secret-47!"));
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    connection, "xuchao", "tiny".toCharArray(), "tiny".toCharArray(), encoder))
                    .hasMessage("PASSWORD_POLICY_REJECTED");
            char[] ownerDerived = "xuchao meadow quartz harbor 47!".toCharArray();
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    connection, "xuchao", ownerDerived, ownerDerived.clone(), encoder))
                    .hasMessage("PASSWORD_POLICY_REJECTED");
        }
    }

    @Test
    void updatesExistingRootRuntimeKeysWithoutAddingAccountsOrPrintingSecrets() throws Exception {
        Path config = tempDir.resolve("active.env");
        Files.writeString(config, "TRADE_MODEL_INITIAL_USERNAME=owner\n"
                + "TRADE_MODEL_INITIAL_PASSWORD='old-value'\nUNCHANGED=true\n");

        PersonalOwnerPasswordResetTool.updateRuntimeBootstrapSecret(config, "xuchao", VALID.clone());

        String content = Files.readString(config);
        assertThat(content).contains("TRADE_MODEL_INITIAL_USERNAME='xuchao'")
                .contains("TRADE_MODEL_INITIAL_PASSWORD='orbit meadow quartz harbor 47!'")
                .contains("UNCHANGED=true");
        assertThat(Files.getPosixFilePermissions(config).toString())
                .contains("OWNER_READ", "OWNER_WRITE")
                .doesNotContain("GROUP_READ", "OTHERS_READ");
    }

    @Test
    void resolvesOnlyTheProductionDatasourceContractUsedByStaging() {
        PersonalOwnerPasswordResetTool.DataSourceSettings settings =
                PersonalOwnerPasswordResetTool.resolveDataSourceSettings(Map.of(
                        "PROD_DATASOURCE_URL", "jdbc:postgresql://database/rine_logic_staging",
                        "PROD_DATASOURCE_USERNAME", "application",
                        "PROD_DATASOURCE_PASSWORD", "test-only-value"));

        assertThat(settings.url()).isEqualTo("jdbc:postgresql://database/rine_logic_staging");
        assertThat(settings.username()).isEqualTo("application");
        assertThat(settings.password()).isEqualTo("test-only-value");
        assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resolveDataSourceSettings(Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://wrong/alias",
                "SPRING_DATASOURCE_USERNAME", "wrong",
                "SPRING_DATASOURCE_PASSWORD", "wrong")))
                .hasMessage("PROD_DATASOURCE_URL_MISSING");
    }

    private Connection database(String name) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:owner-reset-" + name + ";DB_CLOSE_DELAY=-1");
        connection.createStatement().execute("CREATE TABLE tm_user (id BIGINT PRIMARY KEY, "
                + "username VARCHAR(64), password_hash VARCHAR(100), role VARCHAR(16), enabled BOOLEAN, "
                + "session_version BIGINT, updated_at TIMESTAMP, owner_slot SMALLINT)");
        return connection;
    }

    private void insertOwner(Connection connection, long id, String username, String hash) throws Exception {
        insert(connection, id, username, hash, "OWNER", true, 0L, 1);
    }

    private void insert(Connection connection, long id, String username, String hash, String role,
                        boolean enabled, long sessionVersion, Integer ownerSlot) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tm_user(id, username, password_hash, role, enabled, session_version, updated_at, owner_slot) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)")) {
            statement.setLong(1, id);
            statement.setString(2, username);
            statement.setString(3, hash);
            statement.setString(4, role);
            statement.setBoolean(5, enabled);
            statement.setLong(6, sessionVersion);
            if (ownerSlot == null) statement.setNull(7, java.sql.Types.SMALLINT);
            else statement.setInt(7, ownerSlot);
            statement.executeUpdate();
        }
    }

    private int count(Connection connection) throws Exception {
        try (ResultSet result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM tm_user")) {
            result.next();
            return result.getInt(1);
        }
    }

    private int ownerCount(Connection connection) throws Exception {
        try (ResultSet result = connection.createStatement()
                .executeQuery("SELECT COUNT(*) FROM tm_user WHERE role = 'OWNER'")) {
            result.next();
            return result.getInt(1);
        }
    }

    private <T> T value(Connection connection, long id, String column, Class<T> type) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + column + " FROM tm_user WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getObject(1, type);
            }
        }
    }
}
