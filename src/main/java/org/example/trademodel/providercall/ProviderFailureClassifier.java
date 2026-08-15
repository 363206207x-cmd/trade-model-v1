package org.example.trademodel.providercall;

import java.util.Locale;
import java.util.Set;

/** Deterministic, value-free classification used before circuit and provider-health mutation. */
public final class ProviderFailureClassifier {
    private static final Set<String> LOCAL_ADMISSION_REASONS = Set.of(
            "PROVIDER_EXECUTOR_REJECTED",
            "PROVIDER_CALL_CANCELLED",
            "PROVIDER_EXECUTOR_QUEUE_TIMEOUT",
            "PROVIDER_PRE_REMOTE_TIMEOUT",
            "PROVIDER_EXECUTOR_QUEUE_FULL",
            "PROVIDER_EXECUTOR_PRIORITY_QUEUE_RESERVED",
            "PROVIDER_EXECUTOR_SHUTDOWN");
    private static final Set<String> LOCAL_BUDGET_REASONS = Set.of(
            "PROVIDER_BUDGET_REJECTED",
            "GLOBAL_REGULAR_BUDGET_EXHAUSTED",
            "GLOBAL_EMERGENCY_RESERVE_EXHAUSTED",
            "PER_SYMBOL_MINIMUM_GAP",
            "PROVIDER_SUSPENDED_RETRY_AFTER");
    private static final Set<String> LOCAL_CONFIGURATION_REASONS = Set.of(
            "PROVIDER_CALL_DISABLED",
            "PROVIDER_NOT_CONFIGURED",
            "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED",
            "PERPETUAL_PRICE_PROVIDER_NOT_CONFIGURED",
            "OHLCV_ADAPTER_EXTERNAL_CALLS_DISABLED",
            "PRICE_ADAPTER_EXTERNAL_CALLS_DISABLED",
            "DERIVATIVES_ADAPTER_EXTERNAL_CALLS_DISABLED",
            "EXTERNAL_CONTEXT_ADAPTER_EXTERNAL_CALLS_DISABLED",
            "AI_REVIEW_PROVIDER_NOT_CONFIGURED",
            "OHLCV_AUTHORITATIVE_WRITE_FAILED");

    private ProviderFailureClassifier() {
    }

    public static ProviderFailureOrigin classify(ProviderAdapterResponse<?> response) {
        if (response == null) return ProviderFailureOrigin.REMOTE_PAYLOAD;
        int status = response.httpStatus();
        String reason = canonicalReason(status, response.reasonCode());

        if (isRegionRestricted(status, reason)) return ProviderFailureOrigin.REMOTE_CAPABILITY;
        if (status == 429 || reason.contains("RATE_LIMIT")) {
            return ProviderFailureOrigin.REMOTE_RATE_LIMIT;
        }
        if (status == 401 || status == 403 || reason.contains("AUTHENTICATION_FAILED")
                || reason.contains("AUTH_ERROR")) {
            return ProviderFailureOrigin.REMOTE_AUTH;
        }
        if (status >= 500 && status <= 599) return ProviderFailureOrigin.REMOTE_SERVER;
        if (LOCAL_ADMISSION_REASONS.contains(reason)) return ProviderFailureOrigin.LOCAL_ADMISSION;
        if ("PROVIDER_CONCURRENCY_REJECTED".equals(reason)) {
            return ProviderFailureOrigin.LOCAL_CONCURRENCY;
        }
        if (LOCAL_BUDGET_REASONS.contains(reason)) return ProviderFailureOrigin.LOCAL_BUDGET;
        if (LOCAL_CONFIGURATION_REASONS.contains(reason)
                || response.sourceStatus() == UnifiedSourceStatus.DISABLED
                || response.sourceStatus() == UnifiedSourceStatus.NOT_CONFIGURED
                || reason.contains("NOT_CONFIGURED")
                || reason.contains("INVALID_PROVIDER_CONFIGURATION")) {
            return ProviderFailureOrigin.LOCAL_CONFIGURATION;
        }
        if ("PROVIDER_TIMEOUT".equals(reason) || "TIMEOUT".equals(reason)
                || "PROVIDER_CALL_FAILED".equals(reason)
                || reason.contains("TRANSPORT_FAILED") || reason.contains("UPSTREAM_UNAVAILABLE")) {
            return ProviderFailureOrigin.REMOTE_TRANSPORT;
        }
        if (status >= 400 && status <= 499) return ProviderFailureOrigin.LOCAL_CONFIGURATION;
        return ProviderFailureOrigin.REMOTE_PAYLOAD;
    }

    public static String canonicalReason(int httpStatus, String reason) {
        String normalized = normalize(reason);
        if (isRegionRestricted(httpStatus, normalized)) return "REGION_RESTRICTED";
        if (!normalized.isBlank()) return normalized;
        return httpStatus > 0 ? "HTTP_" + httpStatus : "PROVIDER_CALL_FAILED";
    }

    public static boolean isRegionRestricted(ProviderAdapterResponse<?> response) {
        return response != null && isRegionRestricted(response.httpStatus(), response.reasonCode());
    }

    public static boolean isRegionRestricted(int httpStatus, String reason) {
        String normalized = normalize(reason);
        return httpStatus == 451
                || normalized.contains("REGION_RESTRICTED")
                || normalized.contains("GEO_RESTRICTED")
                || normalized.contains("ELIGIBILITY_RESTRICTED")
                || normalized.contains("HTTP_451");
    }

    public static <T> ProviderAdapterResponse<T> httpFailure(
            int httpStatus, String reasonCode, Long retryAfterSeconds) {
        return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, httpStatus,
                canonicalReason(httpStatus, reasonCode), retryAfterSeconds);
    }

    private static String normalize(String reason) {
        return reason == null ? "" : reason.trim().toUpperCase(Locale.ROOT);
    }
}
