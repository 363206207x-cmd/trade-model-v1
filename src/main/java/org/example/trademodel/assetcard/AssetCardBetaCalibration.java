package org.example.trademodel.assetcard;

/** Offline-fitted beta parameters. This class never fits, substitutes or blends probabilities. */
public final class AssetCardBetaCalibration {
    private AssetCardBetaCalibration() {}

    public record Parameters(double a, double b, double c, double epsilon) {
        public Parameters {
            if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c)
                    || a < 0 || b < 0 || a + b == 0
                    || !Double.isFinite(epsilon) || epsilon <= 0 || epsilon >= .5)
                throw new IllegalArgumentException("Invalid monotone beta calibration parameters");
        }
    }

    public static double calibrate(double raw, Parameters parameters) {
        if (parameters == null || !Double.isFinite(raw) || raw < 0 || raw > 1)
            throw new IllegalArgumentException("A real raw probability and independent calibrator are required");
        double p = Math.max(parameters.epsilon(), Math.min(1 - parameters.epsilon(), raw));
        double logit = parameters.a() * Math.log(p) - parameters.b() * Math.log1p(-p) + parameters.c();
        return logit >= 0 ? 1 / (1 + Math.exp(-logit)) : Math.exp(logit) / (1 + Math.exp(logit));
    }
}
