#!/usr/bin/env bash
set -euo pipefail

ATTESTATION_FILE="${1:-}"
EXPECTED_CLASS="${2:-}"
DUMP_LIST_FILE="${3:-}"
ACTUAL_FLYWAY_VERSION="${4:-}"

fail() {
  echo "ATTESTATION_VALIDATION_STATUS: $1"
  echo "ATTESTATION_RAW_CONTENT: NOT_EMITTED"
  exit 2
}

parse_utc_epoch() {
  local value="$1"
  if date -u -j -f '%Y-%m-%dT%H:%M:%SZ' "${value}" '+%s' >/dev/null 2>&1; then
    date -u -j -f '%Y-%m-%dT%H:%M:%SZ' "${value}" '+%s'
    return
  fi
  date -u -d "${value}" '+%s' 2>/dev/null
}

if [ -z "${ATTESTATION_FILE}" ] || [ ! -f "${ATTESTATION_FILE}" ]; then
  fail "BLOCKED_FILE_MISSING"
fi
if [ -L "${ATTESTATION_FILE}" ]; then
  fail "BLOCKED_SYMLINK"
fi
case "${EXPECTED_CLASS}" in
  GENERATED_RELEASE_LIKE|SANITIZED_RELEASE_LIKE) ;;
  *) fail "BLOCKED_EXPECTED_CLASS" ;;
esac
if LC_ALL=C grep -q '[[:cntrl:]]' "${ATTESTATION_FILE}"; then
  fail "BLOCKED_CONTROL_CHARACTER"
fi

data_source_class_count=0
sanitization_owner_count=0
generated_at_count=0
postgresql_version_count=0
flyway_version_count=0
user_identifiers_count=0
secrets_removed_count=0
free_text_count=0
local_rehearsal_count=0
not_production_count=0
fixture_seed_count=0
real_user_data_count=0
real_account_data_count=0
real_market_data_count=0
sanitized_gate_count=0

data_source_class=""
sanitization_owner=""
generated_at_utc=""
source_postgresql_version=""
source_flyway_version=""
user_identifiers_removed=""
secrets_removed=""
free_text_cleaned=""
local_rehearsal_allowed=""
not_production=""
fixture_seed=""
real_user_data=""
real_account_data=""
real_market_data=""
sanitized_gate=""

while IFS= read -r line || [ -n "${line}" ]; do
  if [[ ! "${line}" =~ ^[A-Z][A-Z0-9_]*=[^[:space:]].*$ ]]; then
    fail "BLOCKED_LINE_FORMAT"
  fi
  key="${line%%=*}"
  value="${line#*=}"
  case "${key}" in
    DATA_SOURCE_CLASS)
      [ "${data_source_class_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      data_source_class_count=1; data_source_class="${value}" ;;
    SANITIZATION_OWNER_OR_PROCESS)
      [ "${sanitization_owner_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      sanitization_owner_count=1; sanitization_owner="${value}" ;;
    GENERATED_AT_UTC)
      [ "${generated_at_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      generated_at_count=1; generated_at_utc="${value}" ;;
    SOURCE_POSTGRESQL_VERSION)
      [ "${postgresql_version_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      postgresql_version_count=1; source_postgresql_version="${value}" ;;
    SOURCE_FLYWAY_VERSION)
      [ "${flyway_version_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      flyway_version_count=1; source_flyway_version="${value}" ;;
    USER_IDENTIFIERS_REMOVED_OR_PSEUDONYMIZED)
      [ "${user_identifiers_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      user_identifiers_count=1; user_identifiers_removed="${value}" ;;
    SECRETS_REMOVED)
      [ "${secrets_removed_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      secrets_removed_count=1; secrets_removed="${value}" ;;
    FREE_TEXT_CLEANED_OR_REPLACED)
      [ "${free_text_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      free_text_count=1; free_text_cleaned="${value}" ;;
    LOCAL_CONTROLLED_REHEARSAL_ALLOWED)
      [ "${local_rehearsal_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      local_rehearsal_count=1; local_rehearsal_allowed="${value}" ;;
    NOT_PRODUCTION_AND_NOT_FOR_PRODUCTION_RESTORE)
      [ "${not_production_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      not_production_count=1; not_production="${value}" ;;
    FIXTURE_SEED)
      [ "${fixture_seed_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      fixture_seed_count=1; fixture_seed="${value}" ;;
    REAL_USER_DATA_INCLUDED)
      [ "${real_user_data_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      real_user_data_count=1; real_user_data="${value}" ;;
    REAL_ACCOUNT_DATA_INCLUDED)
      [ "${real_account_data_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      real_account_data_count=1; real_account_data="${value}" ;;
    REAL_MARKET_PROVIDER_DATA_INCLUDED)
      [ "${real_market_data_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      real_market_data_count=1; real_market_data="${value}" ;;
    SUITABLE_FOR_FINAL_SANITIZED_CLONE_GATE)
      [ "${sanitized_gate_count}" -eq 0 ] || fail "BLOCKED_DUPLICATE_KEY"
      sanitized_gate_count=1; sanitized_gate="${value}" ;;
    *) fail "BLOCKED_UNKNOWN_KEY" ;;
  esac
