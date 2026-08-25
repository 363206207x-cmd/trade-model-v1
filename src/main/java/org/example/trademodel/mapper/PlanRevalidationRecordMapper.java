package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.PlanRevalidationRecordDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PlanRevalidationRecordMapper {
    @Insert("INSERT INTO tm_plan_revalidation_record(record_id, plan_id, analysis_id, trigger_type, state, "
            + "source_plan_version, result_plan_version, result_plan_id, reason, result_summary, trace_id, "
            + "requested_by_user_id, started_at, completed_at, created_at, updated_at, "
            + "not_trade_instruction, not_order_execution) VALUES(#{recordId}, #{planId}, #{analysisId}, "
            + "#{triggerType}, #{state}, #{sourcePlanVersion}, #{resultPlanVersion}, #{resultPlanId}, "
            + "#{reason}, #{resultSummary}, #{traceId}, #{requestedByUserId}, #{startedAt}, #{completedAt}, "
            + "#{createdAt}, #{updatedAt}, #{notTradeInstruction}, #{notOrderExecution})")
    int insert(PlanRevalidationRecordDO row);

    @Select("SELECT * FROM tm_plan_revalidation_record WHERE record_id = #{recordId}")
    PlanRevalidationRecordDO selectById(String recordId);

    @Select("SELECT * FROM tm_plan_revalidation_record WHERE plan_id = #{planId} "
            + "ORDER BY created_at DESC, record_id DESC LIMIT #{limit}")
    List<PlanRevalidationRecordDO> listByPlanId(@Param("planId") String planId,
                                                @Param("limit") int limit);

    @Select("SELECT record.* FROM tm_plan_revalidation_record record "
            + "INNER JOIN tm_analysis_run ar ON ar.analysis_id = record.analysis_id "
            + "WHERE record.plan_id = #{planId} AND ar.owner_type = 'USER' AND ar.owner_id = #{userId} "
            + "ORDER BY record.created_at DESC, record.record_id DESC LIMIT #{limit}")
    List<PlanRevalidationRecordDO> listByPlanIdForUser(@Param("planId") String planId,
                                                       @Param("userId") Long userId,
                                                       @Param("limit") int limit);

    @Update("UPDATE tm_plan_revalidation_record SET state = #{state}, result_plan_version = #{resultPlanVersion}, "
            + "result_plan_id = #{resultPlanId}, result_summary = #{resultSummary}, completed_at = #{completedAt}, "
            + "updated_at = #{updatedAt} WHERE record_id = #{recordId} AND state IN ('QUEUED', 'RUNNING', 'PARTIAL')")
    int complete(PlanRevalidationRecordDO row);

    @Update("UPDATE tm_plan_revalidation_record SET state = 'RUNNING', started_at = #{startedAt}, "
            + "updated_at = #{startedAt} WHERE record_id = #{recordId} AND state = 'QUEUED'")
    int markRunning(@Param("recordId") String recordId, @Param("startedAt") LocalDateTime startedAt);
}
