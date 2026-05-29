package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestDTO;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationResult;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationStatusEnum;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyOutputDTO;
import org.junit.jupiter.api.Test;

class MarketReadRequestReviewOnlyAssemblerTest {

    private final MarketReadRequestGuardValidator guardValidator = new MarketReadRequestGuardValidator();
    private final MarketReadRequestReviewOnlyAssembler assembler = new MarketReadRequestReviewOnlyAssembler();

    @Test
    void validReviewOnlyRequestAssemblesReviewOnlyOutput() {
        MarketReadRequestDTO request = validRequest();
        MarketReadReviewOnlyOutputDTO output = assembler.assemble(request, guardValidator.validate(request));

        assertThat(output.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(output.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(output.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(output.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
        assertThat(output.getGuardValidationStatus())
                .isEqualTo(MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY);
        assertThat(output.isFailClosed()).isFalse();
        assertThat(output.getAllowedNextStep()).isEqualTo("WAIT_FOR_MARKET_READ_AUTHORIZATION");
        assertThat(output.getReviewOnlyMessage()).contains("manual review only");
        assertSafetyFlags(output);
    }

    @Test
    void validReviewOnlyRequestWithoutRiskBlockersWaitsForReview() {
        MarketReadRequestDTO request = validRequestWithoutRiskBlockers();
        MarketReadReviewOnlyOutputDTO output = assembler.assemble(request, guardValidator.validate(request));

        assertThat(output.getAllowedNextStep()).isEqualTo("WAIT_FOR_REVIEW");
        assertThat(output.isFailClosed()).isFalse();
        assertSafetyFlags(output);
    }

    @Test
    void blockedGuardResultAssemblesBlockedOutput() {
        MarketReadRequestDTO request = requestWithSourceContractId(null);
        MarketReadReviewOnlyOutputDTO output = assembler.assemble(request, guardValidator.validate(request));

        assertThat(output.getGuardValidationStatus())
                .isEqualTo(MarketReadRequestGuardValidationStatusEnum.BLOCKED);
        assertThat(output.isFailClosed()).isTrue();
        assertThat(output.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(output.getBlockingReasons()).contains("MARKET_READ_REQUEST_GUARD_BLOCKED");
        assertSafetyFlags(output);
    }

    @Test
    void missingSourceContractIdReasonIsPreserved() {
        MarketReadRequestDTO request = requestWithSourceContractId("");
        MarketReadReviewOnlyOutputDTO output = assembler.assemble(request, guardValidator.validate(request));

        assertThat(output.getValidationReasons()).contains("MISSING_SOURCE_CONTRACT_ID");
        assertThat(output.getBlockingReasons()).contains("BLOCKED_MISSING_SOURCE_CONTRACT_ID");
    }

    @Test
    void missingWatchlistPoolProofReasonIsPreserved() {
        MarketReadRequestDTO request = requestWithWatchlistPoolProof(null);
        MarketReadReviewOnlyOutputDTO output = assembler.assemble(request, guardValidator.validate(request));

        assertThat(output.getValidationReasons()).contains("MISSING_WATCHLIST_POOL_PROOF");
        assertThat(output.getBlockingReasons()).contains("BLOCKED_MISSING_WATCHLIST_POOL_PROOF");
    }

    @Test
    void requestedTimeframesArePreserved() {
        MarketReadReviewOnlyOutputDTO output = assembleValid();

        assertThat(output.getRequestedTimeframes()).containsExactly("15m", "1h");
        output.getRequestedTimeframes().add("mutated");
        assertThat(output.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        MarketReadReviewOnlyOutputDTO output = assembleValid();

        assertThat(output.getBlockingReasons())
                .contains("manual_gate", "risk_guard_before_delivery", "MARKET_READ_REQUEST_REVIEW_ONLY");
        output.getBlockingReasons().add("mutated");
        assertThat(output.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        MarketReadReviewOnlyOutputDTO output = assembleValid();

        assertThat(output.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        output.getRiskBlockers().add("mutated");
        assertThat(output.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void outputIsReviewOnly() {
        assertThat(assembleValid().isReviewOnly()).isTrue();
    }

    @Test
    void outputIsNotTradeInstruction() {
        assertThat(assembleValid().isNotTradeInstruction()).isTrue();
    }

    @Test
    void outputIsManualReviewRequired() {
        assertThat(assembleValid().isManualReviewRequired()).isTrue();
    }

    @Test
    void outputDoesNotContainPointOrPlanFieldsByReflection() {
        assertNoFragmentInFields(
                MarketReadReviewOnlyOutputDTO.class,
                List.of("price", "entry", "stop", "takeprofit", "targetprofit", "riskreward", "rr")
        );
        assertNoPointAcronymField(MarketReadReviewOnlyOutputDTO.class);
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
    void assemblerDoesNotCreateScanScoreCandidatePushReadinessPointOrTradingBehavior() {
        assertNoForbiddenSurface(List.of(
                "ScanOutput",
                "Score",
                "Candidate",
                "Push",
                "Readiness",
                "Point",
                "Trading",
                "Order",
                "Execution",
                "Entry",
                "Stop",
                "TakeProfit"
        ));
        assertMainSourcesDoNotContain(List.of(
                "ScanOutput",
                "Score",
                "Candidate",
                "Push",
                "Readiness",
                "Point",
                "Trading",
                "Order",
                "Execution",
                "Entry",
                "Stop",
                "TakeProfit"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        MarketReadRequestReviewOnlyAssembler plainAssembler = new MarketReadRequestReviewOnlyAssembler();
        MarketReadRequestDTO request = validRequest();

        assertThat(plainAssembler.assemble(request, guardValidator.validate(request)).isReviewOnly()).isTrue();
        assertThat(MarketReadRequestReviewOnlyAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void missingGuardResultFailsClosedBeforeAnyRead() {
        MarketReadReviewOnlyOutputDTO output = assembler.assemble(validRequest(), null);

        assertThat(output.getGuardValidationStatus()).isEqualTo(MarketReadRequestGuardValidationStatusEnum.BLOCKED);
        assertThat(output.isFailClosed()).isTrue();
        assertThat(output.getAllowedNextStep()).isEqualTo("BLOCKED_BY_GUARD");
        assertThat(output.getBlockingReasons()).contains("MARKET_READ_REQUEST_GUARD_RESULT_MISSING");
        assertSafetyFlags(output);
    }

    private MarketReadReviewOnlyOutputDTO assembleValid() {
        MarketReadRequestDTO request = validRequest();
        MarketReadRequestGuardValidationResult result = guardValidator.validate(request);
        return assembler.assemble(request, result);
    }

    private MarketReadRequestDTO validRequest() {
        return request(
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                List.of("stampede_review", "risk_action_guard_required"),
                List.of("manual_gate", "risk_guard_before_delivery")
        );
    }

    private MarketReadRequestDTO validRequestWithoutRiskBlockers() {
        return request(
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                List.of(),
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO requestWithSourceContractId(String sourceContractId) {
        return request(
                sourceContractId,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                List.of("stampede_review"),
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO requestWithWatchlistPoolProof(String watchlistPoolProof) {
        return request(
                "real-scan-input-contract-001",
                watchlistPoolProof,
                List.of("15m", "1h"),
                List.of("stampede_review"),
                List.of("manual_gate")
        );
    }

    private MarketReadRequestDTO request(
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            List<String> riskBlockers,
            List<String> blockingReasons
    ) {
        return MarketReadRequestDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                sourceContractId,
                watchlistPoolProof,
                "watchlist-v1",
                "manual_review_scan",
                requestedTimeframes,
                Instant.parse("2026-05-28T00:00:00Z"),
                "EXPECTED_REVIEW_ONLY",
                "FAIL_CLOSED",
                "FAIL_CLOSED",
                riskBlockers,
                "REVIEW_ONLY",
                blockingReasons
        );
    }

    private void assertSafetyFlags(MarketReadReviewOnlyOutputDTO output) {
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

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                MarketReadRequestReviewOnlyAssembler.class,
                MarketReadReviewOnlyOutputDTO.class
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
                                    + "MarketReadRequestReviewOnlyAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/marketread/"
                                    + "MarketReadReviewOnlyOutputDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read market read review-only source files", ex);
        }
    }

    private Constructor<MarketReadRequestReviewOnlyAssembler> defaultConstructor() {
        try {
            return MarketReadRequestReviewOnlyAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(MarketReadRequestReviewOnlyAssembler.class.getDeclaredFields()).stream()
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
