package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class SourceTraceEntryProductionOwnershipReviewBoundarySkeletonTest {

    private static final List<Class<?>> SKELETON_TYPES = List.of(
            SourceTraceEntryProductionOwnershipReviewBoundary.class,
            SourceTraceEntryProductionOwnershipReviewRequest.class,
            SourceTraceEntryProductionOwnershipReviewResult.class,
            SourceTraceEntryProductionOwnershipAuditEnvelope.class,
            SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope.class,
            SourceTraceEntryProductionOwnershipReviewStatusEnum.class,
            SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.class
    );
    private static final List<String> FORBIDDEN_METHOD_TOKENS = List.of(
            "order",
            "execution",
            "close",
            "reverse",
            "autoTrading",
            "autoTrade",
            "tradeReady",
            "readyToTrade"
    );
    private static final List<String> BOUNDARY_FORBIDDEN_METHOD_TOKENS = List.of(
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
    private static final List<String> OWNER_EVIDENCE_FIELDS = List.of(
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
    private static final List<String> RUNTIME_SUBSTITUTION_BLOCKERS = List.of(
            "latestPriceOnly",
            "rawKlineOnly",
            "aiText",
            "dashboardText",
            "externalData",
            "orderData",
            "executionData"
    );
    private static final List<String> PRODUCTION_CONSUMER_BLOCKERS = List.of(
            "boundaryCandidateServiceValidIsolation",
            "executionPlanReadinessIsolation",
            "dashboardMutationIsolation",
            "orderPathIsolation",
            "executionPathIsolation",
            "automationPathIsolation",
            "externalDataPathIsolation"
    );
    private static final List<String> RISK_ACTION_GUARD_BLOCKERS = List.of(
            "highRiskDoesNotMeanAction",
            "wickPinBarNotTrendReversal",
            "liquidityStressRequiresReview",
            "missingEventDataNotNoRisk",
            "multiTimeframeAgreementNotSourceTraceCompletion"
    );

    @Test
    void resultDefaultsRemainFailClosedReviewOnlyAndNonInstructional() {
        SourceTraceEntryProductionOwnershipReviewResult result =
                new SourceTraceEntryProductionOwnershipReviewResult();

        assertThat(result.getReviewStatus())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewStatusEnum.INCOMPLETE);
        assertThat(result.getDowngradeReason())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.DEFAULT_FAIL_CLOSED);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isSourceTraceEntryCompleted()).isFalse();
        assertThat(result.isCompletionReady()).isFalse();
        assertThat(result.getMissingFields()).contains(
                "sourceTraceEntryOwnershipCompletionPath",
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef",
                "auditEnvelope",
                "consumerIsolationEnvelope",
                "authenticationVisibility"
        );
        assertThat(result.getBlockingFields()).contains(
                "productionOwnershipReviewBoundaryUnwired",
                "productionWiringStillBlocked"
        );
        assertThat(result.getUnsafeFields()).isEmpty();
    }

    @Test
    void requestDefaultsKeepOwnerEvidenceMissingAndSafetyEnvelopesFailClosed() {
        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();

        assertThat(request.getMissingFields()).contains(
                "sourceTraceEntryOwnershipCompletionPath",
                "entryPriceSource",
                "freshnessOwnership",
                "conflictFamilyOwnership",
                "auditEnvelope",
                "consumerIsolationEnvelope"
        );
        assertThat(request.getOwnerEvidenceFields()).isEmpty();
        assertThat(request.getSourceRefs()).isEmpty();
        assertThat(request.getAuditEnvelope()).isNotNull();
        assertThat(request.getAuditEnvelope().isAuditEvidencePresent()).isFalse();
        assertThat(request.getConsumerIsolationEnvelope()).isNotNull();
        assertThat(request.getConsumerIsolationEnvelope().isIsolationEvidencePresent()).isFalse();
    }

    @Test
    void requestCarriesSyntheticOwnerMetadataWithoutCreatingReadiness() {
        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();
        request.setSymbol("BTCUSDT");
        request.setTimeframe("15m");
        request.setSourceTraceEntryOwnershipCompletionPath("fixture-completion-path-owner");
        request.setEntryPriceSource("fixture-entry-source-metadata");
        request.setEntrySourceType("RULE_OWNED_BOUNDARY");
        request.setEntrySourceTimeframe("15m");
        request.setEntrySourceReason("fixture-review-reason");
        request.setEntrySourceRef("fixture-source-ref");
        request.setSourceWindow("fixture-source-window");
        request.setRuleId("fixture-rule");
        request.setRuleVersion("fixture-v1");
        request.setFreshnessOwnership("fixture-freshness-owner");
        request.setConflictFamilyOwnership("fixture-conflict-family-owner");
        request.setAuthenticationVisibility("fixture-visible-for-review");
        request.setOwnerEvidenceFields(List.of("sourceTraceEntryOwnershipCompletionPath", "entryPriceSource"));
        request.setSourceRefs(List.of("fixture-source-ref"));
        request.setMissingFields(List.of("productionOwnershipReviewBoundaryUnwired"));

        assertThat(request.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.getTimeframe()).isEqualTo("15m");
        assertThat(request.getSourceTraceEntryOwnershipCompletionPath())
                .isEqualTo("fixture-completion-path-owner");
        assertThat(request.getEntryPriceSource()).isEqualTo("fixture-entry-source-metadata");
        assertThat(request.getEntrySourceType()).isEqualTo("RULE_OWNED_BOUNDARY");
        assertThat(request.getEntrySourceTimeframe()).isEqualTo("15m");
        assertThat(request.getEntrySourceReason()).isEqualTo("fixture-review-reason");
        assertThat(request.getEntrySourceRef()).isEqualTo("fixture-source-ref");
        assertThat(request.getSourceWindow()).isEqualTo("fixture-source-window");
        assertThat(request.getRuleId()).isEqualTo("fixture-rule");
        assertThat(request.getRuleVersion()).isEqualTo("fixture-v1");
        assertThat(request.getFreshnessOwnership()).isEqualTo("fixture-freshness-owner");
        assertThat(request.getConflictFamilyOwnership()).isEqualTo("fixture-conflict-family-owner");
        assertThat(request.getAuthenticationVisibility()).isEqualTo("fixture-visible-for-review");
        assertThat(request.getOwnerEvidenceFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath", "entryPriceSource");
        assertThat(request.getSourceRefs()).containsExactly("fixture-source-ref");
        assertThat(request.getMissingFields()).containsExactly("productionOwnershipReviewBoundaryUnwired");
    }

    @Test
    void auditAndConsumerIsolationEnvelopesDefaultToMissingEvidence() {
        SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope =
                new SourceTraceEntryProductionOwnershipAuditEnvelope();
        SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope isolationEnvelope =
                new SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope();

        assertThat(auditEnvelope.isAuditEvidencePresent()).isFalse();
        assertThat(auditEnvelope.getMissingAuditFields()).contains(
                "ownershipFieldKey",
                "ownerFamily",
                "ownerId",
                "sourceRef",
                "consumerIsolationProof"
        );
        assertThat(isolationEnvelope.isIsolationEvidencePresent()).isFalse();
        assertThat(isolationEnvelope.getMissingIsolationFields()).contains(
                "boundaryCandidateServiceValidIsolation",
                "executionPlanReadinessIsolation",
                "dashboardMutationIsolation",
                "orderPathIsolation",
                "executionPathIsolation",
                "externalDataPathIsolation"
        );
        assertThat(isolationEnvelope.getBlockedConsumerFamilies()).contains(
                "boundaryCandidateServiceValidIsolation",
                "executionPlanReadinessIsolation",
                "orderPathIsolation",
                "executionPathIsolation"
        );
    }

    @Test
    void listFieldsUseDefensiveCopies() {
        SourceTraceEntryProductionOwnershipReviewResult result =
                new SourceTraceEntryProductionOwnershipReviewResult();
        List<String> mutableMissing = new ArrayList<>();
        mutableMissing.add("sourceTraceEntryOwnershipCompletionPath");
        result.setMissingFields(mutableMissing);
        mutableMissing.add("entryPriceSource");

        List<String> returnedMissing = result.getMissingFields();
        returnedMissing.add("mutated-outside-result");

        assertThat(result.getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");

        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();
        List<String> sourceRefs = new ArrayList<>();
        sourceRefs.add("fixture-source-ref");
        request.setSourceRefs(sourceRefs);
        sourceRefs.add("mutated-source-ref");

        assertThat(request.getSourceRefs()).containsExactly("fixture-source-ref");
    }

    @Test
    void nullAndEmptyCollectionsNormalizeBackToFailClosedDefaults() {
        SourceTraceEntryProductionOwnershipReviewResult result =
                new SourceTraceEntryProductionOwnershipReviewResult();
        result.setReviewStatus(null);
        result.setDowngradeReason(null);
        result.setMissingFields(List.of());
        result.setBlockingFields(null);

        assertThat(result.getReviewStatus())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewStatusEnum.INCOMPLETE);
        assertThat(result.getDowngradeReason())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.DEFAULT_FAIL_CLOSED);
        assertThat(result.getMissingFields()).contains("sourceTraceEntryOwnershipCompletionPath");
        assertThat(result.getBlockingFields()).contains("productionOwnershipReviewBoundaryUnwired");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isSourceTraceEntryCompleted()).isFalse();
        assertThat(result.isCompletionReady()).isFalse();
    }

    @Test
    void boundaryInterfaceExposesOnlyReadOnlyReviewShape() throws NoSuchMethodException {
        Method method = SourceTraceEntryProductionOwnershipReviewBoundary.class.getDeclaredMethod(
                "reviewEntryOwnership",
                SourceTraceEntryProductionOwnershipReviewRequest.class
        );

        assertThat(method.getReturnType()).isEqualTo(SourceTraceEntryProductionOwnershipReviewResult.class);
        assertThat(Arrays.stream(SourceTraceEntryProductionOwnershipReviewBoundary.class.getDeclaredMethods()))
                .extracting(Method::getName)
                .containsExactly("reviewEntryOwnership");
        assertNoForbiddenMethodNames(SourceTraceEntryProductionOwnershipReviewBoundary.class);
    }

    @Test
    void skeletonTypesHaveNoSpringAnnotationsAndNoProductionBoundaryImplementations() {
        for (Class<?> type : SKELETON_TYPES) {
            assertThat(type.getAnnotation(Service.class)).isNull();
            assertThat(type.getAnnotation(Component.class)).isNull();
            assertThat(type.getAnnotation(Repository.class)).isNull();
            assertThat(type.getAnnotation(Controller.class)).isNull();
            assertThat(type.getAnnotation(RestController.class)).isNull();
            assertNoForbiddenMethodNames(type);
        }

        assertThat(SourceTraceEntryProductionOwnershipReviewRequest.class.getInterfaces()).isEmpty();
        assertThat(SourceTraceEntryProductionOwnershipReviewResult.class.getInterfaces()).isEmpty();
        assertThat(SourceTraceEntryProductionOwnershipAuditEnvelope.class.getInterfaces()).isEmpty();
        assertThat(SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope.class.getInterfaces()).isEmpty();
    }

    @Test
    void productionAdaptersCompletionContractsAndDefaultImplementationsRemainAbsent() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryProductionOwnershipReviewBoundary"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void skeletonHasNoGeneratedEntryStopTakeProfitOrRiskRewardValueSurface() {
        for (Class<?> type : SKELETON_TYPES) {
            assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getName))
                    .noneMatch(this::containsGeneratedTradingValueToken);
            assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getReturnType))
                    .doesNotContain(BigDecimal.class);
            assertThat(Arrays.stream(type.getDeclaredMethods())
                    .flatMap(method -> Arrays.stream(method.getParameterTypes())))
                    .doesNotContain(BigDecimal.class);
        }
    }

    @Test
    void resultSafetyFlagsCannotBeChangedBySetters() {
        assertThat(Arrays.stream(SourceTraceEntryProductionOwnershipReviewResult.class.getDeclaredMethods())
                .map(Method::getName))
                .doesNotContain(
                        "setManualReviewRequired",
                        "setNotTradeInstruction",
                        "setSourceTraceEntryCompleted",
                        "setCompletionReady"
                );
    }

    @Test
    void nullRequestFailsClosedWithoutCompletingOwnershipReview() {
        SourceTraceEntryProductionOwnershipReviewResult result = failClosedResult(
                List.of("request"),
                List.of(),
                List.of("nullRequest", "productionOwnershipReviewBoundaryUnwired", "productionWiringStillBlocked")
        );

        assertFailClosedReviewOnly(result);
        assertThat(result.getMissingFields()).containsExactly("request");
        assertThat(result.getBlockingFields()).contains(
                "nullRequest",
                "productionOwnershipReviewBoundaryUnwired",
                "productionWiringStillBlocked"
        );
    }

    @Test
    void missingOwnerEvidenceFailsClosedAcrossEveryRequiredOwnershipField() {
        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();
        SourceTraceEntryProductionOwnershipReviewResult result = failClosedResult(
                request.getMissingFields(),
                List.of(),
                List.of("ownerEvidenceMissing", "productionOwnershipReviewBoundaryUnwired")
        );

        assertFailClosedReviewOnly(result);
        assertThat(result.getMissingFields()).containsAll(OWNER_EVIDENCE_FIELDS);
        assertThat(result.getBlockingFields()).contains("ownerEvidenceMissing");
        assertThat(request.getOwnerEvidenceFields()).isEmpty();
    }

    @Test
    void duplicateAmbiguousAndStaleOwnerEvidenceFailClosed() {
        for (String blocker : List.of("duplicateOwnerEvidence", "ambiguousOwnerEvidence", "staleOwnerEvidence")) {
            SourceTraceEntryProductionOwnershipReviewRequest request =
                    new SourceTraceEntryProductionOwnershipReviewRequest();
            request.setOwnerEvidenceFields(List.of("entryPriceSource", "entryPriceSource"));
            request.setSourceRefs(List.of("fixture-ref-a", "fixture-ref-b"));

            SourceTraceEntryProductionOwnershipReviewResult result = failClosedResult(
                    List.of("entryPriceSource"),
                    List.of(blocker),
                    List.of(blocker, "productionOwnershipReviewBoundaryUnwired")
            );

            assertFailClosedReviewOnly(result);
            assertThat(result.getUnsafeFields()).contains(blocker);
            assertThat(result.getBlockingFields()).contains(blocker);
            assertThat(request.getSourceRefs()).containsExactly("fixture-ref-a", "fixture-ref-b");
        }
    }

    @Test
    void runtimeLikeSubstitutionEvidenceFailsClosedOneSourceAtATime() {
        for (String substitutionBlocker : RUNTIME_SUBSTITUTION_BLOCKERS) {
            SourceTraceEntryProductionOwnershipReviewRequest request =
                    new SourceTraceEntryProductionOwnershipReviewRequest();
            request.setEntryPriceSource(substitutionBlocker);
            request.setOwnerEvidenceFields(List.of(substitutionBlocker));

            SourceTraceEntryProductionOwnershipReviewResult result = failClosedResult(
                    List.of("entryPriceSource"),
                    List.of(substitutionBlocker),
                    List.of("forbiddenRuntimeSubstitution", substitutionBlocker)
            );

            assertFailClosedReviewOnly(result);
            assertThat(result.getUnsafeFields()).contains(substitutionBlocker);
            assertThat(result.getBlockingFields()).contains("forbiddenRuntimeSubstitution", substitutionBlocker);
            assertThat(request.getEntryPriceSource()).isEqualTo(substitutionBlocker);
        }
    }

    @Test
    void missingAuditVisibilityAndConsumerIsolationEvidenceFailClosed() {
        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();
        request.setAuditEnvelope(null);
        request.setConsumerIsolationEnvelope(null);

        SourceTraceEntryProductionOwnershipReviewResult result = failClosedResult(
                List.of("auditEnvelope", "authenticationVisibility", "consumerIsolationEnvelope"),
                List.of(),
                List.of(
                        "auditMetadataMissing",
                        "authenticationVisibilityMissing",
                        "consumerIsolationMissing",
                        "productionOwnershipReviewBoundaryUnwired"
                )
        );

        assertFailClosedReviewOnly(result);
        assertThat(result.getMissingFields()).contains(
                "auditEnvelope",
                "authenticationVisibility",
                "consumerIsolationEnvelope"
        );
        assertThat(result.getBlockingFields()).contains(
                "auditMetadataMissing",
                "authenticationVisibilityMissing",
                "consumerIsolationMissing"
        );
        assertThat(request.getAuditEnvelope().isAuditEvidencePresent()).isFalse();
        assertThat(request.getConsumerIsolationEnvelope().isIsolationEvidencePresent()).isFalse();
    }

    @Test
    void riskActionGuardBlockersRemainReviewOnlyAndBlockCompletion() {
        SourceTraceEntryProductionOwnershipReviewResult result = failClosedResult(
                List.of("riskActionGuardReview"),
                RISK_ACTION_GUARD_BLOCKERS,
                RISK_ACTION_GUARD_BLOCKERS
        );

        assertFailClosedReviewOnly(result);
        assertThat(result.getUnsafeFields()).containsAll(RISK_ACTION_GUARD_BLOCKERS);
        assertThat(result.getBlockingFields()).containsAll(RISK_ACTION_GUARD_BLOCKERS);
    }

    @Test
    void positiveLookingLabelsDoNotImplyCompletionReadinessValidityOrRuntimeConsumers() {
        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();
        request.setEntrySourceReason("valid-completed-signal-buy-sell-open-looking-label");
        request.setOwnerEvidenceFields(List.of("positive-looking-owner"));
        request.setSourceRefs(List.of("ready-to-trade-looking-ref"));

        SourceTraceEntryProductionOwnershipReviewResult result = failClosedResult(
                List.of("productionOwnershipReviewBoundaryUnwired"),
                List.of(
                        "validLookingValue",
                        "completedLookingValue",
                        "signalLookingValue",
                        "buySellOpenLookingValue"
                ),
                PRODUCTION_CONSUMER_BLOCKERS
        );

        assertFailClosedReviewOnly(result);
        assertThat(result.getUnsafeFields()).contains(
                "validLookingValue",
                "completedLookingValue",
                "signalLookingValue",
                "buySellOpenLookingValue"
        );
        assertThat(result.getBlockingFields()).containsAll(PRODUCTION_CONSUMER_BLOCKERS);
        assertThat(request.getEntrySourceReason()).contains("valid-completed-signal");
    }

    @Test
    void downgradeAndRollbackOutputsPreserveFailClosedSafetyFlagsAndBlockerEvidence() {
        SourceTraceEntryProductionOwnershipReviewResult downgraded = failClosedResult(
                List.of("unsafeOwnerEvidence"),
                List.of("downgradeRequired"),
                List.of("downgradeRequired", "productionWiringStillBlocked")
        );
        downgraded.setDowngradeReason(
                SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.UNSAFE_OWNER_EVIDENCE);

        SourceTraceEntryProductionOwnershipReviewResult rolledBack = failClosedResult(
                List.of("rollbackReason"),
                List.of("rollbackRequired"),
                List.of("rollbackRequired", "productionOwnershipReviewBoundaryUnwired")
        );
        rolledBack.setDowngradeReason(
                SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.REVIEW_BOUNDARY_UNWIRED);

        assertFailClosedReviewOnly(downgraded);
        assertFailClosedReviewOnly(rolledBack);
        assertThat(downgraded.getDowngradeReason())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.UNSAFE_OWNER_EVIDENCE);
        assertThat(rolledBack.getDowngradeReason())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.REVIEW_BOUNDARY_UNWIRED);
        assertThat(downgraded.getBlockingFields()).contains("downgradeRequired");
        assertThat(rolledBack.getBlockingFields()).contains("rollbackRequired");
    }

    @Test
    void boundaryInterfaceMethodNamesExposeNoTradeReadyValiditySignalOrActionSurface() {
        assertThat(Arrays.stream(SourceTraceEntryProductionOwnershipReviewBoundary.class.getDeclaredMethods())
                .map(Method::getName))
                .noneMatch(this::containsBoundaryForbiddenMethodToken);
    }

    private SourceTraceEntryProductionOwnershipReviewResult failClosedResult(
            List<String> missingFields,
            List<String> unsafeFields,
            List<String> blockingFields
    ) {
        SourceTraceEntryProductionOwnershipReviewResult result =
                new SourceTraceEntryProductionOwnershipReviewResult();
        result.setMissingFields(missingFields);
        result.setUnsafeFields(unsafeFields);
        result.setBlockingFields(blockingFields);
        return result;
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

    private void assertNoForbiddenMethodNames(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getName))
                .noneMatch(this::containsForbiddenMethodToken);
    }

    private boolean containsBoundaryForbiddenMethodToken(String methodName) {
        String lowerMethodName = methodName.toLowerCase(Locale.ROOT);
        return BOUNDARY_FORBIDDEN_METHOD_TOKENS.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(lowerMethodName::contains);
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
