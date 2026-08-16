package org.example.trademodel.telegram;

public interface TelegramClient {
    TelegramClientResult sendMessage(TelegramOutboundMessage message);

    TelegramClientResult getMe();
}
