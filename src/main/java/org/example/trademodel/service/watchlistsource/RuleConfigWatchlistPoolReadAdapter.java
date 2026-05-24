package org.example.trademodel.service.watchlistsource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceTypeEnum;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;

public class RuleConfigWatchlistPoolReadAdapter implements WatchlistPoolRuntimeSourceReadAdapter {

    private static final String WATCHLIST_RULE_KEY = "push.watchlist.symbols";
    private static final String FIELD_REQUEST = "request";
    private static final String FIELD_WATCHLIST_POOL_ONLY = "watchlistPoolOnly";
    private static final String FIELD_WATCHLIST_RULE_KEY = "push.watchlist.symbols";
    private static final String FIELD_SYMBOL = "symbol";
    private static final String REASON_REQUEST_MISSING = "REQUEST_MISSING";
    private static final String REASON_WATCHLIST_POOL_READ_BLOCKED = "WATCHLIST_POOL_READ_BLOCKED";
    private static final String REASON_WATCHLIST_POOL_ONLY_REQUIRED = "WATCHLIST_POOL_ONLY_REQUIRED";
    private static final String REASON_REQUEST_INCOMPLETE = "REQUEST_INCOMPLETE";
    private static final String REASON_RULE_CONFIG_SERVICE_MISSING = "RULE_CONFIG_SERVICE_MISSING";
    private static final String REASON_RULE_CONFIG_READ_FAILED = "RULE_CONFIG_READ_FAILED";
    private static final String REASON_WATCHLIST_CONFIG_MISSING_OR_EMPTY =
            "WATCHLIST_CONFIG_MISSING_OR_EMPTY";
    private static final String REASON_BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String REASON_REVIEW_ONLY_DB_WATCHLIST_READ = "REVIEW_ONLY_DB_WATCHLIST_READ";

    private final RuleConfigService ruleConfigService;

    public RuleConfigWatchlistPoolReadAdapter(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @Override
    public RuntimeSourceReadResultDTO read(RuntimeSourceReadRequestDTO request) {
        if (request == null) {
            return RuntimeSourceReadResultDTO.incomplete(
                    null,
                    List.of(FIELD_REQUEST),
                    List.of(REASON_REQUEST_MISSING, REASON_WATCHLIST_POOL_READ_BLOCKED)
            );
        }

        if (!Boolean.TRUE.equals(request.getWatchlistPoolOnly())) {
            return RuntimeSourceReadResultDTO.incomplete(
                    request.getSymbol(),
                    List.of(FIELD_WATCHLIST_POOL_ONLY),
                    withReasons(request.getBlockingReasons(), REASON_WATCHLIST_POOL_ONLY_REQUIRED)
            );
        }

        List<String> requestMissingFields = request.getMissingFields();
        if (!requestMissingFields.isEmpty()) {
            return RuntimeSourceReadResultDTO.incomplete(
                    request.getSymbol(),
                    requestMissingFields,
                    withReasons(request.getBlockingReasons(), REASON_REQUEST_INCOMPLETE)
            );
        }

        if (ruleConfigService == null) {
            return RuntimeSourceReadResultDTO.sourceUnavailable(
                    request.getSymbol(),
                    withReasons(request.getBlockingReasons(), REASON_RULE_CONFIG_SERVICE_MISSING)
            );
        }

        RuleConfigDO ruleConfig;
        try {
            ruleConfig = readWatchlistRuleConfig();
        } catch (RuntimeException ex) {
            return RuntimeSourceReadResultDTO.sourceUnavailable(
                    request.getSymbol(),
                    withReasons(request.getBlockingReasons(), REASON_RULE_CONFIG_READ_FAILED)
            );
        }

        Set<String> watchlistSymbols = parseWatchlistSymbols(ruleConfig);
        if (watchlistSymbols.isEmpty()) {
            return RuntimeSourceReadResultDTO.incomplete(
                    request.getSymbol(),
                    List.of(FIELD_WATCHLIST_RULE_KEY),
                    withReasons(request.getBlockingReasons(), REASON_WATCHLIST_CONFIG_MISSING_OR_EMPTY)
            );
        }

        String normalizedSymbol = normalizeSymbol(request.getSymbol());
        if (normalizedSymbol.isBlank() || !watchlistSymbols.contains(normalizedSymbol)) {
            return RuntimeSourceReadResultDTO.incomplete(
                    request.getSymbol(),
                    List.of(FIELD_SYMBOL),
                    withReasons(request.getBlockingReasons(), REASON_BLOCKED_NOT_WATCHLIST)
            );
        }

        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.availableReviewOnly(
                request.getSymbol(),
                WatchlistRuntimeSourceTypeEnum.WATCHLIST_CONFIG,
                WATCHLIST_RULE_KEY,
                withReasons(request.getBlockingReasons(), REASON_REVIEW_ONLY_DB_WATCHLIST_READ)
        );
        return RuntimeSourceReadResultDTO.fromRuntimeSource(source);
    }

    private RuleConfigDO readWatchlistRuleConfig() {
        Map<String, RuleConfigDO> ruleConfigMap = ruleConfigService.getRuleConfigMap();
        if (ruleConfigMap == null) {
            return null;
        }
        return ruleConfigMap.get(WATCHLIST_RULE_KEY);
    }

    private static Set<String> parseWatchlistSymbols(RuleConfigDO ruleConfig) {
        Set<String> symbols = new LinkedHashSet<>();
        if (ruleConfig == null || Boolean.FALSE.equals(ruleConfig.getEnabled())) {
            return symbols;
        }
        String ruleValue = ruleConfig.getRuleValue();
        if (ruleValue == null || ruleValue.isBlank()) {
            return symbols;
        }
        for (String rawSymbol : ruleValue.split(",")) {
            String symbol = normalizeSymbol(rawSymbol);
            if (!symbol.isBlank()) {
                symbols.add(symbol);
            }
        }
        return symbols;
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> withReasons(List<String> baseReasons, String reason) {
        List<String> resolvedReasons = new ArrayList<>();
        if (baseReasons != null) {
            resolvedReasons.addAll(baseReasons);
        }
        addIfAbsent(resolvedReasons, reason);
        return resolvedReasons;
    }

    private static void addIfAbsent(List<String> values, String value) {
        if (value != null && !values.contains(value)) {
            values.add(value);
        }
    }
}
