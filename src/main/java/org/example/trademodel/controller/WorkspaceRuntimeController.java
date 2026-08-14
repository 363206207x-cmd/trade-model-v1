package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.PlanRevalidationRecordDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MacroEventMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.NewsEventMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.AsyncTaskService;
import org.example.trademodel.service.ChannelDeliveryService;
import org.example.trademodel.service.EventAssetRelationService;
import org.example.trademodel.service.MessageFactService;
import org.example.trademodel.service.PlanRevalidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceRuntimeController {
    private final AuthenticatedUserIdResolver userIdResolver;
    private final ExecutionPlanMapper executionPlanMapper;
    private final PlanRevalidationService planRevalidationService;
    private final MessageFactService messageFactService;
    private final ChannelDeliveryService channelDeliveryService;
    private final AsyncTaskService asyncTaskService;
    private final MacroEventMapper macroEventMapper;
    private final NewsEventMapper newsEventMapper;
    private final EventAssetRelationService eventAssetRelationService;
    private final MessageMapper messageMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;

    public WorkspaceRuntimeController(AuthenticatedUserIdResolver userIdResolver,
                                      ExecutionPlanMapper executionPlanMapper,
                                      PlanRevalidationService planRevalidationService,
                                      MessageFactService messageFactService,
                                      ChannelDeliveryService channelDeliveryService,
                                      AsyncTaskService asyncTaskService,
                                      MacroEventMapper macroEventMapper,
                                      NewsEventMapper newsEventMapper,
                                      EventAssetRelationService eventAssetRelationService,
                                      MessageMapper messageMapper,
                                      PushSnapshotMapper pushSnapshotMapper,
                                      PushRecheckLogMapper pushRecheckLogMapper) {
        this.userIdResolver = userIdResolver;
        this.executionPlanMapper = executionPlanMapper;
        this.planRevalidationService = planRevalidationService;
        this.messageFactService = messageFactService;
        this.channelDeliveryService = channelDeliveryService;
        this.asyncTaskService = asyncTaskService;
        this.macroEventMapper = macroEventMapper;
        this.newsEventMapper = newsEventMapper;
        this.eventAssetRelationService = eventAssetRelationService;
        this.messageMapper = messageMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<ApiResponse<ExecutionPlanDO>> finalPlan(@PathVariable String planId) {
        userIdResolver.requireCurrentUserId();
        ExecutionPlanDO plan = executionPlanMapper.selectByPlanId(planId);
        if (!isValidatedFinal(plan)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("validated final plan not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @GetMapping("/plan-revalidations")
    public ApiResponse<List<PlanRevalidationRecordDO>> revalidations(@RequestParam String planId,
                                                                     @RequestParam(defaultValue = "20") int limit) {
        userIdResolver.requireCurrentUserId();
        return ApiResponse.success(planRevalidationService.list(planId, limit));
    }

    @PostMapping("/plan-revalidations")
    public ResponseEntity<ApiResponse<PlanRevalidationRecordDO>> requestRevalidation(
            @RequestBody PlanRevalidationRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(planRevalidationService.request(
                    userIdResolver.requireCurrentUserId(), request.planId(),
                    request.triggerType(), request.reason())));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @GetMapping("/messages")
    public ApiResponse<List<MessageDO>> messages(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(messageFactService.listForUser(userIdResolver.requireCurrentUserId(), limit));
    }

    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<ApiResponse<Boolean>> markMessageRead(@PathVariable String messageId) {
        boolean updated = messageFactService.markRead(userIdResolver.requireCurrentUserId(), messageId);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("message not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    @GetMapping("/messages/{messageId}/deliveries")
    public ApiResponse<List<ChannelDeliveryDO>> deliveries(@PathVariable String messageId) {
        return ApiResponse.success(channelDeliveryService.listForMessage(
                userIdResolver.requireCurrentUserId(), messageId));
    }

    @GetMapping("/tasks")
    public ApiResponse<?> tasks(@RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.success(asyncTaskService.listForUser(userIdResolver.requireCurrentUserId(), limit));
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ResponseEntity<ApiResponse<?>> retryTask(@PathVariable String taskId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    asyncTaskService.retryForUser(userIdResolver.requireCurrentUserId(), taskId)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelTask(@PathVariable String taskId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    asyncTaskService.cancelForUser(userIdResolver.requireCurrentUserId(), taskId)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @GetMapping("/rechecks/{pushSnapshotId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recheck(@PathVariable String pushSnapshotId) {
        Long userId = userIdResolver.requireCurrentUserId();
        MessageDO owner = messageMapper.selectByRecheckIdForUser(pushSnapshotId, userId);
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("push snapshot not found or not owned"));
        }
        Long pushId;
        try {
            pushId = Long.valueOf(owner.getSourceId());
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("push snapshot identity unavailable"));
        }
        TmPushSnapshotDO snapshot = pushSnapshotMapper.selectByPushId(pushId);
        if (snapshot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("push snapshot unavailable"));
        }
        TmPushRecheckLogDO latest = pushRecheckLogMapper.selectLatestByPushId(pushId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pushSnapshotId", pushSnapshotId);
        data.put("originalSnapshot", snapshot);
        data.put("currentResult", latest);
        data.put("resultState", latest == null ? "INSUFFICIENT_DATA" : latest.getRecheckStatus());
        data.put("reason", latest == null ? "WAITING_RECHECK_RESULT" : latest.getExecutionErrorMessage());
        data.put("notTradeInstruction", true);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/events")
    public ApiResponse<Map<String, Object>> events(@RequestParam(defaultValue = "30") int limit,
                                                   @RequestParam(required = false) String symbol) {
        userIdResolver.requireCurrentUserId();
        int safeLimit = Math.max(1, Math.min(limit, 100));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("macro", macroEventMapper.selectRecent(safeLimit));
        data.put("industryAndProject", newsEventMapper.selectRecent(safeLimit));
        data.put("assetRelations", symbol == null ? List.of()
                : eventAssetRelationService.listBySymbol(symbol, safeLimit));
        return ApiResponse.success(data);
    }

    private boolean isValidatedFinal(ExecutionPlanDO plan) {
        return plan != null
                && Boolean.TRUE.equals(plan.getFinalPlan())
                && "PASS".equalsIgnoreCase(plan.getRuleValidationStatus())
                && "FINAL_VALIDATED".equalsIgnoreCase(plan.getChainStatus());
    }

    public record PlanRevalidationRequest(String planId, String triggerType, String reason) {
    }
}
