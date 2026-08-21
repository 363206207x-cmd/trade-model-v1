package org.example.trademodel.service.impl;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.PushRecheckCoreTransactionService;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.RecheckExecutionCommand;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.service.support.RuleConfigContractService;
import org.example.trademodel.telegram.HighValueAlertMessageService;
import org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TradeModelApplication.class)
@Tag("core-regression")
class PushRecheckCoreTransactionIntegrationTest {
    private static final Long USER_ID = 9111L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-22T10:00:00Z"), ZoneOffset.UTC);

    @Autowired private PushSnapshotMapper pushSnapshotMapper;
    @Autowired private PushRecheckLogMapper recheckLogMapper;
    @Autowired private MessageMapper messageMapper;
    @Autowired private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanContractRows() {
        jdbcTemplate.update("DELETE FROM tm_message WHERE message_id LIKE 'b111-%'");
        jdbcTemplate.update("DELETE FROM tm_push_recheck_log WHERE push_id IN "
                + "(SELECT push_id FROM tm_push_snapshot WHERE analysis_id LIKE 'b111-%')");
        jdbcTemplate.update("DELETE FROM tm_push_snapshot WHERE analysis_id LIKE 'b111-%'");
    }

    @Test
    void successCommitsExactlyOneCompletedSnapshotAndRealMessageLineage() {
        Fixture fixture = fixture("success", false);

        PushRecheckCoreTransactionService.AttemptResult attempt = fixture.coordinator().execute(
                USER_ID, fixture.messageId(), fixture.pushId(), null, 1);

        assertThat(attempt.completed()).isTrue();
        assertSingleCompleted(fixture.pushId(), "REVIEW_PASSED");
        assertThat(pushSnapshotMapper.selectByPushId(fixture.pushId()).getPushStatus())
                .isEqualTo("RECHECK_REVIEW_PASSED");
        assertThat(messageMapper.selectByIdForUser(fixture.messageId(), USER_ID).getCurrentRecheckId())
                .isEqualTo(String.valueOf(attempt.log().getLogId()));
    }

    @Test
    void snapshotUpdateFailureRollsBackCompletedThenPersistsOneIndependentError() {
        PushSnapshotMapper failingSnapshotMapper = updateFailureMapper();
        Fixture fixture = fixture("snapshot-failure", false, failingSnapshotMapper, null);

        PushRecheckCoreTransactionService.AttemptResult attempt = fixture.coordinator().execute(
                USER_ID, fixture.messageId(), fixture.pushId(), null, 1);

        assertThat(attempt.completed()).isFalse();
        assertSingleErrorAndNoCompleted(fixture.pushId());
        assertThat(pushSnapshotMapper.selectByPushId(fixture.pushId()).getPushStatus()).isEqualTo("CAPTURED");
        assertThat(messageMapper.selectByIdForUser(fixture.messageId(), USER_ID).getCurrentRecheckId()).isNull();
    }

    @Test
    void messageBindingFailureRollsBackCompletedAndSnapshotThenPersistsOneError() {
        Fixture fixture = fixture("message-failure", false);

        PushRecheckCoreTransactionService.AttemptResult attempt = fixture.coordinator().execute(
                USER_ID + 1, fixture.messageId(), fixture.pushId(), null, 1);

        assertThat(attempt.completed()).isFalse();
        assertSingleErrorAndNoCompleted(fixture.pushId());
        assertThat(pushSnapshotMapper.selectByPushId(fixture.pushId()).getPushStatus()).isEqualTo("CAPTURED");
        assertThat(messageMapper.selectByIdForUser(fixture.messageId(), USER_ID).getCurrentRecheckId()).isNull();
    }

