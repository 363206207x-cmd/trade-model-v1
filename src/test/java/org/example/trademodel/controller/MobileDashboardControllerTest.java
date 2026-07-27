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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class MobileDashboardControllerTest {
    @Mock
    private DashboardHomeService dashboardHomeService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MobileDashboardController(dashboardHomeService, authenticatedUserIdResolver)).build();
    }

    @Test
    void mobileRouteProjectsExistingDashboardHomeVoWithThreeAssets() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 3, null)).thenReturn(home);

        mockMvc.perform(get("/dashboard/mobile").param("selectedSymbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard-mobile"))
                .andExpect(model().attribute("home", home));

        verify(dashboardHomeService).getHomeForUser(7L, "BTCUSDT", 3, null);
    }

    @Test
    void mobileRouteKeepsSelectionOptionalAndDoesNotSelectAPosition() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        when(dashboardHomeService.getHomeForUser(7L, null, 3, null)).thenReturn(home);

        mockMvc.perform(get("/dashboard/mobile"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard-mobile"))
                .andExpect(model().attribute("home", home));

        verify(dashboardHomeService).getHomeForUser(7L, null, 3, null);
    }

    @Test
    void mobileRouteFiltersDefaultSlotsBeforeRendering() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        home.setAssets(List.of(
                asset("BTCUSDT", "DECISION"),
                asset("ETHUSDT", "DEFAULT_SLOT"),
                asset("SOLUSDT", "MARKET_DATA")));
        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 3, null)).thenReturn(home);

        MvcResult result = mockMvc.perform(get("/dashboard/mobile").param("selectedSymbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mobileAssets(result))
                .extracting(DashboardHomeVO.AssetVO::getRawSymbol)
                .containsExactly("BTCUSDT", "SOLUSDT");
        assertThat(mobileAssets(result))
                .extracting(DashboardHomeVO.AssetVO::getSlotType)
                .doesNotContain("DEFAULT_SLOT");
    }

    @Test
    void mobileRouteFailsClosedWhenImplicitSelectionDoesNotHaveARealCard() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        home.setAssets(List.of(
                asset("BNBUSDT", "MARKET_DATA"),
                asset("BTCUSDT", "DEFAULT_SLOT"),
                asset("ETHUSDT", "DEFAULT_SLOT")));
        when(dashboardHomeService.getHomeForUser(7L, null, 3, null)).thenReturn(home);

        MvcResult result = mockMvc.perform(get("/dashboard/mobile"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mobileAssets(result)).isEmpty();
        assertThat(home.getSelectedSymbol()).isEqualTo("BTCUSDT");
    }

    @Test
    void mobileRouteKeepsDeepLinkedAssetSelectedWithinThreeVisibleCards() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("SOLUSDT");
        home.setAssets(List.of(
                asset("BTCUSDT", "DECISION"),
                asset("ETHUSDT", "DECISION"),
                asset("BNBUSDT", "DECISION"),
                asset("SOLUSDT", "DECISION")));
        when(dashboardHomeService.getHomeForUser(7L, "SOLUSDT", 3, null)).thenReturn(home);

        MvcResult result = mockMvc.perform(get("/dashboard/mobile").param("selectedSymbol", "SOLUSDT"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mobileAssets(result))
                .extracting(DashboardHomeVO.AssetVO::getRawSymbol)
                .containsExactly("BTCUSDT", "ETHUSDT", "SOLUSDT");
    }

    @Test
    void mobileRouteFailsClosedWhenDeepLinkedAssetHasNoRealCard() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("XRPUSDT");
        home.setAssets(List.of(
                asset("BTCUSDT", "DECISION"),
                asset("XRPUSDT", "DEFAULT_SLOT")));
        when(dashboardHomeService.getHomeForUser(7L, "XRPUSDT", 3, null)).thenReturn(home);

        MvcResult result = mockMvc.perform(get("/dashboard/mobile").param("selectedSymbol", "XRPUSDT"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mobileAssets(result)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<DashboardHomeVO.AssetVO> mobileAssets(MvcResult result) {
        return (List<DashboardHomeVO.AssetVO>) result.getModelAndView()
                .getModel()
                .get("mobileAssets");
    }

    private DashboardHomeVO.AssetVO asset(String symbol, String slotType) {
        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setRawSymbol(symbol);
        asset.setSymbol(symbol);
        asset.setSlotType(slotType);
        return asset;
    }
}
