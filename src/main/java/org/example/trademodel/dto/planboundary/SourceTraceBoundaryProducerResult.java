package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class SourceTraceBoundaryProducerResult {

    private SourceTraceDTO sourceTrace;
    private BoundaryEntryDTO entry;
    private BoundaryStopDTO stop;
    private List<BoundaryTakeProfitLevelDTO> takeProfitLevels = new ArrayList<>();
    private BoundarySourceFieldsDTO sourceFields;
    private boolean boundaryReady;
    private boolean sourceTraceReady;
    private List<String> missingFields = new ArrayList<>();
    private List<String> blockingReasons = new ArrayList<>();
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;
    private List<BoundarySourceRefDTO> sourceRefs = new ArrayList<>();

    public SourceTraceDTO getSourceTrace() {
        return sourceTrace;
    }

    public void setSourceTrace(SourceTraceDTO sourceTrace) {
        this.sourceTrace = sourceTrace;
    }

    public BoundaryEntryDTO getEntry() {
        return entry;
    }

    public void setEntry(BoundaryEntryDTO entry) {
        this.entry = entry;
    }

    public BoundaryStopDTO getStop() {
        return stop;
    }

    public void setStop(BoundaryStopDTO stop) {
        this.stop = stop;
    }

    public List<BoundaryTakeProfitLevelDTO> getTakeProfitLevels() {
        return takeProfitLevels;
    }

    public void setTakeProfitLevels(List<BoundaryTakeProfitLevelDTO> takeProfitLevels) {
        this.takeProfitLevels = takeProfitLevels == null ? new ArrayList<>() : new ArrayList<>(takeProfitLevels);
    }

    public BoundarySourceFieldsDTO getSourceFields() {
        return sourceFields;
    }

    public void setSourceFields(BoundarySourceFieldsDTO sourceFields) {
        this.sourceFields = sourceFields;
    }

    public boolean isBoundaryReady() {
        return boundaryReady;
    }

    public void setBoundaryReady(boolean boundaryReady) {
        this.boundaryReady = boundaryReady;
    }

    public boolean isSourceTraceReady() {
        return sourceTraceReady;
    }

    public void setSourceTraceReady(boolean sourceTraceReady) {
        this.sourceTraceReady = sourceTraceReady;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? new ArrayList<>() : new ArrayList<>(missingFields);
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons == null ? new ArrayList<>() : new ArrayList<>(blockingReasons);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
    }
    public List<BoundarySourceRefDTO> getSourceRefs() { return new ArrayList<>(sourceRefs); }
    public void setSourceRefs(List<BoundarySourceRefDTO> value) {
        this.sourceRefs = value == null ? new ArrayList<>() : new ArrayList<>(value);
    }
}
