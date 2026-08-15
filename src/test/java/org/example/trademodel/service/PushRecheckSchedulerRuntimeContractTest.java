package org.example.trademodel.service;

import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("core-regression")
class PushRecheckSchedulerRuntimeContractTest {

    @Test
    void scheduledQueryReceivesJavaComputedCutoff() {
        Fixture fixture = new Fixture();
        when(fixture.snapshotMapper.listPendingRecheckNext(
                any(), any(), any(), anyInt(), any(), any(), anyInt())).thenReturn(List.of());

        fixture.scheduler.recheckPendingPushesScheduled();

        LocalDateTime referenceAt = LocalDateTime.of(2026, 8, 14, 12, 0);
        verify(fixture.snapshotMapper).listPendingRecheckNext(
                "CAPTURED", "RECHECK_REVIEW_WAITING", "RECHECK_VALID_WAITING",
                3, referenceAt, referenceAt.minusMinutes(5), 50);
        assertThat(fixture.scheduler.getLastExecution().state()).isEqualTo("SUCCEEDED");
        assertThat(fixture.scheduler.getLastExecution().traceId()).startsWith("SCH-");
    }

    @Test
    void queryFailureIsRecordedAsFailedAndNeverMasqueradesAsSuccess() {
        Fixture fixture = new Fixture();
        when(fixture.snapshotMapper.listPendingRecheckNext(
                any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenThrow(new IllegalStateException("cutoff query failed"));

        fixture.scheduler.recheckPendingPushesScheduled();

        PushRecheckScheduler.SchedulerExecution execution = fixture.scheduler.getLastExecution();
        assertThat(execution.state()).isEqualTo("FAILED");
        assertThat(execution.errorClass()).isEqualTo(IllegalStateException.class.getName());
        assertThat(execution.errorMessage()).isEqualTo("cutoff query failed");
        assertThat(execution.traceId()).startsWith("SCH-");
        verify(fixture.recheckService, never()).recheck(any(), any(), any());
    }

    private static final class Fixture {
        private final PushSnapshotMapper snapshotMapper = mock(PushSnapshotMapper.class);
        private final PushRecheckLogMapper logMapper = mock(PushRecheckLogMapper.class);
        private final PushRecheckService recheckService = mock(PushRecheckService.class);
        private final PushRecheckDispatchConfigService configService = mock(PushRecheckDispatchConfigService.class);
        private final PushRecheckScheduler scheduler;

        private Fixture() {
            when(configService.loadOrInit(anyInt(), anyInt(), anyInt()))
                    .thenReturn(Map.of("limit", 50, "maxAttempts", 3, "minRetryMinutes", 5));
            scheduler = new PushRecheckScheduler(
                    snapshotMapper, logMapper, recheckService, configService,
                    50, 3, 5, true, true);
            scheduler.setClock(Clock.fixed(Instant.parse("2026-08-14T12:00:00Z"), ZoneOffset.UTC));
        }
    }
}
