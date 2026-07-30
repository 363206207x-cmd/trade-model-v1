package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.opportunitylog.OpportunityLogCountRow;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.userpositionreview.UserPositionReviewPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class OpportunityLogServiceImpl implements OpportunityLogService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int DEFAULT_QUERY_LIMIT = 50;
    private static final int MAX_QUERY_LIMIT = 200;
    private static final int MAX_MARKET_BARS = 2000;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final OpportunityLogMapper opportunityLogMapper;
    private final UserPositionMapper userPositionMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    private final PersistedOhlcvBarMapper persistedOhlcvBarMapper;

    public OpportunityLogServiceImpl(OpportunityLogMapper opportunityLogMapper,
                                     UserPositionMapper userPositionMapper,
                                     DecisionResultMapper decisionResultMapper,
                                     ExecutionPlanMapper executionPlanMapper,
                                     PushSnapshotMapper pushSnapshotMapper,
                                     PushRecheckLogMapper pushRecheckLogMapper,
                                     AccountRiskSnapshotMapper accountRiskSnapshotMapper,
                                     PersistedOhlcvBarMapper persistedOhlcvBarMapper) {
        this.opportunityLogMapper = opportunityLogMapper;
        this.userPositionMapper = userPositionMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
        this.persistedOhlcvBarMapper = persistedOhlcvBarMapper;
    }

    @Override
    @Transactional
    public OpportunityLogDTO recordFromAuthoritativeAnalysis(AnalysisRunDO run,
                                                             DecisionResult decision,
                                                             ExecutionPlanDO plan,
                                                             Long accountRiskSnapshotId,
                                                             String traceId) {
        if (run == null || decision == null || blank(run.getAnalysisId()) || blank(decision.getDecisionId())) {
            return null;
        }
        if (!Boolean.TRUE.equals(decision.getIsWorthOpening())) {
            return null;
        }
        String direction = directionFromRuleBias(decision.getMarketBiasHierarchy());
        if (direction == null) {
            return null;
        }
        if (normalizeSymbol(firstNonBlank(run.getSymbol(), decision.getSymbol())) == null) {
            return null;
        }
        String key = run.getAnalysisId() + ":" + decision.getDecisionId();
        OpportunityLogDO existing = opportunityLogMapper.selectByOpportunityKey(key);
        if (existing != null) {
            OpportunityLogDTO dto = toSharedDto(existing);
            dto.setDeduplicated(true);
            return dto;
        }

        List<TmPushSnapshotDO> pushes = pushSnapshotMapper.listByAnalysisId(run.getAnalysisId());
        TmPushSnapshotDO push = pushes == null || pushes.isEmpty() ? null : pushes.get(0);
        OpportunityLogDO row = baseRow(run, decision, plan, push, direction, key, traceId);
        if (!isPlanCandidateReady(plan, decision)) {
            row.setLifecycleStatus(OpportunityLogStatus.SOURCE_INCOMPLETE);
            row.setReasonCodes(reason("SOURCE_INCOMPLETE"));
            opportunityLogMapper.insert(row);
            return OpportunityLogDTO.from(row);
        }

        BoundaryResolution boundary = resolveBoundaries(plan, push, direction);
        row.setEntryReference(boundary.entryReference);
        row.setTargetPrice(boundary.targetPrice);
        row.setInvalidationPrice(boundary.invalidationPrice);
        row.setReasonCodes(boundary.reasonCodes);
        if (!boundary.complete) {
            row.setLifecycleStatus(OpportunityLogStatus.SOURCE_INCOMPLETE);
        }
        opportunityLogMapper.insert(row);
        return OpportunityLogDTO.from(row);
    }

    @Override
    @Transactional
    public OpportunityLogDTO evaluateOpportunityForUser(String opportunityId, Long userId, LocalDateTime asOf) {
        requireUserId(userId);
        evaluateSharedOpportunity(opportunityId, asOf);
        return findByIdForUser(opportunityId, userId);
    }

    @Override
    @Transactional
    public OpportunityLogDTO evaluateOpportunityForSystem(String opportunityId, LocalDateTime asOf) {
        return evaluateSharedOpportunity(opportunityId, asOf);
    }

    private OpportunityLogDTO evaluateSharedOpportunity(String opportunityId, LocalDateTime asOf) {
        OpportunityLogDO row = requireOpportunity(opportunityId);
        normalizeSharedState(row);
        if (OpportunityLogStatus.RESOLVED.equals(row.getLifecycleStatus()) || row.getOpportunityStatus() != null) {
            OpportunityLogDTO dto = toSharedDto(row);
            dto.setDeduplicated(true);
            return dto;
        }
        if (!hasCompleteSource(row)) {
            return updateNonFinal(row, OpportunityLogStatus.SOURCE_INCOMPLETE, appendReason(row, "SOURCE_INCOMPLETE"));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime evaluationEnd = asOf != null ? asOf : now;
        row.setUserPositionPresent(false);
        row.setUserPositionId(null);
        row.setEvaluationAsOf(evaluationEnd);
        if (evaluationEnd.isBefore(row.getAnchorTime())) {
            return updateNonFinal(row, OpportunityLogStatus.SOURCE_INCOMPLETE,
                    appendReason(row, "EVALUATION_AS_OF_BEFORE_ANCHOR"));
        }

        InvalidationEvidence persistedInvalidation = persistedInvalidationEvidence(row, evaluationEnd);
        row.setReasonCodes(appendReason(row, persistedInvalidation.ignoredReasonCode));
        MarketPathResult marketPath = scanMarketPath(row, evaluationEnd);
        if (!marketPath.available) {
            if (persistedInvalidation.time != null) {
                marketPath.invalidationHit = true;
                marketPath.invalidationHitAt = persistedInvalidation.time;
                marketPath.hitOrder = OpportunityLogStatus.INVALIDATION_FIRST;
                marketPath.reasonCodes = reason("HOT_RESET_INVALIDATION_FIRST", persistedInvalidation.reasonCode);
            } else {
                return updateNonFinal(row, OpportunityLogStatus.MARKET_PATH_UNAVAILABLE,
                        appendReason(row, "MARKET_PATH_UNAVAILABLE"));
            }
        }
        if (persistedInvalidation.time != null
                && (marketPath.targetHitAt == null || !persistedInvalidation.time.isAfter(marketPath.targetHitAt))
                && (marketPath.invalidationHitAt == null || persistedInvalidation.time.isBefore(marketPath.invalidationHitAt))) {
            marketPath.invalidationHit = true;
            marketPath.invalidationHitAt = persistedInvalidation.time;
            marketPath.hitOrder = OpportunityLogStatus.INVALIDATION_FIRST;
            marketPath.reasonCodes = appendReason(marketPath.reasonCodes, persistedInvalidation.reasonCode);
        }

        applyMarketPath(row, marketPath);
        RiskEvidence risk = riskEvidence(row);
        row.setRiskBlockedEvidence(risk.blocked);
        row.setRiskBlockedAt(risk.blockedAt);
        row.setReasonCodes(appendReason(appendReason(row, marketPath.reasonCodes), risk.reasonCodes));

        if (OpportunityLogStatus.AMBIGUOUS_SAME_BAR.equals(marketPath.hitOrder)) {
            return updateNonFinal(row, OpportunityLogStatus.AMBIGUOUS_MARKET_PATH,
                    appendReason(row, "AMBIGUOUS_SAME_BAR"));
        }
        if (marketPath.hitOrder == null) {
            return updateNonFinal(row, OpportunityLogStatus.PENDING_EVALUATION,
                    appendReason(row, "NO_TARGET_OR_INVALIDATION_HIT"));
        }

        String finalStatus = classifyShared(marketPath, risk, row);
        row.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        row.setOpportunityStatus(finalStatus);
        row.setResolvedAt(now);
        row.setUpdatedAt(now);
        int updated = opportunityLogMapper.updateEvaluation(row);
        OpportunityLogDO persisted = opportunityLogMapper.selectByOpportunityId(row.getOpportunityId());
        OpportunityLogDTO dto = toSharedDto(persisted);
        dto.setDeduplicated(updated == 0);
        return dto;
    }

    @Override
    public OpportunityLogDTO findByIdForUser(String opportunityId, Long userId) {
        requireUserId(userId);
        OpportunityLogDO row = opportunityLogMapper.selectByOpportunityId(opportunityId);
        return row == null ? null : toUserScopedDto(row, userId);
    }

    @Override
    public OpportunityLogDTO findByIdForSystem(String opportunityId) {
        OpportunityLogDO row = opportunityLogMapper.selectByOpportunityId(opportunityId);
        return row == null ? null : toSharedDto(row);
    }

    @Override
    public OpportunityLogPublicDTO evaluatePublicOpportunityForUser(
            String opportunityId, Long userId, LocalDateTime asOf) {
        return toPublicDto(evaluateOpportunityForUser(opportunityId, userId, asOf));
    }

    @Override
    public OpportunityLogPublicDTO findPublicById(String opportunityId) {
        return opportunityLogMapper.selectPublicApiByOpportunityId(opportunityId);
    }

    @Override
    public List<OpportunityLogPublicDTO> queryPublic(String analysisId,
                                                     String decisionId,
                                                     String executionPlanId,
                                                     String symbol,
                                                     String opportunityStatus,
                                                     String lifecycleStatus,
                                                     LocalDateTime from,
                                                     LocalDateTime to,
                                                     int limit) {
        return opportunityLogMapper.queryPublicApi(
                trimToNull(analysisId),
                trimToNull(decisionId),
                trimToNull(executionPlanId),
                trimToNull(symbol),
                trimToNull(opportunityStatus),
                trimToNull(lifecycleStatus),
                from,
                to,
                sanitizeLimit(limit));
    }

    @Override
    public List<OpportunityLogDTO> queryForUser(Long userId,
                                                String analysisId,
                                                String decisionId,
                                                String executionPlanId,
                                                String symbol,
                                                String opportunityStatus,
                                                String lifecycleStatus,
                                                LocalDateTime from,
                                                LocalDateTime to,
                                                int limit) {
        requireUserId(userId);
        String statusFilter = trimToNull(opportunityStatus);
        String lifecycleFilter = trimToNull(lifecycleStatus);
        int sanitizedLimit = sanitizeLimit(limit);
        int sourceLimit = statusFilter == null && lifecycleFilter == null ? sanitizedLimit : MAX_QUERY_LIMIT;
        return opportunityLogMapper.query(trimToNull(analysisId), trimToNull(decisionId),
                        trimToNull(executionPlanId), trimToNull(symbol), null, null,
                        from, to, sourceLimit)
                .stream()
                .map(row -> toUserScopedDto(row, userId))
                .filter(dto -> statusFilter == null || statusFilter.equals(dto.getOpportunityStatus()))
                .filter(dto -> lifecycleFilter == null || lifecycleFilter.equals(dto.getLifecycleStatus()))
                .limit(sanitizedLimit)
                .toList();
    }

    @Override
    public List<OpportunityLogDTO> queryForSystem(String analysisId,
                                                  String decisionId,
                                                  String executionPlanId,
                                                  String symbol,
                                                  String opportunityStatus,
                                                  String lifecycleStatus,
                                                  LocalDateTime from,
                                                  LocalDateTime to,
                                                  int limit) {
        return opportunityLogMapper.query(trimToNull(analysisId), trimToNull(decisionId), trimToNull(executionPlanId),
                        trimToNull(symbol), trimToNull(opportunityStatus), trimToNull(lifecycleStatus),
                        from, to, sanitizeLimit(limit))
                .stream()
                .map(this::toSharedDto)
                .toList();
    }

    @Override
    public OpportunityLogStatsDTO getStats(String symbol, LocalDateTime from, LocalDateTime to) {
        String normalizedSymbol = trimToNull(symbol);
        OpportunityLogStatsDTO stats = opportunityLogMapper.aggregateStats(normalizedSymbol, from, to);
        if (stats == null) {
            stats = new OpportunityLogStatsDTO();
        }
        stats.setGeneratedAt(LocalDateTime.now());
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> sourceCounts = new LinkedHashMap<>();
        for (OpportunityLogCountRow row : safeList(opportunityLogMapper.countByStatus(normalizedSymbol, from, to))) {
            statusCounts.put(blank(row.getName()) ? "UNKNOWN" : row.getName(), row.getCount() == null ? 0 : row.getCount());
        }
        for (OpportunityLogCountRow row : safeList(opportunityLogMapper.countBySource(normalizedSymbol, from, to))) {
            sourceCounts.put(blank(row.getName()) ? "UNKNOWN" : row.getName(), row.getCount() == null ? 0 : row.getCount());
        }
        int valid = stats.getValidOpportunityCount();
        int invalid = stats.getInvalidOpportunityCount();
        if (valid + invalid > 0) {
            stats.setValidRate(BigDecimal.valueOf(valid).divide(BigDecimal.valueOf(valid + invalid), 8, RoundingMode.HALF_UP));
        }
        stats.setStatusCounts(statusCounts);
        stats.setSourceCounts(sourceCounts);
        return stats;
    }

    private OpportunityLogDO baseRow(AnalysisRunDO run, DecisionResult decision, ExecutionPlanDO plan,
                                     TmPushSnapshotDO push, String direction, String key, String traceId) {
        LocalDateTime now = LocalDateTime.now();
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId("opp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        row.setOpportunityKey(key);
        row.setAnalysisId(run.getAnalysisId());
        row.setDecisionId(decision.getDecisionId());
        row.setExecutionPlanId(plan != null ? plan.getPlanId() : null);
        row.setPushId(push != null ? push.getPushId() : null);
        row.setSymbol(normalizeSymbol(firstNonBlank(run.getSymbol(), decision.getSymbol())));
        row.setTimeframe(firstNonBlank(run.getTimeframe(), push != null ? push.getTimeframe() : null, "UNKNOWN"));
        row.setDirection(direction);
        row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
        row.setAnchorTime(firstNonNull(run.getAnalysisTime(), decision.getCreateTime(), plan != null ? plan.getCreateTime() : null, now));
        row.setPushPresent(push != null);
        row.setRiskBlockedEvidence(false);
        row.setUserPositionPresent(false);
        row.setSourceType("AUTHORITATIVE_ANALYSIS");
        row.setSourceReference("analysisId=" + run.getAnalysisId() + ";decisionId=" + decision.getDecisionId());
        row.setTraceId(firstNonBlank(traceId, run.getTraceId()));
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }

    private boolean isPlanCandidateReady(ExecutionPlanDO plan, DecisionResult decision) {
        return plan != null
                && !Boolean.TRUE.equals(decision.getHotResetInvalidated())
                && !Boolean.TRUE.equals(plan.getNeedsRevalidation())
                && Boolean.TRUE.equals(plan.getSourceGateComplete())
                && "VALID".equalsIgnoreCase(safe(plan.getSourceGateStatus()))
                && "VALID".equalsIgnoreCase(safe(plan.getExecutionPlanStatus()));
    }

    private BoundaryResolution resolveBoundaries(ExecutionPlanDO plan, TmPushSnapshotDO push, String direction) {
        BoundaryResolution r = new BoundaryResolution();
        r.entryReference = UserPositionReviewPolicy.parseSingleNumberOrRangeMidpoint(plan.getEntryZone());
        if (r.entryReference == null && push != null) {
            r.entryReference = positiveOrNull(push.getTriggerPrice());
        }
        r.targetPrice = UserPositionReviewPolicy.parseSingleNumberOrRangeMidpoint(plan.getTakeProfitRules());
        r.invalidationPrice = UserPositionReviewPolicy.parseSingleNumberOrRangeMidpoint(plan.getStopLoss());
        if (r.invalidationPrice == null && push != null) {
            r.invalidationPrice = parseInvalidationCondition(push.getInvalidationConditionJson());
        }
        List<String> reasons = new ArrayList<>();
        if (r.entryReference == null) {
            reasons.add("ENTRY_REFERENCE_NOT_COMPUTABLE");
        }
        if (r.targetPrice == null) {
            reasons.add("TARGET_PRICE_NOT_COMPUTABLE");
        }
        if (r.invalidationPrice == null) {
            reasons.add("INVALIDATION_PRICE_NOT_COMPUTABLE");
        }
        if (r.entryReference != null && r.targetPrice != null && r.invalidationPrice != null
                && !validDirectionalBoundaries(direction, r.entryReference, r.targetPrice, r.invalidationPrice)) {
            reasons.add("DIRECTIONAL_BOUNDARY_INVALID");
        }
        r.complete = reasons.isEmpty();
        r.reasonCodes = reason(reasons.toArray(String[]::new));
        return r;
    }

    private MarketPathResult scanMarketPath(OpportunityLogDO row, LocalDateTime evaluationEnd) {
        MarketPathResult result = new MarketPathResult();
        long startMs = toEpochMs(row.getAnchorTime());
        long endMs = toEpochMs(evaluationEnd);
        List<PersistedOhlcvBarDO> bars = persistedOhlcvBarMapper.selectClosedBarsBetween(
                row.getSymbol(), row.getTimeframe(), startMs, endMs, MAX_MARKET_BARS + 1);
        if (bars == null || bars.isEmpty()) {
            result.reasonCodes = reason("NO_PERSISTED_MARKET_PATH");
            return result;
        }
        if (bars.size() > MAX_MARKET_BARS) {
            result.reasonCodes = reason("MARKET_PATH_LIMIT_EXCEEDED");
            return result;
        }
        result.available = true;
        result.marketDataSource = bars.get(0).getProvider();
        result.marketDataTraceId = bars.get(0).getSourceTraceId();
        for (PersistedOhlcvBarDO bar : bars) {
            if (!validBar(bar)) {
                result.available = false;
                result.reasonCodes = reason("INVALID_MARKET_BAR");
                return result;
            }
            updateExcursions(row, result, bar);
            boolean target = targetHit(row, bar);
            boolean invalidation = invalidationHit(row, bar);
            LocalDateTime hitAt = fromEpochMs(bar.getOpenTimeMs());
            if (target && invalidation && result.hitOrder == null) {
                result.targetHit = true;
                result.invalidationHit = true;
                result.targetHitAt = hitAt;
                result.invalidationHitAt = hitAt;
                result.hitOrder = OpportunityLogStatus.AMBIGUOUS_SAME_BAR;
                result.reasonCodes = reason("AMBIGUOUS_SAME_BAR");
                return result;
            }
            if (target && result.hitOrder == null) {
                result.targetHit = true;
                result.targetHitAt = hitAt;
                result.hitOrder = OpportunityLogStatus.TARGET_FIRST;
                result.reasonCodes = reason("TARGET_FIRST");
                return result;
            }
            if (invalidation && result.hitOrder == null) {
                result.invalidationHit = true;
                result.invalidationHitAt = hitAt;
                result.hitOrder = OpportunityLogStatus.INVALIDATION_FIRST;
                result.reasonCodes = reason("INVALIDATION_FIRST");
                return result;
            }
        }
        result.reasonCodes = reason("NO_TARGET_OR_INVALIDATION_HIT");
        return result;
    }

    private LinkedUserPosition resolveLinkedUserPositionForUser(OpportunityLogDO row, Long userId) {
        requireUserId(userId);
        try {
            List<UserPositionDO> rows = new ArrayList<>();
            if (!blank(row.getExecutionPlanId())) {
                rows = safeList(userPositionMapper.listByExactSourceRefIdAndUserId(row.getExecutionPlanId(), userId));
            }
            if (rows.isEmpty() && !blank(row.getAnalysisId())) {
                rows = safeList(userPositionMapper.listByExactSourceRefIdAndUserId(row.getAnalysisId(), userId));
            }
            if (rows.size() > 1) {
                return new LinkedUserPosition(null, true, false);
            }
            return new LinkedUserPosition(rows.isEmpty() ? null : rows.get(0), false, false);
        } catch (RuntimeException ignored) {
            return new LinkedUserPosition(null, false, true);
        }
    }

    private OpportunityLogDTO toUserScopedDto(OpportunityLogDO row, Long userId) {
        LinkedUserPosition linked = resolveLinkedUserPositionForUser(row, userId);
        LocalDateTime firstOutcomeTime = firstOutcomeTime(row);
        boolean present = linked.position != null
                && linked.position.getOpenedAt() != null
                && firstOutcomeTime != null
                && !linked.position.getOpenedAt().isAfter(firstOutcomeTime);

        OpportunityLogDTO dto = toSharedDto(row);
        dto.setEvaluationAsOf(null);
        dto.setUserPositionPresent(present);
        dto.setUserPositionId(present ? linked.position.getId() : null);
        dto.setReasonCodes(userScopedReasonCodes(row.getReasonCodes(), linked, firstOutcomeTime));
        if (requiresUserReview(dto.getLifecycleStatus(), linked)) {
            dto.setLifecycleStatus(OpportunityLogStatus.REVIEW_REQUIRED);
            dto.setOpportunityStatus(null);
        } else if (OpportunityLogStatus.RESOLVED.equals(dto.getLifecycleStatus())) {
            dto.setOpportunityStatus(classifyForUser(row, present));
        }
        return dto;
    }

    private OpportunityLogDTO toSharedDto(OpportunityLogDO row) {
        boolean userDerived = hasPersistedUserDerivedState(row);
        OpportunityLogDTO dto = OpportunityLogDTO.from(row);
        dto.setUserPositionId(null);
        dto.setUserPositionPresent(false);
        dto.setReasonCodes(sharedReasonCodes(row.getReasonCodes()));
        if (!isSharedLifecycleStatus(row.getLifecycleStatus())) {
            dto.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
            dto.setOpportunityStatus(null);
            dto.setResolvedAt(null);
        } else if (!isSharedOpportunityStatus(row.getOpportunityStatus())) {
            dto.setOpportunityStatus(OpportunityLogStatus.RESOLVED.equals(row.getLifecycleStatus())
                    ? classifyForUser(row, false)
                    : null);
        }
        if (userDerived) {
            dto.setEvaluationAsOf(null);
        }
        return dto;
    }

    private static OpportunityLogPublicDTO toPublicDto(OpportunityLogDTO source) {
        if (source == null) {
            return null;
        }
        String lifecycleStatus = OpportunityLogStatus.REVIEW_REQUIRED.equals(source.getLifecycleStatus())
                ? OpportunityLogStatus.PENDING_EVALUATION
                : source.getLifecycleStatus();
        String opportunityStatus = isSharedOpportunityStatus(source.getOpportunityStatus())
                ? source.getOpportunityStatus()
                : null;
        return new OpportunityLogPublicDTO(
                source.getOpportunityId(),
                source.getAnalysisId(),
                source.getSymbol(),
                source.getTimeframe(),
                source.getDirection(),
                lifecycleStatus,
                opportunityStatus,
                source.getAnchorTime(),
                source.getResolvedAt(),
                source.getEntryReference(),
                source.getTargetPrice(),
                source.getInvalidationPrice(),
                source.getTargetHit(),
                source.getInvalidationHit(),
                source.getTargetHitAt(),
                source.getInvalidationHitAt(),
                source.getHitOrder(),
                source.getMfePrice(),
                source.getMfeRatio(),
                source.getMaePrice(),
                source.getMaeRatio(),
                source.getMarketDataSource(),
                source.getCreatedAt(),
                source.getUpdatedAt(),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    private void normalizeSharedState(OpportunityLogDO row) {
        boolean userDerived = hasPersistedUserDerivedState(row);
        row.setUserPositionId(null);
        row.setUserPositionPresent(false);
        row.setReasonCodes(sharedReasonCodes(row.getReasonCodes()));
        if (!isSharedLifecycleStatus(row.getLifecycleStatus())) {
            row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
            row.setOpportunityStatus(null);
            row.setResolvedAt(null);
        } else if (!isSharedOpportunityStatus(row.getOpportunityStatus())) {
            row.setOpportunityStatus(OpportunityLogStatus.RESOLVED.equals(row.getLifecycleStatus())
                    ? classifyForUser(row, false)
                    : null);
        }
        if (userDerived) {
            row.setEvaluationAsOf(null);
        }
    }

    private static boolean requiresUserReview(String sharedLifecycleStatus, LinkedUserPosition linked) {
        if (OpportunityLogStatus.SOURCE_INCOMPLETE.equals(sharedLifecycleStatus)) {
            return false;
        }
        return linked.reviewRequired
                || linked.lookupFailed
                || linked.position != null && linked.position.getOpenedAt() == null;
    }

    private String classifyForUser(OpportunityLogDO row, boolean userPositionPresent) {
        if (OpportunityLogStatus.TARGET_FIRST.equals(row.getHitOrder())) {
            if (userPositionPresent) {
                return OpportunityLogStatus.EXECUTED_VALID;
            }
            if (Boolean.TRUE.equals(row.getRiskBlockedEvidence())
                    && row.getRiskBlockedAt() != null
                    && row.getTargetHitAt() != null
                    && !row.getRiskBlockedAt().isAfter(row.getTargetHitAt())) {
                return OpportunityLogStatus.BLOCKED_BY_RISK_VALID;
            }
            return Boolean.TRUE.equals(row.getPushPresent())
                    ? OpportunityLogStatus.PUSHED_NOT_FILLED_VALID
                    : OpportunityLogStatus.MISSED_VALID;
        }
        if (OpportunityLogStatus.INVALIDATION_FIRST.equals(row.getHitOrder())) {
            return userPositionPresent
                    ? OpportunityLogStatus.EXECUTED_INVALID
                    : OpportunityLogStatus.MISSED_INVALID;
        }
        return null;
    }

    private static String userScopedReasonCodes(String existing,
                                                LinkedUserPosition linked,
                                                LocalDateTime firstOutcomeTime) {
        List<String> reasons = new ArrayList<>();
        if (!blank(existing)) {
            for (String value : existing.split(",")) {
                String reason = value.trim();
                if (!blank(reason) && !isUserPositionReason(reason)) {
                    reasons.add(reason);
                }
            }
        }
        if (linked.reviewRequired) {
            reasons.add("MULTIPLE_LINKED_USER_POSITIONS");
        } else if (linked.lookupFailed) {
            reasons.add("USER_POSITION_PROJECTION_UNAVAILABLE");
        } else if (linked.position != null && linked.position.getOpenedAt() == null) {
            reasons.add("LINKED_USER_POSITION_OPEN_TIME_MISSING");
        } else if (linked.position != null && firstOutcomeTime != null
                && linked.position.getOpenedAt().isAfter(firstOutcomeTime)) {
            reasons.add("LINKED_USER_POSITION_OPENED_AFTER_OUTCOME");
        }
        return String.join(",", reasons);
    }

    private static boolean isUserPositionReason(String reason) {
        return "MULTIPLE_LINKED_USER_POSITIONS".equals(reason)
                || "LINKED_USER_POSITION_OPEN_TIME_MISSING".equals(reason)
                || "LINKED_USER_POSITION_OPENED_AFTER_OUTCOME".equals(reason)
                || "USER_POSITION_PROJECTION_UNAVAILABLE".equals(reason);
    }

    private static String sharedReasonCodes(String existing) {
        if (blank(existing)) {
            return existing;
        }
        List<String> reasons = new ArrayList<>();
        for (String value : existing.split(",")) {
            String reason = value.trim();
            if (!blank(reason) && !isUserPositionReason(reason)) {
                reasons.add(reason);
            }
        }
        return String.join(",", reasons);
    }

    private static boolean hasPersistedUserDerivedState(OpportunityLogDO row) {
        if (row.getUserPositionId() != null || Boolean.TRUE.equals(row.getUserPositionPresent())) {
            return true;
        }
        if (!isSharedLifecycleStatus(row.getLifecycleStatus())
                || !isSharedOpportunityStatus(row.getOpportunityStatus())) {
            return true;
        }
        if (!blank(row.getReasonCodes())) {
            for (String value : row.getReasonCodes().split(",")) {
                if (isUserPositionReason(value.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSharedLifecycleStatus(String status) {
        return status == null
                || OpportunityLogStatus.PENDING_EVALUATION.equals(status)
                || OpportunityLogStatus.RESOLVED.equals(status)
                || OpportunityLogStatus.SOURCE_INCOMPLETE.equals(status)
                || OpportunityLogStatus.MARKET_PATH_UNAVAILABLE.equals(status)
                || OpportunityLogStatus.AMBIGUOUS_MARKET_PATH.equals(status);
    }

    private static boolean isSharedOpportunityStatus(String status) {
        return status == null
                || OpportunityLogStatus.MISSED_VALID.equals(status)
                || OpportunityLogStatus.MISSED_INVALID.equals(status)
                || OpportunityLogStatus.PUSHED_NOT_FILLED_VALID.equals(status)
                || OpportunityLogStatus.BLOCKED_BY_RISK_VALID.equals(status);
    }

    private static LocalDateTime firstOutcomeTime(OpportunityLogDO row) {
        if (OpportunityLogStatus.TARGET_FIRST.equals(row.getHitOrder())) {
            return row.getTargetHitAt();
        }
        if (OpportunityLogStatus.INVALIDATION_FIRST.equals(row.getHitOrder())) {
            return row.getInvalidationHitAt();
        }
        return null;
    }

    private RiskEvidence riskEvidence(OpportunityLogDO row) {
        RiskEvidence risk = new RiskEvidence();
        TmAccountRiskSnapshotDO snapshot = accountRiskSnapshotMapper.selectLatestByAnalysisId(row.getAnalysisId());
        if (snapshot != null && Boolean.FALSE.equals(snapshot.getRiskAllowed())) {
            risk.blocked = true;
            risk.blockedAt = snapshot.getCreateTime();
            risk.reasonCodes = reason("ACCOUNT_RISK_BLOCKED", snapshot.getRiskReasonCode());
        }
        List<TmPushSnapshotDO> pushes = safeList(pushSnapshotMapper.listByAnalysisId(row.getAnalysisId()));
        for (TmPushSnapshotDO push : pushes) {
            for (TmPushRecheckLogDO log : safeList(pushRecheckLogMapper.selectByPushId(push.getPushId()))) {
                if ("RISK_BLOCKED".equalsIgnoreCase(safe(log.getRecheckStatus()))) {
                    risk.blocked = true;
                    LocalDateTime t = firstNonNull(log.getRecheckTime(), log.getCreateTime());
                    risk.blockedAt = earlier(risk.blockedAt, t);
                    risk.reasonCodes = appendReason(risk.reasonCodes, "PUSH_RECHECK_RISK_BLOCKED");
                }
            }
        }
        return risk;
    }

    private InvalidationEvidence persistedInvalidationEvidence(OpportunityLogDO row, LocalDateTime evaluationEnd) {
        InvalidationEvidence evidence = new InvalidationEvidence();
        DecisionResult decision = !blank(row.getDecisionId()) ? decisionResultMapper.selectByDecisionId(row.getDecisionId()) : null;
        if (decision != null && Boolean.TRUE.equals(decision.getHotResetInvalidated())
                && decision.getHotResetInvalidatedAt() != null) {
            applyPersistedInvalidationCandidate(evidence, decision.getHotResetInvalidatedAt(),
                    firstNonBlank(decision.getHotResetReasonCode(), "DECISION_HOT_RESET_INVALIDATED"), evaluationEnd);
        }
        ExecutionPlanDO plan = !blank(row.getExecutionPlanId()) ? executionPlanMapper.selectByPlanId(row.getExecutionPlanId()) : null;
        if (plan != null && Boolean.TRUE.equals(plan.getNeedsRevalidation()) && plan.getRevalidationRequiredAt() != null) {
            applyPersistedInvalidationCandidate(evidence, plan.getRevalidationRequiredAt(),
                    "EXECUTION_PLAN_NEEDS_REVALIDATION", evaluationEnd);
        }
        return evidence;
    }

    private void applyPersistedInvalidationCandidate(InvalidationEvidence evidence,
                                                     LocalDateTime candidateTime,
                                                     String reasonCode,
                                                     LocalDateTime evaluationEnd) {
        if (candidateTime == null) {
            return;
        }
        if (evaluationEnd != null && candidateTime.isAfter(evaluationEnd)) {
            evidence.ignoredReasonCode = appendReason(evidence.ignoredReasonCode,
                    "PERSISTED_INVALIDATION_AFTER_AS_OF_IGNORED");
            return;
        }
        if (evidence.time == null || candidateTime.isBefore(evidence.time)) {
            evidence.time = candidateTime;
        }
        evidence.reasonCode = appendReason(evidence.reasonCode, reasonCode);
    }

    private String classifyShared(MarketPathResult marketPath, RiskEvidence risk, OpportunityLogDO row) {
        if (OpportunityLogStatus.TARGET_FIRST.equals(marketPath.hitOrder)) {
            if (risk.blocked && risk.blockedAt != null && !risk.blockedAt.isAfter(marketPath.targetHitAt)) {
                return OpportunityLogStatus.BLOCKED_BY_RISK_VALID;
            }
            if (Boolean.TRUE.equals(row.getPushPresent())) {
                return OpportunityLogStatus.PUSHED_NOT_FILLED_VALID;
            }
            return OpportunityLogStatus.MISSED_VALID;
        }
        if (OpportunityLogStatus.INVALIDATION_FIRST.equals(marketPath.hitOrder)) {
            return OpportunityLogStatus.MISSED_INVALID;
        }
        throw new IllegalStateException("opportunity has no final hit order");
    }

    private void applyMarketPath(OpportunityLogDO row, MarketPathResult result) {
        row.setTargetHit(result.targetHit);
        row.setInvalidationHit(result.invalidationHit);
        row.setTargetHitAt(result.targetHitAt);
        row.setInvalidationHitAt(result.invalidationHitAt);
        row.setHitOrder(result.hitOrder);
        row.setMfePrice(result.mfePrice);
        row.setMfeRatio(result.mfeRatio);
        row.setMaePrice(result.maePrice);
        row.setMaeRatio(result.maeRatio);
        row.setMarketDataSource(result.marketDataSource);
        row.setMarketDataTraceId(result.marketDataTraceId);
    }

    private OpportunityLogDTO updateNonFinal(OpportunityLogDO row, String lifecycleStatus, String reasons) {
        row.setLifecycleStatus(lifecycleStatus);
        row.setOpportunityStatus(null);
        row.setReasonCodes(reasons);
        row.setUpdatedAt(LocalDateTime.now());
        opportunityLogMapper.updateEvaluation(row);
        return toSharedDto(opportunityLogMapper.selectByOpportunityId(row.getOpportunityId()));
    }

    private OpportunityLogDO requireOpportunity(String opportunityId) {
        OpportunityLogDO row = opportunityLogMapper.selectByOpportunityId(opportunityId);
        if (row == null) {
            throw new IllegalArgumentException("OPPORTUNITY_NOT_FOUND");
        }
        return row;
    }

    private boolean hasCompleteSource(OpportunityLogDO row) {
        return positive(row.getEntryReference()) && positive(row.getTargetPrice()) && positive(row.getInvalidationPrice())
                && validDirectionalBoundaries(row.getDirection(), row.getEntryReference(), row.getTargetPrice(), row.getInvalidationPrice());
    }

    private boolean targetHit(OpportunityLogDO row, PersistedOhlcvBarDO bar) {
        if ("LONG".equals(row.getDirection())) {
            return bar.getHighPrice().compareTo(row.getTargetPrice()) >= 0;
        }
        return bar.getLowPrice().compareTo(row.getTargetPrice()) <= 0;
    }

    private boolean invalidationHit(OpportunityLogDO row, PersistedOhlcvBarDO bar) {
        if ("LONG".equals(row.getDirection())) {
            return bar.getLowPrice().compareTo(row.getInvalidationPrice()) <= 0;
        }
        return bar.getHighPrice().compareTo(row.getInvalidationPrice()) >= 0;
    }

    private void updateExcursions(OpportunityLogDO row, MarketPathResult result, PersistedOhlcvBarDO bar) {
        BigDecimal entry = row.getEntryReference();
        if ("LONG".equals(row.getDirection())) {
            result.mfePrice = result.mfePrice == null ? bar.getHighPrice() : max(result.mfePrice, bar.getHighPrice());
            result.maePrice = result.maePrice == null ? bar.getLowPrice() : min(result.maePrice, bar.getLowPrice());
            result.mfeRatio = max(ZERO, result.mfePrice.subtract(entry).divide(entry, 10, RoundingMode.HALF_UP));
            result.maeRatio = max(ZERO, entry.subtract(result.maePrice).divide(entry, 10, RoundingMode.HALF_UP));
        } else {
            result.mfePrice = result.mfePrice == null ? bar.getLowPrice() : min(result.mfePrice, bar.getLowPrice());
            result.maePrice = result.maePrice == null ? bar.getHighPrice() : max(result.maePrice, bar.getHighPrice());
            result.mfeRatio = max(ZERO, entry.subtract(result.mfePrice).divide(entry, 10, RoundingMode.HALF_UP));
            result.maeRatio = max(ZERO, result.maePrice.subtract(entry).divide(entry, 10, RoundingMode.HALF_UP));
        }
    }

    private static String directionFromRuleBias(String marketBiasHierarchy) {
        String b = safe(marketBiasHierarchy).toUpperCase(Locale.ROOT);
        if (b.endsWith("BULLISH")) {
            return "LONG";
        }
        if (b.endsWith("BEARISH")) {
            return "SHORT";
        }
        return null;
    }

    private static boolean validDirectionalBoundaries(String direction, BigDecimal entry, BigDecimal target, BigDecimal invalidation) {
        if ("LONG".equals(direction)) {
            return target.compareTo(entry) > 0 && invalidation.compareTo(entry) < 0;
        }
        if ("SHORT".equals(direction)) {
            return target.compareTo(entry) < 0 && invalidation.compareTo(entry) > 0;
        }
        return false;
    }

    private static BigDecimal parseInvalidationCondition(String raw) {
        if (blank(raw)) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(raw);
            return Optional.ofNullable(root.get("invalidPriceBelow"))
                    .or(() -> Optional.ofNullable(root.get("invalidPriceAbove")))
                    .filter(JsonNode::isNumber)
                    .map(JsonNode::decimalValue)
                    .filter(OpportunityLogServiceImpl::positive)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean validBar(PersistedOhlcvBarDO bar) {
        return bar != null && Boolean.TRUE.equals(bar.getClosed())
                && positive(bar.getOpenPrice()) && positive(bar.getHighPrice())
                && positive(bar.getLowPrice()) && positive(bar.getClosePrice())
                && bar.getVolume() != null && bar.getVolume().compareTo(BigDecimal.ZERO) >= 0
                && bar.getOpenTimeMs() != null && bar.getCloseTimeMs() != null
                && bar.getOpenTimeMs() >= 0 && bar.getCloseTimeMs() > bar.getOpenTimeMs()
                && bar.getHighPrice().compareTo(bar.getLowPrice()) >= 0
                && bar.getHighPrice().compareTo(bar.getOpenPrice()) >= 0
                && bar.getHighPrice().compareTo(bar.getClosePrice()) >= 0
                && bar.getLowPrice().compareTo(bar.getOpenPrice()) <= 0
                && bar.getLowPrice().compareTo(bar.getClosePrice()) <= 0
                && "OK".equals(bar.getQualityStatus())
                && "READY".equals(bar.getSourceStatus())
                && "FRESH".equals(bar.getFreshnessStatus())
                && !blank(bar.getProvider())
                && !blank(bar.getProviderMarketType())
                && !blank(bar.getSourceEndpoint())
                && !blank(bar.getSourceBatchId())
                && !blank(bar.getSourceTraceId())
                && bar.getSourceVersion() != null
                && bar.getFetchTime() != null
                && !blank(bar.getProvenanceVersion())
                && !blank(bar.getIngestionRunId())
                && bar.getIngestedAt() != null;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal positiveOrNull(BigDecimal value) {
        return positive(value) ? value : null;
    }

    private static BigDecimal max(BigDecimal a, BigDecimal b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static LocalDateTime earlier(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!blank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String appendReason(OpportunityLogDO row, String reason) {
        return appendReason(row.getReasonCodes(), reason);
    }

    private static String appendReason(String existing, String reason) {
        if (blank(reason)) {
            return existing;
        }
        if (blank(existing)) {
            return reason;
        }
        if (existing.contains(reason)) {
            return existing;
        }
        return existing + "," + reason;
    }

    private static String reason(String... reasons) {
        List<String> clean = new ArrayList<>();
        if (reasons != null) {
            for (String r : reasons) {
                if (!blank(r)) {
                    clean.add(r.trim());
                }
            }
        }
        return String.join(",", clean);
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeSymbol(String symbol) {
        return blank(symbol) ? null : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_QUERY_LIMIT;
        }
        return Math.min(limit, MAX_QUERY_LIMIT);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private static long toEpochMs(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalDateTime fromEpochMs(Long ms) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
    }

    private static void bump(Map<String, Integer> counts, String key) {
        String k = blank(key) ? "UNKNOWN" : key;
        counts.put(k, counts.getOrDefault(k, 0) + 1);
    }

    private static final class BoundaryResolution {
        private BigDecimal entryReference;
        private BigDecimal targetPrice;
        private BigDecimal invalidationPrice;
        private boolean complete;
        private String reasonCodes;
    }

    private static final class MarketPathResult {
        private boolean available;
        private boolean targetHit;
        private boolean invalidationHit;
        private LocalDateTime targetHitAt;
        private LocalDateTime invalidationHitAt;
        private String hitOrder;
        private BigDecimal mfePrice;
        private BigDecimal mfeRatio;
        private BigDecimal maePrice;
        private BigDecimal maeRatio;
        private String marketDataSource;
        private String marketDataTraceId;
        private String reasonCodes;
    }

    private static final class LinkedUserPosition {
        private final UserPositionDO position;
        private final boolean reviewRequired;
        private final boolean lookupFailed;

        private LinkedUserPosition(UserPositionDO position, boolean reviewRequired, boolean lookupFailed) {
            this.position = position;
            this.reviewRequired = reviewRequired;
            this.lookupFailed = lookupFailed;
        }
    }

    private static final class RiskEvidence {
        private boolean blocked;
        private LocalDateTime blockedAt;
        private String reasonCodes;
    }

    private static final class InvalidationEvidence {
        private LocalDateTime time;
        private String reasonCode;
        private String ignoredReasonCode;
    }
}
