package org.example.trademodel.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rule")
public class RuleController {

    private static final String WATCHLIST_RULE_KEY = "push.watchlist.symbols";
    private static final String STATUS_READY = "WATCHLIST_REVIEW_ONLY_READY";
    private static final String STATUS_EMPTY = "WATCHLIST_EMPTY_FAIL_CLOSED";
    private static final String STATUS_MISSING = "WATCHLIST_CONFIG_MISSING";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String RULECONFIG_STATUS_READY = "RULECONFIG_AUDIT_REVIEW_ONLY_READY";
    private static final String RULECONFIG_STATUS_WATCHLIST_READY = "RULECONFIG_WATCHLIST_KEY_READY_CONTEXT";
    private static final String RULECONFIG_STATUS_METADATA_PARTIAL = "RULECONFIG_VERSION_OR_DESCRIPTION_PARTIAL";
    private static final String RULECONFIG_STATUS_MISSING = "RULECONFIG_CONFIG_MISSING_FAIL_CLOSED";
    private static final String RULECONFIG_STATUS_PARSE_RISK = "RULECONFIG_VALUE_EMPTY_OR_PARSE_RISK_FAIL_CLOSED";
    private static final String RULECONFIG_STATUS_BLOCKED = "RULECONFIG_AUDIT_BLOCKED_FAIL_CLOSED";
    private static final String AUDIT_CONTEXT_PARTIAL = "RULECONFIG_AUDIT_CONTEXT_PARTIAL";
    private static final String SOURCE_DB = "DB";
    private static final String SOURCE_MISSING = "MISSING";
    private static final String SOURCE_UNKNOWN = "UNKNOWN";
    private static final String RULECONFIG_SOURCE_REF = "RuleConfigService#getRuleConfigMap enabled-rule cache";
    private static final String RULE_VERSION_LOG_CONTEXT =
            "RuleVersionLog is review/analysis audit context only; current RuleConfig status stays on RuleConfigService.";

    private final RuleConfigService ruleConfigService;

