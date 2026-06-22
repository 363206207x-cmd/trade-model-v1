package org.example.trademodel.service;

import org.example.trademodel.enums.RecheckStatusEnum;

import java.util.Locale;

/**
 * Push/Recheck 统一状态语义契约。
 * <p>
 * 新写入只使用 review-only 状态；历史状态只在本类集中兼容读取。
 */
public final class PushRecheckStatusContract {

    private static final String LEGACY_RECHECK_VALID_EXECUTABLE = "VALID_EXECUTABLE";
    private static final String LEGACY_RECHECK_VALID_WAITING = "VALID_WAITING";
    private static final String LEGACY_RECHECK_DRIFTED = "DRIFTED";
    private static final String LEGACY_PUSH_VALID_EXECUTABLE = "RECHECK_VALID_EXECUTABLE";
    private static final String LEGACY_PUSH_VALID_WAITING = "RECHECK_VALID_WAITING";
    private static final String LEGACY_PUSH_DRIFTED = "RECHECK_DRIFTED";

    public static final String PUSH_STATUS_CAPTURED = "CAPTURED";
    public static final String PUSH_STATUS_REVIEW_PASSED = "RECHECK_REVIEW_PASSED";
    public static final String PUSH_STATUS_REVIEW_WAITING = "RECHECK_REVIEW_WAITING";
    public static final String PUSH_STATUS_DRIFTED_FROM_ENTRY_ZONE = "RECHECK_DRIFTED_FROM_ENTRY_ZONE";
    public static final String PUSH_STATUS_INVALIDATED = "RECHECK_INVALIDATED";
    public static final String PUSH_STATUS_RISK_BLOCKED = "RECHECK_RISK_BLOCKED";
    public static final String PUSH_STATUS_CONFUSED_BLOCKED = "RECHECK_CONFUSED_BLOCKED";
    public static final String PUSH_STATUS_EXPIRED = "RECHECK_EXPIRED";
    public static final String PUSH_STATUS_UNKNOWN = "RECHECK_UNKNOWN";

    private PushRecheckStatusContract() {
    }

    public enum ReviewTag {
        PASS("复查通过（仅供人工复核）"),
        WAITING("等待人工复核"),
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
            return PUSH_STATUS_UNKNOWN;
        }
        return switch (recheckStatus) {
            case REVIEW_PASSED -> PUSH_STATUS_REVIEW_PASSED;
            case REVIEW_WAITING -> PUSH_STATUS_REVIEW_WAITING;
            case DRIFTED_FROM_ENTRY_ZONE, DRIFTED -> PUSH_STATUS_DRIFTED_FROM_ENTRY_ZONE;
            case INVALIDATED -> PUSH_STATUS_INVALIDATED;
            case RISK_BLOCKED -> PUSH_STATUS_RISK_BLOCKED;
            case CONFUSED_BLOCKED -> PUSH_STATUS_CONFUSED_BLOCKED;
            case EXPIRED -> PUSH_STATUS_EXPIRED;
        };
    }

    public static ReviewTag toReviewTag(RecheckStatusEnum recheckStatus) {
        if (recheckStatus == null) {
            return ReviewTag.BLOCKED;
        }
        return switch (recheckStatus) {
            case REVIEW_PASSED -> ReviewTag.PASS;
            case REVIEW_WAITING -> ReviewTag.WAITING;
            case EXPIRED -> ReviewTag.TERMINATED;
            case DRIFTED_FROM_ENTRY_ZONE, DRIFTED, INVALIDATED, RISK_BLOCKED, CONFUSED_BLOCKED -> ReviewTag.BLOCKED;
        };
    }

    public static ReviewTag toReviewTagByRecheckRaw(String rawRecheckStatus) {
        RecheckStatusEnum status = tryParseRecheckStatus(rawRecheckStatus);
        return toReviewTag(status);
    }

    public static ReviewTag toReviewTagByPushStatus(String pushStatus) {
        if (pushStatus == null || pushStatus.isBlank()) {
            return ReviewTag.BLOCKED;
        }
        if (PUSH_STATUS_CAPTURED.equalsIgnoreCase(pushStatus)) {
            return ReviewTag.WAITING;
        }
        if (PUSH_STATUS_UNKNOWN.equalsIgnoreCase(pushStatus)) {
            return ReviewTag.BLOCKED;
        }
        String normalized = canonicalizePushStatus(pushStatus);
        if (!normalized.startsWith("RECHECK_")) {
            return ReviewTag.BLOCKED;
        }
        String enumName = normalized.substring("RECHECK_".length());
        RecheckStatusEnum status = tryParseRecheckStatus(enumName);
        return toReviewTag(status);
    }

    public static boolean isPendingPushStatusForScheduler(String pushStatus) {
        String normalized = canonicalizePushStatus(pushStatus);
        return PUSH_STATUS_CAPTURED.equalsIgnoreCase(normalized)
                || PUSH_STATUS_REVIEW_WAITING.equalsIgnoreCase(normalized);
    }

    public static RecheckStatusEnum tryParseRecheckStatus(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_RECHECK_VALID_EXECUTABLE.equals(normalized)) {
            return RecheckStatusEnum.REVIEW_PASSED;
        }
        if (LEGACY_RECHECK_VALID_WAITING.equals(normalized)) {
            return RecheckStatusEnum.REVIEW_WAITING;
        }
        if (LEGACY_RECHECK_DRIFTED.equals(normalized)) {
            return RecheckStatusEnum.DRIFTED;
        }
        try {
            return RecheckStatusEnum.valueOf(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    public static String canonicalizeRecheckStatusName(String raw) {
        if (raw != null && LEGACY_RECHECK_DRIFTED.equals(raw.trim().toUpperCase(Locale.ROOT))) {
            return RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE.name();
        }
        RecheckStatusEnum parsed = tryParseRecheckStatus(raw);
        return parsed != null ? parsed.name() : raw;
    }

    public static String canonicalizePushStatus(String raw) {
        if (raw == null) {
            return PUSH_STATUS_UNKNOWN;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_PUSH_VALID_EXECUTABLE.equals(normalized)) {
            return PUSH_STATUS_REVIEW_PASSED;
        }
        if (LEGACY_PUSH_VALID_WAITING.equals(normalized)) {
            return PUSH_STATUS_REVIEW_WAITING;
        }
        if (LEGACY_PUSH_DRIFTED.equals(normalized)) {
            return PUSH_STATUS_DRIFTED_FROM_ENTRY_ZONE;
        }
        return normalized;
    }

    public static boolean isReviewPassed(RecheckStatusEnum status) {
        return status == RecheckStatusEnum.REVIEW_PASSED;
    }

    public static boolean isWaiting(RecheckStatusEnum status) {
        return status == RecheckStatusEnum.REVIEW_WAITING;
    }

    public static boolean isBlocking(RecheckStatusEnum status) {
        return status == RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE
                || status == RecheckStatusEnum.DRIFTED
                || status == RecheckStatusEnum.INVALIDATED
                || status == RecheckStatusEnum.RISK_BLOCKED
                || status == RecheckStatusEnum.CONFUSED_BLOCKED;
    }
}
