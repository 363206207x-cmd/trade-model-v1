package org.example.trademodel.providercall;

public enum AssetPriority {
    P0_POSITION(0),
    P1_CORE(1),
    P2_CANDIDATE(2),
    P3_POOL(3);

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
