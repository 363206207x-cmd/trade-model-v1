package org.example.trademodel.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotPolicy;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.BinanceDerivativesSnapshotService;
import org.example.trademodel.providercall.snapshot.MinimalDerivativesSnapshot;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds {@link MarketEnvironmentVO} from real quote data when available (V1).
 */
@Service
public class RealMarketEnvironmentService {

    private static final Logger log = LoggerFactory.getLogger(RealMarketEnvironmentService.class);

    /** Below this range% (exclusive of upper band edge): 窄幅 — fixed thresholds, do not tune implicitly. */
    private static final double RANGE_NARROW_MAX_PCT = 2.0;
    /** At or above: 高波动 — between narrow max and this: 中等波动. */
    private static final double RANGE_HIGH_MIN_PCT = 6.0;
    static final String DERIVATIVES_CROWDING_STATE_NEUTRAL = "NEUTRAL";
    static final String DERIVATIVES_CROWDING_STATE_CROWDED_LONG = "CROWDED_LONG";
    static final String DERIVATIVES_CROWDING_STATE_CROWDED_SHORT = "CROWDED_SHORT";

    private final MarketPriceSnapshotService marketPriceSnapshotService;
    private final BinanceDerivativesSnapshotService derivativesSnapshotService;

    public RealMarketEnvironmentService(MarketPriceSnapshotService marketPriceSnapshotService,
                                        BinanceDerivativesSnapshotService derivativesSnapshotService) {
        this.marketPriceSnapshotService = marketPriceSnapshotService;
        this.derivativesSnapshotService = derivativesSnapshotService;
    }

    /**
     * @return non-empty when a real snapshot was fetched and mapped successfully
     */
    public Optional<MarketEnvironmentVO> tryBuildFromRealQuote(String assetSymbol, String timeframe) {
        try {
            ProviderCallResult<MarketPriceSnapshot> result = marketPriceSnapshotService.get(assetSymbol,
                    AssetPriority.P1_WATCHLIST, Duration.ofSeconds(30), "market-env-" + UUID.randomUUID());
            if (!MarketPriceSnapshotPolicy.isFresh(result)) {
                log.info("[market-env] real quote unavailable, will fallback asset={}", assetSymbol);
                return Optional.empty();
            }
            MarketPriceSnapshot snap = result.payload();
            MarketEnvironmentVO env = mapSnapshot(snap, timeframe);
            mergeDerivativesIntoSummary(assetSymbol, env);
            // derivativesCrowdingState：assemble 在 enrichOpenInterestDeltaFromPreviousSnapshot 之后计算（二刀 B）。
            log.info("[market-env] built from REAL quote provider={} symbol={} tf={}",
                    snap.sourceProvider(), snap.symbol(), timeframe);
            return Optional.of(env);
        } catch (Exception e) {
            log.warn("[market-env] real mapping failed, fallback asset={} err={}", assetSymbol, e.getMessage());
            return Optional.empty();
        }
    }

    private MarketEnvironmentVO mapSnapshot(MarketPriceSnapshot q, String timeframe) {
        BigDecimal pctBd = q.priceChangePercent24h() != null ? q.priceChangePercent24h() : BigDecimal.ZERO;
        double pct = pctBd.doubleValue();
        double abs = Math.abs(pct);

        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPriceChangePercent24h(q.priceChangePercent24h());
        env.setEnvironmentType(abs >= 2.0 ? "trend_market" : "range_market");
        env.setRiskMode(abs >= 8.0 ? "elevated" : "normal");
        int friendliness = (int) Math.round(Math.max(0, Math.min(100, 50 + pct * 2.5)));
        env.setTrendFriendliness((double) friendliness);
        env.setLeverageSuggestion(abs >= 6.0 ? "low_leverage" : "moderate_leverage");
        Double rangePct = computeRangePercent24h(q.lastPrice(), q.highPrice24h(), q.lowPrice24h());
        if (rangePct != null) {
            env.setRangePct24h(rangePct);
            env.setVolatilityRegime(describeVolatilityRegime(rangePct));
        }
        env.setSummary(buildSummary(q, timeframe, rangePct));
        return env;
    }

