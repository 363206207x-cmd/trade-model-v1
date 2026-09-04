package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.TelegramChannelTestAuditDO;

import java.time.LocalDateTime;

@Mapper
public interface TelegramChannelTestAuditMapper {
    @Insert("INSERT INTO tm_telegram_channel_test_audit(test_id, user_id, idempotency_key, status, "
            + "provider_reference, bot_username, recipient_fingerprint, response_code, error_code, error_message, "
            + "requested_at, attempted_at, completed_at, not_trade_instruction, not_order_execution) VALUES("
            + "#{testId}, #{userId}, #{idempotencyKey}, #{status}, #{providerReference}, #{botUsername}, "
            + "#{recipientFingerprint}, #{responseCode}, #{errorCode}, #{errorMessage}, #{requestedAt}, "
            + "#{attemptedAt}, #{completedAt}, #{notTradeInstruction}, #{notOrderExecution})")
    int insert(TelegramChannelTestAuditDO row);

    @Select("SELECT * FROM tm_telegram_channel_test_audit WHERE user_id = #{userId} "
            + "AND idempotency_key = #{idempotencyKey} LIMIT 1")
    TelegramChannelTestAuditDO selectByIdempotencyKey(@Param("userId") Long userId,
                                                       @Param("idempotencyKey") String idempotencyKey);

    @Select("SELECT * FROM tm_telegram_channel_test_audit WHERE user_id = #{userId} "
            + "ORDER BY requested_at DESC, test_id DESC LIMIT 1")
    TelegramChannelTestAuditDO selectLatestForUser(@Param("userId") Long userId);

    @Select("SELECT * FROM tm_telegram_channel_test_audit WHERE user_id = #{userId} "
            + "AND attempted_at IS NOT NULL AND attempted_at >= #{cutoff} "
            + "ORDER BY attempted_at DESC, test_id DESC LIMIT 1")
    TelegramChannelTestAuditDO selectRecentAttempt(@Param("userId") Long userId,
                                                    @Param("cutoff") LocalDateTime cutoff);

    @Update("UPDATE tm_telegram_channel_test_audit SET status = #{status}, "
            + "provider_reference = #{providerReference}, bot_username = #{botUsername}, "
            + "recipient_fingerprint = #{recipientFingerprint}, response_code = #{responseCode}, "
            + "error_code = #{errorCode}, error_message = #{errorMessage}, attempted_at = #{attemptedAt}, "
            + "completed_at = #{completedAt} WHERE test_id = #{testId} AND user_id = #{userId}")
    int updateOutcome(TelegramChannelTestAuditDO row);
}
