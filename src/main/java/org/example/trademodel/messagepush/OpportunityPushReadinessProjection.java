package org.example.trademodel.messagepush;

import java.time.LocalDateTime;

public class OpportunityPushReadinessProjection {
    private Long pushId;
    private String analysisId;
    private String pushStatus;
    private LocalDateTime pushCreateTime;
    private LocalDateTime createTime;

    public Long getPushId() {
        return pushId;
    }

    public void setPushId(Long pushId) {
        this.pushId = pushId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getPushStatus() {
        return pushStatus;
    }

    public void setPushStatus(String pushStatus) {
        this.pushStatus = pushStatus;
    }

    public LocalDateTime getPushCreateTime() {
        return pushCreateTime;
    }

    public void setPushCreateTime(LocalDateTime pushCreateTime) {
        this.pushCreateTime = pushCreateTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
