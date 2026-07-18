# Candidate Promotion Stability Contract

## Runtime Owner

P3-CALL1 uses the existing candidate/AssetState ownership where suitable and a
bounded in-memory `AutoCandidateRegistry` for promoted discovery candidates.

`AUTO_CANDIDATE_PERSISTENCE: RUNTIME_ONLY_UNTIL_REAL_MARKET_TRACE_PHASE`

No table or migration is added.

## Default Stability Rules

- promotion confirmation cycles: 2
- degradation confirmation cycles: 2
- minimum candidate hold: configurable, default 300 seconds
- candidate TTL: configurable, default 3600 seconds
- promotion event cooldown: configurable, default 300 seconds
- retrigger cooldown: configurable, default 900 seconds

One positive observation does not promote. Ordinary weakening does not remove
a candidate before the minimum hold and requires consecutive degradation
confirmation. Hard risk, invalidation, cooling, or confused blocking exits
immediately. TTL expiration exits at the boundary.

The same `evidenceHash` cannot create a duplicate promotion event. Cooldown may
allow the candidate state to be restored while suppressing a repeated
notification event. Every result records status, reason codes, base/effective
profile, profile reasons, frequency-matrix version, evaluation time, and expiry.

All time decisions use an injected `Clock`. No provider, AI, position, order,
or external-message call is performed. Production readiness remains `BLOCKED`.
