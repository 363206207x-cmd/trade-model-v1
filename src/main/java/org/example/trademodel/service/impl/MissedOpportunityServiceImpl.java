package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.service.MissedOpportunityService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MissedOpportunityServiceImpl implements MissedOpportunityService {

    private static final String MISSED_RULE_VERSION = "missed-v1";
    private static final ObjectMapper REASON_JSON = new ObjectMapper();

    private final MissedOpportunityMapper missedOpportunityMapper;

    public MissedOpportunityServiceImpl(MissedOpportunityMapper missedOpportunityMapper) {
        this.missedOpportunityMapper = missedOpportunityMapper;
    }

    @Override
    public void recordFromAuthoritativeAnalysisIfEligible(String analysisId, String symbol, String traceId,
                                                          DecisionBundleVO decision, boolean hotResetWouldFire) {
        // P1-4 freezes this legacy write path. Authoritative opportunity outcomes are
        // recorded by OpportunityLogService after persisted analysis facts exist.
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String buildReasonJsonV1(DecisionBundleVO decision, String analysisId, String decisionId, String symbol,
                                     boolean hotResetWouldFire) {
        ObjectNode root = REASON_JSON.createObjectNode();
        root.put("version", "1");
        root.put("rule", "WORTH_OPENING_NO_OPEN_POSITION_NOT_INVALIDATED_NOT_HOT_RESET");
        root.put("whyMissed",
                "Worth opening and no open position on symbol, but trade not executed in this system scope (minimal rule v1).");
        ObjectNode facts = REASON_JSON.createObjectNode();
        facts.put("isWorthOpening", Boolean.TRUE.equals(decision.getIsWorthOpening()));
        facts.put("assetState", decision.getAssetState() != null ? decision.getAssetState().name() : "");
        facts.put("confusedScore", decision.getConfusedScore() != null ? decision.getConfusedScore() : 0);
        facts.put("multiTimeframeAligned", decision.isMultiTimeframeAligned());
        facts.put("hotResetWouldFire", hotResetWouldFire);
        facts.put("openPositionCountForSymbol", 0);
        root.set("facts", facts);
        ObjectNode refs = REASON_JSON.createObjectNode();
        refs.put("analysisId", analysisId);
        refs.put("decisionId", decisionId);
        refs.put("symbol", symbol != null ? symbol.trim() : "");
        root.set("refs", refs);
        try {
            return REASON_JSON.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"version\":\"1\",\"rule\":\"serialization_failed\",\"whyMissed\":\"\",\"facts\":{},\"refs\":{}}";
        }
    }

    @Override
    public void save(MissedOpportunityDO row) {
        if (row.getCreateTime() == null) {
            row.setCreateTime(LocalDateTime.now());
        }
        missedOpportunityMapper.insert(row);
    }

    @Override
    public MissedOpportunityDO findByMissedId(String missedId) {
        return missedOpportunityMapper.selectByMissedId(missedId);
    }

    @Override
    public MissedOpportunityDO findByMissedIdForUser(Long userId, String missedId) {
        return missedOpportunityMapper.selectByMissedIdForUser(
                trimToNull(missedId), requireUserId(userId));
    }

    @Override
    public List<MissedOpportunityDO> listByDecisionId(String decisionId) {
        return missedOpportunityMapper.listByDecisionId(decisionId);
    }

    @Override
    public List<MissedOpportunityDO> listBySymbol(String symbol, int limit) {
        return missedOpportunityMapper.listBySymbol(symbol, limit);
    }

    @Override
    public List<MissedOpportunityDO> listByAnalysisId(String analysisId) {
        return missedOpportunityMapper.listByAnalysisId(analysisId);
    }

    @Override
    public List<MissedOpportunityDO> listByBizDate(LocalDate bizDate, int limit) {
        return missedOpportunityMapper.listByBizDate(bizDate, sanitizeLimit(limit));
    }

    @Override
    public List<MissedOpportunityDO> query(String analysisId, String symbol, LocalDate bizDate, int limit) {
        return missedOpportunityMapper.listByQuery(trimToNull(analysisId), normalizeSymbol(symbol), bizDate, sanitizeLimit(limit));
    }

    @Override
    public List<MissedOpportunityDO> queryForUser(Long userId, String analysisId, String symbol,
                                                  LocalDate bizDate, int limit) {
        return missedOpportunityMapper.listByQueryForUser(
                requireUserId(userId), trimToNull(analysisId), normalizeSymbol(symbol),
                bizDate, sanitizeLimit(limit));
    }

    @Override
    public int countByBizDate(LocalDate bizDate) {
        return missedOpportunityMapper.countByBizDate(bizDate);
    }

    @Override
    public int countByBizDateForUser(Long userId, LocalDate bizDate) {
        return missedOpportunityMapper.countByBizDateForUser(requireUserId(userId), bizDate);
    }

    private static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 200);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId;
    }
}
