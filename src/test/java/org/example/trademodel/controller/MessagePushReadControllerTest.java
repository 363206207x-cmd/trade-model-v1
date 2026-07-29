package org.example.trademodel.controller;

import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.MessagePushReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class MessagePushReadControllerTest {
    @Mock
    private MessagePushReadService messagePushReadService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MessagePushReadController(messagePushReadService, authenticatedUserIdResolver)).build();
    }

    @Test
    void listReadFailureRemainsDistinctFromEmpty() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        when(messagePushReadService.listForUser(7L, null))
                .thenReturn(new MessageListDTO(MessageReadState.ERROR, null, "MESSAGE_READ_FAILED"));

        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.data.state").value("ERROR"))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.reason").value("MESSAGE_READ_FAILED"));
    }

    @Test
    void missingExactMessageIdentityReturnsNotFound() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        when(messagePushReadService.findPushDetailForUser(7L, "9007199254740993"))
                .thenReturn(new PushDetailDTO(
                        MessageReadState.MISSING,
                        "9007199254740993",
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        "MESSAGE_NOT_FOUND",
                        true,
                        true,
                        true,
                        true));

        mockMvc.perform(get("/api/messages/9007199254740993/push-detail"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data.state").value("MISSING"))
                .andExpect(jsonPath("$.data.messageId").value("9007199254740993"));
    }

    @Test
    void controllerExposesOnlyReadMappings() {
        for (Method method : MessagePushReadController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PostMapping.class)).isNull();
            assertThat(method.getAnnotation(PutMapping.class)).isNull();
            assertThat(method.getAnnotation(PatchMapping.class)).isNull();
            assertThat(method.getAnnotation(DeleteMapping.class)).isNull();
        }
    }
}
