package org.example.trademodel.uireview;

import org.example.trademodel.dto.assetpool.AssetAnalysisPreviewDTO;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** In-memory search membership used only by the local UI review profile. */
@Primary
@Profile("ui-review")
@Service
public class UiReviewAssetPoolService implements AssetPoolService {
    private static final List<MarketAssetDTO> MARKET = List.of(
            market("BTCUSDT", "Bitcoin"), market("ETHUSDT", "Ethereum"), market("SOLUSDT", "Solana"),
            market("LINKUSDT", "Chainlink"), market("AVAXUSDT", "Avalanche"), market("DOTUSDT", "Polkadot"),
            market("AAVEUSDT", "Aave"), market("SUIUSDT", "Sui"), market("ARBUSDT", "Arbitrum"));
    private final Map<String, AssetPoolAssetDTO> membership = new LinkedHashMap<>();

    public UiReviewAssetPoolService() {
        addControlled("BTCUSDT", true);
        addControlled("ETHUSDT", true);
        addControlled("SOLUSDT", true);
        addControlled("LINKUSDT", true);
        addControlled("AVAXUSDT", true);
        addControlled("DOTUSDT", true);
    }

    @Override
    public List<AssetPoolAssetDTO> listForUser(Long userId) {
        return List.copyOf(membership.values());
    }

    @Override
    public List<AssetPoolAssetDTO> listSystemDefaults() {
        return List.copyOf(membership.values());
    }

    @Override
    public List<String> listFocusSymbols(Long userId, int limit) {
        return membership.keySet().stream().limit(Math.max(0, limit)).toList();
    }

    @Override
    public List<String> listScanSymbols() {
        return List.copyOf(membership.keySet());
    }

    @Override
    public List<MarketAssetDTO> searchMarket(String query, int limit) {
        String normalized = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        return MARKET.stream()
                .filter(asset -> normalized.isEmpty()
                        || asset.symbol().contains(normalized)
                        || asset.baseAsset().toUpperCase(Locale.ROOT).contains(normalized))
                .limit(Math.max(0, limit))
                .toList();
    }

    @Override
    public AssetAnalysisPreviewDTO analyzePreviewForUser(Long userId, String symbol, String timeframe) {
        return new AssetAnalysisPreviewDTO(normalize(symbol), timeframe, "ui-review-preview-analysis",
                "ui-review-preview-trace", "SUCCESS", null, true,
                false, false, false, false, null);
    }

    @Override
    public AssetPoolAssetDTO addForUser(Long userId, String symbol, boolean focusEnabled) {
        return addControlled(normalize(symbol), focusEnabled);
    }

    @Override
    public List<AssetPoolAssetDTO> addManyForUser(Long userId, List<String> symbols, boolean focusEnabled) {
        if (symbols == null) return List.of();
        return symbols.stream().map(symbol -> addControlled(normalize(symbol), focusEnabled)).toList();
    }

    @Override
    public void removeForUser(Long userId, String symbol) {
        membership.remove(normalize(symbol));
    }

    @Override
    public void removeManyForUser(Long userId, List<String> symbols) {
        if (symbols != null) symbols.forEach(symbol -> membership.remove(normalize(symbol)));
    }

    @Override
    public List<AssetPoolAssetDTO> topUpDefaults(Long userId) {
        MARKET.stream().limit(6).forEach(asset -> addControlled(asset.symbol(), true));
        return listForUser(userId);
    }

    @Override
    public List<AssetPoolAssetDTO> resetDefaults(Long userId) {
        membership.clear();
        return topUpDefaults(userId);
    }

    @Override
    public boolean isOpportunitySource(String ownerType, Long ownerId, Long assetId, String symbol) {
        return membership.containsKey(normalize(symbol));
    }

    @Override
    public List<AssetPoolScanResultDTO> scanForUser(Long userId, String timeframe) {
        return List.of();
    }

    @Override
    public List<AssetPoolScanResultDTO> scanSelectedForUser(Long userId, List<String> symbols, String timeframe) {
        return List.of();
    }

    private AssetPoolAssetDTO addControlled(String symbol, boolean focusEnabled) {
        MarketAssetDTO market = MARKET.stream().filter(asset -> asset.symbol().equals(symbol)).findFirst()
                .orElse(new MarketAssetDTO(symbol, symbol.replace("USDT", ""), "USDT", "SPOT"));
        AssetPoolAssetDTO item = new AssetPoolAssetDTO(
                9800L + Math.abs(symbol.hashCode() % 100), symbol, market.baseAsset(), "SPOT", "USDT",
                focusEnabled, membership.size() + 1, "USER", 9900L + membership.size(), null,
                market.baseAsset(), "USER", "OBSERVING", LocalDateTime.now(), LocalDateTime.now(), 1, null);
        membership.put(symbol, item);
        return item;
    }

    private static MarketAssetDTO market(String symbol, String name) {
        return new MarketAssetDTO(symbol, name, "USDT", "SPOT");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
