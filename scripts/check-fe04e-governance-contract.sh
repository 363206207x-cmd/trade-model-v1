#!/usr/bin/env bash

set -uo pipefail

tests=0
failures=0
errors=0
skipped=0

fail() {
  echo "FAIL: $*"
  failures=$((failures + 1))
}

error() {
  echo "ERROR: $*"
  errors=$((errors + 1))
}

assert_contains() {
  local file="$1"
  local text="$2"
  tests=$((tests + 1))
  if [[ ! -f "$file" ]]; then
    fail "missing file: $file"
  elif ! grep -Fq -- "$text" "$file"; then
    fail "$file is missing: $text"
  fi
}

assert_not_contains() {
  local file="$1"
  local text="$2"
  tests=$((tests + 1))
  if [[ ! -f "$file" ]]; then
    fail "missing file: $file"
  elif grep -Fq -- "$text" "$file"; then
    fail "$file still contains forbidden text: $text"
  fi
}

assert_contains_named() {
  local name="$1"
  local file="$2"
  local text="$3"
  tests=$((tests + 1))
  if [[ ! -f "$file" ]]; then
    fail "[$name] document=$file expected contract unavailable because the file is missing"
  elif ! grep -Fq -- "$text" "$file"; then
    fail "[$name] document=$file expected contract missing: $text"
  fi
}

assert_line_named() {
  local name="$1"
  local file="$2"
  local line="$3"
  tests=$((tests + 1))
  if [[ ! -f "$file" ]]; then
    fail "[$name] document=$file expected exact contract line unavailable because the file is missing"
  elif ! grep -Fxq -- "$line" "$file"; then
    fail "[$name] document=$file expected exact contract line missing: $line"
  fi
}

assert_section_contains_named() {
  local name="$1"
  local file="$2"
  local start_heading="$3"
  local end_heading="$4"
  local text="$5"
  local section
  tests=$((tests + 1))

  if [[ ! -f "$file" ]]; then
    fail "[$name] document=$file expected section contract unavailable because the file is missing"
    return
  fi
  if ! grep -Fxq -- "$start_heading" "$file"; then
    fail "[$name] document=$file section start missing: $start_heading"
    return
  fi
  if ! grep -Fxq -- "$end_heading" "$file"; then
    fail "[$name] document=$file section end missing: $end_heading"
    return
  fi

  section="$(
    awk -v start="$start_heading" -v end="$end_heading" '
      $0 == start { in_section = 1 }
      in_section && $0 == end && $0 != start { exit }
      in_section { print }
    ' "$file"
  )"
  if [[ "$section" != *"$text"* ]]; then
    fail "[$name] document=$file section=$start_heading expected contract missing: $text"
  fi
}

assert_files_not_contain_named() {
  local name="$1"
  local text="$2"
  shift 2
  local file
  tests=$((tests + 1))
  for file in "$@"; do
    if [[ ! -f "$file" ]]; then
      fail "[$name] document=$file conflict check unavailable because the file is missing"
      return
    fi
    if grep -Fq -- "$text" "$file"; then
      fail "[$name] document=$file conflicting authorization found: $text"
      return
    fi
  done
}

canonical_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
repo_root="${FE04E_GOVERNANCE_ROOT:-$canonical_root}"
script_path="$canonical_root/scripts/check-fe04e-governance-contract.sh"
semantic_helper_path="$canonical_root/scripts/check_fe04e_governance_semantics.py"
helper_test_path="$canonical_root/scripts/test_check_fe04e_governance_semantics.py"

semantic_rel="docs/design/FE04_SEMANTIC_CONTRACT_V2.md"
interaction_rel="docs/INTERACTION_CONTRACT_V3.md"
freeze_rel="docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md"
state_rel="docs/PROJECT_CURRENT_STATE.md"
matrix_rel="docs/DELIVERY_PROGRESS_MATRIX.md"
active_rel="docs/ACTIVE_MAINLINE_STATUS.yml"
next_task_rel="docs/CODEX_NEXT_TASK.yml"
change_log_rel="docs/CONTRACT_CHANGE_LOG.md"
frontend_audit_rel="docs/FRONTEND_IMPLEMENTATION_CONTRACT_AUDIT_V2.md"
delivery_contract_rel="docs/PROJECT_DELIVERY_CONTRACT.md"
capability_matrix_rel="docs/V1_CAPABILITY_MATRIX.md"

semantic="$repo_root/$semantic_rel"
interaction="$repo_root/$interaction_rel"
freeze="$repo_root/$freeze_rel"
state="$repo_root/$state_rel"
matrix="$repo_root/$matrix_rel"
active="$repo_root/$active_rel"
next_task="$repo_root/$next_task_rel"
change_log="$repo_root/$change_log_rel"
frontend_audit="$repo_root/$frontend_audit_rel"
delivery_contract="$repo_root/$delivery_contract_rel"
capability_matrix="$repo_root/$capability_matrix_rel"

