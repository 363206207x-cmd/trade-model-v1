package org.example.trademodel.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
    void resetsOnlyExistingSingleConfiguredOwnerAndPreservesIdentity() throws Exception {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        try (Connection connection = database("success")) {
            insert(connection, 9L, "xuchao", encoder.encode("old-secret-47!"));

            PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    connection, "xuchao", VALID.clone(), VALID.clone(), encoder);

            assertThat(count(connection)).isEqualTo(1);
            assertThat(userId(connection)).isEqualTo(9L);
            assertThat(username(connection)).isEqualTo("xuchao");
            assertThat(encoder.matches(new String(VALID), passwordHash(connection))).isTrue();
        }
    }

    @Test
    void rejectsMismatchMissingOwnerDuplicateOwnerAndWrongConfiguredIdentity() throws Exception {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        try (Connection mismatch = database("mismatch")) {
            insert(mismatch, 1L, "xuchao", encoder.encode("old-secret-47!"));
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
            insert(duplicate, 1L, "xuchao", encoder.encode("old-secret-47!"));
            insert(duplicate, 2L, "other", encoder.encode("old-secret-48!"));
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    duplicate, "xuchao", VALID.clone(), VALID.clone(), encoder))
                    .hasMessage("MULTIPLE_OWNERS_REJECTED");
        }
        try (Connection wrong = database("wrong")) {
            insert(wrong, 1L, "owner", encoder.encode("old-secret-47!"));
            assertThatThrownBy(() -> PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    wrong, "xuchao", VALID.clone(), VALID.clone(), encoder))
                    .hasMessage("CONFIGURED_OWNER_NOT_FOUND");
        }
    }

    @Test
    void rejectsWeakOrOwnerDerivedPassphrases() throws Exception {
        PasswordEncoder encoder = SecurityConfig.passwordEncoder();
        try (Connection connection = database("policy")) {
            insert(connection, 1L, "xuchao", encoder.encode("old-secret-47!"));
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

    private Connection database(String name) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:owner-reset-" + name + ";DB_CLOSE_DELAY=-1");
        connection.createStatement().execute("CREATE TABLE tm_user (id BIGINT PRIMARY KEY, username VARCHAR(64), password_hash VARCHAR(100))");
        return connection;
    }

    private void insert(Connection connection, long id, String username, String hash) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tm_user(id, username, password_hash) VALUES (?, ?, ?)")) {
            statement.setLong(1, id);
            statement.setString(2, username);
            statement.setString(3, hash);
            statement.executeUpdate();
        }
    }

    private int count(Connection connection) throws Exception {
        try (ResultSet result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM tm_user")) {
            result.next();
            return result.getInt(1);
        }
    }

    private long userId(Connection connection) throws Exception {
        try (ResultSet result = connection.createStatement().executeQuery("SELECT id FROM tm_user")) {
            result.next();
            return result.getLong(1);
        }
    }

    private String username(Connection connection) throws Exception {
        try (ResultSet result = connection.createStatement().executeQuery("SELECT username FROM tm_user")) {
            result.next();
            return result.getString(1);
        }
    }

    private String passwordHash(Connection connection) throws Exception {
        try (ResultSet result = connection.createStatement().executeQuery("SELECT password_hash FROM tm_user")) {
            result.next();
            return result.getString(1);
        }
    }
}
