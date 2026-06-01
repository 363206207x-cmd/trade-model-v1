package org.example.trademodel.service.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyPointProposalDTO;
import org.example.trademodel.dto.point.ReviewOnlyPointProposalDisplayDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyPointProposalDisplayAssemblerTest {

    private final ReviewOnlyPointProposalDisplayAssembler assembler =
            new ReviewOnlyPointProposalDisplayAssembler();

    @Test
    void validPointProposalEntersDisplayGateStillReviewOnly() {
        ReviewOnlyPointProposalDisplayDTO display = assembler.assemble(validProposalWithValues());

        assertThat(display.getDisplayGateStatus())
                .isEqualTo("REVIEW_ONLY_POINT_PROPOSAL_CLOSURE_DISPLAY_GATE");
        assertThat(display.isReviewOnly()).isTrue();
        assertThat(display.isBlocked()).isFalse();
        assertThat(display.isIncomplete()).isFalse();
        assertDisplayPlaceholders(display);
    }

    @Test
    void displayGatePreservesNotTradeInstruction() {
        assertThat(assembler.assemble(validProposalWithValues()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void displayGatePreservesManualReviewRequired() {
        assertThat(assembler.assemble(validProposalWithValues()).isManualReviewRequired()).isTrue();
    }

    @Test
    void displayGatePreservesRecheckRequired() {
        assertThat(assembler.assemble(validProposalWithValues()).isRecheckRequired()).isTrue();
    }

    @Test
    void displayGatePreservesRiskActionGuardRequired() {
        assertThat(assembler.assemble(validProposalWithValues()).isRiskActionGuardRequired()).isTrue();
    }

    @Test
    void displayGatePreservesSourceTraceRequired() {
        assertThat(assembler.assemble(validProposalWithValues()).isSourceTraceRequired()).isTrue();
    }

    @Test
    void displayGatePreservesRuntimeKlineContextRequired() {
        assertThat(assembler.assemble(validProposalWithValues()).isRuntimeKlineContextRequired()).isTrue();
    }

    @Test
    void incompleteProposalDoesNotDisplayExecutableValues() {
        ReviewOnlyPointProposalDisplayDTO display = assembler.assemble(incompleteProposal());

        assertThat(display.getDisplayGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(display.getIncompleteReason()).isEqualTo("INCOMPLETE_SOURCE_OWNED_POINT_INPUT");
        assertThat(display.isFailClosed()).isTrue();
        assertThat(display.isIncomplete()).isTrue();
        assertDisplayPlaceholders(display);
    }

    @Test
    void blockedProposalKeepsFailClosed() {
        ReviewOnlyPointProposalDisplayDTO display = assembler.assemble(blockedProposal());

        assertThat(display.getDisplayGateStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(display.getBlockedReason()).isEqualTo("BLOCKED_BY_POINT_BOUNDARY_GATE");
        assertThat(display.isFailClosed()).isTrue();
        assertThat(display.isBlocked()).isTrue();
        assertThat(display.isIncomplete()).isFalse();
        assertDisplayPlaceholders(display);
    }

    @Test
    void riskBlockersDoNotGeneratePointDisplayInstruction() {
        ReviewOnlyPointProposalDisplayDTO display = assembler.assemble(proposalWithRiskBlockers());

        assertThat(display.getDisplayGateStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(display.getBlockedReason()).isEqualTo("BLOCKED_BY_RISK_ACTION_GUARD");
        assertThat(display.getRiskBlockers()).containsExactly("stampede_review", "liquidity_stress");
        assertDisplayPlaceholders(display);
    }

    @Test
    void displayMessageDoesNotContainTradingSemantics() {
        for (ReviewOnlyPointProposalDisplayDTO display : List.of(
                assembler.assemble(validProposalWithValues()),
                assembler.assemble(incompleteProposal()),
                assembler.assemble(blockedProposal())
        )) {
            assertNoForbiddenDisplayWords(display.getSafeDisplayMessage());
        }
    }

    @Test
    void assemblerAndDtoHaveNoForbiddenDependenciesOrAnnotations() {
        assertNoForbiddenSurface(List.of(
                "Spring",
                "Controller",
                "Mapper",
                "Repository",
                "Scheduler",
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "RuntimeKlineContextDTO",
                "SourceTraceDTO",
                "DataSource",
                "Jdbc",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp",
                "Telegram",
                "Webhook",
                "MessageSender",
                "OrderIntent",
                "ExecutionIntent"
        ));
        assertMainSourcesDoNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "DataSource",
                "Jdbc",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp",
                "Telegram",
                "Webhook",
                "MessageSender",
                "OrderIntent",
                "ExecutionIntent"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        ReviewOnlyPointProposalDisplayAssembler plainAssembler =
                new ReviewOnlyPointProposalDisplayAssembler();

        assertThat(plainAssembler.assemble(validProposalWithValues()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyPointProposalDisplayAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedAndStaysDisplayOnly() {
        ReviewOnlyPointProposalDisplayDTO display = assembler.assemble(null);

        assertThat(display.getDisplayGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(display.getIncompleteReason()).isEqualTo("REVIEW_ONLY_POINT_PROPOSAL_MISSING");
        assertThat(display.isFailClosed()).isTrue();
        assertThat(display.isBlocked()).isTrue();
        assertThat(display.isIncomplete()).isTrue();
        assertDisplayPlaceholders(display);
        assertRequiredFlags(display);
    }

    private ReviewOnlyPointProposalDTO validProposalWithValues() {
        return ReviewOnlyPointProposalDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "REVIEW_ONLY_POINT_BOUNDARY_GATE",
                "REVIEW_ONLY_POINT_PROPOSAL",
                true,
                null,
                List.of("REVIEW_ONLY_POINT_PROPOSAL"),
                List.of(),
                "WAIT_FOR_REVIEW",
                "Review-only proposal with source-owned placeholders.",
                "100.00",
                "99.50-100.50",
                "96.00",
                "95.50-96.50",
                "108.00",
                "tiered plan",
                "2.0",
                "2R"
        );
    }

    private ReviewOnlyPointProposalDTO incompleteProposal() {
        return ReviewOnlyPointProposalDTO.incomplete(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "REVIEW_ONLY_POINT_BOUNDARY_GATE",
                "INCOMPLETE_FAIL_CLOSED",
                true,
                "INCOMPLETE_SOURCE_OWNED_POINT_INPUT",
                List.of("INCOMPLETE_SOURCE_OWNED_POINT_INPUT"),
                List.of(),
                "WAIT_FOR_SOURCE_OWNED_POINT_INPUT",
                "Source-owned review-only point proposal is incomplete."
        );
    }

    private ReviewOnlyPointProposalDTO blockedProposal() {
        return ReviewOnlyPointProposalDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "REVIEW_ONLY_POINT_BOUNDARY_GATE",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_POINT_PROPOSAL_FAIL_CLOSED"),
                List.of("risk_action_guard_required"),
                "BLOCKED_BY_POINT_BOUNDARY_GATE",
                "Source-owned review-only point proposal remains blocked.",
                "BLOCKED_BY_POINT_BOUNDARY_GATE"
        );
    }

    private ReviewOnlyPointProposalDTO proposalWithRiskBlockers() {
        return ReviewOnlyPointProposalDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "REVIEW_ONLY_POINT_BOUNDARY_GATE",
                "REVIEW_ONLY_POINT_PROPOSAL",
                true,
                null,
                List.of("REVIEW_ONLY_POINT_PROPOSAL"),
                List.of("stampede_review", "liquidity_stress"),
                "WAIT_FOR_REVIEW",
                "Risk Action Guard blocks display escalation.",
                "100.00",
                "99.50-100.50",
                "96.00",
                "95.50-96.50",
                "108.00",
                "tiered plan",
                "2.0",
                "2R"
        );
    }

    private void assertRequiredFlags(ReviewOnlyPointProposalDisplayDTO display) {
        assertThat(display.isReviewOnly()).isTrue();
        assertThat(display.isNotTradeInstruction()).isTrue();
        assertThat(display.isManualReviewRequired()).isTrue();
        assertThat(display.isRecheckRequired()).isTrue();
        assertThat(display.isRiskActionGuardRequired()).isTrue();
        assertThat(display.isSourceTraceRequired()).isTrue();
        assertThat(display.isRuntimeKlineContextRequired()).isTrue();
    }

    private void assertDisplayPlaceholders(ReviewOnlyPointProposalDisplayDTO display) {
        assertRequiredFlags(display);
        assertThat(display.getEntryDisplayText()).isEqualTo("UNAVAILABLE_REVIEW_ONLY_PLACEHOLDER");
        assertThat(display.getStopDisplayText()).isEqualTo("UNAVAILABLE_REVIEW_ONLY_PLACEHOLDER");
        assertThat(display.getTakeProfitDisplayText()).isEqualTo("UNAVAILABLE_REVIEW_ONLY_PLACEHOLDER");
        assertThat(display.getRiskRewardDisplayText()).isEqualTo("UNAVAILABLE_REVIEW_ONLY_PLACEHOLDER");
        assertThat(display.getEntryDisplayText()).doesNotContain("100.00");
        assertThat(display.getStopDisplayText()).doesNotContain("96.00");
        assertThat(display.getTakeProfitDisplayText()).doesNotContain("108.00");
        assertThat(display.getRiskRewardDisplayText()).doesNotContain("2R");
    }

    private void assertNoForbiddenDisplayWords(String message) {
        assertThat(message.toLowerCase()).doesNotContain(
                "buy",
                "sell",
                "long",
                "short",
                "open long",
                "open short",
                "close position",
                "reverse",
                "order",
                "execute",
                "auto trade",
                "auto-trade"
        );
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                ReviewOnlyPointProposalDisplayAssembler.class,
                ReviewOnlyPointProposalDisplayDTO.class
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
                            "src/main/java/org/example/trademodel/service/point/"
                                    + "ReviewOnlyPointProposalDisplayAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/point/"
                                    + "ReviewOnlyPointProposalDisplayDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only point proposal display source files", ex);
        }
    }

    private Constructor<ReviewOnlyPointProposalDisplayAssembler> defaultConstructor() {
        try {
            return ReviewOnlyPointProposalDisplayAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyPointProposalDisplayAssembler.class.getDeclaredFields()).stream()
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
