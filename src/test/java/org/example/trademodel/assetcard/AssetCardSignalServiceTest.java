package org.example.trademodel.assetcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.dmlc.xgboost4j.java.Booster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.example.trademodel.assetcard.AssetCardProperties.ModelMode.*;
import static org.example.trademodel.assetcard.AssetCardSnapshot.Direction.*;

/** Synthetic decision fixtures only: no native inference, training, provider, database or production readiness. */
@org.junit.jupiter.api.Tag("core-regression")
class AssetCardSignalServiceTest {
    private static final Instant CLOSE = Instant.parse("2026-09-10T00:04:59.999Z");
    private static final AssetCardModelBundle.Thresholds THRESHOLDS = new AssetCardModelBundle.Thresholds(
            new AssetCardModelBundle.Tier(.60, .10), new AssetCardModelBundle.Tier(.70, .20),
            new AssetCardModelBundle.Tier(.85, .40), .03, .50);
    private final AssetCardSignalService service = new AssetCardSignalService();
    private final List<Booster> fixtureBoosters = new ArrayList<>();

    @AfterEach void signalTransitionsNeverUseNativeModels() {
        verifyNoInteractions(fixtureBoosters.toArray());
    }

    @Test void allEightDirectionsUseOnlyBoundCalibratedThresholdsAfterTwoClosedBars() {
        var cases = Map.of(STRONG_LONG, List.of(.92, .10), LONG, List.of(.75, .25), WEAK_LONG, List.of(.62, .45),
                STRONG_SHORT, List.of(.10, .92), SHORT, List.of(.25, .75), WEAK_SHORT, List.of(.45, .62),
                RANGE, List.of(.40, .41), WATCH, List.of(.65, .64));
        for (var entry : cases.entrySet()) {
            var bundle = bundle("fixture-model-1");
            var prediction = prediction(bundle, entry.getValue().get(0), entry.getValue().get(1));
            var first = evaluate(AssetCardSignalService.State.initial("BTCUSDT"), 0, bundle, prediction, ACTIVE, Set.of());
            assertThat(first.signal().direction()).isNull();
            assertThat(first.signal().calibratedConfidence()).isNull();
            assertThat(first.confirmationCount()).isEqualTo(1);
            var second = evaluate(first, 1, bundle, prediction, ACTIVE, Set.of());
            assertThat(second.signal().direction()).isEqualTo(entry.getKey());
            assertThat(second.signal().status()).isEqualTo("VALID");
            assertThat(second.signal().oneHourState()).isEqualTo("OPPORTUNITY");
            assertThat(second.signal().fourHourTrend()).isEqualTo("LONG");
            if (entry.getKey().directional()) {
                double probability = entry.getKey().longSide() ? entry.getValue().get(0) : entry.getValue().get(1);
                assertThat(second.signal().calibratedConfidence()).isEqualTo((int) Math.round(probability * 100));
            } else assertThat(second.signal().calibratedConfidence()).isNull();
        }
    }

    @Test void directionChangesRequireTwoDifferentConsecutiveClosedFiveMinuteBars() {
        var bundle = bundle("fixture-model-1");
        var original = confirmed(bundle, .75, .25);
        var first = evaluate(original, 2, bundle, prediction(bundle, .25, .75), ACTIVE, Set.of());
        assertThat(first.signal()).isEqualTo(original.signal());
        assertThat(first.pendingDirection()).isEqualTo(SHORT);
        var duplicate = evaluate(first, 2, bundle, prediction(bundle, .99, .01), ACTIVE, Set.of());
        assertThat(duplicate).isSameAs(first);
        assertThat(evaluate(first, 1, bundle, prediction(bundle, .99, .01), ACTIVE, Set.of())).isSameAs(first);
        var switched = evaluate(first, 3, bundle, prediction(bundle, .25, .75), ACTIVE, Set.of());
        assertThat(switched.signal().direction()).isEqualTo(SHORT);
        assertThat(switched.signal().calibratedConfidence()).isEqualTo(75);
        assertThat(switched.signal().signalAsOf()).isEqualTo(frame(3).signalAsOf());
    }

    @Test void gapsAndAlternatingCandidatesCannotAccumulateConfirmations() {
        var bundle = bundle("fixture-model-1");
        var first = evaluate(AssetCardSignalService.State.initial("BTCUSDT"), 0, bundle, prediction(bundle, .75, .25), ACTIVE, Set.of());
        var gap = evaluate(first, 2, bundle, prediction(bundle, .75, .25), ACTIVE, Set.of());
        assertThat(gap.confirmationCount()).isEqualTo(1);
        assertThat(gap.signal().direction()).isNull();
        var alternate = evaluate(gap, 3, bundle, prediction(bundle, .25, .75), ACTIVE, Set.of());
        assertThat(alternate.confirmationCount()).isEqualTo(1);
        assertThat(alternate.signal().direction()).isNull();
        assertThat(evaluate(alternate, 4, bundle, prediction(bundle, .25, .75), ACTIVE, Set.of()).signal().direction()).isEqualTo(SHORT);
    }

