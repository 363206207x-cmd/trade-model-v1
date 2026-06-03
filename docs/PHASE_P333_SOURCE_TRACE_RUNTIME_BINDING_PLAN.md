# Phase P333 SourceTrace Runtime / Source Binding Plan

## Purpose / 目的

P333 belongs to the Readiness / Point Mainline.

P333 follows P332 `SOURCETRACE_NUMERIC_SOURCE_ASSEMBLER_VERIFICATION`, which was merged as a docs-only verification closure for P331.

P333 is a docs-only skeleton plan for future SourceTrace runtime / source binding.

P333 defines the maximum safe planning package before any runtime source binding work may be considered.

P333 does not wire runtime code.

P333 不接运行链路。

P333 只定义未来 SourceTrace runtime / source binding 的安全方案边界。

P333 不读取真实行情、不接外部来源、不生成真实点位。

## Capability Movement / 能力移动

`SOURCETRACE_NUMERIC_SOURCE_ASSEMBLER_VERIFICATION -> SOURCE_TRACE_RUNTIME_BINDING_PLAN`

This movement is docs-only.

该能力移动仅为文档层级。

It verifies that the project may plan future source binding only after P331 assembler and P332 verification are complete.

它确认只有在 P331 组装器骨架和 P332 验证收口完成后，才可以进入“未来来源绑定方案”的规划。

## Verification Scope / 验证范围

P333 verifies the following current boundary:

- `SourceTraceNumericSourceContextDTO` exists as a carrier only;
- `SourceTraceNumericSourceReadModelValidator` exists as a validator only;
- `SourceTraceNumericSourceReadModelAssembler` exists as an explicit-input assembler only;
- P332 verified that P331 only moves explicit `AssemblyInput` fields into the DTO;
- P332 verified that P331 immediately calls the validator;
- P332 verified that P331 returns both `context` and `validationResult`;
- P332 verified null input remains `INCOMPLETE`;
- P332 verified `BLOCKED_FAIL_CLOSED` remains fail-closed;
- P332 verified degraded status remains review-only degraded;
- P332 verified review-only status remains review-only;
- P332 verified forbidden source types remain rejected by validator;
- P332 verified safety flags remain required true;
- P332 verified `riskActionGuardRef` remains required by validation;
- P332 verified missing / stale / unknown conditions remain incomplete-safe;
- P332 verified explicitly provided `BigDecimal` values are only preserved, not calculated;
- P332 verified safe outputs do not produce executable semantics.

P333 验证当前边界仍然只是：

- DTO 承载；
- validator 校验；
- assembler 显式字段搬运；
- 不读取真实 runtime；
- 不生成真实 entry / stop / TP / RR。

## Runtime / Source Binding Plan Boundary / 运行来源绑定方案边界

P333 may define future planning requirements for source binding:

- what a future SourceTrace runtime/source binding plan must prove;
- how source identifiers may be bound to pre-existing upstream evidence;
- why every source must keep `reviewOnly = true`;
- why every source must keep `notTradeInstruction = true`;
- why every source must keep `manualReviewRequired = true`;
- why every runtime-bound source must remain incomplete-safe;
- why `riskActionGuardRef` remains required before numeric source review can continue;
- why forbidden source types must remain blocked;
- why source binding must not bypass the validator.

P333 可以定义未来方案要求，但不能实现这些要求。

P333 只能说明未来如何规划 SourceTrace runtime/source binding，不能实际接入 runtime。

## Explicit Non-Scope / 非范围说明

P333 does not modify:

- `src/main/java`;
- `src/test/java`;
- `src/main/resources`;
- schema;
- config;
- `pom.xml`;
- dashboard;
- service;
- controller;
- mapper;
- repository;
- scheduler.

P333 does not connect:

- RuntimeKlineContext runtime;
- DataQuality runtime;
- MultiTimeframe runtime;
- RiskActionGuard runtime;
- WatchlistPoolProof;
- market data;
- latest price;
- latest close;
- external provider;
- push send;
- order;
- execution;
- auto-trading.

P333 does not generate:

- executable point values;
- entry;
- stop;
- TP;
- RR;
- final direction;
- trading action.

P333 不修改 Java / test / resources / schema / config / pom / dashboard。

P333 不创建 service / controller / mapper / repository / scheduler。

