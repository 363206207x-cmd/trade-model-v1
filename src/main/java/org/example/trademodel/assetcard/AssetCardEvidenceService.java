package org.example.trademodel.assetcard;

import org.example.trademodel.entity.ExternalContextEventDO;
import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.mapper.MacroEventMapper;
import org.example.trademodel.mapper.NewsEventMapper;
import org.example.trademodel.providercall.*;
import org.example.trademodel.providercall.coinglass.*;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;

/** Reads existing provider caches and event facts. It never requests a provider refresh. */
@Service
public class AssetCardEvidenceService {
    private final CoinGlassOpenInterestSnapshotService openInterest;
    private final CoinGlassFundingSnapshotService funding;
    private final CoinGlassLongShortSnapshotService longShort;
    private final CoinGlassLiquidationSnapshotService liquidation;
    private final CoinGlassProperties properties;
    private final MacroEventMapper macroEvents;
    private final NewsEventMapper newsEvents;
    private final CoinGlassSymbolMapper symbolMapper;
    private List<ExternalContextEventDO> eventCandidates = List.of();
    private boolean eventCandidatesLoaded;
    private long eventQueryNanos;
    private Instant eventQueryAsOf;

    public AssetCardEvidenceService(CoinGlassOpenInterestSnapshotService oi, CoinGlassFundingSnapshotService funding,
                                    CoinGlassLongShortSnapshotService ratio, CoinGlassLiquidationSnapshotService liquidation,
                                    CoinGlassProperties properties, MacroEventMapper macroEvents, NewsEventMapper newsEvents) {
        this(oi, funding, ratio, liquidation, properties, macroEvents, newsEvents, new CoinGlassSymbolMapper());
    }

    @Autowired
    public AssetCardEvidenceService(CoinGlassOpenInterestSnapshotService oi, CoinGlassFundingSnapshotService funding,
                                    CoinGlassLongShortSnapshotService ratio, CoinGlassLiquidationSnapshotService liquidation,
                                    CoinGlassProperties properties, MacroEventMapper macroEvents, NewsEventMapper newsEvents,
                                    CoinGlassSymbolMapper symbolMapper) {
        this.openInterest = oi; this.funding = funding; this.longShort = ratio; this.liquidation = liquidation;
        this.properties = properties; this.macroEvents = macroEvents; this.newsEvents = newsEvents;
        this.symbolMapper = Objects.requireNonNull(symbolMapper);
    }

    public record Fact(double value, String source, Instant observedAt, Instant availableAt, Instant expiresAt,
                       String sourceVersion, String instrument, String unit) {
        public Fact(double value,String source,Instant observedAt,Instant availableAt,Instant expiresAt,String sourceVersion) {
            this(value,source,observedAt,availableAt,expiresAt,sourceVersion,null,null);
        }
        public boolean freshAt(Instant at) {
            return at != null && Double.isFinite(value) && source != null && !source.isBlank()
                    && observedAt != null && availableAt != null && expiresAt != null && !availableAt.isBefore(observedAt)
                    && !observedAt.isAfter(at) && !availableAt.isAfter(at) && !expiresAt.isBefore(at);
        }
        public AssetCardFeatureService.Observation observation() {
            return new AssetCardFeatureService.Observation(value, source, observedAt, availableAt,instrument,sourceVersion,unit,expiresAt,null);
        }
    }
    public record EvidenceFrame(Map<String, Fact> facts, Map<String, String> missingReasons,
                                AssetCardSnapshot.RiskItem eventRisk) {
        public EvidenceFrame { facts = Map.copyOf(facts); missingReasons = Map.copyOf(missingReasons); }
        public Map<String, AssetCardFeatureService.Observation> observations(Instant at) {
            Map<String, AssetCardFeatureService.Observation> result = new LinkedHashMap<>();
            facts.forEach((key, fact) -> { if (fact.freshAt(at)) result.put(key, fact.observation()); });
            return result;
        }
    }

