package org.example.trademodel.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipAuditEnvelope;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.service.SourceTraceEntryProductionOwnershipReviewBoundary;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class FailClosedSourceTraceEntryProductionOwnershipReviewBoundaryTest {

    private static final List<String> FORBIDDEN_METHOD_TOKENS = List.of(
            "order",
            "execution",
            "close",
            "reverse",
            "autoTrading",
            "autoTrade",
            "tradeReady",
            "readyToTrade",
            "valid",
            "completed",
            "signal",
            "buy",
            "sell",
            "open"
    );
    private static final List<String> GENERATED_VALUE_METHOD_TOKENS = List.of(
            "generatedEntry",
            "generatedStop",
            "generatedTakeProfit",
            "generatedRiskReward",
            "stopValue",
            "takeProfitValue",
            "riskRewardValue"
    );

    private final FailClosedSourceTraceEntryProductionOwnershipReviewBoundary boundary =
            new FailClosedSourceTraceEntryProductionOwnershipReviewBoundary();

    @Test
    void implementationPresenceAloneFailsClosed() {
        SourceTraceEntryProductionOwnershipReviewResult result =
                boundary.reviewEntryOwnership(completeLookingRequest());

        assertFailClosedReviewOnly(result);
        assertThat(result.getMissingFields()).contains("auditEnvelope", "consumerIsolationEnvelope");
        assertThat(result.getBlockingFields()).contains(
                "productionOwnershipReviewBoundaryUnwired",
                "productionWiringStillBlocked",
                "failClosedImplementationSkeleton",
                "auditMetadataMissing",
                "consumerIsolationMissing"
        );
    }

    @Test
    void nullRequestFailsClosed() {
        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(null);

        assertFailClosedReviewOnly(result);
        assertThat(result.getMissingFields()).containsExactly("request");
        assertThat(result.getBlockingFields()).contains(
                "nullRequest",
                "productionOwnershipReviewBoundaryUnwired",
                "productionWiringStillBlocked"
        );
    }

    @Test
    void missingOwnerEvidenceFailsClosed() {
        SourceTraceEntryProductionOwnershipReviewResult result =
                boundary.reviewEntryOwnership(new SourceTraceEntryProductionOwnershipReviewRequest());

        assertFailClosedReviewOnly(result);
        assertThat(result.getMissingFields()).contains(
                "sourceTraceEntryOwnershipCompletionPath",
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef",
                "sourceWindow",
                "ruleId",
                "ruleVersion",
                "freshnessOwnership",
                "conflictFamilyOwnership"
        );
        assertThat(result.getBlockingFields()).contains("ownerEvidenceMissing");
    }

    @Test
    void duplicateAmbiguousAndStaleOwnerEvidenceFailClosed() {
        SourceTraceEntryProductionOwnershipReviewRequest request = completeLookingRequest();
        request.setOwnerEvidenceFields(List.of("entryPriceSource", "entryPriceSource"));
        request.setSourceRefs(List.of("fixture-ref-a", "fixture-ref-b"));
        request.setFreshnessOwnership("stale-owner-evidence");

        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

        assertFailClosedReviewOnly(result);
        assertThat(result.getUnsafeFields()).contains(
                "duplicateOwnerEvidence",
                "ambiguousOwnerEvidence",
                "staleOwnerEvidence"
        );
        assertThat(result.getBlockingFields()).contains(
                "duplicateOwnerEvidence",
                "ambiguousOwnerEvidence",
                "staleOwnerEvidence"
        );
    }

    @Test
    void runtimeLikeSubstitutionsFailClosedOneAtATime() {
        for (String source : List.of(
                "latest-price-only",
                "raw-kline-only",
                "AI text substitution",
                "dashboard text substitution",
                "external data substitution",
                "order data substitution",
                "execution data substitution"
        )) {
            SourceTraceEntryProductionOwnershipReviewRequest request = completeLookingRequest();
            request.setEntryPriceSource(source);

            SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

            assertFailClosedReviewOnly(result);
            assertThat(result.getUnsafeFields()).contains("runtimeLikeSubstitution");
            assertThat(result.getBlockingFields()).contains("runtimeLikeSubstitution");
        }
    }

    @Test
    void missingAuditMetadataAuthenticationVisibilityAndConsumerIsolationFailClosed() {
        SourceTraceEntryProductionOwnershipReviewRequest request = completeLookingRequest();
        request.setAuthenticationVisibility(null);
        request.setAuditEnvelope(new SourceTraceEntryProductionOwnershipAuditEnvelope());
        request.setConsumerIsolationEnvelope(new SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope());

        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

        assertFailClosedReviewOnly(result);
        assertThat(result.getMissingFields()).contains(
                "auditEnvelope",
                "authenticationVisibility",
                "consumerIsolationEnvelope"
        );
        assertThat(result.getBlockingFields()).contains(
                "auditMetadataMissing",
                "authenticationVisibilityMissing",
                "payloadWithheldForReview",
                "consumerIsolationMissing"
        );
    }

    @Test
    void riskActionGuardBlockersRemainReviewOnlyAndBlockCompletion() {
        SourceTraceEntryProductionOwnershipReviewRequest request = completeLookingRequest();
        request.setConflictFamilyOwnership(
                "high-risk wick pin-bar liquidity-stress stampede missing-event multi-timeframe"
        );

        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

        assertFailClosedReviewOnly(result);
        assertThat(result.getUnsafeFields()).contains("riskActionGuardReviewRequired");
        assertThat(result.getBlockingFields()).contains("riskActionGuardReviewRequired");
    }

    @Test
    void positiveLookingLabelsDoNotImplyCompletionReadinessValidDashboardOrderExecutionAutomationOrExternalPaths() {
        SourceTraceEntryProductionOwnershipReviewRequest request = completeLookingRequest();
        request.setEntrySourceReason("valid completed signal buy sell open ready-looking review label");

        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

        assertFailClosedReviewOnly(result);
        assertThat(result.getUnsafeFields()).contains("positiveLookingLabel");
        assertThat(result.getBlockingFields()).contains("positiveLookingLabel");
        assertThat(result.getBlockingFields()).contains(
                "boundaryCandidateServiceValidIsolation",
                "executionPlanReadinessIsolation",
                "dashboardMutationIsolation",
                "orderPathIsolation",
                "executionPathIsolation",
                "automationPathIsolation",
                "externalDataPathIsolation"
        );
    }

    @Test
    void downgradeAndRollbackOutputsPreserveFailClosedFlagsAndBlockerEvidence() {
        SourceTraceEntryProductionOwnershipReviewRequest request = completeLookingRequest();
        SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope =
                new SourceTraceEntryProductionOwnershipAuditEnvelope();
        auditEnvelope.setDowngradeReason("downgrade-required");
        auditEnvelope.setRollbackReason("rollback-required");
        request.setAuditEnvelope(auditEnvelope);

        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

        assertFailClosedReviewOnly(result);
        assertThat(result.getDowngradeReason())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.REVIEW_BOUNDARY_UNWIRED);
        assertThat(result.getUnsafeFields()).contains("downgradeRequired", "rollbackRequired");
        assertThat(result.getBlockingFields()).contains("downgradeRequired", "rollbackRequired");
    }

    @Test
    void implementationAcceptsOnlyReviewRequestAndExposesNoForbiddenMethodNames() throws NoSuchMethodException {
        Method method = FailClosedSourceTraceEntryProductionOwnershipReviewBoundary.class.getDeclaredMethod(
                "reviewEntryOwnership",
                SourceTraceEntryProductionOwnershipReviewRequest.class
        );

        assertThat(method.getReturnType()).isEqualTo(SourceTraceEntryProductionOwnershipReviewResult.class);
        assertThat(FailClosedSourceTraceEntryProductionOwnershipReviewBoundary.class.getInterfaces())
                .containsExactly(SourceTraceEntryProductionOwnershipReviewBoundary.class);
        assertThat(Arrays.stream(FailClosedSourceTraceEntryProductionOwnershipReviewBoundary.class
                        .getDeclaredMethods())
                .map(Method::getName))
                .noneMatch(this::containsForbiddenMethodToken)
                .noneMatch(this::containsGeneratedTradingValueToken);
    }

    @Test
    void implementationHasNoSpringAnnotationsAndNoGeneratedTradingValueSurface() {
        Class<?> type = FailClosedSourceTraceEntryProductionOwnershipReviewBoundary.class;

        assertThat(type.getAnnotation(Service.class)).isNull();
        assertThat(type.getAnnotation(Component.class)).isNull();
        assertThat(type.getAnnotation(Repository.class)).isNull();
        assertThat(type.getAnnotation(Controller.class)).isNull();
        assertThat(type.getAnnotation(RestController.class)).isNull();
        assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getReturnType))
                .doesNotContain(BigDecimal.class);
        assertThat(Arrays.stream(type.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes())))
                .doesNotContain(BigDecimal.class);
    }

    @Test
    void productionAdapterAndProductionCompletionContractRemainAbsent() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private SourceTraceEntryProductionOwnershipReviewRequest completeLookingRequest() {
        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();
        request.setSymbol("BTCUSDT");
        request.setTimeframe("15m");
        request.setSourceTraceEntryOwnershipCompletionPath("fixture-owner-completion-path");
        request.setEntryPriceSource("fixture-entry-source-metadata");
        request.setEntrySourceType("RULE_OWNED_BOUNDARY");
        request.setEntrySourceTimeframe("15m");
        request.setEntrySourceReason("fixture-review-only-reason");
        request.setEntrySourceRef("fixture-source-ref");
        request.setSourceWindow("fixture-source-window");
        request.setRuleId("fixture-rule-id");
        request.setRuleVersion("fixture-rule-version");
        request.setFreshnessOwnership("fixture-freshness-owner");
        request.setConflictFamilyOwnership("fixture-conflict-owner");
        request.setAuthenticationVisibility("fixture-review-visibility");
        request.setOwnerEvidenceFields(List.of(
                "sourceTraceEntryOwnershipCompletionPath",
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef",
                "sourceWindow",
                "ruleId",
                "ruleVersion",
                "freshnessOwnership",
                "conflictFamilyOwnership"
        ));
        request.setSourceRefs(List.of("fixture-source-ref"));
        request.setMissingFields(List.of("productionOwnershipReviewBoundaryUnwired"));
        return request;
    }

    private void assertFailClosedReviewOnly(SourceTraceEntryProductionOwnershipReviewResult result) {
        assertThat(result.getReviewStatus())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewStatusEnum.INCOMPLETE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isSourceTraceEntryCompleted()).isFalse();
        assertThat(result.isCompletionReady()).isFalse();
    }

    private boolean containsForbiddenMethodToken(String methodName) {
        String lowerMethodName = methodName.toLowerCase(Locale.ROOT);
        return FORBIDDEN_METHOD_TOKENS.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(lowerMethodName::contains);
    }

    private boolean containsGeneratedTradingValueToken(String methodName) {
        String lowerMethodName = methodName.toLowerCase(Locale.ROOT);
        return GENERATED_VALUE_METHOD_TOKENS.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(lowerMethodName::contains);
    }
}
