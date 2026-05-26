package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceResultDTO;

public interface OpportunityPushAuditEnvelopePersistencePort {

    OpportunityPushAuditPersistenceResultDTO evaluate(
            String symbol,
            OpportunityPushAuditEnvelopeDTO auditEnvelope
    );
}