    @Test void shadowLegacyAndUnlistedCanaryRetainAuditButNeverExposeModelNumbers() {
        var bundle = bundle("fixture-model-1");
        for (var mode : List.of(SHADOW, LEGACY, CANARY)) {
            var first = evaluate(AssetCardSignalService.State.initial("BTCUSDT"), 0, bundle, prediction(bundle, .92, .1), mode, Set.of("ETHUSDT"));
            var second = evaluate(first, 1, bundle, prediction(bundle, .92, .1), mode, Set.of("ETHUSDT"));
            assertThat(second.signal().direction()).isNull();
            assertThat(second.signal().calibratedConfidence()).isNull();
            assertThat(second.signal().pLong()).isNull();
            assertThat(second.signal().pShort()).isNull();
            assertThat(second.auditPrediction().pLong()).isEqualTo(.92);
            if (mode == SHADOW) assertThat(second.signal().status()).isEqualTo("SHADOW");
        }
        var allowed = evaluate(AssetCardSignalService.State.initial("BTCUSDT"), 0, bundle, prediction(bundle, .92, .1), CANARY, Set.of("BTCUSDT"));
        assertThat(evaluate(allowed, 1, bundle, prediction(bundle, .92, .1), CANARY, Set.of("BTCUSDT")).signal().direction()).isEqualTo(STRONG_LONG);
    }

    @Test void bundleOrModeChangesDoNotAttachNewConfidenceToAnOldDirection() {
        var oldBundle = bundle("fixture-model-1");
        var original = confirmed(oldBundle, .75, .25);
        var newBundle = bundle("fixture-model-2");
        var changed = evaluate(original, 2, newBundle, prediction(newBundle, .1, .92), ACTIVE, Set.of());
        assertThat(changed.signal().direction()).isNull();
        assertThat(changed.signal().calibratedConfidence()).isNull();
        assertThat(changed.confirmationCount()).isEqualTo(1);
        assertThat(evaluate(changed, 3, newBundle, prediction(newBundle, .1, .92), ACTIVE, Set.of()).signal().direction()).isEqualTo(STRONG_SHORT);
        var shadow = evaluate(original, 2, oldBundle, prediction(oldBundle, .1, .92), SHADOW, Set.of());
        assertThat(shadow.signal().status()).isEqualTo("SHADOW");
        assertThat(shadow.signal().direction()).isNull();
        assertThat(shadow.signal().calibratedConfidence()).isNull();
    }

    @Test void absentModelUnboundPredictionAndMissingDataFailClosedWithoutWatchOrRawConfidence() {
        var bundle = bundle("fixture-model-1");
        var unavailable = AssetCardModelBundle.unavailable("NO_REAL_HISTORY");
        var initial = AssetCardSignalService.State.initial("BTCUSDT");
        assertMissing(evaluate(initial, 0, unavailable, prediction(bundle, .99, .01), ACTIVE, Set.of()));
        var forged = new AssetCardModelBundle.Prediction(true, .99, .99, .99, .01,
                "another-model", bundle.calibrationVersion(), bundle.thresholdVersion(), THRESHOLDS, List.of());
        assertMissing(evaluate(initial, 0, bundle, forged, ACTIVE, Set.of()));
        var rawOnly = new AssetCardModelBundle.Prediction(true, .99, .01, null, null,
                bundle.modelVersion(), bundle.calibrationVersion(), bundle.thresholdVersion(), THRESHOLDS, List.of());
        assertMissing(evaluate(initial, 0, bundle, rawOnly, ACTIVE, Set.of()));
        var missing = frame(0);
        missing = new AssetCardFeatureService.Frame(missing.symbol(), missing.closed5mAt(), missing.signalAsOf(), missing.availableAt(),
                missing.featureVersion(), missing.featureNames(), missing.vector(), false, List.of("MISSING_CORE_DATA"),
                missing.oneHourState(), missing.fourHourTrend(), missing.atr(), missing.structuralSupport(), missing.structuralResistance(), missing.realInputs());
        assertMissing(service.evaluate(initial, missing, bundle, prediction(bundle, .99, .01), ACTIVE, Set.of()));
    }

