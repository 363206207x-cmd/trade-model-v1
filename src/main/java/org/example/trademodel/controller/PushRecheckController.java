package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.req.PushRecheckDispatchConfigRequest;
import org.example.trademodel.dto.req.PushRecheckReplayRequest;
import org.example.trademodel.dto.req.PushRecheckTriggerRequest;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.PushRecheckScheduler;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.PushRecheckService;
import org.example.trademodel.service.RecheckExecutionCommand;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.vo.PushRecheckDispatchConfigAuditVO;
import org.example.trademodel.vo.PushRecheckLogItemVO;
import org.example.trademodel.vo.PushRecheckOpsOverviewVO;
import org.example.trademodel.vo.PushRecheckReplaySummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/push/recheck")
public class PushRecheckController {

    private final PushRecheckService pushRecheckService;
    private final PushRecheckScheduler pushRecheckScheduler;
    private final PushRecheckDispatchConfigService dispatchConfigService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @Autowired
    public PushRecheckController(PushRecheckService pushRecheckService,
                                 PushRecheckScheduler pushRecheckScheduler,
                                 PushRecheckDispatchConfigService dispatchConfigService,
                                 AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.pushRecheckService = pushRecheckService;
        this.pushRecheckScheduler = pushRecheckScheduler;
        this.dispatchConfigService = dispatchConfigService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    /**
     * 按 pushId 执行一次二次校验并落库；当前价由调用方传入（与内部 recheck 逻辑一致）。
     */
    @PostMapping("/{pushId}")
    public ApiResponse<RecheckResult> triggerRecheck(
            @PathVariable Long pushId,
            @RequestBody(required = false) PushRecheckTriggerRequest body) {
        BigDecimal price = body != null ? body.getCurrentPrice() : null;
        RecheckExecutionCommand command = RecheckExecutionCommand.manual();
        if (body != null) {
            command.setDispatchBatchId(body.getDispatchBatchId());
            command.setDispatchInstructionId(body.getDispatchInstructionId());
        }
        RecheckResult result = pushRecheckService.recheck(pushId, price, command);
        return ApiResponse.success(result);
    }

    @GetMapping("/dispatch/config")
    public ApiResponse<Map<String, Integer>> getDispatchConfig() {
        return ApiResponse.success(pushRecheckScheduler.getDispatchConfig());
    }

    @PostMapping("/dispatch/config")
    public ApiResponse<Map<String, Integer>> updateDispatchConfig(
            @RequestBody PushRecheckDispatchConfigRequest request) {
        return ApiResponse.success(
                pushRecheckScheduler.updateDispatchConfig(
                        request != null ? request.getLimit() : null,
                        request != null ? request.getMaxAttempts() : null,
                        request != null ? request.getMinRetryMinutes() : null
                ));
    }

    @GetMapping("/dispatch/config/audit")
    public ApiResponse<List<PushRecheckDispatchConfigAuditVO>> listDispatchConfigAudit(
            @RequestParam(value = "limit", required = false) Integer limit) {
        int safeLimit = limit != null ? limit : 50;
        return ApiResponse.success(dispatchConfigService.listRecentAudit(safeLimit).stream().map(row -> {
            PushRecheckDispatchConfigAuditVO vo = new PushRecheckDispatchConfigAuditVO();
            vo.setAuditId(row.getAuditId());
            vo.setConfigKey(row.getConfigKey());
            vo.setOldValue(row.getOldValue());
            vo.setNewValue(row.getNewValue());
            vo.setChangedBy(row.getChangedBy());
            vo.setChangeSource(row.getChangeSource());
            vo.setCreateTime(row.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
    }

    @PostMapping("/replay")
    public ApiResponse<List<RecheckResult>> replay(@RequestBody PushRecheckReplayRequest request) {
        String batchId = request != null ? request.getDispatchBatchId() : null;
        String instructionId = request != null ? request.getDispatchInstructionId() : null;
        return ApiResponse.success(pushRecheckService.replayByDispatch(batchId, instructionId));
    }

    @GetMapping("/replay/summary")
    public ApiResponse<PushRecheckReplaySummaryVO> replaySummary(
            @RequestParam(value = "dispatchBatchId", required = false) String dispatchBatchId,
            @RequestParam(value = "dispatchInstructionId", required = false) String dispatchInstructionId) {
        return ApiResponse.success(pushRecheckService.summarizeReplayByDispatch(dispatchBatchId, dispatchInstructionId));
    }

    @GetMapping("/ops/overview")
    public ApiResponse<PushRecheckOpsOverviewVO> opsOverview(
            @RequestParam(value = "dispatchBatchId", required = false) String dispatchBatchId,
            @RequestParam(value = "dispatchInstructionId", required = false) String dispatchInstructionId,
            @RequestParam(value = "auditLimit", required = false) Integer auditLimit,
            @RequestParam(value = "logLimit", required = false) Integer logLimit) {
        return ApiResponse.success(pushRecheckService.getOpsOverview(
                dispatchBatchId, dispatchInstructionId, auditLimit, logLimit));
    }

    /** Raw Recheck rows have no owner identity, so user-facing reads fail closed. */
    @GetMapping("/{pushId}/latest")
    public ResponseEntity<ApiResponse<PushRecheckLogItemVO>> latest(@PathVariable Long pushId) {
        authenticatedUserIdResolver.requireCurrentUserId();
        return privateRecheckReadUnavailable();
    }

    /** Raw Recheck rows have no owner identity, so user-facing reads fail closed. */
    @GetMapping("/{pushId}/logs")
    public ResponseEntity<ApiResponse<List<PushRecheckLogItemVO>>> logs(@PathVariable Long pushId) {
        authenticatedUserIdResolver.requireCurrentUserId();
        return privateRecheckReadUnavailable();
    }

    private static <T> ResponseEntity<ApiResponse<T>> privateRecheckReadUnavailable() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("push recheck private data unavailable"));
    }
}