probe_contract_files=(
  "$semantic_rel"
  "$interaction_rel"
  "$freeze_rel"
  "$state_rel"
  "$matrix_rel"
  "$active_rel"
  "$next_task_rel"
  "$change_log_rel"
  "$frontend_audit_rel"
  "$delivery_contract_rel"
  "$capability_matrix_rel"
)

replace_exact_line() {
  local file="$1"
  local from="$2"
  local to="$3"
  local output="$file.tmp"
  if ! awk -v from="$from" -v to="$to" '
    $0 == from {
      print to
      replaced = 1
      next
    }
    { print }
    END {
      if (!replaced) {
        exit 3
      }
    }
  ' "$file" >"$output"; then
    rm -f "$output"
    return 1
  fi
  mv "$output" "$file"
}

run_negative_probe() {
  local name="$1"
  local file_rel="$2"
  local from="$3"
  local to="$4"
  local expected_assertion="$5"
  local probe_root
  local probe_file
  local probe_output
  local rel

  tests=$((tests + 1))
  probe_root="$(mktemp -d "${TMPDIR:-/tmp}/fe04e-governance-probe.XXXXXX")" || {
    error "[$name] could not create a temporary probe root"
    return
  }

  for rel in "${probe_contract_files[@]}"; do
    if ! mkdir -p "$(dirname "$probe_root/$rel")"; then
      error "[$name] could not create temporary path for $rel"
      rm -rf "$probe_root"
      return
    fi
    if ! cp "$repo_root/$rel" "$probe_root/$rel"; then
      error "[$name] could not copy governance source $rel"
      rm -rf "$probe_root"
      return
    fi
  done

  probe_file="$probe_root/$file_rel"
  if ! replace_exact_line "$probe_file" "$from" "$to"; then
    error "[$name] could not install the controlled regression in $file_rel"
    rm -rf "$probe_root"
    return
  fi

  probe_output="$probe_root/probe-output.txt"
  if FE04E_GOVERNANCE_ROOT="$probe_root" FE04E_SKIP_NEGATIVE_PROBES=1 \
    FE04E_SKIP_HELPER_UNIT_TESTS=1 \
    bash "$script_path" >"$probe_output" 2>&1; then
    fail "[$name] controlled regression was accepted; expected assertion=[$expected_assertion]"
  elif ! grep -Fq -- "FAIL: [$expected_assertion]" "$probe_output"; then
    fail "[$name] controlled regression failed for an unexpected reason; expected assertion=[$expected_assertion]"
  else
    echo "PASS: negative probe [$name] rejected by [$expected_assertion]"
  fi

  rm -rf "$probe_root"
}

# Public OPPORTUNITY contract: public inputs only and no private state oracle.
assert_contains "$semantic" 'AUTHENTICATED_SHARED_PUBLIC_PROJECTION'
assert_contains "$semantic" 'Its public state inputs are limited to public'
assert_contains "$semantic" 'MUST NOT read or depend on private/internal `pushId`'
assert_contains "$semantic" 'This is a strict no-private-state-oracle rule.'
assert_contains "$semantic" 'same lifecycle, status, and readiness for all authenticated users'
assert_not_contains "$semantic" 'A separate server-side readiness projection may validate'
assert_not_contains "$semantic" 'required Recheck is complete'

assert_contains "$interaction" 'public `OPPORTUNITY` never exposes or pivots through private/internal'
assert_contains "$interaction" 'never change public'
assert_contains "$interaction" 'private pivot or state oracle.'
assert_contains "$interaction" 'No system-notification, AI-generated-message, delivery, or third message source'
assert_not_contains "$interaction" 'complete legal matching Push/Recheck data with completed execution'
assert_not_contains "$interaction" 'System notices may be shown as a separate informational category'

# Cross-surface OPPORTUNITY contract: each surface is pinned to the same
# public-only source and the shared no-private-state-oracle policy.
assert_section_contains_named \
  "Dashboard OPPORTUNITY shared public projection" \
  "$interaction" \
  "### 3.1 Overview Dashboard" \
  "### 3.2 Evidence & Scoring" \
  $'- any `OPPORTUNITY` preview uses the shared public projection and public state\n  only; it never pivots through PushRecheck or UserPosition risk;'
assert_section_contains_named \
  "Opportunity Log OPPORTUNITY shared public projection" \
  "$interaction" \
  "### 3.10 Mobile Push Detail" \
  "### 3.11 Mobile Profile & Settings" \
  $'This public/private split is shared by Dashboard opportunity previews,\nOpportunity Log, Message Center contracts, and Message Detail contracts. No'
assert_section_contains_named \
  "Message Center OPPORTUNITY shared public projection" \
  "$interaction" \
  "### 3.12 Mobile Message Center" \
  "### 3.13 AI Analysis And Asset Search" \
  '1. authenticated shared public `OPPORTUNITY`;'
