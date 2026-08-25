package org.example.trademodel.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:actuator-health-security;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=Ownr8!Aa",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.probes.enabled=true",
        "management.endpoint.health.show-details=never",
        "management.endpoint.health.show-components=never"
})
@AutoConfigureMockMvc
class ActuatorHealthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublicAndMinimal() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void livenessAndReadinessHealthGroupsArePublicAndMinimal() throws Exception {
        for (String path : List.of("/actuator/health/liveness", "/actuator/health/readiness")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("WWW-Authenticate"))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist())
                    .andExpect(jsonPath("$.details").doesNotExist());
        }
    }

    @Test
    void sensitiveActuatorEndpointsAreNotExposedEvenWithAuthentication() throws Exception {
        for (String path : List.of(
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/mappings",
                "/actuator/loggers")) {
            MvcResult result = mockMvc.perform(get(path).with(user("operator").roles("OPERATOR")))
                    .andExpect(status().isNotFound())
                    .andReturn();
            String body = result.getResponse().getContentAsString();

            assertThat(body)
                    .contains("\"msg\":\"resource not found\"")
                    .doesNotContain("No static resource")
                    .doesNotContain("propertySources")
                    .doesNotContain("contexts")
                    .doesNotContain("configurationProperties")
                    .doesNotContain("dispatcherServlets")
                    .doesNotContain("configuredLevel")
                    .doesNotContain("effectiveLevel");
        }
    }
}
