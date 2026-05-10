package org.example.trademodel.service;

import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.vo.AssetEventTimelineItemVO;

import java.util.List;

public interface MonitorService {
    List<MonitorAlertDO> getRecentAlerts(int limit);

    /**
     * 当前 analysis 下的监控告警时间线（仅 tm_monitor_alert）；无事件返回空列表。
     */
    List<AssetEventTimelineItemVO> listAssetEventTimelineByAnalysisId(String analysisId, int limit);
}
