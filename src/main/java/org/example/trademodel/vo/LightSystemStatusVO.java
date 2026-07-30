package org.example.trademodel.vo;

import java.time.LocalDateTime;

public class LightSystemStatusVO {

    private String status;
    private Integer monitoredCoins;
    private LocalDateTime lastDecisionTime;
    private Integer totalDecisionsToday;

    /** 今日错失有效机会条数：按 tm_missed_opportunity.biz_date = 当日（与 JVM 默认时区的「今天」一致）。 */
    private Integer missedValidOpportunityCount;

    /**
     * 当前方向结论被阻断的 symbol 个数：tm_asset_state 全库当前态中 confused_score 达到
     * {@code ConfusedStatePolicy.DIRECTIONAL_PUSH_BLOCK_THRESHOLD} 的行数（每 symbol 一行）。
     * 不是「当日困惑决策条数」，也不包含仅大于 0 但未达到方向阻断阈值的行。
     */
    private Integer confusedCount;

    /**
     * 反转信号数量：当前仍有 OPEN 持仓的 distinct symbol 中，该 symbol 在 tm_decision_result 最新一条决策的
     * {@code market_bias_hierarchy}（规范化后）与持仓 {@code position_side} 方向相反的数量。
     * <p>
     * 反向判定：LONG + 任一偏空级别、SHORT + 任一偏多级别；强/中/弱方向使用同一方向族语义。
     * 不计入：无 OPEN 持仓、决策方向缺失、RANGE/WAIT/未知方向、持仓 side 缺失或非 LONG/SHORT。
     * 计数按 symbol 去重（同一 symbol 多条 OPEN 仅计一次）；比较使用每 symbol 最新决策，create_time 并列时以 decision_id 决胜。
     */
    private Integer reverseSignalCount;

    /** 最近一次 Hot Reset（全库按 hot_reset_time 聚合）；无事件时为 false。 */
    private Boolean hotResetFired;
    /** 触发该次 Hot Reset 的 symbol（最近一次事件所在行） */
    private String hotResetSymbol;
    private String hotResetTriggerType;
    private String hotResetTriggerValue;
    private LocalDateTime hotResetTime;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMonitoredCoins() {
        return monitoredCoins;
    }

    public void setMonitoredCoins(Integer monitoredCoins) {
        this.monitoredCoins = monitoredCoins;
    }

    public LocalDateTime getLastDecisionTime() {
        return lastDecisionTime;
    }

    public void setLastDecisionTime(LocalDateTime lastDecisionTime) {
        this.lastDecisionTime = lastDecisionTime;
    }

    public Integer getTotalDecisionsToday() {
        return totalDecisionsToday;
    }

    public void setTotalDecisionsToday(Integer totalDecisionsToday) {
        this.totalDecisionsToday = totalDecisionsToday;
    }

    public Integer getMissedValidOpportunityCount() {
        return missedValidOpportunityCount;
    }

    public void setMissedValidOpportunityCount(Integer missedValidOpportunityCount) {
        this.missedValidOpportunityCount = missedValidOpportunityCount;
    }

    public Integer getConfusedCount() {
        return confusedCount;
    }

    public void setConfusedCount(Integer confusedCount) {
        this.confusedCount = confusedCount;
    }

    public Integer getReverseSignalCount() {
        return reverseSignalCount;
    }

    public void setReverseSignalCount(Integer reverseSignalCount) {
        this.reverseSignalCount = reverseSignalCount;
    }

    public Boolean getHotResetFired() {
        return hotResetFired;
    }

    public void setHotResetFired(Boolean hotResetFired) {
        this.hotResetFired = hotResetFired;
    }

    public String getHotResetSymbol() {
        return hotResetSymbol;
    }

    public void setHotResetSymbol(String hotResetSymbol) {
        this.hotResetSymbol = hotResetSymbol;
    }

    public String getHotResetTriggerType() {
        return hotResetTriggerType;
    }

    public void setHotResetTriggerType(String hotResetTriggerType) {
        this.hotResetTriggerType = hotResetTriggerType;
    }

    public String getHotResetTriggerValue() {
        return hotResetTriggerValue;
    }

    public void setHotResetTriggerValue(String hotResetTriggerValue) {
        this.hotResetTriggerValue = hotResetTriggerValue;
    }

    public LocalDateTime getHotResetTime() {
        return hotResetTime;
    }

    public void setHotResetTime(LocalDateTime hotResetTime) {
        this.hotResetTime = hotResetTime;
    }
}
