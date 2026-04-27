package org.example.trademodel.controller;

import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.PushRecheckScheduler;
import org.example.trademodel.service.PushRecheckService;
import org.example.trademodel.vo.PushRecheckOpsOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PushRecheckControllerTest {

    @Mock
    private PushRecheckService pushRecheckService;
    @Mock
    private PushRecheckScheduler pushRecheckScheduler;
    @Mock
    private PushRecheckDispatchConfigService dispatchConfigService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PushRecheckController controller = new PushRecheckController(
                pushRecheckService,
                pushRecheckScheduler,
                dispatchConfigService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void opsOverview_shouldUseDefaultsWhenLimitNotProvided() throws Exception {
        PushRecheckOpsOverviewVO body = new PushRecheckOpsOverviewVO();
        PushRecheckOpsOverviewVO.ConfigSummary config = new PushRecheckOpsOverviewVO.ConfigSummary();
        config.setLimit(50);
        body.setConfig(config);
        body.setRecentLogs(List.of());
        when(pushRecheckService.getOpsOverview(null, null, null, null)).thenReturn(body);

        mockMvc.perform(get("/api/push/recheck/ops/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.config.limit").value(50));

        verify(pushRecheckService).getOpsOverview(null, null, null, null);
    }

    @Test
    void opsOverview_shouldPassThroughQueryParams() throws Exception {
        PushRecheckOpsOverviewVO body = new PushRecheckOpsOverviewVO();
        body.setRecentLogs(List.of());
        when(pushRecheckService.getOpsOverview("B1", "I1", 6, 12)).thenReturn(body);

        mockMvc.perform(get("/api/push/recheck/ops/overview")
                        .param("dispatchBatchId", "B1")
                        .param("dispatchInstructionId", "I1")
                        .param("auditLimit", "6")
                        .param("logLimit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(pushRecheckService).getOpsOverview("B1", "I1", 6, 12);
    }
}
