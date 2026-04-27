package org.example.trademodel.service;

import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import java.util.List;

public interface EvidenceService {
    List<EvidenceItemVO> buildEvidence(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv);

    List<EvidenceBriefVO> listTopEvidenceBriefByAnalysisId(String analysisId);
}
