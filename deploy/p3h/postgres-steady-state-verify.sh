#!/bin/sh
set -eu

admin_secret=/run/secrets/postgres_admin_password
if [ ! -f "${admin_secret}" ] || [ -L "${admin_secret}" ] || [ ! -s "${admin_secret}" ]; then
  echo "P3H_STEADY_STATE_VERIFY: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

case "${P3H_STEADY_VERIFY_SCOPE:-}" in
  CORE_STATE_VERIFY|FULL_READONLY_STATE_VERIFY) ;;
  *) echo "P3H_STEADY_STATE_VERIFY: BLOCKED_VERIFY_SCOPE" >&2; exit 2 ;;
esac

pgpass_file="$(mktemp)"
trap 'rm -f "${pgpass_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${admin_secret}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

psql_primary="psql --host=postgres --username=p3h_bootstrap --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1"
psql_postgres="psql --host=postgres --username=p3h_bootstrap --dbname=postgres --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1"

flyway_state="$(${psql_primary} --command="
  SELECT count(*) FILTER (WHERE success)::text || '|' ||
         coalesce(max(version) FILTER (WHERE success), '') || '|' ||
         count(*) FILTER (WHERE NOT success)::text || '|' ||
         coalesce(string_agg(version, ',' ORDER BY installed_rank) FILTER (WHERE success), '')
  FROM flyway_schema_history")"
[ "${flyway_state}" = "7|7|0|1,2,3,4,5,6,7" ] \
  || { echo "P3H_STEADY_STATE_VERIFY: BLOCKED_FLYWAY_STATE" >&2; exit 2; }

role_count="$(${psql_postgres} --command="
  SELECT count(*) FROM pg_roles
  WHERE rolname IN (
    'p3h_migration_owner', 'p3h_app_readonly',
    'p3h_backup_reader', 'p3h_recovery_owner'
  )
    AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole
    AND NOT rolinherit AND NOT rolreplication")"
[ "${role_count}" = "4" ] \
  || { echo "P3H_STEADY_STATE_VERIFY: BLOCKED_ROLE_STATE" >&2; exit 2; }

database_count="$(${psql_postgres} --command="
  SELECT count(*)
  FROM pg_database d
  JOIN pg_roles r ON r.oid = d.datdba
  WHERE (d.datname = 'trade_model_v1_p3h_primary' AND r.rolname = 'p3h_migration_owner')
     OR (d.datname = 'trade_model_v1_p3h_recovery' AND r.rolname = 'p3h_recovery_owner')")"
[ "${database_count}" = "2" ] \
  || { echo "P3H_STEADY_STATE_VERIFY: BLOCKED_DATABASE_STATE" >&2; exit 2; }

