# V1 Workflow Optimization Rules

P291A resets the work style from boundary-only repetition to business-chain MAX_SAFE_PACK progression.

P291C enforces that reset with `docs/SESSION_BOOTSTRAP.md`, `docs/ACTIVE_MAINLINE_STATUS.yml`, `docs/ANSWER_FORMAT_CONTRACT.md`, `.github/pull_request_template.md`, `scripts/check-workflow-contract.sh`, and `.github/workflows/workflow-contract.yml`.

P291D adds command automation through `docs/WORKFLOW_COMMAND_AUTOMATION.md` and the `scripts/v1-*.sh` helpers.

## Core Rules

1. Business-chain MAX_SAFE_PACK comes first.
2. Closure-only loops are no longer a default route.
3. Docs-only closure plus next authorization should be merged into one scope pack when the risk level does not change.
4. Every skeleton pack must include targeted tests and a closure document.
5. After a skeleton pack, the default next step is test-only wiring.
6. After two consecutive docs-only packs, the next pack must raise a capability level unless the work crosses a risk boundary.
7. Every 8 to 10 PRs require a Global Audit or source-of-truth refresh.
8. Every progress answer must cite the progress source of truth.
9. Every PR review must check whether at least one capability level increased.
10. Every new window must start from `docs/SESSION_BOOTSTRAP.md`.
11. Every status answer must follow `docs/ANSWER_FORMAT_CONTRACT.md`.
12. Open PR / branch / Issue / Codex output does not count as done; merged main only.

## Low-Value Loop Stop Rules

Stop and regroup when a proposed package is only:

- closure of a closure;
- another broad blocked-list restatement;
- an authorization gate for a target already authorized;
- a marker file plus status update without capability change;
- a skeleton split away from its targeted test;
- a still-blocked doc without an allowed review-only output path.

## Progress Rules

Do not use P-package count as progress.

Do not raise production progress because a docs-only gate merged.

Do not raise runtime progress because a skeleton merged.

Do raise governance or skeleton/test progress when the package genuinely improves those layers.

If `bash scripts/check-workflow-contract.sh` fails, stop and fix workflow enforcement before continuing feature work.

## Workflow Command Automation

- 新窗口优先运行 `bash scripts/v1-session-bootstrap.sh`
- 状态检查优先运行 `bash scripts/v1-status.sh`
- 审 PR 优先运行 `bash scripts/v1-pr-review-input.sh <PR_NUMBER>`
- 合并同步优先运行 `bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"`
- Codex 完成后优先运行 `bash scripts/v1-safe-check.sh`

## Business Chain Priority

Future work should prefer this order:

1. Watchlist candidate source
2. Market read request wiring
3. Market read adapter
4. scan output
5. review-only ScanScore
6. review-only Candidate
7. internal Opportunity Push preview
8. Push Recheck
9. review-only Execution Advice
10. entry / stop / TP / RR proposal
11. manual position entry
12. position monitoring
13. AI conflict downgrade
14. dashboard MVP smoke
15. review / missed-valid logging
