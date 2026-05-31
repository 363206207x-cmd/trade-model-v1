package org.example.trademodel.service.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.evidence.ReviewOnlyEvidenceScoreEntryDTO;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationStatusEnum;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyScanOutputDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyEvidenceScoreEntryAssemblerTest {

    private final ReviewOnlyEvidenceScoreEntryAssembler assembler =
            new ReviewOnlyEvidenceScoreEntryAssembler();

    @Test
    void validReviewOnlyScanOutputAssemblesReviewOnlyEvidenceScoreEntry() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(validScanOutput());

        assertThat(entry.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(entry.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(entry.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(entry.getScanOutputStatus()).isEqualTo("REVIEW_ONLY_SCAN_OUTPUT");
        assertThat(entry.getEntryStatus()).isEqualTo("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY");
        assertThat(entry.getAllowedNextStep()).isEqualTo("READY_FOR_EVIDENCE_NORMALIZATION_REVIEW_ONLY");
        assertThat(entry.isBlocked()).isFalse();
        assertThat(entry.isFailClosed()).isFalse();
        assertSafetyFlags(entry);
    }

    @Test
    void blockedScanOutputAssemblesBlockedEntry() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(blockedScanOutput());

        assertThat(entry.getScanOutputStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(entry.getEntryStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(entry.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(entry.isBlocked()).isTrue();
        assertThat(entry.isFailClosed()).isTrue();
        assertSafetyFlags(entry);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(blockedScanOutput());

        assertThat(entry.getBlockingReasons())
                .contains("MARKET_READ_REVIEW_ONLY_INPUT_FAIL_CLOSED", "REVIEW_ONLY_SCAN_OUTPUT_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(validScanOutput());

        assertThat(entry.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(validScanOutput());

        assertThat(entry.getRequestedTimeframes()).containsExactly("15m", "1h");
        entry.getRequestedTimeframes().add("mutated");
        assertThat(entry.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(validScanOutput());

        assertThat(entry.getBlockingReasons())
                .contains("MARKET_READ_REVIEW_ONLY_SCAN_OUTPUT", "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY");
        entry.getBlockingReasons().add("mutated");
        assertThat(entry.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(validScanOutputWithRiskBlockers());

        assertThat(entry.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(entry.getAllowedNextStep()).isEqualTo("WAIT_FOR_SCORE_AUTHORIZATION");
        entry.getRiskBlockers().add("mutated");
        assertThat(entry.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void entryIsReviewOnly() {
        assertThat(assembler.assemble(validScanOutput()).isReviewOnly()).isTrue();
    }

    @Test
    void entryIsNotTradeInstruction() {
        assertThat(assembler.assemble(validScanOutput()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void entryIsManualReviewRequired() {
        assertThat(assembler.assemble(validScanOutput()).isManualReviewRequired()).isTrue();
    }

    @Test
    void entryDoesNotContainRealEvidenceItemScoreItemOrPointFieldsByReflection() {
        assertNoFragmentInFields(
                ReviewOnlyEvidenceScoreEntryDTO.class,
                List.of(
                        "evidenceitem",
                        "scoreitem",
                        "trend",
                        "liquidity",
                        "macro",
                        "confidence",
                        "candidate",
                        "push",
                        "readiness",
                        "pointgeneration",
                        "entryzone",
                        "stopzone",
                        "takeprofit",
                        "targetprofit",
                        "riskreward",
                        "orderintent",
                        "executionintent"
                )
        );
        assertNoExactField(ReviewOnlyEvidenceScoreEntryDTO.class, "tp");
        assertNoExactField(ReviewOnlyEvidenceScoreEntryDTO.class, "rr");
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
                "EvidenceItem",
                "ScoreItem",
                "Candidate",
                "Push",
                "Readiness",
                "Point",
                "Trading",
                "Order",
                "Execution"
        ));
        assertMainSourcesDoNotContain(List.of(
                "EvidenceItem",
                "ScoreItem",
                "WatchlistScanScore",
                "ScoreService",
                "EvidenceService",
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
        ReviewOnlyEvidenceScoreEntryAssembler plainAssembler =
                new ReviewOnlyEvidenceScoreEntryAssembler();

        assertThat(plainAssembler.assemble(validScanOutput()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyEvidenceScoreEntryAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyEvidenceScoreEntryDTO entry = assembler.assemble(null);

        assertThat(entry.getEntryStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(entry.getAllowedNextStep()).isEqualTo("BLOCKED_BY_SCAN_OUTPUT");
        assertThat(entry.getBlockingReasons()).contains("REVIEW_ONLY_SCAN_OUTPUT_MISSING");
        assertThat(entry.isBlocked()).isTrue();
        assertThat(entry.isFailClosed()).isTrue();
        assertSafetyFlags(entry);
    }

    private MarketReadReviewOnlyScanOutputDTO validScanOutput() {
        return MarketReadReviewOnlyScanOutputDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY,
                List.of("MARKET_READ_REVIEW_ONLY_SCAN_OUTPUT"),
                List.of(),
                "REVIEW_ONLY_SCAN_OUTPUT",
                "READY_FOR_EVIDENCE_SCORE_ENTRY_REVIEW_ONLY",
                "Market read review-only scan output is ready for the next manual-review skeleton."
        );
    }

    private MarketReadReviewOnlyScanOutputDTO validScanOutputWithRiskBlockers() {
        return MarketReadReviewOnlyScanOutputDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY,
                List.of("MARKET_READ_REVIEW_ONLY_SCAN_OUTPUT"),
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY_SCAN_OUTPUT",
                "READY_FOR_EVIDENCE_SCORE_ENTRY_REVIEW_ONLY",
                "Market read review-only scan output is ready for the next manual-review skeleton."
        );
    }

    private MarketReadReviewOnlyScanOutputDTO blockedScanOutput() {
        return MarketReadReviewOnlyScanOutputDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                MarketReadRequestGuardValidationStatusEnum.BLOCKED,
                List.of("MARKET_READ_REVIEW_ONLY_INPUT_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "BLOCKED_FAIL_CLOSED",
                "FIX_INPUT_CONTRACT",
                "Market read review-only scan output remains blocked and fail-closed."
        );
    }

    private void assertSafetyFlags(ReviewOnlyEvidenceScoreEntryDTO entry) {
        assertThat(entry.isReviewOnly()).isTrue();
        assertThat(entry.isNotTradeInstruction()).isTrue();
        assertThat(entry.isManualReviewRequired()).isTrue();
    }

    private void assertNoFragmentInFields(Class<?> type, List<String> forbiddenFragments) {
        for (Field field : type.getDeclaredFields()) {
            assertNoFragment(field.getName(), forbiddenFragments);
        }
    }

    private void assertNoExactField(Class<?> type, String fieldName) {
        for (Field field : type.getDeclaredFields()) {
            assertThat(field.getName()).isNotEqualToIgnoringCase(fieldName);
        }
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                ReviewOnlyEvidenceScoreEntryAssembler.class,
                ReviewOnlyEvidenceScoreEntryDTO.class
        )) {
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
                            "src/main/java/org/example/trademodel/service/evidence/"
                                    + "ReviewOnlyEvidenceScoreEntryAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/evidence/"
                                    + "ReviewOnlyEvidenceScoreEntryDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only evidence/score entry source files", ex);
        }
    }

    private Constructor<ReviewOnlyEvidenceScoreEntryAssembler> defaultConstructor() {
        try {
            return ReviewOnlyEvidenceScoreEntryAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyEvidenceScoreEntryAssembler.class.getDeclaredFields()).stream()
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
