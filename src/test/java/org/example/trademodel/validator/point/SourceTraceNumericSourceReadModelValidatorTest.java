package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.SourceTraceNumericSourceContextDTO;
import org.junit.jupiter.api.Test;

class SourceTraceNumericSourceReadModelValidatorTest {

    private final SourceTraceNumericSourceReadModelValidator validator =
            new SourceTraceNumericSourceReadModelValidator();

    @Test
    void nullContextReturnsIncomplete() {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(null);

        assertIncompleteFor(result, "SOURCE_TRACE_CONTEXT_MISSING");
    }

    @Test
    void incompleteContextWithMissingReasonReturnsIncomplete() {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(incompleteContext());

        assertIncompleteFor(result, "SOURCE_TRACE_MISSING");
    }

    @Test
    void incompleteContextWithoutMissingReasonIsSafelyIncomplete() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result =
                validator.validate(contextWithStatus(
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.INCOMPLETE,
                        SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                        new BigDecimal("100.25"),
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                        null,
                        null
                ));

        assertIncompleteFor(result, "MISSING_REASON_REQUIRED");
    }

    @Test
    void blockedContextWithBlockedReasonReturnsBlockedFailClosed() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result =
                validator.validate(blockedContext("SOURCE_TRACE_BLOCKED"));

        assertBlockedFor(result, "SOURCE_TRACE_BLOCKED");
    }

    @Test
    void blockedContextWithoutBlockedReasonReturnsBlockedFailClosed() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(blockedContext(null));

        assertBlockedFor(result, "BLOCKED_REASON_REQUIRED");
    }

    @Test
    void reviewOnlySourceTraceWithCompleteAllowedSourceReturnsReviewOnlySourceTrace() {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(completeContext());

        assertThat(result.getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.REVIEW_ONLY_SOURCE_TRACE);
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isIncomplete()).isFalse();
        assertThat(result.isBlockedFailClosed()).isFalse();
    }

    @Test
    void degradedSourceTraceWithMissingReasonReturnsReviewOnlyDegraded() {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result =
                validator.validate(degradedContext("SOURCE_TRACE_DEGRADED"));

        assertThat(result.getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED);
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.getReasons()).containsExactly("SOURCE_TRACE_DEGRADED");
    }

    @Test
    void degradedSourceTraceWithoutMissingReasonReturnsIncomplete() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result =
                validator.validate(contextWithStatus(
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED,
                        SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                        new BigDecimal("100.25"),
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                        null,
                        null
                ));

        assertIncompleteFor(result, "MISSING_REASON_REQUIRED");
    }

    @Test
    void missingSourceTraceIdReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithSourceTraceId(null)), "SOURCE_TRACE_ID_MISSING");
    }

    @Test
    void missingSourceOwnerReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithSourceOwner(" ")), "SOURCE_OWNER_MISSING");
    }

    @Test
    void missingSourceTypeReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithSourceType(null)), "SOURCE_TYPE_MISSING");
    }

    @Test
    void missingSourceContractIdReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithSourceContractId("")), "SOURCE_CONTRACT_ID_MISSING");
    }

    @Test
    void missingSymbolReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithSymbol(null)), "SYMBOL_MISSING");
    }

    @Test
    void missingMarketReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithMarket(" ")), "MARKET_MISSING");
    }

    @Test
    void missingTimeframeReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithTimeframe(null)), "TIMEFRAME_MISSING");
    }

    @Test
    void missingNumericFieldNameReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithNumericFieldName("")), "NUMERIC_FIELD_NAME_MISSING");
    }

    @Test
    void missingNumericFieldRoleReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithNumericFieldRole(null)), "NUMERIC_FIELD_ROLE_MISSING");
    }

    @Test
    void missingSourceRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithSourceRef(null)), "SOURCE_REF_MISSING");
    }

    @Test
    void missingObservedAtReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithObservedAt(" ")), "OBSERVED_AT_MISSING");
    }

    @Test
    void missingRuntimeKlineContextRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithRuntimeKlineContextRef(null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING"
        );
    }

    @Test
    void missingDataQualityContextRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithDataQualityContextRef("")),
                "DATA_QUALITY_CONTEXT_REF_MISSING"
        );
    }

    @Test
    void missingMultiTimeframeContextRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithMultiTimeframeContextRef(null)),
                "MULTI_TIMEFRAME_CONTEXT_REF_MISSING"
        );
    }

    @Test
    void missingRiskActionGuardRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithRiskActionGuardRef(" ")),
                "RISK_ACTION_GUARD_REF_MISSING"
        );
    }

    @Test
    void staleFreshnessReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithFreshness(SourceTraceNumericSourceContextDTO.FreshnessStatus.STALE)),
                "FRESHNESS_STALE"
        );
    }

    @Test
    void unknownFreshnessReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithFreshness(SourceTraceNumericSourceContextDTO.FreshnessStatus.UNKNOWN)),
                "FRESHNESS_UNKNOWN"
        );
    }

    @Test
    void nullFreshnessReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithFreshness(null)), "FRESHNESS_MISSING");
    }

    @Test
    void forbiddenSourceTypeAiProseOnlyReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.AI_PROSE_ONLY);
    }

    @Test
    void forbiddenSourceTypeDashboardTextOnlyReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.DASHBOARD_TEXT_ONLY);
    }

    @Test
    void forbiddenSourceTypeScoreLabelOnlyReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.SCORE_LABEL_ONLY);
    }

    @Test
    void forbiddenSourceTypeFinalDecisionTextOnlyReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.FINAL_DECISION_TEXT_ONLY);
    }

    @Test
    void forbiddenSourceTypeLatestPriceOnlyReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.LATEST_PRICE_ONLY);
    }

    @Test
    void forbiddenSourceTypeLatestCloseOnlyReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.LATEST_CLOSE_ONLY);
    }

    @Test
    void forbiddenSourceTypeHardcodedDefaultReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.HARDCODED_DEFAULT);
    }

    @Test
    void forbiddenSourceTypeManuallyInventedFallbackReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.MANUALLY_INVENTED_FALLBACK);
    }

    @Test
    void forbiddenSourceTypeDisplaySlotOnlyReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.DISPLAY_SLOT_ONLY);
    }

    @Test
    void forbiddenSourceTypeOrderBookDirectReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.ORDER_BOOK_DIRECT);
    }

    @Test
    void forbiddenSourceTypeExternalProviderDirectReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.EXTERNAL_PROVIDER_DIRECT);
    }

    @Test
    void forbiddenSourceTypeOrderExecutionPathReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.ORDER_EXECUTION_PATH);
    }

    @Test
    void forbiddenSourceTypeAutoTradingPathReturnsBlockedFailClosed() throws Exception {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.AUTO_TRADING_PATH);
    }

    @Test
    void entryPriceMissingNumericValueReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                        null,
                        null,
                        null
                )),
                "NUMERIC_VALUE_MISSING"
        );
    }

    @Test
    void stopPriceMissingNumericValueReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.STOP_PRICE,
                        null,
                        null,
                        null
                )),
                "NUMERIC_VALUE_MISSING"
        );
    }

    @Test
    void takeProfitPriceMissingNumericValueReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.TAKE_PROFIT_PRICE,
                        null,
                        null,
                        null
                )),
                "NUMERIC_VALUE_MISSING"
        );
    }

    @Test
    void riskRewardValueMissingNumericValueReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.RISK_REWARD_VALUE,
                        null,
                        null,
                        null
                )),
                "NUMERIC_VALUE_MISSING"
        );
    }

    @Test
    void sourceOnlyReferenceWithoutNumericValueButWithRefsReturnsReviewOnlySourceTrace() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(
                contextWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.SOURCE_ONLY_REFERENCE,
                        null,
                        null,
                        null
                )
        );

        assertThat(result.getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.REVIEW_ONLY_SOURCE_TRACE);
        assertThat(result.isValidForReviewOnly()).isTrue();
    }

    @Test
    void forbiddenExecutableSemanticInMissingReasonReturnsBlockedFailClosed() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result =
                validator.validate(contextWithStatus(
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.INCOMPLETE,
                        SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                        new BigDecimal("100.25"),
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                        "buy",
                        null
                ));

        assertBlockedFor(result, "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInBlockedReasonReturnsBlockedFailClosed() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result =
                validator.validate(contextWithStatus(
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.BLOCKED_FAIL_CLOSED,
                        SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                        new BigDecimal("100.25"),
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                        null,
                        "send order"
                ));

        assertBlockedFor(result, "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInSourceRefReturnsBlockedFailClosed() throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result =
                validator.validate(contextWithSourceRef("execute-now"));

        assertBlockedFor(result, "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void safetyFlagFalseReturnsBlockedFailClosed() throws Exception {
        SourceTraceNumericSourceContextDTO context = completeContext();
        forceBoolean(context, "reviewOnly", false);

        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(context);

        assertBlockedFor(result, "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void untrustedReviewOnlySourceReturnsBlockedFailClosed() throws Exception {
        SourceTraceNumericSourceContextDTO context = completeContext();
        forceBoolean(context, "trustedSource", false);

        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(context);

        assertBlockedFor(result, "SOURCE_UNTRUSTED");
    }

    @Test
    void validatorResultNormalSafeOutputsDoNotContainForbiddenExecutableSemantics() {
        List<SourceTraceNumericSourceReadModelValidator.ValidationResult> results = List.of(
                validator.validate(incompleteContext()),
                validator.validate(blockedContextWithSafeReason()),
                validator.validate(degradedContext("SOURCE_TRACE_DEGRADED")),
                validator.validate(completeContext())
        );

        for (SourceTraceNumericSourceReadModelValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    @Test
    void validatorHasNoSpringAnnotations() {
        assertNoAnnotations(SourceTraceNumericSourceReadModelValidator.class);
        assertNoAnnotations(SourceTraceNumericSourceReadModelValidator.ValidationResult.class);
    }

    @Test
    void validatorDoesNotReferenceAssemblerServiceControllerMapperRepositoryOrScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Assembler",
                "Service",
                "Repository",
                "Scheduler"
        ));
    }

    @Test
    void validatorDoesNotReferenceMarketQuoteHttpOrDataSource() throws Exception {
        assertSourceDoesNotContain(List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp",
                "DataSource",
                "Jdbc"
        ));
    }

    @Test
    void validatorDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
        assertSourceDoesNotContain(List.of(
                "Telegram",
                "Webhook",
                "MessageSender",
                "PushSend",
                "OrderIntent",
                "ExecutionIntent",
                "AutoTrading"
        ));
    }

    private SourceTraceNumericSourceContextDTO incompleteContext() {
        return SourceTraceNumericSourceContextDTO.incomplete(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                "SOURCE_TRACE_MISSING"
        );
    }

    private SourceTraceNumericSourceContextDTO blockedContextWithSafeReason() {
        return SourceTraceNumericSourceContextDTO.blockedFailClosed(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                "SOURCE_TRACE_BLOCKED"
        );
    }

    private SourceTraceNumericSourceContextDTO blockedContext(String blockedReason) throws Exception {
        return contextWith(
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.BLOCKED_FAIL_CLOSED,
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                new BigDecimal("100.25"),
                null,
                null,
                "2026-06-02T07:00:00Z",
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1",
                null,
                blockedReason
        );
    }

    private SourceTraceNumericSourceContextDTO completeContext() {
        return SourceTraceNumericSourceContextDTO.reviewOnly(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                new BigDecimal("100.25"),
                null,
                null,
                "USDT",
                "2026-06-02T07:00:00Z",
                "2026-06-02T07:01:00Z",
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                new BigDecimal("0.82"),
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1"
        );
    }

    private SourceTraceNumericSourceContextDTO degradedContext(String missingReason) {
        return SourceTraceNumericSourceContextDTO.degraded(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                new BigDecimal("100.25"),
                null,
                null,
                "USDT",
                "2026-06-02T07:00:00Z",
                "2026-06-02T07:01:00Z",
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                new BigDecimal("0.75"),
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1",
                missingReason
        );
    }

    private SourceTraceNumericSourceContextDTO contextWithSourceTraceId(String sourceTraceId) throws Exception {
        return contextWith(sourceTraceId, "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithSourceOwner(String sourceOwner) throws Exception {
        return contextWith("source-trace-1", sourceOwner, SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithSourceType(
            SourceTraceNumericSourceContextDTO.SourceType sourceType
    ) throws Exception {
        return contextWith("source-trace-1", "review-read-model", sourceType, "contract-1", "BTCUSDT", "SPOT",
                "15m", "entryPrice", SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                new BigDecimal("100.25"), SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1",
                "runtime-15m", "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithSourceContractId(String sourceContractId) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                sourceContractId, "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithSymbol(String symbol) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", symbol, "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithMarket(String market) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", market, "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithTimeframe(String timeframe) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", timeframe, "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithNumericFieldName(String numericFieldName) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", numericFieldName,
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithNumericFieldRole(
            SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole
    ) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice", numericFieldRole,
                new BigDecimal("100.25"), SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1",
                "runtime-15m", "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithSourceRef(String sourceRef) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, sourceRef, "runtime-15m", "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithObservedAt(String observedAt) throws Exception {
        return contextWith(
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE,
                "source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"), null,
                null, observedAt, SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1",
                "runtime-15m", "dq-1", "mtf-1", "rag-1", null, null
        );
    }

    private SourceTraceNumericSourceContextDTO contextWithRuntimeKlineContextRef(String runtimeRef) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", runtimeRef, "dq-1",
                "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithDataQualityContextRef(String dataQualityRef)
            throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m",
                dataQualityRef, "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithMultiTimeframeContextRef(String multiTimeframeRef)
            throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                multiTimeframeRef, "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithRiskActionGuardRef(String riskActionGuardRef)
            throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m", "dq-1",
                "mtf-1", riskActionGuardRef);
    }

    private SourceTraceNumericSourceContextDTO contextWithFreshness(
            SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus
    ) throws Exception {
        return contextWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                freshnessStatus, "source-ref-1", "runtime-15m", "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceContextDTO contextWithNumeric(
            SourceTraceNumericSourceContextDTO.NumericFieldRole role,
            BigDecimal numericValue,
            BigDecimal numericValueLow,
            BigDecimal numericValueHigh
    ) throws Exception {
        return contextWith(
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE,
                "source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "numericField", role, numericValue, numericValueLow,
                numericValueHigh, "2026-06-02T07:00:00Z", SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                "source-ref-1", "runtime-15m", "dq-1", "mtf-1", "rag-1", null, null
        );
    }

    private SourceTraceNumericSourceContextDTO contextWith(
            String sourceTraceId,
            String sourceOwner,
            SourceTraceNumericSourceContextDTO.SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus,
            String sourceRef,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef
    ) throws Exception {
        return contextWith(
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE,
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                numericValue,
                null,
                null,
                "2026-06-02T07:00:00Z",
                freshnessStatus,
                sourceRef,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                null,
                null
        );
    }

    private SourceTraceNumericSourceContextDTO contextWithStatus(
            SourceTraceNumericSourceContextDTO.SourceTraceStatus status,
            SourceTraceNumericSourceContextDTO.SourceType sourceType,
            SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus,
            String missingReason,
            String blockedReason
    ) throws Exception {
        return contextWith(
                status,
                "source-trace-1",
                "review-read-model",
                sourceType,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                numericFieldRole,
                numericValue,
                null,
                null,
                "2026-06-02T07:00:00Z",
                freshnessStatus,
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1",
                missingReason,
                blockedReason
        );
    }

    private SourceTraceNumericSourceContextDTO contextWith(
            SourceTraceNumericSourceContextDTO.SourceTraceStatus status,
            String sourceTraceId,
            String sourceOwner,
            SourceTraceNumericSourceContextDTO.SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            BigDecimal numericValueLow,
            BigDecimal numericValueHigh,
            String observedAt,
            SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus,
            String sourceRef,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String missingReason,
            String blockedReason
    ) throws Exception {
        Constructor<SourceTraceNumericSourceContextDTO> constructor =
                SourceTraceNumericSourceContextDTO.class.getDeclaredConstructor(
                        String.class,
                        String.class,
                        SourceTraceNumericSourceContextDTO.SourceType.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.class,
                        BigDecimal.class,
                        BigDecimal.class,
                        BigDecimal.class,
                        String.class,
                        String.class,
                        String.class,
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.class,
                        BigDecimal.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance(
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                numericValue,
                numericValueLow,
                numericValueHigh,
                "USDT",
                observedAt,
                "2026-06-02T07:01:00Z",
                freshnessStatus,
                new BigDecimal("0.82"),
                sourceRef,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                missingReason,
                blockedReason,
                status
        );
    }

    private void assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType sourceType) throws Exception {
        SourceTraceNumericSourceReadModelValidator.ValidationResult result = validator.validate(contextWithSourceType(sourceType));

        assertBlockedFor(result, "FORBIDDEN_SOURCE_TYPE");
    }

    private void forceBoolean(SourceTraceNumericSourceContextDTO context, String fieldName, boolean value)
            throws Exception {
        Field field = SourceTraceNumericSourceContextDTO.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(context, value);
    }

    private void assertIncompleteFor(SourceTraceNumericSourceReadModelValidator.ValidationResult result, String reason) {
        assertThat(result.getStatus()).isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.isBlockedFailClosed()).isFalse();
        assertThat(result.getReasons()).contains(reason);
    }

    private void assertBlockedFor(SourceTraceNumericSourceReadModelValidator.ValidationResult result, String reason) {
        assertThat(result.getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.isBlockedFailClosed()).isTrue();
        assertThat(result.getReasons()).contains(reason);
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/validator/point/SourceTraceNumericSourceReadModelValidator.java"
        ));

        for (String fragment : fragments) {
            assertThat(source).doesNotContain(fragment);
        }
    }

    private void assertNoForbiddenExecutableSemantics(List<String> outputs) {
        List<String> forbiddenWords = List.of(
                "buy",
                "sell",
                "long",
                "short",
                "open long",
                "open short",
                "close position",
                "reverse",
                "market close",
                "market cut",
                "order",
                "execute",
                "execution",
                "auto-trade",
                "auto trading",
                "take-profit order",
                "stop-loss order",
                "send order",
                "push opportunity"
        );

        for (String output : outputs) {
            String lowerOutput = output == null ? "" : output.toLowerCase();
            for (String forbiddenWord : forbiddenWords) {
                assertThat(lowerOutput).doesNotContain(forbiddenWord);
            }
        }
    }
}
