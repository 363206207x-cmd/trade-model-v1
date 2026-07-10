package org.example.trademodel.providercall;

import org.example.trademodel.providercall.ai.AiCheckpoint;
import org.example.trademodel.providercall.ai.AiInvocationPolicy;
import org.example.trademodel.providercall.ai.AiInvocationRequest;
import org.example.trademodel.providercall.ai.AiInvocationStatus;
import org.example.trademodel.providercall.snapshot.AnalysisInputBundle;
import org.example.trademodel.providercall.snapshot.AnalysisInputBundleAssembler;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.OhlcvSnapshotReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisInputAndAiPolicyTest {

    @Test
    void scanProfileChangeDoesNotInvokeAi() {
        AiInvocationStatus status = new AiInvocationPolicy().decide(request(Set.of(), "hash-2", "hash-1"));
        assertThat(status).isEqualTo(AiInvocationStatus.SKIP_NOT_TRIGGERED);
    }

    @Test
    void routinePoolScanDoesNotInvokeAi() {
        AiInvocationStatus status = new AiInvocationPolicy().decide(request(Set.of(), "pool-hash", null));
        assertThat(status).isEqualTo(AiInvocationStatus.SKIP_NOT_TRIGGERED);
    }

    @Test
    void unchangedEvidenceSkipsAi() {
        AiInvocationStatus status = new AiInvocationPolicy().decide(
                request(Set.of(AiCheckpoint.ENTERED_CANDIDATE), "same", "same"));
        assertThat(status).isEqualTo(AiInvocationStatus.SKIP_SAME_EVIDENCE);
    }

    @Test
    void analysisInputBundleUsesAuthoritativeFourTimeframeReferences() {
        String trace = "analysis-trace-1";
        List<OhlcvSnapshotReference> refs = List.of(ref("5m", trace), ref("15m", trace),
                ref("1h", trace), ref("4h", trace));
        MarketPriceSnapshot price = new MarketPriceSnapshot("BTCUSDT", new BigDecimal("65000"), null, null, null,
                null, null, null, "BINANCE_PUBLIC", Instant.now(),
                metadata(ProviderDatasetType.PRICE, "GLOBAL", trace));
        AnalysisInputBundle bundle = new AnalysisInputBundleAssembler(
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC))
                .assemble("BTCUSDT", refs, price, null, null, null, null, "v1", trace);

        assertThat(bundle.traceId()).isEqualTo(trace);
        assertThat(List.of(bundle.ohlcv5m(), bundle.ohlcv15m(), bundle.ohlcv1h(), bundle.ohlcv4h()))
                .allMatch(ref -> trace.equals(ref.metadata().traceId()));
        assertThat(bundle.currentPrice().metadata().traceId()).isEqualTo(trace);
    }

    @Test
    void triggeredCheckpointRunsConfiguredRoleOnlyAfterGuardsPass() {
        AiInvocationRequest request = new AiInvocationRequest(Set.of(AiCheckpoint.PUSH_RECHECK),
                new BigDecimal("90"), new BigDecimal("60"), "new", "old",
                true, true, true, true, false);
        assertThat(new AiInvocationPolicy().decide(request)).isEqualTo(AiInvocationStatus.RUN_GEMINI);
    }

    private static AiInvocationRequest request(Set<AiCheckpoint> checkpoints, String evidence, String previous) {
        return new AiInvocationRequest(checkpoints, new BigDecimal("90"), new BigDecimal("60"),
                evidence, previous, true, true, true, false, false);
    }

    private static OhlcvSnapshotReference ref(String timeframe, String trace) {
        return new OhlcvSnapshotReference("BTCUSDT", timeframe, 1L, 100,
                "tm_persisted_ohlcv_bar", metadata(ProviderDatasetType.OHLCV, timeframe, trace));
    }

    private static ProviderSnapshotMetadata metadata(ProviderDatasetType type, String timeframe, String trace) {
        Instant now = Instant.parse("2026-07-10T10:00:00Z");
        ProviderRequestKey key = new ProviderRequestKey("TEST", type, "BTCUSDT", timeframe, "bucket");
        return new ProviderSnapshotMetadata("TEST", type, "BTCUSDT", timeframe, now, now, now.plusSeconds(60),
                UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, trace, key.canonical(), false, false,
                null, List.of());
    }
}
