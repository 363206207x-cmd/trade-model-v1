#!/usr/bin/env python3
"""Standard-library unit tests for the FE-04E governance semantic helper."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from typing import List, Optional


SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent
sys.path.insert(0, str(SCRIPT_DIR))

import check_fe04e_governance_semantics as semantics  # noqa: E402


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

    def test_heading_normalization_removes_numbering_and_punctuation(self) -> None:
        self.assertEqual(
            semantics.normalize_heading("Part A — Telegram（未来扩展）"),
            "telegram 未来扩展",
        )
        self.assertEqual(
            semantics.normalize_heading("3.1: Message / Telegram"),
            "message telegram",
        )

    def test_production_alias_discovers_bare_telegram_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "## Telegram\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual([item.title for item in sections], ["Telegram"])

    def test_production_alias_discovers_chinese_status_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "### Telegram 状态\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual([item.title for item in sections], ["Telegram 状态"])

    def test_production_alias_discovers_future_extension_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "### Telegram（未来扩展）\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual([item.title for item in sections], ["Telegram（未来扩展）"])

    def test_production_alias_discovers_numbered_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "#### 3.1 Telegram Authorization\nTelegram remains prohibited.\n",
            "telegram",
        )
        self.assertEqual(len(sections), 1)
        self.assertEqual(sections[0].normalized_title, "telegram authorization")

    def test_production_alias_discovers_step_prefixed_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "## STEP 4 — Telegram\nTelegram remains prohibited.\n", "telegram"
        )
        self.assertEqual(len(sections), 1)

    def test_production_alias_discovers_bilingual_heading(self) -> None:
        sections = semantics.production_scope_sections(
            "### 通知 / Telegram（未来扩展）\nTelegram remains prohibited.\n",
            "telegram",
        )
        self.assertEqual(len(sections), 1)

    def test_section_extraction_honors_heading_hierarchy(self) -> None:
        text = "## Telegram\nroot\n### Child\nchild\n## Next\nnext\n"
        sections = semantics.production_scope_sections(text, "telegram")
        self.assertEqual(len(sections), 1)
        self.assertIn("child", sections[0].body)
        self.assertNotIn("next", sections[0].body)

    def test_production_alias_returns_multiple_telegram_sections(self) -> None:
        text = "## Telegram\ndisabled\n### Telegram Status\nblocked\n## Next\n"
        self.assertEqual(
            len(semantics.production_scope_sections(text, "telegram")), 2
        )

    def test_empty_section_is_observable(self) -> None:
        sections = semantics.production_scope_sections(
            "## Telegram\n\n## Next\nbody\n", "telegram"
        )
        self.assertEqual(len(sections), 1)
        self.assertEqual(sections[0].body, "")

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

    def test_bare_telegram_heading_detects_enabled(self) -> None:
        violations = self._telegram_violations("Telegram", "Telegram is enabled.")
        self.assertEqual(violations[0].category, "telegram authorization")

    def test_chinese_status_heading_detects_enabled(self) -> None:
        violations = self._telegram_violations(
            "Telegram 状态", "Telegram is enabled."
        )
        self.assertEqual(violations[0].category, "telegram authorization")

    def test_future_extension_heading_detects_implemented(self) -> None:
        violations = self._telegram_violations(
            "Telegram（未来扩展）", "Telegram is implemented."
        )
        self.assertEqual(violations[0].category, "telegram authorization")

    def test_numbered_heading_detects_active(self) -> None:
        violations = self._telegram_violations(
            "3. Telegram Authorization", "Telegram is active."
        )
        self.assertEqual(violations[0].category, "telegram authorization")

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

    def test_telegram_prohibited_prose_is_safe(self) -> None:
        self.assertFalse(
            self._telegram_violations("Telegram", "Telegram remains prohibited.")
        )

    def test_telegram_not_authorized_is_safe(self) -> None:
        self.assertFalse(
            self._telegram_violations("Telegram", "Telegram is not authorized.")
        )

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

    def test_comma_compound_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("telegram", "DISABLED, ENABLED")
        self.assertEqual(
            [item.matched_token for item in declarations], ["DISABLED", "ENABLED"]
        )

    def test_slash_compound_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("telegram", "BLOCKED / ACTIVE")
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    def test_pipe_compound_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("trading", "NONE | LIMITED")
        self.assertEqual(
            [item.matched_token for item in declarations], ["NONE", "LIMITED"]
        )

    def test_english_conjunction_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_IMPLEMENTED but AUTHORIZED"
        )
        self.assertEqual(
            [item.classification for item in declarations],
            [semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS],
        )

    def test_chinese_punctuation_collects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations("telegram", "未授权，但已启用")
        self.assertEqual(
            [item.matched_token for item in declarations], ["未授权", "已启用"]
        )

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

    def test_markdown_table_collects_compound_statuses(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "| Telegram | NOT_AUTHORIZED + ACTIVE |"
        )
        self.assertEqual(
            [item.matched_token for item in declarations],
            ["NOT_AUTHORIZED", "ACTIVE"],
        )
        self.assertTrue(all(item.source_kind == "TABLE" for item in declarations))

    def test_all_value_aggregation_rejects_safe_and_dangerous(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "NOT_AUTHORIZED + AUTHORIZED"
        )
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "CONTRADICTORY_AUTHORIZATION")

    def test_all_value_aggregation_rejects_dangerous_then_safe(self) -> None:
        declarations = self._yaml_declarations(
            "telegram", "AUTHORIZED then NOT_AUTHORIZED"
        )
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "CONTRADICTORY_AUTHORIZATION")

    def test_multiple_dangerous_values_are_all_retained(self) -> None:
        declarations = self._yaml_declarations("telegram", "ENABLED + ACTIVE")
        self.assertEqual(
            [item.matched_token for item in declarations], ["ENABLED", "ACTIVE"]
        )
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "telegram authorization")

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
            violations[0].category, "UNKNOWN_OR_AMBIGUOUS_AUTHORIZATION"
        )

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

    def test_negated_authorization_is_safe_only(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "Telegram is not authorized."
        )
        self.assertEqual(
            {item.classification for item in declarations},
            {semantics.StatusClass.SAFE},
        )

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

    def test_execution_fallback_equivalent_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Execution is used as fallback.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_FALLBACK")

    def test_pending_execution_equivalent_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Pending execution makes public PARTIAL.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_PENDING_PARTIAL")

    def test_completed_execution_gate_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "COMPLETED is required for public READY.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_COMPLETION_GATE")

    def test_failed_execution_mapping_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "FAILED produces public ERROR.",
            "private execution contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_EXECUTION_FAILURE_ERROR")

    def test_risk_context_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Risk context determines public readiness.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

    def test_account_risk_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Account risk supplements public status.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

    def test_position_risk_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Position risk refines public evaluation.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

    def test_english_position_direction_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Long, short, or no position changes public lifecycle.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    def test_english_holding_direction_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "Holding direction determines public evaluation.",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    def test_chinese_long_position_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "多仓改变公共状态。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    def test_chinese_short_position_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "空仓影响公开生命周期。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    def test_chinese_no_position_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "无仓决定公开就绪状态。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    def test_chinese_position_direction_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "持仓方向参与公开评估。", "UserPosition/private risk contradiction guard"
        )
        self.assertEqual(
            violation.category, "POSITION_DIRECTION_PUBLIC_STATE_DEPENDENCY"
        )

    def test_chinese_private_risk_is_rejected_in_public_scope(self) -> None:
        violation = self._public_violation(
            "私有风险虽不展示但参与计算。",
            "UserPosition/private risk contradiction guard",
        )
        self.assertEqual(violation.category, "PRIVATE_RISK_PUBLIC_STATE_DEPENDENCY")

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

    def test_same_private_recheck_control_fails_in_public_scope(self) -> None:
        violation = self._public_violation(
            semantics.POSITION_RISK_PRIVATE_RECHECK_CONTROL,
            "private Recheck contradiction guard",
        )
        self.assertEqual(
            violation.category, "PRIVATE_RECHECK_PUBLIC_STATE_DEPENDENCY"
        )

    def test_next_task_mutation_delivery_is_dangerous(self) -> None:
        self.assertEqual(
            semantics.classify_next_task_statement(
                "Message/Push UI add mutation and automatic delivery"
            ),
            semantics.StatusClass.DANGEROUS,
        )

    def test_next_task_review_mutation_prohibition_is_safe(self) -> None:
        self.assertEqual(
            semantics.classify_next_task_statement("Review mutation prohibition"),
            semantics.StatusClass.SAFE,
        )

    def test_next_task_verify_no_delivery_is_safe(self) -> None:
        self.assertEqual(
            semantics.classify_next_task_statement("Verify no automatic delivery"),
            semantics.StatusClass.SAFE,
        )

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

    def test_generic_capability_movement_collects_every_value(self) -> None:
        declarations = semantics.trading_movement_values(
            "CAPABILITY_MOVEMENT: NONE; CAPABILITY_MOVEMENT: EXPANDED"
        )
        self.assertEqual([item.value for item in declarations], ["none", "expanded"])

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
