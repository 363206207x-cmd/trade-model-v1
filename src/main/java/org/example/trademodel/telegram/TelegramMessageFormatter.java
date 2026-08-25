package org.example.trademodel.telegram;

import org.example.trademodel.entity.MessageDO;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageFormatter {
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
        } else if ("PUSH_SNAPSHOT".equals(message.getSourceType()) && hasText(message.getSourceId())) {
            link = linkPolicy.pushRecheckLink(message.getSourceId());
            label = link == null ? null : "打开系统重新校验";
        }
        String fallback = link == null ? "\n\n请打开 TRINE LOGIC 站内消息查看。" : "";
        String text = trim(message.getTitle(), "TRINE LOGIC 提醒") + "\n\n"
                + trim(message.getBody(), "请打开系统进行人工复核。") + fallback;
        return new TelegramOutboundMessage(text, label, link);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trim(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim();
        return normalized.length() <= 3000 ? normalized : normalized.substring(0, 3000);
    }
}
