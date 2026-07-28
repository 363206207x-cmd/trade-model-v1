package org.example.trademodel.controller;

import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.vo.DashboardHomeVO;
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
class DashboardHomeControllerTest {
    @Mock
    private DashboardHomeService dashboardHomeService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DashboardHomeController(dashboardHomeService, authenticatedUserIdResolver)).build();
    }

    @Test
    void homeReturnsApiResponseSuccess() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 6, null)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.selectedSymbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.data.safety.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.safety.notAutoTrading").value(true));

        verify(dashboardHomeService).getHomeForUser(7L, "BTCUSDT", 6, null);
    }

    @Test
    void homeSerializesBusinessLabelsAndFailClosedPlanSemantics() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");

        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setSymbol("BTC/USDT");
        asset.setRawSymbol("BTCUSDT");
        asset.setAnalysisId("analysis-BTCUSDT-exact");
        asset.setAssetState("CONFUSED");
        asset.setAssetStateLabel("冲突状态");
        asset.setMarketBias("WAIT");
        asset.setMarketBiasLabel("观望");
        asset.setWorthOpening(false);
        home.setAssets(List.of(asset));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = new DashboardHomeVO.ExecutionSuggestionVO();
        suggestion.setStatus("DATA_QUALITY_BLOCKED");
        suggestion.setStatusLabel("当前暂无完整执行计划");
        suggestion.setBlockedReason("数据质量不足，等待有效分析");
        home.setExecutionSuggestion(suggestion);

        DashboardHomeVO.AiDecisionVO ai = new DashboardHomeVO.AiDecisionVO();
        ai.setRunStatus("NOT_CALLED");
        ai.setRunStatusLabel("未调用");
        ai.setDecisionModeLabel("仅规则判断");
        DashboardHomeVO.ConsistencyVO consistency = new DashboardHomeVO.ConsistencyVO();
        consistency.setConsistencyLevel("不适用");
        ai.setConsistency(consistency);
        home.setAiDecision(ai);

        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 6, null)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assets[0].assetState").value("CONFUSED"))
                .andExpect(jsonPath("$.data.assets[0].assetStateLabel").value("冲突状态"))
                .andExpect(jsonPath("$.data.assets[0].analysisId").value("analysis-BTCUSDT-exact"))
                .andExpect(jsonPath("$.data.assets[0].marketBias").value("WAIT"))
                .andExpect(jsonPath("$.data.assets[0].marketBiasLabel").value("观望"))
                .andExpect(jsonPath("$.data.aiDecision.runStatusLabel").value("未调用"))
                .andExpect(jsonPath("$.data.aiDecision.decisionModeLabel").value("仅规则判断"))
                .andExpect(jsonPath("$.data.aiDecision.consistency.consistencyLevel").value("不适用"))
                .andExpect(jsonPath("$.data.executionSuggestion.status").value("DATA_QUALITY_BLOCKED"))
                .andExpect(jsonPath("$.data.executionSuggestion.entryZone").doesNotExist())
                .andExpect(jsonPath("$.data.executionSuggestion.stopLoss").doesNotExist())
                .andExpect(jsonPath("$.data.executionSuggestion.takeProfitRules").doesNotExist());
    }

    @Test
    void homeControllerPassesSelectedPositionId() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        home.setSelectedPositionId(42L);
        home.setPositionSelectionStatus("EXACT_POSITION_SELECTED");
        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 6, 42L)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("positionId", "42")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedPositionId").value("42"))
                .andExpect(jsonPath("$.data.positionSelectionStatus").value("EXACT_POSITION_SELECTED"));

        verify(dashboardHomeService).getHomeForUser(7L, "BTCUSDT", 6, 42L);
    }

    @Test
    void homeSerializesPositionIdentityBeyondJavascriptSafeIntegerAsStrings() throws Exception {
        long positionId = 9_007_199_254_740_993L;
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedPositionId(positionId);
        DashboardHomeVO.PositionVO position = new DashboardHomeVO.PositionVO();
        position.setPositionId(positionId);
        position.setPositionStatus("OPEN");
        home.setPositions(List.of(position));
        when(dashboardHomeService.getHomeForUser(7L, null, null, positionId)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("positionId", Long.toString(positionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedPositionId")
                        .value("9007199254740993"))
                .andExpect(jsonPath("$.data.positions[0].positionId")
                        .value("9007199254740993"));
    }
}
