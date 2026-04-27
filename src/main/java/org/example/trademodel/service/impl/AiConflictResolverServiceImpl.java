package org.example.trademodel.service.impl;

import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.AiConflictResult;
import org.example.trademodel.service.DecisionContext;
import org.springframework.stereotype.Service;

@Service
public class AiConflictResolverServiceImpl implements AiConflictResolverService {

    @Override
    public AiConflictResult resolve(DecisionContext context) {
        if (!context.isHasRuleBaseOutput()) {
            throw new IllegalStateException("规则层必须先产出基础方向");
        }

        int directionConflict = calculateDirectionConflict(context);
        int riskConflict = calculateRiskConflict(context);
        int planConflict = calculatePlanConflict(context);

        int aiConflictScore = Math.min(100, directionConflict + riskConflict + planConflict);
        context.setAiConflictScore(aiConflictScore);

        if (aiConflictScore <= 20 && context.isGptConsistentWithRule()) {
            return new AiConflictResult(
                    AiConflictLevelEnum.LEVEL_1_CONSISTENT,
                    context.getRuleMarketBias(),
                    context.getRuleConfidenceLevel(),
                    "CONFIRM",
                    aiConflictScore
            );
        } else if (aiConflictScore <= 45) {
            return new AiConflictResult(
                    AiConflictLevelEnum.LEVEL_2_LIGHT_DIVERGENCE,
                    context.getRuleMarketBias(),
                    downgradeConfidence(context),
                    "REDUCED",
                    aiConflictScore
            );
        } else if (aiConflictScore <= 70) {
            return new AiConflictResult(
                    AiConflictLevelEnum.LEVEL_3_SIGNIFICANT_DIVERGENCE,
                    null,
                    null,
                    "PREPARE_ONLY",
                    aiConflictScore
            );
        } else {
            return new AiConflictResult(
                    AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE,
                    null,
                    null,
                    "CONFUSED",
                    aiConflictScore
            );
        }
    }

    /** 来源：规则层 1m 方向 vs 综合裁决方向、多周期是否对齐（由 DecisionEngineService 写入 context） */
    private int calculateDirectionConflict(DecisionContext ctx) {
        int c = 0;
        if (!ctx.isGptConsistentWithRule()) {
            c += 32;
        }
        if (!ctx.isMultiTimeframeAligned()) {
            c += 24;
        }
        return Math.min(56, c);
    }

    /** 来源：本 run 风险档位 riskTier */
    private int calculateRiskConflict(DecisionContext ctx) {
        if ("MEDIUM".equalsIgnoreCase(ctx.getRiskTier())) {
            return 18;
        }
        return 8;
    }

    /** 来源：本 run 是否 worthOpening 与计划张力 */
    private int calculatePlanConflict(DecisionContext ctx) {
        if (Boolean.TRUE.equals(ctx.getWorthOpening())) {
            return 10;
        }
        return 20;
    }

    private String downgradeConfidence(DecisionContext ctx) {
        return "MEDIUM";
    }
}
