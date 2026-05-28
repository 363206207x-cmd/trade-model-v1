# V1 Drift Guard Checklist

Use this checklist in every PR review, source-of-truth refresh, and new-window migration.

## Required Drift Checks

- Does the PR write docs-only work as production complete?
- Does the PR write a skeleton as production wiring?
- Does the PR write targeted tests as runtime behavior?
- Does the PR write test-only wiring as production?
- Does the PR write review-only output as executable?
- Does the PR write review-only as no output?
- Does the PR write blocked as no useful output?
- Does the PR treat open PR / branch / Issue as merged main?
- Does the PR treat legacy `MarketQuoteClient` / `BinanceMarketQuoteClient` as new scan-chain completion?
- Does the PR treat Display Slots as Watchlist Pool?
- Does the PR bypass Risk Action Guard?
- Does the PR let AI conflict become infinite wait with no downgrade or recovery condition?
- Does the PR let Push Recheck permanently prevent every internal preview?
- Does the PR connect order / execution / auto-trading early?

## Required Answers Before Merge

Each PR review must answer:

- Current merged `main` HEAD?
- Is this PR merged or still open?
- Which business chain step moved?
- Which capability level changed?
- Which user-visible or review-only output improved?
- Which blocked capability remains blocked?
- Does the title overclaim compared with the diff?
- Did source-of-truth docs change when progress language changed?

## Blocking Review Conditions

Pause review if any answer suggests:

- production progress was raised by docs-only work;
- production wiring was claimed from skeleton/test-only work;
- a blocked capability was described as no useful review-only output;
- legacy runtime was used to claim new scan-chain completion;
- P292 or any open PR was counted before merge.
