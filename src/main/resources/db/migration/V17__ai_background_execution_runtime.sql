ALTER TABLE tm_ai_call_log
    ADD COLUMN task_state VARCHAR(32) DEFAULT 'QUEUED';
ALTER TABLE tm_ai_call_log
    ADD COLUMN attempt INT DEFAULT 1;
ALTER TABLE tm_ai_call_log
    ADD COLUMN role_state VARCHAR(32);
ALTER TABLE tm_ai_call_log
    ADD COLUMN data_state VARCHAR(32);
ALTER TABLE tm_ai_call_log
    ADD COLUMN submitted_at TIMESTAMP;
ALTER TABLE tm_ai_call_log
    ADD COLUMN reasoning_tokens BIGINT;
ALTER TABLE tm_ai_call_log
    ADD COLUMN failure_classification VARCHAR(128);
ALTER TABLE tm_ai_call_log
    ADD COLUMN prompt_version VARCHAR(64) DEFAULT 'V41-AI-ROLE-PROMPT-1';
ALTER TABLE tm_ai_call_log
    ADD COLUMN schema_version VARCHAR(64) DEFAULT 'V41-AI-STRUCTURED-SCHEMA-1';
ALTER TABLE tm_ai_call_log
    ADD COLUMN input_contract_version VARCHAR(64) DEFAULT 'V41-AI-INPUT-COMPACT-1';
ALTER TABLE tm_ai_call_log
    ADD COLUMN runtime_config_version VARCHAR(64) DEFAULT 'V41-AI-BACKGROUND-TIMEOUT-1';
ALTER TABLE tm_ai_call_log
    ADD COLUMN background_mode VARCHAR(64);
ALTER TABLE tm_ai_call_log
    ADD COLUMN active_task_key VARCHAR(160);

UPDATE tm_ai_call_log
SET task_state = CASE
        WHEN call_status = 'SUCCESS' THEN 'SUCCEEDED'
        WHEN call_status = 'TIMEOUT' THEN 'TIMED_OUT'
        WHEN call_status = 'STARTED' THEN 'RUNNING'
        ELSE 'FAILED'
    END,
    attempt = COALESCE(attempt, 1),
    role_state = CASE
        WHEN call_status = 'SUCCESS' THEN 'READY'
        WHEN call_status = 'STARTED' THEN 'PARTIAL'
        WHEN call_status = 'TIMEOUT' THEN 'ERROR'
        WHEN call_status IN ('DISABLED', 'NOT_CONFIGURED') THEN 'UNAVAILABLE'
        WHEN fallback_flag = TRUE THEN 'FALLBACK'
        ELSE 'ERROR'
    END,
    data_state = CASE
        WHEN call_status = 'SUCCESS' THEN 'READY'
        WHEN call_status = 'TIMEOUT' THEN 'AI_TIMEOUT'
        WHEN call_status IN ('DISABLED', 'NOT_CONFIGURED') THEN 'SOURCE_UNAVAILABLE'
        WHEN call_status = 'STARTED' THEN NULL
        WHEN fallback_flag = TRUE THEN 'FALLBACK_RULE_ONLY'
        ELSE 'AI_FAILED'
    END,
    submitted_at = COALESCE(submitted_at, started_at),
    prompt_version = COALESCE(prompt_version, 'V41-AI-ROLE-PROMPT-1'),
    schema_version = COALESCE(schema_version, 'V41-AI-STRUCTURED-SCHEMA-1'),
    input_contract_version = COALESCE(input_contract_version, 'V41-AI-INPUT-COMPACT-1'),
    runtime_config_version = COALESCE(runtime_config_version, 'V41-AI-BACKGROUND-TIMEOUT-1');

ALTER TABLE tm_ai_call_log
    ALTER COLUMN task_state SET NOT NULL;
ALTER TABLE tm_ai_call_log
    ALTER COLUMN attempt SET NOT NULL;
ALTER TABLE tm_ai_call_log
    ALTER COLUMN prompt_version SET NOT NULL;
ALTER TABLE tm_ai_call_log
    ALTER COLUMN schema_version SET NOT NULL;
ALTER TABLE tm_ai_call_log
    ALTER COLUMN input_contract_version SET NOT NULL;
ALTER TABLE tm_ai_call_log
    ALTER COLUMN runtime_config_version SET NOT NULL;

ALTER TABLE tm_ai_call_log
    ADD CONSTRAINT ck_tm_ai_call_log_task_state CHECK (
        task_state IN ('QUEUED', 'SUBMITTED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'TIMED_OUT', 'CANCELLED')
    );
ALTER TABLE tm_ai_call_log
    ADD CONSTRAINT ck_tm_ai_call_log_attempt CHECK (attempt BETWEEN 1 AND 2);

CREATE INDEX idx_tm_ai_call_log_task_state
    ON tm_ai_call_log(task_state, submitted_at);

CREATE UNIQUE INDEX uk_tm_ai_call_log_active_task
    ON tm_ai_call_log(active_task_key);
