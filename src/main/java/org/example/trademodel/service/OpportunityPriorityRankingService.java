package org.example.trademodel.service;

import org.example.trademodel.vo.HomeTopAssetProjection;

import java.util.List;

public interface OpportunityPriorityRankingService {
    List<HomeTopAssetProjection> rankForHome(Long userId, int limit);
}
