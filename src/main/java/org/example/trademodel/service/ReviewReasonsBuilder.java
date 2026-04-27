package org.example.trademodel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.enums.AiConflictLevelEnum;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 由本 run 信号（风险、多周期、冲突、数据质量）生成 review_reasons JSON 数组文本；无命中时 {@code []}。
 */
public final class ReviewReasonsBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReviewReasonsBuilder() {}

    public static String toJsonArray(String riskLevel,
                                     String multiTfConvergence,
                                     AiConflictLevelEnum conflictLevel,
                                     Integer aiConflictScore,
                                     Integer dataQualityScore,
                                     Integer trendStructureScore) {
        Set<String> codes = new LinkedHashSet<>();
        if (riskLevel != null) {
            String r = riskLevel.trim().toUpperCase();
            if ("HIGH".equals(r) || "CRITICAL".equals(r)) {
                codes.add("RISK_LEVEL_HIGH");
            }
        }
        if (multiTfConvergence != null && "WEAK".equalsIgnoreCase(multiTfConvergence.trim())) {
            codes.add("MULTI_TF_CONVERGENCE_WEAK");
        }
        boolean conflictElevated = conflictLevel == AiConflictLevelEnum.LEVEL_3_SIGNIFICANT_DIVERGENCE
                || conflictLevel == AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE;
        if (conflictElevated) {
            codes.add("AI_CONFLICT_LEVEL_ELEVATED");
        }
        if (aiConflictScore != null && aiConflictScore >= 50) {
            codes.add("AI_CONFLICT_SCORE_HIGH");
        }
        if (dataQualityScore != null && dataQualityScore < 60) {
            codes.add("DATA_QUALITY_INSUFFICIENT");
        }
        if (trendStructureScore != null && trendStructureScore < 50) {
            codes.add("TREND_STRUCTURE_SCORE_INSUFFICIENT");
        }
        List<String> list = new ArrayList<>(codes);
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}
