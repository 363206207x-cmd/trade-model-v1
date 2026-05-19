package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.service.EntryCompletionValidationContextAssembler;
import org.example.trademodel.service.SourceTraceEntryCompletionContract;
import org.example.trademodel.service.SourceTraceEntryOwnershipAdapter;
import org.example.trademodel.service.SourceTraceEntryOwnershipValidator;
import org.example.trademodel.service.impl.FailClosedSourceTraceEntryCompletionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class SourceTraceEntryPositiveCompletionContractDTOTest {

    @Test
    void defaultStateIsFailClosedReviewOnlyAndNonInstructional() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.DEFAULT_FAIL_CLOSED);
        assertThat(dto.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(dto.isManualReviewRequired()).isTrue();
        assertThat(dto.isNotTradeInstruction()).isTrue();
        assertThat(dto.isSourceTraceEntryCompleted()).isFalse();
        assertThat(dto.isCompletionReady()).isFalse();
        assertThat(dto.getMissingFields()).contains(
                "sourceTraceEntryOwnershipCompletionPath",
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef",
                "conflictsWithLiquidity",
                "conflictsWithEvent",
                "conflictsWithWick"
        );
    }

    @Test
    void dtoCarriesAllowedContractShapeWithoutCompletingSourceTrace() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();

        dto.setSymbol("BTCUSDT");
        dto.setTimeframe("15m");
        dto.setSourceTraceEntryOwnershipCompletionPath("fixture-only-completion-path");
        dto.setEntryPriceSource(new BigDecimal("1.00"));
        dto.setEntrySourceType("rule-owned-boundary");
        dto.setEntrySourceTimeframe("15m");
        dto.setEntrySourceReason("fixture-only-source-reason");
        dto.setEntrySourceRef("fixture-source-ref");
        dto.setRuleId("fixture-entry-rule");
        dto.setRuleVersion("fixture-v1");
        dto.setSourceWindow("fixture-window");
        dto.setFreshnessStatus("FRESH");
        dto.setObservedAtMs(100L);
        dto.setDecisionCreateTimeMs(200L);
        dto.setConflictsWithStop(Boolean.FALSE);
        dto.setConflictsWithTakeProfit(Boolean.FALSE);
        dto.setConflictsWithRiskReward(Boolean.FALSE);
        dto.setConflictsWithLiquidity(Boolean.FALSE);
        dto.setConflictsWithMultiTimeframe(Boolean.FALSE);
        dto.setConflictsWithEvent(Boolean.FALSE);
        dto.setConflictsWithWick(Boolean.FALSE);
        dto.setDowngradeReason(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.FIXTURE_ONLY_NOT_PRODUCTION_READY
        );
        dto.setMissingFields(List.of("sourceTraceEntryOwnershipCompletionPath"));

        assertThat(dto.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(dto.getTimeframe()).isEqualTo("15m");
        assertThat(dto.getSourceTraceEntryOwnershipCompletionPath())
                .isEqualTo("fixture-only-completion-path");
        assertThat(dto.getEntryPriceSource()).isEqualByComparingTo("1.00");
        assertThat(dto.getEntrySourceType()).isEqualTo("rule-owned-boundary");
        assertThat(dto.getEntrySourceTimeframe()).isEqualTo("15m");
        assertThat(dto.getEntrySourceReason()).isEqualTo("fixture-only-source-reason");
        assertThat(dto.getEntrySourceRef()).isEqualTo("fixture-source-ref");
        assertThat(dto.getRuleId()).isEqualTo("fixture-entry-rule");
        assertThat(dto.getRuleVersion()).isEqualTo("fixture-v1");
        assertThat(dto.getSourceWindow()).isEqualTo("fixture-window");
        assertThat(dto.getFreshnessStatus()).isEqualTo("FRESH");
        assertThat(dto.getObservedAtMs()).isEqualTo(100L);
        assertThat(dto.getDecisionCreateTimeMs()).isEqualTo(200L);
        assertThat(dto.getConflictsWithStop()).isFalse();
        assertThat(dto.getConflictsWithTakeProfit()).isFalse();
        assertThat(dto.getConflictsWithRiskReward()).isFalse();
        assertThat(dto.getConflictsWithLiquidity()).isFalse();
        assertThat(dto.getConflictsWithMultiTimeframe()).isFalse();
        assertThat(dto.getConflictsWithEvent()).isFalse();
        assertThat(dto.getConflictsWithWick()).isFalse();
        assertThat(dto.getMissingFields()).containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertStillNonProduction(dto);
    }

    @Test
    void positiveFixtureReadyMetadataDoesNotBecomeProductionReadiness() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        dto.setCompletionTransition(
                SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY
        );
        dto.setDowngradeReason(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.FIXTURE_ONLY_NOT_PRODUCTION_READY
        );

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY);
        assertStillNonProduction(dto);
        assertThat(dto.getMissingFields()).isNotEmpty();
    }

    @Test
    void nullStatusTransitionDowngradeAndMissingFieldsNormalizeBackToFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_DESIGN_REVIEW_ONLY);
        dto.setCompletionTransition(
                SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_DESIGN_REVIEW_ONLY
        );
        dto.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
        dto.setMissingFields(List.of("fixture-field"));

        dto.setCompletionStatus(null);
        dto.setCompletionTransition(null);
        dto.setDowngradeReason(null);
        dto.setMissingFields(null);

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.DEFAULT_FAIL_CLOSED);
        assertThat(dto.getMissingFields()).contains("sourceTraceEntryOwnershipCompletionPath");
        assertStillNonProduction(dto);
    }

    @Test
    void emptyMissingFieldsNormalizeBackToFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();

        dto.setMissingFields(List.of());

        assertThat(dto.getMissingFields()).contains(
                "sourceTraceEntryOwnershipCompletionPath",
                "entryPriceSource",
                "conflictsWithEvent"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void missingFieldsGetterReturnsDefensiveCopy() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        dto.setMissingFields(List.of("sourceTraceEntryOwnershipCompletionPath"));

        List<String> returnedMissingFields = dto.getMissingFields();
        returnedMissingFields.add("mutated-outside-dto");

        assertThat(dto.getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertStillNonProduction(dto);
    }

    @Test
    void settingMissingFieldsFromMutableListDoesNotRetainExternalMutation() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        List<String> mutableMissingFields = new ArrayList<>();
        mutableMissingFields.add("sourceTraceEntryOwnershipCompletionPath");

        dto.setMissingFields(mutableMissingFields);
        mutableMissingFields.add("entryPriceSource");

        assertThat(dto.getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertStillNonProduction(dto);
    }

    @Test
    void transitionStatusMismatchDoesNotImplyReadiness() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                syntheticFixtureDto();
        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        dto.setCompletionTransition(
                SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY
        );
        dto.setMissingFields(List.of("transitionStatusMismatch"));

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY);
        assertThat(dto.getMissingFields()).containsExactly("transitionStatusMismatch");
        assertStillNonProduction(dto);
    }

    @Test
    void unsafeDowngradeReasonDoesNotChangeReviewOnlySafetyFlags() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                syntheticFixtureDto();
        dto.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);

        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
        assertStillNonProduction(dto);
    }

    @Test
    void sourceTraceEntryCompletedRemainsFalseEvenWithPositiveStatus() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                syntheticFixtureDto();
        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        assertThat(dto.isSourceTraceEntryCompleted()).isFalse();
        assertStillNonProduction(dto);
    }

    @Test
    void completionReadyRemainsFalseEvenWithPositiveTransition() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                syntheticFixtureDto();
        dto.setCompletionTransition(
                SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY
        );

        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY);
        assertThat(dto.isCompletionReady()).isFalse();
        assertStillNonProduction(dto);
    }

    @Test
    void dtoAcceptsSyntheticFixtureValuesButDoesNotInferRealEntryReadiness() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                syntheticFixtureDto();
        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        dto.setCompletionTransition(
                SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY
        );

        assertThat(dto.getSourceTraceEntryOwnershipCompletionPath())
                .isEqualTo("fixture-only-completion-path");
        assertThat(dto.getEntryPriceSource()).isEqualByComparingTo("1.00");
        assertThat(dto.getEntrySourceReason()).isEqualTo("fixture-only-source-reason");
        assertThat(dto.getEntrySourceRef()).isEqualTo("fixture-source-ref");
        assertThat(dto.getMissingFields()).containsExactly("fixture-only-not-runtime-ready");
        assertStillNonProduction(dto);
    }

    @Test
    void dtoExposesNoOrderExecutionCloseReverseAutoTradingOrTradeReadyMethodNames() {
        assertNoForbiddenMethodNames(SourceTraceEntryPositiveCompletionContractDTO.class);
    }

    @Test
    void dtoIsNotASpringServiceAndDoesNotImplementProductionBoundaries() {
        Class<SourceTraceEntryPositiveCompletionContractDTO> dtoType =
                SourceTraceEntryPositiveCompletionContractDTO.class;

        assertThat(dtoType.getAnnotation(Service.class)).isNull();
        assertThat(dtoType.getAnnotation(Component.class)).isNull();
        assertThat(SourceTraceEntryCompletionContract.class.isAssignableFrom(dtoType)).isFalse();
        assertThat(SourceTraceEntryOwnershipAdapter.class.isAssignableFrom(dtoType)).isFalse();
        assertThat(SourceTraceEntryOwnershipValidator.class.isAssignableFrom(dtoType)).isFalse();
        assertThat(EntryCompletionValidationContextAssembler.class.isAssignableFrom(dtoType)).isFalse();
        assertThat(FailClosedSourceTraceEntryCompletionResolver.class.isAssignableFrom(dtoType)).isFalse();
    }

    @Test
    void dtoDoesNotRequireProductionAdapterOrProductionCompletionContract() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void dtoEnumNamesStayFixtureOnlyOrReviewOnly() {
        assertThat(SourceTraceEntryPositiveCompletionStatusEnum.values())
                .containsExactly(
                        SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE,
                        SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY,
                        SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_DESIGN_REVIEW_ONLY
                );
        assertThat(SourceTraceEntryPositiveCompletionTransitionEnum.values())
                .containsExactly(
                        SourceTraceEntryPositiveCompletionTransitionEnum.NONE,
                        SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY,
                        SourceTraceEntryPositiveCompletionTransitionEnum.POSITIVE_FIXTURE_READY_TO_INCOMPLETE,
                        SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_DESIGN_REVIEW_ONLY,
                        SourceTraceEntryPositiveCompletionTransitionEnum.POSITIVE_DESIGN_REVIEW_ONLY_TO_INCOMPLETE
                );
    }

    private SourceTraceEntryPositiveCompletionContractDTO syntheticFixtureDto() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        dto.setSymbol("BTCUSDT");
        dto.setTimeframe("15m");
        dto.setSourceTraceEntryOwnershipCompletionPath("fixture-only-completion-path");
        dto.setEntryPriceSource(new BigDecimal("1.00"));
        dto.setEntrySourceType("rule-owned-boundary");
        dto.setEntrySourceTimeframe("15m");
        dto.setEntrySourceReason("fixture-only-source-reason");
        dto.setEntrySourceRef("fixture-source-ref");
        dto.setRuleId("fixture-entry-rule");
        dto.setRuleVersion("fixture-v1");
        dto.setSourceWindow("fixture-window");
        dto.setFreshnessStatus("FRESH");
        dto.setObservedAtMs(100L);
        dto.setDecisionCreateTimeMs(200L);
        dto.setConflictsWithStop(Boolean.FALSE);
        dto.setConflictsWithTakeProfit(Boolean.FALSE);
        dto.setConflictsWithRiskReward(Boolean.FALSE);
        dto.setConflictsWithLiquidity(Boolean.FALSE);
        dto.setConflictsWithMultiTimeframe(Boolean.FALSE);
        dto.setConflictsWithEvent(Boolean.FALSE);
        dto.setConflictsWithWick(Boolean.FALSE);
        dto.setDowngradeReason(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.FIXTURE_ONLY_NOT_PRODUCTION_READY
        );
        dto.setMissingFields(List.of("fixture-only-not-runtime-ready"));
        return dto;
    }

    private void assertStillNonProduction(SourceTraceEntryPositiveCompletionContractDTO dto) {
        assertThat(dto.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(dto.isManualReviewRequired()).isTrue();
        assertThat(dto.isNotTradeInstruction()).isTrue();
        assertThat(dto.isSourceTraceEntryCompleted()).isFalse();
        assertThat(dto.isCompletionReady()).isFalse();
    }

    private void assertNoForbiddenMethodNames(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("order");
                    assertThat(name).doesNotContain("execution");
                    assertThat(name).doesNotContain("execute");
                    assertThat(name).doesNotContain("close");
                    assertThat(name).doesNotContain("reverse");
                    assertThat(name).doesNotContain("autotrading");
                    assertThat(name).doesNotContain("auto");
                    assertThat(name).doesNotContain("tradeready");
                });
    }
}
