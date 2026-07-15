\set ON_ERROR_STOP on
\pset pager off
\pset format unaligned
\pset tuples_only on

BEGIN TRANSACTION READ ONLY;

WITH inventory_clock AS (
    SELECT CURRENT_TIMESTAMP AT TIME ZONE 'UTC' AS as_of_utc
), field_catalog(field_name) AS (
    VALUES
        ('tm_monitor_alert.created_at'),
        ('tm_monitor_alert.updated_at'),
        ('tm_monitor_alert.cooldown_until'),
        ('tm_analysis_run.analysis_time'),
        ('tm_analysis_run.created_at'),
        ('tm_analysis_run.started_at'),
        ('tm_analysis_run.completed_at'),
        ('tm_hot_reset_event.event_time'),
        ('tm_hot_reset_event.create_time'),
        ('tm_hot_reset_event.completed_at'),
        ('tm_decision_result.create_time'),
        ('tm_decision_result.valid_from'),
        ('tm_decision_result.expires_at')
), time_values(field_name, observed_time, reference_time) AS (
    SELECT 'tm_monitor_alert.created_at', created_at, updated_at FROM tm_monitor_alert
    UNION ALL SELECT 'tm_monitor_alert.updated_at', updated_at, created_at FROM tm_monitor_alert
    UNION ALL SELECT 'tm_monitor_alert.cooldown_until', cooldown_until, created_at FROM tm_monitor_alert
    UNION ALL SELECT 'tm_analysis_run.analysis_time', analysis_time, created_at FROM tm_analysis_run
    UNION ALL SELECT 'tm_analysis_run.created_at', created_at, analysis_time FROM tm_analysis_run
    UNION ALL SELECT 'tm_analysis_run.started_at', started_at, analysis_time FROM tm_analysis_run
    UNION ALL SELECT 'tm_analysis_run.completed_at', completed_at, analysis_time FROM tm_analysis_run
    UNION ALL SELECT 'tm_hot_reset_event.event_time', event_time, create_time FROM tm_hot_reset_event
    UNION ALL SELECT 'tm_hot_reset_event.create_time', create_time, event_time FROM tm_hot_reset_event
    UNION ALL SELECT 'tm_hot_reset_event.completed_at', completed_at, event_time FROM tm_hot_reset_event
    UNION ALL
    SELECT 'tm_decision_result.create_time', d.create_time, r.analysis_time
    FROM tm_decision_result d
    LEFT JOIN tm_analysis_run r ON r.analysis_id = d.analysis_id
    UNION ALL
    SELECT 'tm_decision_result.valid_from', d.valid_from AT TIME ZONE 'UTC', d.create_time
    FROM tm_decision_result d
    UNION ALL
    SELECT 'tm_decision_result.expires_at', d.expires_at AT TIME ZONE 'UTC', d.create_time
    FROM tm_decision_result d
), field_summary AS (
    SELECT field_catalog.field_name,
           COUNT(time_values.field_name) AS total_rows,
           COUNT(time_values.field_name) FILTER (WHERE observed_time IS NULL) AS null_rows,
           MIN(observed_time) AS earliest_time,
           MAX(observed_time) AS latest_time,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL
                 AND observed_time > inventory_clock.as_of_utc + INTERVAL '5 minutes'
           ) AS obvious_future_rows,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL AND reference_time IS NULL
           ) AS no_reference_rows,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL
                 AND observed_time IS NOT NULL AND reference_time IS NOT NULL
                 AND ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) > 43200
           ) AS reference_mismatch_rows
    FROM field_catalog
    LEFT JOIN time_values USING (field_name)
    CROSS JOIN inventory_clock
    GROUP BY field_catalog.field_name
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
), offset_patterns AS (
    SELECT field_catalog.field_name,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL
                 AND EXTRACT(EPOCH FROM observed_time - reference_time) = 28800
           ) AS plus_8h_rows,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL
                 AND EXTRACT(EPOCH FROM observed_time - reference_time) = -28800
           ) AS minus_8h_rows,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL
                 AND EXTRACT(EPOCH FROM observed_time - reference_time) = 14400
           ) AS plus_4h_rows,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL
                 AND EXTRACT(EPOCH FROM observed_time - reference_time) = -14400
           ) AS minus_4h_rows,
           COUNT(*) FILTER (
               WHERE time_values.field_name IS NOT NULL
                 AND observed_time IS NOT NULL AND reference_time IS NOT NULL
                 AND observed_time <> reference_time
                 AND MOD(ABS(EXTRACT(EPOCH FROM observed_time - reference_time))::BIGINT, 3600) = 0
           ) AS nonzero_whole_hour_rows
    FROM field_catalog
    LEFT JOIN time_values USING (field_name)
    GROUP BY field_catalog.field_name
), delta_buckets AS (
    SELECT field_name,
           CASE
               WHEN reference_time IS NULL THEN 'NO_REFERENCE'
               WHEN observed_time IS NULL THEN 'NULL_OBSERVED_TIME'
               WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) < 60 THEN 'SAME_MINUTE'
               WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) <= 3600 THEN 'LE_1H'
               WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) <= 14400 THEN 'LE_4H'
               WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) <= 43200 THEN 'LE_12H'
               ELSE 'GT_12H'
           END AS delta_bucket,
           COUNT(*) AS row_count
    FROM time_values
    GROUP BY field_name,
             CASE
                 WHEN reference_time IS NULL THEN 'NO_REFERENCE'
                 WHEN observed_time IS NULL THEN 'NULL_OBSERVED_TIME'
                 WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) < 60 THEN 'SAME_MINUTE'
                 WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) <= 3600 THEN 'LE_1H'
                 WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) <= 14400 THEN 'LE_4H'
                 WHEN ABS(EXTRACT(EPOCH FROM observed_time - reference_time)) <= 43200 THEN 'LE_12H'
                 ELSE 'GT_12H'
             END
), output_lines AS (
    SELECT 10 AS section_order, field_name AS sort_key,
           'FIELD_SUMMARY|' || field_name
             || '|total=' || total_rows
             || '|null=' || null_rows
             || '|earliest=' || COALESCE(earliest_time::TEXT, 'none')
             || '|latest=' || COALESCE(latest_time::TEXT, 'none')
             || '|future=' || obvious_future_rows
             || '|no_reference=' || no_reference_rows
             || '|reference_mismatch=' || reference_mismatch_rows AS output_line
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
           'OFFSET_PATTERN|' || field_name
             || '|plus_8h=' || plus_8h_rows
             || '|minus_8h=' || minus_8h_rows
             || '|plus_4h=' || plus_4h_rows
             || '|minus_4h=' || minus_4h_rows
             || '|whole_hour_nonzero=' || nonzero_whole_hour_rows
    FROM offset_patterns
    UNION ALL
    SELECT 50, field_name || delta_bucket,
           'DELTA_BUCKET|' || field_name || '|bucket=' || delta_bucket || '|count=' || row_count
    FROM delta_buckets
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

COMMIT;
