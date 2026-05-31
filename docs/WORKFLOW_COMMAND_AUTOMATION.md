# Workflow Command Automation

This document defines the command shortcuts introduced by P291D.

The goal is to reduce repeated copy-paste while preserving P291C workflow enforcement.

## Scripts

| Script | Purpose | Read-only | May merge |
|---|---|---:|---:|
| `scripts/v1-status.sh` | Show current branch, status, recent log, active mainline summary, open PRs, open Issues, and source-of-truth file existence. | yes | no |
| `scripts/v1-pr-review-input.sh <PR_NUMBER>` | Generate PR review input: metadata, changed files, and forbidden path attention. | yes | no |
| `scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"` | Mark ready, squash merge, switch to main, pull, and print final status/log. | no | yes |
| `scripts/v1-safe-check.sh` | Run workflow contract, local diff/status/stat, and forbidden-change check after Codex work. | yes | no |
| `scripts/v1-session-bootstrap.sh` | Print fixed new-window context from source-of-truth files and current git state. | yes | no |
| `scripts/v1-next-pack-context.sh` | Print current HEAD, active mainline/block/level, next required action, source-of-truth files, open PRs, and open Issues. | yes | no |

## Required Usage

For a new window, run:

```bash
bash scripts/v1-session-bootstrap.sh
```

For status checks, run:

```bash
bash scripts/v1-status.sh
```

For PR review, run:

```bash
bash scripts/v1-pr-review-input.sh <PR_NUMBER>
```

For merge and sync after explicit user approval, run:

```bash
bash scripts/v1-merge-sync.sh <PR_NUMBER> "<SUBJECT>"
```

For Codex completion checks, run:

```bash
bash scripts/v1-safe-check.sh
```

For the next package context, run:

```bash
bash scripts/v1-next-pack-context.sh
```

## Merge Safety

`v1-merge-sync.sh` does not decide whether a PR can be merged.

For B/C PRs and C PRs, the user must explicitly say the equivalent of:

```text
同意合并 PR #<PR_NUMBER>
```

before `v1-merge-sync.sh` may be run.

The script checks PR state, mergeability, and CI status before merging.

## Boundaries

These scripts do not authorize:

- Java changes;
- tests or DTO changes;
- dashboard, schema, config, or resource changes;
- runtime/live/external data reads;
- `MarketQuoteClient` / `BinanceMarketQuoteClient` wiring;
- scan output / score / Candidate / Push / Readiness / point generation;
- entry / stop / TP / RR implementation;
- order / execution / auto-trading.

If a script reports failure, do not continue feature work until the failure is understood.
