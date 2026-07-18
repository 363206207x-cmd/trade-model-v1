package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "trade-model.watchlist")
public class WatchlistProperties {
    private int maxAssets = 20;
    private String venue = "BINANCE";
    private MarketType marketType = MarketType.PERPETUAL;
    private ContractType contractType = ContractType.LINEAR;
    private List<String> symbols = new ArrayList<>();

    public int getMaxAssets() { return maxAssets; }
    public void setMaxAssets(int maxAssets) { this.maxAssets = maxAssets; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public MarketType getMarketType() { return marketType; }
    public void setMarketType(MarketType marketType) { this.marketType = marketType; }
    public ContractType getContractType() { return contractType; }
    public void setContractType(ContractType contractType) { this.contractType = contractType; }
    public List<String> getSymbols() { return List.copyOf(symbols); }
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols == null ? new ArrayList<>() : new ArrayList<>(symbols);
    }
}
