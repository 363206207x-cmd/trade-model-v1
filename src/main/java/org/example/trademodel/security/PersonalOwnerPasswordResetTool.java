package org.example.trademodel.security;

import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.example.trademodel.config.SecurityConfig;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Root-operated recovery tool for the existing single personal account. */
public final class PersonalOwnerPasswordResetTool {
    private static final Pattern WORD = Pattern.compile("\\p{L}{2,}");
    private static final int MIN_LENGTH = 16;

    private PersonalOwnerPasswordResetTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            fail("PASSWORD_ARGUMENTS_FORBIDDEN");
        }
        Console console = System.console();
        if (console == null) {
            fail("INTERACTIVE_CONSOLE_REQUIRED");
        }
        char[] first = console.readPassword("New Owner password: ");
        char[] second = console.readPassword("Repeat Owner password: ");
        try {
            Map<String, String> environment = System.getenv();
            String username = requiredEnvironment(environment, "TRADE_MODEL_INITIAL_USERNAME");
            DataSourceSettings dataSource = resolveDataSourceSettings(environment);
            Path runtimeConfig = Path.of(requiredEnvironment(environment, "RINE_LOGIC_ACTIVE_ENV_FILE"));
            if (!dataSource.url().startsWith("jdbc:postgresql:")) {
                fail("POSTGRESQL_REQUIRED");
            }
            PasswordEncoder encoder = SecurityConfig.passwordEncoder();
            try (Connection connection = DriverManager.getConnection(
                    dataSource.url(), dataSource.username(), dataSource.password())) {
                resetExistingSingleOwner(connection, username, first, second, encoder);
            }
            updateRuntimeBootstrapSecret(runtimeConfig, username, first);
            System.out.println("OWNER_PASSWORD_RESET=PASS");
        } finally {
            clear(first);
            clear(second);
        }
    }

    static void resetExistingSingleOwner(Connection connection,
                                         String configuredUsername,
                                         char[] first,
                                         char[] second,
                                         PasswordEncoder encoder) throws SQLException {
        String username = PersonalUsernamePolicy.normalize(configuredUsername);
        if (!PersonalUsernamePolicy.isValid(username)) {
            throw new IllegalStateException("CONFIGURED_USERNAME_INVALID");
        }
        validatePasswords(username, first, second);
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long userId;
            String storedUsername;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, username, enabled, owner_slot FROM tm_user "
                            + "WHERE role = 'OWNER' ORDER BY id FOR UPDATE");
                 ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("OWNER_MISSING");
                }
                userId = resultSet.getLong("id");
                storedUsername = resultSet.getString("username");
                boolean enabled = resultSet.getBoolean("enabled");
                int ownerSlot = resultSet.getInt("owner_slot");
                if (resultSet.next()) {
                    throw new IllegalStateException("MULTIPLE_OWNERS_REJECTED");
                }
                if (userId != 1L || !enabled || ownerSlot != 1) {
                    throw new IllegalStateException("CANONICAL_OWNER_INVALID");
                }
            }
            if (!username.equals(PersonalUsernamePolicy.normalize(storedUsername))) {
                throw new IllegalStateException("CONFIGURED_OWNER_NOT_FOUND");
            }
            String encoded = encoder.encode(new String(first));
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tm_user SET password_hash = ?, session_version = session_version + 1, "
                            + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND username = ? "
                            + "AND role = 'OWNER' AND enabled = TRUE AND owner_slot = 1")) {
                statement.setString(1, encoded);
                statement.setLong(2, userId);
                statement.setString(3, storedUsername);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("OWNER_PASSWORD_UPDATE_COUNT_INVALID");
                }
            }
            connection.commit();
        } catch (RuntimeException | SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    static void updateRuntimeBootstrapSecret(Path runtimeConfig,
                                             String username,
                                             char[] password) throws IOException {
        if (!Files.isRegularFile(runtimeConfig)) {
            throw new IllegalStateException("RUNTIME_CONFIG_MISSING");
        }
        List<String> lines = Files.readAllLines(runtimeConfig, StandardCharsets.UTF_8);
        List<String> updated = new ArrayList<>(lines.size());
        int usernameCount = 0;
        int passwordCount = 0;
        String passwordValue = new String(password);
        for (String line : lines) {
            if (line.startsWith("TRADE_MODEL_INITIAL_USERNAME=")) {
                updated.add("TRADE_MODEL_INITIAL_USERNAME=" + shellQuote(username));
                usernameCount++;
            } else if (line.startsWith("TRADE_MODEL_INITIAL_PASSWORD=")) {
                updated.add("TRADE_MODEL_INITIAL_PASSWORD=" + shellQuote(passwordValue));
                passwordCount++;
            } else {
                updated.add(line);
            }
        }
        if (usernameCount != 1 || passwordCount != 1) {
            throw new IllegalStateException("RUNTIME_BOOTSTRAP_KEYS_INVALID");
        }
        Files.writeString(runtimeConfig, String.join("\n", updated) + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.setPosixFilePermissions(runtimeConfig, PosixFilePermissions.fromString("rw-------"));
    }

    private static void validatePasswords(String username, char[] first, char[] second) {
        if (first == null || second == null || first.length == 0 || second.length == 0) {
            throw new IllegalArgumentException("PASSWORD_MISSING");
        }
        if (!Arrays.equals(first, second)) {
            throw new IllegalArgumentException("PASSWORD_MISMATCH");
        }
        String value = new String(first);
        if (!InitialPasswordPolicy.validate(value).accepted() || value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("PASSWORD_POLICY_REJECTED");
        }
        long digits = value.chars().filter(Character::isDigit).count();
        boolean symbol = value.chars().anyMatch(character ->
                !Character.isLetterOrDigit(character) && !Character.isWhitespace(character));
        Matcher words = WORD.matcher(value);
        int wordCount = 0;
        while (words.find()) {
            wordCount++;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (digits < 2 || !symbol || wordCount < 4
                || normalized.contains(username.toLowerCase(Locale.ROOT))
                || normalized.contains("rine logic")
                || normalized.contains("rine-logic")
                || normalized.contains("rinelogic")
                || normalized.contains("100.97.230.66")) {
            throw new IllegalArgumentException("PASSWORD_POLICY_REJECTED");
        }
    }

    static DataSourceSettings resolveDataSourceSettings(Map<String, String> environment) {
        return new DataSourceSettings(
                requiredEnvironment(environment, "PROD_DATASOURCE_URL"),
                requiredEnvironment(environment, "PROD_DATASOURCE_USERNAME"),
                requiredEnvironment(environment, "PROD_DATASOURCE_PASSWORD"));
    }

    private static String requiredEnvironment(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + "_MISSING");
        }
        return value;
    }

    record DataSourceSettings(String url, String username, String password) {
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static void clear(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }

    private static void fail(String reason) {
        System.err.println("OWNER_PASSWORD_RESET=FAILED");
        System.err.println("REASON_CODE=" + reason);
        System.exit(1);
    }
}