done <"${ATTESTATION_FILE}"

for required_count in \
  "${data_source_class_count}" "${sanitization_owner_count}" \
  "${generated_at_count}" "${postgresql_version_count}" \
  "${flyway_version_count}" "${user_identifiers_count}" \
  "${secrets_removed_count}" "${free_text_count}" \
  "${local_rehearsal_count}" "${not_production_count}"; do
  [ "${required_count}" -eq 1 ] || fail "BLOCKED_REQUIRED_KEY"
done

[ "${data_source_class}" = "${EXPECTED_CLASS}" ] || fail "BLOCKED_DATA_SOURCE_CLASS"
[ -n "${sanitization_owner}" ] || fail "BLOCKED_OWNER"
for required_yes in \
  "${user_identifiers_removed}" "${secrets_removed}" "${free_text_cleaned}" \
  "${local_rehearsal_allowed}" "${not_production}"; do
  [ "${required_yes}" = "YES" ] || fail "BLOCKED_REQUIRED_YES"
done

if [[ ! "${generated_at_utc}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$ ]]; then
  fail "BLOCKED_GENERATED_AT_FORMAT"
fi
generated_epoch="$(parse_utc_epoch "${generated_at_utc}" || true)"
[ -n "${generated_epoch}" ] || fail "BLOCKED_GENERATED_AT_FORMAT"
if [ "${generated_epoch}" -gt "$(date -u '+%s')" ]; then
  fail "BLOCKED_GENERATED_AT_FUTURE"
fi
[[ "${source_postgresql_version}" =~ ^[0-9]+([.][0-9]+){0,2}$ ]] \
  || fail "BLOCKED_POSTGRESQL_VERSION_FORMAT"
[[ "${source_flyway_version}" =~ ^(6|7)$ ]] \
  || fail "BLOCKED_FLYWAY_VERSION_FORMAT"

if [ "${EXPECTED_CLASS}" = "GENERATED_RELEASE_LIKE" ]; then
  for generated_count in \
    "${fixture_seed_count}" "${real_user_data_count}" "${real_account_data_count}" \
    "${real_market_data_count}" "${sanitized_gate_count}"; do
    [ "${generated_count}" -eq 1 ] || fail "BLOCKED_GENERATED_REQUIRED_KEY"
  done
  [ "${fixture_seed}" = "20260715" ] || fail "BLOCKED_FIXTURE_SEED"
  [ "${real_user_data}" = "NO" ] || fail "BLOCKED_REAL_USER_DATA"
  [ "${real_account_data}" = "NO" ] || fail "BLOCKED_REAL_ACCOUNT_DATA"
  [ "${real_market_data}" = "NO" ] || fail "BLOCKED_REAL_MARKET_DATA"
  [ "${sanitized_gate}" = "NO" ] || fail "BLOCKED_SANITIZED_GATE_CLAIM"
else
  if [ "${fixture_seed_count}" -ne 0 ] || [ "${real_user_data_count}" -ne 0 ] \
    || [ "${real_account_data_count}" -ne 0 ] || [ "${real_market_data_count}" -ne 0 ] \
    || [ "${sanitized_gate_count}" -ne 0 ]; then
    fail "BLOCKED_CLASS_SPECIFIC_KEY"
  fi
fi

if [ -n "${DUMP_LIST_FILE}" ]; then
  [ -f "${DUMP_LIST_FILE}" ] || fail "BLOCKED_DUMP_HEADER_MISSING"
  dump_version_count="$(grep -Ec '^;[[:space:]]+Dumped from database version:' "${DUMP_LIST_FILE}" || true)"
  [ "${dump_version_count}" -eq 1 ] || fail "BLOCKED_DUMP_HEADER_VERSION"
  dump_postgresql_version="$(sed -n \
    's/^;[[:space:]]*Dumped from database version:[[:space:]]*\([^[:space:]]*\).*$/\1/p' \
    "${DUMP_LIST_FILE}")"
  [ "${source_postgresql_version}" = "${dump_postgresql_version}" ] \
    || fail "BLOCKED_POSTGRESQL_VERSION_MISMATCH"
fi

if [ -n "${ACTUAL_FLYWAY_VERSION}" ]; then
  [[ "${ACTUAL_FLYWAY_VERSION}" =~ ^(6|7)$ ]] \
    || fail "BLOCKED_ACTUAL_FLYWAY_VERSION"
  [ "${source_flyway_version}" = "${ACTUAL_FLYWAY_VERSION}" ] \
    || fail "BLOCKED_FLYWAY_VERSION_MISMATCH"
fi

echo "ATTESTATION_VALIDATION_STATUS: PASS"
echo "ATTESTATION_UNIQUENESS_STATUS: PASS"
echo "ATTESTATION_DATA_SOURCE_CLASS_STATUS: PASS"
echo "ATTESTATION_GENERATED_AT_STATUS: PASS"
echo "ATTESTATION_POSTGRESQL_VERSION_STATUS: PASS"
echo "ATTESTATION_FLYWAY_VERSION_STATUS: PASS"
echo "ATTESTATION_RAW_CONTENT: NOT_EMITTED"
