package org.example.trademodel.service.impl;

import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
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
    @Mock
    private DerivativesSnapshotReadPort derivativesSnapshotReadPort;

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
        service.setDerivativesSnapshotReadPort(derivativesSnapshotReadPort);
        lenient().when(userPositionRiskAdapter.currentRiskForSystem()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        lenient().when(ruleConfigContractService.requirePushRecheckThresholds())
                .thenReturn(new RuleConfigContractService.PushRecheckThresholds(
                        new BigDecimal("0.02"), 70, 85, 60));
    }

    @Test
    void manualTriggerFailsClosedBeforeRepositoryMutation() {
        assertThatThrownBy(() -> service.recheck(1L, new BigDecimal("100")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("USER_TRIGGER_AND_REPLAY_DISABLED");

        verifyNoInteractions(pushSnapshotMapper, pushRecheckLogMapper);
    }

    @Test
    void snapshotMissing_invalidated() {
        when(pushSnapshotMapper.selectByPushId(1L)).thenReturn(null);
        RecheckResult r = scheduledRecheck(1L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(pushRecheckLogMapper).insert(any());
        verify(pushSnapshotMapper).updatePushStatus(1L, "RECHECK_INVALIDATED");
    }

    @Test
    void expired() {
        service.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC));
        TmPushSnapshotDO s = baseSnap();
        s.setExpiresAt(LocalDateTime.parse("2026-07-14T11:59:00"));
        when(pushSnapshotMapper.selectByPushId(2L)).thenReturn(s);
        RecheckResult r = scheduledRecheck(2L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.EXPIRED);
        assertThat(r.isReviewPassed()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(pushRecheckLogMapper).insert(any());
        verify(pushSnapshotMapper).updatePushStatus(2L, "RECHECK_EXPIRED");
    }

    @Test
    void pushRecheckExpiresAtExactBoundary() {
        LocalDateTime expiresAt = LocalDateTime.parse("2026-07-14T12:00:00");
        TmPushSnapshotDO snapshot = baseSnap();
        snapshot.setExpiresAt(expiresAt);
        when(pushSnapshotMapper.selectByPushId(201L)).thenReturn(snapshot);
        when(pushSnapshotMapper.selectByPushId(202L)).thenReturn(snapshot);
        when(pushSnapshotMapper.selectByPushId(203L)).thenReturn(snapshot);

        service.setClock(Clock.fixed(Instant.parse("2026-07-14T11:59:59Z"), ZoneOffset.UTC));
        RecheckResult before = scheduledRecheck(201L, new BigDecimal("100"));
        service.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC));
        RecheckResult equal = scheduledRecheck(202L, new BigDecimal("100"));
        service.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:01Z"), ZoneOffset.UTC));
        RecheckResult after = scheduledRecheck(203L, new BigDecimal("100"));

        assertThat(before.getRecheckStatus()).isNotEqualTo(RecheckStatusEnum.EXPIRED);
        assertThat(equal.getRecheckStatus()).isEqualTo(RecheckStatusEnum.EXPIRED);
        assertThat(after.getRecheckStatus()).isEqualTo(RecheckStatusEnum.EXPIRED);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> captor =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper, times(3)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(org.example.trademodel.entity.TmPushRecheckLogDO::getRecheckTime)
                .containsExactly(
                        LocalDateTime.parse("2026-07-14T11:59:59"),
                        LocalDateTime.parse("2026-07-14T12:00:00"),
                        LocalDateTime.parse("2026-07-14T12:00:01"));
        assertThat(captor.getAllValues()).allSatisfy(log ->
                assertThat(log.getCreateTime()).isEqualTo(log.getRecheckTime()));
    }

    @Test
    void drifted() {
        TmPushSnapshotDO s = baseSnap();
        s.setTriggerPrice(new BigDecimal("100"));
        s.setConfusedScoreSnapshot(22);
        s.setDataQualityScoreSnapshot(72);
        when(pushSnapshotMapper.selectByPushId(3L)).thenReturn(s);
        RecheckResult r = scheduledRecheck(3L, new BigDecimal("110"));
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
        RecheckResult r = scheduledRecheck(4L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertSafeReviewOnlyResult(r);
        verify(pushSnapshotMapper).updatePushStatus(4L, "RECHECK_INVALIDATED");
    }

    @Test
    void invalidCurrentPrice_invalidated() {
        TmPushSnapshotDO s = baseSnap();
        when(pushSnapshotMapper.selectByPushId(41L)).thenReturn(s);

        RecheckResult r = scheduledRecheck(41L, BigDecimal.ZERO);

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

        RecheckResult r = scheduledRecheck(42L, null);

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

        RecheckResult r = scheduledRecheck(43L, null);

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

        RecheckResult r = scheduledRecheck(44L, null);

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

        RecheckResult r = scheduledRecheck(45L, null);

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
    void ownedPushOpenInfrastructureFailurePersistsRealErrorForExplicitRetry() {
        TmPushSnapshotDO snapshot = baseSnap();
        snapshot.setSymbol("BTCUSDT");
        snapshot.setTraceId("trace-error");
        when(pushSnapshotMapper.selectByPushId(451L)).thenReturn(snapshot);
        when(marketQuoteClient.fetch24hTicker("BTCUSDT"))
                .thenThrow(new RuntimeException("provider unavailable"));

        RecheckResult result = service.recheckForOwnedPushOpen(451L, null, 1);

        assertThat(result.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_WAITING);
        assertThat(result.isReviewPassed()).isFalse();
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getExecutionStatus()).isEqualTo("ERROR");
        assertThat(cap.getValue().getExecutionErrorCode()).isEqualTo("RECHECK_EXECUTION_FAILED");
        assertThat(cap.getValue().getTriggerSource()).isEqualTo("PUSH_OPEN");
        assertThat(cap.getValue().getRetryAttempt()).isEqualTo(1);
        assertThat(cap.getValue().getRecheckStatus()).isNull();
        assertThat(cap.getValue().getTraceId()).isEqualTo("trace-error");
        verify(pushSnapshotMapper, never()).updatePushStatus(any(), any());
    }

    @Test
    void ownedPushOpenBusinessInvalidationRemainsCompletedAndIsNotExecutionError() {
        when(pushSnapshotMapper.selectByPushId(452L)).thenReturn(null);

        RecheckResult result = service.recheckForOwnedPushOpen(452L, null, 1);

        assertThat(result.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getExecutionStatus()).isEqualTo("COMPLETED");
        assertThat(cap.getValue().getRecheckStatus()).isEqualTo("INVALIDATED");
        verify(pushSnapshotMapper).updatePushStatus(452L, "RECHECK_INVALIDATED");
    }

    @Test
    void missingCurrentPriceSnapshotSymbolMissingFailsClosedWithPriceRequired() {
        TmPushSnapshotDO s = baseSnap();
        s.setSymbol(null);
        when(pushSnapshotMapper.selectByPushId(46L)).thenReturn(s);

        RecheckResult r = scheduledRecheck(46L, null);

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

        RecheckResult r = scheduledRecheck(47L, new BigDecimal("100"));

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isTrue();
        assertSafeReviewOnlyResult(r);
        verifyNoInteractions(marketQuoteClient);
        verify(pushSnapshotMapper).updatePushStatus(47L, "RECHECK_REVIEW_PASSED");
    }

    @Test
    void pushRecheckUsesSharedDerivativesSnapshot() {
        TmPushSnapshotDO snap = baseSnap();
        snap.setSymbol("BTCUSDT");
        snap.setInvalidationConditionJson("{\"derivativesRequired\":true}");
        snap.setConfusedScoreSnapshot(10);
        when(pushSnapshotMapper.selectByPushId(470L)).thenReturn(snap);
        when(derivativesSnapshotReadPort.readCached(any(), any(), any(), any()))
                .thenReturn(new ProviderCallResult<>(derivatives(UnifiedSourceStatus.READY,
                        SnapshotFreshnessStatus.FRESH, "COMPLETE"), null, null));

        RecheckResult result = scheduledRecheck(470L, new BigDecimal("100"));

        assertThat(result.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        verify(derivativesSnapshotReadPort).readCached(any(), any(), any(), any());
    }

    @Test
    void pushRecheckStaleDerivativesFailsClosed() {
        TmPushSnapshotDO snap = baseSnap();
        snap.setSymbol("BTCUSDT");
        snap.setInvalidationConditionJson("{\"derivativesRequired\":true}");
        when(pushSnapshotMapper.selectByPushId(471L)).thenReturn(snap);
        when(derivativesSnapshotReadPort.readCached(any(), any(), any(), any()))
                .thenReturn(new ProviderCallResult<>(derivatives(UnifiedSourceStatus.STALE,
                        SnapshotFreshnessStatus.STALE_READABLE, "COMPLETE"), null, null));

        RecheckResult result = scheduledRecheck(471L, new BigDecimal("100"));

        assertThat(result.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertThat(result.isReviewPassed()).isFalse();
        ArgumentCaptor<org.example.trademodel.entity.TmPushRecheckLogDO> cap =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushRecheckLogDO.class);
        verify(pushRecheckLogMapper).insert(cap.capture());
        assertThat(cap.getValue().getExecutionErrorCode()).isEqualTo("DERIVATIVES_STALE");
        assertThat(cap.getValue().getFailReasonJson()).contains("DERIVATIVES_STALE");
    }

    private static DerivativesRiskSnapshot derivatives(UnifiedSourceStatus status,
                                                       SnapshotFreshnessStatus freshness,
                                                       String availability) {
        Instant now = Instant.now();
        List<String> datasets = List.of(
                ProviderDatasetType.COINGLASS_OPEN_INTEREST.name(),
                ProviderDatasetType.COINGLASS_FUNDING.name(),
                ProviderDatasetType.COINGLASS_LIQUIDATION.name(),
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO.name());
        return new DerivativesRiskSnapshot("BTCUSDT", "COINGLASS_V4", now, now, now.plusSeconds(60),
                new BigDecimal("100000000"), null, new BigDecimal("0.05"), new BigDecimal("0.05"), null,
                new BigDecimal("0.0001"), null, BigDecimal.ONE, "GLOBAL_ACCOUNT",
                null, new BigDecimal("1000"), null, null,
                null, new BigDecimal("1000"), null, null,
                null, new BigDecimal("0.20"), datasets, List.of(), List.of(), status, freshness,
                availability, List.of(), "trace-push-derivatives", Map.of(), null);
    }

    @Test
    void reviewWaiting_highConfused() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(75);
        s.setDataQualityScoreSnapshot(65);
        when(pushSnapshotMapper.selectByPushId(5L)).thenReturn(s);
        RecheckResult r = scheduledRecheck(5L, new BigDecimal("100"));
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

        RecheckResult r = scheduledRecheck(52L, new BigDecimal("100"));

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

        RecheckResult r = scheduledRecheck(51L, new BigDecimal("100"));
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
        RecheckResult r = scheduledRecheck(6L, new BigDecimal("100"));
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

        RecheckResult r = scheduledRecheck(6L, new BigDecimal("100"));
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

        RecheckResult r = scheduledRecheck(7L, new BigDecimal("100"));
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

        RecheckResult r = scheduledRecheck(8L, new BigDecimal("100"));
        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertSafeReviewOnlyResult(r);
    }

    @Test
    void pushRecheckConsumesAllowedUserPositionRiskResultReadOnly() {
        TmPushSnapshotDO s = baseSnap();
        s.setConfusedScoreSnapshot(10);
        s.setDataQualityScoreSnapshot(88);
        when(pushSnapshotMapper.selectByPushId(81L)).thenReturn(s);
        when(userPositionRiskAdapter.currentRiskForSystem()).thenReturn(UserPositionRiskResult.noOpenPosition(0));

        RecheckResult r = scheduledRecheck(81L, new BigDecimal("100"));

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertThat(r.isValid()).isFalse();
        assertThat(r.isReviewPassed()).isTrue();
        assertSafeReviewOnlyResult(r);
        verify(userPositionRiskAdapter).currentRiskForSystem();

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
        when(userPositionRiskAdapter.currentRiskForSystem()).thenReturn(blocked);

        RecheckResult r = scheduledRecheck(82L, new BigDecimal("100"));

        assertThat(r.getRecheckStatus()).isEqualTo(RecheckStatusEnum.RISK_BLOCKED);
        assertThat(r.isValid()).isFalse();
        assertSafeReviewOnlyResult(r);
        verify(userPositionRiskAdapter).currentRiskForSystem();

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
    void replayByInstructionFailsClosedBeforeHistoricalOrMutationAccess() {
        assertThatThrownBy(() -> service.replayByDispatch(null, "SCH-B2-PUSH-10"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("MUTATE_REPLAY_DISABLED");

        verifyNoInteractions(pushSnapshotMapper, pushRecheckLogMapper);
    }

    @Test
    void summarizeReplayByInstructionFailsClosedBeforeGlobalRead() {
        assertThatThrownBy(() -> service.summarizeReplayByDispatch(null, "SCH-B3-PUSH-11"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("READ_REPLAY_SUMMARY_DISABLED");

        verifyNoInteractions(pushRecheckLogMapper);
    }

    @Test
    void rawLogServiceReadsFailClosedWithoutAuthoritativeOwnerRelation() {
        assertThatThrownBy(() -> service.getLatestLog(12L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("READ_LATEST_DISABLED");
        assertThatThrownBy(() -> service.listLogs(12L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("READ_LOGS_DISABLED");
        verifyNoInteractions(pushRecheckLogMapper);
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
    void summarizeReplayByDispatchDoesNotExposeEmptyGlobalShell() {
        assertThatThrownBy(() -> service.summarizeReplayByDispatch("SCH-EMPTY", null))
                .isInstanceOf(SecurityException.class)
                .hasMessage("READ_REPLAY_SUMMARY_DISABLED");

        verifyNoInteractions(pushRecheckLogMapper);
    }

    @Test
    void getOpsOverviewDoesNotExposeGlobalAuthenticatedData() {
        assertThatThrownBy(() -> service.getOpsOverview(null, null, null, null))
                .isInstanceOf(SecurityException.class)
                .hasMessage("READ_OPS_DISABLED");

        verifyNoInteractions(dispatchConfigService, pushRecheckLogMapper);
    }

    @Test
    void getOpsOverviewExplicitDispatchParametersCannotBypassBoundary() {
        assertThatThrownBy(() -> service.getOpsOverview("SCH-B10", null, 8, 7))
                .isInstanceOf(SecurityException.class)
                .hasMessage("READ_OPS_DISABLED");

        verifyNoInteractions(dispatchConfigService, pushRecheckLogMapper);
    }

    @Test
    void getOpsOverviewDoesNotReturnMisleadingEmptyShells() {
        assertThatThrownBy(() -> service.getOpsOverview(null, null, 5, 10))
                .isInstanceOf(SecurityException.class)
                .hasMessage("READ_OPS_DISABLED");

        verifyNoInteractions(dispatchConfigService, pushRecheckLogMapper);
    }

    private RecheckResult scheduledRecheck(Long pushId, BigDecimal currentPrice) {
        return service.recheck(
                pushId,
                currentPrice,
                RecheckExecutionCommand.scheduled(
                        "TEST-BATCH-" + pushId,
                        "TEST-INSTRUCTION-" + pushId,
                        1,
                        3,
                        0));
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
