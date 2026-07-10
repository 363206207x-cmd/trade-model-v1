package org.example.trademodel.service.impl;

import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.RecheckExecutionCommand;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.service.support.RuleConfigContractService;
import org.example.trademodel.vo.PushRecheckOpsOverviewVO;
import org.example.trademodel.vo.PushRecheckReplaySummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class PushRecheckServiceImplTest {

    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Mock
    private PushRecheckLogMapper pushRecheckLogMapper;
    @Mock
    private PushRecheckDispatchConfigService dispatchConfigService;
    @Mock
    private UserPositionRiskAdapter userPositionRiskAdapter;
    @Mock
    private MarketQuoteClient marketQuoteClient;
    @Mock
    private RuleConfigContractService ruleConfigContractService;

    private PushRecheckServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PushRecheckServiceImpl(
                pushSnapshotMapper,
                accountRiskSnapshotMapper,
                pushRecheckLogMapper,
                dispatchConfigService,
                userPositionRiskAdapter,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                ruleConfigContractService);
        lenient().when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        lenient().when(ruleConfigContractService.requirePushRecheckThresholds())
                .thenReturn(new RuleConfigContractService.PushRecheckThresholds(
                        new BigDecimal("0.02"), 70, 85, 60));
    }

    @Test
    void snapshotMissing_invalidated() {
        when(pushSnapshotMapper.selectByPushId(1L)).thenReturn(null);
        RecheckResult r = service.recheck(1L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(pushRecheckLogMapper).insert(any());
        verify(pushSnapshotMapper).updatePushStatus(1L, "RECHECK_INVALIDATED");
    }

    @Test
    void expired() {
        TmPushSnapshotDO s = baseSnap();
        s.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(pushSnapshotMapper.selectByPushId(2L)).thenReturn(s);
        RecheckResult r = service.recheck(2L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.EXPIRED);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(pushRecheckLogMapper).insert(any());
        verify(pushSnapshotMapper).updatePushStatus(2L, "RECHECK_EXPIRED");
    }

    @Test
    void drifted() {
        TmPushSnapshotDO s = baseSnap();
        s.setTriggerPrice(new BigDecimal("100"));
        s.setConfusedScoreSnapshot(22);
        s.setDataQualityScoreSnapshot(72);
        when(pushSnapshotMapper.selectByPushId(3L)).thenReturn(s);
        RecheckResult r = service.recheck(3L, new BigDecimal("110"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getPriceDriftRatio()).isNotNull();
        assertThat(cap.getValue().getCurrentConfusedScore()).isEqualTo(22);
        assertThat(cap.getValue().getCurrentDataQualityScore()).isEqualTo(72);
        verify(pushSnapshotMapper).updatePushStatus(3L, "RECHECK_DRIFTED_FROM_ENTRY_ZONE");
    }

    @Test
    void invalidationPriceBelow() {
        TmPushSnapshotDO s = baseSnap();
        s.setInvalidationConditionJson("{\"invalidPriceBelow\":105,\"text\":\"x\"}");
        when(pushSnapshotMapper.selectByPushId(4L)).thenReturn(s);
        RecheckResult r = service.recheck(4L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertSafeReviewOnlyResult(r);
        verify(pushSnapshotMapper).updatePushStatus(4L, "RECHECK_INVALIDATED");
    }

    @Test
    void invalidCurrentPrice_invalidated() {
        TmPushSnapshotDO s = baseSnap();
        when(pushSnapshotMapper.selectByPushId(41L)).thenReturn(s);

        RecheckResult r = service.recheck(41L, BigDecimal.ZERO);

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(pushSnapshotMapper).updatePushStatus(41L, "RECHECK_INVALIDATED");
    }

    @Test
    void missingCurrentPriceFetchesCurrentQuoteReadOnly() {
        TmPushSnapshotDO s = baseSnap();
        s.setSymbol("BTCUSDT");
        s.setTriggerPrice(new BigDecimal("100"));
        s.setConfusedScoreSnapshot(10);
        when(pushSnapshotMapper.selectByPushId(42L)).thenReturn(s);
        MarketQuoteSnapshot quote = new MarketQuoteSnapshot();
        quote.setLastPrice(new BigDecimal("101"));
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(quote));

        RecheckResult r = service.recheck(42L, null);

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertThat(r.getCurrentPrice()).isEqualByComparingTo("101");
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getCurrentPrice()).isEqualByComparingTo("101");
        assertThat(cap.getValue().getExecutionErrorCode()).isNull();
    }

    @Test
    void missingCurrentPriceQuoteEmptyFailsClosedWithQuoteUnavailable() {
        TmPushSnapshotDO s = baseSnap();
        s.setSymbol("BTCUSDT");
        when(pushSnapshotMapper.selectByPushId(43L)).thenReturn(s);
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        RecheckResult r = service.recheck(43L, null);

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getExecutionErrorCode()).isEqualTo("QUOTE_UNAVAILABLE");
        assertThat(cap.getValue().getFailReasonJson()).contains("\"code\":\"QUOTE_UNAVAILABLE\"");
        assertThat(cap.getValue().getCurrentPrice()).isNull();
        verify(pushSnapshotMapper).updatePushStatus(43L, "RECHECK_INVALIDATED");
    }

    @Test
    void missingCurrentPriceQuoteNullLastPriceFailsClosedWithQuoteUnavailable() {
        TmPushSnapshotDO s = baseSnap();
        s.setSymbol("BTCUSDT");
        when(pushSnapshotMapper.selectByPushId(44L)).thenReturn(s);
        MarketQuoteSnapshot quote = new MarketQuoteSnapshot();
        quote.setLastPrice(null);
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(quote));

        RecheckResult r = service.recheck(44L, null);

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getExecutionErrorCode()).isEqualTo("QUOTE_UNAVAILABLE");
        assertThat(cap.getValue().getFailReasonJson()).contains("\"code\":\"QUOTE_UNAVAILABLE\"");
        assertThat(cap.getValue().getCurrentPrice()).isNull();
        verify(pushSnapshotMapper).updatePushStatus(44L, "RECHECK_INVALIDATED");
    }

    @Test
    void missingCurrentPriceQuoteThrowsFailsClosedWithQuoteUnavailable() {
        TmPushSnapshotDO s = baseSnap();
        s.setSymbol("BTCUSDT");
        when(pushSnapshotMapper.selectByPushId(45L)).thenReturn(s);
        when(marketQuoteClient.fetch24hTicker("BTCUSDT"))
                .thenThrow(new RuntimeException("provider unavailable"));

        RecheckResult r = service.recheck(45L, null);

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getExecutionErrorCode()).isEqualTo("QUOTE_UNAVAILABLE");
        assertThat(cap.getValue().getFailReasonJson()).contains("\"code\":\"QUOTE_UNAVAILABLE\"");
        assertThat(cap.getValue().getCurrentPrice()).isNull();
        verify(pushSnapshotMapper).updatePushStatus(45L, "RECHECK_INVALIDATED");
    }

    @Test
    void missingCurrentPriceSnapshotSymbolMissingFailsClosedWithPriceRequired() {
        TmPushSnapshotDO s = baseSnap();
        s.setSymbol(null);
        when(pushSnapshotMapper.selectByPushId(46L)).thenReturn(s);

        RecheckResult r = service.recheck(46L, null);

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        verifyNoInteractions(marketQuoteClient);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getExecutionErrorCode()).isEqualTo("PRICE_REQUIRED");
        assertThat(cap.getValue().getFailReasonJson()).contains("\"code\":\"PRICE_REQUIRED\"");
        assertThat(cap.getValue().getCurrentPrice()).isNull();
        verify(pushSnapshotMapper).updatePushStatus(46L, "RECHECK_INVALIDATED");
    }

    @Test
    void providedCurrentPriceKeepsExistingBehaviorAndDoesNotFetchQuote() {
        TmPushSnapshotDO s = baseSnap();
        s.setSymbol("BTCUSDT");
        s.setTriggerPrice(new BigDecimal("100"));
        s.setConfusedScoreSnapshot(10);
        when(pushSnapshotMapper.selectByPushId(47L)).thenReturn(s);

        RecheckResult r = service.recheck(47L, new BigDecimal("100"));

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isTrue();
        assertSafeReviewOnlyResult(r);
        verifyNoInteractions(marketQuoteClient);
        verify(pushSnapshotMapper).updatePushStatus(47L, "RECHECK_REVIEW_PASSED");
    }

    @Test
    void reviewWaiting_highConfused() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(75);
        s.setDataQualityScoreSnapshot(65);
        when(pushSnapshotMapper.selectByPushId(5L)).thenReturn(s);
        RecheckResult r = service.recheck(5L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_WAITING);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getCurrentConfusedScore()).isEqualTo(75);
        assertThat(cap.getValue().getCurrentDataQualityScore()).isEqualTo(65);
        verify(pushSnapshotMapper).updatePushStatus(5L, "RECHECK_REVIEW_WAITING");
    }

    @Test
    void reviewWaiting_lowExecutionFeasibility() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(20);
        s.setExecutionFeasibilitySnapshot(55);
        when(pushSnapshotMapper.selectByPushId(52L)).thenReturn(s);

        RecheckResult r = service.recheck(52L, new BigDecimal("100"));

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_WAITING);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(pushSnapshotMapper).updatePushStatus(52L, "RECHECK_REVIEW_WAITING");
    }

    @Test
    void confusedBlocked_whenConfusedVeryHigh() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(90);
        when(pushSnapshotMapper.selectByPushId(51L)).thenReturn(s);

        RecheckResult r = service.recheck(51L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.CONFUSED_BLOCKED);
        assertThat(r.isValid()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(pushSnapshotMapper).updatePushStatus(51L, "RECHECK_CONFUSED_BLOCKED");
    }

    @Test
    void reviewPassed() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(10);
        s.setDataQualityScoreSnapshot(88);
        when(pushSnapshotMapper.selectByPushId(6L)).thenReturn(s);
        RecheckResult r = service.recheck(6L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isTrue();
        assertSafeReviewOnlyResult(r);
        assertThat(r.getMessage()).isEqualTo("复查条件通过，仅供人工复核，不是交易指令");
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getCurrentConfusedScore()).isEqualTo(10);
        assertThat(cap.getValue().getCurrentDataQualityScore()).isEqualTo(88);
        verify(pushSnapshotMapper).updatePushStatus(6L, "RECHECK_REVIEW_PASSED");
    }

    @Test
    void reviewPassed_shouldUpdatePushStatus() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(10);
        s.setDataQualityScoreSnapshot(88);
        when(pushSnapshotMapper.selectByPushId(6L)).thenReturn(s);

        RecheckResult r = service.recheck(6L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);

        verify(pushSnapshotMapper).updatePushStatus(6L, "RECHECK_REVIEW_PASSED");
    }

    @Test
    void riskBlocked_whenAccountRiskNotAllowed() {
        TmPushSnapshotDO s = baseSnap();
        s.setAccountRiskSnapshotId(99L);
        when(pushSnapshotMapper.selectByPushId(7L)).thenReturn(s);
        TmAccountRiskSnapshotDO risk = new TmAccountRiskSnapshotDO();
        risk.setId(99L);
        risk.setRiskAllowed(Boolean.FALSE);
        when(accountRiskSnapshotMapper.selectById(99L)).thenReturn(risk);

        RecheckResult r = service.recheck(7L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.RISK_BLOCKED);
        assertThat(r.isValid()).isFalse();
        assertSafeReviewOnlyResult(r);

        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getCurrentAccountRiskAllowed()).isFalse();
        verify(pushSnapshotMapper).updatePushStatus(7L, "RECHECK_RISK_BLOCKED");
    }

    @Test
    void riskNullDoesNotBlock() {
        TmPushSnapshotDO s = baseSnap();
        s.setAccountRiskSnapshotId(100L);
        s.setConfusedScoreSnapshot(10);
        when(pushSnapshotMapper.selectByPushId(8L)).thenReturn(s);
        when(accountRiskSnapshotMapper.selectById(100L)).thenReturn(null);

        RecheckResult r = service.recheck(8L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertSafeReviewOnlyResult(r);
    }

    @Test
    void pushRecheckConsumesAllowedUserPositionRiskResultReadOnly() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(10);
        s.setDataQualityScoreSnapshot(88);
        when(pushSnapshotMapper.selectByPushId(81L)).thenReturn(s);
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));

        RecheckResult r = service.recheck(81L, new BigDecimal("100"));

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isTrue();
        assertSafeReviewOnlyResult(r);
        verify(userPositionRiskAdapter).currentRisk();

        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getCurrentAccountRiskAllowed()).isTrue();
    }

    @Test
    void pushRecheckConsumesBlockedUserPositionRiskResultFailClosed() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(10);
        s.setDataQualityScoreSnapshot(88);
        when(pushSnapshotMapper.selectByPushId(82L)).thenReturn(s);
        UserPositionRiskResult blocked = UserPositionRiskResult.failClosed("HIGH_LEVERAGE_RISK");
        when(userPositionRiskAdapter.currentRisk()).thenReturn(blocked);

        RecheckResult r = service.recheck(82L, new BigDecimal("100"));

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.RISK_BLOCKED);
        assertThat(r.isValid()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(userPositionRiskAdapter).currentRisk();

        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getCurrentAccountRiskAllowed()).isFalse();
        assertThat(cap.getValue().getExecutionErrorCode()).isEqualTo("RISK_BLOCKED");
        assertThat(cap.getValue().getFailReasonJson()).contains("HIGH_LEVERAGE_RISK");
        verify(pushSnapshotMapper).updatePushStatus(82L, "RECHECK_RISK_BLOCKED");
    }

    @Test
    void shouldPersistDispatchMetadataFromScheduledCommand() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(10);
        when(pushSnapshotMapper.selectByPushId(9L)).thenReturn(s);

        service.recheck(
                9L,
                new BigDecimal("100"),
                RecheckExecutionCommand.scheduled("SCH-B1", "SCH-B1-PUSH-9", 2, 3, 5));

        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getDispatchBatchId()).isEqualTo("SCH-B1");
        assertThat(cap.getValue().getDispatchInstructionId()).isEqualTo("SCH-B1-PUSH-9");
        assertThat(cap.getValue().getTriggerSource()).isEqualTo("SCHEDULED");
        assertThat(cap.getValue().getRetryAttempt()).isEqualTo(2);
        assertThat(cap.getValue().getMaxAttempts()).isEqualTo(3);
        assertThat(cap.getValue().getRetryBackoffMinutes()).isEqualTo(5);
        assertThat(cap.getValue().getExecutionStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void replayByInstruction_shouldRecheckUsingHistoricalPrice() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(10);
        when(pushSnapshotMapper.selectByPushId(10L)).thenReturn(s);

        org.example.trademodel.entity.TmPushRecheckLogDO old = new org.example.trademodel.entity.TmPushRecheckLogDO();
        old.setLogId(1001L);
        old.setPushId(10L);
        old.setCurrentPrice(new BigDecimal("101"));
        old.setDispatchInstructionId("SCH-B2-PUSH-10");
        when(pushRecheckLogMapper.selectByInstructionId("SCH-B2-PUSH-10"))
                .thenReturn(java.util.List.of(old));

        List<RecheckResult> results = service.replayByDispatch(null, "SCH-B2-PUSH-10");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPushId()).isEqualTo(10L);

        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getTriggerSource()).isEqualTo("REPLAY");
        assertThat(cap.getValue().getReplayFromLogId()).isEqualTo(1001L);
    }

    @Test
    void summarizeReplayByInstruction_shouldAggregateMinimalMetrics() {
        org.example.trademodel.entity.TmPushRecheckLogDO latest = new org.example.trademodel.entity.TmPushRecheckLogDO();
        latest.setLogId(2003L);
        latest.setDispatchBatchId("SCH-B3");
        latest.setDispatchInstructionId("SCH-B3-PUSH-11");
        latest.setTriggerSource("REPLAY");
        latest.setRecheckStatus("VALID_WAITING");
        latest.setExecutionStatus("COMPLETED");
        latest.setExecutionErrorCode("RISK_UNKNOWN_WAIT");
        latest.setRecheckTime(LocalDateTime.now());

        org.example.trademodel.entity.TmPushRecheckLogDO old1 = new org.example.trademodel.entity.TmPushRecheckLogDO();
        old1.setLogId(2002L);
        old1.setTriggerSource("SCHEDULED");
        old1.setRecheckStatus("VALID_EXECUTABLE");
        old1.setExecutionStatus("COMPLETED");

        org.example.trademodel.entity.TmPushRecheckLogDO old2 = new org.example.trademodel.entity.TmPushRecheckLogDO();
        old2.setLogId(2001L);
        old2.setTriggerSource("REPLAY");
        old2.setRecheckStatus("RISK_BLOCKED");
        old2.setExecutionStatus("FAILED");

        when(pushRecheckLogMapper.selectByInstructionId("SCH-B3-PUSH-11"))
                .thenReturn(List.of(latest, old1, old2));

        PushRecheckReplaySummaryVO summary = service.summarizeReplayByDispatch(null, "SCH-B3-PUSH-11");

        assertThat(summary.getDispatchBatchId()).isEqualTo("SCH-B3");
        assertThat(summary.getDispatchInstructionId()).isEqualTo("SCH-B3-PUSH-11");
        assertThat(summary.getTriggerSource()).isEqualTo("REPLAY");
        assertThat(summary.getTotalCount()).isEqualTo(3);
        assertThat(summary.getSuccessCount()).isEqualTo(1);
        assertThat(summary.getBlockingCount()).isEqualTo(1);
        assertThat(summary.getWaitingCount()).isEqualTo(1);
        assertThat(summary.getExpiredCount()).isEqualTo(0);
        assertThat(summary.getReplayCount()).isEqualTo(2);
        assertThat(summary.getLatestExecutionStatus()).isEqualTo("COMPLETED");
        assertThat(summary.getLatestErrorCode()).isEqualTo("RISK_UNKNOWN_WAIT");
        assertThat(summary.getHasError()).isTrue();
    }

    @Test
    void getLatestLog_shouldCanonicalizeLegacyStatusForReadApi() {
        org.example.trademodel.entity.TmPushRecheckLogDO old = new org.example.trademodel.entity.TmPushRecheckLogDO();
        old.setLogId(4001L);
        old.setPushId(12L);
        old.setRecheckStatus("DRIFTED");
        when(pushRecheckLogMapper.selectByPushId(12L)).thenReturn(List.of(old));

        assertThat(service.getLatestLog(12L).getRecheckStatus())
                .isEqualTo("DRIFTED_FROM_ENTRY_ZONE");
    }

    @Test
    void pushRecheckRemainsReviewOnly() {
        List<String> fieldNames = Arrays.stream(RecheckResult.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toList();

        assertThat(fieldNames).doesNotContain(
                "executable",
                "tradeAllowed",
                "orderAllowed",
                "openAllowed",
                "executionAuthorized",
                "tradingAuthorized",
                "sendablePayload",
                "providerPayload",
                "orderAction",
                "executionAction",
                "autoTradingAction");
    }

    @Test
    void summarizeReplayByDispatch_shouldReturnZeroSummaryWhenNoLogs() {
        when(pushRecheckLogMapper.selectByBatchId("SCH-EMPTY")).thenReturn(List.of());

        PushRecheckReplaySummaryVO summary = service.summarizeReplayByDispatch("SCH-EMPTY", null);

        assertThat(summary.getDispatchBatchId()).isEqualTo("SCH-EMPTY");
        assertThat(summary.getDispatchInstructionId()).isNull();
        assertThat(summary.getTotalCount()).isEqualTo(0);
        assertThat(summary.getSuccessCount()).isEqualTo(0);
        assertThat(summary.getBlockingCount()).isEqualTo(0);
        assertThat(summary.getWaitingCount()).isEqualTo(0);
        assertThat(summary.getExpiredCount()).isEqualTo(0);
        assertThat(summary.getReplayCount()).isEqualTo(0);
        assertThat(summary.getHasError()).isFalse();
    }

    @Test
    void getOpsOverview_shouldAggregateFourReadOnlyBlocks() {
        when(dispatchConfigService.getCurrentConfig()).thenReturn(java.util.Map.of(
                "limit", 50,
                "maxAttempts", 3,
                "minRetryMinutes", 5));
        org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO latestAudit =
                new org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO();
        latestAudit.setChangedBy("ops");
        latestAudit.setConfigKey("maxAttempts");
        latestAudit.setOldValue(2);
        latestAudit.setNewValue(3);
        latestAudit.setCreateTime(LocalDateTime.now());
        when(dispatchConfigService.listRecentAudit(5)).thenReturn(List.of(latestAudit));

        org.example.trademodel.entity.TmPushRecheckLogDO latestLog = new org.example.trademodel.entity.TmPushRecheckLogDO();
        latestLog.setLogId(3001L);
        latestLog.setDispatchBatchId("SCH-B9");
        latestLog.setDispatchInstructionId("SCH-B9-PUSH-1");
        latestLog.setTriggerSource("SCHEDULED");
        latestLog.setExecutionStatus("COMPLETED");
        latestLog.setCreateTime(LocalDateTime.now());
        when(pushRecheckLogMapper.selectRecent(10)).thenReturn(List.of(latestLog));
        when(pushRecheckLogMapper.selectByInstructionId("SCH-B9-PUSH-1")).thenReturn(List.of(latestLog));

        PushRecheckOpsOverviewVO overview = service.getOpsOverview(null, null, null, null);

        assertThat(overview.getConfig().getLimit()).isEqualTo(50);
        assertThat(overview.getAuditSummary().getAuditCount()).isEqualTo(1);
        assertThat(overview.getLatestReplaySummary().getDispatchInstructionId()).isEqualTo("SCH-B9-PUSH-1");
        assertThat(overview.getRecentLogs()).hasSize(1);
        assertThat(overview.getRecentLogs().get(0).getLogId()).isEqualTo(3001L);
    }

    @Test
    void getOpsOverview_shouldRespectExplicitDispatchParams() {
        when(dispatchConfigService.getCurrentConfig()).thenReturn(java.util.Map.of());
        when(dispatchConfigService.listRecentAudit(8)).thenReturn(List.of());
        when(pushRecheckLogMapper.selectRecent(7)).thenReturn(List.of());
        when(pushRecheckLogMapper.selectByBatchId("SCH-B10")).thenReturn(List.of());

        service.getOpsOverview("SCH-B10", null, 8, 7);

        verify(pushRecheckLogMapper).selectByBatchId("SCH-B10");
    }

    @Test
    void getOpsOverview_emptyAuditAndReplay_stillReturnsNestedShells() {
        when(dispatchConfigService.getCurrentConfig()).thenReturn(java.util.Map.of());
        when(dispatchConfigService.listRecentAudit(anyInt())).thenReturn(List.of());
        when(pushRecheckLogMapper.selectRecent(anyInt())).thenReturn(List.of());

        PushRecheckOpsOverviewVO overview = service.getOpsOverview(null, null, null, null);

        assertThat(overview.getAuditSummary()).isNotNull();
        assertThat(overview.getAuditSummary().getAuditCount()).isEqualTo(0);
        assertThat(overview.getLatestReplaySummary()).isNotNull();
        assertThat(overview.getLatestReplaySummary().getTotalCount()).isEqualTo(0);
    }

    private static TmPushSnapshotDO baseSnap() {
        TmPushSnapshotDO s = new TmPushSnapshotDO();
        s.setExpiresAt(LocalDateTime.now().plusHours(1));
        return s;
    }

    private static void assertSafeReviewOnlyResult(RecheckResult result) {
        assertThat(result.isValid()).isFalse();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
        assertThat(result.isNotPositionMutation()).isTrue();
        assertThat(result.isNotTradingAuthorization()).isTrue();
        assertThat(result.getMessage()).doesNotContain("可执行", "允许执行", "可以交易", "允许交易",
                "可以买入", "可以卖出", "可以开仓", "交易授权", "下单授权");
    }
}
