package org.example.trademodel.service.push;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidateAttentionDTO;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidateHandoffDTO;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidatePreviewGuardDTO;
import org.example.trademodel.dto.push.ReviewOnlyInternalPushPreviewDTO;
import org.example.trademodel.dto.score.ReviewOnlyScoreAssemblyDTO;
import org.example.trademodel.service.candidate.ReviewOnlyCandidateAttentionAssembler;
import org.example.trademodel.service.candidate.ReviewOnlyCandidatePreviewGuardAssembler;
import org.example.trademodel.service.candidate.ReviewOnlyScoreToCandidateHandoffAssembler;
import org.junit.jupiter.api.Test;

class CandidatePushReviewOnlyMvpClosureTest {

    private static final Path DASHBOARD_TEMPLATE =
            Path.of("src/main/resources/templates/dashboard.html");
    private static final String INTERNAL_PUSH_PREVIEW_START =
            "<section class=\"card module-status-card review-display-card\" id=\"internalPushPreviewDisplay\"";
    private static final String SECTION_END = "</section>";

    private final ReviewOnlyScoreToCandidateHandoffAssembler handoffAssembler =
            new ReviewOnlyScoreToCandidateHandoffAssembler();
    private final ReviewOnlyCandidateAttentionAssembler attentionAssembler =
            new ReviewOnlyCandidateAttentionAssembler();
    private final ReviewOnlyCandidatePreviewGuardAssembler previewGuardAssembler =
            new ReviewOnlyCandidatePreviewGuardAssembler();
    private final ReviewOnlyInternalPushPreviewAssembler internalPushPreviewAssembler =
            new ReviewOnlyInternalPushPreviewAssembler();

    @Test
    void reviewOnlyCandidatePushMvpChainPreservesSafetyFlagsToInternalPushPreview() {
        ReviewOnlyCandidateHandoffDTO handoff = handoffAssembler.assemble(validScoreAssembly());
        ReviewOnlyCandidateAttentionDTO attention = attentionAssembler.assemble(handoff);
        ReviewOnlyCandidatePreviewGuardDTO previewGuard = previewGuardAssembler.assemble(attention);
        ReviewOnlyInternalPushPreviewDTO preview = internalPushPreviewAssembler.assemble(previewGuard);

        assertThat(handoff.isReviewOnly()).isTrue();
        assertThat(attention.isReviewOnly()).isTrue();
        assertThat(previewGuard.isReviewOnly()).isTrue();
        assertThat(preview.isReviewOnly()).isTrue();
        assertThat(preview.isNotTradeInstruction()).isTrue();
        assertThat(preview.isManualReviewRequired()).isTrue();
        assertThat(preview.isRecheckRequired()).isTrue();
        assertThat(preview.isRiskActionGuardRequired()).isTrue();
        assertThat(preview.getInternalPushPreviewStatus())
                .isEqualTo("REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK");
        assertThat(preview.getAllowedNextStep()).isEqualTo("READY_FOR_PUSH_PREVIEW_CLOSURE_REVIEW_ONLY");
    }

