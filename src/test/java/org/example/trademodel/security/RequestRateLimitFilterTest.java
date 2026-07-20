package org.example.trademodel.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "trade-model.auth.enabled=true",
        "trade-model.security.rate-limit.enabled=true",
        "trade-model.security.rate-limit.requests-per-minute=2",
        "trade-model.security.rate-limit.window-ms=60000"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RequestRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void blocksExcessiveRequestsWithoutPrintingSecrets(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/dashboard/home").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/home").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/home").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"code\":429,\"msg\":\"rate limit exceeded\"}"));

        assertThat(output).contains("RATE_LIMIT_BLOCKED");
        assertThat(output).contains("path=/api/dashboard/home");
        assertThat(output).doesNotContain("operator-secret");
    }
}
