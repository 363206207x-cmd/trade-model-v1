package org.example.trademodel.service.impl;

import org.example.trademodel.service.ConfusedResult;
import org.example.trademodel.service.ConfusedStateService;
import org.example.trademodel.service.DecisionContext;
import org.springframework.stereotype.Service;

@Service
public class ConfusedStateServiceImpl implements ConfusedStateService {

    @Override
    public ConfusedResult calculateConfused(String symbol, DecisionContext context) {
        int driverConflict = context.getDriverConflictScore() != null ? context.getDriverConflictScore() : 30;
        int executionInstability = context.getExecutionInstabilityScore() != null ? context.getExecutionInstabilityScore() : 25;
        int microstructureTrap = context.getMicrostructureTrapScore() != null ? context.getMicrostructureTrapScore() : 35;
        int causeEffectDivergence = context.getCauseEffectDivergenceScore() != null ? context.getCauseEffectDivergenceScore() : 20;
        int aiConflict = context.getAiConflictScore() != null ? context.getAiConflictScore() : 40;

        int confusedScore = (int) (
                0.30 * driverConflict +
                0.20 * executionInstability +
                0.20 * microstructureTrap +
                0.15 * causeEffectDivergence +
                0.15 * aiConflict
        );

        boolean shouldEnter = confusedScore >= 70;
        boolean shouldExit = confusedScore < 55 && context.getConsecutiveLowConfusedCount() >= 2;

        return new ConfusedResult(
                confusedScore,
                shouldEnter,
                shouldExit,
                generateConflictReasons(context)
        );
    }

    private String generateConflictReasons(DecisionContext context) {
        return "Driver冲突:" + context.getDriverConflictScore()
                + " | 执行不稳定:" + context.getExecutionInstabilityScore();
    }
}
