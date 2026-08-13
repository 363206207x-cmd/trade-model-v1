package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.service.ConfusedResult;
import org.example.trademodel.service.ConfusedStatePolicy;
import org.example.trademodel.service.ConfusedStateService;
import org.example.trademodel.service.DecisionContext;
import org.example.trademodel.service.OpportunityStateIdentity;
import org.springframework.stereotype.Service;

@Service
public class ConfusedStateServiceImpl implements ConfusedStateService {

    private final AssetStateMapper assetStateMapper;

    public ConfusedStateServiceImpl(AssetStateMapper assetStateMapper) {
        this.assetStateMapper = assetStateMapper;
    }

    @Override
    public ConfusedResult calculateConfused(String symbol, DecisionContext context) {
        return calculateConfusedInternal(OpportunityStateIdentity.system(symbol, "global"), context);
    }

    @Override
    public ConfusedResult calculateConfused(String symbol, String timeframe, DecisionContext context) {
        return calculateConfusedInternal(
                OpportunityStateIdentity.system(symbol, normalizeTimeframe(timeframe)), context);
    }

    @Override
    public ConfusedResult calculateConfused(OpportunityStateIdentity identity, DecisionContext context) {
        if (identity == null) {
            throw new IllegalArgumentException("opportunity identity is required");
        }
        return calculateConfusedInternal(identity, context);
    }

    private ConfusedResult calculateConfusedInternal(OpportunityStateIdentity identity, DecisionContext context) {
        DecisionContext safeContext = context != null ? context : new DecisionContext();
        int driverConflict = safeContext.getDriverConflictScore() != null ? safeContext.getDriverConflictScore() : 30;
        int executionInstability = safeContext.getExecutionInstabilityScore() != null ? safeContext.getExecutionInstabilityScore() : 25;
        int microstructureTrap = safeContext.getMicrostructureTrapScore() != null ? safeContext.getMicrostructureTrapScore() : 35;
        int causeEffectDivergence = safeContext.getCauseEffectDivergenceScore() != null ? safeContext.getCauseEffectDivergenceScore() : 20;
        int aiConflict = safeContext.getAiConflictScore() != null ? safeContext.getAiConflictScore() : 40;

        int confusedScore = (int) (
                0.30 * driverConflict +
                0.20 * executionInstability +
                0.20 * microstructureTrap +
                0.15 * causeEffectDivergence +
                0.15 * aiConflict
        );

        PersistedState persisted = readPersistedState(identity, safeContext);
        if (persisted.readFailed) {
            int failClosedScore = Math.max(confusedScore, ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD);
            return new ConfusedResult(
                    failClosedScore,
                    persisted.previousState.name(),
                    AssetStateEnum.CONFUSED.name(),
                    persisted.previousState != AssetStateEnum.CONFUSED,
                    false,
                    0,
                    failClosedScore >= ConfusedStatePolicy.DIRECTIONAL_PUSH_BLOCK_THRESHOLD,
                    generateConflictReasons(safeContext),
                    "ASSET_STATE_READ_FAILED_FAIL_CLOSED"
            );
        }

        boolean directionalPushBlocked =
                confusedScore >= ConfusedStatePolicy.DIRECTIONAL_PUSH_BLOCK_THRESHOLD;
        AssetStateEnum previousState = persisted.previousState;
        AssetStateEnum nextState;
        boolean shouldEnter;
        boolean shouldExit;
        int lowStreak;
        String transitionReason;

        if (confusedScore >= ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD) {
            nextState = AssetStateEnum.CONFUSED;
            shouldEnter = previousState != AssetStateEnum.CONFUSED;
            shouldExit = false;
            lowStreak = 0;
            transitionReason = "CONFUSED_SCORE_ENTER_THRESHOLD";
        } else if (previousState == AssetStateEnum.CONFUSED) {
            shouldEnter = false;
            if (confusedScore >= ConfusedStatePolicy.CONFUSED_EXIT_THRESHOLD_EXCLUSIVE) {
                nextState = AssetStateEnum.CONFUSED;
                shouldExit = false;
                lowStreak = 0;
                transitionReason = "CONFUSED_SCORE_NOT_BELOW_EXIT_THRESHOLD";
            } else {
                int nextLowStreak = persisted.confusedLowStreak + 1;
                if (nextLowStreak >= ConfusedStatePolicy.CONFUSED_EXIT_REQUIRED_CONSECUTIVE_CYCLES
                        && exitSignalsRecovered(safeContext)) {
                    nextState = Boolean.TRUE.equals(safeContext.getWorthOpening())
                            ? AssetStateEnum.CANDIDATE
                            : AssetStateEnum.OBSERVING;
                    shouldExit = true;
                    lowStreak = 0;
                    transitionReason = "CONFUSED_EXIT_CONDITIONS_RECOVERED";
                } else {
                    nextState = AssetStateEnum.CONFUSED;
                    shouldExit = false;
                    lowStreak = nextLowStreak;
                    transitionReason = nextLowStreak < ConfusedStatePolicy.CONFUSED_EXIT_REQUIRED_CONSECUTIVE_CYCLES
                            ? "CONFUSED_LOW_STREAK_WAITING_FOR_SECOND_CYCLE"
                            : "CONFUSED_EXIT_SIGNALS_NOT_RECOVERED";
                }
            }
        } else {
            nextState = Boolean.TRUE.equals(safeContext.getWorthOpening())
                    ? AssetStateEnum.CANDIDATE
                    : AssetStateEnum.OBSERVING;
            shouldEnter = false;
            shouldExit = false;
            lowStreak = 0;
            transitionReason = "NON_CONFUSED_BASE_STATE";
        }

        return new ConfusedResult(
                confusedScore,
                previousState.name(),
                nextState.name(),
                shouldEnter,
                shouldExit,
                lowStreak,
                directionalPushBlocked,
                generateConflictReasons(safeContext),
                transitionReason
        );
    }

