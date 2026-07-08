package org.example.trademodel.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "trade-model.auth.enabled=true",
        "trade-model.auth.admin-username=operator",
        "trade-model.auth.admin-password=operator-secret",
        "trade-model.security.rate-limit.enabled=true",
        "trade-model.security.rate-limit.requests-per-minute=100",
        "trade-model.security.rate-limit.window-ms=60000"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SecurityObservabilityGuardTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void accessLogExistsAndDoesNotPrintSensitiveHeaderValues(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/actuator/health?api_key=secret-value")
                        .header("X-Api-Key", "secret-value")
                        .header("Cookie", "SESSION=secret-cookie"))
                .andExpect(status().isOk());

        assertThat(output).contains("ACCESS_LOG");
        assertThat(output).contains("path=/actuator/health");
        assertThat(output).doesNotContain("secret-value");
        assertThat(output).doesNotContain("secret-cookie");
    }

    @Test
    void authenticationFailuresAreAuditedWithoutCredentialValues(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/dashboard/home").with(httpBasic("operator", "bad-secret")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));

        assertThat(output).contains("AUTH_AUDIT");
        assertThat(output).contains("outcome=FAILURE");
        assertThat(output).contains("path=/api/dashboard/home");
        assertThat(output).doesNotContain("bad-secret");
    }

    @Test
    void sanitizerRedactsSensitiveHeaderValues() {
        assertThat(SensitiveLogSanitizer.sanitizeHeaderValue("Authorization", "Basic abc"))
                .isEqualTo("[REDACTED]");
        assertThat(SensitiveLogSanitizer.sanitizeHeaderValue("Cookie", "SESSION=abc"))
                .isEqualTo("[REDACTED]");
        assertThat(SensitiveLogSanitizer.sanitizeHeaderValue("X-Api-Key", "key-value"))
                .isEqualTo("[REDACTED]");
        assertThat(SensitiveLogSanitizer.sanitizeHeaderValue("X-Request-Id", "req-1"))
                .isEqualTo("req-1");
    }
}