    public EvidenceFrame read(String symbol, Instant asOf) {
        Objects.requireNonNull(asOf, "asOf");
        Map<String, Fact> facts = new LinkedHashMap<>();
        Map<String, String> missing = new LinkedHashMap<>();
        // Dataset owners apply their read-freshness window once; this is the original card cadence.
        Duration ttl = Duration.ofSeconds(60);
        var mapped = mappedSymbol(symbol);
        var oi = safePeek(() -> openInterest.peek(symbol, AssetPriority.P1_WATCHLIST, ttl, "asset-card-read"));
        var f = safePeek(() -> funding.peek(symbol, AssetPriority.P1_WATCHLIST, ttl, "asset-card-read"));
        var ls = safePeek(() -> longShort.peek(symbol, AssetPriority.P1_WATCHLIST, ttl, "asset-card-read"));
        var liq = safePeek(() -> liquidation.peek(symbol, AssetPriority.P1_WATCHLIST, ttl, "asset-card-read"));
        var oiPayload = oi == null ? null : oi.payload();
        var fundingPayload = f == null ? null : f.payload();
        var ratioPayload = ls == null ? null : ls.payload();
        var liquidationPayload = liq == null ? null : liq.payload();
        put(facts, missing, "openInterest", ProviderDatasetType.COINGLASS_OPEN_INTEREST, "CURRENT", "openInterestUsd", oi,
                oiPayload == null ? null : oiPayload.openInterestUsd(), oiPayload == null ? null : oiPayload.symbol(),
                oiPayload == null ? null : oiPayload.providerDataTime(), oiPayload == null ? null : oiPayload.fieldSources(), mapped, asOf);
        put(facts, missing, "openInterestChange1h", ProviderDatasetType.COINGLASS_OPEN_INTEREST, "CURRENT", "openInterestChange1h", oi,
                oiPayload == null ? null : oiPayload.openInterestChange1h(), oiPayload == null ? null : oiPayload.symbol(),
                oiPayload == null ? null : oiPayload.providerDataTime(), oiPayload == null ? null : oiPayload.fieldSources(), mapped, asOf);
        put(facts, missing, "fundingRate", ProviderDatasetType.COINGLASS_FUNDING, "1M", "weightedFundingRate", f,
                fundingPayload == null ? null : fundingPayload.weightedFundingRate(), fundingPayload == null ? null : fundingPayload.symbol(),
                fundingPayload == null ? null : fundingPayload.providerDataTime(), fundingPayload == null ? null : fundingPayload.fieldSources(), mapped, asOf);
        put(facts, missing, "longShortRatio", ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, "1M", "longShortRatio", ls,
                ratioPayload == null ? null : ratioPayload.longShortRatio(), ratioPayload == null ? null : ratioPayload.symbol(),
                ratioPayload == null ? null : ratioPayload.providerDataTime(), ratioPayload == null ? null : ratioPayload.fieldSources(), mapped, asOf);
        put(facts, missing, "longLiquidation", ProviderDatasetType.COINGLASS_LIQUIDATION, "1M", "longLiquidationUsd5m", liq,
                liquidationPayload == null ? null : liquidationPayload.longLiquidationUsd5m(), liquidationPayload == null ? null : liquidationPayload.symbol(),
                liquidationPayload == null ? null : liquidationPayload.providerDataTime(), liquidationPayload == null ? null : liquidationPayload.fieldSources(), mapped, asOf);
        put(facts, missing, "shortLiquidation", ProviderDatasetType.COINGLASS_LIQUIDATION, "1M", "shortLiquidationUsd5m", liq,
                liquidationPayload == null ? null : liquidationPayload.shortLiquidationUsd5m(), liquidationPayload == null ? null : liquidationPayload.symbol(),
                liquidationPayload == null ? null : liquidationPayload.providerDataTime(), liquidationPayload == null ? null : liquidationPayload.fieldSources(), mapped, asOf);
        return new EvidenceFrame(facts, missing, readEventRisk(symbol, asOf));
    }

