#!/usr/bin/env python3
"""Standard-library unit tests for the FE-04E governance semantic helper."""

from __future__ import annotations

import sys
import unittest
import random
from dataclasses import replace
from pathlib import Path
from typing import List, Optional


SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent
sys.path.insert(0, str(SCRIPT_DIR))

import check_fe04e_governance_semantics as semantics  # noqa: E402


def coverage_spec(
    layer: str,
    entrypoint: str,
    guard: str,
    objective: str,
    fixture_identity: object,
    expected_result: str = "PASS",
):
    return {
        "layer": layer,
        "entrypoint": entrypoint,
        "target_guard": guard,
        "assertion_objective": objective,
        "fixture_identity": fixture_identity,
        "expected_result": expected_result,
    }


HELPER_COVERAGE_MANIFEST = {
    "scope.heading.normalize": coverage_spec(
        "L1_UNIT", "normalize_heading", "REQUIRED_SCOPE_DISCOVERY",
        "normalizes_numbering_and_punctuation",
        ("Part A — Telegram（未来扩展）", "3.1: Message / Telegram"),
    ),
    "scope.telegram.bare-heading": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "discovers_bare_telegram_heading",
        "## Telegram\nTelegram remains prohibited.",
    ),
    "scope.telegram.chinese-status-heading": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "discovers_chinese_status_heading",
        "### Telegram 状态\nTelegram remains prohibited.",
    ),
    "scope.telegram.future-extension-heading": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "discovers_future_extension_heading",
        "### Telegram（未来扩展）\nTelegram remains prohibited.",
    ),
    "scope.telegram.numbered-heading": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "discovers_numbered_heading",
        "#### 3.1 Telegram Authorization\nTelegram remains prohibited.",
    ),
    "scope.telegram.step-heading": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "discovers_step_prefixed_heading",
        "## STEP 4 — Telegram\nTelegram remains prohibited.",
    ),
    "scope.telegram.bilingual-heading": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "discovers_bilingual_heading",
        "### 通知 / Telegram（未来扩展）\nTelegram remains prohibited.",
    ),
    "scope.telegram.heading-hierarchy": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "honors_heading_hierarchy",
        "## Telegram\nroot\n### Child\nchild\n## Next\nnext",
    ),
    "scope.telegram.multiple-sections": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "returns_every_matching_section",
        "## Telegram\ndisabled\n### Telegram Status\nblocked\n## Next",
    ),
    "scope.telegram.empty-section-observable": coverage_spec(
        "L1_UNIT", "production_scope_sections", "REQUIRED_SCOPE_DISCOVERY",
        "preserves_empty_section",
        "## Telegram\n\n## Next\nbody",
    ),
    "scope.telegram.missing-fail-closed": coverage_spec(
        "L2_HELPER_INTEGRATION", "required_scope_violations",
        "FAIL_CLOSED_MISSING_SCOPE", "rejects_missing_telegram_scope",
        "## Messages\nNo governed Telegram section.",
        "FAIL_CLOSED_MISSING_SCOPE",
    ),
    "scope.telegram.empty-fail-closed": coverage_spec(
        "L2_HELPER_INTEGRATION", "required_scope_violations",
        "FAIL_CLOSED_SCOPE_CONTENT_MISSING", "rejects_empty_telegram_scope",
        "## Telegram\n\n## Next\nbody",
        "FAIL_CLOSED_SCOPE_CONTENT_MISSING",
    ),
    "telegram.authorization.bare-enabled": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "detects_enabled_under_bare_heading",
        {"heading": "Telegram", "body": "Telegram is enabled."},
        "telegram authorization",
    ),
    "telegram.authorization.chinese-enabled": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "detects_enabled_under_chinese_status_heading",
        {"heading": "Telegram 状态", "body": "Telegram is enabled."},
        "telegram authorization",
    ),
    "telegram.authorization.future-implemented": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "detects_implemented_under_future_heading",
        {"heading": "Telegram（未来扩展）", "body": "Telegram is implemented."},
        "telegram authorization",
    ),
    "telegram.authorization.numbered-active": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "detects_active_under_numbered_heading",
        {"heading": "3. Telegram Authorization", "body": "Telegram is active."},
        "telegram authorization",
    ),
    "telegram.authorization.multisection-conflict": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation",
        "CONTRADICTORY_AUTHORIZATION", "detects_later_dangerous_section",
        "## Telegram\nTelegram remains prohibited.\n## Telegram Status\nTelegram is enabled.",
        "CONTRADICTORY_AUTHORIZATION",
    ),
    "telegram.authorization.prohibited-safe": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "accepts_explicit_prohibition",
        {"heading": "Telegram", "body": "Telegram remains prohibited."},
    ),
    "telegram.authorization.not-authorized-safe": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "accepts_explicit_non_authorization",
        {"heading": "Telegram", "body": "Telegram is not authorized."},
    ),
    "status.compound.plus": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "COMPOUND_AUTHORIZATION",
        "collects_plus_separated_safe_and_dangerous_tokens",
        "telegram: NOT_AUTHORIZED + AUTHORIZED",
    ),
    "status.compound.comma": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "COMPOUND_AUTHORIZATION",
        "collects_comma_separated_safe_and_dangerous_tokens",
        "telegram: DISABLED, ENABLED",
    ),
    "status.compound.slash": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "COMPOUND_AUTHORIZATION",
        "collects_slash_separated_safe_and_dangerous_tokens",
        "telegram: BLOCKED / ACTIVE",
    ),
    "status.compound.pipe": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "TRADING_AUTHORIZATION",
        "collects_pipe_separated_trading_tokens",
        "trading: NONE | LIMITED",
    ),
    "status.compound.english-conjunction": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "COMPOUND_AUTHORIZATION",
        "collects_conjunction_separated_tokens",
        "telegram: NOT_IMPLEMENTED but AUTHORIZED",
    ),
    "status.compound.chinese-punctuation": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "COMPOUND_AUTHORIZATION",
        "collects_chinese_punctuation_separated_tokens",
        "telegram: 未授权，但已启用",
    ),
    "status.yaml.nested-values": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "COMPOUND_AUTHORIZATION",
        "collects_nested_yaml_status_values",
        "telegram:\n  status: NOT_AUTHORIZED\n  enabled: true",
    ),
    "status.markdown.multiple-sentences": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "COMPOUND_AUTHORIZATION",
        "collects_statuses_across_markdown_sentences",
        "Telegram remains prohibited. Telegram is enabled.",
    ),
    "status.markdown.table-compound": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "COMPOUND_AUTHORIZATION",
        "collects_compound_table_statuses",
        "| Telegram | NOT_AUTHORIZED + ACTIVE |",
    ),
    "status.aggregate.safe-dangerous": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation",
        "CONTRADICTORY_AUTHORIZATION", "rejects_safe_then_dangerous_values",
        "telegram: NOT_AUTHORIZED + AUTHORIZED",
        "CONTRADICTORY_AUTHORIZATION",
    ),
    "status.aggregate.dangerous-safe": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation",
        "CONTRADICTORY_AUTHORIZATION", "rejects_dangerous_then_safe_values",
        "telegram: AUTHORIZED then NOT_AUTHORIZED",
        "CONTRADICTORY_AUTHORIZATION",
    ),
    "status.aggregate.multiple-dangerous": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "retains_all_dangerous_values",
        "telegram: ENABLED + ACTIVE",
        "telegram authorization",
    ),
    "status.aggregate.multiple-safe": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation", "TELEGRAM_AUTHORIZATION",
        "accepts_compatible_safe_values",
        "telegram: NOT_AUTHORIZED + BLOCKED",
    ),
    "status.unknown.compound": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation",
        "UNKNOWN_AUTHORIZATION_STATE", "fails_closed_on_unknown_compound_token",
        "telegram: NOT_AUTHORIZED + MAYBE",
        "UNKNOWN_AUTHORIZATION_STATE",
    ),
    "status.compound.original-order": coverage_spec(
        "L1_UNIT", "yaml_status_declarations", "COMPOUND_AUTHORIZATION",
        "preserves_original_value_and_token_order",
        "telegram: NOT_AUTHORIZED + AUTHORIZED",
    ),
    "status.conflict.diagnostic": coverage_spec(
        "L1_UNIT", "aggregate_authorization_violation",
        "CONTRADICTORY_AUTHORIZATION", "reports_original_tokens_and_logical_source",
        "fixture.yml/telegram: NOT_AUTHORIZED + AUTHORIZED",
        "CONTRADICTORY_AUTHORIZATION",
    ),
    "status.negated-authorization.safe": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "TELEGRAM_AUTHORIZATION",
        "classifies_negated_authorization_as_safe",
        "Telegram is not authorized.",
    ),
    "status.long-prohibition.safe": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "TRADING_AUTHORIZATION",
        "classifies_long_no_is_allowed_prohibition_as_safe",
        "No auto-open, auto-close, auto-reverse, order execution, auto-trading, external push send, fake positions, fake review records, or production-ready claim is allowed.",
    ),
    "execution.public.fallback": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "private execution contradiction guard", "contract_rejection",
        "Execution is used as fallback.", "REJECT",
    ),
    "execution.public.pending-partial": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "private execution contradiction guard", "contract_rejection",
        "Pending execution makes public PARTIAL.", "REJECT",
    ),
    "execution.public.required-completed": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "private execution contradiction guard", "contract_rejection",
        "COMPLETED is required for public READY.", "REJECT",
    ),
    "execution.public.failed-error": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "private execution contradiction guard", "contract_rejection",
        "FAILED produces public ERROR.", "REJECT",
    ),
    "risk.public.context": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "Risk context determines public readiness.", "REJECT",
    ),
    "risk.public.account": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "Account risk supplements public status.", "REJECT",
    ),
    "risk.public.position": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "Position risk refines public evaluation.", "REJECT",
    ),
    "position.public.direction-english": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "Long, short, or no position changes public lifecycle.", "REJECT",
    ),
    "position.public.holding-direction": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
        "rejects_holding_direction_dependency",
        "Holding direction determines public evaluation.",
        "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY",
    ),
    "position.public.chinese-long": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "多仓改变公共状态。", "REJECT",
    ),
    "position.public.chinese-empty": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "空仓影响公开生命周期。", "REJECT",
    ),
    "position.public.chinese-no-position": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "无仓决定公开就绪状态。", "REJECT",
    ),
    "position.public.chinese-direction": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "持仓方向参与公开评估。", "REJECT",
    ),
    "risk.public.chinese-private": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "UserPosition/private risk contradiction guard", "contract_rejection",
        "私有风险虽不展示但参与计算。", "REJECT",
    ),
    "recheck.private.control": coverage_spec(
        "L2_HELPER_INTEGRATION", "validate_position_risk_private_recheck_control",
        "PRIVATE_RECHECK_SCOPE", "accepts_owner_scoped_private_recheck_control",
        semantics.POSITION_RISK_PRIVATE_RECHECK_CONTROL,
    ),
    "recheck.public.control": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "private Recheck contradiction guard", "contract_rejection",
        semantics.POSITION_RISK_PRIVATE_RECHECK_CONTROL, "REJECT",
    ),
    "next-task.mutation-delivery-dangerous": coverage_spec(
        "L1_UNIT", "classify_next_task_statement",
        "NEXT_TASK_CAPABILITY_AUTHORIZATION",
        "classifies_mutation_and_delivery_as_dangerous",
        "Message/Push UI add mutation and automatic delivery", "DANGEROUS",
    ),
    "next-task.review-prohibition-safe": coverage_spec(
        "L1_UNIT", "classify_next_task_statement",
        "NEXT_TASK_CAPABILITY_AUTHORIZATION",
        "classifies_review_of_mutation_prohibition_as_safe",
        "Review mutation prohibition", "SAFE",
    ),
    "next-task.no-delivery-safe": coverage_spec(
        "L1_UNIT", "classify_next_task_statement",
        "NEXT_TASK_CAPABILITY_AUTHORIZATION",
        "classifies_no_delivery_verification_as_safe",
        "Verify no automatic delivery", "SAFE",
    ),
    "movement.trading.multivalue": coverage_spec(
        "L1_UNIT", "trading_movement_values", "TRADING_CAPABILITY_MOVEMENT",
        "collects_every_trading_movement_value",
        "TRADING_CAPABILITY_MOVEMENT: NONE; TRADING_CAPABILITY_MOVEMENT: LIMITED",
    ),
    "movement.capability.multivalue": coverage_spec(
        "L1_UNIT", "trading_movement_values", "CAPABILITY_MOVEMENT",
        "collects_every_generic_capability_movement_value",
        "CAPABILITY_MOVEMENT: NONE; CAPABILITY_MOVEMENT: EXPANDED",
    ),
    "clause.safe-then-dangerous": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "CLAUSE_ISOLATION",
        "isolates_safe_then_dangerous_clauses",
        "No Telegram capability is authorized, but Telegram delivery is allowed.",
        "CONTRADICTORY_AUTHORIZATION",
    ),
    "clause.dangerous-then-safe": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "CLAUSE_ISOLATION",
        "isolates_dangerous_then_safe_clauses",
        "Telegram is enabled, but Telegram remains prohibited.",
    ),
    "clause.english-but-offsets": coverage_spec(
        "L1_UNIT", "logical_clauses", "CLAUSE_ISOLATION",
        "preserves_offsets_across_english_but",
        "Telegram is disabled, but Telegram is active.",
    ),
    "clause.english-however": coverage_spec(
        "L1_UNIT", "logical_clauses", "CLAUSE_ISOLATION",
        "splits_english_however_boundary",
        "System notification is disabled, however automatic delivery is enabled.",
    ),
    "clause.english-yet": coverage_spec(
        "L1_UNIT", "logical_clauses", "CLAUSE_ISOLATION",
        "splits_english_yet_boundary",
        "Trading is not authorized, yet order placement is allowed.",
    ),
    "clause.semicolon": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "CLAUSE_ISOLATION",
        "splits_semicolon_boundary",
        "Telegram is not enabled; Telegram delivery is active.",
    ),
    "clause.period": coverage_spec(
        "L1_UNIT", "logical_clauses", "CLAUSE_ISOLATION",
        "splits_period_boundary",
        "Telegram remains prohibited. Telegram integration is supported.",
    ),
    "clause.chinese-but": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "CLAUSE_ISOLATION",
        "splits_chinese_but_boundary",
        "未授权 Telegram，但 Telegram 推送已启用。",
    ),
    "clause.chinese-however": coverage_spec(
        "L1_UNIT", "logical_clauses", "CLAUSE_ISOLATION",
        "splits_chinese_however_boundary",
        "Telegram 已禁用，然而 Telegram 推送已启用。",
    ),
    "clause.chinese-sentence": coverage_spec(
        "L1_UNIT", "logical_clauses", "CLAUSE_ISOLATION",
        "splits_chinese_sentence_boundary",
        "Telegram 已禁用。Telegram 推送已启用。",
    ),
    "clause.safe-span-offset": coverage_spec(
        "L1_UNIT", "status_tokens", "CLAUSE_ISOLATION",
        "ends_safe_span_before_later_dangerous_clause",
        "No Telegram capability is authorized, but Telegram delivery is allowed.",
    ),
    "clause.overlap-safe-dangerous": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "CLAUSE_ISOLATION",
        "preserves_overlapping_safe_and_dangerous_relations",
        "Telegram is not authorized but Telegram is authorized.",
    ),
    "unknown.residue.maybe": coverage_spec(
        "L1_UNIT", "status_value_tokens", "UNKNOWN_AUTHORIZATION_STATE",
        "preserves_unknown_maybe_residue",
        "NOT_AUTHORIZED MAYBE", "UNKNOWN_AUTHORIZATION_STATE",
    ),
    "unknown.residue.and-maybe": coverage_spec(
        "L1_UNIT", "status_value_tokens", "UNKNOWN_AUTHORIZATION_STATE",
        "preserves_compound_unknown_residue",
        "NOT_AUTHORIZED AND_MAYBE", "UNKNOWN",
    ),
    "unknown.residue.optional": coverage_spec(
        "L1_UNIT", "status_value_tokens", "UNKNOWN_AUTHORIZATION_STATE",
        "classifies_optional_residue_unknown",
        "DISABLED OPTIONAL", "UNKNOWN",
    ),
    "unknown.residue.none-maybe": coverage_spec(
        "L1_UNIT", "status_value_tokens", "UNKNOWN_AUTHORIZATION_STATE",
        "preserves_unknown_after_none",
        "NONE MAYBE", "UNKNOWN",
    ),
    "unknown.residue.offsets": coverage_spec(
        "L1_UNIT", "status_value_tokens", "UNKNOWN_AUTHORIZATION_STATE",
        "preserves_original_unknown_offsets",
        "NOT_AUTHORIZED MAYBE", "UNKNOWN",
    ),
    "unknown.residue.punctuation-safe": coverage_spec(
        "L1_UNIT", "status_value_tokens", "UNKNOWN_AUTHORIZATION_STATE",
        "ignores_punctuation_only_residue",
        "NOT_AUTHORIZED --", "SAFE",
    ),
    "unknown.residue.grammar-safe": coverage_spec(
        "L1_UNIT", "status_declarations_for_text", "UNKNOWN_AUTHORIZATION_STATE",
        "ignores_grammar_only_residue",
        "Telegram is explicitly NOT_AUTHORIZED.", "SAFE",
    ),
    "unknown.residue.dangerous": coverage_spec(
        "L1_UNIT", "status_value_tokens", "UNKNOWN_AUTHORIZATION_STATE",
        "classifies_known_dangerous_residue_as_dangerous",
        "NOT_AUTHORIZED ALLOWED", "DANGEROUS",
    ),
    "execution.requirement.public-ready": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate",
        "private execution contradiction guard", "contract_rejection",
        "Public READY requires execution completion.", "REJECT",
    ),
    "execution.requirement.depends-on": coverage_spec(
        "L1_UNIT", "private_semantic_category",
        "PRIVATE_EXECUTION_REQUIREMENT",
        "classifies_depends_on_as_requirement",
        "Public readiness depends on execution completion.",
        "PRIVATE_EXECUTION_REQUIREMENT",
    ),
    "execution.requirement.required-and-gates": coverage_spec(
        "L1_UNIT", "private_semantic_category", "PRIVATE_EXECUTION_PRECEDENCE",
        "classifies_required_for_and_explicit_gate_relations",
        (
            "Execution completion is required for Public READY.",
            "Execution completion gates public READY.",
            "Execution controls public readiness.",
        ),
        "REQUIREMENT_AND_GATE_CATEGORIES",
    ),
    "execution.requirement.waits-for": coverage_spec(
        "L1_UNIT", "private_semantic_category",
        "PRIVATE_EXECUTION_REQUIREMENT",
        "classifies_waits_for_as_requirement",
        "Public visibility waits for execution completion.",
        "PRIVATE_EXECUTION_REQUIREMENT",
    ),
    "execution.pending.precedence": coverage_spec(
        "L1_UNIT", "private_semantic_category",
        "PRIVATE_EXECUTION_PENDING_PARTIAL",
        "preserves_pending_category_precedence",
        "Pending execution keeps public state PARTIAL.",
        "PRIVATE_EXECUTION_PENDING_PARTIAL",
    ),
    "execution.explicit-non-use": coverage_spec(
        "L2_HELPER_INTEGRATION", "semantic_evaluate", "LEGAL_CONTROL",
        "contract_acceptance",
        "Public READY is determined only by the shared public OPPORTUNITY projection; execution completion is not used.",
        "ACCEPT",
    ),
    "inventory.dedup.same-semantic-record": coverage_spec(
        "L1_UNIT", "deduplicate_coverage_records", "QUALIFYING_INVENTORY_DEDUP",
        "deduplicates_same_layer_equivalent_scenario",
        {"layer": "L2_HELPER_INTEGRATION", "fixture": "same fixture"},
    ),
    "inventory.dedup.layer-separation": coverage_spec(
        "L1_UNIT", "deduplicate_coverage_records", "QUALIFYING_INVENTORY_DEDUP",
        "keeps_distinct_layers_separate",
        {"layers": ["L1_UNIT", "L2_HELPER_INTEGRATION"], "fixture": "same fixture"},
    ),
    "inventory.metadata.validation": coverage_spec(
        "L1_UNIT", "helper_coverage_audit_from_classes",
        "QUALIFYING_METADATA_VALIDATION",
        "rejects_missing_duplicate_and_invalid_metadata",
        {
            "valid_fixture_pair": [
                "Public READY requires execution completion.",
                "public ready requires execution completion",
            ],
            "invalid_cases": ["missing metadata", "duplicate id", "invalid layer", "missing objective"],
        },
    ),
    "inventory.counting.raw-vs-qualifying": coverage_spec(
        "L1_UNIT", "build_coverage_inventory", "QUALIFYING_INVENTORY_COUNTING",
        "separates_raw_and_qualifying_counts",
        {"static_assertions": 1, "legacy_negative_probes": 0},
    ),
    "inventory.stability.rename-order-path": coverage_spec(
        "L1_UNIT", "semantic_inventory_sha256", "QUALIFYING_INVENTORY_STABILITY",
        "proves_rename_order_path_and_duplicate_stability",
        {
            "orders": ["original", "reversed", "deterministic-shuffle-a", "deterministic-shuffle-b", "category-reordered"],
            "paths": ["/Users/xuchao/worktree", "/tmp/worktree-a", "/tmp/worktree-b", "scripts/helper.py"],
            "display": ["OriginalClass.test_original", "RenamedClass.test_renamed"],
        },
    ),
    "inventory.metadata.completeness": coverage_spec(
        "L1_UNIT", "helper_unit_coverage_audit",
        "QUALIFYING_METADATA_VALIDATION",
        "audits_all_helper_metadata_and_unique_ids",
        "helper coverage manifest with 93 explicitly decorated semantic cases",
    ),
    "scope.required-markdown-headings": coverage_spec(
        "L2_HELPER_INTEGRATION", "required_scope_violations",
        "FAIL_CLOSED_MISSING_SCOPE",
        "rejects_each_removed_required_markdown_heading",
        "REQUIRED_SCOPE_REQUIREMENTS with each governed heading removed",
        "FAIL_CLOSED_MISSING_SCOPE",
    ),
    "scope.next-task.empty-key": coverage_spec(
        "L2_HELPER_INTEGRATION", "required_scope_violations",
        "FAIL_CLOSED_MISSING_OR_EMPTY_NEXT_TASK_SCOPE",
        "rejects_empty_next_task_module",
        'module: ""',
        "FAIL_CLOSED_MISSING_OR_EMPTY_NEXT_TASK_SCOPE",
    ),
    "scope.semantic.required-heading-missing": coverage_spec(
        "L2_HELPER_INTEGRATION", "required_scope_violations",
        "FAIL_CLOSED_MISSING_SCOPE",
        "rejects_missing_required_semantic_scope",
        "replace Message And Telegram V2 heading with Unrelated Capability",
        "FAIL_CLOSED_MISSING_SCOPE",
    ),
}


