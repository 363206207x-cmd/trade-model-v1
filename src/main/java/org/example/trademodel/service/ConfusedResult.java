package org.example.trademodel.service;

public class ConfusedResult {

    private int confusedScore;
    private String previousState;
    private String nextState;
    private boolean shouldEnter;
    private boolean shouldExit;
    private int confusedLowStreak;
    private boolean directionalPushBlocked;
    private String conflictReasons;
    private String transitionReason;

    private final boolean reviewOnly = true;
    private final boolean manualReviewOnly = true;
    private final boolean notTradeInstruction = true;
    private final boolean notExecutable = true;
    private final boolean notAutoTrading = true;
    private final boolean notOrderExecution = true;
    private final boolean notStateMachineExecution = true;

    public ConfusedResult() {
    }

    public ConfusedResult(int confusedScore, boolean shouldEnter, boolean shouldExit, String conflictReasons) {
        this.confusedScore = confusedScore;
        this.shouldEnter = shouldEnter;
        this.shouldExit = shouldExit;
        this.conflictReasons = conflictReasons;
    }

    public ConfusedResult(int confusedScore, String previousState, String nextState, boolean shouldEnter,
                          boolean shouldExit, int confusedLowStreak, boolean directionalPushBlocked,
                          String conflictReasons, String transitionReason) {
        this.confusedScore = confusedScore;
        this.previousState = previousState;
        this.nextState = nextState;
        this.shouldEnter = shouldEnter;
        this.shouldExit = shouldExit;
        this.confusedLowStreak = confusedLowStreak;
        this.directionalPushBlocked = directionalPushBlocked;
        this.conflictReasons = conflictReasons;
        this.transitionReason = transitionReason;
    }

    public int getConfusedScore() {
        return confusedScore;
    }

    public void setConfusedScore(int confusedScore) {
        this.confusedScore = confusedScore;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public String getNextState() {
        return nextState;
    }

    public void setNextState(String nextState) {
        this.nextState = nextState;
    }

    public boolean isShouldEnter() {
        return shouldEnter;
    }

    public void setShouldEnter(boolean shouldEnter) {
        this.shouldEnter = shouldEnter;
    }

    public boolean isShouldExit() {
        return shouldExit;
    }

    public void setShouldExit(boolean shouldExit) {
        this.shouldExit = shouldExit;
    }

    public int getConfusedLowStreak() {
        return confusedLowStreak;
    }

    public void setConfusedLowStreak(int confusedLowStreak) {
        this.confusedLowStreak = confusedLowStreak;
    }

    public boolean isDirectionalPushBlocked() {
        return directionalPushBlocked;
    }

    public void setDirectionalPushBlocked(boolean directionalPushBlocked) {
        this.directionalPushBlocked = directionalPushBlocked;
    }

    public String getConflictReasons() {
        return conflictReasons;
    }

    public void setConflictReasons(String conflictReasons) {
        this.conflictReasons = conflictReasons;
    }

    public String getTransitionReason() {
        return transitionReason;
    }

    public void setTransitionReason(String transitionReason) {
        this.transitionReason = transitionReason;
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

    public boolean isNotStateMachineExecution() {
        return notStateMachineExecution;
    }
}
