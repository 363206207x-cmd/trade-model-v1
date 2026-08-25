package org.example.trademodel.service;

import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.positionmonitor.PositionPlanSourceResolver;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Owner-scoped read projection for the full Positions workspace. */
@Service
@Profile("!ui-review")
public class PositionMonitoringProjectionService implements PositionMonitoringReadService {
    private final UserPositionService userPositionService;
    private final PositionMonitorLogService monitorLogService;
    private final PositionPlanSourceResolver sourceResolver;
    private Clock clock = Clock.systemUTC();

    public PositionMonitoringProjectionService(UserPositionService userPositionService,
                                               PositionMonitorLogService monitorLogService,
                                               ExecutionPlanMapper executionPlanMapper,
                                               AnalysisRunMapper analysisRunMapper) {
        this.userPositionService = userPositionService;
        this.monitorLogService = monitorLogService;
        this.sourceResolver = new PositionPlanSourceResolver(executionPlanMapper, analysisRunMapper);
    }

    @Override
    public CollectionProjection listForUser(Long userId) {
        List<ItemProjection> items = new ArrayList<>();
        for (UserPositionVO position : userPositionService.listOpenPositionsForUser(userId)) {
            items.add(project(userId, position, true));
        }
        boolean complete = !items.isEmpty() && items.stream().allMatch(ItemProjection::monitorAvailable);
        String coverage = items.isEmpty() ? "NO_POSITION" : complete ? "COMPLETE" : "PARTIAL_COVERAGE";
        return new CollectionProjection(List.copyOf(items), items.size(), coverage);
    }

    @Override
    public ItemProjection findForUser(Long userId, Long positionId) {
        UserPositionVO position = userPositionService.findByIdForUser(positionId, userId);
        boolean active = "OPEN".equalsIgnoreCase(position.getStatus())
                || "PARTIALLY_CLOSED".equalsIgnoreCase(position.getStatus());
        return project(userId, position, active);
    }

    @Override
    public HistoryProjection historyForUser(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<UserPositionVO> positions = userPositionService.listClosedPositionsForUser(userId, safeLimit);
        return new HistoryProjection(positions, userPositionService.countClosedPositionsForUser(userId));
    }

    private ItemProjection project(Long userId, UserPositionVO position, boolean includeLiveMonitor) {
        PositionMonitorLogDTO monitor = null;
        if (includeLiveMonitor) {
            List<PositionMonitorLogDTO> logs = monitorLogService.listByPositionIdForUser(userId, position.getId(), 1);
            monitor = logs == null || logs.isEmpty() ? null : logs.get(0);
        }
        LocalDateTime asOf = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        boolean trusted = includeLiveMonitor && trusted(monitor, asOf);
        DashboardHomeVO.PositionVO view = base(position);
        if (trusted) {
            applyTrusted(view, position, monitor);
        } else {
            applyUnavailable(view, includeLiveMonitor ? trustState(monitor, asOf) : "CLOSED");
        }
        if (trusted) {
            PositionPlanSourceResolver.Resolution source = sourceResolver.resolveTrustedMonitorSourceForUser(
                    userId, position.getId(), position.getAssetSymbol(), position.getSourceRefId(),
                    monitor.getAnalysisId(), monitor.getExecutionPlanId());
            if (source.verified()) {
                view.setSourceAnalysisId(source.analysisId());
                view.setSourceExecutionPlanId(source.executionPlanId());
                view.setSourceTraceId(source.sourceTraceId());
            }
        }
        return new ItemProjection(position, view, trusted,
                trusted ? "AVAILABLE" : includeLiveMonitor ? view.getMonitorTrustState() : "CLOSED");
    }

    private DashboardHomeVO.PositionVO base(UserPositionVO position) {
        DashboardHomeVO.PositionVO view = new DashboardHomeVO.PositionVO();
        view.setPositionId(position.getId());
        view.setSymbol(position.getAssetSymbol());
        view.setDirection(position.getSide());
        view.setEntryPrice(position.getEntryPrice());
        view.setLeverage(position.getLeverage());
        view.setPositionSize(position.getQuantity());
        view.setPositionStatus(position.getStatus());
        view.setUserStopLoss(position.getStopLoss());
        view.setUserTakeProfit(position.getTakeProfit());
        view.setOpenedAt(position.getOpenedAt());
        view.setUpdatedAt(position.getUpdatedAt());
        view.setSourceType(position.getSourceType());
        view.setSourceRefId(position.getSourceRefId());
        view.setFinalPlanId(position.getFinalPlanId());
        return view;
    }

