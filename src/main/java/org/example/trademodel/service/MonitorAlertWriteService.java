package org.example.trademodel.service;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;

/**
 * 分析主链落库后的最小告警写入（规则集保持精简，由实现类版本化）。
 */
public interface MonitorAlertWriteService {

    /**
     * 在 tm_analysis_run 已成功插入且同一事务内，根据本 run 的决策与数据质量写入 tm_monitor_alert。
     */
    void emitAfterAnalysisPersist(AnalysisRunDO run, AssetAnalysisVO analysis, DecisionBundleVO decision);
}
