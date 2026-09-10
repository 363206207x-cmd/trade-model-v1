package org.example.trademodel.assetcard;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardBetaCalibrationTest {
    @Test void identityAndIndependentSidesUseThePublishedEquation() {
        var identity = new AssetCardBetaCalibration.Parameters(1, 1, 0, 1e-7);
        assertThat(AssetCardBetaCalibration.calibrate(.2, identity)).isCloseTo(.2, within(1e-12));
        var shortSide = new AssetCardBetaCalibration.Parameters(.8, 1.3, -.2, 1e-7);
        assertThat(AssetCardBetaCalibration.calibrate(.2, shortSide))
                .isCloseTo(1 / (1 + Math.exp(-(.8 * Math.log(.2) - 1.3 * Math.log(.8) - .2))), within(1e-12));
        assertThat(AssetCardBetaCalibration.calibrate(0, identity)).isBetween(0.0, 1.0);
        assertThat(AssetCardBetaCalibration.calibrate(1, identity)).isBetween(0.0, 1.0);
    }
    @Test void invalidInputsNeverProduceFallbackConfidence() {
        var identity = new AssetCardBetaCalibration.Parameters(1, 1, 0, 1e-7);
        assertThatThrownBy(() -> AssetCardBetaCalibration.calibrate(Double.NaN, identity)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AssetCardBetaCalibration.Parameters(1, 1, 0, .5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AssetCardBetaCalibration.Parameters(-1, 1, 0, 1e-7)).isInstanceOf(IllegalArgumentException.class);
    }
}
