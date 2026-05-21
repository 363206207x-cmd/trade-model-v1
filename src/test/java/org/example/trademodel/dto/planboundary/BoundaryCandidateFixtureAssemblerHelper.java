package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixtureStatus;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.FixtureStatus;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture;

public final class BoundaryCandidateFixtureAssemblerHelper {

    public static final String REVIEW_ONLY = "REVIEW_ONLY";

    private BoundaryCandidateFixtureAssemblerHelper() {
    }

    public static BoundaryCandidateFixture assemble(
            EntryFixture entryFixture,
            StopFixture stopFixture,
            TpFixture tpFixture,
            RrFixture rrFixture
    ) {
        Evidence evidence = new Evidence();
        collectEntryDependency(entryFixture, evidence);
        collectStopDependency(stopFixture, evidence);
        collectTpDependency(tpFixture, evidence);
        collectRrDependency(rrFixture, evidence);

        AssemblerStatus status = resolveStatus(evidence);
        SourceOwnerSummary sourceOwnerSummary = sourceOwnerSummary(entryFixture, stopFixture, tpFixture, rrFixture);
        SourceFamilySummary sourceFamilySummary = sourceFamilySummary(entryFixture, stopFixture, tpFixture);
        NumericSourceTokenSummary numericSourceTokenSummary = numericSourceTokenSummary(
                entryFixture,
                stopFixture,
                tpFixture,
                rrFixture
        );

        return new BoundaryCandidateFixture(
                status,
                entryReview(entryFixture),
                stopReview(stopFixture),
                takeProfitReview(tpFixture),
                riskRewardReview(rrFixture),
                sourceOwnerSummary,
                sourceFamilySummary,
                numericSourceTokenSummary,
                evidence.dependencyEvidence(),
                evidence.all(),
                true,
                true,
                REVIEW_ONLY,
                boundaryCandidateStyleFieldNames()
        );
    }

    public enum AssemblerStatus {
        INCOMPLETE,
        BLOCKED,
        FIXTURE_VALID_CANDIDATE
    }

    public record BoundaryCandidateFixture(
            AssemblerStatus assemblerStatus,
            ReviewField entryReview,
            ReviewField stopReview,
            ReviewField takeProfitReview,
            ReviewField riskRewardReview,
            SourceOwnerSummary sourceOwnerSummary,
            SourceFamilySummary sourceFamilySummary,
            NumericSourceTokenSummary numericSourceTokenSummary,
            List<String> dependencyEvidence,
            List<String> blockerEvidence,
            boolean manualReviewRequired,
            boolean notTradeInstruction,
            String reviewMode,
            List<String> boundaryCandidateStyleFieldNames
    ) {

        public BoundaryCandidateFixture {
            dependencyEvidence = dependencyEvidence == null ? List.of() : List.copyOf(dependencyEvidence);
            blockerEvidence = blockerEvidence == null ? List.of() : List.copyOf(blockerEvidence);
            boundaryCandidateStyleFieldNames = boundaryCandidateStyleFieldNames == null
                    ? List.of()
                    : List.copyOf(boundaryCandidateStyleFieldNames);
        }
    }

    public record ReviewField(
            String dependencyName,
            String fixtureStatus,
            String sourceOwner,
            String sourceFamily,
            String sourceRef,
            String numericSourceToken
    ) {
    }

    public record SourceOwnerSummary(
            String entrySourceOwner,
            String stopSourceOwner,
            String tpSourceOwner,
            String rrSourceOwner
    ) {
    }

    public record SourceFamilySummary(
            String entrySourceFamily,
            String stopSourceFamily,
            String tpSourceFamily
    ) {
    }

    public record NumericSourceTokenSummary(
            String entryNumericSourceToken,
            String stopNumericSourceToken,
            String tpNumericSourceToken,
            String rrNumericSourceToken
    ) {
    }

    private static void collectEntryDependency(EntryFixture fixture, Evidence evidence) {
        if (fixture == null) {
            evidence.addMissingDependency("entryFixtureDependency", "missing_entry_dependency");
            return;
        }
        evidence.addDependencyEvidence("entryFixtureDependency", fixture.fixtureStatus().name());
        if (fixture.fixtureStatus() == EntryFixtureStatus.BLOCKED) {
            evidence.addBlockedDependency("entryFixtureDependency", "blocked_entry_dependency", fixture.blockerEvidence());
        } else if (fixture.fixtureStatus() == EntryFixtureStatus.INCOMPLETE) {
            evidence.addIncompleteDependency(
                    "entryFixtureDependency",
                    "incomplete_entry_dependency",
                    fixture.blockerEvidence()
            );
        }
    }

