package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
public class CoinGlassProviderHealthService {
    private static final List<String> REQUIRED_CAPABILITIES = List.of(
            CoinGlassV4ResponseValidator.OI_CAPABILITY,
            CoinGlassV4ResponseValidator.FUNDING_CAPABILITY,
            CoinGlassV4ResponseValidator.LIQUIDATION_CAPABILITY,
            CoinGlassV4ResponseValidator.LONG_SHORT_CAPABILITY);

    private final Map<String, CoinGlassEndpointHealth> health = new ConcurrentHashMap<>();
    private final Clock clock;

    private final Map<String, RuntimeSnapshot> runtime = new ConcurrentHashMap<>();
    private final Map<String, java.time.Duration> refreshCadences = new ConcurrentHashMap<>();
    private final Map<String, java.time.Duration> scheduledCadences = new ConcurrentHashMap<>();
    private org.springframework.jdbc.core.JdbcTemplate runtimeJdbc;
    private boolean postgresRuntimeStore;
    private CoinGlassProperties runtimeProperties = new CoinGlassProperties();

    @org.springframework.beans.factory.annotation.Autowired
    void setRuntimeProperties(CoinGlassProperties properties) {
        this.runtimeProperties = properties;
    }

    @org.springframework.beans.factory.annotation.Autowired
    synchronized void setRuntimeJdbcTemplate(org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.runtimeJdbc = jdbc;
        this.postgresRuntimeStore = Boolean.TRUE.equals(jdbc.execute(
                (org.springframework.jdbc.core.ConnectionCallback<Boolean>) connection ->
                        "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())));
        jdbc.query("SELECT * FROM tm_coinglass_runtime_snapshot", (rs, row) -> runtimeRow(rs))
                .forEach(value -> runtime.put(value.capabilityId() + "|" + value.providerSymbol(), value));
    }

    public void noteRefreshCadence(String capability, String symbol, java.time.Duration cadence) {
        if (cadence != null && !cadence.isNegative() && !cadence.isZero()) {
            refreshCadences.put(runtimeKey(capability, symbol), cadence);
        }
    }

    public java.time.Duration refreshCadence(String capability, String symbol) {
        return refreshCadences.getOrDefault(runtimeKey(capability, symbol),
                java.time.Duration.ofSeconds(Math.max(1, runtimeProperties.getFreshTtlSeconds())));
    }

    CoinGlassDerivativesSnapshotAssembler readAssembler(CoinGlassProperties properties, String symbol) {
        // Scope freshness to this asset, never change the globally configured collection cadence.
        long seconds = REQUIRED_CAPABILITIES.stream()
                .map(capability -> properties.readFreshnessWindow(refreshCadence(capability, symbol)))
                .mapToLong(java.time.Duration::toSeconds).min().orElse(properties.getFreshTtlSeconds());
        CoinGlassProperties readPolicy = new CoinGlassProperties();
        readPolicy.setFreshTtlSeconds(Math.toIntExact(seconds));
        return new CoinGlassDerivativesSnapshotAssembler(readPolicy, clock);
    }

    /** Called only by the scheduled refresh port, not by cache readers. */
    public void noteRefreshSchedule(String symbol, java.time.Duration cadence) {
        if (cadence == null || cadence.isNegative() || cadence.isZero()) return;
        REQUIRED_CAPABILITIES.forEach(capability -> {
            scheduledCadences.put(runtimeKey(capability, symbol), cadence);
            noteRefreshCadence(capability, symbol, cadence);
        });
    }

    private Instant scheduledCheck(String key, Instant attempt) {
        java.time.Duration cadence = scheduledCadences.get(key);
        return cadence == null ? null : attempt.plus(cadence);
    }

    public synchronized void beginDataset(String capability, String symbol, java.time.Duration cadence) {
        String key = runtimeKey(capability, symbol);
        Instant now = clock.instant();
        RuntimeSnapshot previous = runtime.get(key);
        long seconds = Math.max(1L, cadence.toSeconds());
        saveRuntime(new RuntimeSnapshot(capability, normalizedSymbol(symbol), "RUNNING",
                UnifiedSourceStatus.WAITING_SYNC.name(), now,
                previous == null ? null : previous.lastSuccessAt(),
                previous == null ? null : previous.providerDataAt(), scheduledCheck(key, now),
                freshnessWindow(seconds), null, null, "REQUEST_RUNNING",
                previous == null ? 1 : previous.stateVersion() + 1, now));
    }

    public synchronized void recordDataset(String capability, String symbol, UnifiedSourceStatus status,
                                           int httpStatus, String providerStatus, String reason,
                                           CoinGlassRateLimitMetadata rateLimit, Instant attemptedAt,
                                           Instant providerDataAt, java.time.Duration cadence) {
        String key = runtimeKey(capability, symbol);
        RuntimeSnapshot previous = runtime.get(key);
        Instant attempt = attemptedAt == null ? clock.instant() : attemptedAt;
        if (previous != null && previous.lastAttemptAt() != null
                && attempt.isBefore(previous.lastAttemptAt())) return;
        boolean success = status == UnifiedSourceStatus.READY && providerDataAt != null
                && !providerDataAt.isAfter(attempt);
        long seconds = Math.max(1L, cadence.toSeconds());
        Long retryAfter = rateLimit == null ? null : rateLimit.retryAfterSeconds();
        Instant nextCheck = retryAfter == null ? scheduledCheck(key, attempt)
                : attempt.plusSeconds(Math.max(1L, retryAfter));
        String state = httpStatus == 429 ? "RATE_LIMITED"
                : status == UnifiedSourceStatus.DISABLED || status == UnifiedSourceStatus.NOT_CONFIGURED
                ? "DISABLED" : success ? "FRESH" : "ERROR";
        String safeReason = status == UnifiedSourceStatus.READY && !success
                ? "PROVIDER_OBSERVATION_TIME_MISSING_OR_FUTURE" : metadataCode(reason, 160);
        RuntimeSnapshot value = new RuntimeSnapshot(capability, normalizedSymbol(symbol), state,
                status.name(), attempt, success ? attempt : previous == null ? null : previous.lastSuccessAt(),
                success ? providerDataAt : previous == null ? null : previous.providerDataAt(),
                nextCheck, freshnessWindow(seconds), httpStatus, metadataCode(providerStatus, 64),
                safeReason, previous == null ? 1 : previous.stateVersion() + 1, clock.instant());
        saveRuntime(value);
        record(capability, status, httpStatus, metadataCode(providerStatus, 64), safeReason, rateLimit, attempt);
    }

    public Map<String, RuntimeSnapshot> runtimeSnapshot() {
        Map<String, RuntimeSnapshot> result = new java.util.TreeMap<>();
        runtime.forEach((key, value) -> result.put(key, atCurrentTime(value)));
        return Map.copyOf(result);
    }

    public List<RuntimeSnapshot> latestCapabilities() {
        Map<String, RuntimeSnapshot> latest = new java.util.TreeMap<>();
        runtimeSnapshot().values().forEach(value -> latest.merge(value.capabilityId(), value,
                (left, right) -> java.util.Comparator.comparing(RuntimeSnapshot::lastAttemptAt,
                        java.util.Comparator.nullsFirst(java.util.Comparator.<Instant>naturalOrder()))
                        .compare(left, right) >= 0 ? left : right));
        return List.copyOf(latest.values());
    }

    public String runtimeState() {
        List<RuntimeSnapshot> rows = latestCapabilities();
        for (String state : List.of("RATE_LIMITED", "ERROR", "RUNNING", "STALE", "DISABLED")) {
            if (rows.stream().anyMatch(row -> state.equals(row.runtimeState()))) return state;
        }
        return rows.size() == REQUIRED_CAPABILITIES.size() ? "FRESH" : "NOT_STARTED";
    }

    private RuntimeSnapshot atCurrentTime(RuntimeSnapshot value) {
        if ("FRESH".equals(value.runtimeState()) && (value.providerDataAt() == null
                || value.providerDataAt().isAfter(clock.instant())
                || !value.providerDataAt().plusSeconds(value.freshTtlSeconds()).isAfter(clock.instant()))) {
            return value.withState("STALE");
        }
        long timeoutSeconds = Math.max(5, runtimeProperties.getRequestTimeoutMs() / 1000L) * 4;
        if ("RUNNING".equals(value.runtimeState()) && value.lastAttemptAt() != null
                && value.lastAttemptAt().plusSeconds(timeoutSeconds).isBefore(clock.instant())) {
            return value.withState("ERROR");
        }
        return value;
    }

    private int freshnessWindow(long cadenceSeconds) {
        return Math.toIntExact(runtimeProperties.readFreshnessWindow(
                java.time.Duration.ofSeconds(cadenceSeconds)).toSeconds());
    }

    private static String normalizedSymbol(String symbol) {
        if (symbol == null || !symbol.trim().matches("[A-Za-z0-9/_:-]{1,64}")) {
            throw new IllegalArgumentException("COINGLASS_RUNTIME_SYMBOL_INVALID");
        }
        return symbol.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String runtimeKey(String capability, String symbol) {
        if (!REQUIRED_CAPABILITIES.contains(capability)) {
            throw new IllegalArgumentException("COINGLASS_RUNTIME_CAPABILITY_INVALID");
        }
        return capability + "|" + normalizedSymbol(symbol);
    }

    private static String metadataCode(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength && value.matches("[A-Za-z0-9_.:-]+")
                ? value : "PROVIDER_METADATA_REDACTED";
    }

    private void saveRuntime(RuntimeSnapshot value) {
        if (runtimeJdbc != null) {
            String columns = "capability_id, provider_symbol, runtime_state, source_status, last_attempt_at, "
                    + "last_success_at, provider_data_at, next_check_at, fresh_ttl_seconds, http_status, "
                    + "provider_status_code, reason_code, state_version, updated_at";
            String sql = postgresRuntimeStore
                    ? "INSERT INTO tm_coinglass_runtime_snapshot (" + columns + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                    + "ON CONFLICT (capability_id, provider_symbol) DO UPDATE SET "
                    + "runtime_state=EXCLUDED.runtime_state, source_status=EXCLUDED.source_status, "
                    + "last_attempt_at=EXCLUDED.last_attempt_at, last_success_at=EXCLUDED.last_success_at, "
                    + "provider_data_at=EXCLUDED.provider_data_at, next_check_at=EXCLUDED.next_check_at, "
                    + "fresh_ttl_seconds=EXCLUDED.fresh_ttl_seconds, http_status=EXCLUDED.http_status, "
                    + "provider_status_code=EXCLUDED.provider_status_code, reason_code=EXCLUDED.reason_code, "
                    + "state_version=tm_coinglass_runtime_snapshot.state_version+1, updated_at=EXCLUDED.updated_at "
                    + "WHERE tm_coinglass_runtime_snapshot.last_attempt_at IS NULL "
                    + "OR EXCLUDED.last_attempt_at >= tm_coinglass_runtime_snapshot.last_attempt_at"
                    : "MERGE INTO tm_coinglass_runtime_snapshot (" + columns + ") "
                    + "KEY (capability_id, provider_symbol) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            int written = runtimeJdbc.update(sql, value.capabilityId(), value.providerSymbol(), value.runtimeState(),
                    value.sourceStatus(), utc(value.lastAttemptAt()), utc(value.lastSuccessAt()),
                    utc(value.providerDataAt()), utc(value.nextCheckAt()), value.freshTtlSeconds(),
                    value.httpStatus(), value.providerStatusCode(), value.reasonCode(), value.stateVersion(),
                    utc(value.updatedAt()));
            if (written == 0) return;
            value = runtimeJdbc.queryForObject(
                    "SELECT * FROM tm_coinglass_runtime_snapshot WHERE capability_id=? AND provider_symbol=?",
                    (rs, row) -> runtimeRow(rs), value.capabilityId(), value.providerSymbol());
        }
        runtime.put(value.capabilityId() + "|" + value.providerSymbol(), value);
    }

    private static java.time.OffsetDateTime utc(Instant time) {
        return time == null ? null : time.atOffset(java.time.ZoneOffset.UTC);
    }

    private static Instant time(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        java.time.OffsetDateTime value = rs.getObject(column, java.time.OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static RuntimeSnapshot runtimeRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RuntimeSnapshot(rs.getString("capability_id"), rs.getString("provider_symbol"),
                rs.getString("runtime_state"), rs.getString("source_status"), time(rs, "last_attempt_at"),
                time(rs, "last_success_at"), time(rs, "provider_data_at"), time(rs, "next_check_at"),
                rs.getInt("fresh_ttl_seconds"), rs.getObject("http_status", Integer.class),
                rs.getString("provider_status_code"), rs.getString("reason_code"),
                rs.getLong("state_version"), time(rs, "updated_at"));
    }

    public record RuntimeSnapshot(String capabilityId, String providerSymbol, String runtimeState,
                                  String sourceStatus, Instant lastAttemptAt, Instant lastSuccessAt,
                                  Instant providerDataAt, Instant nextCheckAt, int freshTtlSeconds,
                                  Integer httpStatus, String providerStatusCode, String reasonCode,
                                  long stateVersion, Instant updatedAt) {
        RuntimeSnapshot withState(String state) {
            return new RuntimeSnapshot(capabilityId, providerSymbol, state, sourceStatus, lastAttemptAt,
                    lastSuccessAt, providerDataAt, nextCheckAt, freshTtlSeconds, httpStatus,
                    providerStatusCode, reasonCode, stateVersion, updatedAt);
        }
    }


    public CoinGlassProviderHealthService() {
        this(Clock.systemUTC());
    }

    CoinGlassProviderHealthService(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public void record(String capabilityId, UnifiedSourceStatus status, int httpStatus,
                       String providerStatusCode, String reasonCode,
                       CoinGlassRateLimitMetadata rateLimit, Instant fetchTime) {
        health.put(capabilityId, new CoinGlassEndpointHealth(capabilityId, status, httpStatus,
                providerStatusCode, reasonCode, rateLimit, fetchTime));
    }

    public CoinGlassEndpointHealth get(String capabilityId) {
        return health.get(capabilityId);
    }

    public Map<String, CoinGlassEndpointHealth> snapshot() {
        return Map.copyOf(health);
    }

    public UnifiedSourceStatus configurationStatus(CoinGlassProperties properties) {
        CoinGlassConfigurationState configuration = configurationState(properties);
        if (configuration != CoinGlassConfigurationState.CONFIGURED) {
            return configuration == CoinGlassConfigurationState.INVALID_RPM
                    ? UnifiedSourceStatus.ERROR : UnifiedSourceStatus.NOT_CONFIGURED;
        }
        if (!runtime.isEmpty()) {
            return switch (runtimeState()) {
                case "FRESH" -> UnifiedSourceStatus.READY;
                case "STALE" -> UnifiedSourceStatus.STALE;
                case "ERROR" -> UnifiedSourceStatus.ERROR;
                case "RUNNING" -> UnifiedSourceStatus.WAITING_SYNC;
                case "DISABLED" -> UnifiedSourceStatus.DISABLED;
                default -> UnifiedSourceStatus.DEGRADED;
            };
        }
        List<CoinGlassEndpointHealth> required = REQUIRED_CAPABILITIES.stream()
                .map(health::get)
                .toList();
        if (required.stream().allMatch(value -> value == null)) return UnifiedSourceStatus.WAITING_SYNC;
        if (required.stream().filter(value -> value != null)
                .anyMatch(value -> value.status() == UnifiedSourceStatus.ERROR)) {
            return UnifiedSourceStatus.ERROR;
        }
        long ready = required.stream()
                .filter(value -> value != null && value.status() == UnifiedSourceStatus.READY).count();
        if (ready == REQUIRED_CAPABILITIES.size()) {
            boolean fresh = required.stream().allMatch(value -> isFresh(value, properties));
            return fresh ? UnifiedSourceStatus.READY : UnifiedSourceStatus.STALE;
        }
        if (ready > 0) return UnifiedSourceStatus.DEGRADED;
        return UnifiedSourceStatus.DEGRADED;
    }

    private boolean isFresh(CoinGlassEndpointHealth value, CoinGlassProperties properties) {
        Instant fetchTime = value.fetchTime();
        Instant now = clock.instant();
        if (fetchTime == null || fetchTime.isAfter(now)) return false;
        long ttlSeconds = Math.max(1L, properties.getFreshTtlSeconds());
        return fetchTime.plusSeconds(ttlSeconds).isAfter(now);
    }

    public CoinGlassConfigurationState configurationState(CoinGlassProperties properties) {
        return properties == null ? CoinGlassConfigurationState.NOT_CONFIGURED : properties.configurationState();
    }

    public record CoinGlassEndpointHealth(
            String capabilityId,
            UnifiedSourceStatus status,
            int httpStatus,
            String providerStatusCode,
            String reasonCode,
            CoinGlassRateLimitMetadata rateLimit,
            Instant fetchTime
    ) {
    }
}
