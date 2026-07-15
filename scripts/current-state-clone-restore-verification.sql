\set ON_ERROR_STOP on
\pset tuples_only on
\pset format unaligned
\pset fieldsep '|'

BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '120s';
SET LOCAL lock_timeout = '5s';

SELECT 'DECISION_WITHOUT_ANALYSIS', COUNT(*)
FROM tm_decision_result d
LEFT JOIN tm_analysis_run a ON a.analysis_id = d.analysis_id
WHERE a.analysis_id IS NULL;

SELECT 'EXECUTION_PLAN_WITHOUT_ANALYSIS', COUNT(*)
FROM tm_execution_plan p
LEFT JOIN tm_analysis_run a ON a.analysis_id = p.analysis_id
WHERE a.analysis_id IS NULL;

SELECT 'POSITION_MONITOR_WITHOUT_POSITION', COUNT(*)
FROM tm_position_monitor_log m
LEFT JOIN tm_user_position p ON p.id = m.position_id
WHERE p.id IS NULL;

SELECT 'POSITION_MONITOR_PLAN_ANALYSIS_MISMATCH', COUNT(*)
FROM tm_position_monitor_log m
JOIN tm_execution_plan p ON p.plan_id = m.execution_plan_id
WHERE m.execution_plan_id IS NOT NULL AND p.analysis_id <> m.analysis_id;

SELECT 'TYPED_POSITION_PLAN_REFERENCE_MISMATCH', COUNT(*)
FROM tm_user_position p
WHERE p.source_ref_id LIKE 'EXECUTION_PLAN:%'
  AND NOT EXISTS (
      SELECT 1
      FROM tm_execution_plan ep
      WHERE ep.plan_id = substring(p.source_ref_id FROM length('EXECUTION_PLAN:') + 1)
  );

SELECT 'DUPLICATE_ANALYSIS_ID', COALESCE(SUM(duplicate_count - 1), 0)
FROM (
    SELECT COUNT(*) AS duplicate_count
    FROM tm_analysis_run
    GROUP BY analysis_id
    HAVING COUNT(*) > 1
) duplicates;

SELECT 'DUPLICATE_DECISION_ID', COALESCE(SUM(duplicate_count - 1), 0)
FROM (
    SELECT COUNT(*) AS duplicate_count
    FROM tm_decision_result
    GROUP BY decision_id
    HAVING COUNT(*) > 1
) duplicates;

SELECT 'DUPLICATE_PLAN_ID', COALESCE(SUM(duplicate_count - 1), 0)
FROM (
    SELECT COUNT(*) AS duplicate_count
    FROM tm_execution_plan
    GROUP BY plan_id
    HAVING COUNT(*) > 1
) duplicates;

SELECT CASE
    WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'tm_decision_result'
          AND column_name = 'valid_from'
    ) THEN
        'SELECT ''VALIDITY_ORDER_INVALID'', COUNT(*) FROM tm_decision_result '
        || 'WHERE valid_from IS NOT NULL AND expires_at IS NOT NULL AND valid_from > expires_at;'
    ELSE
        'SELECT ''VALIDITY_ORDER_INVALID'', ''NOT_APPLICABLE_PRE_V7'';'
END
\gexec

SELECT CASE
    WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'tm_decision_result'
          AND column_name = 'valid_from'
    ) THEN
        'SELECT ''VALIDITY_PARTIAL_NULL'', COUNT(*) FROM tm_decision_result '
        || 'WHERE (valid_from IS NULL) <> (expires_at IS NULL);'
    ELSE
        'SELECT ''VALIDITY_PARTIAL_NULL'', ''NOT_APPLICABLE_PRE_V7'';'
END
\gexec

