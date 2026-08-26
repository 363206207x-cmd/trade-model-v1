package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class AssetStateMapperIntegrationTest {

    @Autowired
    private AssetStateMapper assetStateMapper;

    @Test
    void defaultH2UpsertUpdatesCoreFieldsAndPreservesHotResetColumns() {
        LocalDateTime initialTime = LocalDateTime.of(2026, 6, 28, 9, 0);
        AssetStateDO initial = row("PDR2C2AUSDT", AssetStateEnum.OBSERVING, 10, 0, initialTime, "trace-initial");
        assetStateMapper.mergeUpsertCore(initial);

        AssetStateDO hotReset = row("PDR2C2AUSDT", AssetStateEnum.HIGH_RISK, 30, 1,
                initialTime.plusMinutes(1), "trace-hot-reset");
        hotReset.setHotResetFlag(true);
        hotReset.setHotResetTriggerType("EXTREME_PRICE_MOVE");
        hotReset.setHotResetTriggerValue("7%");
        hotReset.setHotResetTime(initialTime.plusMinutes(2));
        hotReset.setPreResetState("TRIGGERED");
        hotReset.setPostResetState("HIGH_RISK");
        assetStateMapper.updateHotResetColumns(hotReset);

        AssetStateDO update = row("PDR2C2AUSDT", AssetStateEnum.CANDIDATE, 42, 2,
                initialTime.plusMinutes(3), "trace-updated");
        assetStateMapper.mergeUpsertCore(update);

        AssetStateDO persisted = assetStateMapper.selectBySymbol("PDR2C2AUSDT");
        assertThat(persisted.getState()).isEqualTo(AssetStateEnum.CANDIDATE);
        assertThat(persisted.getConfusedScore()).isEqualTo(42);
        assertThat(persisted.getConfusedLowStreak()).isEqualTo(2);
        assertThat(persisted.getTraceId()).isEqualTo("trace-updated");
        assertThat(persisted.getHotResetFlag()).isTrue();
        assertThat(persisted.getHotResetTriggerType()).isEqualTo("EXTREME_PRICE_MOVE");
        assertThat(persisted.getHotResetTriggerValue()).isEqualTo("7%");
        assertThat(persisted.getPreResetState()).isEqualTo("TRIGGERED");
        assertThat(persisted.getPostResetState()).isEqualTo("HIGH_RISK");
    }

    @Test
    void listBySymbolsReturnsOnlyRequestedOpportunitySourcesAcrossTimeframes() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 12, 0);
        AssetStateDO btc = row("BTCUSDT", AssetStateEnum.CANDIDATE, 0, 0, now, "trace-btc");
        btc.setTimeframe("5m");
        btc.setLastAnalysisId("analysis-btc");
        AssetStateDO link = row("LINKUSDT", AssetStateEnum.WAITING_TRIGGER, 0, 0, now, "trace-link");
        link.setTimeframe("1h");
        link.setLastAnalysisId("analysis-link");
        AssetStateDO sol = row("SOLUSDT", AssetStateEnum.OBSERVING, 0, 0, now, "trace-sol");
        assetStateMapper.mergeUpsertCore(btc);
        assetStateMapper.mergeUpsertCore(link);
        assetStateMapper.mergeUpsertCore(sol);

        List<AssetStateDO> rows = assetStateMapper.listBySymbols(List.of("BTCUSDT", "LINKUSDT"));

        assertThat(rows).extracting(AssetStateDO::getSymbol)
                .containsExactlyInAnyOrder("BTCUSDT", "LINKUSDT");
        assertThat(rows).noneMatch(row -> "SOLUSDT".equals(row.getSymbol()));
    }

    @Test
    void lightweightScanClaimIsAtomicAcrossSchedulerInstances() {
        LocalDateTime previous = LocalDateTime.of(2026, 8, 12, 9, 0);
        LocalDateTime started = previous.plusMinutes(16);
        AssetStateDO initial = row(
                "LOCKUSDT", AssetStateEnum.OBSERVING, 0, 0, previous, "trace-before");
        initial.setOwnerType("SYSTEM");
        initial.setOwnerId(0L);
        initial.setAssetId(null);
        initial.setPoolItemId(null);
        initial.setTimeframe("5m");
        initial.setOpportunityId("opp-user-lock-5m");
        assetStateMapper.mergeUpsertCore(initial);

        int first = assetStateMapper.claimScheduledScan(
                "SYSTEM", 0L, "LOCKUSDT", "5m", "OBSERVING", previous,
                started, "trace-first", "rules-v1");
        int second = assetStateMapper.claimScheduledScan(
                "SYSTEM", 0L, "LOCKUSDT", "5m", "OBSERVING", previous,
                started, "trace-second", "rules-v1");

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        AssetStateDO persisted = assetStateMapper.selectByIdentity("SYSTEM", 0L, "LOCKUSDT", "5m");
        assertThat(persisted.getState()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(persisted.getLastUpdateTime()).isEqualTo(started);
        assertThat(persisted.getTraceId()).isEqualTo("trace-first");
    }

    @Test
    void scanAuditCanCompleteAgainstTheClaimOrItsResultingAnalysisTrace() {
        LocalDateTime started = LocalDateTime.of(2026, 8, 12, 10, 0);
        AssetStateDO initial = row(
                "AUDITUSDT", AssetStateEnum.TRIGGERED, 0, 0, started, "scan-trace");
        initial.setOwnerType("SYSTEM");
        initial.setOwnerId(0L);
        initial.setTimeframe("5m");
        assetStateMapper.mergeUpsertCore(initial);

        AssetStateDO analysisUpdate = row(
                "AUDITUSDT", AssetStateEnum.TRIGGERED, 0, 0,
                started.plusSeconds(5), "analysis-trace");
        analysisUpdate.setOwnerType("SYSTEM");
        analysisUpdate.setOwnerId(0L);
        analysisUpdate.setTimeframe("5m");
        assetStateMapper.mergeUpsertCore(analysisUpdate);

        AssetStateDO audit = new AssetStateDO();
        audit.setOwnerType("SYSTEM");
        audit.setOwnerId(0L);
        audit.setSymbol("AUDITUSDT");
        audit.setTimeframe("5m");
        audit.setExtJson("{\"schedulerScan\":{\"fullAnalysisSucceeded\":true}}");
        audit.setUpdatedAt(started.plusSeconds(6));

        int updated = assetStateMapper.completeScheduledScanAudit(
                audit, "scan-trace", "analysis-trace");

        assertThat(updated).isEqualTo(1);
        AssetStateDO persisted = assetStateMapper.selectByIdentity(
                "SYSTEM", 0L, "AUDITUSDT", "5m");
        assertThat(persisted.getExtJson()).contains("\"fullAnalysisSucceeded\":true");
        assertThat(persisted.getTraceId()).isEqualTo("analysis-trace");
        assertThat(persisted.getState()).isEqualTo(AssetStateEnum.TRIGGERED);
    }

    private static AssetStateDO row(String symbol, AssetStateEnum state, Integer confusedScore,
                                    Integer confusedLowStreak, LocalDateTime updateTime, String traceId) {
        AssetStateDO row = new AssetStateDO();
        row.setSymbol(symbol);
        row.setTimeframe("global");
        row.setOpportunityId("opp-" + symbol.toLowerCase() + "-global");
        row.setStateEnteredAt(updateTime);
        row.setState(state);
        row.setConfusedScore(confusedScore);
        row.setConfusedLowStreak(confusedLowStreak);
        row.setLastUpdateTime(updateTime);
        row.setTraceId(traceId);
        row.setRuleVersion("rules-asset-state-it");
        return row;
    }
}
