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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rule")
public class RuleController {

    private static final String WATCHLIST_RULE_KEY = "push.watchlist.symbols";
    private static final String STATUS_READY = "WATCHLIST_REVIEW_ONLY_READY";
    private static final String STATUS_EMPTY = "WATCHLIST_EMPTY_FAIL_CLOSED";
    private static final String STATUS_MISSING = "WATCHLIST_CONFIG_MISSING";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String SOURCE_DB = "DB";
    private static final String SOURCE_MISSING = "MISSING";
    private static final String SOURCE_UNKNOWN = "UNKNOWN";

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

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
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
