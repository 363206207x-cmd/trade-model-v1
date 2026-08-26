package org.example.trademodel.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.derivatives.DerivativesBusinessInput;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorDataStateEnum;
import org.example.trademodel.positionmonitor.PositionPlanSourceResolver;
import org.example.trademodel.positionmonitor.PositionMonitorPolicy;
import org.example.trademodel.positionmonitor.PositionReversalEvaluator;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.positionmonitor.SinglePositionRiskCalculator;
import org.example.trademodel.positionmonitorlog.PositionEntryLogicStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorConclusionEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorSourceStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorSuggestedActionEnum;
import org.example.trademodel.positionmonitorlog.PositionReversalStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionRiskChangeReasonEnum;
import org.example.trademodel.positionmonitorlog.PositionRiskTrendEnum;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.market.util.BinanceUsdtSymbol;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.telegram.HighValueAlertMessageService;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy.PersistedPlanState;
import org.example.trademodel.service.support.DataQualityCircuitBreakerPolicy;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.ExternalContextSnapshot;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PositionMonitorServiceImpl implements PositionMonitorService {
    private final UserPositionMapper userPositionMapper;
    private final MarketPriceSnapshotService marketPriceSnapshotService;
    private final SinglePositionRiskCalculator singlePositionRiskCalculator;
    private final PositionReversalEvaluator positionReversalEvaluator;
    private final PositionPlanSourceResolver positionPlanSourceResolver;
    private final PositionMonitorLogService positionMonitorLogService;
    private final EvidenceItemMapper evidenceItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final ObjectMapper objectMapper;
    private final ExternalContextEvidenceBuilder externalContextEvidenceBuilder;
    private DerivativesSnapshotReadPort derivativesSnapshotReadPort;
    private DerivativesBusinessIntegrationService derivativesBusinessIntegrationService;
    private HighValueAlertMessageService highValueAlertMessageService;
    private PersistedOhlcvQueryService persistedOhlcvQueryService;
    private final Set<Long> activeSystemMonitorClaims = ConcurrentHashMap.newKeySet();

    public PositionMonitorServiceImpl(UserPositionMapper userPositionMapper,
                                      MarketPriceSnapshotService marketPriceSnapshotService,
                                      UserPositionRiskAdapter ignoredAggregateRiskAdapter,
                                      ExecutionPlanMapper executionPlanMapper,
                                      PositionMonitorLogService positionMonitorLogService,
                                      EvidenceItemMapper evidenceItemMapper,
                                      ScoreItemMapper scoreItemMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      ObjectMapper objectMapper) {
        this(userPositionMapper, marketPriceSnapshotService, ignoredAggregateRiskAdapter, executionPlanMapper,
                positionMonitorLogService, evidenceItemMapper, scoreItemMapper, decisionResultMapper,
                objectMapper, null, null);
    }

    public PositionMonitorServiceImpl(UserPositionMapper userPositionMapper,
                                      MarketPriceSnapshotService marketPriceSnapshotService,
                                      UserPositionRiskAdapter ignoredAggregateRiskAdapter,
                                      ExecutionPlanMapper executionPlanMapper,
                                      PositionMonitorLogService positionMonitorLogService,
                                      EvidenceItemMapper evidenceItemMapper,
                                      ScoreItemMapper scoreItemMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      ObjectMapper objectMapper,
                                      ExternalContextEvidenceBuilder externalContextEvidenceBuilder) {
        this(userPositionMapper, marketPriceSnapshotService, ignoredAggregateRiskAdapter, executionPlanMapper,
                positionMonitorLogService, evidenceItemMapper, scoreItemMapper, decisionResultMapper,
                objectMapper, null, externalContextEvidenceBuilder);
    }

    @Autowired
    public PositionMonitorServiceImpl(UserPositionMapper userPositionMapper,
                                      MarketPriceSnapshotService marketPriceSnapshotService,
                                      UserPositionRiskAdapter ignoredAggregateRiskAdapter,
                                      ExecutionPlanMapper executionPlanMapper,
                                      PositionMonitorLogService positionMonitorLogService,
                                      EvidenceItemMapper evidenceItemMapper,
                                      ScoreItemMapper scoreItemMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      ObjectMapper objectMapper,
                                      AnalysisRunMapper analysisRunMapper,
                                      ExternalContextEvidenceBuilder externalContextEvidenceBuilder) {
        this.userPositionMapper = userPositionMapper;
        this.marketPriceSnapshotService = marketPriceSnapshotService;
        this.singlePositionRiskCalculator = new SinglePositionRiskCalculator();
        this.positionReversalEvaluator = new PositionReversalEvaluator();
        this.positionPlanSourceResolver = new PositionPlanSourceResolver(executionPlanMapper, analysisRunMapper);
        this.positionMonitorLogService = positionMonitorLogService;
        this.evidenceItemMapper = evidenceItemMapper;
        this.scoreItemMapper = scoreItemMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.objectMapper = objectMapper;
        this.externalContextEvidenceBuilder = externalContextEvidenceBuilder;
    }

    @Autowired(required = false)
    void setDerivativesBusinessIntegration(DerivativesSnapshotReadPort derivativesSnapshotReadPort,
                                           DerivativesBusinessIntegrationService derivativesBusinessIntegrationService) {
        this.derivativesSnapshotReadPort = derivativesSnapshotReadPort;
        this.derivativesBusinessIntegrationService = derivativesBusinessIntegrationService;
    }

    @Autowired(required = false)
    void setHighValueAlertMessageService(HighValueAlertMessageService value) {
        this.highValueAlertMessageService = value;
    }

    @Autowired(required = false)
    public void setPersistedOhlcvQueryService(PersistedOhlcvQueryService value) {
        this.persistedOhlcvQueryService = value;
    }

    @Override
    public PositionMonitorResultDTO monitorUserPositionForUser(Long positionId, Long userId) {
        if (positionId == null || positionId <= 0) {
            throw new IllegalArgumentException("position_id is required");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        UserPositionDO position = userPositionMapper.selectByIdAndUserId(positionId, userId);
        if (position == null) {
            throw new UserPositionNotFoundException();
        }
        return monitorActivePosition(position, userId, false);
    }

    @Override
    public PositionMonitorBatchResultDTO monitorClaimedOpenPositionsForSystem() {
        List<UserPositionDO> positions = Optional.ofNullable(
                userPositionMapper.listClaimedOpenForSystemMonitoring()).orElse(List.of());
        List<PositionMonitorResultDTO> results = new ArrayList<>();
        List<PositionMonitorBatchResultDTO.FailureItem> failures = new ArrayList<>();
        int blockedCount = 0;
        for (UserPositionDO position : positions) {
            Long positionId = position == null ? null : position.getId();
            if (positionId == null || !activeSystemMonitorClaims.add(positionId)) {
                failures.add(new PositionMonitorBatchResultDTO.FailureItem(
                        positionId, position == null ? null : position.getAssetSymbol(),
                        "POSITION_MONITOR_ALREADY_RUNNING"));
                continue;
            }
            try {
                PositionMonitorResultDTO result = monitorActivePosition(
                        position, position == null ? null : position.getUserId(), true);
                results.add(result);
                if (result.isRiskBlocked() || "EXTREME".equals(result.getRiskLevel())) {
                    blockedCount++;
                }
            } catch (UserPositionConflictException | UserPositionNotFoundException
                     | PositionMonitorDataUnavailableException ex) {
                failures.add(new PositionMonitorBatchResultDTO.FailureItem(
                        position == null ? null : position.getId(),
                        position == null ? null : position.getAssetSymbol(),
                        ex.getMessage()));
            } catch (RuntimeException ex) {
                failures.add(new PositionMonitorBatchResultDTO.FailureItem(
                        position == null ? null : position.getId(),
                        position == null ? null : position.getAssetSymbol(),
                        "POSITION_MONITOR_FAILED:" + ex.getClass().getSimpleName()));
            } finally {
                activeSystemMonitorClaims.remove(positionId);
            }
        }
        PositionMonitorBatchResultDTO batch = new PositionMonitorBatchResultDTO();
        batch.setTotalCount(positions.size());
        batch.setSuccessCount(results.size());
        batch.setFailureCount(failures.size());
        batch.setBlockedCount(blockedCount);
        batch.setResults(results);
        batch.setFailures(failures);
        return batch;
    }

    private PositionMonitorResultDTO monitorActivePosition(UserPositionDO position, Long userId, boolean systemScope) {
        validateActivePosition(position);
        String side = normalize(position.getSide());
        if (!"LONG".equals(side) && !"SHORT".equals(side)) {
            throw new IllegalArgumentException("UserPosition side must be LONG or SHORT");
        }
        String assetSymbol = requireText(position.getAssetSymbol(), "asset_symbol");
        if (systemScope) {
            requireBinanceClosedWindows(assetSymbol);
        }
        MarkPriceContext markPrice = readMarkPrice(assetSymbol);
        DerivativesBusinessAssessment derivativesAssessment = readDerivativesAssessment(
                position, side, markPrice.price());
        Set<String> reasons = new LinkedHashSet<>();
        if (derivativesAssessment != null) {
            reasons.addAll(derivativesAssessment.reasonCodes());
        }

        PlanContext planContext = resolvePlanContext(position, reasons);
        MonitorEvidenceContext monitorEvidence = resolveMonitorEvidenceContext(assetSymbol, markPrice);
        if (monitorEvidence.reasonCode() != null) {
            reasons.add(monitorEvidence.reasonCode());
        }
        String currentAnalysisId = monitorEvidence.analysisId() == null
                ? planContext.analysisId : monitorEvidence.analysisId();
        ExternalContextSnapshot externalContext = resolveExternalContext(assetSymbol, currentAnalysisId);
        boolean externalSourceBlocked = ExternalContextPolicy.SOURCE_HEALTH_BLOCKED.equalsIgnoreCase(
                externalContext.getSourceHealth());
        boolean externalBlocked = externalContext.isExternalContextBlocked() || externalSourceBlocked;
        boolean externalHighRisk = ExternalContextPolicy.RISK_HIGH.equalsIgnoreCase(externalContext.getRiskLevel())
                || externalBlocked;
        if (externalSourceBlocked) {
            reasons.add(ExternalContextPolicy.REASON_MISSING_SOURCE);
        }
        if (externalBlocked && !externalSourceBlocked) {
            reasons.add(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        }
        boolean derivativesHighRisk = derivativesAssessment != null && derivativesAssessment.isHighRisk();
        if (derivativesHighRisk) {
            reasons.add("DERIVATIVES_RISK_REVIEW_REQUIRED");
        }
        if (externalHighRisk) {
            reasons.add("EXTERNAL_CONTEXT_REVIEW_REQUIRED");
        }
        SinglePositionRiskCalculator.Assessment risk = singlePositionRiskCalculator.calculate(
                position, markPrice.price(), externalHighRisk || derivativesHighRisk, externalBlocked);
        reasons.addAll(risk.reasonCodes());
        String riskLevel = risk.level().name();
        boolean riskBlocked = risk.riskBlocked();
        PositionRiskTrendEnum riskTrend = riskTrend(position.getId(), userId, systemScope, riskLevel);
        boolean riskIncreased = riskTrend != PositionRiskTrendEnum.STABLE;
        if (riskBlocked) {
            reasons.add("RISK_BLOCKED");
        }
        if (riskIncreased) {
            reasons.add("RISK_LEVEL_INCREASED");
        }

        BigDecimal stopLoss = position.getStopLoss();
        BigDecimal takeProfit = position.getTakeProfit();
        boolean missingStopLoss = !positive(stopLoss);
        boolean missingTakeProfit = !positive(takeProfit);
        if (missingStopLoss) {
            reasons.add("STOP_LOSS_MISSING");
        }
        if (missingTakeProfit) {
            reasons.add("TAKE_PROFIT_MISSING");
        }

        boolean stopLossBreached = PositionMonitorPolicy.stopLossBreached(side, markPrice.price(), stopLoss);
        boolean nearStopLoss = PositionMonitorPolicy.nearStopLoss(side, markPrice.price(), stopLoss);
        boolean takeProfitReached = PositionMonitorPolicy.takeProfitReached(side, markPrice.price(), takeProfit);
        boolean nearTakeProfit = PositionMonitorPolicy.nearTakeProfit(side, markPrice.price(), takeProfit);
        if (stopLossBreached) {
            reasons.add("STOP_LOSS_BREACHED");
        }
        if (takeProfitReached) {
            reasons.add("TAKE_PROFIT_REACHED");
        }
        if (nearStopLoss) {
            reasons.add("NEAR_STOP_LOSS");
        }
        if (nearTakeProfit) {
            reasons.add("NEAR_TAKE_PROFIT");
        }

        boolean persistedPlanInvalidated = planContext.persistedPlanState == PersistedPlanState.INVALID
                || planContext.persistedPlanState == PersistedPlanState.BLOCKED;
        boolean planNeedsReview = planContext.persistedPlanState == PersistedPlanState.REVALIDATION_REQUIRED
                || planContext.persistedPlanState == PersistedPlanState.INCOMPLETE
                || planContext.persistedPlanState == PersistedPlanState.REVIEW_ONLY
                || planContext.persistedPlanState == PersistedPlanState.MISSING;
        PositionReversalEvaluator.Assessment reversalAssessment = positionReversalEvaluator.evaluate(
                side, monitorEvidence.verified() ? monitorEvidence.decision().getMarketBiasHierarchy() : null);
        PositionReversalStatusEnum reversalStatus = reversalAssessment.status();
        if (!reversalAssessment.sourceAvailable()) {
            reasons.add("REVERSAL_SOURCE_UNAVAILABLE");
        }
        boolean strongReversal = reversalStatus == PositionReversalStatusEnum.STRONG_REVERSAL;
        boolean weakReversal = reversalStatus == PositionReversalStatusEnum.WEAK_REVERSAL;
        boolean planInvalidated = stopLossBreached || persistedPlanInvalidated || strongReversal;
        boolean logicWeakened = planContext.missing
                || planNeedsReview
                || riskIncreased
                || derivativesAssessment != null && derivativesAssessment.needsRevalidation()
                || nearStopLoss
                || weakReversal
                || missingStopLoss
                || missingTakeProfit
                || (externalHighRisk && !externalBlocked);

        PositionEntryLogicStatusEnum entryLogicStatus = planInvalidated
                ? PositionEntryLogicStatusEnum.INVALIDATED
                : logicWeakened ? PositionEntryLogicStatusEnum.WEAKENED
                : PositionEntryLogicStatusEnum.STILL_VALID;
        PositionMonitorConclusionEnum monitorConclusion = monitorConclusion(
                stopLossBreached, takeProfitReached, planInvalidated, nearStopLoss,
                nearTakeProfit, risk.level().name(), entryLogicStatus);
        PositionMonitorSuggestedActionEnum suggestedAction = suggestedAction(monitorConclusion, risk.level().name());
        PositionRiskChangeReasonEnum riskReason = riskChangeReason(
                planContext.missing, externalSourceBlocked, externalHighRisk, derivativesHighRisk, planInvalidated,
                reversalStatus, logicWeakened, riskIncreased);
        if (monitorConclusion == PositionMonitorConclusionEnum.LOGIC_VALID) {
            reasons.add("LOGIC_VALID");
        }
        PositionMonitorSourceStatusEnum sourceStatus = monitorEvidence.sourceStatus();
        if (sourceStatus == PositionMonitorSourceStatusEnum.VERIFIED
                && (planContext.missing || !reversalAssessment.sourceAvailable())) {
            sourceStatus = PositionMonitorSourceStatusEnum.PENDING_VERIFICATION;
        }
        boolean trustedMonitorResult = sourceStatus == PositionMonitorSourceStatusEnum.VERIFIED;

        RecordPositionMonitorLogCommand command = new RecordPositionMonitorLogCommand();
        command.setPositionId(position.getId());
        command.setAnalysisId(planContext.missing || currentAnalysisId == null
                ? PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID
                : currentAnalysisId);
        command.setExecutionPlanId(planContext.executionPlanId);
        command.setCurrentPrice(markPrice.price());
        command.setMarkPriceSource(markPrice.source());
        command.setEntryLogicStatus(trustedMonitorResult ? entryLogicStatus.name() : null);
        command.setMonitorConclusion(trustedMonitorResult ? monitorConclusion.name() : null);
        command.setReversalStatus(trustedMonitorResult ? reversalStatus.name() : null);
        command.setRiskChangeReason(trustedMonitorResult ? riskReason.name() : null);
        command.setRiskLevel(trustedMonitorResult ? riskLevel : null);
        command.setRiskTrend(trustedMonitorResult ? riskTrend.name() : null);
        command.setSuggestedAction(trustedMonitorResult ? suggestedAction.name() : null);
        command.setMonitorSourceStatus(sourceStatus.name());
        command.setObservedAt(markPrice.observedAt());
        command.setFreshUntil(markPrice.freshUntil());
        command.setReason(String.join(",", reasons));
        command.setEvidenceSnapshot(trustedMonitorResult
                ? snapshotCount("evidence", currentAnalysisId, monitorEvidence.evidenceCount()) : null);
        command.setScoreSnapshot(trustedMonitorResult
                ? snapshotCount("score", currentAnalysisId, monitorEvidence.scoreCount()) : null);
        command.setDecisionSnapshot(trustedMonitorResult ? decisionSnapshot(monitorEvidence.decision()) : null);
        command.setRiskSnapshot(trustedMonitorResult ? riskSnapshot(risk, externalContext) : null);
        command.setTraceId("POSITION_MONITOR_" + position.getId());
        PositionMonitorLogDTO log = systemScope
                ? positionMonitorLogService.recordMonitorRunForSystem(command)
                : positionMonitorLogService.recordMonitorRunForUser(userId, command);

        PositionMonitorResultDTO result = new PositionMonitorResultDTO();
        result.setPositionId(position.getId());
        result.setAssetSymbol(assetSymbol);
        result.setSide(side);
        result.setPositionStatus(normalize(position.getStatus()));
        result.setAnalysisId(trustedMonitorResult ? currentAnalysisId : null);
        result.setExecutionPlanId(planContext.executionPlanId);
        result.setEntryPrice(position.getEntryPrice());
        result.setStopLoss(stopLoss);
        result.setTakeProfit(takeProfit);
        if (trustedMonitorResult) {
            result.setCurrentPrice(markPrice.price());
            result.setMarkPrice(markPrice.price());
            result.setMarkPriceSource(markPrice.source());
            result.setMarkPriceObservedAt(markPrice.observedAt());
            result.setMarkPriceFresh(true);
            result.setEntryLogicStatus(entryLogicStatus.name());
            result.setMonitorConclusion(monitorConclusion.name());
            result.setDirectionSupportStatus(directionSupportStatus(entryLogicStatus));
            result.setReversalStatus(reversalStatus.name());
            result.setRiskReason(riskReason.name());
            result.setRiskLevel(riskLevel);
            result.setRiskTrend(riskTrend.name());
            result.setRiskBlocked(riskBlocked);
            result.setRiskIncreased(riskIncreased);
            result.setNearStopLoss(nearStopLoss);
            result.setNearTakeProfit(nearTakeProfit);
            result.setStopLossBreached(stopLossBreached);
            result.setTakeProfitReached(takeProfitReached);
            result.setSuggestedAction(suggestedAction.name());
            result.setSuggestedManualAction(suggestedAction.name());
            result.setSuggestedManualActionText(suggestedActionText(suggestedAction));
            applyPnl(result, side, position.getEntryPrice(), markPrice.price(), position.getQuantity());
        } else {
            result.setMarkPriceFresh(false);
        }
        result.setReasonCodes(new ArrayList<>(reasons));
        applyExternalContext(result, externalContext);
        result.setMonitorLogId(log.getLogId());
        result.setMonitoredAt(log.getCreatedAt());
        result.setLastMonitorTime(log.getCreatedAt());
        result.setDataState(dataState(sourceStatus, monitorConclusion, riskTrend).name());
        if (highValueAlertMessageService != null) {
            highValueAlertMessageService.recordPosition(position, log, result);
        }
        return result;
    }

    private void requireBinanceClosedWindows(String symbol) {
        if (persistedOhlcvQueryService == null) {
            throw new PositionMonitorDataUnavailableException("PERSISTED_OHLCV_QUERY_UNAVAILABLE");
        }
        String marketSymbol = BinanceUsdtSymbol.toUsdtPair(symbol);
        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            PersistedOhlcvReadinessResult readiness = persistedOhlcvQueryService.evaluateReadinessForSource(
                    marketSymbol, timeframe, 100, maxReadLagMs(timeframe), "BINANCE_PUBLIC", "SPOT");
            if (readiness == null || !readiness.isFresh()) {
                String reason = readiness == null || readiness.getStaleReasonCode() == null
                        ? "PERSISTED_OHLCV_NOT_READY" : readiness.getStaleReasonCode().name();
                throw new PositionMonitorDataUnavailableException(
                        "AUTHORITATIVE_OHLCV_UNAVAILABLE:" + timeframe + ":" + reason);
            }
        }
    }

    private static long maxReadLagMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 11L * 60_000L;
            case "15m" -> 31L * 60_000L;
            case "1h" -> 121L * 60_000L;
            case "4h" -> 481L * 60_000L;
            default -> 0L;
        };
    }

    private DerivativesBusinessAssessment readDerivativesAssessment(UserPositionDO position, String side,
                                                                    BigDecimal currentPrice) {
        if (position == null || derivativesSnapshotReadPort == null
                || derivativesBusinessIntegrationService == null) return null;
        try {
            ProviderCallResult<DerivativesRiskSnapshot> result = derivativesSnapshotReadPort.readCached(
                    position.getAssetSymbol(), AssetPriority.P0_POSITION,
                    Duration.ofSeconds(derivativesBusinessIntegrationService.monitorRefreshSeconds()),
                    "position-monitor-derivatives-" + position.getId());
            if (result == null || result.payload() == null) return null;
            DerivativesBusinessInput input = new DerivativesBusinessInput(position.getAssetSymbol(), side,
                    currentPrice, position.getEntryPrice(), false, Map.of(), true, 100, true,
                    false, true, null, result.payload(), "POSITION_MONITOR_" + position.getId(),
                    optionalText(position.getSourceRefId()), "POSITION_MONITOR");
            return derivativesBusinessIntegrationService.evaluate(input);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void validateActivePosition(UserPositionDO position) {
        if (position == null || position.getId() == null || position.getId() <= 0) {
            throw new IllegalArgumentException("UserPosition not found");
        }
        String status = normalize(position.getStatus());
        if (!"OPEN".equals(status) && !"PARTIALLY_CLOSED".equals(status)) {
            if ("CLOSED".equals(status)) {
                throw new UserPositionConflictException("CLOSED UserPosition cannot be monitored");
            }
            throw new IllegalArgumentException("UserPosition status must be OPEN or PARTIALLY_CLOSED");
        }
    }

    private MarkPriceContext readMarkPrice(String assetSymbol) {
        ProviderCallResult<MarketPriceSnapshot> result;
        try {
            result = marketPriceSnapshotService.get(assetSymbol, AssetPriority.P0_POSITION,
                    Duration.ofSeconds(15), "position-monitor-" + UUID.randomUUID());
        } catch (RuntimeException ex) {
            throw new PositionMonitorDataUnavailableException("QUOTE_UNAVAILABLE", ex);
        }
        if (!MarketPriceSnapshotPolicy.isFresh(result)) {
            throw new PositionMonitorDataUnavailableException(MarketPriceSnapshotPolicy.failureCode(result));
        }
        BigDecimal lastPrice = result.payload().lastPrice();
        if (!positive(lastPrice)) {
            throw new PositionMonitorDataUnavailableException("INVALID_MARKET_PRICE");
        }
        Instant observedInstant = result.payload().sourceFetchedAt() != null
                ? result.payload().sourceFetchedAt()
                : result.metadata().providerDataTime() != null
                ? result.metadata().providerDataTime() : result.metadata().fetchTime();
        Instant freshUntilInstant = result.metadata().expiresAt();
        if (observedInstant == null || freshUntilInstant == null || freshUntilInstant.isBefore(observedInstant)) {
            throw new PositionMonitorDataUnavailableException("QUOTE_TRUST_METADATA_INCOMPLETE");
        }
        String source = optionalText(result.payload().sourceProvider());
        if (source == null) {
            source = requireText(result.metadata().provider(), "mark_price_source");
        }
        return new MarkPriceContext(lastPrice, source,
                LocalDateTime.ofInstant(observedInstant, ZoneOffset.UTC),
                LocalDateTime.ofInstant(freshUntilInstant, ZoneOffset.UTC));
    }

    private static void applyPnl(PositionMonitorResultDTO result, String side, BigDecimal entryPrice,
                                 BigDecimal currentPrice, BigDecimal quantity) {
        if (!positive(entryPrice) || !positive(currentPrice)) {
            return;
        }
        BigDecimal pct;
        if ("SHORT".equals(side)) {
            pct = entryPrice.subtract(currentPrice).divide(entryPrice, 8, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        } else {
            pct = currentPrice.subtract(entryPrice).divide(entryPrice, 8, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        result.setPnlPct(pct);
        result.setPnlPercent(pct);
        if (positive(quantity)) {
            BigDecimal unitPnl = "SHORT".equals(side)
                    ? entryPrice.subtract(currentPrice)
                    : currentPrice.subtract(entryPrice);
            result.setPnlAmount(unitPnl.multiply(quantity));
        }
    }

    private static String directionSupportStatus(PositionEntryLogicStatusEnum entryLogicStatus) {
        return switch (entryLogicStatus) {
            case STILL_VALID -> "SUPPORTED";
            case WEAKENED -> "WEAKENED";
            case INVALIDATED -> "NOT_SUPPORTED";
        };
    }

    private static PositionMonitorConclusionEnum monitorConclusion(
            boolean stopLossBreached,
            boolean takeProfitReached,
            boolean persistedPlanInvalidated,
            boolean nearStopLoss,
            boolean nearTakeProfit,
            String riskLevel,
            PositionEntryLogicStatusEnum entryLogicStatus) {
        if (stopLossBreached || persistedPlanInvalidated) {
            return PositionMonitorConclusionEnum.PLAN_INVALIDATED;
        }
        if (takeProfitReached) {
            return PositionMonitorConclusionEnum.WAIT_USER_CONFIRM_CLOSE;
        }
        if (nearStopLoss) {
            return PositionMonitorConclusionEnum.NEAR_STOP_LOSS;
        }
        if (nearTakeProfit) {
            return PositionMonitorConclusionEnum.NEAR_TAKE_PROFIT;
        }
        if ("HIGH".equals(riskLevel) || "EXTREME".equals(riskLevel)) {
            return PositionMonitorConclusionEnum.HIGH_RISK_OBSERVATION;
        }
        if (entryLogicStatus == PositionEntryLogicStatusEnum.WEAKENED) {
            return PositionMonitorConclusionEnum.LOGIC_WEAKENED;
        }
        return PositionMonitorConclusionEnum.LOGIC_VALID;
    }

    private static PositionMonitorSuggestedActionEnum suggestedAction(
            PositionMonitorConclusionEnum conclusion, String riskLevel) {
        return switch (conclusion) {
            case WAIT_USER_CONFIRM_CLOSE -> PositionMonitorSuggestedActionEnum.RECORD_CLOSE_REVIEW;
            case PLAN_INVALIDATED -> PositionMonitorSuggestedActionEnum.WAIT_CONFIRMATION;
            case NEAR_STOP_LOSS -> PositionMonitorSuggestedActionEnum.TIGHTEN_STOP;
            case NEAR_TAKE_PROFIT -> PositionMonitorSuggestedActionEnum.PARTIAL_TAKE_PROFIT;
            case HIGH_RISK_OBSERVATION -> "EXTREME".equals(riskLevel)
                    ? PositionMonitorSuggestedActionEnum.WAIT_CONFIRMATION
                    : PositionMonitorSuggestedActionEnum.REDUCE_POSITION;
            case LOGIC_WEAKENED -> PositionMonitorSuggestedActionEnum.NO_ADD_POSITION;
            case LOGIC_VALID -> PositionMonitorSuggestedActionEnum.CONTINUE_HOLD;
        };
    }

    private static PositionRiskChangeReasonEnum riskChangeReason(
            boolean planSourceMissing,
            boolean externalSourceBlocked,
            boolean externalContextHighRisk,
            boolean derivativesHighRisk,
            boolean planInvalidated,
            PositionReversalStatusEnum reversalStatus,
            boolean logicWeakened,
            boolean riskIncreased) {
        if (planSourceMissing || externalSourceBlocked) {
            return PositionRiskChangeReasonEnum.DATA_QUALITY_DEGRADED;
        }
        if (externalContextHighRisk) {
            return PositionRiskChangeReasonEnum.EVENT_IMPACT;
        }
        if (planInvalidated || reversalStatus != null
                && reversalStatus != PositionReversalStatusEnum.NO_REVERSAL) {
            return PositionRiskChangeReasonEnum.STRUCTURE_CHANGED;
        }
        if (derivativesHighRisk || logicWeakened || riskIncreased) {
            return PositionRiskChangeReasonEnum.OPPOSING_EVIDENCE_INCREASED;
        }
        return PositionRiskChangeReasonEnum.NO_CLEAR_RISK_FACTOR;
    }

    private static PositionMonitorDataStateEnum dataState(
            PositionMonitorSourceStatusEnum sourceStatus,
            PositionMonitorConclusionEnum conclusion,
            PositionRiskTrendEnum riskTrend) {
        if (sourceStatus != PositionMonitorSourceStatusEnum.VERIFIED) {
            return PositionMonitorDataStateEnum.WAITING_MONITOR_DATA;
        }
        if (conclusion == PositionMonitorConclusionEnum.PLAN_INVALIDATED) {
            return PositionMonitorDataStateEnum.PLAN_INVALIDATED;
        }
        if (riskTrend != PositionRiskTrendEnum.STABLE) {
            return PositionMonitorDataStateEnum.RISK_ESCALATED;
        }
        return PositionMonitorDataStateEnum.OPEN_MONITORING;
    }

    private static String suggestedActionText(PositionMonitorSuggestedActionEnum suggestedAction) {
        return switch (suggestedAction) {
            case CONTINUE_HOLD -> "继续持有";
            case NO_ADD_POSITION -> "暂不加仓";
            case REDUCE_POSITION -> "降低仓位";
            case TIGHTEN_STOP -> "收紧止损";
            case MOVE_STOP -> "移动止损";
            case PARTIAL_TAKE_PROFIT -> "分批止盈";
            case WAIT_CONFIRMATION -> "等待人工确认";
            case RECORD_CLOSE_REVIEW -> "记录平仓并进入复盘";
        };
    }

    private PlanContext resolvePlanContext(UserPositionDO position, Set<String> reasons) {
        PositionPlanSourceResolver.Resolution resolution = positionPlanSourceResolver.resolveTypedReference(
                position.getId(), position.getAssetSymbol(), position.getSourceRefId());
        if (!resolution.verified()) {
            return unverifiedPlanContext(reasons);
        }
        ExecutionPlanDO plan = resolution.executionPlan();
        PersistedPlanState persistedPlanState = ExecutionPlanReviewPolicy.persistedPlanState(plan);
        boolean boundaryComplete = ExecutionPlanReviewPolicy.hasCompleteBoundaries(plan);
        String executionPlanStatus = normalize(plan.getExecutionPlanStatus());
        String sourceGateStatus = normalize(plan.getSourceGateStatus());
        boolean sourceGateComplete = Boolean.TRUE.equals(plan.getSourceGateComplete());
        if (!sourceGateComplete || "INCOMPLETE".equals(sourceGateStatus)) {
            reasons.add("SOURCE_GATE_INCOMPLETE");
        }
        if (persistedPlanState == PersistedPlanState.INVALID
                || persistedPlanState == PersistedPlanState.BLOCKED) {
            reasons.add("PLAN_INVALID");
        } else if (persistedPlanState == PersistedPlanState.REVALIDATION_REQUIRED) {
            reasons.add("PLAN_REVALIDATION_REQUIRED");
        } else if (persistedPlanState == PersistedPlanState.INCOMPLETE
                || persistedPlanState == PersistedPlanState.REVIEW_ONLY) {
            reasons.add("PLAN_REVIEW_REQUIRED");
        }
        if (!boundaryComplete) {
            reasons.add("PLAN_BOUNDARY_INCOMPLETE");
        }
        return new PlanContext(plan,
                resolution.analysisId(),
                resolution.executionPlanId(),
                false,
                Boolean.TRUE.equals(plan.getNeedsRevalidation()),
                optionalText(plan.getRevalidationReason()),
                boundaryComplete,
                executionPlanStatus,
                sourceGateStatus,
                sourceGateComplete,
                persistedPlanState);
    }

    private PlanContext unverifiedPlanContext(Set<String> reasons) {
        reasons.add("PLAN_SOURCE_UNVERIFIED");
        reasons.add("PLAN_CONTEXT_MISSING");
        return PlanContext.missing();
    }

    private PositionRiskTrendEnum riskTrend(
            Long positionId, Long userId, boolean systemScope, String currentRiskLevel) {
        List<PositionMonitorLogDTO> logs = systemScope
                ? positionMonitorLogService.listByPositionIdForSystem(positionId, 1)
                : positionMonitorLogService.listByPositionIdForUser(userId, positionId, 1);
        if (logs == null || logs.isEmpty()) {
            return PositionRiskTrendEnum.STABLE;
        }
        PositionMonitorLogDTO previousLog = logs.get(0);
        if (!historicallyTrustedRisk(previousLog)) {
            return PositionRiskTrendEnum.STABLE;
        }
        int delta = PositionMonitorPolicy.riskRank(currentRiskLevel)
                - PositionMonitorPolicy.riskRank(previousLog.getRiskLevel());
        if (delta >= 2) {
            return PositionRiskTrendEnum.SHARPLY_INCREASED;
        }
        return delta == 1 ? PositionRiskTrendEnum.INCREASED : PositionRiskTrendEnum.STABLE;
    }

    private static boolean historicallyTrustedRisk(PositionMonitorLogDTO log) {
        return log != null
                && PositionMonitorSourceStatusEnum.VERIFIED.name().equals(log.getMonitorSourceStatus())
                && log.getObservedAt() != null
                && log.getFreshUntil() != null
                && log.getFreshUntil().isAfter(log.getObservedAt())
                && PositionMonitorPolicy.riskRank(log.getRiskLevel()) > 0;
    }

    private String snapshotCount(String type, String analysisId, int count) {
        if (optionalText(analysisId) == null || count <= 0) {
            return null;
        }
        return json(Map.of("analysisId", analysisId, "snapshotType", type, "itemCount", count));
    }

    private String decisionSnapshot(DecisionResultVO decision) {
        if (decision == null || optionalText(decision.getAnalysisId()) == null) {
            return null;
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("analysisId", decision.getAnalysisId());
        safe.put("snapshotType", "decision");
        safe.put("present", true);
        return json(safe);
    }

    private MonitorEvidenceContext resolveMonitorEvidenceContext(
            String assetSymbol, MarkPriceContext markPrice) {
        String normalizedSymbol = normalizeSymbol(assetSymbol);
        if (normalizedSymbol == null || decisionResultMapper == null || analysisRunMapper == null) {
            return MonitorEvidenceContext.pending(null, "MONITOR_REQUIRED_CONTEXT_UNAVAILABLE");
        }
        try {
            DecisionResultVO decision = decisionResultMapper.findLatestDecisionResultBySymbolJoined(normalizedSymbol);
            if (decision == null) {
                return MonitorEvidenceContext.pending(null, "MONITOR_RESULT_MISSING");
            }
            String analysisId = optionalText(decision.getAnalysisId());
            if (analysisId == null
                    || !normalizedSymbol.equals(normalizeSymbol(decision.getSymbol()))) {
                return MonitorEvidenceContext.invalid(analysisId, "MONITOR_RESULT_IDENTITY_INVALID");
            }
            AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
            if (run == null) {
                return MonitorEvidenceContext.pending(analysisId, "MONITOR_ANALYSIS_CONTEXT_MISSING");
            }
            if (!analysisId.equals(optionalText(run.getAnalysisId()))
                    || !normalizedSymbol.equals(normalizeSymbol(run.getSymbol()))) {
                return MonitorEvidenceContext.invalid(analysisId, "MONITOR_ANALYSIS_IDENTITY_INVALID");
            }
            if (!"SUCCESS".equals(normalize(run.getStatus()))) {
                return MonitorEvidenceContext.pending(analysisId, "MONITOR_RESULT_NOT_SUCCESSFUL");
            }
            if (!DataQualityCircuitBreakerPolicy.isValid(run.getDataQualityScore())
                    || !DataQualityCircuitBreakerPolicy.isValid(decision.getDataQualityScore())) {
                return MonitorEvidenceContext.invalid(analysisId, "MONITOR_DATA_QUALITY_INVALID");
            }
            if (!DataQualityCircuitBreakerPolicy.passes(run.getDataQualityScore())
                    || !DataQualityCircuitBreakerPolicy.passes(decision.getDataQualityScore())) {
                return MonitorEvidenceContext.pending(analysisId, "MONITOR_DATA_QUALITY_BLOCKED");
            }
            int evidenceCount = Optional.ofNullable(
                    analysisRunMapper.countEvidenceByAnalysisId(analysisId)).orElse(0);
            int scoreCount = Optional.ofNullable(
                    analysisRunMapper.countScoresByAnalysisId(analysisId)).orElse(0);
            if (evidenceCount <= 0 || scoreCount < 8
                    || optionalText(decision.getMarketBiasHierarchy()) == null
                    || optionalText(decision.getMultiTfConvergence()) == null) {
                return MonitorEvidenceContext.pending(analysisId, "MONITOR_REQUIRED_CONTEXT_INCOMPLETE");
            }
            Duration freshness = evidenceFreshness(run.getTimeframe() == null
                    ? decision.getTimeframe() : run.getTimeframe());
            LocalDateTime completedAt = run.getCompletedAt();
            LocalDateTime decisionAt = decision.getCreateTime();
            if (freshness == null || completedAt == null || decisionAt == null) {
                return MonitorEvidenceContext.pending(analysisId, "MONITOR_EVIDENCE_FRESHNESS_UNKNOWN");
            }
            LocalDateTime asOf = markPrice == null ? null : markPrice.observedAt();
            if (asOf == null
                    || completedAt.isAfter(asOf.plusMinutes(1))
                    || decisionAt.isAfter(asOf.plusMinutes(1))) {
                return MonitorEvidenceContext.invalid(analysisId, "MONITOR_EVIDENCE_TIMESTAMP_INVALID");
            }
            if (!asOf.isBefore(completedAt.plus(freshness))
                    || !asOf.isBefore(decisionAt.plus(freshness))) {
                return MonitorEvidenceContext.pending(analysisId, "MONITOR_EVIDENCE_STALE");
            }
            return MonitorEvidenceContext.verified(decision, evidenceCount, scoreCount);
        } catch (RuntimeException ignored) {
            return MonitorEvidenceContext.pending(null, "MONITOR_REQUIRED_CONTEXT_READ_FAILED");
        }
    }

    private static Duration evidenceFreshness(String timeframe) {
        String normalized = normalize(timeframe);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "5M" -> Duration.ofMinutes(10);
            case "15M" -> Duration.ofMinutes(30);
            case "1H" -> Duration.ofHours(2);
            case "4H" -> Duration.ofHours(8);
            default -> null;
        };
    }

    private ExternalContextSnapshot resolveExternalContext(String assetSymbol, String analysisId) {
        if (externalContextEvidenceBuilder == null) {
            return new ExternalContextSnapshot();
        }
        try {
            return externalContextEvidenceBuilder.buildSnapshot(analysisId, assetSymbol, null, LocalDateTime.now(), null);
        } catch (RuntimeException ignored) {
            ExternalContextSnapshot snapshot = new ExternalContextSnapshot();
            snapshot.setSourceHealth(ExternalContextPolicy.SOURCE_HEALTH_BLOCKED);
            snapshot.setExternalContextBlocked(true);
            snapshot.setRiskLevel(ExternalContextPolicy.RISK_HIGH);
            snapshot.addReason(ExternalContextPolicy.REASON_MISSING_SOURCE);
            return snapshot;
        }
    }

    private static void applyExternalContext(PositionMonitorResultDTO result, ExternalContextSnapshot snapshot) {
        result.setExternalContextStatus(snapshot.getStatus());
        result.setActiveExternalEventCount(snapshot.getActiveExternalEventCount());
        result.setActiveMacroEventCount(snapshot.getActiveMacroEventCount());
        result.setActiveNewsEventCount(snapshot.getActiveNewsEventCount());
        result.setExternalContextRiskLevel(snapshot.getRiskLevel());
        result.setExternalContextBlocked(snapshot.isExternalContextBlocked());
        result.setExternalEventIds(snapshot.getExternalEventIds());
        result.setExternalContextReasonCodes(snapshot.getReasonCodes());
        result.setNextExternalEventTime(snapshot.getNextExternalEventTime());
        result.setExternalContextSourceHealth(snapshot.getSourceHealth());
    }

    private String riskSnapshot(SinglePositionRiskCalculator.Assessment risk,
                                ExternalContextSnapshot externalContext) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("scope", "SINGLE_POSITION");
        safe.put("riskLevel", risk.level().name());
        safe.put("riskBlocked", risk.riskBlocked());
        safe.put("positionNotional", risk.positionNotional());
        safe.put("leveragedAdverseMovePercent", risk.leveragedAdverseMovePercent());
        safe.put("reasonCodes", risk.reasonCodes());
        if (externalContext != null) {
            Map<String, Object> external = new LinkedHashMap<>();
            external.put("status", externalContext.getStatus());
            external.put("sourceHealth", externalContext.getSourceHealth());
            external.put("riskLevel", externalContext.getRiskLevel());
            external.put("blocked", externalContext.isExternalContextBlocked());
            external.put("activeCount", externalContext.getActiveExternalEventCount());
            external.put("macroCount", externalContext.getActiveMacroEventCount());
            external.put("newsCount", externalContext.getActiveNewsEventCount());
            external.put("reasonCodes", externalContext.getReasonCodes());
            external.put("eventIds", externalContext.getExternalEventIds());
            safe.put("externalContext", external);
        }
        return json(safe);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("MONITOR_SNAPSHOT_SERIALIZATION_FAILED", e);
        }
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String requireText(String value, String fieldName) {
        String text = optionalText(value);
        if (text == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return text;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeSymbol(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized
                .replace("/", "")
                .replace("-", "")
                .replace("_", "");
    }

    private static final class PositionMonitorDataUnavailableException extends IllegalStateException {
        private PositionMonitorDataUnavailableException(String message) {
            super(message);
        }

        private PositionMonitorDataUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record MarkPriceContext(BigDecimal price,
                                    String source,
                                    LocalDateTime observedAt,
                                    LocalDateTime freshUntil) {
    }

    private record MonitorEvidenceContext(PositionMonitorSourceStatusEnum sourceStatus,
                                          String analysisId,
                                          DecisionResultVO decision,
                                          int evidenceCount,
                                          int scoreCount,
                                          String reasonCode) {
        private static MonitorEvidenceContext verified(
                DecisionResultVO decision, int evidenceCount, int scoreCount) {
            return new MonitorEvidenceContext(PositionMonitorSourceStatusEnum.VERIFIED,
                    decision.getAnalysisId(), decision, evidenceCount, scoreCount, null);
        }

        private static MonitorEvidenceContext pending(String analysisId, String reasonCode) {
            return new MonitorEvidenceContext(PositionMonitorSourceStatusEnum.PENDING_VERIFICATION,
                    analysisId, null, 0, 0, reasonCode);
        }

        private static MonitorEvidenceContext invalid(String analysisId, String reasonCode) {
            return new MonitorEvidenceContext(PositionMonitorSourceStatusEnum.INVALID,
                    analysisId, null, 0, 0, reasonCode);
        }

        private boolean verified() {
            return sourceStatus == PositionMonitorSourceStatusEnum.VERIFIED && decision != null;
        }
    }

    private static class PlanContext {
        private final ExecutionPlanDO plan;
        private final String analysisId;
        private final String executionPlanId;
        private final boolean missing;
        private final boolean needsRevalidation;
        private final String revalidationReason;
        private final boolean planBoundaryComplete;
        private final String executionPlanStatus;
        private final String sourceGateStatus;
        private final boolean sourceGateComplete;
        private final PersistedPlanState persistedPlanState;

        private PlanContext(ExecutionPlanDO plan,
                            String analysisId,
                            String executionPlanId,
                            boolean missing,
                            boolean needsRevalidation,
                            String revalidationReason,
                            boolean planBoundaryComplete,
                            String executionPlanStatus,
                            String sourceGateStatus,
                            boolean sourceGateComplete,
                            PersistedPlanState persistedPlanState) {
            this.plan = plan;
            this.analysisId = analysisId;
            this.executionPlanId = executionPlanId;
            this.missing = missing;
            this.needsRevalidation = needsRevalidation;
            this.revalidationReason = revalidationReason;
            this.planBoundaryComplete = planBoundaryComplete;
            this.executionPlanStatus = executionPlanStatus;
            this.sourceGateStatus = sourceGateStatus;
            this.sourceGateComplete = sourceGateComplete;
            this.persistedPlanState = persistedPlanState;
        }

        private static PlanContext missing() {
            return new PlanContext(null, null, null, true, false, null,
                    false, null, null, false, PersistedPlanState.MISSING);
        }
    }
}
