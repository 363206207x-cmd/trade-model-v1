package org.example.trademodel.vo;

import java.time.LocalDateTime;
import java.util.List;

public class PushRecheckOpsOverviewVO {

    private ConfigSummary config;
    private AuditSummary auditSummary;
    private PushRecheckReplaySummaryVO latestReplaySummary;
    private List<RecentLogSummary> recentLogs;

    public ConfigSummary getConfig() {
        return config;
    }

    public void setConfig(ConfigSummary config) {
        this.config = config;
    }

    public AuditSummary getAuditSummary() {
        return auditSummary;
    }

    public void setAuditSummary(AuditSummary auditSummary) {
        this.auditSummary = auditSummary;
    }

    public PushRecheckReplaySummaryVO getLatestReplaySummary() {
        return latestReplaySummary;
    }

    public void setLatestReplaySummary(PushRecheckReplaySummaryVO latestReplaySummary) {
        this.latestReplaySummary = latestReplaySummary;
    }

    public List<RecentLogSummary> getRecentLogs() {
        return recentLogs;
    }

    public void setRecentLogs(List<RecentLogSummary> recentLogs) {
        this.recentLogs = recentLogs;
    }

    public static class ConfigSummary {
        private Integer limit;
        private Integer maxAttempts;
        private Integer minRetryMinutes;
        private String updatedBy;
        private LocalDateTime updatedTime;

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Integer getMinRetryMinutes() {
            return minRetryMinutes;
        }

        public void setMinRetryMinutes(Integer minRetryMinutes) {
            this.minRetryMinutes = minRetryMinutes;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }

        public LocalDateTime getUpdatedTime() {
            return updatedTime;
        }

        public void setUpdatedTime(LocalDateTime updatedTime) {
            this.updatedTime = updatedTime;
        }
    }

    public static class AuditSummary {
        private Integer auditCount;
        private LocalDateTime latestAuditTime;
        private String latestAuditOperator;
        private String latestAuditSummary;

        public Integer getAuditCount() {
            return auditCount;
        }

        public void setAuditCount(Integer auditCount) {
            this.auditCount = auditCount;
        }

        public LocalDateTime getLatestAuditTime() {
            return latestAuditTime;
        }

        public void setLatestAuditTime(LocalDateTime latestAuditTime) {
            this.latestAuditTime = latestAuditTime;
        }

        public String getLatestAuditOperator() {
            return latestAuditOperator;
        }

        public void setLatestAuditOperator(String latestAuditOperator) {
            this.latestAuditOperator = latestAuditOperator;
        }

        public String getLatestAuditSummary() {
            return latestAuditSummary;
        }

        public void setLatestAuditSummary(String latestAuditSummary) {
            this.latestAuditSummary = latestAuditSummary;
        }
    }

    public static class RecentLogSummary {
        private Long logId;
        private String dispatchBatchId;
        private String dispatchInstructionId;
        private String triggerSource;
        private String executionStatus;
        private String executionErrorCode;
        private LocalDateTime createTime;

        public Long getLogId() {
            return logId;
        }

        public void setLogId(Long logId) {
            this.logId = logId;
        }

        public String getDispatchBatchId() {
            return dispatchBatchId;
        }

        public void setDispatchBatchId(String dispatchBatchId) {
            this.dispatchBatchId = dispatchBatchId;
        }

        public String getDispatchInstructionId() {
            return dispatchInstructionId;
        }

        public void setDispatchInstructionId(String dispatchInstructionId) {
            this.dispatchInstructionId = dispatchInstructionId;
        }

        public String getTriggerSource() {
            return triggerSource;
        }

        public void setTriggerSource(String triggerSource) {
            this.triggerSource = triggerSource;
        }

        public String getExecutionStatus() {
            return executionStatus;
        }

        public void setExecutionStatus(String executionStatus) {
            this.executionStatus = executionStatus;
        }

        public String getExecutionErrorCode() {
            return executionErrorCode;
        }

        public void setExecutionErrorCode(String executionErrorCode) {
            this.executionErrorCode = executionErrorCode;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }
}
