ALTER TABLE tm_async_task
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);

ALTER TABLE tm_async_task
    ADD COLUMN IF NOT EXISTS result_resource_id VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_async_task_idempotency_key
    ON tm_async_task(owner_type, owner_id, idempotency_key);

ALTER TABLE tm_position_monitor_log
    ADD COLUMN IF NOT EXISTS monitor_run_key VARCHAR(180);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tm_position_monitor_log_run_key
    ON tm_position_monitor_log(monitor_run_key);

ALTER TABLE tm_execution_plan
    ADD COLUMN IF NOT EXISTS decision_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_tm_execution_plan_decision_identity
    ON tm_execution_plan(analysis_id, decision_id, create_time DESC);

COMMENT ON COLUMN tm_async_task.idempotency_key IS
    'Owner-scoped client submission identity used to recover one canonical asynchronous analysis task.';

COMMENT ON COLUMN tm_async_task.result_resource_id IS
    'Canonical result identity, such as analysisId, restored after navigation or refresh.';

COMMENT ON COLUMN tm_position_monitor_log.monitor_run_key IS
    'Stable position and observation-window identity preventing duplicate monitor rows across processes.';

COMMENT ON COLUMN tm_execution_plan.decision_id IS
    'Exact decision identity for new validated and blocked plan results; legacy rows remain unchanged.';
