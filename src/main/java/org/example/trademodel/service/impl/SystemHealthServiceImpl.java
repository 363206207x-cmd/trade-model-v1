package org.example.trademodel.service.impl;

import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemHealthServiceImpl implements SystemHealthService {

    private static final int DATABASE_VALIDATION_TIMEOUT_SECONDS = 2;
    private static final int SCHEDULER_STALE_MINUTES = 10;

    private final DataSource dataSource;
    private final PositionSyncService positionSyncService;

    public SystemHealthServiceImpl(DataSource dataSource, PositionSyncService positionSyncService) {
        this.dataSource = dataSource;
        this.positionSyncService = positionSyncService;
    }

    @Override
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = totalMemory == 0 ? 0.0 : (usedMemory * 100.0 / totalMemory);

        double loadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        double cpuUsage = loadAverage < 0 ? 0.0 : Math.min(loadAverage * 100.0, 100.0);
        DatabaseCheckResult databaseCheckResult = probeDatabase();
        SchedulerCheckResult schedulerCheckResult = probeScheduler();

        health.put("cpuUsage", formatPercent(cpuUsage));
        health.put("memoryUsage", formatPercent(memoryUsage));
        health.put("databaseStatus", databaseCheckResult.status);
        health.put("databaseStatusDetail", databaseCheckResult.detail);
        health.put("schedulerStatus", schedulerCheckResult.status);
        health.put("schedulerStatusDetail", schedulerCheckResult.detail);
        return health;
    }

    private String formatPercent(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private DatabaseCheckResult probeDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(DATABASE_VALIDATION_TIMEOUT_SECONDS);
            if (valid) {
                return new DatabaseCheckResult("UP", "database connection probe succeeded");
            }
            return new DatabaseCheckResult("DOWN", "database connection probe returned invalid");
        } catch (Exception e) {
            return new DatabaseCheckResult("ERROR", "database probe failed: " + safeMessage(e.getMessage()));
        }
    }

    private SchedulerCheckResult probeScheduler() {
        PositionSyncStatusVO syncStatus = positionSyncService.getPositionSyncStatus();
        LocalDateTime lastSyncStartTime = syncStatus.getLastSyncStartTime();
        LocalDateTime lastSyncEndTime = syncStatus.getLastSyncEndTime();
        LocalDateTime latestActivityTime = latestActivityTime(lastSyncStartTime, lastSyncEndTime);
        if (latestActivityTime == null) {
            return new SchedulerCheckResult(
                    "NO_RECENT_ACTIVITY",
                    "scheduler is enabled but no recent position sync activity has been observed yet"
            );
        }
        LocalDateTime staleCutoff = LocalDateTime.now().minusMinutes(SCHEDULER_STALE_MINUTES);
        if (latestActivityTime.isBefore(staleCutoff)) {
            return new SchedulerCheckResult(
                    "STALE",
                    "last scheduler activity is older than " + SCHEDULER_STALE_MINUTES + " minutes"
            );
        }
        if (lastSyncStartTime != null && (lastSyncEndTime == null || lastSyncStartTime.isAfter(lastSyncEndTime))) {
            return new SchedulerCheckResult(
                    "RUNNING",
                    "recent position sync start detected within " + SCHEDULER_STALE_MINUTES + " minutes"
            );
        }
        return new SchedulerCheckResult(
                "RUNNING",
                "recent position sync completion detected within " + SCHEDULER_STALE_MINUTES + " minutes"
        );
    }

    private LocalDateTime latestActivityTime(LocalDateTime lastSyncStartTime, LocalDateTime lastSyncEndTime) {
        if (lastSyncStartTime == null) {
            return lastSyncEndTime;
        }
        if (lastSyncEndTime == null) {
            return lastSyncStartTime;
        }
        return lastSyncStartTime.isAfter(lastSyncEndTime) ? lastSyncStartTime : lastSyncEndTime;
    }

    private String safeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "unknown error";
        }
        return message.trim();
    }

    private static final class DatabaseCheckResult {
        private final String status;
        private final String detail;

        private DatabaseCheckResult(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }
    }

    private static final class SchedulerCheckResult {
        private final String status;
        private final String detail;

        private SchedulerCheckResult(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }
    }
}