WITH secret_counts(candidate_count) AS (
    SELECT COUNT(*) FROM tm_analysis_run
      WHERE concat_ws(' ', input_snapshot_json, error_message) ~* '(sk-[A-Za-z0-9_-]{12,}|AIza[0-9A-Za-z_-]{20,}|gh[pousr]_[0-9A-Za-z]{20,}|xox[baprs]-|Bearer[[:space:]]+[A-Za-z0-9._-]{12,}|BEGIN[[:space:]]+PRIVATE[[:space:]]+KEY|AKIA[0-9A-Z]{16}|jdbc:postgresql:|postgres(ql)?://|password[[:space:]]*[:=])'
    UNION ALL
    SELECT COUNT(*) FROM tm_decision_result
      WHERE concat_ws(' ', conclusion_summary, invalid_condition, evidence_summary, explanation_json, review_reasons) ~* '(sk-[A-Za-z0-9_-]{12,}|AIza[0-9A-Za-z_-]{20,}|gh[pousr]_[0-9A-Za-z]{20,}|xox[baprs]-|Bearer[[:space:]]+[A-Za-z0-9._-]{12,}|BEGIN[[:space:]]+PRIVATE[[:space:]]+KEY|AKIA[0-9A-Z]{16}|jdbc:postgresql:|postgres(ql)?://|password[[:space:]]*[:=])'
    UNION ALL
    SELECT COUNT(*) FROM tm_position_monitor_log
      WHERE concat_ws(' ', reason, evidence_snapshot, score_snapshot, decision_snapshot, risk_snapshot) ~* '(sk-[A-Za-z0-9_-]{12,}|AIza[0-9A-Za-z_-]{20,}|gh[pousr]_[0-9A-Za-z]{20,}|xox[baprs]-|Bearer[[:space:]]+[A-Za-z0-9._-]{12,}|BEGIN[[:space:]]+PRIVATE[[:space:]]+KEY|AKIA[0-9A-Z]{16}|jdbc:postgresql:|postgres(ql)?://|password[[:space:]]*[:=])'
    UNION ALL
    SELECT COUNT(*) FROM tm_ai_call_log
      WHERE concat_ws(' ', fallback_reason, error_message, request_summary, response_summary) ~* '(sk-[A-Za-z0-9_-]{12,}|AIza[0-9A-Za-z_-]{20,}|gh[pousr]_[0-9A-Za-z]{20,}|xox[baprs]-|Bearer[[:space:]]+[A-Za-z0-9._-]{12,}|BEGIN[[:space:]]+PRIVATE[[:space:]]+KEY|AKIA[0-9A-Z]{16}|jdbc:postgresql:|postgres(ql)?://|password[[:space:]]*[:=])'
    UNION ALL
    SELECT COUNT(*) FROM tm_hot_reset_event
      WHERE concat_ws(' ', source_reference, execution_error_message, trigger_reason_text) ~* '(sk-[A-Za-z0-9_-]{12,}|AIza[0-9A-Za-z_-]{20,}|gh[pousr]_[0-9A-Za-z]{20,}|xox[baprs]-|Bearer[[:space:]]+[A-Za-z0-9._-]{12,}|BEGIN[[:space:]]+PRIVATE[[:space:]]+KEY|AKIA[0-9A-Z]{16}|jdbc:postgresql:|postgres(ql)?://|password[[:space:]]*[:=])'
)
SELECT 'SECRET_CANDIDATE_TOTAL', COALESCE(SUM(candidate_count), 0)
FROM secret_counts;

WITH pii_counts(candidate_count) AS (
    SELECT COUNT(*) FROM tm_analysis_run
      WHERE concat_ws(' ', input_snapshot_json, error_message) ~* '([A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}|\+[1-9][0-9]{9,14}|[0-9]{3}[- ][0-9]{3,4}[- ][0-9]{4})'
    UNION ALL
    SELECT COUNT(*) FROM tm_decision_result
      WHERE concat_ws(' ', conclusion_summary, invalid_condition, evidence_summary, explanation_json, review_reasons) ~* '([A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}|\+[1-9][0-9]{9,14}|[0-9]{3}[- ][0-9]{3,4}[- ][0-9]{4})'
    UNION ALL
    SELECT COUNT(*) FROM tm_position_monitor_log
      WHERE concat_ws(' ', reason, evidence_snapshot, score_snapshot, decision_snapshot, risk_snapshot) ~* '([A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}|\+[1-9][0-9]{9,14}|[0-9]{3}[- ][0-9]{3,4}[- ][0-9]{4})'
)
SELECT 'PII_CANDIDATE_TOTAL', COALESCE(SUM(candidate_count), 0)
FROM pii_counts;

WITH production_reference_counts(candidate_count) AS (
    SELECT COUNT(*) FROM tm_analysis_run
      WHERE concat_ws(' ', input_snapshot_json, error_message) ~* '(jdbc:postgresql:|postgres(ql)?://|(^|[^a-z])(prod|production|primary)[.-][a-z0-9])'
    UNION ALL
    SELECT COUNT(*) FROM tm_ai_call_log
      WHERE concat_ws(' ', request_summary, response_summary, error_message) ~* '(jdbc:postgresql:|postgres(ql)?://|(^|[^a-z])(prod|production|primary)[.-][a-z0-9])'
)
SELECT 'PRODUCTION_REFERENCE_CANDIDATE_TOTAL', COALESCE(SUM(candidate_count), 0)
FROM production_reference_counts;

COMMIT;
