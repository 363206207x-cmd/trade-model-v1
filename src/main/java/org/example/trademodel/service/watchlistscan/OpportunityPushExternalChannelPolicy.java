package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.OpportunityPushExternalChannelDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushProviderChannelDTO;

public interface OpportunityPushExternalChannelPolicy {

    OpportunityPushExternalChannelDTO evaluate(
            String symbol,
            OpportunityPushProviderChannelDTO providerChannel
    );
}
