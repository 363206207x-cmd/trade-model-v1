package org.example.trademodel.service.watchlistscan;

import java.util.List;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionDTO;

public interface OpportunityPushDeliveryPolicy {

    OpportunityPushDeliveryDecisionDTO evaluate(
            String symbol,
            OpportunityPushDTO opportunityPush,
            List<String> riskGuardReasons
    );
}
