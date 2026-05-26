package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushProviderChannelDTO;

public interface OpportunityPushProviderChannelPolicy {

    OpportunityPushProviderChannelDTO evaluate(
            String symbol,
            OpportunityPushMessageEnvelopeDTO messageEnvelope
    );
}
