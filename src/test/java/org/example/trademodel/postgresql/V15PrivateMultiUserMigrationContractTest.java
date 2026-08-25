package org.example.trademodel.postgresql;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class V15PrivateMultiUserMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V15__private_multi_user_account_registration.sql");

    @Test
    void v15PreservesTheUniqueOwnerAndAddsBoundedPrivateRegistrationOwners() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER'",
                "ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE",
                "ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0",
                "V15_OWNER_IDENTITY_AMBIGUOUS",
                "V15_CASE_INSENSITIVE_USERNAME_CONFLICT",
                "V15_EXISTING_OWNER_IDENTITY_MISMATCH",
                "V15_ACTIVE_ACCOUNT_LIMIT_EXCEEDED",
                "V15_LEGACY_POSITION_OWNER_MISSING",
                "V15_USER_ID_SEQUENCE_MISSING",
                "LOWER(username) = 'xuchao'",
                "role = 'OWNER'",
                "CREATE UNIQUE INDEX uk_tm_user_username_lower ON tm_user(LOWER(username))",
                "UPDATE tm_user_position\nSET user_id = 1\nWHERE user_id IS NULL",
                "ALTER COLUMN user_id SET NOT NULL",
                "pg_get_serial_sequence('tm_user', 'id')",
                "max_active_accounts = 10",
                "CREATE TABLE tm_owner_password_setup_token",
                "token_hash VARCHAR(64) NOT NULL",
                "CROSS JOIN tm_asset_pool_item defaults",
                "defaults.owner_type = 'SYSTEM'",
                "ON CONFLICT (owner_type, owner_id, symbol) DO NOTHING")
                .doesNotContain(
                        "DELETE FROM tm_user",
                        "DROP TABLE tm_user",
                        "UPDATE tm_analysis_run SET owner_type = 'USER'",
                        "UPDATE tm_execution_plan SET owner_type = 'USER'",
                        "UPDATE tm_message SET user_id = 1");
    }

    @Test
    void h2SchemaCarriesEquivalentAccountLifecycleAndTokenContracts() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        assertThat(schema).contains(
                "username VARCHAR_IGNORECASE(64) NOT NULL",
                "role VARCHAR(16) NOT NULL DEFAULT 'USER'",
                "enabled BOOLEAN NOT NULL DEFAULT TRUE",
                "session_version BIGINT NOT NULL DEFAULT 0",
                "CONSTRAINT uk_tm_user_owner_slot UNIQUE (owner_slot)",
                "max_active_accounts INT NOT NULL",
                "CONSTRAINT ck_tm_user_registration_guard_limit CHECK (max_active_accounts = 10)",
                "CREATE TABLE IF NOT EXISTS tm_owner_password_setup_token",
                "token_hash VARCHAR(64) NOT NULL",
                "user_id BIGINT NOT NULL");
    }
}
