package org.example.trademodel.telegram;

import org.example.trademodel.entity.MessageDO;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class HighValueAlertPolicy {
    static final String OPPORTUNITY_EVENT = "OPPORTUNITY_READY";
    static final String POSITION_EVENT = "POSITION_RISK_CHANGE";
    static final String OPPORTUNITY_TELEGRAM_CATEGORY = "EXECUTABLE_FINAL_PLAN";
    static final String POSITION_TELEGRAM_CATEGORY = "ACTIVE_POSITION_ATTENTION";
    static final String OPPORTUNITY_SHORT_TITLE = "【可复核执行计划】";
    static final String POSITION_SHORT_TITLE = "【持仓需关注】";

    private static final Set<String> BLOCKED_OPPORTUNITY_STATES = Set.of(
            "HIGH_RISK", "INVALIDATED", "COOLING", "CONFUSED");
    private static final Set<String> POSITION_MATERIAL_CONCLUSIONS = Set.of(
            "NEAR_STOP_LOSS", "NEAR_TAKE_PROFIT", "HIGH_RISK_OBSERVATION", "PLAN_INVALIDATED",
            "WAIT_USER_CONFIRM_CLOSE");
    private static final Set<String> TELEGRAM_POSITION_STATES = Set.of(
            "RISK_HIGH", "RISK_EXTREME", "RISK_INCREASED", "RISK_SHARPLY_INCREASED",
            "NEAR_STOP_LOSS", "STOP_LOSS_BREACHED", "NEAR_TAKE_PROFIT",
            "TAKE_PROFIT_REACHED", "STRONG_REVERSAL");
    private static final Set<String> USER_VISIBLE_FIXED_TAILS = Set.of(
            "不构成交易指令", "非交易指令", "系统不会自动下单", "系统不会自动平仓",
            "请打开Fundamental AI站内消息查看", "请打开 TRINE LOGIC 站内消息查看");

    public boolean allowsOpportunity(OpportunityQualification value) {
        if (value == null || value.userId() == null || value.userId() <= 0) return false;
        String opportunityState = normalized(value.opportunityState());
        return value.assetInPool()
                && value.persistedOpportunity()
                && value.finalPlan()
                && value.ruleValidated()
                && "CONFIRMATION".equals(normalized(value.finalPlanMode()))
                && "CURRENT".equals(normalized(value.planLifecycleState()))
                && !opportunityState.isEmpty()
                && !BLOCKED_OPPORTUNITY_STATES.contains(opportunityState)
                && !value.expired()
                && value.dataQualityPassed()
                && value.fresh()
                && value.sourceGatePassed()
                && value.executionFeasibilityPassed()
                && value.traceable()
                && value.entryPresent()
                && value.triggerPresent()
                && value.stopPresent()
                && value.targetPresent()
                && value.expiryPresent()
                && value.directionPresent()
                && !value.preview()
                && !value.candidateOnly()
                && value.notTradeInstruction()
                && value.notOrderExecution();
    }

    public boolean allowsSafetyChange(SafetyQualification value) {
        return value != null && value.userId() != null && value.userId() > 0
                && value.changeType() != null && value.traceable()
                && value.notTradeInstruction() && value.notOrderExecution();
    }

    /** Preserves the broader in-application position Message contract. */
    public boolean allowsPositionMessage(PositionQualification value) {
        if (value == null || value.userId() == null || value.userId() <= 0
                || !value.activeManualPosition() || !value.verified() || !value.fresh()) return false;
        return Set.of("WEAKENED", "INVALIDATED").contains(normalized(value.entryLogicStatus()))
                || "STRONG_REVERSAL".equals(normalized(value.reversalStatus()))
                || Set.of("HIGH", "EXTREME").contains(normalized(value.riskLevel()))
                || Set.of("INCREASED", "SHARPLY_INCREASED").contains(normalized(value.riskTrend()))
                || POSITION_MATERIAL_CONCLUSIONS.contains(normalized(value.monitorConclusion()));
    }

    public Optional<PositionTelegramChange> resolveTelegramPositionChange(PositionTelegramQualification value) {
        if (value == null || value.userId() == null || value.userId() <= 0
                || !value.activeManualPosition() || !value.verified() || !value.fresh()
                || !value.sameMonitorResult() || !value.currentPriceTrusted()
                || !value.entryPricePresent() || !value.notTradeInstruction() || !value.notOrderExecution()) {
            return Optional.empty();
        }
        if (value.stopLossPresent() && value.stopLossBreached()) {
            return Optional.of(PositionTelegramChange.STOP_LOSS_BREACHED);
        }
        if ("EXTREME".equals(normalized(value.riskLevel()))) {
            return Optional.of(PositionTelegramChange.RISK_EXTREME);
        }
        if (value.takeProfitPresent() && value.takeProfitReached()) {
            return Optional.of(PositionTelegramChange.TAKE_PROFIT_REACHED);
        }
        if (value.stopLossPresent() && value.nearStopLoss()) {
            return Optional.of(PositionTelegramChange.NEAR_STOP_LOSS);
        }
        if ("SHARPLY_INCREASED".equals(normalized(value.riskTrend()))) {
            return Optional.of(PositionTelegramChange.RISK_SHARPLY_INCREASED);
        }
        if ("HIGH".equals(normalized(value.riskLevel()))) {
            return Optional.of(PositionTelegramChange.RISK_HIGH);
        }
        if ("INCREASED".equals(normalized(value.riskTrend()))) {
            return Optional.of(PositionTelegramChange.RISK_INCREASED);
        }
        if (value.takeProfitPresent() && value.nearTakeProfit()) {
            return Optional.of(PositionTelegramChange.NEAR_TAKE_PROFIT);
        }
        if ("STRONG_REVERSAL".equals(normalized(value.reversalStatus()))) {
            return Optional.of(PositionTelegramChange.STRONG_REVERSAL);
        }
        return Optional.empty();
    }

    public static boolean telegramDeliveryEventEligible(String dedupeKey) {
        if (!TelegramDedupeKey.managed(dedupeKey)) return false;
        String event = normalized(TelegramDedupeKey.eventType(dedupeKey));
        String state = normalized(TelegramDedupeKey.state(dedupeKey));
        return (OPPORTUNITY_EVENT.equals(event) && "CONFIRMATION".equals(state))
                || (POSITION_EVENT.equals(event) && TELEGRAM_POSITION_STATES.contains(state));
    }

    /**
     * Shared deterministic Delivery allowlist. It deliberately checks the short-message shape as well as
     * category/state/source so legacy long-form STRONG_REVERSAL messages fail closed without a migration.
     */
    public static Optional<TelegramDeliveryIdentity> telegramDeliveryIdentity(MessageDO message) {
        if (message == null || message.getUserId() == null || message.getUserId() <= 0
                || !Boolean.TRUE.equals(message.getNotTradeInstruction())
                || !Boolean.TRUE.equals(message.getNotOrderExecution())
                || !telegramDeliveryEventEligible(message.getDedupeKey())
                || !hasText(message.getSymbol()) || !hasText(message.getTraceId())
                || !hasText(message.getTitle()) || !hasText(message.getBody())
                || containsFixedTail(message.getTitle()) || containsFixedTail(message.getBody())) {
            return Optional.empty();
        }
        String event = normalized(TelegramDedupeKey.eventType(message.getDedupeKey()));
        String state = normalized(TelegramDedupeKey.state(message.getDedupeKey()));
        if (OPPORTUNITY_EVENT.equals(event)) {
            if (!"HIGH_PERMISSION_OPPORTUNITY".equals(normalized(message.getCategory()))
                    || !OPPORTUNITY_SHORT_TITLE.equals(message.getTitle().trim())
                    || !hasText(message.getPlanId()) || !hasText(message.getAnalysisId())
                    || message.getExpiresAt() == null || !validOpportunitySource(message)
                    || !TelegramDedupeKey.matchesSubject(message.getDedupeKey(), message.getUserId(),
                    "FINAL_PLAN", message.getPlanId())
                    || !opportunityShortBody(message)) {
                return Optional.empty();
            }
            return Optional.of(new TelegramDeliveryIdentity(
                    OPPORTUNITY_TELEGRAM_CATEGORY, "CONFIRMATION", "FINAL_PLAN",
                    message.getPlanId().trim(), 3));
        }
        if (!"POSITION_LOGIC_RISK_CHANGE".equals(normalized(message.getCategory()))
                || !POSITION_SHORT_TITLE.equals(message.getTitle().trim())
                || message.getPositionId() == null || message.getPositionId() <= 0
                || !"POSITION_MONITOR".equals(normalized(message.getSourceType()))
                || !positiveInteger(message.getSourceId()) || message.getExpiresAt() == null
                || !TelegramDedupeKey.matchesSubject(message.getDedupeKey(), message.getUserId(),
                "USER_POSITION", String.valueOf(message.getPositionId()))) {
            return Optional.empty();
        }
        PositionTelegramChange change;
        try {
            change = PositionTelegramChange.valueOf(state);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
        if (!positionShortBody(message, change)) return Optional.empty();
        return Optional.of(new TelegramDeliveryIdentity(
                POSITION_TELEGRAM_CATEGORY, change.name(), "USER_POSITION",
                String.valueOf(message.getPositionId()), change.severityRank()));
    }

    private static boolean validOpportunitySource(MessageDO message) {
        String sourceType = normalized(message.getSourceType());
        if ("FINAL_PLAN".equals(sourceType)) {
            return message.getPlanId().trim().equals(message.getSourceId());
        }
        return "PUSH_SNAPSHOT".equals(sourceType) && positiveInteger(message.getSourceId());
    }

    private static boolean opportunityShortBody(MessageDO message) {
        String[] lines = message.getBody().split("\\n", -1);
        return lines.length == 9
                && lines[0].startsWith(message.getSymbol().trim() + "  ·  ")
                && lines[0].endsWith("  ·  确认型")
                && lines[0].length() > message.getSymbol().trim().length() + "  ·    ·  确认型".length()
                && lines[1].isEmpty()
                && populatedLine(lines[2], "入场：")
                && populatedLine(lines[3], "触发：")
                && populatedLine(lines[4], "止损：")
                && populatedLine(lines[5], "目标：")
                && populatedLine(lines[6], "有效至：")
                && lines[7].isEmpty()
                && "操作：打开系统重新校验".equals(lines[8]);
    }

    private static boolean positionShortBody(MessageDO message, PositionTelegramChange change) {
        String[] lines = message.getBody().split("\\n", -1);
        if (lines.length != 9
                || !lines[0].startsWith(message.getSymbol().trim() + "  ·  ")
                || lines[0].length() <= message.getSymbol().trim().length() + "  ·  ".length()
                || !lines[1].isEmpty()
                || !("变化：" + change.displayText()).equals(lines[2])
                || !lines[3].isEmpty()
                || !populatedLine(lines[4], "入场：")
                || !populatedLine(lines[5], "现价：")
                || !lines[7].isEmpty()
                || !"操作：打开持仓详情".equals(lines[8])) {
            return false;
        }
        int targetIndex = lines[6].indexOf("  目标：");
        return lines[6].startsWith("止损：")
                && targetIndex > "止损：".length()
                && targetIndex + "  目标：".length() < lines[6].length();
    }

    private static boolean populatedLine(String line, String prefix) {
        return line.startsWith(prefix) && line.length() > prefix.length();
    }

    private static boolean containsFixedTail(String value) {
        if (value == null) return false;
        return USER_VISIBLE_FIXED_TAILS.stream().anyMatch(value::contains);
    }

    private static boolean positiveInteger(String value) {
        return hasText(value) && value.trim().matches("[1-9][0-9]*");
    }

    public enum SafetyChangeType {
        CONFUSED,
        HIGH_CONFUSED,
        LIQUIDITY_TRAP,
        HOT_RESET,
        FINAL_INVALIDATED,
        RISK_BLOCKED,
        EXECUTION_DRIFT,
        PLAN_EXPIRED,
        DATA_QUALITY_BLOCKED,
        SOURCE_INVALID,
        NEEDS_REVALIDATION
    }

    public enum PositionTelegramChange {
        STOP_LOSS_BREACHED("触及止损", 4),
        RISK_EXTREME("风险极高", 4),
        TAKE_PROFIT_REACHED("触及止盈", 3),
        NEAR_STOP_LOSS("接近止损", 3),
        RISK_SHARPLY_INCREASED("风险快速上升", 3),
        RISK_HIGH("风险高", 3),
        RISK_INCREASED("风险上升", 2),
        NEAR_TAKE_PROFIT("接近止盈", 2),
        STRONG_REVERSAL("强反转", 3);

        private final String displayText;
        private final int severityRank;

        PositionTelegramChange(String displayText, int severityRank) {
            this.displayText = displayText;
            this.severityRank = severityRank;
        }

        public String displayText() { return displayText; }
        public int severityRank() { return severityRank; }
    }

    public record OpportunityQualification(
            Long userId,
            boolean assetInPool,
            boolean persistedOpportunity,
            boolean finalPlan,
            boolean ruleValidated,
            String finalPlanMode,
            String planLifecycleState,
            String opportunityState,
            boolean expired,
            boolean dataQualityPassed,
            boolean fresh,
            boolean sourceGatePassed,
            boolean executionFeasibilityPassed,
            boolean traceable,
            boolean entryPresent,
            boolean triggerPresent,
            boolean stopPresent,
            boolean targetPresent,
            boolean expiryPresent,
            boolean directionPresent,
            boolean preview,
            boolean candidateOnly,
            boolean notTradeInstruction,
            boolean notOrderExecution) {
    }

    public record SafetyQualification(Long userId, SafetyChangeType changeType, boolean traceable,
                                      boolean notTradeInstruction, boolean notOrderExecution) {
    }

    public record PositionQualification(Long userId, boolean activeManualPosition,
                                        boolean verified, boolean fresh,
                                        String entryLogicStatus, String reversalStatus,
                                        String riskLevel, String riskTrend,
                                        String monitorConclusion) {
    }

    public record PositionTelegramQualification(
            Long userId,
            boolean activeManualPosition,
            boolean verified,
            boolean fresh,
            boolean sameMonitorResult,
            boolean currentPriceTrusted,
            boolean entryPricePresent,
            boolean stopLossPresent,
            boolean takeProfitPresent,
            boolean nearStopLoss,
            boolean stopLossBreached,
            boolean nearTakeProfit,
            boolean takeProfitReached,
            String riskLevel,
            String riskTrend,
            String reversalStatus,
            boolean notTradeInstruction,
            boolean notOrderExecution) {
    }

    public record TelegramDeliveryIdentity(String telegramCategory,
                                           String changeState,
                                           String subjectType,
                                           String subjectId,
                                           int severityRank) {
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
