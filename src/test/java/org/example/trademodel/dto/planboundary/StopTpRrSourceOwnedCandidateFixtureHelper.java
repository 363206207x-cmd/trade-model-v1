package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFamily;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixtureStatus;

public final class StopTpRrSourceOwnedCandidateFixtureHelper {

    public static final String REVIEW_ONLY = "REVIEW_ONLY";

    private StopTpRrSourceOwnedCandidateFixtureHelper() {
    }

    public static StopFixtureBuilder stop() {
        return new StopFixtureBuilder();
    }

    public static TpFixtureBuilder tp(TpFamily family) {
        return new TpFixtureBuilder(family);
    }

    public static RrFixtureBuilder rr(EntryFixture entryFixture, StopFixture stopFixture, TpFixture tpFixture) {
        return new RrFixtureBuilder(entryFixture, stopFixture, tpFixture);
    }

    public static StopFixture completeStopFixture() {
        return stop().build();
    }

    public static TpFixture completeTpFixture(TpFamily family) {
        return tp(family).build();
    }

    public static RrFixture completeRrFixture() {
        return rr(
                EntrySourceOwnedCandidateFixtureHelper.completeFixture(EntryFamily.STRUCTURE_CONFIRMATION_ZONE),
                completeStopFixture(),
                completeTpFixture(TpFamily.STRUCTURE_TARGET)
        ).build();
    }

    public enum FixtureStatus {
        INCOMPLETE,
        BLOCKED,
        FIXTURE_VALID_CANDIDATE
    }

    public enum StopFamily {
        STRUCTURAL_INVALIDATION_WITH_BUFFER
    }

    public enum TpFamily {
        STRUCTURE_TARGET,
        LIQUIDITY_TARGET,
        PRIOR_HIGH_LOW,
        RR_LADDER
    }

    public enum ForbiddenSource {
        AI_TEXT("ai_text_source"),
        DASHBOARD_TEXT("dashboard_text_source"),
        LATEST_PRICE_ONLY("latest_price_only"),
        SINGLE_KLINE_ONLY("single_kline_only"),
        AGGREGATE_SCORE_ONLY("aggregate_score_only"),
        ORDER_EXECUTION_BACKFILL("order_execution_backfill"),
        STRONG_REVERSAL_DIRECT_REVERSE("strong_reversal_direct_reverse"),
        WICK_PIN_BAR_DIRECT_TREND_REVERSAL("wick_pin_bar_direct_trend_reversal"),
        LIQUIDITY_STRESS_STAMPEDE_OPPORTUNITY_PUSH("liquidity_stress_stampede_opportunity_push");

        private final String blockerEvidence;

        ForbiddenSource(String blockerEvidence) {
            this.blockerEvidence = blockerEvidence;
        }

        public String blockerEvidence() {
            return blockerEvidence;
        }
    }

    public enum RiskActionGuardBlocker {
        HIGH_RISK_LIQUIDITY_DETERIORATING("risk_action_guard_liquidity_deteriorating"),
        HIGH_RISK_STAMPEDE("risk_action_guard_stampede"),
        SHORT_TERM_WICK_PIN_BAR_DIRECT_REVERSAL("risk_action_guard_wick_pin_bar_direct_reversal"),
        MISSING_EVENT_DATA("risk_action_guard_missing_event_data");

        private final String blockerEvidence;

        RiskActionGuardBlocker(String blockerEvidence) {
            this.blockerEvidence = blockerEvidence;
        }

        public String blockerEvidence() {
            return blockerEvidence;
        }
    }

    public enum EntryStopDistanceState {
        PRESENT,
        MISSING,
        ZERO,
        NEGATIVE,
        AMBIGUOUS,
        STALE,
        UNSUPPORTED
    }

    public record SourceWindow(String value, boolean stale) {

        public static SourceWindow fresh(String value) {
            return new SourceWindow(value, false);
        }

        public static SourceWindow stale(String value) {
            return new SourceWindow(value, true);
        }
    }

    public record NumericSource(
            boolean fixtureOnly,
            boolean marketDerived,
            String sourceType,
            String sourceRef,
            String sourceTimeframe,
            String valueToken
    ) {

        public static NumericSource synthetic(String sourceType, String valueToken) {
            return new NumericSource(
                    true,
                    false,
                    sourceType,
                    "fixture-" + sourceType.toLowerCase() + "-ref",
                    "1h",
                    valueToken
            );
        }
    }

