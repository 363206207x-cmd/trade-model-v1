package org.example.trademodel.vo;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardDetailResponseVO {
    private String symbol;
    private DecisionResultVO decision;
    private MarketEnvironmentMiniVO marketEnvironmentMini;
    private List<EvidenceBriefVO> evidenceTopItems;
    private List<ScoreBriefVO> scoreTopItems;
    private PlanBoundaryDisplayVO planBoundaryDisplay;
    private ExecutionPlanDisplayVO executionPlanDisplay;
    private RiskActionGuardDisplayVO riskActionGuardDisplay;
    private PaperObservationDisplayVO paperObservationDisplay;
    private SourceTraceDTO sourceTrace;
    private DerivativesRiskContextDTO derivativesRiskContext;

    /**
     * Creates a dashboard detail response with safe read-only display defaults.
     * The defaults are fail-closed and do not represent executable trading signals.
     */
    public static DashboardDetailResponseVO withSafeDefaultDisplays() {
        DashboardDetailResponseVO response = new DashboardDetailResponseVO();
        response.ensureSafeDefaultDisplays();
        return response;
    }

    /**
     * Initializes missing display models with safe BACKEND_PENDING defaults without
     * overwriting display models that were already populated by a caller.
     */
    public void ensureSafeDefaultDisplays() {
        if (planBoundaryDisplay == null) {
            planBoundaryDisplay = new PlanBoundaryDisplayVO();
        }
        if (executionPlanDisplay == null) {
            executionPlanDisplay = new ExecutionPlanDisplayVO();
        }
        if (riskActionGuardDisplay == null) {
            riskActionGuardDisplay = new RiskActionGuardDisplayVO();
        }
        if (paperObservationDisplay == null) {
            paperObservationDisplay = new PaperObservationDisplayVO();
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public DecisionResultVO getDecision() {
        return decision;
    }

    public void setDecision(DecisionResultVO decision) {
        this.decision = decision;
    }

    public MarketEnvironmentMiniVO getMarketEnvironmentMini() {
        return marketEnvironmentMini;
    }

    public void setMarketEnvironmentMini(MarketEnvironmentMiniVO marketEnvironmentMini) {
        this.marketEnvironmentMini = marketEnvironmentMini;
    }

    public List<EvidenceBriefVO> getEvidenceTopItems() {
        return evidenceTopItems;
    }

    public void setEvidenceTopItems(List<EvidenceBriefVO> evidenceTopItems) {
        this.evidenceTopItems = evidenceTopItems;
    }

    public List<ScoreBriefVO> getScoreTopItems() {
        return scoreTopItems;
    }

    public void setScoreTopItems(List<ScoreBriefVO> scoreTopItems) {
        this.scoreTopItems = scoreTopItems;
    }

    public PlanBoundaryDisplayVO getPlanBoundaryDisplay() {
        return planBoundaryDisplay;
    }

    public void setPlanBoundaryDisplay(PlanBoundaryDisplayVO planBoundaryDisplay) {
        this.planBoundaryDisplay = planBoundaryDisplay;
    }

    public ExecutionPlanDisplayVO getExecutionPlanDisplay() {
        return executionPlanDisplay;
    }

    public void setExecutionPlanDisplay(ExecutionPlanDisplayVO executionPlanDisplay) {
        this.executionPlanDisplay = executionPlanDisplay;
    }

    public RiskActionGuardDisplayVO getRiskActionGuardDisplay() {
        return riskActionGuardDisplay;
    }

    public void setRiskActionGuardDisplay(RiskActionGuardDisplayVO riskActionGuardDisplay) {
        this.riskActionGuardDisplay = riskActionGuardDisplay;
    }

    public PaperObservationDisplayVO getPaperObservationDisplay() {
        return paperObservationDisplay;
    }

    public void setPaperObservationDisplay(PaperObservationDisplayVO paperObservationDisplay) {
        this.paperObservationDisplay = paperObservationDisplay;
    }

    public SourceTraceDTO getSourceTrace() {
        return sourceTrace;
    }

    public void setSourceTrace(SourceTraceDTO sourceTrace) {
        this.sourceTrace = sourceTrace;
    }

    public DerivativesRiskContextDTO getDerivativesRiskContext() {
        return derivativesRiskContext;
    }

    public void setDerivativesRiskContext(DerivativesRiskContextDTO derivativesRiskContext) {
        this.derivativesRiskContext = derivativesRiskContext;
    }

    public static class MarketEnvironmentMiniVO {
        private String summary;
        private String environmentType;
        private String riskMode;
        private String sourceType;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getEnvironmentType() {
            return environmentType;
        }

        public void setEnvironmentType(String environmentType) {
            this.environmentType = environmentType;
        }

        public String getRiskMode() {
            return riskMode;
        }

        public void setRiskMode(String riskMode) {
            this.riskMode = riskMode;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }
    }

    public static class PlanBoundaryDisplayVO {
        private String planBoundaryStatus = "BACKEND_PENDING";
        private String planBoundaryStatusLabel = "后端未接入";
        private String sourceTraceStatus = "BACKEND_PENDING";
        private String backendConnectionStatus = "BACKEND_PENDING";
        private List<String> incompleteReasons = new ArrayList<>();
        private List<String> blockingReasons = new ArrayList<>();
        private Boolean manualReviewRequired = true;
        private Boolean notTradeInstruction = true;
        private LocalDateTime updatedAt;

        public String getPlanBoundaryStatus() {
            return planBoundaryStatus;
        }

        public void setPlanBoundaryStatus(String planBoundaryStatus) {
            this.planBoundaryStatus = planBoundaryStatus;
        }

        public String getPlanBoundaryStatusLabel() {
            return planBoundaryStatusLabel;
        }

        public void setPlanBoundaryStatusLabel(String planBoundaryStatusLabel) {
            this.planBoundaryStatusLabel = planBoundaryStatusLabel;
        }

        public String getSourceTraceStatus() {
            return sourceTraceStatus;
        }

        public void setSourceTraceStatus(String sourceTraceStatus) {
            this.sourceTraceStatus = sourceTraceStatus;
        }

        public String getBackendConnectionStatus() {
            return backendConnectionStatus;
        }

        public void setBackendConnectionStatus(String backendConnectionStatus) {
            this.backendConnectionStatus = backendConnectionStatus;
        }

        public List<String> getIncompleteReasons() {
            return incompleteReasons;
        }

        public void setIncompleteReasons(List<String> incompleteReasons) {
            this.incompleteReasons = incompleteReasons;
        }

        public List<String> getBlockingReasons() {
            return blockingReasons;
        }

        public void setBlockingReasons(List<String> blockingReasons) {
            this.blockingReasons = blockingReasons;
        }

        public Boolean getManualReviewRequired() {
            return manualReviewRequired;
        }

        public void setManualReviewRequired(Boolean manualReviewRequired) {
            this.manualReviewRequired = manualReviewRequired;
        }

        public Boolean getNotTradeInstruction() {
            return notTradeInstruction;
        }

        public void setNotTradeInstruction(Boolean notTradeInstruction) {
            this.notTradeInstruction = notTradeInstruction;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class ExecutionPlanDisplayVO {
        private String executionPlanStatus = "BOUNDARY_PENDING";
        private String executionPlanStatusLabel = "等待边界接入";
        private Boolean executionPlanBoundaryAligned = false;
        private String planBoundaryStatus = "BACKEND_PENDING";
        private String executionPlanSummary;
        private String notExecutableReason = "PLAN_BOUNDARY_BACKEND_PENDING";
        private List<String> incompleteReasons = new ArrayList<>();
        private Boolean manualReviewRequired = true;
        private Boolean notTradeInstruction = true;
        private LocalDateTime updatedAt;

        public String getExecutionPlanStatus() {
            return executionPlanStatus;
        }

        public void setExecutionPlanStatus(String executionPlanStatus) {
            this.executionPlanStatus = executionPlanStatus;
        }

        public String getExecutionPlanStatusLabel() {
            return executionPlanStatusLabel;
        }

        public void setExecutionPlanStatusLabel(String executionPlanStatusLabel) {
            this.executionPlanStatusLabel = executionPlanStatusLabel;
        }

        public Boolean getExecutionPlanBoundaryAligned() {
            return executionPlanBoundaryAligned;
        }

        public void setExecutionPlanBoundaryAligned(Boolean executionPlanBoundaryAligned) {
            this.executionPlanBoundaryAligned = executionPlanBoundaryAligned;
        }

        public String getPlanBoundaryStatus() {
            return planBoundaryStatus;
        }

        public void setPlanBoundaryStatus(String planBoundaryStatus) {
            this.planBoundaryStatus = planBoundaryStatus;
        }

        public String getExecutionPlanSummary() {
            return executionPlanSummary;
        }

        public void setExecutionPlanSummary(String executionPlanSummary) {
            this.executionPlanSummary = executionPlanSummary;
        }

        public String getNotExecutableReason() {
            return notExecutableReason;
        }

        public void setNotExecutableReason(String notExecutableReason) {
            this.notExecutableReason = notExecutableReason;
        }

        public List<String> getIncompleteReasons() {
            return incompleteReasons;
        }

        public void setIncompleteReasons(List<String> incompleteReasons) {
            this.incompleteReasons = incompleteReasons;
        }

        public Boolean getManualReviewRequired() {
            return manualReviewRequired;
        }

        public void setManualReviewRequired(Boolean manualReviewRequired) {
            this.manualReviewRequired = manualReviewRequired;
        }

        public Boolean getNotTradeInstruction() {
            return notTradeInstruction;
        }

        public void setNotTradeInstruction(Boolean notTradeInstruction) {
            this.notTradeInstruction = notTradeInstruction;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class RiskActionGuardDisplayVO {
        private String riskActionGuardStatus = "BACKEND_PENDING";
        private String riskActionGuardStatusLabel = "后端未接入";
        private String riskActionAdvice;
        private String riskActionBlockingReason;
        private String liquidityState = "BACKEND_PENDING";
        private Boolean stampedeDetected = false;
        private Boolean wickOnlyRisk = false;
        private Boolean opportunityPushAllowed = false;
        private Boolean reverseTradeAllowed = false;
        private Boolean newPositionAllowed = false;
        private Boolean marketOrderExitAllowed = false;
        private Boolean manualRiskReviewRequired = true;
        private Boolean notTradeInstruction = true;
        private LocalDateTime updatedAt;

        public String getRiskActionGuardStatus() {
            return riskActionGuardStatus;
        }

        public void setRiskActionGuardStatus(String riskActionGuardStatus) {
            this.riskActionGuardStatus = riskActionGuardStatus;
        }

        public String getRiskActionGuardStatusLabel() {
            return riskActionGuardStatusLabel;
        }

        public void setRiskActionGuardStatusLabel(String riskActionGuardStatusLabel) {
            this.riskActionGuardStatusLabel = riskActionGuardStatusLabel;
        }

        public String getRiskActionAdvice() {
            return riskActionAdvice;
        }

        public void setRiskActionAdvice(String riskActionAdvice) {
            this.riskActionAdvice = riskActionAdvice;
        }

        public String getRiskActionBlockingReason() {
            return riskActionBlockingReason;
        }

        public void setRiskActionBlockingReason(String riskActionBlockingReason) {
            this.riskActionBlockingReason = riskActionBlockingReason;
        }

        public String getLiquidityState() {
            return liquidityState;
        }

        public void setLiquidityState(String liquidityState) {
            this.liquidityState = liquidityState;
        }

        public Boolean getStampedeDetected() {
            return stampedeDetected;
        }

        public void setStampedeDetected(Boolean stampedeDetected) {
            this.stampedeDetected = stampedeDetected;
        }

        public Boolean getWickOnlyRisk() {
            return wickOnlyRisk;
        }

        public void setWickOnlyRisk(Boolean wickOnlyRisk) {
            this.wickOnlyRisk = wickOnlyRisk;
        }

        public Boolean getOpportunityPushAllowed() {
            return opportunityPushAllowed;
        }

        public void setOpportunityPushAllowed(Boolean opportunityPushAllowed) {
            this.opportunityPushAllowed = opportunityPushAllowed;
        }

        public Boolean getReverseTradeAllowed() {
            return reverseTradeAllowed;
        }

        public void setReverseTradeAllowed(Boolean reverseTradeAllowed) {
            this.reverseTradeAllowed = reverseTradeAllowed;
        }

        public Boolean getNewPositionAllowed() {
            return newPositionAllowed;
        }

        public void setNewPositionAllowed(Boolean newPositionAllowed) {
            this.newPositionAllowed = newPositionAllowed;
        }

        public Boolean getMarketOrderExitAllowed() {
            return marketOrderExitAllowed;
        }

        public void setMarketOrderExitAllowed(Boolean marketOrderExitAllowed) {
            this.marketOrderExitAllowed = marketOrderExitAllowed;
        }

        public Boolean getManualRiskReviewRequired() {
            return manualRiskReviewRequired;
        }

        public void setManualRiskReviewRequired(Boolean manualRiskReviewRequired) {
            this.manualRiskReviewRequired = manualRiskReviewRequired;
        }

        public Boolean getNotTradeInstruction() {
            return notTradeInstruction;
        }

        public void setNotTradeInstruction(Boolean notTradeInstruction) {
            this.notTradeInstruction = notTradeInstruction;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class PaperObservationDisplayVO {
        private String paperObservationStatus = "BACKEND_PENDING";
        private String paperObservationStatusLabel = "后端未接入";
        private Boolean paperObservationAvailable = false;
        private Boolean manualReviewEntryAvailable = false;
        private Integer linkedPaperObservationCount = 0;
        private Integer linkedReviewCount = 0;
        private Boolean missedOpportunityFlag = false;
        private String reviewSummary;
        private Boolean notRealPosition = true;
        private Boolean notTradeInstruction = true;
        private Boolean manualReviewRequired = true;
        private String backendConnectionStatus = "BACKEND_PENDING";
        private LocalDateTime updatedAt;

        public String getPaperObservationStatus() {
            return paperObservationStatus;
        }

        public void setPaperObservationStatus(String paperObservationStatus) {
            this.paperObservationStatus = paperObservationStatus;
        }

        public String getPaperObservationStatusLabel() {
            return paperObservationStatusLabel;
        }

        public void setPaperObservationStatusLabel(String paperObservationStatusLabel) {
            this.paperObservationStatusLabel = paperObservationStatusLabel;
        }

        public Boolean getPaperObservationAvailable() {
            return paperObservationAvailable;
        }

        public void setPaperObservationAvailable(Boolean paperObservationAvailable) {
            this.paperObservationAvailable = paperObservationAvailable;
        }

        public Boolean getManualReviewEntryAvailable() {
            return manualReviewEntryAvailable;
        }

        public void setManualReviewEntryAvailable(Boolean manualReviewEntryAvailable) {
            this.manualReviewEntryAvailable = manualReviewEntryAvailable;
        }

        public Integer getLinkedPaperObservationCount() {
            return linkedPaperObservationCount;
        }

        public void setLinkedPaperObservationCount(Integer linkedPaperObservationCount) {
            this.linkedPaperObservationCount = linkedPaperObservationCount;
        }

        public Integer getLinkedReviewCount() {
            return linkedReviewCount;
        }

        public void setLinkedReviewCount(Integer linkedReviewCount) {
            this.linkedReviewCount = linkedReviewCount;
        }

        public Boolean getMissedOpportunityFlag() {
            return missedOpportunityFlag;
        }

        public void setMissedOpportunityFlag(Boolean missedOpportunityFlag) {
            this.missedOpportunityFlag = missedOpportunityFlag;
        }

        public String getReviewSummary() {
            return reviewSummary;
        }

        public void setReviewSummary(String reviewSummary) {
            this.reviewSummary = reviewSummary;
        }

        public Boolean getNotRealPosition() {
            return notRealPosition;
        }

        public void setNotRealPosition(Boolean notRealPosition) {
            this.notRealPosition = notRealPosition;
        }

        public Boolean getNotTradeInstruction() {
            return notTradeInstruction;
        }

        public void setNotTradeInstruction(Boolean notTradeInstruction) {
            this.notTradeInstruction = notTradeInstruction;
        }

        public Boolean getManualReviewRequired() {
            return manualReviewRequired;
        }

        public void setManualReviewRequired(Boolean manualReviewRequired) {
            this.manualReviewRequired = manualReviewRequired;
        }

        public String getBackendConnectionStatus() {
            return backendConnectionStatus;
        }

        public void setBackendConnectionStatus(String backendConnectionStatus) {
            this.backendConnectionStatus = backendConnectionStatus;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
