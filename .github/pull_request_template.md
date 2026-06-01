# Pull Request Checklist

Default workflow is GPT + Codex + GitHub-native.
Terminal scripts are fallback only except local main sync after merge.
Codex must output PR number and stop.

Mainline（主线）:

Block（模块）:

Capability Level Before（修改前能力层级）:

Capability Level After（修改后能力层级）:

User-visible Output Improved?（是否改善用户可见输出）:

Source of Truth Updated?（是否更新真相源）:

Overreach Check（越界检查）:

- [ ] No unauthorized Java / tests / DTO changes
- [ ] No dashboard / schema / config change unless authorized
- [ ] No runtime/live/external data read unless authorized
- [ ] No MarketQuoteClient / BinanceMarketQuoteClient wiring unless authorized
- [ ] No scan output / score / Candidate / Push / Readiness / point / trading path unless authorized
- [ ] No order / execution / auto-trading

Review-only Output Preserved?（是否保留只读输出）:

Blocked Actions Preserved?（是否保留禁止动作）:

CI / Test（测试）:
