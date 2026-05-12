package org.example.trademodel.service;

import org.example.trademodel.dto.PushWatchlistConfigRequest;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.vo.PushWatchlistConfigAuditVO;
import org.example.trademodel.vo.PushWatchlistConfigVO;

import java.util.List;
import java.util.Map;

public interface RuleConfigService {
    Map<String, RuleConfigDO> getRuleConfigMap();
    void reloadRules();

    default PushWatchlistConfigVO getPushWatchlistConfig() {
        throw new UnsupportedOperationException("push watchlist config is not supported");
    }

    default PushWatchlistConfigVO updatePushWatchlistConfig(PushWatchlistConfigRequest request) {
        throw new UnsupportedOperationException("push watchlist config is not supported");
    }

    default List<PushWatchlistConfigAuditVO> listPushWatchlistConfigAudit(int limit) {
        throw new UnsupportedOperationException("push watchlist config audit is not supported");
    }
}
