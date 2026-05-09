package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.vo.DecisionResultVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DecisionResultMapper {

    @Insert("INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, market_bias_hierarchy, trade_type, confidence_level, risk_level, action_priority, conclusion_summary, is_worth_opening, multi_tf_convergence, ai_role_results, is_adopted, valid_period, invalid_condition, evidence_summary, explanation_json, review_reasons, ai_conflict_level, ai_conflict_score, ai_plan_mode, confused_score, asset_state_snapshot, create_time) " +
            "VALUES(#{decisionId}, #{analysisId}, #{symbol}, #{marketBiasHierarchy}, #{tradeType}, #{confidenceLevel}, #{riskLevel}, #{actionPriority}, #{conclusionSummary}, #{isWorthOpening}, #{multiTfConvergence}, #{aiRoleResults}, #{isAdopted}, #{validPeriod}, #{invalidCondition}, #{evidenceSummary}, #{explanationJson}, #{reviewReasons}, #{aiConflictLevel}, #{aiConflictScore}, #{aiPlanMode}, #{confusedScore}, #{assetStateSnapshot}, #{createTime})")
    int insert(DecisionResult decision);

    @Select("SELECT * FROM tm_decision_result ORDER BY create_time DESC LIMIT #{limit}")
    List<DecisionResult> findLatestDecisions(int limit);

    @Select("""
            SELECT d.decision_id AS decisionId, d.analysis_id AS analysisId, d.symbol AS symbol,
            d.market_bias_hierarchy AS marketBiasHierarchy, d.trade_type AS tradeType,
            d.confidence_level AS confidenceLevel, d.risk_level AS riskLevel, d.action_priority AS actionPriority,
            d.conclusion_summary AS conclusionSummary, d.is_worth_opening AS isWorthOpening,
            d.multi_tf_convergence AS multiTfConvergence, d.ai_role_results AS aiRoleResults,
            d.is_adopted AS isAdopted, d.valid_period AS validPeriod, d.invalid_condition AS invalidCondition,
            d.evidence_summary AS evidenceSummary, d.explanation_json AS explanationJson, d.review_reasons AS reviewReasons,
            d.ai_conflict_level AS aiConflictLevel, d.ai_conflict_score AS aiConflictScore, d.ai_plan_mode AS aiPlanMode,
            d.confused_score AS confusedScore, d.asset_state_snapshot AS assetStateSnapshot, d.create_time AS createTime,
            NULLIF(TRIM(COALESCE(d.valid_period, '') || CASE WHEN COALESCE(TRIM(d.valid_period), '') <> '' AND COALESCE(TRIM(d.invalid_condition), '') <> '' THEN ' | ' ELSE '' END || COALESCE(d.invalid_condition, '')), '') AS executionPlanSummary,
            p.recommended_action AS recommendedAction,
            p.plan_mode AS planMode,
            p.entry_zone AS entryZone,
            p.stop_loss AS stopLoss,
            p.take_profit_rules AS takeProfitRules,
            p.leverage_suggestion AS leverageSuggestion,
            p.position_suggestion AS positionSuggestion,
            ar.data_quality_score AS dataQualityScore
            FROM tm_decision_result d
            LEFT JOIN (
              SELECT plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules,
                     leverage_suggestion, position_suggestion,
                     ROW_NUMBER() OVER (PARTITION BY analysis_id ORDER BY create_time DESC, plan_id DESC) AS rn
              FROM tm_execution_plan
            ) p ON d.analysis_id = p.analysis_id AND p.rn = 1
            LEFT JOIN tm_analysis_run ar ON d.analysis_id = ar.analysis_id
            ORDER BY d.create_time DESC LIMIT #{limit}
            """)
    List<DecisionResultVO> findLatestDecisionResultsJoined(int limit);

    @Select("""
            SELECT d.decision_id AS decisionId, d.analysis_id AS analysisId, d.symbol AS symbol,
            d.market_bias_hierarchy AS marketBiasHierarchy, d.trade_type AS tradeType,
            d.confidence_level AS confidenceLevel, d.risk_level AS riskLevel, d.action_priority AS actionPriority,
            d.conclusion_summary AS conclusionSummary, d.is_worth_opening AS isWorthOpening,
            d.multi_tf_convergence AS multiTfConvergence, d.ai_role_results AS aiRoleResults,
            d.is_adopted AS isAdopted, d.valid_period AS validPeriod, d.invalid_condition AS invalidCondition,
            d.evidence_summary AS evidenceSummary, d.explanation_json AS explanationJson, d.review_reasons AS reviewReasons,
            d.ai_conflict_level AS aiConflictLevel, d.ai_conflict_score AS aiConflictScore, d.ai_plan_mode AS aiPlanMode,
            d.confused_score AS confusedScore, d.asset_state_snapshot AS assetStateSnapshot, d.create_time AS createTime,
            NULLIF(TRIM(COALESCE(d.valid_period, '') || CASE WHEN COALESCE(TRIM(d.valid_period), '') <> '' AND COALESCE(TRIM(d.invalid_condition), '') <> '' THEN ' | ' ELSE '' END || COALESCE(d.invalid_condition, '')), '') AS executionPlanSummary
            FROM tm_decision_result d
            WHERE UPPER(TRIM(d.symbol)) = #{normalizedSymbol}
            ORDER BY d.create_time DESC
            LIMIT 1
            """)
    DecisionResultVO findLatestDecisionResultBaseBySymbol(@Param("normalizedSymbol") String normalizedSymbol);

    @Select("""
            SELECT d.decision_id AS decisionId, d.analysis_id AS analysisId, d.symbol AS symbol,
            d.market_bias_hierarchy AS marketBiasHierarchy, d.trade_type AS tradeType,
            d.confidence_level AS confidenceLevel, d.risk_level AS riskLevel, d.action_priority AS actionPriority,
            d.conclusion_summary AS conclusionSummary, d.is_worth_opening AS isWorthOpening,
            d.multi_tf_convergence AS multiTfConvergence, d.ai_role_results AS aiRoleResults,
            d.is_adopted AS isAdopted, d.valid_period AS validPeriod, d.invalid_condition AS invalidCondition,
            d.evidence_summary AS evidenceSummary, d.explanation_json AS explanationJson, d.review_reasons AS reviewReasons,
            d.ai_conflict_level AS aiConflictLevel, d.ai_conflict_score AS aiConflictScore, d.ai_plan_mode AS aiPlanMode,
            d.confused_score AS confusedScore, d.asset_state_snapshot AS assetStateSnapshot, d.create_time AS createTime,
            NULLIF(TRIM(COALESCE(d.valid_period, '') || CASE WHEN COALESCE(TRIM(d.valid_period), '') <> '' AND COALESCE(TRIM(d.invalid_condition), '') <> '' THEN ' | ' ELSE '' END || COALESCE(d.invalid_condition, '')), '') AS executionPlanSummary,
            p.recommended_action AS recommendedAction,
            p.plan_mode AS planMode,
            p.entry_zone AS entryZone,
            p.stop_loss AS stopLoss,
            p.take_profit_rules AS takeProfitRules,
            p.leverage_suggestion AS leverageSuggestion,
            p.position_suggestion AS positionSuggestion,
            ar.data_quality_score AS dataQualityScore
            FROM tm_decision_result d
            LEFT JOIN (
              SELECT plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules,
                     leverage_suggestion, position_suggestion,
                     ROW_NUMBER() OVER (PARTITION BY analysis_id ORDER BY create_time DESC, plan_id DESC) AS rn
              FROM tm_execution_plan
            ) p ON d.analysis_id = p.analysis_id AND p.rn = 1
            LEFT JOIN tm_analysis_run ar ON d.analysis_id = ar.analysis_id
            WHERE UPPER(TRIM(d.symbol)) = #{normalizedSymbol}
            ORDER BY d.create_time DESC LIMIT 1
            """)
    DecisionResultVO findLatestDecisionResultBySymbolJoined(@Param("normalizedSymbol") String normalizedSymbol);

    @Select("SELECT MAX(create_time) FROM tm_decision_result")
    LocalDateTime selectLastDecisionTime();

    @Select("SELECT COUNT(*) FROM tm_decision_result WHERE CAST(create_time AS DATE) = CURRENT_DATE")
    int countDecisionsToday();

    @Select("SELECT * FROM tm_decision_result WHERE analysis_id = #{analysisId} ORDER BY create_time DESC LIMIT 1")
    DecisionResult selectLatestByAnalysisId(String analysisId);

    @Select("""
            SELECT analysis_id
            FROM tm_decision_result
            WHERE UPPER(TRIM(symbol)) = #{normalizedSymbol}
            ORDER BY create_time DESC, decision_id DESC
            LIMIT 1
            """)
    String selectLatestAnalysisIdBySymbol(@Param("normalizedSymbol") String normalizedSymbol);

    /**
     * OPEN 持仓（每 symbol 一条代表行，多条 OPEN 时按 position_id 取首条）与每 symbol 最新决策（create_time DESC，并列时 decision_id DESC）；
     * 仅在最新 bias 为 BULLISH/BEARISH 且与 LONG/SHORT 反向时计入；按 symbol 去重计数。
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
                AND UPPER(TRIM(d.bias_raw)) IN ('BULLISH', 'BEARISH')
                AND (
                  (ps.side = 'LONG' AND UPPER(TRIM(d.bias_raw)) = 'BEARISH')
                  OR (ps.side = 'SHORT' AND UPPER(TRIM(d.bias_raw)) = 'BULLISH')
                )
            ) cnt
            """)
    int countOpenSymbolsWithReverseSignal();
}
