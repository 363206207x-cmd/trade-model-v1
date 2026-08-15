package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class PushSnapshotCutoffMapperIntegrationTest {

    @Autowired
    private PushSnapshotMapper pushSnapshotMapper;

    @Autowired
    private PushRecheckLogMapper pushRecheckLogMapper;

    @Test
    void h2QueryUsesJavaComputedCutoffAndExcludesRecentRetry() {
        LocalDateTime referenceAt = LocalDateTime.of(2026, 8, 14, 12, 0);
        TmPushSnapshotDO eligible = snapshot("cutoff-eligible", referenceAt);
        TmPushSnapshotDO recent = snapshot("cutoff-recent", referenceAt);
        TmPushSnapshotDO firstAttempt = snapshot("cutoff-first", referenceAt);
        pushSnapshotMapper.insert(eligible);
        pushSnapshotMapper.insert(recent);
        pushSnapshotMapper.insert(firstAttempt);
        pushRecheckLogMapper.insert(recheck(eligible.getPushId(), referenceAt.minusMinutes(6)));
        pushRecheckLogMapper.insert(recheck(recent.getPushId(), referenceAt.minusMinutes(2)));

        List<TmPushSnapshotDO> selected = pushSnapshotMapper.listPendingRecheckNext(
                "CAPTURED", "RECHECK_REVIEW_WAITING", "RECHECK_VALID_WAITING",
                3, referenceAt, referenceAt.minusMinutes(5), 50);

        assertThat(selected).extracting(TmPushSnapshotDO::getAnalysisId)
                .contains("cutoff-eligible", "cutoff-first")
                .doesNotContain("cutoff-recent");
    }

    private static TmPushSnapshotDO snapshot(String analysisId, LocalDateTime referenceAt) {
        TmPushSnapshotDO row = new TmPushSnapshotDO();
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("5m");
        row.setPushType("CONTRACT_TEST");
        row.setPushStatus("CAPTURED");
        row.setPushCreateTime(referenceAt.minusMinutes(20));
        row.setExpiresAt(referenceAt.plusHours(1));
        row.setTraceId("trace-" + analysisId);
        row.setCreateTime(referenceAt.minusMinutes(20));
        return row;
    }

    private static TmPushRecheckLogDO recheck(Long pushId, LocalDateTime at) {
        TmPushRecheckLogDO row = new TmPushRecheckLogDO();
        row.setPushId(pushId);
        row.setDispatchBatchId("batch-cutoff");
        row.setDispatchInstructionId("instruction-" + pushId);
        row.setTriggerSource("SCHEDULED");
        row.setRetryAttempt(1);
        row.setMaxAttempts(3);
        row.setRetryBackoffMinutes(5);
        row.setExecutionStatus("SUCCEEDED");
        row.setRecheckTime(at);
        row.setRecheckStatus("RECHECK_REVIEW_WAITING");
        row.setTraceId("trace-recheck-" + pushId);
        row.setCreateTime(at);
        return row;
    }
}
