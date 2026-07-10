package org.example.trademodel.providercall.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.trademodel.providercall.UserScanProfile;

import java.time.Instant;

public record ScanProfileUpdateRequest(
        @NotNull UserScanProfile baseProfile,
        @NotNull UserScanProfile positionMonitorProfile,
        @NotNull UserScanProfile poolProfile,
        @NotNull Boolean autoEscalationEnabled,
        Instant manualOverrideUntil,
        @NotBlank @Size(max = 500) String updateReason
) {
}
