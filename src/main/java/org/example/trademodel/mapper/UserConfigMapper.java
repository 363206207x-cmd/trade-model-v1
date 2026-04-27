package org.example.trademodel.mapper;

import org.example.trademodel.entity.UserConfigDO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserConfigMapper {

    @Select("SELECT * FROM tm_user_config WHERE user_id = #{userId}")
    UserConfigDO findByUserId(String userId);

    @Insert("INSERT INTO tm_user_config(user_id, risk_preference, ai_model_preference, notify_channels, cooldown_minutes) " +
            "VALUES(#{userId}, #{riskPreference}, #{aiModelPreference}, #{notifyChannels}, #{cooldownMinutes}) " +
            "ON DUPLICATE KEY UPDATE risk_preference = #{riskPreference}, ai_model_preference = #{aiModelPreference}, " +
            "notify_channels = #{notifyChannels}, cooldown_minutes = #{cooldownMinutes}")
    int saveOrUpdate(UserConfigDO userConfig);
}
