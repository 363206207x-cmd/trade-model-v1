#!/usr/bin/env bash
set -euo pipefail

destination=/run/credentials/p3hlab1

if [ -z "${CREDENTIALS_DIRECTORY:-}" ] \
    || [ ! -d "${CREDENTIALS_DIRECTORY}" ] \
    || [ ! -d "${destination}" ] \
    || [ -L "${destination}" ]; then
  exit 2
fi

credential_names=(
  postgres_admin_password flyway_password
  app_database_password_v1 app_database_password_v2
  app_admin_password_v1 app_admin_password_v2
  backup_reader_password recovery_owner_password
  binance_nonfunctional_key binance_nonfunctional_secret
  tls_certificate tls_private_key tls_ca_certificate
  tls_certificate_v2 tls_private_key_v2
)

[ -z "$(find "${destination}" -mindepth 1 -maxdepth 1 -print -quit)" ] \
  || exit 2
for credential_name in "${credential_names[@]}"; do
  source_file="${CREDENTIALS_DIRECTORY}/${credential_name}"
  [ -f "${source_file}" ] && [ ! -L "${source_file}" ] && [ -s "${source_file}" ] \
    || exit 2
  install -m 0400 "${source_file}" "${destination}/${credential_name}"
done

[ "$(find "${destination}" -mindepth 1 -maxdepth 1 -type f | wc -l | tr -d ' ')" \
  -eq "${#credential_names[@]}" ]
