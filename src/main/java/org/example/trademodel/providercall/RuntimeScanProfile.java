package org.example.trademodel.providercall;

public enum RuntimeScanProfile {
    LOW(0),
    STANDARD(1),
    HIGH(2),
    EMERGENCY(3);

    private final int rank;

    RuntimeScanProfile(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public static RuntimeScanProfile max(RuntimeScanProfile... profiles) {
        RuntimeScanProfile result = LOW;
        if (profiles == null) return result;
        for (RuntimeScanProfile profile : profiles) {
            if (profile != null && profile.rank > result.rank) result = profile;
        }
        return result;
    }

    public RuntimeScanProfile oneLevelDown() {
        return switch (this) {
            case EMERGENCY -> HIGH;
            case HIGH -> STANDARD;
            case STANDARD, LOW -> LOW;
        };
    }
}