    private boolean trusted(PositionMonitorLogDTO log, LocalDateTime asOf) {
        return log != null && log.isTrustedAndFreshAt(asOf)
                && positive(log.getCurrentPrice()) && hasText(log.getMarkPriceSource())
                && hasText(log.getEntryLogicStatus()) && hasText(log.getMonitorConclusion())
                && hasText(log.getReversalStatus()) && hasText(log.getRiskChangeReason())
                && hasText(log.getRiskLevel()) && hasText(log.getRiskTrend())
                && hasText(log.getSuggestedAction());
    }

    private void applyTrusted(DashboardHomeVO.PositionVO view, UserPositionVO position,
                              PositionMonitorLogDTO log) {
        view.setMarkPrice(log.getCurrentPrice());
        view.setCurrentPrice(log.getCurrentPrice());
        view.setMarkPriceSource(log.getMarkPriceSource());
        view.setMarkPriceObservedAt(log.getObservedAt());
        view.setMarkPriceFresh(true);
        applyPnl(view, position, log.getCurrentPrice());
        view.setEntryLogicStatus(log.getEntryLogicStatus());
        view.setMonitorConclusion(log.getMonitorConclusion());
        view.setReversalStatus(log.getReversalStatus());
        view.setRiskReason(log.getRiskChangeReason());
        view.setRiskLevel(log.getRiskLevel());
        view.setRiskTrend(log.getRiskTrend());
        view.setSuggestedAction(log.getSuggestedAction());
        view.setSuggestedManualAction(log.getSuggestedAction());
        view.setLastMonitorAt(log.getCreatedAt());
        view.setLastMonitorTime(log.getCreatedAt());
        view.setMonitorTrustState("VERIFIED_FRESH");
        view.setDataState(dataState(log));
        view.setModuleState("READY");
    }

    private void applyUnavailable(DashboardHomeVO.PositionVO view, String trustState) {
        view.setMarkPrice(null);
        view.setCurrentPrice(null);
        view.setMarkPriceFresh(false);
        view.setEntryLogicStatus(null);
        view.setMonitorConclusion(null);
        view.setReversalStatus(null);
        view.setRiskReason(null);
        view.setRiskLevel(null);
        view.setRiskTrend(null);
        view.setSuggestedAction(null);
        view.setSuggestedManualAction(null);
        view.setSuggestedManualActionText(null);
        view.setMonitorTrustState(trustState);
        view.setDataState("CLOSED".equals(trustState) ? "CLOSED" : "WAITING_MONITOR_DATA");
        view.setModuleState("PARTIAL");
    }

    private String trustState(PositionMonitorLogDTO log, LocalDateTime asOf) {
        if (log == null) return "SOURCE_UNAVAILABLE";
        String status = upper(log.getMonitorSourceStatus());
        if ("PENDING_VERIFICATION".equals(status)) return "PENDING";
        if ("INVALID".equals(status)) return "INVALID";
        if ("VERIFIED".equals(status) && log.getFreshUntil() != null
                && !asOf.isBefore(log.getFreshUntil())) return "STALE";
        return "INVALID";
    }

    private String dataState(PositionMonitorLogDTO log) {
        if ("PLAN_INVALIDATED".equals(upper(log.getMonitorConclusion()))) return "PLAN_INVALIDATED";
        if ("INCREASED".equals(upper(log.getRiskTrend()))
                || "SHARPLY_INCREASED".equals(upper(log.getRiskTrend()))) return "RISK_ESCALATED";
        return "OPEN_MONITORING";
    }

    private void applyPnl(DashboardHomeVO.PositionVO view, UserPositionVO position, BigDecimal markPrice) {
        if (!positive(position.getEntryPrice()) || !positive(markPrice)) return;
        BigDecimal unit = "SHORT".equalsIgnoreCase(position.getSide())
                ? position.getEntryPrice().subtract(markPrice)
                : markPrice.subtract(position.getEntryPrice());
        BigDecimal percent = unit.divide(position.getEntryPrice(), 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        view.setPnlPercent(percent);
        view.setPnlPct(percent);
        if (positive(position.getQuantity())) {
            BigDecimal amount = unit.multiply(position.getQuantity());
            view.setPnlAmount(amount);
            view.setFloatingPnl(amount);
        }
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public record ItemProjection(UserPositionVO position,
                                 DashboardHomeVO.PositionVO monitor,
                                 boolean monitorAvailable,
                                 String collectionState) { }

    public record CollectionProjection(List<ItemProjection> positions,
                                       int activeCount,
                                       String accountRiskCoverageState) { }

    public record HistoryProjection(List<UserPositionVO> positions, int totalCount) { }
}
