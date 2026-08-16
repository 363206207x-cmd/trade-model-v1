package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.UserConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/user-config", "/api/user-config"})
public class UserConfigController {
    private final UserConfigService userConfigService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public UserConfigController(UserConfigService userConfigService,
                                AuthenticatedUserIdResolver userIdResolver) {
        this.userConfigService = userConfigService;
        this.userIdResolver = userIdResolver;
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("user config controller ok");
    }

    @GetMapping
    public ApiResponse<SettingsView> current() {
        String userId = String.valueOf(userIdResolver.requireCurrentUserId());
        UserConfigDO config = userConfigService.getUserConfig(userId);
        if (config == null) {
            config = defaults(userId);
        }
        return ApiResponse.success(view(config));
    }

    @PutMapping
    public ApiResponse<SettingsView> update(@RequestBody SettingsRequest request) {
        String userId = String.valueOf(userIdResolver.requireCurrentUserId());
        UserConfigDO config = userConfigService.getUserConfig(userId);
        if (config == null) {
            config = defaults(userId);
        }
        if (hasText(request.riskPreference())) {
            config.setRiskPreference(request.riskPreference().trim());
        }
        if (request.notificationFiltersJson() != null) {
            config.setNotificationFiltersJson(request.notificationFiltersJson().trim());
        }
        if (hasText(request.defaultPoolMode())) {
            String poolMode = request.defaultPoolMode().trim().toUpperCase();
            if (!"SYSTEM_DEFAULT".equals(poolMode) && !"USER_CUSTOM".equals(poolMode)) {
                throw new IllegalArgumentException("unsupported defaultPoolMode");
            }
            config.setDefaultPoolMode(poolMode);
        }
        userConfigService.saveUserConfig(config);
        return ApiResponse.success(view(config));
    }

    private UserConfigDO defaults(String userId) {
        UserConfigDO config = new UserConfigDO();
        config.setUserId(userId);
        config.setRiskPreference("BALANCED");
        config.setAiModelPreference("DEFAULT");
        config.setNotifyChannels("IN_APP");
        config.setCooldownMinutes(15);
        config.setScanBaseProfile("AUTO");
        config.setScanPositionProfile("AUTO");
        config.setScanPoolProfile("AUTO");
        config.setScanAutoEscalationEnabled(true);
        config.setTelegramBindingStatus("UNBOUND");
        config.setDefaultPoolMode("SYSTEM_DEFAULT");
        return config;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private SettingsView view(UserConfigDO config) {
        return new SettingsView(config.getRiskPreference(), config.getNotificationFiltersJson(),
                config.getDefaultPoolMode(), config.getTelegramBindingStatus(),
                config.getCooldownMinutes(), config.getNotifyChannels());
    }

    public record SettingsRequest(String riskPreference, String notificationFiltersJson,
                                  String defaultPoolMode) {
    }

    public record SettingsView(String riskPreference, String notificationFiltersJson,
                               String defaultPoolMode, String telegramBindingStatus,
                               Integer cooldownMinutes, String notifyChannels) {
    }
}
