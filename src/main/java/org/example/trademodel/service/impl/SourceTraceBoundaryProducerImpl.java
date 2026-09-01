package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryCandidateSourceGate;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceFieldsDTO;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.ExecutionPlanSourceGate;
import org.example.trademodel.dto.planboundary.ExecutionPlanSourceGateResultDTO;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryDTO;
import org.example.trademodel.dto.planboundary.MarketStructureTakeProfitTargetDTO;
import org.example.trademodel.dto.planboundary.SourceTraceBoundaryProducerResult;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.planboundary.SourceTraceBoundaryProducer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class SourceTraceBoundaryProducerImpl implements SourceTraceBoundaryProducer {

    private static final String SOURCE_OWNER = "MARKET_STRUCTURE_BOUNDARY_EXTRACTOR";
    private static final String MIXED_TP_SOURCE_TYPE = "MARKET_STRUCTURE_TARGET_SET";
    private static final String DEFAULT_RR_RULE = "MARKET_STRUCTURE_RR";
    private static final List<String> NON_BOUNDARY_MISSING_FIELDS = List.of(
            "liquiditySource",
            "multiTimeframeSource",
            "eventSource",
            "wickSource"
    );

    @Override
    public SourceTraceBoundaryProducerResult produce(MarketStructureBoundaryDTO boundary) {
        SourceTraceDTO sourceTrace = baseTrace(boundary);
        SourceTraceBoundaryProducerResult result = new SourceTraceBoundaryProducerResult();
        result.setSourceTrace(sourceTrace);
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);

        if (boundary == null) {
            return failClosed(result, sourceTrace, List.of("boundary"), List.of("boundary missing"));
        }

        List<String> missingFields = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>(boundary.getBlockingReasons());
        validateBoundaryShape(boundary, missingFields, blockingReasons);

        if (!missingFields.isEmpty() || !blockingReasons.isEmpty()) {
            return failClosed(result, sourceTrace, missingFields, blockingReasons);
        }

        BigDecimal entryMidpoint = entryMidpoint(boundary);
        BoundaryEntryDTO entry = buildEntry(boundary, entryMidpoint);
        BoundaryStopDTO stop = buildStop(boundary);
        List<BoundaryTakeProfitLevelDTO> takeProfitLevels = buildTakeProfitLevels(boundary);
        BoundarySourceFieldsDTO sourceFields = buildSourceFields(boundary, takeProfitLevels);

        List<String> candidateMissing = BoundaryCandidateSourceGate.validate(
                entry,
                stop,
                takeProfitLevels,
                sourceFields
        ).getMissingSourceReasons();
        if (!candidateMissing.isEmpty()) {
            return failClosed(result, sourceTrace, candidateMissing, List.of("boundary candidate source incomplete"));
        }

        populateReadyBoundaryTrace(sourceTrace, boundary, takeProfitLevels, sourceFields, entryMidpoint);
        sourceTrace.setMissingFields(NON_BOUNDARY_MISSING_FIELDS);
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);

        result.setEntry(entry);
        result.setStop(stop);
        result.setTakeProfitLevels(takeProfitLevels);
        result.setSourceFields(sourceFields);
        result.setSourceRefs(boundary.getSourceRefs());
        result.setBoundaryReady(true);
        result.setMissingFields(NON_BOUNDARY_MISSING_FIELDS);
        result.setBlockingReasons(List.of());
        ExecutionPlanSourceGateResultDTO sourceGate = ExecutionPlanSourceGate.validate(sourceTrace);
        result.setSourceTraceReady(sourceGate.isValid());
        return result;
    }

    private SourceTraceDTO baseTrace(MarketStructureBoundaryDTO boundary) {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSourceOwner(SOURCE_OWNER);
        sourceTrace.setReviewMode(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name());
        sourceTrace.setManualReviewRequired(true);
        sourceTrace.setNotTradeInstruction(true);
        if (boundary == null) {
            sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
            return sourceTrace;
        }
        sourceTrace.setSymbol(boundary.getSymbol());
        sourceTrace.setSymbolSource(SOURCE_OWNER);
        sourceTrace.setTimeframe(boundary.getTimeframe());
        sourceTrace.setTimeframeSource(SOURCE_OWNER);
        sourceTrace.setSourceTimeframe(boundary.getTimeframe());
        sourceTrace.setSourceWindow(boundary.getGeneratedAt() == null ? null : boundary.getGeneratedAt().toString());
        sourceTrace.setFreshnessStatus(boundary.getFreshnessStatus());
        sourceTrace.setDataQualityScoreSource(boundary.getDataQualityStatus());
        sourceTrace.setSourceRef(aggregateSourceRef(boundary));
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        return sourceTrace;
    }

    private void validateBoundaryShape(
            MarketStructureBoundaryDTO boundary,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        if (!boundary.isBoundaryReady()) {
            blockingReasons.add("boundaryReady=false");
        }
        addWhenBlank(boundary.getDirection(), "direction", missingFields);
        addWhenBlank(boundary.getTimeframe(), "timeframe", missingFields);
        addWhenNull(boundary.getEntryLower(), "entryLower", missingFields);
        addWhenNull(boundary.getEntryUpper(), "entryUpper", missingFields);
        addWhenBlank(boundary.getEntrySourceRef(), "entrySourceRef", missingFields);
        addWhenBlank(boundary.getEntryReason(), "entryReason", missingFields);
        addWhenBlank(boundary.getEntrySourceType(), "entrySourceType", missingFields);
        addWhenNull(boundary.getStopPrice(), "stopPrice", missingFields);
        addWhenBlank(boundary.getStopSourceRef(), "stopSourceRef", missingFields);
        addWhenBlank(boundary.getStopReason(), "stopReason", missingFields);
        addWhenBlank(boundary.getStopSourceType(), "stopSourceType", missingFields);

        Direction direction = normalizeDirection(boundary.getDirection());
        if (direction == Direction.UNKNOWN && hasText(boundary.getDirection())) {
            blockingReasons.add("direction unsupported: " + boundary.getDirection());
        }
        if (isUnsafeStatus(boundary.getFreshnessStatus())) {
            blockingReasons.add("freshnessStatus unsafe: " + boundary.getFreshnessStatus());
        }
        if (isUnsafeStatus(boundary.getDataQualityStatus())) {
            blockingReasons.add("dataQualityStatus unsafe: " + boundary.getDataQualityStatus());
        }

        if (!missingFields.isEmpty()) {
            return;
        }

        BigDecimal entryMidpoint = entryMidpoint(boundary);
        validateEntryRange(boundary, blockingReasons);
        validateStopDirection(boundary, direction, entryMidpoint, blockingReasons);
        validateTakeProfitTargets(boundary, direction, entryMidpoint, missingFields, blockingReasons);
    }

    private void validateEntryRange(MarketStructureBoundaryDTO boundary, List<String> blockingReasons) {
        if (boundary.getEntryLower().compareTo(boundary.getEntryUpper()) > 0) {
            blockingReasons.add("entryLower above entryUpper");
        }
    }

    private void validateStopDirection(
            MarketStructureBoundaryDTO boundary,
            Direction direction,
            BigDecimal entryMidpoint,
            List<String> blockingReasons
    ) {
        if (direction == Direction.LONG && boundary.getStopPrice().compareTo(entryMidpoint) >= 0) {
            blockingReasons.add("LONG stop must be below entry midpoint");
        }
        if (direction == Direction.SHORT && boundary.getStopPrice().compareTo(entryMidpoint) <= 0) {
            blockingReasons.add("SHORT stop must be above entry midpoint");
        }
    }

    private void validateTakeProfitTargets(
            MarketStructureBoundaryDTO boundary,
            Direction direction,
            BigDecimal entryMidpoint,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        List<MarketStructureTakeProfitTargetDTO> targets = boundary.getTakeProfitTargets();
        if (targets == null || targets.isEmpty()) {
            missingFields.add("takeProfitTargets");
            return;
        }
        for (int i = 0; i < targets.size(); i++) {
            MarketStructureTakeProfitTargetDTO target = targets.get(i);
            if (target == null) {
                missingFields.add("takeProfitTargets[" + i + "]");
                continue;
            }
            addWhenNull(target.getTargetPrice(), "takeProfitTargets[" + i + "].targetPrice", missingFields);
            addWhenBlank(target.getTargetType(), "takeProfitTargets[" + i + "].targetType", missingFields);
            addWhenBlank(target.getSourceRef(), "takeProfitTargets[" + i + "].sourceRef", missingFields);
            addWhenBlank(target.getReason(), "takeProfitTargets[" + i + "].reason", missingFields);
            addWhenNull(target.getRr(), "takeProfitTargets[" + i + "].rr", missingFields);
            if (isRrLadder(target) && target.getRr() == null) {
                missingFields.add("takeProfitTargets[" + i + "].rrLadderRr");
            }
            if (target.getTargetPrice() != null && direction == Direction.LONG
                    && target.getTargetPrice().compareTo(entryMidpoint) <= 0) {
                blockingReasons.add("LONG target must be above entry midpoint");
            }
            if (target.getTargetPrice() != null && direction == Direction.SHORT
                    && target.getTargetPrice().compareTo(entryMidpoint) >= 0) {
                blockingReasons.add("SHORT target must be below entry midpoint");
            }
        }
    }

    private BoundaryEntryDTO buildEntry(MarketStructureBoundaryDTO boundary, BigDecimal entryMidpoint) {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryType(boundary.getEntrySourceType());
        entry.setEntryPrice(entryMidpoint);
        entry.setEntryZoneLow(boundary.getEntryLower());
        entry.setEntryZoneHigh(boundary.getEntryUpper());
        entry.setNumericSourceType(boundary.getEntrySourceType());
        entry.setNumericSourceValue(entryMidpoint);
        entry.setSourceTimeframe(boundary.getTimeframe());
        entry.setReason(boundary.getEntryReason());
        return entry;
    }

    private BoundaryStopDTO buildStop(MarketStructureBoundaryDTO boundary) {
        BoundaryStopDTO stop = new BoundaryStopDTO();
        stop.setStopType(boundary.getStopSourceType());
        stop.setStopPrice(boundary.getStopPrice());
        stop.setNumericSourceType(boundary.getStopSourceType());
        stop.setNumericSourceValue(boundary.getStopPrice());
        stop.setSourceTimeframe(boundary.getTimeframe());
        stop.setReason(boundary.getStopReason());
        return stop;
    }

    private List<BoundaryTakeProfitLevelDTO> buildTakeProfitLevels(MarketStructureBoundaryDTO boundary) {
        List<BoundaryTakeProfitLevelDTO> levels = new ArrayList<>();
        List<MarketStructureTakeProfitTargetDTO> targets = boundary.getTakeProfitTargets();
        for (int i = 0; i < targets.size(); i++) {
            MarketStructureTakeProfitTargetDTO target = targets.get(i);
            BoundaryTakeProfitLevelDTO level = new BoundaryTakeProfitLevelDTO();
            level.setLevel(i + 1);
            level.setPrice(target.getTargetPrice());
            level.setRr(target.getRr());
            level.setSource(target.getTargetType());
            level.setNumericSourceType(target.getTargetType());
            level.setNumericSourceValue(target.getTargetPrice());
            level.setSourceTimeframe(boundary.getTimeframe());
            level.setSourceRef(target.getSourceRef());
            level.setReason(target.getReason());
            levels.add(level);
        }
        return levels;
    }

    private BoundarySourceFieldsDTO buildSourceFields(
            MarketStructureBoundaryDTO boundary,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels
    ) {
        BoundarySourceFieldsDTO sourceFields = new BoundarySourceFieldsDTO();
        sourceFields.setEntrySourceField(boundary.getEntrySourceType());
        sourceFields.setStopSourceField(boundary.getStopSourceType());
        sourceFields.setTakeProfitSourceField(aggregateTpSourceType(takeProfitLevels));
        sourceFields.setRrRule(rrRuleRef(boundary));
        sourceFields.setDataSource(SOURCE_OWNER);
        sourceFields.setEvidenceRefs(canonicalEvidenceRefs(boundary));
        return sourceFields;
    }

    private void populateReadyBoundaryTrace(
            SourceTraceDTO sourceTrace,
            MarketStructureBoundaryDTO boundary,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal entryMidpoint
    ) {
        sourceTrace.setEntryPriceSource(entryMidpoint);
        sourceTrace.setEntrySourceType(boundary.getEntrySourceType());
        sourceTrace.setEntrySourceTimeframe(boundary.getTimeframe());
        sourceTrace.setEntrySourceReason(boundary.getEntryReason());
        sourceTrace.setEntrySourceRef(boundary.getEntrySourceRef());

        sourceTrace.setStopPriceSource(boundary.getStopPrice());
        sourceTrace.setStopSourceType(boundary.getStopSourceType());
        sourceTrace.setStopSourceTimeframe(boundary.getTimeframe());
        sourceTrace.setStopSourceReason(boundary.getStopReason());
        sourceTrace.setStopSourceRef(boundary.getStopSourceRef());

        sourceTrace.setTpPriceSources(takeProfitLevels.stream()
                .map(BoundaryTakeProfitLevelDTO::getPrice)
                .toList());
        sourceTrace.setTpSourceType(sourceFields.getTakeProfitSourceField());
        sourceTrace.setTpSourceTimeframe(boundary.getTimeframe());
        sourceTrace.setTpSourceReason(aggregateTpReason(takeProfitLevels));
        sourceTrace.setTpSourceRef(aggregateTpSourceRef(takeProfitLevels));

        sourceTrace.setRrSource(rrSource(boundary));
        sourceTrace.setRrRuleRef(sourceFields.getRrRule());
    }

    private SourceTraceBoundaryProducerResult failClosed(
            SourceTraceBoundaryProducerResult result,
            SourceTraceDTO sourceTrace,
            List<String> missingFields,
            List<String> blockingReasons
    ) {
        List<String> safeMissingFields = copyWithoutBlank(missingFields);
        List<String> safeBlockingReasons = copyWithoutBlank(blockingReasons);
        sourceTrace.setMissingFields(safeMissingFields);
        sourceTrace.setBlockingReasons(safeBlockingReasons);
        sourceTrace.setFallbackStatus(safeBlockingReasons.isEmpty()
                ? SourceTraceFallbackStatusEnum.INCOMPLETE
                : SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        sourceTrace.setManualReviewRequired(true);
        sourceTrace.setNotTradeInstruction(true);

        result.setBoundaryReady(false);
        result.setSourceTraceReady(false);
        result.setMissingFields(safeMissingFields);
        result.setBlockingReasons(safeBlockingReasons);
        return result;
    }

    private BigDecimal entryMidpoint(MarketStructureBoundaryDTO boundary) {
        return boundary.getEntryLower().add(boundary.getEntryUpper()).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
    }

    private BigDecimal rrSource(MarketStructureBoundaryDTO boundary) {
        if (boundary.getRrRatio() != null) {
            return boundary.getRrRatio();
        }
        return boundary.getTakeProfitTargets().stream()
                .map(MarketStructureTakeProfitTargetDTO::getRr)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String rrRuleRef(MarketStructureBoundaryDTO boundary) {
        return boundary.getTakeProfitTargets().stream()
                .map(MarketStructureTakeProfitTargetDTO::getRuleRef)
                .filter(this::hasText)
                .findFirst()
                .orElse(DEFAULT_RR_RULE);
    }

    private String aggregateSourceRef(MarketStructureBoundaryDTO boundary) {
        List<String> refs = canonicalEvidenceRefs(boundary);
        if (refs.isEmpty()) {
            return null;
        }
        return String.join("|", refs);
    }

    private List<String> canonicalEvidenceRefs(MarketStructureBoundaryDTO boundary) {
        Set<String> refs = new LinkedHashSet<>();
        if (boundary == null) {
            return List.of();
        }
        addIfText(refs, boundary.getEntrySourceRef());
        addIfText(refs, boundary.getStopSourceRef());
        if (boundary.getTakeProfitTargets() != null) {
            boundary.getTakeProfitTargets().stream()
                    .filter(Objects::nonNull)
                    .map(MarketStructureTakeProfitTargetDTO::getSourceRef)
                    .filter(this::hasText)
                    .forEach(refs::add);
        }
        return List.copyOf(refs);
    }

    private String aggregateTpSourceType(List<BoundaryTakeProfitLevelDTO> takeProfitLevels) {
        Set<String> sourceTypes = new LinkedHashSet<>();
        takeProfitLevels.stream()
                .map(BoundaryTakeProfitLevelDTO::getSource)
                .filter(this::hasText)
                .forEach(sourceTypes::add);
        if (sourceTypes.size() == 1) {
            return sourceTypes.iterator().next();
        }
        return MIXED_TP_SOURCE_TYPE;
    }

    private String aggregateTpSourceRef(List<BoundaryTakeProfitLevelDTO> takeProfitLevels) {
        return takeProfitLevels.stream()
                .map(BoundaryTakeProfitLevelDTO::getSourceRef)
                .filter(this::hasText)
                .distinct()
                .reduce((left, right) -> left + "|" + right)
                .orElse(null);
    }

    private String aggregateTpReason(List<BoundaryTakeProfitLevelDTO> takeProfitLevels) {
        if (takeProfitLevels.size() == 1) {
            return takeProfitLevels.get(0).getReason();
        }
        return "market structure target set: " + takeProfitLevels.size() + " levels";
    }

    private Direction normalizeDirection(String direction) {
        if (!hasText(direction)) {
            return Direction.UNKNOWN;
        }
        String normalized = direction.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("LONG") || normalized.contains("BULLISH") || normalized.contains("做多")) {
            return Direction.LONG;
        }
        if (normalized.contains("SHORT") || normalized.contains("BEARISH") || normalized.contains("做空")) {
            return Direction.SHORT;
        }
        return Direction.UNKNOWN;
    }

    private boolean isRrLadder(MarketStructureTakeProfitTargetDTO target) {
        return hasText(target.getTargetType()) && "RR_LADDER".equalsIgnoreCase(target.getTargetType().trim());
    }

    private boolean isUnsafeStatus(String status) {
        if (!hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("STALE")
                || normalized.contains("UNSAFE")
                || normalized.contains("CONFLICT")
                || normalized.contains("BLOCKED")
                || normalized.contains("FAIL")
                || normalized.contains("INVALID")
                || normalized.contains("ERROR");
    }

    private void addWhenNull(Object value, String fieldName, List<String> missingFields) {
        if (value == null) {
            missingFields.add(fieldName);
        }
    }

    private void addWhenBlank(String value, String fieldName, List<String> missingFields) {
        if (!hasText(value)) {
            missingFields.add(fieldName);
        }
    }

    private void addIfText(Set<String> refs, String value) {
        if (hasText(value)) {
            refs.add(value.trim());
        }
    }

    private List<String> copyWithoutBlank(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(this::hasText)
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private enum Direction {
        LONG,
        SHORT,
        UNKNOWN
    }
}
