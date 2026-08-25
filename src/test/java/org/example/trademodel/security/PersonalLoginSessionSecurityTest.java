package org.example.trademodel.security;

import jakarta.servlet.http.HttpSession;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
        "spring.datasource.url=jdbc:h2:mem:personal-login-session;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=owner-login-session-secret",
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("注册账户")))
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

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(USERNAME));

        mockMvc.perform(get("/dashboard/mobile").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(USERNAME))
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
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/dashboard/mobile").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(post("/login")
                        .session(session)
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME))
                .andExpect(redirectedUrlPattern("**/dashboard/mobile*"));
    }

    @Test
    void loginMigratesPreAuthenticationSessionId() throws Exception {
        MockHttpSession before = new MockHttpSession();
        String originalId = before.getId();

        MvcResult result = mockMvc.perform(post("/login")
                        .session(before)
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME))
                .andReturn();

        HttpSession after = result.getRequest().getSession(false);
        assertThat(after).isNotNull();
        assertThat(after.getId()).isNotEqualTo(originalId);
    }

    @Test
    void authenticatedUserVisitingLoginReturnsToDashboard() throws Exception {
        MockHttpSession session = loginSession();
        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
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

        MockHttpSession session = loginSession();
        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(USERNAME).password(PASSWORD))
                .andExpect(authenticated().withUsername(USERNAME))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
