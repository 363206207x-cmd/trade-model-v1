package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanBatchResultDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.AssetAnalysisPreviewDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.dto.req.AddAssetPoolItemReq;
import org.example.trademodel.dto.req.AssetPoolBatchReq;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
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
    private final ProviderCapabilityRegistry providerCapabilityRegistry;

    public AssetPoolController(AssetPoolService assetPoolService,
                               AuthenticatedUserIdResolver userIdResolver,
                               AsyncTaskService asyncTaskService,
                               ProviderCapabilityRegistry providerCapabilityRegistry) {
        this.assetPoolService = assetPoolService;
        this.userIdResolver = userIdResolver;
        this.asyncTaskService = asyncTaskService;
        this.providerCapabilityRegistry = providerCapabilityRegistry;
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
            @RequestParam(defaultValue = "5m") String timeframe,
            @RequestParam(required = false) String submissionId) {
        Long userId = userIdResolver.requireCurrentUserId();
        String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedTimeframe = timeframe == null ? "5m" : timeframe.trim().toLowerCase(java.util.Locale.ROOT);
        String taskKey = "analysis-preview:" + userId + ":" + normalizedSymbol + ":" + normalizedTimeframe
                + (submissionId == null || submissionId.isBlank() ? "" : ":" + submissionId.trim());
        AsyncTaskDO task = asyncTaskService.queueIdempotentForUser(
                userId, "ANALYSIS_PREVIEW", "ASSET", normalizedSymbol + ":" + normalizedTimeframe,
                null, taskKey);
        if ("FAILED".equalsIgnoreCase(task.getState()) || "PARTIAL".equalsIgnoreCase(task.getState())) {
            task = asyncTaskService.retryForUser(userId, task.getTaskId());
        }
        if (!"QUEUED".equalsIgnoreCase(task.getState())) {
            return ApiResponse.success(existingTaskPreview(normalizedSymbol, normalizedTimeframe, task));
        }
        if (!asyncTaskService.claimForExecution(task, "ANALYSIS")) {
            return ApiResponse.success(existingTaskPreview(normalizedSymbol, normalizedTimeframe, task));
        }
        try {
            AssetAnalysisPreviewDTO result = assetPoolService.analyzePreviewForUser(userId, symbol, timeframe);
            if (result != null && result.analysisId() != null && !result.analysisId().isBlank()) {
                asyncTaskService.bindResultIdentity(task, result.analysisId(), result.traceId());
            }
            if (previewSucceeded(result)) {
                asyncTaskService.complete(task, false, "COMPLETE");
            } else if (previewAccepted(result)) {
                if (!terminalTask(task)) {
                    asyncTaskService.markRunning(task, previewStage(result));
                }
            } else {
                String reasonCode = result == null || result.reasonCode() == null
                        ? "ANALYSIS_PREVIEW_FAILED" : result.reasonCode();
                asyncTaskService.fail(task, reasonCode, previewFailureMessage(reasonCode));
            }
            String responseState = terminalTask(task) ? task.getState()
                    : previewSucceeded(result) ? "SUCCEEDED"
                    : previewAccepted(result) ? "RUNNING" : "FAILED";
            String responseStage = terminalTask(task) ? task.getStage()
                    : previewAccepted(result) ? previewStage(result) : task.getStage();
            return ApiResponse.success(result == null ? null
                    : result.withTask(task.getTaskId(), responseState, responseStage));
        } catch (RuntimeException exception) {
            String reasonCode = previewExceptionReason(exception);
            asyncTaskService.fail(task, reasonCode, previewFailureMessage(reasonCode));
            throw exception;
        }
    }

    private static AssetAnalysisPreviewDTO existingTaskPreview(String symbol,
                                                                String timeframe,
                                                                AsyncTaskDO task) {
        String state = task.getState() == null ? "RUNNING" : task.getState();
        String status = "SUCCEEDED".equalsIgnoreCase(state) ? "EXISTING_SUCCESS"
                : "FAILED".equalsIgnoreCase(state) ? "FAILED" : "QUEUED";
        return new AssetAnalysisPreviewDTO(
                symbol, timeframe, task.getResultResourceId(), task.getTraceId(), status,
                "EXISTING_TASK", true, false, false, false, false, null)
                .withTask(task.getTaskId(), state, task.getStage());
    }

    private static boolean terminalTask(AsyncTaskDO task) {
        if (task == null || task.getState() == null) return false;
        return switch (task.getState().trim().toUpperCase(java.util.Locale.ROOT)) {
            case "SUCCEEDED", "FAILED", "PARTIAL", "CANCELLED" -> true;
            default -> false;
        };
    }

    public ApiResponse<AssetAnalysisPreviewDTO> analyzePreview(String symbol, String timeframe) {
        return analyzePreview(symbol, timeframe, null);
    }

    private static boolean previewSucceeded(AssetAnalysisPreviewDTO result) {
        if (result == null || result.status() == null) return false;
        return switch (result.status().trim().toUpperCase(java.util.Locale.ROOT)) {
            case "EXECUTED", "RECOVERED_FAILED_EXECUTED",
                    "RECOVERED_EXPIRED_LEASE_EXECUTED", "EXISTING_SUCCESS" -> true;
            default -> false;
        };
    }

    private static boolean previewAccepted(AssetAnalysisPreviewDTO result) {
        if (result == null || result.status() == null || result.analysisId() == null) return false;
        return switch (result.status().trim().toUpperCase(java.util.Locale.ROOT)) {
            case "QUEUED", "CONCURRENT_TRIGGER_BLOCKED" -> true;
            default -> false;
        };
    }

    private static String previewStage(AssetAnalysisPreviewDTO result) {
        return "CONCURRENT_TRIGGER_BLOCKED".equalsIgnoreCase(result.status())
                ? "ANALYSIS_RUN_IN_PROGRESS" : "ANALYSIS_RUN_QUEUED";
    }

    private static String previewFailureMessage(String reasonCode) {
        String normalized = reasonCode == null
                ? "" : reasonCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalized.contains("AUTHORITATIVE_OHLCV")
                || normalized.contains("REAL_MARKET_ENVIRONMENT")) {
            return "可信市场数据尚未就绪，分析未完成";
        }
        return "分析未完成，请稍后重试";
    }

    private static String previewExceptionReason(RuntimeException exception) {
        String message = exception == null || exception.getMessage() == null
                ? "" : exception.getMessage().trim().toUpperCase(java.util.Locale.ROOT);
        if (message.contains("AUTHORITATIVE_OHLCV_UNAVAILABLE")) {
            return "AUTHORITATIVE_OHLCV_UNAVAILABLE";
        }
        if (message.contains("REAL_MARKET_ENVIRONMENT_REQUIRED")
                || message.contains("REAL_MARKET_PROVENANCE_INCOMPLETE")) {
            return "REAL_MARKET_ENVIRONMENT_UNAVAILABLE";
        }
        return "ANALYSIS_PREVIEW_FAILED";
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

    @PostMapping("/scan-summary")
    public ApiResponse<AssetPoolScanBatchResultDTO> scanSummary(
            @RequestParam(defaultValue = "5m") String timeframe) {
        Long userId = userIdResolver.requireCurrentUserId();
        return ApiResponse.success(assetPoolService.scanSummaryForUser(userId, timeframe));
    }

    @PostMapping("/batch-scan-summary")
    public ApiResponse<AssetPoolScanBatchResultDTO> batchScanSummary(@RequestBody AssetPoolBatchReq request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        Long userId = userIdResolver.requireCurrentUserId();
        return ApiResponse.success(assetPoolService.scanSelectedSummaryForUser(
                userId, request.getSymbols(), request.getTimeframe()));
    }

    @GetMapping("/capabilities/{symbol}")
    public ApiResponse<List<ProviderInstrumentCapability>> capabilities(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "5m") String timeframe) {
        userIdResolver.requireCurrentUserId();
        return ApiResponse.success(providerCapabilityRegistry.capabilities(symbol, timeframe));
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
