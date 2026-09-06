package org.example.trademodel.v41;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class V41ConfidenceCalibrationPolicyTest {

    @Test
    void appliesQualityCapsAndNeverUsesCoverageTwice() {
        var mediumQuality = V41ConfidenceCalibrationPolicy.calibrate(
                new V41ConfidenceCalibrationPolicy.Input(92, 78, 74, 68, 500, 0.16, 0.05));
        var highQuality = V41ConfidenceCalibrationPolicy.calibrate(
                new V41ConfidenceCalibrationPolicy.Input(98, 88, 82, 76, 500, 0.12, 0.04));

        assertThat(mediumQuality.version()).isEqualTo("V41-CONFIDENCE-CALIBRATED-1");
        assertThat(mediumQuality.confidence()).isLessThanOrEqualTo(75);
        assertThat(highQuality.confidence()).isLessThanOrEqualTo(95);
        assertThat(highQuality.confidence()).isLessThan(100);
    }

    @Test
    void insufficientQualityOrSamplesReturnsUnavailable() {
        assertThat(V41ConfidenceCalibrationPolicy.calibrate(
                new V41ConfidenceCalibrationPolicy.Input(84, 90, 90, 90, 500, 0.1, 0.02)).available()).isFalse();
        assertThat(V41ConfidenceCalibrationPolicy.calibrate(
                new V41ConfidenceCalibrationPolicy.Input(98, 90, 90, 90, 20, 0.1, 0.02)).available()).isFalse();
    }
}
