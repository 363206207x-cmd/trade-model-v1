package org.example.trademodel.uireview;

import org.example.trademodel.service.PositionMonitoringProjectionService;
import org.example.trademodel.service.PositionMonitoringReadService;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Profile-limited, non-persistent position projection used by UI review only. */
@Profile("ui-review")
@Service
public class UiReviewPositionMonitoringReadService implements PositionMonitoringReadService {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 14, 30);
    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(7101L, "BTCUSDT", "LONG", "SYSTEM_PLAN_POSITION", "62000", "64100", "3.39",
                    "MEDIUM", "STABLE", "STILL_VALID", "NO_REVERSAL", "NO_CLEAR_RISK_FACTOR",
                    "LOGIC_VALID", "CONTINUE_HOLD", "逻辑仍成立", "继续持有", NOW.minusDays(12)),
            new Definition(7102L, "ETHUSDT", "SHORT", "MANUAL_INDEPENDENT", "3400", "3490", "-2.65",
                    "HIGH", "INCREASED", "WEAKENED", "WEAK_REVERSAL", "OPPOSING_EVIDENCE_INCREASED",
                    "LOGIC_WEAKENED", "TIGHTEN_STOP", "逻辑弱化", "收紧止损", NOW.minusDays(7)),
            new Definition(7103L, "SOLUSDT", "LONG", "SYSTEM_PLAN_POSITION", "145", "129.5", "-10.69",
                    "EXTREME", "SHARPLY_INCREASED", "INVALIDATED", "STRONG_REVERSAL", "STRUCTURE_CHANGED",
                    "PLAN_INVALIDATED", "WAIT_CONFIRMATION", "计划失效", "等待人工确认", NOW.minusDays(4)));

    @Override
    public PositionMonitoringProjectionService.CollectionProjection listForUser(Long userId) {
        List<PositionMonitoringProjectionService.ItemProjection> positions = DEFINITIONS.stream()
                .map(definition -> item(definition, null))
                .toList();
        return new PositionMonitoringProjectionService.CollectionProjection(
                positions, positions.size(), coverageState(positions));
    }

    @Override
    public PositionMonitoringProjectionService.ItemProjection findForUser(Long userId, Long positionId) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.id().equals(positionId))
                .findFirst()
                .map(definition -> item(definition, null))
                .orElseThrow(UserPositionNotFoundException::new);
    }

    @Override
    public PositionMonitoringProjectionService.HistoryProjection historyForUser(Long userId, int limit) {
        return new PositionMonitoringProjectionService.HistoryProjection(List.of(), 0);
    }

    public List<DashboardHomeVO.PositionVO> homeTopThree(Long monitorScenarioId) {
        if (monitorScenarioId != null && monitorScenarioId >= 7201L && monitorScenarioId <= 7204L) {
            return List.of(item(DEFINITIONS.get(0), monitorScenarioId).monitor());
        }
        return DEFINITIONS.stream().limit(3).map(definition -> item(definition, null).monitor()).toList();
    }

    public DashboardHomeVO.PositionAggregateVO aggregate() {
        PositionMonitoringProjectionService.CollectionProjection projection = listForUser(1L);
        DashboardHomeVO.PositionAggregateVO aggregate = new DashboardHomeVO.PositionAggregateVO();
        aggregate.setActiveCount(projection.activeCount());
        aggregate.setHighestTrustedRisk(highestTrustedRisk(projection.positions()));
        aggregate.setCoverageState(projection.accountRiskCoverageState());
        return aggregate;
    }

    private PositionMonitoringProjectionService.ItemProjection item(Definition definition, Long monitorScenarioId) {
        UserPositionVO position = position(definition);
        DashboardHomeVO.PositionVO monitor = monitor(definition);
        if (monitorScenarioId != null) applyUntrustedMonitorState(monitor, monitorScenarioId);
        boolean available = "VERIFIED_FRESH".equals(monitor.getMonitorTrustState());
        return new PositionMonitoringProjectionService.ItemProjection(
                position, monitor, available, available ? "AVAILABLE" : monitor.getMonitorTrustState());
    }

    private UserPositionVO position(Definition definition) {
        UserPositionVO position = new UserPositionVO();
        position.setId(definition.id());
        position.setAssetSymbol(definition.symbol());
        position.setSide(definition.direction());
        position.setStatus("OPEN");
        position.setEntryPrice(new BigDecimal(definition.entry()));
        position.setOpenedAt(definition.openedAt());
        position.setUpdatedAt(NOW.minusMinutes(3));
        position.setSourceType(definition.sourceType());
        position.setFinalPlanId("MANUAL_INDEPENDENT".equals(definition.sourceType())
                ? null : "ui-review-final-" + definition.symbol().toLowerCase());
        position.setManualReviewRequired(true);
        position.setNotTradeInstruction(true);
        position.setNotAutoTrading(true);
        position.setNotOrderExecution(true);
        position.setNotPositionSync(true);
        return position;
    }

    private DashboardHomeVO.PositionVO monitor(Definition definition) {
        DashboardHomeVO.PositionVO position = new DashboardHomeVO.PositionVO();
        position.setPositionId(definition.id());
        position.setSymbol(definition.symbol());
        position.setDirection(definition.direction());
        position.setDirectionLabel("LONG".equals(definition.direction()) ? "做多" : "做空");
        position.setSourceType(definition.sourceType());
        position.setEntryPrice(new BigDecimal(definition.entry()));
        position.setMarkPrice(new BigDecimal(definition.mark()));
        position.setCurrentPrice(new BigDecimal(definition.mark()));
        position.setMarkPriceSource("MARKET_SNAPSHOT");
        position.setMarkPriceObservedAt(NOW.minusMinutes(2));
        position.setMarkPriceFresh(true);
        position.setPnlPercent(new BigDecimal(definition.pnlPercent()));
        position.setPnlPct(new BigDecimal(definition.pnlPercent()));
        position.setRiskLevel(definition.risk());
        position.setRiskLevelLabel(riskLabel(definition.risk()));
        position.setRiskTrend(definition.riskTrend());
        position.setEntryLogicStatus(definition.logic());
        position.setEntryLogicStatusLabel(labelFor(definition.logic()));
        position.setReversalStatus(definition.reversal());
        position.setReversalStatusLabel(labelFor(definition.reversal()));
        position.setRiskReason(definition.riskReason());
        position.setRiskReasonLabel(labelFor(definition.riskReason()));
        position.setMonitorConclusion(definition.conclusion());
        position.setMonitorConclusionLabel(definition.conclusionLabel());
        position.setSuggestedAction(definition.action());
        position.setSuggestedManualAction(definition.action());
        position.setSuggestedManualActionText(definition.actionLabel());
        position.setOpenedAt(definition.openedAt());
        position.setLastMonitorAt(NOW.minusMinutes(3));
        position.setLastMonitorTime(NOW.minusMinutes(3));
        position.setUpdatedAt(NOW.minusMinutes(3));
        position.setPositionStatus("OPEN");
        position.setModuleState("READY");
        position.setWarningState("EXTREME".equals(definition.risk()) ? "HIGH" : "NORMAL");
        position.setDataState("STABLE".equals(definition.riskTrend()) ? "OPEN_MONITORING"
                : "PLAN_INVALIDATED".equals(definition.conclusion()) ? "PLAN_INVALIDATED" : "RISK_ESCALATED");
        position.setMonitorTrustState("VERIFIED_FRESH");
        position.setFinalPlanId("MANUAL_INDEPENDENT".equals(definition.sourceType())
                ? null : "ui-review-final-" + definition.symbol().toLowerCase());
        return position;
    }

    private void applyUntrustedMonitorState(DashboardHomeVO.PositionVO position, Long scenarioId) {
        String trustState = switch (scenarioId.intValue()) {
            case 7201 -> "PENDING";
            case 7202 -> "STALE";
            case 7203 -> "INVALID";
            default -> "SOURCE_UNAVAILABLE";
        };
        position.setMonitorTrustState(trustState);
        position.setDataState("WAITING_MONITOR_DATA");
        position.setMarkPrice(null);
        position.setCurrentPrice(null);
        position.setMarkPriceFresh(false);
        position.setPnlAmount(null);
        position.setPnlPercent(null);
        position.setPnlPct(null);
        position.setRiskLevel(null);
        position.setRiskLevelLabel(null);
        position.setRiskTrend(null);
        position.setEntryLogicStatus(null);
        position.setEntryLogicStatusLabel(null);
        position.setReversalStatus(null);
        position.setReversalStatusLabel(null);
        position.setRiskReason(null);
        position.setRiskReasonLabel(null);
        position.setMonitorConclusion(null);
        position.setMonitorConclusionLabel(null);
        position.setSuggestedAction(null);
        position.setSuggestedManualAction(null);
        position.setSuggestedManualActionText(null);
        position.setLastMonitorAt(null);
        position.setLastMonitorTime(null);
        position.setModuleState("MISSING");
    }

    private String coverageState(List<PositionMonitoringProjectionService.ItemProjection> positions) {
        return positions.isEmpty() ? "NO_POSITION"
                : positions.stream().allMatch(PositionMonitoringProjectionService.ItemProjection::monitorAvailable)
                ? "COMPLETE" : "PARTIAL_COVERAGE";
    }

    private String highestTrustedRisk(List<PositionMonitoringProjectionService.ItemProjection> positions) {
        return positions.stream()
                .filter(PositionMonitoringProjectionService.ItemProjection::monitorAvailable)
                .map(item -> item.monitor().getRiskLevel())
                .max((left, right) -> Integer.compare(riskRank(left), riskRank(right)))
                .orElse(null);
    }

    private int riskRank(String value) {
        return Map.of("LOW", 1, "MEDIUM", 2, "HIGH", 3, "EXTREME", 4).getOrDefault(value, 0);
    }

    private String riskLabel(String risk) {
        return Map.of("LOW", "低", "MEDIUM", "中", "HIGH", "高", "EXTREME", "极高").get(risk);
    }

    private String labelFor(String value) {
        return Map.ofEntries(
                Map.entry("STILL_VALID", "仍成立"), Map.entry("WEAKENED", "弱化"),
                Map.entry("INVALIDATED", "失效"), Map.entry("NO_REVERSAL", "无明显反转"),
                Map.entry("WEAK_REVERSAL", "弱反转"), Map.entry("STRONG_REVERSAL", "强反转"),
                Map.entry("NO_CLEAR_RISK_FACTOR", "暂无明显风险因素"),
                Map.entry("OPPOSING_EVIDENCE_INCREASED", "反向证据增加"),
                Map.entry("STRUCTURE_CHANGED", "结构变化")).get(value);
    }

    private record Definition(Long id, String symbol, String direction, String sourceType,
                              String entry, String mark, String pnlPercent, String risk,
                              String riskTrend, String logic, String reversal, String riskReason,
                              String conclusion, String action, String conclusionLabel,
                              String actionLabel, LocalDateTime openedAt) { }
}
