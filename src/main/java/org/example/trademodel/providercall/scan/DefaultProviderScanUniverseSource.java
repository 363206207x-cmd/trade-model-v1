package org.example.trademodel.providercall.scan;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserConfigDO;
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
import org.example.trademodel.providercall.candidate.AutoCandidateRegistry;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.profile.ProfileTransitionResult;
import org.example.trademodel.providercall.profile.ProfileTransitionSignal;
import org.example.trademodel.providercall.profile.ProviderCallProfilePreferenceService;
import org.example.trademodel.providercall.profile.ScanProfileTransitionService;
import org.example.trademodel.providercall.universe.DiscoveryUniverseSource;
import org.example.trademodel.providercall.universe.WatchlistAssetSource;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PushRecheckStatusContract;
import org.example.trademodel.service.UserConfigService;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DefaultProviderScanUniverseSource implements ProviderScanUniverseSource {
    private static final String SCAN_PROVIDER = "BINANCE";

    private final ProviderCallProperties properties;
    private final UserPositionMapper userPositionMapper;
    private final AssetStateMapper assetStateMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PositionMonitorLogService monitorLogService;
    private final UserConfigService userConfigService;
    private final ScanProfileTransitionService transitionService;
    private final ProviderRefreshStateRegistry refreshRegistry;
    private final WatchlistAssetSource watchlistSource;
    private final DiscoveryUniverseSource discoverySource;
    private final AutoCandidateRegistry autoCandidateRegistry;
    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final ProviderCallProfilePreferenceService profilePreferenceService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultProviderScanUniverseSource(ProviderCallProperties properties,
                                             UserPositionMapper userPositionMapper,
                                             AssetStateMapper assetStateMapper,
                                             DecisionResultMapper decisionResultMapper,
                                             PushSnapshotMapper pushSnapshotMapper,
                                             PositionMonitorLogService monitorLogService,
                                             UserConfigService userConfigService,
                                             ScanProfileTransitionService transitionService,
                                             ProviderRefreshStateRegistry refreshRegistry,
                                             WatchlistAssetSource watchlistSource,
                                             DiscoveryUniverseSource discoverySource,
                                             AutoCandidateRegistry autoCandidateRegistry,
                                             ProviderSymbolMappingRegistry mappingRegistry,
                                             ProviderCallProfilePreferenceService profilePreferenceService) {
        this(properties, userPositionMapper, assetStateMapper, decisionResultMapper, pushSnapshotMapper,
                monitorLogService, userConfigService, transitionService, refreshRegistry, watchlistSource,
                discoverySource, autoCandidateRegistry, mappingRegistry, profilePreferenceService,
                Clock.systemUTC());
    }

    public DefaultProviderScanUniverseSource(ProviderCallProperties properties,
                                             UserPositionMapper userPositionMapper,
                                             AssetStateMapper assetStateMapper,
                                             DecisionResultMapper decisionResultMapper,
                                             PushSnapshotMapper pushSnapshotMapper,
                                             PositionMonitorLogService monitorLogService,
                                             UserConfigService userConfigService,
                                             ScanProfileTransitionService transitionService,
                                             ProviderRefreshStateRegistry refreshRegistry,
                                             WatchlistAssetSource watchlistSource,
                                             DiscoveryUniverseSource discoverySource,
                                             AutoCandidateRegistry autoCandidateRegistry,
                                             ProviderSymbolMappingRegistry mappingRegistry,
                                             ProviderCallProfilePreferenceService profilePreferenceService,
                                             Clock clock) {
        this.properties = properties;
        this.userPositionMapper = userPositionMapper;
        this.assetStateMapper = assetStateMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.monitorLogService = monitorLogService;
        this.userConfigService = userConfigService;
        this.transitionService = transitionService;
        this.refreshRegistry = refreshRegistry;
        this.watchlistSource = watchlistSource;
        this.discoverySource = discoverySource;
        this.autoCandidateRegistry = autoCandidateRegistry;
        this.mappingRegistry = mappingRegistry;
        this.profilePreferenceService = profilePreferenceService;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public ScanUniverseInput currentUniverse() {
        Instant asOf = clock.instant();
        UserConfigDO config = safeUserConfig();
        UserScanProfile base = profilePreferenceService.getBaseProfile();
        boolean autoEscalation = config == null || config.getScanAutoEscalationEnabled() == null
                ? properties.isAutoEscalationEnabled() : config.getScanAutoEscalationEnabled();

        List<CanonicalInstrumentId> watchlist = safeWatchlist();
        Set<CanonicalInstrumentId> watchlistSet = Set.copyOf(watchlist);
        List<CanonicalInstrumentId> discovery = safeDiscovery();
        List<UserPositionDO> openPositions = safeOpenPositions();
        List<PositionScanAsset> positions = openPositions.stream()
                .filter(DefaultProviderScanUniverseSource::validManualPosition)
                .map(this::positionAsset)
                .flatMap(Optional::stream)
                .toList();

        List<AssetStateDO> candidateRows = safeCandidates();
        Map<CanonicalInstrumentId, String> pushSignals = safePushEscalationSignals();
        LinkedHashSet<CanonicalInstrumentId> candidates = new LinkedHashSet<>();
        candidateRows.stream().map(AssetStateDO::getSymbol).map(this::resolvePerpetual)
                .flatMap(Optional::stream).filter(watchlistSet::contains).forEach(candidates::add);
        pushSignals.keySet().stream().filter(watchlistSet::contains).forEach(candidates::add);
        autoCandidateRegistry.activeAt(asOf).stream()
                .map(AutoCandidateRegistry.AutoCandidateSnapshot::canonicalInstrumentId)
                .forEach(candidates::add);

        Map<CanonicalInstrumentId, RuntimeScanProfile> escalations = new LinkedHashMap<>();
        Map<CanonicalInstrumentId, String> reasons = new LinkedHashMap<>();
        if (autoEscalation && properties.isProfileEscalationEnabled()) {
            Map<CanonicalInstrumentId, UserPositionDO> positionByInstrument = new LinkedHashMap<>();
            for (UserPositionDO row : openPositions) {
                resolvePerpetual(row.getAssetSymbol()).ifPresent(id -> positionByInstrument.putIfAbsent(id, row));
            }
            Map<CanonicalInstrumentId, AssetStateDO> stateByInstrument = new LinkedHashMap<>();
            for (AssetStateDO row : candidateRows) {
                resolvePerpetual(row.getSymbol()).ifPresent(id -> stateByInstrument.putIfAbsent(id, row));
            }
            Set<CanonicalInstrumentId> evaluated = new LinkedHashSet<>();
            evaluated.addAll(positionByInstrument.keySet());
            evaluated.addAll(stateByInstrument.keySet());
            evaluated.addAll(pushSignals.keySet());
            for (CanonicalInstrumentId instrument : evaluated) {
                String providerSymbol = mappingRegistry.resolve(SCAN_PROVIDER, instrument).providerSymbol();
                ProfileTransitionResult transition = transitionService.evaluate(providerSymbol, base,
                        signal(positionByInstrument.get(instrument), stateByInstrument.get(instrument),
                                pushSignals.get(instrument)),
                        "provider-universe-" + UUID.randomUUID());
                escalations.put(instrument, transition.effectiveProfile());
                reasons.put(instrument, transition.effectiveReason());
            }
        }
        return new ScanUniverseInput(watchlist, positions, List.copyOf(candidates), discovery, base,
                RuntimeScanProfile.LOW, escalations, reasons, refreshRegistry.lastAttempts(), asOf);
    }

    private ProfileTransitionSignal signal(UserPositionDO position, AssetStateDO state, String pushSignal) {
        PositionMonitorLogDTO log = latestLog(position);
        String reason = upper(log == null ? null : log.getReason());
        String logic = upper(log == null ? null : log.getLogicStatus());
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

    private Optional<PositionScanAsset> positionAsset(UserPositionDO row) {
        return resolvePerpetual(row.getAssetSymbol()).map(id -> new PositionScanAsset(id,
                mappingRegistry.resolve(SCAN_PROVIDER, id).providerSymbol(), row.getStatus()));
    }

    private Optional<CanonicalInstrumentId> resolvePerpetual(String symbol) {
        try {
            ProviderSymbolMapping mapping = mappingRegistry.resolve(SCAN_PROVIDER, symbol, MarketType.PERPETUAL);
            return Optional.of(mapping.canonicalInstrumentId());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private List<CanonicalInstrumentId> safeWatchlist() {
        try { return watchlistSource.currentWatchlist(); }
        catch (RuntimeException ignored) { return List.of(); }
    }

    private List<CanonicalInstrumentId> safeDiscovery() {
        try { return discoverySource.currentDiscoveryUniverse(); }
        catch (RuntimeException ignored) { return List.of(); }
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
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<AssetStateDO> safeCandidates() {
        try {
            List<AssetStateDO> rows = assetStateMapper.listCandidateOrWaitingTrigger(properties.getMaxCandidateAssets());
            return rows == null ? List.of() : rows;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private Map<CanonicalInstrumentId, String> safePushEscalationSignals() {
        try {
            int limit = Math.max(1, properties.getMaxWatchlistAssets()
                    + properties.getMaxCandidateAssets() + properties.getMaxDiscoveryAssets());
            List<TmPushSnapshotDO> rows = pushSnapshotMapper.listRecent(limit);
            if (rows == null || rows.isEmpty()) return Map.of();
            Map<CanonicalInstrumentId, String> signals = new LinkedHashMap<>();
            for (TmPushSnapshotDO row : rows) {
                String status = row == null ? null
                        : PushRecheckStatusContract.canonicalizePushStatus(row.getPushStatus());
                if (!isPushEscalation(status)) continue;
                resolvePerpetual(row.getSymbol()).ifPresent(id -> signals.putIfAbsent(id, status));
            }
            return Map.copyOf(signals);
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static boolean isPushEscalation(String status) {
        return PushRecheckStatusContract.PUSH_STATUS_DRIFTED_FROM_ENTRY_ZONE.equals(status)
                || PushRecheckStatusContract.PUSH_STATUS_INVALIDATED.equals(status)
                || PushRecheckStatusContract.PUSH_STATUS_RISK_BLOCKED.equals(status)
                || PushRecheckStatusContract.PUSH_STATUS_CONFUSED_BLOCKED.equals(status);
    }

    private static boolean validManualPosition(UserPositionDO row) {
        return row != null && "MANUAL".equalsIgnoreCase(row.getSourceType())
                && ("OPEN".equalsIgnoreCase(row.getStatus())
                || "PARTIALLY_CLOSED".equalsIgnoreCase(row.getStatus()));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static UserScanProfile parseUserProfile(String raw, UserScanProfile fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return UserScanProfile.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
