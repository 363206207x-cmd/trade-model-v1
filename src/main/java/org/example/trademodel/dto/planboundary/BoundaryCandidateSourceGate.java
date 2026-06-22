package org.example.trademodel.dto.planboundary;

import java.util.List;

public final class BoundaryCandidateSourceGate {

    private BoundaryCandidateSourceGate() {
    }

    public static ExecutionPlanSourceGateResultDTO validate(
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields
    ) {
        List<String> missing = NumericBoundarySourceValidator.validate(entry, stop, takeProfitLevels, sourceFields);
        if (missing.isEmpty()) {
            return ExecutionPlanSourceGateResultDTO.valid();
        }
        return ExecutionPlanSourceGateResultDTO.incomplete(missing);
    }

    public static void requireValid(
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields
    ) {
        ExecutionPlanSourceGateResultDTO result = validate(entry, stop, takeProfitLevels, sourceFields);
        if (!result.isValid()) {
            throw new IllegalArgumentException("BoundaryCandidateSourceGate must be VALID: "
                    + String.join("; ", result.getMissingSourceReasons()));
        }
    }
}
