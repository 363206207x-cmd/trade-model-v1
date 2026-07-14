package org.example.trademodel.positionmonitorlog;

import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;

/** Separates generic source identifiers from resolver-verified business source identity. */
public final class PositionMonitorLogSourceViewPolicy {
    public static final String SOURCE_VERIFIED = "VERIFIED";
    public static final String SOURCE_PENDING_VERIFICATION = "PENDING_VERIFICATION";
    public static final String SOURCE_UNVERIFIED = "UNVERIFIED";

    private PositionMonitorLogSourceViewPolicy() {
    }

    /** Generic log reads carry identifiers but cannot prove their relationship to a position source. */
    public static PositionMonitorLogDTO sanitize(PositionMonitorLogDTO dto) {
        if (dto == null) {
            return null;
        }
        String analysisId = trimToNull(dto.getAnalysisId());
        if (analysisId == null || PositionMonitorSourceContract.isUnverifiedAnalysisId(analysisId)) {
            return markUnverified(dto, true);
        }
        dto.setAnalysisId(analysisId);
        dto.setExecutionPlanId(trimToNull(dto.getExecutionPlanId()));
        dto.setSourceVerified(false);
        dto.setSourceStatus(SOURCE_PENDING_VERIFICATION);
        dto.setSourceStatusLabel("来源待验证");
        return dto;
    }

    /** Called only after PositionPlanSourceResolver validates typed position, plan, run, and symbol. */
    public static PositionMonitorLogDTO markVerified(PositionMonitorLogDTO dto,
                                                     String analysisId,
                                                     String executionPlanId) {
        if (dto == null) {
            return null;
        }
        String safeAnalysisId = trimToNull(analysisId);
        String safeExecutionPlanId = trimToNull(executionPlanId);
        if (safeAnalysisId == null
                || PositionMonitorSourceContract.isUnverifiedAnalysisId(safeAnalysisId)
                || safeExecutionPlanId == null) {
            return markUnverified(dto, true);
        }
        dto.setAnalysisId(safeAnalysisId);
        dto.setExecutionPlanId(safeExecutionPlanId);
        dto.setSourceVerified(true);
        dto.setSourceStatus(SOURCE_VERIFIED);
        dto.setSourceStatusLabel("来源已验证");
        return dto;
    }

    public static PositionMonitorLogDTO markUnverified(PositionMonitorLogDTO dto, boolean clearIdentifiers) {
        if (dto == null) {
            return null;
        }
        if (clearIdentifiers) {
            dto.setAnalysisId(null);
            dto.setExecutionPlanId(null);
        }
        dto.setSourceVerified(false);
        dto.setSourceStatus(SOURCE_UNVERIFIED);
        dto.setSourceStatusLabel("来源不可验证");
        return dto;
    }

    /** Review Center accepts only resolver-marked rows and hides identifiers from all other rows. */
    public static PositionMonitorLogDTO sanitizeResolvedBusinessView(PositionMonitorLogDTO dto) {
        if (dto == null) {
            return null;
        }
        if (dto.isSourceVerified() && SOURCE_VERIFIED.equals(dto.getSourceStatus())) {
            return markVerified(dto, dto.getAnalysisId(), dto.getExecutionPlanId());
        }
        return markUnverified(dto, true);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
