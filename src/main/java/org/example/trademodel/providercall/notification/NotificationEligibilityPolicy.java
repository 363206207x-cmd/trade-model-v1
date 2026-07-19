package org.example.trademodel.providercall.notification;

import org.example.trademodel.providercall.AssetPriority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class NotificationEligibilityPolicy {
    public NotificationEligibilityResult evaluate(NotificationEligibilityRequest request) {
        validate(request);
        List<String> blockers = switch (request.type()) {
            case OPPORTUNITY_DISCOVERED -> discoveredBlockers(request);
            case OPPORTUNITY_REVIEW_READY -> reviewReadyBlockers(request);
            case POSITION_RISK_WARNING -> positionBlockers(request);
            case SYSTEM_DATA_WARNING -> systemBlockers(request);
        };
        boolean eligible = blockers.isEmpty();
        return new NotificationEligibilityResult(request.type(), request.canonicalInstrumentId(), eligible,
                eligible ? dedupKey(request) : null,
                eligible ? List.of("NOTIFICATION_ELIGIBLE") : blockers,
                request.baseProfile(), request.effectiveProfile(), request.profileReasonCodes(),
                request.frequencyMatrixVersion(), true, true);
    }

    private static List<String> discoveredBlockers(NotificationEligibilityRequest request) {
        List<String> blockers = new ArrayList<>();
        if (request.origin() != NotificationOrigin.WATCHLIST
                && request.origin() != NotificationOrigin.PROMOTED_DISCOVERY_CANDIDATE) {
            blockers.add("DISCOVERY_NOT_PROMOTED_TO_CANDIDATE");
        }
        if (!request.candidatePromoted()) blockers.add("CANDIDATE_PROMOTION_REQUIRED");
        if (request.scope() == OpportunityScope.WATCHLIST_ONLY
                && request.origin() != NotificationOrigin.WATCHLIST) {
            blockers.add("DISCOVERY_NOTIFICATION_OUT_OF_SCOPE");
        }
        return blockers;
    }

    private static List<String> reviewReadyBlockers(NotificationEligibilityRequest request) {
        List<String> blockers = new ArrayList<>(discoveredBlockers(request));
        require(request.triggered(), "TRIGGERED_REQUIRED", blockers);
        require(request.dataFresh(), "FRESH_DATA_REQUIRED", blockers);
        require(request.fourTimeframesComplete(), "FOUR_TIMEFRAMES_REQUIRED", blockers);
        require(request.dataQualityPassed(), "DATA_QUALITY_GATE_REQUIRED", blockers);
        require(request.entryComplete(), "ENTRY_BOUNDARY_REQUIRED", blockers);
        require(request.stopComplete(), "STOP_BOUNDARY_REQUIRED", blockers);
        require(request.takeProfitComplete(), "TAKE_PROFIT_BOUNDARY_REQUIRED", blockers);
        require(request.rewardRiskComputable(), "REWARD_RISK_REQUIRED", blockers);
        require(request.planNotExpired(), "PLAN_MUST_BE_CURRENT", blockers);
        require(request.riskGatePassed(), "RISK_GATE_REQUIRED", blockers);
        require(!request.confusedBlocked(), "CONFUSED_BLOCKED", blockers);
        require(request.hotResetReviewComplete(), "HOT_RESET_REVIEW_REQUIRED", blockers);
        require(request.pushRecheckPassed(), "PUSH_RECHECK_REQUIRED", blockers);
        return blockers;
    }

    private static List<String> positionBlockers(NotificationEligibilityRequest request) {
        return request.priority() == AssetPriority.P0_POSITION
                && request.origin() == NotificationOrigin.ACTIVE_POSITION
                ? List.of() : List.of("POSITION_WARNING_REQUIRES_P0_POSITION");
    }

    private static List<String> systemBlockers(NotificationEligibilityRequest request) {
        return request.systemDataWarning() && request.origin() == NotificationOrigin.SYSTEM
                ? List.of() : List.of("SYSTEM_DATA_WARNING_CONDITION_MISSING");
    }

    private static String dedupKey(NotificationEligibilityRequest request) {
        String raw = request.type() + "|" + request.canonicalInstrumentId().canonical() + "|"
                + text(request.strategyVersion()) + "|" + text(request.evidenceHash()) + "|"
                + text(request.planId()) + "|" + text(request.riskLevel());
        try {
            return "NTF-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8))).substring(0, 24).toUpperCase();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void validate(NotificationEligibilityRequest request) {
        if (request == null || request.type() == null || request.scope() == null || request.origin() == null
                || request.canonicalInstrumentId() == null || request.priority() == null) {
            throw new IllegalArgumentException("notification identity is required");
        }
        if (request.evidenceHash() == null || request.evidenceHash().isBlank()) {
            throw new IllegalArgumentException("evidenceHash is required");
        }
        if (request.baseProfile() == null || request.effectiveProfile() == null
                || request.frequencyMatrixVersion() == null || request.frequencyMatrixVersion().isBlank()) {
            throw new IllegalArgumentException("profile and frequency evidence is required");
        }
    }

    private static void require(boolean condition, String reason, List<String> blockers) {
        if (!condition) blockers.add(reason);
    }

    private static String text(String value) {
        return value == null ? "NONE" : value.trim();
    }
}
