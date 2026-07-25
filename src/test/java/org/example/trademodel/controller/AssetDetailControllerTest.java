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

class AssetDetailControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AssetDetailController())
                .setViewResolvers((viewName, locale) -> new AbstractView() {
                    @Override
                    protected void renderMergedOutputModel(
                            Map<String, Object> model,
                            HttpServletRequest request,
                            HttpServletResponse response) {
                        // No-op view keeps this standalone controller test focused on routing and model state.
                    }
                })
                .build();
    }

    @Test
    void detailRouteNormalizesSymbolAndRendersDesktopContext() throws Exception {
        mockMvc.perform(get("/dashboard/asset-detail").param("selectedSymbol", "btc/usdt"))
                .andExpect(status().isOk())
                .andExpect(view().name("asset-detail"))
                .andExpect(model().attribute("selectedSymbol", "BTCUSDT"))
                .andExpect(model().attribute("mobileView", false));
    }

    @Test
    void detailRoutePreservesMobileReturnContext() throws Exception {
        mockMvc.perform(get("/dashboard/asset-detail")
                        .param("selectedSymbol", "SOLUSDT")
                        .param("view", "mobile"))
                .andExpect(status().isOk())
                .andExpect(view().name("asset-detail"))
                .andExpect(model().attribute("selectedSymbol", "SOLUSDT"))
                .andExpect(model().attribute("mobileView", true));
    }

    @Test
    void invalidOrPlaceholderSymbolsFailClosed() throws Exception {
        mockMvc.perform(get("/dashboard/asset-detail"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("asset-detail"))
                .andExpect(model().attribute("selectedSymbol", ""));
        mockMvc.perform(get("/dashboard/asset-detail").param("selectedSymbol", "DEFAULT_SLOT"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("asset-detail"));
        mockMvc.perform(get("/dashboard/asset-detail").param("selectedSymbol", "../BTCUSDT"))
                .andExpect(status().isBadRequest());

        assertThat(AssetDetailController.normalizeSymbol("   ")).isNull();
        assertThat(AssetDetailController.normalizeSymbol(null)).isNull();
    }
}
