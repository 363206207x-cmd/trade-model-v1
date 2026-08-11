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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        UniverseSnapshot snapshot = collectSnapshot();
        return toInput(snapshot, readOnlyTransitions(snapshot));
    }

    @Override
    public ScanUniverseInput evaluateUniverseForExecution(String scanCycleTraceId) {
        String safeTraceId = requiredTraceId(scanCycleTraceId);
        UniverseSnapshot snapshot = collectSnapshot();
        return toInput(snapshot, executionTransitions(snapshot, safeTraceId));
    }

    private UniverseSnapshot collectSnapshot() {
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

        Map<CanonicalInstrumentId, UserPositionDO> positionByInstrument = new LinkedHashMap<>();
        for (UserPositionDO row : openPositions) {
            resolvePerpetual(row.getAssetSymbol()).ifPresent(id -> positionByInstrument.putIfAbsent(id, row));
        }
        Map<CanonicalInstrumentId, AssetStateDO> stateByInstrument = new LinkedHashMap<>();
        for (AssetStateDO row : candidateRows) {
            resolvePerpetual(row.getSymbol()).ifPresent(id -> stateByInstrument.putIfAbsent(id, row));
        }
        Set<CanonicalInstrumentId> transitionInstruments = new LinkedHashSet<>();
        transitionInstruments.addAll(positionByInstrument.keySet());
        transitionInstruments.addAll(stateByInstrument.keySet());
        transitionInstruments.addAll(pushSignals.keySet());

        boolean transitionsEnabled = autoEscalation && properties.isProfileEscalationEnabled();
        return new UniverseSnapshot(watchlist, positions, List.copyOf(candidates), discovery, base,
                transitionsEnabled, Map.copyOf(positionByInstrument), Map.copyOf(stateByInstrument),
                pushSignals, Set.copyOf(transitionInstruments), refreshRegistry.lastAttempts(), asOf);
    }

    private TransitionView readOnlyTransitions(UniverseSnapshot snapshot) {
        Map<CanonicalInstrumentId, RuntimeScanProfile> escalations = new LinkedHashMap<>();
        Map<CanonicalInstrumentId, String> reasons = new LinkedHashMap<>();
        if (snapshot.transitionsEnabled()) {
            for (CanonicalInstrumentId instrument : snapshot.transitionInstruments()) {
                String providerSymbol = mappingRegistry.resolve(SCAN_PROVIDER, instrument).providerSymbol();
                ProfileTransitionResult transition = transitionService.current(
                        providerSymbol, "provider-universe-read-only");
                escalations.put(instrument, transition.effectiveProfile());
                reasons.put(instrument, transition.effectiveReason());
            }
        }
        return new TransitionView(escalations, reasons);
    }

    private TransitionView executionTransitions(UniverseSnapshot snapshot, String scanCycleTraceId) {
        Map<CanonicalInstrumentId, RuntimeScanProfile> escalations = new LinkedHashMap<>();
        Map<CanonicalInstrumentId, String> reasons = new LinkedHashMap<>();
        if (snapshot.transitionsEnabled()) {
            for (CanonicalInstrumentId instrument : snapshot.transitionInstruments()) {
                String providerSymbol = mappingRegistry.resolve(SCAN_PROVIDER, instrument).providerSymbol();
                ProfileTransitionResult transition = transitionService.evaluate(providerSymbol, snapshot.base(),
                        signal(snapshot.positionByInstrument().get(instrument),
                                snapshot.stateByInstrument().get(instrument),
                                snapshot.pushSignals().get(instrument)),
                        scanCycleTraceId + ":" + instrument.canonical());
                escalations.put(instrument, transition.effectiveProfile());
                reasons.put(instrument, transition.effectiveReason());
            }
        }
        return new TransitionView(escalations, reasons);
    }

    private static ScanUniverseInput toInput(UniverseSnapshot snapshot, TransitionView transitions) {
        return new ScanUniverseInput(snapshot.watchlist(), snapshot.positions(), snapshot.candidates(),
                snapshot.discovery(), snapshot.base(), RuntimeScanProfile.LOW, transitions.escalations(),
                transitions.reasons(), snapshot.lastRefreshes(), snapshot.asOf());
    }

    private static String requiredTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("scanCycleTraceId is required");
        }
        return traceId.trim();
    }

    private ProfileTransitionSignal signal(UserPositionDO position, AssetStateDO state, String pushSignal) {
        PositionMonitorLogDTO log = latestLog(position);
        String reason = upper(log == null ? null : log.getReason());
        String conclusion = upper(log == null ? null : log.getMonitorConclusion());
        DecisionResultVO decision = state == null ? null : safeDecision(state.getSymbol());
        boolean highRisk = "HIGH".equalsIgnoreCase(log == null ? null : log.getRiskLevel())
                || "EXTREME".equalsIgnoreCase(log == null ? null : log.getRiskLevel())
                || "HIGH_RISK_OBSERVATION".equals(conclusion)
                || "PLAN_INVALIDATED".equals(conclusion)
                || "WAIT_USER_CONFIRM_CLOSE".equals(conclusion)
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
            List<PositionMonitorLogDTO> rows = monitorLogService.listByPositionIdForSystem(position.getId(), 1);
            if (rows == null || rows.isEmpty()) return null;
            PositionMonitorLogDTO latest = rows.get(0);
            LocalDateTime asOf = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
            return latest != null && latest.isTrustedAndFreshAt(asOf) ? latest : null;
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
            List<UserPositionDO> rows = userPositionMapper.listClaimedOpenForSystemMonitoring();
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
        return row != null && ("MANUAL".equalsIgnoreCase(row.getSourceType())
                || "MANUAL_POSITION".equalsIgnoreCase(row.getSourceType())
                || "SYSTEM_PLAN_POSITION".equalsIgnoreCase(row.getSourceType()))
                && ("OPEN".equalsIgnoreCase(row.getStatus())
                || "PARTIALLY_CLOSED".equalsIgnoreCase(row.getStatus()));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record UniverseSnapshot(
            List<CanonicalInstrumentId> watchlist,
            List<PositionScanAsset> positions,
            List<CanonicalInstrumentId> candidates,
            List<CanonicalInstrumentId> discovery,
            UserScanProfile base,
            boolean transitionsEnabled,
            Map<CanonicalInstrumentId, UserPositionDO> positionByInstrument,
            Map<CanonicalInstrumentId, AssetStateDO> stateByInstrument,
            Map<CanonicalInstrumentId, String> pushSignals,
            Set<CanonicalInstrumentId> transitionInstruments,
            Map<ScanUniverseInput.DatasetRefreshKey, Instant> lastRefreshes,
            Instant asOf) {
    }

    private record TransitionView(
            Map<CanonicalInstrumentId, RuntimeScanProfile> escalations,
            Map<CanonicalInstrumentId, String> reasons) {
    }
}
