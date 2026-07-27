package org.example.trademodel.controller;

import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountRiskControllerTest {
    @Mock
    private UserPositionRiskAdapter userPositionRiskAdapter;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AccountRiskController(userPositionRiskAdapter, authenticatedUserIdResolver)).build();
    }

    @Test
    void currentUserPositionRiskReturnsReadOnlySafetyFields() throws Exception {
        when(userPositionRiskAdapter.currentRiskForUser(7L)).thenReturn(UserPositionRiskResult.noOpenPosition(0));

        mockMvc.perform(get("/api/account-risk/user-positions/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.riskStatus").value("NO_OPEN_USER_POSITION"))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notExecutable").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true))
                .andExpect(jsonPath("$.data.notAutoReduce").value(true))
                .andExpect(jsonPath("$.data.notAutoClose").value(true))
                .andExpect(jsonPath("$.data.notAutoReverse").value(true))
                .andExpect(jsonPath("$.data.notUserPositionMutation").value(true));

        verify(userPositionRiskAdapter).currentRiskForUser(7L);
    }
}
