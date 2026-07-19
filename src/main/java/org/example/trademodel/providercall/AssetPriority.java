package org.example.trademodel.providercall;

public enum AssetPriority {
    P0_POSITION(0),
    P1_WATCHLIST(2),
    P2_CANDIDATE(1),
    P3_DISCOVERY(3);

    private final int rank;

    AssetPriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public static AssetPriority highest(AssetPriority left, AssetPriority right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.rank <= right.rank ? left : right;
    }
}