    public record StopFixture(
            FixtureStatus fixtureStatus,
            String stopCandidateFamily,
            String stopSourceOwner,
            SourceWindow sourceWindow,
            String ruleId,
            String ruleVersion,
            String freshnessOwnership,
            String conflictFamilyOwnership,
            NumericSource numericSource,
            boolean manualReviewRequired,
            boolean notTradeInstruction,
            String reviewMode,
            List<String> blockerEvidence
    ) {

        public StopFixture {
            blockerEvidence = blockerEvidence == null ? List.of() : List.copyOf(blockerEvidence);
        }
    }

    public record TpFixture(
            FixtureStatus fixtureStatus,
            String tpCandidateFamily,
            String tpSourceOwner,
            SourceWindow sourceWindow,
            String ruleId,
            String ruleVersion,
            String freshnessOwnership,
            String conflictFamilyOwnership,
            NumericSource numericSource,
            boolean manualReviewRequired,
            boolean notTradeInstruction,
            String reviewMode,
            List<String> blockerEvidence
    ) {

        public TpFixture {
            blockerEvidence = blockerEvidence == null ? List.of() : List.copyOf(blockerEvidence);
        }
    }

    public record RrFixture(
            FixtureStatus fixtureStatus,
            String rrSourceOwner,
            EntryFixture entryFixtureDependency,
            StopFixture stopFixtureDependency,
            TpFixture tpFixtureDependency,
            EntryStopDistanceState entryStopDistanceState,
            SourceWindow sourceWindow,
            String ruleId,
            String ruleVersion,
            String freshnessOwnership,
            String conflictFamilyOwnership,
            NumericSource numericSource,
            boolean manualReviewRequired,
            boolean notTradeInstruction,
            String reviewMode,
            List<String> blockerEvidence
    ) {

        public RrFixture {
            blockerEvidence = blockerEvidence == null ? List.of() : List.copyOf(blockerEvidence);
        }
    }

    public static final class StopFixtureBuilder {

        private String stopCandidateFamily = StopFamily.STRUCTURAL_INVALIDATION_WITH_BUFFER.name();
        private String stopSourceOwner = "fixture-stop-source-owner";
        private SourceWindow sourceWindow = SourceWindow.fresh("fixture-stop-source-window-p109");
        private String ruleId = "STOP_SOURCE_OWNED_FIXTURE_RULE";
        private String ruleVersion = "p109.stop.fixture.v1";
        private String freshnessOwnership = "fixture-stop-freshness-owner";
        private String conflictFamilyOwnership = "fixture-stop-conflict-family-owner";
        private NumericSource numericSource = NumericSource.synthetic("STOP", "fixture-stop-token:p109");
        private boolean unsafeStaleEvidence;
        private boolean entryStopInversion;
        private final List<ForbiddenSource> forbiddenSources = new ArrayList<>();
        private final List<RiskActionGuardBlocker> riskActionGuardBlockers = new ArrayList<>();

        public StopFixtureBuilder missingStopOwner() {
            this.stopSourceOwner = null;
            return this;
        }

        public StopFixtureBuilder missingNumericSource() {
            this.numericSource = null;
            return this;
        }

        public StopFixtureBuilder staleSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-stale-stop-window-p109");
            this.unsafeStaleEvidence = false;
            return this;
        }