assert_section_contains_named \
  "Message Center OPPORTUNITY has no private state pivot" \
  "$interaction" \
  "### 3.12 Mobile Message Center" \
  "### 3.13 AI Analysis And Asset Search" \
  $'Public `OPPORTUNITY` cards may use only public lifecycle and\npublic evaluation state; they never use private `pushId`, PushRecheck,\nUserPosition, or monitor risk.'
assert_section_contains_named \
  "Message Detail OPPORTUNITY public projection" \
  "$interaction" \
  "### 3.10 Mobile Push Detail" \
  "### 3.11 Mobile Profile & Settings" \
  '- authenticated shared server-side `OPPORTUNITY` public projection;'
assert_section_contains_named \
  "all OPPORTUNITY surfaces share one contract" \
  "$interaction" \
  "### 3.10 Mobile Push Detail" \
  "### 3.11 Mobile Profile & Settings" \
  $'This public/private split is shared by Dashboard opportunity previews,\nOpportunity Log, Message Center contracts, and Message Detail contracts. No\nsurface may recreate a private pivot or state oracle.'
assert_section_contains_named \
  "shared OPPORTUNITY no-private-state-oracle" \
  "$semantic" \
  "## 7. Message And Telegram V2" \
  "## 8. Search Asset V2" \
  'This is a strict no-private-state-oracle rule.'
assert_section_contains_named \
  "shared OPPORTUNITY uses public inputs only" \
  "$semantic" \
  "## 7. Message And Telegram V2" \
  "## 8. Search Asset V2" \
  $'Its public state inputs are limited to public `messageId`,\n`sourceIdentity=OPPORTUNITY`, public opportunity identity, public lifecycle,\npublic status, public market evidence, public evaluation completeness, public\ntimestamps, public expiry/staleness, and public source validity.'
assert_section_contains_named \
  "shared OPPORTUNITY excludes private inputs" \
  "$semantic" \
  "## 7. Message And Telegram V2" \
  "## 8. Search Asset V2" \
  $'state MUST NOT read or depend on private/internal `pushId`, a Push\nentity, a PushRecheck row, Recheck existence/completeness/validity, private\n`execution_status`, `failReasonJson`, account risk, position risk,\nUserPosition, current-user position direction, or private monitor state.'

# Private POSITION_RISK contract: one resolver and the authoritative latest monitor.
assert_contains "$freeze" 'list and detail use one shared resolver'
assert_contains "$freeze" 'authoritative latest monitor through the exact owner/message/position'
assert_contains "$freeze" 'selected historical monitor cannot substitute'
assert_contains "$freeze" 'Monitor-row existence alone must never imply `READY`.'
assert_contains "$freeze" 'Account-risk level and monitor composite-risk level are distinct dimensions.'
assert_contains "$freeze" 'inequality between the two levels is valid and is not itself `ERROR`'
assert_contains "$freeze" 'FE04E_POSITION_RISK_STATE_RESOLVER: LIST_DETAIL_SHARED_AUTHORITATIVE_LATEST_MONITOR'
assert_contains "$freeze" 'FE04E_POSITION_RISK_ROW_EXISTENCE_RULE: ROW_EXISTENCE_NEVER_IMPLIES_READY'
assert_contains "$freeze" 'FE04E_RISK_LEVEL_DIMENSIONS: ACCOUNT_AND_MONITOR_COMPOSITE_INDEPENDENT_MISMATCH_NOT_ERROR'

# Exact merged-main evidence and runtime/governance separation.
assert_contains "$state" 'FE-04E Source Implementation PR: #1155 / MERGED'
assert_contains "$state" 'FE-04E Source Authorized Head: 269ec97c11efa30fe58d99a4d78d09387e6fd277'
assert_contains "$state" 'FE-04E Source Merge Commit: 2552dd24b1b756d5eb517e640baa772e1c5bcab6'
assert_contains "$state" 'FE-04E Source Merged At: 2026-07-31T03:55:03Z'
assert_contains "$state" 'FE-04E Source Merge Method: SQUASH'
assert_contains "$state" 'FE-04E Runtime Status: EFFECTIVE_MERGED_MAIN'
assert_contains "$state" 'FE-04E Governance Alignment: PR #1156 / PENDING_MERGED_MAIN'

assert_contains "$matrix" '| FE-04 | Position Monitoring Frontend | IN_PROGRESS | PARTIAL |'
assert_contains "$matrix" 'PR #1155 authorized Head `269ec97c11efa30fe58d99a4d78d09387e6fd277` was squash-merged at `2026-07-31T03:55:03Z`'
assert_contains "$matrix" 'FE-04E Message/Push UI and FE-04F Profile remain unimplemented'
assert_contains "$matrix" 'mismatch alone is not ERROR'

