package org.example.trademodel.service;

import org.example.trademodel.entity.MonitorAlertDO;

import java.util.List;

public interface MonitorService {
    List<MonitorAlertDO> getRecentAlerts(int limit);

    List<MonitorAlertDO> getRecentAlertsForUser(Long userId, int limit);
}
