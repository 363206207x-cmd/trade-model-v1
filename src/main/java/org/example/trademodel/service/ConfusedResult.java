package org.example.trademodel.service;

public class ConfusedResult {

    private int confusedScore;
    private boolean shouldEnter;
    private boolean shouldExit;
    private String conflictReasons;

    public ConfusedResult() {
    }

    public ConfusedResult(int confusedScore, boolean shouldEnter, boolean shouldExit, String conflictReasons) {
        this.confusedScore = confusedScore;
        this.shouldEnter = shouldEnter;
        this.shouldExit = shouldExit;
        this.conflictReasons = conflictReasons;
    }

    public int getConfusedScore() {
        return confusedScore;
    }

    public void setConfusedScore(int confusedScore) {
        this.confusedScore = confusedScore;
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

    public String getConflictReasons() {
        return conflictReasons;
    }

    public void setConflictReasons(String conflictReasons) {
        this.conflictReasons = conflictReasons;
    }
}