    @Autowired
    public RuleController(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @GetMapping("/reload")
    public ApiResponse<String> reloadRules() {
        try {
            ruleConfigService.reloadRules();
            return ApiResponse.success("规则已热加载");
        } catch (Exception e) {
            return ApiResponse.fail("规则热加载失败: " + e.getMessage());
        }
    }

    @GetMapping("/push-watchlist")
    public ApiResponse<Map<String, Object>> pushWatchlistStatus() {
        try {
            return ApiResponse.success(buildPushWatchlistStatus());
        } catch (RuntimeException ex) {
            return ApiResponse.success(statusPayload(
                    STATUS_BLOCKED,
                    SOURCE_UNKNOWN,
                    Collections.emptyList(),
                    true,
                    "RULE_CONFIG_READ_FAILED",
                    "Watchlist Pool read failed; review-only status remains fail-closed."
            ));
        }
    }

    @GetMapping("/config-audit-status")
    public ApiResponse<Map<String, Object>> configAuditStatus(
            @RequestParam(value = "ruleKey", defaultValue = WATCHLIST_RULE_KEY) String ruleKey
    ) {
        try {
            return ApiResponse.success(buildConfigAuditStatus(ruleKey));
        } catch (RuntimeException ex) {
            String requestedRuleKey = normalizeRuleKey(ruleKey);
            if (requestedRuleKey.isBlank()) {
                requestedRuleKey = WATCHLIST_RULE_KEY;
            }
            return ApiResponse.success(ruleConfigAuditPayload(
                    RULECONFIG_STATUS_BLOCKED,
                    requestedRuleKey,
                    null,
                    null,
                    Collections.emptyList(),
                    SOURCE_UNKNOWN,
                    true,
                    "RULE_CONFIG_AUDIT_READ_FAILED",
                    "RuleConfig audit status read failed; review-only status remains fail-closed."
            ));
        }
    }

    private Map<String, Object> buildPushWatchlistStatus() {
        Map<String, RuleConfigDO> ruleConfigMap = ruleConfigService == null
                ? Collections.emptyMap()
                : ruleConfigService.getRuleConfigMap();
        RuleConfigDO ruleConfig = ruleConfigMap == null ? null : ruleConfigMap.get(WATCHLIST_RULE_KEY);
        if (ruleConfig == null || Boolean.FALSE.equals(ruleConfig.getEnabled())) {
            return statusPayload(
                    STATUS_MISSING,
                    SOURCE_MISSING,
                    Collections.emptyList(),
                    true,
                    "WATCHLIST_CONFIG_MISSING",
                    "Watchlist Pool config is missing; Display Slots are not the candidate pool."
            );
        }

        SymbolParseResult parsed = parseSymbols(ruleConfig.getRuleValue());
        if (parsed.parseError) {
            return statusPayload(
                    STATUS_BLOCKED,
                    SOURCE_DB,
                    Collections.emptyList(),
                    true,
                    "WATCHLIST_CONFIG_PARSE_ERROR",
                    "Watchlist Pool config is unsafe; review-only status remains fail-closed."
            );
        }
        if (parsed.symbols.isEmpty()) {
            return statusPayload(
                    STATUS_EMPTY,
                    SOURCE_DB,
                    Collections.emptyList(),
                    true,
                    "WATCHLIST_EMPTY",
                    "Watchlist Pool is empty; Display Slots are not the candidate pool."
            );
        }

        return statusPayload(
                STATUS_READY,
                SOURCE_DB,
                parsed.symbols,
                false,
                "REVIEW_ONLY_DB_WATCHLIST_READ",
                "Watchlist Pool loaded from DB config; review-only status does not send Push."
        );
    }

    private Map<String, Object> buildConfigAuditStatus(String ruleKey) {
        String requestedRuleKey = normalizeRuleKey(ruleKey);
        if (requestedRuleKey.isBlank()) {
            requestedRuleKey = WATCHLIST_RULE_KEY;
        }
        Map<String, RuleConfigDO> ruleConfigMap = ruleConfigService == null
                ? Collections.emptyMap()
                : ruleConfigService.getRuleConfigMap();
        RuleConfigDO ruleConfig = ruleConfigMap == null ? null : ruleConfigMap.get(requestedRuleKey);
        if (ruleConfig == null || Boolean.FALSE.equals(ruleConfig.getEnabled())) {
            return ruleConfigAuditPayload(
                    RULECONFIG_STATUS_MISSING,
                    requestedRuleKey,
                    null,
                    isWatchlistRule(requestedRuleKey) ? STATUS_MISSING : null,
                    Collections.emptyList(),
                    SOURCE_MISSING,
                    true,
                    "RULECONFIG_ENABLED_RULE_NOT_FOUND",
                    "RuleConfig key is missing from the enabled-rule view; disabled-vs-missing remains partial and fail-closed."
            );
        }

        SymbolParseResult parsed = isWatchlistRule(requestedRuleKey)
                ? parseSymbols(ruleConfig.getRuleValue())
                : new SymbolParseResult(Collections.emptyList(), false);
        if (parsed.parseError || isBlank(ruleConfig.getRuleValue())
                || (isWatchlistRule(requestedRuleKey) && parsed.symbols.isEmpty())) {
            return ruleConfigAuditPayload(
                    RULECONFIG_STATUS_PARSE_RISK,
                    requestedRuleKey,
                    ruleConfig,
                    parsed.parseError ? STATUS_BLOCKED : STATUS_EMPTY,
                    Collections.emptyList(),
                    SOURCE_DB,
                    true,
                    parsed.parseError ? "RULECONFIG_WATCHLIST_PARSE_RISK" : "RULECONFIG_VALUE_EMPTY",
                    "RuleConfig value cannot be safely explained; review-only status remains fail-closed."
            );
        }

        boolean metadataPartial = isBlank(ruleConfig.getVersion()) || isBlank(ruleConfig.getDescription());
        String status = metadataPartial
                ? RULECONFIG_STATUS_METADATA_PARTIAL
                : isWatchlistRule(requestedRuleKey) ? RULECONFIG_STATUS_WATCHLIST_READY : RULECONFIG_STATUS_READY;
        String reason = metadataPartial
                ? "RULECONFIG_VERSION_OR_DESCRIPTION_PARTIAL"
                : isWatchlistRule(requestedRuleKey) ? "RULECONFIG_WATCHLIST_KEY_READABLE" : "RULECONFIG_KEY_READABLE";
        String message = metadataPartial
                ? "RuleConfig key is readable, but version or description metadata is partial; downstream action remains fail-closed."
                : "RuleConfig key is readable for manual review only; this explains configuration state, not action intent.";

        return ruleConfigAuditPayload(
                status,
                requestedRuleKey,
                ruleConfig,
                isWatchlistRule(requestedRuleKey) ? STATUS_READY : null,
                parsed.symbols,
                SOURCE_DB,
                metadataPartial,
                reason,
                message
        );
    }

    private static Map<String, Object> statusPayload(
            String status,
            String source,
            List<String> symbols,
            boolean failClosed,
            String reason,
            String message
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("configKey", WATCHLIST_RULE_KEY);
        payload.put("symbols", symbols == null ? Collections.emptyList() : List.copyOf(symbols));
        payload.put("source", source);
        payload.put("empty", symbols == null || symbols.isEmpty());
        payload.put("failClosed", failClosed);
        payload.put("reviewOnly", true);
        payload.put("displaySlotsAreCandidatePool", false);
        payload.put("reason", reason);
        payload.put("message", message);
        return payload;
    }

    private static Map<String, Object> ruleConfigAuditPayload(
            String status,
            String requestedRuleKey,
            RuleConfigDO ruleConfig,
            String watchlistStatus,
            List<String> watchlistSymbols,
            String source,
            boolean failClosed,
            String reason,
            String message
    ) {
        boolean ruleValuePresent = ruleConfig != null && !isBlank(ruleConfig.getRuleValue());
        boolean descriptionPresent = ruleConfig != null && !isBlank(ruleConfig.getDescription());
        boolean versionPresent = ruleConfig != null && !isBlank(ruleConfig.getVersion());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("ruleKey", requestedRuleKey);
        payload.put("ruleType", firstNonBlank(ruleConfig == null ? null : ruleConfig.getRuleType(), "UNKNOWN"));
        payload.put("configKey", requestedRuleKey);
        payload.put("version", firstNonBlank(ruleConfig == null ? null : ruleConfig.getVersion(), "UNKNOWN"));
        payload.put("descriptionPresent", descriptionPresent);
        payload.put("ruleValuePresent", ruleValuePresent);
        payload.put("ruleValueSummary", summarizeRuleValue(requestedRuleKey, ruleConfig, watchlistSymbols));
        payload.put("enabledKnown", ruleConfig != null);
        payload.put("enabledOnlyView", true);
        payload.put("source", source);
        payload.put("sourceRef", RULECONFIG_SOURCE_REF);
        payload.put("watchlistStatus", watchlistStatus == null ? "NOT_WATCHLIST_KEY" : watchlistStatus);
        payload.put("watchlistSymbols", isWatchlistRule(requestedRuleKey)
                ? List.copyOf(watchlistSymbols == null ? Collections.emptyList() : watchlistSymbols)
                : Collections.emptyList());
        payload.put("auditContextStatus", AUDIT_CONTEXT_PARTIAL);
        payload.put("ruleVersionLogContext", RULE_VERSION_LOG_CONTEXT);
        payload.put("versionOrDescriptionPartial", !versionPresent || !descriptionPresent);
        payload.put("reason", reason);
        payload.put("message", message);
        payload.put("failClosed", failClosed);
        payload.put("reviewOnly", true);
        payload.put("notTradingSignal", true);
        payload.put("notCandidateSignal", true);
        payload.put("notDecisionGeneration", true);
        payload.put("notPointSignal", true);
        payload.put("notExecutable", true);
        payload.put("displaySlotsAreCandidatePool", false);
        return payload;
    }

    private static SymbolParseResult parseSymbols(String ruleValue) {
        if (ruleValue == null || ruleValue.isBlank()) {
            return new SymbolParseResult(Collections.emptyList(), false);
        }
        Set<String> symbols = new LinkedHashSet<>();
        for (String raw : ruleValue.split(",")) {
            String symbol = normalizeSymbol(raw);
            if (symbol.isBlank()) {
                continue;
            }
            if (!symbol.matches("[A-Z0-9]+")) {
                return new SymbolParseResult(Collections.emptyList(), true);
            }
            symbols.add(symbol);
        }
        return new SymbolParseResult(new ArrayList<>(symbols), false);
    }

    private static String summarizeRuleValue(String requestedRuleKey, RuleConfigDO ruleConfig, List<String> watchlistSymbols) {
        if (ruleConfig == null || isBlank(ruleConfig.getRuleValue())) {
            return "empty";
        }
        if (isWatchlistRule(requestedRuleKey)) {
            int count = watchlistSymbols == null ? 0 : watchlistSymbols.size();
            return "watchlistSymbols=" + count;
        }
        return "present:length=" + ruleConfig.getRuleValue().trim().length();
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeRuleKey(String ruleKey) {
        return ruleKey == null ? "" : ruleKey.trim();
    }

    private static boolean isWatchlistRule(String ruleKey) {
        return WATCHLIST_RULE_KEY.equals(ruleKey);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static final class SymbolParseResult {
        private final List<String> symbols;
        private final boolean parseError;

        private SymbolParseResult(List<String> symbols, boolean parseError) {
            this.symbols = symbols;
            this.parseError = parseError;
        }
    }
}
