package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionDTO;

public interface OpportunityPushAuditEnvelopeAssembler {

    OpportunityPushAuditEnvelopeDTO assemble(
            String symbol,
            OpportunityPushDeliveryDecisionDTO deliveryDecision
    );
}
