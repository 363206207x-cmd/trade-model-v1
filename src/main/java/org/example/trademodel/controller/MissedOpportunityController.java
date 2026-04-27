package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.service.MissedOpportunityService;
import org.example.trademodel.service.MissedReasonViewParser;
import org.example.trademodel.vo.MissedOpportunityQueryItemVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/missed-opportunity")
public class MissedOpportunityController {

    private final MissedOpportunityService missedOpportunityService;

    public MissedOpportunityController(MissedOpportunityService missedOpportunityService) {
        this.missedOpportunityService = missedOpportunityService;
    }

    @GetMapping("/query")
    public ApiResponse<List<MissedOpportunityQueryItemVO>> query(
            @RequestParam(required = false) String analysisId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bizDate,
            @RequestParam(required = false) String missedId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        if (missedId != null && !missedId.trim().isEmpty()) {
            MissedOpportunityDO row = missedOpportunityService.findByMissedId(missedId.trim());
            if (row == null) {
                return ApiResponse.success(Collections.emptyList());
            }
            return ApiResponse.success(List.of(toQueryItem(row)));
        }
        List<MissedOpportunityDO> rows = missedOpportunityService.query(analysisId, symbol, bizDate, safeLimit(limit));
        List<MissedOpportunityQueryItemVO> data = rows.stream()
                .map(MissedOpportunityController::toQueryItem)
                .collect(Collectors.toList());
        return ApiResponse.success(data);
    }

    private static int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 200);
    }

    private static MissedOpportunityQueryItemVO toQueryItem(MissedOpportunityDO row) {
        MissedOpportunityQueryItemVO vo = new MissedOpportunityQueryItemVO();
        vo.setMissedId(row.getMissedId());
        vo.setDecisionId(row.getDecisionId());
        vo.setAnalysisId(row.getAnalysisId());
        vo.setSymbol(row.getSymbol());
        vo.setBizDate(row.getBizDate());
        vo.setReasonJson(row.getReasonJson());
        vo.setReasonView(MissedReasonViewParser.parse(row.getReasonJson()));
        vo.setRuleVersion(row.getRuleVersion());
        vo.setTraceId(row.getTraceId());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }
}
