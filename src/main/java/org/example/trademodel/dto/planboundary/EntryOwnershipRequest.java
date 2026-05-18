package org.example.trademodel.dto.planboundary;

/**
 * Request envelope for future SourceTrace entry ownership resolution.
 *
 * <p>This DTO intentionally keeps runtime market context separate from the
 * rule-owned candidate so a future adapter cannot silently derive an entry
 * value from latest price, quote, AI text, or dashboard text.
 */
public class EntryOwnershipRequest {

    private RuntimeKlineContextDTO runtimeKlineContext;
    private RuleOwnedEntryCandidateDTO ruleOwnedEntryCandidate;
    private EntrySourceFreshnessDTO freshness;
    private EntrySourceConflictDTO conflict;
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;

    public RuntimeKlineContextDTO getRuntimeKlineContext() {
        return runtimeKlineContext;
    }

    public void setRuntimeKlineContext(RuntimeKlineContextDTO runtimeKlineContext) {
        this.runtimeKlineContext = runtimeKlineContext;
    }

    public RuleOwnedEntryCandidateDTO getRuleOwnedEntryCandidate() {
        return ruleOwnedEntryCandidate;
    }

    public void setRuleOwnedEntryCandidate(RuleOwnedEntryCandidateDTO ruleOwnedEntryCandidate) {
        this.ruleOwnedEntryCandidate = ruleOwnedEntryCandidate;
    }

    public EntrySourceFreshnessDTO getFreshness() {
        return freshness;
    }

    public void setFreshness(EntrySourceFreshnessDTO freshness) {
        this.freshness = freshness;
    }

    public EntrySourceConflictDTO getConflict() {
        return conflict;
    }

    public void setConflict(EntrySourceConflictDTO conflict) {
        this.conflict = conflict;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }
}
