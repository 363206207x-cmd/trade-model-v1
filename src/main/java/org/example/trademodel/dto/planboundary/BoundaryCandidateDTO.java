package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BoundaryCandidateDTO {

    private String symbol;
    private String timeframe;
    private BoundaryStatusEnum boundaryStatus;
    private BoundaryEntryDTO entry;
    private BoundaryStopDTO stop;
    private List<BoundaryTakeProfitLevelDTO> takeProfitLevels = new ArrayList<>();
    private BoundarySourceFieldsDTO sourceFields;
    private BigDecimal dataQualityScore;
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;
    private List<String> blockingReasons = new ArrayList<>();

    public static BoundaryCandidateDTO valid(
            String symbol,
            String timeframe,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore
    ) {
        requireText(symbol, "symbol");
        requireText(timeframe, "timeframe");
        requireNonNull(entry, "entry");
        requireNonNull(stop, "stop");
        requireNonEmpty(takeProfitLevels, "takeProfitLevels");
        requireNonNull(sourceFields, "sourceFields");
        requireNonNull(dataQualityScore, "dataQualityScore");
        BoundaryCandidateSourceGate.requireValid(entry, stop, takeProfitLevels, sourceFields);

        BoundaryCandidateDTO candidate = new BoundaryCandidateDTO();
        candidate.setSymbol(symbol);
        candidate.setTimeframe(timeframe);
        candidate.setBoundaryStatus(BoundaryStatusEnum.VALID);
        candidate.setEntry(entry);
        candidate.setStop(stop);
        candidate.setTakeProfitLevels(takeProfitLevels);
        candidate.setSourceFields(sourceFields);
        candidate.setDataQualityScore(dataQualityScore);
        candidate.setManualReviewRequired(true);
        candidate.setNotTradeInstruction(true);
        candidate.setBlockingReasons(new ArrayList<>());
        return candidate;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private static void requireNonEmpty(List<?> value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public BoundaryStatusEnum getBoundaryStatus() {
        return boundaryStatus;
    }

    public void setBoundaryStatus(BoundaryStatusEnum boundaryStatus) {
        this.boundaryStatus = boundaryStatus;
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

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(BigDecimal dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
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

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons == null ? new ArrayList<>() : new ArrayList<>(blockingReasons);
    }
}
