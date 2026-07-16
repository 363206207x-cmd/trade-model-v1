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
  WITH expected_tables(name) AS (VALUES
    ('tm_analysis_run'), ('tm_evidence_item'), ('tm_score_item'),
    ('tm_macro_event'), ('tm_news_event'), ('tm_decision_result'),
    ('tm_execution_plan'), ('tm_market_environment_snapshot'),
    ('tm_persisted_ohlcv_bar'), ('tm_rule_config'), ('tm_user_config'),
    ('tm_real_position'), ('tm_user_position'), ('tm_position_monitor_log'),
    ('tm_push_snapshot'), ('tm_account_risk_snapshot'), ('tm_push_recheck_log'),
    ('tm_push_recheck_dispatch_config'), ('tm_push_recheck_dispatch_config_audit'),
    ('tm_monitor_alert'), ('tm_opportunity_log'), ('tm_missed_opportunity'),
    ('tm_review_result'), ('tm_rule_version_log'), ('tm_asset_state'),
    ('tm_hot_reset_event'), ('tm_ai_call_log')
  ), violations AS (
    SELECT 'missing_table' AS kind, e.name
    FROM expected_tables e
    WHERE to_regclass('public.' || e.name) IS NULL
    UNION ALL
    SELECT 'unexpected_table', c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
      AND c.relname <> 'flyway_schema_history'
      AND NOT EXISTS (SELECT 1 FROM expected_tables e WHERE e.name = c.relname)
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
          AND EXISTS (SELECT 1 FROM expected_tables e WHERE e.name = owner_table.relname)
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

rule_config_violation_count="$(${psql_primary} --command="
  WITH expected(rule_id, min_version) AS (VALUES
    ('cfg-ai-conflict-level1-max',3), ('cfg-ai-conflict-level2-max',3),
    ('cfg-ai-conflict-level3-max',3), ('cfg-ai-conflict-single-objection-max',3),
    ('cfg-confused-enter-threshold',3), ('cfg-confused-exit-cycles',3),
    ('cfg-confused-exit-threshold',3), ('cfg-confused-push-block-threshold',3),
    ('cfg-hot-reset-liquidity-drain',3), ('cfg-hot-reset-oi-collapse',3),
    ('cfg-hot-reset-price-move',3), ('cfg-hot-reset-systemic-severity',3),
    ('cfg-missed-max-mae-ratio',3), ('cfg-missed-min-mfe-ratio',3),
    ('cfg-missed-review-window-hours',3), ('cfg-push-recheck-confused-block',3),
    ('cfg-push-recheck-confused-wait',3), ('cfg-push-recheck-drift-ratio',3),
    ('cfg-push-recheck-exec-feas-wait',3),
    ('cfg-provider-scan-data-quality',5), ('cfg-provider-scan-downgrade-cooldown',5),
    ('cfg-provider-scan-emergency-confused',5), ('cfg-provider-scan-emergency-hold',5),
    ('cfg-provider-scan-emergency-liquidation',5), ('cfg-provider-scan-emergency-price',5),
    ('cfg-provider-scan-high-atr',5), ('cfg-provider-scan-high-funding',5),
    ('cfg-provider-scan-high-hold',5), ('cfg-provider-scan-high-oi',5),
    ('cfg-provider-scan-high-price',5), ('cfg-provider-scan-high-spread',5),
    ('cfg-provider-scan-high-volume',5), ('cfg-provider-scan-near-boundary',5),
    ('cfg-provider-scan-recovery-cycles',5), ('cfg-provider-scan-standard-confused',5),
    ('cfg-deriv-eight-score-cap',6), ('cfg-deriv-eight-score-factor',6),
    ('cfg-deriv-exchange-concentration',6), ('cfg-deriv-funding-negative',6),
    ('cfg-deriv-funding-positive',6), ('cfg-deriv-high-risk-downgrade',6),
    ('cfg-deriv-liquidation-15m',6), ('cfg-deriv-liquidation-5m',6),
    ('cfg-deriv-liquidation-imbalance',6), ('cfg-deriv-long-crowding',6),
    ('cfg-deriv-max-age',6), ('cfg-deriv-min-data-quality',6),
    ('cfg-deriv-min-datasets',6), ('cfg-deriv-monitor-refresh',6),
    ('cfg-deriv-oi-15m-strong',6), ('cfg-deriv-oi-15m-weak',6),
    ('cfg-deriv-oi-5m-strong',6), ('cfg-deriv-oi-5m-weak',6),
    ('cfg-deriv-partial-penalty',6), ('cfg-deriv-required-confirm',6),
    ('cfg-deriv-score-cap',6), ('cfg-deriv-short-crowding',6),
    ('cfg-deriv-stale-penalty',6), ('cfg-deriv-trend-score-cap',6)
  ), expected_for_version AS (
    SELECT rule_id FROM expected WHERE min_version <= ${applied_version}
  ), differences AS (
    (SELECT rule_id FROM expected_for_version EXCEPT SELECT rule_id FROM tm_rule_config)
    UNION ALL
    (SELECT rule_id FROM tm_rule_config EXCEPT SELECT rule_id FROM expected_for_version)
  )
  SELECT count(*) FROM differences")"
[ "${rule_config_violation_count}" = "0" ] \
  || { echo "P3H_GREENFIELD_RECOVERY_VERIFY: BLOCKED_RULE_DEFAULT_STATE" >&2; exit 2; }

echo "P3H_GREENFIELD_RECOVERY_VERIFY: PASS"
echo "P3H_RECOVERY_APPLIED_PREFIX: ${applied_versions}"
echo "P3H_RECOVERY_BUSINESS_ROWS: 0"
echo "P3H_RECOVERY_UNKNOWN_BUSINESS_OBJECTS: 0"
