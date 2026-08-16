package org.example.trademodel.telegram;

import org.example.trademodel.entity.MessageDO;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageFormatter {
    private static final String MANUAL_REVIEW = "仅供人工复核，不构成交易指令。\n系统不会自动下单、平仓或反手。";
    private final TelegramLinkPolicy linkPolicy;

    public TelegramMessageFormatter(TelegramLinkPolicy linkPolicy) {
        this.linkPolicy = linkPolicy;
    }

    public TelegramOutboundMessage format(MessageDO message) {
        if (message == null) throw new IllegalArgumentException("Message is required");
        String link = null;
        String label = null;
        if ("POSITION_LOGIC_RISK_CHANGE".equals(message.getCategory())) {
            link = linkPolicy.positionDetailLink(message.getPositionId());
            label = link == null ? null : "查看持仓详情";
        } else if (message.getCurrentRecheckId() != null) {
            link = linkPolicy.pushRecheckLink(message.getCurrentRecheckId());
            label = link == null ? null : "打开并重新校验";
        }
        String fallback = link == null ? "\n\n请打开Fundamental AI站内消息查看。" : "";
        String text = trim(message.getTitle(), "Fundamental AI 提醒") + "\n\n"
                + trim(message.getBody(), "请打开系统进行人工复核。") + fallback + "\n\n" + MANUAL_REVIEW;
        return new TelegramOutboundMessage(text, label, link);
    }

    private static String trim(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim();
        return normalized.length() <= 3000 ? normalized : normalized.substring(0, 3000);
    }
}
