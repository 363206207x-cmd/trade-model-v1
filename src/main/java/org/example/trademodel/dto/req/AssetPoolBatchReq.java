package org.example.trademodel.dto.req;

import java.util.List;

public class AssetPoolBatchReq {
    private List<String> symbols;
    private Boolean focusEnabled = true;
    private String timeframe = "5m";

    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }
    public Boolean getFocusEnabled() { return focusEnabled; }
    public void setFocusEnabled(Boolean focusEnabled) { this.focusEnabled = focusEnabled; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
}
