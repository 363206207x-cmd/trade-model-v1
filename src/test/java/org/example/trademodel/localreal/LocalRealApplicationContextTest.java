package org.example.trademodel.localreal;

import org.example.trademodel.TradeModelApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:local_real_context;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.flyway.enabled=false",
        "trade-model.schedulers.enabled=false",
        "trade-model.ohlcv.public-provider.external-calls-enabled=false",
        "trade-model.ohlcv.kraken.external-calls-enabled=false",
        "trade-model.ohlcv.binance.external-calls-enabled=false",
        "trade-model.provider-call.external-calls-enabled=false"
})
@ActiveProfiles("local-real")
@AutoConfigureMockMvc
class LocalRealApplicationContextTest {
    @Autowired MockMvc mvc;

    @Test
    void localRealProfileLoadsWithoutExternalCallsAndExposesTruthfulStatus() throws Exception {
        mvc.perform(get("/api/local-real/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("LOCAL_REAL_DATA"))
                .andExpect(jsonPath("$.marketData.closedBarCount").value(0))
                .andExpect(jsonPath("$.assets[0].marketDataStatus").value("MARKET_DATA_NOT_READY"))
                .andExpect(jsonPath("$.assets[0].realMarketEnvironment").value(false))
                .andExpect(jsonPath("$.assets[0].analysisStatus").value("WAITING"))
                .andExpect(jsonPath("$.ai.enabled").value(false))
                .andExpect(jsonPath("$.notAutoTrading").value(true));

        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
