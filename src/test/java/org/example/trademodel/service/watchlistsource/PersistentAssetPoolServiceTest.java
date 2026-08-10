package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetPoolItemDO;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PersistentAssetPoolServiceTest {

    @Mock
    private AssetPoolItemMapper mapper;
    @Mock
    private MarketAssetCatalog marketAssetCatalog;
    @Mock
    private AnalysisRunOrchestrator analysisRunOrchestrator;

    private PersistentAssetPoolService service;

    @BeforeEach
    void setUp() {
        service = new PersistentAssetPoolService(mapper, marketAssetCatalog, analysisRunOrchestrator);
    }

    @Test
    void userOverridesApplyOnTopOfSystemDefaultsWithoutInventingAssets() {
        when(mapper.listSystemDefaults()).thenReturn(List.of(
                row("SYSTEM", 0L, "BTCUSDT", true, true, 10, "DEFAULT"),
                row("SYSTEM", 0L, "ETHUSDT", true, true, 20, "DEFAULT")));
        when(mapper.listUserOverrides(7L)).thenReturn(List.of(
                row("USER", 7L, "ETHUSDT", false, false, 30, "USER_OVERRIDE"),
                row("USER", 7L, "XRPUSDT", true, true, 40, "USER_ADDED")));

        List<AssetPoolAssetDTO> assets = service.listForUser(7L);

        assertThat(assets).extracting(AssetPoolAssetDTO::symbol)
                .containsExactly("BTCUSDT", "XRPUSDT");
        assertThat(service.listFocusSymbols(7L, 6)).containsExactly("BTCUSDT", "XRPUSDT");
    }

    @Test
    void addRemoveAndRestorePersistOnlyExplicitUserIntent() {
        when(marketAssetCatalog.requireTradable("eth/usdt"))
                .thenReturn(new MarketAssetDTO("ETHUSDT", "ETH", "USDT", "SPOT"));
        when(mapper.maxUserSortOrder(9L)).thenReturn(20);

        AssetPoolAssetDTO added = service.addForUser(9L, "eth/usdt", true);

        ArgumentCaptor<AssetPoolItemDO> writes = ArgumentCaptor.forClass(AssetPoolItemDO.class);
        verify(mapper).upsert(writes.capture());
        assertThat(added.symbol()).isEqualTo("ETHUSDT");
        assertThat(writes.getValue().getOwnerType()).isEqualTo("USER");
        assertThat(writes.getValue().getOwnerId()).isEqualTo(9L);
        assertThat(writes.getValue().getSourceType()).isEqualTo("USER_ADDED");
        assertThat(writes.getValue().getActive()).isTrue();

        service.removeForUser(9L, "eth-usdt");

        verify(mapper, org.mockito.Mockito.times(2)).upsert(writes.capture());
        AssetPoolItemDO removed = writes.getAllValues().get(2);
        assertThat(removed.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(removed.getSourceType()).isEqualTo("USER_OVERRIDE");
        assertThat(removed.getActive()).isFalse();
        assertThat(removed.getFocusEnabled()).isFalse();

        when(mapper.listSystemDefaults()).thenReturn(List.of(
                row("SYSTEM", 0L, "BTCUSDT", true, true, 10, "DEFAULT")));
        when(mapper.listUserOverrides(9L)).thenReturn(List.of());
        assertThat(service.restoreDefaults(9L)).extracting(AssetPoolAssetDTO::symbol)
                .containsExactly("BTCUSDT");
        verify(mapper).deleteUserOverrides(9L);
    }

    @Test
    void searchAndOpportunityMembershipAreDelegatedToCanonicalSources() {
        when(marketAssetCatalog.search("eth", 25))
                .thenReturn(List.of(new MarketAssetDTO("ETHUSDT", "ETH", "USDT", "SPOT")));
        when(mapper.countActiveBySymbol("ETHUSDT")).thenReturn(1);

        assertThat(service.searchMarket("eth", 25)).extracting(MarketAssetDTO::symbol)
                .containsExactly("ETHUSDT");
        assertThat(service.isOpportunitySource("eth/usdt")).isTrue();
        assertThat(service.isOpportunitySource("unknownusdt")).isFalse();
    }

    @Test
    void scanUsesOnlyEffectiveAssetPoolAndMarksEveryRunAsAssetPoolScan() {
        when(mapper.listSystemDefaults()).thenReturn(List.of(
                row("SYSTEM", 0L, "BTCUSDT", true, true, 10, "DEFAULT"),
                row("SYSTEM", 0L, "ETHUSDT", true, false, 20, "DEFAULT")));
        when(mapper.listUserOverrides(11L)).thenReturn(List.of());
        when(analysisRunOrchestrator.run(any())).thenAnswer(invocation -> {
            AnalysisRunCommand command = invocation.getArgument(0);
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId("analysis-" + command.getSymbol());
            run.setTraceId("trace-" + command.getSymbol());
            run.setStatus("SUCCESS");
            return AnalysisRunResult.executed(run, null, false, false);
        });

        List<AssetPoolScanResultDTO> results = service.scanForUser(11L, "15m");

        assertThat(results).extracting(AssetPoolScanResultDTO::symbol)
                .containsExactly("BTCUSDT", "ETHUSDT");
        ArgumentCaptor<AnalysisRunCommand> commands = ArgumentCaptor.forClass(AnalysisRunCommand.class);
        verify(analysisRunOrchestrator, org.mockito.Mockito.times(2)).run(commands.capture());
        assertThat(commands.getAllValues()).allSatisfy(command -> {
            assertThat(command.getTriggerType()).isEqualTo(AnalysisRunTriggerType.ASSET_POOL_SCAN);
            assertThat(command.getTimeframe()).isEqualTo("15m");
            assertThat(command.getTriggerReference()).startsWith("asset-pool-scan-");
        });
    }

    private static AssetPoolItemDO row(String ownerType,
                                       Long ownerId,
                                       String symbol,
                                       boolean active,
                                       boolean focus,
                                       int sortOrder,
                                       String sourceType) {
        AssetPoolItemDO row = new AssetPoolItemDO();
        row.setOwnerType(ownerType);
        row.setOwnerId(ownerId);
        row.setSymbol(symbol);
        row.setDisplayName(symbol.replace("USDT", ""));
        row.setMarketType("SPOT");
        row.setQuoteAsset("USDT");
        row.setActive(active);
        row.setFocusEnabled(focus);
        row.setSortOrder(sortOrder);
        row.setSourceType(sourceType);
        return row;
    }
}
