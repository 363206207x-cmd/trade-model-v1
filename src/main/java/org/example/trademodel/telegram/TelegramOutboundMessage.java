package org.example.trademodel.telegram;

public record TelegramOutboundMessage(String text, String buttonLabel, String buttonUrl) {
    public TelegramOutboundMessage {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Telegram text is required");
        }
        if ((buttonLabel == null) != (buttonUrl == null)) {
            throw new IllegalArgumentException("Telegram button label and URL must be supplied together");
        }
    }
}
