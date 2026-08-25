package org.example.trademodel.controller;

import org.example.trademodel.dto.assetpool.AssetAnalysisPreviewDTO;
import org.example.trademodel.entity.AsyncTaskDO;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.AsyncTaskService;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetPoolControllerTaskSemanticsTest {
    @Mock AssetPoolService assetPoolService;
    @Mock AuthenticatedUserIdResolver userIdResolver;
    @Mock AsyncTaskService asyncTaskService;
    @Mock ProviderCapabilityRegistry providerCapabilityRegistry;

    private AssetPoolController controller;
    private AsyncTaskDO task;

    @BeforeEach
    void setUp() {
        controller = new AssetPoolController(
                assetPoolService, userIdResolver, asyncTaskService, providerCapabilityRegistry);
        task = new AsyncTaskDO();
        task.setTaskId("task-preview-1");
        task.setOwnerId(41L);
        when(userIdResolver.requireCurrentUserId()).thenReturn(41L);
        when(asyncTaskService.queueForUser(41L, "ANALYSIS_PREVIEW", "ASSET", "BTCUSDT", null))
                .thenReturn(task);
    }

    @Test
    void authoritativeDataFailureIsTerminalFailedWithUserReadableMessage() {
        AssetAnalysisPreviewDTO result = preview("FAILED", "AUTHORITATIVE_OHLCV_UNAVAILABLE");
        when(assetPoolService.analyzePreviewForUser(41L, "BTCUSDT", "5m")).thenReturn(result);

        controller.analyzePreview("BTCUSDT", "5m");

        verify(asyncTaskService).fail(task, "AUTHORITATIVE_OHLCV_UNAVAILABLE",
                "可信市场数据尚未就绪，分析未完成");
        verify(asyncTaskService, never()).complete(task, true, "COMPLETE");
        verify(asyncTaskService, never()).complete(task, false, "COMPLETE");
    }

    @Test
    void executedPreviewCompletesWithoutPartialState() {
        AssetAnalysisPreviewDTO result = preview("EXECUTED", "ANALYSIS_EXECUTED");
        when(assetPoolService.analyzePreviewForUser(41L, "BTCUSDT", "5m")).thenReturn(result);

        controller.analyzePreview("BTCUSDT", "5m");

        verify(asyncTaskService).complete(task, false, "COMPLETE");
        verify(asyncTaskService, never()).fail(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void thrownAuthoritativeFailureDoesNotPersistTheRawTechnicalMessage() {
        when(assetPoolService.analyzePreviewForUser(41L, "BTCUSDT", "5m"))
                .thenThrow(new IllegalStateException("AUTHORITATIVE_OHLCV_UNAVAILABLE:5m"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> controller.analyzePreview("BTCUSDT", "5m"))
                .isInstanceOf(IllegalStateException.class);

        verify(asyncTaskService).fail(task, "AUTHORITATIVE_OHLCV_UNAVAILABLE",
                "可信市场数据尚未就绪，分析未完成");
    }

    private static AssetAnalysisPreviewDTO preview(String status, String reasonCode) {
        return new AssetAnalysisPreviewDTO(
                "BTCUSDT", "5m", "analysis-1", "trace-1", status, reasonCode,
                true, false, false, false, false, null);
    }
}
