package org.example.trademodel.analysisrun;

public class AnalysisRunCommand {
    private final String symbol;
    private final String timeframe;
    private final AnalysisRunTriggerType triggerType;
    private final String triggerReference;
    private final String requestId;
    private final String analysisTime;
    private final String parentAnalysisId;
    private final String parentTraceId;

    private AnalysisRunCommand(String symbol, String timeframe, AnalysisRunTriggerType triggerType,
                               String triggerReference, String requestId, String analysisTime,
                               String parentAnalysisId, String parentTraceId) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.triggerType = triggerType;
        this.triggerReference = triggerReference;
        this.requestId = requestId;
        this.analysisTime = analysisTime;
        this.parentAnalysisId = parentAnalysisId;
        this.parentTraceId = parentTraceId;
    }

    public static AnalysisRunCommand manual(String symbol, String timeframe, String requestId, String analysisTime) {
        return new AnalysisRunCommand(symbol, timeframe, AnalysisRunTriggerType.MANUAL_API,
                null, requestId, analysisTime, null, null);
    }

    public static AnalysisRunCommand scheduled(String symbol, String timeframe, String requestId, String triggerReference) {
        return new AnalysisRunCommand(symbol, timeframe, AnalysisRunTriggerType.SCHEDULED,
                triggerReference, requestId, null, null, null);
    }

    public static AnalysisRunCommand assetPoolScan(String symbol, String timeframe, String requestId,
                                                   String triggerReference) {
        return new AnalysisRunCommand(symbol, timeframe, AnalysisRunTriggerType.ASSET_POOL_SCAN,
                triggerReference, requestId, null, null, null);
    }

    public static AnalysisRunCommand hotResetRebuild(String symbol, String timeframe, String eventId,
                                                     String requestId, String parentAnalysisId, String parentTraceId) {
        return new AnalysisRunCommand(symbol, timeframe, AnalysisRunTriggerType.HOT_RESET_REBUILD,
                eventId, requestId, null, parentAnalysisId, parentTraceId);
    }

    public static AnalysisRunCommand marketDataCompatibility(String symbol, String timeframe, String requestId) {
        return new AnalysisRunCommand(symbol, timeframe, AnalysisRunTriggerType.MARKET_DATA_COMPATIBILITY,
                null, requestId, null, null, null);
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public AnalysisRunTriggerType getTriggerType() {
        return triggerType;
    }

    public String getTriggerReference() {
        return triggerReference;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getAnalysisTime() {
        return analysisTime;
    }

    public String getParentAnalysisId() {
        return parentAnalysisId;
    }

    public String getParentTraceId() {
        return parentTraceId;
    }
}
