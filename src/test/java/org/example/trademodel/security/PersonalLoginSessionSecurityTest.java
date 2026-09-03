package org.example.trademodel.security;

import jakarta.servlet.http.Cookie;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=operator",
        "trade-model.auth.initial-password=operator-secret-123",
        "trade-model.security.rate-limit.requests-per-minute=1000"
})
@AutoConfigureMockMvc
class PersonalLoginSessionSecurityTest {
    private static final String USERNAME = "operator";
    private static final String PASSWORD = "operator-secret-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonalUserMapper personalUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetUserAndAttempts() {
        PersonalUserDO user = personalUserMapper.findByUsername(USERNAME);
        if (user == null) {
            user = new PersonalUserDO();
            user.setUsername(USERNAME);
            user.setPasswordHash(passwordEncoder.encode(PASSWORD));
            user.setCreatedAt(java.time.LocalDateTime.of(2026, 7, 20, 0, 0));
            personalUserMapper.insert(user);
        }
        jdbcTemplate.update("UPDATE tm_user SET password_hash = ?, last_login_at = NULL WHERE username = ?",
                passwordEncoder.encode(PASSWORD), USERNAME);
        loginAttemptService.resetKnownUser(USERNAME);
        loginAttemptService.resetUnknownUsername("unknown-user");
    }

    @Test
    void loginPageGetContainsAccessiblePersonalLoginForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"username\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"password\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("autocomplete=\"username\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("autocomplete=\"current-password\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("width=device-width, initial-scale=1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("登录")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("注册"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("OAuth"))));
    }

    @Test
    void unauthenticatedBrowserRedirectsWhileApiReturnsSanitized401() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/dashboard/mobile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/api/dashboard/home"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"code\":401,\"msg\":\"authentication required\"}"));
    }

    @Test
    void staticLoginResourceIsPublic() throws Exception {
        mockMvc.perform(get("/css/login.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("font-size: 16px")));
    }

    @Test
    void successfulLoginCreatesSessionAndSessionKeepsDashboardAuthenticated() throws Exception {
        MvcResult login = mockMvc.perform(formLogin().user(USERNAME).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername(USERNAME))
                .andReturn();

        Cookie session = sessionCookie(login);
        mockMvc.perform(get("/dashboard").cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard/mobile").cookie(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI 三角色复核")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-asset-search-toggle")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-asset-add")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("完整持仓页待实现"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{asset"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{position"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{aiDecision"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{marketTrend"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{riskLevel"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("top.holdingRisk"))));
    }

    @Test
    void mobileSavedRequestReturnsToMobileProjectionAfterLogin() throws Exception {
        MvcResult protectedRequest = mockMvc.perform(get("/dashboard/mobile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"))
                .andReturn();
        Cookie preAuthenticationSession = sessionCookie(protectedRequest);

        mockMvc.perform(post("/login")
                        .cookie(preAuthenticationSession)
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME))
                .andExpect(redirectedUrlPattern("**/dashboard/mobile*"));
    }

    @Test
    void loginMigratesPreAuthenticationSessionId() throws Exception {
        MvcResult protectedRequest = mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Cookie before = sessionCookie(protectedRequest);

        MvcResult result = mockMvc.perform(post("/login")
                        .cookie(before)
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME))
                .andReturn();

        Cookie after = sessionCookie(result);
        assertThat(after.getValue()).isNotEqualTo(before.getValue());
        mockMvc.perform(get("/dashboard").cookie(after))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserVisitingLoginReturnsToDashboard() throws Exception {
        Cookie session = loginSessionCookie();
        mockMvc.perform(get("/login").cookie(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void persistentSessionSurvivesTwentyRefreshesAndSixAssetSelections() throws Exception {
        Cookie session = loginSessionCookie();

        for (int refresh = 0; refresh < 20; refresh++) {
            mockMvc.perform(get("/dashboard").cookie(copyCookie(session)))
                    .andExpect(status().isOk());
        }

        for (String symbol : List.of("BTCUSDT", "ETHUSDT", "BNBUSDT", "XRPUSDT", "ADAUSDT", "SOLUSDT")) {
            mockMvc.perform(get("/api/dashboard/home")
                            .queryParam("selectedSymbol", symbol)
                            .cookie(copyCookie(session)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void persistentSessionSurvivesDirectLinksAndPageRoundTrips() throws Exception {
        Cookie session = loginSessionCookie();

        for (String path : List.of(
                "/dashboard?asset=BTCUSDT",
                "/analysis",
                "/positions",
                "/messages",
                "/me",
                "/dashboard?asset=SOLUSDT")) {
            mockMvc.perform(get(path).cookie(copyCookie(session)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void concurrentHomeApiRequestsKeepPersistentSessionAuthenticated() throws Exception {
        Cookie session = loginSessionCookie();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Integer>> requests = new ArrayList<>();
            for (String symbol : List.of(
                    "BTCUSDT", "ETHUSDT", "BNBUSDT", "XRPUSDT", "ADAUSDT", "SOLUSDT",
                    "BTCUSDT", "ETHUSDT", "BNBUSDT", "XRPUSDT", "ADAUSDT", "SOLUSDT")) {
                requests.add(() -> mockMvc.perform(get("/api/dashboard/home")
                                .queryParam("selectedSymbol", symbol)
                                .cookie(copyCookie(session)))
                        .andReturn()
                        .getResponse()
                        .getStatus());
            }

            List<Future<Integer>> responses = executor.invokeAll(requests);
            for (Future<Integer> response : responses) {
                assertThat(response.get(30, TimeUnit.SECONDS)).isEqualTo(200);
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void wrongPasswordAndUnknownUsernameUseSameFailureAndDoNotUpdateLastLogin() throws Exception {
        mockMvc.perform(formLogin().user(USERNAME).password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());

        mockMvc.perform(formLogin().user("unknown-user").password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());

        assertThat(personalUserMapper.findByUsername(USERNAME).getLastLoginAt()).isNull();
        mockMvc.perform(get("/login?error=true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "用户名或密码错误，或当前登录暂时受限。")));
    }

    @Test
    void invalidUsernameUsesUnifiedLoginFailureResponse() throws Exception {
        mockMvc.perform(formLogin().user("operator outcome=SUCCESS").password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/login?error=true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "用户名或密码错误，或当前登录暂时受限。")));
        assertThat(personalUserMapper.findByUsername(USERNAME).getLastLoginAt()).isNull();
    }

    @Test
    void storedPasswordIsBcryptAndSuccessfulLoginUpdatesLastLoginOnlyThen() throws Exception {
        PersonalUserDO before = personalUserMapper.findByUsername(USERNAME);
        assertThat(before.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, before.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", before.getPasswordHash())).isFalse();
        assertThat(before.getLastLoginAt()).isNull();

        mockMvc.perform(formLogin().user(USERNAME).password(PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME));

        assertThat(personalUserMapper.findByUsername(USERNAME).getLastLoginAt()).isNotNull();
    }

    @Test
    void loginFailureLimitBlocksCorrectPasswordUntilStateIsResetBySuccessfulLogin() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(formLogin().user(USERNAME).password("wrong-password"))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error=true"));
        }
        assertThat(loginAttemptService.isKnownUserBlocked(USERNAME)).isTrue();

        mockMvc.perform(formLogin().user(USERNAME).password(PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error=true"));
        assertThat(personalUserMapper.findByUsername(USERNAME).getLastLoginAt()).isNull();

        loginAttemptService.resetKnownUser(USERNAME);
        mockMvc.perform(formLogin().user(USERNAME).password("wrong-password"))
                .andExpect(unauthenticated());
        assertThat(loginAttemptService.knownUserFailureCount(USERNAME)).isEqualTo(1);
        mockMvc.perform(formLogin().user(USERNAME).password(PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME));
        assertThat(loginAttemptService.knownUserFailureCount(USERNAME)).isZero();
    }

    @Test
    void loginAndLogoutRequireCsrfAndPostLogoutInvalidatesAuthentication() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());

        Cookie session = loginSessionCookie();
        mockMvc.perform(post("/logout").cookie(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/dashboard").cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").cookie(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/dashboard").cookie(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    private Cookie loginSessionCookie() throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(USERNAME).password(PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME))
                .andReturn();
        return sessionCookie(result);
    }

    private static Cookie sessionCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("JSESSIONID");
        assertThat(cookie).as("persistent session cookie").isNotNull();
        return cookie;
    }

    private static Cookie copyCookie(Cookie source) {
        Cookie copy = new Cookie(source.getName(), source.getValue());
        copy.setPath(source.getPath());
        copy.setSecure(source.getSecure());
        copy.setHttpOnly(source.isHttpOnly());
        copy.setMaxAge(source.getMaxAge());
        return copy;
    }
}
