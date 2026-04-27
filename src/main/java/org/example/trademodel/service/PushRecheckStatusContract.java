package org.example.trademodel.service;

import org.example.trademodel.enums.RecheckStatusEnum;

import java.util.Locale;

/**
 * 第九阶段 Step 1：Push/Recheck 统一状态语义契约。
 * <p>
 * 用一套集中映射统一三层语义：
 * recheck_status -> push_status -> review 标签（通过/等待/阻断/终止）。
 */
public final class PushRecheckStatusContract {

    private PushRecheckStatusContract() {
    }

    public enum ReviewTag {
        PASS("通过"),
        WAITING("等待"),
        BLOCKED("阻断"),
        TERMINATED("终止");

        private final String zhLabel;

        ReviewTag(String zhLabel) {
            this.zhLabel = zhLabel;
        }

        public String getZhLabel() {
            return zhLabel;
        }
    }

    public static String toPushStatus(RecheckStatusEnum recheckStatus) {
        if (recheckStatus == null) {
            return "RECHECK_UNKNOWN";
        }
        switch (recheckStatus) {
            case VALID_EXECUTABLE:
                return "RECHECK_VALID_EXECUTABLE";
            case VALID_WAITING:
                return "RECHECK_VALID_WAITING";
            case DRIFTED:
                return "RECHECK_DRIFTED";
            case INVALIDATED:
                return "RECHECK_INVALIDATED";
            case RISK_BLOCKED:
                return "RECHECK_RISK_BLOCKED";
            case CONFUSED_BLOCKED:
                return "RECHECK_CONFUSED_BLOCKED";
            case EXPIRED:
                return "RECHECK_EXPIRED";
            default:
                return "RECHECK_UNKNOWN";
        }
    }

    public static ReviewTag toReviewTag(RecheckStatusEnum recheckStatus) {
        if (recheckStatus == null) {
            return ReviewTag.BLOCKED;
        }
        switch (recheckStatus) {
            case VALID_EXECUTABLE:
                return ReviewTag.PASS;
            case VALID_WAITING:
                return ReviewTag.WAITING;
            case EXPIRED:
                return ReviewTag.TERMINATED;
            case DRIFTED:
            case INVALIDATED:
            case RISK_BLOCKED:
            case CONFUSED_BLOCKED:
            default:
                return ReviewTag.BLOCKED;
        }
    }

    public static ReviewTag toReviewTagByRecheckRaw(String rawRecheckStatus) {
        RecheckStatusEnum status = tryParseRecheckStatus(rawRecheckStatus);
        return toReviewTag(status);
    }

    public static ReviewTag toReviewTagByPushStatus(String pushStatus) {
        if (pushStatus == null || pushStatus.isBlank()) {
            return ReviewTag.BLOCKED;
        }
        if ("CAPTURED".equalsIgnoreCase(pushStatus)) {
            return ReviewTag.WAITING;
        }
        if ("RECHECK_UNKNOWN".equalsIgnoreCase(pushStatus)) {
            return ReviewTag.BLOCKED;
        }
        String normalized = pushStatus.trim().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("RECHECK_")) {
            return ReviewTag.BLOCKED;
        }
        String enumName = normalized.substring("RECHECK_".length());
        RecheckStatusEnum status = tryParseRecheckStatus(enumName);
        return toReviewTag(status);
    }

    public static boolean isPendingPushStatusForScheduler(String pushStatus) {
        return "CAPTURED".equalsIgnoreCase(pushStatus)
                || "RECHECK_VALID_WAITING".equalsIgnoreCase(pushStatus);
    }

    public static RecheckStatusEnum tryParseRecheckStatus(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return RecheckStatusEnum.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }
}
