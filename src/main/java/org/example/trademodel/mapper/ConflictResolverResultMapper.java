package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.ConflictResolverResultDO;

@Mapper
public interface ConflictResolverResultMapper {
    @Insert("INSERT INTO tm_conflict_resolver_result(resolver_result_id, candidate_id, analysis_id, trace_id, "
            + "rule_direction, rule_confidence, rule_risk, gemini_review_json, grok_challenge_json, conflict_level, "
            + "conflict_score, plan_mode_before, plan_mode_after, confidence_before, confidence_after, risk_before, "
            + "risk_after, downgrade_reason, confused_decision, rule_veto_reason, rule_direction_preserved, created_at) "
            + "VALUES(#{resolverResultId}, #{candidateId}, #{analysisId}, #{traceId}, #{ruleDirection}, "
            + "#{ruleConfidence}, #{ruleRisk}, #{geminiReviewJson}, #{grokChallengeJson}, #{conflictLevel}, "
            + "#{conflictScore}, #{planModeBefore}, #{planModeAfter}, #{confidenceBefore}, #{confidenceAfter}, "
            + "#{riskBefore}, #{riskAfter}, #{downgradeReason}, #{confusedDecision}, #{ruleVetoReason}, "
            + "#{ruleDirectionPreserved}, #{createdAt})")
    int insert(ConflictResolverResultDO row);

    @Select("SELECT * FROM tm_conflict_resolver_result WHERE analysis_id = #{analysisId} LIMIT 1")
    ConflictResolverResultDO selectByAnalysisId(@Param("analysisId") String analysisId);
}
