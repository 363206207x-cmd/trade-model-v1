package org.example.trademodel.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SwitchablePositionProviderTest {

    @Test
    void simulatedProviderIsEmptyAndNonAuthoritative() {
        BinancePositionProvider binance = mock(BinancePositionProvider.class);
        SwitchablePositionProvider provider = provider("SIMULATED", binance);

        PositionProviderResult result = provider.fetchOpenPositions();

        assertThat(result.getOpenPositions()).isEmpty();
        assertThat(result.isAuthoritativeSnapshot()).isFalse();
        assertThat(result.getFallbackReason())
                .isEqualTo("SIMULATED_POSITION_SOURCE_DISABLED_BY_PRODUCT_CONTRACT");
        verify(binance, never()).fetchOpenPositions();
    }

    @Test
    void missingBinanceCredentialsFailsClosedWithoutSyntheticFallback() {
        BinancePositionProvider binance = mock(BinancePositionProvider.class);
        when(binance.hasCredentials()).thenReturn(false);
        SwitchablePositionProvider provider = provider("BINANCE", binance);

        PositionProviderResult result = provider.fetchOpenPositions();

        assertThat(result.getSourceType()).isEqualTo("UNAVAILABLE");
        assertThat(result.getOpenPositions()).isEmpty();
        assertThat(result.isFallbackOccurred()).isTrue();
        assertThat(result.isAuthoritativeSnapshot()).isFalse();
        verify(binance, never()).fetchOpenPositions();
    }

    @Test
    void providerFailureFailsClosedWithoutSyntheticPositions() {
        BinancePositionProvider binance = mock(BinancePositionProvider.class);
        when(binance.hasCredentials()).thenReturn(true);
        when(binance.fetchOpenPositions()).thenThrow(new IllegalStateException("provider unavailable"));
        SwitchablePositionProvider provider = provider("BINANCE", binance);

        PositionProviderResult result = provider.fetchOpenPositions();

        assertThat(result.getSourceType()).isEqualTo("UNAVAILABLE");
        assertThat(result.getOpenPositions()).isEmpty();
        assertThat(result.isAuthoritativeSnapshot()).isFalse();
        assertThat(result.getFallbackReason()).contains("provider unavailable");
    }

    @Test
    void verifiedBinanceSnapshotRemainsAuthoritative() {
        BinancePositionProvider binance = mock(BinancePositionProvider.class);
        when(binance.hasCredentials()).thenReturn(true);
        when(binance.fetchOpenPositions())
                .thenReturn(new PositionProviderResult("BINANCE", "binance-provider-v1", List.of()));
        SwitchablePositionProvider provider = provider("BINANCE", binance);

        PositionProviderResult result = provider.fetchOpenPositions();

        assertThat(result.isAuthoritativeSnapshot()).isTrue();
        assertThat(result.isFallbackOccurred()).isFalse();
        assertThat(result.getOpenPositions()).isEmpty();
    }

    private SwitchablePositionProvider provider(String providerType, BinancePositionProvider binance) {
        SwitchablePositionProvider provider = new SwitchablePositionProvider(
                new SimulatedPositionProvider(),
                binance
        );
        ReflectionTestUtils.setField(provider, "providerType", providerType);
        return provider;
    }
}
