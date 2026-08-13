package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.DecisionChainAuditQueryService;
import org.example.trademodel.vo.DecisionChainAuditVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiDecisionAuditController {
    private final DecisionChainAuditQueryService auditQueryService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public AiDecisionAuditController(DecisionChainAuditQueryService auditQueryService,
                                     AuthenticatedUserIdResolver userIdResolver) {
        this.auditQueryService = auditQueryService;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping("/audit-chain")
    public ResponseEntity<ApiResponse<DecisionChainAuditVO>> auditChain(
            @RequestParam(required = false) String analysisId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String candidateId) {
        try {
            return auditQueryService.queryForUser(userIdResolver.requireCurrentUserId(), analysisId, traceId, candidateId)
                    .map(value -> ResponseEntity.ok(ApiResponse.success(value)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.notFound("decision chain not found or not owned")));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(exception.getMessage()));
        }
    }
}
