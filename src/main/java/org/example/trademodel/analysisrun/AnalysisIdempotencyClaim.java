package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;

public class AnalysisIdempotencyClaim {
    private final AnalysisIdempotencyClaimStatus status;
    private final AnalysisRunDO run;
    private final String reasonCode;
    private final String message;

    public AnalysisIdempotencyClaim(AnalysisIdempotencyClaimStatus status, AnalysisRunDO run,
                                    String reasonCode, String message) {
        this.status = status;
        this.run = run;
        this.reasonCode = reasonCode;
        this.message = message;
    }

    public AnalysisIdempotencyClaimStatus getStatus() { return status; }
    public AnalysisRunDO getRun() { return run; }
    public String getReasonCode() { return reasonCode; }
    public String getMessage() { return message; }
}
