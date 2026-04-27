package org.example.trademodel.service;

import org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO;

import java.util.List;
import java.util.Map;

public interface PushRecheckDispatchConfigService {

    Map<String, Integer> loadOrInit(int defaultLimit, int defaultMaxAttempts, int defaultMinRetryMinutes);

    Map<String, Integer> updateConfig(Integer limit,
                                      Integer maxAttempts,
                                      Integer minRetryMinutes,
                                      String changedBy,
                                      String changeSource);

    Map<String, Integer> getCurrentConfig();

    List<PushRecheckDispatchConfigAuditDO> listRecentAudit(int limit);
}
