package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BoundaryCandidateDTO {
    private String candidateId;
    private String symbol;
    private String timeframe;
    private String direction;
    private String decisionBias;
    private String decisionId;
    private String analysisId;
    private String ruleVersion;
    private LocalDateTime generatedAt;
    private String sourceType;
    private BigDecimal dataQualityScore;
    private String confidenceLevel;
    private String riskLevel;
    private BoundaryStatusEnum boundaryStatus;
    private String boundaryStatusText;
    private String statusReason;
    private List<BoundaryBlockingReasonDTO> blockingReasons = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();
    private List<String> invalidFields = new ArrayList<>();
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;
    private BoundaryEntryDTO entry;
    private BoundaryStopDTO stop;
    private List<BoundaryTakeProfitLevelDTO> takeProfitLevels = new ArrayList<>();
    private BoundarySourceFieldsDTO sourceFields;

    public static BoundaryCandidateDTO incomplete(String symbol, String timeframe, String reason) {
        return withStatus(symbol, timeframe, BoundaryStatusEnum.INCOMPLETE, "边界不完整", reason);
    }

    public static BoundaryCandidateDTO watchOnly(String symbol, String timeframe, String reason) {
        return withStatus(symbol, timeframe, BoundaryStatusEnum.WATCH_ONLY, "仅观察", reason);
    }

    public static BoundaryCandidateDTO invalid(String symbol, String timeframe, String reason) {
        return withStatus(symbol, timeframe, BoundaryStatusEnum.INVALID, "边界无效", reason);
    }

    public static BoundaryCandidateDTO valid(String symbol,
                                             String timeframe,
                                             BoundaryEntryDTO entry,
                                             BoundaryStopDTO stop,
                                             List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
                                             BoundarySourceFieldsDTO sourceFields,
                                             BigDecimal dataQualityScore) {
        requireText(symbol, "symbol");
        requireText(timeframe, "timeframe");
        requireNonNull(entry, "entry");
        requireNonNull(stop, "stop");
        requireTakeProfitLevels(takeProfitLevels);
        requireNonNull(sourceFields, "sourceFields");
        requireNonNull(dataQualityScore, "dataQualityScore");

        BoundaryCandidateDTO candidate = new BoundaryCandidateDTO();
        candidate.setSymbol(symbol);
        candidate.setTimeframe(timeframe);
        candidate.setBoundaryStatus(BoundaryStatusEnum.VALID);
        candidate.setBoundaryStatusText("边界有效");
        candidate.setGeneratedAt(LocalDateTime.now());
        candidate.setEntry(entry);
        candidate.setStop(stop);
        candidate.setTakeProfitLevels(new ArrayList<>(takeProfitLevels));
        candidate.setSourceFields(sourceFields);
        candidate.setDataQualityScore(dataQualityScore);
        candidate.setManualReviewRequired(true);
        candidate.setNotTradeInstruction(true);
        return candidate;
    }

    private static BoundaryCandidateDTO withStatus(String symbol,
                                                   String timeframe,
                                                   BoundaryStatusEnum status,
                                                   String statusText,
                                                   String reason) {
        BoundaryCandidateDTO candidate = new BoundaryCandidateDTO();
        candidate.setSymbol(symbol);
        candidate.setTimeframe(timeframe);
        candidate.setBoundaryStatus(status);
        candidate.setBoundaryStatusText(statusText);
        candidate.setStatusReason(reason);
        candidate.setGeneratedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) {
            candidate.getBlockingReasons().add(BoundaryBlockingReasonDTO.of(status.name(), reason));
        }
        return candidate;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private static void requireTakeProfitLevels(List<BoundaryTakeProfitLevelDTO> takeProfitLevels) {
        if (takeProfitLevels == null || takeProfitLevels.isEmpty()) {
            throw new IllegalArgumentException("takeProfitLevels is required");
        }
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDecisionBias() {
        return decisionBias;
    }

    public void setDecisionBias(String decisionBias) {
        this.decisionBias = decisionBias;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(BigDecimal dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BoundaryStatusEnum getBoundaryStatus() {
        return boundaryStatus;
    }

    public void setBoundaryStatus(BoundaryStatusEnum boundaryStatus) {
        this.boundaryStatus = boundaryStatus;
    }

    public String getBoundaryStatusText() {
        return boundaryStatusText;
    }

    public void setBoundaryStatusText(String boundaryStatusText) {
        this.boundaryStatusText = boundaryStatusText;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public List<BoundaryBlockingReasonDTO> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<BoundaryBlockingReasonDTO> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public List<String> getInvalidFields() {
        return invalidFields;
    }

    public void setInvalidFields(List<String> invalidFields) {
        this.invalidFields = invalidFields;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
    }

    public BoundaryEntryDTO getEntry() {
        return entry;
    }

    public void setEntry(BoundaryEntryDTO entry) {
        this.entry = entry;
    }

    public BoundaryStopDTO getStop() {
        return stop;
    }

    public void setStop(BoundaryStopDTO stop) {
        this.stop = stop;
    }

    public List<BoundaryTakeProfitLevelDTO> getTakeProfitLevels() {
        return takeProfitLevels;
    }

    public void setTakeProfitLevels(List<BoundaryTakeProfitLevelDTO> takeProfitLevels) {
        this.takeProfitLevels = takeProfitLevels;
    }

    public BoundarySourceFieldsDTO getSourceFields() {
        return sourceFields;
    }

    public void setSourceFields(BoundarySourceFieldsDTO sourceFields) {
        this.sourceFields = sourceFields;
    }
}
