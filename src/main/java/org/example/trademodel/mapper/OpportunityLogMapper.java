package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.OpportunityLogDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OpportunityLogMapper {
    String BASE_SELECT = "SELECT opportunity_id AS opportunityId, opportunity_key AS opportunityKey, "
            + "analysis_id AS analysisId, decision_id AS decisionId, execution_plan_id AS executionPlanId, "
            + "push_id AS pushId, user_position_id AS userPositionId, symbol, timeframe, direction, "
            + "lifecycle_status AS lifecycleStatus, opportunity_status AS opportunityStatus, "
            + "anchor_time AS anchorTime, evaluation_as_of AS evaluationAsOf, resolved_at AS resolvedAt, "
            + "entry_reference AS entryReference, target_price AS targetPrice, invalidation_price AS invalidationPrice, "
            + "target_hit AS targetHit, invalidation_hit AS invalidationHit, target_hit_at AS targetHitAt, "
            + "invalidation_hit_at AS invalidationHitAt, hit_order AS hitOrder, mfe_price AS mfePrice, "
            + "mfe_ratio AS mfeRatio, mae_price AS maePrice, mae_ratio AS maeRatio, push_present AS pushPresent, "
            + "risk_blocked_evidence AS riskBlockedEvidence, risk_blocked_at AS riskBlockedAt, "
            + "user_position_present AS userPositionPresent, source_type AS sourceType, source_reference AS sourceReference, "
            + "market_data_source AS marketDataSource, market_data_trace_id AS marketDataTraceId, "
            + "reason_codes AS reasonCodes, trace_id AS traceId, review_only AS reviewOnly, "
            + "manual_review_only AS manualReviewOnly, not_trade_instruction AS notTradeInstruction, "
            + "not_executable AS notExecutable, not_auto_trading AS notAutoTrading, "
            + "not_order_execution AS notOrderExecution, not_user_position_creation AS notUserPositionCreation, "
            + "not_user_position_mutation AS notUserPositionMutation, not_push_send AS notPushSend, "
            + "not_external_channel AS notExternalChannel, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM tm_opportunity_log ";

    @Insert("INSERT INTO tm_opportunity_log(opportunity_id, opportunity_key, analysis_id, decision_id, execution_plan_id, "
            + "push_id, user_position_id, symbol, timeframe, direction, lifecycle_status, opportunity_status, anchor_time, "
            + "evaluation_as_of, resolved_at, entry_reference, target_price, invalidation_price, target_hit, invalidation_hit, "
            + "target_hit_at, invalidation_hit_at, hit_order, mfe_price, mfe_ratio, mae_price, mae_ratio, push_present, "
            + "risk_blocked_evidence, risk_blocked_at, user_position_present, source_type, source_reference, "
            + "market_data_source, market_data_trace_id, reason_codes, trace_id, review_only, manual_review_only, "
            + "not_trade_instruction, not_executable, not_auto_trading, not_order_execution, not_user_position_creation, "
            + "not_user_position_mutation, not_push_send, not_external_channel, created_at, updated_at) "
            + "VALUES(#{opportunityId}, #{opportunityKey}, #{analysisId}, #{decisionId}, #{executionPlanId}, "
            + "#{pushId}, #{userPositionId}, #{symbol}, #{timeframe}, #{direction}, #{lifecycleStatus}, #{opportunityStatus}, "
            + "#{anchorTime}, #{evaluationAsOf}, #{resolvedAt}, #{entryReference}, #{targetPrice}, #{invalidationPrice}, "
            + "#{targetHit}, #{invalidationHit}, #{targetHitAt}, #{invalidationHitAt}, #{hitOrder}, #{mfePrice}, "
            + "#{mfeRatio}, #{maePrice}, #{maeRatio}, #{pushPresent}, #{riskBlockedEvidence}, #{riskBlockedAt}, "
            + "#{userPositionPresent}, #{sourceType}, #{sourceReference}, #{marketDataSource}, #{marketDataTraceId}, "
            + "#{reasonCodes}, #{traceId}, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, #{createdAt}, #{updatedAt})")
    int insert(OpportunityLogDO row);

    @Select(BASE_SELECT + "WHERE opportunity_id = #{opportunityId}")
    OpportunityLogDO selectByOpportunityId(@Param("opportunityId") String opportunityId);

    @Select(BASE_SELECT + "WHERE opportunity_key = #{opportunityKey}")
    OpportunityLogDO selectByOpportunityKey(@Param("opportunityKey") String opportunityKey);

    @Select({
            "<script>",
            BASE_SELECT,
            "WHERE 1 = 1",
            "<if test='analysisId != null and analysisId != \"\"'> AND analysis_id = #{analysisId}</if>",
            "<if test='decisionId != null and decisionId != \"\"'> AND decision_id = #{decisionId}</if>",
            "<if test='executionPlanId != null and executionPlanId != \"\"'> AND execution_plan_id = #{executionPlanId}</if>",
            "<if test='symbol != null and symbol != \"\"'> AND UPPER(TRIM(symbol)) = UPPER(TRIM(#{symbol}))</if>",
            "<if test='opportunityStatus != null and opportunityStatus != \"\"'> AND opportunity_status = #{opportunityStatus}</if>",
            "<if test='lifecycleStatus != null and lifecycleStatus != \"\"'> AND lifecycle_status = #{lifecycleStatus}</if>",
            "<if test='from != null'> AND anchor_time &gt;= #{from}</if>",
            "<if test='to != null'> AND anchor_time &lt;= #{to}</if>",
            "ORDER BY anchor_time DESC, opportunity_id DESC LIMIT #{limit}",
            "</script>"
    })
    List<OpportunityLogDO> query(@Param("analysisId") String analysisId,
                                 @Param("decisionId") String decisionId,
                                 @Param("executionPlanId") String executionPlanId,
                                 @Param("symbol") String symbol,
                                 @Param("opportunityStatus") String opportunityStatus,
                                 @Param("lifecycleStatus") String lifecycleStatus,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("limit") int limit);

    @Update("UPDATE tm_opportunity_log SET user_position_id = #{userPositionId}, "
            + "user_position_present = #{userPositionPresent}, lifecycle_status = #{lifecycleStatus}, "
            + "opportunity_status = #{opportunityStatus}, evaluation_as_of = #{evaluationAsOf}, "
            + "resolved_at = #{resolvedAt}, target_hit = #{targetHit}, invalidation_hit = #{invalidationHit}, "
            + "target_hit_at = #{targetHitAt}, invalidation_hit_at = #{invalidationHitAt}, hit_order = #{hitOrder}, "
            + "mfe_price = #{mfePrice}, mfe_ratio = #{mfeRatio}, mae_price = #{maePrice}, mae_ratio = #{maeRatio}, "
            + "push_present = #{pushPresent}, risk_blocked_evidence = #{riskBlockedEvidence}, "
            + "risk_blocked_at = #{riskBlockedAt}, market_data_source = #{marketDataSource}, "
            + "market_data_trace_id = #{marketDataTraceId}, reason_codes = #{reasonCodes}, updated_at = #{updatedAt} "
            + "WHERE opportunity_id = #{opportunityId} AND lifecycle_status <> 'RESOLVED'")
    int updateEvaluation(OpportunityLogDO row);
}