    private String buildSummary(MarketPriceSnapshot q, String timeframe, Double rangePct) {
        String tf = timeframe != null && !timeframe.isBlank() ? timeframe.trim() : "n/a";
        String price = q.lastPrice() != null ? q.lastPrice().toPlainString() : "?";
        String pctf = q.priceChangePercent24h() != null ? q.priceChangePercent24h().toPlainString() : "?";
        StringBuilder sb = new StringBuilder();
        sb.append("Real feed (Binance 24h): ").append(q.symbol())
                .append(" last ").append(price).append(" USDT, 24h change ").append(pctf).append("%.");
        if (rangePct != null) {
            String regime = describeVolatilityRegime(rangePct);
            sb.append(" 24h 价格振幅约 ")
                    .append(String.format(Locale.US, "%.2f", rangePct))
                    .append("%（").append(regime).append("）。");
        }
        sb.append(" Analysis timeframe: ").append(tf).append(". [V1 market-env]");
        return sb.toString();
    }

    /**
     * range% = (high - low) / last * 100; returns null if inputs missing or invalid (no throw).
     */
    static Double computeRangePercent24h(MarketQuoteSnapshot q) {
        if (q == null) {
            return null;
        }
        return computeRangePercent24h(q.getLastPrice(), q.getHighPrice(), q.getLowPrice());
    }

