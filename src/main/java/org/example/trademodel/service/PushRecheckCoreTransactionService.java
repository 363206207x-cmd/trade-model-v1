package org.example.trademodel.service;

import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

/** Coordinates only the three owner writes that form one PUSH_OPEN result. */
@Service
public class PushRecheckCoreTransactionService {
    private static final String PUSH_OPEN = "PUSH_OPEN";

    private final PushRecheckService pushRecheckService;
    private final PushRecheckLogMapper recheckLogMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final MessageMapper messageMapper;
    private final TransactionTemplate coreTransaction;
    private final TransactionTemplate errorTransaction;
    private Clock clock = Clock.systemUTC();

    public PushRecheckCoreTransactionService(PushRecheckService pushRecheckService,
                                             PushRecheckLogMapper recheckLogMapper,
                                             PushSnapshotMapper pushSnapshotMapper,
                                             MessageMapper messageMapper,
                                             PlatformTransactionManager transactionManager) {
        this.pushRecheckService = pushRecheckService;
        this.recheckLogMapper = recheckLogMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.messageMapper = messageMapper;
        this.coreTransaction = new TransactionTemplate(transactionManager);
        this.errorTransaction = new TransactionTemplate(transactionManager);
        this.errorTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public AttemptResult execute(Long userId, String messageId, Long pushId,
                                 Long retryFromLogId, int retryAttempt) {
        try {
            AttemptResult completed = coreTransaction.execute(status -> {
                TmPushRecheckLogDO previous = recheckLogMapper
                        .selectLatestByPushIdAndTriggerSource(pushId, PUSH_OPEN);
                RecheckResult result = pushRecheckService.recheck(
                        pushId, null, RecheckExecutionCommand.pushOpen(retryAttempt, retryFromLogId));
                TmPushRecheckLogDO log = recheckLogMapper.selectLatestByPushIdAndTriggerSource(pushId, PUSH_OPEN);
                if (log == null || log.getLogId() == null
                        || !"COMPLETED".equalsIgnoreCase(log.getExecutionStatus())
                        || previous != null && previous.getLogId() != null
                        && previous.getLogId().equals(log.getLogId())) {
                    throw new IllegalStateException("completed push recheck result is missing");
                }
                if (messageMapper.updateCurrentRecheckIdForUser(messageId, userId,
                        String.valueOf(log.getLogId()), UtcLocalTimePolicy.now(clock)) != 1) {
                    throw new IllegalStateException("recheck message lineage update failed");
                }
                return new AttemptResult(result, log, true);
            });
            if (completed == null) throw new IllegalStateException("push recheck transaction returned no result");
            return completed;
        } catch (RuntimeException failure) {
            return new AttemptResult(errorResult(pushId), persistError(pushId, retryFromLogId, retryAttempt, failure), false);
        }
    }

    private TmPushRecheckLogDO persistError(Long pushId, Long retryFromLogId, int retryAttempt,
                                            RuntimeException failure) {
        try {
            TmPushRecheckLogDO error = errorTransaction.execute(status -> {
                LocalDateTime now = UtcLocalTimePolicy.now(clock);
                TmPushSnapshotDO snapshot = pushId == null ? null : pushSnapshotMapper.selectByPushId(pushId);
                TmPushRecheckLogDO row = new TmPushRecheckLogDO();
                row.setPushId(pushId);
                row.setTriggerSource(PUSH_OPEN);
                row.setRetryAttempt(Math.max(1, retryAttempt));
                row.setReplayFromLogId(retryFromLogId);
                row.setExecutionStatus("ERROR");
                row.setExecutionErrorCode(executionErrorCode(failure));
                row.setExecutionErrorMessage("复核执行失败，请由用户明确重试");
                row.setRecheckTime(now);
                row.setFailReasonJson("{\"code\":\"" + row.getExecutionErrorCode()
                        + "\",\"detail\":\"PUSH_OPEN execution failed\"}");
                row.setTraceId(snapshot == null ? null : snapshot.getTraceId());
                row.setCreateTime(now);
                if (recheckLogMapper.insert(row) != 1 || row.getLogId() == null) {
                    throw new IllegalStateException("push recheck error persistence failed");
                }
                return row;
            });
            if (error == null) throw new IllegalStateException("push recheck error transaction returned no result");
            return error;
        } catch (RuntimeException persistenceFailure) {
            persistenceFailure.addSuppressed(failure);
            throw persistenceFailure;
        }
    }

    private static RecheckResult errorResult(Long pushId) {
        RecheckResult result = new RecheckResult();
        result.setPushId(pushId);
        result.setRecheckStatus(org.example.trademodel.enums.RecheckStatusEnum.REVIEW_WAITING);
        result.setValid(false);
        result.setReviewPassed(false);
        result.setMessage("复核执行失败，请由用户明确重试");
        return result;
    }

    private static String executionErrorCode(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String name = current.getClass().getSimpleName().toUpperCase();
            if (name.contains("TIMEOUT") || name.contains("TIMEDOUT")) return "RECHECK_TIMEOUT";
            current = current.getCause();
        }
        return "RECHECK_EXECUTION_FAILED";
    }

    void setClock(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public record AttemptResult(RecheckResult result, TmPushRecheckLogDO log, boolean completed) { }
}
