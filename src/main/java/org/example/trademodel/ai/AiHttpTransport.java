package org.example.trademodel.ai;

import java.io.IOException;

public interface AiHttpTransport {
    AiHttpResponse post(AiHttpRequest request) throws IOException, InterruptedException;

    default AiHttpResponse get(AiHttpRequest request) throws IOException, InterruptedException {
        throw new IOException("HTTP_GET_NOT_SUPPORTED");
    }
}
