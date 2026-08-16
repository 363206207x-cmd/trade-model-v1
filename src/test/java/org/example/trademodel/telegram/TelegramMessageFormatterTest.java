package org.example.trademodel.telegram;

import org.example.trademodel.entity.MessageDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramMessageFormatterTest {

    @Test
    void opportunityUsesOnlySafeRecheckLinkAndManualReviewLanguage() {
        TelegramProperties properties = new TelegramProperties();
        properties.setPublicBaseUrl("https://app.example.test");
        TelegramMessageFormatter formatter = new TelegramMessageFormatter(new TelegramLinkPolicy(properties));
        MessageDO message = message("HIGH_PERMISSION_OPPORTUNITY");
        message.setCurrentRecheckId("71");

        TelegramOutboundMessage output = formatter.format(message);

        assertThat(output.buttonLabel()).isEqualTo("打开并重新校验");
        assertThat(output.buttonUrl()).isEqualTo("https://app.example.test/recheck/71");
        assertThat(output.text()).contains(
                "仅供人工复核，不构成交易指令。", "系统不会自动下单、平仓或反手。");
    }

    @Test
    void positionUsesOnlyPositionDetailAndPrivateDeploymentFallsBackToInAppText() {
        TelegramProperties publicProperties = new TelegramProperties();
        publicProperties.setPublicBaseUrl("https://app.example.test");
        MessageDO position = message("POSITION_LOGIC_RISK_CHANGE");
        position.setPositionId(91L);
        TelegramOutboundMessage linked = new TelegramMessageFormatter(
                new TelegramLinkPolicy(publicProperties)).format(position);
        assertThat(linked.buttonLabel()).isEqualTo("查看持仓详情");
        assertThat(linked.buttonUrl()).isEqualTo("https://app.example.test/positions/91");

        TelegramProperties privateProperties = new TelegramProperties();
        privateProperties.setPublicBaseUrl("http://localhost:8080");
        TelegramOutboundMessage fallback = new TelegramMessageFormatter(
                new TelegramLinkPolicy(privateProperties)).format(position);
        assertThat(fallback.buttonUrl()).isNull();
        assertThat(fallback.text()).contains("请打开Fundamental AI站内消息查看。");
    }

    private static MessageDO message(String category) {
        MessageDO message = new MessageDO();
        message.setCategory(category);
        message.setTitle("高价值提醒");
        message.setBody("资产：BTCUSDT\n建议：打开系统人工复核");
        return message;
    }
}
