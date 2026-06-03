# Phase P335 RuntimeKlineContext Source Binding Plan

## Purpose / 目的

P335 defines the docs-only plan for future RuntimeKlineContext source binding.

P335 定义未来 RuntimeKlineContext 来源绑定的 docs-only 方案。

It follows P334 `SOURCE_TRACE_RUNTIME_BINDING_VERIFICATION`.

它承接 P334 `SOURCE_TRACE_RUNTIME_BINDING_VERIFICATION`。

## Capability Movement / 能力移动

`SOURCE_TRACE_RUNTIME_BINDING_VERIFICATION -> RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_PLAN`

This is a docs-only plan movement.

这是 docs-only 方案层级移动。

## Scope / 范围

P335 may describe:

- RuntimeKlineContext responsibility boundary;
- chain position after SourceTrace numeric source read model;
- minimum future RuntimeKlineContext fields;
- SourceTrace ref binding rules;
- latestPrice / latestClose boundaries;
- OHLCV completeness rules;
- wick / gap / liquidity / stampede risk rules;
- `INCOMPLETE` rules;
- `BLOCKED_FAIL_CLOSED` rules;
- future Java split.

P335 可以描述：

- RuntimeKlineContext 职责边界；
- SourceTrace numeric source read model 之后的链路位置；
- 未来 RuntimeKlineContext 最小字段；
- SourceTrace ref 绑定规则；
- latestPrice / latestClose 边界；
- OHLCV 完整性规则；
- wick / gap / liquidity / stampede 风险规则；
- `INCOMPLETE` 规则；
- `BLOCKED_FAIL_CLOSED` 规则；
- 后续 Java 拆包。

## Explicit Non-Scope / 明确非范围

P335 does not:

- write Java;
- add tests;
- add DTOs;
- add validators;
- add assemblers;
- connect services;
- connect controllers;
- connect mappers;
- connect repositories;
- connect schedulers;
- modify resources;
- modify schema / config / pom;
- modify dashboard;
- read market data;
- read latest price;
- read latest close;
- connect external providers;
- connect DataQualityContext;
- connect MultiTimeframeContext;
- connect RiskActionGuardContext;
- connect WatchlistPoolProof;
- generate real entry / stop / TP / RR;
- generate final direction;
- connect external channel;
- connect Push;
- connect order / execution / auto-trading.

P335 不：

- 写 Java；
- 新增测试；
- 新增 DTO；
- 新增 validator；
- 新增 assembler；
- 接 service；
- 接 controller；
- 接 mapper；
- 接 repository；
- 接 scheduler；
- 修改 resources；
- 修改 schema / config / pom；
- 修改 dashboard；
- 读取行情；
- 读取 latest price；
- 读取 latest close；
- 接外部 provider；
- 接 DataQualityContext；
- 接 MultiTimeframeContext；
- 接 RiskActionGuardContext；
- 接 WatchlistPoolProof；
- 生成真实 entry / stop / TP / RR；
- 生成 final direction；
- 接 external channel；
- 接 Push；
- 接 order / execution / auto-trading。

## Safety Rules / 安全规则

RuntimeKlineContext must remain K-line scene evidence only.

RuntimeKlineContext 必须只作为 K 线现场证据。

It cannot become a point generator.

它不能成为点位生成器。

It cannot bypass SourceTrace, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, or NumericPointSafetyValidator.

它不能绕过 SourceTrace、DataQualityContext、MultiTimeframeContext、RiskActionGuardContext 或 NumericPointSafetyValidator。

latestPrice-only and latestClose-only remain `INCOMPLETE`.

latestPrice-only 和 latestClose-only 仍为 `INCOMPLETE`。

forged evidence, severe gap, confirmed stampede, executable semantics, and safety-flag bypass remain `BLOCKED_FAIL_CLOSED`.

forged evidence、severe gap、confirmed stampede、可执行语义和 safety flag 绕过仍为 `BLOCKED_FAIL_CLOSED`。

## Next Safe Package / 下一安全包

The next safe package should be `RuntimeKlineContextSourceBindingDTO Java Skeleton`.

下一安全包建议为 `RuntimeKlineContextSourceBindingDTO Java Skeleton`。

Do not jump directly to service runtime, dashboard runtime, DataQualityContext binding, external push, order, execution, auto-trading, or real point generation.

不要直接跳到 service runtime、dashboard runtime、DataQualityContext binding、external push、order、execution、auto-trading 或真实点位生成。
