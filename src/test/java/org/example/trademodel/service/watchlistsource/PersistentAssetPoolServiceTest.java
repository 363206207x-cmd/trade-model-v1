package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetAnalysisPreviewDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanBatchResultDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetPoolItemDO;
import org.example.trademodel.entity.AssetDO;
import org.example.trademodel.mapper.AssetMapper;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PersistentAssetPoolServiceTest {

    @Mock
    private AssetPoolItemMapper mapper;
    @Mock
    private AssetMapper assetMapper;
    @Mock
    private MarketAssetCatalog marketAssetCatalog;
    @Mock
    private AnalysisRunOrchestrator analysisRunOrchestrator;

    private PersistentAssetPoolService service;

    @BeforeEach
    void setUp() {
        service = new PersistentAssetPoolService(mapper, assetMapper, marketAssetCatalog, analysisRunOrchestrator);
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
    void addAndRemovePersistOnlyExplicitUserIntent() {
        when(marketAssetCatalog.requireTradable("eth/usdt"))
                .thenReturn(new MarketAssetDTO("ETHUSDT", "ETH", "USDT", "SPOT"));
        when(mapper.maxUserSortOrder(9L)).thenReturn(20);
        when(assetMapper.selectBySymbol("ETHUSDT")).thenReturn(asset(101L, "ETHUSDT"));

        AssetPoolAssetDTO added = service.addForUser(9L, "eth/usdt", true);

        ArgumentCaptor<AssetPoolItemDO> writes = ArgumentCaptor.forClass(AssetPoolItemDO.class);
        verify(mapper).upsert(writes.capture());
        assertThat(added.symbol()).isEqualTo("ETHUSDT");
        assertThat(writes.getValue().getOwnerType()).isEqualTo("USER");
        assertThat(writes.getValue().getOwnerId()).isEqualTo(9L);
        assertThat(writes.getValue().getAssetId()).isEqualTo(101L);
        assertThat(writes.getValue().getSourceType()).isEqualTo("USER_ADDED");
        assertThat(writes.getValue().getActive()).isTrue();

        service.removeForUser(9L, "eth-usdt");

        verify(mapper, org.mockito.Mockito.times(2)).upsert(writes.capture());
        AssetPoolItemDO removed = writes.getAllValues().get(2);
        assertThat(removed.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(removed.getSourceType()).isEqualTo("USER_OVERRIDE");
        assertThat(removed.getActive()).isFalse();
        assertThat(removed.getFocusEnabled()).isFalse();
        assertThat(removed.getWatchStatus()).isEqualTo("TRACKING_STOPPED");
        verify(marketAssetCatalog, org.mockito.Mockito.times(1)).requireTradable(any());
    }

    @Test
    void topUpDefaultsRestoresOnlyMissingDefaultsAndKeepsCustomAssets() {
        AssetPoolItemDO btcDefault = row("SYSTEM", 0L, "BTCUSDT", true, true, 10, "DEFAULT");
        AssetPoolItemDO disabledBtc = row("USER", 9L, "BTCUSDT", false, false, 30, "USER_OVERRIDE");
        AssetPoolItemDO customLink = row("USER", 9L, "LINKUSDT", true, true, 40, "USER_ADDED");
        AssetPoolItemDO restoredBtc = row("USER", 9L, "BTCUSDT", true, true, 10, "USER_OVERRIDE");
        when(mapper.listSystemDefaults()).thenReturn(List.of(btcDefault));
        when(mapper.listUserOverrides(9L)).thenReturn(
                List.of(disabledBtc, customLink), List.of(restoredBtc, customLink));

        List<AssetPoolAssetDTO> result = service.topUpDefaults(9L);

        assertThat(result).extracting(AssetPoolAssetDTO::symbol)
                .containsExactly("BTCUSDT", "LINKUSDT");
        ArgumentCaptor<AssetPoolItemDO> write = ArgumentCaptor.forClass(AssetPoolItemDO.class);
        verify(mapper).upsert(write.capture());
        assertThat(write.getValue().getSymbol()).isEqualTo("BTCUSDT");
        assertThat(write.getValue().getActive()).isTrue();
        assertThat(write.getValue().getWatchStatus()).isEqualTo("OBSERVING");
    }

    @Test
    void resetDefaultsRestoresSystemSetAndMarksCustomTrackingStopped() {
        AssetPoolItemDO btcDefault = row("SYSTEM", 0L, "BTCUSDT", true, true, 10, "DEFAULT");
        AssetPoolItemDO overriddenBtc = row("USER", 9L, "BTCUSDT", true, true, 20, "USER_OVERRIDE");
        AssetPoolItemDO customLink = row("USER", 9L, "LINKUSDT", true, true, 30, "USER_ADDED");
        AssetPoolItemDO stoppedLink = row("USER", 9L, "LINKUSDT", false, false, 30, "USER_OVERRIDE");
        stoppedLink.setWatchStatus("TRACKING_STOPPED");
        when(mapper.listSystemDefaults()).thenReturn(List.of(btcDefault));
        when(mapper.listUserOverrides(9L)).thenReturn(
                List.of(overriddenBtc, customLink), List.of(stoppedLink));

        List<AssetPoolAssetDTO> result = service.resetDefaults(9L);

        verify(mapper).deleteUserOverride(9L, "BTCUSDT");
        ArgumentCaptor<AssetPoolItemDO> write = ArgumentCaptor.forClass(AssetPoolItemDO.class);
        verify(mapper).upsert(write.capture());
        assertThat(write.getValue().getSymbol()).isEqualTo("LINKUSDT");
        assertThat(write.getValue().getActive()).isFalse();
        assertThat(write.getValue().getWatchStatus()).isEqualTo("TRACKING_STOPPED");
        assertThat(result).extracting(AssetPoolAssetDTO::symbol).containsExactly("BTCUSDT");
    }

    @Test
    void removalUsesCanonicalAssetAndDoesNotDependOnMarketProviderAvailability() {
        when(assetMapper.selectBySymbol("ETHUSDT")).thenReturn(asset(101L, "ETHUSDT"));
        when(mapper.selectByOwnerAndSymbol("USER", 9L, "ETHUSDT")).thenReturn(null);
        when(mapper.selectByOwnerAndSymbol("SYSTEM", 0L, "ETHUSDT"))
                .thenReturn(row("SYSTEM", 0L, "ETHUSDT", true, true, 20, "DEFAULT"));

        service.removeForUser(9L, "eth/usdt");

        verifyNoInteractions(marketAssetCatalog);
        ArgumentCaptor<AssetPoolItemDO> write = ArgumentCaptor.forClass(AssetPoolItemDO.class);
        verify(mapper).upsert(write.capture());
        assertThat(write.getValue().getAssetId()).isEqualTo(101L);
        assertThat(write.getValue().getActive()).isFalse();
        assertThat(write.getValue().getWatchStatus()).isEqualTo("TRACKING_STOPPED");
    }

    @Test
    void searchAndOpportunityMembershipAreScopedToTheEffectiveOwnerPool() {
        when(marketAssetCatalog.search("eth", 25))
                .thenReturn(List.of(new MarketAssetDTO("ETHUSDT", "ETH", "USDT", "SPOT")));
        when(mapper.listSystemDefaults()).thenReturn(List.of(
                row("SYSTEM", 0L, "ETHUSDT", true, true, 10, "DEFAULT")));
        when(mapper.listUserOverrides(41L)).thenReturn(List.of(
                row("USER", 41L, "ETHUSDT", false, false, 20, "USER_OVERRIDE")));
        when(mapper.listUserOverrides(42L)).thenReturn(List.of());
        when(mapper.listUserOverrides(99L)).thenReturn(List.of());

        assertThat(service.searchMarket("eth", 25)).extracting(MarketAssetDTO::symbol)
                .containsExactly("ETHUSDT");
        assertThat(service.isOpportunitySource("SYSTEM", 0L, 10L, "eth/usdt")).isTrue();
        assertThat(service.isOpportunitySource("USER", 41L, null, "eth/usdt")).isFalse();
        assertThat(service.isOpportunitySource("USER", 42L, 10L, "eth/usdt")).isTrue();
        assertThat(service.isOpportunitySource("USER", 42L, 999L, "eth/usdt")).isFalse();
        assertThat(service.isOpportunitySource("USER", 99L, null, "unknownusdt")).isFalse();
        assertThat(service.isOpportunitySource("USER", null, null, "eth/usdt")).isFalse();
    }

    @Test
    void batchAddRemoveAndSelectedScanUseOnlyExplicitEffectivePoolAssets() {
        when(marketAssetCatalog.requireTradable("AAVEUSDT"))
                .thenReturn(new MarketAssetDTO("AAVEUSDT", "AAVE", "USDT", "SPOT"));
        when(marketAssetCatalog.requireTradable("LINKUSDT"))
                .thenReturn(new MarketAssetDTO("LINKUSDT", "LINK", "USDT", "SPOT"));
        when(mapper.maxUserSortOrder(55L)).thenReturn(100, 110, 120, 130);
        when(assetMapper.selectBySymbol("AAVEUSDT")).thenReturn(asset(201L, "AAVEUSDT"));
        when(assetMapper.selectBySymbol("LINKUSDT")).thenReturn(asset(202L, "LINKUSDT"));

        List<AssetPoolAssetDTO> added = service.addManyForUser(
                55L, List.of("aave/usdt", "LINK-USDT", "aave_usdt"), true);

        assertThat(added).extracting(AssetPoolAssetDTO::symbol)
                .containsExactly("AAVEUSDT", "LINKUSDT");
        service.removeManyForUser(55L, List.of("aaveusdt", "LINK/USDT"));
        verify(mapper, org.mockito.Mockito.times(4)).upsert(any());

        when(mapper.listSystemDefaults()).thenReturn(List.of(
                row("SYSTEM", 0L, "BTCUSDT", true, true, 10, "DEFAULT"),
                row("SYSTEM", 0L, "ETHUSDT", true, true, 20, "DEFAULT")));
        when(mapper.listUserOverrides(55L)).thenReturn(List.of(
                row("USER", 55L, "LINKUSDT", true, true, 120, "USER_ADDED")));
        when(analysisRunOrchestrator.run(any())).thenAnswer(invocation -> {
            AnalysisRunCommand command = invocation.getArgument(0);
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId("analysis-" + command.getSymbol());
            run.setStatus("SUCCESS");
            return AnalysisRunResult.executed(run, null, false, false);
        });

        List<AssetPoolScanResultDTO> results = service.scanSelectedForUser(
                55L, List.of("linkusdt", "btcusdt"), "1h");

        assertThat(results).extracting(AssetPoolScanResultDTO::symbol)
                .containsExactly("BTCUSDT", "LINKUSDT");
        ArgumentCaptor<AnalysisRunCommand> scans = ArgumentCaptor.forClass(AnalysisRunCommand.class);
        verify(analysisRunOrchestrator, org.mockito.Mockito.times(2)).run(scans.capture());
        assertThat(scans.getAllValues()).allSatisfy(command -> {
            assertThat(command.getOwnerType()).isEqualTo("USER");
            assertThat(command.getOwnerId()).isEqualTo(55L);
            assertThat(command.getTimeframe()).isEqualTo("1h");
        });
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.scanSelectedForUser(55L, List.of("SOLUSDT"), "1h"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in the effective Asset Pool");
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

    @Test
    void oneAssetFailureDoesNotEraseFiveSuccessfulAnalysesAndBatchRemainsTruthfullyPartial() {
        when(mapper.listSystemDefaults()).thenReturn(List.of(
                row("SYSTEM", 0L, "ADAUSDT", true, true, 10, "DEFAULT"),
                row("SYSTEM", 0L, "BTCUSDT", true, true, 20, "DEFAULT"),
                row("SYSTEM", 0L, "ETHUSDT", true, true, 30, "DEFAULT"),
                row("SYSTEM", 0L, "SOLUSDT", true, true, 40, "DEFAULT"),
                row("SYSTEM", 0L, "BNBUSDT", true, true, 50, "DEFAULT"),
                row("SYSTEM", 0L, "XRPUSDT", true, true, 60, "DEFAULT")));
        when(mapper.listUserOverrides(42L)).thenReturn(List.of());
        AnalysisRunDO btcRun = new AnalysisRunDO();
        btcRun.setAnalysisId("analysis-btc");
        btcRun.setStatus("SUCCESS");
        AssetAnalysisVO btcAnalysis = new AssetAnalysisVO();
        btcAnalysis.setDataQualityScore(55);
        when(analysisRunOrchestrator.run(any()))
                .thenThrow(new IllegalStateException("REGION_RESTRICTED"))
                .thenReturn(AnalysisRunResult.executed(btcRun, btcAnalysis, false, false));

        AssetPoolScanBatchResultDTO batch = service.scanSummaryForUser(42L, "5m");

        assertThat(batch.overallState()).isEqualTo("PARTIAL");
        assertThat(batch.successCount()).isEqualTo(5);
        assertThat(batch.failedCount()).isEqualTo(1);
        assertThat(batch.perAssetResults()).extracting(AssetPoolScanResultDTO::symbol)
                .containsExactly("ADAUSDT", "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT");
        assertThat(batch.perAssetResults().get(0).state()).isEqualTo("FAILED");
        assertThat(batch.perAssetResults().get(0).failureReason()).isEqualTo("REGION_RESTRICTED");
        assertThat(batch.perAssetResults().get(1).state()).isEqualTo("SUCCESS");
        assertThat(batch.perAssetResults().get(1).analysisId()).isEqualTo("analysis-btc");
        assertThat(batch.perAssetResults().get(1).dataQuality()).isEqualTo(55);
        verify(analysisRunOrchestrator, org.mockito.Mockito.times(6)).run(any());
    }

    @Test
    void assetPoolManagesMoreThanSixAssetsWithoutDisplaySlotTruncation() {
        when(mapper.listSystemDefaults()).thenReturn(List.of(
                row("SYSTEM", 0L, "BTCUSDT", true, true, 10, "DEFAULT"),
                row("SYSTEM", 0L, "ETHUSDT", true, true, 20, "DEFAULT"),
                row("SYSTEM", 0L, "SOLUSDT", true, true, 30, "DEFAULT"),
                row("SYSTEM", 0L, "AAVEUSDT", true, true, 40, "DEFAULT"),
                row("SYSTEM", 0L, "LINKUSDT", true, true, 50, "DEFAULT"),
                row("SYSTEM", 0L, "TAOUSDT", true, true, 60, "DEFAULT")));
        when(mapper.listUserOverrides(21L)).thenReturn(List.of(
                row("USER", 21L, "SUIUSDT", true, true, 70, "USER_ADDED"),
                row("USER", 21L, "ARBUSDT", true, true, 80, "USER_ADDED"),
                row("USER", 21L, "OPUSDT", true, true, 90, "USER_ADDED"),
                row("USER", 21L, "NEARUSDT", true, true, 100, "USER_ADDED")));

        List<AssetPoolAssetDTO> assets = service.listForUser(21L);

        assertThat(assets).hasSize(10);
        assertThat(assets).extracting(AssetPoolAssetDTO::symbol)
                .containsExactly("BTCUSDT", "ETHUSDT", "SOLUSDT", "AAVEUSDT", "LINKUSDT",
                        "TAOUSDT", "SUIUSDT", "ARBUSDT", "OPUSDT", "NEARUSDT");
    }

    @Test
    void searchedAssetPreviewRunsThreeAiAnalysisWithoutMutatingPoolOrDecisionOwners() {
        when(marketAssetCatalog.requireTradable("aave/usdt"))
                .thenReturn(new MarketAssetDTO("AAVEUSDT", "AAVE", "USDT", "SPOT"));
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("preview-analysis-1");
        run.setTraceId("preview-trace-1");
        run.setStatus("SUCCESS");
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setAiRoleResults("""
                {"roles":{"GPT_FINAL":{},"GEMINI_REVIEW":{},"GROK_CHALLENGE":{}}}
                """);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId("preview-analysis-1");
        analysis.setDecisionBundle(decision);
        when(analysisRunOrchestrator.run(any())).thenReturn(AnalysisRunResult.executed(run, analysis, false, false));

        AssetAnalysisPreviewDTO preview = service.analyzePreviewForUser(31L, "aave/usdt", "15m");

        assertThat(preview.previewOnly()).isTrue();
        assertThat(preview.poolMutationPerformed()).isFalse();
        assertThat(preview.opportunityPersisted()).isFalse();
        assertThat(preview.candidatePersisted()).isFalse();
        assertThat(preview.finalPlanPersisted()).isFalse();
        assertThat(preview.symbol()).isEqualTo("AAVEUSDT");
        assertThat(preview.analysisId()).isEqualTo("preview-analysis-1");
        assertThat(preview.analysis().getDecisionBundle().getAiRoleResults())
                .contains("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        ArgumentCaptor<AnalysisRunCommand> command = ArgumentCaptor.forClass(AnalysisRunCommand.class);
        verify(analysisRunOrchestrator).run(command.capture());
        assertThat(command.getValue().getTriggerType()).isEqualTo(AnalysisRunTriggerType.ANALYSIS_PREVIEW);
        assertThat(command.getValue().isPreview()).isTrue();
        assertThat(command.getValue().getOwnerType()).isEqualTo("USER");
        assertThat(command.getValue().getOwnerId()).isEqualTo(31L);
        assertThat(command.getValue().getAssetId()).isNull();
        verifyNoInteractions(mapper);
    }

    private static AssetPoolItemDO row(String ownerType,
                                       Long ownerId,
                                       String symbol,
                                       boolean active,
                                       boolean focus,
                                       int sortOrder,
                                       String sourceType) {
        AssetPoolItemDO row = new AssetPoolItemDO();
        row.setId((long) sortOrder);
        row.setAssetId((long) sortOrder);
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
        row.setWatchStatus(active ? "OBSERVING" : "REMOVED");
        row.setVersion(1);
        return row;
    }

    private static AssetDO asset(Long id, String symbol) {
        AssetDO row = new AssetDO();
        row.setId(id);
        row.setSymbol(symbol);
        row.setAssetName(symbol.replace("USDT", ""));
        row.setSource("MARKET_CATALOG");
        row.setStatus("ACTIVE");
        row.setVersion(1);
        return row;
    }
}
