package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.ChannelDeliveryDO;

import java.util.List;

@Mapper
public interface ChannelDeliveryMapper {
    @Insert("INSERT INTO tm_channel_delivery(delivery_id, message_id, user_id, channel, status, provider_reference, "
            + "attempt_count, error_code, error_message, attempted_at, delivered_at, created_at, updated_at) "
            + "VALUES(#{deliveryId}, #{messageId}, #{userId}, #{channel}, #{status}, #{providerReference}, "
            + "#{attemptCount}, #{errorCode}, #{errorMessage}, #{attemptedAt}, #{deliveredAt}, #{createdAt}, #{updatedAt})")
    int insert(ChannelDeliveryDO row);

    @Select("SELECT * FROM tm_channel_delivery WHERE message_id = #{messageId} AND user_id = #{userId} "
            + "ORDER BY created_at DESC, delivery_id DESC")
    List<ChannelDeliveryDO> listByMessageForUser(@Param("messageId") String messageId,
                                                 @Param("userId") Long userId);

    @Update("UPDATE tm_channel_delivery SET status = #{status}, provider_reference = #{providerReference}, "
            + "attempt_count = #{attemptCount}, error_code = #{errorCode}, error_message = #{errorMessage}, "
            + "attempted_at = #{attemptedAt}, delivered_at = #{deliveredAt}, updated_at = #{updatedAt} "
            + "WHERE delivery_id = #{deliveryId} AND user_id = #{userId}")
    int updateStatus(ChannelDeliveryDO row);
}
