package org.example.trademodel.service.watchlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.trademodel.service.watchlist.WatchlistLowFrequencyScanScheduler.ScanRunResult;
import org.example.trademodel.service.watchlist.WatchlistLowFrequencyScanScheduler.ScanStatus;
import org.junit.jupiter.api.Test;

class WatchlistLowFrequencyScanSchedulerTest {

    @Test
    void defaultsToDisabledAndReturnsReviewOnlyDisabledResult() {
        WatchlistLowFrequencyScanScheduler scheduler = new WatchlistLowFrequencyScanScheduler();

        ScanRunResult result = scheduler.runScheduledScan();

        assertThat(scheduler.isEnabled()).isFalse();
        assertThat(result.getStatus()).isIn(
                ScanStatus.DISABLED,
                ScanStatus.SKIPPED,
                ScanStatus.REVIEW_ONLY,
                ScanStatus.NOT_IMPLEMENTED);
        assertThat(result.getStatus()).isEqualTo(ScanStatus.DISABLED);
        assertThat(result.getReason()).isEqualTo("LOW_FREQUENCY_SCAN_DISABLED_BY_DEFAULT");
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
    }

    @Test
    void disabledSkeletonDoesNotCallMarketDataOrCreatePushReadinessOrTradingActions() {
        WatchlistLowFrequencyScanScheduler scheduler = new WatchlistLowFrequencyScanScheduler(false);

        ScanRunResult result = scheduler.runScheduledScan();

        assertThat(result.isMarketDataCalled()).isFalse();
        assertThat(result.isOpportunityPushCreated()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }

    @Test
    void disabledSkeletonDoesNotGenerateScanScoreAttentionOrPromoteToHome() {
        ScanRunResult result = new WatchlistLowFrequencyScanScheduler().runScheduledScan();

        assertThat(result.isScanScoreGenerated()).isFalse();
        assertThat(result.isCandidateAttentionGenerated()).isFalse();
        assertThat(result.isPromoteToHomeGenerated()).isFalse();
    }

    @Test
    void disabledSkeletonDoesNotScanDisplaySlotsOrNonWatchlistAssets() {
        ScanRunResult result = new WatchlistLowFrequencyScanScheduler().runScheduledScan();

        assertThat(result.isWatchlistPoolScanned()).isFalse();
        assertThat(result.isDisplaySlotsScannedAsUniverse()).isFalse();
        assertThat(result.isNonWatchlistAssetsScanned()).isFalse();
    }

    @Test
    void schedulerDoesNotDeclareForbiddenRuntimeDependencies() {
        Set<String> fieldTypeNames = Arrays.stream(WatchlistLowFrequencyScanScheduler.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName)
                .collect(Collectors.toSet());

        assertThat(fieldTypeNames).noneMatch(name -> name.contains("MarketQuoteClient"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("BinanceMarketQuoteClient"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("PushRecheckService"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("PushSnapshotService"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("RuleConfigService"));
        assertThat(fieldTypeNames).noneMatch(name -> name.endsWith("Mapper"));
    }

    @Test
    void enabledConstructorStillStaysNotImplementedAndDoesNotScan() {
        WatchlistLowFrequencyScanScheduler scheduler = new WatchlistLowFrequencyScanScheduler(true);

        ScanRunResult result = scheduler.runScheduledScan();

        assertThat(scheduler.isEnabled()).isTrue();
        assertThat(result.getStatus()).isEqualTo(ScanStatus.NOT_IMPLEMENTED);
        assertThat(result.isMarketDataCalled()).isFalse();
        assertThat(result.isWatchlistPoolScanned()).isFalse();
        assertThat(result.isOpportunityPushCreated()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }

    @Test
    void frequencyConstantsAreDocumentedButDoNotEnableRuntimeScanning() {
        assertThat(WatchlistLowFrequencyScanScheduler.NORMAL_WATCHLIST_POOL_SCAN_INTERVAL_MILLIS)
                .isEqualTo(15L * 60L * 1000L);
        assertThat(WatchlistLowFrequencyScanScheduler.ABNORMAL_CANDIDATE_RESCAN_INTERVAL_MILLIS)
                .isEqualTo(5L * 60L * 1000L);
        assertThat(WatchlistLowFrequencyScanScheduler.HOME_PROMOTED_REVIEW_INTERVAL_MIN_MILLIS)
                .isEqualTo(60L * 1000L);
        assertThat(WatchlistLowFrequencyScanScheduler.HOME_PROMOTED_REVIEW_INTERVAL_MAX_MILLIS)
                .isEqualTo(3L * 60L * 1000L);
        assertThat(WatchlistLowFrequencyScanScheduler.POSITION_MONITOR_INTERVAL_MIN_MILLIS)
                .isEqualTo(30L * 1000L);
        assertThat(WatchlistLowFrequencyScanScheduler.POSITION_MONITOR_INTERVAL_MAX_MILLIS)
                .isEqualTo(60L * 1000L);
    }
}
