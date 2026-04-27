package org.example.trademodel.service.impl;

import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.service.RuleVersionLogQueryService;
import org.example.trademodel.vo.ReviewAggregateVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RuleVersionLogQueryServiceImpl implements RuleVersionLogQueryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final RuleVersionLogMapper ruleVersionLogMapper;

    public RuleVersionLogQueryServiceImpl(RuleVersionLogMapper ruleVersionLogMapper) {
        this.ruleVersionLogMapper = ruleVersionLogMapper;
    }

    @Override
    public List<ReviewAggregateVO.RuleVersionLogSummary> listByAnalysisId(String analysisId, int limit) {
        if (analysisId == null || analysisId.isBlank()) {
            return Collections.emptyList();
        }
        return query(analysisId, null, null, null, null, null, null, null, null, limit);
    }

    @Override
    public List<ReviewAggregateVO.RuleVersionLogSummary> query(
            String analysisId,
            String ruleVersion,
            String operator,
            String rollbackFlag,
            String errorType,
            String changeCategory,
            String keyword,
            String createdAtFrom,
            String createdAtTo,
            int limit) {
        int safeLimit = normalizeLimit(limit);
        List<RuleVersionLogDO> rows = ruleVersionLogMapper.queryLogs(
                normalizeBlank(analysisId),
                normalizeBlank(ruleVersion),
                normalizeBlank(operator),
                normalizeBlank(rollbackFlag),
                normalizeBlank(errorType),
                normalizeBlank(changeCategory),
                normalizeBlank(keyword),
                normalizeBlank(createdAtFrom),
                normalizeBlank(createdAtTo),
                safeLimit);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewAggregateVO.RuleVersionLogSummary> out = new ArrayList<>(rows.size());
        for (RuleVersionLogDO row : rows) {
            out.add(toSummary(row));
        }
        return out;
    }

    private static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static ReviewAggregateVO.RuleVersionLogSummary toSummary(RuleVersionLogDO row) {
        ReviewAggregateVO.RuleVersionLogSummary s = new ReviewAggregateVO.RuleVersionLogSummary();
        s.setId(row.getId());
        s.setAnalysisId(valueOrFallback(row.getAnalysisId(), parseKv(row.getChangeSummary(), "analysisId")));
        s.setRuleVersion(row.getRuleVersion());
        s.setErrorType(valueOrFallback(row.getErrorType(), parseKv(row.getChangeSummary(), "errorType")));
        s.setChangeCategory(valueOrFallback(row.getChangeCategory(), parseCategory(row.getChangeSummary())));
        s.setChangeSummary(row.getChangeSummary());
        s.setChangeDetail(row.getChangeDetail());
        s.setOperator(row.getOperator());
        s.setRollbackFlag(row.getRollbackFlag());
        s.setCreatedAt(row.getCreatedAt());
        s.setFallbackMatched(Boolean.TRUE.equals(needsFallback(row)));
        return s;
    }

    private static Boolean needsFallback(RuleVersionLogDO row) {
        if (row == null || row.getChangeSummary() == null || row.getChangeSummary().isBlank()) {
            return false;
        }
        return isBlank(row.getAnalysisId()) || isBlank(row.getErrorType()) || isBlank(row.getChangeCategory());
    }

    private static String parseCategory(String changeSummary) {
        if (changeSummary == null || changeSummary.isBlank()) {
            return null;
        }
        int idx = changeSummary.indexOf(';');
        if (idx <= 0) {
            return changeSummary.trim();
        }
        return changeSummary.substring(0, idx).trim();
    }

    private static String parseKv(String text, String key) {
        if (text == null || text.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        String[] parts = text.split(";");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0 || idx >= part.length() - 1) {
                continue;
            }
            String k = part.substring(0, idx).trim();
            if (!key.equals(k)) {
                continue;
            }
            String value = part.substring(idx + 1).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private static String valueOrFallback(String value, String fallback) {
        if (!isBlank(value)) {
            return value;
        }
        return isBlank(fallback) ? null : fallback;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
