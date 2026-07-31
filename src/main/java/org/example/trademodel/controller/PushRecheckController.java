package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.req.PushRecheckDispatchConfigRequest;
import org.example.trademodel.dto.req.PushRecheckReplayRequest;
import org.example.trademodel.dto.req.PushRecheckTriggerRequest;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.PushRecheckAccessBoundary;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.vo.PushRecheckDispatchConfigAuditVO;
import org.example.trademodel.vo.PushRecheckLogItemVO;
import org.example.trademodel.vo.PushRecheckOpsOverviewVO;
import org.example.trademodel.vo.PushRecheckReplaySummaryVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/push/recheck")
public class PushRecheckController {

    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;
    private final PushRecheckAccessBoundary accessBoundary;

    public PushRecheckController(AuthenticatedUserIdResolver authenticatedUserIdResolver,
                                 PushRecheckAccessBoundary accessBoundary) {
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
        this.accessBoundary = accessBoundary;
    }

    @PostMapping("/{pushId}")
    public ResponseEntity<ApiResponse<RecheckResult>> triggerRecheck(
            @PathVariable Long pushId,
            @RequestBody(required = false) PushRecheckTriggerRequest body) {
        return unavailable(PushRecheckAccessBoundary.Operation.MUTATE_TRIGGER);
    }

    @GetMapping("/dispatch/config")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getDispatchConfig() {
        return unavailable(PushRecheckAccessBoundary.Operation.READ_CONFIG);
    }

    @PostMapping("/dispatch/config")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> updateDispatchConfig(
            @RequestBody PushRecheckDispatchConfigRequest request) {
        return unavailable(PushRecheckAccessBoundary.Operation.MUTATE_CONFIG);
    }

    @GetMapping("/dispatch/config/audit")
    public ResponseEntity<ApiResponse<List<PushRecheckDispatchConfigAuditVO>>> listDispatchConfigAudit(
            @RequestParam(value = "limit", required = false) Integer limit) {
        return unavailable(PushRecheckAccessBoundary.Operation.READ_CONFIG_AUDIT);
    }

    @PostMapping("/replay")
    public ResponseEntity<ApiResponse<List<RecheckResult>>> replay(
            @RequestBody PushRecheckReplayRequest request) {
        return unavailable(PushRecheckAccessBoundary.Operation.MUTATE_REPLAY);
    }

    @GetMapping("/replay/summary")
    public ResponseEntity<ApiResponse<PushRecheckReplaySummaryVO>> replaySummary(
            @RequestParam(value = "dispatchBatchId", required = false) String dispatchBatchId,
            @RequestParam(value = "dispatchInstructionId", required = false) String dispatchInstructionId) {
        return unavailable(PushRecheckAccessBoundary.Operation.READ_REPLAY_SUMMARY);
    }

    @GetMapping("/ops/overview")
    public ResponseEntity<ApiResponse<PushRecheckOpsOverviewVO>> opsOverview(
            @RequestParam(value = "dispatchBatchId", required = false) String dispatchBatchId,
            @RequestParam(value = "dispatchInstructionId", required = false) String dispatchInstructionId,
            @RequestParam(value = "auditLimit", required = false) Integer auditLimit,
            @RequestParam(value = "logLimit", required = false) Integer logLimit) {
        return unavailable(PushRecheckAccessBoundary.Operation.READ_OPS);
    }

    @GetMapping("/{pushId}/latest")
    public ResponseEntity<ApiResponse<PushRecheckLogItemVO>> latest(@PathVariable Long pushId) {
        return unavailable(PushRecheckAccessBoundary.Operation.READ_LATEST);
    }

    @GetMapping("/{pushId}/logs")
    public ResponseEntity<ApiResponse<List<PushRecheckLogItemVO>>> logs(@PathVariable Long pushId) {
        return unavailable(PushRecheckAccessBoundary.Operation.READ_LOGS);
    }

    private <T> ResponseEntity<ApiResponse<T>> unavailable(PushRecheckAccessBoundary.Operation operation) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        PushRecheckAccessBoundary.Decision decision = accessBoundary.evaluateUserRequest(
                new PushRecheckAccessBoundary.Request(userId, null, operation, null, null, null));
        if (decision.allowed()) {
            throw new IllegalStateException("raw PushRecheck route cannot be authorized without exact owner identity");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("push recheck private data unavailable"));
    }
}
