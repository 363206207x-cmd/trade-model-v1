package org.example.trademodel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RuleControllerTest {

    private static final String WATCHLIST_RULE_KEY = "push.watchlist.symbols";

    @Test
    void pushWatchlistEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        MockMvc mockMvc = mockMvcWith(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT, ethusdt")));

        mockMvc.perform(get("/api/rule/push-watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WATCHLIST_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.data.configKey").value(WATCHLIST_RULE_KEY))
                .andExpect(jsonPath("$.data.symbols[0]").value("BTCUSDT"))
                .andExpect(jsonPath("$.data.symbols[1]").value("ETHUSDT"))
                .andExpect(jsonPath("$.data.source").value("DB"))
                .andExpect(jsonPath("$.data.empty").value(false))
                .andExpect(jsonPath("$.data.failClosed").value(false))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.data.reason").value("REVIEW_ONLY_DB_WATCHLIST_READ"));
    }

    @Test
    void pushWatchlistEndpointFailsClosedWhenConfigMissing() throws Exception {
        MockMvc mockMvc = mockMvcWith(Map.of());

        mockMvc.perform(get("/api/rule/push-watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WATCHLIST_CONFIG_MISSING"))
                .andExpect(jsonPath("$.data.source").value("MISSING"))
                .andExpect(jsonPath("$.data.empty").value(true))
                .andExpect(jsonPath("$.data.failClosed").value(true))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.data.reason").value("WATCHLIST_CONFIG_MISSING"));
    }

    @Test
    void pushWatchlistEndpointFailsClosedWhenConfigEmpty() throws Exception {
        MockMvc mockMvc = mockMvcWith(Map.of(WATCHLIST_RULE_KEY, ruleConfig(" ")));

        mockMvc.perform(get("/api/rule/push-watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WATCHLIST_EMPTY_FAIL_CLOSED"))
                .andExpect(jsonPath("$.data.source").value("DB"))
                .andExpect(jsonPath("$.data.empty").value(true))
                .andExpect(jsonPath("$.data.failClosed").value(true))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.data.reason").value("WATCHLIST_EMPTY"));
    }

    @Test
    void pushWatchlistEndpointDoesNotExposeExecutableOrExternalRuntimeFields() throws Exception {
        MockMvc mockMvc = mockMvcWith(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT")));

        MvcResult result = mockMvc.perform(get("/api/rule/push-watchlist"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(
                "MarketQuote",
                "candidateRanking",
                "finalDirection",
                "placeOrder",
                "createOrder",
                "submitOrder",
                "auto-trading",
                "order execution"
        );
    }

    private static MockMvc mockMvcWith(Map<String, RuleConfigDO> ruleConfigMap) {
        RuleConfigService service = new StubRuleConfigService(ruleConfigMap);
        return MockMvcBuilders.standaloneSetup(new RuleController(service)).build();
    }

    private static RuleConfigDO ruleConfig(String ruleValue) {
        RuleConfigDO ruleConfig = new RuleConfigDO();
        ruleConfig.setRuleKey(WATCHLIST_RULE_KEY);
        ruleConfig.setRuleValue(ruleValue);
        ruleConfig.setEnabled(true);
        return ruleConfig;
    }

    private static final class StubRuleConfigService implements RuleConfigService {
        private final Map<String, RuleConfigDO> ruleConfigMap;

        private StubRuleConfigService(Map<String, RuleConfigDO> ruleConfigMap) {
            this.ruleConfigMap = ruleConfigMap;
        }

        @Override
        public Map<String, RuleConfigDO> getRuleConfigMap() {
            return ruleConfigMap;
        }

        @Override
        public void reloadRules() {
        }
    }
}
