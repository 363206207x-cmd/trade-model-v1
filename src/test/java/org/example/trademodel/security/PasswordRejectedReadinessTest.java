package org.example.trademodel.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password_rejected_readiness;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=operator",
        "trade-model.auth.initial-password=short",
        "trade-model.schedulers.enabled=false",
        "trade-model.analysis.scheduler.enabled=false",
        "trade-model.provider-call.scheduler-enabled=false"
})
@AutoConfigureMockMvc
class PasswordRejectedReadinessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectedBootstrapMakesReadinessDownWithoutTakingLivenessDown() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
