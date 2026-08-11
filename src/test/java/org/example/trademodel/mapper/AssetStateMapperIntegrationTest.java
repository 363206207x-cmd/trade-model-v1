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
        return row;
    }
}
