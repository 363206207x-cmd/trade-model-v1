package org.example.trademodel.messagepush;

import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class PublicOpportunityProjectionPolicy {
    private static final Pattern OPPORTUNITY_ID = Pattern.compile("opp-[A-Za-z0-9_-]{1,60}");
    private static final Set<String> LIFECYCLE_STATUSES = Set.of(
            OpportunityLogStatus.PENDING_EVALUATION,
            OpportunityLogStatus.RESOLVED,
            OpportunityLogStatus.SOURCE_INCOMPLETE,
            OpportunityLogStatus.MARKET_PATH_UNAVAILABLE,
            OpportunityLogStatus.AMBIGUOUS_MARKET_PATH);
    private static final Set<String> TERMINAL_STATUSES = Set.of(
            OpportunityLogStatus.MISSED_VALID,
            OpportunityLogStatus.MISSED_INVALID);
    private static final Set<String> DIRECTIONS = Set.of("LONG", "SHORT");
    private static final Set<String> HIT_ORDERS = Set.of(
            OpportunityLogStatus.TARGET_FIRST,
            OpportunityLogStatus.INVALIDATION_FIRST,
            OpportunityLogStatus.AMBIGUOUS_SAME_BAR);

    private PublicOpportunityProjectionPolicy() {
    }

    public static Evaluation evaluate(OpportunityLogPublicDTO source, String expectedOpportunityId) {
        if (source == null) {
            return evaluation(
                    MessageReadState.MISSING, null, null, List.of(), "MESSAGE_NOT_FOUND");
        }
        String opportunityId = text(source.opportunityId());
        if (!validOpportunityId(opportunityId)
                || expectedOpportunityId != null && !expectedOpportunityId.equals(opportunityId)) {
            return evaluation(
                    MessageReadState.ERROR,
                    null,
                    null,
                    List.of(),
                    "PUBLIC_OPPORTUNITY_IDENTITY_INVALID");
        }

        String lifecycle = upper(source.lifecycleStatus());
        String status = upper(source.opportunityStatus());
        String direction = upper(source.direction());
        String hitOrder = upper(source.hitOrder());
        if (lifecycle != null && !LIFECYCLE_STATUSES.contains(lifecycle)) {
            return evaluation(
                    MessageReadState.ERROR,
                    lifecycle,
                    status,
                    List.of(),
                    "PUBLIC_LIFECYCLE_INVALID");
        }
        if (status != null && !TERMINAL_STATUSES.contains(status)) {
            return evaluation(
                    MessageReadState.ERROR,
                    lifecycle,
                    status,
                    List.of(),
                    "PUBLIC_STATUS_INVALID");
        }
        if (status != null && !OpportunityLogStatus.RESOLVED.equals(lifecycle)) {
            return evaluation(
                    MessageReadState.ERROR,
                    lifecycle,
                    status,
                    List.of(),
                    "PUBLIC_STATE_CONFLICT");
        }
        if (OpportunityLogStatus.RESOLVED.equals(lifecycle) && status == null) {
            return evaluation(
                    MessageReadState.ERROR,
                    lifecycle,
                    null,
                    List.of(),
                    "PUBLIC_RESOLUTION_INCOMPLETE");
        }
        if (direction != null && !DIRECTIONS.contains(direction)) {
            return evaluation(
                    MessageReadState.ERROR,
                    lifecycle,
                    status,
                    List.of(),
                    "PUBLIC_DIRECTION_INVALID");
        }
        if (hitOrder != null && !HIT_ORDERS.contains(hitOrder)) {
            return evaluation(
                    MessageReadState.ERROR,
                    lifecycle,
                    status,
                    List.of(),
                    "PUBLIC_MARKET_EVIDENCE_INVALID");
        }
        if (!projectionFlagsValid(source)
                || marketEvidenceInvalid(source, direction, hitOrder, lifecycle, status)) {
            return evaluation(
                    MessageReadState.ERROR,
                    lifecycle,
                    status,
                    List.of(),
                    "PUBLIC_MARKET_EVIDENCE_INVALID");
        }

        List<String> missing = new ArrayList<>();
        if (text(source.analysisId()) == null) {
            missing.add("opportunityIdentity.analysisId");
        }
        if (text(source.symbol()) == null) {
            missing.add("symbol");
        }
        if (direction == null) {
            missing.add("direction");
        }
        if (upper(source.timeframe()) == null) {
            missing.add("timeframe");
        }
        if (lifecycle == null) {
            missing.add("publicLifecycle");
        }
        if (publicTimestamp(source) == null) {
            missing.add("publicTimestamp");
        }
        if (OpportunityLogStatus.RESOLVED.equals(lifecycle)) {
            if (source.entryReference() == null) {
                missing.add("publicEvidence.entryReference");
            }
            if (source.targetPrice() == null) {
                missing.add("publicEvidence.targetPrice");
            }
            if (source.invalidationPrice() == null) {
                missing.add("publicEvidence.invalidationPrice");
            }
            addResolvedEvidenceMissing(source, status, hitOrder, missing);
        }
        if (!missing.isEmpty()) {
            return evaluation(
                    MessageReadState.PARTIAL,
                    lifecycle,
                    status,
                    missing,
                    "PUBLIC_OPPORTUNITY_INCOMPLETE");
        }
        if (!OpportunityLogStatus.RESOLVED.equals(lifecycle)) {
            return evaluation(
                    MessageReadState.PARTIAL,
                    lifecycle,
                    status,
                    List.of("publicEvaluation"),
                    "PUBLIC_EVALUATION_PENDING");
        }
        return evaluation(MessageReadState.READY, lifecycle, status, List.of(), null);
    }

    public static LocalDateTime publicTimestamp(OpportunityLogPublicDTO source) {
        if (source == null) {
            return null;
        }
        if (source.anchorTime() != null) {
            return source.anchorTime();
        }
        return source.createdAt();
    }

    public static String publicDescription(OpportunityLogPublicDTO source) {
        if (source == null || text(source.symbol()) == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        parts.add(text(source.symbol()).toUpperCase(Locale.ROOT));
        if (upper(source.direction()) != null) {
            parts.add(upper(source.direction()));
        }
        if (upper(source.timeframe()) != null) {
            parts.add(upper(source.timeframe()));
        }
        return String.join(" ", parts);
    }

    private static String displayStatus(String lifecycle, String status) {
        return status != null ? status : lifecycle;
    }

    private static Evaluation evaluation(
            MessageReadState state,
            String lifecycle,
            String status,
            List<String> missingFields,
            String reason) {
        return new Evaluation(
                state,
                lifecycle,
                status,
                displayStatus(lifecycle, status),
                missingFields,
                reason);
    }

    private static void addResolvedEvidenceMissing(
            OpportunityLogPublicDTO source,
            String status,
            String hitOrder,
            List<String> missing) {
        if (source.resolvedAt() == null) {
            missing.add("publicEvidence.resolvedAt");
        }
        if (source.targetHit() == null) {
            missing.add("publicEvidence.targetHit");
        }
        if (source.invalidationHit() == null) {
            missing.add("publicEvidence.invalidationHit");
        }
        if (hitOrder == null) {
            missing.add("publicEvidence.hitOrder");
        }
        if (text(source.marketDataSource()) == null) {
            missing.add("publicEvidence.marketDataSource");
        }
        if (OpportunityLogStatus.MISSED_VALID.equals(status)
                && Boolean.TRUE.equals(source.targetHit())
                && source.targetHitAt() == null) {
            missing.add("publicEvidence.targetHitAt");
        }
        if (OpportunityLogStatus.MISSED_INVALID.equals(status)
                && Boolean.TRUE.equals(source.invalidationHit())
                && source.invalidationHitAt() == null) {
            missing.add("publicEvidence.invalidationHitAt");
        }
    }

    private static boolean marketEvidenceInvalid(
            OpportunityLogPublicDTO source,
            String direction,
            String hitOrder,
            String lifecycle,
            String status) {
        if (nonPositive(source.entryReference())
                || nonPositive(source.targetPrice())
                || nonPositive(source.invalidationPrice())
                || nonPositive(source.mfePrice())
                || nonPositive(source.maePrice())
                || negative(source.mfeRatio())
                || negative(source.maeRatio())) {
            return true;
        }
        if (direction != null
                && source.entryReference() != null
                && source.targetPrice() != null
                && source.invalidationPrice() != null
                && !directionalBoundariesValid(
                        direction,
                        source.entryReference(),
                        source.targetPrice(),
                        source.invalidationPrice())) {
            return true;
        }
        if (Boolean.FALSE.equals(source.targetHit()) && source.targetHitAt() != null
                || Boolean.FALSE.equals(source.invalidationHit()) && source.invalidationHitAt() != null) {
            return true;
        }
        LocalDateTime anchor = source.anchorTime();
        if (before(source.resolvedAt(), anchor)
                || before(source.targetHitAt(), anchor)
                || before(source.invalidationHitAt(), anchor)) {
            return true;
        }
        if (source.resolvedAt() != null
                && (after(source.targetHitAt(), source.resolvedAt())
                || after(source.invalidationHitAt(), source.resolvedAt()))) {
            return true;
        }
        if (OpportunityLogStatus.TARGET_FIRST.equals(hitOrder)
                && after(source.targetHitAt(), source.invalidationHitAt())
                || OpportunityLogStatus.INVALIDATION_FIRST.equals(hitOrder)
                && after(source.invalidationHitAt(), source.targetHitAt())) {
            return true;
        }
        if (OpportunityLogStatus.AMBIGUOUS_MARKET_PATH.equals(lifecycle)) {
            return hitOrder != null && !OpportunityLogStatus.AMBIGUOUS_SAME_BAR.equals(hitOrder)
                    || Boolean.FALSE.equals(source.targetHit())
                    || Boolean.FALSE.equals(source.invalidationHit())
                    || source.targetHitAt() != null
                    && source.invalidationHitAt() != null
                    && !source.targetHitAt().equals(source.invalidationHitAt())
                    || source.resolvedAt() != null;
        }
        if (!OpportunityLogStatus.RESOLVED.equals(lifecycle)) {
            return hitOrder != null
                    || Boolean.TRUE.equals(source.targetHit())
                    || Boolean.TRUE.equals(source.invalidationHit())
                    || source.resolvedAt() != null;
        }
        if (OpportunityLogStatus.MISSED_VALID.equals(status)) {
            return hitOrder != null && !OpportunityLogStatus.TARGET_FIRST.equals(hitOrder)
                    || Boolean.FALSE.equals(source.targetHit());
        }
        if (OpportunityLogStatus.MISSED_INVALID.equals(status)) {
            return hitOrder != null && !OpportunityLogStatus.INVALIDATION_FIRST.equals(hitOrder)
                    || Boolean.FALSE.equals(source.invalidationHit());
        }
        return false;
    }

    private static boolean directionalBoundariesValid(
            String direction,
            BigDecimal entry,
            BigDecimal target,
            BigDecimal invalidation) {
        if ("LONG".equals(direction)) {
            return target.compareTo(entry) > 0 && invalidation.compareTo(entry) < 0;
        }
        if ("SHORT".equals(direction)) {
            return target.compareTo(entry) < 0 && invalidation.compareTo(entry) > 0;
        }
        return false;
    }

    private static boolean projectionFlagsValid(OpportunityLogPublicDTO source) {
        return source.reviewOnly()
                && source.manualReviewOnly()
                && source.notTradeInstruction()
                && source.notExecutable()
                && source.notAutoTrading()
                && source.notOrderExecution()
                && source.notUserPositionCreation()
                && source.notUserPositionMutation()
                && source.notPushSend()
                && source.notExternalChannel();
    }

    private static boolean nonPositive(BigDecimal value) {
        return value != null && value.signum() <= 0;
    }

    private static boolean negative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private static boolean before(LocalDateTime value, LocalDateTime boundary) {
        return value != null && boundary != null && value.isBefore(boundary);
    }

    private static boolean after(LocalDateTime value, LocalDateTime boundary) {
        return value != null && boundary != null && value.isAfter(boundary);
    }

    private static boolean validOpportunityId(String value) {
        return value != null && OPPORTUNITY_ID.matcher(value).matches();
    }

    private static String upper(String value) {
        String normalized = text(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String text(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record Evaluation(
            MessageReadState state,
            String publicLifecycle,
            String publicStatus,
            String displayStatus,
            List<String> missingFields,
            String reason) {

        public Evaluation {
            missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
        }
    }
}
