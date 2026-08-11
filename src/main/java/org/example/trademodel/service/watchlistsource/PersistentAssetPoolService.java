package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.entity.AssetPoolItemDO;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PersistentAssetPoolService implements AssetPoolService {
    private final AssetPoolItemMapper mapper;
    private final MarketAssetCatalog marketAssetCatalog;
    private final AnalysisRunOrchestrator analysisRunOrchestrator;

    public PersistentAssetPoolService(AssetPoolItemMapper mapper,
                                      MarketAssetCatalog marketAssetCatalog,
                                      @Lazy AnalysisRunOrchestrator analysisRunOrchestrator) {
        this.mapper = mapper;
        this.marketAssetCatalog = marketAssetCatalog;
        this.analysisRunOrchestrator = analysisRunOrchestrator;
    }

    @Override
    public List<AssetPoolAssetDTO> listForUser(Long userId) {
        requireUserId(userId);
        Map<String, AssetPoolItemDO> effective = new LinkedHashMap<>();
        for (AssetPoolItemDO row : safe(mapper.listSystemDefaults())) {
            effective.put(normalizeSymbol(row.getSymbol()), row);
        }
        for (AssetPoolItemDO row : safe(mapper.listUserOverrides(userId))) {
            String symbol = normalizeSymbol(row.getSymbol());
            if (Boolean.TRUE.equals(row.getActive())) {
                effective.put(symbol, row);
            } else {
                effective.remove(symbol);
            }
        }
        return effective.values().stream()
                .sorted((left, right) -> Integer.compare(sortOrder(left), sortOrder(right)))
                .map(PersistentAssetPoolService::toDto)
                .toList();
    }

    @Override
    public List<AssetPoolAssetDTO> listSystemDefaults() {
        return safe(mapper.listSystemDefaults()).stream()
                .sorted((left, right) -> Integer.compare(sortOrder(left), sortOrder(right)))
                .map(PersistentAssetPoolService::toDto)
                .toList();
    }

    @Override
    public List<String> listFocusSymbols(Long userId, int limit) {
        return listForUser(userId).stream()
                .filter(AssetPoolAssetDTO::focusEnabled)
                .map(AssetPoolAssetDTO::symbol)
                .limit(Math.max(1, Math.min(12, limit)))
                .toList();
    }

    @Override
    public List<String> listScanSymbols() {
        return safeStrings(mapper.listAllActiveSymbols());
    }

    @Override
    public List<MarketAssetDTO> searchMarket(String query, int limit) {
        return marketAssetCatalog.search(query, limit);
    }

    @Override
    @Transactional
    public AssetPoolAssetDTO addForUser(Long userId, String symbol, boolean focusEnabled) {
        requireUserId(userId);
        MarketAssetDTO marketAsset = marketAssetCatalog.requireTradable(symbol);
        LocalDateTime now = LocalDateTime.now();
        AssetPoolItemDO row = new AssetPoolItemDO();
        row.setOwnerType("USER");
        row.setOwnerId(userId);
        row.setSymbol(normalizeSymbol(marketAsset.symbol()));
        row.setDisplayName(marketAsset.baseAsset());
        row.setMarketType(marketAsset.marketType());
        row.setQuoteAsset(marketAsset.quoteAsset());
        row.setActive(true);
        row.setFocusEnabled(focusEnabled);
        row.setSortOrder(mapper.maxUserSortOrder(userId) + 10);
        row.setSourceType("USER_ADDED");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.upsert(row);
        return toDto(row);
    }

    @Override
    @Transactional
    public void removeForUser(Long userId, String symbol) {
        requireUserId(userId);
        String normalized = normalizeSymbol(symbol);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        LocalDateTime now = LocalDateTime.now();
        AssetPoolItemDO row = new AssetPoolItemDO();
        row.setOwnerType("USER");
        row.setOwnerId(userId);
        row.setSymbol(normalized);
        row.setDisplayName(baseAsset(normalized));
        row.setMarketType("SPOT");
        row.setQuoteAsset("USDT");
        row.setActive(false);
        row.setFocusEnabled(false);
        row.setSortOrder(mapper.maxUserSortOrder(userId) + 10);
        row.setSourceType("USER_OVERRIDE");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        mapper.upsert(row);
    }

    @Override
    @Transactional
    public List<AssetPoolAssetDTO> restoreDefaults(Long userId) {
        requireUserId(userId);
        mapper.deleteUserOverrides(userId);
        return listForUser(userId);
    }

    @Override
    public boolean isOpportunitySource(String symbol) {
        String normalized = normalizeSymbol(symbol);
        return !normalized.isBlank() && mapper.countActiveBySymbol(normalized) > 0;
    }

    @Override
    public List<AssetPoolScanResultDTO> scanForUser(Long userId, String timeframe) {
        requireUserId(userId);
        String effectiveTimeframe = timeframe == null || timeframe.isBlank() ? "5m" : timeframe.trim();
        List<AssetPoolScanResultDTO> results = new ArrayList<>();
        String scanId = "asset-pool-scan-" + RequestIdSupport.generate();
        for (AssetPoolAssetDTO asset : listForUser(userId)) {
            AnalysisRunResult result = analysisRunOrchestrator.run(AnalysisRunCommand.assetPoolScan(
                    asset.symbol(), effectiveTimeframe, RequestIdSupport.generate(), scanId));
            results.add(new AssetPoolScanResultDTO(
                    asset.symbol(),
                    result == null ? null : result.getAnalysisId(),
                    result == null ? "FAILED" : result.getStatus(),
                    result == null ? "ANALYSIS_RESULT_MISSING" : result.getReasonCode()));
        }
        return results;
    }

    private static AssetPoolAssetDTO toDto(AssetPoolItemDO row) {
        return new AssetPoolAssetDTO(
                row.getId(),
                normalizeSymbol(row.getSymbol()),
                row.getDisplayName(),
                row.getMarketType(),
                row.getQuoteAsset(),
                Boolean.TRUE.equals(row.getFocusEnabled()),
                sortOrder(row),
                row.getSourceType());
    }

    private static int sortOrder(AssetPoolItemDO row) {
        return row.getSortOrder() == null ? Integer.MAX_VALUE : row.getSortOrder();
    }

    private static List<AssetPoolItemDO> safe(List<AssetPoolItemDO> rows) {
        return rows == null ? List.of() : rows;
    }

    private static List<String> safeStrings(List<String> rows) {
        return rows == null ? List.of() : rows.stream()
                .map(PersistentAssetPoolService::normalizeSymbol)
                .filter(symbol -> !symbol.isBlank())
                .distinct()
                .toList();
    }

    private static String normalizeSymbol(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT)
                .replace("/", "").replace("-", "").replace("_", "");
    }

    private static String baseAsset(String symbol) {
        return symbol.endsWith("USDT") ? symbol.substring(0, symbol.length() - 4) : symbol;
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}
