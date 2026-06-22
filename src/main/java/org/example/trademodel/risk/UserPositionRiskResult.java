package org.example.trademodel.risk;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserPositionRiskResult {
    private String riskStatus;
    private String riskLevel;
    private boolean riskBlocked;
    private int includedPositionCount;
    private int excludedClosedPositionCount;
    private int openPositionCount;
    private int partiallyClosedPositionCount;
    private BigDecimal grossNotional = BigDecimal.ZERO;
    private BigDecimal leverageRisk = BigDecimal.ZERO;
    private BigDecimal positionSizeRisk = BigDecimal.ZERO;
    private BigDecimal concentrationRisk = BigDecimal.ZERO;
    private BigDecimal correlationRisk = BigDecimal.ZERO;
    private BigDecimal drawdownOrVarRisk = BigDecimal.ZERO;
    private BigDecimal aggregateRiskScore = BigDecimal.ZERO;
    private List<String> reasonCodes = new ArrayList<>();
    private String calculationMethod;
    private LocalDateTime calculatedAt;
    private boolean reviewOnly = true;
    private boolean manualReviewOnly = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoTrading = true;
    private boolean notOrderExecution = true;
    private boolean notAutoReduce = true;
    private boolean notAutoClose = true;
    private boolean notAutoReverse = true;
    private boolean notUserPositionMutation = true;

    public static UserPositionRiskResult noOpenPosition(int excludedClosedPositionCount) {
        UserPositionRiskResult result = base("NO_OPEN_USER_POSITION", "LOW", false);
        result.setExcludedClosedPositionCount(excludedClosedPositionCount);
        result.setReasonCodes(List.of("NO_OPEN_USER_POSITION"));
        return result;
    }

    public static UserPositionRiskResult failClosed(String reasonCode) {
        UserPositionRiskResult result = base("RISK_BLOCKED", "HIGH", true);
        result.setReasonCodes(List.of(reasonCode));
        result.setAggregateRiskScore(new BigDecimal("100"));
        return result;
    }

    static UserPositionRiskResult base(String riskStatus, String riskLevel, boolean riskBlocked) {
        UserPositionRiskResult result = new UserPositionRiskResult();
        result.riskStatus = riskStatus;
        result.riskLevel = riskLevel;
        result.riskBlocked = riskBlocked;
        result.calculatedAt = LocalDateTime.now();
        result.calculationMethod = "BigDecimal deterministic read-only calculation; "
                + "position size uses entry_price * quantity * leverage; "
                + "correlation uses conservative directional proxy, not statistical correlation; "
                + "drawdown uses stop-loss potential loss proxy.";
        return result;
    }

    public String getRiskStatus() {
        return riskStatus;
    }

    public void setRiskStatus(String riskStatus) {
        this.riskStatus = riskStatus;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isRiskBlocked() {
        return riskBlocked;
    }

    public void setRiskBlocked(boolean riskBlocked) {
        this.riskBlocked = riskBlocked;
    }

    public int getIncludedPositionCount() {
        return includedPositionCount;
    }

    public void setIncludedPositionCount(int includedPositionCount) {
        this.includedPositionCount = includedPositionCount;
    }

    public int getExcludedClosedPositionCount() {
        return excludedClosedPositionCount;
    }

    public void setExcludedClosedPositionCount(int excludedClosedPositionCount) {
        this.excludedClosedPositionCount = excludedClosedPositionCount;
    }

    public int getOpenPositionCount() {
        return openPositionCount;
    }

    public void setOpenPositionCount(int openPositionCount) {
        this.openPositionCount = openPositionCount;
    }

    public int getPartiallyClosedPositionCount() {
        return partiallyClosedPositionCount;
    }

    public void setPartiallyClosedPositionCount(int partiallyClosedPositionCount) {
        this.partiallyClosedPositionCount = partiallyClosedPositionCount;
    }

    public BigDecimal getGrossNotional() {
        return grossNotional;
    }

    public void setGrossNotional(BigDecimal grossNotional) {
        this.grossNotional = zeroIfNull(grossNotional);
    }

    public BigDecimal getLeverageRisk() {
        return leverageRisk;
    }

    public void setLeverageRisk(BigDecimal leverageRisk) {
        this.leverageRisk = zeroIfNull(leverageRisk);
    }

    public BigDecimal getPositionSizeRisk() {
        return positionSizeRisk;
    }

    public void setPositionSizeRisk(BigDecimal positionSizeRisk) {
        this.positionSizeRisk = zeroIfNull(positionSizeRisk);
    }

    public BigDecimal getConcentrationRisk() {
        return concentrationRisk;
    }

    public void setConcentrationRisk(BigDecimal concentrationRisk) {
        this.concentrationRisk = zeroIfNull(concentrationRisk);
    }

    public BigDecimal getCorrelationRisk() {
        return correlationRisk;
    }

    public void setCorrelationRisk(BigDecimal correlationRisk) {
        this.correlationRisk = zeroIfNull(correlationRisk);
    }

    public BigDecimal getDrawdownOrVarRisk() {
        return drawdownOrVarRisk;
    }

    public void setDrawdownOrVarRisk(BigDecimal drawdownOrVarRisk) {
        this.drawdownOrVarRisk = zeroIfNull(drawdownOrVarRisk);
    }

    public BigDecimal getAggregateRiskScore() {
        return aggregateRiskScore;
    }

    public void setAggregateRiskScore(BigDecimal aggregateRiskScore) {
        this.aggregateRiskScore = zeroIfNull(aggregateRiskScore);
    }

    public List<String> getReasonCodes() {
        return Collections.unmodifiableList(reasonCodes);
    }

    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes);
    }

    public String getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(String calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public boolean isReviewOnly() {
        return reviewOnly;
    }

    public boolean isManualReviewOnly() {
        return manualReviewOnly;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isNotExecutable() {
        return notExecutable;
    }

    public boolean isNotAutoTrading() {
        return notAutoTrading;
    }

    public boolean isNotOrderExecution() {
        return notOrderExecution;
    }

    public boolean isNotAutoReduce() {
        return notAutoReduce;
    }

    public boolean isNotAutoClose() {
        return notAutoClose;
    }

    public boolean isNotAutoReverse() {
        return notAutoReverse;
    }

    public boolean isNotUserPositionMutation() {
        return notUserPositionMutation;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
