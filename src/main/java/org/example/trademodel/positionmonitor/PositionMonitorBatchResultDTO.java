package org.example.trademodel.positionmonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PositionMonitorBatchResultDTO {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private int blockedCount;
    private List<PositionMonitorResultDTO> results = new ArrayList<>();
    private List<FailureItem> failures = new ArrayList<>();
    private boolean reviewOnly = true;
    private boolean manualReviewOnly = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoReduce = true;
    private boolean notAutoClose = true;
    private boolean notAutoReverse = true;
    private boolean notOrderExecution = true;
    private boolean notAutoTrading = true;
    private boolean notPositionMutation = true;

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }
    public List<PositionMonitorResultDTO> getResults() { return Collections.unmodifiableList(results); }
    public void setResults(List<PositionMonitorResultDTO> results) { this.results = results == null ? new ArrayList<>() : new ArrayList<>(results); }
    public List<FailureItem> getFailures() { return Collections.unmodifiableList(failures); }
    public void setFailures(List<FailureItem> failures) { this.failures = failures == null ? new ArrayList<>() : new ArrayList<>(failures); }
    public boolean isReviewOnly() { return reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public boolean isNotAutoReduce() { return notAutoReduce; }
    public boolean isNotAutoClose() { return notAutoClose; }
    public boolean isNotAutoReverse() { return notAutoReverse; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public boolean isNotPositionMutation() { return notPositionMutation; }

    public static class FailureItem {
        private Long positionId;
        private String assetSymbol;
        private String reason;

        public FailureItem() {
        }

        public FailureItem(Long positionId, String assetSymbol, String reason) {
            this.positionId = positionId;
            this.assetSymbol = assetSymbol;
            this.reason = reason;
        }

        public Long getPositionId() { return positionId; }
        public void setPositionId(Long positionId) { this.positionId = positionId; }
        public String getAssetSymbol() { return assetSymbol; }
        public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
