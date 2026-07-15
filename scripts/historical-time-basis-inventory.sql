\set ON_ERROR_STOP on
\pset pager off
\pset format unaligned
\pset tuples_only on

BEGIN TRANSACTION READ ONLY;

-- INVENTORY_QUERY_BEGIN
WITH inventory_clock AS (
    SELECT COALESCE(
        NULLIF(current_setting('trade_model.inventory_as_of_utc', true), '')::TIMESTAMP WITHOUT TIME ZONE,
        CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
    ) AS as_of_utc
), decision_validity_values AS (
    SELECT CASE WHEN to_jsonb(decision) ? 'valid_from'
                THEN (to_jsonb(decision) ->> 'valid_from')::TIMESTAMP WITH TIME ZONE
           END AS valid_from,
           CASE WHEN to_jsonb(decision) ? 'expires_at'
                THEN (to_jsonb(decision) ->> 'expires_at')::TIMESTAMP WITH TIME ZONE
           END AS expires_at
    FROM tm_decision_result decision
), decision_validity_schema AS (
    SELECT COUNT(*) AS available_column_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'tm_decision_result'
      AND column_name IN ('valid_from', 'expires_at')
), field_catalog(
    field_name,
    semantic_type,
    future_check_mode,
    relation_check_mode,
    reference_field,
    expected_ordering,
    tolerance_contract,
    offset_pattern_applicable
) AS (
    VALUES
        ('tm_monitor_alert.created_at', 'EVENT_INSTANT', 'AS_OF_PLUS_5_MINUTES',
         'NOT_APPLICABLE', 'none', 'NONE', 'AS_OF_PLUS_5_MINUTES', false),
        ('tm_monitor_alert.updated_at', 'AUDIT_UPDATE_TIME', 'AS_OF_PLUS_5_MINUTES',
         'ORDERING_ONLY', 'tm_monitor_alert.created_at', 'OBSERVED_GTE_REFERENCE',
         'AS_OF_PLUS_5_MINUTES_NO_MAX_DELTA', false),
        ('tm_monitor_alert.cooldown_until', 'SCHEDULED_DEADLINE', 'NOT_APPLICABLE',
         'ORDERING_AND_DURATION', 'tm_monitor_alert.created_at', 'OBSERVED_GTE_REFERENCE',
         'CONFIG_DRIVEN_NO_ASSUMED_MAX', false),
        ('tm_analysis_run.analysis_time', 'CANONICAL_ANALYSIS_TIME', 'NOT_APPLICABLE',
         'DISTRIBUTION_ONLY', 'tm_analysis_run.created_at', 'NONE',
         'NO_TIMEZONE_CLASSIFICATION', false),
        ('tm_analysis_run.created_at', 'EVENT_INSTANT', 'AS_OF_PLUS_5_MINUTES',
         'NEAR_SIMULTANEOUS_CANDIDATE', 'tm_analysis_run.started_at', 'NEAR_SIMULTANEOUS',
         'WRITER_CREATED_NEAR_STARTED_REFERENCE', true),
        ('tm_analysis_run.updated_at', 'AUDIT_UPDATE_TIME', 'AS_OF_PLUS_5_MINUTES',
         'ORDERING_ONLY', 'tm_analysis_run.created_at', 'OBSERVED_GTE_REFERENCE',
         'AS_OF_PLUS_5_MINUTES_NO_MAX_DELTA', false),
        ('tm_analysis_run.started_at', 'EVENT_INSTANT', 'NOT_APPLICABLE',
         'LIFECYCLE_ORDERING', 'tm_analysis_run.completed_at', 'OBSERVED_LTE_REFERENCE_WHEN_COMPLETE',
         'COMPLETION_MAY_BE_NULL', false),
        ('tm_analysis_run.completed_at', 'LIFECYCLE_COMPLETION_TIME', 'NOT_APPLICABLE',
         'LIFECYCLE_ORDERING', 'tm_analysis_run.started_at', 'OBSERVED_GTE_REFERENCE',
         'NO_ASSUMED_MAX_DELAY', false),
        ('tm_hot_reset_event.event_time', 'EVENT_INSTANT', 'AS_OF_PLUS_5_MINUTES',
         'DISTRIBUTION_ONLY', 'tm_hot_reset_event.create_time', 'MAY_PRECEDE_REFERENCE',
         'NO_ASSUMED_MAX_DELAY', false),
        ('tm_hot_reset_event.create_time', 'EVENT_INSTANT', 'AS_OF_PLUS_5_MINUTES',
         'NOT_APPLICABLE', 'none', 'NONE', 'AS_OF_PLUS_5_MINUTES', false),
        ('tm_hot_reset_event.completed_at', 'LIFECYCLE_COMPLETION_TIME', 'NOT_APPLICABLE',
         'ORDERING_AND_DURATION', 'tm_hot_reset_event.create_time', 'OBSERVED_GTE_REFERENCE',
         'NO_ASSUMED_MAX_DELAY', false),
        ('tm_decision_result.create_time', 'EVENT_INSTANT', 'AS_OF_PLUS_5_MINUTES',
         'NOT_APPLICABLE', 'none', 'NONE', 'AS_OF_PLUS_5_MINUTES', false),
        ('tm_decision_result.valid_from', 'VALIDITY_START', 'NOT_APPLICABLE',
         'VALIDITY_INTERVAL', 'tm_decision_result.expires_at', 'OBSERVED_LTE_REFERENCE',
         'FUTURE_ALLOWED_NO_ASSUMED_HORIZON', false),
        ('tm_decision_result.expires_at', 'VALIDITY_END', 'NOT_APPLICABLE',
         'VALIDITY_INTERVAL', 'tm_decision_result.valid_from', 'OBSERVED_GTE_REFERENCE',
         'FUTURE_ALLOWED_NO_ASSUMED_HORIZON', false)
), time_values(field_name, observed_time, reference_time) AS (
    SELECT 'tm_monitor_alert.created_at', created_at, NULL::TIMESTAMP FROM tm_monitor_alert
    UNION ALL SELECT 'tm_monitor_alert.updated_at', updated_at, created_at FROM tm_monitor_alert
    UNION ALL SELECT 'tm_monitor_alert.cooldown_until', cooldown_until, created_at FROM tm_monitor_alert
    UNION ALL SELECT 'tm_analysis_run.analysis_time', analysis_time, created_at FROM tm_analysis_run
    UNION ALL SELECT 'tm_analysis_run.created_at', created_at, started_at FROM tm_analysis_run
    UNION ALL SELECT 'tm_analysis_run.updated_at', updated_at, created_at FROM tm_analysis_run
    UNION ALL SELECT 'tm_analysis_run.started_at', started_at, completed_at FROM tm_analysis_run
    UNION ALL SELECT 'tm_analysis_run.completed_at', completed_at, started_at FROM tm_analysis_run
    UNION ALL SELECT 'tm_hot_reset_event.event_time', event_time, create_time FROM tm_hot_reset_event
    UNION ALL SELECT 'tm_hot_reset_event.create_time', create_time, NULL::TIMESTAMP FROM tm_hot_reset_event
    UNION ALL SELECT 'tm_hot_reset_event.completed_at', completed_at, create_time FROM tm_hot_reset_event
    UNION ALL SELECT 'tm_decision_result.create_time', create_time, NULL::TIMESTAMP FROM tm_decision_result
    UNION ALL
    SELECT 'tm_decision_result.valid_from', valid_from AT TIME ZONE 'UTC', expires_at AT TIME ZONE 'UTC'
    FROM decision_validity_values
    UNION ALL
    SELECT 'tm_decision_result.expires_at', expires_at AT TIME ZONE 'UTC', valid_from AT TIME ZONE 'UTC'
    FROM decision_validity_values
), field_summary AS (
    SELECT catalog.field_name,
           catalog.future_check_mode,
           catalog.relation_check_mode,
           COUNT(values.field_name) AS total_rows,
           COUNT(values.field_name) FILTER (WHERE values.observed_time IS NULL) AS null_rows,
           MIN(values.observed_time) AS earliest_time,
           MAX(values.observed_time) AS latest_time,
           COUNT(*) FILTER (
               WHERE values.field_name IS NOT NULL
                 AND catalog.future_check_mode = 'AS_OF_PLUS_5_MINUTES'
                 AND values.observed_time IS NOT NULL
                 AND values.observed_time > clock.as_of_utc + INTERVAL '5 minutes'
           ) AS future_event_candidate_rows,
           COUNT(*) FILTER (
               WHERE values.field_name IS NOT NULL
                 AND catalog.relation_check_mode <> 'NOT_APPLICABLE'
                 AND values.observed_time IS NOT NULL
                 AND values.reference_time IS NULL
           ) AS missing_reference_rows
    FROM field_catalog catalog
    LEFT JOIN time_values values USING (field_name)
    CROSS JOIN inventory_clock clock
    GROUP BY catalog.field_name, catalog.future_check_mode, catalog.relation_check_mode
), day_buckets AS (
    SELECT field_name, DATE_TRUNC('day', observed_time) AS bucket, COUNT(*) AS row_count
    FROM time_values
    WHERE observed_time IS NOT NULL
    GROUP BY field_name, DATE_TRUNC('day', observed_time)
), hour_buckets AS (
    SELECT field_name, DATE_TRUNC('hour', observed_time) AS bucket, COUNT(*) AS row_count
    FROM time_values
    WHERE observed_time IS NOT NULL
    GROUP BY field_name, DATE_TRUNC('hour', observed_time)
), ordering_anomalies(anomaly_code, relation_name, anomaly_count) AS (
    SELECT 'AUDIT_ORDER_INVALID', 'tm_monitor_alert.updated_at>=tm_monitor_alert.created_at', COUNT(*)
    FROM tm_monitor_alert WHERE updated_at < created_at
    UNION ALL
    SELECT 'SCHEDULE_ORDER_INVALID', 'tm_monitor_alert.cooldown_until>=tm_monitor_alert.created_at', COUNT(*)
    FROM tm_monitor_alert WHERE cooldown_until IS NOT NULL AND cooldown_until < created_at
    UNION ALL
    SELECT 'AUDIT_ORDER_INVALID', 'tm_analysis_run.updated_at>=tm_analysis_run.created_at', COUNT(*)
    FROM tm_analysis_run WHERE updated_at < created_at
    UNION ALL
    SELECT 'ANALYSIS_LIFECYCLE_ORDER_INVALID', 'tm_analysis_run.started_at<=tm_analysis_run.completed_at', COUNT(*)
    FROM tm_analysis_run
    WHERE started_at IS NOT NULL AND completed_at IS NOT NULL AND started_at > completed_at
    UNION ALL
    SELECT 'HOT_RESET_LIFECYCLE_ORDER_INVALID', 'tm_hot_reset_event.create_time<=tm_hot_reset_event.completed_at', COUNT(*)
    FROM tm_hot_reset_event
    WHERE completed_at IS NOT NULL AND create_time > completed_at
    UNION ALL
    SELECT 'VALIDITY_ORDER_INVALID', 'tm_decision_result.valid_from<=tm_decision_result.expires_at', COUNT(*)
    FROM decision_validity_values
    WHERE valid_from IS NOT NULL AND expires_at IS NOT NULL AND valid_from > expires_at
    UNION ALL
    SELECT 'VALIDITY_PARTIAL_NULL', 'tm_decision_result.valid_from_and_expires_at_pair', COUNT(*)
    FROM decision_validity_values
    WHERE (valid_from IS NULL) <> (expires_at IS NULL)
), duration_catalog(metric_name, duration_contract) AS (
    VALUES
        ('MONITOR_COOLDOWN', 'cooldown_until-created_at'),
        ('ANALYSIS_CREATED_MINUS_CANONICAL', 'created_at-analysis_time_distribution_only'),
        ('HOT_RESET_PROCESSING_DELAY', 'completed_at-create_time'),
        ('DECISION_VALIDITY', 'expires_at-valid_from')
), duration_values(metric_name, duration_seconds) AS (
    SELECT 'MONITOR_COOLDOWN', EXTRACT(EPOCH FROM cooldown_until - created_at)
    FROM tm_monitor_alert WHERE cooldown_until IS NOT NULL
    UNION ALL
    SELECT 'ANALYSIS_CREATED_MINUS_CANONICAL', EXTRACT(EPOCH FROM created_at - analysis_time)
    FROM tm_analysis_run
    UNION ALL
    SELECT 'HOT_RESET_PROCESSING_DELAY', EXTRACT(EPOCH FROM completed_at - create_time)
    FROM tm_hot_reset_event WHERE completed_at IS NOT NULL
    UNION ALL
    SELECT 'DECISION_VALIDITY', EXTRACT(EPOCH FROM expires_at - valid_from)
    FROM decision_validity_values WHERE valid_from IS NOT NULL AND expires_at IS NOT NULL
), duration_summary AS (
    SELECT catalog.metric_name,
           catalog.duration_contract,
           COUNT(values.metric_name) AS measured_rows,
           COUNT(values.metric_name) FILTER (WHERE values.duration_seconds < 0) AS negative_rows,
           MIN(values.duration_seconds)::BIGINT AS min_seconds,
           MAX(values.duration_seconds)::BIGINT AS max_seconds
    FROM duration_catalog catalog
    LEFT JOIN duration_values values USING (metric_name)
    GROUP BY catalog.metric_name, catalog.duration_contract
), duration_buckets AS (
    SELECT metric_name,
           CASE
               WHEN duration_seconds < 0 THEN 'NEGATIVE'
               WHEN duration_seconds = 0 THEN 'ZERO'
               WHEN duration_seconds <= 300 THEN 'GT_0_LE_5M'
               WHEN duration_seconds <= 1800 THEN 'GT_5M_LE_30M'
               WHEN duration_seconds <= 3600 THEN 'GT_30M_LE_1H'
               WHEN duration_seconds <= 14400 THEN 'GT_1H_LE_4H'
               WHEN duration_seconds <= 43200 THEN 'GT_4H_LE_12H'
               WHEN duration_seconds <= 86400 THEN 'GT_12H_LE_24H'
               ELSE 'GT_24H'
           END AS duration_bucket,
           COUNT(*) AS row_count
    FROM duration_values
    GROUP BY metric_name,
             CASE
                 WHEN duration_seconds < 0 THEN 'NEGATIVE'
                 WHEN duration_seconds = 0 THEN 'ZERO'
                 WHEN duration_seconds <= 300 THEN 'GT_0_LE_5M'
                 WHEN duration_seconds <= 1800 THEN 'GT_5M_LE_30M'
                 WHEN duration_seconds <= 3600 THEN 'GT_30M_LE_1H'
                 WHEN duration_seconds <= 14400 THEN 'GT_1H_LE_4H'
                 WHEN duration_seconds <= 43200 THEN 'GT_4H_LE_12H'
                 WHEN duration_seconds <= 86400 THEN 'GT_12H_LE_24H'
                 ELSE 'GT_24H'
             END
), validity_state_catalog(validity_state) AS (
    VALUES ('NOT_ACTIVE'), ('ACTIVE'), ('EXPIRED')
), validity_state_values(validity_state) AS (
    SELECT CASE
               WHEN decision.valid_from > clock.as_of_utc AT TIME ZONE 'UTC' THEN 'NOT_ACTIVE'
               WHEN decision.expires_at <= clock.as_of_utc AT TIME ZONE 'UTC' THEN 'EXPIRED'
               ELSE 'ACTIVE'
           END
    FROM decision_validity_values decision
    CROSS JOIN inventory_clock clock
    WHERE decision.valid_from IS NOT NULL
      AND decision.expires_at IS NOT NULL
      AND decision.valid_from <= decision.expires_at
), validity_states AS (
    SELECT catalog.validity_state, COUNT(values.validity_state) AS row_count
    FROM validity_state_catalog catalog
    LEFT JOIN validity_state_values values USING (validity_state)
    GROUP BY catalog.validity_state
), validity_null_states AS (
    SELECT COUNT(*) FILTER (WHERE valid_from IS NULL AND expires_at IS NULL) AS both_null_rows,
           COUNT(*) FILTER (WHERE (valid_from IS NULL) <> (expires_at IS NULL)) AS partial_null_rows
    FROM decision_validity_values
), offset_pattern_candidates AS (
    SELECT catalog.field_name,
           catalog.reference_field,
           COUNT(*) FILTER (
               WHERE values.observed_time IS NOT NULL AND values.reference_time IS NOT NULL
                 AND EXTRACT(EPOCH FROM values.observed_time - values.reference_time) = 28800
           ) AS plus_8h_rows,
           COUNT(*) FILTER (
               WHERE values.observed_time IS NOT NULL AND values.reference_time IS NOT NULL
                 AND EXTRACT(EPOCH FROM values.observed_time - values.reference_time) = -28800
           ) AS minus_8h_rows,
           COUNT(*) FILTER (
               WHERE values.observed_time IS NOT NULL AND values.reference_time IS NOT NULL
                 AND EXTRACT(EPOCH FROM values.observed_time - values.reference_time) = 14400
           ) AS plus_4h_rows,
           COUNT(*) FILTER (
               WHERE values.observed_time IS NOT NULL AND values.reference_time IS NOT NULL
                 AND EXTRACT(EPOCH FROM values.observed_time - values.reference_time) = -14400
           ) AS minus_4h_rows
    FROM field_catalog catalog
    LEFT JOIN time_values values USING (field_name)
    WHERE catalog.offset_pattern_applicable
    GROUP BY catalog.field_name, catalog.reference_field
), output_lines AS (
    SELECT 1 AS section_order, 'DECISION_VALIDITY_COLUMNS' AS sort_key,
           'SCHEMA_FIELD_STATUS|tm_decision_result.validity_columns|available_column_count='
             || available_column_count AS output_line
    FROM decision_validity_schema
    UNION ALL
    SELECT 5 AS section_order, field_name AS sort_key,
           'FIELD_POLICY|' || field_name
             || '|semantic_type=' || semantic_type
             || '|future_check_mode=' || future_check_mode
             || '|relation_check_mode=' || relation_check_mode
             || '|reference_field=' || reference_field
             || '|expected_ordering=' || expected_ordering
             || '|tolerance_contract=' || tolerance_contract
             || '|offset_pattern_applicable=' || CASE WHEN offset_pattern_applicable THEN 'true' ELSE 'false' END
             AS output_line
    FROM field_catalog
    UNION ALL
    SELECT 10, field_name,
           'FIELD_SUMMARY|' || field_name
             || '|total=' || total_rows
             || '|null=' || null_rows
             || '|earliest=' || COALESCE(earliest_time::TEXT, 'none')
             || '|latest=' || COALESCE(latest_time::TEXT, 'none')
             || '|future_check=' || future_check_mode
             || '|future_event_candidates=' || CASE
                    WHEN future_check_mode = 'NOT_APPLICABLE' THEN 'NOT_APPLICABLE'
                    ELSE future_event_candidate_rows::TEXT
                END
             || '|reference_check=' || relation_check_mode
             || '|missing_reference=' || CASE
                    WHEN relation_check_mode = 'NOT_APPLICABLE' THEN 'NOT_APPLICABLE'
                    ELSE missing_reference_rows::TEXT
                END
             AS output_line
    FROM field_summary
    UNION ALL
    SELECT 20, field_name || bucket::TEXT,
           'DAY_BUCKET|' || field_name || '|bucket=' || bucket::TEXT || '|count=' || row_count
    FROM day_buckets
    UNION ALL
    SELECT 30, field_name || bucket::TEXT,
           'HOUR_BUCKET|' || field_name || '|bucket=' || bucket::TEXT || '|count=' || row_count
    FROM hour_buckets
    UNION ALL
    SELECT 40, field_name,
           'FUTURE_EVENT_CANDIDATE|EVENT_FUTURE_OUTLIER|field=' || field_name
             || '|count=' || future_event_candidate_rows
    FROM field_summary
    WHERE future_check_mode = 'AS_OF_PLUS_5_MINUTES'
    UNION ALL
    SELECT 50, anomaly_code || relation_name,
           'ORDERING_ANOMALY|' || anomaly_code || '|relation=' || relation_name || '|count=' || anomaly_count
    FROM ordering_anomalies
    UNION ALL
    SELECT 60, metric_name,
           'DURATION_SUMMARY|' || metric_name
             || '|contract=' || duration_contract
             || '|count=' || measured_rows
             || '|negative=' || negative_rows
             || '|min_seconds=' || COALESCE(min_seconds::TEXT, 'none')
             || '|max_seconds=' || COALESCE(max_seconds::TEXT, 'none')
    FROM duration_summary
    UNION ALL
    SELECT 70, metric_name || duration_bucket,
           'DURATION_BUCKET|' || metric_name || '|bucket=' || duration_bucket || '|count=' || row_count
    FROM duration_buckets
    UNION ALL
    SELECT 80, validity_state,
           'VALIDITY_STATE|' || validity_state || '|count=' || row_count
    FROM validity_states
    UNION ALL
    SELECT 85, 'BOTH_NULL',
           'VALIDITY_NULL_STATE|BOTH_NULL|count=' || both_null_rows
    FROM validity_null_states
    UNION ALL
    SELECT 85, 'PARTIAL_NULL',
           'VALIDITY_NULL_STATE|PARTIAL_NULL|count=' || partial_null_rows
    FROM validity_null_states
    UNION ALL
    SELECT 90, field_name,
           'OFFSET_PATTERN_CANDIDATE|field=' || field_name
             || '|reference=' || reference_field
             || '|plus_8h=' || plus_8h_rows
             || '|minus_8h=' || minus_8h_rows
             || '|plus_4h=' || plus_4h_rows
             || '|minus_4h=' || minus_4h_rows
    FROM offset_pattern_candidates
), fingerprint AS (
    SELECT MD5(STRING_AGG(output_line, E'\n' ORDER BY section_order, sort_key)) AS aggregate_hash
    FROM output_lines
)
SELECT output_line
FROM output_lines
UNION ALL
SELECT 'AGGREGATE_MD5|' || aggregate_hash
FROM fingerprint
ORDER BY 1;
-- INVENTORY_QUERY_END

COMMIT;
