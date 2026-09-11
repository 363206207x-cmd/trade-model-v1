package org.example.trademodel.assetcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.mapper.AssetCardMapper;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.v41.DashboardLiveEventService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardIsolationIntegrationTest {
    @Test
    void repeatedCardReadsNeverRegisterSubscribePublishOrWrite() {
        Long userId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        var mapper = mock(AssetCardMapper.class);
        var pool = mock(AssetPoolService.class);
        var market = mock(AssetCardMarketDataService.class);
        var events = mock(DashboardLiveEventService.class);
        when(pool.listForUser(userId)).thenReturn(List.of(new AssetPoolAssetDTO(1L, "BTCUSDT", "Bitcoin",
                "SPOT", "USDT", true, 1, "USER")));
        var properties = new AssetCardProperties();
        properties.setEnabled(true);
        properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
        var service = new AssetCardService(properties, market, mapper, pool, events,
                new ObjectMapper().findAndRegisterModules());
        try {
            for (int i = 0; i < 10; i++) {
                var cards = service.snapshotsForUser(userId, List.of("BTCUSDT"));
                assertThat(cards).hasSize(1);
                assertThat(cards.get(0).signal().calibratedConfidence()).isNull();
                assertThat(cards.get(0).signal().direction()).isNull();
            }
            verify(mapper, times(10)).selectSnapshotJson("BTCUSDT");
            verifyNoMoreInteractions(mapper);
            // quote() only reads the in-memory quote map and applies freshness; it never subscribes or fetches.
            verify(market, times(10)).quote(eq("BTCUSDT"), any(java.time.Instant.class));
            verifyNoMoreInteractions(market);
            verifyNoInteractions(events);
            verify(pool, never()).listScanSymbols();
            assertThatThrownBy(() -> service.snapshotsForUser(userId, List.of("ETHUSDT")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.snapshotsForUser(null, List.of("BTCUSDT")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.snapshotsForUser(userId, List.of("BTCUSDT", "BTCUSDT")))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(pool, times(12)).listForUser(userId);
            verifyNoMoreInteractions(pool, mapper, market, events);
        } finally { service.close(); }
    }

    @Test
    void disabledAndShadowCardReadsExposeNoSnapshotButRetainUserMembershipValidation() {
        for (boolean enabled : List.of(false, true)) {
            var properties = new AssetCardProperties();
            properties.setEnabled(enabled);
            properties.setModelMode(enabled ? AssetCardProperties.ModelMode.SHADOW : AssetCardProperties.ModelMode.ACTIVE);
            var mapper = mock(AssetCardMapper.class);
            var pool = mock(AssetPoolService.class);
            var market = mock(AssetCardMarketDataService.class);
            var events = mock(DashboardLiveEventService.class);
            when(pool.listForUser(7L)).thenReturn(List.of(new AssetPoolAssetDTO(1L, "BTCUSDT", "Bitcoin",
                    "SPOT", "USDT", true, 1, "USER")));
            var service = new AssetCardService(properties, market, mapper, pool, events,
                    new ObjectMapper().findAndRegisterModules());
            try {
                for (int i = 0; i < 10; i++) assertThat(service.snapshotsForUser(7L, List.of("BTCUSDT"))).isEmpty();
                assertThatThrownBy(() -> service.snapshotsForUser(7L, List.of("ETHUSDT")))
                        .isInstanceOf(IllegalArgumentException.class);
                verify(pool, times(11)).listForUser(7L);
                verifyNoMoreInteractions(pool);
                verifyNoInteractions(mapper, market, events);
            } finally { service.close(); }
        }
    }
}
