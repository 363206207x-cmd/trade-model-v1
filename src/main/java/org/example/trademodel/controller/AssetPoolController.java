package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.dto.req.AddAssetPoolItemReq;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asset-pool")
public class AssetPoolController {
    private final AssetPoolService assetPoolService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public AssetPoolController(AssetPoolService assetPoolService,
                               AuthenticatedUserIdResolver userIdResolver) {
        this.assetPoolService = assetPoolService;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping
    public ApiResponse<List<AssetPoolAssetDTO>> list() {
        return ApiResponse.success(assetPoolService.listForUser(userIdResolver.requireCurrentUserId()));
    }

    @GetMapping("/search")
    public ApiResponse<List<MarketAssetDTO>> search(@RequestParam(defaultValue = "") String query,
                                                    @RequestParam(defaultValue = "30") int limit) {
        userIdResolver.requireCurrentUserId();
        return ApiResponse.success(assetPoolService.searchMarket(query, limit));
    }

    @PostMapping
    public ApiResponse<AssetPoolAssetDTO> add(@RequestBody AddAssetPoolItemReq request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        return ApiResponse.success(assetPoolService.addForUser(
                userIdResolver.requireCurrentUserId(),
                request.getSymbol(),
                !Boolean.FALSE.equals(request.getFocusEnabled())));
    }

    @DeleteMapping("/{symbol}")
    public ApiResponse<Void> remove(@PathVariable String symbol) {
        assetPoolService.removeForUser(userIdResolver.requireCurrentUserId(), symbol);
        return ApiResponse.success(null);
    }

    @PostMapping("/restore-default")
    public ApiResponse<List<AssetPoolAssetDTO>> restoreDefault() {
        return ApiResponse.success(assetPoolService.restoreDefaults(userIdResolver.requireCurrentUserId()));
    }

    @PostMapping("/scan")
    public ApiResponse<List<AssetPoolScanResultDTO>> scan(@RequestParam(defaultValue = "5m") String timeframe) {
        return ApiResponse.success(assetPoolService.scanForUser(
                userIdResolver.requireCurrentUserId(), timeframe));
    }
}
