package org.example.trademodel.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.session.jdbc.initialize-schema=always",
        "trade-model.auth.enabled=true"
})
@AutoConfigureMockMvc
class PersistentJdbcSessionContractTest {

    @Autowired
    @SuppressWarnings("rawtypes")
    private SessionRepository sessionRepository;

    @Autowired
    private JdbcOperations jdbcOperations;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void authenticatedSessionSurvivesRepositoryReplacementAndRepeatedReads() {
        assertThat(sessionRepository).isInstanceOf(JdbcIndexedSessionRepository.class);

        Session created = (Session) sessionRepository.createSession();
        created.setMaxInactiveInterval(Duration.ofMinutes(30));
        created.setAttribute("authenticatedPrincipal", "operator");
        sessionRepository.save(created);

        SessionRepository replacement = new JdbcIndexedSessionRepository(
                jdbcOperations, new TransactionTemplate(transactionManager));
        String sessionId = created.getId();

        for (int attempt = 0; attempt < 20; attempt++) {
            Session loaded = replacement.findById(sessionId);
            assertThat(loaded).isNotNull();
            assertThat((String) loaded.getAttribute("authenticatedPrincipal")).isEqualTo("operator");
        }

        replacement.deleteById(sessionId);
        assertThat(sessionRepository.findById(sessionId)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void unknownAndExpiredSessionIdsFailClosed() {
        assertThat(sessionRepository.findById("forged-session-id")).isNull();

        Session expired = (Session) sessionRepository.createSession();
        expired.setMaxInactiveInterval(Duration.ZERO);
        sessionRepository.save(expired);

        assertThat(sessionRepository.findById(expired.getId())).isNull();
    }

    @Test
    void legacyCookieThatDecodesToInvalidDatabaseTextFailsClosedWithoutDashboard500() throws Exception {
        mockMvc.perform(get("/dashboard").cookie(new Cookie("JSESSIONID", "AA==")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
