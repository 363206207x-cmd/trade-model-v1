package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextSnapshot;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class DashboardHomeServiceImpl implements DashboardHomeService {
    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 12;
    private static final List<String> DEFAULT_SYMBOLS = List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT"
    );
    private static final List<String> AI_ROLES = List.of("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");

    private final DecisionService decisionService;
    private final MonitorService monitorService;
    private final UserPositionService userPositionService;
    private final PositionSyncService positionSyncService;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final ExternalContextEvidenceBuilder externalContextEvidenceBuilder;
    private final ObjectMapper objectMapper;

    public DashboardHomeServiceImpl(DecisionService decisionService,
                                    MonitorService monitorService,
                                    UserPositionService userPositionService,
                                    PositionSyncService positionSyncService,
                                    PushSnapshotMapper pushSnapshotMapper,
                                    ExternalContextEvidenceBuilder externalContextEvidenceBuilder,
                                    ObjectMapper objectMapper) {
        this.decisionService = decisionService;
        this.monitorService = monitorService;
        this.userPositionService = userPositionService;
        this.positionSyncService = positionSyncService;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.externalContextEvidenceBuilder = externalContextEvidenceBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardHomeVO getHome(String selectedSymbol, Integer limit) {
        int effectiveLimit = normalizeLimit(limit);
        LightSystemStatusVO systemStatus = safeSystemStatus();
        List<DecisionResultVO> decisions = safeDecisions(Math.max(effectiveLimit, DEFAULT_LIMIT));
        List<MonitorAlertDO> alerts = safeAlerts();
        List<UserPositionVO> positions = safePositions();
        PositionSyncStatusVO positionSyncStatus = safePositionSyncStatus();

        String normalizedSelected = normalizeSymbol(selectedSymbol);
        if (normalizedSelected == null) {
            normalizedSelected = firstDecisionSymbol(decisions);
        }
        if (normalizedSelected == null) {
            normalizedSelected = DEFAULT_SYMBOLS.get(0);
        }

        DecisionResultVO selectedDecision = findDecision(decisions, normalizedSelected);
        if (selectedDecision == null) {
            selectedDecision = safeDecisionBySymbol(normalizedSelected);
        }

        ExternalContextSnapshot externalContext = safeExternalContext(normalizedSelected, selectedDecision);
        PushInboxContext pushInboxContext = buildPushInbox(positions, effectiveLimit);

        DashboardHomeVO home = new DashboardHomeVO();
        home.setHeader(buildHeader(systemStatus, decisions, positionSyncStatus, externalContext));
        home.setSystemState(buildSystemState(systemStatus, decisions, selectedDecision));
        home.setAlerts(buildAlerts(alerts));
        home.setEvents(buildEvents(externalContext));
        home.setAssets(buildAssets(decisions, effectiveLimit));
        home.setPositions(buildPositions(positions));
        home.setSelectedSymbol(normalizedSelected);
        home.setExecutionSuggestion(buildExecutionSuggestion(selectedDecision));
        home.setAiDecision(buildAiDecision(selectedDecision));
        home.setPushInbox(pushInboxContext.pushInbox());
        home.setDiagnostics(buildDiagnostics(systemStatus, decisions, selectedDecision, positionSyncStatus, pushInboxContext));
        home.setSafety(new DashboardHomeVO.SafetyVO());
        return home;
    }

    private DashboardHomeVO.HeaderVO buildHeader(LightSystemStatusVO systemStatus,
                                                 List<DecisionResultVO> decisions,
                                                 PositionSyncStatusVO positionSyncStatus,
                                                 ExternalContextSnapshot externalContext) {
        DashboardHomeVO.HeaderVO header = new DashboardHomeVO.HeaderVO();
        header.setPageTitle("首页总览");
        header.setDataStatus(firstNonBlank(systemStatus != null ? systemStatus.getStatus() : null, "WAITING_SYNC"));
        header.setAiStatus(decisions == null || decisions.isEmpty() ? "WAITING_SYNC" : "CONNECTED");
        header.setDataSourceText(dataSourceText(positionSyncStatus, externalContext));
        header.setUpdatedAt(LocalDateTime.now());
        return header;
    }

    private DashboardHomeVO.SystemStateVO buildSystemState(LightSystemStatusVO systemStatus,
                                                           List<DecisionResultVO> decisions,
                                                           DecisionResultVO selectedDecision) {
        DashboardHomeVO.SystemStateVO state = new DashboardHomeVO.SystemStateVO();
        DecisionResultVO trendDecision = selectedDecision != null ? selectedDecision : firstDecision(decisions);
        state.setMarketTrend(card(
                "marketTrend",
                "市场趋势",
                trendDecision != null ? trendDecision.getMarketBiasHierarchy() : null,
                biasLabel(trendDecision != null ? trendDecision.getMarketBiasHierarchy() : null),
                "决策摘要",
                statusForText(trendDecision != null ? trendDecision.getMarketBiasHierarchy() : null),
                null
        ));
        state.setRiskLevel(card(
                "riskLevel",
                "风险等级",
                selectedDecision != null ? selectedDecision.getRiskLevel() : riskLevelFrom(decisions),
                riskLabel(selectedDecision != null ? selectedDecision.getRiskLevel() : riskLevelFrom(decisions)),
                "决策风险",
                statusForText(selectedDecision != null ? selectedDecision.getRiskLevel() : riskLevelFrom(decisions)),
                null
        ));
        Integer averageDataQuality = averageDataQuality(decisions);
        state.setDataQuality(card(
                "dataQuality",
                "数据质量分",
                averageDataQuality,
                averageDataQuality != null ? String.valueOf(averageDataQuality) : null,
                "摘要均值",
                averageDataQuality != null ? "CONNECTED" : "WAITING_SYNC",
                averageDataQuality
        ));
        AiConflictSummary conflict = aiConflictSummary(decisions);
        state.setAiConflict(card(
                "aiConflict",
                "AI 冲突等级",
                conflict.level(),
                conflict.level(),
                "AI 冲突",
                conflict.level() != null || conflict.score() != null ? "CONNECTED" : "WAITING_SYNC",
                conflict.score()
        ));
        Integer pendingCount = systemStatus != null ? systemStatus.getPendingCount() : null;
        state.setPendingReview(card(
                "pendingReview",
                "待复核机会",
                pendingCount,
                pendingCount != null && pendingCount > 0 ? String.valueOf(pendingCount) : "暂无",
                "pendingCount",
                pendingCount != null ? "CONNECTED" : "WAITING_SYNC",
                pendingCount
        ));
        Integer confusedCount = confusedCount(systemStatus, decisions);
        state.setConfused(card(
                "confused",
                "Confused",
                confusedCount,
                confusedCount != null ? String.valueOf(confusedCount) : null,
                "当前积压",
                confusedCount != null ? "CONNECTED" : "WAITING_SYNC",
                confusedCount
        ));
        Boolean hotResetFired = systemStatus != null ? systemStatus.getHotResetFired() : null;
        Map<String, Object> hotMeta = new LinkedHashMap<>();
        if (systemStatus != null) {
            hotMeta.put("symbol", systemStatus.getHotResetSymbol());
            hotMeta.put("triggerType", systemStatus.getHotResetTriggerType());
            hotMeta.put("triggerValue", systemStatus.getHotResetTriggerValue());
            hotMeta.put("time", systemStatus.getHotResetTime());
        }
        DashboardHomeVO.StatusCardVO hotReset = card(
                "hotReset",
                "Hot Reset",
                Boolean.TRUE.equals(hotResetFired),
                Boolean.TRUE.equals(hotResetFired) ? "已触发" : "未触发",
                Boolean.TRUE.equals(hotResetFired) ? "最近一次" : "暂无",
                hotResetFired != null ? "CONNECTED" : "WAITING_SYNC",
                null
        );
        hotReset.setMeta(hotMeta);
        state.setHotReset(hotReset);
        return state;
    }

    private List<DashboardHomeVO.AlertRowVO> buildAlerts(List<MonitorAlertDO> alerts) {
        List<DashboardHomeVO.AlertRowVO> rows = new ArrayList<>();
        for (MonitorAlertDO alert : alerts == null ? List.<MonitorAlertDO>of() : alerts) {
            if (rows.size() >= 2) {
                break;
            }
            DashboardHomeVO.AlertRowVO row = new DashboardHomeVO.AlertRowVO();
            row.setLevel(trimToNull(alert.getAlertLevel()));
            row.setMessage(trimToNull(alert.getAlertMessage()));
            row.setSymbol(toDisplaySymbol(alert.getAssetSymbol()));
            row.setTime(trimToNull(alert.getCreatedAt()));
            rows.add(row);
        }
        return rows;
    }

    private List<DashboardHomeVO.EventRowVO> buildEvents(ExternalContextSnapshot externalContext) {
        if (externalContext == null || !hasText(externalContext.getLatestExternalEventLabel())) {
            return List.of();
        }
        DashboardHomeVO.EventRowVO row = new DashboardHomeVO.EventRowVO();
        row.setType("EXTERNAL_CONTEXT");
        row.setLabel(externalContext.getLatestExternalEventLabel());
        row.setImpactLevel(externalContext.getRiskLevel());
        row.setTimeWindow(externalContext.getEventWindowStart() != null || externalContext.getEventWindowEnd() != null
                ? String.valueOf(externalContext.getEventWindowStart()) + " ~ " + externalContext.getEventWindowEnd()
                : externalContext.getLatestExternalEventTime());
        return List.of(row);
    }

    private List<DashboardHomeVO.AssetVO> buildAssets(List<DecisionResultVO> decisions, int limit) {
        List<DashboardHomeVO.AssetVO> assets = new ArrayList<>();
        LinkedHashSet<String> used = new LinkedHashSet<>();
        for (DecisionResultVO decision : decisions == null ? List.<DecisionResultVO>of() : decisions) {
            if (assets.size() >= limit) {
                break;
            }
            String symbol = normalizeSymbol(decision.getSymbol());
            if (symbol == null || !used.add(symbol)) {
                continue;
            }
            assets.add(assetFromDecision(assets.size() + 1, decision));
        }
        for (String symbol : DEFAULT_SYMBOLS) {
            if (assets.size() >= limit) {
                break;
            }
            if (!used.add(symbol)) {
                continue;
            }
            assets.add(assetPlaceholder(assets.size() + 1, symbol));
        }
        return assets;
    }

    private DashboardHomeVO.AssetVO assetFromDecision(int slot, DecisionResultVO decision) {
        DashboardHomeVO.AssetVO asset = assetBase(slot, normalizeSymbol(decision.getSymbol()));
        asset.setSlotType("DECISION");
        asset.setMarketBias(trimToNull(decision.getMarketBiasHierarchy()));
        asset.setMarketBiasLabel(biasLabel(decision.getMarketBiasHierarchy()));
        asset.setCompositeScore(null);
        asset.setConfidenceLevel(trimToNull(decision.getConfidenceLevel()));
        asset.setConfidenceLabel(confidenceLabel(decision.getConfidenceLevel()));
        asset.setRiskLevel(trimToNull(decision.getRiskLevel()));
        asset.setRiskLabel(riskLabel(decision.getRiskLevel()));
        asset.setAssetState(trimToNull(decision.getAssetStateSnapshot()));
        asset.setAssetStateLabel(assetStateLabel(decision));
        asset.setWorthOpening(decision.getIsWorthOpening());
        return asset;
    }

    private DashboardHomeVO.AssetVO assetPlaceholder(int slot, String symbol) {
        DashboardHomeVO.AssetVO asset = assetBase(slot, symbol);
        asset.setSlotType("DEFAULT_SLOT");
        return asset;
    }

    private DashboardHomeVO.AssetVO assetBase(int slot, String normalizedSymbol) {
        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setSlot(slot);
        asset.setRawSymbol(normalizedSymbol);
        asset.setSymbol(toDisplaySymbol(normalizedSymbol));
        return asset;
    }

    private List<DashboardHomeVO.PositionVO> buildPositions(List<UserPositionVO> positions) {
        List<DashboardHomeVO.PositionVO> rows = new ArrayList<>();
        for (UserPositionVO position : positions == null ? List.<UserPositionVO>of() : positions) {
            DashboardHomeVO.PositionVO row = new DashboardHomeVO.PositionVO();
            row.setPositionId(position.getId());
            row.setSymbol(toDisplaySymbol(position.getAssetSymbol()));
            row.setDirection(trimToNull(position.getSide()));
            row.setEntryPrice(position.getEntryPrice());
            row.setCurrentPrice(null);
            row.setFloatingPnl(null);
            row.setLeverage(position.getLeverage());
            row.setPositionSize(position.getQuantity());
            row.setPositionStatus(trimToNull(position.getStatus()));
            row.setMonitorConclusion(null);
            row.setUpdatedAt(position.getUpdatedAt());
            rows.add(row);
        }
        return rows;
    }

    private DashboardHomeVO.ExecutionSuggestionVO buildExecutionSuggestion(DecisionResultVO decision) {
        DashboardHomeVO.ExecutionSuggestionVO suggestion = new DashboardHomeVO.ExecutionSuggestionVO();
        if (decision == null) {
            return suggestion;
        }
        suggestion.setDirection(trimToNull(decision.getMarketBiasHierarchy()));
        suggestion.setEntryZone(trimToNull(decision.getEntryZone()));
        suggestion.setStopLoss(trimToNull(decision.getStopLoss()));
        suggestion.setTakeProfitRules(trimToNull(decision.getTakeProfitRules()));
        suggestion.setLeverageSuggestion(trimToNull(decision.getLeverageSuggestion()));
        suggestion.setPositionSuggestion(trimToNull(decision.getPositionSuggestion()));
        suggestion.setValidPeriod(trimToNull(decision.getValidPeriod()));
        suggestion.setInvalidCondition(trimToNull(decision.getInvalidCondition()));
        return suggestion;
    }

    private DashboardHomeVO.AiDecisionVO buildAiDecision(DecisionResultVO decision) {
        DashboardHomeVO.AiDecisionVO ai = new DashboardHomeVO.AiDecisionVO();
        ai.setActiveTab("GPT_FINAL");
        JsonNode root = parseAiRoleResults(decision != null ? decision.getAiRoleResults() : null);
        List<DashboardHomeVO.AiTabVO> tabs = new ArrayList<>();
        for (String role : AI_ROLES) {
            tabs.add(buildAiTab(role, roleNode(root, role), decision));
        }
        ai.setTabs(tabs);
        DashboardHomeVO.ConsistencyVO consistency = new DashboardHomeVO.ConsistencyVO();
        consistency.setLevel(decision != null ? trimToNull(decision.getAiConflictLevel()) : null);
        consistency.setScore(decision != null ? decision.getAiConflictScore() : null);
        consistency.setConfused(decision != null && decision.getConfusedScore() != null && decision.getConfusedScore() > 0);
        ai.setConsistency(consistency);
        return ai;
    }

    private DashboardHomeVO.AiTabVO buildAiTab(String role, JsonNode roleNode, DecisionResultVO decision) {
        DashboardHomeVO.AiTabVO tab = new DashboardHomeVO.AiTabVO();
        tab.setRole(role);
        tab.setRoleLabel(roleLabel(role));
        tab.setDirection(firstNonBlank(text(roleNode, "direction", "marketBias", "market_bias"),
                decision != null ? decision.getMarketBiasHierarchy() : null));
        tab.setConfidenceLevel(firstNonBlank(text(roleNode, "confidenceLevel", "confidence_level", "confidence"),
                decision != null ? decision.getConfidenceLevel() : null));
        tab.setSupportEvidence(textList(roleNode, "supportEvidence", "supportEvidences", "support_evidence",
                "support_evidences", "supportingEvidence", "supportingEvidences"));
        tab.setAgainstEvidence(textList(roleNode, "againstEvidence", "againstEvidences", "against_evidence",
                "against_evidences", "opposingEvidence", "opposingEvidences"));
        tab.setRiskPoints(textList(roleNode, "riskPoint", "riskPoints", "risk_point", "risk_points"));
        tab.setDowngradeReason(text(roleNode, "downgradeReason", "downgrade_reason", "blockReason", "block_reason"));
        tab.setReviewConclusion(text(roleNode, "reviewConclusion", "review_conclusion", "conclusion", "summary"));
        if (tab.getReviewConclusion() == null && roleNode != null && roleNode.isTextual()) {
            tab.setReviewConclusion(trimToNull(roleNode.asText()));
        }
        return tab;
    }

    private PushInboxContext buildPushInbox(List<UserPositionVO> positions, int limit) {
        DashboardHomeVO.PushInboxVO inbox = new DashboardHomeVO.PushInboxVO();
        boolean hasOpenPosition = positions != null && !positions.isEmpty();
        inbox.setHasOpenPosition(hasOpenPosition);
        inbox.setMode(hasOpenPosition ? "OPPORTUNITY_AND_POSITION_RISK" : "OPPORTUNITY_ONLY");
        inbox.setTelegramStatus("WAITING_SYNC");

        boolean readOk = false;
        int waiting = 0;
        List<DashboardHomeVO.PushItemVO> items = new ArrayList<>();
        try {
            waiting = Math.max(0, pushSnapshotMapper.countPendingRecheckBacklog());
            items.addAll(pushItems("CAPTURED", limit));
            if (items.size() < limit) {
                items.addAll(pushItems("RECHECK_REVIEW_WAITING", limit - items.size()));
            }
            if (items.size() < limit) {
                items.addAll(pushItems("RECHECK_VALID_WAITING", limit - items.size()));
            }
            readOk = true;
        } catch (RuntimeException ignored) {
            waiting = 0;
            items = List.of();
        }

        DashboardHomeVO.PushCountsVO counts = new DashboardHomeVO.PushCountsVO();
        counts.setExecutable(0);
        counts.setWaiting(waiting);
        counts.setInvalidated(0);
        counts.setPositionRisk(positions != null ? positions.size() : 0);
        inbox.setCounts(counts);
        inbox.setItems(items.size() > limit ? items.subList(0, limit) : items);
        return new PushInboxContext(inbox, readOk);
    }

    private List<DashboardHomeVO.PushItemVO> pushItems(String status, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<DashboardHomeVO.PushItemVO> items = new ArrayList<>();
        for (TmPushSnapshotDO row : pushSnapshotMapper.listPendingRecheck(status, limit)) {
            DashboardHomeVO.PushItemVO item = new DashboardHomeVO.PushItemVO();
            item.setPushId(row.getPushId());
            item.setSymbol(toDisplaySymbol(row.getSymbol()));
            item.setStatus(row.getPushStatus());
            item.setCreatedAt(row.getPushCreateTime() != null ? row.getPushCreateTime() : row.getCreateTime());
            items.add(item);
        }
        return items;
    }

    private DashboardHomeVO.DiagnosticsVO buildDiagnostics(LightSystemStatusVO systemStatus,
                                                           List<DecisionResultVO> decisions,
                                                           DecisionResultVO selectedDecision,
                                                           PositionSyncStatusVO positionSyncStatus,
                                                           PushInboxContext pushInboxContext) {
        DashboardHomeVO.DiagnosticsVO diagnostics = new DashboardHomeVO.DiagnosticsVO();
        diagnostics.setDataIngestion(diagnosticFromFreshness(positionSyncStatus));
        diagnostics.setDataQuality(averageDataQuality(decisions) != null ? "CONNECTED" : "WAITING_SYNC");
        diagnostics.setAiCall(hasText(selectedDecision != null ? selectedDecision.getAiRoleResults() : null) ? "CONNECTED" : "WAITING_SYNC");
        diagnostics.setPushRecheck(pushInboxContext.readOk() ? "CONNECTED" : "UNKNOWN");
        diagnostics.setTelegram("WAITING_SYNC");
        diagnostics.setConfused(confusedCount(systemStatus, decisions) != null ? "CONNECTED" : "UNKNOWN");
        diagnostics.setHotReset(systemStatus != null && systemStatus.getHotResetFired() != null ? "CONNECTED" : "WAITING_SYNC");
        diagnostics.setOpportunityLog("UNKNOWN");
        diagnostics.setReview("UNKNOWN");
        return diagnostics;
    }

    private DashboardHomeVO.StatusCardVO card(String key,
                                              String label,
                                              Object value,
                                              String valueLabel,
                                              String helper,
                                              String status,
                                              Integer score) {
        DashboardHomeVO.StatusCardVO card = new DashboardHomeVO.StatusCardVO();
        card.setKey(key);
        card.setLabel(label);
        card.setValue(value);
        card.setValueLabel(valueLabel);
        card.setHelper(helper);
        card.setStatus(status);
        card.setScore(score);
        return card;
    }

    private LightSystemStatusVO safeSystemStatus() {
        try {
            return decisionService.getLightSystemStatus();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<DecisionResultVO> safeDecisions(int limit) {
        try {
            List<DecisionResultVO> decisions = decisionService.getLatestDecisionResults(limit);
            return decisions != null ? decisions : List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private DecisionResultVO safeDecisionBySymbol(String symbol) {
        try {
            return hasText(symbol) ? decisionService.getLatestDecisionResultBySymbol(symbol) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<MonitorAlertDO> safeAlerts() {
        try {
            List<MonitorAlertDO> alerts = monitorService.getRecentAlerts(2);
            return alerts != null ? alerts : List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<UserPositionVO> safePositions() {
        try {
            List<UserPositionVO> positions = userPositionService.listOpenPositions();
            return positions != null ? positions : List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private PositionSyncStatusVO safePositionSyncStatus() {
        try {
            return positionSyncService.getPositionSyncStatus();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ExternalContextSnapshot safeExternalContext(String selectedSymbol, DecisionResultVO decision) {
        try {
            return externalContextEvidenceBuilder.buildSnapshot(
                    "dashboard-home",
                    selectedSymbol,
                    firstNonBlank(decision != null ? decision.getTimeframe() : null, "1h"),
                    LocalDateTime.now(),
                    "CRYPTO"
            );
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private JsonNode parseAiRoleResults(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonNode roleNode(JsonNode root, String role) {
        if (root == null || !root.isObject()) {
            return null;
        }
        for (String key : roleKeys(role)) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private List<String> roleKeys(String role) {
        if ("GPT_FINAL".equals(role)) {
            return List.of("GPT_FINAL", "gptFinal", "gpt_final", "GPT", "gpt", "final");
        }
        if ("GEMINI_REVIEW".equals(role)) {
            return List.of("GEMINI_REVIEW", "geminiReview", "gemini_review", "GEMINI", "gemini");
        }
        return List.of("GROK_CHALLENGE", "grokChallenge", "grok_challenge", "GROK", "grok");
    }

    private String text(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String key : keys) {
            JsonNode child = node.get(key);
            if (child == null || child.isNull()) {
                continue;
            }
            if (child.isTextual() || child.isNumber() || child.isBoolean()) {
                String value = trimToNull(child.asText());
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private List<String> textList(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) {
            return List.of();
        }
        for (String key : keys) {
            JsonNode child = node.get(key);
            if (child == null || child.isNull()) {
                continue;
            }
            List<String> values = new ArrayList<>();
            if (child.isArray()) {
                child.forEach(item -> {
                    String value = item.isTextual() || item.isNumber() || item.isBoolean() ? trimToNull(item.asText()) : null;
                    if (value != null) {
                        values.add(value);
                    }
                });
            } else if (child.isTextual() || child.isNumber() || child.isBoolean()) {
                String value = trimToNull(child.asText());
                if (value != null) {
                    values.add(value);
                }
            }
            if (!values.isEmpty()) {
                return values;
            }
        }
        return List.of();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String firstDecisionSymbol(List<DecisionResultVO> decisions) {
        DecisionResultVO first = firstDecision(decisions);
        return first != null ? normalizeSymbol(first.getSymbol()) : null;
    }

    private DecisionResultVO firstDecision(List<DecisionResultVO> decisions) {
        if (decisions == null) {
            return null;
        }
        return decisions.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private DecisionResultVO findDecision(List<DecisionResultVO> decisions, String normalizedSymbol) {
        if (!hasText(normalizedSymbol) || decisions == null) {
            return null;
        }
        return decisions.stream()
                .filter(Objects::nonNull)
                .filter(d -> normalizedSymbol.equals(normalizeSymbol(d.getSymbol())))
                .findFirst()
                .orElse(null);
    }

    private String riskLevelFrom(List<DecisionResultVO> decisions) {
        DecisionResultVO first = firstDecision(decisions);
        return first != null ? first.getRiskLevel() : null;
    }

    private Integer averageDataQuality(List<DecisionResultVO> decisions) {
        if (decisions == null || decisions.isEmpty()) {
            return null;
        }
        int sum = 0;
        int count = 0;
        for (DecisionResultVO decision : decisions) {
            if (decision != null && decision.getDataQualityScore() != null) {
                sum += decision.getDataQualityScore();
                count++;
            }
        }
        return count > 0 ? Math.round((float) sum / count) : null;
    }

    private AiConflictSummary aiConflictSummary(List<DecisionResultVO> decisions) {
        String level = null;
        Integer score = null;
        if (decisions != null) {
            for (DecisionResultVO decision : decisions) {
                if (decision == null) {
                    continue;
                }
                if (level == null && hasText(decision.getAiConflictLevel())) {
                    level = decision.getAiConflictLevel();
                }
                if (decision.getAiConflictScore() != null && (score == null || decision.getAiConflictScore() > score)) {
                    score = decision.getAiConflictScore();
                    if (hasText(decision.getAiConflictLevel())) {
                        level = decision.getAiConflictLevel();
                    }
                }
            }
        }
        return new AiConflictSummary(level, score);
    }

    private Integer confusedCount(LightSystemStatusVO systemStatus, List<DecisionResultVO> decisions) {
        if (systemStatus != null && systemStatus.getConfusedCount() != null) {
            return systemStatus.getConfusedCount();
        }
        if (decisions == null) {
            return null;
        }
        int count = 0;
        boolean sawField = false;
        for (DecisionResultVO decision : decisions) {
            if (decision != null && decision.getConfusedScore() != null) {
                sawField = true;
                if (decision.getConfusedScore() > 0) {
                    count++;
                }
            }
        }
        return sawField ? count : null;
    }

    private String dataSourceText(PositionSyncStatusVO positionSyncStatus, ExternalContextSnapshot externalContext) {
        String provider = positionSyncStatus != null
                ? firstNonBlank(positionSyncStatus.getActiveProviderType(), positionSyncStatus.getConfiguredProviderType())
                : null;
        String sourceHealth = externalContext != null ? externalContext.getSourceHealth() : null;
        if (hasText(provider) && hasText(sourceHealth)) {
            return provider + " / " + sourceHealth;
        }
        return firstNonBlank(provider, sourceHealth, "WAITING_SYNC");
    }

    private String diagnosticFromFreshness(PositionSyncStatusVO positionSyncStatus) {
        if (positionSyncStatus == null || !hasText(positionSyncStatus.getFreshnessStatus())) {
            return "WAITING_SYNC";
        }
        String value = positionSyncStatus.getFreshnessStatus().toUpperCase(Locale.ROOT);
        if (value.contains("FRESH") || value.contains("READY")) {
            return "CONNECTED";
        }
        if (value.contains("STALE") || value.contains("PARTIAL")) {
            return "PARTIAL";
        }
        return "UNKNOWN";
    }

    private String statusForText(String value) {
        return hasText(value) ? "CONNECTED" : "WAITING_SYNC";
    }

    private String assetStateLabel(DecisionResultVO decision) {
        if (decision == null) {
            return null;
        }
        String raw = trimToNull(decision.getAssetStateSnapshot());
        if (raw != null) {
            return raw;
        }
        if (Boolean.TRUE.equals(decision.getIsWorthOpening())) {
            return "候选";
        }
        if (Boolean.FALSE.equals(decision.getIsWorthOpening())) {
            return "暂不建议";
        }
        return null;
    }

    private String biasLabel(String bias) {
        String value = upper(bias);
        return switch (value) {
            case "STRONG_BULLISH" -> "强偏多";
            case "BULLISH" -> "偏多";
            case "WEAK_BULLISH" -> "弱偏多";
            case "RANGE" -> "震荡";
            case "WEAK_BEARISH" -> "弱偏空";
            case "BEARISH" -> "偏空";
            case "STRONG_BEARISH" -> "强偏空";
            case "WAIT" -> "等待";
            default -> null;
        };
    }

    private String confidenceLabel(String confidence) {
        String value = upper(confidence);
        return switch (value) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> null;
        };
    }

    private String riskLabel(String risk) {
        String value = upper(risk);
        return switch (value) {
            case "EXTREME", "VERY_HIGH" -> "极高";
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> hasText(risk) ? risk : null;
        };
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "GPT_FINAL" -> "最终裁决官";
            case "GEMINI_REVIEW" -> "冲突复核官";
            case "GROK_CHALLENGE" -> "反方挑战官";
            default -> role;
        };
    }

    private String normalizeSymbol(String symbol) {
        if (!hasText(symbol)) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT).replace("/", "");
    }

    private String toDisplaySymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            return null;
        }
        if (normalized.endsWith("USDT") && normalized.length() > 4) {
            return normalized.substring(0, normalized.length() - 4) + "/USDT";
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    private String upper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private record AiConflictSummary(String level, Integer score) {
    }

    private record PushInboxContext(DashboardHomeVO.PushInboxVO pushInbox, boolean readOk) {
    }
}
