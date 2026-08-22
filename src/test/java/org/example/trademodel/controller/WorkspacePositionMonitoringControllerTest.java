package org.example.trademodel.controller;

import org.example.trademodel.common.GlobalExceptionHandler;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.PositionMonitoringProjectionService;
import org.example.trademodel.service.PositionMonitoringReadService;
import org.example.trademodel.userposition.UserPositionNotFoundException;
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
class WorkspacePositionMonitoringControllerTest {
    @Mock AuthenticatedUserIdResolver userIdResolver;
    @Mock PositionMonitoringReadService projectionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkspacePositionMonitoringController(
                        userIdResolver, projectionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crossUserPositionDetailFailsClosedAsNotFound() throws Exception {
        when(userIdResolver.requireCurrentUserId()).thenReturn(7L);
        when(projectionService.findForUser(7L, 41L)).thenThrow(new UserPositionNotFoundException());

        mockMvc.perform(get("/api/workspace/positions/41/monitoring"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verify(projectionService).findForUser(7L, 41L);
    }
}