    @Test
    void providerFailureBeforeCompletedPersistsOnlyOneError() {
        MarketQuoteClient provider = mock(MarketQuoteClient.class);
        when(provider.fetch24hTicker("BTCUSDT")).thenThrow(new RuntimeException("provider unavailable"));
        Fixture fixture = fixture("provider-failure", false, pushSnapshotMapper, provider);

        PushRecheckCoreTransactionService.AttemptResult attempt = fixture.coordinator().execute(
                USER_ID, fixture.messageId(), fixture.pushId(), null, 1);

        assertThat(attempt.completed()).isFalse();
        assertSingleErrorAndNoCompleted(fixture.pushId());
        assertThat(pushSnapshotMapper.selectByPushId(fixture.pushId()).getPushStatus()).isEqualTo("CAPTURED");
    }

    @Test
    void businessInvalidationIsCompletedAndNeverTurnsIntoErrorOrRetry() {
        Fixture fixture = fixture("invalidated", true);

        PushRecheckCoreTransactionService.AttemptResult attempt = fixture.coordinator().execute(
                USER_ID, fixture.messageId(), fixture.pushId(), null, 1);

        assertThat(attempt.completed()).isTrue();
        assertSingleCompleted(fixture.pushId(), "INVALIDATED");
        assertThat(attempt.log().getRetryAttempt()).isEqualTo(1);
        assertThat(pushSnapshotMapper.selectByPushId(fixture.pushId()).getPushStatus())
                .isEqualTo("RECHECK_INVALIDATED");
    }

    @Test
    void safetyMessageRunsAfterCoreLineageWriteAndSuccessDoesNotChangeCoreResult() {
        AtomicBoolean sawCommittedLineage = new AtomicBoolean();
        HighValueAlertMessageService alerts = mock(HighValueAlertMessageService.class);
        Fixture fixture = fixture("safety-success", true, pushSnapshotMapper, null, alerts);
        when(alerts.recordSafetyChange(any())).thenAnswer(invocation -> {
            String recheckId = messageMapper.selectByIdForUser(fixture.messageId(), USER_ID)
                    .getCurrentRecheckId();
            sawCommittedLineage.set(recheckId != null);
            return new MessageDO();
        });

        PushRecheckCoreTransactionService.AttemptResult attempt = fixture.coordinator().execute(
                USER_ID, fixture.messageId(), fixture.pushId(), null, 1);

        assertThat(attempt.completed()).isTrue();
        assertThat(sawCommittedLineage).isTrue();
        assertSingleCompleted(fixture.pushId(), "INVALIDATED");
        assertThat(attempt.log().getExecutionErrorCode()).isEqualTo("INVALIDATED");
    }