assert_contains "$active" 'fe_04e_source_pr_status: "MERGED"'
assert_contains "$active" 'fe_04e_source_merge_method: "SQUASH"'
assert_contains "$active" 'fe_04e_governance_alignment_status: "PENDING_MERGED_MAIN"'
assert_contains "$active" 'fe_04e_position_risk_state_resolver: "LIST_DETAIL_SHARED_AUTHORITATIVE_LATEST_MONITOR"'
assert_contains "$active" 'fe_04e_risk_level_dimensions: "ACCOUNT_AND_MONITOR_COMPOSITE_INDEPENDENT_MISMATCH_NOT_ERROR"'

assert_line_named \
  "next task remains read-only readiness re-evaluation" \
  "$next_task" \
  'module: "FE-04E Message/Push UI Readiness and Governance Re-evaluation"'
assert_contains "$next_task" 'fe_04e_ui_status: "NOT_STARTED_PENDING_READINESS_AND_GOVERNANCE_REEVALUATION"'
assert_contains "$next_task" 'fe_04e_source_authorized_head: "269ec97c11efa30fe58d99a4d78d09387e6fd277"'
assert_contains "$next_task" 'fe_04e_source_merged_at: "2026-07-31T03:55:03Z"'
assert_contains "$next_task" 'fe_04e_authorization: "READ_ONLY_READINESS_REEVALUATION_ONLY"'

assert_contains "$change_log" '## v1.0-fe04e-privacy-state-foundation-effective-merged-main'
assert_not_contains "$change_log" '## v1.0-fe04e-opportunity-public-projection-candidate'
assert_contains "$change_log" '`269ec97c11efa30fe58d99a4d78d09387e6fd277` was squash-merged at'
assert_contains "$change_log" '`2026-07-31T03:55:03Z` as current main'
assert_contains "$change_log" 'Message/Push UI remains'
assert_contains "$change_log" '`NOT_IMPLEMENTED`'

assert_contains "$frontend_audit" 'public `OPPORTUNITY` readiness uses public opportunity inputs only'
assert_contains "$frontend_audit" 'Private `POSITION_RISK` states use the same authoritative'

# Notification and trading prohibitions are checked as semantic combinations,
# not as bare keyword absence, so legitimate "blocked" wording is accepted.
assert_contains_named \
  "Telegram semantic status remains pending implementation" \
  "$semantic" \
  'Telegram is an `EXTENSION / PENDING_IMPLEMENTATION` notification outlet.'
assert_section_contains_named \
  "Telegram Message Center status remains unavailable" \
  "$interaction" \
  "### 3.12 Mobile Message Center" \
  "### 3.13 AI Analysis And Asset Search" \
  $'Telegram is `EXTENSION / PENDING_IMPLEMENTATION`. It is a future delivery\noutlet only and must not be presented as connected, delivered, or actionable.'
assert_line_named \
  "Telegram active status remains not connected" \
  "$active" \
  'fe_04e_telegram_boundary_status: "PASS_EXTENSION_NOT_CONNECTED"'
assert_contains_named \
  "Telegram remains blocked in next task" \
  "$next_task" \
  'system notifications, Telegram, external send, automatic notification'

assert_section_contains_named \
  "system notification is not a Message Center source" \
  "$interaction" \
  "### 3.12 Mobile Message Center" \
  "### 3.13 AI Analysis And Asset Search" \
  $'No system-notification, AI-generated-message, delivery, or third message source\nis authorized.'
assert_contains_named \
  "system notification remains unauthorized in semantic contract" \
  "$semantic" \
  'system notifications are not authorized'
assert_contains_named \
  "system notification remains blocked in active state" \
  "$active" \
  'Message/Push UI implementation, system notifications, Telegram'
assert_contains_named \
  "system notification remains blocked in next task" \
  "$next_task" \
  'Message/Push UI implementation, system notifications, Telegram'

assert_contains_named \
  "external notification remains blocked in current state" \
  "$state" \
  'System notifications, Telegram, external send, automatic notification,'
assert_contains_named \
  "external notification remains blocked in active state" \
  "$active" \
  'Telegram, external send, automatic notification'
assert_contains_named \
  "external notification remains blocked in next task" \
  "$next_task" \
  'Telegram, external send, automatic notification'

assert_contains_named \
  "automatic notification remains blocked in current state" \
  "$state" \
  'System notifications, Telegram, external send, automatic notification,'
assert_contains_named \
  "automatic notification remains blocked in active state" \
  "$active" \
  'external send, automatic notification, fabricated unread/message counts'
assert_contains_named \
  "automatic notification remains blocked in next task" \
  "$next_task" \
  'external send, automatic notification, fabricated counts or data'
assert_section_contains_named \
  "Message Push UI has no delivery control" \
  "$interaction" \
  "### 3.12 Mobile Message Center" \
  "### 3.13 AI Analysis And Asset Search" \
  $'Provide one read-only entry for high-value product events without becoming a\ntrading or delivery-control surface.'