    private static void collectStopDependency(StopFixture fixture, Evidence evidence) {
        if (fixture == null) {
            evidence.addMissingDependency("stopFixtureDependency", "missing_stop_dependency");
            return;
        }
        evidence.addDependencyEvidence("stopFixtureDependency", fixture.fixtureStatus().name());
        if (fixture.fixtureStatus() == FixtureStatus.BLOCKED) {
            evidence.addBlockedDependency("stopFixtureDependency", "blocked_stop_dependency", fixture.blockerEvidence());
        } else if (fixture.fixtureStatus() == FixtureStatus.INCOMPLETE) {
            evidence.addIncompleteDependency(
                    "stopFixtureDependency",
                    "incomplete_stop_dependency",
                    fixture.blockerEvidence()
            );
        }
    }

    private static void collectTpDependency(TpFixture fixture, Evidence evidence) {
        if (fixture == null) {
            evidence.addMissingDependency("tpFixtureDependency", "missing_tp_dependency");
            return;
        }
        evidence.addDependencyEvidence("tpFixtureDependency", fixture.fixtureStatus().name());
        if (fixture.fixtureStatus() == FixtureStatus.BLOCKED) {
            evidence.addBlockedDependency("tpFixtureDependency", "blocked_tp_dependency", fixture.blockerEvidence());
        } else if (fixture.fixtureStatus() == FixtureStatus.INCOMPLETE) {
            evidence.addIncompleteDependency(
                    "tpFixtureDependency",
                    "incomplete_tp_dependency",
                    fixture.blockerEvidence()
            );
        }
    }

    private static void collectRrDependency(RrFixture fixture, Evidence evidence) {
        if (fixture == null) {
            evidence.addMissingDependency("rrFixtureDependency", "missing_rr_dependency");
            return;
        }
        evidence.addDependencyEvidence("rrFixtureDependency", fixture.fixtureStatus().name());
        if (fixture.fixtureStatus() == FixtureStatus.BLOCKED) {
            evidence.addBlockedDependency("rrFixtureDependency", "blocked_rr_dependency", fixture.blockerEvidence());
        } else if (fixture.fixtureStatus() == FixtureStatus.INCOMPLETE) {
            evidence.addIncompleteDependency(
                    "rrFixtureDependency",
                    "incomplete_rr_dependency",
                    fixture.blockerEvidence()
            );
        }
    }

    private static AssemblerStatus resolveStatus(Evidence evidence) {
        if (!evidence.blockedEvidence.isEmpty()) {
            return AssemblerStatus.BLOCKED;
        }
        if (!evidence.missingEvidence.isEmpty()) {
            return AssemblerStatus.INCOMPLETE;
        }
        return AssemblerStatus.FIXTURE_VALID_CANDIDATE;
    }

    private static ReviewField entryReview(EntryFixture fixture) {
        if (fixture == null) {
            return missingReview("entry");
        }
        return new ReviewField(
                "entry",
                fixture.fixtureStatus().name(),
                fixture.sourceOwner(),
                fixture.entrySourceType(),
                fixture.entrySourceRef(),
                fixture.numericSource() == null ? null : fixture.numericSource().valueToken()
        );
    }

    private static ReviewField stopReview(StopFixture fixture) {
        if (fixture == null) {
            return missingReview("stop");
        }
        return new ReviewField(
                "stop",
                fixture.fixtureStatus().name(),
                fixture.stopSourceOwner(),
                fixture.stopCandidateFamily(),
                fixture.numericSource() == null ? null : fixture.numericSource().sourceRef(),
                fixture.numericSource() == null ? null : fixture.numericSource().valueToken()
        );
    }

    private static ReviewField takeProfitReview(TpFixture fixture) {
        if (fixture == null) {
            return missingReview("takeProfit");
        }
        return new ReviewField(
                "takeProfit",
                fixture.fixtureStatus().name(),
                fixture.tpSourceOwner(),
                fixture.tpCandidateFamily(),
                fixture.numericSource() == null ? null : fixture.numericSource().sourceRef(),
                fixture.numericSource() == null ? null : fixture.numericSource().valueToken()
        );
    }

