#!/usr/bin/env python3
"""Standard-library unit tests for the FE-04E governance semantic helper."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent
sys.path.insert(0, str(SCRIPT_DIR))

import check_fe04e_governance_semantics as semantics  # noqa: E402


class GovernanceSemanticHelperTest(unittest.TestCase):
    def test_heading_normalization_removes_numbering_and_punctuation(self) -> None:
        self.assertEqual(
            semantics.normalize_heading("Part A — Telegram（未来扩展）"),
            "telegram 未来扩展",
        )
        self.assertEqual(
            semantics.normalize_heading("3.1: Message / Telegram"),
            "message telegram",
        )

    def test_section_extraction_honors_heading_hierarchy(self) -> None:
        text = "## Telegram\nroot\n### Child\nchild\n## Next\nnext\n"
        sections = semantics.find_markdown_sections(text, ("telegram",))
        self.assertEqual(len(sections), 1)
        self.assertIn("child", sections[0].body)
        self.assertNotIn("next", sections[0].body)

    def test_section_extraction_returns_multiple_alias_matches(self) -> None:
        text = "## Telegram\ndisabled\n### Telegram Status\nblocked\n## Next\n"
        self.assertEqual(
            len(semantics.find_markdown_sections(text, ("telegram",))), 2
        )

    def test_empty_section_is_observable(self) -> None:
        sections = semantics.find_markdown_sections(
            "## Telegram\n\n## Next\nbody\n", ("telegram",)
        )
        self.assertEqual(len(sections), 1)
        self.assertEqual(sections[0].body, "")

    def test_all_value_aggregation_rejects_safe_and_dangerous(self) -> None:
        declarations = [
            semantics.StatusDeclaration(
                "telegram", semantics.StatusClass.SAFE, "disabled", "a", "s", "safe"
            ),
            semantics.StatusDeclaration(
                "telegram",
                semantics.StatusClass.DANGEROUS,
                "enabled",
                "b",
                "s",
                "danger",
            ),
        ]
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(violations[0].category, "CONTRADICTORY_AUTHORIZATION")

    def test_unknown_plus_safe_fails_closed(self) -> None:
        declarations = [
            semantics.StatusDeclaration(
                "telegram", semantics.StatusClass.SAFE, "disabled", "a", "s", "safe"
            ),
            semantics.StatusDeclaration(
                "telegram", semantics.StatusClass.UNKNOWN, "maybe", "a", "s", "maybe"
            ),
        ]
        violations = semantics.aggregate_authorization_violation(
            "telegram", "guard", declarations
        )
        self.assertEqual(
            violations[0].category, "UNKNOWN_OR_AMBIGUOUS_AUTHORIZATION"
        )

    def test_negated_authorization_is_safe_only(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "telegram", "Telegram is not authorized."
        )
        self.assertEqual(
            {item.classification for item in declarations},
            {semantics.StatusClass.SAFE},
        )

    def test_nested_yaml_status_is_dangerous(self) -> None:
        declarations = semantics.yaml_status_declarations(
            "telegram", "fixture.yml", "telegram:\n  status: implemented\n"
        )
        self.assertIn(
            semantics.StatusClass.DANGEROUS,
            {item.classification for item in declarations},
        )

    def test_markdown_prose_keeps_both_statuses(self) -> None:
        declarations = []
        for unit in semantics.statement_units(
            "Telegram remains prohibited. Telegram is enabled."
        ):
            declarations.extend(
                semantics.status_declarations_for_text("telegram", unit)
            )
        self.assertEqual(
            {item.classification for item in declarations},
            {semantics.StatusClass.SAFE, semantics.StatusClass.DANGEROUS},
        )

    def test_markdown_table_status_is_dangerous(self) -> None:
        declarations = semantics.status_declarations_for_text(
            "external notification", "| External Notification | Active |"
        )
        self.assertIn(
            semantics.StatusClass.DANGEROUS,
            {item.classification for item in declarations},
        )

    def test_position_direction_equivalent_is_unsafe(self) -> None:
        self.assertTrue(
            semantics.is_unsafe_private_statement(
                "Long, short, or no position changes public lifecycle.",
                semantics.PRIVATE_ENTITY_PATTERNS["UserPosition/private risk"],
            )
        )

    def test_execution_gating_equivalent_is_unsafe(self) -> None:
        self.assertTrue(
            semantics.is_unsafe_private_statement(
                "COMPLETED is required for public READY.",
                semantics.PRIVATE_ENTITY_PATTERNS["private execution"],
            )
        )

    def test_private_risk_equivalent_is_unsafe(self) -> None:
        self.assertTrue(
            semantics.is_unsafe_private_statement(
                "Account risk supplements public status.",
                semantics.PRIVATE_ENTITY_PATTERNS["UserPosition/private risk"],
            )
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

    def test_position_risk_private_recheck_control_is_scope_aware(self) -> None:
        documents = semantics.load_documents(ROOT)
        semantics.insert_before(
            documents,
            semantics.SEMANTIC,
            "## 8. Search Asset V2",
            "Private Recheck is permitted only inside owner-scoped POSITION_RISK state resolution.",
        )
        results = semantics.evaluate(documents)
        self.assertFalse(results["private Recheck contradiction guard"])

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
