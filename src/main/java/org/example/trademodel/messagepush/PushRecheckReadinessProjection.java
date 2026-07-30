package org.example.trademodel.messagepush;

import java.time.LocalDateTime;

public class PushRecheckReadinessProjection {
    private Long logId;
    private String recheckStatus;
    private LocalDateTime recheckTime;
    private String executionStatus;
    private String failReasonJson;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getRecheckStatus() {
        return recheckStatus;
    }

    public void setRecheckStatus(String recheckStatus) {
        this.recheckStatus = recheckStatus;
    }

    public LocalDateTime getRecheckTime() {
        return recheckTime;
    }

    public void setRecheckTime(LocalDateTime recheckTime) {
        this.recheckTime = recheckTime;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getFailReasonJson() {
        return failReasonJson;
    }

    public void setFailReasonJson(String failReasonJson) {
        this.failReasonJson = failReasonJson;
    }
}
