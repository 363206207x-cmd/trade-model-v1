package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.AssetAnalysisPreviewDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.dto.req.AddAssetPoolItemReq;
import org.example.trademodel.dto.req.AssetPoolBatchReq;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.entity.AsyncTaskDO;
import org.example.trademodel.service.AsyncTaskService;
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
    private final AsyncTaskService asyncTaskService;

    public AssetPoolController(AssetPoolService assetPoolService,
                               AuthenticatedUserIdResolver userIdResolver,
                               AsyncTaskService asyncTaskService) {
        this.assetPoolService = assetPoolService;
        this.userIdResolver = userIdResolver;
        this.asyncTaskService = asyncTaskService;
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

    @PostMapping("/search/{symbol}/analysis-preview")
    public ApiResponse<AssetAnalysisPreviewDTO> analyzePreview(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5m") String timeframe) {
        Long userId = userIdResolver.requireCurrentUserId();
        AsyncTaskDO task = asyncTaskService.queueForUser(
                userId, "ANALYSIS_PREVIEW", "ASSET", symbol, null);
        asyncTaskService.markRunning(task, "ANALYSIS");
        try {
            AssetAnalysisPreviewDTO result = assetPoolService.analyzePreviewForUser(userId, symbol, timeframe);
            asyncTaskService.complete(task, result == null || !"SUCCESS".equalsIgnoreCase(result.status()), "COMPLETE");
            return ApiResponse.success(result);
        } catch (RuntimeException exception) {
            asyncTaskService.fail(task, "ANALYSIS_PREVIEW_FAILED", exception.getMessage());
            throw exception;
        }
    }

    @PostMapping
    public ApiResponse<AssetPoolAssetDTO> add(@RequestBody AddAssetPoolItemReq request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        return ApiResponse.success(assetPoolService.addForUser(
                userIdResolver.requireCurrentUserId(),
                request.getSymbol(),
                !Boolean.FALSE.equals(request.getFocusEnabled())));
    }

    @PostMapping("/batch-add")
    public ApiResponse<List<AssetPoolAssetDTO>> batchAdd(@RequestBody AssetPoolBatchReq request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        return ApiResponse.success(assetPoolService.addManyForUser(
                userIdResolver.requireCurrentUserId(),
                request.getSymbols(),
                !Boolean.FALSE.equals(request.getFocusEnabled())));
    }

    @DeleteMapping("/{symbol}")
    public ApiResponse<Void> remove(@PathVariable String symbol) {
        assetPoolService.removeForUser(userIdResolver.requireCurrentUserId(), symbol);
        return ApiResponse.success(null);
    }

    @PostMapping("/batch-remove")
    public ApiResponse<Void> batchRemove(@RequestBody AssetPoolBatchReq request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        assetPoolService.removeManyForUser(
                userIdResolver.requireCurrentUserId(), request.getSymbols());
        return ApiResponse.success(null);
    }

    @PostMapping("/restore-default")
    public ApiResponse<List<AssetPoolAssetDTO>> restoreDefault() {
        return ApiResponse.success(assetPoolService.topUpDefaults(userIdResolver.requireCurrentUserId()));
    }

    @PostMapping("/defaults/top-up")
    public ApiResponse<List<AssetPoolAssetDTO>> topUpDefaults() {
        return ApiResponse.success(assetPoolService.topUpDefaults(userIdResolver.requireCurrentUserId()));
    }

    @PostMapping("/defaults/reset")
    public ApiResponse<List<AssetPoolAssetDTO>> resetDefaults() {
        return ApiResponse.success(assetPoolService.resetDefaults(userIdResolver.requireCurrentUserId()));
    }

    @PostMapping("/scan")
    public ApiResponse<List<AssetPoolScanResultDTO>> scan(@RequestParam(defaultValue = "5m") String timeframe) {
        Long userId = userIdResolver.requireCurrentUserId();
        return ApiResponse.success(runScanTask(userId, "POOL", timeframe,
                () -> assetPoolService.scanForUser(userId, timeframe)));
    }

    @PostMapping("/batch-scan")
    public ApiResponse<List<AssetPoolScanResultDTO>> batchScan(@RequestBody AssetPoolBatchReq request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        Long userId = userIdResolver.requireCurrentUserId();
        return ApiResponse.success(runScanTask(userId, "ASSET_SELECTION", request.getTimeframe(),
                () -> assetPoolService.scanSelectedForUser(userId, request.getSymbols(), request.getTimeframe())));
    }

    private List<AssetPoolScanResultDTO> runScanTask(Long userId, String resourceType, String resourceId,
                                                     java.util.function.Supplier<List<AssetPoolScanResultDTO>> scan) {
        AsyncTaskDO task = asyncTaskService.queueForUser(userId, "POOL_SCAN", resourceType, resourceId, null);
        asyncTaskService.markRunning(task, "SCANNING");
        try {
            List<AssetPoolScanResultDTO> result = scan.get();
            boolean partial = result == null || result.stream()
                    .anyMatch(row -> row == null || !"SUCCESS".equalsIgnoreCase(row.status()));
            asyncTaskService.complete(task, partial, "COMPLETE");
            return result == null ? List.of() : result;
        } catch (RuntimeException exception) {
            asyncTaskService.fail(task, "POOL_SCAN_FAILED", exception.getMessage());
            throw exception;
        }
    }
}
