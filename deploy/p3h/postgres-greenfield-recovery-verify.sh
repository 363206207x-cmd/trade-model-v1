#!/bin/sh
set -eu

admin_secret=/run/secrets/postgres_admin_password
if [ ! -f "${admin_secret}" ] || [ -L "${admin_secret}" ] || [ ! -s "${admin_secret}" ]; then
  echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

pgpass_file="$(mktemp)"
trap 'rm -f "${pgpass_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${admin_secret}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

psql_primary="psql --host=postgres --username=p3h_bootstrap --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1"
psql_postgres="psql --host=postgres --username=p3h_bootstrap --dbname=postgres --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1"

flyway_state="$(${psql_primary} --command="
  SELECT count(*) FILTER (WHERE success)::text || '|' ||
         coalesce(max(version::integer) FILTER (WHERE success), 0)::text || '|' ||
         count(*) FILTER (WHERE NOT success)::text || '|' ||
         coalesce(string_agg(version, ',' ORDER BY installed_rank) FILTER (WHERE success), '')
  FROM flyway_schema_history")"
IFS='|' read -r successful_count applied_version failed_count applied_versions <<EOF
${flyway_state}
EOF

case "${applied_version}" in
  1) expected_versions=1 ;;
  2) expected_versions=1,2 ;;
  3) expected_versions=1,2,3 ;;
  4) expected_versions=1,2,3,4 ;;
  5) expected_versions=1,2,3,4,5 ;;
  6) expected_versions=1,2,3,4,5,6 ;;
  7) expected_versions=1,2,3,4,5,6,7 ;;
  8) expected_versions=1,2,3,4,5,6,7,8 ;;
  9) expected_versions=1,2,3,4,5,6,7,8,9 ;;
  *) echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_FLYWAY_PREFIX" >&2; exit 2 ;;
esac
if [ "${successful_count}" != "${applied_version}" ] \
    || [ "${failed_count}" != "0" ] \
    || [ "${applied_versions}" != "${expected_versions}" ]; then
  echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_FLYWAY_PREFIX" >&2
  exit 2
fi

role_count="$(${psql_postgres} --command="
  SELECT count(*) FROM pg_roles
  WHERE rolname IN (
    'p3h_migration_owner', 'p3h_app_readonly',
    'p3h_backup_reader', 'p3h_recovery_owner'
  )
    AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole
    AND NOT rolinherit AND NOT rolreplication")"
[ "${role_count}" = "4" ] \
  || { echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_ROLE_IDENTITY" >&2; exit 2; }

database_count="$(${psql_postgres} --command="
  SELECT count(*)
  FROM pg_database d
  JOIN pg_roles r ON r.oid = d.datdba
  WHERE (d.datname = 'trade_model_v1_p3h_primary' AND r.rolname = 'p3h_migration_owner')
     OR (d.datname = 'trade_model_v1_p3h_recovery' AND r.rolname = 'p3h_recovery_owner')")"
[ "${database_count}" = "2" ] \
  || { echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_DATABASE_IDENTITY" >&2; exit 2; }

object_violation_count="$(${psql_primary} --command="
  WITH expected_tables(name, min_version) AS (VALUES
    ('tm_analysis_run', 1), ('tm_evidence_item', 1), ('tm_score_item', 1),
    ('tm_macro_event', 1), ('tm_news_event', 1), ('tm_decision_result', 1),
    ('tm_execution_plan', 1), ('tm_market_environment_snapshot', 1),
    ('tm_persisted_ohlcv_bar', 1), ('tm_rule_config', 1), ('tm_user_config', 1),
    ('tm_real_position', 1), ('tm_user_position', 1), ('tm_position_monitor_log', 1),
    ('tm_push_snapshot', 1), ('tm_account_risk_snapshot', 1), ('tm_push_recheck_log', 1),
    ('tm_push_recheck_dispatch_config', 1), ('tm_push_recheck_dispatch_config_audit', 1),
    ('tm_monitor_alert', 1), ('tm_opportunity_log', 1), ('tm_missed_opportunity', 1),
    ('tm_review_result', 1), ('tm_rule_version_log', 1), ('tm_asset_state', 1),
    ('tm_hot_reset_event', 1), ('tm_ai_call_log', 1), ('tm_user', 8)
  ), violations AS (
    SELECT 'missing_table' AS kind, e.name
    FROM expected_tables e
    WHERE e.min_version <= ${applied_version}
      AND to_regclass('public.' || e.name) IS NULL
    UNION ALL
    SELECT 'unexpected_table', c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
      AND c.relname <> 'flyway_schema_history'
      AND NOT EXISTS (
        SELECT 1 FROM expected_tables e
        WHERE e.name = c.relname AND e.min_version <= ${applied_version}
      )
    UNION ALL
    SELECT 'unexpected_relation', c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind IN ('v', 'm', 'f')
    UNION ALL
    SELECT 'unexpected_sequence', c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind = 'S'
      AND NOT EXISTS (
        SELECT 1
        FROM pg_depend d
        JOIN pg_class owner_table ON owner_table.oid = d.refobjid
        WHERE d.objid = c.oid AND d.deptype IN ('a', 'i')
          AND EXISTS (
            SELECT 1 FROM expected_tables e
            WHERE e.name = owner_table.relname AND e.min_version <= ${applied_version}
          )
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
[ "${object_violation_count}" = "0" ] \
  || { echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_UNKNOWN_BUSINESS_OBJECT" >&2; exit 2; }

${psql_primary} --command="
  DO \$\$
  DECLARE
    candidate record;
    contains_rows boolean;
  BEGIN
    FOR candidate IN
      SELECT quote_ident(schemaname) || '.' || quote_ident(tablename) AS qualified_name
      FROM pg_tables
      WHERE schemaname = 'public'
        AND tablename NOT IN ('flyway_schema_history', 'tm_rule_config')
    LOOP
      EXECUTE 'SELECT EXISTS (SELECT 1 FROM ' || candidate.qualified_name || ' LIMIT 1)'
        INTO contains_rows;
      IF contains_rows THEN
        RAISE EXCEPTION 'P3-H recovery business rows are not allowed';
      END IF;
    END LOOP;
  END
  \$\$" >/dev/null \
  || { echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_BUSINESS_DATA" >&2; exit 2; }

if ! /p3h/postgres-versioned-contract-verify.sh "${applied_version}" RECOVERY; then
  echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_VERSIONED_CONTENT_CONTRACT" >&2
  exit 2
fi

echo "P3H_GREENFIELD_RECOVERY_VERIFY: PASS"
echo "P3H_RECOVERY_APPLIED_PREFIX: ${applied_versions}"
echo "P3H_RECOVERY_BUSINESS_ROWS: 0"
echo "P3H_RECOVERY_UNKNOWN_BUSINESS_OBJECTS: 0"
