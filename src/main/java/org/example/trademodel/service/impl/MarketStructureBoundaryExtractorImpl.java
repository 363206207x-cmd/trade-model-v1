package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryLevelDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceRefDTO;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryDTO;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryRequest;
import org.example.trademodel.dto.planboundary.MarketStructureTakeProfitTargetDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.service.planboundary.MarketStructureBoundaryExtractor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MarketStructureBoundaryExtractorImpl implements MarketStructureBoundaryExtractor {

    public static final String REASON_OHLCV_MISSING = "OHLCV_MISSING";
    public static final String REASON_OHLCV_INSUFFICIENT_BARS = "OHLCV_INSUFFICIENT_BARS";
    public static final String REASON_OHLCV_STALE = "OHLCV_STALE";
    public static final String REASON_OHLCV_NOT_CONTIGUOUS = "OHLCV_NOT_CONTIGUOUS";
    public static final String REASON_OHLCV_PRICE_INVALID = "OHLCV_PRICE_INVALID";
    public static final String REASON_SOURCE_REF_MISSING = "SOURCE_REF_MISSING";
    public static final String REASON_DATA_QUALITY_UNSAFE = "DATA_QUALITY_UNSAFE";
    public static final String REASON_DIRECTION_MISSING = "DIRECTION_MISSING";
    public static final String REASON_DIRECTION_CONFLICTING = "DIRECTION_CONFLICTING";
    public static final String REASON_DIRECTION_UNSUPPORTED = "DIRECTION_UNSUPPORTED";
    public static final String REASON_TIMEFRAME_UNSUPPORTED = "TIMEFRAME_UNSUPPORTED";
    public static final String REASON_STRUCTURE_LEVEL_MISSING = "STRUCTURE_LEVEL_MISSING";
    public static final String REASON_TP_SOURCE_MISSING = "TP_SOURCE_MISSING";
    public static final String REASON_RR_UNAVAILABLE = "RR_UNAVAILABLE";
    public static final String REASON_RISK_GUARD_BLOCKED = "RISK_GUARD_BLOCKED";

    private static final int DEFAULT_MIN_BARS = 7;
    private static final int PIVOT_WINDOW = 2;
    private static final int DEFAULT_MAX_TARGETS = 2;
    private static final BigDecimal ENTRY_BUFFER_MULTIPLIER = new BigDecimal("0.10");
    private static final BigDecimal STOP_BUFFER_MULTIPLIER = new BigDecimal("0.25");
    private static final BigDecimal RR_LADDER_TARGET = new BigDecimal("2.00");
    private static final String QUALITY_OK = "OK";
    private static final String LONG = "LONG";
    private static final String SHORT = "SHORT";

    @Override
    public MarketStructureBoundaryDTO extract(MarketStructureBoundaryRequest request) {
        MarketStructureBoundaryDTO result = baseResult(request);
        List<String> blockingReasons = new ArrayList<>();
        String direction = normalizeDirection(request != null ? request.getDirection() : null, blockingReasons);
        result.setDirection(direction);

        if (request == null) {
            blockingReasons.add(REASON_OHLCV_MISSING);
            return fail(result, blockingReasons, "MISSING", "UNKNOWN");
        }
        if (Boolean.TRUE.equals(request.getRiskActionGuardBlocked())) {
            blockingReasons.add(REASON_RISK_GUARD_BLOCKED);
            if (hasText(request.getRiskActionGuardReason())) {
                blockingReasons.add(request.getRiskActionGuardReason().trim());
            }
        }

        List<RuntimeKlineItemDTO> bars = sortedBars(request.getBars());
        validateBars(request, bars, blockingReasons);
        if (!blockingReasons.isEmpty()) {
            return fail(result, blockingReasons, freshnessStatus(request, bars), dataQualityStatus(bars));
        }

        List<BoundaryLevelDTO> supports = extractSwingLevels(bars, request.getTimeframe(), "SUPPORT");
        List<BoundaryLevelDTO> resistances = extractSwingLevels(bars, request.getTimeframe(), "RESISTANCE");
        result.setSupportLevels(supports);
        result.setResistanceLevels(resistances);
        result.setSwingLow(mostRecent(supports));
        result.setSwingHigh(mostRecent(resistances));

        BigDecimal latestClose = bars.get(bars.size() - 1).getClosePrice();
        BigDecimal averageRange = averageRange(bars);
        if (LONG.equals(direction)) {
            if (supports.isEmpty()) {
                blockingReasons.add(REASON_STRUCTURE_LEVEL_MISSING);
                return fail(result, blockingReasons, "FRESH", "OK");
            }
            buildLongBoundary(request, result, supports, resistances, latestClose, averageRange, blockingReasons);
        } else if (SHORT.equals(direction)) {
            if (resistances.isEmpty()) {
                blockingReasons.add(REASON_STRUCTURE_LEVEL_MISSING);
                return fail(result, blockingReasons, "FRESH", "OK");
            }
            buildShortBoundary(request, result, supports, resistances, latestClose, averageRange, blockingReasons);
        } else {
            blockingReasons.add(REASON_DIRECTION_UNSUPPORTED);
        }

        if (!blockingReasons.isEmpty()) {
            return fail(result, blockingReasons, "FRESH", "OK");
        }
        result.setBoundaryReady(true);
        result.setFreshnessStatus("FRESH");
        result.setDataQualityStatus("OK");
        result.setPositionSizingStatus("POSITION_SIZING_NOT_PRODUCED");
        result.setLeverageSuggestion(request.getLeverageSuggestion());
        return result;
    }

    private void buildLongBoundary(
            MarketStructureBoundaryRequest request,
            MarketStructureBoundaryDTO result,
            List<BoundaryLevelDTO> supports,
            List<BoundaryLevelDTO> resistances,
            BigDecimal latestClose,
            BigDecimal averageRange,
            List<String> blockingReasons
    ) {
        BoundaryLevelDTO support = supports.stream()
                .filter(level -> level.getPrice().compareTo(latestClose) <= 0)
                .max(Comparator.comparing(BoundaryLevelDTO::getBarTime))
                .orElse(null);
        if (support == null) {
            blockingReasons.add(REASON_STRUCTURE_LEVEL_MISSING);
            return;
        }
        BigDecimal entry = support.getPrice();
        BigDecimal entryBuffer = buffer(averageRange, ENTRY_BUFFER_MULTIPLIER);
        BigDecimal stopBuffer = buffer(averageRange, STOP_BUFFER_MULTIPLIER);
        BigDecimal stop = entry.subtract(stopBuffer);
        result.setEntryLower(entry.subtract(entryBuffer));
        result.setEntryUpper(entry.add(entryBuffer));
        result.setEntrySourceType("SUPPORT_RETEST");
        result.setEntrySourceRef(support.getSourceRef());
        result.setEntryReason("基于新鲜 OHLCV 摆动低点的支撑回踩区");
        result.setStopPrice(stop);
        result.setStopSourceType("SWING_LOW_STRUCTURE_BUFFER");
        result.setStopSourceRef(support.getSourceRef());
        result.setStopReason("位于选定支撑摆动低点下方，并包含确定性结构缓冲");

        List<BoundaryLevelDTO> targets = resistances.stream()
                .filter(level -> level.getPrice().compareTo(entry) > 0)
                .sorted(Comparator.comparing(BoundaryLevelDTO::getPrice))
                .limit(maxTargets(request))
                .toList();
        buildTargetsOrRrLadder(request, result, targets, entry, stop, true, blockingReasons);
        if (stop.compareTo(entry) >= 0) {
            blockingReasons.add(REASON_RR_UNAVAILABLE);
        }
    }

    private void buildShortBoundary(
            MarketStructureBoundaryRequest request,
            MarketStructureBoundaryDTO result,
            List<BoundaryLevelDTO> supports,
            List<BoundaryLevelDTO> resistances,
            BigDecimal latestClose,
            BigDecimal averageRange,
            List<String> blockingReasons
    ) {
        BoundaryLevelDTO resistance = resistances.stream()
                .filter(level -> level.getPrice().compareTo(latestClose) >= 0)
                .max(Comparator.comparing(BoundaryLevelDTO::getBarTime))
                .orElse(null);
        if (resistance == null) {
            blockingReasons.add(REASON_STRUCTURE_LEVEL_MISSING);
            return;
        }
        BigDecimal entry = resistance.getPrice();
        BigDecimal entryBuffer = buffer(averageRange, ENTRY_BUFFER_MULTIPLIER);
        BigDecimal stopBuffer = buffer(averageRange, STOP_BUFFER_MULTIPLIER);
        BigDecimal stop = entry.add(stopBuffer);
        result.setEntryLower(entry.subtract(entryBuffer));
        result.setEntryUpper(entry.add(entryBuffer));
        result.setEntrySourceType("RESISTANCE_RETEST");
        result.setEntrySourceRef(resistance.getSourceRef());
        result.setEntryReason("基于新鲜 OHLCV 摆动高点的阻力回踩区");
        result.setStopPrice(stop);
        result.setStopSourceType("SWING_HIGH_STRUCTURE_BUFFER");
        result.setStopSourceRef(resistance.getSourceRef());
        result.setStopReason("位于选定阻力摆动高点上方，并包含确定性结构缓冲");

        List<BoundaryLevelDTO> targets = supports.stream()
                .filter(level -> level.getPrice().compareTo(entry) < 0)
                .sorted(Comparator.comparing(BoundaryLevelDTO::getPrice).reversed())
                .limit(maxTargets(request))
                .toList();
        buildTargetsOrRrLadder(request, result, targets, entry, stop, false, blockingReasons);
        if (stop.compareTo(entry) <= 0) {
            blockingReasons.add(REASON_RR_UNAVAILABLE);
        }
    }

    private void buildTargetsOrRrLadder(
            MarketStructureBoundaryRequest request,
            MarketStructureBoundaryDTO result,
            List<BoundaryLevelDTO> targets,
            BigDecimal entry,
            BigDecimal stop,
            boolean longDirection,
            List<String> blockingReasons
    ) {
        List<MarketStructureTakeProfitTargetDTO> takeProfitTargets = new ArrayList<>();
        if (!targets.isEmpty()) {
            for (BoundaryLevelDTO target : targets) {
                MarketStructureTakeProfitTargetDTO dto = new MarketStructureTakeProfitTargetDTO();
                dto.setTargetPrice(target.getPrice());
                dto.setTargetType(longDirection ? "STRUCTURE_RESISTANCE" : "STRUCTURE_SUPPORT");
                dto.setSourceRef(target.getSourceRef());
                dto.setReason(longDirection
                        ? "取自新鲜 OHLCV 摆动高点的下一阻力位"
                        : "取自新鲜 OHLCV 摆动低点的下一支撑位");
                dto.setRr(computeRr(entry, stop, target.getPrice(), longDirection));
                takeProfitTargets.add(dto);
            }
        } else if (request.isAllowRrLadder()) {
            MarketStructureTakeProfitTargetDTO ladder = new MarketStructureTakeProfitTargetDTO();
            BigDecimal risk = longDirection ? entry.subtract(stop) : stop.subtract(entry);
            if (risk.compareTo(BigDecimal.ZERO) <= 0) {
                blockingReasons.add(REASON_RR_UNAVAILABLE);
                return;
            }
            ladder.setTargetPrice(longDirection
                    ? entry.add(risk.multiply(RR_LADDER_TARGET))
                    : entry.subtract(risk.multiply(RR_LADDER_TARGET)));
            ladder.setTargetType("RR_LADDER");
            ladder.setRuleRef("RR_LADDER");
            ladder.setSourceRef("RR_LADDER:" + result.getEntrySourceRef() + ":" + result.getStopSourceRef());
            ladder.setReason("基于来源明确的入场与止损边界生成 RR 阶梯目标");
            ladder.setRr(RR_LADDER_TARGET);
            takeProfitTargets.add(ladder);
        } else {
            blockingReasons.add(REASON_TP_SOURCE_MISSING);
            return;
        }

        MarketStructureTakeProfitTargetDTO firstTarget = takeProfitTargets.get(0);
        BigDecimal rr = computeRr(entry, stop, firstTarget.getTargetPrice(), longDirection);
        if (rr == null || rr.compareTo(BigDecimal.ZERO) <= 0) {
            blockingReasons.add(REASON_RR_UNAVAILABLE);
            return;
        }
        result.setTakeProfitTargets(takeProfitTargets);
        result.setRrRatio(rr);
        List<BoundarySourceRefDTO> refs = new ArrayList<>();
        refs.add(sourceRef(request, "ENTRY", result.getTimeframe(), null, null,
                result.getEntrySourceRef(), result.getEntryReason()));
        refs.add(sourceRef(request, "STOP", result.getTimeframe(), null, null,
                result.getStopSourceRef(), result.getStopReason()));
        for (int i = 0; i < takeProfitTargets.size(); i++) {
            MarketStructureTakeProfitTargetDTO target = takeProfitTargets.get(i);
            refs.add(sourceRef(request, target.getTargetType(), result.getTimeframe(), null, i,
                    target.getSourceRef(), target.getReason()));
        }
        refs.add(sourceRef(request, "RISK_REWARD", result.getTimeframe(), null, null,
                result.getEntrySourceRef() + "|" + result.getStopSourceRef()
                        + "|" + firstTarget.getSourceRef(),
                "由同一分析的确定性入场、止损和目标实时计算"));
        result.setSourceRefs(refs);
    }

    private List<BoundaryLevelDTO> extractSwingLevels(
            List<RuntimeKlineItemDTO> bars,
            String timeframe,
            String type
    ) {
        List<BoundaryLevelDTO> levels = new ArrayList<>();
        BigDecimal tolerance = buffer(averageRange(bars), ENTRY_BUFFER_MULTIPLIER);
        for (int i = PIVOT_WINDOW; i < bars.size() - PIVOT_WINDOW; i++) {
            RuntimeKlineItemDTO bar = bars.get(i);
            boolean support = "SUPPORT".equals(type);
            if (support ? isSwingLow(bars, i) : isSwingHigh(bars, i)) {
                BoundaryLevelDTO level = new BoundaryLevelDTO();
                level.setPrice(support ? bar.getLowPrice() : bar.getHighPrice());
                level.setLevelType(support ? "SUPPORT" : "RESISTANCE");
                level.setTimeframe(timeframe);
                level.setBarTime(bar.getCloseTimeMs());
                level.setSourceRef(levelSourceRef(support ? "SUPPORT" : "RESISTANCE", timeframe, i, bar));
                level.setReason(support
                        ? "新鲜 OHLCV 中识别的枢轴摆动低点"
                        : "新鲜 OHLCV 中识别的枢轴摆动高点");
                level.setStrength(touchStrength(bars, level.getPrice(), support, tolerance));
                levels.add(level);
            }
        }
        return levels;
    }

    private boolean isSwingHigh(List<RuntimeKlineItemDTO> bars, int index) {
        BigDecimal current = bars.get(index).getHighPrice();
        boolean greaterThanAny = false;
        for (int i = index - PIVOT_WINDOW; i <= index + PIVOT_WINDOW; i++) {
            if (i == index) {
                continue;
            }
            int cmp = current.compareTo(bars.get(i).getHighPrice());
            if (cmp < 0) {
                return false;
            }
            if (cmp > 0) {
                greaterThanAny = true;
            }
        }
        return greaterThanAny;
    }

    private boolean isSwingLow(List<RuntimeKlineItemDTO> bars, int index) {
        BigDecimal current = bars.get(index).getLowPrice();
        boolean lowerThanAny = false;
        for (int i = index - PIVOT_WINDOW; i <= index + PIVOT_WINDOW; i++) {
            if (i == index) {
                continue;
            }
            int cmp = current.compareTo(bars.get(i).getLowPrice());
            if (cmp > 0) {
                return false;
            }
            if (cmp < 0) {
                lowerThanAny = true;
            }
        }
        return lowerThanAny;
    }

    private int touchStrength(
            List<RuntimeKlineItemDTO> bars,
            BigDecimal price,
            boolean support,
            BigDecimal tolerance
    ) {
        int strength = 1;
        for (RuntimeKlineItemDTO bar : bars) {
            BigDecimal candidate = support ? bar.getLowPrice() : bar.getHighPrice();
            if (candidate.subtract(price).abs().compareTo(tolerance) <= 0) {
                strength++;
            }
        }
        return strength;
    }

    private void validateBars(
            MarketStructureBoundaryRequest request,
            List<RuntimeKlineItemDTO> bars,
            List<String> blockingReasons
    ) {
        if (bars == null || bars.isEmpty()) {
            blockingReasons.add(REASON_OHLCV_MISSING);
            return;
        }
        int minBars = minBars(request);
        if (bars.size() < minBars) {
            blockingReasons.add(REASON_OHLCV_INSUFFICIENT_BARS);
        }
        Long timeframeMs = parseTimeframeMs(request.getTimeframe());
        if (timeframeMs == null) {
            blockingReasons.add(REASON_TIMEFRAME_UNSUPPORTED);
        }
        boolean qualityUnsafe = false;
        for (RuntimeKlineItemDTO bar : bars) {
            if (!isValidBar(bar)) {
                blockingReasons.add(REASON_OHLCV_PRICE_INVALID);
                break;
            }
            if (!hasText(bar.getSourceTraceId())) {
                blockingReasons.add(REASON_SOURCE_REF_MISSING);
                break;
            }
            if (!QUALITY_OK.equals(bar.getQualityStatus())) {
                qualityUnsafe = true;
            }
        }
        if (qualityUnsafe) {
            blockingReasons.add(REASON_DATA_QUALITY_UNSAFE);
        }
        if (timeframeMs != null && !isContiguous(bars, timeframeMs)) {
            blockingReasons.add(REASON_OHLCV_NOT_CONTIGUOUS);
        }
        if (isStale(request, bars)) {
            blockingReasons.add(REASON_OHLCV_STALE);
        }
    }

    private boolean isValidBar(RuntimeKlineItemDTO bar) {
        if (bar == null) {
            return false;
        }
        if (bar.getOpenTimeMs() == null || bar.getCloseTimeMs() == null
                || bar.getOpenTimeMs() >= bar.getCloseTimeMs()) {
            return false;
        }
        if (nonPositive(bar.getHighPrice()) || nonPositive(bar.getLowPrice()) || nonPositive(bar.getClosePrice())) {
            return false;
        }
        return bar.getHighPrice().compareTo(bar.getLowPrice()) >= 0;
    }

    private boolean isContiguous(List<RuntimeKlineItemDTO> bars, long timeframeMs) {
        for (int i = 0; i < bars.size() - 1; i++) {
            RuntimeKlineItemDTO current = bars.get(i);
            RuntimeKlineItemDTO next = bars.get(i + 1);
            if (current.getOpenTimeMs() == null || next.getOpenTimeMs() == null) {
                return false;
            }
            if (next.getOpenTimeMs() - current.getOpenTimeMs() != timeframeMs) {
                return false;
            }
        }
        return true;
    }

    private boolean isStale(MarketStructureBoundaryRequest request, List<RuntimeKlineItemDTO> bars) {
        if (request.getFreshnessLimitMs() == null || request.getFreshnessLimitMs() <= 0
                || request.getGeneratedAtEpochMs() == null) {
            return false;
        }
        RuntimeKlineItemDTO latest = bars.get(bars.size() - 1);
        return latest.getCloseTimeMs() == null
                || request.getGeneratedAtEpochMs() - latest.getCloseTimeMs() > request.getFreshnessLimitMs();
    }

    private MarketStructureBoundaryDTO baseResult(MarketStructureBoundaryRequest request) {
        MarketStructureBoundaryDTO result = new MarketStructureBoundaryDTO();
        if (request != null) {
            result.setSymbol(request.getSymbol());
            result.setTimeframe(request.getTimeframe());
            result.setGeneratedAt(request.getGeneratedAt());
            result.setLeverageSuggestion(request.getLeverageSuggestion());
        }
        result.setBoundaryReady(false);
        result.setFreshnessStatus("UNKNOWN");
        result.setDataQualityStatus("UNKNOWN");
        result.setPositionSizingStatus("POSITION_SIZING_NOT_PRODUCED");
        return result;
    }

    private MarketStructureBoundaryDTO fail(
            MarketStructureBoundaryDTO result,
            List<String> blockingReasons,
            String freshnessStatus,
            String dataQualityStatus
    ) {
        result.setBoundaryReady(false);
        result.setBlockingReasons(dedupe(blockingReasons));
        result.setFreshnessStatus(freshnessStatus);
        result.setDataQualityStatus(dataQualityStatus);
        result.setPositionSizingStatus("POSITION_SIZING_NOT_PRODUCED");
        return result;
    }

    private String normalizeDirection(String raw, List<String> blockingReasons) {
        if (!hasText(raw)) {
            blockingReasons.add(REASON_DIRECTION_MISSING);
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("LONG".equals(normalized) || normalized.endsWith("BULLISH") || "做多".equals(raw.trim())) {
            return LONG;
        }
        if ("SHORT".equals(normalized) || normalized.endsWith("BEARISH") || "做空".equals(raw.trim())) {
            return SHORT;
        }
        if ("CONFLICT".equals(normalized) || "CONFLICTING".equals(normalized) || "MIXED".equals(normalized)) {
            blockingReasons.add(REASON_DIRECTION_CONFLICTING);
            return null;
        }
        blockingReasons.add(REASON_DIRECTION_UNSUPPORTED);
        return null;
    }

    private List<RuntimeKlineItemDTO> sortedBars(List<RuntimeKlineItemDTO> bars) {
        if (bars == null) {
            return List.of();
        }
        return bars.stream()
                .sorted(Comparator
                        .comparing(RuntimeKlineItemDTO::getOpenTimeMs, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(RuntimeKlineItemDTO::getCloseTimeMs, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private String freshnessStatus(MarketStructureBoundaryRequest request, List<RuntimeKlineItemDTO> bars) {
        if (bars == null || bars.isEmpty()) {
            return "MISSING";
        }
        if (isStale(request, bars)) {
            return "STALE";
        }
        return "UNKNOWN";
    }

    private String dataQualityStatus(List<RuntimeKlineItemDTO> bars) {
        if (bars == null || bars.isEmpty()) {
            return "UNKNOWN";
        }
        return bars.stream().allMatch(bar -> bar != null && QUALITY_OK.equals(bar.getQualityStatus()))
                ? "OK"
                : "UNSAFE";
    }

    private Long parseTimeframeMs(String timeframe) {
        if (!hasText(timeframe) || timeframe.length() < 2) {
            return null;
        }
        String unit = timeframe.substring(timeframe.length() - 1);
        String amountText = timeframe.substring(0, timeframe.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) {
            return null;
        }
        return switch (unit) {
            case "m" -> amount * 60_000L;
            case "h" -> amount * 60L * 60_000L;
            case "d" -> amount * 24L * 60L * 60_000L;
            default -> null;
        };
    }

    private BoundaryLevelDTO mostRecent(List<BoundaryLevelDTO> levels) {
        return levels.stream()
                .max(Comparator.comparing(BoundaryLevelDTO::getBarTime, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private BigDecimal averageRange(List<RuntimeKlineItemDTO> bars) {
        if (bars == null || bars.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (RuntimeKlineItemDTO bar : bars) {
            total = total.add(bar.getHighPrice().subtract(bar.getLowPrice()).abs());
        }
        return total.divide(BigDecimal.valueOf(bars.size()), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal buffer(BigDecimal averageRange, BigDecimal multiplier) {
        if (averageRange == null || averageRange.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return averageRange.multiply(multiplier).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal computeRr(BigDecimal entry, BigDecimal stop, BigDecimal target, boolean longDirection) {
        BigDecimal risk = longDirection ? entry.subtract(stop) : stop.subtract(entry);
        BigDecimal reward = longDirection ? target.subtract(entry) : entry.subtract(target);
        if (risk.compareTo(BigDecimal.ZERO) <= 0 || reward.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return reward.divide(risk, 4, RoundingMode.HALF_UP);
    }

    private String levelSourceRef(String sourceType, String timeframe, int index, RuntimeKlineItemDTO bar) {
        return "MARKET_STRUCTURE:" + sourceType + ":" + timeframe + ":" + bar.getCloseTimeMs()
                + ":" + bar.getSourceTraceId() + ":" + index;
    }

    private BoundarySourceRefDTO sourceRef(
            MarketStructureBoundaryRequest request,
            String sourceType,
            String timeframe,
            Long barTime,
            Integer index,
            String sourceId,
            String calculationReason
    ) {
        BoundarySourceRefDTO ref = new BoundarySourceRefDTO();
        RuntimeKlineItemDTO latest = request == null || request.getBars() == null
                || request.getBars().isEmpty() ? null
                : sortedBars(request.getBars()).get(sortedBars(request.getBars()).size() - 1);
        Long effectiveBarTime = barTime != null ? barTime : latest == null ? null : latest.getCloseTimeMs();
        ref.setSourceType(sourceType);
        ref.setTimeframe(timeframe);
        ref.setBarTime(effectiveBarTime);
        ref.setIndex(index);
        ref.setReason(calculationReason);
        ref.setSourceId(sourceId);
        ref.setProvider(latest == null ? null : latest.getProvider());
        ref.setObservedAt(effectiveBarTime == null ? null
                : java.time.Instant.ofEpochMilli(effectiveBarTime).toString());
        ref.setStructureId(sourceType + ":" + timeframe + ":" + sourceId);
        ref.setCalculationReason(calculationReason);
        ref.setAnalysisId(request == null ? null : request.getAnalysisId());
        return ref;
    }

    private List<String> dedupe(List<String> values) {
        List<String> deduped = new ArrayList<>();
        if (values == null) {
            return deduped;
        }
        for (String value : values) {
            if (hasText(value) && !deduped.contains(value)) {
                deduped.add(value);
            }
        }
        return deduped;
    }

    private int minBars(MarketStructureBoundaryRequest request) {
        return request.getMinBars() != null && request.getMinBars() > 0
                ? request.getMinBars()
                : DEFAULT_MIN_BARS;
    }

    private int maxTargets(MarketStructureBoundaryRequest request) {
        return request.getMaxTargets() != null && request.getMaxTargets() > 0
                ? request.getMaxTargets()
                : DEFAULT_MAX_TARGETS;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean nonPositive(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }
}
