package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Nullable conflict metadata for a future SourceTrace entry ownership adapter.
 *
 * <p>Null means the conflict check is missing or unevaluated. A future adapter
 * must fail closed when any required conflict flag is null.
 */
public class EntrySourceConflictDTO {

    private Boolean conflictsWithStop;
    private Boolean conflictsWithTakeProfit;
    private Boolean conflictsWithRiskReward;
    private Boolean conflictsWithLiquidity;
    private Boolean conflictsWithMultiTimeframe;
    private Boolean conflictsWithEvent;
    private Boolean conflictsWithWick;
    private List<String> conflictReasons = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();

    public Boolean getConflictsWithStop() {
        return conflictsWithStop;
    }

    public void setConflictsWithStop(Boolean conflictsWithStop) {
        this.conflictsWithStop = conflictsWithStop;
    }

    public Boolean getConflictsWithTakeProfit() {
        return conflictsWithTakeProfit;
    }

    public void setConflictsWithTakeProfit(Boolean conflictsWithTakeProfit) {
        this.conflictsWithTakeProfit = conflictsWithTakeProfit;
    }

    public Boolean getConflictsWithRiskReward() {
        return conflictsWithRiskReward;
    }

    public void setConflictsWithRiskReward(Boolean conflictsWithRiskReward) {
        this.conflictsWithRiskReward = conflictsWithRiskReward;
    }

    public Boolean getConflictsWithLiquidity() {
        return conflictsWithLiquidity;
    }

    public void setConflictsWithLiquidity(Boolean conflictsWithLiquidity) {
        this.conflictsWithLiquidity = conflictsWithLiquidity;
    }

    public Boolean getConflictsWithMultiTimeframe() {
        return conflictsWithMultiTimeframe;
    }

    public void setConflictsWithMultiTimeframe(Boolean conflictsWithMultiTimeframe) {
        this.conflictsWithMultiTimeframe = conflictsWithMultiTimeframe;
    }

    public Boolean getConflictsWithEvent() {
        return conflictsWithEvent;
    }

    public void setConflictsWithEvent(Boolean conflictsWithEvent) {
        this.conflictsWithEvent = conflictsWithEvent;
    }

    public Boolean getConflictsWithWick() {
        return conflictsWithWick;
    }

    public void setConflictsWithWick(Boolean conflictsWithWick) {
        this.conflictsWithWick = conflictsWithWick;
    }

    public List<String> getConflictReasons() {
        return new ArrayList<>(conflictReasons);
    }

    public void setConflictReasons(List<String> conflictReasons) {
        this.conflictReasons = conflictReasons == null ? new ArrayList<>() : new ArrayList<>(conflictReasons);
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? new ArrayList<>() : new ArrayList<>(missingFields);
    }
}
