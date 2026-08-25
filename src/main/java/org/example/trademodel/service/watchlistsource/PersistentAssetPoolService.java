package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.AssetAnalysisPreviewDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.entity.AssetPoolItemDO;
import org.example.trademodel.entity.AssetDO;
import org.example.trademodel.mapper.AssetMapper;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.example.trademodel.localreal.LocalRealReadinessService;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PersistentAssetPoolService implements AssetPoolService {
    private final AssetPoolItemMapper mapper;
    private final AssetMapper assetMapper;
    private final MarketAssetCatalog marketAssetCatalog;
    private final AnalysisRunOrchestrator analysisRunOrchestrator;
    private final ProviderCapabilityRegistry providerCapabilityRegistry;
    private LocalRealReadinessService localRealReadinessService;

    @org.springframework.beans.factory.annotation.Autowired
    public PersistentAssetPoolService(AssetPoolItemMapper mapper,
                                      AssetMapper assetMapper,
                                      MarketAssetCatalog marketAssetCatalog,
                                      @Lazy AnalysisRunOrchestrator analysisRunOrchestrator,
                                      ProviderCapabilityRegistry providerCapabilityRegistry) {
        this.mapper = mapper;
        this.assetMapper = assetMapper;
        this.marketAssetCatalog = marketAssetCatalog;
        this.analysisRunOrchestrator = analysisRunOrchestrator;
        this.providerCapabilityRegistry = providerCapabilityRegistry;
    }

    PersistentAssetPoolService(AssetPoolItemMapper mapper,
                               AssetMapper assetMapper,
                               MarketAssetCatalog marketAssetCatalog,
                               AnalysisRunOrchestrator analysisRunOrchestrator) {
        this(mapper, assetMapper, marketAssetCatalog, analysisRunOrchestrator, null);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setLocalRealReadinessService(LocalRealReadinessService localRealReadinessService) {
        this.localRealReadinessService = localRealReadinessService;
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
    public List<AssetPoolScanTarget> listScanTargets() {
        return safe(mapper.listAllActiveItems()).stream()
                .filter(row -> row != null && Boolean.TRUE.equals(row.getActive()))
                .map(row -> new AssetPoolScanTarget(
                        row.getOwnerType(), row.getOwnerId(), row.getAssetId(), normalizeSymbol(row.getSymbol())))
                .distinct()
                .toList();
    }

    @Override
    public List<MarketAssetDTO> searchMarket(String query, int limit) {
        return marketAssetCatalog.search(query, limit);
    }

    @Override
    public AssetAnalysisPreviewDTO analyzePreviewForUser(Long userId, String symbol, String timeframe) {
        requireUserId(userId);
        MarketAssetDTO marketAsset = marketAssetCatalog.requireTradable(symbol);
        String normalized = normalizeSymbol(marketAsset.symbol());
        String effectiveTimeframe = timeframe == null || timeframe.isBlank() ? "5m" : timeframe.trim();
        AnalysisRunResult result = analysisRunOrchestrator.run(AnalysisRunCommand.preview(
                userId, normalized, effectiveTimeframe, RequestIdSupport.generate(), null));
        return new AssetAnalysisPreviewDTO(
                normalized,
                effectiveTimeframe,
                result == null ? null : result.getAnalysisId(),
                result == null ? null : result.getTraceId(),
                result == null ? "FAILED" : result.getStatus(),
                previewReasonCode(result),
                true,
                false,
                false,
                false,
                false,
                result == null ? null : result.getAnalysis());
    }

    static String previewReasonCode(AnalysisRunResult result) {
        if (result == null) return "ANALYSIS_RESULT_MISSING";
        String message = result.getMessage() == null
                ? "" : result.getMessage().trim().toUpperCase(Locale.ROOT);
        if (message.contains("AUTHORITATIVE_OHLCV_UNAVAILABLE")) {
            return "AUTHORITATIVE_OHLCV_UNAVAILABLE";
        }
        if (message.contains("REAL_MARKET_ENVIRONMENT_REQUIRED")
                || message.contains("REAL_MARKET_PROVENANCE_INCOMPLETE")) {
            return "REAL_MARKET_ENVIRONMENT_UNAVAILABLE";
        }
        return result.getReasonCode();
    }

    @Override
    @Transactional
    public AssetPoolAssetDTO addForUser(Long userId, String symbol, boolean focusEnabled) {
        requireUserId(userId);
        MarketAssetDTO marketAsset = marketAssetCatalog.requireTradable(symbol);
        LocalDateTime now = LocalDateTime.now();
        String normalized = normalizeSymbol(marketAsset.symbol());
        AssetDO asset = requireCanonicalAsset(marketAsset, now);
        AssetPoolItemDO existing = mapper.selectByOwnerAndSymbol("USER", userId, normalized);
        AssetPoolItemDO row = new AssetPoolItemDO();
        row.setOwnerType("USER");
        row.setOwnerId(userId);
        row.setAssetId(asset.getId());
        row.setSymbol(normalized);
        row.setDisplayName(marketAsset.baseAsset());
        row.setMarketType(marketAsset.marketType());
        row.setQuoteAsset(marketAsset.quoteAsset());
        row.setActive(true);
        row.setFocusEnabled(focusEnabled);
        row.setSortOrder(mapper.maxUserSortOrder(userId) + 10);
        row.setSourceType("USER_ADDED");
        row.setWatchStatus("OBSERVING");
        row.setVersion(nextVersion(existing));
        row.setExtJson(existing == null ? null : existing.getExtJson());
        row.setCreatedAt(existing == null || existing.getCreatedAt() == null ? now : existing.getCreatedAt());
        row.setUpdatedAt(now);
        mapper.upsert(row);
        AssetPoolItemDO persisted = mapper.selectByOwnerAndSymbol("USER", userId, normalized);
        return toDto(persisted == null ? row : persisted);
    }

    @Override
    @Transactional
    public List<AssetPoolAssetDTO> addManyForUser(Long userId, List<String> symbols, boolean focusEnabled) {
        requireUserId(userId);
        return requireSymbols(symbols).stream()
                .map(symbol -> addForUser(userId, symbol, focusEnabled))
                .toList();
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
        AssetDO asset = assetMapper.selectBySymbol(normalized);
        if (asset == null || asset.getId() == null) {
            throw new IllegalArgumentException("asset is not in the canonical Asset Pool catalog: " + normalized);
        }
        AssetPoolItemDO existing = mapper.selectByOwnerAndSymbol("USER", userId, normalized);
        AssetPoolItemDO effective = existing == null
                ? mapper.selectByOwnerAndSymbol("SYSTEM", 0L, normalized)
                : existing;
        AssetPoolItemDO row = new AssetPoolItemDO();
        row.setOwnerType("USER");
        row.setOwnerId(userId);
        row.setAssetId(asset.getId());
        row.setSymbol(normalized);
        row.setDisplayName(effective == null ? baseAsset(normalized) : effective.getDisplayName());
        row.setMarketType(effective == null ? "SPOT" : effective.getMarketType());
        row.setQuoteAsset(effective == null ? "USDT" : effective.getQuoteAsset());
        row.setActive(false);
        row.setFocusEnabled(false);
        row.setSortOrder(mapper.maxUserSortOrder(userId) + 10);
        row.setSourceType("USER_OVERRIDE");
        row.setWatchStatus("TRACKING_STOPPED");
        row.setVersion(nextVersion(existing));
        row.setExtJson(existing == null ? null : existing.getExtJson());
        row.setCreatedAt(existing == null || existing.getCreatedAt() == null ? now : existing.getCreatedAt());
        row.setUpdatedAt(now);
        mapper.upsert(row);
    }

    @Override
    @Transactional
    public void removeManyForUser(Long userId, List<String> symbols) {
        requireUserId(userId);
        requireSymbols(symbols).forEach(symbol -> removeForUser(userId, symbol));
    }

    @Override
    @Transactional
    public List<AssetPoolAssetDTO> topUpDefaults(Long userId) {
        requireUserId(userId);
        Map<String, AssetPoolItemDO> overrides = safe(mapper.listUserOverrides(userId)).stream()
                .collect(Collectors.toMap(row -> normalizeSymbol(row.getSymbol()), row -> row,
                        (left, right) -> right, LinkedHashMap::new));
        LocalDateTime now = LocalDateTime.now();
        for (AssetPoolItemDO systemDefault : safe(mapper.listSystemDefaults())) {
            String symbol = normalizeSymbol(systemDefault.getSymbol());
            AssetPoolItemDO existing = overrides.get(symbol);
            if (existing != null && Boolean.TRUE.equals(existing.getActive())) {
                continue;
            }
            mapper.upsert(defaultOverride(userId, systemDefault, existing, true, "OBSERVING", now));
        }
        return listForUser(userId);
    }

    @Override
    @Transactional
    public List<AssetPoolAssetDTO> resetDefaults(Long userId) {
        requireUserId(userId);
        Set<String> defaultSymbols = safe(mapper.listSystemDefaults()).stream()
                .map(row -> normalizeSymbol(row.getSymbol()))
                .collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        Map<String, AssetPoolItemDO> systemDefaults = safe(mapper.listSystemDefaults()).stream()
                .collect(Collectors.toMap(row -> normalizeSymbol(row.getSymbol()), row -> row,
                        (left, right) -> right, LinkedHashMap::new));
        Map<String, AssetPoolItemDO> overrides = safe(mapper.listUserOverrides(userId)).stream()
                .collect(Collectors.toMap(row -> normalizeSymbol(row.getSymbol()), row -> row,
                        (left, right) -> right, LinkedHashMap::new));
        for (Map.Entry<String, AssetPoolItemDO> entry : systemDefaults.entrySet()) {
            mapper.upsert(defaultOverride(userId, entry.getValue(), overrides.get(entry.getKey()),
                    true, "OBSERVING", now));
        }
        for (AssetPoolItemDO override : safe(mapper.listUserOverrides(userId))) {
            String symbol = normalizeSymbol(override.getSymbol());
            if (!defaultSymbols.contains(symbol) && Boolean.TRUE.equals(override.getActive())) {
                mapper.upsert(defaultOverride(
                        userId, override, override, false, "TRACKING_STOPPED", now));
            }
        }
        return listForUser(userId);
    }

    @Override
    public boolean isOpportunitySource(String ownerType, Long ownerId, Long assetId, String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized.isBlank()) return false;
        String normalizedOwnerType = ownerType == null ? "" : ownerType.trim().toUpperCase(Locale.ROOT);
        List<AssetPoolAssetDTO> effective;
        if ("SYSTEM".equals(normalizedOwnerType) && Long.valueOf(0L).equals(ownerId)) {
            effective = listSystemDefaults();
        } else if ("USER".equals(normalizedOwnerType) && ownerId != null && ownerId > 0) {
            effective = listForUser(ownerId);
        } else {
            return false;
        }
        return effective.stream().anyMatch(asset -> normalized.equals(asset.symbol())
                && (assetId == null || assetId.equals(asset.assetId())));
    }

    @Override
    public Long resolvePoolItemId(String ownerType, Long ownerId, Long assetId, String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized.isBlank()) return null;
        String normalizedOwnerType = ownerType == null ? "" : ownerType.trim().toUpperCase(Locale.ROOT);
        List<AssetPoolAssetDTO> effective;
        if ("SYSTEM".equals(normalizedOwnerType) && Long.valueOf(0L).equals(ownerId)) {
            effective = listSystemDefaults();
        } else if ("USER".equals(normalizedOwnerType) && ownerId != null && ownerId > 0) {
            effective = listForUser(ownerId);
        } else {
            return null;
        }
        return effective.stream()
                .filter(asset -> normalized.equals(asset.symbol()))
                .filter(asset -> assetId == null || assetId.equals(asset.assetId()))
                .map(AssetPoolAssetDTO::poolItemId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<AssetPoolScanResultDTO> scanForUser(Long userId, String timeframe) {
        requireUserId(userId);
        return scanAssets(userId, listForUser(userId), timeframe);
    }

    @Override
    public List<AssetPoolScanResultDTO> scanSelectedForUser(Long userId,
                                                            List<String> symbols,
                                                            String timeframe) {
        requireUserId(userId);
        Set<String> requested = Set.copyOf(requireSymbols(symbols));
        Map<String, AssetPoolAssetDTO> effective = listForUser(userId).stream()
                .collect(Collectors.toMap(AssetPoolAssetDTO::symbol, asset -> asset));
        List<String> missing = requested.stream()
                .filter(symbol -> !effective.containsKey(symbol))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("assets are not in the effective Asset Pool: " + missing);
        }
        return scanAssets(userId, requested.stream().sorted()
                .map(effective::get)
                .toList(), timeframe);
    }

    private List<AssetPoolScanResultDTO> scanAssets(Long userId,
                                                     List<AssetPoolAssetDTO> assets,
                                                     String timeframe) {
        String effectiveTimeframe = timeframe == null || timeframe.isBlank() ? "5m" : timeframe.trim();
        List<AssetPoolScanResultDTO> results = new ArrayList<>();
        String scanId = "asset-pool-scan-" + RequestIdSupport.generate();
        if (localRealReadinessService != null) {
            localRealReadinessService.synchronizeTrackedAssets(listScanSymbols());
            localRealReadinessService.transition(
                    org.example.trademodel.localreal.LocalRealReadinessState.ANALYSIS_RUNNING,
                    "MANUAL_ASSET_POOL_SCAN_RUNNING");
        }
        for (AssetPoolAssetDTO asset : assets) {
            Instant observedAt = Instant.now();
            try {
                AnalysisRunResult result = analysisRunOrchestrator.run(AnalysisRunCommand.assetPoolScan(
                        userId, asset.assetId(), asset.symbol(), effectiveTimeframe,
                        RequestIdSupport.generate(), scanId));
                String state = scanState(result);
                String reason = result == null ? "ANALYSIS_RESULT_MISSING" : result.getReasonCode();
                results.add(new AssetPoolScanResultDTO(
                        asset.symbol(),
                        result == null ? null : result.getAnalysisId(),
                        state,
                        reason,
                        asset.assetId(),
                        provider(result, asset.symbol(), effectiveTimeframe),
                        state,
                        dataQuality(result),
                        "SUCCESS".equals(state) ? null : reason,
                        observedAt));
            } catch (RuntimeException failure) {
                String failureReason = scanFailureReason(failure);
                results.add(new AssetPoolScanResultDTO(
                        asset.symbol(), null, "FAILED", failureReason,
                        asset.assetId(), provider(null, asset.symbol(), effectiveTimeframe),
                        "FAILED", null, failureReason, observedAt));
            }
        }
        if (localRealReadinessService != null) {
            localRealReadinessService.refreshFromPersistedAnalyses(listScanSymbols());
        }
        return results;
    }

    private static String scanState(AnalysisRunResult result) {
        if (result == null) return "FAILED";
        if (result.isSuccessfulAnalysisAvailable()) return "SUCCESS";
        return result.hasAnalysisId() ? "PARTIAL" : "FAILED";
    }

    private static Integer dataQuality(AnalysisRunResult result) {
        return result == null || result.getAnalysis() == null
                ? null : result.getAnalysis().getDataQualityScore();
    }

    private static String scanFailureReason(RuntimeException failure) {
        String message = failure == null || failure.getMessage() == null
                ? "" : failure.getMessage().toUpperCase(Locale.ROOT);
        if (message.contains("REGION_RESTRICTED") || message.contains("HTTP_451")) {
            return "REGION_RESTRICTED";
        }
        if (message.contains("UNSUPPORTED_SYMBOL") || message.contains("PAIR_NOT_SUPPORTED")) {
            return "UNSUPPORTED_SYMBOL";
        }
        if (message.contains("UNSUPPORTED_TIMEFRAME")) {
            return "UNSUPPORTED_TIMEFRAME";
        }
        if (message.contains("PROVIDER_DISABLED")) {
            return "PROVIDER_DISABLED";
        }
        if (message.contains("NOT_CONFIGURED")) {
            return "NOT_CONFIGURED";
        }
        return "ASSET_SCAN_EXCEPTION";
    }

    private String provider(AnalysisRunResult result, String symbol, String timeframe) {
        if (result != null && result.getAnalysis() != null
                && result.getAnalysis().getMarketEnvironment() != null
                && result.getAnalysis().getMarketEnvironment().getSourceProvider() != null) {
            return result.getAnalysis().getMarketEnvironment().getSourceProvider();
        }
        if (providerCapabilityRegistry == null) return null;
        ProviderInstrumentCapability capability = providerCapabilityRegistry.best(symbol, timeframe);
        return capability == null ? null : capability.provider();
    }

    private static List<String> requireSymbols(List<String> symbols) {
        if (symbols == null) throw new IllegalArgumentException("symbols are required");
        List<String> normalized = symbols.stream()
                .map(PersistentAssetPoolService::normalizeSymbol)
                .filter(symbol -> !symbol.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) throw new IllegalArgumentException("symbols are required");
        return normalized;
    }

    private static AssetPoolAssetDTO toDto(AssetPoolItemDO row) {
        return new AssetPoolAssetDTO(
                row.getAssetId(),
                normalizeSymbol(row.getSymbol()),
                row.getDisplayName(),
                row.getMarketType(),
                row.getQuoteAsset(),
                Boolean.TRUE.equals(row.getFocusEnabled()),
                sortOrder(row),
                row.getSourceType(),
                row.getId(),
                row.getOwnerId(),
                row.getDisplayName(),
                row.getSourceType(),
                defaultWatchStatus(row),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getVersion() == null ? 1 : row.getVersion(),
                row.getExtJson());
    }

    private AssetDO requireCanonicalAsset(MarketAssetDTO marketAsset, LocalDateTime now) {
        String symbol = normalizeSymbol(marketAsset.symbol());
        AssetDO existing = assetMapper.selectBySymbol(symbol);
        if (existing != null && existing.getId() != null) {
            return existing;
        }
        AssetDO row = new AssetDO();
        row.setSymbol(symbol);
        row.setAssetName(marketAsset.baseAsset());
        row.setSource("MARKET_CATALOG");
        row.setStatus("ACTIVE");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        row.setVersion(1);
        assetMapper.upsert(row);
        AssetDO persisted = assetMapper.selectBySymbol(symbol);
        if (persisted == null || persisted.getId() == null) {
            throw new IllegalStateException("canonical asset persistence failed for " + symbol);
        }
        return persisted;
    }

    private static int nextVersion(AssetPoolItemDO existing) {
        return existing == null || existing.getVersion() == null ? 1 : existing.getVersion() + 1;
    }

    private static AssetPoolItemDO defaultOverride(Long userId,
                                                   AssetPoolItemDO source,
                                                   AssetPoolItemDO existing,
                                                   boolean active,
                                                   String watchStatus,
                                                   LocalDateTime now) {
        AssetPoolItemDO row = new AssetPoolItemDO();
        row.setOwnerType("USER");
        row.setOwnerId(userId);
        row.setAssetId(source.getAssetId());
        row.setSymbol(normalizeSymbol(source.getSymbol()));
        row.setDisplayName(source.getDisplayName());
        row.setMarketType(source.getMarketType());
        row.setQuoteAsset(source.getQuoteAsset());
        row.setActive(active);
        row.setFocusEnabled(active && Boolean.TRUE.equals(source.getFocusEnabled()));
        row.setSortOrder(sortOrder(source));
        row.setSourceType("USER_OVERRIDE");
        row.setWatchStatus(watchStatus);
        row.setVersion(nextVersion(existing));
        row.setExtJson(existing == null ? source.getExtJson() : existing.getExtJson());
        row.setCreatedAt(existing == null || existing.getCreatedAt() == null ? now : existing.getCreatedAt());
        row.setUpdatedAt(now);
        return row;
    }

    private static String defaultWatchStatus(AssetPoolItemDO row) {
        if (row.getWatchStatus() != null && !row.getWatchStatus().isBlank()) {
            return row.getWatchStatus();
        }
        return Boolean.TRUE.equals(row.getActive()) ? "OBSERVING" : "REMOVED";
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
