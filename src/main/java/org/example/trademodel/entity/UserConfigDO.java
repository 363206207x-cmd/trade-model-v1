package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class UserConfigDO {
    private String userId;
    private String riskPreference;
    private String aiModelPreference;
    private String notifyChannels;
    private Integer cooldownMinutes;
    private String scanBaseProfile;
    private String scanPositionProfile;
    private String scanPoolProfile;
    private Boolean scanAutoEscalationEnabled;
    private LocalDateTime scanManualOverrideUntil;
    private String scanUpdateReason;
    private LocalDateTime scanUpdatedAt;
    private String telegramChatId;
    private String telegramBindingStatus = "UNBOUND";
    private String notificationFiltersJson;
    private String defaultPoolMode = "SYSTEM_DEFAULT";

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRiskPreference() { return riskPreference; }
    public void setRiskPreference(String riskPreference) { this.riskPreference = riskPreference; }
    public String getAiModelPreference() { return aiModelPreference; }
    public void setAiModelPreference(String aiModelPreference) { this.aiModelPreference = aiModelPreference; }
    public String getNotifyChannels() { return notifyChannels; }
    public void setNotifyChannels(String notifyChannels) { this.notifyChannels = notifyChannels; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public String getScanBaseProfile() { return scanBaseProfile; }
    public void setScanBaseProfile(String scanBaseProfile) { this.scanBaseProfile = scanBaseProfile; }
    public String getScanPositionProfile() { return scanPositionProfile; }
    public void setScanPositionProfile(String scanPositionProfile) { this.scanPositionProfile = scanPositionProfile; }
    public String getScanPoolProfile() { return scanPoolProfile; }
    public void setScanPoolProfile(String scanPoolProfile) { this.scanPoolProfile = scanPoolProfile; }
    public Boolean getScanAutoEscalationEnabled() { return scanAutoEscalationEnabled; }
    public void setScanAutoEscalationEnabled(Boolean scanAutoEscalationEnabled) { this.scanAutoEscalationEnabled = scanAutoEscalationEnabled; }
    public LocalDateTime getScanManualOverrideUntil() { return scanManualOverrideUntil; }
    public void setScanManualOverrideUntil(LocalDateTime scanManualOverrideUntil) { this.scanManualOverrideUntil = scanManualOverrideUntil; }
    public String getScanUpdateReason() { return scanUpdateReason; }
    public void setScanUpdateReason(String scanUpdateReason) { this.scanUpdateReason = scanUpdateReason; }
    public LocalDateTime getScanUpdatedAt() { return scanUpdatedAt; }
    public void setScanUpdatedAt(LocalDateTime scanUpdatedAt) { this.scanUpdatedAt = scanUpdatedAt; }
    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String value) { this.telegramChatId = value; }
    public String getTelegramBindingStatus() { return telegramBindingStatus; }
    public void setTelegramBindingStatus(String value) { this.telegramBindingStatus = value; }
    public String getNotificationFiltersJson() { return notificationFiltersJson; }
    public void setNotificationFiltersJson(String value) { this.notificationFiltersJson = value; }
    public String getDefaultPoolMode() { return defaultPoolMode; }
    public void setDefaultPoolMode(String value) { this.defaultPoolMode = value; }
}
