package org.example.trademodel.ai;

/** Durable provider-task lifecycle; this is intentionally separate from AI role state. */
public enum AiBackgroundTaskState {
    QUEUED,
    SUBMITTED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    TIMED_OUT,
    CANCELLED;

    public boolean terminal() {
        return this == SUCCEEDED || this == FAILED || this == TIMED_OUT || this == CANCELLED;
    }

    public boolean active() {
        return this == QUEUED || this == SUBMITTED || this == RUNNING;
    }
}
