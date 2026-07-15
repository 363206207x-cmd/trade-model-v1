\set ON_ERROR_STOP on
\pset tuples_only on
\pset format unaligned
\pset fieldsep '|'

\if :{?fingerprint_mode}
\else
\set fingerprint_mode FULL
\endif

SELECT :'fingerprint_mode' NOT IN ('FULL', 'MIGRATION_STABLE') AS mode_invalid
\gset fingerprint_
\if :fingerprint_mode_invalid
\echo 'CONTENT_FINGERPRINT_STATUS|BLOCKED_INVALID_MODE'
\quit 3
\endif

BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '120s';
SET LOCAL lock_timeout = '5s';
SET LOCAL TIME ZONE 'UTC';

SELECT 'CONTENT_FINGERPRINT_VERSION', '1';
SELECT 'CONTENT_FINGERPRINT_MODE', :'fingerprint_mode';
SELECT 'CONTENT_HASH_SEED_A', '20260715';
SELECT 'CONTENT_HASH_SEED_B', '11270031';

WITH fingerprint_tables AS (
    SELECT table_schema,
           table_name,
           CASE
               WHEN :'fingerprint_mode' = 'MIGRATION_STABLE'
                    AND table_name = 'tm_decision_result'
                   THEN '(to_jsonb(row_data) - ''valid_from'' - ''expires_at'')'
               ELSE 'to_jsonb(row_data)'
           END AS row_json_expression
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
      AND table_name LIKE 'tm\_%' ESCAPE '\'
)
SELECT format(
    'SELECT %L, COUNT(*)::bigint, '
    || 'COALESCE(SUM(hashtextextended((%s)::text, 20260715)), 0)::numeric, '
    || 'COALESCE(bit_xor(hashtextextended((%s)::text, 20260715)), 0)::bigint, '
    || 'COALESCE(SUM(hashtextextended((%s)::text, 11270031)), 0)::numeric, '
    || 'COALESCE(bit_xor(hashtextextended((%s)::text, 11270031)), 0)::bigint '
    || 'FROM %I.%I AS row_data;',
    'CONTENT_TABLE|' || table_name,
    row_json_expression,
    row_json_expression,
    row_json_expression,
    row_json_expression,
    table_schema,
    table_name
)
FROM fingerprint_tables
ORDER BY table_name
\gexec

COMMIT;
