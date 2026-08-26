#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

source_of_truth="docs/PRODUCT_SOURCE_OF_TRUTH.md"
task_file="${PRODUCT_TASK_FILE:-docs/CODEX_NEXT_TASK.yml}"
simulation=""

usage() {
  printf '%s\n' "usage: bash scripts/product-source-gate.sh [--task-file <path>] [--simulate home-card|position-monitor|three-ai]"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-file)
      [[ $# -ge 2 ]] || { usage; exit 2; }
      task_file="$2"
      shift 2
      ;;
    --simulate)
      [[ $# -ge 2 ]] || { usage; exit 2; }
      simulation="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage
      exit 2
      ;;
  esac
done

declare -a blockers=()
declare -a source_lines=()
declare -a registry_ids=()
declare -a registry_paths=()
declare -a registry_hashes=()
declare -a registry_modules=()
declare -a required_sources=()
declare -a simulation_mapping=()

block() {
  blockers+=("$1")
}

hash_file() {
  local path="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$path" | awk '{print $1}'
  else
    shasum -a 256 "$path" | awk '{print $1}'
  fi
}

yaml_scalar() {
  local file="$1"
  local key="$2"
  awk -v key="$key" '
    $0 ~ "^" key ":[[:space:]]*" {
      value=$0
      sub("^[^:]*:[[:space:]]*", "", value)
      gsub(/^\"|\"$/, "", value)
      print value
      exit
    }
  ' "$file"
}

yaml_nested_scalar() {
  local file="$1"
  local section="$2"
  local key="$3"
  awk -v section="$section" -v key="$key" '
    $0 == section ":" {inside=1; next}
    inside && /^[^[:space:]]/ {exit}
    inside && $0 ~ "^[[:space:]]+" key ":[[:space:]]*" {
      value=$0
      sub("^[[:space:]]+[^:]*:[[:space:]]*", "", value)
      gsub(/^\"|\"$/, "", value)
      print value
      exit
    }
  ' "$file"
}

yaml_section_items() {
  local file="$1"
  local section="$2"
  awk -v section="$section" '
    $0 == section ":" {inside=1; next}
    inside && /^[^[:space:]]/ {exit}
    inside && /^[[:space:]]+-[[:space:]]+/ {
      value=$0
      sub("^[[:space:]]+-[[:space:]]+", "", value)
      gsub(/^\"|\"$/, "", value)
      print value
    }
  ' "$file"
}

yaml_section_has_content() {
  local file="$1"
  local section="$2"
  awk -v section="$section" '
    $0 ~ "^" section ":[[:space:]]*[^|>[:space:]].*" {found=1; exit}
    $0 == section ":" || $0 ~ "^" section ":[[:space:]]*[|>][[:space:]]*$" {inside=1; next}
    inside && /^[^[:space:]]/ {exit}
    inside && /^[[:space:]]+[^[:space:]#]/ {found=1; exit}
    END {exit(found ? 0 : 1)}
  ' "$file"
}

registry_has_id() {
  local wanted="$1"
  local id
  for id in "${registry_ids[@]}"; do
    [[ "$id" == "$wanted" ]] && return 0
  done
  return 1
}

if [[ ! -s "$source_of_truth" ]]; then
  block "Product Source of Truth is missing or empty: $source_of_truth"
else
  while IFS='|' read -r marker source_id source_path expected_hash applicable_module; do
    [[ "$marker" == "PRODUCT_SOURCE" ]] || continue
    registry_ids+=("$source_id")
    registry_paths+=("$source_path")
    registry_hashes+=("$expected_hash")
    registry_modules+=("$applicable_module")
  done < <(grep '^<!-- PRODUCT_SOURCE|' "$source_of_truth" | sed -e 's/^<!-- //' -e 's/ -->$//')
fi

for required_registry_id in \
  PS-V1-ARCHITECTURE \
  PS-POSITION-MONITORING \
  PS-AI-CONFLICT-RECHECK-REVIEW \
  PS-HOME-INTERACTION \
  PS-P2-POSITION-MONITORING-AUTHORIZATION \
  PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN \
  PS-FUNDAMENTAL-AI-V4-1-FINAL-INTERACTION-AUTHORIZATION \
  PS-FUNDAMENTAL-AI-V4-1-TARGET-RUNTIME-REMEDIATION-AUTHORIZATION \
  PS-FUNDAMENTAL-AI-V4-1-TELEGRAM-AUTHORIZATION \
  PS-TRINE-LOGIC-TELEGRAM-TWO-CATEGORY-REMEDIATION-AUTHORIZATION \
  PS-FUNDAMENTAL-AI-FRONTEND-INTERACTION-RUNTIME-CLOSURE-AUTHORIZATION \
  PS-TRINE-LOGIC-MULTI-USER-ACCOUNT-REGISTRATION-AUTHORIZATION; do
  registry_has_id "$required_registry_id" || block "Required formal product source is not registered: $required_registry_id"
done

v4_1_canonical_count=0
v4_1_legacy_authorization_count=0
for source_id in "${registry_ids[@]}"; do
  [[ "$source_id" == "PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN" ]] && ((v4_1_canonical_count+=1))
  [[ "$source_id" == "PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN-AUTHORIZATION" ]] && ((v4_1_legacy_authorization_count+=1))
done
[[ "$v4_1_canonical_count" == "1" ]] \
  || block "Exactly one canonical v4.1 Product Source must be active"
[[ "$v4_1_legacy_authorization_count" == "0" ]] \
  || block "Superseded v4.1 Decision Chain authorization must not remain in the active registry"

for index in "${!registry_ids[@]}"; do
  source_id="${registry_ids[$index]}"
  source_path="${registry_paths[$index]}"
  expected_hash="${registry_hashes[$index]}"
  applicable_module="${registry_modules[$index]}"
  exists="NO"
  non_empty="NO"
  actual_hash="UNAVAILABLE"
  if [[ -f "$source_path" ]]; then
    exists="YES"
    if [[ -s "$source_path" ]]; then
      non_empty="YES"
      actual_hash="$(hash_file "$source_path")"
    fi
  fi
  source_lines+=("$source_id|$source_path|$exists|$non_empty|$actual_hash|$applicable_module")
  [[ "$exists" == "YES" ]] || block "Registered source path does not exist: $source_id -> $source_path"
  [[ "$non_empty" == "YES" ]] || block "Registered source is empty: $source_id -> $source_path"
  [[ "$actual_hash" == "$expected_hash" ]] || block "Registered source hash mismatch: $source_id"
done

[[ -f AGENTS.md ]] && grep -Fq 'scripts/product-source-gate.sh' AGENTS.md \
  || block "AGENTS.md does not require scripts/product-source-gate.sh"
[[ -f docs/SESSION_BOOTSTRAP.md ]] && grep -Fq 'Product Source Gate' docs/SESSION_BOOTSTRAP.md \
  || block "SESSION_BOOTSTRAP.md does not place Product Source Gate in startup"

task_product_scope=""
task_mapping_status="PASS"
conflict_status="NONE"

if [[ -n "$simulation" ]]; then
  case "$simulation" in
    home-card)
      task_product_scope="P1 Home / Focus Asset Card"
      required_sources=(PS-V1-ARCHITECTURE PS-HOME-INTERACTION PS-FIGMA-BASELINE PS-FORMAL-BUSINESS-CONTRACT)
      simulation_mapping=(
        "fields: market bias, confidence, AssetState, risk, worth-opening, ExecutionPlan, data quality, multi-timeframe, AI consistency"
        "click: card body switches selected asset context and does not navigate to detail"
        "linkage: ExecutionPlan and GPT/Gemini/Grok summaries follow the selected authoritative analysis context"
        "data: every visible field requires source, timestamp, freshness, cache, and empty/error behavior"
        "fail-closed: no fabricated percentages, plan, scores, evidence, or AI output"
        "boundary: asset selection cannot alter position or message semantics"
      )
      ;;
    position-monitor)
      task_product_scope="P2 Position / Position Monitoring Risk Prompt"
      required_sources=(PS-V1-ARCHITECTURE PS-POSITION-MONITORING PS-AI-CONFLICT-RECHECK-REVIEW PS-HOME-INTERACTION PS-P2-POSITION-MONITORING-AUTHORIZATION PS-FORMAL-BUSINESS-CONTRACT)
      simulation_mapping=(
        "identity: exact owner-scoped UserPosition and original ExecutionPlan remain separate"
        "monitor: original logic, reversal, liquidity, wick filtering, risk, alert, and manual adjustment suggestion"
        "state: market opportunity AssetState cannot be used as UserPosition or PositionMonitor state"
        "fail-closed: a short wick alone cannot become strong reversal"
        "boundary: PositionMonitor cannot automatically close, reduce, add, or reverse"
        "boundary: ExecutionPlan cannot substitute for the user's actual position facts"
      )
      ;;
    three-ai)
      task_product_scope="P3 AI Analysis / Three AI"
      required_sources=(PS-V1-ARCHITECTURE PS-AI-CONFLICT-RECHECK-REVIEW PS-HOME-INTERACTION PS-FORMAL-BUSINESS-CONTRACT)
      simulation_mapping=(
        "input: real evidence, data-quality gate, eight scores, and multi-timeframe convergence"
        "authority: rule layer produces the base conclusion before AI"
        "roles: GPT Final, Gemini Review, and Grok Challenge are fixed roles, not parallel voters"
        "trigger: AI runs only at defined checkpoints and need not run every cycle"
        "fallback: AI failure preserves the rule chain without fabricated output"
        "state: formal conflict and Confused cannot override the product state machine"
        "boundary: AI never creates trading authorization"
      )
      ;;
    *)
      block "Unknown simulation: $simulation"
      task_mapping_status="BLOCKED"
      conflict_status="BLOCKED"
      ;;
  esac
else
  if [[ ! -s "$task_file" ]]; then
    block "Task declaration is missing or empty: $task_file"
  else
    gate_required="$(yaml_nested_scalar "$task_file" product_source_gate required)"
    [[ "$gate_required" == "true" ]] || block "product_source_gate.required must be true"
    product_authority="$(yaml_scalar "$task_file" product_authority)"
    [[ "$product_authority" == "PRODUCT_SOURCE_OF_TRUTH" ]] \
      || block "product_authority must be PRODUCT_SOURCE_OF_TRUTH"
    task_product_scope="$(yaml_scalar "$task_file" product_module)"
    [[ -n "$task_product_scope" ]] || block "product_module is missing"
    while IFS= read -r required_source; do
      required_sources+=("$required_source")
    done < <(yaml_section_items "$task_file" required_product_sources)
    [[ "${#required_sources[@]}" -gt 0 ]] || block "required_product_sources is empty"

    for required_key in \
      product_contract_mapping \
      design_interaction_mapping \
      data_source_mapping \
      current_implementation_gap \
      allowed_scope \
      blocked_scope \
      real_scenario_requirement \
      stop_conditions; do
      yaml_section_has_content "$task_file" "$required_key" \
        || block "Task mapping is missing or empty: $required_key"
    done

    for hard_boundary in \
      auto_trading \
      automatic_position_mutation \
      push_recheck_trading_authorization \
      fake_data_as_real \
      owner_scope_bypass; do
      hard_value="$(yaml_nested_scalar "$task_file" hard_boundaries "$hard_boundary")"
      [[ "$hard_value" == "BLOCKED" ]] || block "hard_boundaries.$hard_boundary must be BLOCKED"
    done
    for hard_boundary in preview_business_persistence figma_change mobile_implementation; do
      hard_value="$(yaml_nested_scalar "$task_file" hard_boundaries "$hard_boundary")"
      [[ "$hard_value" == "BLOCKED" ]] || block "hard_boundaries.$hard_boundary must be BLOCKED"
    done
  fi
fi

for required_id in "${required_sources[@]}"; do
  registry_has_id "$required_id" || block "Task requires an unregistered product source: $required_id"
done

if [[ "${#blockers[@]}" -gt 0 ]]; then
  product_gate_status="BLOCKED"
  task_mapping_status="BLOCKED"
  conflict_status="BLOCKED"
else
  product_gate_status="PASS"
fi

printf 'PRODUCT_SOURCE_GATE_STATUS:\n%s\n\n' "$product_gate_status"
printf 'PRODUCT_SOURCES:\n'
for line in "${source_lines[@]}"; do
  IFS='|' read -r source_id source_path exists non_empty content_hash applicable_module <<< "$line"
  printf -- '- source_id: %s\n  path: %s\n  exists: %s\n  non_empty: %s\n  content_hash: %s\n  applicable_module: %s\n' \
    "$source_id" "$source_path" "$exists" "$non_empty" "$content_hash" "$applicable_module"
done
printf '\nTASK_PRODUCT_SCOPE:\n%s\n\n' "${task_product_scope:-UNDECLARED}"
printf 'REQUIRED_PRODUCT_SOURCES:\n'
if [[ "${#required_sources[@]}" -eq 0 ]]; then
  printf -- '- NONE\n'
else
  for required_id in "${required_sources[@]}"; do
    printf -- '- %s\n' "$required_id"
  done
fi

if [[ -n "$simulation" ]]; then
  printf '\nSIMULATION_MAPPING:\n'
  for mapping in "${simulation_mapping[@]}"; do
    printf -- '- %s\n' "$mapping"
  done
fi

printf '\nTASK_MAPPING_STATUS:\n%s\n\n' "$task_mapping_status"
printf 'CONFLICT_STATUS:\n%s\n\n' "$conflict_status"
printf 'BLOCKERS:\n'
if [[ "${#blockers[@]}" -eq 0 ]]; then
  printf -- '- NONE\n'
else
  for blocker in "${blockers[@]}"; do
    printf -- '- %s\n' "$blocker"
  done
fi

if [[ "$product_gate_status" == "PASS" ]]; then
  printf '\nNOTE:\n- This deterministic gate proves registration and task mapping only; it does not prove product-plan understanding.\n'
  exit 0
fi

exit 1
