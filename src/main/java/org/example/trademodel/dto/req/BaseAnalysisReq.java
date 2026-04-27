package org.example.trademodel.dto.req;

import jakarta.validation.constraints.NotBlank;

public class BaseAnalysisReq {

    @NotBlank(message = "assetSymbol cannot be blank")
    private String assetSymbol;
    @NotBlank(message = "timeframe cannot be blank")
    private String timeframe;
    private String analysisTime;
    private String triggerType;
    @NotBlank(message = "requestId cannot be blank")
    private String requestId;
    private String userId;

    public BaseAnalysisReq() {
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(String analysisTime) {
        this.analysisTime = analysisTime;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
