# Phase P337 RuntimeKlineContextSourceBindingValidator Java Skeleton

## Purpose / 目的

P337 adds the first Java validator skeleton for future RuntimeKlineContext source binding.

P337 新增未来 RuntimeKlineContext source binding 的第一个 Java validator 骨架。

It follows P336 `RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_DTO_JAVA_SKELETON`.

它承接 P336 `RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_DTO_JAVA_SKELETON`。

## Capability Movement / 能力移动

`RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_DTO_JAVA_SKELETON -> RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_VALIDATOR_JAVA_SKELETON`

This is a Java/test validator skeleton movement.

这是 Java/test validator skeleton 层级移动。

## Implemented Scope / 已实现范围

P337 adds:

- `RuntimeKlineContextSourceBindingValidator`;
- `RuntimeKlineContextSourceBindingValidatorTest`;
- safety flag checks;
- required runtime binding field checks;
- OHLCV presence and completeness checks;
- freshness / wick / gap / liquidity / stampede checks;
- SourceTrace refs checks;
- forbidden executable semantics checks;
- dependency / annotation guard tests.

P337 新增：

- `RuntimeKlineContextSourceBindingValidator`；
- `RuntimeKlineContextSourceBindingValidatorTest`；
- safety flag 校验；
- required runtime binding field 校验；
- OHLCV presence / completeness 校验；
- freshness / wick / gap / liquidity / stampede 校验；
- SourceTrace refs 校验；
- forbidden executable semantics 校验；
- dependency / annotation guard 测试。

## Explicit Non-Scope / 明确非范围

P337 does not add assembler, service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, external channel, Push send, order, execution, or auto-trading.

P337 不新增 assembler、service、controller、mapper、repository、scheduler、resources、schema、config、pom、dashboard、external channel、Push send、order、execution 或 auto-trading。

P337 does not read market data, latest price, latest close, exchanges, or external providers.

P337 不读取 market data、latest price、latest close、交易所或 external provider。

P337 does not generate real entry / stop / TP / RR and does not calculate RR.

P337 不生成真实 entry / stop / TP / RR，也不计算 RR。

## Safety Confirmation / 安全确认

The validator requires:

- `reviewOnly = true`;
- `notTradeInstruction = true`;
- `manualReviewRequired = true`;
- `incompleteSafe = true`.

validator 要求：

- `reviewOnly = true`；
- `notTradeInstruction = true`；
- `manualReviewRequired = true`；
- `incompleteSafe = true`。

Disabled safety flags return `BLOCKED_FAIL_CLOSED`.

关闭 safety flags 会返回 `BLOCKED_FAIL_CLOSED`。

Missing / stale / unknown runtime evidence remains `INCOMPLETE`.

缺失 / stale / unknown runtime evidence 保持 `INCOMPLETE`。

Severe gap, confirmed stampede, executable semantics, and unsafe flags remain `BLOCKED_FAIL_CLOSED`.

severe gap、confirmed stampede、可执行语义和 unsafe flags 保持 `BLOCKED_FAIL_CLOSED`。

## Progress Boundary / 进度边界

P337 does not raise Production Runtime Progress.

P337 不提高 Production Runtime Progress。

P337 does not mean RuntimeKlineContext assembler is complete.

P337 不代表 RuntimeKlineContext assembler 已完成。

P337 does not mean RuntimeKlineContext real runtime integration is complete.

P337 不代表 RuntimeKlineContext 真实运行接入已完成。

P337 does not mean source context integration, DataQualityContext, MultiTimeframeContext, or RiskActionGuardContext is connected.

P337 不代表 source context integration、DataQualityContext、MultiTimeframeContext 或 RiskActionGuardContext 已接入。

P337 does not authorize dashboard runtime, external push, order, execution, or auto-trading.

P337 不授权 dashboard runtime、external push、order、execution 或 auto-trading。

## Next Safe Package / 下一安全包

Next safe package should be `RuntimeKlineContextSourceBindingValidator Verification` or `RuntimeKlineContextSourceBindingAssembler Java Skeleton`.

下一安全包应为 `RuntimeKlineContextSourceBindingValidator Verification` 或 `RuntimeKlineContextSourceBindingAssembler Java Skeleton`。

Do not jump directly to service runtime, dashboard runtime, real point generation, external push, order, execution, or auto-trading.

不要直接跳到 service runtime、dashboard runtime、真实点位生成、external push、order、execution 或 auto-trading。
