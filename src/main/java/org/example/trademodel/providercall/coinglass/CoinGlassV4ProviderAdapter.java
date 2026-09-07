package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderFailureClassifier;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CoinGlassV4ProviderAdapter {
    private final CoinGlassProperties properties;
    private final CoinGlassV4Client client;
    private final CoinGlassSymbolMapper symbolMapper;
    private final CoinGlassV4ResponseValidator validator;
    private final CoinGlassProviderHealthService health;

    public CoinGlassV4ProviderAdapter(CoinGlassProperties properties,
                                      CoinGlassV4Client client,
                                      CoinGlassSymbolMapper symbolMapper,
                                      CoinGlassV4ResponseValidator validator,
                                      CoinGlassProviderHealthService health) {
        this.properties = properties;
        this.client = client;
        this.symbolMapper = symbolMapper;
        this.validator = validator;
        this.health = health;
    }

    public ProviderAdapterResponse<CoinGlassOpenInterestSnapshot> fetchOpenInterest(String internalSymbol) {
        CoinGlassSymbolMapper.CoinGlassSymbol symbol = symbolMapper.map(internalSymbol);
        begin(CoinGlassV4ResponseValidator.OI_CAPABILITY, internalSymbol);
        CoinGlassClientResponse response = client.get(CoinGlassV4ResponseValidator.OI_CAPABILITY,
                properties.getEndpoints().getOpenInterest(), Map.of("symbol", symbol.coinSymbol()));
        if (!response.successful()) return clientFailure(response, internalSymbol);
        return mapped(response, validator.openInterest(response.data(), symbol, response.fetchTime()), internalSymbol);
    }

    public ProviderAdapterResponse<CoinGlassFundingSnapshot> fetchFunding(String internalSymbol) {
        CoinGlassSymbolMapper.CoinGlassSymbol symbol = symbolMapper.map(internalSymbol);
        begin(CoinGlassV4ResponseValidator.FUNDING_CAPABILITY, internalSymbol);
        CoinGlassClientResponse response = client.get(CoinGlassV4ResponseValidator.FUNDING_CAPABILITY,
                properties.getEndpoints().getFunding(), Map.of(
                        "symbol", symbol.coinSymbol(), "interval", "1m", "limit", "1"));
        if (!response.successful()) return clientFailure(response, internalSymbol);
        return mapped(response, validator.funding(response.data(), symbol, response.fetchTime()), internalSymbol);
    }

    public ProviderAdapterResponse<CoinGlassLiquidationSnapshot> fetchLiquidation(String internalSymbol) {
        CoinGlassSymbolMapper.CoinGlassSymbol symbol = symbolMapper.map(internalSymbol);
        begin(CoinGlassV4ResponseValidator.LIQUIDATION_CAPABILITY, internalSymbol);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("exchange_list", properties.getLiquidationExchangeList());
        query.put("symbol", symbol.coinSymbol());
        query.put("interval", "1m");
        query.put("limit", "60");
        CoinGlassClientResponse response = client.get(CoinGlassV4ResponseValidator.LIQUIDATION_CAPABILITY,
                properties.getEndpoints().getLiquidation(), query);
        if (!response.successful()) return clientFailure(response, internalSymbol);
        return mapped(response, validator.liquidation(response.data(), symbol, response.fetchTime()), internalSymbol);
    }

    public ProviderAdapterResponse<CoinGlassLongShortSnapshot> fetchLongShortRatio(String internalSymbol) {
        CoinGlassSymbolMapper.CoinGlassSymbol symbol = symbolMapper.map(internalSymbol);
        begin(CoinGlassV4ResponseValidator.LONG_SHORT_CAPABILITY, internalSymbol);
        CoinGlassClientResponse response = client.get(CoinGlassV4ResponseValidator.LONG_SHORT_CAPABILITY,
                properties.getEndpoints().getLongShortRatio(), Map.of(
                        "exchange", properties.getLongShortExchange(),
                        "symbol", symbol.pairSymbol(), "interval", "1m", "limit", "1"));
        if (!response.successful()) return clientFailure(response, internalSymbol);
        return mapped(response, validator.longShort(response.data(), symbol, response.fetchTime(),
                properties.getLongShortExchange()), internalSymbol);
    }

    private void begin(String capability, String symbol) {
        health.beginDataset(capability, symbol, health.refreshCadence(capability, symbol));
    }

    private <T> ProviderAdapterResponse<T> mapped(
            CoinGlassClientResponse response, CoinGlassMappingResult<T> result, String symbol) {
        health.recordDataset(response.endpointCapabilityId(), symbol, result.status(), response.httpStatus(),
                response.providerStatusCode(), result.reasonCode(), response.rateLimit(), response.fetchTime(),
                result.providerDataTime(), health.refreshCadence(response.endpointCapabilityId(), symbol));
        if (result.status() == UnifiedSourceStatus.READY && result.payload() != null) {
            return ProviderAdapterResponse.ready(result.payload(), result.providerDataTime());
        }
        String reason = response.endpointCapabilityId() + ":" + result.reasonCode();
        return ProviderAdapterResponse.failed(result.status(), response.httpStatus(), reason,
                response.rateLimit() == null ? null : response.rateLimit().retryAfterSeconds());
    }

    private <T> ProviderAdapterResponse<T> clientFailure(CoinGlassClientResponse response, String symbol) {
        UnifiedSourceStatus status = switch (response.errorCode() == null ? "" : response.errorCode()) {
            case "RATE_LIMITED" -> UnifiedSourceStatus.DEGRADED;
            case "AUTHENTICATION_FAILED", "UPSTREAM_UNAVAILABLE", "PROVIDER_TRANSPORT_FAILED",
                    "MALFORMED_RESPONSE", "INVALID_PROVIDER_CONFIGURATION" -> UnifiedSourceStatus.ERROR;
            default -> UnifiedSourceStatus.ERROR;
        };
        String canonicalReason = ProviderFailureClassifier.canonicalReason(
                response.httpStatus(), response.errorCode());
        String reason = "REGION_RESTRICTED".equals(canonicalReason)
                ? canonicalReason : response.endpointCapabilityId() + ":" + canonicalReason;
        health.recordDataset(response.endpointCapabilityId(), symbol, status, response.httpStatus(),
                response.providerStatusCode(), reason, response.rateLimit(), response.fetchTime(), null,
                health.refreshCadence(response.endpointCapabilityId(), symbol));
        return ProviderAdapterResponse.failed(status, response.httpStatus(), reason,
                response.rateLimit() == null ? null : response.rateLimit().retryAfterSeconds());
    }
}
