package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.position.PositionProvider;
import org.example.trademodel.position.PositionProviderResult;
import org.junit.jupiter.api.Test;

class PositionSyncServiceTest {

    @Test
    void nonAuthoritativeSnapshotNeverMutatesOrClosesStoredPositions() {
        PositionProvider provider = mock(PositionProvider.class);
        RealPositionMapper mapper = mock(RealPositionMapper.class);
        when(mapper.countOpenPositions()).thenReturn(2);
        when(provider.fetchOpenPositions()).thenReturn(new PositionProviderResult(
                "UNAVAILABLE",
                "fail-closed-position-provider",
                List.of(),
                "BINANCE",
                true,
                "credentials missing",
                false
        ));
        PositionSyncService service = service(provider, mapper);

        service.syncPositions();

        verify(mapper, never()).updateOpenPositionBySymbol(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).insertOpenPosition(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(mapper, never()).closeMissingOpenPositions(anyList(), any());
        assertThat(service.getPositionSyncStatus().getLastSyncSuccess()).isFalse();
        assertThat(service.getPositionSyncStatus().getLastSyncMessage())
                .contains("not authoritative");
        assertThat(service.getPositionSyncStatus().getCurrentOpenPositionCount()).isEqualTo(2);
    }

    @Test
    void authoritativeEmptySnapshotMayCloseMissingExchangePositions() {
        PositionProvider provider = mock(PositionProvider.class);
        RealPositionMapper mapper = mock(RealPositionMapper.class);
        when(mapper.countOpenPositions()).thenReturn(2, 0);
        when(mapper.closeMissingOpenPositions(anyList(), any())).thenReturn(2);
        when(provider.fetchOpenPositions())
                .thenReturn(new PositionProviderResult("BINANCE", "binance-provider-v1", List.of()));
        PositionSyncService service = service(provider, mapper);

        service.syncPositions();

        verify(mapper).closeMissingOpenPositions(anyList(), any());
        assertThat(service.getPositionSyncStatus().getLastSyncSuccess()).isTrue();
        assertThat(service.getPositionSyncStatus().getLastClosedCount()).isEqualTo(2);
    }

    private PositionSyncService service(PositionProvider provider, RealPositionMapper mapper) {
        return new PositionSyncService(provider, mapper, new RuntimeMetricService(), "BINANCE");
    }
}
