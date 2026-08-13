package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.vo.DecisionResultVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DecisionResultMapper {

    @Insert("INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, market_bias_hierarchy, trade_type, confidence_level, risk_level, action_priority, conclusion_summary, is_worth_opening, multi_tf_convergence, ai_role_results, is_adopted, valid_period, valid_from, expires_at, invalid_condition, evidence_summary, explanation_json, review_reasons, ai_conflict_level, ai_conflict_score, ai_plan_mode, rule_market_bias, final_market_bias, rule_confidence, rule_risk, rule_plan_mode, rule_can_execute, candidate_plan_mode, final_plan_mode, bias_adjustment_reason, plan_mode_adjustment_reason, confused_score, asset_state_snapshot, create_time) " +
            "VALUES(#{decisionId}, #{analysisId}, #{symbol}, #{marketBiasHierarchy}, #{tradeType}, #{confidenceLevel}, #{riskLevel}, #{actionPriority}, #{conclusionSummary}, #{isWorthOpening}, #{multiTfConvergence}, #{aiRoleResults}, #{isAdopted}, #{validPeriod}, #{validFrom}, #{expiresAt}, #{invalidCondition}, #{evidenceSummary}, #{explanationJson}, #{reviewReasons}, #{aiConflictLevel}, #{aiConflictScore}, #{aiPlanMode}, #{ruleMarketBias}, #{finalMarketBias}, #{ruleConfidence}, #{ruleRisk}, #{rulePlanMode}, #{ruleCanExecute}, #{candidatePlanMode}, #{finalPlanMode}, #{biasAdjustmentReason}, #{planModeAdjustmentReason}, #{confusedScore}, #{assetStateSnapshot}, #{createTime})")
    int insert(DecisionResult decision);

    @Select("SELECT * FROM tm_decision_result ORDER BY create_time DESC LIMIT #{limit}")
    List<DecisionResult> findLatestDecisions(int limit);

    @Select("""
            SELECT d.decision_id AS decisionId, d.analysis_id AS analysisId, d.symbol AS symbol,
            ar.timeframe AS timeframe,
            d.market_bias_hierarchy AS marketBiasHierarchy, d.trade_type AS tradeType,
            d.confidence_level AS confidenceLevel, d.risk_level AS riskLevel, d.action_priority AS actionPriority,
            d.conclusion_summary AS conclusionSummary, d.is_worth_opening AS isWorthOpening,
            d.multi_tf_convergence AS multiTfConvergence, d.ai_role_results AS aiRoleResults,
            d.is_adopted AS isAdopted, d.valid_period AS validPeriod,
            d.valid_from AS validFrom, d.expires_at AS expiresAt,
            p.invalid_condition AS invalidCondition,
            d.evidence_summary AS evidenceSummary, d.explanation_json AS explanationJson, d.review_reasons AS reviewReasons,
            d.ai_conflict_level AS aiConflictLevel, d.ai_conflict_score AS aiConflictScore, d.ai_plan_mode AS aiPlanMode,
            d.confused_score AS confusedScore, d.asset_state_snapshot AS assetStateSnapshot, d.create_time AS createTime,
            p.invalid_condition AS executionPlanSummary,
            p.recommended_action AS recommendedAction,
            p.final_plan_mode AS planMode,
            p.entry_zone AS entryZone,
            p.stop_loss AS stopLoss,
            p.take_profit_rules AS takeProfitRules,
            p.leverage_limit AS leverageSuggestion,
            p.position_limit AS positionSuggestion,
            ar.data_quality_score AS dataQualityScore
            FROM tm_decision_result d
            LEFT JOIN (
              SELECT plan_id, analysis_id, final_plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules,
                     leverage_limit, position_limit, invalid_condition,
                     ROW_NUMBER() OVER (PARTITION BY analysis_id ORDER BY create_time DESC, plan_id DESC) AS rn
              FROM tm_execution_plan
              WHERE final_plan = TRUE
                AND rule_validation_status = 'PASS'
                AND chain_status = 'FINAL_VALIDATED'
            ) p ON d.analysis_id = p.analysis_id AND p.rn = 1
            LEFT JOIN tm_analysis_run ar ON d.analysis_id = ar.analysis_id
            ORDER BY d.create_time DESC LIMIT #{limit}
            """)
    List<DecisionResultVO> findLatestDecisionResultsJoined(int limit);

    @Select("""
            SELECT d.decision_id AS decisionId, d.analysis_id AS analysisId, d.symbol AS symbol,
            ar.timeframe AS timeframe,
            d.market_bias_hierarchy AS marketBiasHierarchy, d.trade_type AS tradeType,
            d.confidence_level AS confidenceLevel, d.risk_level AS riskLevel, d.action_priority AS actionPriority,
            d.conclusion_summary AS conclusionSummary, d.is_worth_opening AS isWorthOpening,
            d.multi_tf_convergence AS multiTfConvergence, d.ai_role_results AS aiRoleResults,
            d.is_adopted AS isAdopted, d.valid_period AS validPeriod,
            d.valid_from AS validFrom, d.expires_at AS expiresAt,
            p.invalid_condition AS invalidCondition,
            d.evidence_summary AS evidenceSummary, d.explanation_json AS explanationJson, d.review_reasons AS reviewReasons,
            d.ai_conflict_level AS aiConflictLevel, d.ai_conflict_score AS aiConflictScore, d.ai_plan_mode AS aiPlanMode,
            d.confused_score AS confusedScore, d.asset_state_snapshot AS assetStateSnapshot, d.create_time AS createTime,
            p.invalid_condition AS executionPlanSummary,
            p.recommended_action AS recommendedAction,
            p.final_plan_mode AS planMode,
            p.entry_zone AS entryZone,
            p.stop_loss AS stopLoss,
            p.take_profit_rules AS takeProfitRules,
            p.leverage_limit AS leverageSuggestion,
            p.position_limit AS positionSuggestion,
            ar.data_quality_score AS dataQualityScore
            FROM tm_decision_result d
            LEFT JOIN (
              SELECT plan_id, analysis_id, final_plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules,
                     leverage_limit, position_limit, invalid_condition,
                     ROW_NUMBER() OVER (PARTITION BY analysis_id ORDER BY create_time DESC, plan_id DESC) AS rn
              FROM tm_execution_plan
              WHERE final_plan = TRUE
                AND rule_validation_status = 'PASS'
                AND chain_status = 'FINAL_VALIDATED'
            ) p ON d.analysis_id = p.analysis_id AND p.rn = 1
            LEFT JOIN tm_analysis_run ar ON d.analysis_id = ar.analysis_id
            WHERE UPPER(TRIM(d.symbol)) = #{normalizedSymbol}
            ORDER BY d.create_time DESC LIMIT 1
            """)
    DecisionResultVO findLatestDecisionResultBySymbolJoined(@Param("normalizedSymbol") String normalizedSymbol);

    @Select({
            "<script>",
            "SELECT d.decision_id AS decisionId, d.analysis_id AS analysisId, d.symbol AS symbol,",
            "ar.timeframe AS timeframe, ar.analysis_time AS analysisTime,",
            "d.market_bias_hierarchy AS marketBiasHierarchy, d.trade_type AS tradeType,",
            "d.confidence_level AS confidenceLevel, d.risk_level AS riskLevel, d.action_priority AS actionPriority,",
            "d.conclusion_summary AS conclusionSummary, d.is_worth_opening AS isWorthOpening,",
            "d.multi_tf_convergence AS multiTfConvergence, d.ai_role_results AS aiRoleResults,",
            "d.is_adopted AS isAdopted, d.valid_period AS validPeriod,",
            "d.valid_from AS validFrom, d.expires_at AS expiresAt,",
            "p.invalid_condition AS invalidCondition,",
            "d.evidence_summary AS evidenceSummary, d.explanation_json AS explanationJson,",
            "d.review_reasons AS reviewReasons, d.ai_conflict_level AS aiConflictLevel,",
            "d.ai_conflict_score AS aiConflictScore, d.ai_plan_mode AS aiPlanMode,",
            "d.confused_score AS confusedScore, d.asset_state_snapshot AS assetStateSnapshot,",
            "d.create_time AS createTime, p.recommended_action AS recommendedAction,",
            "p.final_plan_mode AS planMode, p.final_market_bias AS finalMarketBias, p.entry_zone AS entryZone, p.stop_loss AS stopLoss,",
            "p.take_profit_rules AS takeProfitRules, p.leverage_limit AS leverageSuggestion,",
            "p.position_limit AS positionSuggestion, ar.data_quality_score AS dataQualityScore,",
            "scores.opportunity_score AS opportunityScore",
            "FROM (",
            "  SELECT src.*, ROW_NUMBER() OVER (",
            "    PARTITION BY UPPER(TRIM(src.symbol))",
            "    ORDER BY owner_run.analysis_time DESC, src.create_time DESC, src.decision_id DESC",
            "  ) AS symbol_rank",
            "  FROM tm_decision_result src",
            "  INNER JOIN tm_analysis_run owner_run ON owner_run.analysis_id = src.analysis_id",
            "  WHERE UPPER(TRIM(src.symbol)) IN",
            "  <foreach collection='symbols' item='symbol' open='(' separator=',' close=')'>",
            "    #{symbol}",
            "  </foreach>",
            "  AND ((#{ownerType} = 'SYSTEM' AND owner_run.owner_type = 'SYSTEM' AND owner_run.owner_id = 0)",
            "    OR (#{ownerType} = 'USER' AND ((owner_run.owner_type = 'USER' AND owner_run.owner_id = #{ownerId})",
            "      OR (owner_run.owner_type = 'SYSTEM' AND owner_run.owner_id = 0))))",
            "  AND EXISTS (SELECT 1 FROM tm_execution_plan eligible_plan",
            "    WHERE eligible_plan.analysis_id = src.analysis_id",
            "      AND eligible_plan.final_plan = TRUE",
            "      AND eligible_plan.rule_validation_status = 'PASS'",
            "      AND eligible_plan.chain_status = 'FINAL_VALIDATED')",
            ") d",
            "LEFT JOIN (",
            "  SELECT plan_id, analysis_id, final_plan_mode, final_market_bias, recommended_action, entry_zone, stop_loss,",
            "         take_profit_rules, leverage_limit, position_limit, invalid_condition,",
            "         ROW_NUMBER() OVER (PARTITION BY analysis_id ORDER BY create_time DESC, plan_id DESC) AS rn",
            "  FROM tm_execution_plan",
            "  WHERE final_plan = TRUE",
            "    AND rule_validation_status = 'PASS'",
            "    AND chain_status = 'FINAL_VALIDATED'",
            ") p ON d.analysis_id = p.analysis_id AND p.rn = 1",
            "LEFT JOIN tm_analysis_run ar ON d.analysis_id = ar.analysis_id",
            "LEFT JOIN (",
            "  SELECT analysis_id, AVG(score_value) AS opportunity_score",
            "  FROM tm_score_item GROUP BY analysis_id",
            ") scores ON scores.analysis_id = d.analysis_id",
            "WHERE d.symbol_rank = 1",
            "ORDER BY ar.analysis_time DESC, d.create_time DESC, d.decision_id DESC",
            "</script>"
    })
    List<DecisionResultVO> findLatestDecisionResultsForSymbolsJoined(
            @Param("symbols") List<String> symbols,
            @Param("ownerType") String ownerType,
            @Param("ownerId") Long ownerId);

    @Select("""
            SELECT d.decision_id AS decisionId, d.analysis_id AS analysisId, d.symbol AS symbol,
            ar.timeframe AS timeframe,
            d.market_bias_hierarchy AS marketBiasHierarchy, d.trade_type AS tradeType,
            d.confidence_level AS confidenceLevel, d.risk_level AS riskLevel, d.action_priority AS actionPriority,
            d.conclusion_summary AS conclusionSummary, d.is_worth_opening AS isWorthOpening,
            d.multi_tf_convergence AS multiTfConvergence, d.ai_role_results AS aiRoleResults,
            d.is_adopted AS isAdopted, d.valid_period AS validPeriod,
            d.valid_from AS validFrom, d.expires_at AS expiresAt,
            p.invalid_condition AS invalidCondition,
            d.evidence_summary AS evidenceSummary, d.explanation_json AS explanationJson, d.review_reasons AS reviewReasons,
            d.ai_conflict_level AS aiConflictLevel, d.ai_conflict_score AS aiConflictScore, d.ai_plan_mode AS aiPlanMode,
            d.confused_score AS confusedScore, d.asset_state_snapshot AS assetStateSnapshot, d.create_time AS createTime,
            p.recommended_action AS recommendedAction,
            p.final_plan_mode AS planMode,
            p.entry_zone AS entryZone,
            p.stop_loss AS stopLoss,
            p.take_profit_rules AS takeProfitRules,
            p.leverage_limit AS leverageSuggestion,
            p.position_limit AS positionSuggestion,
            ar.data_quality_score AS dataQualityScore
            FROM tm_decision_result d
            INNER JOIN tm_execution_plan p
              ON p.plan_id = #{planId} AND p.analysis_id = d.analysis_id
            INNER JOIN tm_analysis_run ar ON ar.analysis_id = d.analysis_id
            WHERE d.analysis_id = #{analysisId}
            ORDER BY d.create_time DESC, d.decision_id DESC
            LIMIT 1
            """)
    DecisionResultVO findByAnalysisIdAndPlanIdJoined(@Param("analysisId") String analysisId,
                                                     @Param("planId") String planId);

    @Select("SELECT MAX(create_time) FROM tm_decision_result")
    LocalDateTime selectLastDecisionTime();

    @Select("SELECT COUNT(*) FROM tm_decision_result "
            + "WHERE create_time >= #{startInclusive} AND create_time < #{endExclusive}")
    int countDecisionsInRange(@Param("startInclusive") LocalDateTime startInclusive,
                              @Param("endExclusive") LocalDateTime endExclusive);

    @Select("SELECT * FROM tm_decision_result WHERE analysis_id = #{analysisId} ORDER BY create_time DESC LIMIT 1")
    DecisionResult selectLatestByAnalysisId(String analysisId);

    @Select("SELECT * FROM tm_decision_result WHERE decision_id = #{decisionId}")
    DecisionResult selectByDecisionId(@Param("decisionId") String decisionId);

    @Update("UPDATE tm_decision_result SET hot_reset_invalidated = TRUE, hot_reset_event_id = #{eventId}, "
            + "hot_reset_invalidated_at = #{invalidatedAt}, hot_reset_reason_code = #{reasonCode} "
            + "WHERE UPPER(TRIM(symbol)) = #{normalizedSymbol} "
            + "AND (hot_reset_invalidated IS NULL OR hot_reset_invalidated = FALSE) "
            + "AND (is_worth_opening = TRUE OR UPPER(TRIM(market_bias_hierarchy)) IN "
            + "('STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH', 'WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH'))")
    int markHotResetInvalidatedBySymbol(
            @Param("normalizedSymbol") String normalizedSymbol,
            @Param("eventId") String eventId,
            @Param("reasonCode") String reasonCode,
            @Param("invalidatedAt") LocalDateTime invalidatedAt);

    /**
     * OPEN 持仓（每 symbol 一条代表行，多条 OPEN 时按 position_id 取首条）与每 symbol 最新决策（create_time DESC，并列时 decision_id DESC）；
     * 仅在最新 bias 属于完整多空方向族且与 LONG/SHORT 反向时计入；按 symbol 去重计数。
     */
    @Select("""
            SELECT COUNT(*) FROM (
              SELECT ps.sym
              FROM (
                SELECT UPPER(TRIM(symbol)) AS sym,
                       UPPER(TRIM(position_side)) AS side,
                       ROW_NUMBER() OVER (PARTITION BY UPPER(TRIM(symbol)) ORDER BY position_id ASC) AS rn
                FROM tm_real_position
                WHERE position_status = 'OPEN'
                  AND symbol IS NOT NULL AND TRIM(symbol) <> ''
                  AND position_side IS NOT NULL
                  AND UPPER(TRIM(position_side)) IN ('LONG', 'SHORT')
              ) ps
              INNER JOIN (
                SELECT UPPER(TRIM(symbol)) AS sym,
                       market_bias_hierarchy AS bias_raw,
                       ROW_NUMBER() OVER (
                         PARTITION BY UPPER(TRIM(symbol))
                         ORDER BY create_time DESC, decision_id DESC
                       ) AS rn
                FROM tm_decision_result
                WHERE symbol IS NOT NULL AND TRIM(symbol) <> ''
              ) d ON d.sym = ps.sym AND d.rn = 1
              WHERE ps.rn = 1
                AND (
                  (ps.side = 'LONG' AND UPPER(TRIM(d.bias_raw)) IN ('WEAK_BEARISH', 'BEARISH', 'STRONG_BEARISH'))
                  OR (ps.side = 'SHORT' AND UPPER(TRIM(d.bias_raw)) IN ('STRONG_BULLISH', 'BULLISH', 'WEAK_BULLISH'))
                )
            ) cnt
            """)
    int countOpenSymbolsWithReverseSignal();
}
