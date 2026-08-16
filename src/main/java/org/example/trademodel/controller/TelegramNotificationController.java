package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.ChannelDeliveryDO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.ChannelDeliveryService;
import org.example.trademodel.telegram.TelegramDeliveryStatus;
import org.example.trademodel.telegram.TelegramProperties;
import org.example.trademodel.telegram.TelegramReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/settings/notifications/telegram")
public class TelegramNotificationController {
    private final AuthenticatedUserIdResolver userIdResolver;
    private final TelegramProperties properties;
    private final TelegramReadinessService readinessService;
    private final ChannelDeliveryService deliveryService;

    public TelegramNotificationController(AuthenticatedUserIdResolver userIdResolver,
                                          TelegramProperties properties,
                                          TelegramReadinessService readinessService,
                                          ChannelDeliveryService deliveryService) {
        this.userIdResolver = userIdResolver;
        this.properties = properties;
        this.readinessService = readinessService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/status")
    public ApiResponse<TelegramStatusView> status() {
        Long userId = userIdResolver.requireCurrentUserId();
        ChannelDeliveryDO latest = deliveryService.latestTelegramForUser(userId);
        return ApiResponse.success(new TelegramStatusView(
                properties.isEnabled(),
                properties.configuredForExternalDelivery(),
                properties.isExternalCallsEnabled(),
                readinessService.state().name(),
                readinessService.botUsername(),
                readinessService.recipientConfigured(),
                latest == null ? null : latest.getStatus(),
                latest == null ? null : latest.getDeliveredAt(),
                latest == null ? null : latest.getErrorCode(),
                deliveryService.retryingCountForUser(userId)));
    }

    @PostMapping("/messages/{messageId}/retry")
    public ApiResponse<TelegramRetryView> retry(@PathVariable String messageId) {
        Long userId = userIdResolver.requireCurrentUserId();
        boolean requeued = deliveryService.requeueTelegramForMessage(userId, messageId);
        return ApiResponse.success(new TelegramRetryView(messageId, requeued,
                requeued ? TelegramDeliveryStatus.QUEUED.name() : "UNCHANGED"));
    }

    public record TelegramStatusView(boolean enabled,
                                     boolean configured,
                                     boolean externalCallsEnabled,
                                     String state,
                                     String botUsername,
                                     boolean recipientConfigured,
                                     String lastDeliveryState,
                                     LocalDateTime lastDeliveredAt,
                                     String lastErrorCode,
                                     int retryingCount) {
    }

    public record TelegramRetryView(String messageId, boolean requeued, String state) {
    }
}
