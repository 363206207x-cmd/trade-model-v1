package org.example.trademodel.analysistrace;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisTraceServiceImpl implements AnalysisTraceService {
    private final AnalysisRunMapper analysisRunMapper;

    public AnalysisTraceServiceImpl(AnalysisRunMapper analysisRunMapper) {
        this.analysisRunMapper = analysisRunMapper;
    }

    @Override
    public AnalysisTraceSnapshot byAnalysisId(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return null;
        }
        AnalysisRunDO run = analysisRunMapper.selectById(analysisId.trim());
        return run != null ? snapshot(run) : null;
    }

    @Override
    public AnalysisTraceSnapshot byTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return null;
        }
        AnalysisRunDO run = analysisRunMapper.selectByTraceId(traceId.trim());
        return run != null ? snapshot(run) : null;
    }

    @Override
    public AnalysisTraceSnapshot byRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        AnalysisRunDO run = analysisRunMapper.selectByRequestId(requestId.trim());
        return run != null ? snapshot(run) : null;
    }

    private AnalysisTraceSnapshot snapshot(AnalysisRunDO run) {
        String analysisId = run.getAnalysisId();
        String traceId = run.getTraceId();
        Integer pushCount = analysisRunMapper.countPushSnapshotsByAnalysisId(analysisId);
        return new AnalysisTraceSnapshot(
                run,
                safeList(analysisRunMapper.selectEvidenceIdsByAnalysisId(analysisId)),
                safeList(analysisRunMapper.selectScoreIdsByAnalysisId(analysisId)),
                safeList(analysisRunMapper.selectDecisionIdsByAnalysisId(analysisId)),
                safeList(analysisRunMapper.selectExecutionPlanIdsByAnalysisId(analysisId)),
                safeList(analysisRunMapper.selectPositionMonitorLogIdsByAnalysisId(analysisId)),
                safeList(analysisRunMapper.selectReviewResultIdsByAnalysisId(analysisId)),
                safeList(analysisRunMapper.selectAiCallIdsByTraceOrAnalysisId(traceId, analysisId)),
                safeList(analysisRunMapper.selectOpportunityIdsByAnalysisId(analysisId)),
                pushCount != null ? pushCount : 0);
    }

    private static List<String> safeList(List<String> rows) {
        return rows != null ? rows : List.of();
    }
}
