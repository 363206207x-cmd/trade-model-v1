# PHASE P251 Accelerated Next Pack Plan

## 1. Phase Positioning

This document defines the next maximum-safe acceleration strategy.

Docs-only gates can be combined into scope packs.

Java skeletons still require one package per review layer.

Production wiring / scheduler / market / score / push / readiness must be separately authorized.

## 2. Recommended Next Packs

Recommended maximum-safe sequence:

- P252 Market-read Java authorization + adapter skeleton pack.
- P253 ScanScore DTO / rule skeleton pack.
- P254 ScanScore calculation review-only skeleton.
- P255 Candidate Attention + Promote To Home docs / Java gate pack.
- P256 Candidate Attention review-only skeleton.
- P257 Opportunity Push authorization + risk guard gate pack.
- P258 Opportunity Push review-only skeleton.
- P259 Readiness / point generation authorization pack.
- P260 Entry / Stop / TP / RR source-owned skeleton.
- P261 Dashboard/API read-only integration pack.
- P262 End-to-end smoke / regression / progress refresh pack.

## 3. Acceleration Rules

- docs-only gates may combine multiple gates.
- Java may only implement one clear layer.
- every Java PR must include targeted tests.
- every production wiring PR must be disabled-by-default.
- pure repetitive closure should not require a standalone PR unless the previous pack was B/C or C risk.

## 4. Conclusion

The remaining package count can likely move from 70-95 down to 45-60.

This acceleration must not weaken fail-closed / review-only / no-auto-trading boundaries.
