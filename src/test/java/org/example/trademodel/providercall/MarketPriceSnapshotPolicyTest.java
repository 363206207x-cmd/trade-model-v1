package org.example.trademodel.providercall;

import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketPriceSnapshotPolicyTest {
    @Test void freshPositiveSnapshotIsFresh() { assertThat(MarketPriceSnapshotPolicy.isFresh(result("1", SnapshotFreshnessStatus.FRESH, null))).isTrue(); }
    @Test void stalePositiveSnapshotIsExplicitlyStale() { assertThat(MarketPriceSnapshotPolicy.isStale(result("1", SnapshotFreshnessStatus.STALE_READABLE, null))).isTrue(); }
    @Test void staleSnapshotReturnsQuoteStaleCode() { assertThat(MarketPriceSnapshotPolicy.failureCode(result("1", SnapshotFreshnessStatus.STALE_READABLE, null))).isEqualTo("QUOTE_STALE"); }
    @Test void unavailableSnapshotPreservesProviderCode() { assertThat(MarketPriceSnapshotPolicy.failureCode(result(null, SnapshotFreshnessStatus.UNAVAILABLE, "QUOTE_UNAVAILABLE"))).isEqualTo("QUOTE_UNAVAILABLE"); }
    @Test void unavailablePriceDoesNotBecomeZero() { assertThat(MarketPriceSnapshotPolicy.hasPositivePrice(result("0", SnapshotFreshnessStatus.FRESH, null))).isFalse(); }
    @Test void negativePriceIsNeverReadable() { assertThat(MarketPriceSnapshotPolicy.hasPositivePrice(result("-1", SnapshotFreshnessStatus.FRESH, null))).isFalse(); }

    private ProviderCallResult<MarketPriceSnapshot> result(String price, SnapshotFreshnessStatus freshness, String error) {
        Instant now = Instant.now();
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("TEST", ProviderDatasetType.PRICE,
                "BTCUSDT", "GLOBAL", now, now, now.plusSeconds(30),
                freshness == SnapshotFreshnessStatus.FRESH ? UnifiedSourceStatus.READY : UnifiedSourceStatus.STALE,
                freshness, "trace", "key", false, freshness == SnapshotFreshnessStatus.STALE_READABLE, error,
                error == null ? List.of() : List.of(error));
        MarketPriceSnapshot payload = price == null ? null : new MarketPriceSnapshot("BTCUSDT",
                new BigDecimal(price), null, null, null, null, null, null, "TEST", now, metadata);
        return new ProviderCallResult<>(payload, metadata, null);
    }
}
