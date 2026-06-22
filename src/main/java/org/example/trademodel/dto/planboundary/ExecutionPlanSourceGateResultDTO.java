package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class ExecutionPlanSourceGateResultDTO {
    public static final String STATUS_VALID = "VALID";
    public static final String STATUS_INCOMPLETE = "INCOMPLETE";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY";
    public static final String STATUS_INVALID = "INVALID";

    private String status = STATUS_INCOMPLETE;
    private boolean sourceComplete;
    private String sourceCompletenessSummary = "source gate not evaluated";
    private List<String> missingSourceReasons = new ArrayList<>();
    private List<String> blockerReasons = new ArrayList<>();
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoTrading = true;
    private boolean notOrderExecution = true;
    private boolean notUserPositionCreation = true;

    public static ExecutionPlanSourceGateResultDTO valid() {
        ExecutionPlanSourceGateResultDTO result = new ExecutionPlanSourceGateResultDTO();
        result.setStatus(STATUS_VALID);
        result.setSourceComplete(true);
        result.setSourceCompletenessSummary(
                "source gate VALID: entry, stop, TP, RR, liquidity, wick confirmation, multi-timeframe, event window, timeframe, and reason sources present"
        );
        return result;
    }

    public static ExecutionPlanSourceGateResultDTO incomplete(List<String> missingSourceReasons) {
        ExecutionPlanSourceGateResultDTO result = new ExecutionPlanSourceGateResultDTO();
        result.setStatus(STATUS_INCOMPLETE);
        result.setSourceComplete(false);
        result.setMissingSourceReasons(missingSourceReasons);
        result.setSourceCompletenessSummary("source gate INCOMPLETE: " + String.join("; ", result.getMissingSourceReasons()));
        return result;
    }

    public static ExecutionPlanSourceGateResultDTO blocked(List<String> blockerReasons) {
        ExecutionPlanSourceGateResultDTO result = new ExecutionPlanSourceGateResultDTO();
        result.setStatus(STATUS_BLOCKED);
        result.setSourceComplete(false);
        result.setBlockerReasons(blockerReasons);
        result.setSourceCompletenessSummary("source gate BLOCKED: " + String.join("; ", result.getBlockerReasons()));
        return result;
    }

    public static ExecutionPlanSourceGateResultDTO reviewOnly(List<String> blockerReasons) {
        ExecutionPlanSourceGateResultDTO result = new ExecutionPlanSourceGateResultDTO();
        result.setStatus(STATUS_REVIEW_ONLY);
        result.setSourceComplete(false);
        result.setBlockerReasons(blockerReasons);
        result.setSourceCompletenessSummary("source gate REVIEW_ONLY: " + String.join("; ", result.getBlockerReasons()));
        return result;
    }

    public static ExecutionPlanSourceGateResultDTO invalid(List<String> blockerReasons) {
        ExecutionPlanSourceGateResultDTO result = new ExecutionPlanSourceGateResultDTO();
        result.setStatus(STATUS_INVALID);
        result.setSourceComplete(false);
        result.setBlockerReasons(blockerReasons);
        result.setSourceCompletenessSummary("source gate INVALID: " + String.join("; ", result.getBlockerReasons()));
        return result;
    }

    public boolean isValid() {
        return STATUS_VALID.equals(status) && sourceComplete;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSourceComplete() {
        return sourceComplete;
    }

    public void setSourceComplete(boolean sourceComplete) {
        this.sourceComplete = sourceComplete;
    }

    public String getSourceCompletenessSummary() {
        return sourceCompletenessSummary;
    }

    public void setSourceCompletenessSummary(String sourceCompletenessSummary) {
        this.sourceCompletenessSummary = sourceCompletenessSummary;
    }

    public List<String> getMissingSourceReasons() {
        return missingSourceReasons;
    }

    public void setMissingSourceReasons(List<String> missingSourceReasons) {
        this.missingSourceReasons = missingSourceReasons == null ? new ArrayList<>() : new ArrayList<>(missingSourceReasons);
    }

    public List<String> getBlockerReasons() {
        return blockerReasons;
    }

    public void setBlockerReasons(List<String> blockerReasons) {
        this.blockerReasons = blockerReasons == null ? new ArrayList<>() : new ArrayList<>(blockerReasons);
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

    public boolean isNotExecutable() {
        return notExecutable;
    }

    public void setNotExecutable(boolean notExecutable) {
        this.notExecutable = notExecutable;
    }

    public boolean isNotAutoTrading() {
        return notAutoTrading;
    }

    public void setNotAutoTrading(boolean notAutoTrading) {
        this.notAutoTrading = notAutoTrading;
    }

    public boolean isNotOrderExecution() {
        return notOrderExecution;
    }

    public void setNotOrderExecution(boolean notOrderExecution) {
        this.notOrderExecution = notOrderExecution;
    }

    public boolean isNotUserPositionCreation() {
        return notUserPositionCreation;
    }

    public void setNotUserPositionCreation(boolean notUserPositionCreation) {
        this.notUserPositionCreation = notUserPositionCreation;
    }
}
