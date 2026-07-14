package org.example.trademodel.positionmonitorlog;

import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;

/** Removes internal source sentinels from DTOs before they cross a user-visible boundary. */
public final class PositionMonitorLogSourceViewPolicy {
    public static final String SOURCE_VERIFIED = "VERIFIED";
    public static final String SOURCE_UNVERIFIED = "UNVERIFIED";

    private PositionMonitorLogSourceViewPolicy() {
    }

    public static PositionMonitorLogDTO sanitize(PositionMonitorLogDTO dto) {
        if (dto == null) {
            return null;
        }
        String analysisId = trimToNull(dto.getAnalysisId());
        if (analysisId == null || PositionMonitorSourceContract.isUnverifiedAnalysisId(analysisId)) {
            dto.setAnalysisId(null);
            dto.setExecutionPlanId(null);
            dto.setSourceVerified(false);
            dto.setSourceStatus(SOURCE_UNVERIFIED);
            dto.setSourceStatusLabel("来源不可验证");
            return dto;
        }
        dto.setAnalysisId(analysisId);
        dto.setExecutionPlanId(trimToNull(dto.getExecutionPlanId()));
        dto.setSourceVerified(true);
        dto.setSourceStatus(SOURCE_VERIFIED);
        dto.setSourceStatusLabel("来源已验证");
        return dto;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