assert_section_contains_named \
  "PushRecheck is never trading authorization in delivery contract" \
  "$delivery_contract" \
  "## 4. Permanent Safety Rules / 永久安全规则" \
  "## 5. Development Order Gate / 开发顺序总门禁" \
  '8. Treat PushRecheck as trading authorization.'
assert_contains_named \
  "PushRecheck never authorizes a trade in FE04 semantics" \
  "$semantic" \
  'PushRecheck never authorizes a trade.'
assert_contains_named \
  "PushRecheck is not trading authorization in interaction contract" \
  "$interaction" \
  'Push recheck is not trading authorization.'
assert_line_named \
  "FE04E active capability remains no-send no-trading" \
  "$active" \
  'fe_04e_capability_boundary_status: "PASS_NO_SEND_NO_TRADING"'
assert_line_named \
  "trading capability movement remains none" \
  "$next_task" \
  '  - "No schema, Figma, Telegram, external-send, automatic-notification, AI, or trading capability movement occurs"'
assert_section_contains_named \
  "current state records no notification or trading capability" \
  "$state" \
  "### FE-04E Privacy/State Foundation And UI Readiness Boundary" \
  "## P3-U2 iPhone Private Test App Foundation" \
  $'This merged privacy-boundary package changes the read API response projection\nbut does not add a new endpoint, mutation, schema, Message/Push UI,\nnotification send, or trading capability.'
assert_contains_named \
  "delivery matrix keeps FE04E trading blocked" \
  "$matrix" \
  'external/automatic notification, fabricated data, mutation, AI expansion, and trading remain blocked.'
assert_contains_named \
  "capability matrix keeps external channels not started" \
  "$capability_matrix" \
  '| External Channel | 0 NOT_STARTED | Not started and requires separate C-level authorization. | No Telegram/email/webhook/app/local notification send is authorized.'
assert_contains_named \
  "capability matrix keeps order execution auto-trading not started" \
  "$capability_matrix" \
  '| order / execution / auto-trading | 0 NOT_STARTED | Explicitly out of V1 runtime scope and blocked. | No order API, execution API, or auto-trading should be built.'
assert_section_contains_named \
  "governance PR remains metadata-only" \
  "$state" \
  "### FE-04E Privacy/State Foundation And UI Readiness Boundary" \
  "## P3-U2 iPhone Private Test App Foundation" \
  $'server-side read projections effective. PR #1156 aligns governance metadata\nonly and remains `PENDING_MERGED_MAIN` until separately reviewed and merged:'

governance_boundary_files=(
  "$semantic"
  "$interaction"
  "$state"
  "$matrix"
  "$active"
  "$next_task"
  "$capability_matrix"
)
assert_files_not_contain_named "Telegram enabled flag is forbidden" "TELEGRAM_ENABLED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "Telegram authorization flag is forbidden" "TELEGRAM_AUTHORIZED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "system notification enabled flag is forbidden" "SYSTEM_NOTIFICATION_ENABLED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "system notification authorization flag is forbidden" "SYSTEM_NOTIFICATION_AUTHORIZED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "external notification enabled flag is forbidden" "EXTERNAL_NOTIFICATION_ENABLED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "external notification authorization flag is forbidden" "EXTERNAL_NOTIFICATION_AUTHORIZED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "automatic notification enabled flag is forbidden" "AUTOMATIC_NOTIFICATION_ENABLED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "automatic notification authorization flag is forbidden" "AUTOMATIC_NOTIFICATION_AUTHORIZED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "trading capability enabled flag is forbidden" "TRADING_CAPABILITY_MOVEMENT: ENABLED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "auto trade authorization flag is forbidden" "AUTO_TRADE_AUTHORIZED" "${governance_boundary_files[@]}"
assert_files_not_contain_named "PushRecheck trade authorization flag is forbidden" "PUSH_RECHECK_IS_TRADE_AUTHORIZATION" "${governance_boundary_files[@]}"

shell_static_assertions="$tests"

