package org.example.trademodel.service.watchlist;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = {"trade-model.schedulers.enabled", "trade-model.schedulers.watchlist.enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class WatchlistLowFrequencyScanScheduler {

    public static final long NORMAL_WATCHLIST_POOL_SCAN_INTERVAL_MILLIS = 15L * 60L * 1000L;
    public static final long ABNORMAL_CANDIDATE_RESCAN_INTERVAL_MILLIS = 5L * 60L * 1000L;
    public static final long HOME_PROMOTED_REVIEW_INTERVAL_MIN_MILLIS = 60L * 1000L;
    public static final long HOME_PROMOTED_REVIEW_INTERVAL_MAX_MILLIS = 3L * 60L * 1000L;
    public static final long POSITION_MONITOR_INTERVAL_MIN_MILLIS = 30L * 1000L;
    public static final long POSITION_MONITOR_INTERVAL_MAX_MILLIS = 60L * 1000L;

    private final boolean enabled;

    public WatchlistLowFrequencyScanScheduler() {
        this(false);
    }

    public WatchlistLowFrequencyScanScheduler(boolean enabled) {
        this.enabled = enabled;
    }

    @Scheduled(
            initialDelay = NORMAL_WATCHLIST_POOL_SCAN_INTERVAL_MILLIS,
            fixedDelay = NORMAL_WATCHLIST_POOL_SCAN_INTERVAL_MILLIS)
    public ScanRunResult runScheduledScan() {
        if (!enabled) {
            return ScanRunResult.disabled("LOW_FREQUENCY_SCAN_DISABLED_BY_DEFAULT");
        }
        return ScanRunResult.notImplemented("LOW_FREQUENCY_SCAN_SKELETON_NOT_IMPLEMENTED_REVIEW_ONLY");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public enum ScanStatus {
        DISABLED,
        SKIPPED,
        REVIEW_ONLY,
        NOT_IMPLEMENTED
    }

    public static final class ScanRunResult {

        private final ScanStatus status;
        private final String reason;
        private final boolean notTradeInstruction;
        private final boolean manualReviewRequired;
        private final boolean marketDataCalled;
        private final boolean opportunityPushCreated;
        private final boolean readinessUpgraded;
        private final boolean tradingActionCreated;
        private final boolean watchlistPoolScanned;
        private final boolean displaySlotsScannedAsUniverse;
        private final boolean nonWatchlistAssetsScanned;
        private final boolean scanScoreGenerated;
        private final boolean candidateAttentionGenerated;
        private final boolean promoteToHomeGenerated;
        private final boolean entryStopTpRrGenerated;

        private ScanRunResult(ScanStatus status, String reason) {
            this.status = status;
            this.reason = reason;
            this.notTradeInstruction = true;
            this.manualReviewRequired = true;
            this.marketDataCalled = false;
            this.opportunityPushCreated = false;
            this.readinessUpgraded = false;
            this.tradingActionCreated = false;
            this.watchlistPoolScanned = false;
            this.displaySlotsScannedAsUniverse = false;
            this.nonWatchlistAssetsScanned = false;
            this.scanScoreGenerated = false;
            this.candidateAttentionGenerated = false;
            this.promoteToHomeGenerated = false;
            this.entryStopTpRrGenerated = false;
        }

        public static ScanRunResult disabled(String reason) {
            return new ScanRunResult(ScanStatus.DISABLED, reason);
        }

        public static ScanRunResult notImplemented(String reason) {
            return new ScanRunResult(ScanStatus.NOT_IMPLEMENTED, reason);
        }

        public ScanStatus getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public boolean isNotTradeInstruction() {
            return notTradeInstruction;
        }

        public boolean isManualReviewRequired() {
            return manualReviewRequired;
        }

        public boolean isMarketDataCalled() {
            return marketDataCalled;
        }

        public boolean isOpportunityPushCreated() {
            return opportunityPushCreated;
        }

        public boolean isReadinessUpgraded() {
            return readinessUpgraded;
        }

        public boolean isTradingActionCreated() {
            return tradingActionCreated;
        }

        public boolean isWatchlistPoolScanned() {
            return watchlistPoolScanned;
        }

        public boolean isDisplaySlotsScannedAsUniverse() {
            return displaySlotsScannedAsUniverse;
        }

        public boolean isNonWatchlistAssetsScanned() {
            return nonWatchlistAssetsScanned;
        }

        public boolean isScanScoreGenerated() {
            return scanScoreGenerated;
        }

        public boolean isCandidateAttentionGenerated() {
            return candidateAttentionGenerated;
        }

        public boolean isPromoteToHomeGenerated() {
            return promoteToHomeGenerated;
        }

        public boolean isEntryStopTpRrGenerated() {
            return entryStopTpRrGenerated;
        }
    }
}
