package org.example.trademodel.mapper;

import org.example.trademodel.entity.UserConfigDO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserConfigMapper {

    @Select("SELECT * FROM tm_user_config WHERE user_id = #{userId}")
    UserConfigDO findByUserId(String userId);

    @Insert("INSERT INTO tm_user_config(user_id, risk_preference, ai_model_preference, notify_channels, cooldown_minutes, " +
            "scan_base_profile, scan_position_profile, scan_pool_profile, scan_auto_escalation_enabled, " +
            "scan_manual_override_until, scan_update_reason, scan_updated_at, telegram_chat_id, telegram_binding_status, " +
            "notification_filters_json, default_pool_mode) " +
            "VALUES(#{userId}, #{riskPreference}, #{aiModelPreference}, #{notifyChannels}, #{cooldownMinutes}, " +
            "#{scanBaseProfile}, #{scanPositionProfile}, #{scanPoolProfile}, #{scanAutoEscalationEnabled}, " +
            "#{scanManualOverrideUntil}, #{scanUpdateReason}, #{scanUpdatedAt}, #{telegramChatId}, #{telegramBindingStatus}, " +
            "#{notificationFiltersJson}, #{defaultPoolMode}) " +
            "ON DUPLICATE KEY UPDATE risk_preference = #{riskPreference}, ai_model_preference = #{aiModelPreference}, " +
            "notify_channels = #{notifyChannels}, cooldown_minutes = #{cooldownMinutes}, " +
            "scan_base_profile = #{scanBaseProfile}, scan_position_profile = #{scanPositionProfile}, " +
            "scan_pool_profile = #{scanPoolProfile}, scan_auto_escalation_enabled = #{scanAutoEscalationEnabled}, " +
            "scan_manual_override_until = #{scanManualOverrideUntil}, scan_update_reason = #{scanUpdateReason}, " +
            "scan_updated_at = #{scanUpdatedAt}, telegram_chat_id = #{telegramChatId}, " +
            "telegram_binding_status = #{telegramBindingStatus}, notification_filters_json = #{notificationFiltersJson}, " +
            "default_pool_mode = #{defaultPoolMode}")
    @Insert(value = "INSERT INTO tm_user_config(user_id, risk_preference, ai_model_preference, notify_channels, cooldown_minutes, " +
            "scan_base_profile, scan_position_profile, scan_pool_profile, scan_auto_escalation_enabled, " +
            "scan_manual_override_until, scan_update_reason, scan_updated_at, telegram_chat_id, telegram_binding_status, " +
            "notification_filters_json, default_pool_mode) " +
            "VALUES(#{userId}, #{riskPreference}, #{aiModelPreference}, #{notifyChannels}, #{cooldownMinutes}, " +
            "#{scanBaseProfile}, #{scanPositionProfile}, #{scanPoolProfile}, #{scanAutoEscalationEnabled}, " +
            "#{scanManualOverrideUntil}, #{scanUpdateReason}, #{scanUpdatedAt}, #{telegramChatId}, #{telegramBindingStatus}, " +
            "#{notificationFiltersJson}, #{defaultPoolMode}) " +
            "ON CONFLICT (user_id) DO UPDATE SET risk_preference = EXCLUDED.risk_preference, " +
            "ai_model_preference = EXCLUDED.ai_model_preference, notify_channels = EXCLUDED.notify_channels, " +
            "cooldown_minutes = EXCLUDED.cooldown_minutes, scan_base_profile = EXCLUDED.scan_base_profile, " +
            "scan_position_profile = EXCLUDED.scan_position_profile, scan_pool_profile = EXCLUDED.scan_pool_profile, " +
            "scan_auto_escalation_enabled = EXCLUDED.scan_auto_escalation_enabled, " +
            "scan_manual_override_until = EXCLUDED.scan_manual_override_until, " +
            "scan_update_reason = EXCLUDED.scan_update_reason, scan_updated_at = EXCLUDED.scan_updated_at, " +
            "telegram_chat_id = EXCLUDED.telegram_chat_id, telegram_binding_status = EXCLUDED.telegram_binding_status, " +
            "notification_filters_json = EXCLUDED.notification_filters_json, default_pool_mode = EXCLUDED.default_pool_mode",
            databaseId = "postgresql")
    int saveOrUpdate(UserConfigDO userConfig);
}
