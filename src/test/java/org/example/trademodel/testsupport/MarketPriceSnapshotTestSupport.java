package org.example.trademodel.testsupport;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.client.OpenInterestClient;
import org.example.trademodel.market.client.PerpFundingRateClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderCircuitState;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.BinanceDerivativesSnapshotService;
import org.example.trademodel.providercall.snapshot.MinimalDerivativesSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.mockito.stubbing.Answer;

public final class MarketPriceSnapshotTestSupport {
    private MarketPriceSnapshotTestSupport() {
    }

    public static MarketPriceSnapshotService snapshotService(MarketQuoteClient client) {
        MarketPriceSnapshotService service = mock(MarketPriceSnapshotService.class);
        Answer<ProviderCallResult<MarketPriceSnapshot>> answer = invocation -> {
            String symbol = invocation.getArgument(0);
            Optional<MarketQuoteSnapshot> raw = client.fetch24hTicker(symbol);
            if (raw.isEmpty()) return unavailable(symbol, "QUOTE_UNAVAILABLE");
            MarketQuoteSnapshot quote = raw.get();
            if (quote.getLastPrice() == null) return unavailable(symbol, "QUOTE_UNAVAILABLE");
            if (quote.getLastPrice().signum() <= 0) {
                return unavailable(symbol, "INVALID_MARKET_PRICE");
            }
            Instant fetchedAt = quote.getFetchedAtEpochMillis() > 0
                    ? Instant.ofEpochMilli(quote.getFetchedAtEpochMillis()) : Instant.now();
            ProviderSnapshotMetadata metadata = metadata(symbol, fetchedAt, UnifiedSourceStatus.READY,
                    SnapshotFreshnessStatus.FRESH, null);
            MarketPriceSnapshot payload = new MarketPriceSnapshot(quote.getSymbolNormalized(), quote.getLastPrice(),
                    null, null, null, quote.getHighPrice(), quote.getLowPrice(), quote.getPriceChangePercent24h(),
                    quote.getProvider(), fetchedAt, metadata);
            return new ProviderCallResult<>(payload, metadata, budget());
        };
        lenient().when(service.get(anyString(), any(), any(), anyString())).thenAnswer(answer);
        lenient().when(service.peek(anyString(), any(), any(), anyString())).thenAnswer(answer);
        return service;
    }

    public static BinanceDerivativesSnapshotService derivativesService(PerpFundingRateClient fundingClient,
                                                                        OpenInterestClient openInterestClient) {
        BinanceDerivativesSnapshotService service = mock(BinanceDerivativesSnapshotService.class);
        lenient().when(service.get(anyString(), any(), any(), anyString())).thenAnswer(invocation -> {
            String symbol = invocation.getArgument(0);
            java.math.BigDecimal funding = fundingClient == null ? null
                    : fundingClient.fetchLastFundingRate(symbol).orElse(null);
            java.math.BigDecimal oi = openInterestClient == null ? null
                    : openInterestClient.fetchOpenInterest(symbol).orElse(null);
            if (funding == null && oi == null) return new ProviderCallResult<>(null, null, budget());
            Instant now = Instant.now();
            ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("TEST", ProviderDatasetType.DERIVATIVES,
                    symbol, "GLOBAL", now, now, now.plusSeconds(60), UnifiedSourceStatus.READY,
                    SnapshotFreshnessStatus.FRESH, "test-trace", "test-derivatives", false, false, null, List.of());
            return new ProviderCallResult<>(new MinimalDerivativesSnapshot(symbol, funding, oi,
                    "TEST_MINIMAL", metadata), metadata, budget());
        });
        return service;
    }

    public static ProviderCallResult<MarketPriceSnapshot> unavailable(String symbol, String code) {
        return new ProviderCallResult<>(null, metadata(symbol, Instant.now(), UnifiedSourceStatus.ERROR,
                SnapshotFreshnessStatus.UNAVAILABLE, code), budget());
    }

    private static ProviderSnapshotMetadata metadata(String symbol, Instant now, UnifiedSourceStatus status,
                                                       SnapshotFreshnessStatus freshness, String code) {
        return new ProviderSnapshotMetadata("TEST", ProviderDatasetType.PRICE, symbol, "GLOBAL", now, now,
                now.plusSeconds(30), status, freshness, "test-trace", "test-key", false, false, code,
                code == null ? List.of() : List.of(code));
    }

    private static ProviderBudgetState budget() {
        return new ProviderBudgetState("TEST", 100, 80, .8, .2, 1, 79, null,
                ProviderCircuitState.CLOSED, null);
    }
}
