package org.example.trademodel.service.impl;

import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.vo.AssetEventTimelineItemVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MonitorServiceImpl implements MonitorService {

    private static final int ASSET_EVENT_TIMELINE_MAX_LIMIT = 5;
    private static final String EVENT_TYPE_MONITOR_ALERT = "MONITOR_ALERT";
    private static final String SOURCE_TYPE_TM_MONITOR_ALERT = "TM_MONITOR_ALERT";

    private final MonitorAlertMapper monitorAlertMapper;
    private final RuntimeMetricService runtimeMetricService;

    public MonitorServiceImpl(MonitorAlertMapper monitorAlertMapper,
                              RuntimeMetricService runtimeMetricService) {
        this.monitorAlertMapper = monitorAlertMapper;
        this.runtimeMetricService = runtimeMetricService;
    }

    @Override
    public List<MonitorAlertDO> getRecentAlerts(int limit) {
        long methodStart = System.currentTimeMillis();
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<MonitorAlertDO> result = monitorAlertMapper.selectRecent(limit);
        long methodCostMs = System.currentTimeMillis() - methodStart;
        System.out.println("[PERF] service_get_recent_alerts=" + methodCostMs + " ms");
        runtimeMetricService.recordDuration("monitor.getRecentAlerts", methodCostMs);
        return result;
    }

    @Override
    public List<AssetEventTimelineItemVO> listAssetEventTimelineByAnalysisId(String analysisId, int limit) {
        if (analysisId == null || analysisId.isBlank()) {
            return Collections.emptyList();
        }
        if (limit <= 0) {
            return Collections.emptyList();
        }
        int safeLimit = Math.min(limit, ASSET_EVENT_TIMELINE_MAX_LIMIT);
        List<MonitorAlertDO> rows = monitorAlertMapper.selectRecentByAnalysisId(analysisId.trim(), safeLimit);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<AssetEventTimelineItemVO> out = new ArrayList<>(rows.size());
        for (MonitorAlertDO row : rows) {
            out.add(toAssetEventItem(row));
        }
        return out;
    }

    private static AssetEventTimelineItemVO toAssetEventItem(MonitorAlertDO row) {
        AssetEventTimelineItemVO vo = new AssetEventTimelineItemVO();
        vo.setEventType(EVENT_TYPE_MONITOR_ALERT);
        vo.setEventLevel(row.getAlertLevel());
        vo.setEventTitle(row.getAlertType());
        vo.setEventMessage(row.getAlertMessage());
        vo.setSourceType(SOURCE_TYPE_TM_MONITOR_ALERT);
        vo.setSourceId(row.getId());
        vo.setSymbol(row.getAssetSymbol());
        vo.setAnalysisId(row.getAnalysisId());
        vo.setOccurredAt(row.getCreatedAt());
        vo.setStatus(row.getStatus());
        vo.setTraceId(row.getTraceId());
        vo.setRelatedUrl(null);
        return vo;
    }
}
