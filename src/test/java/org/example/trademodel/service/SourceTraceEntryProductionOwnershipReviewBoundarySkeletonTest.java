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
    private static final List<String> GENERATED_VALUE_METHOD_TOKENS = List.of(
            "generatedEntry",
            "generatedStop",
            "generatedTakeProfit",
            "generatedRiskReward",
            "stopValue",
            "takeProfitValue",
            "riskRewardValue"
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

    private void assertNoForbiddenMethodNames(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getName))
                .noneMatch(this::containsForbiddenMethodToken);
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