    @Test void invalidationRetainsDirectionAndNeedsFreshConfirmationBeforeRestoration() {
        var bundle = bundle("fixture-model-1");
        var original = confirmed(bundle, .75, .25);
        var invalid = service.invalidate(original, CLOSE.plusSeconds(320));
        assertThat(invalid.signal().direction()).isEqualTo(LONG);
        assertThat(invalid.signal().status()).isEqualTo("INVALIDATED");
        assertThat(invalid.signal().calibratedConfidence()).isNull();
        assertThat(invalid.signal().pLong()).isNull();
        assertThat(invalid.invalidatedAt()).isEqualTo(CLOSE.plusSeconds(320));
        assertThat(service.invalidate(invalid, CLOSE.plusSeconds(321))).isSameAs(invalid);
        var one = evaluate(invalid, 2, bundle, prediction(bundle, .75, .25), ACTIVE, Set.of());
        assertThat(one.signal().status()).isEqualTo("INVALIDATED");
        assertThat(evaluate(one, 3, bundle, prediction(bundle, .75, .25), ACTIVE, Set.of()).signal().status()).isEqualTo("VALID");
    }

    @Test void stateIsJacksonAuditableAndCannotCrossSymbols() throws Exception {
        var bundle = bundle("fixture-model-1");
        var state = confirmed(bundle, .75, .25);
        var json = new ObjectMapper().findAndRegisterModules();
        assertThat(json.readValue(json.writeValueAsBytes(state), AssetCardSignalService.State.class)).isEqualTo(state);
        assertThatThrownBy(() -> service.evaluate(AssetCardSignalService.State.initial("ETHUSDT"), frame(0), bundle,
                prediction(bundle, .75, .25), ACTIVE, Set.of())).isInstanceOf(IllegalArgumentException.class);
    }

    private AssetCardSignalService.State confirmed(AssetCardModelBundle bundle, double pLong, double pShort) {
        var state = evaluate(AssetCardSignalService.State.initial("BTCUSDT"), 0, bundle, prediction(bundle, pLong, pShort), ACTIVE, Set.of());
        return evaluate(state, 1, bundle, prediction(bundle, pLong, pShort), ACTIVE, Set.of());
    }
    private AssetCardSignalService.State evaluate(AssetCardSignalService.State state, int index, AssetCardModelBundle bundle,
                                                  AssetCardModelBundle.Prediction prediction, AssetCardProperties.ModelMode mode, Set<String> canary) {
        return service.evaluate(state, frame(index), bundle, prediction, mode, canary);
    }
    private static void assertMissing(AssetCardSignalService.State state) {
        assertThat(state.signal().direction()).isNull();
        assertThat(state.signal().calibratedConfidence()).isNull();
        assertThat(state.signal().pLong()).isNull();
        assertThat(state.signal().pShort()).isNull();
    }
    private AssetCardModelBundle bundle(String version) {
        // Test-only constructor access keeps the production final class and verified-loading API unchanged.
        // The non-null models are mocks; no native methods, loading, inference or training may be invoked.
        var longModel = mock(Booster.class); var shortModel = mock(Booster.class);
        fixtureBoosters.add(longModel); fixtureBoosters.add(shortModel);
        var calibration = new AssetCardBetaCalibration.Parameters(1, 1, 0, 1e-12);
        try {
            var constructor = AssetCardModelBundle.class.getDeclaredConstructor(Booster.class, Booster.class,
                    AssetCardBetaCalibration.Parameters.class, AssetCardBetaCalibration.Parameters.class,
                    String.class, String.class, String.class, AssetCardModelBundle.Thresholds.class, Map.class, Set.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(longModel, shortModel, calibration, calibration, version,
                    "fixture-calibration", "fixture-thresholds", THRESHOLDS, Map.of(), Set.of("BTCUSDT"), null);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Test-only bundle fixture no longer matches the immutable bundle constructor", failure);
        }
    }
    private static AssetCardModelBundle.Prediction prediction(AssetCardModelBundle bundle, double pLong, double pShort) {
        return new AssetCardModelBundle.Prediction(true, .99, .99, pLong, pShort, bundle.modelVersion(),
                bundle.calibrationVersion(), bundle.thresholdVersion(), bundle.thresholds(), List.of());
    }
    private static AssetCardFeatureService.Frame frame(int index) {
        Instant closed = CLOSE.plusSeconds(index * 300L), asOf = closed.plusSeconds(1);
        return new AssetCardFeatureService.Frame("BTCUSDT", closed, asOf, asOf,
                AssetCardFeatureService.FEATURE_VERSION, AssetCardFeatureService.FEATURE_NAMES,
                Collections.nCopies(AssetCardFeatureService.FEATURE_NAMES.size(), 1.0), true, List.of(),
                "OPPORTUNITY", "LONG", 1.0, 90.0, 110.0, Map.of());
    }
}
