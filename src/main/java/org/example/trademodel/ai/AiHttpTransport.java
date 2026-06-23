package org.example.trademodel.ai;

import java.io.IOException;

public interface AiHttpTransport {
    AiHttpResponse post(AiHttpRequest request) throws IOException, InterruptedException;
}
