package org.example.trademodel.dto.planboundary;

import java.util.List;

final class SourceTraceEntryPositiveCompletionFixtureMapper {

    SourceTraceEntryPositiveCompletionContractDTO fromFixture(
            SourceTraceEntryPositiveCompletionFixtureInput fixtureInput
    ) {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        if (fixtureInput == null) {
            dto.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
            dto.setMissingFields(List.of("fixtureInput"));
            return dto;
        }
        if (fixtureInput.hasRuntimeLikeSourceTags()) {
            dto.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
            dto.setMissingFields(fixtureInput.getSourceTags());
            return dto;
        }

        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        dto.setCompletionTransition(
                SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY
        );
        dto.setSymbol(fixtureInput.getSymbol());
        dto.setTimeframe(fixtureInput.getTimeframe());
        dto.setSourceTraceEntryOwnershipCompletionPath(
                fixtureInput.getSourceTraceEntryOwnershipCompletionPath()
        );
        dto.setEntryPriceSource(fixtureInput.getEntryPriceSource());
        dto.setEntrySourceType(fixtureInput.getEntrySourceType());
        dto.setEntrySourceTimeframe(fixtureInput.getEntrySourceTimeframe());
        dto.setEntrySourceReason(fixtureInput.getEntrySourceReason());
        dto.setEntrySourceRef(fixtureInput.getEntrySourceRef());
        dto.setRuleId(fixtureInput.getRuleId());
        dto.setRuleVersion(fixtureInput.getRuleVersion());
        dto.setSourceWindow(fixtureInput.getSourceWindow());
        dto.setFreshnessStatus(fixtureInput.getFreshnessStatus());
        dto.setObservedAtMs(fixtureInput.getObservedAtMs());
        dto.setDecisionCreateTimeMs(fixtureInput.getDecisionCreateTimeMs());
        dto.setConflictsWithStop(fixtureInput.getConflictsWithStop());
        dto.setConflictsWithTakeProfit(fixtureInput.getConflictsWithTakeProfit());
        dto.setConflictsWithRiskReward(fixtureInput.getConflictsWithRiskReward());
        dto.setConflictsWithLiquidity(fixtureInput.getConflictsWithLiquidity());
        dto.setConflictsWithMultiTimeframe(fixtureInput.getConflictsWithMultiTimeframe());
        dto.setConflictsWithEvent(fixtureInput.getConflictsWithEvent());
        dto.setConflictsWithWick(fixtureInput.getConflictsWithWick());
        dto.setDowngradeReason(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.FIXTURE_ONLY_NOT_PRODUCTION_READY
        );
        dto.setMissingFields(fixtureInput.getMissingFields());
        return dto;
    }
}
