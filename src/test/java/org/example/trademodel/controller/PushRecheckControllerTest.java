package org.example.trademodel.controller;

import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.PushRecheckAccessBoundary;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PushRecheckControllerTest {

    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PushRecheckController controller = new PushRecheckController(
                authenticatedUserIdResolver,
                new PushRecheckAccessBoundary());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void opsOverviewFailsClosedWithoutGlobalAuthenticatedAccess() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);

        mockMvc.perform(get("/api/push/recheck/ops/overview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("push recheck private data unavailable"));

        verify(authenticatedUserIdResolver).requireCurrentUserId();
    }

    @Test
    void opsOverviewQueryParametersCannotRestoreGlobalAccess() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);

        mockMvc.perform(get("/api/push/recheck/ops/overview")
                        .param("dispatchBatchId", "B1")
                        .param("dispatchInstructionId", "I1")
                        .param("auditLimit", "6")
                        .param("logLimit", "12"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verify(authenticatedUserIdResolver).requireCurrentUserId();
    }

    @Test
    void manualTriggerFailsClosedBeforeMutation() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);

        mockMvc.perform(post("/api/push/recheck/101")
                        .contentType("application/json")
                        .content("{\"currentPrice\":100}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verify(authenticatedUserIdResolver).requireCurrentUserId();
    }

    @Test
    void latestFailsClosedWhenNoOwnerScopedProjectionExists() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);

        mockMvc.perform(get("/api/push/recheck/101/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("push recheck private data unavailable"));

        verify(authenticatedUserIdResolver).requireCurrentUserId();
    }

    @Test
    void logsFailClosedWhenNoOwnerScopedProjectionExists() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);

        mockMvc.perform(get("/api/push/recheck/101/logs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("push recheck private data unavailable"));

        verify(authenticatedUserIdResolver).requireCurrentUserId();
    }

    @Test
    void replayAndConfigRoutesRemainUnavailable() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);

        mockMvc.perform(post("/api/push/recheck/replay")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/push/recheck/replay/summary"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/push/recheck/dispatch/config"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/push/recheck/dispatch/config")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/push/recheck/dispatch/config/audit"))
                .andExpect(status().isNotFound());
    }
}
