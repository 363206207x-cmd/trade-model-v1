package org.example.trademodel.v41;

/** Deterministic projection of a versioned walk-forward calibration result. */
public final class V41ConfidenceCalibrationPolicy {
    public static final String VERSION = "V41-CONFIDENCE-CALIBRATED-1";
    public static final int MINIMUM_SAMPLE_COUNT = 60;

    private V41ConfidenceCalibrationPolicy() {
    }

    public static Result calibrate(Input input) {
        if (input == null || input.dataQuality() < 85 || input.sampleCount() < MINIMUM_SAMPLE_COUNT) {
            return new Result(VERSION, false, null, input == null ? 0 : input.sampleCount(),
                    input == null ? null : input.brierScore(), input == null ? null : input.calibrationError(),
                    input != null && input.dataQuality() < 85 ? "DATA_QUALITY_BELOW_85" : "CALIBRATION_SAMPLE_INSUFFICIENT");
        }
        double raw = 0.50 * input.walkForwardHitRate()
                + 0.30 * input.multiTimeframeAgreement()
                + 0.20 * input.crossSourceConsistency();
        int cap = input.dataQuality() >= 95 ? 95 : 75;
        int calibrated = Math.max(1, Math.min(cap, (int) Math.round(raw)));
        return new Result(VERSION, true, calibrated, input.sampleCount(), input.brierScore(),
                input.calibrationError(), null);
    }

    public record Input(int dataQuality, double walkForwardHitRate,
                        double multiTimeframeAgreement, double crossSourceConsistency,
                        int sampleCount, Double brierScore, Double calibrationError) {
    }

    public record Result(String version, boolean available, Integer confidence, int sampleCount,
                         Double brierScore, Double calibrationError, String unavailableReason) {
    }
}
