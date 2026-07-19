package org.example.trademodel.providercall.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trade-model.provider-call.notification")
public class NotificationEligibilityProperties {
    private OpportunityScope opportunityScope = OpportunityScope.WATCHLIST_AND_DISCOVERY;

    public OpportunityScope getOpportunityScope() {
        return opportunityScope;
    }

    public void setOpportunityScope(OpportunityScope opportunityScope) {
        this.opportunityScope = opportunityScope == null
                ? OpportunityScope.WATCHLIST_AND_DISCOVERY : opportunityScope;
    }
}
