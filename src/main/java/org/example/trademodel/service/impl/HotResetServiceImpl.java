package org.example.trademodel.service.impl;

import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.service.DecisionContext;
import org.example.trademodel.service.HotResetService;
import org.springframework.stereotype.Service;

@Service
public class HotResetServiceImpl implements HotResetService {

    /**
     * 在当前 {@link org.example.trademodel.service.impl.ConfusedStateServiceImpl} 加权下，困惑分上界约 48，
     * 若与「进入 CONFUSED(70)」同一阈值则与 MTF 不对齐组合几乎永不触发。本轮 Hot Reset 取可触发的最小高分位。
     */
    /** Hot Reset 异常清理触发阈值，不是 confused 真值阈值。 */
    static final int hotResetTriggerThreshold = 40;

    @Override
    public boolean shouldTriggerHotReset(int confusedScore, boolean multiTimeframeAligned) {
        return confusedScore >= hotResetTriggerThreshold && !multiTimeframeAligned;
    }

    @Override
    public DecisionResult executeHotReset(DecisionContext context, DecisionResult currentResult) {
        return currentResult;
    }
}
