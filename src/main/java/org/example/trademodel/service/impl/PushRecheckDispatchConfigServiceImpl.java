package org.example.trademodel.service.impl;

import org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO;
import org.example.trademodel.entity.PushRecheckDispatchConfigDO;
import org.example.trademodel.mapper.PushRecheckDispatchConfigAuditMapper;
import org.example.trademodel.mapper.PushRecheckDispatchConfigMapper;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PushRecheckDispatchConfigServiceImpl implements PushRecheckDispatchConfigService {

    private static final String KEY_LIMIT = "limit";
    private static final String KEY_MAX_ATTEMPTS = "maxAttempts";
    private static final String KEY_MIN_RETRY_MINUTES = "minRetryMinutes";

    private final PushRecheckDispatchConfigMapper configMapper;
    private final PushRecheckDispatchConfigAuditMapper auditMapper;

    public PushRecheckDispatchConfigServiceImpl(PushRecheckDispatchConfigMapper configMapper,
                                                PushRecheckDispatchConfigAuditMapper auditMapper) {
        this.configMapper = configMapper;
        this.auditMapper = auditMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> loadOrInit(int defaultLimit, int defaultMaxAttempts, int defaultMinRetryMinutes) {
        Map<String, Integer> config = currentAsMap();
        upsertIfMissing(config, KEY_LIMIT, defaultLimit, "system", "BOOTSTRAP");
        upsertIfMissing(config, KEY_MAX_ATTEMPTS, defaultMaxAttempts, "system", "BOOTSTRAP");
        upsertIfMissing(config, KEY_MIN_RETRY_MINUTES, defaultMinRetryMinutes, "system", "BOOTSTRAP");
        return currentAsMap();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> updateConfig(Integer limit,
                                             Integer maxAttempts,
                                             Integer minRetryMinutes,
                                             String changedBy,
                                             String changeSource) {
        Map<String, Integer> current = currentAsMap();
        Integer currentLimit = current.get(KEY_LIMIT);
        Integer currentMaxAttempts = current.get(KEY_MAX_ATTEMPTS);
        Integer currentMinRetryMinutes = current.get(KEY_MIN_RETRY_MINUTES);

        Integer nextLimit = normalizePositiveOrCurrent(limit, currentLimit);
        Integer nextMaxAttempts = normalizePositiveOrCurrent(maxAttempts, currentMaxAttempts);
        Integer nextMinRetryMinutes = normalizePositiveOrCurrent(minRetryMinutes, currentMinRetryMinutes);

        LocalDateTime now = LocalDateTime.now();
        String operator = normalizeActor(changedBy);
        String source = normalizeSource(changeSource);
        updateOne(KEY_LIMIT, currentLimit, nextLimit, operator, source, now);
        updateOne(KEY_MAX_ATTEMPTS, currentMaxAttempts, nextMaxAttempts, operator, source, now);
        updateOne(KEY_MIN_RETRY_MINUTES, currentMinRetryMinutes, nextMinRetryMinutes, operator, source, now);
        return currentAsMap();
    }

    @Override
    public Map<String, Integer> getCurrentConfig() {
        return currentAsMap();
    }

    @Override
    public List<PushRecheckDispatchConfigAuditDO> listRecentAudit(int limit) {
        int safeLimit = limit > 0 ? Math.min(limit, 200) : 50;
        return auditMapper.selectRecent(safeLimit);
    }

    private void upsertIfMissing(Map<String, Integer> current,
                                 String key,
                                 int defaultValue,
                                 String updatedBy,
                                 String updateSource) {
        if (current.get(key) != null) {
            return;
        }
        PushRecheckDispatchConfigDO row = new PushRecheckDispatchConfigDO();
        row.setConfigKey(key);
        row.setConfigValue(defaultValue);
        row.setUpdatedBy(updatedBy);
        row.setUpdateSource(updateSource);
        row.setUpdateTime(LocalDateTime.now());
        configMapper.insert(row);
    }

    private void updateOne(String key,
                           Integer oldValue,
                           Integer newValue,
                           String operator,
                           String source,
                           LocalDateTime now) {
        if (newValue == null || Objects.equals(oldValue, newValue)) {
            return;
        }
        configMapper.updateValue(key, newValue, operator, source, now);
        PushRecheckDispatchConfigAuditDO audit = new PushRecheckDispatchConfigAuditDO();
        audit.setConfigKey(key);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setChangedBy(operator);
        audit.setChangeSource(source);
        audit.setCreateTime(now);
        auditMapper.insert(audit);
    }

    private Map<String, Integer> currentAsMap() {
        List<PushRecheckDispatchConfigDO> rows = configMapper.selectAll();
        Map<String, Integer> map = rows == null
                ? new LinkedHashMap<>()
                : rows.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        PushRecheckDispatchConfigDO::getConfigKey,
                        PushRecheckDispatchConfigDO::getConfigValue,
                        (a, b) -> b,
                        LinkedHashMap::new));
        map.putIfAbsent(KEY_LIMIT, null);
        map.putIfAbsent(KEY_MAX_ATTEMPTS, null);
        map.putIfAbsent(KEY_MIN_RETRY_MINUTES, null);
        return map;
    }

    private static Integer normalizePositiveOrCurrent(Integer candidate, Integer current) {
        if (candidate == null || candidate <= 0) {
            return current;
        }
        return candidate;
    }

    private static String normalizeActor(String changedBy) {
        if (changedBy == null || changedBy.isBlank()) {
            return "system";
        }
        return changedBy.trim();
    }

    private static String normalizeSource(String changeSource) {
        if (changeSource == null || changeSource.isBlank()) {
            return "UNKNOWN";
        }
        return changeSource.trim();
    }
}
