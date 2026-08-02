package org.example.trademodel.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardHomeVO {
    private HeaderVO header = new HeaderVO();
    private SystemStateVO systemState = new SystemStateVO();
    private List<AlertRowVO> alerts = new ArrayList<>();
    private List<EventRowVO> events = new ArrayList<>();
    private List<AssetVO> assets = new ArrayList<>();
    private List<PositionVO> positions = new ArrayList<>();
    private ModuleStatesVO states = new ModuleStatesVO();
    private String selectedSymbol;
    private Long selectedPositionId;
    private String positionSelectionStatus;
    private Integer matchingPositionCount;
    private ExecutionSuggestionVO executionSuggestion = new ExecutionSuggestionVO();
    private AiDecisionVO aiDecision = new AiDecisionVO();
    private PushInboxVO pushInbox = new PushInboxVO();
    private DerivativesSummaryVO derivatives = new DerivativesSummaryVO();
    private DiagnosticsVO diagnostics = new DiagnosticsVO();
    private SafetyVO safety = new SafetyVO();

    public HeaderVO getHeader() {
        return header;
    }

    public void setHeader(HeaderVO header) {
        this.header = header;
    }

    public SystemStateVO getSystemState() {
        return systemState;
    }

    public void setSystemState(SystemStateVO systemState) {
        this.systemState = systemState;
    }

    public List<AlertRowVO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertRowVO> alerts) {
        this.alerts = alerts;
    }

    public List<EventRowVO> getEvents() {
        return events;
    }

    public void setEvents(List<EventRowVO> events) {
        this.events = events;
    }

    public List<AssetVO> getAssets() {
        return assets;
    }

    public void setAssets(List<AssetVO> assets) {
        this.assets = assets;
    }

    public List<PositionVO> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionVO> positions) {
        this.positions = positions;
    }

    public ModuleStatesVO getStates() {
        return states;
    }

    public void setStates(ModuleStatesVO states) {
        this.states = states;
    }

    public String getSelectedSymbol() {
        return selectedSymbol;
    }

    public void setSelectedSymbol(String selectedSymbol) {
        this.selectedSymbol = selectedSymbol;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    public Long getSelectedPositionId() {
        return selectedPositionId;
    }

    public void setSelectedPositionId(Long selectedPositionId) {
        this.selectedPositionId = selectedPositionId;
    }

    public String getPositionSelectionStatus() {
        return positionSelectionStatus;
    }

    public void setPositionSelectionStatus(String positionSelectionStatus) {
        this.positionSelectionStatus = positionSelectionStatus;
    }

    public Integer getMatchingPositionCount() {
        return matchingPositionCount;
    }

    public void setMatchingPositionCount(Integer matchingPositionCount) {
        this.matchingPositionCount = matchingPositionCount;
    }

    public ExecutionSuggestionVO getExecutionSuggestion() {
        return executionSuggestion;
    }

    public void setExecutionSuggestion(ExecutionSuggestionVO executionSuggestion) {
        this.executionSuggestion = executionSuggestion;
    }

    public AiDecisionVO getAiDecision() {
        return aiDecision;
    }

    public void setAiDecision(AiDecisionVO aiDecision) {
        this.aiDecision = aiDecision;
    }

    public PushInboxVO getPushInbox() {
        return pushInbox;
    }

    public void setPushInbox(PushInboxVO pushInbox) {
        this.pushInbox = pushInbox;
    }

    public DerivativesSummaryVO getDerivatives() { return derivatives; }
    public void setDerivatives(DerivativesSummaryVO derivatives) { this.derivatives = derivatives; }

    public DiagnosticsVO getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(DiagnosticsVO diagnostics) {
        this.diagnostics = diagnostics;
    }

    public SafetyVO getSafety() {
        return safety;
    }

    public void setSafety(SafetyVO safety) {
        this.safety = safety;
    }

    public static class HeaderVO {
        private String pageTitle = "首页总览";
        private String dataStatus = "WAITING_SYNC";
        private String aiStatus = "WAITING_SYNC";
        private String aiStatusLabel = "等待同步";
        private String dataSourceText = "WAITING_SYNC";
        private LocalDateTime updatedAt;

        public String getPageTitle() {
            return pageTitle;
        }

        public void setPageTitle(String pageTitle) {
            this.pageTitle = pageTitle;
        }

        public String getDataStatus() {
            return dataStatus;
        }

        public void setDataStatus(String dataStatus) {
            this.dataStatus = dataStatus;
        }

        public String getAiStatus() {
            return aiStatus;
        }

        public void setAiStatus(String aiStatus) {
            this.aiStatus = aiStatus;
        }

        public String getAiStatusLabel() {
            return aiStatusLabel;
        }

        public void setAiStatusLabel(String aiStatusLabel) {
            this.aiStatusLabel = aiStatusLabel;
        }

        public String getDataSourceText() {
            return dataSourceText;
        }

        public void setDataSourceText(String dataSourceText) {
            this.dataSourceText = dataSourceText;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    public static class ModuleStatesVO {
        private String overall = "LOADING";
        private String assets = "LOADING";
        private String executionPlan = "LOADING";
        private String positions = "LOADING";
        private String ai = "LOADING";
        private String consistency = "LOADING";

        public String getOverall() { return overall; }
        public void setOverall(String overall) { this.overall = overall; }
        public String getAssets() { return assets; }
        public void setAssets(String assets) { this.assets = assets; }
        public String getExecutionPlan() { return executionPlan; }
        public void setExecutionPlan(String executionPlan) { this.executionPlan = executionPlan; }
        public String getPositions() { return positions; }
        public void setPositions(String positions) { this.positions = positions; }
        public String getAi() { return ai; }
        public void setAi(String ai) { this.ai = ai; }
        public String getConsistency() { return consistency; }
        public void setConsistency(String consistency) { this.consistency = consistency; }
    }

    public static class SystemStateVO {
        private StatusCardVO marketTrend = new StatusCardVO();
        private StatusCardVO riskLevel = new StatusCardVO();
        private StatusCardVO dataQuality = new StatusCardVO();
        private StatusCardVO aiConflict = new StatusCardVO();
        private StatusCardVO pendingReview = new StatusCardVO();
        private StatusCardVO confused = new StatusCardVO();
        private StatusCardVO hotReset = new StatusCardVO();

        public StatusCardVO getMarketTrend() {
            return marketTrend;
        }

        public void setMarketTrend(StatusCardVO marketTrend) {
            this.marketTrend = marketTrend;
        }

        public StatusCardVO getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(StatusCardVO riskLevel) {
            this.riskLevel = riskLevel;
        }

        public StatusCardVO getDataQuality() {
            return dataQuality;
        }

        public void setDataQuality(StatusCardVO dataQuality) {
            this.dataQuality = dataQuality;
        }

        public StatusCardVO getAiConflict() {
            return aiConflict;
        }

        public void setAiConflict(StatusCardVO aiConflict) {
            this.aiConflict = aiConflict;
        }

        public StatusCardVO getPendingReview() {
            return pendingReview;
        }

        public void setPendingReview(StatusCardVO pendingReview) {
            this.pendingReview = pendingReview;
        }

        public StatusCardVO getConfused() {
            return confused;
        }

        public void setConfused(StatusCardVO confused) {
            this.confused = confused;
        }

        public StatusCardVO getHotReset() {
            return hotReset;
        }

        public void setHotReset(StatusCardVO hotReset) {
            this.hotReset = hotReset;
        }
    }

    public static class StatusCardVO {
        private String key;
        private String label;
        private Object value;
        private String valueLabel;
        private String helper;
        private String status = "WAITING_SYNC";
        private Integer score;
        private Map<String, Object> meta;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getValueLabel() {
            return valueLabel;
        }

        public void setValueLabel(String valueLabel) {
            this.valueLabel = valueLabel;
        }

        public String getHelper() {
            return helper;
        }

        public void setHelper(String helper) {
            this.helper = helper;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }

        public void setMeta(Map<String, Object> meta) {
            this.meta = meta;
        }
    }

    public static class AlertRowVO {
        private String level;
        private String message;
        private String symbol;
        private String time;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }

    public static class EventRowVO {
        private String type;
        private String label;
        private String impactLevel;
        private Object timeWindow;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getImpactLevel() {
            return impactLevel;
        }

        public void setImpactLevel(String impactLevel) {
            this.impactLevel = impactLevel;
        }

        public Object getTimeWindow() {
            return timeWindow;
        }

        public void setTimeWindow(Object timeWindow) {
            this.timeWindow = timeWindow;
        }
    }

    public static class AssetVO {
        private Integer slot;
        private String slotType;
        private String symbol;
        private String rawSymbol;
        private String analysisId;
        private String marketBias;
        private String marketBiasLabel;
        private Integer compositeScore;
        private String confidenceLevel;
        private String confidenceLabel;
        private String riskLevel;
        private String riskLabel;
        private String assetState;
        private String assetStateLabel;
        private Boolean worthOpening;
        private BigDecimal latestPrice;
        private String dataFreshness;
        private Map<String, String> timeframeFreshness;
        private String sourceProvider;
        private String unavailableReason;
        private Integer evidenceCount;
        private LocalDateTime latestAnalysisTime;
        private String currentConclusion;
        private String moduleState = "MISSING";
        private Map<String, String> fieldSourceStatus = Map.of();
        private String dataQuality = "MISSING";
        private String multiTimeframeState;
        private Boolean confused;
        private LocalDateTime updatedAt;

        public Integer getSlot() {
            return slot;
        }

        public void setSlot(Integer slot) {
            this.slot = slot;
        }

        public String getSlotType() {
            return slotType;
        }

        public void setSlotType(String slotType) {
            this.slotType = slotType;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getRawSymbol() {
            return rawSymbol;
        }

        public void setRawSymbol(String rawSymbol) {
            this.rawSymbol = rawSymbol;
        }

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public String getMarketBias() {
            return marketBias;
        }

        public void setMarketBias(String marketBias) {
            this.marketBias = marketBias;
        }

        public String getMarketBiasLabel() {
            return marketBiasLabel;
        }

        public void setMarketBiasLabel(String marketBiasLabel) {
            this.marketBiasLabel = marketBiasLabel;
        }

        public Integer getCompositeScore() {
            return compositeScore;
        }

        public void setCompositeScore(Integer compositeScore) {
            this.compositeScore = compositeScore;
        }

        public String getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(String confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }

        public String getConfidenceLabel() {
            return confidenceLabel;
        }

        public void setConfidenceLabel(String confidenceLabel) {
            this.confidenceLabel = confidenceLabel;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getRiskLabel() {
            return riskLabel;
        }

        public void setRiskLabel(String riskLabel) {
            this.riskLabel = riskLabel;
        }

        public String getAssetState() {
            return assetState;
        }

        public void setAssetState(String assetState) {
            this.assetState = assetState;
        }

        public String getAssetStateLabel() {
            return assetStateLabel;
        }

        public void setAssetStateLabel(String assetStateLabel) {
            this.assetStateLabel = assetStateLabel;
        }

        public Boolean getWorthOpening() {
            return worthOpening;
        }

        public void setWorthOpening(Boolean worthOpening) {
            this.worthOpening = worthOpening;
        }

        public BigDecimal getLatestPrice() { return latestPrice; }
        public void setLatestPrice(BigDecimal latestPrice) { this.latestPrice = latestPrice; }
        public String getDataFreshness() { return dataFreshness; }
        public void setDataFreshness(String dataFreshness) { this.dataFreshness = dataFreshness; }
        public Map<String, String> getTimeframeFreshness() { return timeframeFreshness; }
        public void setTimeframeFreshness(Map<String, String> timeframeFreshness) { this.timeframeFreshness = timeframeFreshness; }
        public String getSourceProvider() { return sourceProvider; }
        public void setSourceProvider(String sourceProvider) { this.sourceProvider = sourceProvider; }
        public String getUnavailableReason() { return unavailableReason; }
        public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
        public Integer getEvidenceCount() { return evidenceCount; }
        public void setEvidenceCount(Integer evidenceCount) { this.evidenceCount = evidenceCount; }
        public LocalDateTime getLatestAnalysisTime() { return latestAnalysisTime; }
        public void setLatestAnalysisTime(LocalDateTime latestAnalysisTime) { this.latestAnalysisTime = latestAnalysisTime; }
        public String getCurrentConclusion() { return currentConclusion; }
        public void setCurrentConclusion(String currentConclusion) { this.currentConclusion = currentConclusion; }
        public String getModuleState() { return moduleState; }
        public void setModuleState(String moduleState) { this.moduleState = moduleState; }
        public Map<String, String> getFieldSourceStatus() { return fieldSourceStatus; }
        public void setFieldSourceStatus(Map<String, String> fieldSourceStatus) {
            this.fieldSourceStatus = fieldSourceStatus == null ? Map.of() : fieldSourceStatus;
        }
        public String getDataQuality() { return dataQuality; }
        public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }
        public String getMultiTimeframeState() { return multiTimeframeState; }
        public void setMultiTimeframeState(String multiTimeframeState) { this.multiTimeframeState = multiTimeframeState; }
        public Boolean getConfused() { return confused; }
        public void setConfused(Boolean confused) { this.confused = confused; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class PositionVO {
        private Long positionId;
        private String symbol;
        private String direction;
        private String directionLabel;
        private BigDecimal entryPrice;
        private BigDecimal currentPrice;
        private BigDecimal floatingPnl;
        private BigDecimal pnlPct;
        private BigDecimal accountImpactPct;
        private BigDecimal leverage;
        private BigDecimal positionSize;
        private String positionStatus;
        private String positionStatusLabel;
        private BigDecimal userStopLoss;
        private BigDecimal userTakeProfit;
        private BigDecimal systemSuggestedStopLoss;
        private BigDecimal systemSuggestedTakeProfit;
        private String monitorConclusion;
        private String entryLogicStatus;
        private String entryLogicStatusLabel;
        private String directionSupportStatus;
        private String directionSupportStatusLabel;
        private String reversalStatus;
        private String reversalStatusLabel;
        private String riskLevel;
        private String riskLevelLabel;
        private String suggestedManualAction;
        private String suggestedManualActionText;
        private LocalDateTime updatedAt;
        private LocalDateTime openedAt;
        private LocalDateTime lastMonitorAt;
        private LocalDateTime nextMonitorAt;
        private String sourceRefId;
        private String sourceAnalysisId;
        private String sourceExecutionPlanId;
        private String sourceTraceId;
        private String moduleState = "MISSING";
        private String warningState = "MISSING";

        @JsonSerialize(using = ToStringSerializer.class)
        public Long getPositionId() {
            return positionId;
        }

        public void setPositionId(Long positionId) {
            this.positionId = positionId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getDirectionLabel() { return directionLabel; }
        public void setDirectionLabel(String directionLabel) { this.directionLabel = directionLabel; }

        public BigDecimal getEntryPrice() {
            return entryPrice;
        }

        public void setEntryPrice(BigDecimal entryPrice) {
            this.entryPrice = entryPrice;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public BigDecimal getFloatingPnl() {
            return floatingPnl;
        }

        public void setFloatingPnl(BigDecimal floatingPnl) {
            this.floatingPnl = floatingPnl;
        }

        public BigDecimal getPnlPct() {
            return pnlPct;
        }

        public void setPnlPct(BigDecimal pnlPct) {
            this.pnlPct = pnlPct;
        }

        public BigDecimal getAccountImpactPct() {
            return accountImpactPct;
        }

        public void setAccountImpactPct(BigDecimal accountImpactPct) {
            this.accountImpactPct = accountImpactPct;
        }

        public BigDecimal getLeverage() {
            return leverage;
        }

        public void setLeverage(BigDecimal leverage) {
            this.leverage = leverage;
        }

        public BigDecimal getPositionSize() {
            return positionSize;
        }

        public void setPositionSize(BigDecimal positionSize) {
            this.positionSize = positionSize;
        }

        public String getPositionStatus() {
            return positionStatus;
        }

        public void setPositionStatus(String positionStatus) {
            this.positionStatus = positionStatus;
        }

        public String getPositionStatusLabel() { return positionStatusLabel; }
        public void setPositionStatusLabel(String positionStatusLabel) { this.positionStatusLabel = positionStatusLabel; }
        public BigDecimal getUserStopLoss() { return userStopLoss; }
        public void setUserStopLoss(BigDecimal userStopLoss) { this.userStopLoss = userStopLoss; }
        public BigDecimal getUserTakeProfit() { return userTakeProfit; }
        public void setUserTakeProfit(BigDecimal userTakeProfit) { this.userTakeProfit = userTakeProfit; }
        public BigDecimal getSystemSuggestedStopLoss() { return systemSuggestedStopLoss; }
        public void setSystemSuggestedStopLoss(BigDecimal systemSuggestedStopLoss) { this.systemSuggestedStopLoss = systemSuggestedStopLoss; }
        public BigDecimal getSystemSuggestedTakeProfit() { return systemSuggestedTakeProfit; }
        public void setSystemSuggestedTakeProfit(BigDecimal systemSuggestedTakeProfit) { this.systemSuggestedTakeProfit = systemSuggestedTakeProfit; }

        public String getMonitorConclusion() {
            return monitorConclusion;
        }

        public void setMonitorConclusion(String monitorConclusion) {
            this.monitorConclusion = monitorConclusion;
        }

        public String getEntryLogicStatus() {
            return entryLogicStatus;
        }

        public void setEntryLogicStatus(String entryLogicStatus) {
            this.entryLogicStatus = entryLogicStatus;
        }

        public String getEntryLogicStatusLabel() { return entryLogicStatusLabel; }
        public void setEntryLogicStatusLabel(String entryLogicStatusLabel) { this.entryLogicStatusLabel = entryLogicStatusLabel; }

        public String getDirectionSupportStatus() {
            return directionSupportStatus;
        }

        public void setDirectionSupportStatus(String directionSupportStatus) {
            this.directionSupportStatus = directionSupportStatus;
        }

        public String getDirectionSupportStatusLabel() { return directionSupportStatusLabel; }
        public void setDirectionSupportStatusLabel(String directionSupportStatusLabel) { this.directionSupportStatusLabel = directionSupportStatusLabel; }

        public String getReversalStatus() {
            return reversalStatus;
        }

        public void setReversalStatus(String reversalStatus) {
            this.reversalStatus = reversalStatus;
        }

        public String getReversalStatusLabel() { return reversalStatusLabel; }
        public void setReversalStatusLabel(String reversalStatusLabel) { this.reversalStatusLabel = reversalStatusLabel; }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getRiskLevelLabel() { return riskLevelLabel; }
        public void setRiskLevelLabel(String riskLevelLabel) { this.riskLevelLabel = riskLevelLabel; }

        public String getSuggestedManualAction() {
            return suggestedManualAction;
        }

        public void setSuggestedManualAction(String suggestedManualAction) {
            this.suggestedManualAction = suggestedManualAction;
        }

        public String getSuggestedManualActionText() {
            return suggestedManualActionText;
        }

        public void setSuggestedManualActionText(String suggestedManualActionText) {
            this.suggestedManualActionText = suggestedManualActionText;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
        public LocalDateTime getOpenedAt() { return openedAt; }
        public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
        public LocalDateTime getLastMonitorAt() { return lastMonitorAt; }
        public void setLastMonitorAt(LocalDateTime lastMonitorAt) { this.lastMonitorAt = lastMonitorAt; }
        public LocalDateTime getNextMonitorAt() { return nextMonitorAt; }
        public void setNextMonitorAt(LocalDateTime nextMonitorAt) { this.nextMonitorAt = nextMonitorAt; }
        public String getSourceRefId() { return sourceRefId; }
        public void setSourceRefId(String sourceRefId) { this.sourceRefId = sourceRefId; }
        public String getSourceAnalysisId() { return sourceAnalysisId; }
        public void setSourceAnalysisId(String sourceAnalysisId) { this.sourceAnalysisId = sourceAnalysisId; }
        public String getSourceExecutionPlanId() { return sourceExecutionPlanId; }
        public void setSourceExecutionPlanId(String sourceExecutionPlanId) { this.sourceExecutionPlanId = sourceExecutionPlanId; }
        public String getSourceTraceId() { return sourceTraceId; }
        public void setSourceTraceId(String sourceTraceId) { this.sourceTraceId = sourceTraceId; }
        public String getModuleState() { return moduleState; }
        public void setModuleState(String moduleState) { this.moduleState = moduleState; }
        public String getWarningState() { return warningState; }
        public void setWarningState(String warningState) { this.warningState = warningState; }
    }

    public static class ExecutionSuggestionVO {
        private String status;
        private String statusLabel;
        private String blockedReason;
        private String sourceAnalysisId;
        private String sourceExecutionPlanId;
        private String sourceTraceId;
        private Boolean positionMode = false;
        private PositionVO positionMonitor;
        private String originalPlanLabel;
        private String originalPlanIdentity;
        private String originalPlanCurrentValidity;
        private String direction;
        private String entryZone;
        private String stopLoss;
        private String takeProfitRules;
        private String leverageSuggestion;
        private String positionSuggestion;
        private String validPeriod;
        private OffsetDateTime validFrom;
        private OffsetDateTime expiresAt;
        private String invalidCondition;
        private String moduleState = "MISSING";

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getBlockedReason() { return blockedReason; }
        public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
        public String getSourceAnalysisId() { return sourceAnalysisId; }
        public void setSourceAnalysisId(String sourceAnalysisId) { this.sourceAnalysisId = sourceAnalysisId; }
        public String getSourceExecutionPlanId() { return sourceExecutionPlanId; }
        public void setSourceExecutionPlanId(String sourceExecutionPlanId) { this.sourceExecutionPlanId = sourceExecutionPlanId; }
        public String getSourceTraceId() { return sourceTraceId; }
        public void setSourceTraceId(String sourceTraceId) { this.sourceTraceId = sourceTraceId; }
        public Boolean getPositionMode() { return positionMode; }
        public void setPositionMode(Boolean positionMode) { this.positionMode = positionMode; }
        public PositionVO getPositionMonitor() { return positionMonitor; }
        public void setPositionMonitor(PositionVO positionMonitor) { this.positionMonitor = positionMonitor; }
        public String getOriginalPlanLabel() { return originalPlanLabel; }
        public void setOriginalPlanLabel(String originalPlanLabel) { this.originalPlanLabel = originalPlanLabel; }
        public String getOriginalPlanIdentity() { return originalPlanIdentity; }
        public void setOriginalPlanIdentity(String originalPlanIdentity) { this.originalPlanIdentity = originalPlanIdentity; }
        public String getOriginalPlanCurrentValidity() { return originalPlanCurrentValidity; }
        public void setOriginalPlanCurrentValidity(String originalPlanCurrentValidity) {
            this.originalPlanCurrentValidity = originalPlanCurrentValidity;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getEntryZone() {
            return entryZone;
        }

        public void setEntryZone(String entryZone) {
            this.entryZone = entryZone;
        }

        public String getStopLoss() {
            return stopLoss;
        }

        public void setStopLoss(String stopLoss) {
            this.stopLoss = stopLoss;
        }

        public String getTakeProfitRules() {
            return takeProfitRules;
        }

        public void setTakeProfitRules(String takeProfitRules) {
            this.takeProfitRules = takeProfitRules;
        }

        public String getLeverageSuggestion() {
            return leverageSuggestion;
        }

        public void setLeverageSuggestion(String leverageSuggestion) {
            this.leverageSuggestion = leverageSuggestion;
        }

        public String getPositionSuggestion() {
            return positionSuggestion;
        }

        public void setPositionSuggestion(String positionSuggestion) {
            this.positionSuggestion = positionSuggestion;
        }

        public String getValidPeriod() {
            return validPeriod;
        }

        public void setValidPeriod(String validPeriod) {
            this.validPeriod = validPeriod;
        }

        public OffsetDateTime getValidFrom() { return validFrom; }
        public void setValidFrom(OffsetDateTime validFrom) { this.validFrom = validFrom; }
        public OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

        public String getInvalidCondition() {
            return invalidCondition;
        }

        public void setInvalidCondition(String invalidCondition) {
            this.invalidCondition = invalidCondition;
        }

        public String getModuleState() { return moduleState; }
        public void setModuleState(String moduleState) { this.moduleState = moduleState; }
    }

    public static class AiDecisionVO {
        private String schemaVersion;
        private String runStatus;
        private String runStatusLabel;
        private String decisionMode;
        private String decisionModeLabel;
        private String activeTab = "GPT_FINAL";
        private List<AiTabVO> tabs = new ArrayList<>();
        private ConsistencyVO consistency = new ConsistencyVO();

        public String getRunStatus() { return runStatus; }
        public void setRunStatus(String runStatus) { this.runStatus = runStatus; }
        public String getRunStatusLabel() { return runStatusLabel; }
        public void setRunStatusLabel(String runStatusLabel) { this.runStatusLabel = runStatusLabel; }
        public String getDecisionMode() { return decisionMode; }
        public void setDecisionMode(String decisionMode) { this.decisionMode = decisionMode; }
        public String getDecisionModeLabel() { return decisionModeLabel; }
        public void setDecisionModeLabel(String decisionModeLabel) { this.decisionModeLabel = decisionModeLabel; }

        public String getSchemaVersion() {
            return schemaVersion;
        }

        public void setSchemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
        }

        public String getActiveTab() {
            return activeTab;
        }

        public void setActiveTab(String activeTab) {
            this.activeTab = activeTab;
        }

        public List<AiTabVO> getTabs() {
            return tabs;
        }

        public void setTabs(List<AiTabVO> tabs) {
            this.tabs = tabs;
        }

        public ConsistencyVO getConsistency() {
            return consistency;
        }

        public void setConsistency(ConsistencyVO consistency) {
            this.consistency = consistency;
        }
    }

    public static class AiTabVO {
        private String role;
        private String roleLabel;
        private String runStatus;
        private String runStatusLabel;
        private Boolean resultAvailable = false;
        private String statusMessage;
        private String stance;
        private String direction;
        private String confidenceLevel;
        private List<String> supportEvidence = new ArrayList<>();
        private List<String> againstEvidence = new ArrayList<>();
        private List<String> riskPoints = new ArrayList<>();
        private String downgradeReason;
        private String reviewConclusion;
        private String finalMarketBias;
        private String finalConfidence;
        private String finalRiskLevel;
        private String finalPlanMode;
        private String worthOpening;
        private String finalConclusion;
        private List<String> coreSupportingEvidence = new ArrayList<>();
        private List<String> coreCounterEvidence = new ArrayList<>();
        private String decisionSummary;
        private String reviewVerdict;
        private List<String> detectedContradictions = new ArrayList<>();
        private List<String> weakEvidence = new ArrayList<>();
        private List<String> logicGaps = new ArrayList<>();
        private String downgradeRecommendation;
        private String riskAdjustmentSuggestion;
        private String manualReviewRequired;
        private String challengeThesis;
        private List<String> eventRisks = new ArrayList<>();
        private List<String> sentimentReversalRisks = new ArrayList<>();
        private List<String> microstructureTraps = new ArrayList<>();
        private List<String> liquidityRisks = new ArrayList<>();
        private List<String> counterEvidence = new ArrayList<>();
        private String challengeConclusion;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getRoleLabel() {
            return roleLabel;
        }

        public void setRoleLabel(String roleLabel) {
            this.roleLabel = roleLabel;
        }

        public String getRunStatus() { return runStatus; }
        public void setRunStatus(String runStatus) { this.runStatus = runStatus; }
        public String getRunStatusLabel() { return runStatusLabel; }
        public void setRunStatusLabel(String runStatusLabel) { this.runStatusLabel = runStatusLabel; }
        public Boolean getResultAvailable() { return resultAvailable; }
        public void setResultAvailable(Boolean resultAvailable) { this.resultAvailable = resultAvailable; }
        public String getStatusMessage() { return statusMessage; }
        public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
        public String getStance() { return stance; }
        public void setStance(String stance) { this.stance = stance; }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(String confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }

        public List<String> getSupportEvidence() {
            return supportEvidence;
        }

        public void setSupportEvidence(List<String> supportEvidence) {
            this.supportEvidence = supportEvidence;
        }

        public List<String> getAgainstEvidence() {
            return againstEvidence;
        }

        public void setAgainstEvidence(List<String> againstEvidence) {
            this.againstEvidence = againstEvidence;
        }

        public List<String> getRiskPoints() {
            return riskPoints;
        }

        public void setRiskPoints(List<String> riskPoints) {
            this.riskPoints = riskPoints;
        }

        public String getDowngradeReason() {
            return downgradeReason;
        }

        public void setDowngradeReason(String downgradeReason) {
            this.downgradeReason = downgradeReason;
        }

        public String getReviewConclusion() {
            return reviewConclusion;
        }

        public void setReviewConclusion(String reviewConclusion) {
            this.reviewConclusion = reviewConclusion;
        }

        public String getFinalMarketBias() {
            return finalMarketBias;
        }

        public void setFinalMarketBias(String finalMarketBias) {
            this.finalMarketBias = finalMarketBias;
        }

        public String getFinalConfidence() {
            return finalConfidence;
        }

        public void setFinalConfidence(String finalConfidence) {
            this.finalConfidence = finalConfidence;
        }

        public String getFinalRiskLevel() {
            return finalRiskLevel;
        }

        public void setFinalRiskLevel(String finalRiskLevel) {
            this.finalRiskLevel = finalRiskLevel;
        }

        public String getFinalPlanMode() {
            return finalPlanMode;
        }

        public void setFinalPlanMode(String finalPlanMode) {
            this.finalPlanMode = finalPlanMode;
        }

        public String getWorthOpening() {
            return worthOpening;
        }

        public void setWorthOpening(String worthOpening) {
            this.worthOpening = worthOpening;
        }

        public String getFinalConclusion() {
            return finalConclusion;
        }

        public void setFinalConclusion(String finalConclusion) {
            this.finalConclusion = finalConclusion;
        }

        public List<String> getCoreSupportingEvidence() {
            return coreSupportingEvidence;
        }

        public void setCoreSupportingEvidence(List<String> coreSupportingEvidence) {
            this.coreSupportingEvidence = coreSupportingEvidence;
        }

        public List<String> getCoreCounterEvidence() {
            return coreCounterEvidence;
        }

        public void setCoreCounterEvidence(List<String> coreCounterEvidence) {
            this.coreCounterEvidence = coreCounterEvidence;
        }

        public String getDecisionSummary() {
            return decisionSummary;
        }

        public void setDecisionSummary(String decisionSummary) {
            this.decisionSummary = decisionSummary;
        }

        public String getReviewVerdict() {
            return reviewVerdict;
        }

        public void setReviewVerdict(String reviewVerdict) {
            this.reviewVerdict = reviewVerdict;
        }

        public List<String> getDetectedContradictions() {
            return detectedContradictions;
        }

        public void setDetectedContradictions(List<String> detectedContradictions) {
            this.detectedContradictions = detectedContradictions;
        }

        public List<String> getWeakEvidence() {
            return weakEvidence;
        }

        public void setWeakEvidence(List<String> weakEvidence) {
            this.weakEvidence = weakEvidence;
        }

        public List<String> getLogicGaps() {
            return logicGaps;
        }

        public void setLogicGaps(List<String> logicGaps) {
            this.logicGaps = logicGaps;
        }

        public String getDowngradeRecommendation() {
            return downgradeRecommendation;
        }

        public void setDowngradeRecommendation(String downgradeRecommendation) {
            this.downgradeRecommendation = downgradeRecommendation;
        }

        public String getRiskAdjustmentSuggestion() {
            return riskAdjustmentSuggestion;
        }

        public void setRiskAdjustmentSuggestion(String riskAdjustmentSuggestion) {
            this.riskAdjustmentSuggestion = riskAdjustmentSuggestion;
        }

        public String getManualReviewRequired() {
            return manualReviewRequired;
        }

        public void setManualReviewRequired(String manualReviewRequired) {
            this.manualReviewRequired = manualReviewRequired;
        }

        public String getChallengeThesis() {
            return challengeThesis;
        }

        public void setChallengeThesis(String challengeThesis) {
            this.challengeThesis = challengeThesis;
        }

        public List<String> getEventRisks() {
            return eventRisks;
        }

        public void setEventRisks(List<String> eventRisks) {
            this.eventRisks = eventRisks;
        }

        public List<String> getSentimentReversalRisks() {
            return sentimentReversalRisks;
        }

        public void setSentimentReversalRisks(List<String> sentimentReversalRisks) {
            this.sentimentReversalRisks = sentimentReversalRisks;
        }

        public List<String> getMicrostructureTraps() {
            return microstructureTraps;
        }

        public void setMicrostructureTraps(List<String> microstructureTraps) {
            this.microstructureTraps = microstructureTraps;
        }

        public List<String> getLiquidityRisks() {
            return liquidityRisks;
        }

        public void setLiquidityRisks(List<String> liquidityRisks) {
            this.liquidityRisks = liquidityRisks;
        }

        public List<String> getCounterEvidence() {
            return counterEvidence;
        }

        public void setCounterEvidence(List<String> counterEvidence) {
            this.counterEvidence = counterEvidence;
        }

        public String getChallengeConclusion() {
            return challengeConclusion;
        }

        public void setChallengeConclusion(String challengeConclusion) {
            this.challengeConclusion = challengeConclusion;
        }
    }

    public static class ConsistencyVO {
        private String level;
        private Integer score;
        private Boolean confused;
        private Boolean aiApplicable;
        private Boolean directionalPushBlocked;
        private Integer consistencyScore;
        private String consistencyLevel;
        private String consistencySummary;
        private String downgradeReason;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Boolean getConfused() {
            return confused;
        }

        public void setConfused(Boolean confused) {
            this.confused = confused;
        }

        public Boolean getAiApplicable() { return aiApplicable; }
        public void setAiApplicable(Boolean aiApplicable) { this.aiApplicable = aiApplicable; }
        public Boolean getDirectionalPushBlocked() { return directionalPushBlocked; }
        public void setDirectionalPushBlocked(Boolean directionalPushBlocked) {
            this.directionalPushBlocked = directionalPushBlocked;
        }

        public Integer getConsistencyScore() {
            return consistencyScore;
        }

        public void setConsistencyScore(Integer consistencyScore) {
            this.consistencyScore = consistencyScore;
        }

        public String getConsistencyLevel() {
            return consistencyLevel;
        }

        public void setConsistencyLevel(String consistencyLevel) {
            this.consistencyLevel = consistencyLevel;
        }

        public String getConsistencySummary() {
            return consistencySummary;
        }

        public void setConsistencySummary(String consistencySummary) {
            this.consistencySummary = consistencySummary;
        }

        public String getDowngradeReason() {
            return downgradeReason;
        }

        public void setDowngradeReason(String downgradeReason) {
            this.downgradeReason = downgradeReason;
        }
    }

    public static class PushInboxVO {
        private String telegramStatus = "WAITING_SYNC";
        private Boolean hasOpenPosition = false;
        private String mode = "OPPORTUNITY_ONLY";
        private PushCountsVO counts = new PushCountsVO();
        private List<PushItemVO> items = new ArrayList<>();

        public String getTelegramStatus() {
            return telegramStatus;
        }

        public void setTelegramStatus(String telegramStatus) {
            this.telegramStatus = telegramStatus;
        }

        public Boolean getHasOpenPosition() {
            return hasOpenPosition;
        }

        public void setHasOpenPosition(Boolean hasOpenPosition) {
            this.hasOpenPosition = hasOpenPosition;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public PushCountsVO getCounts() {
            return counts;
        }

        public void setCounts(PushCountsVO counts) {
            this.counts = counts;
        }

        public List<PushItemVO> getItems() {
            return items;
        }

        public void setItems(List<PushItemVO> items) {
            this.items = items;
        }
    }

    public static class PushCountsVO {
        private Integer executable = 0;
        private Integer waiting = 0;
        private Integer invalidated = 0;
        private Integer positionRisk = 0;

        public Integer getExecutable() {
            return executable;
        }

        public void setExecutable(Integer executable) {
            this.executable = executable;
        }

        public Integer getWaiting() {
            return waiting;
        }

        public void setWaiting(Integer waiting) {
            this.waiting = waiting;
        }

        public Integer getInvalidated() {
            return invalidated;
        }

        public void setInvalidated(Integer invalidated) {
            this.invalidated = invalidated;
        }

        public Integer getPositionRisk() {
            return positionRisk;
        }

        public void setPositionRisk(Integer positionRisk) {
            this.positionRisk = positionRisk;
        }
    }

    public static class PushItemVO {
        private String messageId;
        private String sourceIdentity;
        private String symbol;
        private String publicLifecycle;
        private String publicStatus;
        private LocalDateTime publicTimestamp;
        private String publicDescription;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getSourceIdentity() {
            return sourceIdentity;
        }

        public void setSourceIdentity(String sourceIdentity) {
            this.sourceIdentity = sourceIdentity;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getPublicLifecycle() {
            return publicLifecycle;
        }

        public void setPublicLifecycle(String publicLifecycle) {
            this.publicLifecycle = publicLifecycle;
        }

        public String getPublicStatus() {
            return publicStatus;
        }

        public void setPublicStatus(String publicStatus) {
            this.publicStatus = publicStatus;
        }

        public LocalDateTime getPublicTimestamp() {
            return publicTimestamp;
        }

        public void setPublicTimestamp(LocalDateTime publicTimestamp) {
            this.publicTimestamp = publicTimestamp;
        }

        public String getPublicDescription() {
            return publicDescription;
        }

        public void setPublicDescription(String publicDescription) {
            this.publicDescription = publicDescription;
        }
    }

    public static class DerivativesSummaryVO {
        private String status = "等待同步";
        private String openInterestStructure = "暂无";
        private String fundingRisk = "暂无";
        private String liquidationRisk = "暂无";
        private String crowdingDirection = "无明确";
        private Instant dataTime;
        private String source = "CoinGlass v4";
        private String decisionImpact = "等待同步";
        private List<String> reasonCodes = new ArrayList<>();

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getOpenInterestStructure() { return openInterestStructure; }
        public void setOpenInterestStructure(String openInterestStructure) { this.openInterestStructure = openInterestStructure; }
        public String getFundingRisk() { return fundingRisk; }
        public void setFundingRisk(String fundingRisk) { this.fundingRisk = fundingRisk; }
        public String getLiquidationRisk() { return liquidationRisk; }
        public void setLiquidationRisk(String liquidationRisk) { this.liquidationRisk = liquidationRisk; }
        public String getCrowdingDirection() { return crowdingDirection; }
        public void setCrowdingDirection(String crowdingDirection) { this.crowdingDirection = crowdingDirection; }
        public Instant getDataTime() { return dataTime; }
        public void setDataTime(Instant dataTime) { this.dataTime = dataTime; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getDecisionImpact() { return decisionImpact; }
        public void setDecisionImpact(String decisionImpact) { this.decisionImpact = decisionImpact; }
        public List<String> getReasonCodes() { return List.copyOf(reasonCodes); }
        public void setReasonCodes(List<String> reasonCodes) {
            this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes);
        }
    }

    public static class DiagnosticsVO {
        private String dataIngestion = "UNKNOWN";
        private String dataQuality = "UNKNOWN";
        private String aiCall = "UNKNOWN";
        private String pushRecheck = "UNKNOWN";
        private String telegram = "WAITING_SYNC";
        private String confused = "UNKNOWN";
        private String hotReset = "UNKNOWN";
        private String opportunityLog = "UNKNOWN";
        private String review = "UNKNOWN";
        private String marketDataProvider = "WAITING_SYNC";
        private String aiProvider = "WAITING_SYNC";
        private String externalContextProvider = "WAITING_SYNC";
        private ProviderReadinessVO providerReadiness = new ProviderReadinessVO();

        public String getDataIngestion() {
            return dataIngestion;
        }

        public void setDataIngestion(String dataIngestion) {
            this.dataIngestion = dataIngestion;
        }

        public String getDataQuality() {
            return dataQuality;
        }

        public void setDataQuality(String dataQuality) {
            this.dataQuality = dataQuality;
        }

        public String getAiCall() {
            return aiCall;
        }

        public void setAiCall(String aiCall) {
            this.aiCall = aiCall;
        }

        public String getPushRecheck() {
            return pushRecheck;
        }

        public void setPushRecheck(String pushRecheck) {
            this.pushRecheck = pushRecheck;
        }

        public String getTelegram() {
            return telegram;
        }

        public void setTelegram(String telegram) {
            this.telegram = telegram;
        }

        public String getConfused() {
            return confused;
        }

        public void setConfused(String confused) {
            this.confused = confused;
        }

        public String getHotReset() {
            return hotReset;
        }

        public void setHotReset(String hotReset) {
            this.hotReset = hotReset;
        }

        public String getOpportunityLog() {
            return opportunityLog;
        }

        public void setOpportunityLog(String opportunityLog) {
            this.opportunityLog = opportunityLog;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        public String getMarketDataProvider() {
            return marketDataProvider;
        }

        public void setMarketDataProvider(String marketDataProvider) {
            this.marketDataProvider = marketDataProvider;
        }

        public String getAiProvider() {
            return aiProvider;
        }

        public void setAiProvider(String aiProvider) {
            this.aiProvider = aiProvider;
        }

        public String getExternalContextProvider() {
            return externalContextProvider;
        }

        public void setExternalContextProvider(String externalContextProvider) {
            this.externalContextProvider = externalContextProvider;
        }

        public ProviderReadinessVO getProviderReadiness() {
            return providerReadiness;
        }

        public void setProviderReadiness(ProviderReadinessVO providerReadiness) {
            this.providerReadiness = providerReadiness == null ? new ProviderReadinessVO() : providerReadiness;
        }
    }

    public static class SafetyVO {
        private Boolean reviewOnly = true;
        private Boolean manualReviewOnly = true;
        private Boolean notTradeInstruction = true;
        private Boolean notExecutable = true;
        private Boolean notAutoTrading = true;
        private Boolean notOrderExecution = true;
        private Boolean notPushSend = true;
        private Boolean notExternalChannel = true;
        private Boolean notUserPositionCreation = true;
        private Boolean notUserPositionMutation = true;

        public Boolean getReviewOnly() {
            return reviewOnly;
        }

        public void setReviewOnly(Boolean reviewOnly) {
            this.reviewOnly = reviewOnly;
        }

        public Boolean getManualReviewOnly() {
            return manualReviewOnly;
        }

        public void setManualReviewOnly(Boolean manualReviewOnly) {
            this.manualReviewOnly = manualReviewOnly;
        }

        public Boolean getNotTradeInstruction() {
            return notTradeInstruction;
        }

        public void setNotTradeInstruction(Boolean notTradeInstruction) {
            this.notTradeInstruction = notTradeInstruction;
        }

        public Boolean getNotExecutable() {
            return notExecutable;
        }

        public void setNotExecutable(Boolean notExecutable) {
            this.notExecutable = notExecutable;
        }

        public Boolean getNotAutoTrading() {
            return notAutoTrading;
        }

        public void setNotAutoTrading(Boolean notAutoTrading) {
            this.notAutoTrading = notAutoTrading;
        }

        public Boolean getNotOrderExecution() {
            return notOrderExecution;
        }

        public void setNotOrderExecution(Boolean notOrderExecution) {
            this.notOrderExecution = notOrderExecution;
        }

        public Boolean getNotPushSend() {
            return notPushSend;
        }

        public void setNotPushSend(Boolean notPushSend) {
            this.notPushSend = notPushSend;
        }

        public Boolean getNotExternalChannel() {
            return notExternalChannel;
        }

        public void setNotExternalChannel(Boolean notExternalChannel) {
            this.notExternalChannel = notExternalChannel;
        }

        public Boolean getNotUserPositionCreation() {
            return notUserPositionCreation;
        }

        public void setNotUserPositionCreation(Boolean notUserPositionCreation) {
            this.notUserPositionCreation = notUserPositionCreation;
        }

        public Boolean getNotUserPositionMutation() {
            return notUserPositionMutation;
        }

        public void setNotUserPositionMutation(Boolean notUserPositionMutation) {
            this.notUserPositionMutation = notUserPositionMutation;
        }
    }
}
