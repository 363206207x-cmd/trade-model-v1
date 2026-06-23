package org.example.trademodel.ai;

import java.math.BigDecimal;

public class AiUsageGuardResult {
    private final boolean allowed;
    private final AiProviderCallStatus status;
    private final String reasonCode;
    private final BigDecimal reservedCostUsd;

    private AiUsageGuardResult(boolean allowed, AiProviderCallStatus status,
                               String reasonCode, BigDecimal reservedCostUsd) {
        this.allowed = allowed;
        this.status = status;
        this.reasonCode = reasonCode;
        this.reservedCostUsd = reservedCostUsd == null ? BigDecimal.ZERO : reservedCostUsd;
    }

    public static AiUsageGuardResult allowed(BigDecimal reservedCostUsd) {
        return new AiUsageGuardResult(true, AiProviderCallStatus.STARTED, "ALLOWED", reservedCostUsd);
    }

    public static AiUsageGuardResult blocked(AiProviderCallStatus status, String reasonCode,
                                             BigDecimal reservedCostUsd) {
        return new AiUsageGuardResult(false, status, reasonCode, reservedCostUsd);
    }

    public boolean isAllowed() { return allowed; }
    public AiProviderCallStatus getStatus() { return status; }
    public String getReasonCode() { return reasonCode; }
    public BigDecimal getReservedCostUsd() { return reservedCostUsd; }
}
