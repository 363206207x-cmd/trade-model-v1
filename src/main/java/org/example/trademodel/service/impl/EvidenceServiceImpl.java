package org.example.trademodel.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.entity.HotResetEventDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvidenceServiceImpl implements EvidenceService {
    /** 与 {@link org.example.trademodel.market.RealMarketEnvironmentService#mapSnapshot} 写入值一致；仅此二者视为行情链真实杠杆建议。 */
    private static final String LEVERAGE_SUGGESTION_LOW = "low_leverage";
    private static final String LEVERAGE_SUGGESTION_MODERATE = "moderate_leverage";

    /**
     * 日内启发式价格结构代理：方向判定死区（百分点）；与行情 {@code priceChangePercent24h} 同量纲。
     */
    public static final BigDecimal PRICE_STRUCTURE_DIRECTION_EPSILON_PCT = new BigDecimal("0.05");

    private static final String PRICE_STRUCTURE_BOUNDARY_TAIL =
            "该证据 = 日内启发式价格结构代理，非规格级 K 线结构模块。";
    private static final String MACRO_EVIDENCE_TEMPLATE_WITH_CROWDING =
            "当前环境呈现%s，24h振幅约%.2f%%；衍生品拥挤状态：%s。";
    private static final String MACRO_EVIDENCE_TEMPLATE_MINIMAL =
            "当前环境呈现%s，24h振幅约%.2f%%。";

    private final EvidenceItemMapper evidenceItemMapper;
    private final HotResetEventMapper hotResetEventMapper;

    public EvidenceServiceImpl(EvidenceItemMapper evidenceItemMapper) {
        this(evidenceItemMapper, null);
    }

    @Autowired
    public EvidenceServiceImpl(EvidenceItemMapper evidenceItemMapper,
                               HotResetEventMapper hotResetEventMapper) {
        this.evidenceItemMapper = evidenceItemMapper;
        this.hotResetEventMapper = hotResetEventMapper;
    }

    @Override
    public List<EvidenceItemVO> buildEvidence(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv) {
        List<EvidenceItemVO> list = new ArrayList<>();
        list.add(buildPriceStructureEvidence(marketEnv));
        appendHotResetEventEvidenceIfExists(list, assetAnalysis);
        populateEventImpactInputFromHotReset(assetAnalysis);
        appendMacroEvidenceFromMarketEnvIfExists(list, marketEnv);

        if (marketEnv != null && marketEnv.getRangePct24h() != null
                && marketEnv.getVolatilityRegime() != null && !marketEnv.getVolatilityRegime().isBlank()) {
            EvidenceItemVO vol = new EvidenceItemVO();
            vol.setEvidenceId("ev-" + System.nanoTime());
            vol.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(EvidenceTypeConstants.RISK));
            vol.setDirection(
                    EvidenceTypeConstants.normalizeEvidenceDirection(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL));
            vol.setSource(
                    EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC));
            double rp = marketEnv.getRangePct24h();
            String regime = marketEnv.getVolatilityRegime().trim();
            vol.setDescription(String.format(Locale.US,
                    "24h 价格振幅约 %.2f%%（%s）；口径：Binance 24h ticker 启发式。",
                    rp, regime));
            list.add(vol);
        }

        if (marketEnv != null && hasRealChainLeverageSuggestion(marketEnv)) {
            EvidenceItemVO lev = new EvidenceItemVO();
            lev.setEvidenceId("ev-" + System.nanoTime());
            lev.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(EvidenceTypeConstants.LEVERAGE));
            lev.setDirection(
                    EvidenceTypeConstants.normalizeEvidenceDirection(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL));
            lev.setSource(
                    EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC));
            lev.setDescription(buildLeverageEvidenceDescription(marketEnv.getLeverageSuggestion()));
            list.add(lev);
        }

        if (marketEnv != null && Boolean.TRUE.equals(marketEnv.getPerpFundingApplied())
                && marketEnv.getLastFundingRate() != null) {
            EvidenceItemVO fund = new EvidenceItemVO();
            fund.setEvidenceId("ev-" + System.nanoTime());
            fund.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(EvidenceTypeConstants.FUNDING));
            fund.setDirection(
                    EvidenceTypeConstants.normalizeEvidenceDirection(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL));
            fund.setSource(
                    EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC));
            fund.setDescription(RealMarketEnvironmentService.buildFundingAppendix(marketEnv.getLastFundingRate()).trim());
            list.add(fund);
        }
        if (marketEnv != null && Boolean.TRUE.equals(marketEnv.getOiApplied())
                && marketEnv.getLastOpenInterest() != null) {
            EvidenceItemVO oi = new EvidenceItemVO();
            oi.setEvidenceId("ev-" + System.nanoTime());
            oi.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(EvidenceTypeConstants.RISK));
            oi.setDirection(
                    EvidenceTypeConstants.normalizeEvidenceDirection(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL));
            oi.setSource(
                    EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC));
            oi.setDescription(RealMarketEnvironmentService.buildOpenInterestAppendix(marketEnv.getLastOpenInterest()).trim());
            list.add(oi);
        }

        return list;
    }

    private void populateEventImpactInputFromHotReset(AssetAnalysisVO assetAnalysis) {
        if (assetAnalysis == null) {
            return;
        }
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.FALSE);
        input.setEventFactCount(0);
        if (hotResetEventMapper == null) {
            assetAnalysis.setEventImpactInput(input);
            return;
        }
        String analysisId = assetAnalysis.getAnalysisId();
        if (analysisId == null || analysisId.isBlank()) {
            assetAnalysis.setEventImpactInput(input);
            return;
        }
        String normalizedId = analysisId.trim();
        HotResetEventDO latest = hotResetEventMapper.selectLatestByAnalysisId(normalizedId);
        Integer count = hotResetEventMapper.countByAnalysisId(normalizedId);
        int safeCount = count != null && count > 0 ? count : 0;
        input.setEventFactCount(safeCount);
        input.setEventFactHit(safeCount > 0 && latest != null);
        if (latest != null) {
            input.setEventLatestTime(latest.getEventTime());
            input.setEventReasonCode(latest.getTriggerReasonCode());
            input.setEventTriggerType(latest.getTriggerType());
            input.setEventVersion(latest.getEventVersion());
            input.setEventTraceId(latest.getTraceId());
        }
        assetAnalysis.setEventImpactInput(input);
    }

    private void appendMacroEvidenceFromMarketEnvIfExists(List<EvidenceItemVO> list, MarketEnvironmentVO marketEnv) {
        if (marketEnv == null || marketEnv.getRangePct24h() == null) {
            return;
        }
        String volatilityRegime = marketEnv.getVolatilityRegime();
        if (volatilityRegime == null || volatilityRegime.isBlank()) {
            return;
        }
        String normalizedRegime = volatilityRegime.trim();
        String crowdingState = marketEnv.getDerivativesCrowdingState();
        String description;
        if (crowdingState != null && !crowdingState.isBlank()) {
            description = String.format(Locale.US,
                    MACRO_EVIDENCE_TEMPLATE_WITH_CROWDING,
                    normalizedRegime,
                    marketEnv.getRangePct24h(),
                    crowdingState.trim());
        } else {
            description = String.format(Locale.US,
                    MACRO_EVIDENCE_TEMPLATE_MINIMAL,
                    normalizedRegime,
                    marketEnv.getRangePct24h());
        }
        EvidenceItemVO macro = new EvidenceItemVO();
        macro.setEvidenceId("ev-" + System.nanoTime());
        macro.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(EvidenceTypeConstants.MACRO));
        macro.setDirection(
                EvidenceTypeConstants.normalizeEvidenceDirection(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL));
        macro.setSource(
                EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_SYSTEM_GENERATED));
        macro.setDescription(description);
        list.add(macro);
    }

    private void appendHotResetEventEvidenceIfExists(List<EvidenceItemVO> list, AssetAnalysisVO assetAnalysis) {
        if (hotResetEventMapper == null) {
            return;
        }
        if (assetAnalysis == null || assetAnalysis.getAnalysisId() == null || assetAnalysis.getAnalysisId().isBlank()) {
            return;
        }
        HotResetEventDO event = hotResetEventMapper.selectLatestByAnalysisId(assetAnalysis.getAnalysisId().trim());
        if (event == null) {
            return;
        }
        EvidenceItemVO ev = new EvidenceItemVO();
        ev.setEvidenceId("ev-" + System.nanoTime());
        ev.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(EvidenceTypeConstants.EVENT));
        ev.setDirection(EvidenceTypeConstants.normalizeEvidenceDirection(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL));
        ev.setSource(EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_SYSTEM_GENERATED));
        ev.setDescription(buildHotResetEventEvidenceDescription(event));
        list.add(ev);
    }

    static String buildHotResetEventEvidenceDescription(HotResetEventDO event) {
        String triggerType = event != null && event.getTriggerType() != null && !event.getTriggerType().isBlank()
                ? event.getTriggerType().trim()
                : "HOT_RESET";
        String reasonCode = event != null && event.getTriggerReasonCode() != null && !event.getTriggerReasonCode().isBlank()
                ? event.getTriggerReasonCode().trim()
                : "N/A";
        return "检测到 Hot Reset 事件：triggerType=" + triggerType + "，reasonCode=" + reasonCode + "。";
    }

    private EvidenceItemVO buildPriceStructureEvidence(MarketEnvironmentVO marketEnv) {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceId("ev-" + System.currentTimeMillis());
        e.setEvidenceType(EvidenceTypeConstants.normalizeEvidenceType(EvidenceTypeConstants.PRICE_STRUCTURE));
        BigDecimal pct = marketEnv != null ? marketEnv.getPriceChangePercent24h() : null;
        if (pct != null) {
            String dir = determinePriceStructureDirection(pct);
            e.setDirection(EvidenceTypeConstants.normalizeEvidenceDirection(dir));
            e.setSource(EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_MARKET_HEURISTIC));
            e.setDescription(buildPriceStructureDescriptionFromPct(pct));
        } else {
            e.setDirection(EvidenceTypeConstants.normalizeEvidenceDirection(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL));
            e.setSource(EvidenceTypeConstants.normalizeEvidenceSource(EvidenceTypeConstants.EVIDENCE_SOURCE_SYSTEM_GENERATED));
            e.setDescription(buildPriceStructureDescriptionWhenPctMissing());
        }
        return e;
    }

    /**
     * {@code abs(pct) &lt; ε} → NEUTRAL；否则按符号分为 BULLISH / BEARISH。不与 environmentType / riskMode 混用。
     */
    static String determinePriceStructureDirection(BigDecimal pct) {
        if (pct == null) {
            return EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL;
        }
        if (pct.abs().compareTo(PRICE_STRUCTURE_DIRECTION_EPSILON_PCT) < 0) {
            return EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL;
        }
        return pct.signum() > 0
                ? EvidenceTypeConstants.EVIDENCE_DIRECTION_BULLISH
                : EvidenceTypeConstants.EVIDENCE_DIRECTION_BEARISH;
    }

    private static String buildPriceStructureDescriptionFromPct(BigDecimal pct) {
        String pctLabel = String.format(Locale.US, "%+.2f%%", pct);
        return String.format(Locale.US,
                "日内启发式价格结构代理：24h 涨跌约 %s（Binance 24h ticker）；口径：启发式。%s",
                pctLabel,
                PRICE_STRUCTURE_BOUNDARY_TAIL);
    }

    private static String buildPriceStructureDescriptionWhenPctMissing() {
        return "日内启发式价格结构代理：当前缺少 24h 涨跌幅标量，无法给出方向代理。" + PRICE_STRUCTURE_BOUNDARY_TAIL;
    }

    /**
     * 占位 fallback 不写 {@code leverageSuggestion}；仅行情链会为 VO 填入 {@code low_leverage}/{@code moderate_leverage}。
     */
    static boolean hasRealChainLeverageSuggestion(MarketEnvironmentVO env) {
        if (env == null) {
            return false;
        }
        String ls = env.getLeverageSuggestion();
        if (ls == null || ls.isBlank()) {
            return false;
        }
        String t = ls.trim();
        return LEVERAGE_SUGGESTION_LOW.equals(t) || LEVERAGE_SUGGESTION_MODERATE.equals(t);
    }

    static String buildLeverageEvidenceDescription(String leverageSuggestion) {
        String tail = "口径：Binance 24h 启发式。";
        String t = leverageSuggestion != null ? leverageSuggestion.trim() : "";
        if (LEVERAGE_SUGGESTION_LOW.equals(t)) {
            return "低杠杆建议；" + tail;
        }
        return "适中杠杆建议；" + tail;
    }

    /**
     * run 级 DQ carve-out 窄匹配：与 {@link #buildLeverageEvidenceDescription} 当前两档输出逐字一致。
     */
    public static final String LEVERAGE_EVIDENCE_DESCRIPTION_LOW =
            buildLeverageEvidenceDescription("low_leverage");
    /** @see #LEVERAGE_EVIDENCE_DESCRIPTION_LOW */
    public static final String LEVERAGE_EVIDENCE_DESCRIPTION_MODERATE =
            buildLeverageEvidenceDescription("moderate_leverage");

    @Override
    public List<EvidenceBriefVO> listTopEvidenceBriefByAnalysisId(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return Collections.emptyList();
        }
        List<EvidenceBriefVO> rows = evidenceItemMapper.selectTop3BriefByAnalysisId(analysisId.trim());
        return rows != null ? rows : Collections.emptyList();
    }
}
