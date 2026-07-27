#!/bin/sh
set -eu

expected_version="${1:-}"
contract_phase="${2:-}"
case "${expected_version}" in
  1|2|3|4|5|6|7|8|9) ;;
  *) echo "P3H_VERSIONED_CONTRACT_VERIFY: BLOCKED_VERSION" >&2; exit 2 ;;
esac
case "${contract_phase}" in
  RECOVERY|STEADY_STATE) ;;
  *) echo "P3H_VERSIONED_CONTRACT_VERIFY: BLOCKED_PHASE" >&2; exit 2 ;;
esac

admin_secret=/run/secrets/postgres_admin_password
if [ ! -f "${admin_secret}" ] || [ -L "${admin_secret}" ] || [ ! -s "${admin_secret}" ]; then
  echo "P3H_VERSIONED_CONTRACT_VERIFY: BLOCKED_MISSING_SECRET" >&2
  exit 2
fi

pgpass_file="$(mktemp)"
trap 'rm -f "${pgpass_file}"' EXIT HUP INT TERM
chmod 600 "${pgpass_file}"
printf 'postgres:5432:*:p3h_bootstrap:%s\n' "$(tr -d '\r\n' <"${admin_secret}")" >"${pgpass_file}"
export PGPASSFILE="${pgpass_file}"

psql_base="psql --host=postgres --username=p3h_bootstrap --dbname=trade_model_v1_p3h_primary --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1"

if ! ${psql_base} --set="p3h_expected_version=${expected_version}" \
    --file=/p3h/postgres-rule-defaults-verify.sql >/dev/null 2>&1; then
  echo "P3H_VERSIONED_CONTRACT_VERIFY: BLOCKED_RULE_DEFAULT_CONTENT" >&2
  exit 2
fi

case "${expected_version}" in
  1) expected_schema_fingerprint=573b90f5113dacf4a272b5ea81ab92bf ;;
  2) expected_schema_fingerprint=38dcfda7f1a5cf93383bb80940c537dc ;;
  3) expected_schema_fingerprint=38dcfda7f1a5cf93383bb80940c537dc ;;
  4) expected_schema_fingerprint=bc883a45f33f337ffc48ab871160d320 ;;
  5) expected_schema_fingerprint=86a2f78fed09fc3df728e441d57f6d87 ;;
  6) expected_schema_fingerprint=86a2f78fed09fc3df728e441d57f6d87 ;;
  7) expected_schema_fingerprint=e302cdbc60b3c0d6a441dbc856106ded ;;
  8) expected_schema_fingerprint=c491831c3a30a7f0cd411b4aedeee4ac ;;
  9) expected_schema_fingerprint=14c497071547839bdf74c015761633a4 ;;
esac

actual_schema_fingerprint="$(${psql_base} --file=/p3h/postgres-schema-contract.sql)" \
  || { echo "P3H_VERSIONED_CONTRACT_VERIFY: BLOCKED_SCHEMA_CAPTURE" >&2; exit 2; }
if [ "${actual_schema_fingerprint}" != "${expected_schema_fingerprint}" ]; then
  echo "P3H_VERSIONED_CONTRACT_VERIFY: BLOCKED_SCHEMA_CONTRACT" >&2
  exit 2
fi

echo "RULE_DEFAULT_CONTENT_CONTRACT: MATCH_EXACT_VERSIONED_ROWS"
case "${contract_phase}" in
  RECOVERY) echo "RECOVERY_SCHEMA_CONTRACT: MATCH_EXACT_PREFIX" ;;
  STEADY_STATE) echo "STEADY_STATE_SCHEMA_CONTRACT: MATCH_EXACT_V${expected_version}" ;;
esac
echo "P3H_VERSIONED_CONTRACT_VERIFY: PASS"
