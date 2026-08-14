-- Canonical Desktop interaction runtime owners. Historical data remains fail closed.

ALTER TABLE tm_analysis_run
    ADD COLUMN analysis_mode VARCHAR(32);

UPDATE tm_analysis_run
SET analysis_mode = CASE
    WHEN preview = TRUE THEN 'ANALYSIS_PREVIEW'
    ELSE 'OPPORTUNITY_DECISION'
END
WHERE analysis_mode IS NULL;

ALTER TABLE tm_analysis_run
    ALTER COLUMN analysis_mode SET NOT NULL,
    ADD CONSTRAINT ck_tm_analysis_run_mode CHECK (
        analysis_mode IN ('ANALYSIS_PREVIEW', 'OPPORTUNITY_DECISION')
    ),
    ADD CONSTRAINT ck_tm_analysis_run_preview_mode CHECK (
        (preview = TRUE AND analysis_mode = 'ANALYSIS_PREVIEW')
        OR (preview = FALSE AND analysis_mode = 'OPPORTUNITY_DECISION')
    );

ALTER TABLE tm_execution_plan
    ADD COLUMN plan_lifecycle_state VARCHAR(32),
    ADD COLUMN plan_version INT NOT NULL DEFAULT 1,
    ADD COLUMN supersedes_plan_id VARCHAR(64),
    ADD COLUMN superseded_by_plan_id VARCHAR(64);

UPDATE tm_execution_plan
SET plan_lifecycle_state = CASE
    WHEN final_plan = TRUE THEN 'NEEDS_REVALIDATION'
    ELSE 'INVALIDATED'
END
WHERE plan_lifecycle_state IS NULL;

ALTER TABLE tm_execution_plan
    ALTER COLUMN plan_lifecycle_state SET NOT NULL,
    ADD CONSTRAINT ck_tm_execution_plan_lifecycle CHECK (
        plan_lifecycle_state IN (
            'CURRENT', 'NEEDS_REVALIDATION', 'SUPERSEDED',
            'TRACKING_STOPPED', 'INVALIDATED', 'EXPIRED'
        )
    ),
    ADD CONSTRAINT ck_tm_execution_plan_version CHECK (plan_version > 0),
    ADD CONSTRAINT fk_tm_execution_plan_supersedes
        FOREIGN KEY (supersedes_plan_id) REFERENCES tm_execution_plan(plan_id),
    ADD CONSTRAINT fk_tm_execution_plan_superseded_by
        FOREIGN KEY (superseded_by_plan_id) REFERENCES tm_execution_plan(plan_id);

CREATE INDEX idx_tm_execution_plan_lifecycle
    ON tm_execution_plan(plan_lifecycle_state, create_time DESC);

