package org.example.trademodel.security;

import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "trade-model.auth.enabled=true"
})
@AutoConfigureMockMvc
class AuthAccessControlSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private RuleVersionLogMapper ruleVersionLogMapper;

    @Autowired
    private PersonalUserMapper personalUserMapper;

    @BeforeEach
    void ensureCanonicalOperator() {
        if (personalUserMapper.findByUsername("operator") != null) {
            return;
        }
        PersonalUserDO user = new PersonalUserDO();
        user.setUsername("operator");
        user.setPasswordHash("{noop}test-only-password");
        user.setCreatedAt(LocalDateTime.now());
        personalUserMapper.insert(user);
    }

    @Test
    void dashboardPageRequiresAuthenticationAndAllowsSessionPrincipal() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/dashboard").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void publicAndProtectedHttpsResponsesExposeExplicitBrowserSecurityHeaders() throws Exception {
        for (String path : List.of("/login", "/dashboard", "/api/dashboard/home")) {
            mockMvc.perform(get(path).secure(true))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().exists("Strict-Transport-Security"));
        }
    }

    @Test
    void dashboardHomeApiRequiresAuthenticationAndAllowsSessionPrincipal() throws Exception {
        mockMvc.perform(get("/api/dashboard/home"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/dashboard/home").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void reviewDashboardRequiresAuthenticationAndAllowsBasicAuth() throws Exception {
        mockMvc.perform(get("/review/dashboard"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/review/dashboard").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void writeAndRecheckEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/user-positions/manual-open").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/review/user-positions/1/feedback").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/opportunity-log/op-1/evaluate").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/push/recheck/1").with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/push/recheck/dispatch/config").with(csrf()).contentType("application/json").content("{}"))
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
    void scanProfileUpdateIsAuthenticatedAndAudited() throws Exception {
        mockMvc.perform(put("/api/config/scan-profile")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/config/scan-profile")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "baseProfile": "HIGH",
                                  "positionMonitorProfile": "HIGH",
                                  "poolProfile": "LOW",
                                  "autoEscalationEnabled": true,
                                  "updateReason": "authenticated safety profile test"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(ruleVersionLogMapper.queryLogs(null, null, "operator", null, null,
                "SCAN_PROFILE_CONFIG", null, null, null, 10)).isNotEmpty();
    }

    @Test
    void staticResourceMissesAreNotAuthenticationChallenges() throws Exception {
        mockMvc.perform(get("/css/not-present.css"))
                .andExpect(status().isNotFound())
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
