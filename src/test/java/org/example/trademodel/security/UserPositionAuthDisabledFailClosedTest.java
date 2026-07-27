package org.example.trademodel.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "trade-model.auth.enabled=false")
@AutoConfigureMockMvc
class UserPositionAuthDisabledFailClosedTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void ownerScopedEndpointsStillFailClosedWhenGlobalAuthenticationIsDisabled() throws Exception {
        mockMvc.perform(get("/api/user-positions/open"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/user-positions/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/user-positions/manual-open")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/account-risk/user-positions/current"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/review/center"))
                .andExpect(status().isUnauthorized());
    }
}
