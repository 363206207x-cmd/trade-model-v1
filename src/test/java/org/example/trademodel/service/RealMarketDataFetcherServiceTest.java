package org.example.trademodel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.ohlcv.PublicKlineFetchResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealMarketDataFetcherServiceTest {
    @Test
    void binance451IsClassifiedAsRegionRestrictedWithoutResponseExposure() {
        RestOperations transport = mock(RestOperations.class);
        when(transport.exchange(anyString(), any(), any(), (Class<String>) any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS,
                        "restricted", HttpHeaders.EMPTY, new byte[0], null));
        RealMarketDataFetcherService service = new RealMarketDataFetcherService(
                mock(AnalysisSchedulerService.class), transport, new ObjectMapper());

        PublicKlineFetchResult result = service.fetchKlinesDetailed("BTCUSDT", "5m", 100);

        assertThat(result.httpStatus()).isEqualTo(451);
        assertThat(result.reasonCode()).isEqualTo("REGION_RESTRICTED");
        assertThat(result.rows()).isEmpty();
    }
}
