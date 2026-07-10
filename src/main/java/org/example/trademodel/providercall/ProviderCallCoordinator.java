package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ProviderCallCoordinator {
    private final ProviderCallProperties properties;
    private final SnapshotCacheService cache;
    private final ProviderSingleFlightGuard singleFlight;
    private final ProviderRateBudgetManager budget;
    private final ProviderCircuitBreaker circuitBreaker;
    private final ProviderCallAuditLog auditLog;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderCallCoordinator(ProviderCallProperties properties,
                                   SnapshotCacheService cache,
                                   ProviderSingleFlightGuard singleFlight,
                                   ProviderRateBudgetManager budget,
                                   ProviderCircuitBreaker circuitBreaker,
                                   ProviderCallAuditLog auditLog) {
        this(properties, cache, singleFlight, budget, circuitBreaker, auditLog, Clock.systemUTC());
    }

    public ProviderCallCoordinator(ProviderCallProperties properties,
                                   SnapshotCacheService cache,
                                   ProviderSingleFlightGuard singleFlight,
                                   ProviderRateBudgetManager budget,
                                   ProviderCircuitBreaker circuitBreaker,
                                   ProviderCallAuditLog auditLog,
                                   Clock clock) {
        this.properties = properties;
        this.cache = cache;
        this.singleFlight = singleFlight;
        this.budget = budget;
        this.circuitBreaker = circuitBreaker;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public <T> ProviderCallResult<T> execute(ProviderCallRequest<T> request) {
        SnapshotCacheService.SnapshotLookup<T> existing = cache.lookup(
                request.key(), clock.instant(), request.freshTtl());
        if (existing.fresh()) return cached(request, existing, false);
        return singleFlight.execute(request.key(), () -> {
            SnapshotCacheService.SnapshotLookup<T> afterJoin = cache.lookup(
                    request.key(), clock.instant(), request.freshTtl());
            if (afterJoin.fresh()) return cached(request, afterJoin, false);
            return refresh(request, afterJoin.staleReadable() ? afterJoin : existing);
        });
    }

    public <T> ProviderCallResult<T> peek(
            ProviderRequestKey key,
            AssetPriority priority,
            Duration freshTtl,
            String traceId) {
        ProviderCallRequest<T> request = new ProviderCallRequest<>(key, priority, freshTtl,
                freshTtl.multipliedBy(4), Duration.ofSeconds(1), traceId,
                () -> { throw new IllegalStateException("read-only snapshot peek must not call provider"); });
        SnapshotCacheService.SnapshotLookup<T> lookup = cache.lookup(key, clock.instant(), freshTtl);
        if (lookup.fresh()) return cached(request, lookup, false);
        if (lookup.staleReadable()) return cached(request, lookup, true);
        return failOrStale(request, lookup, UnifiedSourceStatus.WAITING_SYNC, "SNAPSHOT_NOT_CACHED");
    }

    private <T> ProviderCallResult<T> refresh(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale) {
        String provider = request.key().provider();
        if (!properties.isEnabled() || !properties.isExternalCallsEnabled()) {
            return failOrStale(request, stale, UnifiedSourceStatus.DISABLED, "PROVIDER_CALL_DISABLED");
        }
        if (!circuitBreaker.allowRequest(provider)) {
            return failOrStale(request, stale, UnifiedSourceStatus.DEGRADED, "PROVIDER_CIRCUIT_OPEN");
        }
        if (!budget.reserve(provider, request.priority())) {
            return failOrStale(request, stale, UnifiedSourceStatus.DEGRADED, "PROVIDER_BUDGET_REJECTED");
        }

        ProviderAdapterResponse<T> response;
        try {
            response = invokeBounded(request);
        } catch (RuntimeException failure) {
            circuitBreaker.recordFailure(provider);
            return failOrStale(request, stale, UnifiedSourceStatus.ERROR, "PROVIDER_CALL_FAILED");
        }
        if (response != null && response.ready()) {
            circuitBreaker.recordSuccess(provider);
            Instant fetchTime = clock.instant();
            ProviderSnapshotMetadata metadata = metadata(request, response.providerDataTime(), fetchTime,
                    fetchTime.plus(request.freshTtl()), UnifiedSourceStatus.READY,
                    SnapshotFreshnessStatus.FRESH, false, false, null, List.of());
            cache.put(request.key(), response.payload(), metadata, request.staleTtl());
            return audited(request, new ProviderCallResult<>(response.payload(), metadata,
                    budget.state(provider, circuitBreaker.state(provider))));
        }

        int status = response == null ? 0 : response.httpStatus();
        String reason = response == null || response.reasonCode() == null
                ? "PROVIDER_RESPONSE_MALFORMED" : response.reasonCode();
        if (status == 429) {
            budget.applyRetryAfter(provider, response.retryAfterSeconds() == null ? 60 : response.retryAfterSeconds());
        }
        circuitBreaker.recordFailure(provider);
        UnifiedSourceStatus sourceStatus = response == null ? UnifiedSourceStatus.ERROR : response.sourceStatus();
        return failOrStale(request, stale, sourceStatus == null ? UnifiedSourceStatus.ERROR : sourceStatus, reason);
    }

    private <T> ProviderAdapterResponse<T> invokeBounded(ProviderCallRequest<T> request) {
        int attempt = 0;
        int timeoutCount = 0;
        while (true) {
            attempt++;
            try {
                CompletableFuture<ProviderAdapterResponse<T>> future = CompletableFuture.supplyAsync(request.adapterCall());
                ProviderAdapterResponse<T> response;
                try {
                    response = future.get(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException timeout) {
                    future.cancel(true);
                    throw timeout;
                }
                if (response == null || response.ready()) return response;
                int status = response.httpStatus();
                if (status == 401 || status == 403 || status == 429) return response;
                if (status >= 500 && status <= 599 && attempt < 3) {
                    boundedBackoff(attempt);
                    continue;
                }
                return response;
            } catch (TimeoutException timeout) {
                timeoutCount++;
                if (timeoutCount >= 2) {
                    return ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                            "PROVIDER_TIMEOUT", null);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("provider call interrupted", interrupted);
            } catch (Exception failure) {
                throw new IllegalStateException("provider call failed", failure);
            }
        }
    }

    private static void boundedBackoff(int attempt) {
        try {
            Thread.sleep(Math.min(200L, 50L << Math.max(0, attempt - 1)));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("provider retry interrupted", interrupted);
        }
    }

    private <T> ProviderCallResult<T> cached(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> lookup,
            boolean fallback) {
        ProviderSnapshotMetadata metadata = lookup.metadata().asCacheHit(
                fallback ? SnapshotFreshnessStatus.STALE : SnapshotFreshnessStatus.FRESH, fallback,
                lookup.metadata().fetchTime().plus(request.freshTtl()));
        return audited(request, new ProviderCallResult<>(lookup.payload(), metadata,
                budget.state(request.key().provider(), circuitBreaker.state(request.key().provider()))));
    }

    private <T> ProviderCallResult<T> failOrStale(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            UnifiedSourceStatus status,
            String reason) {
        if (stale != null && stale.staleReadable()) return cached(request, stale, true);
        Instant now = clock.instant();
        ProviderSnapshotMetadata metadata = metadata(request, null, now, now, status,
                status == UnifiedSourceStatus.ERROR ? SnapshotFreshnessStatus.ERROR : SnapshotFreshnessStatus.UNAVAILABLE,
                false, false, reason, List.of(reason));
        return audited(request, new ProviderCallResult<>(null, metadata,
                budget.state(request.key().provider(), circuitBreaker.state(request.key().provider()))));
    }

    private <T> ProviderSnapshotMetadata metadata(
            ProviderCallRequest<T> request,
            Instant providerDataTime,
            Instant fetchTime,
            Instant expiresAt,
            UnifiedSourceStatus sourceStatus,
            SnapshotFreshnessStatus freshness,
            boolean cacheHit,
            boolean fallback,
            String errorCode,
            List<String> reasons) {
        ProviderRequestKey key = request.key();
        return new ProviderSnapshotMetadata(key.provider(), key.datasetType(), key.symbol(), key.timeframe(),
                providerDataTime, fetchTime, expiresAt, sourceStatus, freshness, request.traceId(), key.canonical(),
                cacheHit, fallback, errorCode, reasons);
    }

    private <T> ProviderCallResult<T> audited(ProviderCallRequest<T> request, ProviderCallResult<T> result) {
        ProviderSnapshotMetadata metadata = result.metadata();
        auditLog.record(new ProviderCallAuditEvent(request.traceId(), request.key().canonical(), request.priority(),
                metadata.sourceStatus(), metadata.cacheHit(), metadata.fallbackUsed(), metadata.errorCode(),
                clock.instant()));
        return result;
    }
}
