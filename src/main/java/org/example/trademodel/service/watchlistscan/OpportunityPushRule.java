package org.example.trademodel.service.watchlistscan;

import java.util.List;
import org.example.trademodel.dto.watchlistscan.CandidateAttentionDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDTO;

public interface OpportunityPushRule {

    OpportunityPushDTO evaluate(
            String symbol,
            CandidateAttentionDTO candidateAttention,
            List<String> riskGuardReasons
    );
}
