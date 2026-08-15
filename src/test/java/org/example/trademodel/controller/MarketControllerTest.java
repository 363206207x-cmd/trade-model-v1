package org.example.trademodel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.math.BigDecimal;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MarketControllerTest {

    @Test
    void quoteStatusEndpointReturnsReviewOnlyReadyForFreshQuote() throws Exception {
        MockMvc mockMvc = mockMvcWith(symbol -> Optional.of(snapshot(System.currentTimeMillis())));

        mockMvc.perform(get("/api/market/quote-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MARKETQUOTE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.sampleSymbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.symbols[0]").value("BTCUSDT"))
                .andExpect(jsonPath("$.source").value("BINANCE"))
                .andExpect(jsonPath("$.sourceType").value("BINANCE_24H_TICKER"))
                .andExpect(jsonPath("$.fresh").value(true))
                .andExpect(jsonPath("$.fallbackActive").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("OK"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.dashboardOnlySample").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(false));
    }

    @Test
    void quoteStatusEndpointFailsClosedWhenQuoteMissing() throws Exception {
        MockMvc mockMvc = mockMvcWith(symbol -> Optional.empty());

        mockMvc.perform(get("/api/market/quote-status").param("symbol", "ETHUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MARKETQUOTE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sampleSymbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.source").value("UNKNOWN"))
                .andExpect(jsonPath("$.sourceType").value("MISSING"))
                .andExpect(jsonPath("$.fresh").value(false))
                .andExpect(jsonPath("$.fallbackActive").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.reason").value("QUOTE_UNAVAILABLE"));
    }

    @Test
    void quoteStatusEndpointDoesNotExposePointOrTradingFields() throws Exception {
        MockMvc mockMvc = mockMvcWith(symbol -> Optional.of(snapshot(System.currentTimeMillis())));

        MvcResult result = mockMvc.perform(get("/api/market/quote-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(
                "candidateRanking",
                "score",
                "finalDirection",
                "entry",
                "stop",
                "takeProfit",
                "riskReward",
                "positionSize",
                "leverage",
                "placeOrder",
                "createOrder",
                "submitOrder",
                "auto-trading",
                "order action"
        );
    }

    private static MockMvc mockMvcWith(MarketQuoteClient marketQuoteClient) {
        MarketPriceSnapshotService snapshotService = MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient);
        return MockMvcBuilders.standaloneSetup(
                new MarketController(mock(RealMarketDataFetcherService.class), snapshotService)
        ).build();
    }

    private static MarketQuoteSnapshot snapshot(long fetchedAtEpochMillis) {
        MarketQuoteSnapshot snapshot = new MarketQuoteSnapshot();
        snapshot.setProvider("binance");
        snapshot.setSymbolNormalized("BTCUSDT");
        snapshot.setLastPrice(BigDecimal.ONE);
        snapshot.setFetchedAtEpochMillis(fetchedAtEpochMillis);
        return snapshot;
    }
}
