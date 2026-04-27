package org.example.trademodel.entity;

public class UserConfigDO {
    private String userId;
    private String riskPreference;
    private String aiModelPreference;
    private String notifyChannels;
    private Integer cooldownMinutes;

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
}
