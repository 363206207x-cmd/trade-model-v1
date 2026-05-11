package org.example.trademodel.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.position.PositionProvider;
import org.example.trademodel.position.PositionProviderResult;
import org.example.trademodel.position.PositionSnapshot;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PositionSyncService {

    private static final Logger log = LoggerFactory.getLogger(PositionSyncService.class);
    private static final int SYNC_STALE_MINUTES = 10;

    private final PositionProvider positionProvider;
    private final RealPositionMapper realPositionMapper;
    private final String configuredProviderType;
    private final RuntimeMetricService runtimeMetricService;
    private final Object statusLock = new Object();
    private final PositionSyncStatusVO latestStatus = new PositionSyncStatusVO();

    public PositionSyncService(PositionProvider positionProvider,
                               RealPositionMapper realPositionMapper,
                               RuntimeMetricService runtimeMetricService,
                               @Value("${position.provider.type:SIMULATED}") String configuredProviderType) {
        this.positionProvider = positionProvider;
        this.realPositionMapper = realPositionMapper;
        this.runtimeMetricService = runtimeMetricService;
        this.configuredProviderType = normalizeProviderType(configuredProviderType);
        this.latestStatus.setConfiguredProviderType(this.configuredProviderType);
        this.latestStatus.setLastSyncMessage("sync not started yet");
        this.latestStatus.setCurrentOpenPositionCount(safeCountOpenPositions());
    }

    @Transactional
    public void syncPositions() {
        LocalDateTime startTime = LocalDateTime.now();
        updateStatusOnStart(startTime);
        log.info("[position-sync] start sync positions");
        try {
            PositionProviderResult result = positionProvider.fetchOpenPositions();
            List<PositionSnapshot> openPositions = result != null ? result.getOpenPositions() : null;
            if (openPositions == null) {
                openPositions = new ArrayList<>();
            }
            String sourceType = result != null ? safeText(result.getSourceType(), "UNKNOWN") : "UNKNOWN";
            String sourceName = result != null ? safeText(result.getSourceName(), "unknown-provider") : "unknown-provider";
            String configuredType = result != null
                    ? safeText(result.getConfiguredProviderType(), this.configuredProviderType)
                    : this.configuredProviderType;
            boolean fallbackOccurred = result != null && result.isFallbackOccurred();
            String fallbackReason = result != null ? safeText(result.getFallbackReason(), null) : null;
            log.info("[position-sync] provider={} sourceType={} fetchedOpenCount={}", sourceName, sourceType, openPositions.size());

            int upserted = 0;
            Set<String> openSymbols = new LinkedHashSet<>();
            for (PositionSnapshot position : openPositions) {
                String symbol = normalizeSymbol(position.getSymbol());
                if (symbol == null) {
                    continue;
                }
                openSymbols.add(symbol);
                int affected = realPositionMapper.updateOpenPositionBySymbol(
                        symbol,
                        sourceType,
                        sourceName,
                        position.getPositionSide(),
                        position.getAvgOpenPrice(),
                        position.getPositionOpenTime(),
                        position.getPositionQuantity(),
                        position.getUnrealizedPnlPct(),
                        position.getMarkPrice(),
                        position.getBreakEvenPrice(),
                        position.getLiquidationPrice(),
                        startTime
                );
                if (affected == 0) {
                    // 手动录入持仓保护：如果已经存在同 symbol 的手动 OPEN，则不要插入同步来源行，避免覆盖/歧义
                    // （updateOpenPositionBySymbol 已排除了 MANUAL_INPUT，但 affected==0 仍可能触发 insert）。
                    int manualOpenCount = realPositionMapper.countOpenManualPositionsBySymbol(symbol);
                    if (manualOpenCount > 0) {
                        continue;
                    }
                    realPositionMapper.insertOpenPosition(
                            UUID.randomUUID().toString(),
                            symbol,
                            sourceType,
                            sourceName,
                            position.getPositionSide(),
                            position.getAvgOpenPrice(),
                            position.getPositionOpenTime(),
                            position.getPositionQuantity(),
                            position.getUnrealizedPnlPct(),
                            position.getMarkPrice(),
                            position.getBreakEvenPrice(),
                            position.getLiquidationPrice(),
                            startTime
                    );
                }
                upserted++;
            }

            int closed = realPositionMapper.closeMissingOpenPositions(new ArrayList<>(openSymbols), startTime);
            int currentOpenCount = safeCountOpenPositions();
            log.info("[position-sync] upserted={} closed={} sourceType={} sourceName={}", upserted, closed, sourceType, sourceName);
            updateStatusOnSuccess(
                    configuredType,
                    sourceType,
                    sourceName,
                    fallbackOccurred,
                    fallbackReason,
                    startTime,
                    LocalDateTime.now(),
                    openPositions.size(),
                    upserted,
                    closed,
                    currentOpenCount
            );
        } catch (Exception e) {
            log.warn("[position-sync] sync failed err={}", e.getMessage(), e);
            updateStatusOnFailure(startTime, LocalDateTime.now(), e.getMessage());
        }
    }

    public PositionSyncStatusVO getPositionSyncStatus() {
        long methodStart = System.currentTimeMillis();
        synchronized (statusLock) {
            PositionSyncStatusVO snapshot = new PositionSyncStatusVO();
            snapshot.setFreshnessStatus(resolveFreshnessStatus(latestStatus.getLastSyncEndTime()));
            snapshot.setFreshnessDetail(buildFreshnessDetail(latestStatus.getLastSyncEndTime(), latestStatus.getLastSyncSuccess()));
            snapshot.setStaleThresholdMinutes(SYNC_STALE_MINUTES);
            snapshot.setConfiguredProviderType(latestStatus.getConfiguredProviderType());
            snapshot.setActiveProviderType(latestStatus.getActiveProviderType());
            snapshot.setActiveProviderName(latestStatus.getActiveProviderName());
            snapshot.setFallbackOccurred(latestStatus.getFallbackOccurred());
            snapshot.setFallbackReason(latestStatus.getFallbackReason());
            snapshot.setLastSyncStartTime(latestStatus.getLastSyncStartTime());
            snapshot.setLastSyncEndTime(latestStatus.getLastSyncEndTime());
            snapshot.setLastSyncSuccess(latestStatus.getLastSyncSuccess());
            snapshot.setLastSyncMessage(latestStatus.getLastSyncMessage());
            snapshot.setLastFetchedOpenCount(latestStatus.getLastFetchedOpenCount());
            snapshot.setLastUpsertedCount(latestStatus.getLastUpsertedCount());
            snapshot.setLastClosedCount(latestStatus.getLastClosedCount());
            snapshot.setCurrentOpenPositionCount(latestStatus.getCurrentOpenPositionCount());
            runtimeMetricService.recordDuration("positionSync.getStatus", System.currentTimeMillis() - methodStart);
            return snapshot;
        }
    }

    public int getStaleThresholdMinutes() {
        return SYNC_STALE_MINUTES;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeText(String text, String fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        return text.trim();
    }

    private String normalizeProviderType(String providerType) {
        if (providerType == null || providerType.trim().isEmpty()) {
            return "SIMULATED";
        }
        return providerType.trim().toUpperCase();
    }

    private void updateStatusOnStart(LocalDateTime startTime) {
        synchronized (statusLock) {
            latestStatus.setConfiguredProviderType(this.configuredProviderType);
            latestStatus.setLastSyncStartTime(startTime);
            latestStatus.setLastSyncEndTime(null);
            latestStatus.setLastSyncSuccess(null);
            latestStatus.setLastSyncMessage("sync in progress");
            latestStatus.setLastFetchedOpenCount(null);
            latestStatus.setLastUpsertedCount(null);
            latestStatus.setLastClosedCount(null);
        }
    }

    private void updateStatusOnSuccess(String configuredType,
                                       String activeProviderType,
                                       String activeProviderName,
                                       boolean fallbackOccurred,
                                       String fallbackReason,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime,
                                       int fetchedCount,
                                       int upsertedCount,
                                       int closedCount,
                                       int currentOpenCount) {
        synchronized (statusLock) {
            latestStatus.setConfiguredProviderType(configuredType);
            latestStatus.setActiveProviderType(activeProviderType);
            latestStatus.setActiveProviderName(activeProviderName);
            latestStatus.setFallbackOccurred(fallbackOccurred);
            latestStatus.setFallbackReason(fallbackOccurred ? fallbackReason : null);
            latestStatus.setLastSyncStartTime(startTime);
            latestStatus.setLastSyncEndTime(endTime);
            latestStatus.setLastSyncSuccess(true);
            latestStatus.setLastSyncMessage(fallbackOccurred ? "sync success with fallback" : "sync success");
            latestStatus.setLastFetchedOpenCount(fetchedCount);
            latestStatus.setLastUpsertedCount(upsertedCount);
            latestStatus.setLastClosedCount(closedCount);
            latestStatus.setCurrentOpenPositionCount(currentOpenCount);
        }
    }

    private void updateStatusOnFailure(LocalDateTime startTime, LocalDateTime endTime, String errorMessage) {
        synchronized (statusLock) {
            latestStatus.setConfiguredProviderType(this.configuredProviderType);
            latestStatus.setLastSyncStartTime(startTime);
            latestStatus.setLastSyncEndTime(endTime);
            latestStatus.setLastSyncSuccess(false);
            latestStatus.setLastSyncMessage("sync failed: " + safeText(errorMessage, "unknown error"));
            latestStatus.setCurrentOpenPositionCount(safeCountOpenPositions());
        }
    }

    private int safeCountOpenPositions() {
        try {
            return realPositionMapper.countOpenPositions();
        } catch (Exception e) {
            log.warn("[position-sync] count open positions failed err={}", e.getMessage());
            return 0;
        }
    }

    private String resolveFreshnessStatus(LocalDateTime lastSyncEndTime) {
        if (lastSyncEndTime == null) {
            return "UNKNOWN";
        }
        LocalDateTime staleCutoff = LocalDateTime.now().minusMinutes(SYNC_STALE_MINUTES);
        return lastSyncEndTime.isBefore(staleCutoff) ? "STALE" : "FRESH";
    }

    private String buildFreshnessDetail(LocalDateTime lastSyncEndTime, Boolean lastSyncSuccess) {
        if (lastSyncEndTime == null) {
            return "no completed sync observed yet";
        }
        if (Boolean.FALSE.equals(lastSyncSuccess)) {
            return "last sync finished with failure";
        }
        LocalDateTime staleCutoff = LocalDateTime.now().minusMinutes(SYNC_STALE_MINUTES);
        if (lastSyncEndTime.isBefore(staleCutoff)) {
            return "last successful sync is older than stale threshold";
        }
        return "last completed sync is within stale threshold";
    }
}