if [[ "${FE04E_SKIP_NEGATIVE_PROBES:-0}" != "1" ]]; then
  run_negative_probe \
    "Message Detail private Recheck regression" \
    "$interaction_rel" \
    '- authenticated shared server-side `OPPORTUNITY` public projection;' \
    '- authenticated private PushRecheck projection;' \
    "Message Detail OPPORTUNITY public projection"
  run_negative_probe \
    "shared surface contract deletion regression" \
    "$interaction_rel" \
    'This public/private split is shared by Dashboard opportunity previews,' \
    'This public/private split is no longer shared across surfaces.' \
    "Opportunity Log OPPORTUNITY shared public projection"
  run_negative_probe \
    "Dashboard private Recheck regression" \
    "$interaction_rel" \
    '- any `OPPORTUNITY` preview uses the shared public projection and public state' \
    '- any `OPPORTUNITY` preview uses private PushRecheck state' \
    "Dashboard OPPORTUNITY shared public projection"
  run_negative_probe \
    "Opportunity Log user-scoped projection regression" \
    "$interaction_rel" \
    'Opportunity Log, Message Center contracts, and Message Detail contracts. No' \
    'Opportunity Log uses a user-scoped projection instead of the shared contract.' \
    "Opportunity Log OPPORTUNITY shared public projection"
  run_negative_probe \
    "Message Center divergent source regression" \
    "$interaction_rel" \
    '1. authenticated shared public `OPPORTUNITY`;' \
    '1. owner-scoped private `OPPORTUNITY` with a separate state source;' \
    "Message Center OPPORTUNITY shared public projection"
  run_negative_probe \
    "Telegram authorization regression" \
    "$active_rel" \
    'fe_04e_telegram_boundary_status: "PASS_EXTENSION_NOT_CONNECTED"' \
    'fe_04e_telegram_boundary_status: "TELEGRAM_AUTHORIZED"' \
    "Telegram active status remains not connected"
  run_negative_probe \
    "system notification authorization regression" \
    "$interaction_rel" \
    'No system-notification, AI-generated-message, delivery, or third message source' \
    'SYSTEM_NOTIFICATION_AUTHORIZED: system notification is a Message Center source' \
    "system notification is not a Message Center source"
  run_negative_probe \
    "external notification enablement regression" \
    "$active_rel" \
    'compatibility_next_business_phase_allowed_note: "This derived file authorizes only the read-only readiness/governance gate. Message/Push UI implementation, system notifications, Telegram, external send, automatic notification, fabricated unread/message counts or data, FE-04F, P4, trading, and production deployment remain blocked."' \
    'compatibility_next_business_phase_allowed_note: "EXTERNAL_NOTIFICATION_ENABLED"' \
    "external notification enabled flag is forbidden"
  run_negative_probe \
    "automatic notification authorization regression" \
    "$next_task_rel" \
    'compatibility_next_business_phase_allowed_note: "Only the read-only FE-04E Message/Push UI readiness and governance re-evaluation is authorized. Message/Push UI implementation, system notifications, Telegram, external send, automatic notification, fabricated counts or data, FE-04F, P4, trading, and production deployment remain blocked."' \
    'compatibility_next_business_phase_allowed_note: "AUTOMATIC_NOTIFICATION_AUTHORIZED"' \
    "automatic notification remains blocked in next task"
  run_negative_probe \
    "trading capability movement regression" \
    "$next_task_rel" \
    '  - "No schema, Figma, Telegram, external-send, automatic-notification, AI, or trading capability movement occurs"' \
    '  - "TRADING_CAPABILITY_MOVEMENT: ENABLED"' \
    "trading capability movement remains none"
  run_negative_probe \
    "next task Telegram implementation regression" \
    "$next_task_rel" \
    'module: "FE-04E Message/Push UI Readiness and Governance Re-evaluation"' \
    'module: "FE-04E Telegram Implementation"' \
    "next task remains read-only readiness re-evaluation"
  run_negative_probe \
    "PushRecheck trade authorization regression" \
    "$semantic_rel" \
    'fallback is permitted. PushRecheck never authorizes a trade.' \
    'fallback is permitted. PUSH_RECHECK_IS_TRADE_AUTHORIZATION' \
    "PushRecheck never authorizes a trade in FE04 semantics"
fi

shell_negative_probes=$((tests - shell_static_assertions))

helper_test_output=""
helper_test_exit=0
helper_unit_tests=0
helper_unit_failures=0
helper_unit_errors=0
helper_unit_skipped=0
if [[ "${FE04E_SKIP_HELPER_UNIT_TESTS:-0}" != "1" ]]; then
  if helper_test_output="$(python3 "$helper_test_path" 2>&1)"; then
    helper_test_exit=0
  else
    helper_test_exit=$?
  fi
  printf '%s\n' "$helper_test_output"

  helper_test_count() {
    local key="$1"
    printf '%s\n' "$helper_test_output" | awk -F': ' -v key="$key" '
      $1 == key {
        print $2
        found = 1
        exit
      }
    '
  }

  helper_unit_tests="$(helper_test_count "FE04E_HELPER_UNIT_TESTS")"
  helper_unit_failures="$(helper_test_count "FE04E_HELPER_UNIT_TEST_FAILURES")"
  helper_unit_errors="$(helper_test_count "FE04E_HELPER_UNIT_TEST_ERRORS")"
  helper_unit_skipped="$(helper_test_count "FE04E_HELPER_UNIT_TEST_SKIPPED")"
  helper_counts=(
    "$helper_unit_tests"
    "$helper_unit_failures"
    "$helper_unit_errors"
    "$helper_unit_skipped"
  )
  for helper_value in "${helper_counts[@]}"; do
    if [[ ! "$helper_value" =~ ^[0-9]+$ ]]; then
      error "helper unit-test runner returned an invalid or missing count"
      helper_test_exit=2
      helper_unit_tests=0
      helper_unit_failures=0
      helper_unit_errors=0
      helper_unit_skipped=0
      break
    fi
  done
  if [[ "$helper_test_exit" -ne 0 && "$helper_unit_failures" -eq 0 && "$helper_unit_errors" -eq 0 ]]; then
    error "helper unit-test runner exited unexpectedly with status $helper_test_exit"
  fi
