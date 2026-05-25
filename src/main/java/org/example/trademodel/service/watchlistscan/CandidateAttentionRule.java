package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.CandidateAttentionDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;

public interface CandidateAttentionRule {

    CandidateAttentionDTO evaluate(String symbol, WatchlistScanScoreDTO score);
}
