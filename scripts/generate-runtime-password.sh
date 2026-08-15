#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
build_log="$(mktemp)"
chmod 600 "${build_log}"
trap 'rm -f -- "${build_log}"' EXIT

if ! (cd "${repo_root}" && ./mvnw -q -DskipTests compile >"${build_log}" 2>&1); then
  printf '%s\n' "PASSWORD_TOOL_BUILD=FAILED" >&2
  exit 1
fi

cd "${repo_root}"
java -cp target/classes org.example.trademodel.security.RuntimePasswordTool generate "$@"
