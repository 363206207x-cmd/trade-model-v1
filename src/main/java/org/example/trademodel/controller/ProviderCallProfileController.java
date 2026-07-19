package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.profile.ProviderBaseProfileResponse;
import org.example.trademodel.providercall.profile.ProviderBaseProfileUpdateRequest;
import org.example.trademodel.providercall.profile.ProviderCallProfilePreferenceService;
import org.example.trademodel.providercall.profile.ProviderCallProfilePreferenceServiceImpl;
import org.example.trademodel.providercall.profile.ProviderCallRuntimeStatus;
import org.example.trademodel.providercall.profile.ProviderCallRuntimeStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/provider-call")
public class ProviderCallProfileController {
    private final ProviderCallProfilePreferenceService preferenceService;
    private final ProviderCallRuntimeStatusService runtimeStatusService;

    public ProviderCallProfileController(ProviderCallProfilePreferenceService preferenceService,
                                         ProviderCallRuntimeStatusService runtimeStatusService) {
        this.preferenceService = preferenceService;
        this.runtimeStatusService = runtimeStatusService;
    }

    @GetMapping("/base-profile")
    public ResponseEntity<ApiResponse<ProviderBaseProfileResponse>> baseProfile(Authentication authentication) {
        if (!authenticated(authentication)) return forbidden();
        UserScanProfile profile = preferenceService.getBaseProfile();
        return ResponseEntity.ok(ApiResponse.success(new ProviderBaseProfileResponse(profile,
                ProviderCallProfilePreferenceServiceImpl.label(profile),
                "EXISTING_USER_CONFIG_OWNER", null)));
    }

    @PutMapping("/base-profile")
    public ResponseEntity<ApiResponse<ProviderBaseProfileResponse>> updateBaseProfile(
            Authentication authentication,
            @RequestBody(required = false) ProviderBaseProfileUpdateRequest request) {
        if (!authenticated(authentication)) return forbidden();
        if (request == null || request.profile() == null || request.profile().isBlank()) {
            return badRequest("请选择调用基础档位");
        }
        UserScanProfile profile;
        try {
            profile = UserScanProfile.valueOf(request.profile().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return badRequest("调用基础档位只支持自动、低频、标准或高频，不允许手动设置紧急档位");
        }
        try {
            ProviderCallProfilePreferenceService.ProfilePreferenceChange change =
                    preferenceService.setBaseProfile(profile, authentication.getName(), request.reason());
            return ResponseEntity.ok(ApiResponse.success(new ProviderBaseProfileResponse(
                    change.currentProfile(), change.currentProfileLabel(),
                    change.persistenceStatus(), change.changedAt())));
        } catch (IllegalArgumentException invalid) {
            return badRequest(invalid.getMessage());
        } catch (RuntimeException failure) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("档位保存失败，请稍后重试"));
        }
    }

    @GetMapping("/runtime-status")
    public ResponseEntity<ApiResponse<ProviderCallRuntimeStatus>> runtimeStatus(Authentication authentication) {
        if (!authenticated(authentication)) return forbidden();
        return ResponseEntity.ok(ApiResponse.success(runtimeStatusService.currentStatus()));
    }

    private static boolean authenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !authentication.getName().isBlank();
    }

    private static <T> ResponseEntity<ApiResponse<T>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("需要管理员认证"));
    }

    private static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(message));
    }
}