fi

semantic_args=(
  --root "$repo_root"
  --static-assertions "$shell_static_assertions"
  --legacy-negative-probes "$shell_negative_probes"
  --helper-test-path "$helper_test_path"
)
if [[ "${FE04E_SKIP_NEGATIVE_PROBES:-0}" == "1" ]]; then
  semantic_args+=(--skip-probes)
fi
if [[ "${FE04E_SKIP_HELPER_UNIT_TESTS:-0}" == "1" ]]; then
  semantic_args+=(--skip-helper-unit-tests)
fi

semantic_output=""
if semantic_output="$(
  python3 "$semantic_helper_path" "${semantic_args[@]}" 2>&1
)"; then
  semantic_exit=0
else
  semantic_exit=$?
fi
printf '%s\n' "$semantic_output"

semantic_count() {
  local key="$1"
  printf '%s\n' "$semantic_output" | awk -F': ' -v key="$key" '
    $1 == key {
      print $2
      found = 1
      exit
    }
  '
}

semantic_guards="$(semantic_count "FE04E_SEMANTIC_GUARDS")"
semantic_contradiction_guards="$(semantic_count "FE04E_CONTRADICTION_GUARDS")"
semantic_authorization_guards="$(semantic_count "FE04E_AUTHORIZATION_SEMANTIC_GUARDS")"
semantic_cross_file_guards="$(semantic_count "FE04E_CROSS_FILE_GUARDS")"
semantic_adversarial_probes="$(semantic_count "FE04E_ADVERSARIAL_PROBES")"
semantic_legal_controls="$(semantic_count "FE04E_LEGAL_CONTROL_PROBES")"
raw_static_assertions="$(semantic_count "FE04E_RAW_STATIC_ASSERTIONS")"
qualifying_static_assertions="$(semantic_count "FE04E_QUALIFYING_STATIC_ASSERTIONS")"
raw_semantic_guards="$(semantic_count "FE04E_RAW_SEMANTIC_GUARDS")"
qualifying_semantic_guards="$(semantic_count "FE04E_QUALIFYING_SEMANTIC_GUARDS")"
raw_helper_unit_tests="$(semantic_count "FE04E_RAW_HELPER_UNIT_TESTS")"
qualifying_helper_unit_tests="$(semantic_count "FE04E_QUALIFYING_HELPER_UNIT_TESTS")"
raw_negative_probes="$(semantic_count "FE04E_RAW_NEGATIVE_PROBES")"
qualifying_negative_probes="$(semantic_count "FE04E_QUALIFYING_NEGATIVE_PROBES")"
raw_legal_controls="$(semantic_count "FE04E_RAW_LEGAL_CONTROLS")"
qualifying_legal_controls="$(semantic_count "FE04E_QUALIFYING_LEGAL_CONTROLS")"
duplicate_execution_count="$(semantic_count "FE04E_DUPLICATE_EXECUTION_COUNT")"
raw_execution_total="$(semantic_count "FE04E_RAW_EXECUTION_TOTAL")"
qualifying_unique_total="$(semantic_count "FE04E_QUALIFYING_UNIQUE_TOTAL")"
qualifying_inventory_sha="$(semantic_count "FE04E_QUALIFYING_INVENTORY_SHA256")"
semantic_failures="$(semantic_count "FE04E_SEMANTIC_FAILURES")"
semantic_errors="$(semantic_count "FE04E_SEMANTIC_ERRORS")"

semantic_counts=(
  "$semantic_guards"
  "$semantic_contradiction_guards"
  "$semantic_authorization_guards"
  "$semantic_cross_file_guards"
  "$semantic_adversarial_probes"
  "$semantic_legal_controls"
  "$raw_static_assertions"
  "$qualifying_static_assertions"
  "$raw_semantic_guards"
  "$qualifying_semantic_guards"
  "$raw_helper_unit_tests"
  "$qualifying_helper_unit_tests"
  "$raw_negative_probes"
  "$qualifying_negative_probes"
  "$raw_legal_controls"
  "$qualifying_legal_controls"
  "$duplicate_execution_count"
  "$raw_execution_total"
  "$qualifying_unique_total"
  "$semantic_failures"
  "$semantic_errors"
)
for semantic_value in "${semantic_counts[@]}"; do
  if [[ ! "$semantic_value" =~ ^[0-9]+$ ]]; then
    error "semantic helper returned an invalid or missing count"
    semantic_exit=2
    semantic_guards=0
    semantic_contradiction_guards=0
    semantic_authorization_guards=0
    semantic_cross_file_guards=0
    semantic_adversarial_probes=0
    semantic_legal_controls=0
    semantic_failures=0
    semantic_errors=0
    break
  fi
