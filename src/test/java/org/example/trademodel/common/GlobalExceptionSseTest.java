package org.example.trademodel.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionSseTest {
    @RestController
    static class DisconnectedStream {
        @GetMapping(value = "/test/closed", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        String closed() throws AsyncRequestNotUsableException {
            throw new AsyncRequestNotUsableException("client stream closed");
        }
        @GetMapping(value = "/test/timeout", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        String timeout() {
            throw new AsyncRequestTimeoutException();
        }
    }

    @Test
    void disconnectedAndTimedOutStreamsDoNotAttemptJsonErrorSerialization() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new DisconnectedStream())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String path : new String[]{"/test/closed", "/test/timeout"}) {
            mvc.perform(get(path).accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""))
                    .andExpect(header().doesNotExist("Content-Type"));
        }
    }
}
