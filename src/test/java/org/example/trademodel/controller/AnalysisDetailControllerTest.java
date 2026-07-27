package org.example.trademodel.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.AbstractView;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AnalysisDetailControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisDetailController())
                .setViewResolvers((viewName, locale) -> new AbstractView() {
                    @Override
                    protected void renderMergedOutputModel(
                            Map<String, Object> model,
                            HttpServletRequest request,
                            HttpServletResponse response) {
                        // No-op view keeps this test focused on the frontend route contract.
                    }
                })
                .build();
    }

    @Test
    void routeCarriesOpaqueAnalysisIdentityAndOptionalSymbolCrossCheck() throws Exception {
        mockMvc.perform(get("/dashboard/analysis-detail")
                        .param("analysisId", "ana-0123abcd")
                        .param("selectedSymbol", "btc/usdt")
                        .param("view", "mobile"))
                .andExpect(status().isOk())
                .andExpect(view().name("analysis-detail"))
                .andExpect(model().attribute("analysisId", "ana-0123abcd"))
                .andExpect(model().attribute("selectedSymbol", "BTCUSDT"))
                .andExpect(model().attribute("mobileView", true));
    }

    @Test
    void routeAllowsAnalysisIdentityWithoutSymbolFallback() throws Exception {
        mockMvc.perform(get("/dashboard/analysis-detail")
                        .param("analysisId", "legacy.analysis:42"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("analysisId", "legacy.analysis:42"))
                .andExpect(model().attribute("selectedSymbol", ""))
                .andExpect(model().attribute("mobileView", false));
    }

    @Test
    void missingOrUnsafeIdentityAndInvalidSymbolFailClosed() throws Exception {
        mockMvc.perform(get("/dashboard/analysis-detail"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("analysis-detail"))
                .andExpect(model().attribute("analysisId", ""));
        mockMvc.perform(get("/dashboard/analysis-detail").param("analysisId", "../ana-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/dashboard/analysis-detail")
                        .param("analysisId", "ana-1")
                        .param("selectedSymbol", "DEFAULT_SLOT"))
                .andExpect(status().isBadRequest());

        assertThat(AnalysisDetailController.normalizeAnalysisId(" ana-1 ")).isEqualTo("ana-1");
        assertThat(AnalysisDetailController.normalizeAnalysisId("   ")).isNull();
        assertThat(AnalysisDetailController.normalizeAnalysisId(null)).isNull();
    }
}
