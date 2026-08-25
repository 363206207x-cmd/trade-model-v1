package org.example.trademodel.service;

import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.vo.DecisionBundleVO;

import java.time.LocalDate;
import java.util.List;

/**
 * Missed Opportunity 最小读写；权威分析主链在满足 {@link #recordFromAuthoritativeAnalysisIfEligible} 规则时可写入。
 */
public interface MissedOpportunityService {

    /**
     * 主链幂等写入：满足最小 missed 规则且本 decision 尚未写过 {@code tm_missed_opportunity} 时插入一行。
     *
     * @param hotResetWouldFire 与 Hot Reset 主链同一布尔（true 时不记 missed）
     */
    void recordFromAuthoritativeAnalysisIfEligible(String analysisId, String symbol, String traceId,
                                                   DecisionBundleVO decision, boolean hotResetWouldFire);

    void save(MissedOpportunityDO row);

    MissedOpportunityDO findByMissedId(String missedId);

    MissedOpportunityDO findByMissedIdForUser(Long userId, String missedId);

    List<MissedOpportunityDO> listByDecisionId(String decisionId);

    List<MissedOpportunityDO> listBySymbol(String symbol, int limit);

    List<MissedOpportunityDO> listByAnalysisId(String analysisId);

    List<MissedOpportunityDO> listByBizDate(LocalDate bizDate, int limit);

    List<MissedOpportunityDO> query(String analysisId, String symbol, LocalDate bizDate, int limit);

    List<MissedOpportunityDO> queryForUser(Long userId, String analysisId, String symbol,
                                           LocalDate bizDate, int limit);

    int countByBizDate(LocalDate bizDate);

    int countByBizDateForUser(Long userId, LocalDate bizDate);
}