        public StopFixtureBuilder staleUnsafeSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-unsafe-stale-stop-window-p109");
            this.unsafeStaleEvidence = true;
            return this;
        }

        public StopFixtureBuilder unsupportedSourceFamily(String unsupportedSourceFamily) {
            this.stopCandidateFamily = unsupportedSourceFamily;
            return this;
        }

        public StopFixtureBuilder entryStopInversion() {
            this.entryStopInversion = true;
            return this;
        }

        public StopFixtureBuilder forbiddenSource(ForbiddenSource forbiddenSource) {
            if (forbiddenSource != null) {
                this.forbiddenSources.add(forbiddenSource);
            }
            return this;
        }

        public StopFixtureBuilder riskActionGuardBlocker(RiskActionGuardBlocker riskActionGuardBlocker) {
            if (riskActionGuardBlocker != null) {
                this.riskActionGuardBlockers.add(riskActionGuardBlocker);
            }
            return this;
        }

        public StopFixture build() {
            Evidence evidence = new Evidence();
            requireText(stopSourceOwner, "stopSourceOwner", "missing_stop_owner", evidence.missingEvidence);
            requireCommonSourceFields(sourceWindow, unsafeStaleEvidence, ruleId, ruleVersion, freshnessOwnership,
                    conflictFamilyOwnership, numericSource, "stop", evidence);

            if (hasText(stopCandidateFamily)
                    && !StopFamily.STRUCTURAL_INVALIDATION_WITH_BUFFER.name().equals(stopCandidateFamily)) {
                evidence.addBlocker(
                        "stopCandidateFamily",
                        "unsupported_source_family",
                        "unsupported_source_family:" + stopCandidateFamily
                );
            }
            if (entryStopInversion) {
                evidence.addBlocker("entryStopDirection", "entry_stop_inversion");
            }
            addForbiddenAndRiskBlockers(forbiddenSources, riskActionGuardBlockers, evidence);

            return new StopFixture(
                    resolveStatus(evidence),
                    stopCandidateFamily,
                    stopSourceOwner,
                    sourceWindow,
                    ruleId,
                    ruleVersion,
                    freshnessOwnership,
                    conflictFamilyOwnership,
                    numericSource,
                    true,
                    true,
                    REVIEW_ONLY,
                    evidence.all()
            );
        }
    }

    public static final class TpFixtureBuilder {

        private String tpCandidateFamily;
        private String tpSourceOwner = "fixture-tp-source-owner";
        private SourceWindow sourceWindow = SourceWindow.fresh("fixture-tp-source-window-p109");
        private String ruleId = "TP_SOURCE_OWNED_FIXTURE_RULE";
        private String ruleVersion = "p109.tp.fixture.v1";
        private String freshnessOwnership = "fixture-tp-freshness-owner";
        private String conflictFamilyOwnership = "fixture-tp-conflict-family-owner";
        private NumericSource numericSource = NumericSource.synthetic("TP", "fixture-tp-token:p109");
        private boolean unsafeStaleEvidence;
        private boolean entryTpDirectionConflict;
        private boolean stopTpOverlap;
        private final List<ForbiddenSource> forbiddenSources = new ArrayList<>();
        private final List<RiskActionGuardBlocker> riskActionGuardBlockers = new ArrayList<>();

        private TpFixtureBuilder(TpFamily family) {
            this.tpCandidateFamily = family == null ? null : family.name();
        }

        public TpFixtureBuilder missingTpOwner() {
            this.tpSourceOwner = null;
            return this;
        }

        public TpFixtureBuilder missingNumericSource() {
            this.numericSource = null;
            return this;
        }

        public TpFixtureBuilder staleSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-stale-tp-window-p109");
            this.unsafeStaleEvidence = false;
            return this;
        }

        public TpFixtureBuilder staleUnsafeSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-unsafe-stale-tp-window-p109");
            this.unsafeStaleEvidence = true;
            return this;
        }

        public TpFixtureBuilder unsupportedSourceFamily(String unsupportedSourceFamily) {
            this.tpCandidateFamily = unsupportedSourceFamily;
            return this;
        }

        public TpFixtureBuilder entryTpDirectionConflict() {
            this.entryTpDirectionConflict = true;
            return this;
        }

        public TpFixtureBuilder stopTpOverlap() {
            this.stopTpOverlap = true;
            return this;
        }

        public TpFixtureBuilder forbiddenSource(ForbiddenSource forbiddenSource) {
            if (forbiddenSource != null) {
                this.forbiddenSources.add(forbiddenSource);
            }
            return this;
        }

        public TpFixtureBuilder riskActionGuardBlocker(RiskActionGuardBlocker riskActionGuardBlocker) {
            if (riskActionGuardBlocker != null) {
                this.riskActionGuardBlockers.add(riskActionGuardBlocker);
            }
            return this;
        }

        public TpFixture build() {
            Evidence evidence = new Evidence();
            requireText(tpSourceOwner, "tpSourceOwner", "missing_tp_owner", evidence.missingEvidence);
            requireCommonSourceFields(sourceWindow, unsafeStaleEvidence, ruleId, ruleVersion, freshnessOwnership,
                    conflictFamilyOwnership, numericSource, "tp", evidence);

            if (hasText(tpCandidateFamily) && !isSupportedTpFamily(tpCandidateFamily)) {
                evidence.addBlocker(
                        "tpCandidateFamily",
                        "unsupported_source_family",
                        "unsupported_source_family:" + tpCandidateFamily
                );
            }
            if (entryTpDirectionConflict) {
                evidence.addBlocker("entryTpDirection", "entry_tp_direction_conflict");
            }
            if (stopTpOverlap) {
                evidence.addBlocker("stopTpBoundary", "stop_tp_overlap");
            }
            addForbiddenAndRiskBlockers(forbiddenSources, riskActionGuardBlockers, evidence);

            return new TpFixture(
                    resolveStatus(evidence),
                    tpCandidateFamily,
                    tpSourceOwner,
                    sourceWindow,
                    ruleId,
                    ruleVersion,
                    freshnessOwnership,
                    conflictFamilyOwnership,
                    numericSource,
                    true,
                    true,
                    REVIEW_ONLY,
                    evidence.all()
            );
        }

        private static boolean isSupportedTpFamily(String tpCandidateFamily) {
            for (TpFamily family : TpFamily.values()) {
                if (family.name().equals(tpCandidateFamily)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class RrFixtureBuilder {

        private String rrSourceOwner = "fixture-rr-source-owner";
        private EntryFixture entryFixtureDependency;
        private StopFixture stopFixtureDependency;
        private TpFixture tpFixtureDependency;
        private EntryStopDistanceState entryStopDistanceState = EntryStopDistanceState.PRESENT;
        private SourceWindow sourceWindow = SourceWindow.fresh("fixture-rr-source-window-p109");
        private String ruleId = "RR_SOURCE_OWNED_FIXTURE_RULE";
        private String ruleVersion = "p109.rr.fixture.v1";
        private String freshnessOwnership = "fixture-rr-freshness-owner";
        private String conflictFamilyOwnership = "fixture-rr-conflict-family-owner";
        private NumericSource numericSource = NumericSource.synthetic("RR", "fixture-rr-token:p109");
        private boolean unsafeStaleEvidence;
        private boolean entryStopInversion;
        private boolean entryTpDirectionConflict;
        private boolean stopTpOverlap;
        private final List<ForbiddenSource> forbiddenSources = new ArrayList<>();
        private final List<RiskActionGuardBlocker> riskActionGuardBlockers = new ArrayList<>();

        private RrFixtureBuilder(EntryFixture entryFixture, StopFixture stopFixture, TpFixture tpFixture) {
            this.entryFixtureDependency = entryFixture;
            this.stopFixtureDependency = stopFixture;
            this.tpFixtureDependency = tpFixture;
        }

        public RrFixtureBuilder missingRrOwner() {
            this.rrSourceOwner = null;
            return this;
        }

        public RrFixtureBuilder missingNumericSource() {
            this.numericSource = null;
            return this;
        }

        public RrFixtureBuilder missingEntryFixtureDependency() {
            this.entryFixtureDependency = null;
            return this;
        }

        public RrFixtureBuilder missingStopFixtureDependency() {
            this.stopFixtureDependency = null;
            return this;
        }

        public RrFixtureBuilder missingTpFixtureDependency() {
            this.tpFixtureDependency = null;
            return this;
        }

        public RrFixtureBuilder entryStopDistance(EntryStopDistanceState state) {
            this.entryStopDistanceState = state;
            return this;
        }

        public RrFixtureBuilder staleSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-stale-rr-window-p109");
            this.unsafeStaleEvidence = false;
            return this;
        }

        public RrFixtureBuilder staleUnsafeSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-unsafe-stale-rr-window-p109");
            this.unsafeStaleEvidence = true;
            return this;
        }

        public RrFixtureBuilder entryStopInversion() {
            this.entryStopInversion = true;
            return this;
        }

        public RrFixtureBuilder entryTpDirectionConflict() {
            this.entryTpDirectionConflict = true;
            return this;
        }

        public RrFixtureBuilder stopTpOverlap() {
            this.stopTpOverlap = true;
            return this;
        }

        public RrFixtureBuilder forbiddenSource(ForbiddenSource forbiddenSource) {
            if (forbiddenSource != null) {
                this.forbiddenSources.add(forbiddenSource);
            }
            return this;
        }

        public RrFixtureBuilder riskActionGuardBlocker(RiskActionGuardBlocker riskActionGuardBlocker) {
            if (riskActionGuardBlocker != null) {
                this.riskActionGuardBlockers.add(riskActionGuardBlocker);
            }
            return this;
        }

        public RrFixture build() {
            Evidence evidence = new Evidence();
            requireText(rrSourceOwner, "rrSourceOwner", "missing_rr_owner", evidence.missingEvidence);
            requireCommonSourceFields(sourceWindow, unsafeStaleEvidence, ruleId, ruleVersion, freshnessOwnership,
                    conflictFamilyOwnership, numericSource, "rr", evidence);
            evaluateDependencies(evidence);
            evaluateEntryStopDistance(evidence);

            if (entryStopInversion) {
                evidence.addBlocker("entryStopDirection", "entry_stop_inversion");
            }
            if (entryTpDirectionConflict) {
                evidence.addBlocker("entryTpDirection", "entry_tp_direction_conflict");
            }
            if (stopTpOverlap) {
                evidence.addBlocker("stopTpBoundary", "stop_tp_overlap");
            }
            addForbiddenAndRiskBlockers(forbiddenSources, riskActionGuardBlockers, evidence);

            return new RrFixture(
                    resolveStatus(evidence),
                    rrSourceOwner,
                    entryFixtureDependency,
                    stopFixtureDependency,
                    tpFixtureDependency,
                    entryStopDistanceState,
                    sourceWindow,
                    ruleId,
                    ruleVersion,
                    freshnessOwnership,
                    conflictFamilyOwnership,
                    numericSource,
                    true,
                    true,
                    REVIEW_ONLY,
                    evidence.all()
            );
        }

        private void evaluateDependencies(Evidence evidence) {
            if (entryFixtureDependency == null) {
                evidence.addMissing("entryFixtureDependency", "missing_entry_fixture_dependency");
            } else if (entryFixtureDependency.fixtureStatus() == EntryFixtureStatus.BLOCKED) {
                evidence.addBlocker("entryFixtureDependency", "blocked_entry_fixture_dependency");
                evidence.blockerEvidence.addAll(entryFixtureDependency.blockerEvidence());
            } else if (entryFixtureDependency.fixtureStatus() == EntryFixtureStatus.INCOMPLETE) {
                evidence.addMissing("entryFixtureDependency", "incomplete_entry_fixture_dependency");
                evidence.missingEvidence.addAll(entryFixtureDependency.blockerEvidence());
            }

            if (stopFixtureDependency == null) {
                evidence.addMissing("stopFixtureDependency", "missing_stop_fixture_dependency");
            } else if (stopFixtureDependency.fixtureStatus() == FixtureStatus.BLOCKED) {
                evidence.addBlocker("stopFixtureDependency", "blocked_stop_fixture_dependency");
                evidence.blockerEvidence.addAll(stopFixtureDependency.blockerEvidence());
            } else if (stopFixtureDependency.fixtureStatus() == FixtureStatus.INCOMPLETE) {
                evidence.addMissing("stopFixtureDependency", "incomplete_stop_fixture_dependency");
                evidence.missingEvidence.addAll(stopFixtureDependency.blockerEvidence());
            }

            if (tpFixtureDependency == null) {
                evidence.addMissing("tpFixtureDependency", "missing_tp_fixture_dependency");
            } else if (tpFixtureDependency.fixtureStatus() == FixtureStatus.BLOCKED) {
                evidence.addBlocker("tpFixtureDependency", "blocked_tp_fixture_dependency");
                evidence.blockerEvidence.addAll(tpFixtureDependency.blockerEvidence());
            } else if (tpFixtureDependency.fixtureStatus() == FixtureStatus.INCOMPLETE) {
                evidence.addMissing("tpFixtureDependency", "incomplete_tp_fixture_dependency");
                evidence.missingEvidence.addAll(tpFixtureDependency.blockerEvidence());
            } else if (!hasText(tpFixtureDependency.tpSourceOwner())) {
                evidence.addMissing("tpFixtureDependency", "missing_tp_owner");
            }
        }

        private void evaluateEntryStopDistance(Evidence evidence) {
            if (entryStopDistanceState == null || entryStopDistanceState == EntryStopDistanceState.MISSING) {
                evidence.addMissing("entryStopDistance", "missing_entry_stop_distance");
                return;
            }
            if (entryStopDistanceState == EntryStopDistanceState.STALE) {
                evidence.addMissing("entryStopDistance", "stale_entry_stop_distance");
                return;
            }
            if (entryStopDistanceState == EntryStopDistanceState.ZERO) {
                evidence.addBlocker("entryStopDistance", "zero_entry_stop_distance");
            } else if (entryStopDistanceState == EntryStopDistanceState.NEGATIVE) {
                evidence.addBlocker("entryStopDistance", "negative_entry_stop_distance");
            } else if (entryStopDistanceState == EntryStopDistanceState.AMBIGUOUS) {
                evidence.addBlocker("entryStopDistance", "ambiguous_entry_stop_distance");
            } else if (entryStopDistanceState == EntryStopDistanceState.UNSUPPORTED) {
                evidence.addBlocker("entryStopDistance", "unsupported_entry_stop_distance");
            }
        }
    }

    private static void requireCommonSourceFields(
            SourceWindow sourceWindow,
            boolean unsafeStaleEvidence,
            String ruleId,
            String ruleVersion,
            String freshnessOwnership,
            String conflictFamilyOwnership,
            NumericSource numericSource,
            String prefix,
            Evidence evidence
    ) {
        requireText(ruleId, prefix + "RuleId", "missing_rule_id", evidence.missingEvidence);
        requireText(ruleVersion, prefix + "RuleVersion", "missing_rule_version", evidence.missingEvidence);
        requireText(freshnessOwnership, prefix + "FreshnessOwnership", "missing_freshness_ownership",
                evidence.missingEvidence);
        requireText(conflictFamilyOwnership, prefix + "ConflictFamilyOwnership", "missing_conflict_family_ownership",
                evidence.missingEvidence);

        if (sourceWindow == null) {
            evidence.addMissing(prefix + "SourceWindow", "missing_source_window");
        } else if (sourceWindow.stale() && unsafeStaleEvidence) {
            evidence.addBlocker(prefix + "SourceWindow", "stale_source_window", "unsafe_stale_source_window");
        } else if (sourceWindow.stale()) {
            evidence.addMissing(prefix + "SourceWindow", "stale_source_window");
        }

        if (numericSource == null) {
            evidence.addMissing(prefix + "NumericSource", "missing_numeric_source");
        } else {
            if (!numericSource.fixtureOnly()) {
                evidence.addBlocker(prefix + "NumericSource", "numeric_source_not_fixture_only");
            }
            if (numericSource.marketDerived()) {
                evidence.addBlocker(prefix + "NumericSource", "market_derived_numeric_source");
            }
            requireText(numericSource.valueToken(), prefix + "NumericSource.valueToken", "missing_numeric_source",
                    evidence.missingEvidence);
            requireText(numericSource.sourceRef(), prefix + "NumericSource.sourceRef", "missing_numeric_source_ref",
                    evidence.missingEvidence);
        }
    }

    private static void addForbiddenAndRiskBlockers(
            List<ForbiddenSource> forbiddenSources,
            List<RiskActionGuardBlocker> riskActionGuardBlockers,
            Evidence evidence
    ) {
        for (ForbiddenSource forbiddenSource : forbiddenSources) {
            evidence.addBlocker("forbidden_source", forbiddenSource.blockerEvidence());
        }
        for (RiskActionGuardBlocker riskActionGuardBlocker : riskActionGuardBlockers) {
            evidence.addBlocker("riskActionGuard", riskActionGuardBlocker.blockerEvidence());
        }
    }

    private static FixtureStatus resolveStatus(Evidence evidence) {
        if (!evidence.blockerEvidence.isEmpty()) {
            return FixtureStatus.BLOCKED;
        }
        if (!evidence.missingEvidence.isEmpty()) {
            return FixtureStatus.INCOMPLETE;
        }
        return FixtureStatus.FIXTURE_VALID_CANDIDATE;
    }

    private static void requireText(String value, String fieldName, String missingToken, List<String> missingEvidence) {
        if (!hasText(value)) {
            missingEvidence.add(fieldName);
            missingEvidence.add(missingToken);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class Evidence {

        private final List<String> missingEvidence = new ArrayList<>();
        private final List<String> blockerEvidence = new ArrayList<>();

        private void addMissing(String... values) {
            for (String value : values) {
                missingEvidence.add(value);
            }
        }

        private void addBlocker(String... values) {
            for (String value : values) {
                blockerEvidence.add(value);
            }
        }

        private List<String> all() {
            Set<String> evidence = new LinkedHashSet<>();
            evidence.addAll(missingEvidence);
            evidence.addAll(blockerEvidence);
            return List.copyOf(evidence);
        }
    }
}
