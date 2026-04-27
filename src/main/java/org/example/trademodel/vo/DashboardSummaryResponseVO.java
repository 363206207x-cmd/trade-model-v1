package org.example.trademodel.vo;

import org.example.trademodel.entity.MonitorAlertDO;

import java.util.List;
import java.util.Map;

public class DashboardSummaryResponseVO {
    private LightSystemStatusVO systemStatus;
    private int openPositionCount;
    private Map<String, Object> systemHealth;
    private List<MonitorAlertDO> alerts;
    private List<DecisionResultVO> decisions;

    public LightSystemStatusVO getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(LightSystemStatusVO systemStatus) {
        this.systemStatus = systemStatus;
    }

    public int getOpenPositionCount() {
        return openPositionCount;
    }

    public void setOpenPositionCount(int openPositionCount) {
        this.openPositionCount = openPositionCount;
    }

    public Map<String, Object> getSystemHealth() {
        return systemHealth;
    }

    public void setSystemHealth(Map<String, Object> systemHealth) {
        this.systemHealth = systemHealth;
    }

    public List<MonitorAlertDO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<MonitorAlertDO> alerts) {
        this.alerts = alerts;
    }

    public List<DecisionResultVO> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<DecisionResultVO> decisions) {
        this.decisions = decisions;
    }
}
