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

    default void persistAuthoritativeState(String symbol, String timeframe, AssetStateEnum state,
                                           int confusedScore, int confusedLowStreak, String traceId) {
        transition(symbol, timeframe, state, confusedScore, confusedLowStreak, null, traceId,
                "LEGACY_AUTHORITATIVE_STATE", OpportunityTriggerSource.LEGACY_ANALYSIS);
    }

    default OpportunityTransitionResult transition(String symbol, AssetStateEnum requestedState, int confusedScore,
                                                    int confusedLowStreak, String analysisId, String traceId,
                                                    String reason, OpportunityTriggerSource triggerSource) {
        return transition(symbol, "GLOBAL", requestedState, confusedScore, confusedLowStreak,
                analysisId, traceId, reason, triggerSource);
    }

    OpportunityTransitionResult transition(String symbol, String timeframe, AssetStateEnum requestedState,
                                           int confusedScore, int confusedLowStreak, String analysisId,
                                           String traceId, String reason,
                                           OpportunityTriggerSource triggerSource);

    OpportunityTransitionResult transition(OpportunityStateIdentity identity,
                                            AssetStateEnum requestedState,
                                            int confusedScore,
                                            int confusedLowStreak,
                                            String analysisId,
                                            String traceId,
                                            String ruleVersion,
                                            String reason,
                                            OpportunityTriggerSource triggerSource);

    void recordOpportunityProjection(OpportunityStateIdentity identity,
                                     Long poolItemId,
                                     String analysisId,
                                     String traceId,
                                     String ruleVersion,
                                     Integer opportunityScore,
                                     String confidence,
                                     String risk,
                                     String extJson);

    /**
     * Atomically claims one due lightweight Asset Pool scan using the existing
     * authoritative AssetState row. The claim changes no opportunity state and
     * therefore cannot promote an asset or grant execution permission.
     */
    ScheduledScanClaim claimScheduledScan(OpportunityStateIdentity identity,
                                           Long poolItemId,
                                           LocalDateTime now,
                                           long intervalSeconds,
                                           String traceId,
                                           String ruleVersion);

    /** Completes the scan audit on the same claimed AssetState row. */
    boolean completeScheduledScan(ScheduledScanClaim claim,
                                  LocalDateTime finishedAt,
                                  String result,
                                  String failureReason,
                                  String dataFreshness,
                                  String structureSignature,
                                  Long latestCloseTimeMs,
                                  String analysisTraceId,
                                  boolean fullAnalysisRequested,
                                  boolean fullAnalysisSucceeded);

    record ScheduledScanClaim(OpportunityStateIdentity identity,
                              String opportunityId,
                              AssetStateEnum state,
                              String lastAnalysisId,
                              String risk,
                              LocalDateTime hotResetTime,
                              String traceId,
                              String ruleVersion,
                              LocalDateTime scheduledAt,
                              LocalDateTime startedAt,
                              LocalDateTime nextEligibleScanAt,
                              String previousExtJson) {
    }

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