    /** Card background read only. One shared candidate query batch per second, never one batch per symbol. */
    public AssetCardSnapshot.RiskItem readEventRisk(String symbol, Instant asOf) {
        Objects.requireNonNull(asOf, "asOf");
        var at = LocalDateTime.ofInstant(asOf, ZoneOffset.UTC);
        AssetCardSnapshot.RiskItem eventRisk = null;
        for (var event : eventCandidates(asOf)) {
            if (event == null || !ExternalContextPolicy.matchesContextScope(event.getAffectedSymbols(), event.getMarketScope(), symbol, "CRYPTO")
                    || !"ACTIVE".equals(ExternalContextPolicy.windowState(event, at))
                    || !ExternalContextPolicy.hasCompleteSource(event) || !ExternalContextPolicy.hasText(event.getEventId())
                    || event instanceof MacroEventDO macro && !"SOURCE_PUBLISHED_AT_PROVIDED".equals(macro.getSourcePublishedAtReasonCode())
                    || event.getCreateTime() == null || event.getUpdateTime() == null || event.getSourcePublishedAt() == null
                    || event.getCreateTime().isAfter(at) || event.getUpdateTime().isAfter(at) || event.getSourcePublishedAt().isAfter(at)) continue;
            String level = event.getSeverity();
            if (!Set.of("LOW", "MEDIUM", "HIGH").contains(String.valueOf(level))) continue;
            var candidate = new AssetCardSnapshot.RiskItem("EVENT", "ASSESSED", level, event.getEventId(),
                    event.getProvider(), event.getSourcePublishedAt().toInstant(ZoneOffset.UTC), "已确认事件窗口：" + event.getEventId(), "EVENT_ID");
            if (eventRisk == null || rank(candidate.level()) > rank(eventRisk.level())
                    || rank(candidate.level()) == rank(eventRisk.level()) && candidate.asOf().isAfter(eventRisk.asOf())) eventRisk = candidate;
        }
        // No matching row is NOT proof of complete event coverage or of no event risk.
        return eventRisk;
    }

    private synchronized List<ExternalContextEventDO> eventCandidates(Instant asOf) {
        long now = monotonicNanos();
        if (!eventCandidatesLoaded || now - eventQueryNanos >= Duration.ofSeconds(1).toNanos()) {
            eventCandidatesLoaded = true;
            eventQueryNanos = now;
            eventQueryAsOf = asOf;
            eventCandidates = List.of(); // A failed read cannot replay an older claimed event assessment.
            List<ExternalContextEventDO> candidates = new ArrayList<>();
            var at = LocalDateTime.ofInstant(asOf, ZoneOffset.UTC);
            var macro = safeEventRead(() -> macroEvents.selectWindowCandidates(at, 500));
            var news = safeEventRead(() -> newsEvents.selectWindowCandidates(at, 500));
            if (macro != null) macro.stream().filter(Objects::nonNull).forEach(candidates::add);
            if (news != null) news.stream().filter(Objects::nonNull).forEach(candidates::add);
            eventCandidates = List.copyOf(candidates);
        }
        // A present-time candidate query cannot be reused as historical completeness evidence.
        return asOf.isBefore(eventQueryAsOf) ? List.of() : eventCandidates;
    }

    long monotonicNanos() { return System.nanoTime(); }

    private static int rank(String level) { return "HIGH".equals(level) ? 3 : "MEDIUM".equals(level) ? 2 : 1; }
    private void put(Map<String, Fact> facts, Map<String, String> missing, String key,
                            ProviderDatasetType expectedDataset, String expectedTimeframe, String payloadKey, ProviderCallResult<?> result,
                            BigDecimal value, String payloadSymbol, Instant payloadDataTime, Map<String, String> fieldSources,
                            CoinGlassSymbolMapper.CoinGlassSymbol mapped, Instant asOf) {
        String fieldSource = fieldSources == null ? null : fieldSources.get(payloadKey);
        if (result == null || !usable(result.metadata(), mapped, asOf) || !Objects.equals(payloadSymbol, mapped.pairSymbol())
                || result.metadata().datasetType() != expectedDataset || !expectedTimeframe.equals(result.metadata().timeframe())
                || !Objects.equals(payloadDataTime, result.metadata().providerDataTime())
                || fieldSource == null || fieldSource.isBlank()
                || value == null || !Double.isFinite(value.doubleValue())) {
            missing.put(key, "缺少新鲜且身份一致的CoinGlass " + key + " 证据");
            return;
        }
        String provenanceIssue = provenanceIssue(key, fieldSource, result.payload());
        if (provenanceIssue != null) {
            missing.put(key, provenanceIssue);
            return;
        }
        var m = result.metadata();
        facts.put(key, new Fact(value.doubleValue(), AssetCardFeatureService.VERIFIED_BINANCE_ACCOUNT_RATIO_SOURCE, m.providerDataTime(),
                m.fetchTime(), m.expiresAt(), m.sourceVersion(), m.canonicalInstrumentId().canonical(),AssetCardFeatureService.observationUnit(key)));
    }

