package org.example.trademodel.service;

/** Read-only owner for the Positions workspace projections. */
public interface PositionMonitoringReadService {
    PositionMonitoringProjectionService.CollectionProjection listForUser(Long userId);

    PositionMonitoringProjectionService.ItemProjection findForUser(Long userId, Long positionId);

    PositionMonitoringProjectionService.HistoryProjection historyForUser(Long userId, int limit);
}