    @Test
    void reviewOnlyCandidatePushMvpChainPreservesSourceReasonsAndRiskBlockers() {
        ReviewOnlyInternalPushPreviewDTO preview =
                assembleToInternalPushPreview(validScoreAssemblyWithRiskBlockers());

        assertThat(preview.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
        assertThat(preview.getRequestedTimeframes()).containsExactly("15m", "1h");
        assertThat(preview.getBlockingReasons())
                .contains(
                        "REVIEW_ONLY_SCORE_ASSEMBLY",
                        "REVIEW_ONLY_CANDIDATE_HANDOFF",
                        "REVIEW_ONLY_CANDIDATE_ATTENTION",
                        "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD",
                        "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK"
                );
        assertThat(preview.getRiskBlockers())
                .containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(preview.getAllowedNextStep()).isEqualTo("WAIT_FOR_RISK_ACTION_GUARD_RECHECK");
    }

    @Test
    void blockedCandidatePushMvpChainPreservesFailClosedBoundary() {
        ReviewOnlyInternalPushPreviewDTO preview =
                assembleToInternalPushPreview(blockedScoreAssembly());

        assertThat(preview.isBlocked()).isTrue();
        assertThat(preview.isFailClosed()).isTrue();
        assertThat(preview.isReviewOnly()).isTrue();
        assertThat(preview.isNotTradeInstruction()).isTrue();
        assertThat(preview.isManualReviewRequired()).isTrue();
        assertThat(preview.isRecheckRequired()).isTrue();
        assertThat(preview.isRiskActionGuardRequired()).isTrue();
        assertThat(preview.getBlockingReasons())
                .contains(
                        "BLOCKED_MISSING_SOURCE_CONTRACT_ID",
                        "REVIEW_ONLY_SCORE_ASSEMBLY_FAIL_CLOSED",
                        "REVIEW_ONLY_CANDIDATE_HANDOFF_FAIL_CLOSED",
                        "REVIEW_ONLY_CANDIDATE_ATTENTION_FAIL_CLOSED",
                        "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_FAIL_CLOSED"
                );
        assertThat(preview.getRiskBlockers())
                .containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void candidatePushReviewOnlyDtosDoNotExposeExternalSendReadinessPointOrTradingFields() {
        assertNoExactFields(
                List.of(
                        ReviewOnlyCandidateHandoffDTO.class,
                        ReviewOnlyCandidateAttentionDTO.class,
                        ReviewOnlyCandidatePreviewGuardDTO.class,
                        ReviewOnlyInternalPushPreviewDTO.class
                ),
                List.of(
                        "telegram",
                        "email",
                        "webhook",
                        "notification",
                        "externalChannel",
                        "messageToSend",
                        "sendMessage",
                        "readiness",
                        "point",
                        "entry",
                        "stop",
                        "takeProfit",
                        "tp",
                        "rr",
                        "orderIntent",
                        "executionIntent"
                )
        );
    }

    @Test
    void assemblersHaveNoMarketQuoteRuntimeOrExternalSendDependency() {
        assertNoForbiddenSurface(List.of("MarketQuoteClient", "BinanceMarketQuoteClient"));
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
        assertAssemblerSourcesDoNotContain(List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp",
                "Telegram",
                "Webhook",
                "Notification",
                "ExternalChannel",
                "MessageToSend",
                "SendMessage",
                "Readiness",
                "PointGeneration",
                "OrderIntent",
                "ExecutionIntent",
                "AutoTrading"
        ));
    }

    @Test
    void assemblersRemainPlainObjectsWithoutSpringContext() {
        for (Class<?> type : assemblerTypes()) {
            assertThat(type.getAnnotations()).isEmpty();
            assertThat(nonStaticFields(type)).isEmpty();
            assertThat(defaultConstructor(type)).isNotNull();
        }
    }

    @Test
    void dashboardDisplaysInternalPushPreviewAsDisabledReviewOnlySurface() throws Exception {
        String section = normalizedInternalPushPreviewDisplay();

        assertThat(section).contains("internal push preview");
        assertThat(section).contains("review-only preview");
        assertThat(section).contains("external channel disabled");
        assertThat(section).contains("risk action guard required");
        assertThat(section).contains("not a trade instruction");
        assertThat(section).contains("manual review required");
        assertThat(section).contains("no telegram / email / webhook connected");
        assertThat(section).contains("no readiness / point / entry / stop / tp / rr generated");

        assertThat(section).doesNotContain("telegram enabled");
        assertThat(section).doesNotContain("email enabled");
        assertThat(section).doesNotContain("webhook enabled");
        assertThat(section).doesNotContain("external channel enabled");
        assertThat(section).doesNotContain("readiness generated");
        assertThat(section).doesNotContain("point generated");
        assertThat(section).doesNotContain("place order");
        assertThat(section).doesNotContain("execute order");
        assertThat(section).doesNotContain("auto-trading enabled");
    }

    private ReviewOnlyInternalPushPreviewDTO assembleToInternalPushPreview(ReviewOnlyScoreAssemblyDTO scoreAssembly) {
        ReviewOnlyCandidateHandoffDTO handoff = handoffAssembler.assemble(scoreAssembly);
        ReviewOnlyCandidateAttentionDTO attention = attentionAssembler.assemble(handoff);
        ReviewOnlyCandidatePreviewGuardDTO previewGuard = previewGuardAssembler.assemble(attention);
        return internalPushPreviewAssembler.assemble(previewGuard);
    }

    private ReviewOnlyScoreAssemblyDTO validScoreAssembly() {
        return ReviewOnlyScoreAssemblyDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCORE_INPUT_PRECHECK",
                "REVIEW_ONLY_SCORE_ASSEMBLY",
                List.of("REVIEW_ONLY_SCORE_ASSEMBLY"),
                List.of(),
                "READY_FOR_SCORE_TO_CANDIDATE_HANDOFF_REVIEW_ONLY",
                "Review-only score input precheck is ready for score handoff review only."
        );
    }

