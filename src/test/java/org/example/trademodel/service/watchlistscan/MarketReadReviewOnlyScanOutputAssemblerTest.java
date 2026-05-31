package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationStatusEnum;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyOutputDTO;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyScanOutputDTO;
import org.junit.jupiter.api.Test;

class MarketReadReviewOnlyScanOutputAssemblerTest {

    private final MarketReadReviewOnlyScanOutputAssembler assembler =
            new MarketReadReviewOnlyScanOutputAssembler();

    @Test
    void validReviewOnlyOutputAssemblesReviewOnlyScanOutput() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(validInputWithoutRiskBlockers());

        assertThat(output.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(output.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(output.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(output.getGuardValidationStatus())
                .isEqualTo(MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY);
        assertThat(output.getScanOutputStatus()).isEqualTo("REVIEW_ONLY_SCAN_OUTPUT");
        assertThat(output.getAllowedNextStep()).isEqualTo("READY_FOR_EVIDENCE_SCORE_ENTRY_REVIEW_ONLY");
        assertThat(output.isBlocked()).isFalse();
        assertThat(output.isFailClosed()).isFalse();
        assertSafetyFlags(output);
    }

    @Test
    void blockedInputAssemblesBlockedScanOutput() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(blockedInput());

        assertThat(output.getGuardValidationStatus()).isEqualTo(MarketReadRequestGuardValidationStatusEnum.BLOCKED);
        assertThat(output.getScanOutputStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(output.isBlocked()).isTrue();
        assertThat(output.isFailClosed()).isTrue();
        assertThat(output.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertSafetyFlags(output);
    }

    @Test
    void failClosedReasonIsPreserved() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(blockedInput());

        assertThat(output.getBlockingReasons())
                .contains("MARKET_READ_REQUEST_GUARD_BLOCKED", "MARKET_READ_REVIEW_ONLY_INPUT_FAIL_CLOSED");
    }

    @Test
    void missingSourceContractIdReasonIsPreservedWhenPresentInInput() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(blockedInput());

        assertThat(output.getBlockingReasons()).contains("BLOCKED_MISSING_SOURCE_CONTRACT_ID");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(validInputWithoutRiskBlockers());

        assertThat(output.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(validInputWithoutRiskBlockers());

        assertThat(output.getRequestedTimeframes()).containsExactly("15m", "1h");
        output.getRequestedTimeframes().add("mutated");
        assertThat(output.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(validInputWithoutRiskBlockers());

        assertThat(output.getBlockingReasons())
                .contains("manual_gate", "MARKET_READ_REVIEW_ONLY_SCAN_OUTPUT");
        output.getBlockingReasons().add("mutated");
        assertThat(output.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(validInputWithRiskBlockers());

        assertThat(output.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(output.getAllowedNextStep()).isEqualTo("WAIT_FOR_MARKET_READ_AUTHORIZATION");
        output.getRiskBlockers().add("mutated");
        assertThat(output.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void scanOutputIsReviewOnly() {
        assertThat(assembler.assemble(validInputWithoutRiskBlockers()).isReviewOnly()).isTrue();
    }

    @Test
    void scanOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validInputWithoutRiskBlockers()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void scanOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validInputWithoutRiskBlockers()).isManualReviewRequired()).isTrue();
    }

    @Test
    void scanOutputDoesNotContainMarketDataOrPointFieldsByReflection() {
        assertNoFragmentInFields(
                MarketReadReviewOnlyScanOutputDTO.class,
                List.of(
                        "price",
                        "livequote",
                        "quote",
                        "ohlc",
                        "open",
                        "high",
                        "volume",
                        "entry",
                        "stop",
                        "takeprofit",
                        "targetprofit",
                        "riskreward",
                        "rr"
                )
        );
        assertNoExactField(MarketReadReviewOnlyScanOutputDTO.class, "low");
        assertNoExactField(MarketReadReviewOnlyScanOutputDTO.class, "close");
        assertNoPointAcronymField(MarketReadReviewOnlyScanOutputDTO.class);
    }

    @Test
    void assemblerHasNoMarketQuoteClientOrBinanceMarketQuoteClientDependency() {
        assertNoForbiddenSurface(List.of("MarketQuoteClient", "BinanceMarketQuoteClient"));
        assertMainSourcesDoNotContain(List.of("MarketQuoteClient", "BinanceMarketQuoteClient"));
    }

    @Test
    void assemblerDoesNotReadRuntimeLiveOrExternalData() {
        assertNoForbiddenSurface(List.of(
                "Runtime",
                "Live",
                "External",
                "DataSource",
                "Provider",
                "Jdbc",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp"
        ));
        assertMainSourcesDoNotContain(List.of(
                "Runtime",
                "Live",
                "External",
                "DataSource",
                "Provider",
                "Jdbc",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp"
        ));
    }

    @Test
    void assemblerDoesNotCreateScoreEvidenceCandidatePushReadinessPointOrTradingBehavior() {
        assertNoForbiddenSurface(List.of(
                "Candidate",
                "Push",
                "Readiness",
                "Point",
                "Trading",
                "Order",
                "Execution"
        ));
        assertMainSourcesDoNotContain(List.of(
                "ScoreService",
                "EvidenceService",
                "WatchlistScanScore",
                "Candidate",
                "Push",
                "Readiness",
                "PointGeneration",
                "Trading",
                "Order",
                "Execution"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        MarketReadReviewOnlyScanOutputAssembler plainAssembler =
                new MarketReadReviewOnlyScanOutputAssembler();

        assertThat(plainAssembler.assemble(validInputWithoutRiskBlockers()).isReviewOnly()).isTrue();
        assertThat(MarketReadReviewOnlyScanOutputAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        MarketReadReviewOnlyScanOutputDTO output = assembler.assemble(null);

        assertThat(output.getScanOutputStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(output.getBlockingReasons()).contains("MARKET_READ_REVIEW_ONLY_OUTPUT_MISSING");
        assertThat(output.isBlocked()).isTrue();
        assertThat(output.isFailClosed()).isTrue();
        assertSafetyFlags(output);
    }

    private MarketReadReviewOnlyOutputDTO validInputWithoutRiskBlockers() {
        return MarketReadReviewOnlyOutputDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY,
                List.of("MARKET_READ_REQUEST_GUARD_REVIEW_ONLY"),
                List.of("manual_gate", "MARKET_READ_REQUEST_REVIEW_ONLY_OUTPUT"),
                List.of(),
                "WAIT_FOR_REVIEW",
                "Market read request validation is readable for manual review only."
        );
    }

    private MarketReadReviewOnlyOutputDTO validInputWithRiskBlockers() {
        return MarketReadReviewOnlyOutputDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY,
                List.of("MARKET_READ_REQUEST_GUARD_REVIEW_ONLY"),
                List.of("manual_gate", "MARKET_READ_REQUEST_REVIEW_ONLY_OUTPUT"),
                List.of("stampede_review", "risk_action_guard_required"),
                "WAIT_FOR_MARKET_READ_AUTHORIZATION",
                "Market read request validation is readable for manual review only."
        );
    }

    private MarketReadReviewOnlyOutputDTO blockedInput() {
        return MarketReadReviewOnlyOutputDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                MarketReadRequestGuardValidationStatusEnum.BLOCKED,
                List.of("MISSING_SOURCE_CONTRACT_ID", "MARKET_READ_REQUEST_GUARD_FAIL_CLOSED"),
                List.of("BLOCKED_MISSING_SOURCE_CONTRACT_ID", "MARKET_READ_REQUEST_GUARD_BLOCKED"),
                List.of("stampede_review"),
                "FIX_INPUT_CONTRACT",
                "Market read request remains blocked and fail-closed before any market read."
        );
    }

    private void assertSafetyFlags(MarketReadReviewOnlyScanOutputDTO output) {
        assertThat(output.isReviewOnly()).isTrue();
        assertThat(output.isNotTradeInstruction()).isTrue();
        assertThat(output.isManualReviewRequired()).isTrue();
    }

    private void assertNoFragmentInFields(Class<?> type, List<String> forbiddenFragments) {
        for (Field field : type.getDeclaredFields()) {
            assertNoFragment(field.getName(), forbiddenFragments);
        }
    }

    private void assertNoPointAcronymField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            String name = field.getName();
            assertThat(name).isNotEqualToIgnoringCase("tp");
            assertThat(name).doesNotContain("TP");
        }
    }

    private void assertNoExactField(Class<?> type, String fieldName) {
        for (Field field : type.getDeclaredFields()) {
            assertThat(field.getName()).isNotEqualToIgnoringCase(fieldName);
        }
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                MarketReadReviewOnlyScanOutputAssembler.class,
                MarketReadReviewOnlyScanOutputDTO.class
        )) {
            assertNoFragment(type.getSimpleName(), forbiddenFragments);
            for (Annotation annotation : type.getAnnotations()) {
                assertNoFragment(annotation.annotationType().getName(), forbiddenFragments);
            }
            for (Field field : type.getDeclaredFields()) {
                assertNoFragment(field.getName(), forbiddenFragments);
                assertNoFragment(field.getType().getSimpleName(), forbiddenFragments);
            }
            for (Method method : type.getDeclaredMethods()) {
                assertNoFragment(method.getName(), forbiddenFragments);
                assertNoFragment(method.getReturnType().getSimpleName(), forbiddenFragments);
                for (Class<?> parameterType : method.getParameterTypes()) {
                    assertNoFragment(parameterType.getSimpleName(), forbiddenFragments);
                }
            }
        }
    }

    private void assertMainSourcesDoNotContain(List<String> forbiddenFragments) {
        for (String source : mainSources()) {
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(source).doesNotContain(forbiddenFragment);
            }
        }
    }

    private List<String> mainSources() {
        try {
            return List.of(
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/service/watchlistscan/"
                                    + "MarketReadReviewOnlyScanOutputAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/marketread/"
                                    + "MarketReadReviewOnlyScanOutputDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read market read review-only scan output source files", ex);
        }
    }

    private Constructor<MarketReadReviewOnlyScanOutputAssembler> defaultConstructor() {
        try {
            return MarketReadReviewOnlyScanOutputAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(MarketReadReviewOnlyScanOutputAssembler.class.getDeclaredFields()).stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    private void assertNoFragment(String value, List<String> forbiddenFragments) {
        String normalized = value.toLowerCase();
        for (String forbiddenFragment : forbiddenFragments) {
            assertThat(normalized).doesNotContain(forbiddenFragment.toLowerCase());
        }
    }
}
