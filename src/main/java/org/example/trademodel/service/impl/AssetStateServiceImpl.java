package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.entity.OpportunityStateTransitionDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.OpportunityStateTransitionMapper;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.OpportunityTriggerSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * 快照 JSON 仍由本 run 合成；权威行写入 tm_asset_state。
 */
@Service
public class AssetStateServiceImpl implements AssetStateService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration ORDINARY_DEBOUNCE = Duration.ofSeconds(30);
    private static final Duration DEFAULT_COOLING = Duration.ofMinutes(15);

    private final AssetStateMapper assetStateMapper;
    private final HotResetEventMapper hotResetEventMapper;
    private final OpportunityStateTransitionMapper transitionMapper;

    public AssetStateServiceImpl(AssetStateMapper assetStateMapper,
                                 HotResetEventMapper hotResetEventMapper) {
        this(assetStateMapper, hotResetEventMapper, null);
    }

    @Autowired
    public AssetStateServiceImpl(AssetStateMapper assetStateMapper,
                                 HotResetEventMapper hotResetEventMapper,
                                 OpportunityStateTransitionMapper transitionMapper) {
        this.assetStateMapper = assetStateMapper;
        this.hotResetEventMapper = hotResetEventMapper;
        this.transitionMapper = transitionMapper;
    }

    @Override
    public String buildSnapshotAtDecision(String symbol, String analysisId, AssetStateEnum state, int confusedScore,
                                          boolean multiTimeframeAligned) {
        return buildSnapshotAtDecision(symbol, analysisId, null, state, confusedScore, 0,
                false, multiTimeframeAligned);
    }

    @Override
    public String buildSnapshotAtDecision(String symbol, String analysisId, AssetStateEnum previousState,
                                          AssetStateEnum nextState, int confusedScore, int confusedLowStreak,
                                          boolean directionalPushBlocked, boolean multiTimeframeAligned) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("source", "synthetic_at_decision");
        n.put("symbol", symbol != null ? symbol : "");
        n.put("analysisId", analysisId != null ? analysisId : "");
        n.put("previousState", previousState != null ? previousState.name() : "");
        n.put("nextState", nextState != null ? nextState.name() : "");
        n.put("state", nextState != null ? nextState.name() : "");
        n.put("confusedScore", confusedScore);
        n.put("confusedLowStreak", Math.max(0, confusedLowStreak));
        n.put("directionalPushBlocked", directionalPushBlocked);
        n.put("multiTimeframeAligned", multiTimeframeAligned);
        String json = n.toString();
        if (json.length() > 512) {
            return json.substring(0, 509) + "...";
        }
        return json;
    }

    @Override
    public void persistAuthoritativeState(String symbol, AssetStateEnum state, int confusedScore, String traceId) {
        persistAuthoritativeState(symbol, state, confusedScore, 0, traceId);
    }

    @Override
    public void persistAuthoritativeState(String symbol, AssetStateEnum state, int confusedScore,
                                          int confusedLowStreak, String traceId) {
        transition(symbol, state, confusedScore, confusedLowStreak, null, traceId,
                "LEGACY_AUTHORITATIVE_STATE", OpportunityTriggerSource.LEGACY_ANALYSIS);
    }

    @Override
    @Transactional
    public synchronized OpportunityTransitionResult transition(
            String symbol,
            String timeframe,
            AssetStateEnum requestedState,
            int confusedScore,
            int confusedLowStreak,
            String analysisId,
            String traceId,
            String reason,
            OpportunityTriggerSource triggerSource) {
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedTimeframe = normalizeTimeframe(timeframe);
        AssetStateEnum requested = requestedState == null ? AssetStateEnum.OBSERVING : requestedState;
        OpportunityTriggerSource source = triggerSource == null
                ? OpportunityTriggerSource.ANALYSIS : triggerSource;
        LocalDateTime now = LocalDateTime.now();
        AssetStateDO current = assetStateMapper.selectBySymbolAndTimeframe(normalizedSymbol, normalizedTimeframe);
        AssetStateEnum previous = current == null || current.getState() == null
                ? AssetStateEnum.OBSERVING : current.getState();
        String opportunityId = current != null && hasText(current.getOpportunityId())
                ? current.getOpportunityId() : opportunityId(normalizedSymbol, normalizedTimeframe);

        AssetStateEnum target = applyPrecedence(previous, requested, current, source, now);
        boolean debounced = shouldDebounce(current, previous, target, source, now);
        if (debounced) {
            target = previous;
        }
        boolean changed = current == null || previous != target;
        String effectiveReason = hasText(reason) ? reason.trim() : "STATE_EVALUATED";
        if (debounced) {
            effectiveReason = "DEBOUNCED:" + effectiveReason;
        } else if (target != requested) {
            effectiveReason = "PRECEDENCE_PRESERVED:" + effectiveReason;
        }

        AssetStateDO row = new AssetStateDO();
        row.setSymbol(normalizedSymbol);
        row.setTimeframe(normalizedTimeframe);
        row.setState(target);
        row.setConfusedScore(Math.max(0, confusedScore));
        row.setConfusedLowStreak(Math.max(0, confusedLowStreak));
        row.setOpportunityId(opportunityId);
        row.setStateEnteredAt(changed || current.getStateEnteredAt() == null
                ? now : current.getStateEnteredAt());
        row.setCoolingUntil(coolingUntil(current, previous, target, now));
        row.setLastTransitionReason(effectiveReason);
        row.setLastTriggerSource(source.name());
        row.setLastAnalysisId(trimToNull(analysisId));
        row.setLastUpdateTime(now);
        row.setTraceId(requireTraceId(traceId));
        assetStateMapper.mergeUpsertCore(row);

        if (changed || debounced || target != requested) {
            OpportunityStateTransitionDO audit = new OpportunityStateTransitionDO();
            audit.setTransitionId("opp-transition-" + UUID.randomUUID());
            audit.setOpportunityId(opportunityId);
            audit.setSymbol(normalizedSymbol);
            audit.setTimeframe(normalizedTimeframe);
            audit.setAnalysisId(trimToNull(analysisId));
            audit.setTraceId(row.getTraceId());
            audit.setFromState(current == null ? null : previous.name());
            audit.setToState(target.name());
            audit.setReason(effectiveReason);
            audit.setTriggerSource(source.name());
            audit.setTransitionPriority(effectivePriority(requested, source));
            audit.setSuppressed(!changed);
            audit.setOccurredAt(now);
            if (transitionMapper != null) {
                transitionMapper.insert(audit);
            }
        }
        return new OpportunityTransitionResult(
                opportunityId,
                normalizedSymbol,
                previous,
                target,
                changed,
                !changed && (debounced || target != requested),
                effectiveReason,
                source.name(),
                executionPermission(target),
                now);
    }

    @Override
    public AssetStateDO findLatestHotResetSnapshot() {
        return assetStateMapper.selectLatestHotResetRow();
    }

    @Override
    public void recordHotResetEvent(String analysisId, String traceId, String symbol, String triggerType, String triggerValue,
                                    String decisionId, AssetStateEnum decisionState, int confusedScoreSnapshot,
                                    boolean multiTimeframeAlignedSnapshot, String triggerReasonCode, String triggerReasonText,
                                    int eventVersion, LocalDateTime occurredAt, AssetStateEnum preState, AssetStateEnum postState) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        String sym = symbol.trim();
        LocalDateTime at = occurredAt != null ? occurredAt : LocalDateTime.now();
        transition(sym, postState != null ? postState : AssetStateEnum.OBSERVING,
                confusedScoreSnapshot, 0, analysisId, traceId,
                hasText(triggerReasonCode) ? triggerReasonCode : "HOT_RESET",
                OpportunityTriggerSource.HOT_RESET);
        AssetStateDO hot = new AssetStateDO();
        hot.setSymbol(sym);
        hot.setTimeframe("global");
        hot.setHotResetFlag(true);
        hot.setHotResetTriggerType(triggerType);
        hot.setHotResetTriggerValue(triggerValue);
        hot.setHotResetTime(at);
        hot.setPreResetState(preState != null ? preState.name() : null);
        hot.setPostResetState(postState != null ? postState.name() : null);
        hot.setLastUpdateTime(LocalDateTime.now());
        assetStateMapper.updateHotResetColumns(hot);

        if (analysisId == null || analysisId.isBlank()) {
            return;
        }
        HotResetEventDO event = new HotResetEventDO();
        String eventId = "hre-" + UUID.randomUUID().toString().substring(0, 12);
        event.setEventId(eventId);
        event.setEventKey("legacy-" + eventId);
        event.setAnalysisId(analysisId);
        event.setTraceId(traceId);
        event.setSymbol(sym);
        event.setTimeframe(null);
        event.setTriggerType(triggerType);
        event.setTriggerValue(triggerValue);
        event.setSourceType("LEGACY_ASSET_STATE_SERVICE");
        event.setSourceReference("AssetStateService.recordHotResetEvent");
        event.setDecisionId(decisionId);
        event.setDecisionState(decisionState != null ? decisionState.name() : null);
        event.setDecisionInvalidatedCount(0);
        event.setPlanRevalidationCount(0);
        event.setPushInvalidatedCount(0);
        event.setConfusedScoreSnapshot(confusedScoreSnapshot);
        event.setConfusedScoreBefore(confusedScoreSnapshot);
        event.setConfusedScoreAfter(confusedScoreSnapshot);
        event.setMultiTimeframeAlignedSnapshot(multiTimeframeAlignedSnapshot);
        event.setRebuildTriggered(false);
        event.setExecutionStatus("COMPLETED");
        event.setTriggerReasonCode(triggerReasonCode);
        event.setTriggerReasonText(triggerReasonText);
        event.setEventVersion(eventVersion);
        event.setEventTime(at);
        event.setPreState(preState != null ? preState.name() : null);
        event.setPostState(postState != null ? postState.name() : null);
        event.setCompletedAt(LocalDateTime.now());
        event.setCreateTime(LocalDateTime.now());
        hotResetEventMapper.insert(event);
    }

    private static AssetStateEnum applyPrecedence(AssetStateEnum current,
                                                  AssetStateEnum requested,
                                                  AssetStateDO row,
                                                  OpportunityTriggerSource source,
                                                  LocalDateTime now) {
        if (source == OpportunityTriggerSource.HOT_RESET) {
            return requested;
        }
        if (requested == AssetStateEnum.CONFUSED) {
            return requested;
        }
        if (current == AssetStateEnum.CONFUSED && requested != AssetStateEnum.COOLING) {
            return current;
        }
        if (requested == AssetStateEnum.INVALIDATED) {
            return requested;
        }
        if (current == AssetStateEnum.INVALIDATED && requested != AssetStateEnum.COOLING) {
            return current;
        }
        if (current == AssetStateEnum.COOLING && row != null && row.getCoolingUntil() != null
                && now.isBefore(row.getCoolingUntil()) && isCoolingPromotion(requested)) {
            return current;
        }
        return requested;
    }

    private static boolean isCoolingPromotion(AssetStateEnum requested) {
        return requested == AssetStateEnum.CANDIDATE
                || requested == AssetStateEnum.WAITING_TRIGGER
                || requested == AssetStateEnum.TRIGGERED;
    }

    private static LocalDateTime coolingUntil(AssetStateDO current,
                                              AssetStateEnum previous,
                                              AssetStateEnum target,
                                              LocalDateTime now) {
        if (target != AssetStateEnum.COOLING) return null;
        if (previous == AssetStateEnum.COOLING && current != null && current.getCoolingUntil() != null) {
            return current.getCoolingUntil();
        }
        return now.plus(DEFAULT_COOLING);
    }

    private static boolean shouldDebounce(AssetStateDO current,
                                          AssetStateEnum previous,
                                          AssetStateEnum target,
                                          OpportunityTriggerSource source,
                                          LocalDateTime now) {
        if (current == null || previous == target || source.priority() >= OpportunityTriggerSource.INVALIDATION.priority()) {
            return false;
        }
        if (target == AssetStateEnum.COOLING
                && (previous == AssetStateEnum.INVALIDATED || previous == AssetStateEnum.CONFUSED)) {
            return false;
        }
        LocalDateTime lastTransition = current.getStateEnteredAt() != null
                ? current.getStateEnteredAt()
                : current.getLastUpdateTime();
        return lastTransition != null
                && Duration.between(lastTransition, now).compareTo(ORDINARY_DEBOUNCE) < 0;
    }

    private static int effectivePriority(AssetStateEnum requested, OpportunityTriggerSource source) {
        if (source == OpportunityTriggerSource.HOT_RESET) return OpportunityTriggerSource.HOT_RESET.priority();
        if (requested == AssetStateEnum.CONFUSED) return OpportunityTriggerSource.CONFUSED.priority();
        if (requested == AssetStateEnum.INVALIDATED) return OpportunityTriggerSource.INVALIDATION.priority();
        return source.priority();
    }

    private static String executionPermission(AssetStateEnum state) {
        if (state == AssetStateEnum.CONFUSED) return "BLOCKED";
        if (state == AssetStateEnum.INVALIDATED || state == AssetStateEnum.COOLING) return "NOT_ELIGIBLE";
        return "ADVISORY_ALLOWED";
    }

    private static String normalizeSymbol(String symbol) {
        if (!hasText(symbol)) throw new IllegalArgumentException("symbol is required");
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String opportunityId(String symbol, String timeframe) {
        String compact = symbol.replaceAll("[^A-Z0-9]", "").toLowerCase(Locale.ROOT);
        String compactTimeframe = timeframe.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return "opp-" + compact + "-" + compactTimeframe;
    }

    private static String normalizeTimeframe(String timeframe) {
        return hasText(timeframe) ? timeframe.trim().toLowerCase(Locale.ROOT) : "global";
    }

    private static String requireTraceId(String traceId) {
        return hasText(traceId) ? traceId.trim() : "trace-" + UUID.randomUUID();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
