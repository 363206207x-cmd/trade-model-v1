package org.example.trademodel.service;

import java.util.Map;

public interface DashboardReadService {
    Map<String, Object> overview();

    Map<String, Object> analysisStatus();

    Map<String, Object> schedulerStatus();

    Map<String, Object> traceSummary(Long userId, String analysisId, String traceId, String requestId);
}