    @Test
    void safetyMessageFailureKeepsCompletedCoreAndPersistsStructuredPartialMarker() {
        HighValueAlertMessageService alerts = mock(HighValueAlertMessageService.class);
        when(alerts.recordSafetyChange(any())).thenThrow(new RuntimeException("message sink unavailable"));
        Fixture fixture = fixture("safety-failure", true, pushSnapshotMapper, null, alerts);

        PushRecheckCoreTransactionService.AttemptResult attempt = fixture.coordinator().execute(
                USER_ID, fixture.messageId(), fixture.pushId(), null, 1);

        List<TmPushRecheckLogDO> logs = recheckLogMapper.selectByPushId(fixture.pushId());
        assertThat(attempt.completed()).isTrue();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getExecutionStatus()).isEqualTo("COMPLETED");
        assertThat(logs.get(0).getExecutionErrorCode()).isEqualTo("SAFETY_MESSAGE_FAILED");
        assertThat(pushSnapshotMapper.selectByPushId(fixture.pushId()).getPushStatus())
                .isEqualTo("RECHECK_INVALIDATED");
        assertThat(messageMapper.selectByIdForUser(fixture.messageId(), USER_ID).getCurrentRecheckId())
                .isEqualTo(String.valueOf(logs.get(0).getLogId()));
    }

    @Test
    void errorRetryCreatesNewAttemptForSameSnapshotWithoutAnalysisCreation() {
        MarketQuoteClient failingProvider = mock(MarketQuoteClient.class);
        when(failingProvider.fetch24hTicker("BTCUSDT")).thenThrow(new RuntimeException("provider unavailable"));
        Fixture failed = fixture("retry", false, pushSnapshotMapper, failingProvider);
        PushRecheckCoreTransactionService.AttemptResult first = failed.coordinator().execute(
                USER_ID, failed.messageId(), failed.pushId(), null, 1);

        Fixture retry = serviceFixture(failed.messageId(), failed.pushId(), false,
                pushSnapshotMapper, successfulProvider(), mock(HighValueAlertMessageService.class));
        PushRecheckCoreTransactionService.AttemptResult second = retry.coordinator().execute(
                USER_ID, failed.messageId(), failed.pushId(), first.log().getLogId(), 2);

        assertThat(first.completed()).isFalse();
        assertThat(second.completed()).isTrue();
        assertThat(recheckLogMapper.selectByPushId(failed.pushId())).hasSize(2)
                .extracting(TmPushRecheckLogDO::getExecutionStatus)
                .containsExactly("COMPLETED", "ERROR");
        assertThat(second.log().getReplayFromLogId()).isEqualTo(first.log().getLogId());
        assertThat(second.log().getRetryAttempt()).isEqualTo(2);
    }

    private Fixture fixture(String suffix, boolean invalidated) {
        return fixture(suffix, invalidated, pushSnapshotMapper, null);
    }

    private Fixture fixture(String suffix, boolean invalidated,
                            PushSnapshotMapper snapshotMapper, MarketQuoteClient provider) {
        return fixture(suffix, invalidated, snapshotMapper, provider,
                mock(HighValueAlertMessageService.class));
    }

    private Fixture fixture(String suffix, boolean invalidated,
                            PushSnapshotMapper snapshotMapper, MarketQuoteClient provider,
                            HighValueAlertMessageService alerts) {
        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setAnalysisId("b111-" + suffix);
        snapshot.setSymbol("BTCUSDT");
        snapshot.setTimeframe("15m");
        snapshot.setPushType("CONTRACT_TEST");
        snapshot.setPushStatus("CAPTURED");
        snapshot.setPushCreateTime(LocalDateTime.of(2026, 8, 22, 9, 50));
        snapshot.setTriggerPrice(new BigDecimal("100"));
        snapshot.setInvalidationConditionJson(invalidated
                ? "{\"invalidPriceBelow\":105,\"text\":\"contract invalidated\"}" : null);
        snapshot.setExecutionFeasibilitySnapshot(90);
        snapshot.setDataQualityScoreSnapshot(90);
        snapshot.setConfusedScoreSnapshot(10);
        snapshot.setExpiresAt(LocalDateTime.of(2026, 8, 22, 11, 0));
        snapshot.setTraceId("trace-b111-" + suffix);
        snapshot.setCreateTime(LocalDateTime.of(2026, 8, 22, 9, 50));
        pushSnapshotMapper.insert(snapshot);

        String messageId = "b111-" + suffix;
        MessageDO message = new MessageDO();
        message.setMessageId(messageId);
        message.setUserId(USER_ID);
        message.setCategory("OPPORTUNITY_PLAN_SAFETY_CHANGE");
        message.setSourceType("PUSH_SNAPSHOT");
        message.setSourceId(String.valueOf(snapshot.getPushId()));
        message.setAnalysisId(snapshot.getAnalysisId());
        message.setSymbol(snapshot.getSymbol());
        message.setTitle("Contract transaction test");
        message.setBody("Not a trade instruction");
        message.setBusinessState("ACTIVE");
        message.setReadState("UNREAD");
        message.setDedupeKey("b111|" + suffix);
        message.setCurrentRecheckId(null);
        message.setTraceId(snapshot.getTraceId());
        message.setCreatedAt(LocalDateTime.of(2026, 8, 22, 9, 50));
        message.setUpdatedAt(LocalDateTime.of(2026, 8, 22, 9, 50));
        message.setNotTradeInstruction(true);
        message.setNotOrderExecution(true);
        messageMapper.insert(message);
        return serviceFixture(messageId, snapshot.getPushId(), invalidated,
                snapshotMapper, provider == null ? successfulProvider() : provider, alerts);
    }

    private Fixture serviceFixture(String messageId, Long pushId, boolean invalidated,
                                   PushSnapshotMapper snapshotMapper, MarketQuoteClient provider,
                                   HighValueAlertMessageService alerts) {
        PushRecheckDispatchConfigService config = mock(PushRecheckDispatchConfigService.class);
        UserPositionRiskAdapter risk = mock(UserPositionRiskAdapter.class);
        RuleConfigContractService rules = mock(RuleConfigContractService.class);
        AnalysisRunMapper analysisRuns = mock(AnalysisRunMapper.class);
        ExecutionPlanMapper plans = mock(ExecutionPlanMapper.class);
        when(risk.currentRiskForSystem()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        when(rules.requirePushRecheckThresholds()).thenReturn(
                new RuleConfigContractService.PushRecheckThresholds(
                        new BigDecimal("0.02"), 70, 85, 60));
        AnalysisRunDO analysis = new AnalysisRunDO();
        analysis.setAnalysisId("b111-" + (invalidated ? "invalidated" : "success"));
        analysis.setOwnerType("USER");
        analysis.setOwnerId(USER_ID);
        when(analysisRuns.selectById(any())).thenAnswer(invocation -> {
            analysis.setAnalysisId(invocation.getArgument(0));
            return analysis;
        });
        PushRecheckServiceImpl service = new PushRecheckServiceImpl(
                snapshotMapper, accountRiskSnapshotMapper, recheckLogMapper, config, risk,
                MarketPriceSnapshotTestSupport.snapshotService(provider), rules);
        service.setHighValueAlertDependencies(alerts, analysisRuns, plans);
        service.setTransactionManager(transactionManager);
        service.setClock(CLOCK);
        PushRecheckCoreTransactionService coordinator = new PushRecheckCoreTransactionService(
                service, recheckLogMapper, snapshotMapper, messageMapper, transactionManager);
        return new Fixture(messageId, pushId, coordinator);
    }

    private static MarketQuoteClient successfulProvider() {
        MarketQuoteClient provider = mock(MarketQuoteClient.class);
        MarketQuoteSnapshot quote = new MarketQuoteSnapshot();
        quote.setLastPrice(new BigDecimal("100"));
        when(provider.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(quote));
        return provider;
    }

    private PushSnapshotMapper updateFailureMapper() {
        return (PushSnapshotMapper) Proxy.newProxyInstance(
                PushSnapshotMapper.class.getClassLoader(),
                new Class<?>[]{PushSnapshotMapper.class},
                (proxy, method, args) -> {
                    if ("updatePushStatus".equals(method.getName())) return 0;
                    try {
                        return method.invoke(pushSnapshotMapper, args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }

    private void assertSingleCompleted(Long pushId, String recheckStatus) {
        List<TmPushRecheckLogDO> logs = recheckLogMapper.selectByPushId(pushId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getExecutionStatus()).isEqualTo("COMPLETED");
        assertThat(logs.get(0).getRecheckStatus()).isEqualTo(recheckStatus);
    }

    private void assertSingleErrorAndNoCompleted(Long pushId) {
        List<TmPushRecheckLogDO> logs = recheckLogMapper.selectByPushId(pushId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getExecutionStatus()).isEqualTo("ERROR");
        assertThat(logs.get(0).getRecheckStatus()).isNull();
    }

    private record Fixture(String messageId, Long pushId,
                           PushRecheckCoreTransactionService coordinator) { }
}
