package org.example.trademodel.assetcard;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static org.example.trademodel.assetcard.AssetCardProperties.ModelMode;
import static org.example.trademodel.assetcard.AssetCardSnapshot.Direction;
import static org.example.trademodel.assetcard.AssetCardSnapshot.Signal;

/** Pure card-only state transition. The caller owns inference, scheduling, persistence and risk invalidation. */
public final class AssetCardSignalService {
    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);

    public record ModelIdentity(String featureVersion, String modelVersion, String calibrationVersion,
                                String thresholdVersion, ModelMode mode, boolean displayAllowed, boolean bundleReady) {}

    /** Audit-only prediction never serves as a second visible confidence source. */
    public record State(String symbol, Direction confirmedDirection, Direction pendingDirection, int confirmationCount,
                        Instant lastClosed5mAt, ModelIdentity identity, AssetCardModelBundle.Prediction auditPrediction,
                        Signal signal, Instant invalidatedAt, String reason) {
        public State {
            symbol = normalize(symbol);
            Objects.requireNonNull(signal, "signal");
            if (confirmationCount < 0 || confirmationCount > 2) throw new IllegalArgumentException("Invalid confirmation count");
        }
        public static State initial(String symbol) {
            return new State(symbol, null, null, 0, null, null, null,
                    Signal.unavailable("INSUFFICIENT_DATA", null), null, "NO_CLOSED_CARD_SIGNAL");
        }
    }

    /** Consumes an already-produced prediction from exactly this verified bundle; never calls predict(). */
    public State evaluate(State previous, AssetCardFeatureService.Frame frame, AssetCardModelBundle validatedBundle,
                          AssetCardModelBundle.Prediction prediction, ModelMode mode, Set<String> canarySymbols) {
        Objects.requireNonNull(previous, "previous state; use State.initial(symbol)");
        if (frame != null && !previous.symbol().equals(normalize(frame.symbol())))
            throw new IllegalArgumentException("Card signal symbol mismatch");
        ModelMode effectiveMode = mode == null ? ModelMode.SHADOW : mode;
        boolean displayAllowed = effectiveMode == ModelMode.ACTIVE || effectiveMode == ModelMode.CANARY
                && canarySymbols != null && canarySymbols.contains(previous.symbol());
        boolean bundleReady = validatedBundle != null && validatedBundle.validated();
        ModelIdentity identity = new ModelIdentity(frame == null ? null : frame.featureVersion(),
                validatedBundle == null ? null : validatedBundle.modelVersion(),
                validatedBundle == null ? null : validatedBundle.calibrationVersion(),
                validatedBundle == null ? null : validatedBundle.thresholdVersion(), effectiveMode, displayAllowed, bundleReady);
        boolean sameIdentity = identity.equals(previous.identity());

        if (!validTimes(frame)) return unavailable(previous, identity, prediction, previous.lastClosed5mAt(),
                previous.signal().signalAsOf(), "INSUFFICIENT_DATA");
        Instant closed = frame.closed5mAt();
        if (previous.lastClosed5mAt() != null && !closed.isAfter(previous.lastClosed5mAt())) {
            // Duplicate/older bars cannot update confidence, clock, or accumulate a second confirmation.
            // A revoked mode/bundle may only remove visibility; it never grants a result from an old bar.
            return sameIdentity ? previous : unavailable(previous, identity, prediction, previous.lastClosed5mAt(),
                    previous.signal().signalAsOf(), "MODEL_CONTEXT_CHANGED");
        }
        if (!frame.ready() || !AssetCardFeatureService.FEATURE_VERSION.equals(frame.featureVersion())
                || !AssetCardFeatureService.FEATURE_NAMES.equals(frame.featureNames())
                || frame.vector().size() != AssetCardFeatureService.FEATURE_NAMES.size())
            return unavailable(previous, identity, prediction, closed, frame.signalAsOf(), "INSUFFICIENT_DATA");
        if (!boundPrediction(validatedBundle, prediction, bundleReady))
            return unavailable(previous, identity, prediction, closed, frame.signalAsOf(), "MODEL_UNAVAILABLE");

        State current = sameIdentity ? previous : new State(previous.symbol(), null, null, 0,
                previous.lastClosed5mAt(), identity, null, Signal.unavailable("CONFIRMING", frame.signalAsOf()), null,
                "MODEL_CONTEXT_CHANGED");
        if (current.invalidatedAt() != null && !closed.isAfter(current.invalidatedAt()))
            return new State(current.symbol(), current.confirmedDirection(), null, 0, closed, identity, prediction,
                    current.signal(), current.invalidatedAt(), "WAITING_POST_INVALIDATION_CLOSE");

        Direction candidate = direction(prediction.pLong(), prediction.pShort(), prediction.thresholds());
        boolean alreadyConfirmed = current.confirmedDirection() == candidate && current.invalidatedAt() == null;
        boolean consecutive = current.lastClosed5mAt() != null
                && Duration.between(current.lastClosed5mAt(), closed).equals(FIVE_MINUTES);
        int count = consecutive && candidate == current.pendingDirection() ? current.confirmationCount() + 1 : 1;
        if (alreadyConfirmed || count >= 2) {
            Signal signal = displayAllowed ? new Signal(candidate, "VALID", null, prediction.pLong(), prediction.pShort(),
                    frame.oneHourState(), frame.fourHourTrend(), frame.signalAsOf())
                    : Signal.unavailable(hiddenStatus(identity), frame.signalAsOf());
            return new State(current.symbol(), candidate, null, 0, closed, identity, prediction, signal, null, null);
        }
        // A single opposing bar cannot replace the previously confirmed direction or relabel its old probability.
        Signal signal = !displayAllowed ? Signal.unavailable(hiddenStatus(identity), frame.signalAsOf())
                : current.confirmedDirection() == null ? Signal.unavailable("CONFIRMING", frame.signalAsOf()) : current.signal();
        return new State(current.symbol(), current.confirmedDirection(), candidate, count, closed, identity,
                prediction, signal, current.invalidatedAt(), "WAITING_SECOND_CONSECUTIVE_CLOSE");
    }

    /** Only the caller's explicit hard-risk result invokes this; no opposite direction is synthesized. */
    public State invalidate(State previous, Instant at) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(at, "invalidation time");
        if (previous.signal().direction() == null || "INVALIDATED".equals(previous.signal().status())
                || previous.signal().signalAsOf() != null && at.isBefore(previous.signal().signalAsOf())) return previous;
        return new State(previous.symbol(), previous.confirmedDirection(), null, 0, previous.lastClosed5mAt(),
                previous.identity(), previous.auditPrediction(), previous.signal().invalidated(), at, "HARD_RISK_INVALIDATION");
    }

    private static State unavailable(State previous, ModelIdentity identity, AssetCardModelBundle.Prediction prediction,
                                     Instant closed, Instant at, String reason) {
        String status = identity.displayAllowed() ? reason : hiddenStatus(identity);
        return new State(previous.symbol(), null, null, 0, closed, identity, prediction,
                Signal.unavailable(status, at), null, reason);
    }

    private static String hiddenStatus(ModelIdentity identity) {
        return identity.mode() == ModelMode.SHADOW ? "SHADOW"
                : identity.mode() == ModelMode.LEGACY ? "LEGACY" : "CANARY_DISABLED";
    }

    private static boolean boundPrediction(AssetCardModelBundle bundle, AssetCardModelBundle.Prediction prediction,
                                           boolean bundleReady) {
        return bundleReady && prediction != null && prediction.available()
                && probability(prediction.pLong()) && probability(prediction.pShort()) && prediction.thresholds() != null
                && nonBlank(prediction.modelVersion()) && nonBlank(prediction.calibrationVersion()) && nonBlank(prediction.thresholdVersion())
                && Objects.equals(bundle.modelVersion(), prediction.modelVersion())
                && Objects.equals(bundle.calibrationVersion(), prediction.calibrationVersion())
                && Objects.equals(bundle.thresholdVersion(), prediction.thresholdVersion())
                && Objects.equals(bundle.thresholds(), prediction.thresholds());
    }

    private static Direction direction(double pLong, double pShort, AssetCardModelBundle.Thresholds thresholds) {
        double gap = Math.abs(pLong - pShort), best = Math.max(pLong, pShort);
        boolean longSide = pLong > pShort;
        if (qualifies(best, gap, thresholds.strong())) return longSide ? Direction.STRONG_LONG : Direction.STRONG_SHORT;
        if (qualifies(best, gap, thresholds.normal())) return longSide ? Direction.LONG : Direction.SHORT;
        if (qualifies(best, gap, thresholds.weak())) return longSide ? Direction.WEAK_LONG : Direction.WEAK_SHORT;
        return gap <= thresholds.rangeMaxGap() && best <= thresholds.rangeMaxProbability() ? Direction.RANGE : Direction.WATCH;
    }

    private static boolean qualifies(double probability, double gap, AssetCardModelBundle.Tier tier) {
        return probability >= tier.minProbability() && gap >= tier.minGap();
    }
    private static boolean probability(Double value) { return value != null && Double.isFinite(value) && value >= 0 && value <= 1; }
    private static boolean nonBlank(String value) { return value != null && !value.isBlank(); }
    private static boolean validTimes(AssetCardFeatureService.Frame frame) {
        if (frame == null || frame.closed5mAt() == null || frame.signalAsOf() == null || frame.availableAt() == null
                || frame.closed5mAt().isAfter(frame.signalAsOf()) || frame.availableAt().isAfter(frame.signalAsOf())
                || frame.availableAt().isBefore(frame.closed5mAt())) return false;
        long boundary = Math.floorMod(frame.closed5mAt().toEpochMilli(), FIVE_MINUTES.toMillis());
        return (boundary == 0 || boundary == FIVE_MINUTES.toMillis() - 1)
                && Duration.between(frame.closed5mAt(), frame.signalAsOf()).compareTo(Duration.ofSeconds(15)) <= 0;
    }
    private static String normalize(String symbol) {
        if (symbol == null) throw new IllegalArgumentException("Missing card symbol");
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{2,32}")) throw new IllegalArgumentException("Invalid card symbol");
        return normalized;
    }
}
