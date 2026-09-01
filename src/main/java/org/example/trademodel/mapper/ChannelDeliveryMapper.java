package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.ChannelDeliveryDO;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface ChannelDeliveryMapper {
    @Insert("INSERT INTO tm_channel_delivery(delivery_id, message_id, user_id, channel, status, provider_reference, "
            + "attempt_count, next_attempt_at, claimed_at, lease_until, claim_token, last_response_code, "
            + "retry_after_seconds, recipient_fingerprint, cooldown_key, severity_rank, error_code, error_message, "
            + "attempted_at, delivered_at, created_at, updated_at) "
            + "VALUES(#{deliveryId}, #{messageId}, #{userId}, #{channel}, #{status}, #{providerReference}, "
            + "#{attemptCount}, #{nextAttemptAt}, #{claimedAt}, #{leaseUntil}, #{claimToken}, #{lastResponseCode}, "
            + "#{retryAfterSeconds}, #{recipientFingerprint}, #{cooldownKey}, #{severityRank}, #{errorCode}, "
            + "#{errorMessage}, #{attemptedAt}, #{deliveredAt}, #{createdAt}, #{updatedAt})")
    int insert(ChannelDeliveryDO row);

    @Select("SELECT * FROM tm_channel_delivery WHERE delivery_id = #{deliveryId}")
    ChannelDeliveryDO selectById(@Param("deliveryId") String deliveryId);

    @Select("SELECT * FROM tm_channel_delivery WHERE message_id = #{messageId} AND channel = #{channel} "
            + "ORDER BY created_at ASC, delivery_id ASC LIMIT 1")
    ChannelDeliveryDO selectByMessageAndChannel(@Param("messageId") String messageId,
                                                @Param("channel") String channel);

    @Select("SELECT * FROM tm_channel_delivery WHERE message_id = #{messageId} AND user_id = #{userId} "
            + "ORDER BY created_at DESC, delivery_id DESC")
    List<ChannelDeliveryDO> listByMessageForUser(@Param("messageId") String messageId,
                                                 @Param("userId") Long userId);

    @Select("SELECT * FROM tm_channel_delivery WHERE user_id = #{userId} AND channel = 'TELEGRAM' "
            + "ORDER BY created_at DESC, delivery_id DESC LIMIT 1")
    ChannelDeliveryDO selectLatestTelegramForUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM tm_channel_delivery WHERE user_id = #{userId} AND channel = 'TELEGRAM' "
            + "AND status IN ('QUEUED', 'SENDING', 'RETRYING')")
    int countRetryingForUser(@Param("userId") Long userId);

    @Select("SELECT * FROM tm_channel_delivery WHERE user_id = #{userId} AND channel = 'TELEGRAM' "
            + "AND cooldown_key = #{cooldownKey} AND created_at >= #{cutoff} "
            + "AND status IN ('QUEUED', 'SENDING', 'SENT', 'RETRYING') "
            + "ORDER BY severity_rank DESC, created_at DESC LIMIT 1")
    ChannelDeliveryDO selectRecentActiveCooldown(@Param("userId") Long userId,
                                                  @Param("cooldownKey") String cooldownKey,
                                                  @Param("cutoff") LocalDateTime cutoff);

    @Select("SELECT * FROM tm_channel_delivery WHERE user_id = #{userId} AND channel = 'TELEGRAM' "
            + "AND cooldown_key = #{cooldownKey} "
            + "ORDER BY created_at ASC, delivery_id ASC LIMIT 1")
    ChannelDeliveryDO selectExistingLifetimeDelivery(@Param("userId") Long userId,
                                                      @Param("cooldownKey") String cooldownKey);

    @Select("SELECT * FROM tm_channel_delivery WHERE channel = 'TELEGRAM' "
            + "AND status IN ('QUEUED', 'RETRYING') "
            + "AND (next_attempt_at IS NULL OR next_attempt_at <= #{now}) "
            + "ORDER BY COALESCE(next_attempt_at, created_at), created_at, delivery_id LIMIT #{limit}")
    List<ChannelDeliveryDO> listDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("UPDATE tm_channel_delivery SET status = 'SENDING', claim_token = #{claimToken}, "
            + "claimed_at = #{now}, lease_until = #{leaseUntil}, attempted_at = #{now}, "
            + "attempt_count = attempt_count + 1, updated_at = #{now} "
            + "WHERE delivery_id = #{deliveryId} AND status IN ('QUEUED', 'RETRYING') "
            + "AND (next_attempt_at IS NULL OR next_attempt_at <= #{now})")
    int claim(@Param("deliveryId") String deliveryId,
              @Param("claimToken") String claimToken,
              @Param("now") LocalDateTime now,
              @Param("leaseUntil") LocalDateTime leaseUntil);

    @Update("UPDATE tm_channel_delivery SET lease_until = #{leaseUntil}, updated_at = #{now} "
            + "WHERE delivery_id = #{deliveryId} AND status = 'SENDING' "
            + "AND claim_token = #{claimToken} AND (lease_until IS NULL OR lease_until >= #{now})")
    int extendClaim(@Param("deliveryId") String deliveryId,
                    @Param("claimToken") String claimToken,
                    @Param("now") LocalDateTime now,
                    @Param("leaseUntil") LocalDateTime leaseUntil);

    @Update("UPDATE tm_channel_delivery SET status = #{status}, provider_reference = #{providerReference}, "
            + "next_attempt_at = #{nextAttemptAt}, last_response_code = #{lastResponseCode}, "
            + "retry_after_seconds = #{retryAfterSeconds}, error_code = #{errorCode}, "
            + "error_message = #{errorMessage}, delivered_at = #{deliveredAt}, "
            + "claim_token = NULL, claimed_at = NULL, lease_until = NULL, updated_at = #{updatedAt} "
            + "WHERE delivery_id = #{deliveryId} AND status = 'SENDING' AND claim_token = #{claimToken}")
    int completeClaim(ChannelDeliveryDO row);

    @Update("UPDATE tm_channel_delivery SET status = 'SENT', provider_reference = #{providerReference}, "
            + "next_attempt_at = NULL, last_response_code = #{lastResponseCode}, retry_after_seconds = NULL, "
            + "error_code = NULL, error_message = NULL, delivered_at = #{deliveredAt}, "
            + "claim_token = NULL, claimed_at = NULL, lease_until = NULL, updated_at = #{updatedAt} "
            + "WHERE delivery_id = #{deliveryId} AND status <> 'SENT' "
            + "AND NOT EXISTS (SELECT 1 FROM tm_channel_delivery sent "
            + "WHERE sent.message_id = #{messageId} AND sent.channel = #{channel} "
            + "AND sent.delivery_id <> #{deliveryId} AND sent.status = 'SENT' "
            + "AND (sent.error_code IS NULL OR sent.error_code <> 'DUPLICATE_MIGRATED'))")
    int finalizeProviderSuccess(ChannelDeliveryDO row);

    @Update("UPDATE tm_channel_delivery SET status = 'FAILED', next_attempt_at = NULL, "
            + "claim_token = NULL, claimed_at = NULL, lease_until = NULL, "
            + "error_code = #{errorCode}, error_message = #{errorMessage}, updated_at = #{now} "
            + "WHERE delivery_id = #{deliveryId} AND status <> 'SENT'")
    int failClosedOutcome(@Param("deliveryId") String deliveryId,
                          @Param("errorCode") String errorCode,
                          @Param("errorMessage") String errorMessage,
                          @Param("now") LocalDateTime now);

    @Update("UPDATE tm_channel_delivery SET status = 'FAILED', next_attempt_at = NULL, "
            + "claim_token = NULL, claimed_at = NULL, lease_until = NULL, "
            + "error_code = 'DELIVERY_OUTCOME_UNKNOWN', "
            + "error_message = 'Delivery claim expired with unknown provider outcome; manual retry required', "
            + "updated_at = #{now} WHERE channel = 'TELEGRAM' AND status = 'SENDING' "
            + "AND lease_until IS NOT NULL AND lease_until < #{now}")
    int recoverExpiredClaims(@Param("now") LocalDateTime now);

    @Update("UPDATE tm_channel_delivery SET status = 'QUEUED', next_attempt_at = #{now}, "
            + "error_code = NULL, error_message = NULL, updated_at = #{now} "
            + "WHERE delivery_id = #{deliveryId} AND user_id = #{userId} "
            + "AND status IN ('NOT_CONFIGURED', 'FAILED')")
    int requeue(@Param("deliveryId") String deliveryId,
                @Param("userId") Long userId,
                @Param("now") LocalDateTime now);

    @Update("UPDATE tm_channel_delivery SET status = #{status}, provider_reference = #{providerReference}, "
            + "attempt_count = #{attemptCount}, error_code = #{errorCode}, error_message = #{errorMessage}, "
            + "next_attempt_at = #{nextAttemptAt}, last_response_code = #{lastResponseCode}, "
            + "retry_after_seconds = #{retryAfterSeconds}, attempted_at = #{attemptedAt}, "
            + "delivered_at = #{deliveredAt}, updated_at = #{updatedAt} "
            + "WHERE delivery_id = #{deliveryId} AND user_id = #{userId}")
    int updateStatus(ChannelDeliveryDO row);
}
