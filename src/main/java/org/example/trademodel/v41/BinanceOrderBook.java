package org.example.trademodel.v41;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Binance USD-M depth synchronizer. A snapshot is accepted first, then buffered incremental
 * events are replayed using Binance's {@code U/u/pu} continuity contract. A gap clears the
 * visible book so stale depth can never be presented as current liquidity.
 */
final class BinanceOrderBook {
    private static final int MAX_BUFFERED_DELTAS = 1_000;

    private final NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
    private final ArrayDeque<DepthDelta> pending = new ArrayDeque<>();
    private long lastUpdateId;
    private boolean initialized;

    synchronized ApplyResult apply(DepthDelta delta) {
        if (delta == null || delta.finalUpdateId() <= 0) return ApplyResult.IGNORED;
        if (!initialized) {
            if (pending.size() >= MAX_BUFFERED_DELTAS) pending.removeFirst();
            pending.addLast(delta);
            return ApplyResult.BUFFERED;
        }
        return applyIncrement(delta);
    }

    synchronized ApplyResult applySnapshot(long snapshotUpdateId,
                                            List<Level> snapshotBids,
                                            List<Level> snapshotAsks) {
        bids.clear();
        asks.clear();
        applyLevels(bids, snapshotBids);
        applyLevels(asks, snapshotAsks);
        lastUpdateId = snapshotUpdateId;
        initialized = snapshotUpdateId > 0;
        if (!initialized) return ApplyResult.GAP;

        while (!pending.isEmpty()) {
            DepthDelta delta = pending.removeFirst();
            if (delta.finalUpdateId() <= lastUpdateId) continue;
            ApplyResult result = applyIncrement(delta);
            if (result == ApplyResult.GAP) return result;
        }
        return ApplyResult.APPLIED;
    }

    synchronized void invalidate() {
        initialized = false;
        lastUpdateId = 0;
        bids.clear();
        asks.clear();
        pending.clear();
    }

    synchronized Snapshot snapshot(long eventTime) {
        if (!initialized || bids.isEmpty() || asks.isEmpty()) return null;
        BigDecimal bid = bids.firstKey();
        BigDecimal ask = asks.firstKey();
        if (bid.signum() <= 0 || ask.compareTo(bid) < 0) return null;
        return new Snapshot(bid, ask, effectiveDepth(bids, bid, true), effectiveDepth(asks, ask, false),
                lastUpdateId, eventTime);
    }

    synchronized boolean initialized() {
        return initialized;
    }

    private ApplyResult applyIncrement(DepthDelta delta) {
        if (delta.finalUpdateId() <= lastUpdateId) return ApplyResult.IGNORED;
        long expected = lastUpdateId + 1;
        boolean firstEventCoversExpected = delta.firstUpdateId() <= expected
                && delta.finalUpdateId() >= expected;
        boolean previousMatches = delta.previousFinalUpdateId() == null
                || delta.previousFinalUpdateId() == lastUpdateId;
        if (!firstEventCoversExpected || !previousMatches) {
            invalidate();
            return ApplyResult.GAP;
        }
        applyLevels(bids, delta.bids());
        applyLevels(asks, delta.asks());
        lastUpdateId = delta.finalUpdateId();
        return ApplyResult.APPLIED;
    }

    private static void applyLevels(NavigableMap<BigDecimal, BigDecimal> side, List<Level> levels) {
        if (levels == null) return;
        for (Level level : levels) {
            if (level == null || level.price() == null || level.quantity() == null
                    || level.price().signum() <= 0 || level.quantity().signum() < 0) continue;
            if (level.quantity().signum() == 0) side.remove(level.price());
            else side.put(level.price(), level.quantity());
        }
    }

    private static BigDecimal effectiveDepth(NavigableMap<BigDecimal, BigDecimal> side,
                                             BigDecimal best, boolean bids) {
        BigDecimal boundary = bids
                ? best.multiply(new BigDecimal("0.999"))
                : best.multiply(new BigDecimal("1.001"));
        BigDecimal notional = BigDecimal.ZERO;
        for (var level : side.entrySet()) {
            boolean inside = bids ? level.getKey().compareTo(boundary) >= 0
                    : level.getKey().compareTo(boundary) <= 0;
            if (!inside) break;
            notional = notional.add(level.getKey().multiply(level.getValue()));
        }
        return notional;
    }

    enum ApplyResult { APPLIED, BUFFERED, IGNORED, GAP }

    record Level(BigDecimal price, BigDecimal quantity) { }

    record DepthDelta(long firstUpdateId, long finalUpdateId, Long previousFinalUpdateId,
                      List<Level> bids, List<Level> asks) {
        DepthDelta {
            bids = bids == null ? List.of() : List.copyOf(bids);
            asks = asks == null ? List.of() : List.copyOf(asks);
        }
    }

    record Snapshot(BigDecimal bid, BigDecimal ask, BigDecimal bidDepth10Bps,
                    BigDecimal askDepth10Bps, long sequence, long eventTime) { }

}
