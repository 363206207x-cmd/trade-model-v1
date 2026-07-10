package org.example.trademodel.providercall.scan;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.profile.ProfileTransitionResult;
import org.example.trademodel.providercall.profile.ProfileTransitionSignal;
import org.example.trademodel.providercall.profile.ScanProfileTransitionService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PushRecheckStatusContract;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.UserConfigService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.entity.UserConfigDO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DefaultProviderScanUniverseSource implements ProviderScanUniverseSource {
    private static final List<String> FALLBACK_CORE = List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");
    private static final Pattern SYMBOL = Pattern.compile("^[A-Z0-9]{3,20}USDT$");
    private static final String WATCHLIST_RULE_KEY = "push.watchlist.symbols";

    private final ProviderCallProperties properties;
    private final UserPositionMapper userPositionMapper;
    private final AssetStateMapper assetStateMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PositionMonitorLogService monitorLogService;
    private final UserConfigService userConfigService;
    private final RuleConfigService ruleConfigService;
    private final ScanProfileTransitionService transitionService;
    private final ProviderRefreshStateRegistry refreshRegistry;

    public DefaultProviderScanUniverseSource(ProviderCallProperties properties,
                                             UserPositionMapper userPositionMapper,
                                             AssetStateMapper assetStateMapper,
                                             DecisionResultMapper decisionResultMapper,
                                             PushSnapshotMapper pushSnapshotMapper,
                                             PositionMonitorLogService monitorLogService,
                                             UserConfigService userConfigService,
                                             RuleConfigService ruleConfigService,
                                             ScanProfileTransitionService transitionService,
                                             ProviderRefreshStateRegistry refreshRegistry) {
        this.properties = properties;
        this.userPositionMapper = userPositionMapper;
        this.assetStateMapper = assetStateMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.monitorLogService = monitorLogService;
        this.userConfigService = userConfigService;
        this.ruleConfigService = ruleConfigService;
        this.transitionService = transitionService;
        this.refreshRegistry = refreshRegistry;
    }

    @Override
    public ScanUniverseInput currentUniverse() {
        UserConfigDO config = safeUserConfig();
        UserScanProfile base = parseUserProfile(config == null ? null : config.getScanBaseProfile(), properties.getBaseProfile());
        RuntimeScanProfile positionProfile = runtime(parseUserProfile(
                config == null ? null : config.getScanPositionProfile(), UserScanProfile.LOW));
        RuntimeScanProfile poolProfile = runtime(parseUserProfile(
                config == null ? null : config.getScanPoolProfile(), UserScanProfile.LOW));
        boolean auto = config == null || config.getScanAutoEscalationEnabled() == null
                ? properties.isAutoEscalationEnabled() : config.getScanAutoEscalationEnabled();

        List<UserPositionDO> openPositions = safeOpenPositions();
        List<PositionScanAsset> positions = openPositions.stream()
                .filter(DefaultProviderScanUniverseSource::validPosition)
                .map(row -> new PositionScanAsset(normalize(row.getAssetSymbol()), row.getStatus())).toList();
        List<AssetStateDO> candidateRows = safeCandidates();
        Map<String, String> pushSignals = safePushEscalationSignals();
        List<String> candidates = boundedCandidates(candidateRows, pushSignals.keySet());

        Map<String, RuntimeScanProfile> escalations = new LinkedHashMap<>();
        Map<String, String> reasons = new LinkedHashMap<>();
        if (auto && properties.isProfileEscalationEnabled()) {
            Map<String, UserPositionDO> positionBySymbol = new LinkedHashMap<>();
            openPositions.forEach(row -> positionBySymbol.putIfAbsent(normalize(row.getAssetSymbol()), row));
            Map<String, AssetStateDO> stateBySymbol = new LinkedHashMap<>();
            candidateRows.forEach(row -> stateBySymbol.putIfAbsent(normalize(row.getSymbol()), row));
            Set<String> evaluated = new LinkedHashSet<>();
            evaluated.addAll(positionBySymbol.keySet());
            evaluated.addAll(stateBySymbol.keySet());
            evaluated.addAll(pushSignals.keySet());
            for (String symbol : evaluated) {
                ProfileTransitionResult transition = transitionService.evaluate(symbol, base,
                        signal(positionBySymbol.get(symbol), stateBySymbol.get(symbol), pushSignals.get(symbol)),
                        "provider-universe-" + UUID.randomUUID());
                escalations.put(symbol, transition.effectiveProfile());
                reasons.put(symbol, transition.effectiveReason());
            }
        }

        // Runtime events are symbol-scoped. A risk event on one asset must not raise every scanned asset.
        return new ScanUniverseInput(coreAssets(), positions, candidates, poolAssets(), base, RuntimeScanProfile.LOW,
                positionProfile, poolProfile, escalations, reasons, refreshRegistry.lastAttempts(), Instant.now());
    }

    private ProfileTransitionSignal signal(UserPositionDO position, AssetStateDO state, String pushSignal) {
        PositionMonitorLogDTO log = latestLog(position);
        String reason = log == null || log.getReason() == null ? "" : log.getReason().toUpperCase(Locale.ROOT);
        String logic = log == null || log.getLogicStatus() == null ? "" : log.getLogicStatus().toUpperCase(Locale.ROOT);
        DecisionResultVO decision = state == null ? null : safeDecision(state.getSymbol());
        boolean highRisk = "HIGH".equalsIgnoreCase(log == null ? null : log.getRiskLevel())
                || "HIGH_RISK".equals(logic) || "PLAN_INVALIDATED".equals(logic)
                || (state != null && (state.getState() == AssetStateEnum.HIGH_RISK
                || state.getState() == AssetStateEnum.INVALIDATED));
        return new ProfileTransitionSignal(null, null, null, null,
                reason.contains("NEAR_STOP_LOSS") ? BigDecimal.ZERO : null,
                reason.contains("NEAR_TAKE_PROFIT") ? BigDecimal.ZERO : null,
                null, null, null,
                reason.contains("EXTERNAL_CONTEXT_BLOCKED") || highRisk || pushSignal != null,
                state == null || state.getConfusedScore() == null ? null : BigDecimal.valueOf(state.getConfusedScore()),
                state != null && Boolean.TRUE.equals(state.getHotResetFlag()),
                highRisk || reason.contains("REVERS"),
                decision == null || decision.getDataQualityScore() == null ? null
                        : BigDecimal.valueOf(decision.getDataQualityScore()));
    }

    private PositionMonitorLogDTO latestLog(UserPositionDO position) {
        if (position == null || position.getId() == null) return null;
        try {
            List<PositionMonitorLogDTO> rows = monitorLogService.listByPositionId(position.getId(), 1);
            return rows == null || rows.isEmpty() ? null : rows.get(0);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private DecisionResultVO safeDecision(String symbol) {
        try { return decisionResultMapper.findLatestDecisionResultBySymbolJoined(normalize(symbol)); }
        catch (RuntimeException ignored) { return null; }
    }

    private UserConfigDO safeUserConfig() {
        try { return userConfigService.getUserConfig(properties.getScanUserId()); }
        catch (RuntimeException ignored) { return null; }
    }

    private List<UserPositionDO> safeOpenPositions() {
        try {
            List<UserPositionDO> rows = userPositionMapper.listOpenPositions();
            return rows == null ? List.of() : rows;
        } catch (RuntimeException ignored) { return List.of(); }
    }

    private List<AssetStateDO> safeCandidates() {
        try {
            List<AssetStateDO> rows = assetStateMapper.listCandidateOrWaitingTrigger(properties.getMaxCandidateAssets());
            return rows == null ? List.of() : rows;
        } catch (RuntimeException ignored) { return List.of(); }
    }

    private Map<String, String> safePushEscalationSignals() {
        try {
            int limit = Math.max(1, properties.getMaxCoreAssets()
                    + properties.getMaxCandidateAssets() + properties.getMaxPoolAssets());
            List<TmPushSnapshotDO> rows = pushSnapshotMapper.listRecent(limit);
            if (rows == null || rows.isEmpty()) return Map.of();
            Map<String, String> signals = new LinkedHashMap<>();
            for (TmPushSnapshotDO row : rows) {
                String symbol = row == null ? null : normalize(row.getSymbol());
                String status = row == null ? null
                        : PushRecheckStatusContract.canonicalizePushStatus(row.getPushStatus());
                if (validSymbol(symbol) && isPushEscalation(status)) signals.putIfAbsent(symbol, status);
            }
            return Map.copyOf(signals);
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private List<String> boundedCandidates(List<AssetStateDO> states, Set<String> pushSymbols) {
        List<String> combined = new ArrayList<>();
        if (states != null) states.stream().map(AssetStateDO::getSymbol).forEach(combined::add);
        if (pushSymbols != null) combined.addAll(pushSymbols);
        return sanitize(combined, properties.getMaxCandidateAssets());
    }

    private static boolean isPushEscalation(String status) {
        return PushRecheckStatusContract.PUSH_STATUS_DRIFTED_FROM_ENTRY_ZONE.equals(status)
                || PushRecheckStatusContract.PUSH_STATUS_INVALIDATED.equals(status)
                || PushRecheckStatusContract.PUSH_STATUS_RISK_BLOCKED.equals(status)
                || PushRecheckStatusContract.PUSH_STATUS_CONFUSED_BLOCKED.equals(status);
    }

    private List<String> coreAssets() {
        List<String> configured = sanitize(properties.getCoreAssets(), 6);
        return configured.size() == 6 ? configured : FALLBACK_CORE;
    }

    private List<String> poolAssets() {
        try {
            Map<String, RuleConfigDO> rules = ruleConfigService.getRuleConfigMap();
            RuleConfigDO row = rules == null ? null : rules.get(WATCHLIST_RULE_KEY);
            if (row == null || row.getRuleValue() == null) return List.of();
            return sanitize(List.of(row.getRuleValue().split(",")), properties.getMaxPoolAssets());
        } catch (RuntimeException ignored) { return List.of(); }
    }

    private static List<String> sanitize(List<String> values, int max) {
        if (values == null || max <= 0) return List.of();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (validSymbol(normalized) && !result.contains(normalized)) result.add(normalized);
            if (result.size() >= max) break;
        }
        return List.copyOf(result);
    }

    private static boolean validPosition(UserPositionDO row) {
        return row != null && validSymbol(normalize(row.getAssetSymbol()))
                && ("OPEN".equalsIgnoreCase(row.getStatus()) || "PARTIALLY_CLOSED".equalsIgnoreCase(row.getStatus()));
    }

    private static boolean validSymbol(String value) { return value != null && SYMBOL.matcher(value).matches(); }
    private static String normalize(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private static RuntimeScanProfile runtime(UserScanProfile value) {
        return value == null || value == UserScanProfile.AUTO ? RuntimeScanProfile.STANDARD
                : RuntimeScanProfile.valueOf(value.name());
    }
    private static UserScanProfile parseUserProfile(String raw, UserScanProfile fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return UserScanProfile.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