    private String generateConflictReasons(DecisionContext context) {
        return "Driver冲突:" + context.getDriverConflictScore()
                + " | 执行不稳定:" + context.getExecutionInstabilityScore();
    }

    private static boolean exitSignalsRecovered(DecisionContext context) {
        return context.isMultiTimeframeAligned()
                && belowExitThreshold(context.getDriverConflictScore())
                && belowExitThreshold(context.getCauseEffectDivergenceScore())
                && belowExitThreshold(context.getExecutionInstabilityScore())
                && belowExitThreshold(context.getMicrostructureTrapScore());
    }

    private static boolean belowExitThreshold(Integer value) {
        return value != null && value < ConfusedStatePolicy.CONFUSED_EXIT_THRESHOLD_EXCLUSIVE;
    }

    private PersistedState readPersistedState(OpportunityStateIdentity identity, DecisionContext context) {
        AssetStateEnum previousState = AssetStateEnum.OBSERVING;
        int lowStreak = context.getConsecutiveLowConfusedCount() != null
                ? Math.max(0, context.getConsecutiveLowConfusedCount())
                : 0;
        if (identity == null || assetStateMapper == null) {
            return new PersistedState(previousState, lowStreak, false);
        }
        try {
            AssetStateDO row = assetStateMapper.selectByIdentity(
                    identity.ownerType(), identity.ownerId(), identity.symbol(), identity.timeframe());
            if (row == null && "SYSTEM".equals(identity.ownerType())) {
                row = "global".equals(identity.timeframe())
                        ? assetStateMapper.selectBySymbol(identity.symbol())
                        : assetStateMapper.selectBySymbolAndTimeframe(identity.symbol(), identity.timeframe());
            }
            if (row == null) {
                return new PersistedState(previousState, lowStreak, false);
            }
            previousState = row.getState() != null ? row.getState() : AssetStateEnum.OBSERVING;
            lowStreak = row.getConfusedLowStreak() != null ? Math.max(0, row.getConfusedLowStreak()) : 0;
            return new PersistedState(previousState, lowStreak, false);
        } catch (Exception e) {
            return new PersistedState(AssetStateEnum.CONFUSED, 0, true);
        }
    }

    private static String normalizeTimeframe(String timeframe) {
        return timeframe == null || timeframe.isBlank() ? "global" : timeframe.trim().toLowerCase();
    }

    private static final class PersistedState {
        private final AssetStateEnum previousState;
        private final int confusedLowStreak;
        private final boolean readFailed;

        private PersistedState(AssetStateEnum previousState, int confusedLowStreak, boolean readFailed) {
            this.previousState = previousState;
            this.confusedLowStreak = confusedLowStreak;
            this.readFailed = readFailed;
        }
    }
}
