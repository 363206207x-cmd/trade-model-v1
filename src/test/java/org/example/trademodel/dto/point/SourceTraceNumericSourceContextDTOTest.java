package org.example.trademodel.dto.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceTraceNumericSourceContextDTOTest {

    @Test
    void incompleteFactoryKeepsReviewOnlyTrue() {
        assertThat(incompleteSource().isReviewOnly()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsNotTradeInstructionTrue() {
        assertThat(incompleteSource().isNotTradeInstruction()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsManualReviewRequiredTrue() {
        assertThat(incompleteSource().isManualReviewRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsIncompleteSafeTrue() {
        assertThat(incompleteSource().isIncompleteSafe()).isTrue();
    }

    @Test
    void blockedFailClosedFactorySetsFailClosedTrue() {
        SourceTraceNumericSourceContextDTO source = blockedSource();

        assertThat(source.isFailClosed()).isTrue();
        assertThat(source.getSourceTraceStatus())
                .isEqualTo(SourceTraceNumericSourceContextDTO.SourceTraceStatus.BLOCKED_FAIL_CLOSED);
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> SourceTraceNumericSourceContextDTO.blockedFailClosed(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.AI_PROSE_ONLY,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void incompleteFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> SourceTraceNumericSourceContextDTO.incomplete(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                ""
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> SourceTraceNumericSourceContextDTO.degraded(
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
                new BigDecimal("99.50"),
                new BigDecimal("101.00"),
                "USDT",
                "2026-06-02T07:00:00Z",
                "2026-06-02T07:01:00Z",
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                new BigDecimal("0.80"),
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesOnlyExplicitFieldsAndDoesNotCalculateNumericValues() {
        SourceTraceNumericSourceContextDTO source = reviewOnlySource();

        assertThat(source.getSourceTraceStatus())
                .isEqualTo(SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE);
        assertThat(source.getNumericValue()).isEqualByComparingTo("100.25");
        assertThat(source.getNumericValueLow()).isEqualByComparingTo("99.50");
        assertThat(source.getNumericValueHigh()).isEqualByComparingTo("101.00");
        assertThat(source.getSourceConfidence()).isEqualByComparingTo("0.82");
        assertThat(source.getMissingReason()).isNull();
        assertThat(source.getBlockedReason()).isNull();
    }

    @Test
    void numericFieldsCanBeNullAndRemainIncompleteSafe() {
        SourceTraceNumericSourceContextDTO source = SourceTraceNumericSourceContextDTO.reviewOnly(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.MANUAL_REVIEW_SOURCE,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "sourceOnly",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.SOURCE_ONLY_REFERENCE,
                null,
                null,
                null,
                null,
                "2026-06-02T07:00:00Z",
                "2026-06-02T07:01:00Z",
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                null,
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1"
        );

        assertThat(source.getNumericValue()).isNull();
        assertThat(source.getNumericValueLow()).isNull();
        assertThat(source.getNumericValueHigh()).isNull();
        assertThat(source.getSourceConfidence()).isNull();
        assertThat(source.isIncompleteSafe()).isTrue();
        assertThat(source.isReviewOnly()).isTrue();
    }

    @Test
    void allowedSourceTypeEnumsExist() {
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL.isSourceOwnedEvidence())
                .isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.INVALIDATION_LEVEL.isSourceOwnedEvidence())
                .isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.LIQUIDITY_POOL_LEVEL.isSourceOwnedEvidence())
                .isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.PRIOR_HIGH_LOW_LEVEL.isSourceOwnedEvidence())
                .isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.VWAP_OR_VOLUME_LEVEL.isSourceOwnedEvidence())
                .isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.ATR_REFERENCE_LEVEL.isSourceOwnedEvidence())
                .isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.MULTITIMEFRAME_CONFIRMATION_LEVEL
                .isSourceOwnedEvidence()).isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.RISK_ACTION_GUARD_REFERENCE
                .isSourceOwnedEvidence()).isTrue();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.MANUAL_REVIEW_SOURCE.isSourceOwnedEvidence())
                .isTrue();
    }

    @Test
    void forbiddenSourceTypeEnumsExistButAreNotAuthorizedAsSourceOwnedEvidence() {
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.AI_PROSE_ONLY.isSourceOwnedEvidence()).isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.DASHBOARD_TEXT_ONLY.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.SCORE_LABEL_ONLY.isSourceOwnedEvidence()).isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.FINAL_DECISION_TEXT_ONLY.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.LATEST_PRICE_ONLY.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.LATEST_CLOSE_ONLY.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.HARDCODED_DEFAULT.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.MANUALLY_INVENTED_FALLBACK
                .isSourceOwnedEvidence()).isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.DISPLAY_SLOT_ONLY.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.ORDER_BOOK_DIRECT.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.ORDER_EXECUTION_PATH.isSourceOwnedEvidence())
                .isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.EXTERNAL_PROVIDER_DIRECT
                .isSourceOwnedEvidence()).isFalse();
        assertThat(SourceTraceNumericSourceContextDTO.SourceType.AUTO_TRADING_PATH.isSourceOwnedEvidence())
                .isFalse();
    }

    @Test
    void numericFieldRoleEnumCoversEntryStopTakeProfitAndRiskReward() {
        assertThat(List.of(SourceTraceNumericSourceContextDTO.NumericFieldRole.values()))
                .contains(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_ZONE_LOW,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_ZONE_HIGH,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.STOP_PRICE,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.STOP_ZONE_LOW,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.STOP_ZONE_HIGH,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.TAKE_PROFIT_PRICE,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.TAKE_PROFIT_ZONE_LOW,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.TAKE_PROFIT_ZONE_HIGH,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.RISK_REWARD_VALUE,
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.SOURCE_ONLY_REFERENCE
                );
    }

    @Test
    void freshnessStatusEnumCoversFreshStaleAndUnknown() {
        assertThat(List.of(SourceTraceNumericSourceContextDTO.FreshnessStatus.values()))
                .contains(
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.STALE,
                        SourceTraceNumericSourceContextDTO.FreshnessStatus.UNKNOWN
                );
    }

    @Test
    void sourceTraceStatusEnumCoversRequiredStatuses() {
        assertThat(List.of(SourceTraceNumericSourceContextDTO.SourceTraceStatus.values()))
                .contains(
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.INCOMPLETE,
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.BLOCKED_FAIL_CLOSED,
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE,
                        SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED
                );
    }

    @Test
    void noSetterBuilderOrFactoryParameterCanDisableSafetyFlags() {
        for (Method method : SourceTraceNumericSourceContextDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                assertThat(List.of(method.getParameterTypes())).doesNotContain(boolean.class);
            }
        }
        for (Constructor<?> constructor : SourceTraceNumericSourceContextDTO.class.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
    }

    @Test
    void dtoHasNoSpringAnnotations() {
        assertNoAnnotations(SourceTraceNumericSourceContextDTO.class);
    }

    @Test
    void dtoHasNoControllerMapperRepositorySchedulerOrServiceDependency() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Service",
                "Provider"
        ));
    }

    @Test
    void dtoHasNoMarketQuoteOrHttpOrDataSourceDependency() throws Exception {
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
    void dtoDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
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

    @Test
    void dtoPublicStatusAndReasonOutputsDoNotContainForbiddenExecutableSemantics() {
        for (SourceTraceNumericSourceContextDTO source : List.of(
                incompleteSource(),
                blockedSource(),
                degradedSource(),
                reviewOnlySource()
        )) {
            assertNoForbiddenExecutableSemantics(List.of(
                    source.getSourceTraceStatus().name(),
                    valueOrEmpty(source.getMissingReason()),
                    valueOrEmpty(source.getBlockedReason())
            ));
        }
    }

    private SourceTraceNumericSourceContextDTO incompleteSource() {
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

    private SourceTraceNumericSourceContextDTO blockedSource() {
        return SourceTraceNumericSourceContextDTO.blockedFailClosed(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.AI_PROSE_ONLY,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                "SOURCE_TRACE_BLOCKED"
        );
    }

    private SourceTraceNumericSourceContextDTO degradedSource() {
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
                new BigDecimal("99.50"),
                new BigDecimal("101.00"),
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
                "SOURCE_TRACE_DEGRADED"
        );
    }

    private SourceTraceNumericSourceContextDTO reviewOnlySource() {
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
                new BigDecimal("99.50"),
                new BigDecimal("101.00"),
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

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/SourceTraceNumericSourceContextDTO.java"
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
