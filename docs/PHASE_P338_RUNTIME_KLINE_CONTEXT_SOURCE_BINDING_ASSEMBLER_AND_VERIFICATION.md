# Phase P338 RuntimeKlineContextSourceBindingAssembler And Verification

## Purpose / 目的

P338 adds a plain Java assembler skeleton for RuntimeKlineContext source binding and closes P335-P338 with verification documentation.

P338 新增 RuntimeKlineContext source binding 的 plain Java assembler 骨架，并用 verification 文档收口 P335-P338。

## Capability Movement / 能力移动

`RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_VALIDATOR_JAVA_SKELETON -> RUNTIME_KLINE_CONTEXT_SOURCE_BINDING_ASSEMBLER_AND_VERIFICATION`

This is a Java/test assembler plus docs verification movement.

这是 Java/test assembler + docs verification 层级移动。

## Implemented Scope / 已实现范围

P338 adds:

- `RuntimeKlineContextSourceBindingAssembler`;
- `RuntimeKlineContextSourceBindingAssemblerTest`;
- explicit input to DTO assembly;
- mandatory `RuntimeKlineContextSourceBindingValidator` call;
- assembled DTO + validation result return;
- verification of P335-P338 source binding skeleton closure.

P338 新增：

- `RuntimeKlineContextSourceBindingAssembler`；
- `RuntimeKlineContextSourceBindingAssemblerTest`；
- 显式 input 到 DTO 的组装；
- 强制调用 `RuntimeKlineContextSourceBindingValidator`；
- 返回 DTO + validation result；
- 验证 P335-P338 source binding skeleton closure。

## Verification Scope / 验证范围

P338 verifies:

- null input remains `INCOMPLETE`;
- blocked input remains `BLOCKED_FAIL_CLOSED`;
- degraded input remains review-only degraded;
- review-only input remains review-only;
- missing / stale / wick-only / severe gap / stampede states are delegated to validator;
- explicit BigDecimal fields are preserved;
- explicit SourceTrace refs are preserved and defensively copied;
- forbidden executable semantics remain fail-closed;
- no service / runtime provider / dashboard / external / execution dependency exists.

P338 验证：

- null input 保持 `INCOMPLETE`；
- blocked input 保持 `BLOCKED_FAIL_CLOSED`；
- degraded input 保持 review-only degraded；
- review-only input 保持 review-only；
- missing / stale / wick-only / severe gap / stampede 状态交由 validator；
- 显式 BigDecimal 字段被保留；
- 显式 SourceTrace refs 被保留并防御性复制；
- forbidden executable semantics 保持 fail-closed；
- 不存在 service / runtime provider / dashboard / external / execution 依赖。

## Explicit Non-Scope / 明确非范围

P338 does not add service, controller, mapper, repository, scheduler, resources, schema, config, pom, dashboard, external channel, Push send, order, execution, or auto-trading.

P338 不新增 service、controller、mapper、repository、scheduler、resources、schema、config、pom、dashboard、external channel、Push send、order、execution 或 auto-trading。

P338 does not read market data, latest price, latest close, exchanges, or external providers.

P338 不读取 market data、latest price、latest close、交易所或 external provider。

P338 does not generate real entry / stop / TP / RR and does not calculate RR.

P338 不生成真实 entry / stop / TP / RR，也不计算 RR。

## Progress Boundary / 进度边界

P338 does not raise Production Runtime Progress.

P338 不提高 Production Runtime Progress。

P338 does not mean RuntimeKlineContext real runtime integration is complete.

P338 不代表 RuntimeKlineContext 真实运行接入已完成。

P338 does not mean source context integration, DataQualityContext, MultiTimeframeContext, or RiskActionGuardContext is connected.

P338 不代表 source context integration、DataQualityContext、MultiTimeframeContext 或 RiskActionGuardContext 已接入。

P338 does not authorize dashboard runtime, external push, order, execution, or auto-trading.

P338 不授权 dashboard runtime、external push、order、execution 或 auto-trading。

## Current Capability / 当前能力

RuntimeKlineContext Source Binding is now complete only as a review-only skeleton closure:

- plan;
- DTO carrier;
- validator;
- assembler;
- verification.

RuntimeKlineContext Source Binding 现在只作为 review-only skeleton closure 完成：

- plan；
- DTO carrier；
- validator；
- assembler；
- verification。

It is not runtime-safe usable and cannot generate real point values.

它还不是 runtime-safe usable，也不能生成真实点位。

## Next Safe Package / 下一安全包

Next safe package should move to `DataQualityContext Source Binding Plan` or a larger DataQuality DTO + Validator + Assembler capability closure package.

下一安全包应进入 `DataQualityContext Source Binding Plan`，或更大的 DataQuality DTO + Validator + Assembler 能力闭环包。

Do not create another RuntimeKlineContext verification micro-package.

不要再创建 RuntimeKlineContext verification 微包。
