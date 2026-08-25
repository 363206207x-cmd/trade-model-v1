package org.example.trademodel.security;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.service.MultiUserAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:multi-user-security;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=owner-security-secret",
        "trade-model.security.rate-limit.requests-per-minute=1000"
})
@AutoConfigureMockMvc
@Transactional
class MultiUserAccountSecurityIntegrationTest {
    private static final String OWNER_PASSWORD = "owner-security-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MultiUserAccountService accountService;

    @Autowired
    private PersonalUserMapper userMapper;

    @Test
    void registrationIsPublicButMutationRequiresCsrf() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("创建账户")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("maxlength=\"32\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pattern=\"[A-Za-z0-9._-]{3,32}\"")));

        mockMvc.perform(post("/register")
                        .param("username", "web_user")
                        .param("password", "12345678")
                        .param("passwordConfirmation", "12345678"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "web_user")
                        .param("password", "12345678")
                        .param("passwordConfirmation", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
        assertThat(userMapper.findByUsername("web_user")).isNotNull();

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "mismatch_user")
                        .param("password", "12345678")
                        .param("passwordConfirmation", "abcdefgh"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("两次输入的密码不一致")));
    }

    @Test
    void concurrentSessionsRemainValidUntilOwnerForceLogout() throws Exception {
        PersonalUserDO user = accountService.register("session_user", "12345678");
        MockHttpSession first = login("session_user", "12345678");
        MockHttpSession second = login("session_user", "12345678");

        mockMvc.perform(get("/dashboard").session(first)).andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").session(second)).andExpect(status().isOk());

        MockHttpSession owner = login("xuchao", OWNER_PASSWORD);
        mockMvc.perform(post("/api/owner/accounts/{id}/force-logout", user.getId())
                        .session(owner).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard").session(first))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired=true"));
        mockMvc.perform(get("/api/dashboard/home").session(second))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/dashboard").session(owner)).andExpect(status().isOk());
    }

    @Test
    void disableBlocksLoginAndReenableRestoresItWithoutDeletingAccount() throws Exception {
        PersonalUserDO user = accountService.register("lifecycle_user", "12345678");
        MockHttpSession userSession = login("lifecycle_user", "12345678");
        MockHttpSession owner = login("xuchao", OWNER_PASSWORD);

        mockMvc.perform(post("/api/owner/accounts/{id}/disable", user.getId())
                        .session(owner).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").session(userSession))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(formLogin().user("lifecycle_user").password("12345678"))
                .andExpect(unauthenticated());
        assertThat(userMapper.findById(user.getId())).isNotNull();

        mockMvc.perform(post("/api/owner/accounts/{id}/enable", user.getId())
                        .session(owner).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(formLogin().user("lifecycle_user").password("12345678"))
                .andExpect(authenticated().withUsername("lifecycle_user"));
    }

    @Test
    void ownerAdministrationIsOwnerOnlyAndSecondUserDoesNotReplaceOwnerSession() throws Exception {
        accountService.register("ordinary_user", "12345678");
        MockHttpSession user = login("ordinary_user", "12345678");
        MockHttpSession owner = login("xuchao", OWNER_PASSWORD);

        mockMvc.perform(get("/me/accounts").session(user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/owner/accounts").session(user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/me/accounts").session(owner)).andExpect(status().isOk());
        mockMvc.perform(get("/api/owner/accounts").session(owner)).andExpect(status().isOk());
        mockMvc.perform(get("/dashboard").session(owner)).andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(user).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/dashboard").session(owner)).andExpect(status().isOk());
    }

    @Test
    void ordinaryUsersCannotReadOrMutateGlobalProviderConfiguration() throws Exception {
        accountService.register("provider_user", "12345678");
        MockHttpSession user = login("provider_user", "12345678");
        MockHttpSession owner = login("xuchao", OWNER_PASSWORD);

        for (String path : java.util.List.of(
                "/api/provider-call/base-profile",
                "/api/config/scan-profile",
                "/api/ai/orchestrator/status",
                "/api/rule/config-audit-status",
                "/api/review/rule-version-logs")) {
            mockMvc.perform(get(path).session(user)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/ai/orchestrator/status").session(owner))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/settings/notifications/telegram/status").session(user))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/settings/notifications/telegram/status").session(owner))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("botToken"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("chatId"))));

        mockMvc.perform(get("/api/dashboard/home").session(user)).andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/summary").session(user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/overview").session(user)).andExpect(status().isForbidden());
    }

    @Test
    void selfPasswordChangeRequiresCsrfAndInvalidatesOnlyThatUsersSessions() throws Exception {
        accountService.register("password_web_user", "12345678");
        MockHttpSession first = login("password_web_user", "12345678");
        MockHttpSession second = login("password_web_user", "12345678");
        MockHttpSession owner = login("xuchao", OWNER_PASSWORD);

        mockMvc.perform(get("/me/security").session(first))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("账户安全")));
        mockMvc.perform(post("/me/security").session(first)
                        .param("currentPassword", "12345678")
                        .param("newPassword", "abcdefgh")
                        .param("passwordConfirmation", "abcdefgh"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/me/security").session(first).with(csrf())
                        .param("currentPassword", "12345678")
                        .param("newPassword", "abcdefgh")
                        .param("passwordConfirmation", "abcdefgh"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordUpdated=true"));

        mockMvc.perform(get("/dashboard").session(second))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired=true"));
        mockMvc.perform(get("/dashboard").session(owner)).andExpect(status().isOk());
        mockMvc.perform(formLogin().user("password_web_user").password("12345678"))
                .andExpect(unauthenticated());
        mockMvc.perform(formLogin().user("password_web_user").password("abcdefgh"))
                .andExpect(authenticated().withUsername("password_web_user"));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(username).password(password))
                .andExpect(authenticated().withUsername(username))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
