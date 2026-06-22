package org.example.trademodel.service;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;

import java.time.LocalDateTime;

/**
 * 资产状态：tm_decision_result 上的快照 JSON + tm_asset_state 权威行。
 */
public interface AssetStateService {

    /**
     * 生成写入 tm_decision_result.asset_state_snapshot 的短 JSON（≤512），来源见实现类注释。
     */
    String buildSnapshotAtDecision(String symbol, String analysisId, AssetStateEnum state, int confusedScore,
                                     boolean multiTimeframeAligned);

    String buildSnapshotAtDecision(String symbol, String analysisId, AssetStateEnum previousState,
                                   AssetStateEnum nextState, int confusedScore, int confusedLowStreak,
                                   boolean directionalPushBlocked, boolean multiTimeframeAligned);

    /**
     * 一次完整分析后写入/合并 {@code tm_asset_state}（仅认 {@link AssetStateEnum}），与 tm_analysis_run.trace_id 对齐。
     */
    void persistAuthoritativeState(String symbol, AssetStateEnum state, int confusedScore, String traceId);

    void persistAuthoritativeState(String symbol, AssetStateEnum state, int confusedScore,
                                   int confusedLowStreak, String traceId);

    /**
     * 全库维度「最近一次 Hot Reset」行（按 hot_reset_time 最大），供 systemStatus 展示；无则 null。
     */
    AssetStateDO findLatestHotResetSnapshot();

    /**
     * 写入一次 Hot Reset 事件到 {@code tm_asset_state}（最小可测；复杂触发规则后续再接）。
     * 若尚无该 symbol 的权威行，会先插入仅含 core 占位再写 hot 列。
     */
    void recordHotResetEvent(String analysisId, String traceId, String symbol, String triggerType, String triggerValue,
                             String decisionId, AssetStateEnum decisionState, int confusedScoreSnapshot,
                             boolean multiTimeframeAlignedSnapshot, String triggerReasonCode, String triggerReasonText,
                             int eventVersion, LocalDateTime occurredAt, AssetStateEnum preState, AssetStateEnum postState);
}
