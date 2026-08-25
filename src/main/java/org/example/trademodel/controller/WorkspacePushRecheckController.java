package org.example.trademodel.controller;

import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.WorkspacePushRecheckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace/rechecks")
public class WorkspacePushRecheckController {
    private final AuthenticatedUserIdResolver userIdResolver;
    private final WorkspacePushRecheckService service;

    public WorkspacePushRecheckController(AuthenticatedUserIdResolver userIdResolver,
                                          WorkspacePushRecheckService service) {
        this.userIdResolver = userIdResolver;
        this.service = service;
    }

    @PostMapping("/{pushSnapshotId}/open")
    public ApiResponse<WorkspacePushRecheckService.Projection> open(
            @PathVariable String pushSnapshotId, @RequestBody MessageRequest request) {
        return ApiResponse.success(service.open(userIdResolver.requireCurrentUserId(),
                request == null ? null : request.messageId(), pushSnapshotId));
    }

    @GetMapping("/{pushSnapshotId}")
    public ApiResponse<WorkspacePushRecheckService.Projection> read(
            @PathVariable String pushSnapshotId, @RequestParam String messageId) {
        return ApiResponse.success(service.read(userIdResolver.requireCurrentUserId(), messageId, pushSnapshotId));
    }

    @PostMapping("/{pushSnapshotId}/retry")
    public ResponseEntity<ApiResponse<WorkspacePushRecheckService.Projection>> retry(
            @PathVariable String pushSnapshotId, @RequestBody MessageRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(service.retry(userIdResolver.requireCurrentUserId(),
                    request == null ? null : request.messageId(), pushSnapshotId)));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @PostMapping("/{pushSnapshotId}/reanalyze")
    public ApiResponse<AnalysisRunResult> reanalyze(
            @PathVariable String pushSnapshotId, @RequestBody MessageRequest request) {
        return ApiResponse.success(service.reanalyze(userIdResolver.requireCurrentUserId(),
                request == null ? null : request.messageId(), pushSnapshotId));
    }

    @ExceptionHandler(WorkspacePushRecheckService.WorkspaceRecheckNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound("recheck target not found"));
    }

    public record MessageRequest(String messageId) { }
}