schema_violation_count="$(${psql_primary} --command="
  WITH expected_tables(name) AS (VALUES
    ('flyway_schema_history'), ('tm_analysis_run'), ('tm_evidence_item'),
    ('tm_score_item'), ('tm_macro_event'), ('tm_news_event'),
    ('tm_decision_result'), ('tm_execution_plan'),
    ('tm_market_environment_snapshot'), ('tm_persisted_ohlcv_bar'),
    ('tm_rule_config'), ('tm_user_config'), ('tm_real_position'),
    ('tm_user_position'), ('tm_position_monitor_log'), ('tm_push_snapshot'),
    ('tm_account_risk_snapshot'), ('tm_push_recheck_log'),
    ('tm_push_recheck_dispatch_config'), ('tm_push_recheck_dispatch_config_audit'),
    ('tm_monitor_alert'), ('tm_opportunity_log'), ('tm_missed_opportunity'),
    ('tm_review_result'), ('tm_rule_version_log'), ('tm_asset_state'),
    ('tm_hot_reset_event'), ('tm_ai_call_log')
  ), required_columns(table_name, column_name) AS (VALUES
    ('tm_persisted_ohlcv_bar','fetch_time'),
    ('tm_persisted_ohlcv_bar','source_status'),
    ('tm_persisted_ohlcv_bar','freshness_status'),
    ('tm_persisted_ohlcv_bar','provenance_version'),
    ('tm_persisted_ohlcv_bar','ingestion_run_id'),
    ('tm_user_config','scan_base_profile'),
    ('tm_user_config','scan_position_profile'),
    ('tm_user_config','scan_pool_profile'),
    ('tm_decision_result','valid_from'),
    ('tm_decision_result','expires_at')
  ), violations AS (
    SELECT 'missing_table' AS kind, e.name
    FROM expected_tables e
    WHERE to_regclass('public.' || e.name) IS NULL
    UNION ALL
    SELECT 'unexpected_table', c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
      AND NOT EXISTS (SELECT 1 FROM expected_tables e WHERE e.name = c.relname)
    UNION ALL
    SELECT 'unexpected_relation', c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind IN ('v', 'm', 'f')
    UNION ALL
    SELECT 'missing_column', r.table_name || '.' || r.column_name
    FROM required_columns r
    WHERE NOT EXISTS (
      SELECT 1 FROM information_schema.columns c
      WHERE c.table_schema = 'public'
        AND c.table_name = r.table_name AND c.column_name = r.column_name
    )
    UNION ALL
    SELECT 'unexpected_schema', nspname FROM pg_namespace
    WHERE nspname <> 'public' AND nspname <> 'information_schema' AND nspname !~ '^pg_'
    UNION ALL
    SELECT 'unexpected_extension', extname FROM pg_extension WHERE extname <> 'plpgsql'
    UNION ALL
    SELECT 'unexpected_routine', n.nspname || '.' || p.proname
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
    UNION ALL
    SELECT 'unexpected_trigger', n.nspname || '.' || t.tgname
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE NOT t.tgisinternal AND n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
    UNION ALL
    SELECT 'unexpected_fdw', fdwname FROM pg_foreign_data_wrapper
    UNION ALL
    SELECT 'unexpected_server', srvname FROM pg_foreign_server
    UNION ALL
    SELECT 'unexpected_owner', c.relname
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_roles r ON r.oid = c.relowner
    WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'S')
      AND r.rolname <> 'p3h_migration_owner'
  )
  SELECT count(*) FROM violations")"
[ "${schema_violation_count}" = "0" ] \
  || { echo "P3H_STEADY_STATE_VERIFY: BLOCKED_SCHEMA_STATE" >&2; exit 2; }

echo "P3H_CORE_STATE_VERIFY: PASS"
if [ "${P3H_STEADY_VERIFY_SCOPE}" = "CORE_STATE_VERIFY" ]; then
  echo "P3H_STEADY_STATE_VERIFY: PASS"
  echo "P3H_FLYWAY_CURRENT_VERSION: 7"
  echo "P3H_FLYWAY_FAILED_MIGRATIONS: 0"
  exit 0
fi

role_membership_count="$(${psql_postgres} --command="
  SELECT count(*)
  FROM pg_auth_members m
  JOIN pg_roles member_role ON member_role.oid = m.member
  WHERE member_role.rolname IN ('p3h_app_readonly', 'p3h_backup_reader')")"
unsafe_grants="$(${psql_primary} --command="
  SELECT count(*)
  FROM information_schema.role_table_grants
  WHERE grantee IN ('p3h_app_readonly', 'p3h_backup_reader')
    AND privilege_type <> 'SELECT'")"
missing_selects="$(${psql_primary} --command="
  SELECT count(*)
  FROM information_schema.tables
  WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
    AND (NOT has_table_privilege('p3h_app_readonly', format('%I.%I', table_schema, table_name), 'SELECT')
      OR NOT has_table_privilege('p3h_backup_reader', format('%I.%I', table_schema, table_name), 'SELECT'))")"
schema_contract="$(${psql_primary} --command="
  SELECT has_schema_privilege('p3h_app_readonly', 'public', 'USAGE')
     AND NOT has_schema_privilege('p3h_app_readonly', 'public', 'CREATE')
     AND has_schema_privilege('p3h_backup_reader', 'public', 'USAGE')
     AND NOT has_schema_privilege('p3h_backup_reader', 'public', 'CREATE')")"
