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
        var service = new AssetCardService(new AssetCardProperties(), market, mapper, pool, events,
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
            verifyNoInteractions(market, events);
            verify(pool, never()).listScanSymbols();
            assertThatThrownBy(() -> service.snapshotsForUser(userId, List.of("ETHUSDT")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.snapshotsForUser(null, List.of("BTCUSDT")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.snapshotsForUser(userId, List.of("BTCUSDT", "BTCUSDT")))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally { service.close(); }
    }
}
