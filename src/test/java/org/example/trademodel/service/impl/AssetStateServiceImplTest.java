package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class AssetStateServiceImplTest {

    @Mock
    private AssetStateMapper assetStateMapper;
    @Mock
    private HotResetEventMapper hotResetEventMapper;

    private AssetStateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper);
    }

    @Test
    void recordHotResetEvent_blankSymbol_skipsAllWrites() {
        service.recordHotResetEvent("a-1", "tr-1", "   ", "CONFUSED", "42",
                "d-1", AssetStateEnum.CONFUSED, 42, false,
                "HOT_RESET_MIN_RULE", "reason", 1, LocalDateTime.now(),
                AssetStateEnum.CONFUSED, AssetStateEnum.OBSERVING);

        verify(assetStateMapper, never()).mergeUpsertCore(any());
        verify(assetStateMapper, never()).updateHotResetColumns(any());
        verify(hotResetEventMapper, never()).insert(any());
    }

    @Test
    void recordHotResetEvent_missingStateRow_seedsAndWritesEvent() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        service.recordHotResetEvent("a-2", "tr-2", " BTCUSDT ", "CONFUSED", "41",
                "d-2", AssetStateEnum.CONFUSED, 41, false,
                "HOT_RESET_MIN_RULE", "reason", 1, LocalDateTime.now(),
                AssetStateEnum.CONFUSED, AssetStateEnum.OBSERVING);

        verify(assetStateMapper).mergeUpsertCore(any(AssetStateDO.class));
        verify(assetStateMapper).updateHotResetColumns(any(AssetStateDO.class));
        verify(hotResetEventMapper).insert(any(HotResetEventDO.class));
    }

    @Test
    void recordHotResetEvent_blankAnalysisId_updatesStateButSkipsEventInsert() {
        when(assetStateMapper.selectBySymbol("ETHUSDT")).thenReturn(new AssetStateDO());

        service.recordHotResetEvent("   ", "tr-3", "ETHUSDT", "CONFUSED", "40",
                "d-3", AssetStateEnum.CONFUSED, 40, false,
                "HOT_RESET_MIN_RULE", "reason", 1, LocalDateTime.now(),
                AssetStateEnum.CONFUSED, AssetStateEnum.OBSERVING);

        verify(assetStateMapper).updateHotResetColumns(any(AssetStateDO.class));
        verify(hotResetEventMapper, never()).insert(any());
    }

    @Test
    void persistAuthoritativeState_trimsSymbolBeforeUpsert() {
        service.persistAuthoritativeState(" SOLUSDT ", AssetStateEnum.CANDIDATE, 12, "tr-9");

        ArgumentCaptor<AssetStateDO> captor = ArgumentCaptor.forClass(AssetStateDO.class);
        verify(assetStateMapper).mergeUpsertCore(captor.capture());
        assertThat(captor.getValue().getSymbol()).isEqualTo("SOLUSDT");
    }
}
