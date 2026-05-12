package org.example.trademodel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.PushWatchlistConfigRequest;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.vo.PushWatchlistConfigAuditVO;
import org.example.trademodel.vo.PushWatchlistConfigVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RuleControllerWatchlistTest {

    @Mock
    private RuleConfigService ruleConfigService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RuleController controller = new RuleController(ruleConfigService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getPushWatchlist_shouldReturnCurrentConfig() throws Exception {
        PushWatchlistConfigVO config = new PushWatchlistConfigVO();
        config.setRuleKey("push.watchlist.symbols");
        config.setSymbols(List.of("BTCUSDT", "ETHUSDT"));
        config.setEnabled(true);
        config.setRuleValue("BTCUSDT,ETHUSDT");
        when(ruleConfigService.getPushWatchlistConfig()).thenReturn(config);

        mockMvc.perform(get("/api/rule/push-watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ruleKey").value("push.watchlist.symbols"))
                .andExpect(jsonPath("$.data.symbols[0]").value("BTCUSDT"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.ruleValue").value("BTCUSDT,ETHUSDT"));

        verify(ruleConfigService).getPushWatchlistConfig();
    }

    @Test
    void updatePushWatchlist_shouldDelegateToService() throws Exception {
        PushWatchlistConfigVO updated = new PushWatchlistConfigVO();
        updated.setRuleKey("push.watchlist.symbols");
        updated.setSymbols(List.of("BTCUSDT", "SOLUSDT"));
        updated.setEnabled(true);
        updated.setRuleValue("BTCUSDT,SOLUSDT");
        when(ruleConfigService.updatePushWatchlistConfig(any(PushWatchlistConfigRequest.class)))
                .thenReturn(updated);

        PushWatchlistConfigRequest request = new PushWatchlistConfigRequest();
        request.setSymbols(List.of("btcusdt", " SOLUSDT "));
        request.setEnabled(true);
        request.setOperator("tester");
        request.setReason("add SOL");

        mockMvc.perform(post("/api/rule/push-watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.symbols[1]").value("SOLUSDT"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        ArgumentCaptor<PushWatchlistConfigRequest> captor =
                ArgumentCaptor.forClass(PushWatchlistConfigRequest.class);
        verify(ruleConfigService).updatePushWatchlistConfig(captor.capture());
        assertThat(captor.getValue().getOperator()).isEqualTo("tester");
        assertThat(captor.getValue().getReason()).isEqualTo("add SOL");
    }

    @Test
    void listPushWatchlistAudit_shouldUseDefaultLimit() throws Exception {
        PushWatchlistConfigAuditVO audit = new PushWatchlistConfigAuditVO();
        audit.setRuleKey("push.watchlist.symbols");
        audit.setBeforeSymbols("BTCUSDT");
        audit.setAfterSymbols("BTCUSDT,ETHUSDT");
        audit.setChangedBy("tester");
        audit.setChangeReason("add ETH");
        when(ruleConfigService.listPushWatchlistConfigAudit(20)).thenReturn(List.of(audit));

        mockMvc.perform(get("/api/rule/push-watchlist/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].ruleKey").value("push.watchlist.symbols"))
                .andExpect(jsonPath("$.data[0].changedBy").value("tester"))
                .andExpect(jsonPath("$.data[0].changeReason").value("add ETH"));

        verify(ruleConfigService).listPushWatchlistConfigAudit(20);
    }

    @Test
    void updatePushWatchlist_whenServiceRejectsRequest_shouldReturnFailureEnvelope() throws Exception {
        when(ruleConfigService.updatePushWatchlistConfig(any(PushWatchlistConfigRequest.class)))
                .thenThrow(new IllegalArgumentException("operator is required"));

        PushWatchlistConfigRequest request = new PushWatchlistConfigRequest();
        request.setSymbols(List.of("BTCUSDT"));
        request.setEnabled(true);
        request.setReason("missing operator");

        mockMvc.perform(post("/api/rule/push-watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("推送观察列表配置更新失败: operator is required"));
    }
}
