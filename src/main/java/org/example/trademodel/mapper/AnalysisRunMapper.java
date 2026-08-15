package org.example.trademodel.mapper;

import org.example.trademodel.entity.AnalysisRunDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AnalysisRunMapper {
    @Insert("INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, rule_version, data_quality_score, trace_id, status, "
            + "idempotency_key, request_id, trigger_type, trigger_reference, parent_analysis_id, parent_trace_id, "
            + "input_snapshot_json, input_snapshot_hash, attempt_count, lease_owner, lease_expires_at, started_at, completed_at, "
            + "error_code, error_message, created_at, updated_at, version_no, owner_type, owner_id, asset_id, preview, analysis_mode) "
            + "VALUES(#{analysisId}, #{symbol}, #{timeframe}, #{analysisTime}, #{ruleVersion}, #{dataQualityScore}, #{traceId}, #{status}, "
            + "#{idempotencyKey}, #{requestId}, #{triggerType}, #{triggerReference}, #{parentAnalysisId}, #{parentTraceId}, "
            + "#{inputSnapshotJson}, #{inputSnapshotHash}, #{attemptCount}, #{leaseOwner}, #{leaseExpiresAt}, #{startedAt}, #{completedAt}, "
            + "#{errorCode}, #{errorMessage}, #{createdAt}, #{updatedAt}, #{versionNo}, #{ownerType}, #{ownerId}, #{assetId}, #{preview}, #{analysisMode})")
    int insert(AnalysisRunDO analysisRun);

    @Insert("INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, rule_version, trace_id, status, "
            + "idempotency_key, request_id, trigger_type, trigger_reference, parent_analysis_id, parent_trace_id, "
            + "input_snapshot_json, input_snapshot_hash, attempt_count, lease_owner, lease_expires_at, started_at, created_at, updated_at, version_no, "
            + "owner_type, owner_id, asset_id, preview, analysis_mode) "
            + "VALUES(#{analysisId}, #{symbol}, #{timeframe}, #{analysisTime}, #{ruleVersion}, #{traceId}, #{status}, "
            + "#{idempotencyKey}, #{requestId}, #{triggerType}, #{triggerReference}, #{parentAnalysisId}, #{parentTraceId}, "
            + "#{inputSnapshotJson}, #{inputSnapshotHash}, #{attemptCount}, #{leaseOwner}, #{leaseExpiresAt}, #{startedAt}, #{createdAt}, #{updatedAt}, #{versionNo}, "
            + "#{ownerType}, #{ownerId}, #{assetId}, #{preview}, #{analysisMode})")
    int insertStarted(AnalysisRunDO analysisRun);

    @Select("SELECT * FROM tm_analysis_run WHERE analysis_id = #{analysisId}")
    AnalysisRunDO selectById(String analysisId);

    @Select("SELECT * FROM tm_analysis_run WHERE trace_id = #{traceId}")
    AnalysisRunDO selectByTraceId(String traceId);

    @Select("SELECT ar.* FROM tm_analysis_run ar WHERE ar.analysis_id = #{analysisId} AND ("
            + "(ar.owner_type = 'USER' AND ar.owner_id = #{userId}) OR "
            + "(ar.owner_type = 'SYSTEM' AND EXISTS (SELECT 1 FROM tm_asset_pool_item ap "
            + "WHERE UPPER(ap.symbol) = UPPER(ar.symbol) AND ap.active = TRUE AND ("
            + "(ap.owner_type = 'USER' AND ap.owner_id = #{userId}) OR "
            + "(ap.owner_type = 'SYSTEM' AND ap.owner_id = 0 AND NOT EXISTS ("
            + "SELECT 1 FROM tm_asset_pool_item ov WHERE ov.owner_type = 'USER' "
            + "AND ov.owner_id = #{userId} AND UPPER(ov.symbol) = UPPER(ar.symbol) AND ov.active = FALSE)))))) "
            + "LIMIT 1")
    AnalysisRunDO selectReadableByUser(@Param("analysisId") String analysisId,
                                       @Param("userId") Long userId);

    @Select("SELECT ar.* FROM tm_analysis_run ar WHERE ar.trace_id = #{traceId} AND ("
            + "(ar.owner_type = 'USER' AND ar.owner_id = #{userId}) OR "
            + "(ar.owner_type = 'SYSTEM' AND EXISTS (SELECT 1 FROM tm_asset_pool_item ap "
            + "WHERE UPPER(ap.symbol) = UPPER(ar.symbol) AND ap.active = TRUE AND ("
            + "(ap.owner_type = 'USER' AND ap.owner_id = #{userId}) OR "
            + "(ap.owner_type = 'SYSTEM' AND ap.owner_id = 0 AND NOT EXISTS ("
            + "SELECT 1 FROM tm_asset_pool_item ov WHERE ov.owner_type = 'USER' "
            + "AND ov.owner_id = #{userId} AND UPPER(ov.symbol) = UPPER(ar.symbol) AND ov.active = FALSE)))))) "
            + "ORDER BY ar.created_at DESC, ar.analysis_id DESC LIMIT 1")
    AnalysisRunDO selectReadableByTraceId(@Param("traceId") String traceId,
                                          @Param("userId") Long userId);

    @Select("SELECT ar.* FROM tm_analysis_run ar WHERE ar.request_id = #{requestId} AND ("
            + "(ar.owner_type = 'USER' AND ar.owner_id = #{userId}) OR "
            + "(ar.owner_type = 'SYSTEM' AND EXISTS (SELECT 1 FROM tm_asset_pool_item ap "
            + "WHERE UPPER(ap.symbol) = UPPER(ar.symbol) AND ap.active = TRUE AND ("
            + "(ap.owner_type = 'USER' AND ap.owner_id = #{userId}) OR "
            + "(ap.owner_type = 'SYSTEM' AND ap.owner_id = 0 AND NOT EXISTS ("
            + "SELECT 1 FROM tm_asset_pool_item ov WHERE ov.owner_type = 'USER' "
            + "AND ov.owner_id = #{userId} AND UPPER(ov.symbol) = UPPER(ar.symbol) AND ov.active = FALSE)))))) "
            + "ORDER BY ar.created_at DESC, ar.analysis_id DESC LIMIT 1")
    AnalysisRunDO selectReadableByRequestId(@Param("requestId") String requestId,
                                            @Param("userId") Long userId);

    @Select("SELECT * FROM tm_analysis_run WHERE request_id = #{requestId} ORDER BY created_at DESC, analysis_id DESC LIMIT 1")
    AnalysisRunDO selectByRequestId(String requestId);

    @Select("SELECT * FROM tm_analysis_run WHERE symbol = #{symbol} "
            + "ORDER BY COALESCE(completed_at, started_at, created_at) DESC, analysis_id DESC LIMIT 1")
    AnalysisRunDO selectLatestBySymbol(String symbol);

    @Select("SELECT * FROM tm_analysis_run WHERE idempotency_key = #{idempotencyKey}")
    AnalysisRunDO selectByIdempotencyKey(String idempotencyKey);

    @Update("UPDATE tm_analysis_run SET status = 'SUCCESS', data_quality_score = #{dataQualityScore}, completed_at = #{completedAt}, "
            + "lease_owner = NULL, lease_expires_at = NULL, updated_at = #{completedAt}, version_no = COALESCE(version_no, 1) + 1 "
            + "WHERE analysis_id = #{analysisId} AND status = 'STARTED' "
            + "AND lease_owner = #{leaseOwner} AND COALESCE(version_no, 1) = #{claimVersion}")
    int markSuccess(@Param("analysisId") String analysisId,
                    @Param("dataQualityScore") Integer dataQualityScore,
                    @Param("completedAt") LocalDateTime completedAt,
                    @Param("leaseOwner") String leaseOwner,
                    @Param("claimVersion") int claimVersion);

    @Update("UPDATE tm_analysis_run SET status = 'FAILED', error_code = #{errorCode}, error_message = #{errorMessage}, "
            + "completed_at = #{completedAt}, lease_owner = NULL, lease_expires_at = NULL, updated_at = #{completedAt}, "
            + "version_no = COALESCE(version_no, 1) + 1 WHERE analysis_id = #{analysisId} AND status = 'STARTED' "
            + "AND lease_owner = #{leaseOwner} AND COALESCE(version_no, 1) = #{claimVersion}")
    int markFailed(@Param("analysisId") String analysisId,
                   @Param("errorCode") String errorCode,
                   @Param("errorMessage") String errorMessage,
                   @Param("completedAt") LocalDateTime completedAt,
                   @Param("leaseOwner") String leaseOwner,
                   @Param("claimVersion") int claimVersion);

    @Update("UPDATE tm_analysis_run SET status = 'STARTED', request_id = #{requestId}, lease_owner = #{leaseOwner}, "
            + "lease_expires_at = #{leaseExpiresAt}, started_at = #{startedAt}, completed_at = NULL, error_code = NULL, error_message = NULL, "
            + "attempt_count = COALESCE(attempt_count, 1) + 1, updated_at = #{startedAt}, version_no = COALESCE(version_no, 1) + 1 "
            + "WHERE analysis_id = #{analysisId} AND status = 'FAILED' AND COALESCE(attempt_count, 1) < #{maxAttempts} "
            + "AND COALESCE(version_no, 1) = #{versionNo}")
    int recoverFailed(@Param("analysisId") String analysisId,
                      @Param("requestId") String requestId,
                      @Param("leaseOwner") String leaseOwner,
                      @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
                      @Param("startedAt") LocalDateTime startedAt,
                      @Param("versionNo") int versionNo,
                      @Param("maxAttempts") int maxAttempts);

    @Update("UPDATE tm_analysis_run SET status = 'STARTED', request_id = #{requestId}, lease_owner = #{leaseOwner}, "
            + "lease_expires_at = #{leaseExpiresAt}, started_at = #{startedAt}, completed_at = NULL, error_code = NULL, error_message = NULL, "
            + "attempt_count = COALESCE(attempt_count, 1) + 1, updated_at = #{startedAt}, version_no = COALESCE(version_no, 1) + 1 "
            + "WHERE analysis_id = #{analysisId} AND status = 'STARTED' AND lease_expires_at < #{startedAt} "
            + "AND COALESCE(attempt_count, 1) < #{maxAttempts} AND COALESCE(version_no, 1) = #{versionNo}")
    int recoverExpiredLease(@Param("analysisId") String analysisId,
                            @Param("requestId") String requestId,
                            @Param("leaseOwner") String leaseOwner,
                            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
                            @Param("startedAt") LocalDateTime startedAt,
                            @Param("versionNo") int versionNo,
                            @Param("maxAttempts") int maxAttempts);

    @Select("SELECT ((SELECT COUNT(*) FROM tm_evidence_item WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_score_item WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_decision_result WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_execution_plan WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_market_environment_snapshot WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_push_snapshot WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_account_risk_snapshot WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_monitor_alert WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_opportunity_log WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_missed_opportunity WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_position_monitor_log WHERE analysis_id = #{analysisId}) "
            + "+ (SELECT COUNT(*) FROM tm_ai_call_log WHERE analysis_id = #{analysisId}))")
    Integer countPartialStateRows(String analysisId);

    @Select("SELECT evidence_id FROM tm_evidence_item WHERE analysis_id = #{analysisId} ORDER BY create_time, evidence_id")
    List<String> selectEvidenceIdsByAnalysisId(String analysisId);

    @Select("SELECT score_id FROM tm_score_item WHERE analysis_id = #{analysisId} ORDER BY score_id")
    List<String> selectScoreIdsByAnalysisId(String analysisId);

    @Select("SELECT decision_id FROM tm_decision_result WHERE analysis_id = #{analysisId} ORDER BY create_time, decision_id")
    List<String> selectDecisionIdsByAnalysisId(String analysisId);

    @Select("SELECT plan_id FROM tm_execution_plan WHERE analysis_id = #{analysisId} ORDER BY create_time, plan_id")
    List<String> selectExecutionPlanIdsByAnalysisId(String analysisId);

    @Select("SELECT CAST(log_id AS VARCHAR) FROM tm_position_monitor_log WHERE analysis_id = #{analysisId} ORDER BY created_at, log_id")
    List<String> selectPositionMonitorLogIdsByAnalysisId(String analysisId);

    @Select("SELECT id FROM tm_review_result WHERE analysis_id = #{analysisId} "
            + "AND review_scope_key = 'SHARED' AND user_id IS NULL AND user_position_id IS NULL "
            + "ORDER BY create_time, id")
    List<String> selectReviewResultIdsByAnalysisId(String analysisId);

    @Select("SELECT call_id FROM tm_ai_call_log WHERE trace_id = #{traceId} OR analysis_id = #{analysisId} ORDER BY started_at, call_id")
    List<String> selectAiCallIdsByTraceOrAnalysisId(@Param("traceId") String traceId,
                                                    @Param("analysisId") String analysisId);

    @Select("SELECT opportunity_id FROM tm_opportunity_log WHERE analysis_id = #{analysisId} ORDER BY created_at, opportunity_id")
    List<String> selectOpportunityIdsByAnalysisId(String analysisId);

    @Select("SELECT COUNT(*) FROM tm_push_snapshot WHERE analysis_id = #{analysisId}")
    Integer countPushSnapshotsByAnalysisId(String analysisId);

    @Select("SELECT COUNT(DISTINCT symbol) FROM tm_analysis_run")
    Integer countDistinctSymbols();

    @Select("SELECT COUNT(*) FROM (SELECT ar.symbol, ar.status, ROW_NUMBER() OVER (PARTITION BY ar.symbol "
            + "ORDER BY COALESCE(ar.completed_at, ar.started_at, ar.created_at) DESC, ar.analysis_id DESC) AS rn "
            + "FROM tm_analysis_run ar WHERE EXISTS (SELECT 1 FROM tm_asset_pool_item ap "
            + "WHERE ap.active = TRUE AND UPPER(ap.symbol) = UPPER(ar.symbol))) latest "
            + "WHERE latest.rn = 1 AND latest.status = 'SUCCESS'")
    Integer countLocalRealSuccessfulSymbols();

    @Select("SELECT MAX(completed_at) FROM tm_analysis_run WHERE status = 'SUCCESS'")
    LocalDateTime selectLatestSuccessfulCompletedAt();

    @Select("SELECT COUNT(*) FROM tm_evidence_item WHERE analysis_id = #{analysisId}")
    Integer countEvidenceByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT COUNT(*) FROM tm_score_item WHERE analysis_id = #{analysisId}")
    Integer countScoresByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT AVG(score_value) FROM tm_score_item WHERE analysis_id = #{analysisId}")
    Double selectAverageScoreByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT COUNT(*) FROM tm_analysis_run "
            + "WHERE analysis_time >= #{windowStartInclusive} "
            + "AND analysis_time <= #{asOfInclusive}")
    Integer countInWindow(@Param("windowStartInclusive") LocalDateTime windowStartInclusive,
                          @Param("asOfInclusive") LocalDateTime asOfInclusive);

    @Select("SELECT COUNT(*) FROM tm_analysis_run "
            + "WHERE analysis_time >= #{windowStartInclusive} "
            + "AND analysis_time <= #{asOfInclusive} "
            + "AND data_quality_score IS NOT NULL AND data_quality_score < #{threshold}")
    Integer countLowQualityInWindow(@Param("windowStartInclusive") LocalDateTime windowStartInclusive,
                                    @Param("asOfInclusive") LocalDateTime asOfInclusive,
                                    @Param("threshold") int threshold);
}
