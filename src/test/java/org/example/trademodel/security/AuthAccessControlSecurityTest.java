package org.example.trademodel.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "trade-model.auth.enabled=true",
        "trade-model.auth.admin-username=operator",
        "trade-model.auth.admin-password=operator-secret"
})
@AutoConfigureMockMvc
class AuthAccessControlSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void dashboardPageRequiresAuthenticationAndAllowsBasicAuth() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/dashboard").with(httpBasic("operator", "operator-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardHomeApiRequiresAuthenticationAndAllowsBasicAuth() throws Exception {
        mockMvc.perform(get("/api/dashboard/home"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/home").with(httpBasic("operator", "operator-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void reviewDashboardRequiresAuthenticationAndAllowsBasicAuth() throws Exception {
        mockMvc.perform(get("/review/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/review/dashboard").with(httpBasic("operator", "operator-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void writeAndRecheckEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/user-positions/manual-open").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/review/user-positions/1/feedback").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/opportunity-log/op-1/evaluate").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/push/recheck/1").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/push/recheck/dispatch/config").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void representativeOperationalApiRoutesRequireAuthentication() throws Exception {
        for (String path : List.of(
                "/api/review/center",
                "/api/rule/reload",
                "/api/system/run-baseline",
                "/api/external-context/current",
                "/api/market/quote-status",
                "/api/ai/orchestrator/status")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void staticResourceMissesAreNotAuthenticationChallenges() throws Exception {
        mockMvc.perform(get("/css/not-present.css"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void noExecutableTradingRouteSurfaceIsIntroduced() {
        Set<String> paths = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPathPatternsCondition().getPatterns().stream())
                .map(pattern -> pattern.getPatternString().toLowerCase())
                .collect(Collectors.toSet());

        assertThat(paths).noneMatch(path -> path.contains("/buy")
                || path.contains("/sell")
                || path.contains("/order")
                || path.contains("/execute")
                || path.contains("/auto-trading"));
    }
}
