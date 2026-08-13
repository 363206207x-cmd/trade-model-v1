package org.example.trademodel.ai;

/** State of one formal AI collection. */
public enum AiCollectionState {
    FOUND,
    NONE_FOUND,
    INSUFFICIENT_DATA,
    SOURCE_UNAVAILABLE,
    STALE;

    public boolean permitsEmptyCollection() {
        return this != FOUND;
    }
}
