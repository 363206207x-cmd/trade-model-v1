package org.example.trademodel.controller;

import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.profile.ProviderBaseProfileUpdateRequest;
import org.example.trademodel.providercall.profile.ProviderCallProfilePreferenceService;
import org.example.trademodel.providercall.profile.ProviderCallRuntimeStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderCallProfileControllerTest {
    @Mock
    private ProviderCallProfilePreferenceService preferenceService;
    @Mock
    private ProviderCallRuntimeStatusService runtimeStatusService;
    @Mock
    private Authentication authentication;

    @Test
    void authenticatedAdministratorCanChangeBaseProfileWithoutRuntimeRefresh() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(preferenceService.setBaseProfile(UserScanProfile.HIGH, "admin", "人工提高扫描档位"))
                .thenReturn(new ProviderCallProfilePreferenceService.ProfilePreferenceChange(
                        UserScanProfile.STANDARD, UserScanProfile.HIGH, "高频", "admin",
                        "人工提高扫描档位", Instant.parse("2026-07-19T10:00:00Z"),
                        "EXISTING_USER_CONFIG_OWNER"));
        ProviderCallProfileController controller = controller();

        var response = controller.updateBaseProfile(authentication,
                new ProviderBaseProfileUpdateRequest("HIGH", "人工提高扫描档位"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().profile()).isEqualTo(UserScanProfile.HIGH);
        verify(preferenceService).setBaseProfile(UserScanProfile.HIGH, "admin", "人工提高扫描档位");
        verifyNoInteractions(runtimeStatusService);
    }

    @Test
    void unauthenticatedUserCannotReadOrChangeProfile() {
        when(authentication.isAuthenticated()).thenReturn(false);
        ProviderCallProfileController controller = controller();

        assertThat(controller.baseProfile(authentication).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.updateBaseProfile(authentication,
                new ProviderBaseProfileUpdateRequest("LOW", "test")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(preferenceService, runtimeStatusService);
    }

    @Test
    void userCannotSelectEmergencyProfile() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        ProviderCallProfileController controller = controller();

        var response = controller.updateBaseProfile(authentication,
                new ProviderBaseProfileUpdateRequest("EMERGENCY", "人工设置紧急档位"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMsg()).contains("不允许手动设置紧急档位");
        verify(preferenceService, never()).setBaseProfile(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRequiresChineseReasonContract() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin");
        when(preferenceService.setBaseProfile(UserScanProfile.LOW, "admin", ""))
                .thenThrow(new IllegalArgumentException("请填写调整原因"));
        ProviderCallProfileController controller = controller();

        var response = controller.updateBaseProfile(authentication,
                new ProviderBaseProfileUpdateRequest("LOW", ""));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMsg()).isEqualTo("请填写调整原因");
    }

    private ProviderCallProfileController controller() {
        return new ProviderCallProfileController(preferenceService, runtimeStatusService);
    }
}
