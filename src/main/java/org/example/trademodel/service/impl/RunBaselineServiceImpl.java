package org.example.trademodel.service.impl;

import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.RunBaselineService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.vo.KeyCountVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.RunBaselineVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RunBaselineServiceImpl implements RunBaselineService {

    private static final int DEFAULT_WINDOW_MINUTES = 60;
    private static final int DATA_QUALITY_THRESHOLD = 60;
    private static final String STATUS_FRESH = "FRESH";
    private static final String STATUS_STALE = "STALE";
    private static final String STATUS_UNKNOWN = "UNKNOWN";

    private final SystemHealthService systemHealthService;
    private final PositionSyncService positionSyncService;
    private final DecisionService decisionService;
    private final RuntimeMetricService runtimeMetricService;
    private final MonitorAlertMapper monitorAlertMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final HotResetEventMapper hotResetEventMapper;
    private Clock clock = Clock.systemUTC();

    public RunBaselineServiceImpl(SystemHealthService systemHealthService,
                                  PositionSyncService positionSyncService,
                                  DecisionService decisionService,
                                  RuntimeMetricService runtimeMetricService,
                                  MonitorAlertMapper monitorAlertMapper,
                                  AnalysisRunMapper analysisRunMapper,
                                  PushRecheckLogMapper pushRecheckLogMapper,
                                  HotResetEventMapper hotResetEventMapper) {
        this.systemHealthService = systemHealthService;
        this.positionSyncService = positionSyncService;
        this.decisionService = decisionService;
        this.runtimeMetricService = runtimeMetricService;
        this.monitorAlertMapper = monitorAlertMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.hotResetEventMapper = hotResetEventMapper;
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public RunBaselineVO getRunBaseline(int windowMinutes) {
        long assembleStartMs = System.currentTimeMillis();
        int effectiveWindowMinutes = windowMinutes > 0 ? windowMinutes : DEFAULT_WINDOW_MINUTES;
        LocalDateTime asOfUtc = UtcLocalTimePolicy.now(clock);
        LocalDateTime windowStartUtc = asOfUtc.minusMinutes(effectiveWindowMinutes);

        RunBaselineVO vo = new RunBaselineVO();
        vo.setGeneratedAt(asOfUtc);
        vo.setWindowMinutes(effectiveWindowMinutes);
        vo.setSystemHealth(buildSystemHealthSnapshot());
        vo.setPositionSync(buildPositionSyncSnapshot());
        vo.setPerformance(buildPerformanceSummary(assembleStartMs));
        vo.setAlertSummary(buildAlertSummary(windowStartUtc, asOfUtc));
        vo.setDataQualitySummary(buildDataQualitySummary(windowStartUtc, asOfUtc));
        vo.setRecheckSummary(buildRecheckSummary(windowStartUtc, asOfUtc));
        vo.setHotResetSummary(buildHotResetSummary(windowStartUtc, asOfUtc));
        return vo;
    }

    private RunBaselineVO.SystemHealthSnapshot buildSystemHealthSnapshot() {
        Map<String, Object> health = systemHealthService.getSystemHealth();
        RunBaselineVO.SystemHealthSnapshot snapshot = new RunBaselineVO.SystemHealthSnapshot();
        snapshot.setCpuUsage(toStringValue(health != null ? health.get("cpuUsage") : null));
        snapshot.setMemoryUsage(toStringValue(health != null ? health.get("memoryUsage") : null));
        snapshot.setDatabaseStatus(toStringValue(health != null ? health.get("databaseStatus") : null));
        snapshot.setDatabaseStatusDetail(toStringValue(health != null ? health.get("databaseStatusDetail") : null));
        snapshot.setSchedulerStatus(toStringValue(health != null ? health.get("schedulerStatus") : null));
        snapshot.setSchedulerStatusDetail(toStringValue(health != null ? health.get("schedulerStatusDetail") : null));
        return snapshot;
    }

    private RunBaselineVO.PositionSyncBaselineSnapshot buildPositionSyncSnapshot() {
        PositionSyncStatusVO rawStatus = positionSyncService.getPositionSyncStatus();
        RunBaselineVO.PositionSyncBaselineSnapshot snapshot = new RunBaselineVO.PositionSyncBaselineSnapshot();
        if (rawStatus == null) {
            snapshot.setAvailabilityStatus(STATUS_UNKNOWN);
            snapshot.setAvailabilityDetail("position sync status is unavailable");
            return snapshot;
        }

        snapshot.setFreshnessStatus(rawStatus.getFreshnessStatus());
        snapshot.setFreshnessDetail(rawStatus.getFreshnessDetail());
        snapshot.setStaleThresholdMinutes(rawStatus.getStaleThresholdMinutes());
        snapshot.setConfiguredProviderType(rawStatus.getConfiguredProviderType());
        snapshot.setActiveProviderType(rawStatus.getActiveProviderType());
        snapshot.setActiveProviderName(rawStatus.getActiveProviderName());
        snapshot.setFallbackOccurred(rawStatus.getFallbackOccurred());
        snapshot.setFallbackReason(rawStatus.getFallbackReason());
        snapshot.setLastSyncStartTime(rawStatus.getLastSyncStartTime());
        snapshot.setLastSyncEndTime(rawStatus.getLastSyncEndTime());
        snapshot.setLastSyncSuccess(rawStatus.getLastSyncSuccess());
        snapshot.setLastSyncMessage(rawStatus.getLastSyncMessage());
        snapshot.setLastFetchedOpenCount(rawStatus.getLastFetchedOpenCount());
        snapshot.setLastUpsertedCount(rawStatus.getLastUpsertedCount());
        snapshot.setLastClosedCount(rawStatus.getLastClosedCount());
        snapshot.setCurrentOpenPositionCount(rawStatus.getCurrentOpenPositionCount());

        snapshot.setAvailabilityStatus(resolvePositionSyncAvailabilityStatus(rawStatus));
        snapshot.setAvailabilityDetail(resolvePositionSyncAvailabilityDetail(rawStatus));
        return snapshot;
    }

    private RunBaselineVO.PerformanceSummary buildPerformanceSummary(long assembleStartMs) {
        RunBaselineVO.PerformanceSummary summary = new RunBaselineVO.PerformanceSummary();
        Map<String, RunBaselineVO.RuntimeMetricSnapshot> metrics = runtimeMetricService.snapshot();
        long totalSampleCount = 0L;
        if (metrics != null) {
            for (RunBaselineVO.RuntimeMetricSnapshot metric : metrics.values()) {
                if (metric != null && metric.getSampleCount() != null) {
                    totalSampleCount += metric.getSampleCount();
                }
            }
        }
        summary.setMetrics(metrics);
        summary.setTotalSampleCount(totalSampleCount);
        summary.setHasSamples(totalSampleCount > 0);
        summary.setSampleBoundaryDetail(totalSampleCount > 0
                ? "runtime metrics are sampled in-process snapshots, not a full performance observability dataset"
                : "no runtime samples recorded yet; metrics reflect an empty in-process sample set");
        summary.setBaselineAssembleDurationMs(System.currentTimeMillis() - assembleStartMs);
        return summary;
    }

    private RunBaselineVO.AlertSummary buildAlertSummary(LocalDateTime windowStartUtc, LocalDateTime asOfUtc) {
        int openCount = safeCount(monitorAlertMapper.countByStatusInWindow("OPEN", windowStartUtc, asOfUtc));
        int suppressedCount = safeCount(monitorAlertMapper.countByStatusInWindow(
                "SUPPRESSED", windowStartUtc, asOfUtc));
        int dataQualityOpenCount = safeCount(monitorAlertMapper.countByStatusAndTypeInWindow(
                "OPEN", MonitorAlertWriteServiceImpl.ALERT_TYPE_DATA_QUALITY_INSUFFICIENT,
                windowStartUtc, asOfUtc));
        int dataQualitySuppressedCount = safeCount(monitorAlertMapper.countByStatusAndTypeInWindow(
                "SUPPRESSED", MonitorAlertWriteServiceImpl.ALERT_TYPE_DATA_QUALITY_INSUFFICIENT,
                windowStartUtc, asOfUtc));

        RunBaselineVO.AlertSummary summary = new RunBaselineVO.AlertSummary();
        summary.setOpenCountWindow(openCount);
        summary.setSuppressedCountWindow(suppressedCount);
        summary.setSuppressionRatioWindow(ratio(suppressedCount, openCount + suppressedCount));
        summary.setDataQualityOpenCountWindow(dataQualityOpenCount);
        summary.setDataQualitySuppressedCountWindow(dataQualitySuppressedCount);
        summary.setDataQualitySuppressionRatioWindow(ratio(dataQualitySuppressedCount,
                dataQualityOpenCount + dataQualitySuppressedCount));
        return summary;
    }

    private RunBaselineVO.DataQualitySummary buildDataQualitySummary(
            LocalDateTime windowStartUtc,
            LocalDateTime asOfUtc) {
        int totalRuns = safeCount(analysisRunMapper.countInWindow(windowStartUtc, asOfUtc));
        int lowQualityRuns = safeCount(analysisRunMapper.countLowQualityInWindow(
                windowStartUtc, asOfUtc, DATA_QUALITY_THRESHOLD));

        RunBaselineVO.DataQualitySummary summary = new RunBaselineVO.DataQualitySummary();
        summary.setAnalysisRunCountWindow(totalRuns);
        summary.setLowQualityCountWindow(lowQualityRuns);
        summary.setLowQualityRatioWindow(ratio(lowQualityRuns, totalRuns));
        summary.setLowQualityThreshold(DATA_QUALITY_THRESHOLD);
        return summary;
    }

    private RunBaselineVO.RecheckSummary buildRecheckSummary(
            LocalDateTime windowStartUtc,
            LocalDateTime asOfUtc) {
        RunBaselineVO.RecheckSummary summary = new RunBaselineVO.RecheckSummary();
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        int total = 0;
        for (RecheckStatusEnum statusEnum : RecheckStatusEnum.values()) {
            int count = safeCount(pushRecheckLogMapper.countByStatusInWindow(
                    statusEnum.name(), windowStartUtc, asOfUtc));
            counts.put(statusEnum.name(), count);
            total += count;
        }
        summary.setTotalCountWindow(total);
        summary.setStatusCountsWindow(counts);
        return summary;
    }

    private RunBaselineVO.HotResetSummary buildHotResetSummary(
            LocalDateTime windowStartUtc,
            LocalDateTime asOfUtc) {
        RunBaselineVO.HotResetSummary summary = new RunBaselineVO.HotResetSummary();
        summary.setEventCountWindow(safeCount(hotResetEventMapper.countInWindow(windowStartUtc, asOfUtc)));

        LinkedHashMap<String, Integer> triggerTypeCounts = new LinkedHashMap<>();
        List<KeyCountVO> rows = hotResetEventMapper.selectTriggerTypeCountsInWindow(windowStartUtc, asOfUtc);
        if (rows != null) {
            for (KeyCountVO row : rows) {
                if (row == null || row.getKey() == null || row.getKey().trim().isEmpty()) {
                    continue;
                }
                triggerTypeCounts.put(row.getKey(), safeCount(row.getCount()));
            }
        }
        summary.setTriggerTypeCountsWindow(triggerTypeCounts);

        LightSystemStatusVO latestHotReset = decisionService.getLightSystemStatus();
        summary.setLatestHotResetFired(Boolean.TRUE.equals(latestHotReset.getHotResetFired()));
        summary.setLatestHotResetSymbol(latestHotReset.getHotResetSymbol());
        summary.setLatestHotResetTriggerType(latestHotReset.getHotResetTriggerType());
        summary.setLatestHotResetTriggerValue(latestHotReset.getHotResetTriggerValue());
        summary.setLatestHotResetTime(latestHotReset.getHotResetTime());
        return summary;
    }

    private static int safeCount(Integer value) {
        return value != null ? value : 0;
    }

    private String resolvePositionSyncAvailabilityStatus(PositionSyncStatusVO rawStatus) {
        if (rawStatus.getLastSyncEndTime() == null) {
            return STATUS_UNKNOWN;
        }
        if (!Boolean.TRUE.equals(rawStatus.getLastSyncSuccess())) {
            return STATUS_UNKNOWN;
        }
        if (STATUS_STALE.equalsIgnoreCase(rawStatus.getFreshnessStatus())) {
            return STATUS_STALE;
        }
        if (STATUS_FRESH.equalsIgnoreCase(rawStatus.getFreshnessStatus())) {
            return STATUS_FRESH;
        }
        return STATUS_UNKNOWN;
    }

    private String resolvePositionSyncAvailabilityDetail(PositionSyncStatusVO rawStatus) {
        if (rawStatus.getLastSyncStartTime() == null && rawStatus.getLastSyncEndTime() == null) {
            return "no sync execution has been observed yet, so current position availability is unknown";
        }
        if (rawStatus.getLastSyncEndTime() == null) {
            return "latest sync has started but has not completed yet, so current position availability is unknown";
        }
        if (!Boolean.TRUE.equals(rawStatus.getLastSyncSuccess())) {
            return "latest sync completed without success, so current position freshness cannot be confirmed";
        }
        if (STATUS_STALE.equalsIgnoreCase(rawStatus.getFreshnessStatus())) {
            return "latest successful position sync is older than the stale threshold";
        }
        if (STATUS_FRESH.equalsIgnoreCase(rawStatus.getFreshnessStatus())) {
            return "latest successful position sync is still within the stale threshold";
        }
        return "position sync freshness is not currently determinable";
    }

    private static BigDecimal ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private static String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
