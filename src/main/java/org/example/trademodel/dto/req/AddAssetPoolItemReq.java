package org.example.trademodel.dto.req;

public class AddAssetPoolItemReq {
    private String symbol;
    private Boolean focusEnabled = true;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Boolean getFocusEnabled() { return focusEnabled; }
    public void setFocusEnabled(Boolean focusEnabled) { this.focusEnabled = focusEnabled; }
}