P333 不接 RuntimeKlineContext / DataQuality / MultiTimeframe / RiskActionGuard / WatchlistPoolProof。

P333 不读行情、不读 latest price、不读 latest close、不接外部 provider。

P333 不生成 entry / stop / TP / RR，不生成 final direction，不接 push / order / execution / auto-trading。

## Safety Rules Verified / 安全规则验证

P333 preserves these safety rules:

- SourceTrace remains a source identity, not a point generator;
- source binding cannot bypass `SourceTraceNumericSourceReadModelValidator`;
- source binding cannot bypass Risk Action Guard requirements;
- forbidden source types remain forbidden;
- missing source binding remains `INCOMPLETE`;
- stale source binding remains `INCOMPLETE`;
- unknown source binding remains `INCOMPLETE`;
- forged or untrusted source binding must remain `BLOCKED_FAIL_CLOSED`;
- executable semantics must remain `BLOCKED_FAIL_CLOSED`;
- explicitly provided `BigDecimal` values may be carried only as source-owned review-only data;
- no value may be derived from latest price, latest close, score, label, AI prose, dashboard text, or external provider direct read;
- all output must stay review-only, not a trade instruction, manual-review required, and incomplete-safe.

P333 保留以下安全规则：

- SourceTrace 只是来源身份，不是点位生成器；
- 来源绑定不能绕过 validator；
- 来源绑定不能绕过 Risk Action Guard；
- forbidden source type 必须继续 fail-closed；
- 缺失 / stale / unknown 必须继续 incomplete-safe；
- forged / untrusted 必须 fail-closed；
- 可执行交易语义必须 fail-closed；
- 显式 BigDecimal 只能作为只读来源字段被承载；
- 不得从 latest price / latest close / score / label / AI 文案 / dashboard 文案 / 外部 provider 推导数值；
- 所有输出必须保持 review-only、notTradeInstruction、manualReviewRequired、incompleteSafe。

## Current Capability After P333 / P333 后当前能力

After P333, the chain still only has:

- SourceTrace DTO carrier;
- SourceTrace validator skeleton;
- SourceTrace explicit-input assembler skeleton;
- docs-only verification closure;
- docs-only runtime/source binding plan boundary.

P333 后仍然只有：

- SourceTrace DTO 承载器；
- SourceTrace validator 骨架；
- SourceTrace 显式输入 assembler 骨架；
- docs-only 验证收口；
- docs-only runtime/source binding 方案边界。

It still cannot:

- read real market data;
- read real SourceTrace runtime;
- bind real source context;
- connect RuntimeKlineContext runtime;
- connect DataQuality runtime;
- connect MultiTimeframe runtime;
- connect RiskActionGuard runtime;
- connect WatchlistPoolProof;
- generate real entry / stop / TP / RR;
- generate executable point values;
- send external push;
- place orders;
- execute;
- auto-trade.

它仍然不能读取真实行情、不能读取真实 SourceTrace runtime、不能绑定真实 source context、不能生成真实点位、不能推送、不能下单、不能执行、不能自动交易。

## Validation / 验证

Required validation for P333:

- `bash scripts/check-workflow-contract.sh`;
- `git diff --name-only`;
- `git diff --check`.

P333 is docs-only. Maven is not required unless a repository rule changes.

P333 是 docs-only；除非仓库规则变化，否则不需要 Maven。

## Next Safe Package / 下一安全包

The fixed next safe package after P333 should be P334 - SourceTrace Runtime / Source Binding Verification.

P333 后固定的下一安全包应为 P334 - SourceTrace 运行时 / 来源绑定验证收口。

The next package should still not implement runtime wiring.

下一包仍不应直接实现 runtime wiring。

Do not jump directly to:

- RuntimeKlineContext runtime wiring;
- DataQuality runtime wiring;
- MultiTimeframe runtime wiring;
- RiskActionGuard runtime wiring;
- WatchlistPoolProof runtime wiring;
- service runtime;
- dashboard runtime;
- executable point generation;
- external push;
- order / execution / auto-trading.

不要直接跳到 RuntimeKlineContext / DataQuality / MultiTimeframe / RiskActionGuard / WatchlistPoolProof 运行接入，不要直接接 service / dashboard，不要生成可执行点位，不要接外部推送或交易执行。
