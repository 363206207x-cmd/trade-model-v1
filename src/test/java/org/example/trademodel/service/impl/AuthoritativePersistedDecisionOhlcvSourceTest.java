package org.example.trademodel.service.impl;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.RuntimeKlineContextAssemblyService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthoritativePersistedDecisionOhlcvSourceTest {

    @Test
    void exposesDatabaseNewestFirstWindowAsChronologicalDecisionSeries() {
        PersistedOhlcvQueryService queryService = mock(PersistedOhlcvQueryService.class);
        RuntimeKlineContextAssemblyService assemblyService = mock(RuntimeKlineContextAssemblyService.class);
        PersistedOhlcvReadinessResult readiness = new PersistedOhlcvReadinessResult();
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        context.setMissingFields(List.of());
        context.setKlineItems(List.of(bar(200L, "102"), bar(100L, "101")));
        when(queryService.evaluateReadiness("BTCUSDT", "5m", 2, 600_000L)).thenReturn(readiness);
        when(assemblyService.assemble(readiness)).thenReturn(context);

        AuthoritativePersistedDecisionOhlcvSource source =
                new AuthoritativePersistedDecisionOhlcvSource(queryService, assemblyService);

        List<String[]> result = source.readClosedBars("BTCUSDT", "5m", 2, "trace-1");

        assertThat(result).extracting(row -> row[0]).containsExactly("100", "200");
        assertThat(result).extracting(row -> row[4]).containsExactly("101", "102");
    }

    @Test
    void consumesVersionedProviderMatrixTtlInsteadOfASecondHardcodedWindow() {
        PersistedOhlcvQueryService queryService = mock(PersistedOhlcvQueryService.class);
        RuntimeKlineContextAssemblyService assemblyService = mock(RuntimeKlineContextAssemblyService.class);
        FundamentalAiV41Properties properties = FundamentalAiV41Properties.contractFixture();
        properties.getProviderMatrix().setFifteenMinuteTtlSeconds(1_234);
        PersistedOhlcvReadinessResult readiness = new PersistedOhlcvReadinessResult();
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        context.setMissingFields(List.of());
        context.setKlineItems(List.of(bar(100L, "101")));
        when(queryService.evaluateReadiness("ETHUSDT", "15m", 1, 1_234_000L)).thenReturn(readiness);
        when(assemblyService.assemble(readiness)).thenReturn(context);

        AuthoritativePersistedDecisionOhlcvSource source =
                new AuthoritativePersistedDecisionOhlcvSource(queryService, assemblyService, properties);

        assertThat(source.readClosedBars("ETHUSDT", "15m", 1, "trace-ttl")).hasSize(1);
        verify(queryService).evaluateReadiness("ETHUSDT", "15m", 1, 1_234_000L);
    }

    private RuntimeKlineItemDTO bar(long openTimeMs, String close) {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setOpenTimeMs(openTimeMs);
        item.setOpenPrice(new BigDecimal(close).subtract(BigDecimal.ONE));
        item.setHighPrice(new BigDecimal(close).add(BigDecimal.ONE));
        item.setLowPrice(new BigDecimal(close).subtract(BigDecimal.valueOf(2)));
        item.setClosePrice(new BigDecimal(close));
        item.setVolume(BigDecimal.TEN);
        return item;
    }
}
