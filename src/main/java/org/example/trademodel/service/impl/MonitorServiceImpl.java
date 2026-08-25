package org.example.trademodel.service.impl;

import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.RuntimeMetricService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class MonitorServiceImpl implements MonitorService {

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
    public List<MonitorAlertDO> getRecentAlertsForUser(Long userId, int limit) {
        long methodStart = System.currentTimeMillis();
        if (userId == null || userId <= 0 || limit <= 0) {
            return Collections.emptyList();
        }
        List<MonitorAlertDO> result = monitorAlertMapper.selectRecentForUser(userId, limit);
        runtimeMetricService.recordDuration(
                "monitor.getRecentAlertsForUser", System.currentTimeMillis() - methodStart);
        return result;
    }
}
