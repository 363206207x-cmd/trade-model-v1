package org.example.trademodel.controller;

import org.example.trademodel.service.ReviewCenterService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.vo.ReviewCenterDashboardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
@Tag("core-regression")
class ReviewCenterControllerTest {
    @Mock
    private ReviewCenterService reviewCenterService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ReviewCenterController(reviewCenterService, authenticatedUserIdResolver)).build();
    }

    @Test
    void centerEndpointReturnsApiResponseWithEmptyArrays() throws Exception {
        when(reviewCenterService.getDashboardForUser(7L)).thenReturn(new ReviewCenterDashboardVO());

        mockMvc.perform(get("/api/review/center"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.summary.positionReviewCount").value(0))
                .andExpect(jsonPath("$.data.summary.opportunityReviewCount").value(0))
                .andExpect(jsonPath("$.data.summary.pushReviewCount").value(0))
                .andExpect(jsonPath("$.data.summary.ruleFeedbackCount").value(0))
                .andExpect(jsonPath("$.data.positionReviews").isArray())
                .andExpect(jsonPath("$.data.opportunityReviews").isArray())
                .andExpect(jsonPath("$.data.pushReviews").isArray())
                .andExpect(jsonPath("$.data.ruleFeedback").isArray())
                .andExpect(jsonPath("$.data.buyAction").doesNotExist())
                .andExpect(jsonPath("$.data.sellAction").doesNotExist())
                .andExpect(jsonPath("$.data.orderAction").doesNotExist())
                .andExpect(jsonPath("$.data.executionAction").doesNotExist());

        verify(reviewCenterService).getDashboardForUser(7L);
    }
}
