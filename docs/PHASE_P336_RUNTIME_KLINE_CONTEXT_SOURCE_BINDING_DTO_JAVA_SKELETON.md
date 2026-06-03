# Phase P336 RuntimeKlineContextSourceBindingDTO Java Skeleton

## Purpose / 目的

P336 adds the first Java DTO carrier for future RuntimeKlineContext source binding.

P336 新增未来 RuntimeKlineContext source binding 的第一个 Java DTO 承载器。

It follows P335 `RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_PLAN`.

它承接 P335 `RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_PLAN`。

## Capability Movement / 能力移动

`RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_PLAN -> RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_DTO_JAVA_SKELETON`

This is a Java/test DTO skeleton movement.

这是 Java/test DTO skeleton 层级移动。

## Implemented Scope / 已实现范围

P336 adds:

- `RuntimeKlineContextSourceBindingDTO`;
- `RuntimeKlineContextSourceBindingDTOTest`;
- safety flag tests;
- enum coverage tests;
- defensive-copy tests for `sourceTraceRefs`;
- dependency / annotation guard tests;
- forbidden executable semantics output tests.

P336 新增：

- `RuntimeKlineContextSourceBindingDTO`；
- `RuntimeKlineContextSourceBindingDTOTest`；
- safety flag 测试；
- enum 覆盖测试；
- `sourceTraceRefs` 防御性复制测试；
- dependency / annotation guard 测试；
- forbidden executable semantics 输出测试。

## Explicit Non-Scope / 明确非范围

P336 does not add validator, assembler, service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, external channel, Push send, order, execution, or auto-trading.

P336 不新增 validator、assembler、service、controller、mapper、repository、scheduler、resources、schema、config、pom、dashboard、external channel、Push send、order、execution 或 auto-trading。

P336 does not read market data, latest price, latest close, exchanges, or external providers.

P336 不读取 market data、latest price、latest close、交易所或 external provider。

P336 does not generate real entry / stop / TP / RR and does not calculate RR.

P336 不生成真实 entry / stop / TP / RR，也不计算 RR。

## Safety Confirmation / 安全确认

All DTO factories force:

- `reviewOnly = true`;
- `notTradeInstruction = true`;
- `manualReviewRequired = true`;
- `incompleteSafe = true`.

所有 DTO factories 强制：

- `reviewOnly = true`；
- `notTradeInstruction = true`；
- `manualReviewRequired = true`；
- `incompleteSafe = true`。

`BLOCKED_FAIL_CLOSED` keeps `failClosed = true`.

`BLOCKED_FAIL_CLOSED` 保持 `failClosed = true`。

Runtime kline values are carried only when explicitly provided.

runtime kline 数值只承载显式传入字段。

They are not point values and are not executable outputs.

它们不是点位，也不是可执行输出。

## Next Safe Package / 下一安全包

Next safe package should be `RuntimeKlineContextSourceBindingValidator Java Skeleton` or `RuntimeKlineContext Source Binding DTO Verification`.

下一安全包应为 `RuntimeKlineContextSourceBindingValidator Java Skeleton` 或 `RuntimeKlineContext Source Binding DTO Verification`。

Do not jump directly to service runtime, dashboard runtime, real point generation, external push, order, execution, or auto-trading.

不要直接跳到 service runtime、dashboard runtime、真实点位生成、external push、order、execution 或 auto-trading。
