package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.AiCallLogDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiCallLogMapper {
    String COLUMNS = "call_id AS callId, analysis_id AS analysisId, trace_id AS traceId, request_id AS requestId, "
            + "opportunity_id AS opportunityId, "
            + "provider_name AS providerName, model_name AS modelName, ai_role AS aiRole, call_status AS callStatus, "
            + "provider_request_id AS providerRequestId, started_at AS startedAt, completed_at AS completedAt, "
            + "latency_ms AS latencyMs, input_tokens AS inputTokens, output_tokens AS outputTokens, "
            + "total_tokens AS totalTokens, reserved_cost_usd AS reservedCostUsd, "
            + "calculated_cost_usd AS calculatedCostUsd, cost_currency AS costCurrency, "
            + "cost_calculation_method AS costCalculationMethod, fallback_flag AS fallbackFlag, "
            + "fallback_reason AS fallbackReason, rate_limited AS rateLimited, budget_blocked AS budgetBlocked, "
            + "timeout_flag AS timeoutFlag, error_code AS errorCode, error_message AS errorMessage, "
            + "request_hash AS requestHash, request_summary AS requestSummary, response_summary AS responseSummary, "
            + "rule_version AS ruleVersion, contract_type AS contractType, candidate_id AS candidateId, "
            + "cache_hit AS cacheHit, observed_at AS observedAt, "
            + "output_payload AS outputPayload, review_only AS reviewOnly, manual_review_only AS manualReviewOnly, "
            + "not_trade_instruction AS notTradeInstruction, not_executable AS notExecutable, "
            + "not_auto_trading AS notAutoTrading, not_order_execution AS notOrderExecution, "
            + "not_user_position_creation AS notUserPositionCreation, not_position_mutation AS notPositionMutation, "
            + "not_state_machine_override AS notStateMachineOverride, "
            + "not_execution_plan_creation AS notExecutionPlanCreation, "
            + "not_final_execution_plan_creation AS notFinalExecutionPlanCreation, "
            + "rule_direction_preserved AS ruleDirectionPreserved, created_at AS createdAt, updated_at AS updatedAt";

    String CHARGEABLE_COST = """
            CASE
              WHEN call_status IN ('DISABLED', 'NOT_CONFIGURED', 'BUDGET_BLOCKED', 'RATE_LIMITED') THEN 0
              WHEN call_status = 'STARTED' THEN COALESCE(reserved_cost_usd, 0)
              WHEN call_status IN ('SUCCESS', 'FAILED', 'TIMEOUT', 'INVALID_RESPONSE') THEN
                CASE
                  WHEN COALESCE(calculated_cost_usd, 0) > 0 THEN calculated_cost_usd
                  ELSE COALESCE(reserved_cost_usd, 0)
                END
              ELSE 0
            END
            """;

    @Insert("""
            INSERT INTO tm_ai_call_log(
              call_id, analysis_id, trace_id, request_id, provider_name, model_name, ai_role, call_status,
              provider_request_id, started_at, completed_at, latency_ms, input_tokens, output_tokens, total_tokens,
              reserved_cost_usd, calculated_cost_usd, cost_currency, cost_calculation_method,
              fallback_flag, fallback_reason, rate_limited, budget_blocked, timeout_flag, error_code, error_message,
              request_hash, request_summary, response_summary, rule_version, contract_type, candidate_id,
              opportunity_id, cache_hit, observed_at, output_payload,
              review_only, manual_review_only, not_trade_instruction, not_executable, not_auto_trading,
              not_order_execution, not_user_position_creation, not_position_mutation,
              not_state_machine_override, not_execution_plan_creation, not_final_execution_plan_creation,
              rule_direction_preserved, created_at, updated_at
            ) VALUES (
              #{callId}, #{analysisId}, #{traceId}, #{requestId}, #{providerName}, #{modelName}, #{aiRole}, #{callStatus},
              #{providerRequestId}, #{startedAt}, #{completedAt}, #{latencyMs}, #{inputTokens}, #{outputTokens}, #{totalTokens},
              #{reservedCostUsd}, #{calculatedCostUsd}, #{costCurrency}, #{costCalculationMethod},
              #{fallbackFlag}, #{fallbackReason}, #{rateLimited}, #{budgetBlocked}, #{timeoutFlag}, #{errorCode}, #{errorMessage},
              #{requestHash}, #{requestSummary}, #{responseSummary}, #{ruleVersion}, #{contractType}, #{candidateId},
              #{opportunityId}, #{cacheHit}, #{observedAt}, #{outputPayload},
              #{reviewOnly}, #{manualReviewOnly}, #{notTradeInstruction}, #{notExecutable}, #{notAutoTrading},
              #{notOrderExecution}, #{notUserPositionCreation}, #{notPositionMutation},
              #{notStateMachineOverride}, #{notExecutionPlanCreation}, #{notFinalExecutionPlanCreation},
              #{ruleDirectionPreserved}, #{createdAt}, #{updatedAt}
            )
            """)
    int insert(AiCallLogDO log);

    @Update("""
            UPDATE tm_ai_call_log SET
              call_status = #{callStatus},
              provider_request_id = #{providerRequestId},
              completed_at = #{completedAt},
              latency_ms = #{latencyMs},
              input_tokens = #{inputTokens},
              output_tokens = #{outputTokens},
              total_tokens = #{totalTokens},
              calculated_cost_usd = #{calculatedCostUsd},
              fallback_flag = #{fallbackFlag},
              fallback_reason = #{fallbackReason},
              rate_limited = #{rateLimited},
              budget_blocked = #{budgetBlocked},
              timeout_flag = #{timeoutFlag},
              error_code = #{errorCode},
              error_message = #{errorMessage},
              response_summary = #{responseSummary},
              cache_hit = #{cacheHit},
              observed_at = #{observedAt},
              output_payload = #{outputPayload},
              updated_at = #{updatedAt}
            WHERE call_id = #{callId}
            """)
    int updateCompletion(AiCallLogDO log);

    @Select("<script>"
            + "SELECT " + COLUMNS + " FROM tm_ai_call_log "
            + """
            WHERE 1 = 1
            <if test='analysisId != null and analysisId != ""'>AND analysis_id = #{analysisId}</if>
            <if test='traceId != null and traceId != ""'>AND trace_id = #{traceId}</if>
            <if test='candidateId != null and candidateId != ""'>AND candidate_id = #{candidateId}</if>
            <if test='aiRole != null and aiRole != ""'>AND ai_role = #{aiRole}</if>
            <if test='providerName != null and providerName != ""'>AND provider_name = #{providerName}</if>
            <if test='callStatus != null and callStatus != ""'>AND call_status = #{callStatus}</if>
            <if test='from != null'>AND started_at &gt;= #{from}</if>
            <if test='to != null'>AND started_at &lt;= #{to}</if>
            ORDER BY started_at DESC, call_id DESC
            LIMIT #{limit}
            </script>
            """)
    List<AiCallLogDO> query(@Param("analysisId") String analysisId,
                            @Param("traceId") String traceId,
                            @Param("candidateId") String candidateId,
                            @Param("aiRole") String aiRole,
                            @Param("providerName") String providerName,
                            @Param("callStatus") String callStatus,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("limit") int limit);

    @Select("<script>"
            + "SELECT " + COLUMNS + " FROM tm_ai_call_log "
            + "WHERE analysis_id IN ("
            + "  SELECT ar.analysis_id FROM tm_analysis_run ar "
            + "  WHERE ar.owner_type = 'USER' AND ar.owner_id = #{userId}"
            + ") "
            + "<if test='analysisId != null and analysisId != \"\"'>AND analysis_id = #{analysisId}</if> "
            + "<if test='traceId != null and traceId != \"\"'>AND trace_id = #{traceId}</if> "
            + "<if test='candidateId != null and candidateId != \"\"'>AND candidate_id = #{candidateId}</if> "
            + "<if test='aiRole != null and aiRole != \"\"'>AND ai_role = #{aiRole}</if> "
            + "<if test='providerName != null and providerName != \"\"'>AND provider_name = #{providerName}</if> "
            + "<if test='callStatus != null and callStatus != \"\"'>AND call_status = #{callStatus}</if> "
            + "<if test='from != null'>AND started_at &gt;= #{from}</if> "
            + "<if test='to != null'>AND started_at &lt;= #{to}</if> "
            + "ORDER BY started_at ASC, call_id ASC LIMIT #{limit}"
            + "</script>")
    List<AiCallLogDO> queryOwned(@Param("userId") Long userId,
                                 @Param("analysisId") String analysisId,
                                 @Param("traceId") String traceId,
                                 @Param("candidateId") String candidateId,
                                 @Param("aiRole") String aiRole,
                                 @Param("providerName") String providerName,
                                 @Param("callStatus") String callStatus,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tm_ai_call_log WHERE provider_name = #{providerName} AND started_at >= #{since}")
    int countProviderAttemptsSince(@Param("providerName") String providerName, @Param("since") LocalDateTime since);

    @Select("SELECT COALESCE(SUM(" + CHARGEABLE_COST + "), 0) FROM tm_ai_call_log WHERE started_at >= #{since}")
    BigDecimal sumChargeableCostSince(@Param("since") LocalDateTime since);

    @Select("SELECT COALESCE(SUM(" + CHARGEABLE_COST + "), 0) FROM tm_ai_call_log WHERE analysis_id = #{analysisId}")
    BigDecimal sumChargeableCostByAnalysisId(@Param("analysisId") String analysisId);
}
