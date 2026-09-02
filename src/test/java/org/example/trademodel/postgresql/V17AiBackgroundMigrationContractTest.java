package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("core-regression")
class V17AiBackgroundMigrationContractTest {
    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V17__ai_background_execution_runtime.sql");

    @Test
    void v17ExtendsCanonicalAiTraceWithoutCreatingASecondTaskOwner() throws Exception {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains(
                "ALTER TABLE tm_ai_call_log",
                "task_state VARCHAR(32) DEFAULT 'QUEUED'",
                "attempt INT DEFAULT 1",
                "role_state VARCHAR(32)",
                "data_state VARCHAR(32)",
                "submitted_at TIMESTAMP",
                "reasoning_tokens BIGINT",
                "failure_classification VARCHAR(128)",
                "prompt_version VARCHAR(64) DEFAULT 'V41-AI-ROLE-PROMPT-1'",
                "schema_version VARCHAR(64) DEFAULT 'V41-AI-STRUCTURED-SCHEMA-1'",
                "input_contract_version VARCHAR(64) DEFAULT 'V41-AI-INPUT-COMPACT-1'",
                "runtime_config_version VARCHAR(64) DEFAULT 'V41-AI-BACKGROUND-TIMEOUT-1'",
                "background_mode VARCHAR(64)",
                "active_task_key VARCHAR(160)",
                "ck_tm_ai_call_log_task_state",
                "ck_tm_ai_call_log_attempt",
                "uk_tm_ai_call_log_active_task")
                .doesNotContain(
                        "CREATE TABLE tm_ai_call_log",
                        "CREATE TABLE tm_ai_background_task",
                        "ALTER TABLE tm_analysis_run",
                        "DROP TABLE",
                        "DROP CONSTRAINT");
    }

    @Test
    void h2SchemaCarriesExactRuntimeVersionsAndTaskStateConstraints() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));

        assertThat(schema).contains(
                "task_state VARCHAR(32) NOT NULL DEFAULT 'QUEUED'",
                "attempt INT NOT NULL DEFAULT 1",
                "'QUEUED', 'SUBMITTED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'TIMED_OUT', 'CANCELLED'",
                "CONSTRAINT ck_tm_ai_call_log_attempt CHECK (attempt BETWEEN 1 AND 2)",
                "input_contract_version VARCHAR(64) NOT NULL DEFAULT 'V41-AI-INPUT-COMPACT-1'",
                "runtime_config_version VARCHAR(64) NOT NULL DEFAULT 'V41-AI-BACKGROUND-TIMEOUT-1'",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_ai_call_log_active_task");
    }

    @Test
    void h2ExecutesV17BackfillAndEnforcesOneActiveTaskPerAnalysisRoleInput() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:v17-ai-background;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE tm_ai_call_log(
                      call_id VARCHAR(64) PRIMARY KEY,
                      call_status VARCHAR(32) NOT NULL,
                      fallback_flag BOOLEAN NOT NULL DEFAULT FALSE,
                      started_at TIMESTAMP
                    )
                    """);
            statement.execute("""
                    INSERT INTO tm_ai_call_log(call_id, call_status, fallback_flag, started_at) VALUES
                      ('success-old', 'SUCCESS', FALSE, TIMESTAMP '2026-09-02 08:00:00'),
                      ('running-old', 'STARTED', FALSE, TIMESTAMP '2026-09-02 08:01:00'),
                      ('timeout-old', 'TIMEOUT', TRUE, TIMESTAMP '2026-09-02 08:02:00')
                    """);

            executeMigration(statement, Files.readString(MIGRATION));

            assertThat(row(statement, "success-old"))
                    .containsExactly("SUCCEEDED", "READY", "READY", "1",
                            "V41-AI-INPUT-COMPACT-1", "V41-AI-BACKGROUND-TIMEOUT-1");
            assertThat(row(statement, "running-old"))
                    .containsExactly("RUNNING", "PARTIAL", null, "1",
                            "V41-AI-INPUT-COMPACT-1", "V41-AI-BACKGROUND-TIMEOUT-1");
            assertThat(row(statement, "timeout-old"))
                    .containsExactly("TIMED_OUT", "ERROR", "AI_TIMEOUT", "1",
                            "V41-AI-INPUT-COMPACT-1", "V41-AI-BACKGROUND-TIMEOUT-1");

            statement.execute("""
                    INSERT INTO tm_ai_call_log(call_id, call_status, fallback_flag, task_state,
                      attempt, active_task_key) VALUES
                      ('active-1', 'STARTED', FALSE, 'RUNNING', 1, 'analysis-1|GPT_FINAL|hash-1')
                    """);
            assertThatThrownBy(() -> statement.execute("""
                    INSERT INTO tm_ai_call_log(call_id, call_status, fallback_flag, task_state,
                      attempt, active_task_key) VALUES
                      ('active-duplicate', 'STARTED', FALSE, 'SUBMITTED', 1,
                       'analysis-1|GPT_FINAL|hash-1')
                    """))
                    .isInstanceOf(SQLException.class);
            statement.execute("""
                    UPDATE tm_ai_call_log SET task_state='SUCCEEDED', active_task_key=NULL
                    WHERE call_id='active-1'
                    """);
            statement.execute("""
                    INSERT INTO tm_ai_call_log(call_id, call_status, fallback_flag, task_state,
                      attempt, active_task_key) VALUES
                      ('active-2', 'STARTED', FALSE, 'RUNNING', 2, 'analysis-1|GPT_FINAL|hash-1')
                    """);
            assertThatThrownBy(() -> statement.execute("""
                    INSERT INTO tm_ai_call_log(call_id, call_status, fallback_flag, task_state,
                      attempt) VALUES ('bad-attempt', 'STARTED', FALSE, 'RUNNING', 3)
                    """))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute("""
                    INSERT INTO tm_ai_call_log(call_id, call_status, fallback_flag, task_state,
                      attempt) VALUES ('bad-state', 'STARTED', FALSE, 'WAITING_PROVIDER', 1)
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    private static void executeMigration(Statement statement, String sql) throws SQLException {
        for (String command : sql.split(";")) {
            if (!command.isBlank()) statement.execute(command);
        }
    }

    private static List<String> row(Statement statement, String callId) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                SELECT task_state, role_state, data_state, CAST(attempt AS VARCHAR),
                       input_contract_version, runtime_config_version
                FROM tm_ai_call_log WHERE call_id='""" + callId + "'")) {
            assertThat(result.next()).isTrue();
            return java.util.Arrays.asList(
                    result.getString(1), result.getString(2), result.getString(3),
                    result.getString(4), result.getString(5), result.getString(6));
        }
    }
}
