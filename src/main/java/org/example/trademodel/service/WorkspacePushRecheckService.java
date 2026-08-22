package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/** Owner-scoped workspace adapter. Raw push-id operations remain globally closed. */
@Service
public class WorkspacePushRecheckService {
    private static final String PUSH_OPEN = "PUSH_OPEN";
    private static final String SNAPSHOT_ID_PREFIX = "push-snapshot-";

    private final MessageMapper messageMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper recheckLogMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final PushRecheckCoreTransactionService coreTransactionService;
    private final AnalysisRunOrchestrator analysisRunOrchestrator;
    private final ConcurrentHashMap<OpenKey, CompletableFuture<Projection>> inFlightOpens = new ConcurrentHashMap<>();

    public WorkspacePushRecheckService(MessageMapper messageMapper,
                                       PushSnapshotMapper pushSnapshotMapper,
                                       PushRecheckLogMapper recheckLogMapper,
                                       ExecutionPlanMapper executionPlanMapper,
                                       PushRecheckCoreTransactionService coreTransactionService,
                                       AnalysisRunOrchestrator analysisRunOrchestrator) {
        this.messageMapper = messageMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.recheckLogMapper = recheckLogMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.coreTransactionService = coreTransactionService;
        this.analysisRunOrchestrator = analysisRunOrchestrator;
    }

    public Projection open(Long userId, String messageId, String pushSnapshotId) {
        OwnedTarget target = requireOwnedTarget(userId, messageId, pushSnapshotId);
        OpenKey key = new OpenKey(userId, target.pushId());
        CompletableFuture<Projection> created = new CompletableFuture<>();
        CompletableFuture<Projection> existing = inFlightOpens.putIfAbsent(key, created);
        if (existing != null) {
            try {
                return existing.join();
            } catch (CompletionException failure) {
                if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
                throw failure;
            }
        }
        try {
            int attempt = recheckLogMapper.countByPushIdAndTriggerSource(target.pushId(), PUSH_OPEN) + 1;
            PushRecheckCoreTransactionService.AttemptResult attemptResult = coreTransactionService.execute(
                    userId, target.message().getMessageId(), target.pushId(), null, attempt);
            Projection projection = projection(target, attemptResult.log());
            created.complete(projection);
            return projection;
        } catch (RuntimeException failure) {
            created.completeExceptionally(failure);
            throw failure;
        } finally {
            inFlightOpens.remove(key, created);
        }
    }

    public Projection read(Long userId, String messageId, String pushSnapshotId) {
        OwnedTarget target = requireOwnedTarget(userId, messageId, pushSnapshotId);
        return projection(target, latestOpen(target.pushId()));
    }

    public synchronized Projection retry(Long userId, String messageId, String pushSnapshotId) {
        OwnedTarget target = requireOwnedTarget(userId, messageId, pushSnapshotId);
        TmPushRecheckLogDO latest = latestOpen(target.pushId());
        if (latest == null || !"ERROR".equalsIgnoreCase(latest.getExecutionStatus())) {
            throw new IllegalStateException("only an ERROR PUSH_OPEN result can be retried");
        }
        int attempt = recheckLogMapper.countByPushIdAndTriggerSource(target.pushId(), PUSH_OPEN) + 1;
        PushRecheckCoreTransactionService.AttemptResult attemptResult = coreTransactionService.execute(
                userId, target.message().getMessageId(), target.pushId(), latest.getLogId(), attempt);
        return projection(target, attemptResult.log());
    }

    public AnalysisRunResult reanalyze(Long userId, String messageId, String pushSnapshotId) {
        OwnedTarget target = requireOwnedTarget(userId, messageId, pushSnapshotId);
        return analysisRunOrchestrator.run(AnalysisRunCommand.manualForUser(
                userId, target.snapshot().getSymbol(), target.snapshot().getTimeframe(),
                RequestIdSupport.currentOrNew(), null));
    }

    private OwnedTarget requireOwnedTarget(Long userId, String messageId, String pushSnapshotId) {
        if (userId == null || userId <= 0 || blank(messageId) || blank(pushSnapshotId)) throw notFound();
        MessageDO message = messageMapper.selectByIdForUser(messageId, userId);
        if (message == null || !Objects.equals(message.getUserId(), userId)
                || !"PUSH_SNAPSHOT".equals(message.getSourceType()) || blank(message.getSourceId())) {
            throw notFound();
        }
        Long pushId;
        try {
            pushId = Long.valueOf(message.getSourceId());
        } catch (RuntimeException ignored) {
            throw notFound();
        }
        if (!snapshotIdentity(pushId).equals(pushSnapshotId)
                && !String.valueOf(pushId).equals(pushSnapshotId)) throw notFound();
        TmPushSnapshotDO snapshot = pushSnapshotMapper.selectByPushId(pushId);
        if (snapshot == null || !same(message.getAnalysisId(), snapshot.getAnalysisId())
                || !sameWhenPresent(message.getTraceId(), snapshot.getTraceId())) {
            throw notFound();
        }
        if (!blank(message.getPlanId())) {
            ExecutionPlanDO plan = executionPlanMapper.selectByPlanId(message.getPlanId());
            if (plan == null || !same(plan.getAnalysisId(), snapshot.getAnalysisId())) throw notFound();
        }
        return new OwnedTarget(message, snapshot, pushId, snapshotIdentity(pushId));
    }

    private Projection projection(OwnedTarget target, TmPushRecheckLogDO latest) {
        String state = latest == null ? "INSUFFICIENT_DATA"
                : "ERROR".equalsIgnoreCase(latest.getExecutionStatus()) ? "ERROR" : latest.getRecheckStatus();
        String reason = latest == null ? "WAITING_RECHECK_RESULT" : latest.getExecutionErrorMessage();
        return new Projection(target.message().getMessageId(), target.snapshotId(),
                target.pushId(), latest == null ? null : latest.getLogId(), target.snapshot().getAnalysisId(),
                target.message().getPlanId(), latest != null && !blank(latest.getTraceId())
                        ? latest.getTraceId() : target.snapshot().getTraceId(),
                target.snapshot(), latest, state, reason,
                latest != null && "ERROR".equalsIgnoreCase(latest.getExecutionStatus()), true);
    }

    private TmPushRecheckLogDO latestOpen(Long pushId) {
        return recheckLogMapper.selectLatestByPushIdAndTriggerSource(pushId, PUSH_OPEN);
    }

    public static String snapshotIdentity(Long pushId) {
        return pushId == null ? null : SNAPSHOT_ID_PREFIX + pushId;
    }

    private static boolean same(String left, String right) {
        return !blank(left) && Objects.equals(left, right);
    }

    private static boolean sameWhenPresent(String left, String right) {
        return blank(left) || Objects.equals(left, right);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static WorkspaceRecheckNotFoundException notFound() {
        return new WorkspaceRecheckNotFoundException();
    }

    private record OwnedTarget(MessageDO message, TmPushSnapshotDO snapshot, Long pushId, String snapshotId) { }

    private record OpenKey(Long userId, Long pushId) { }

    public record Projection(String messageId,
                             String pushSnapshotId,
                             Long pushId,
                             Long recheckId,
                             String analysisId,
                             String planId,
                             String traceId,
                             TmPushSnapshotDO originalSnapshot,
                             TmPushRecheckLogDO currentResult,
                             String resultState,
                             String reason,
                             boolean retryAvailable,
                             boolean notTradeInstruction) { }

    public static class WorkspaceRecheckNotFoundException extends RuntimeException { }
}
