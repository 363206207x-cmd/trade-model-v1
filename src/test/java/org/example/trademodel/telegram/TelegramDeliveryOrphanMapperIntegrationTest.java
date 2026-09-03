package org.example.trademodel.telegram;

import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.service.MessageFactService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Tag("core-regression")
class TelegramDeliveryOrphanMapperIntegrationTest {
    @Autowired private MessageFactService messageFactService;
    @Autowired private MessageMapper messageMapper;

    @Test
    void orphanQueryReturnsAllThreeCanonicalUnexpiredTelegramShapes() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        MessageDO opportunity = HighValueAlertPolicyTest.eligibleOpportunityMessage();
        opportunity.setMessageId("orphan-plan");
        opportunity.setExpiresAt(now.plusHours(1));
        opportunity.setDedupeKey(TelegramDedupeKey.create(
                "OPPORTUNITY_READY", "CONFIRMATION", 3, 15,
                41L, "FINAL_PLAN", "plan-9", now));
        messageFactService.record(opportunity);

        MessageDO position = HighValueAlertPolicyTest.eligiblePositionMessage("RISK_HIGH");
        position.setMessageId("orphan-position");
        position.setExpiresAt(now.plusHours(1));
        position.setDedupeKey(TelegramDedupeKey.create(
                "POSITION_RISK_CHANGE", "RISK_HIGH", 3, 15,
                41L, "USER_POSITION", "91", now));
        messageFactService.record(position);

        MessageDO safety = HighValueAlertPolicyTest.eligibleSafetyMessage();
        safety.setMessageId("orphan-safety");
        safety.setExpiresAt(now.plusHours(1));
        safety.setDedupeKey(TelegramDedupeKey.create(
                "PLAN_SAFETY_CHANGE", "HOT_RESET", 4, 15,
                41L, "FINAL_PLAN", "plan-9", now));
        messageFactService.record(safety);

        messageFactService.record(inAppOnly("orphan-old-plan", "HIGH_PERMISSION_OPPORTUNITY",
                "OPPORTUNITY_READY", "TRIGGERED", "【机会达到人工复核条件】", now));
        messageFactService.record(inAppOnly("orphan-old-position", "POSITION_LOGIC_RISK_CHANGE",
                "POSITION_RISK_CHANGE", "STRONG_REVERSAL", "【持仓逻辑发生重要变化】", now));
        messageFactService.record(malformedCanonicalOpportunity(now));
        messageFactService.record(malformedCanonicalPosition(now));
        messageFactService.record(malformedCanonicalSafety(now));
        MessageDO expiredSafety = HighValueAlertPolicyTest.eligibleSafetyMessage();
        expiredSafety.setMessageId("orphan-expired-safety");
        expiredSafety.setSourceId("plan-expired");
        expiredSafety.setPlanId("plan-expired");
        expiredSafety.setExpiresAt(now.minusSeconds(1));
        expiredSafety.setDedupeKey(TelegramDedupeKey.create(
                "PLAN_SAFETY_CHANGE", "FINAL_INVALIDATED", 4, 15,
                41L, "FINAL_PLAN", "plan-expired", now.minusHours(2)));
        messageFactService.record(expiredSafety);

        List<MessageDO> orphans = messageMapper.listTelegramDeliveryOrphans(now, 20);

        assertThat(orphans).extracting(MessageDO::getMessageId)
                .containsExactly("orphan-plan", "orphan-position", "orphan-safety");
    }

    private static MessageDO malformedCanonicalOpportunity(LocalDateTime now) {
        MessageDO message = HighValueAlertPolicyTest.eligibleOpportunityMessage();
        message.setMessageId("orphan-malformed-plan");
        message.setPlanId("plan-malformed");
        message.setSourceType("FINAL_PLAN");
        message.setSourceId("plan-malformed");
        message.setExpiresAt(now.plusHours(1));
        message.setBody("BTCUSDT  ·  强偏多  ·  确认型\n\n字段不完整");
        message.setDedupeKey(TelegramDedupeKey.create(
                "OPPORTUNITY_READY", "CONFIRMATION", 3, 15,
                41L, "FINAL_PLAN", "plan-malformed", now));
        return message;
    }

    private static MessageDO malformedCanonicalPosition(LocalDateTime now) {
        MessageDO message = HighValueAlertPolicyTest.eligiblePositionMessage("RISK_HIGH");
        message.setMessageId("orphan-malformed-position");
        message.setPositionId(92L);
        message.setSourceId("302");
        message.setExpiresAt(now.plusHours(1));
        message.setBody("BTCUSDT  ·  做多\n\n变化：风险高");
        message.setDedupeKey(TelegramDedupeKey.create(
                "POSITION_RISK_CHANGE", "RISK_HIGH", 3, 15,
                41L, "USER_POSITION", "92", now));
        return message;
    }

    private static MessageDO malformedCanonicalSafety(LocalDateTime now) {
        MessageDO message = HighValueAlertPolicyTest.eligibleSafetyMessage();
        message.setMessageId("orphan-malformed-safety");
        message.setSourceId("plan-malformed-safety");
        message.setPlanId("plan-malformed-safety");
        message.setExpiresAt(now.plusHours(1));
        message.setBody("资产：BTCUSDT\n变化：Hot Reset");
        message.setDedupeKey(TelegramDedupeKey.create(
                "PLAN_SAFETY_CHANGE", "HOT_RESET", 4, 15,
                41L, "FINAL_PLAN", "plan-malformed-safety", now));
        return message;
    }

    private static MessageDO inAppOnly(String messageId, String category,
                                       String event, String state, String title,
                                       LocalDateTime now) {
        MessageDO message = new MessageDO();
        message.setMessageId(messageId);
        message.setUserId(41L);
        message.setCategory(category);
        message.setSourceType("POSITION_MONITOR");
        message.setSourceId("301");
        message.setPositionId(91L);
        message.setPlanId("plan-9");
        message.setAnalysisId("analysis-9");
        message.setSymbol("BTCUSDT");
        message.setTraceId("trace-9");
        message.setTitle(title);
        message.setBody("真实站内业务事实，仅供站内呈现");
        message.setExpiresAt(now.plusHours(1));
        message.setDedupeKey(TelegramDedupeKey.create(
                event, state, 4, 15, 41L, "USER_POSITION", messageId,
                now));
        return message;
    }
}
