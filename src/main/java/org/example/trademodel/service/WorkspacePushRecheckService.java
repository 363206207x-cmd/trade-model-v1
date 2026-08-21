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

/** Owner-scoped workspace adapter. Raw push-id operations remain globally closed. */
@Service
public class WorkspacePushRecheckService {
    private static final String PUSH_OPEN = "PUSH_OPEN";

    private final MessageMapper messageMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper recheckLogMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final PushRecheckService pushRecheckService;
    private final AnalysisRunOrchestrator analysisRunOrchestrator;

    public WorkspacePushRecheckService(MessageMapper messageMapper,
                                       PushSnapshotMapper pushSnapshotMapper,
                                       PushRecheckLogMapper recheckLogMapper,
                                       ExecutionPlanMapper executionPlanMapper,
                                       PushRecheckService pushRecheckService,
                                       AnalysisRunOrchestrator analysisRunOrchestrator) {
        this.messageMapper = messageMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.recheckLogMapper = recheckLogMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.pushRecheckService = pushRecheckService;
        this.analysisRunOrchestrator = analysisRunOrchestrator;
    }

    public synchronized Projection open(Long userId, String messageId, String pushSnapshotId) {
        OwnedTarget target = requireOwnedTarget(userId, messageId, pushSnapshotId);
        TmPushRecheckLogDO latestOpen = latestOpen(target.pushId());
        if (latestOpen == null) {
            pushRecheckService.recheckForOwnedPushOpen(target.pushId(), null, 1);
        }
        return projection(target, latestOpen(target.pushId()));
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
        pushRecheckService.recheckForOwnedPushOpen(target.pushId(), latest.getLogId(), attempt);
        return projection(target, latestOpen(target.pushId()));
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
                || !"PUSH_SNAPSHOT".equals(message.getSourceType())
                || !pushSnapshotId.equals(message.getCurrentRecheckId()) || blank(message.getSourceId())) {
            throw notFound();
        }
        Long pushId;
        try {
            pushId = Long.valueOf(message.getSourceId());
        } catch (RuntimeException ignored) {
            throw notFound();
        }
        TmPushSnapshotDO snapshot = pushSnapshotMapper.selectByPushId(pushId);
        if (snapshot == null || !same(message.getAnalysisId(), snapshot.getAnalysisId())
                || !sameWhenPresent(message.getTraceId(), snapshot.getTraceId())) {
            throw notFound();
        }
        if (!blank(message.getPlanId())) {
            ExecutionPlanDO plan = executionPlanMapper.selectByPlanId(message.getPlanId());
            if (plan == null || !same(plan.getAnalysisId(), snapshot.getAnalysisId())) throw notFound();
        }
        return new OwnedTarget(message, snapshot, pushId);
    }

    private Projection projection(OwnedTarget target, TmPushRecheckLogDO latest) {
        String state = latest == null ? "INSUFFICIENT_DATA" : latest.getRecheckStatus();
        String reason = latest == null ? "WAITING_RECHECK_RESULT" : latest.getExecutionErrorMessage();
        return new Projection(target.message().getMessageId(), target.message().getCurrentRecheckId(),
                target.pushId(), latest == null ? null : latest.getLogId(), target.snapshot().getAnalysisId(),
                target.message().getPlanId(), latest != null && !blank(latest.getTraceId())
                        ? latest.getTraceId() : target.snapshot().getTraceId(),
                target.snapshot(), latest, state, reason,
                latest != null && "ERROR".equalsIgnoreCase(latest.getExecutionStatus()), true);
    }

    private TmPushRecheckLogDO latestOpen(Long pushId) {
        return recheckLogMapper.selectLatestByPushIdAndTriggerSource(pushId, PUSH_OPEN);
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

    private record OwnedTarget(MessageDO message, TmPushSnapshotDO snapshot, Long pushId) { }

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
