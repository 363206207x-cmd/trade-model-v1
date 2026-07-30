package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.opportunitylog.OpportunityLogCountRow;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OpportunityLogMapper {
    String SHARED_STATE_PREDICATE = " AND user_position_id IS NULL "
            + "AND COALESCE(user_position_present, FALSE) = FALSE "
            + "AND COALESCE(lifecycle_status, '') != 'REVIEW_REQUIRED' "
            + "AND COALESCE(opportunity_status, '') NOT IN ('EXECUTED_VALID', 'EXECUTED_INVALID') "
            + "AND COALESCE(reason_codes, '') NOT LIKE '%MULTIPLE_LINKED_USER_POSITIONS%' "
            + "AND COALESCE(reason_codes, '') NOT LIKE '%LINKED_USER_POSITION_%' "
            + "AND COALESCE(reason_codes, '') NOT LIKE '%USER_POSITION_PROJECTION_UNAVAILABLE%' ";

    String PUBLIC_PROJECTION_PREDICATE = " AND opportunity_id IS NOT NULL ";

    String PUBLIC_LIFECYCLE_EXPRESSION = "CASE "
            + "WHEN lifecycle_status IN ('PENDING_EVALUATION', 'RESOLVED', 'SOURCE_INCOMPLETE', "
            + "'MARKET_PATH_UNAVAILABLE', 'AMBIGUOUS_MARKET_PATH') THEN lifecycle_status "
            + "WHEN lifecycle_status = 'REVIEW_REQUIRED' AND hit_order IN ('TARGET_FIRST', 'INVALIDATION_FIRST') "
            + "THEN 'RESOLVED' "
            + "WHEN lifecycle_status = 'REVIEW_REQUIRED' THEN 'PENDING_EVALUATION' "
            + "ELSE lifecycle_status END";

    String PUBLIC_STATUS_EXPRESSION = "CASE "
            + "WHEN hit_order = 'TARGET_FIRST' THEN 'MISSED_VALID' "
            + "WHEN hit_order = 'INVALIDATION_FIRST' THEN 'MISSED_INVALID' "
            + "WHEN opportunity_status IN ('MISSED_VALID', 'MISSED_INVALID') THEN opportunity_status "
            + "WHEN opportunity_status IN ('PUSHED_NOT_FILLED_VALID', 'BLOCKED_BY_RISK_VALID', "
            + "'EXECUTED_VALID', 'EXECUTED_INVALID') THEN NULL "
            + "ELSE opportunity_status END";

    String PUBLIC_RESOLVED_AT_EXPRESSION = "CASE WHEN (" + PUBLIC_LIFECYCLE_EXPRESSION
            + ") = 'RESOLVED' THEN resolved_at ELSE NULL END";

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

    String PUBLIC_MESSAGE_SELECT = "SELECT opportunity_id AS opportunityId, analysis_id AS analysisId, "
            + "symbol, timeframe, direction, " + PUBLIC_LIFECYCLE_EXPRESSION + " AS lifecycleStatus, "
            + PUBLIC_STATUS_EXPRESSION + " AS opportunityStatus, "
            + "anchor_time AS anchorTime, created_at AS createdAt "
            + "FROM tm_opportunity_log ";

    String PUBLIC_API_SELECT = "SELECT opportunity_id AS opportunityId, analysis_id AS analysisId, "
            + "symbol, timeframe, direction, " + PUBLIC_LIFECYCLE_EXPRESSION + " AS lifecycleStatus, "
            + PUBLIC_STATUS_EXPRESSION + " AS opportunityStatus, "
            + "anchor_time AS anchorTime, " + PUBLIC_RESOLVED_AT_EXPRESSION + " AS resolvedAt, "
            + "entry_reference AS entryReference, "
            + "target_price AS targetPrice, invalidation_price AS invalidationPrice, "
            + "target_hit AS targetHit, invalidation_hit AS invalidationHit, "
            + "target_hit_at AS targetHitAt, invalidation_hit_at AS invalidationHitAt, "
            + "hit_order AS hitOrder, mfe_price AS mfePrice, mfe_ratio AS mfeRatio, "
            + "mae_price AS maePrice, mae_ratio AS maeRatio, market_data_source AS marketDataSource, "
            + "created_at AS createdAt, updated_at AS updatedAt, "
            + "TRUE AS reviewOnly, TRUE AS manualReviewOnly, TRUE AS notTradeInstruction, "
            + "TRUE AS notExecutable, TRUE AS notAutoTrading, TRUE AS notOrderExecution, "
            + "TRUE AS notUserPositionCreation, TRUE AS notUserPositionMutation, "
            + "TRUE AS notPushSend, TRUE AS notExternalChannel "
            + "FROM tm_opportunity_log ";

    String PUBLIC_EVALUATION_SELECT = "SELECT opportunity_id AS opportunityId, "
            + "opportunity_key AS opportunityKey, analysis_id AS analysisId, decision_id AS decisionId, "
            + "execution_plan_id AS executionPlanId, symbol, timeframe, direction, "
            + PUBLIC_LIFECYCLE_EXPRESSION + " AS lifecycleStatus, "
            + PUBLIC_STATUS_EXPRESSION + " AS opportunityStatus, anchor_time AS anchorTime, "
            + PUBLIC_RESOLVED_AT_EXPRESSION + " AS resolvedAt, entry_reference AS entryReference, "
            + "target_price AS targetPrice, invalidation_price AS invalidationPrice, "
            + "target_hit AS targetHit, invalidation_hit AS invalidationHit, "
            + "target_hit_at AS targetHitAt, invalidation_hit_at AS invalidationHitAt, "
            + "hit_order AS hitOrder, mfe_price AS mfePrice, mfe_ratio AS mfeRatio, "
            + "mae_price AS maePrice, mae_ratio AS maeRatio, source_type AS sourceType, "
            + "source_reference AS sourceReference, market_data_source AS marketDataSource, "
            + "market_data_trace_id AS marketDataTraceId, trace_id AS traceId, "
            + "created_at AS createdAt, updated_at AS updatedAt "
            + "FROM tm_opportunity_log ";

    String PUBLIC_EVALUATION_UPDATE = "UPDATE tm_opportunity_log SET "
            + "lifecycle_status = #{lifecycleStatus}, opportunity_status = #{opportunityStatus}, "
            + "evaluation_as_of = #{evaluationAsOf}, resolved_at = #{resolvedAt}, "
            + "target_hit = #{targetHit}, invalidation_hit = #{invalidationHit}, "
            + "target_hit_at = #{targetHitAt}, invalidation_hit_at = #{invalidationHitAt}, "
            + "hit_order = #{hitOrder}, mfe_price = #{mfePrice}, mfe_ratio = #{mfeRatio}, "
            + "mae_price = #{maePrice}, mae_ratio = #{maeRatio}, "
            + "market_data_source = #{marketDataSource}, market_data_trace_id = #{marketDataTraceId}, "
            + "reason_codes = #{reasonCodes}, updated_at = #{updatedAt} "
            + "WHERE opportunity_id = #{opportunityId} AND lifecycle_status <> 'RESOLVED'";

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

    @Select(BASE_SELECT + "WHERE opportunity_id = #{opportunityId} AND push_id IS NOT NULL"
            + SHARED_STATE_PREDICATE)
    OpportunityLogDO selectPushBackedSharedByOpportunityId(@Param("opportunityId") String opportunityId);

    @Select(BASE_SELECT + "WHERE push_id IS NOT NULL"
            + SHARED_STATE_PREDICATE
            + " ORDER BY anchor_time DESC, opportunity_id DESC LIMIT #{limit}")
    List<OpportunityLogDO> listPushBackedShared(@Param("limit") int limit);

    @Select(PUBLIC_MESSAGE_SELECT + "WHERE opportunity_id = #{opportunityId}"
            + PUBLIC_PROJECTION_PREDICATE)
    OpportunityLogDO selectPublicMessageByOpportunityId(@Param("opportunityId") String opportunityId);

    @Select(PUBLIC_MESSAGE_SELECT + "WHERE 1 = 1"
            + PUBLIC_PROJECTION_PREDICATE
            + " ORDER BY anchor_time DESC, opportunity_id DESC LIMIT #{limit}")
    List<OpportunityLogDO> listPublicMessages(@Param("limit") int limit);

    @Select(PUBLIC_API_SELECT + "WHERE opportunity_id = #{opportunityId}"
            + PUBLIC_PROJECTION_PREDICATE)
    OpportunityLogPublicDTO selectPublicApiByOpportunityId(
            @Param("opportunityId") String opportunityId);

    @Select(PUBLIC_EVALUATION_SELECT + "WHERE opportunity_id = #{opportunityId}"
            + PUBLIC_PROJECTION_PREDICATE)
    OpportunityLogDO selectPublicEvaluationSourceByOpportunityId(
            @Param("opportunityId") String opportunityId);

    @Select({
            "<script>",
            PUBLIC_API_SELECT,
            "WHERE 1 = 1",
            PUBLIC_PROJECTION_PREDICATE,
            "<if test='analysisId != null and analysisId != \"\"'> AND analysis_id = #{analysisId}</if>",
            "<if test='decisionId != null and decisionId != \"\"'> AND decision_id = #{decisionId}</if>",
            "<if test='executionPlanId != null and executionPlanId != \"\"'> AND execution_plan_id = #{executionPlanId}</if>",
            "<if test='symbol != null and symbol != \"\"'> AND UPPER(TRIM(symbol)) = UPPER(TRIM(#{symbol}))</if>",
            "<if test='opportunityStatus != null and opportunityStatus != \"\"'> AND (",
            PUBLIC_STATUS_EXPRESSION,
            ") = #{opportunityStatus}</if>",
            "<if test='lifecycleStatus != null and lifecycleStatus != \"\"'> AND (",
            PUBLIC_LIFECYCLE_EXPRESSION,
            ") = #{lifecycleStatus}</if>",
            "<if test='from != null'> AND anchor_time &gt;= #{from}</if>",
            "<if test='to != null'> AND anchor_time &lt;= #{to}</if>",
            "ORDER BY anchor_time DESC, opportunity_id DESC LIMIT #{limit}",
            "</script>"
    })
    List<OpportunityLogPublicDTO> queryPublicApi(
            @Param("analysisId") String analysisId,
            @Param("decisionId") String decisionId,
            @Param("executionPlanId") String executionPlanId,
            @Param("symbol") String symbol,
            @Param("opportunityStatus") String opportunityStatus,
            @Param("lifecycleStatus") String lifecycleStatus,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("limit") int limit);

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

    @Select({
            "<script>",
            "SELECT",
            "COUNT(*) AS totalCount,",
            "COALESCE(SUM(CASE WHEN lifecycle_status = 'RESOLVED' THEN 1 ELSE 0 END), 0) AS resolvedCount,",
            "COALESCE(SUM(CASE WHEN lifecycle_status &lt;&gt; 'RESOLVED' THEN 1 ELSE 0 END), 0) AS pendingCount,",
            "COALESCE(SUM(CASE WHEN opportunity_status = 'EXECUTED_VALID' THEN 1 ELSE 0 END), 0) AS executedValidCount,",
            "COALESCE(SUM(CASE WHEN opportunity_status = 'EXECUTED_INVALID' THEN 1 ELSE 0 END), 0) AS executedInvalidCount,",
            "COALESCE(SUM(CASE WHEN opportunity_status = 'MISSED_VALID' THEN 1 ELSE 0 END), 0) AS missedValidCount,",
            "COALESCE(SUM(CASE WHEN opportunity_status = 'MISSED_INVALID' THEN 1 ELSE 0 END), 0) AS missedInvalidCount,",
            "COALESCE(SUM(CASE WHEN opportunity_status = 'PUSHED_NOT_FILLED_VALID' THEN 1 ELSE 0 END), 0) AS pushedNotFilledValidCount,",
            "COALESCE(SUM(CASE WHEN opportunity_status = 'BLOCKED_BY_RISK_VALID' THEN 1 ELSE 0 END), 0) AS blockedByRiskValidCount,",
            "COALESCE(SUM(CASE WHEN hit_order = 'TARGET_FIRST' THEN 1 ELSE 0 END), 0) AS targetFirstCount,",
            "COALESCE(SUM(CASE WHEN hit_order = 'INVALIDATION_FIRST' THEN 1 ELSE 0 END), 0) AS invalidationFirstCount,",
            "COALESCE(SUM(CASE WHEN hit_order = 'AMBIGUOUS_SAME_BAR' THEN 1 ELSE 0 END), 0) AS ambiguousCount,",
            "COALESCE(AVG(mfe_ratio), 0) AS averageMfeRatio,",
            "COALESCE(AVG(mae_ratio), 0) AS averageMaeRatio,",
            "COALESCE(MAX(mfe_ratio), 0) AS maxMfeRatio,",
            "COALESCE(MAX(mae_ratio), 0) AS maxMaeRatio,",
            "COALESCE(SUM(CASE WHEN opportunity_status IN ('EXECUTED_VALID', 'MISSED_VALID',",
            "'PUSHED_NOT_FILLED_VALID', 'BLOCKED_BY_RISK_VALID') THEN 1 ELSE 0 END), 0) AS validOpportunityCount,",
            "COALESCE(SUM(CASE WHEN opportunity_status IN ('EXECUTED_INVALID', 'MISSED_INVALID') THEN 1 ELSE 0 END), 0) AS invalidOpportunityCount",
            "FROM tm_opportunity_log",
            "WHERE 1 = 1",
            SHARED_STATE_PREDICATE,
            "<if test='symbol != null and symbol != \"\"'> AND UPPER(TRIM(symbol)) = UPPER(TRIM(#{symbol}))</if>",
            "<if test='from != null'> AND anchor_time &gt;= #{from}</if>",
            "<if test='to != null'> AND anchor_time &lt;= #{to}</if>",
            "</script>"
    })
    OpportunityLogStatsDTO aggregateStats(@Param("symbol") String symbol,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Select({
            "<script>",
            "SELECT COALESCE(opportunity_status, lifecycle_status, 'UNKNOWN') AS name, COUNT(*) AS count",
            "FROM tm_opportunity_log",
            "WHERE 1 = 1",
            SHARED_STATE_PREDICATE,
            "<if test='symbol != null and symbol != \"\"'> AND UPPER(TRIM(symbol)) = UPPER(TRIM(#{symbol}))</if>",
            "<if test='from != null'> AND anchor_time &gt;= #{from}</if>",
            "<if test='to != null'> AND anchor_time &lt;= #{to}</if>",
            "GROUP BY COALESCE(opportunity_status, lifecycle_status, 'UNKNOWN')",
            "ORDER BY name ASC",
            "</script>"
    })
    List<OpportunityLogCountRow> countByStatus(@Param("symbol") String symbol,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Select({
            "<script>",
            "SELECT COALESCE(source_type, 'UNKNOWN') AS name, COUNT(*) AS count",
            "FROM tm_opportunity_log",
            "WHERE 1 = 1",
            SHARED_STATE_PREDICATE,
            "<if test='symbol != null and symbol != \"\"'> AND UPPER(TRIM(symbol)) = UPPER(TRIM(#{symbol}))</if>",
            "<if test='from != null'> AND anchor_time &gt;= #{from}</if>",
            "<if test='to != null'> AND anchor_time &lt;= #{to}</if>",
            "GROUP BY COALESCE(source_type, 'UNKNOWN')",
            "ORDER BY name ASC",
            "</script>"
    })
    List<OpportunityLogCountRow> countBySource(@Param("symbol") String symbol,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Update(PUBLIC_EVALUATION_UPDATE)
    int updateEvaluation(OpportunityLogDO row);
}
