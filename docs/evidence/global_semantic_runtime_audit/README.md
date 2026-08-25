# Global Semantic Runtime Audit Evidence

Runtime date: 2026-08-21
Profile: normal smoke plus isolated `ui-review` visual fixture
Java: 17
External providers, AI, Telegram, and schedulers: disabled

| File | Evidence |
|---|---|
| `fundamental-ai-global-audit-home-1440.png` | Home first viewport, 1440 x 900 |
| `fundamental-ai-global-audit-home-full.png` | Home full page, 1440 wide |
| `fundamental-ai-global-audit-home-1280.png` | responsive Home, 1280 x 800 |
| `fundamental-ai-global-audit-home-1080.png` | responsive Home, 1080 x 800 |
| `fundamental-ai-global-audit-gemini.png` | Gemini independent collection groups |
| `fundamental-ai-global-audit-grok.png` | Grok failure-path-first hierarchy |
| `fundamental-ai-global-audit-position-pending.png` | PENDING fail-closed row |
| `fundamental-ai-global-audit-position-stale.png` | STALE fail-closed row |
| `fundamental-ai-global-audit-position-invalid.png` | INVALID fail-closed row |
| `fundamental-ai-global-audit-position-source-unavailable.png` | SOURCE_UNAVAILABLE fail-closed row |

Normal mode returned authenticated `/dashboard` and `/api/dashboard/home`
HTTP 200 and contained no UI-review marker. The controlled fixture is guarded
by the existing `ui-review` profile and was used only for the screenshots.

See `browser-qa.json` for the measured DOM results.