done

if [[ "$semantic_exit" -ne 0 && "$semantic_failures" -eq 0 && "$semantic_errors" -eq 0 ]]; then
  error "semantic helper exited unexpectedly with status $semantic_exit"
fi

if [[ "$raw_static_assertions" -ne "$shell_static_assertions" ]]; then
  error "coverage inventory static count does not match runner execution count"
fi
if [[ "$raw_semantic_guards" -ne "$semantic_guards" ]]; then
  error "coverage inventory semantic count does not match helper execution count"
fi
if [[ "$raw_helper_unit_tests" -ne "$helper_unit_tests" ]]; then
  error "coverage inventory helper count does not match unittest execution count"
fi
if [[ "$raw_negative_probes" -ne $((shell_negative_probes + semantic_adversarial_probes)) ]]; then
  error "coverage inventory negative-probe count does not match executed probes"
fi
if [[ "$raw_legal_controls" -ne "$semantic_legal_controls" ]]; then
  error "coverage inventory legal-control count does not match executed controls"
fi
if [[ ! "$qualifying_inventory_sha" =~ ^[0-9a-f]{64}$ ]]; then
  error "coverage inventory digest is missing or invalid"
fi

tests="$raw_execution_total"
failures=$((failures + semantic_failures + helper_unit_failures))
errors=$((errors + semantic_errors + helper_unit_errors))
skipped=$((skipped + helper_unit_skipped))

echo "FE04E_BASE_STATIC_ASSERTIONS: $shell_static_assertions"
echo "FE04E_STATIC_ASSERTIONS: $shell_static_assertions"
echo "FE04E_SEMANTIC_GUARDS: $semantic_guards"
echo "FE04E_CONTRADICTION_GUARDS: $semantic_contradiction_guards"
echo "FE04E_AUTHORIZATION_SEMANTIC_GUARDS: $semantic_authorization_guards"
echo "FE04E_CROSS_FILE_GUARDS: $semantic_cross_file_guards"
echo "FE04E_LEGACY_NEGATIVE_PROBES: $shell_negative_probes"
echo "FE04E_ADVERSARIAL_PROBES: $semantic_adversarial_probes"
echo "FE04E_NEGATIVE_PROBES: $((shell_negative_probes + semantic_adversarial_probes))"
echo "FE04E_LEGAL_CONTROL_PROBES: $semantic_legal_controls"
echo "FE04E_HELPER_UNIT_TESTS: $helper_unit_tests"
echo "FE04E_OLD_RAW_GOVERNANCE_TESTS: 280"
echo "FE04E_OLD_QUALIFYING_TOTAL: NOT_ESTABLISHED"
echo "FE04E_RAW_HELPER_UNIT_TESTS: $raw_helper_unit_tests"
echo "FE04E_QUALIFYING_HELPER_UNIT_TESTS: $qualifying_helper_unit_tests"
echo "FE04E_RAW_NEGATIVE_PROBES: $raw_negative_probes"
echo "FE04E_QUALIFYING_NEGATIVE_PROBES: $qualifying_negative_probes"
echo "FE04E_RAW_LEGAL_CONTROLS: $raw_legal_controls"
echo "FE04E_QUALIFYING_LEGAL_CONTROLS: $qualifying_legal_controls"
echo "FE04E_DUPLICATE_EXECUTION_COUNT: $duplicate_execution_count"
echo "FE04E_RAW_EXECUTION_TOTAL: $raw_execution_total"
echo "FE04E_QUALIFYING_GOVERNANCE_TESTS: $qualifying_unique_total"
echo "FE04E_QUALIFYING_UNIQUE_TOTAL: $qualifying_unique_total"
echo "FE04E_QUALIFYING_INVENTORY_SHA256: $qualifying_inventory_sha"
echo "FE04E_GOVERNANCE_TESTS: $tests"
echo "FE04E_RAW_COUNTING_FORMULA: $raw_static_assertions + $raw_semantic_guards + $raw_helper_unit_tests + $raw_negative_probes + $raw_legal_controls = $raw_execution_total"
echo "FE04E_QUALIFYING_COUNTING_FORMULA: $qualifying_static_assertions + $qualifying_semantic_guards + $qualifying_helper_unit_tests + $qualifying_negative_probes + $qualifying_legal_controls = $qualifying_unique_total"
echo "FAILURES: $failures"
echo "ERRORS: $errors"
echo "SKIPPED: $skipped"

if [[ "$failures" -eq 0 && "$errors" -eq 0 ]]; then
  echo "FE04E_GOVERNANCE_CONTRACT_OK"
  exit 0
fi

echo "FE04E_GOVERNANCE_CONTRACT_FAILED"
exit 1
