package org.example.trademodel.service;

/**
 * Server-side push eligibility gate for the V1 watchlist boundary.
 */
public interface WatchlistPushEligibilityService {

    /**
     * Returns true only when the symbol is explicitly allowed by the backend watchlist source.
     */
    boolean isEligibleForDirectionalPush(String symbol);
}
