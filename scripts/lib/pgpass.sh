#!/usr/bin/env bash

escape_pgpass_field() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//:/\\:}"
  printf '%s' "${value}"
}

portable_file_mode() {
  stat -c '%a' "$1" 2>/dev/null || stat -f '%Lp' "$1"
}

portable_file_owner_uid() {
  stat -c '%u' "$1" 2>/dev/null || stat -f '%u' "$1"
}

read_secure_password_file() {
  local password_file="$1"
  local mode owner_uid newline_count carriage_count nul_count last_byte
  local password_value

  [ -f "${password_file}" ] && [ ! -L "${password_file}" ] \
    && [ -s "${password_file}" ] || return 1
  mode="$(portable_file_mode "${password_file}")" || return 1
  owner_uid="$(portable_file_owner_uid "${password_file}")" || return 1
  case "${mode}" in ''|*[!0-7]*) return 1 ;; esac
  if [ "$(id -u)" -ne 0 ]; then
    [ "${owner_uid}" = "$(id -u)" ] || [ "${owner_uid}" = 0 ] || return 1
  fi
  (( (8#${mode} & 077) == 0 )) || return 1

  nul_count="$(LC_ALL=C od -An -tx1 "${password_file}" \
    | awk '{ for (i = 1; i <= NF; i++) if ($i == "00") count++ } END { print count + 0 }')"
  [ "${nul_count}" -eq 0 ] || return 1
  carriage_count="$(LC_ALL=C tr -cd '\r' <"${password_file}" \
    | wc -c | tr -d '[:space:]')"
  [ "${carriage_count}" -eq 0 ] || return 1
  newline_count="$(LC_ALL=C tr -cd '\n' <"${password_file}" | wc -c | tr -d '[:space:]')"
  case "${newline_count}" in ''|*[!0-9]*) return 1 ;; esac
  [ "${newline_count}" -le 1 ] || return 1
  if [ "${newline_count}" -eq 1 ]; then
    last_byte="$(tail -c 1 "${password_file}" | od -An -tx1 | tr -d '[:space:]')"
    [ "${last_byte}" = 0a ] || return 1
  fi

  password_value=""
  IFS= read -r password_value <"${password_file}" || [ -n "${password_value}" ]
  [ -n "${password_value}" ] || return 1
  P3H_SECURE_PASSWORD_VALUE="${password_value}"
}

write_pgpass_file() {
  local password_file="$1"
  local output_file="$2"
  local host="$3"
  local port="$4"
  local database="$5"
  local username="$6"
  local password

  read_secure_password_file "${password_file}" || return 1
  password="${P3H_SECURE_PASSWORD_VALUE}"
  P3H_SECURE_PASSWORD_VALUE=""
  printf '%s:%s:%s:%s:%s\n' \
    "$(escape_pgpass_field "${host}")" \
    "$(escape_pgpass_field "${port}")" \
    "$(escape_pgpass_field "${database}")" \
    "$(escape_pgpass_field "${username}")" \
    "$(escape_pgpass_field "${password}")" >"${output_file}"
  password=""
  chmod 600 "${output_file}"
}
