package org.example.trademodel.ai;

/** Collection state for Grok failure paths, including the frozen explicit no-path result. */
public enum GrokFailurePathState {
    FOUND,
    NONE_FOUND,
    INSUFFICIENT_DATA,
    SOURCE_UNAVAILABLE,
    STALE,
    NO_VERIFIABLE_FAILURE_PATH;

    public boolean permitsEmptyCollection() {
        return this != FOUND;
    }
}
