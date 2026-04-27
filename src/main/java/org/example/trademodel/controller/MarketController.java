package org.example.trademodel.controller;

import org.example.trademodel.service.RealMarketDataFetcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final RealMarketDataFetcherService realMarketDataFetcherService;

    @Autowired
    public MarketController(RealMarketDataFetcherService realMarketDataFetcherService) {
        this.realMarketDataFetcherService = realMarketDataFetcherService;
    }

    @GetMapping("/real-fetch")
    public Map<String, Object> fetchRealMarketData(@RequestParam String symbol,
                                                   @RequestParam(defaultValue = "1m") String interval) {
        Map<String, Object> result = new HashMap<>();
        try {
            realMarketDataFetcherService.fetchRealMarketData(symbol, interval);
            result.put("code", 200);
            result.put("msg", "SUCCESS");
            result.put("data", "Real market data fetch started for " + symbol + " " + interval);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "ERROR: " + e.getMessage());
        }
        return result;
    }
}