def helper_coverage(coverage_id: str):
    metadata = HELPER_COVERAGE_MANIFEST[coverage_id]
    return semantics.coverage_case(coverage_id=coverage_id, **metadata)


class GovernanceSemanticHelperTest(unittest.TestCase):
    def _telegram_declarations(
        self, heading: str, body: str
    ) -> List[semantics.StatusDeclaration]:
        text = f"## {heading}\n{body}\n\n## Next\nUnrelated.\n"
        sections = semantics.production_scope_sections(text, "telegram")
        self.assertTrue(sections, f"production Telegram scope missing for {heading!r}")
        declarations: List[semantics.StatusDeclaration] = []
        for section in sections:
            for unit in semantics.statement_units(section.body):
                declarations.extend(
                    semantics.status_declarations_for_text(
                        "telegram",
                        unit,
                        "fixture.md",
                        section.title,
                    )
                )
        return declarations

    def _telegram_violations(
        self, heading: str, body: str
    ) -> List[semantics.Violation]:
        return semantics.aggregate_authorization_violation(
            "telegram",
            "Telegram semantic authorization guard",
            self._telegram_declarations(heading, body),
        )

    def _yaml_declarations(
        self, capability: str, value: str, key: Optional[str] = None
    ) -> List[semantics.StatusDeclaration]:
        yaml_key = key or capability.replace(" ", "_")
        return semantics.yaml_status_declarations(
            capability,
            "fixture.yml",
            f'{yaml_key}: "{value}"\n',
        )

    def _public_violation(
        self, statement: str, guard: str
    ) -> semantics.Violation:
        documents = semantics.load_documents(ROOT)
        semantics.insert_before(
            documents,
            semantics.SEMANTIC,
            "Owner-scoped `POSITION_RISK` detail remains a separate",
            statement,
        )
        matches = [
            item
            for item in semantics.evaluate(documents)[guard]
            if statement.rstrip("。.") in item.excerpt.rstrip("。.")
        ]
        self.assertTrue(matches, f"target guard did not reject {statement!r}")
        return matches[0]

    @helper_coverage("scope.heading.normalize")
    def test_heading_normalization_removes_numbering_and_punctuation(self) -> None:
        self.assertEqual(
            semantics.normalize_heading("Part A — Telegram（未来扩展）"),
            "telegram 未来扩展",
        )
        self.assertEqual(
            semantics.normalize_heading("3.1: Message / Telegram"),
            "message telegram",
        )

    @helper_coverage("scope.telegram.bare-heading")
    def test_production_alias_discovers_bare_telegram_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "## Telegram\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual([item.title for item in sections], ["Telegram"])

    @helper_coverage("scope.telegram.chinese-status-heading")
    def test_production_alias_discovers_chinese_status_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "### Telegram 状态\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual([item.title for item in sections], ["Telegram 状态"])

    @helper_coverage("scope.telegram.future-extension-heading")
    def test_production_alias_discovers_future_extension_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "### Telegram（未来扩展）\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual([item.title for item in sections], ["Telegram（未来扩展）"])

    @helper_coverage("scope.telegram.numbered-heading")
    def test_production_alias_discovers_numbered_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "#### 3.1 Telegram Authorization\nTelegram remains prohibited.\n",
            "telegram",
        )
        self.assertEqual(len(sections), 1)
        self.assertEqual(sections[0].normalized_title, "telegram authorization")

    @helper_coverage("scope.telegram.step-heading")
    def test_production_alias_discovers_step_prefixed_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "## STEP 4 — Telegram\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual(len(sections), 1)

    @helper_coverage("scope.telegram.bilingual-heading")
    def test_production_alias_discovers_bilingual_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "### 通知 / Telegram（未来扩展）\nTelegram remains prohibited.\n",
            "telegram",
        )
        self.assertEqual(len(sections), 1)

    @helper_coverage("scope.telegram.heading-hierarchy")
    def test_section_extraction_honors_heading_hierarchy(self) -> None:
        text = "## Telegram\nroot\n### Child\nchild\n## Next\nnext\n"
        sections = semantics.production_scope_sections(text, "telegram")
        self.assertEqual(len(sections), 1)
        self.assertIn("child", sections[0].body)
        self.assertNotIn("next", sections[0].body)

    @helper_coverage("scope.telegram.multiple-sections")
    def test_production_alias_returns_multiple_telegram_sections(self) -> None:
        text = "## Telegram\ndisabled\n### Telegram Status\nblocked\n## Next\n"
        self.assertEqual(
            len(semantics.production_scope_sections(text, "telegram")), 2
        )

    @helper_coverage("scope.telegram.empty-section-observable")
    def test_empty_section_is_observable(self) -> None:
        sections = semantics.production_scope_sections(
            "## Telegram\n\n## Next\nbody\n", "telegram"
        )
        self.assertEqual(len(sections), 1)
        self.assertEqual(sections[0].body, "")

    @helper_coverage("scope.telegram.missing-fail-closed")
    def test_missing_production_telegram_scope_fails_closed(self) -> None:
        documents = semantics.load_documents(ROOT)
        documents[semantics.SEMANTIC] = "## Messages\nNo governed Telegram section.\n"
        violations = semantics.required_scope_violations(documents)
        self.assertTrue(
            any(
                item.scope == "Telegram"
                and item.category == "FAIL_CLOSED_MISSING_SCOPE"
                for item in violations
            )
        )

    @helper_coverage("scope.telegram.empty-fail-closed")
    def test_empty_production_telegram_scope_fails_closed(self) -> None:
        documents = semantics.load_documents(ROOT)
        documents[semantics.SEMANTIC] = "## Telegram\n\n## Next\nbody\n"
        violations = semantics.required_scope_violations(documents)
        self.assertTrue(
            any(
                item.scope == "Telegram"
                and item.category == "FAIL_CLOSED_SCOPE_CONTENT_MISSING"
                for item in violations
            )
        )

    @helper_coverage("telegram.authorization.bare-enabled")
    def test_bare_telegram_heading_detects_enabled(self) -> None:
        violations = self._telegram_violations("Telegram", "Telegram is enabled.")
        self.assertEqual(violations[0].category, "telegram authorization")

    @helper_coverage("telegram.authorization.chinese-enabled")
    def test_chinese_status_heading_detects_enabled(self) -> None:
        violations = self._telegram_violations(
            "Telegram 状态", "Telegram is enabled."
        )
        self.assertEqual(violations[0].category, "telegram authorization")

    @helper_coverage("telegram.authorization.future-implemented")
    def test_future_extension_heading_detects_implemented(self) -> None:
        violations = self._telegram_violations(
            "Telegram（未来扩展）", "Telegram is implemented."
        )
        self.assertEqual(violations[0].category, "telegram authorization")

    @helper_coverage("telegram.authorization.numbered-active")
    def test_numbered_heading_detects_active(self) -> None:
        violations = self._telegram_violations(
            "3. Telegram Authorization", "Telegram is active."
        )
        self.assertEqual(violations[0].category, "telegram authorization")

    @helper_coverage("telegram.authorization.multisection-conflict")
    def test_second_telegram_section_danger_is_not_skipped(self) -> None:
        text = (
            "## Telegram\nTelegram remains prohibited.\n"
            "## Telegram Status\nTelegram is enabled.\n"
        )
        declarations: List[semantics.StatusDeclaration] = []
        sections = semantics.production_scope_sections(text, "telegram")
        self.assertEqual(len(sections), 2)
        for section in sections:
            for unit in semantics.statement_units(section.body):
                declarations.extend(
                    semantics.status_declarations_for_text(
                        "telegram", unit, "fixture.md", section.title
                    )
                )
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "CONTRADICTORY_AUTHORIZATION")

    @helper_coverage("telegram.authorization.prohibited-safe")
    def test_telegram_prohibited_prose_is_safe(self) -> None:
        self.assertFalse(
            self._telegram_violations("Telegram", "Telegram remains prohibited.")
        )

    @helper_coverage("telegram.authorization.not-authorized-safe")
    def test_telegram_not_authorized_is_safe(self) -> None:
        self.assertFalse(
            self._telegram_violations("Telegram", "Telegram is not authorized.")
        )

    @helper_coverage("status.compound.plus")
    def test_plus_compound_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_AUTHORIZED + AUTHORIZED"
        )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )
        self.assertEqual(
            [item.matched_token for item in declarations],
            ["NOT_AUTHORIZED", "AUTHORIZED"],
        )

    @helper_coverage("status.compound.comma")
    def test_comma_compound_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("telegram", "DISABLED, ENABLED")
        self.assertEqual(
            [item.matched_token for item in declarations], ["DISABLED", "ENABLED"]
        )

    @helper_coverage("status.compound.slash")
    def test_slash_compound_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("telegram", "BLOCKED / ACTIVE")
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    @helper_coverage("status.compound.pipe")
    def test_pipe_compound_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("trading", "NONE | LIMITED")
        self.assertEqual(
            [item.matched_token for item in declarations], ["NONE", "LIMITED"]
        )

    @helper_coverage("status.compound.english-conjunction")
    def test_english_conjunction_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_IMPLEMENTED but AUTHORIZED"
        )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    @helper_coverage("status.compound.chinese-punctuation")
    def test_chinese_punctuation_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("telegram", "未授权，但已启用")
        self.assertEqual(
            [item.matched_token for item in declarations], ["未授权", "已启用"]
        )

    @helper_coverage("status.yaml.nested-values")
    def test_nested_yaml_status_collects_all_values(self) -> None:
        declarations = semantics.yaml_status_declarations(
            "telegram",
            "fixture.yml",
            "telegram:\n  status: NOT_AUTHORIZED\n  enabled: true\n",
        )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )
        self.assertEqual(
            [item.key_path for item in declarations],
            ["telegram.status", "telegram.enabled"],
        )

    @helper_coverage("status.markdown.multiple-sentences")
    def test_markdown_prose_keeps_both_statuses(self) -> None:
        declarations: List[semantics.StatusDeclaration] = []
        for unit in semantics.statement_units(
            "Telegram remains prohibited. Telegram is enabled."
        ):
            declarations.extend(
                semantics.status_declarations_for_text("telegram", unit)
            )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    @helper_coverage("status.markdown.table-compound")
    def test_markdown_table_collects_compound_statuses(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "| Telegram | NOT_AUTHORIZED + ACTIVE |"
        )
        self.assertEqual(
            [item.matched_token for item in declarations],
            ["NOT_AUTHORIZED", "ACTIVE"],
        )
        self.assertTrue(all(item.source_kind == "TABLE" for item in declarations))

    @helper_coverage("status.aggregate.safe-dangerous")
    def test_all_value_aggregation_rejects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_AUTHORIZED + AUTHORIZED"
        )
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "CONTRADICTORY_AUTHORIZATION")

    @helper_coverage("status.aggregate.dangerous-safe")
    def test_all_value_aggregation_rejects_dangerous_then_safe(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "AUTHORIZED then NOT_AUTHORIZED"
        )
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "CONTRADICTORY_AUTHORIZATION")

    @helper_coverage("status.aggregate.multiple-dangerous")
    def test_multiple_dangerous_values_are_all_retained(self) -> None:
        declarations = self._yaml_declarations("telegram", "ENABLED + ACTIVE")
        self.assertEqual(
            [item.matched_token for item in declarations], ["ENABLED", "ACTIVE"]
        )
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "telegram authorization")

    @helper_coverage("status.aggregate.multiple-safe")
    def test_compatible_multiple_safe_values_pass(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_AUTHORIZED + BLOCKED"
        )
        self.assertEqual(
            {item.classification for item in declarations},
            {semantics.StatusClass.SAFE},
        )
        self.assertFalse(
            semantics.aggregate_authorization_violation(
                "telegram", "guard", declarations
            )
        )

    @helper_coverage("status.unknown.compound")
    def test_unknown_compound_token_fails_closed(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_AUTHORIZED + MAYBE"
        )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.UNKNOWN],
        )
        self.assertEqual(declarations[1].matched_token, "MAYBE")
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(
            violations[0].category, "UNKNOWN_AUTHORIZATION_STATE"
        )

    @helper_coverage("status.compound.original-order")
    def test_original_compound_value_and_token_order_are_retained(self) -> None:
        original = "NOT_AUTHORIZED + AUTHORIZED"
        declarations = self._yaml_declarations("telegram", original)
        self.assertEqual(
            [item.original_value for item in declarations], [original, original]
        )
        self.assertEqual([item.token_order for item in declarations], [1, 2])
        self.assertEqual(
            [item.normalized_value for item in declarations],
            ["not authorized", "authorized"],
        )

    @helper_coverage("status.conflict.diagnostic")
    def test_conflict_diagnostic_contains_original_tokens_and_source(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_AUTHORIZED + AUTHORIZED"
        )
        violation = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )[0]
        self.assertIn("ORIGINAL_VALUE=['NOT_AUTHORIZED + AUTHORIZED']", violation.excerpt)
        self.assertIn("NOT_AUTHORIZED [SAFE]", violation.excerpt)
        self.assertIn("AUTHORIZED [DANGEROUS]", violation.excerpt)
        self.assertIn("fixture.yml/telegram", violation.excerpt)
        self.assertIn("kind=YAML", violation.excerpt)

    @helper_coverage("status.negated-authorization.safe")
    def test_negated_authorization_is_safe_only(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "Telegram is not authorized."
        )
        self.assertEqual(
            {item.classification for item in declarations},
            {semantics.StatusClass.SAFE},
        )

    @helper_coverage("status.long-prohibition.safe")
    def test_long_no_is_allowed_prohibition_is_safe(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "trading",
            "No auto-open, auto-close, auto-reverse, order execution, auto-trading, "
            "external push send, fake positions, fake review records, or production-ready "
            "claim is allowed.",
        )
        self.assertEqual(
            {item.classification for item in declarations},
            {semantics.StatusClass.SAFE},
        )

    @helper_coverage("execution.public.fallback")
    def test_execution_fallback_equivalent_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Execution is used as fallback.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_FALLBACK")

    @helper_coverage("execution.public.pending-partial")
    def test_pending_execution_equivalent_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Pending execution makes public PARTIAL.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_PENDING_PARTIAL")

    @helper_coverage("execution.public.required-completed")
    def test_completed_execution_requirement_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "COMPLETED is required for public READY.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_REQUIREMENT")

    @helper_coverage("execution.public.failed-error")
    def test_failed_execution_mapping_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "FAILED produces public ERROR.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_FAILURE_ERROR")

    @helper_coverage("risk.public.context")
    def test_risk_context_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Risk context determines public readiness.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

    @helper_coverage("risk.public.account")
    def test_account_risk_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Account risk supplements public status.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

    @helper_coverage("risk.public.position")
    def test_position_risk_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Position risk refines public evaluation.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

    @helper_coverage("position.public.direction-english")
    def test_english_position_direction_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Long, short, or no position changes public lifecycle.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    @helper_coverage("position.public.holding-direction")
    def test_english_holding_direction_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Holding direction determines public evaluation.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    @helper_coverage("position.public.chinese-long")
    def test_chinese_long_position_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "多仓改变公共状态。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    @helper_coverage("position.public.chinese-empty")
    def test_chinese_short_position_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "空仓影响公开生命周期。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    @helper_coverage("position.public.chinese-no-position")
    def test_chinese_no_position_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "无仓决定公开就绪状态。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    @helper_coverage("position.public.chinese-direction")
    def test_chinese_position_direction_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "持仓方向参与公开评估。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    @helper_coverage("risk.public.chinese-private")
    def test_chinese_private_risk_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "私有风险虽不展示但参与计算。",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

    @helper_coverage("recheck.private.control")
    def test_position_risk_private_recheck_control_traverses_real_private_scope(self) -> None:
        documents = semantics.load_documents(ROOT)
        semantics.insert_after(
            documents,
            semantics.SEMANTIC,
            "Owner-scoped `POSITION_RISK` detail remains a separate\n"
            "`OWNER_SCOPED_PRIVATE_PROJECTION`.",
            semantics.POSITION_RISK_PRIVATE_RECHECK_CONTROL,
        )
        self.assertIsNone(
            semantics.validate_position_risk_private_recheck_control(documents)
        )
        public_scopes, private_scopes = semantics.telegram_domain_scopes(documents)
        self.assertFalse(
            any(
                semantics.POSITION_RISK_PRIVATE_RECHECK_CONTROL in scope.text
                for scope in public_scopes
            )
        )
        self.assertTrue(
            any(
                semantics.POSITION_RISK_PRIVATE_RECHECK_CONTROL in scope.text
                for scope in private_scopes
            )
        )
        self.assertFalse(
            semantics.evaluate(documents)["private Recheck contradiction guard"]
        )

    @helper_coverage("recheck.public.control")
    def test_same_private_recheck_control_fails_in_public_scope(self) -> None:
        violation = self._public_violation(
            semantics.POSITION_RISK_PRIVATE_RECHECK_CONTROL,
            "private Recheck contradiction guard",
        )
        self.assertEqual(
            violation.category, "PRIVATE_RECHECK_PUBLIC_STATE_DEPENDENCY"
        )

    @helper_coverage("next-task.mutation-delivery-dangerous")
    def test_next_task_mutation_delivery_is_dangerous(self) -> None:
        self.assertEqual(
            semantics.classify_next_task_statement(
                "Message/Push UI add mutation and automatic delivery"
            ),
            semantics.StatusClass.DANGEROUS,
        )

    @helper_coverage("next-task.review-prohibition-safe")
    def test_next_task_review_mutation_prohibition_is_safe(self) -> None:
        self.assertEqual(
            semantics.classify_next_task_statement("Review mutation prohibition"),
            semantics.StatusClass.SAFE,
        )

    @helper_coverage("next-task.no-delivery-safe")
    def test_next_task_verify_no_delivery_is_safe(self) -> None:
        self.assertEqual(
            semantics.classify_next_task_statement("Verify no automatic delivery"),
            semantics.StatusClass.SAFE,
        )

    @helper_coverage("movement.trading.multivalue")
    def test_trading_movement_collects_every_value(self) -> None:
        declarations = semantics.trading_movement_values(
            "TRADING_CAPABILITY_MOVEMENT: NONE; "
            "TRADING_CAPABILITY_MOVEMENT: LIMITED"
        )
        self.assertEqual([item.value for item in declarations], ["none", "limited"])
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    @helper_coverage("movement.capability.multivalue")
    def test_generic_capability_movement_collects_every_value(self) -> None:
        declarations = semantics.trading_movement_values(
            "CAPABILITY_MOVEMENT: NONE; CAPABILITY_MOVEMENT: EXPANDED"
        )
        self.assertEqual([item.value for item in declarations], ["none", "expanded"])

    @helper_coverage("clause.safe-then-dangerous")
    def test_clause_safe_then_dangerous_isolated(self) -> None:
        text = "No Telegram capability is authorized, but Telegram delivery is allowed."
        declarations = semantics.status_declarations_for_text("telegram", text)
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )
        self.assertEqual([item.clause_order for item in declarations], [1, 2])
        violation = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )[0]
        self.assertIn("CLAUSES=[1. No Telegram capability is authorized", violation.excerpt)
        self.assertIn("2. Telegram delivery is allowed CLASSIFICATION=DANGEROUS", violation.excerpt)

    @helper_coverage("clause.dangerous-then-safe")
    def test_clause_dangerous_then_safe_isolated(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "Telegram is enabled, but Telegram remains prohibited."
        )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.DANGEROUS, semantics.StatusClass.SAFE],
        )

    @helper_coverage("clause.english-but-offsets")
    def test_clause_english_but_boundary_retains_offsets(self) -> None:
        text = "Telegram is disabled, but Telegram is active."
        clauses = semantics.logical_clauses(text)
        self.assertEqual([item.text for item in clauses], ["Telegram is disabled", "Telegram is active"])
        self.assertEqual(text[clauses[1].start : clauses[1].end], clauses[1].text)

    @helper_coverage("clause.english-however")
    def test_clause_english_however_boundary(self) -> None:
        clauses = semantics.logical_clauses(
            "System notification is disabled, however automatic delivery is enabled."
        )
        self.assertEqual(len(clauses), 2)
        self.assertEqual(clauses[1].text, "automatic delivery is enabled")

    @helper_coverage("clause.english-yet")
    def test_clause_english_yet_boundary(self) -> None:
        clauses = semantics.logical_clauses(
            "Trading is not authorized, yet order placement is allowed."
        )
        self.assertEqual(len(clauses), 2)

    @helper_coverage("clause.semicolon")
    def test_clause_semicolon_boundary(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "Telegram is not enabled; Telegram delivery is active."
        )
        self.assertEqual([item.clause_order for item in declarations], [1, 2])

    @helper_coverage("clause.period")
    def test_clause_period_boundary(self) -> None:
        clauses = semantics.logical_clauses(
            "Telegram remains prohibited. Telegram integration is supported."
        )
        self.assertEqual(len(clauses), 2)

    @helper_coverage("clause.chinese-but")
    def test_clause_chinese_but_boundary(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "未授权 Telegram，但 Telegram 推送已启用。"
        )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    @helper_coverage("clause.chinese-however")
    def test_clause_chinese_however_boundary(self) -> None:
        clauses = semantics.logical_clauses("Telegram 已禁用，然而 Telegram 推送已启用。")
        self.assertEqual(len(clauses), 2)

    @helper_coverage("clause.chinese-sentence")
    def test_clause_chinese_sentence_boundary(self) -> None:
        clauses = semantics.logical_clauses("Telegram 已禁用。Telegram 推送已启用。")
        self.assertEqual(len(clauses), 2)

    @helper_coverage("clause.safe-span-offset")
    def test_safe_span_ends_before_later_dangerous_clause(self) -> None:
        text = "No Telegram capability is authorized, but Telegram delivery is allowed."
        tokens = semantics.status_tokens(text)
        self.assertLess(tokens[0].end, tokens[1].start)
        self.assertEqual(tokens[0].clause_end, text.index(","))

    @helper_coverage("clause.overlap-safe-dangerous")
    def test_clause_overlap_preserves_safe_and_dangerous(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "Telegram is not authorized but Telegram is authorized."
        )
        self.assertEqual(
            {item.classification for item in declarations},
            {semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS},
        )

    @helper_coverage("unknown.residue.maybe")
    def test_unknown_residue_not_authorized_maybe(self) -> None:
        tokens = semantics.status_value_tokens("NOT_AUTHORIZED MAYBE", include_unknown=True)
        self.assertEqual(
            [item.classification for item in tokens],
            [semantics.StatusClass.SAFE, semantics.StatusClass.UNKNOWN],
        )
        self.assertEqual(tokens[1].original, "MAYBE")
        declarations = self._yaml_declarations("telegram", "NOT_AUTHORIZED MAYBE")
        violation = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )[0]
        self.assertIn("UNCONSUMED=['MAYBE']", violation.excerpt)

    @helper_coverage("unknown.residue.and-maybe")
    def test_unknown_residue_not_authorized_and_maybe(self) -> None:
        tokens = semantics.status_value_tokens(
            "NOT_AUTHORIZED AND_MAYBE", include_unknown=True
        )
        self.assertEqual(tokens[1].original, "AND_MAYBE")

    @helper_coverage("unknown.residue.optional")
    def test_unknown_residue_disabled_optional(self) -> None:
        tokens = semantics.status_value_tokens("DISABLED OPTIONAL", include_unknown=True)
        self.assertEqual(tokens[1].classification, semantics.StatusClass.UNKNOWN)

    @helper_coverage("unknown.residue.none-maybe")
    def test_unknown_residue_none_maybe(self) -> None:
        tokens = semantics.status_value_tokens("NONE MAYBE", include_unknown=True)
        self.assertEqual(tokens[1].original, "MAYBE")

    @helper_coverage("unknown.residue.offsets")
    def test_unknown_residue_offsets_are_original_offsets(self) -> None:
        source = "NOT_AUTHORIZED MAYBE"
        unknown = semantics.status_value_tokens(source, include_unknown=True)[1]
        self.assertEqual(source[unknown.start : unknown.end], "MAYBE")

    @helper_coverage("unknown.residue.punctuation-safe")
    def test_punctuation_only_residue_is_allowed(self) -> None:
        tokens = semantics.status_value_tokens("NOT_AUTHORIZED --", include_unknown=True)
        self.assertEqual({item.classification for item in tokens}, {semantics.StatusClass.SAFE})

    @helper_coverage("unknown.residue.grammar-safe")
    def test_grammar_only_residue_is_allowed(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "Telegram is explicitly NOT_AUTHORIZED."
        )
        self.assertEqual(
            {item.classification for item in declarations}, {semantics.StatusClass.SAFE}
        )

    @helper_coverage("unknown.residue.dangerous")
    def test_dangerous_residue_is_dangerous_not_unknown(self) -> None:
        tokens = semantics.status_value_tokens(
            "NOT_AUTHORIZED ALLOWED", include_unknown=True
        )
        self.assertEqual(
            [item.classification for item in tokens],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    @helper_coverage("execution.requirement.public-ready")
    def test_public_ready_requires_execution_completion(self) -> None:
        statement = "Public READY requires execution completion."
        violation = self._public_violation(
            statement,
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_REQUIREMENT")
        self.assertEqual(violation.scope, "Public OPPORTUNITY")
        self.assertEqual(violation.excerpt, statement)
        self.assertEqual(
            violation.error_code,
            "PRIVATE_STATE_INFLUENCES_PUBLIC_READINESS",
        )

    @helper_coverage("execution.requirement.depends-on")
    def test_public_readiness_depends_on_execution_completion(self) -> None:
        statement = "Public readiness depends on execution completion."
        self.assertEqual(
            semantics.private_semantic_category("private execution", statement),
            "PRIVATE_EXECUTION_REQUIREMENT",
        )
        self.assertTrue(
            semantics.is_unsafe_private_statement(
                statement, semantics.PRIVATE_ENTITY_PATTERNS["private execution"]
            )
        )

    @helper_coverage("execution.requirement.required-and-gates")
    def test_execution_completion_is_required_for_ready(self) -> None:
        self.assertEqual(
            semantics.private_semantic_category(
                "private execution",
                "Execution completion is required for Public READY.",
            ),
            "PRIVATE_EXECUTION_REQUIREMENT",
        )
        for statement in (
            "Execution completion gates public READY.",
            "Execution controls public readiness.",
        ):
            with self.subTest(statement=statement):
                self.assertEqual(
                    semantics.private_semantic_category(
                        "private execution", statement
                    ),
                    "PRIVATE_EXECUTION_COMPLETION_GATE",
                )

    @helper_coverage("execution.requirement.waits-for")
    def test_public_visibility_waits_for_execution_completion(self) -> None:
        statement = "Public visibility waits for execution completion."
        self.assertEqual(
            semantics.private_semantic_category("private execution", statement),
            "PRIVATE_EXECUTION_REQUIREMENT",
        )
        self.assertTrue(
            semantics.is_unsafe_private_statement(
                statement,
                semantics.PRIVATE_ENTITY_PATTERNS["private execution"],
            )
        )

    @helper_coverage("execution.pending.precedence")
    def test_pending_execution_keeps_state_partial(self) -> None:
        statement = "Pending execution keeps public state PARTIAL."
        self.assertEqual(
            semantics.private_semantic_category("private execution", statement),
            "PRIVATE_EXECUTION_PENDING_PARTIAL",
        )
        self.assertTrue(
            semantics.is_unsafe_private_statement(
                statement, semantics.PRIVATE_ENTITY_PATTERNS["private execution"]
            )
        )

    @helper_coverage("execution.explicit-non-use")
    def test_legal_explicit_execution_non_use_control(self) -> None:
        statement = (
            "Public READY is determined only by the shared public OPPORTUNITY projection; "
            "execution completion is not used."
        )
        documents = semantics.load_documents(ROOT)
        semantics.insert_before(
            documents,
            semantics.SEMANTIC,
            "Owner-scoped `POSITION_RISK` detail remains a separate",
            statement,
        )
        self.assertFalse(
            semantics.evaluate(documents)["private execution contradiction guard"]
        )

    @helper_coverage("inventory.dedup.same-semantic-record")
    def test_identical_l2_records_deduplicate(self) -> None:
        fingerprint = semantics.coverage_fingerprint("same fixture")
        records = [
            semantics.CoverageRecord(
                item,
                "HELPER_UNIT_TEST" if item == "a" else "NEGATIVE_PROBE",
                "L2_HELPER_INTEGRATION",
                "semantic_evaluate",
                "guard",
                "REJECT",
                "contract_rejection",
                fingerprint,
                item,
            )
            for item in ("a", "b")
        ]
        qualifying, duplicates = semantics.deduplicate_coverage_records(records)
        self.assertEqual(qualifying, ["a"])
        self.assertEqual(duplicates, {"b": "a"})
        reversed_qualifying, reversed_duplicates = (
            semantics.deduplicate_coverage_records(list(reversed(records)))
        )
        self.assertEqual(reversed_qualifying, qualifying)
        self.assertEqual(reversed_duplicates, duplicates)

    @helper_coverage("inventory.dedup.layer-separation")
    def test_l1_and_l2_same_text_count_separately(self) -> None:
        fingerprint = semantics.coverage_fingerprint("same fixture")
        base = dict(
            category="HELPER_UNIT_TEST",
            entrypoint="semantic_evaluate",
            target_guard="guard",
            expected_result="REJECT",
            assertion_objective="contract_rejection",
            fixture_fingerprint=fingerprint,
            source="test",
        )
        records = [
            semantics.CoverageRecord(id="l1", layer="L1_UNIT", **base),
            semantics.CoverageRecord(id="l2", layer="L2_HELPER_INTEGRATION", **base),
            semantics.CoverageRecord(
                id="different-objective",
                layer="L1_UNIT",
                **{**base, "assertion_objective": "different_contract_assertion"},
            ),
        ]
        qualifying, duplicates = semantics.deduplicate_coverage_records(records)
        self.assertEqual(
            set(qualifying), {"l1", "l2", "different-objective"}
        )
        self.assertFalse(duplicates)

    @helper_coverage("inventory.metadata.validation")
    def test_duplicate_fixture_fingerprint_is_reported(self) -> None:
        first = semantics.coverage_fingerprint("Public READY requires execution completion.")
        second = semantics.coverage_fingerprint("public ready requires execution completion")
        self.assertEqual(first, second)

        valid = dict(
            coverage_id="metadata.valid",
            layer="L1_UNIT",
            entrypoint="status_value_tokens",
            target_guard="UNKNOWN_AUTHORIZATION_STATE",
            assertion_objective="preserves_unknown_residue",
            fixture_identity="NOT_AUTHORIZED MAYBE",
            expected_result="UNKNOWN",
        )

        def covered(metadata):
            def method(self):
                return None

            setattr(method, "__fe04e_coverage__", metadata)
            return method

        missing_class = type(
            "MissingMetadata",
            (),
            {"test_missing": covered({})},
        )
        missing_audit = semantics.helper_coverage_audit_from_classes(
            [missing_class], strict=False
        )
        self.assertEqual(missing_audit["metadata_missing"], 1)
        with self.assertRaisesRegex(
            semantics.CoverageMetadataError,
            "ERROR: QUALIFYING_METADATA_MISSING",
        ):
            semantics.helper_coverage_audit_from_classes([missing_class])

        duplicate_class = type(
            "DuplicateCoverageId",
            (),
            {
                "test_first": covered(dict(valid)),
                "test_second": covered(dict(valid)),
            },
        )
        duplicate_audit = semantics.helper_coverage_audit_from_classes(
            [duplicate_class], strict=False
        )
        self.assertEqual(duplicate_audit["duplicate_coverage_ids"], 1)
        with self.assertRaisesRegex(
            semantics.CoverageMetadataError,
            "ERROR: DUPLICATE_COVERAGE_ID",
        ):
            semantics.helper_coverage_audit_from_classes([duplicate_class])

        invalid_layer = dict(valid, coverage_id="metadata.invalid-layer")
        invalid_layer["layer"] = "L9_UNKNOWN"
        missing_objective = dict(valid, coverage_id="metadata.missing-objective")
        del missing_objective["assertion_objective"]
        invalid_class = type(
            "InvalidMetadata",
            (),
            {
                "test_invalid_layer": covered(invalid_layer),
                "test_missing_objective": covered(missing_objective),
            },
        )
        invalid_audit = semantics.helper_coverage_audit_from_classes(
            [invalid_class], strict=False
        )
        self.assertEqual(invalid_audit["invalid_records"], 1)
        self.assertEqual(invalid_audit["metadata_missing"], 1)

    @helper_coverage("inventory.counting.raw-vs-qualifying")
    def test_inventory_raw_total_differs_from_qualifying_total(self) -> None:
        inventory = semantics.build_coverage_inventory(
            static_assertions=1,
            legacy_negative_probes=0,
            helper_test_path=Path(__file__),
        )
        self.assertEqual(
            inventory["raw_total"],
            inventory["qualifying_total"]
            + inventory["duplicate_total"]
            + inventory["non_qualifying_metadata_count"]
            + inventory["invalid_record_count"],
        )
        helper = inventory["categories"]["HELPER_UNIT_TEST"]
        self.assertEqual(helper["raw"], 93)
        self.assertEqual(helper["metadata_complete"], 93)
        self.assertEqual(helper["metadata_missing"], 0)
        self.assertEqual(helper["invalid"], 0)
        skipped = semantics.build_coverage_inventory(
            static_assertions=1,
            legacy_negative_probes=0,
            helper_test_path=Path(__file__),
            include_helper_unit_tests=False,
            include_semantic_probes=False,
        )
        self.assertEqual(skipped["categories"]["HELPER_UNIT_TEST"]["raw"], 0)
        self.assertEqual(skipped["categories"]["NEGATIVE_PROBE"]["raw"], 0)
        self.assertEqual(skipped["categories"]["LEGAL_CONTROL"]["raw"], 0)

    @helper_coverage("inventory.stability.rename-order-path")
    def test_inventory_is_stable_across_two_runs(self) -> None:
        first = semantics.build_coverage_inventory(
            static_assertions=1,
            legacy_negative_probes=0,
            helper_test_path=Path(__file__),
        )
        second = semantics.build_coverage_inventory(
            static_assertions=1,
            legacy_negative_probes=0,
            helper_test_path=Path(__file__),
        )
        self.assertEqual(first["digest"], second["digest"])
        self.assertEqual(first["duplicate_of"], second["duplicate_of"])
        self.assertTrue(first["stability_match"])
        self.assertEqual(
            {
                first["semantic_digest"],
                first["rename_stability_digest"],
                first["reversed_order_digest"],
                first["alternate_path_digest"],
            },
            {first["semantic_digest"]},
        )

        records = list(first["records"])
        variants = [
            list(reversed(records)),
            sorted(records, key=lambda record: (record.category, record.id)),
            sorted(records, key=lambda record: (record.category, record.id), reverse=True),
        ]
        for seed in (20260801, 1156):
            shuffled = list(records)
            random.Random(seed).shuffle(shuffled)
            variants.append(shuffled)
        duplicate_first = sorted(
            records,
            key=lambda record: (
                record.id not in first["duplicate_of"],
                record.id,
            ),
        )
        variants.append(duplicate_first)
        for variant in variants:
            summary = semantics.summarize_coverage_records(variant)
            self.assertEqual(summary["semantic_digest"], first["semantic_digest"])
            self.assertEqual(summary["duplicate_of"], first["duplicate_of"])
            self.assertEqual(summary["qualifying_total"], first["qualifying_total"])

        for display_prefix in (
            "/Users/xuchao/repository",
            "/tmp/worktree-a",
            "/tmp/worktree-b",
            "scripts/renamed-helper.py",
        ):
            path_variant = [
                replace(record, source=f"{display_prefix}/{index}")
                for index, record in enumerate(records, 1)
            ]
            summary = semantics.summarize_coverage_records(path_variant)
            self.assertEqual(summary["semantic_digest"], first["semantic_digest"])
            self.assertEqual(summary["duplicate_of"], first["duplicate_of"])

        rename_metadata = coverage_spec(
            "L1_UNIT",
            "status_value_tokens",
            "UNKNOWN_AUTHORIZATION_STATE",
            "preserves_unknown_residue",
            "NOT_AUTHORIZED MAYBE",
            "UNKNOWN",
        )
        second_rename_metadata = coverage_spec(
            "L1_UNIT",
            "logical_clauses",
            "CLAUSE_ISOLATION",
            "splits_semicolon_boundary",
            "Telegram is disabled; Telegram is active.",
            "PASS",
        )

        def renamed_method(coverage_id, metadata):
            def method(self):
                return None

            return semantics.coverage_case(
                coverage_id=coverage_id,
                **metadata,
            )(method)

        original_class = type(
            "OriginalClass",
            (),
            {
                "test_original_name": renamed_method(
                    "stability.renamed-method", rename_metadata
                ),
                "test_second_original_name": renamed_method(
                    "stability.second-renamed-method", second_rename_metadata
                ),
            },
        )
        renamed_class = type(
            "RenamedClass",
            (),
            {
                "test_completely_different_name": renamed_method(
                    "stability.renamed-method", rename_metadata
                ),
                "test_another_different_name": renamed_method(
                    "stability.second-renamed-method", second_rename_metadata
                ),
            },
        )
        original_audit = semantics.helper_coverage_audit_from_classes(
            [original_class], source_label="/tmp/worktree-a/original.py"
        )
        renamed_audit = semantics.helper_coverage_audit_from_classes(
            [renamed_class], source_label="/Users/xuchao/renamed.py"
        )
        original_summary = semantics.summarize_coverage_records(
            original_audit["records"]
        )
        renamed_summary = semantics.summarize_coverage_records(
            renamed_audit["records"]
        )
        self.assertEqual(
            original_summary["semantic_digest"],
            renamed_summary["semantic_digest"],
        )
        self.assertEqual(
            original_summary["duplicate_of"],
            renamed_summary["duplicate_of"],
        )

    @helper_coverage("inventory.metadata.completeness")
    def test_inventory_ids_are_deterministic_and_unique(self) -> None:
        inventory = semantics.build_coverage_inventory(
            static_assertions=1,
            legacy_negative_probes=0,
            helper_test_path=Path(__file__),
        )
        records = inventory["records"]
        ids = [item.id for item in records]
        self.assertEqual(len(ids), len(set(ids)))
        audit = semantics.helper_unit_coverage_audit(Path(__file__))
        self.assertEqual(audit["raw"], 93)
        self.assertEqual(audit["metadata_complete"], 93)
        self.assertEqual(audit["metadata_missing"], 0)
        self.assertEqual(audit["duplicate_coverage_ids"], 0)
        self.assertEqual(audit["invalid_records"], 0)
        helper_ids = [record.id for record in audit["records"]]
        self.assertEqual(len(helper_ids), len(set(helper_ids)))
        self.assertTrue(all(not Path(record.source).is_absolute() for record in records))
        self.assertTrue(
            all(
                "source" not in record.semantic_record
                and "id" not in record.semantic_record
                for record in records
            )
        )

    @helper_coverage("scope.required-markdown-headings")
    def test_each_required_markdown_scope_fails_when_heading_is_removed(self) -> None:
        base = semantics.load_documents(ROOT)
        for requirement in semantics.REQUIRED_SCOPE_REQUIREMENTS:
            with self.subTest(scope=requirement.name):
                documents = dict(base)
                sections = semantics.find_markdown_sections(
                    documents[requirement.document], requirement.aliases
                )
                self.assertTrue(sections)
                section = sections[0]
                source = documents[requirement.document]
                documents[requirement.document] = (
                    source[: section.heading_start]
                    + "## Unrelated Scope"
                    + source[section.body_start :]
                )
                violations = semantics.required_scope_violations(documents)
                self.assertTrue(
                    any(item.scope == requirement.name for item in violations)
                )

    @helper_coverage("scope.next-task.empty-key")
    def test_empty_next_task_key_fails_closed(self) -> None:
        documents = semantics.load_documents(ROOT)
        documents[semantics.NEXT_TASK] = documents[semantics.NEXT_TASK].replace(
            'module: "FE-04E Message/Push UI Readiness and Governance Re-evaluation"',
            'module: ""',
            1,
        )
        violations = semantics.required_scope_violations(documents)
        self.assertTrue(
            any(
                item.category == "FAIL_CLOSED_MISSING_OR_EMPTY_NEXT_TASK_SCOPE"
                for item in violations
            )
        )

    @helper_coverage("scope.semantic.required-heading-missing")
    def test_required_scope_missing_fails_closed(self) -> None:
        documents = semantics.load_documents(ROOT)
        documents[semantics.SEMANTIC] = documents[semantics.SEMANTIC].replace(
            "## 7. Message And Telegram V2", "## Unrelated Capability", 1
        )
        violations = semantics.required_scope_violations(documents)
        self.assertTrue(
            any(item.category == "FAIL_CLOSED_MISSING_SCOPE" for item in violations)
        )


def main() -> int:
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(
        GovernanceSemanticHelperTest
    )
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    print(f"FE04E_HELPER_UNIT_TESTS: {result.testsRun}")
    print(f"FE04E_HELPER_UNIT_TEST_FAILURES: {len(result.failures)}")
    print(f"FE04E_HELPER_UNIT_TEST_ERRORS: {len(result.errors)}")
    print(f"FE04E_HELPER_UNIT_TEST_SKIPPED: {len(result.skipped)}")
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
