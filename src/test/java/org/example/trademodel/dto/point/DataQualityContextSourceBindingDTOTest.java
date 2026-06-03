package org.example.trademodel.dto.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataQualityContextSourceBindingDTOTest {

    @Test
    void factoriesKeepSafetyFlagsForcedTrue() {
        List<DataQualityContextSourceBindingDTO> contexts = List.of(
                incompleteContext(),
                blockedContext(),
                degradedContext(),
                reviewOnlyContext()
        );

        for (DataQualityContextSourceBindingDTO context : contexts) {
            assertThat(context.isReviewOnly()).isTrue();
            assertThat(context.isNotTradeInstruction()).isTrue();
            assertThat(context.isManualReviewRequired()).isTrue();
            assertThat(context.isIncompleteSafe()).isTrue();
        }
    }

    @Test
    void blockedFailClosedFactorySetsFailClosedTrue() {
        DataQualityContextSourceBindingDTO context = blockedContext();

        assertThat(context.isFailClosed()).isTrue();
        assertThat(context.getBindingStatus())
                .isEqualTo(DataQualityContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED);
    }

    @Test
    void incompleteFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> DataQualityContextSourceBindingDTO.incomplete(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                List.of("dataQualityScore"),
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> DataQualityContextSourceBindingDTO.blockedFailClosed(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                List.of("HARD_THRESHOLD_BLOCKED"),
                ""
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> DataQualityContextSourceBindingDTO.degraded(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("80"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.MEDIUM,
                Boolean.TRUE,
                Boolean.FALSE,
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                List.of(),
                List.of("QUALITY_DEGRADED"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                Boolean.TRUE
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesExplicitFieldsAndDoesNotCalculateScores() {
        DataQualityContextSourceBindingDTO context = reviewOnlyContext();

        assertThat(context.getDataQualityScore()).isEqualByComparingTo("92");
        assertThat(context.getSourceTraceCompletenessScore()).isEqualByComparingTo("91");
        assertThat(context.getRuntimeKlineCompletenessScore()).isEqualByComparingTo("93");
        assertThat(context.getOhlcvCompletenessScore()).isEqualByComparingTo("94");
        assertThat(context.getFreshnessScore()).isEqualByComparingTo("95");
        assertThat(context.getMultiTimeframeConsistencyScore()).isEqualByComparingTo("90");
        assertThat(context.getMissingReason()).isNull();
        assertThat(context.getBlockedReason()).isNull();
    }

    @Test
    void listFieldsAreDefensivelyCopiedAndImmutable() {
        List<String> sourceRefs = new ArrayList<>();
        sourceRefs.add("source-trace-ref");
        List<String> missingFields = new ArrayList<>();
        missingFields.add("runtimeKlineContextRef");

        DataQualityContextSourceBindingDTO context = DataQualityContextSourceBindingDTO.incomplete(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                sourceRefs,
                "runtime-kline-ref",
                missingFields,
                "DATA_QUALITY_MISSING"
        );
        sourceRefs.add("mutated-ref");
        missingFields.add("mutated-field");

        assertThat(context.getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(context.getMissingFields()).containsExactly("runtimeKlineContextRef");
        assertThatThrownBy(() -> context.getSourceTraceRefs().add("new-ref"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void enumsCoverRequiredStatusesAndGrades() {
        assertThat(List.of(DataQualityContextSourceBindingDTO.BindingStatus.values()))
                .contains(
                        DataQualityContextSourceBindingDTO.BindingStatus.INCOMPLETE,
                        DataQualityContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED,
                        DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING,
                        DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
                );
        assertThat(List.of(DataQualityContextSourceBindingDTO.DataQualityGrade.values()))
                .contains(
                        DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH,
                        DataQualityContextSourceBindingDTO.DataQualityGrade.MEDIUM,
                        DataQualityContextSourceBindingDTO.DataQualityGrade.LOW,
                        DataQualityContextSourceBindingDTO.DataQualityGrade.UNKNOWN
                );
    }

    @Test
    void noSetterBuilderOrFactoryCanDisableSafetyFlags() {
        for (Method method : DataQualityContextSourceBindingDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            assertThat(method.getName()).doesNotContain("ReviewOnlyFalse");
            assertThat(method.getName()).doesNotContain("TradeInstructionFalse");
        }
        for (Field field : DataQualityContextSourceBindingDTO.class.getDeclaredFields()) {
            if (field.getName().equals("reviewOnly")
                    || field.getName().equals("notTradeInstruction")
                    || field.getName().equals("manualReviewRequired")
                    || field.getName().equals("incompleteSafe")) {
                assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
            }
        }
    }

    @Test
    void dtoHasNoSpringAnnotations() {
        assertNoAnnotations(DataQualityContextSourceBindingDTO.class);
    }

    @Test
    void dtoDoesNotReferenceServiceControllerMapperRepositoryOrScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Service",
                "Repository",
                "Scheduler"
        ));
    }

    @Test
    void dtoDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
        assertSourceDoesNotContain(List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp",
                "javax.sql.DataSource",
                "import javax.sql",
                "DataSource ",
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

    private DataQualityContextSourceBindingDTO incompleteContext() {
        return DataQualityContextSourceBindingDTO.incomplete(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                List.of("dataQualityScore"),
                "DATA_QUALITY_MISSING"
        );
    }

    private DataQualityContextSourceBindingDTO blockedContext() {
        return DataQualityContextSourceBindingDTO.blockedFailClosed(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                List.of("HARD_THRESHOLD_BLOCKED"),
                "DATA_QUALITY_BLOCKED"
        );
    }

    private DataQualityContextSourceBindingDTO degradedContext() {
        return DataQualityContextSourceBindingDTO.degraded(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("80"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.MEDIUM,
                Boolean.TRUE,
                Boolean.FALSE,
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                List.of(),
                List.of("QUALITY_DEGRADED"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "DATA_QUALITY_DEGRADED",
                Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO reviewOnlyContext() {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("92"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH,
                Boolean.TRUE,
                Boolean.TRUE,
                bd("91"),
                bd("93"),
                bd("94"),
                bd("95"),
                bd("90"),
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                Boolean.TRUE
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/DataQualityContextSourceBindingDTO.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