CREATE TABLE tm_plan_revalidation_record (
    record_id VARCHAR(64) PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL,
    analysis_id VARCHAR(64) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL,
    source_plan_version INT NOT NULL,
    result_plan_version INT,
    result_plan_id VARCHAR(64),
    reason TEXT,
    result_summary TEXT,
    trace_id VARCHAR(128) NOT NULL,
    requested_by_user_id BIGINT,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_tm_plan_revalidation_trigger CHECK (
        trigger_type IN (
            'HOT_RESET', 'EVENT_WINDOW', 'DATA_REFRESH',
            'EVIDENCE_CHANGED', 'MANUAL_REVALIDATION'
        )
    ),
    CONSTRAINT ck_tm_plan_revalidation_state CHECK (
        state IN ('QUEUED', 'RUNNING', 'PARTIAL', 'SUCCEEDED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT ck_tm_plan_revalidation_versions CHECK (
        source_plan_version > 0 AND (result_plan_version IS NULL OR result_plan_version > source_plan_version)
    ),
    CONSTRAINT ck_tm_plan_revalidation_safety CHECK (
        not_trade_instruction = TRUE AND not_order_execution = TRUE
    ),
    CONSTRAINT fk_tm_plan_revalidation_plan
        FOREIGN KEY (plan_id) REFERENCES tm_execution_plan(plan_id),
    CONSTRAINT fk_tm_plan_revalidation_result_plan
        FOREIGN KEY (result_plan_id) REFERENCES tm_execution_plan(plan_id)
);

CREATE INDEX idx_tm_plan_revalidation_plan
    ON tm_plan_revalidation_record(plan_id, created_at DESC);
CREATE INDEX idx_tm_plan_revalidation_trace
    ON tm_plan_revalidation_record(trace_id, created_at DESC);

CREATE TABLE tm_message (
    message_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    analysis_id VARCHAR(64),
    position_id BIGINT,
    plan_id VARCHAR(64),
    symbol VARCHAR(32),
    title VARCHAR(256) NOT NULL,
    body TEXT,
    business_state VARCHAR(32) NOT NULL,
    read_state VARCHAR(16) NOT NULL DEFAULT 'UNREAD',
    dedupe_key VARCHAR(256) NOT NULL,
    current_recheck_id VARCHAR(64),
    trace_id VARCHAR(128),
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    not_trade_instruction BOOLEAN NOT NULL DEFAULT TRUE,
    not_order_execution BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_tm_message_dedupe UNIQUE (user_id, dedupe_key),
    CONSTRAINT ck_tm_message_category CHECK (
        category IN (
            'HIGH_PERMISSION_OPPORTUNITY',
            'OPPORTUNITY_PLAN_SAFETY_CHANGE',
            'POSITION_LOGIC_RISK_CHANGE'
        )
    ),
    CONSTRAINT ck_tm_message_business_state CHECK (
        business_state IN ('ACTIVE', 'EXPIRED', 'RESOLVED')
    ),
    CONSTRAINT ck_tm_message_read_state CHECK (read_state IN ('UNREAD', 'READ')),
    CONSTRAINT ck_tm_message_safety CHECK (
        not_trade_instruction = TRUE AND not_order_execution = TRUE
    )
);

CREATE INDEX idx_tm_message_user_time ON tm_message(user_id, created_at DESC);
CREATE INDEX idx_tm_message_source ON tm_message(source_type, source_id);

CREATE TABLE tm_channel_delivery (
    delivery_id VARCHAR(64) PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    channel VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider_reference VARCHAR(256),
    attempt_count INT NOT NULL DEFAULT 0,
    error_code VARCHAR(128),
    error_message TEXT,
    attempted_at TIMESTAMP WITHOUT TIME ZONE,
    delivered_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_channel_delivery_channel CHECK (channel IN ('TELEGRAM')),
    CONSTRAINT ck_tm_channel_delivery_status CHECK (
        status IN ('QUEUED', 'SENT', 'DELIVERED', 'FAILED', 'SUPPRESSED')
    ),
    CONSTRAINT ck_tm_channel_delivery_attempt CHECK (attempt_count >= 0),
    CONSTRAINT fk_tm_channel_delivery_message
        FOREIGN KEY (message_id) REFERENCES tm_message(message_id)
);

CREATE INDEX idx_tm_channel_delivery_message
    ON tm_channel_delivery(message_id, created_at DESC);

CREATE TABLE tm_async_task (
    task_id VARCHAR(64) PRIMARY KEY,
    owner_type VARCHAR(16) NOT NULL,
    owner_id BIGINT NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    state VARCHAR(16) NOT NULL,
    stage VARCHAR(64),
    resource_type VARCHAR(32),
    resource_id VARCHAR(64),
    trace_id VARCHAR(128),
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 0,
    error_code VARCHAR(128),
    error_message TEXT,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_async_task_owner CHECK (
        (owner_type = 'SYSTEM' AND owner_id = 0)
        OR (owner_type = 'USER' AND owner_id > 0)
    ),
    CONSTRAINT ck_tm_async_task_type CHECK (
        task_type IN (
            'POOL_SCAN', 'ANALYSIS_PREVIEW', 'REANALYSIS',
            'THREE_AI', 'PLAN_REVALIDATION', 'HOT_RESET'
        )
    ),
    CONSTRAINT ck_tm_async_task_state CHECK (
        state IN ('QUEUED', 'RUNNING', 'PARTIAL', 'SUCCEEDED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT ck_tm_async_task_retry CHECK (
        retry_count >= 0 AND max_retries >= 0 AND retry_count <= max_retries
    )
);

CREATE INDEX idx_tm_async_task_owner_time
    ON tm_async_task(owner_type, owner_id, created_at DESC);
CREATE INDEX idx_tm_async_task_trace
    ON tm_async_task(trace_id, created_at DESC);

CREATE TABLE tm_event_asset_relation (
    relation_id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(24) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    asset_id BIGINT,
    symbol VARCHAR(32) NOT NULL,
    plan_id VARCHAR(64),
    relation_type VARCHAR(32) NOT NULL,
    source_reference VARCHAR(512) NOT NULL,
    trace_id VARCHAR(128),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tm_event_asset_event_type CHECK (
        event_type IN ('MACRO', 'INDUSTRY', 'PROJECT', 'HOT_RESET')
    ),
    CONSTRAINT ck_tm_event_asset_relation_type CHECK (
        relation_type IN ('AFFECTS_ASSET', 'TRIGGERS_REVALIDATION', 'CONTEXT_ONLY')
    ),
    CONSTRAINT fk_tm_event_asset_asset FOREIGN KEY (asset_id) REFERENCES tm_asset(id),
    CONSTRAINT fk_tm_event_asset_plan FOREIGN KEY (plan_id) REFERENCES tm_execution_plan(plan_id)
);

CREATE INDEX idx_tm_event_asset_symbol_time
    ON tm_event_asset_relation(symbol, created_at DESC);
CREATE INDEX idx_tm_event_asset_event
    ON tm_event_asset_relation(event_type, event_id);

ALTER TABLE tm_account_risk_snapshot
    ADD COLUMN account_risk_coverage_state VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    ADD CONSTRAINT ck_tm_account_risk_coverage CHECK (
        account_risk_coverage_state IN ('COMPLETE', 'PARTIAL', 'UNKNOWN')
    );

ALTER TABLE tm_review_result
    ADD COLUMN missed_reason VARCHAR(32),
    ADD COLUMN later_outcome VARCHAR(16),
    ADD CONSTRAINT ck_tm_review_missed_reason CHECK (
        missed_reason IS NULL OR missed_reason IN (
            'NOT_TRIGGERED', 'BLOCKED_BY_SYSTEM', 'PUSHED_NOT_FILLED', 'USER_SKIPPED'
        )
    ),
    ADD CONSTRAINT ck_tm_review_later_outcome CHECK (
        later_outcome IS NULL OR later_outcome IN ('VALID', 'INVALID', 'INCONCLUSIVE')
    );

ALTER TABLE tm_user_config
    ADD COLUMN telegram_chat_id VARCHAR(128),
    ADD COLUMN telegram_binding_status VARCHAR(24) NOT NULL DEFAULT 'UNBOUND',
    ADD COLUMN notification_filters_json TEXT,
    ADD COLUMN default_pool_mode VARCHAR(24) NOT NULL DEFAULT 'SYSTEM_DEFAULT',
    ADD CONSTRAINT ck_tm_user_config_telegram_binding CHECK (
        telegram_binding_status IN ('UNBOUND', 'PENDING', 'BOUND', 'FAILED')
    ),
    ADD CONSTRAINT ck_tm_user_config_default_pool CHECK (
        default_pool_mode IN ('SYSTEM_DEFAULT', 'USER_CUSTOM')
    );
