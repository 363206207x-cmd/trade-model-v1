package org.example.trademodel.providercall;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.client.OpenInterestClient;
import org.example.trademodel.market.client.PerpFundingRateClient;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderCapabilityState;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.snapshot.BinanceDerivativesSnapshotService;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderConsumerCapabilityGateTest {

    @Test
    void priceRefreshCannotReachQuoteClientWhenCapabilityIsRejected() {
        ProviderCapabilityRegistry gate = mock(ProviderCapabilityRegistry.class);
        when(gate.authorize("BINANCE", "AAVEUSDT", "GLOBAL", MarketType.SPOT, ContractType.NONE))
                .thenReturn(blocked(MarketType.SPOT, ContractType.NONE, ProviderCapabilityState.UNSUPPORTED_SYMBOL));
        MarketQuoteClient client = mock(MarketQuoteClient.class);
        ProviderSnapshotRefreshService refresh = mock(ProviderSnapshotRefreshService.class);
        MarketPriceSnapshotService service = new MarketPriceSnapshotService(
                mock(ProviderSnapshotQueryService.class), refresh, client,
                mock(ProviderSymbolMappingRegistry.class), mock(ProviderRequestKeyFactory.class), gate,
                Clock.systemUTC());

        ProviderCallResult<?> result = service.get("AAVEUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(30), "trace");

        assertThat(result.payload()).isNull();
        assertThat(result.metadata().errorCode()).isEqualTo("UNSUPPORTED_SYMBOL");
        verifyNoInteractions(client, refresh);
    }

    @Test
    void derivativesRefreshCannotReachFundingOrOpenInterestWhenExactPerpetualCapabilityIsRejected() {
        ProviderCapabilityRegistry gate = mock(ProviderCapabilityRegistry.class);
        when(gate.authorize("BINANCE", "BTCUSDT", "GLOBAL", MarketType.PERPETUAL, ContractType.LINEAR))
                .thenReturn(blocked(MarketType.PERPETUAL, ContractType.LINEAR,
                        ProviderCapabilityState.REGION_RESTRICTED));
        PerpFundingRateClient funding = mock(PerpFundingRateClient.class);
        OpenInterestClient openInterest = mock(OpenInterestClient.class);
        ProviderCallCoordinator coordinator = mock(ProviderCallCoordinator.class);
        BinanceDerivativesSnapshotService service = new BinanceDerivativesSnapshotService(coordinator,
                funding, openInterest, mock(ProviderSymbolMappingRegistry.class),
                mock(ProviderRequestKeyFactory.class), gate, Clock.systemUTC());

        ProviderCallResult<?> result = service.get("BTCUSDT", AssetPriority.P0_POSITION,
                Duration.ofSeconds(30), "trace");

        assertThat(result.payload()).isNull();
        assertThat(result.metadata().errorCode()).isEqualTo("REGION_RESTRICTED");
        verifyNoInteractions(funding, openInterest, coordinator);
    }

    @Test
    void coordinatedOhlcvRefreshCannotReachProviderOrWriterWhenGateRejects() {
        ProviderCapabilityRegistry gate = mock(ProviderCapabilityRegistry.class);
        when(gate.authorize("BINANCE", "AAVE/USDT", "5m", MarketType.SPOT, ContractType.NONE))
                .thenReturn(blocked(MarketType.SPOT, ContractType.NONE, ProviderCapabilityState.SOURCE_UNAVAILABLE));
        PublicOhlcvProvider provider = mock(PublicOhlcvProvider.class);
        PersistedOhlcvIngestionService writer = mock(PersistedOhlcvIngestionService.class);
        ProviderCallCoordinator coordinator = mock(ProviderCallCoordinator.class);
        CoordinatedOhlcvSnapshotService service = new CoordinatedOhlcvSnapshotService(coordinator, provider, writer,
                mock(ProviderSymbolMappingRegistry.class), mock(ProviderRequestKeyFactory.class), gate,
                Clock.systemUTC());

        ProviderCallResult<?> result = service.refresh(new CanonicalInstrumentId("AAVE", "USDT",
                        MarketType.SPOT, "BINANCE", ContractType.NONE), "5m", 100,
                AssetPriority.P1_WATCHLIST, "trace");

        assertThat(result.payload()).isNull();
        assertThat(result.metadata().errorCode()).isEqualTo("SOURCE_UNAVAILABLE");
        verify(provider, never()).fetchClosedBars(anyString(), anyString(), anyInt(), anyString());
        verifyNoInteractions(writer, coordinator);
    }

    private static ProviderInstrumentCapability blocked(MarketType marketType,
                                                        ContractType contractType,
                                                        ProviderCapabilityState state) {
        Instant now = Instant.now();
        CanonicalInstrumentId instrument = new CanonicalInstrumentId(
                marketType == MarketType.SPOT ? "AAVE" : "BTC", "USDT", marketType, "BINANCE", contractType);
        return new ProviderInstrumentCapability("BINANCE", instrument.canonical(), instrument.baseAsset(),
                instrument.quoteAsset(), marketType, contractType, null, List.of("5m"), state,
                "TEST", null, state.name(), now);
    }
}
