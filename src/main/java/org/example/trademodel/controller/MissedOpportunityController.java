package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.MissedOpportunityService;
import org.example.trademodel.service.MissedReasonViewParser;
import org.example.trademodel.vo.MissedOpportunityQueryItemVO;
import org.example.trademodel.vo.MissedReasonViewVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/missed-opportunity")
public class MissedOpportunityController {
    private static final String REVIEW_ARCHIVE_AGGREGATE_READY = "REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY";
    private static final String REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING = "REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING_FAIL_CLOSED";
    private static final String REVIEW_ARCHIVE_AGGREGATE_MISSING = "REVIEW_ARCHIVE_AGGREGATE_MISSING_FAIL_CLOSED";
    private static final String REVIEW_ARCHIVE_AGGREGATE_PARTIAL = "REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY";
    private static final String MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY = "MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY";
    private static final String REVIEW_ARCHIVE_COUNT_REVIEW_ONLY = "REVIEW_ARCHIVE_COUNT_REVIEW_ONLY";
    private static final String MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED = "MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED = "MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED = "REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String REPLAY_BOUNDARY_BLOCKED = "REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String RECHECK_BOUNDARY_BLOCKED = "RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String PUSH_BOUNDARY_BLOCKED = "PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String CANDIDATE_BOUNDARY_BLOCKED = "CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String POINT_BOUNDARY_BLOCKED = "POINT_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String TRADING_BOUNDARY_BLOCKED = "TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED";