    private static ReviewField riskRewardReview(RrFixture fixture) {
        if (fixture == null) {
            return missingReview("riskReward");
        }
        return new ReviewField(
                "riskReward",
                fixture.fixtureStatus().name(),
                fixture.rrSourceOwner(),
                "RR_FIXTURE",
                fixture.numericSource() == null ? null : fixture.numericSource().sourceRef(),
                fixture.numericSource() == null ? null : fixture.numericSource().valueToken()
        );
    }

    private static ReviewField missingReview(String dependencyName) {
        return new ReviewField(dependencyName, "INCOMPLETE", null, null, null, null);
    }

    private static SourceOwnerSummary sourceOwnerSummary(
            EntryFixture entryFixture,
            StopFixture stopFixture,
            TpFixture tpFixture,
            RrFixture rrFixture
    ) {
        return new SourceOwnerSummary(
                entryFixture == null ? null : entryFixture.sourceOwner(),
                stopFixture == null ? null : stopFixture.stopSourceOwner(),
                tpFixture == null ? null : tpFixture.tpSourceOwner(),
                rrFixture == null ? null : rrFixture.rrSourceOwner()
        );
    }

    private static SourceFamilySummary sourceFamilySummary(
            EntryFixture entryFixture,
            StopFixture stopFixture,
            TpFixture tpFixture
    ) {
        return new SourceFamilySummary(
                entryFixture == null ? null : entryFixture.entrySourceType(),
                stopFixture == null ? null : stopFixture.stopCandidateFamily(),
                tpFixture == null ? null : tpFixture.tpCandidateFamily()
        );
    }

    private static NumericSourceTokenSummary numericSourceTokenSummary(
            EntryFixture entryFixture,
            StopFixture stopFixture,
            TpFixture tpFixture,
            RrFixture rrFixture
    ) {
        return new NumericSourceTokenSummary(
                entryFixture == null || entryFixture.numericSource() == null
                        ? null
                        : entryFixture.numericSource().valueToken(),
                stopFixture == null || stopFixture.numericSource() == null
                        ? null
                        : stopFixture.numericSource().valueToken(),
                tpFixture == null || tpFixture.numericSource() == null
                        ? null
                        : tpFixture.numericSource().valueToken(),
                rrFixture == null || rrFixture.numericSource() == null
                        ? null
                        : rrFixture.numericSource().valueToken()
        );
    }

    private static List<String> boundaryCandidateStyleFieldNames() {
        return List.of(
                "entry",
                "stop",
                "takeProfitLevels",
                "sourceFields",
                "dataQualityScore",
                "blockingReasons"
        );
    }

    private static final class Evidence {

        private final List<String> dependencyEvidence = new ArrayList<>();
        private final List<String> missingEvidence = new ArrayList<>();
        private final List<String> blockedEvidence = new ArrayList<>();

        private void addDependencyEvidence(String dependencyName, String status) {
            dependencyEvidence.add(dependencyName);
            dependencyEvidence.add(dependencyName + ":" + status);
        }

        private void addMissingDependency(String dependencyName, String token) {
            dependencyEvidence.add(dependencyName);
            missingEvidence.add(dependencyName);
            missingEvidence.add(token);
        }

        private void addIncompleteDependency(String dependencyName, String token, List<String> dependencyBlockers) {
            dependencyEvidence.add(dependencyName);
            missingEvidence.add(dependencyName);
            missingEvidence.add(token);
            missingEvidence.addAll(dependencyBlockers);
        }

        private void addBlockedDependency(String dependencyName, String token, List<String> dependencyBlockers) {
            dependencyEvidence.add(dependencyName);
            blockedEvidence.add(dependencyName);
            blockedEvidence.add(token);
            blockedEvidence.addAll(dependencyBlockers);
        }

        private List<String> dependencyEvidence() {
            return distinct(dependencyEvidence);
        }

        private List<String> all() {
            List<String> allEvidence = new ArrayList<>();
            allEvidence.addAll(missingEvidence);
            allEvidence.addAll(blockedEvidence);
            return distinct(allEvidence);
        }

        private static List<String> distinct(List<String> evidence) {
            Set<String> distinctEvidence = new LinkedHashSet<>(evidence);
            return List.copyOf(distinctEvidence);
        }
    }
}
