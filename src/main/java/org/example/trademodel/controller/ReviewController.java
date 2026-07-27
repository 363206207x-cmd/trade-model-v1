package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogSourceViewPolicy;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.RuleVersionLogQueryService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.userpositionreview.UserPositionReviewAdapter;
import org.example.trademodel.userpositionreview.UserPositionReviewFeedbackReq;
import org.example.trademodel.userpositionreview.UserPositionReviewFeedbackResultDTO;
import org.example.trademodel.userpositionreview.UserPositionReviewSummaryDTO;
import org.example.trademodel.vo.ReviewAggregateDetailVO;
import org.example.trademodel.vo.ReviewAggregateSummaryVO;
import org.example.trademodel.vo.ReviewAggregateVO;
import org.example.trademodel.vo.ReviewStateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewAggregateService reviewAggregateService;
    private final RuleVersionLogQueryService ruleVersionLogQueryService;
    private final PositionMonitorLogService positionMonitorLogService;
    private final UserPositionReviewAdapter userPositionReviewAdapter;
    private final OpportunityLogService opportunityLogService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    @Autowired
    public ReviewController(ReviewService reviewService,
                            ReviewAggregateService reviewAggregateService,
                            RuleVersionLogQueryService ruleVersionLogQueryService,
                            PositionMonitorLogService positionMonitorLogService,
                            UserPositionReviewAdapter userPositionReviewAdapter,
                            OpportunityLogService opportunityLogService,
                            AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.reviewService = reviewService;
        this.reviewAggregateService = reviewAggregateService;
        this.ruleVersionLogQueryService = ruleVersionLogQueryService;
        this.positionMonitorLogService = positionMonitorLogService;
        this.userPositionReviewAdapter = userPositionReviewAdapter;
        this.opportunityLogService = opportunityLogService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    /**
     * 复盘聚合：按 analysisId 返回管道摘要；无 tm_analysis_run 行时 404。
     * 用户可编辑的复盘字段见 {@link #getState(String)} / {@link #saveReview(WriteReviewResultReq)}。
     */
    @GetMapping("/aggregate/{analysisId}")
    public ResponseEntity<ApiResponse<ReviewAggregateVO>> getAggregate(@PathVariable String analysisId) {
        return reviewAggregateService.getAggregateByAnalysisId(analysisId)
                .map(v -> ResponseEntity.ok(ApiResponse.success(v)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("analysis not found: " + analysisId)));
    }

    /**
     * Step 3：首屏摘要（轻载），不返回大列表明细。
     */
    @GetMapping("/aggregate/{analysisId}/summary")
    public ResponseEntity<ApiResponse<ReviewAggregateSummaryVO>> getAggregateSummary(@PathVariable String analysisId) {
        return reviewAggregateService.getAggregateSummaryByAnalysisId(analysisId)
                .map(v -> ResponseEntity.ok(ApiResponse.success(v)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("analysis not found: " + analysisId)));
    }

    /**
     * Step 3：明细懒加载，按 section 拉取并对 limit 应用上限护栏。
     */
    @GetMapping("/aggregate/{analysisId}/detail")
    public ResponseEntity<ApiResponse<ReviewAggregateDetailVO>> getAggregateDetail(
            @PathVariable String analysisId,
            @RequestParam(defaultValue = "pushRecheck") String section,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            return reviewAggregateService.getAggregateDetailByAnalysisId(analysisId, section, limit)
                    .map(v -> ResponseEntity.ok(ApiResponse.success(v)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.notFound("analysis not found: " + analysisId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * RuleVersion 审计链检索（Step 2）：保持最小查询面，先服务“可稳定检索”。
     */
    @GetMapping("/rule-version-logs")
    public ResponseEntity<ApiResponse<java.util.List<ReviewAggregateVO.RuleVersionLogSummary>>> queryRuleVersionLogs(
            @RequestParam(required = false) String analysisId,
            @RequestParam(required = false) String ruleVersion,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String rollbackFlag,
            @RequestParam(required = false) String errorType,
            @RequestParam(required = false) String changeCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createdAtFrom,
            @RequestParam(required = false) String createdAtTo,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        java.util.List<ReviewAggregateVO.RuleVersionLogSummary> rows = ruleVersionLogQueryService.query(
                analysisId, ruleVersion, operator, rollbackFlag, errorType,
                changeCategory, keyword, createdAtFrom, createdAtTo, limit);
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    /**
     * P0-4：Review 只读查询持仓监控日志，不触发监控运行或复盘摘要生成。
     */
    @GetMapping("/positions/{positionId}/monitor-logs")
    public ResponseEntity<ApiResponse<java.util.List<PositionMonitorLogDTO>>> listPositionMonitorLogs(
            @PathVariable Long positionId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        java.util.List<PositionMonitorLogDTO> rows = positionMonitorLogService
                .listByPositionIdForUser(userId, positionId, limit);
        java.util.List<PositionMonitorLogDTO> safeRows = rows == null ? java.util.List.of() : rows.stream()
                .map(PositionMonitorLogSourceViewPolicy::sanitize)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(safeRows));
    }

    @GetMapping("/user-positions/{positionId}/summary")
    public ResponseEntity<ApiResponse<UserPositionReviewSummaryDTO>> getUserPositionReviewSummary(
            @PathVariable Long positionId) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                userPositionReviewAdapter.buildSummaryForUser(userId, positionId)));
    }

    @PostMapping("/user-positions/{positionId}/feedback")
    public ResponseEntity<ApiResponse<UserPositionReviewFeedbackResultDTO>> recordUserPositionReviewFeedback(
            @PathVariable Long positionId,
            @RequestBody UserPositionReviewFeedbackReq request) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                userPositionReviewAdapter.recordFeedbackForUser(userId, positionId, request)));
    }

    @GetMapping("/opportunities/stats")
    public ResponseEntity<ApiResponse<OpportunityLogStatsDTO>> getOpportunityStats(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(opportunityLogService.getStats(symbol, from, to)));
    }

    /**
     * 已保存复盘状态：无记录时 data 为 null（HTTP 200），便于前端空表单。
     */
    @GetMapping("/state/{analysisId}")
    public ResponseEntity<ApiResponse<ReviewStateVO>> getState(@PathVariable String analysisId) {
        ReviewStateVO vo = reviewService.getStateByAnalysisId(analysisId);
        return ResponseEntity.ok(ApiResponse.success(vo));
    }

    /**
     * 保存复盘：按 analysisId upsert；同一 analysisId 仅一行，重复保存为覆盖更新。
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<ReviewStateVO>> saveReview(@Valid @RequestBody WriteReviewResultReq req) {
        ReviewStateVO vo = reviewService.saveOrUpdate(req);
        return ResponseEntity.ok(ApiResponse.success(vo));
    }
}
