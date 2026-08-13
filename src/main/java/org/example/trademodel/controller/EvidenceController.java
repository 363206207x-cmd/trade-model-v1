package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @Autowired
    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping("/build")
    public ApiResponse<java.util.List<EvidenceItemVO>> buildEvidence(@RequestBody AssetAnalysisVO request) {
        return ApiResponse.conflict(
                "DIRECT_EVIDENCE_BUILD_DISABLED_USE_ANALYSIS_RUN_DECISION_CHAIN");
    }
}