database_contract="$(${psql_primary} --command="
  SELECT has_database_privilege('p3h_app_readonly', 'trade_model_v1_p3h_primary', 'CONNECT')
     AND NOT has_database_privilege('p3h_app_readonly', 'trade_model_v1_p3h_primary', 'CREATE')
     AND NOT has_database_privilege('p3h_app_readonly', 'trade_model_v1_p3h_primary', 'TEMP')
     AND has_database_privilege('p3h_backup_reader', 'trade_model_v1_p3h_primary', 'CONNECT')
     AND NOT has_database_privilege('p3h_backup_reader', 'trade_model_v1_p3h_primary', 'CREATE')
     AND NOT has_database_privilege('p3h_backup_reader', 'trade_model_v1_p3h_primary', 'TEMP')")"
required_default_selects="$(${psql_primary} --command="
  SELECT count(*)
  FROM pg_default_acl d
  JOIN pg_roles owner_role ON owner_role.oid = d.defaclrole
  JOIN pg_namespace n ON n.oid = d.defaclnamespace
  CROSS JOIN LATERAL aclexplode(d.defaclacl) acl
  JOIN pg_roles grantee_role ON grantee_role.oid = acl.grantee
  WHERE owner_role.rolname = 'p3h_migration_owner'
    AND n.nspname = 'public'
    AND grantee_role.rolname IN ('p3h_app_readonly', 'p3h_backup_reader')
    AND d.defaclobjtype IN ('r', 'S')
    AND acl.privilege_type = 'SELECT'")"
unexpected_default_grants="$(${psql_primary} --command="
  SELECT count(*)
  FROM pg_default_acl d
  JOIN pg_roles owner_role ON owner_role.oid = d.defaclrole
  JOIN pg_namespace n ON n.oid = d.defaclnamespace
  CROSS JOIN LATERAL aclexplode(d.defaclacl) acl
  LEFT JOIN pg_roles grantee_role ON grantee_role.oid = acl.grantee
  WHERE owner_role.rolname = 'p3h_migration_owner'
    AND n.nspname = 'public'
    AND d.defaclobjtype IN ('r', 'S')
    AND acl.grantee <> d.defaclrole
    AND (coalesce(grantee_role.rolname, 'PUBLIC') NOT IN ('p3h_app_readonly', 'p3h_backup_reader')
      OR acl.privilege_type <> 'SELECT')")"
unsafe_sequence_count="$(${psql_primary} --command="
  SELECT count(*)
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'public' AND c.relkind = 'S'
    AND (
      NOT has_sequence_privilege('p3h_app_readonly', c.oid, 'SELECT')
      OR has_sequence_privilege('p3h_app_readonly', c.oid, 'USAGE')
      OR has_sequence_privilege('p3h_app_readonly', c.oid, 'UPDATE')
      OR NOT has_sequence_privilege('p3h_backup_reader', c.oid, 'SELECT')
      OR has_sequence_privilege('p3h_backup_reader', c.oid, 'USAGE')
      OR has_sequence_privilege('p3h_backup_reader', c.oid, 'UPDATE')
    )")"

if [ "${role_membership_count}" != "0" ]; then
  echo "P3H_STEADY_STATE_VERIFY: BLOCKED_ROLE_MEMBERSHIP" >&2
  exit 2
fi
if [ "${unsafe_grants}" != "0" ] || [ "${missing_selects}" != "0" ] \
    || [ "${schema_contract}" != "t" ] || [ "${database_contract}" != "t" ] \
    || [ "${required_default_selects}" != "4" ] \
    || [ "${unexpected_default_grants}" != "0" ] \
    || [ "${unsafe_sequence_count}" != "0" ]; then
  echo "P3H_STEADY_STATE_VERIFY: BLOCKED_READONLY_CONTRACT" >&2
  exit 2
fi

echo "P3H_FULL_READONLY_STATE_VERIFY: PASS"
echo "P3H_STEADY_STATE_VERIFY: PASS"
echo "P3H_FLYWAY_CURRENT_VERSION: 7"
echo "P3H_FLYWAY_FAILED_MIGRATIONS: 0"
echo "P3H_ROLE_AND_GRANT_CONTRACT: PASS"
echo "READONLY_ROLE_MEMBERSHIP_CONTRACT: PASS"
echo "READONLY_DEFAULT_ACL_CONTRACT: PASS"
echo "READONLY_SEQUENCE_PRIVILEGE_CONTRACT: PASS"