    private final MissedOpportunityService missedOpportunityService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public MissedOpportunityController(MissedOpportunityService missedOpportunityService,
                                       AuthenticatedUserIdResolver userIdResolver) {
        this.missedOpportunityService = missedOpportunityService;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping("/query")
    public ApiResponse<List<MissedOpportunityQueryItemVO>> query(
            @RequestParam(required = false) String analysisId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate,
            @RequestParam(required = false) String missedId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        Long userId = userIdResolver.requireCurrentUserId();
        if (missedId != null && !missedId.trim().isEmpty()) {
            MissedOpportunityDO row = missedOpportunityService.findByMissedIdForUser(
                    userId, missedId.trim());
            if (row == null) {
                return ApiResponse.success(Collections.emptyList());
            }
            return ApiResponse.success(List.of(toQueryItem(row)));
        }
        List<MissedOpportunityDO> rows = missedOpportunityService.queryForUser(
                userId, analysisId, symbol, bizDate, safeLimit(limit));
        List<MissedOpportunityQueryItemVO> data = rows.stream()
                .map(MissedOpportunityController::toQueryItem)
                .collect(Collectors.toList());
        return ApiResponse.success(data);
    }

    @GetMapping("/review-archive-status")
    public Map<String, Object> reviewArchiveStatus(
            @RequestParam(required = false) String analysisId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate,
            @RequestParam(required = false) String missedId,
            @RequestParam(required = false, defaultValue = "5") Integer limit) {
        Long userId = userIdResolver.requireCurrentUserId();
        String normalizedAnalysisId = trimToNull(analysisId);
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedMissedId = trimToNull(missedId);
        LocalDate scopedBizDate = bizDate != null ? bizDate : LocalDate.now();
        Map<String, Object> status = baseReviewArchiveStatus(
                normalizedAnalysisId,
                normalizedSymbol,
                scopedBizDate
        );

        List<MissedOpportunityDO> rows;
        try {
            int count = missedOpportunityService.countByBizDateForUser(userId, scopedBizDate);
            status.put("todayMissedCount", count);
            status.put("countAvailable", true);
            if (normalizedMissedId != null) {
                MissedOpportunityDO row = missedOpportunityService.findByMissedIdForUser(
                        userId, normalizedMissedId);
                rows = row == null ? Collections.emptyList() : List.of(row);
            } else {
                LocalDate queryBizDate = (normalizedAnalysisId == null && normalizedSymbol == null)
                        ? scopedBizDate
                        : bizDate;
                rows = missedOpportunityService.queryForUser(
                        userId, normalizedAnalysisId, normalizedSymbol, queryBizDate, safeLimit(limit));
            }
            status.put("queryAvailable", true);
        } catch (Exception ignored) {
            applyReviewArchiveStatus(
                    status,
                    REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING,
                    "REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING",
                    "Missed Opportunity archive read owner path 不可用；只读状态 fail-closed。",
                    true,
                    "BLOCKED"
            );
            return status;
        }

        int missedCount = rows == null ? 0 : rows.size();
        status.put("missedCount", missedCount);
        if (missedCount <= 0) {
            boolean countOnly = ((Integer) status.getOrDefault("todayMissedCount", 0)) > 0
                    && normalizedMissedId == null
                    && normalizedAnalysisId == null;
            applyReviewArchiveStatus(
                    status,
                    countOnly ? REVIEW_ARCHIVE_AGGREGATE_PARTIAL : REVIEW_ARCHIVE_AGGREGATE_MISSING,
                    countOnly ? "REVIEW_ARCHIVE_AGGREGATE_PARTIAL_COUNT_ONLY" : "REVIEW_ARCHIVE_AGGREGATE_MISSING",
                    countOnly
                            ? "Missed Opportunity 仅有 count 信号；archive detail 暂不可读，只读状态保持 partial。"
                            : "Missed Opportunity archive row 未证明存在；只读状态 fail-closed。",
                    true,
                    countOnly ? "PARTIAL" : "MISSING"
            );
            return status;
        }

        MissedOpportunityDO latest = rows.get(0);
        MissedReasonViewVO reasonView = MissedReasonViewParser.parse(latest.getReasonJson());
        String parseStatus = firstNonBlank(reasonView.getParseStatus(), "UNKNOWN");
        boolean reasonViewAvailable = "OK".equals(parseStatus);
        boolean traceIdPresent = hasText(latest.getTraceId());
        boolean archiveLinked = hasText(latest.getAnalysisId());

        status.put("analysisId", firstNonBlank(latest.getAnalysisId(), normalizedAnalysisId));
        status.put("symbol", firstNonBlank(latest.getSymbol(), normalizedSymbol));
        status.put("bizDate", latest.getBizDate() != null ? latest.getBizDate() : scopedBizDate);
        status.put("latestMissedId", latest.getMissedId());
        status.put("latestCreateTime", latest.getCreateTime());
        status.put("latestRuleVersion", latest.getRuleVersion());
        status.put("traceIdPresent", traceIdPresent);
        status.put("reasonViewAvailable", reasonViewAvailable);
        status.put("reasonParseStatus", parseStatus);
        status.put("archiveLinked", archiveLinked);
        status.put("reviewAggregateMissedAvailable", false);
        status.put("sourceRef", firstNonBlank(latest.getMissedId(), "tm_missed_opportunity"));

        if ("PARSE_FAILED".equals(parseStatus)) {
            applyReviewArchiveStatus(
                    status,
                    REVIEW_ARCHIVE_AGGREGATE_PARTIAL,
                    "REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REASON_PARSE_FAILED",
                    "Missed Opportunity reason_json 解析失败；只读状态 fail-closed，不生成复盘结论。",
                    true,
                    "BLOCKED"
            );
        } else if ("EMPTY_REASON_JSON".equals(parseStatus)) {
            applyReviewArchiveStatus(
                    status,
                    REVIEW_ARCHIVE_AGGREGATE_PARTIAL,
                    "REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REASON_EMPTY",
                    "Missed Opportunity reason_json 缺失；仅展示 archive count/detail，状态 partial。",
                    true,
                    "PARTIAL"
            );
        } else if (!archiveLinked || !traceIdPresent) {
            applyReviewArchiveStatus(
                    status,
                    REVIEW_ARCHIVE_AGGREGATE_PARTIAL,
                    "REVIEW_ARCHIVE_AGGREGATE_PARTIAL_LINKAGE",
                    "Missed Opportunity archive linkage 或 trace id 不完整；只读展示，不生成交易动作。",
                    true,
                    "PARTIAL"
            );
        } else {
            applyReviewArchiveStatus(
                    status,
                    REVIEW_ARCHIVE_AGGREGATE_READY,
                    "REVIEW_ARCHIVE_AGGREGATE_OWNER_PATH_READ",
                    "Missed Opportunity / Review Archive 只读状态可读；不生成 missed opportunity、复盘结果或交易信号。",
                    false,
                    "OK"
            );
        }
        return status;
    }

    private static int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 200);
    }

    private static Map<String, Object> baseReviewArchiveStatus(
            String analysisId,
            String symbol,
            LocalDate bizDate) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", REVIEW_ARCHIVE_AGGREGATE_MISSING);
        status.put("aggregateStatus", REVIEW_ARCHIVE_AGGREGATE_MISSING);
        status.put("analysisId", analysisId);
        status.put("symbol", symbol);
        status.put("bizDate", bizDate);
        status.put("missedCount", 0);
        status.put("todayMissedCount", 0);
        status.put("missedOpportunityCountStatus", MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY);
        status.put("reviewArchiveCountStatus", REVIEW_ARCHIVE_COUNT_REVIEW_ONLY);
        status.put("latestMissedId", null);
        status.put("latestCreateTime", null);
        status.put("latestRuleVersion", null);
        status.put("traceIdPresent", false);
        status.put("reasonViewAvailable", false);
        status.put("reasonParseStatus", "UNKNOWN");
        status.put("archiveLinked", false);
        status.put("reviewAggregateMissedAvailable", false);
        status.put("queryAvailable", false);
        status.put("countAvailable", false);
        status.put("sourceHealth", "MISSING");
        status.put("sourceRef", "tm_missed_opportunity");
        status.put("reason", "REVIEW_ARCHIVE_AGGREGATE_STATUS_PENDING");
        status.put("message", "Missed Opportunity / Review Archive 只读状态待确认；不是交易信号。");
        status.put("failClosed", true);
        status.put("reviewOnly", true);
        status.put("manualReviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notFinalDirection", true);
        status.put("notEntryStopTpRr", true);
        status.put("notReplayExecution", true);
        status.put("notRecheckExecution", true);
        status.put("notMissedOpportunityGeneration", true);
        status.put("notMissedOpportunityWrite", true);
        status.put("notReviewResultGeneration", true);
        status.put("notPushSend", true);
        status.put("notExternalChannel", true);
        status.put("notExecutable", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("missedOpportunityGenerationBoundaryStatus", MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED);
        status.put("missedOpportunityWriteBoundaryStatus", MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED);
        status.put("reviewResultGenerationBoundaryStatus", REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED);
        status.put("replayBoundaryStatus", REPLAY_BOUNDARY_BLOCKED);
        status.put("recheckBoundaryStatus", RECHECK_BOUNDARY_BLOCKED);
        status.put("pushBoundaryStatus", PUSH_BOUNDARY_BLOCKED);
        status.put("candidateBoundaryStatus", CANDIDATE_BOUNDARY_BLOCKED);
        status.put("pointBoundaryStatus", POINT_BOUNDARY_BLOCKED);
        status.put("tradingBoundaryStatus", TRADING_BOUNDARY_BLOCKED);
        return status;
    }

    private static void applyReviewArchiveStatus(Map<String, Object> status,
                                                 String statusValue,
                                                 String reason,
                                                 String message,
                                                 boolean failClosed,
                                                 String sourceHealth) {
        status.put("status", statusValue);
        status.put("aggregateStatus", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private static String normalizeSymbol(String symbol) {
        String trimmed = trimToNull(symbol);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static MissedOpportunityQueryItemVO toQueryItem(MissedOpportunityDO row) {
        MissedOpportunityQueryItemVO vo = new MissedOpportunityQueryItemVO();
        vo.setMissedId(row.getMissedId());
        vo.setDecisionId(row.getDecisionId());
        vo.setAnalysisId(row.getAnalysisId());
        vo.setSymbol(row.getSymbol());
        vo.setBizDate(row.getBizDate());
        vo.setReasonJson(row.getReasonJson());
        vo.setReasonView(MissedReasonViewParser.parse(row.getReasonJson()));
        vo.setRuleVersion(row.getRuleVersion());
        vo.setTraceId(row.getTraceId());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }
}
