package org.example.trademodel.telegram;

import org.example.trademodel.entity.MessageDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramMessageFormatterTest {

    @Test
    void eligibleOpportunityWithRealSnapshotUsesRecheckLinkWithoutFixedTail() {
        TelegramProperties properties = new TelegramProperties();
        properties.setPublicBaseUrl("https://app.example.test");
        TelegramMessageFormatter formatter = new TelegramMessageFormatter(new TelegramLinkPolicy(properties));
        MessageDO message = HighValueAlertPolicyTest.eligibleOpportunityMessage();
        message.setSourceType("PUSH_SNAPSHOT");
        message.setSourceId("99");

        TelegramOutboundMessage output = formatter.format(message);

        assertThat(output.buttonLabel()).isEqualTo("打开并重新校验");
        assertThat(output.buttonUrl()).isEqualTo("https://app.example.test/recheck/push-snapshot-99");
        assertThat(output.text()).contains("【可复核执行计划】", "止损：98", "操作：打开系统重新校验")
                .doesNotContain("不构成交易指令", "系统不会自动下单", "请打开Fundamental AI站内消息查看");
    }

    @Test
    void eligibleOpportunityWithoutSnapshotHasNoLinkAndNoGenericFallback() {
        TelegramProperties properties = new TelegramProperties();
        properties.setPublicBaseUrl("https://app.example.test");
        MessageDO message = HighValueAlertPolicyTest.eligibleOpportunityMessage();

        TelegramOutboundMessage output = new TelegramMessageFormatter(
                new TelegramLinkPolicy(properties)).format(message);

        assertThat(output.buttonUrl()).isNull();
        assertThat(output.buttonLabel()).isNull();
        assertThat(output.text()).doesNotContain("请打开Fundamental AI站内消息查看", "不构成交易指令");
    }

    @Test
    void eligiblePositionUsesOnlyItsOwnedDetailLinkAndShortBody() {
        TelegramProperties properties = new TelegramProperties();
        properties.setPublicBaseUrl("https://app.example.test");
        MessageDO message = HighValueAlertPolicyTest.eligiblePositionMessage("RISK_HIGH");

        TelegramOutboundMessage output = new TelegramMessageFormatter(
                new TelegramLinkPolicy(properties)).format(message);

        assertThat(output.buttonLabel()).isEqualTo("查看持仓详情");
        assertThat(output.buttonUrl()).isEqualTo("https://app.example.test/positions/91");
        assertThat(output.text()).contains("【持仓需关注】", "入场：100", "现价：99")
                .doesNotContain("不构成交易指令", "系统不会自动平仓", "请打开Fundamental AI站内消息查看");
    }
}
