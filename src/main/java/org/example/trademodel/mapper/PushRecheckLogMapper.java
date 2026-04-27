package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.TmPushRecheckLogDO;

import java.util.List;

@Mapper
public interface PushRecheckLogMapper {

    @Insert("INSERT INTO tm_push_recheck_log(push_id, dispatch_batch_id, dispatch_instruction_id, trigger_source, "
            + "retry_attempt, max_attempts, retry_backoff_minutes, replay_from_log_id, execution_status, execution_error_code, "
            + "execution_error_message, recheck_time, recheck_status, current_price, price_drift_ratio, "
            + "current_slippage_estimation, current_data_quality_score, current_confused_score, current_account_risk_allowed, "
            + "fail_reason_json, trace_id, create_time) "
            + "VALUES(#{pushId}, #{dispatchBatchId}, #{dispatchInstructionId}, #{triggerSource}, #{retryAttempt}, "
            + "#{maxAttempts}, #{retryBackoffMinutes}, #{replayFromLogId}, #{executionStatus}, #{executionErrorCode}, "
            + "#{executionErrorMessage}, #{recheckTime}, #{recheckStatus}, #{currentPrice}, #{priceDriftRatio}, "
            + "#{currentSlippageEstimation}, #{currentDataQualityScore}, #{currentConfusedScore}, #{currentAccountRiskAllowed}, "
            + "#{failReasonJson}, #{traceId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "logId", keyColumn = "log_id")
    int insert(TmPushRecheckLogDO row);

    @Select("SELECT * FROM tm_push_recheck_log WHERE push_id = #{pushId} ORDER BY log_id DESC")
    List<TmPushRecheckLogDO> selectByPushId(Long pushId);

    @Select("SELECT COUNT(*) FROM tm_push_recheck_log WHERE push_id = #{pushId}")
    Integer countByPushId(@Param("pushId") Long pushId);

    @Select("SELECT * FROM tm_push_recheck_log WHERE dispatch_instruction_id = #{instructionId} ORDER BY log_id DESC")
    List<TmPushRecheckLogDO> selectByInstructionId(@Param("instructionId") String instructionId);

    @Select("SELECT * FROM tm_push_recheck_log WHERE dispatch_batch_id = #{batchId} ORDER BY log_id DESC")
    List<TmPushRecheckLogDO> selectByBatchId(@Param("batchId") String batchId);

    @Select("SELECT * FROM tm_push_recheck_log ORDER BY log_id DESC LIMIT #{limit}")
    List<TmPushRecheckLogDO> selectRecent(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tm_push_recheck_log "
            + "WHERE UPPER(TRIM(COALESCE(recheck_status, ''))) = UPPER(TRIM(#{status})) "
            + "AND create_time >= DATEADD('MINUTE', -#{windowMinutes}, CURRENT_TIMESTAMP)")
    Integer countByStatusInWindow(@Param("status") String status, @Param("windowMinutes") int windowMinutes);
}
