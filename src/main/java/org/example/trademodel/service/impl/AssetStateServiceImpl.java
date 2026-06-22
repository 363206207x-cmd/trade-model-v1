package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.service.AssetStateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 快照 JSON 仍由本 run 合成；权威行写入 tm_asset_state。
 */
@Service
public class AssetStateServiceImpl implements AssetStateService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AssetStateMapper assetStateMapper;
    private final HotResetEventMapper hotResetEventMapper;

    public AssetStateServiceImpl(AssetStateMapper assetStateMapper, HotResetEventMapper hotResetEventMapper) {
        this.assetStateMapper = assetStateMapper;
        this.hotResetEventMapper = hotResetEventMapper;
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
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        AssetStateDO row = new AssetStateDO();
        row.setSymbol(symbol.trim());
        row.setState(state);
        row.setConfusedScore(confusedScore);
        row.setConfusedLowStreak(Math.max(0, confusedLowStreak));
        row.setLastUpdateTime(LocalDateTime.now());
        row.setTraceId(traceId);
        assetStateMapper.mergeUpsertCore(row);
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
        AssetStateDO existing = assetStateMapper.selectBySymbol(sym);
        if (existing == null) {
            AssetStateDO seed = new AssetStateDO();
            seed.setSymbol(sym);
            seed.setState(postState != null ? postState : AssetStateEnum.OBSERVING);
            seed.setConfusedScore(0);
            seed.setConfusedLowStreak(0);
            seed.setLastUpdateTime(LocalDateTime.now());
            seed.setTraceId(null);
            assetStateMapper.mergeUpsertCore(seed);
        }
        AssetStateDO hot = new AssetStateDO();
        hot.setSymbol(sym);
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
}
