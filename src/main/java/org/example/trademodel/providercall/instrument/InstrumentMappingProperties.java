package org.example.trademodel.providercall.instrument;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

@Component
@ConfigurationProperties(prefix = "trade-model.instruments")
public class InstrumentMappingProperties {
    private List<Mapping> mappings = new ArrayList<>();
    private List<String> defaultSupportedTimeframes = List.of("5m", "15m", "1h", "4h");
    private Instant defaultVerifiedAt;

    public List<Mapping> getMappings() {
        return List.copyOf(mappings);
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings == null ? new ArrayList<>() : new ArrayList<>(mappings);
    }

    public List<String> getDefaultSupportedTimeframes() { return List.copyOf(defaultSupportedTimeframes); }
    public void setDefaultSupportedTimeframes(List<String> values) {
        this.defaultSupportedTimeframes = values == null ? List.of() : List.copyOf(values);
    }
    public Instant getDefaultVerifiedAt() { return defaultVerifiedAt; }
    public void setDefaultVerifiedAt(Instant defaultVerifiedAt) { this.defaultVerifiedAt = defaultVerifiedAt; }

    List<ProviderSymbolMapping> toDomainMappings() {
        return mappings.stream()
                .map(mapping -> mapping.toDomain(defaultSupportedTimeframes, defaultVerifiedAt))
                .toList();
    }

    public static class Mapping {
        private String provider;
        private String baseAsset;
        private String quoteAsset;
        private MarketType marketType;
        private String venue;
        private ContractType contractType;
        private String providerSymbol;
        private boolean enabled = true;
        private String sourceVersion = "MAPPING_V1";
        private List<String> supportedTimeframes;
        private Instant verifiedAt;

        ProviderSymbolMapping toDomain(List<String> defaultTimeframes, Instant defaultVerification) {
            return new ProviderSymbolMapping(provider,
                    new CanonicalInstrumentId(baseAsset, quoteAsset, marketType, venue, contractType),
                    providerSymbol, enabled, sourceVersion,
                    supportedTimeframes == null ? defaultTimeframes : supportedTimeframes,
                    verifiedAt == null ? defaultVerification : verifiedAt);
        }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getBaseAsset() { return baseAsset; }
        public void setBaseAsset(String baseAsset) { this.baseAsset = baseAsset; }
        public String getQuoteAsset() { return quoteAsset; }
        public void setQuoteAsset(String quoteAsset) { this.quoteAsset = quoteAsset; }
        public MarketType getMarketType() { return marketType; }
        public void setMarketType(MarketType marketType) { this.marketType = marketType; }
        public String getVenue() { return venue; }
        public void setVenue(String venue) { this.venue = venue; }
        public ContractType getContractType() { return contractType; }
        public void setContractType(ContractType contractType) { this.contractType = contractType; }
        public String getProviderSymbol() { return providerSymbol; }
        public void setProviderSymbol(String providerSymbol) { this.providerSymbol = providerSymbol; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSourceVersion() { return sourceVersion; }
        public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
        public List<String> getSupportedTimeframes() { return supportedTimeframes; }
        public void setSupportedTimeframes(List<String> supportedTimeframes) {
            this.supportedTimeframes = supportedTimeframes;
        }
        public Instant getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    }
}
