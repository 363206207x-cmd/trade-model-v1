#!/usr/bin/env bash
set -euo pipefail

mode="${1:-}"
evidence_file="${2:-}"
case "${mode}" in
  ADMIN|DATABASE|TLS) ;;
  *) echo "P3H_SECRET_ROTATION: BLOCKED_MODE"; exit 2 ;;
esac
if [ -z "${evidence_file}" ] || [ ! -f "${evidence_file}" ] || [ -L "${evidence_file}" ]; then
  echo "P3H_SECRET_ROTATION: BLOCKED_EVIDENCE"
  exit 2
fi

required=(
  OLD_VERSION_PRECHECK
  NEW_VERSION_PRE_ACTIVATION
  NEW_VERSION_POST_ACTIVATION
  OLD_VERSION_POST_REVOCATION
  SECRET_VALUE_EXPOSED
)
for key in "${required[@]}"; do
  [ "$(grep -c "^${key}=" "${evidence_file}" || true)" = "1" ] \
    || { echo "P3H_SECRET_ROTATION: BLOCKED_EVIDENCE"; exit 2; }
done
if [ "$(wc -l <"${evidence_file}" | tr -d ' ')" != "5" ] \
    || grep -Ev '^(OLD_VERSION_PRECHECK|NEW_VERSION_PRE_ACTIVATION|NEW_VERSION_POST_ACTIVATION|OLD_VERSION_POST_REVOCATION|SECRET_VALUE_EXPOSED)=' \
      "${evidence_file}" | grep -q .; then
  echo "P3H_SECRET_ROTATION: BLOCKED_EVIDENCE"
  exit 2
fi
grep -qx 'OLD_VERSION_PRECHECK=PASS' "${evidence_file}"
grep -qx 'NEW_VERSION_PRE_ACTIVATION=DENIED' "${evidence_file}"
grep -qx 'NEW_VERSION_POST_ACTIVATION=PASS' "${evidence_file}"
grep -qx 'OLD_VERSION_POST_REVOCATION=DENIED' "${evidence_file}"
grep -qx 'SECRET_VALUE_EXPOSED=NO' "${evidence_file}"

case "${mode}" in
  ADMIN)
    echo "ADMIN_SECRET_ROTATION: PASS"
    echo "OLD_ADMIN_SECRET: REVOKED"
    ;;
  DATABASE)
    echo "APP_DATABASE_SECRET_ROTATION: PASS"
    echo "OLD_DATABASE_SECRET: REVOKED"
    ;;
  TLS)
    echo "TLS_RENEWAL_OR_ROTATION: PASS"
    echo "OLD_TLS_CERTIFICATE: REVOKED"
    ;;
esac
