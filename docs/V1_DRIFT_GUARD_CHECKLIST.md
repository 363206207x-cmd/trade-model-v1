# V1 Drift Guard Checklist

Use this checklist in every PR review and every progress refresh.

First read `docs/SESSION_BOOTSTRAP.md` and `docs/ACTIVE_MAINLINE_STATUS.yml`.

## Required Checks

- Does the PR write docs-only work as production complete?
- Does the PR write a skeleton as production wiring?
- Does the PR write review-only output as executable?
- Does the PR write blocked as no useful output?
- Does the PR ignore `docs/ANSWER_FORMAT_CONTRACT.md`?
- Does the PR skip `scripts/check-workflow-contract.sh`?
- Does the PR treat legacy MarketQuoteClient / BinanceMarketQuoteClient as new scan-chain completion?
- Does the PR treat Display Slots as Watchlist Pool?
- Does the PR bypass Risk Action Guard?
- Does the PR let AI conflict become infinite wait with no downgrade or recovery condition?
- Does the PR let Push Recheck permanently prevent every push preview?
- Does the PR connect order / execution / auto-trading early?

## Required Answers Before Merge

Each PR review must answer:

- Which capability level increased?
- Which business-chain step moved?
- Which allowed review-only output became clearer or more usable?
- Which blocked capabilities remain blocked by reference to `docs/V1_BLOCKED_CAPABILITY_REGISTRY.md`?
- Does the title overclaim compared with the diff?
- Does the PR update source-of-truth docs if it changes progress language?
- Does `bash scripts/check-workflow-contract.sh` output `WORKFLOW_CONTRACT_OK`?
