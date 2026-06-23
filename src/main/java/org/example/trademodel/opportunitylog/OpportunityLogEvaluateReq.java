package org.example.trademodel.opportunitylog;

import java.time.LocalDateTime;

public class OpportunityLogEvaluateReq {
    private LocalDateTime asOf;

    public LocalDateTime getAsOf() {
        return asOf;
    }

    public void setAsOf(LocalDateTime asOf) {
        this.asOf = asOf;
    }
}
