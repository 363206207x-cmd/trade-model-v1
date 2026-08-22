package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspacePushRecheckServiceTest {
    @Mock MessageMapper messageMapper;
    @Mock PushSnapshotMapper snapshotMapper;
    @Mock PushRecheckLogMapper logMapper;
    @Mock ExecutionPlanMapper executionPlanMapper;
    @Mock PushRecheckCoreTransactionService coreTransactionService;
    @Mock AnalysisRunOrchestrator analysisRunOrchestrator;

    private WorkspacePushRecheckService service;

    @BeforeEach
    void setUp() {
        service = new WorkspacePushRecheckService(messageMapper, snapshotMapper, logMapper,
                executionPlanMapper, coreTransactionService, analysisRunOrchestrator);
    }

    @Test
    void firstOpenCreatesOnePushOpenAndKeepsAllIdsDistinct() {
        stubOwned();
        TmPushRecheckLogDO created = log(701L, "COMPLETED");
        when(coreTransactionService.execute(7L, "message-1", 99L, null, 1))
                .thenReturn(attempt(created));

        WorkspacePushRecheckService.Projection result = service.open(7L, "message-1", "push-snapshot-99");

        verify(coreTransactionService).execute(7L, "message-1", 99L, null, 1);
        assertThat(result.messageId()).isEqualTo("message-1");
        assertThat(result.pushSnapshotId()).isEqualTo("push-snapshot-99");
        assertThat(result.pushId()).isEqualTo(99L);
        assertThat(result.recheckId()).isEqualTo(701L);
        assertThat(result.analysisId()).isEqualTo("analysis-1");
        assertThat(result.planId()).isEqualTo("plan-1");
    }

    @Test
    void reloadAndReadOnlyGetNeverCreateOpen() {
        stubOwned();
        when(logMapper.selectLatestByPushIdAndTriggerSource(99L, "PUSH_OPEN"))
                .thenReturn(log(701L, "COMPLETED"));

        service.read(7L, "message-1", "push-snapshot-99");

        verify(coreTransactionService, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(messageMapper, never()).updateCurrentRecheckIdForUser(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void completedOldOpenDoesNotPermanentlyBlockNewExplicitMessageOpen() {
        stubOwned();
        when(logMapper.countByPushIdAndTriggerSource(99L, "PUSH_OPEN")).thenReturn(1);
        when(coreTransactionService.execute(7L, "message-1", 99L, null, 2))
                .thenReturn(attempt(log(702L, "COMPLETED")));

        WorkspacePushRecheckService.Projection result = service.open(
                7L, "message-1", "push-snapshot-99");

        verify(coreTransactionService).execute(7L, "message-1", 99L, null, 2);
        assertThat(result.recheckId()).isEqualTo(702L);
    }

    @Test
    void errorRetryCreatesNewAttemptForSameTarget() {
        stubOwned();
        TmPushRecheckLogDO failed = log(701L, "ERROR");
        TmPushRecheckLogDO retried = log(702L, "COMPLETED");
        when(logMapper.selectLatestByPushIdAndTriggerSource(99L, "PUSH_OPEN"))
                .thenReturn(failed);
        when(logMapper.countByPushIdAndTriggerSource(99L, "PUSH_OPEN")).thenReturn(1);
        when(coreTransactionService.execute(7L, "message-1", 99L, 701L, 2))
                .thenReturn(attempt(retried));

        WorkspacePushRecheckService.Projection result = service.retry(7L, "message-1", "push-snapshot-99");

        verify(coreTransactionService).execute(7L, "message-1", 99L, 701L, 2);
        assertThat(result.recheckId()).isEqualTo(702L);
    }

    @Test
    void completedBusinessResultCannotUseInfrastructureRetry() {
        stubOwned();
        when(logMapper.selectLatestByPushIdAndTriggerSource(99L, "PUSH_OPEN"))
                .thenReturn(log(701L, "COMPLETED"));

        assertThatThrownBy(() -> service.retry(7L, "message-1", "push-snapshot-99"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only an ERROR");
        verify(coreTransactionService, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void reanalysisUsesIndependentAnalysisRunAndNeverRetriesRecheck() {
        stubOwned();

        service.reanalyze(7L, "message-1", "push-snapshot-99");

        verify(analysisRunOrchestrator).run(org.mockito.ArgumentMatchers.any(AnalysisRunCommand.class));
        verify(coreTransactionService, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void crossUserOrIdentityMismatchFailsAsNotFoundWithoutEngineInvocation() {
        when(messageMapper.selectByIdForUser("message-1", 8L)).thenReturn(null);

        assertThatThrownBy(() -> service.open(8L, "message-1", "push-snapshot-99"))
                .isInstanceOf(WorkspacePushRecheckService.WorkspaceRecheckNotFoundException.class);
        verify(coreTransactionService, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void mapperResultWithMismatchedOwnerStillFailsClosed() {
        MessageDO message = new MessageDO();
        message.setMessageId("message-1");
        message.setUserId(9L);
        message.setSourceType("PUSH_SNAPSHOT");
        message.setSourceId("99");
        message.setCurrentRecheckId(null);
        when(messageMapper.selectByIdForUser("message-1", 7L)).thenReturn(message);
        lenient().when(messageMapper.updateCurrentRecheckIdForUser(
                org.mockito.Mockito.eq("message-1"), org.mockito.Mockito.eq(7L),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);

        assertThatThrownBy(() -> service.open(7L, "message-1", "push-snapshot-99"))
                .isInstanceOf(WorkspacePushRecheckService.WorkspaceRecheckNotFoundException.class);
        verify(coreTransactionService, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void legacyCurrentRecheckAliasIsIgnoredAndSnapshotResolvesFromOwnedMessageSource() {
        stubOwned();
        MessageDO message = messageMapper.selectByIdForUser("message-1", 7L);
        message.setCurrentRecheckId("99");
        when(logMapper.selectLatestByPushIdAndTriggerSource(99L, "PUSH_OPEN"))
                .thenReturn(log(701L, "COMPLETED"));

        WorkspacePushRecheckService.Projection result = service.read(
                7L, "message-1", "push-snapshot-99");

        assertThat(result.pushSnapshotId()).isEqualTo("push-snapshot-99");
        assertThat(result.recheckId()).isEqualTo(701L);
    }

    @Test
    void arbitrarySnapshotIdentityFailsClosed() {
        stubOwned();

        assertThatThrownBy(() -> service.read(7L, "message-1", "push-snapshot-100"))
                .isInstanceOf(WorkspacePushRecheckService.WorkspaceRecheckNotFoundException.class);
        verify(coreTransactionService, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void concurrentDoubleOpenReusesSingleInProcessAttempt() throws Exception {
        stubOwned();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return attempt(log(701L, "COMPLETED"));
        }).when(coreTransactionService).execute(7L, "message-1", 99L, null, 1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<WorkspacePushRecheckService.Projection> first = executor.submit(
                    () -> service.open(7L, "message-1", "push-snapshot-99"));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            Future<WorkspacePushRecheckService.Projection> second = executor.submit(
                    () -> service.open(7L, "message-1", "push-snapshot-99"));
            Thread.sleep(100);
            release.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS).recheckId()).isEqualTo(701L);
            assertThat(second.get(5, TimeUnit.SECONDS).recheckId()).isEqualTo(701L);
            verify(coreTransactionService, times(1)).execute(7L, "message-1", 99L, null, 1);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private void stubOwned() {
        MessageDO message = new MessageDO();
        message.setMessageId("message-1");
        message.setUserId(7L);
        message.setSourceType("PUSH_SNAPSHOT");
        message.setSourceId("99");
        message.setCurrentRecheckId(null);
        message.setAnalysisId("analysis-1");
        message.setPlanId("plan-1");
        message.setTraceId("trace-1");
        when(messageMapper.selectByIdForUser("message-1", 7L)).thenReturn(message);
        lenient().when(messageMapper.updateCurrentRecheckIdForUser(
                        org.mockito.Mockito.eq("message-1"),
                        org.mockito.Mockito.eq(7L),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);

        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setPushId(99L);
        snapshot.setAnalysisId("analysis-1");
        snapshot.setTraceId("trace-1");
        snapshot.setSymbol("BTCUSDT");
        snapshot.setTimeframe("15m");
        snapshot.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        lenient().when(snapshotMapper.selectByPushId(99L)).thenReturn(snapshot);

        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-1");
        plan.setAnalysisId("analysis-1");
        lenient().when(executionPlanMapper.selectByPlanId("plan-1")).thenReturn(plan);
    }

    private static TmPushRecheckLogDO log(Long id, String executionStatus) {
        TmPushRecheckLogDO value = new TmPushRecheckLogDO();
        value.setLogId(id);
        value.setPushId(99L);
        value.setTriggerSource("PUSH_OPEN");
        value.setExecutionStatus(executionStatus);
        value.setRecheckStatus("REVIEW_PASSED");
        value.setTraceId("trace-recheck");
        return value;
    }

    private static PushRecheckCoreTransactionService.AttemptResult attempt(TmPushRecheckLogDO log) {
        return new PushRecheckCoreTransactionService.AttemptResult(new RecheckResult(), log,
                "COMPLETED".equalsIgnoreCase(log.getExecutionStatus()));
    }
}