    /** The registry identifies the requested instrument, not what an aggregate endpoint actually returned.
     * Only the existing pair-specific account-ratio chain carries a provable matching market/window/unit.
     * These failures quarantine card inputs; they do not alter a shared cache or request a replacement. */
    private String provenanceIssue(String key, String fieldSource, Object payload) {
        return switch (key) {
            case "openInterest", "openInterestChange1h" ->
                    "来源待核验：跨交易所 All 的 USD 持仓量不能等同 Binance USDT 合约；来源观察时间与实际可用时间缺少独立证明";
            case "fundingRate" ->
                    "来源待核验：币种级加权资金费率并非单一 Binance USDT 合约，百分数或小数单位尚未证明";
            case "longLiquidation", "shortLiquidation" ->
                    "来源待核验：多交易所 USD 清算额的5分钟汇总不能等同单一 Binance USDT 合约；交易所与统计窗口证明不完整";
            case "longShortRatio" -> payload instanceof CoinGlassLongShortSnapshot ratio
                    && ratio.longShortRatio() != null && ratio.longShortRatio().signum() > 0
                    && "BINANCE_GLOBAL_ACCOUNT_RATIO".equals(ratio.longShortRatioSource())
                    && AssetCardFeatureService.BINANCE_ACCOUNT_RATIO_FIELD.equals(fieldSource)
                    && "Binance".equals(properties.getLongShortExchange())
                    && CoinGlassProperties.OFFICIAL_BASE_URL.equals(properties.getBaseUrl())
                    && CoinGlassProperties.LONG_SHORT_PATH.equals(properties.getEndpoints().getLongShortRatio())
                    ? null : "来源待核验：需要 Binance 单交易对1分钟账户多空比及原始比例字段，不能使用其他市场或未知来源";
            default -> "来源待核验：未登记的资产卡片证据";
        };
    }

    static boolean usable(ProviderSnapshotMetadata m, String symbol, Instant asOf) {
        try { return usable(m, new CoinGlassSymbolMapper().map(symbol), asOf); }
        catch (IllegalArgumentException unsupported) { return false; }
    }

    private static boolean usable(ProviderSnapshotMetadata m, CoinGlassSymbolMapper.CoinGlassSymbol mapped, Instant asOf) {
        return asOf != null && m != null && mapped != null && "COINGLASS".equals(m.provider())
                && Objects.equals(m.providerSymbol(), mapped.pairSymbol())
                && Objects.equals(m.canonicalInstrumentId(), mapped.canonicalInstrumentId())
                && Objects.equals(m.sourceVersion(), mapped.sourceVersion())
                && m.sourceStatus() == UnifiedSourceStatus.READY && m.freshnessStatus() == SnapshotFreshnessStatus.FRESH
                && !m.fallbackUsed() && m.sourceVersion() != null && !m.sourceVersion().isBlank()
                && !Set.of("UNVERIFIED", "UNKNOWN").contains(m.sourceVersion())
                && m.providerDataTime() != null && m.fetchTime() != null && m.expiresAt() != null
                && !m.providerDataTime().isAfter(asOf) && !m.fetchTime().isAfter(asOf) && !m.expiresAt().isBefore(asOf)
                && !m.fetchTime().isBefore(m.providerDataTime());
    }
    private CoinGlassSymbolMapper.CoinGlassSymbol mappedSymbol(String symbol) {
        try { return symbolMapper.map(symbol); }
        catch (RuntimeException unavailable) { return null; }
    }
    private static <T> ProviderCallResult<T> safePeek(Supplier<ProviderCallResult<T>> cachedRead) {
        try { return cachedRead.get(); }
        catch (RuntimeException unavailable) { return null; }
    }
    private static <T extends ExternalContextEventDO> List<T> safeEventRead(Supplier<List<T>> storedRead) {
        try { return storedRead.get(); }
        catch (RuntimeException unavailable) { return List.of(); }
    }
}
