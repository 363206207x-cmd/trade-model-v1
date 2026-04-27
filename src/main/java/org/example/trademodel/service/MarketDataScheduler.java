package org.example.trademodel.service;

import org.example.trademodel.entity.RuleConfigDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MarketDataScheduler - V2 增强版
 * 支持任意币种 + 简单自定义规则
 *
 * <p>定时路径每 symbol 每轮只触发一次权威主链：{@code fetchRealMarketData → assemble → makeDecision（含 dataQuality）+ 落库}。
 * 不在此再调用 {@code DecisionEngineService#makeDecision}，避免与 assemble 内决策重复。</p>
 */
@Component
public class MarketDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketDataScheduler.class);

    private final RealMarketDataFetcherService realMarketDataFetcherService;

    private final RuleConfigService ruleConfigService;

    private static final String KEY_SCHEDULER_SYMBOLS = "scheduler.symbols";
    private static final String DEFAULT_SCHEDULER_SYMBOLS =
            "BTCUSDT,ETHUSDT,SOLUSDT,XRPUSDT,BNBUSDT,DOGEUSDT";

    public MarketDataScheduler(RealMarketDataFetcherService realMarketDataFetcherService,
                                 RuleConfigService ruleConfigService) {
        this.realMarketDataFetcherService = realMarketDataFetcherService;
        this.ruleConfigService = ruleConfigService;
    }

    @Scheduled(initialDelay = 60000, fixedRate = 30000)  // 首次延迟60秒，之后每30秒执行一次
    public void fetchRealMarketDataScheduled() {
        log.info("=== [V2 定时任务] 开始：每币种仅走 assemble 主链（含单次 AI 决策与落库） ===");

        Map<String, RuleConfigDO> ruleMap = ruleConfigService != null
                ? ruleConfigService.getRuleConfigMap()
                : null;

        String symbolsRaw = getString(ruleMap, KEY_SCHEDULER_SYMBOLS, DEFAULT_SCHEDULER_SYMBOLS);
        String[] symbols = parseSymbols(symbolsRaw);

        for (String symbol : symbols) {
            log.info("[sched-single-chain] symbol={} interval=1m → fetchRealMarketData only (no extra makeDecision)", symbol);
            realMarketDataFetcherService.fetchRealMarketData(symbol, "1m");
        }

        log.info("=== [V2 定时任务] 本轮 {} 个币种处理完成（每 symbol 单次决策链） ===", symbols.length);
    }

    private static String[] parseSymbols(String raw) {
        String effectiveRaw = raw;
        if (effectiveRaw == null) {
            effectiveRaw = DEFAULT_SCHEDULER_SYMBOLS;
        }
        String[] parts = effectiveRaw.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String t = p.trim().toUpperCase();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            return new String[]{"BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "BNBUSDT", "DOGEUSDT"};
        }
        return out.toArray(new String[0]);
    }

    private static String getString(Map<String, RuleConfigDO> cfgMap, String key, String defaultVal) {
        if (cfgMap == null || key == null) {
            return defaultVal;
        }
        RuleConfigDO cfg = cfgMap.get(key);
        if (cfg == null || cfg.getRuleValue() == null) {
            return defaultVal;
        }
        String raw = cfg.getRuleValue().trim();
        return raw.isEmpty() ? defaultVal : raw;
    }
}
