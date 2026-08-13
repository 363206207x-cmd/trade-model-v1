package org.example.trademodel.service.support;

import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared fail-closed policy for the existing account-risk snapshot owner. */
public final class AccountRiskPlanPolicy {
    private static final Pattern FIRST_NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

    private AccountRiskPlanPolicy() {
    }

    public static Assessment assess(TmAccountRiskSnapshotDO snapshot,
                                    ExecutionPlanCandidateDO candidate,
                                    String finalRiskLevel,
                                    FundamentalAiV41Properties.AccountRisk limits,
                                    LocalDateTime now) {
        if (snapshot == null) {
            return blocked("ACCOUNT_RISK_SNAPSHOT_MISSING", "Account risk snapshot is required", null, null, null, null);
        }
        if (!"VERIFIED".equals(snapshot.getSourceStatus())) {
            return blocked("ACCOUNT_RISK_SOURCE_NOT_VERIFIED", "Account risk source is not verified",
                    null, null, null, limits == null ? null : limits.getMaxLeverage());
        }
        if (snapshot.getObservedAt() == null || snapshot.getFreshUntil() == null || now == null
                || now.isBefore(snapshot.getObservedAt()) || !now.isBefore(snapshot.getFreshUntil())) {
            return blocked("ACCOUNT_RISK_SNAPSHOT_STALE", "Account risk snapshot is outside its freshness window",
                    null, null, null, limits == null ? null : limits.getMaxLeverage());
        }
        if (!Boolean.TRUE.equals(snapshot.getRiskAllowed())) {
            return blocked(defaultText(snapshot.getRiskReasonCode(), "ACCOUNT_RISK_BLOCKED"),
                    defaultText(snapshot.getRiskReasonText(), "Current account risk blocks a plan"),
                    null, null, null, limits == null ? null : limits.getMaxLeverage());
        }
        if (limits == null) {
            return blocked("ACCOUNT_RISK_LIMITS_MISSING", "Account risk limits are unavailable", null, null, null, null);
        }
        BigDecimal maxExposure = limits.maxExposureFor(finalRiskLevel);
        BigDecimal maxLeverage = limits.getMaxLeverage();
        BigDecimal exposure = parseExposure(candidate == null ? null : candidate.getPositionSuggestion());
        BigDecimal leverage = parseLeverage(candidate == null ? null : candidate.getLeverageSuggestion());
        if (exposure == null) {
            return blocked("POSITION_EXPOSURE_UNAVAILABLE",
                    "Candidate position suggestion is not a parseable non-negative exposure", null, leverage,
                    maxExposure, maxLeverage);
        }
        if (leverage == null) {
            return blocked("CANDIDATE_LEVERAGE_UNAVAILABLE",
                    "Candidate leverage suggestion is not a parseable positive leverage", exposure, null,
                    maxExposure, maxLeverage);
        }
        if (maxExposure == null) {
            return blocked("MAX_EXPOSURE_LIMIT_UNAVAILABLE", "Risk-level exposure limit is unavailable",
                    exposure, leverage, null, maxLeverage);
        }
        if (maxLeverage == null) {
            return blocked("MAX_LEVERAGE_LIMIT_UNAVAILABLE", "Maximum leverage limit is unavailable",
                    exposure, leverage, maxExposure, null);
        }
        if (exposure.compareTo(maxExposure) > 0) {
            return blocked("EXPOSURE_LIMIT_EXCEEDED",
                    "positionExposure=" + exposure + " > maxAllowedExposure=" + maxExposure,
                    exposure, leverage, maxExposure, maxLeverage);
        }
        if (leverage.compareTo(maxLeverage) > 0) {
            return blocked("LEVERAGE_LIMIT_EXCEEDED",
                    "candidateLeverage=" + leverage + " > maxAllowedLeverage=" + maxLeverage,
                    exposure, leverage, maxExposure, maxLeverage);
        }
        return new Assessment(true, "ACCOUNT_RISK_ALLOWED",
                "Account risk, candidate exposure and leverage are within verified limits",
                exposure, leverage, maxExposure, maxLeverage);
    }

    public static BigDecimal parseExposure(String raw) {
        BigDecimal parsed = firstNumber(raw);
        if (parsed == null || parsed.signum() < 0) return null;
        if (raw != null && raw.contains("%")) return parsed.movePointLeft(2);
        return parsed.compareTo(BigDecimal.ONE) > 0 ? parsed.movePointLeft(2) : parsed;
    }

    public static BigDecimal parseLeverage(String raw) {
        BigDecimal parsed = firstNumber(raw);
        return parsed == null || parsed.signum() <= 0 ? null : parsed;
    }

    private static BigDecimal firstNumber(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Matcher matcher = FIRST_NUMBER.matcher(raw.trim());
        if (!matcher.find()) return null;
        try {
            return new BigDecimal(matcher.group());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Assessment blocked(String code, String text, BigDecimal exposure,
                                      BigDecimal leverage, BigDecimal maxExposure, BigDecimal maxLeverage) {
        return new Assessment(false, code, text, exposure, leverage, maxExposure, maxLeverage);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record Assessment(boolean allowed,
                             String reasonCode,
                             String reasonText,
                             BigDecimal positionExposure,
                             BigDecimal candidateLeverage,
                             BigDecimal maxAllowedExposure,
                             BigDecimal maxAllowedLeverage) {
    }
}
