package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EntrySourceOwnedCandidateFixtureHelper {

    public static final String REVIEW_ONLY = "REVIEW_ONLY";

    private EntrySourceOwnedCandidateFixtureHelper() {
    }

    public static EntrySourceOwnedCandidateFixtureBuilder forFamily(EntryFamily family) {
        return new EntrySourceOwnedCandidateFixtureBuilder(family);
    }

    public static EntryFixture completeFixture(EntryFamily family) {
        return forFamily(family).build();
    }

    public enum EntryFixtureStatus {
        INCOMPLETE,
        BLOCKED,
        FIXTURE_VALID_CANDIDATE
    }

    public enum EntryFamily {
        STRUCTURE_CONFIRMATION_ZONE,
        BREAKOUT_RETEST_ZONE,
        SUPPORT_RESISTANCE_FLIP_ZONE
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

        public static NumericSource synthetic(String valueToken) {
            return new NumericSource(
                    true,
                    false,
                    "FIXTURE_ENTRY_NUMERIC_SOURCE",
                    "fixture-entry-numeric-ref",
                    "1h",
                    valueToken
            );
        }
    }

    public record EntryFixture(
            EntryFixtureStatus fixtureStatus,
            String symbol,
            String timeframe,
            String entrySourceType,
            String entrySourceTimeframe,
            String entrySourceReason,
            String entrySourceRef,
            SourceWindow sourceWindow,
            String ruleId,
            String ruleVersion,
            String freshnessOwnership,
            String conflictFamilyOwnership,
            NumericSource numericSource,
            String sourceOwner,
            boolean manualReviewRequired,
            boolean notTradeInstruction,
            String reviewMode,
            List<String> blockerEvidence
    ) {

        public EntryFixture {
            blockerEvidence = blockerEvidence == null ? List.of() : List.copyOf(blockerEvidence);
        }
    }

    public static final class EntrySourceOwnedCandidateFixtureBuilder {

        private String symbol = "BTCUSDT_FIXTURE";
        private String timeframe = "1h";
        private String entrySourceType;
        private String entrySourceTimeframe = "1h";
        private String entrySourceReason = "fixture source-owned entry review reason";
        private String entrySourceRef = "fixture-entry-source-ref";
        private SourceWindow sourceWindow = SourceWindow.fresh("fixture-source-window-p108");
        private String ruleId = "ENTRY_SOURCE_OWNED_FIXTURE_RULE";
        private String ruleVersion = "p108.fixture.v1";
        private String freshnessOwnership = "fixture-freshness-owner";
        private String conflictFamilyOwnership = "fixture-conflict-family-owner";
        private NumericSource numericSource = NumericSource.synthetic("fixture-entry-token:p108");
        private String sourceOwner = "fixture-entry-source-owner";
        private boolean unsafeStaleEvidence;
        private final List<ForbiddenSource> forbiddenSources = new ArrayList<>();
        private final List<RiskActionGuardBlocker> riskActionGuardBlockers = new ArrayList<>();

        private EntrySourceOwnedCandidateFixtureBuilder(EntryFamily family) {
            this.entrySourceType = family == null ? null : family.name();
        }

        public EntrySourceOwnedCandidateFixtureBuilder missingSourceOwner() {
            this.sourceOwner = null;
            return this;
        }

        public EntrySourceOwnedCandidateFixtureBuilder missingNumericSource() {
            this.numericSource = null;
            return this;
        }

        public EntrySourceOwnedCandidateFixtureBuilder staleSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-stale-source-window-p108");
            this.unsafeStaleEvidence = false;
            return this;
        }

        public EntrySourceOwnedCandidateFixtureBuilder staleUnsafeSourceWindow() {
            this.sourceWindow = SourceWindow.stale("fixture-unsafe-stale-source-window-p108");
            this.unsafeStaleEvidence = true;
            return this;
        }

        public EntrySourceOwnedCandidateFixtureBuilder unsupportedSourceFamily(String unsupportedSourceFamily) {
            this.entrySourceType = unsupportedSourceFamily;
            return this;
        }

        public EntrySourceOwnedCandidateFixtureBuilder forbiddenSource(ForbiddenSource forbiddenSource) {
            if (forbiddenSource != null) {
                this.forbiddenSources.add(forbiddenSource);
            }
            return this;
        }

        public EntrySourceOwnedCandidateFixtureBuilder riskActionGuardBlocker(
                RiskActionGuardBlocker riskActionGuardBlocker
        ) {
            if (riskActionGuardBlocker != null) {
                this.riskActionGuardBlockers.add(riskActionGuardBlocker);
            }
            return this;
        }

        public EntryFixture build() {
            Set<String> evidence = new LinkedHashSet<>();
            List<String> missingEvidence = new ArrayList<>();
            List<String> blockerEvidence = new ArrayList<>();

            requireText(symbol, "symbol", missingEvidence);
            requireText(timeframe, "timeframe", missingEvidence);
            requireText(entrySourceType, "entrySourceType", missingEvidence);
            requireText(entrySourceTimeframe, "entrySourceTimeframe", missingEvidence);
            requireText(entrySourceReason, "entrySourceReason", missingEvidence);
            requireText(entrySourceRef, "entrySourceRef", missingEvidence);
            requireText(ruleId, "ruleId", missingEvidence);
            requireText(ruleVersion, "ruleVersion", missingEvidence);
            requireText(freshnessOwnership, "freshnessOwnership", missingEvidence);
            requireText(conflictFamilyOwnership, "conflictFamilyOwnership", missingEvidence);

            if (sourceWindow == null) {
                addMissing(missingEvidence, "sourceWindow", "missing_source_window");
            } else if (sourceWindow.stale() && unsafeStaleEvidence) {
                addBlocker(blockerEvidence, "sourceWindow", "stale_source_window", "unsafe_stale_source_window");
            } else if (sourceWindow.stale()) {
                addMissing(missingEvidence, "sourceWindow", "stale_source_window");
            }

            if (numericSource == null) {
                addMissing(missingEvidence, "numericSource", "missing_numeric_source");
            } else {
                if (!numericSource.fixtureOnly()) {
                    addBlocker(blockerEvidence, "numericSource", "numeric_source_not_fixture_only");
                }
                if (numericSource.marketDerived()) {
                    addBlocker(blockerEvidence, "numericSource", "market_derived_numeric_source");
                }
                requireText(numericSource.valueToken(), "numericSource.valueToken", missingEvidence);
                requireText(numericSource.sourceRef(), "numericSource.sourceRef", missingEvidence);
            }

            if (!hasText(sourceOwner)) {
                addMissing(missingEvidence, "sourceOwner", "missing_source_owner");
            }

            if (hasText(entrySourceType) && !isSupportedFamily(entrySourceType)) {
                addBlocker(
                        blockerEvidence,
                        "entrySourceType",
                        "unsupported_source_family",
                        "unsupported_source_family:" + entrySourceType
                );
            }

            for (ForbiddenSource forbiddenSource : forbiddenSources) {
                addBlocker(blockerEvidence, "forbidden_source", forbiddenSource.blockerEvidence());
            }

            for (RiskActionGuardBlocker riskActionGuardBlocker : riskActionGuardBlockers) {
                addBlocker(blockerEvidence, "riskActionGuard", riskActionGuardBlocker.blockerEvidence());
            }

            evidence.addAll(missingEvidence);
            evidence.addAll(blockerEvidence);

            EntryFixtureStatus status;
            if (!blockerEvidence.isEmpty()) {
                status = EntryFixtureStatus.BLOCKED;
            } else if (!missingEvidence.isEmpty()) {
                status = EntryFixtureStatus.INCOMPLETE;
            } else {
                status = EntryFixtureStatus.FIXTURE_VALID_CANDIDATE;
            }

            return new EntryFixture(
                    status,
                    symbol,
                    timeframe,
                    entrySourceType,
                    entrySourceTimeframe,
                    entrySourceReason,
                    entrySourceRef,
                    sourceWindow,
                    ruleId,
                    ruleVersion,
                    freshnessOwnership,
                    conflictFamilyOwnership,
                    numericSource,
                    sourceOwner,
                    true,
                    true,
                    REVIEW_ONLY,
                    List.copyOf(evidence)
            );
        }

        private static boolean isSupportedFamily(String entrySourceType) {
            for (EntryFamily family : EntryFamily.values()) {
                if (family.name().equals(entrySourceType)) {
                    return true;
                }
            }
            return false;
        }

        private static void requireText(String value, String fieldName, List<String> missingEvidence) {
            if (!hasText(value)) {
                addMissing(missingEvidence, fieldName, "missing_required_field:" + fieldName);
            }
        }

        private static void addMissing(List<String> evidence, String... values) {
            for (String value : values) {
                evidence.add(value);
            }
        }

        private static void addBlocker(List<String> evidence, String... values) {
            for (String value : values) {
                evidence.add(value);
            }
        }

        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