    private ReviewOnlyScoreAssemblyDTO validScoreAssemblyWithRiskBlockers() {
        return ReviewOnlyScoreAssemblyDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCORE_INPUT_PRECHECK",
                "REVIEW_ONLY_SCORE_ASSEMBLY",
                List.of("REVIEW_ONLY_SCORE_ASSEMBLY"),
                List.of("stampede_review", "risk_action_guard_required"),
                "READY_FOR_SCORE_TO_CANDIDATE_HANDOFF_REVIEW_ONLY",
                "Review-only score input precheck is ready for score handoff review only."
        );
    }

    private ReviewOnlyScoreAssemblyDTO blockedScoreAssembly() {
        return ReviewOnlyScoreAssemblyDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only score assembly remains blocked and fail-closed."
        );
    }

    private void assertNoExactFields(List<Class<?>> types, List<String> fieldNames) {
        for (Class<?> type : types) {
            for (Field field : type.getDeclaredFields()) {
                for (String fieldName : fieldNames) {
                    assertThat(field.getName()).isNotEqualToIgnoringCase(fieldName);
                }
            }
        }
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : assemblerTypes()) {
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

    private void assertAssemblerSourcesDoNotContain(List<String> forbiddenFragments) {
        for (String source : assemblerSources()) {
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(source).doesNotContain(forbiddenFragment);
            }
        }
    }

    private List<Class<?>> assemblerTypes() {
        return List.of(
                ReviewOnlyScoreToCandidateHandoffAssembler.class,
                ReviewOnlyCandidateAttentionAssembler.class,
                ReviewOnlyCandidatePreviewGuardAssembler.class,
                ReviewOnlyInternalPushPreviewAssembler.class
        );
    }

    private List<String> assemblerSources() {
        try {
            return List.of(
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/service/candidate/"
                                    + "ReviewOnlyScoreToCandidateHandoffAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/service/candidate/"
                                    + "ReviewOnlyCandidateAttentionAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/service/candidate/"
                                    + "ReviewOnlyCandidatePreviewGuardAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/service/push/"
                                    + "ReviewOnlyInternalPushPreviewAssembler.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read Candidate / Push review-only assembler sources", ex);
        }
    }

    private Constructor<?> defaultConstructor(Class<?> type) {
        try {
            return type.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields(Class<?> type) {
        return List.of(type.getDeclaredFields()).stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    private String normalizedInternalPushPreviewDisplay() throws Exception {
        return internalPushPreviewDisplay()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String internalPushPreviewDisplay() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);
        int start = html.indexOf(INTERNAL_PUSH_PREVIEW_START);
        assertThat(start).isNotNegative();
        int end = html.indexOf(SECTION_END, start);
        assertThat(end).isGreaterThan(start);
        return html.substring(start, end + SECTION_END.length());
    }

    private void assertNoFragment(String value, List<String> forbiddenFragments) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String forbiddenFragment : forbiddenFragments) {
            assertThat(normalized).doesNotContain(forbiddenFragment.toLowerCase(Locale.ROOT));
        }
    }
}
