package org.example.trademodel.market.dto;

import java.math.BigDecimal;

/**
 * Minimal 24h ticker snapshot from an external quote provider (V1).
 */
public class MarketQuoteSnapshot {

    private String provider;
    private String symbolNormalized;
    private BigDecimal lastPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal priceChangePercent24h;
    private long fetchedAtEpochMillis;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSymbolNormalized() {
        return symbolNormalized;
    }

    public void setSymbolNormalized(String symbolNormalized) {
        this.symbolNormalized = symbolNormalized;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(BigDecimal lastPrice) {
        this.lastPrice = lastPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public BigDecimal getPriceChangePercent24h() {
        return priceChangePercent24h;
    }

    public void setPriceChangePercent24h(BigDecimal priceChangePercent24h) {
        this.priceChangePercent24h = priceChangePercent24h;
    }

    public long getFetchedAtEpochMillis() {
        return fetchedAtEpochMillis;
    }

    public void setFetchedAtEpochMillis(long fetchedAtEpochMillis) {
        this.fetchedAtEpochMillis = fetchedAtEpochMillis;
    }
}
