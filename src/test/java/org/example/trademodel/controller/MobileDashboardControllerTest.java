package org.example.trademodel.controller;

import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.vo.DashboardHomeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MobileDashboardController(dashboardHomeService)).build();
    }

    @Test
    void mobileRouteProjectsExistingDashboardHomeVoWithThreeAssets() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        when(dashboardHomeService.getHome("BTCUSDT", 3, null)).thenReturn(home);

        mockMvc.perform(get("/dashboard/mobile").param("selectedSymbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard-mobile"))
                .andExpect(model().attribute("home", home));

        verify(dashboardHomeService).getHome("BTCUSDT", 3, null);
        verifyNoMoreInteractions(dashboardHomeService);
    }

    @Test
    void mobileRouteKeepsSelectionOptionalAndDoesNotSelectAPosition() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        when(dashboardHomeService.getHome(null, 3, null)).thenReturn(home);

        mockMvc.perform(get("/dashboard/mobile"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard-mobile"))
                .andExpect(model().attribute("home", home));

        verify(dashboardHomeService).getHome(null, 3, null);
    }
}
