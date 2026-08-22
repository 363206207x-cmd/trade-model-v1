package org.example.trademodel.controller;

import org.example.trademodel.common.GlobalExceptionHandler;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.uireview.UiReviewPositionMonitoringReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkspaceUiReviewPositionRuntimeTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedUserIdResolver resolver = mock(AuthenticatedUserIdResolver.class);
        when(resolver.requireCurrentUserId()).thenReturn(1L);
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkspacePositionMonitoringController(
                        resolver, new UiReviewPositionMonitoringReadService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listContainsTheThreeHomePositionIdentities() throws Exception {
        mockMvc.perform(get("/api/workspace/positions/monitoring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeCount").value(3))
                .andExpect(jsonPath("$.data.positions.length()").value(3))
                .andExpect(jsonPath("$.data.positions[0].position.id").value(7101))
                .andExpect(jsonPath("$.data.positions[1].position.id").value(7102))
                .andExpect(jsonPath("$.data.positions[2].position.id").value(7103));
    }

    @Test
    void everyDetailReturnsOnlyItsRequestedPositionIdentity() throws Exception {
        assertDetail(7101L, "BTCUSDT", "LONG", "SYSTEM_PLAN_POSITION");
        assertDetail(7102L, "ETHUSDT", "SHORT", "MANUAL_INDEPENDENT");
        assertDetail(7103L, "SOLUSDT", "LONG", "SYSTEM_PLAN_POSITION");
    }

    @Test
    void unknownUiReviewPositionFailsClosedWithoutFirstRowFallback() throws Exception {
        mockMvc.perform(get("/api/workspace/positions/7999/monitoring"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    private void assertDetail(long id, String symbol, String side, String sourceType) throws Exception {
        mockMvc.perform(get("/api/workspace/positions/{id}/monitoring", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.position.id").value(id))
                .andExpect(jsonPath("$.data.monitor.positionId").value(id))
                .andExpect(jsonPath("$.data.position.assetSymbol").value(symbol))
                .andExpect(jsonPath("$.data.monitor.symbol").value(symbol))
                .andExpect(jsonPath("$.data.position.side").value(side))
                .andExpect(jsonPath("$.data.position.sourceType").value(sourceType));
    }
}
