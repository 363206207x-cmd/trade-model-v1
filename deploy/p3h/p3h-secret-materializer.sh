#!/bin/sh
set -eu

readonly_target=/materialized
app_uid=10001
app_gid=10001

if [ "$(readlink -f "${readonly_target}")" != "${readonly_target}" ]; then
  echo "P3H_SECRET_MATERIALIZER: BLOCKED_TARGET" >&2
  exit 2
fi

for secret_name in app_database_password_v1 app_admin_password_v1 \
    binance_nonfunctional_key binance_nonfunctional_secret; do
  secret_path="/run/secrets/${secret_name}"
  if [ ! -f "${secret_path}" ] || [ -L "${secret_path}" ] || [ ! -s "${secret_path}" ]; then
    echo "P3H_SECRET_MATERIALIZER: BLOCKED_SOURCE" >&2
    exit 2
  fi
done

find "${readonly_target}" -mindepth 1 -delete
mkdir -p "${readonly_target}/config"
chown "${app_uid}:${app_gid}" "${readonly_target}" "${readonly_target}/config"
chmod 700 "${readonly_target}" "${readonly_target}/config"

copy_secret() {
  source_path="$1"
  target_path="$2"
  cp "${source_path}" "${target_path}"
  chown "${app_uid}:${app_gid}" "${target_path}"
  chmod 400 "${target_path}"
}

copy_secret /run/secrets/app_database_password_v1 \
  "${readonly_target}/config/spring.datasource.password"
copy_secret /run/secrets/app_admin_password_v1 \
  "${readonly_target}/config/trade-model.auth.admin-password"
copy_secret /run/secrets/binance_nonfunctional_key \
  "${readonly_target}/config/binance.api.key"
copy_secret /run/secrets/binance_nonfunctional_secret \
  "${readonly_target}/config/binance.api.secret"

for materialized_secret in "${readonly_target}"/config/*; do
  ownership_and_mode="$(stat -c '%u:%g:%a' "${materialized_secret}")"
  if [ "${ownership_and_mode}" != "${app_uid}:${app_gid}:400" ]; then
    echo "P3H_SECRET_MATERIALIZER: BLOCKED_PERMISSIONS" >&2
    exit 2
  fi
done

echo "P3H_SECRET_MATERIALIZER: PASS"
echo "P3H_SECRET_TARGET_UID: ${app_uid}"
echo "P3H_SECRET_TARGET_GID: ${app_gid}"
echo "P3H_SECRET_TARGET_MODE: 0400"