    private static Double computeRangePercent24h(BigDecimal lastPrice, BigDecimal highPrice, BigDecimal lowPrice) {
        if (lastPrice == null || highPrice == null || lowPrice == null) {
            return null;
        }
        if (lastPrice.signum() <= 0) {
            return null;
        }
        BigDecimal diff = highPrice.subtract(lowPrice);
        if (diff.signum() < 0) {
            return null;
        }
        return diff.divide(lastPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    static String describeVolatilityRegime(double rangePct) {
        if (rangePct < RANGE_NARROW_MAX_PCT) {
            return "窄幅";
        }
        if (rangePct >= RANGE_HIGH_MIN_PCT) {
            return "高波动";
        }
        return "中等波动";
    }

    /** Best-effort：衍生品最小快照失败不拖垮现货 summary。 */
    private void mergeDerivativesIntoSummary(String assetSymbol, MarketEnvironmentVO env) {
        env.setPerpFundingApplied(Boolean.FALSE);
        env.setOiApplied(Boolean.FALSE);
        if (derivativesSnapshotService == null) {
            return;
        }
        try {
            ProviderCallResult<MinimalDerivativesSnapshot> result = derivativesSnapshotService.get(assetSymbol,
                    AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "market-env-derivatives-" + UUID.randomUUID());
            MinimalDerivativesSnapshot snapshot = result == null ? null : result.payload();
            if (snapshot == null) return;
            if (snapshot.lastFundingRate() != null) {
                env.setLastFundingRate(snapshot.lastFundingRate());
                env.setSummary((env.getSummary() == null ? "" : env.getSummary())
                        + buildFundingAppendix(snapshot.lastFundingRate()));
                env.setPerpFundingApplied(Boolean.TRUE);
            }
            if (snapshot.openInterest() != null) {
                env.setLastOpenInterest(snapshot.openInterest());
                env.setSummary((env.getSummary() == null ? "" : env.getSummary())
                        + buildOpenInterestAppendix(snapshot.openInterest()));
                env.setOiApplied(Boolean.TRUE);
            }
        } catch (Exception e) {
            log.info("[market-env] derivatives snapshot merge skipped asset={} err={}", assetSymbol, e.getMessage());
        }
    }

    /**
     * 与 {@code OI_MINIMAL_ACCESS_CONTRACT.md} §6 summary 附录句式一致。
     */
    public static final String BUILD_OPEN_INTEREST_APPENDIX_TRIM_PREFIX = "USDⓈ-M 未平仓量约 ";
    /** 与 {@link #buildOpenInterestAppendix(BigDecimal)} 格式串尾部一致（trim 后）。 */
    public static final String BUILD_OPEN_INTEREST_APPENDIX_TRIM_SUFFIX =
            "（合约口径，API 字段 openInterest）；Binance 启发式。";

    /**
     * 与 summary 追加句、{@code EvidenceServiceImpl} OI 风险行 description 唯一同源（仅 trim 差异可接受）。
     */
    public static String buildOpenInterestAppendix(BigDecimal openInterest) {
        String plain = openInterest.stripTrailingZeros().toPlainString();
        return String.format(Locale.US,
                " USDⓈ-M 未平仓量约 %s（合约口径，API 字段 openInterest）；Binance 启发式。",
                plain);
    }

    /**
     * {@link #buildFundingAppendix(BigDecimal)} 经 {@link String#trim()} 后的固定前后缀，供 run 级 DQ carve-out 与 evidence 描述窄匹配。
     */
    public static final String BUILD_FUNDING_APPENDIX_TRIM_PREFIX = "Perp（USDⓈ-M）上期资金费率约 ";
    /** 与 {@link #buildFundingAppendix(BigDecimal)} 格式串尾部一致（trim 后）。 */
    public static final String BUILD_FUNDING_APPENDIX_TRIM_SUFFIX = "；Binance USDT-M 启发式。";

    /**
     * lastFundingRate 为小数（非百分数）；口语化 8h 周期。
     * 与 summary 追加句、{@code EvidenceServiceImpl} Funding 行 description 唯一同源（仅 trim 差异可接受）。
     */
    public static String buildFundingAppendix(BigDecimal lastFundingRate) {
        BigDecimal pctPer8h = lastFundingRate.multiply(new BigDecimal("100"));
        String direction = fundingPayDirectionCn(lastFundingRate);
        return String.format(Locale.US,
                " Perp（USDⓈ-M）上期资金费率约 %+.6f%%/8h（%s）；Binance USDT-M 启发式。",
                pctPer8h.doubleValue(), direction);
    }

    private static String fundingPayDirectionCn(BigDecimal rate) {
        int cmp = rate.compareTo(BigDecimal.ZERO);
        if (cmp > 0) {
            return "多头付费";
        }
        if (cmp < 0) {
            return "空头付费";
        }
        return "中性";
    }

    /**
     * OI/Funding 联合派生最小离散标签（第二刀 B：Funding 主导，OI delta 轻量过滤）：
     * 仅当 OI/Funding 两条应用标记均成立且关键值非空时进入联合判定，其余回退 NEUTRAL。
     * Funding 正 / 负给出拥挤方向候选；Funding 为零则 NEUTRAL。
     * {@link MarketEnvironmentVO#getOpenInterestDelta()}：{@code null} 时不改变 Funding-only 结论；
     * 为零或与 Funding 符号相反则 NEUTRAL；与 Funding 同号则保留候选。
     */
    public static String computeDerivativesCrowdingState(MarketEnvironmentVO env) {
        if (env == null) {
            return DERIVATIVES_CROWDING_STATE_NEUTRAL;
        }
        if (!Boolean.TRUE.equals(env.getOiApplied())
                || !Boolean.TRUE.equals(env.getPerpFundingApplied())
                || env.getLastOpenInterest() == null
                || env.getLastFundingRate() == null) {
            return DERIVATIVES_CROWDING_STATE_NEUTRAL;
        }
        int fundingSign = env.getLastFundingRate().signum();
        if (fundingSign == 0) {
            return DERIVATIVES_CROWDING_STATE_NEUTRAL;
        }
        String crowded = fundingSign > 0
                ? DERIVATIVES_CROWDING_STATE_CROWDED_LONG
                : DERIVATIVES_CROWDING_STATE_CROWDED_SHORT;
        BigDecimal delta = env.getOpenInterestDelta();
        if (delta == null) {
            return crowded;
        }
        int deltaSign = delta.signum();
        if (deltaSign == 0) {
            return DERIVATIVES_CROWDING_STATE_NEUTRAL;
        }
        if ((fundingSign > 0 && deltaSign > 0) || (fundingSign < 0 && deltaSign < 0)) {
            return crowded;
        }
        return DERIVATIVES_CROWDING_STATE_NEUTRAL;
    }

    /**
     * OI 变化维度第一刀：仅在当前 OI 已应用、当前值非空且前一 snapshot OI 非空时计算 current - previous；否则返回 null。
     */
    public static BigDecimal computeOpenInterestDelta(Boolean oiApplied, BigDecimal currentOpenInterest,
                                                      BigDecimal previousOpenInterest) {
        if (!Boolean.TRUE.equals(oiApplied)) {
            return null;
        }
        if (currentOpenInterest == null || previousOpenInterest == null) {
            return null;
        }
        return currentOpenInterest.subtract(previousOpenInterest);
    }
}
