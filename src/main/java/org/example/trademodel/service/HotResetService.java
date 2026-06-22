package org.example.trademodel.service;

import org.example.trademodel.entity.DecisionResult;

/**
 * Hot Reset：最小规则在 {@link #shouldTriggerHotReset(int, boolean)}；主链在 assemble 落库路径触发并写库。
 */
public interface HotResetService {

    /**
     * 最小规则：困惑分达到阈值且多周期未对齐时触发（阈值由实现类定义，当前为 40）。
     */
    boolean shouldTriggerHotReset(int confusedScore, boolean multiTimeframeAligned);

    boolean shouldTriggerHotReset(HotResetCommand command);

    HotResetResult evaluateAndExecute(HotResetCommand command);

    /** 预留：当前主链仅写 tm_asset_state，不改写已落库的 {@link DecisionResult}。 */
    DecisionResult executeHotReset(DecisionContext context, DecisionResult currentResult);
}
