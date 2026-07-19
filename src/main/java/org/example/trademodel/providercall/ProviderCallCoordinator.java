package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProviderCallCoordinator {
    private final ProviderCallProperties properties;
    private final ProviderSnapshotCache cache;
    private final ProviderSingleFlightRegistry singleFlight;
    private final ProviderRateBudget budget;
    private final ProviderCircuitBreaker circuitBreaker;
    private final ProviderCallAuditService auditLog;
    private final ProviderConcurrencyGuard concurrencyGuard;
    private final ProviderHealthRegistry healthRegistry;
    private final ProviderCallExecutor callExecutor;
    private final ProviderSnapshotRetentionPolicy retentionPolicy;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderCallCoordinator(ProviderCallProperties properties,
                                   SnapshotCacheService cache,
                                   ProviderSingleFlightGuard singleFlight,
                                   ProviderRateBudgetManager budget,
                                   ProviderCircuitBreaker circuitBreaker,
                                   ProviderCallAuditLog auditLog,
                                   ProviderConcurrencyGuard concurrencyGuard,
                                   ProviderHealthRegistry healthRegistry,
                                   ProviderCallExecutor callExecutor,
                                   ProviderSnapshotRetentionPolicy retentionPolicy) {
        this(properties, cache, singleFlight, budget, circuitBreaker, auditLog, concurrencyGuard,
                healthRegistry, callExecutor, retentionPolicy, Clock.systemUTC());
    }

    /** Compatibility constructor retained for focused unit tests and existing adapters. */
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
        this(properties, cache, singleFlight, budget, circuitBreaker, auditLog,
                new ProviderConcurrencyGuard(properties), new ProviderHealthRegistry(clock),
                new ProviderCallExecutor(properties), new ProviderSnapshotRetentionPolicy(), clock);
    }

    public ProviderCallCoordinator(ProviderCallProperties properties,
                                   ProviderSnapshotCache cache,
                                   ProviderSingleFlightRegistry singleFlight,
                                   ProviderRateBudget budget,
                                   ProviderCircuitBreaker circuitBreaker,
                                   ProviderCallAuditService auditLog,
                                   ProviderConcurrencyGuard concurrencyGuard,
                                   ProviderHealthRegistry healthRegistry,
                                   Clock clock) {
        this(properties, cache, singleFlight, budget, circuitBreaker, auditLog, concurrencyGuard,
                healthRegistry, new ProviderCallExecutor(properties),
                new ProviderSnapshotRetentionPolicy(), clock);
    }

    public ProviderCallCoordinator(ProviderCallProperties properties,
                                   ProviderSnapshotCache cache,
                                   ProviderSingleFlightRegistry singleFlight,
                                   ProviderRateBudget budget,
                                   ProviderCircuitBreaker circuitBreaker,
                                   ProviderCallAuditService auditLog,
                                   ProviderConcurrencyGuard concurrencyGuard,
                                   ProviderHealthRegistry healthRegistry,
                                   ProviderCallExecutor callExecutor,
                                   ProviderSnapshotRetentionPolicy retentionPolicy,
                                   Clock clock) {
        this.properties = properties;
        this.cache = cache;
        this.singleFlight = singleFlight;
        this.budget = budget;
        this.circuitBreaker = circuitBreaker;
        this.auditLog = auditLog;
        this.concurrencyGuard = concurrencyGuard;
        this.healthRegistry = healthRegistry;
        this.callExecutor = callExecutor;
        this.retentionPolicy = retentionPolicy;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public <T> ProviderCallResult<T> execute(ProviderCallRequest<T> request) {
        ProviderSnapshotKey snapshotKey = request.key().snapshotKey();
        SnapshotCacheService.SnapshotLookup<T> existing = cache.lookup(
                snapshotKey, clock.instant(), request.freshTtl());
        if (existing.fresh()) return cached(request, existing, false);

        ProviderSingleFlightRegistration<ProviderCallResult<T>> registration = singleFlight.register(snapshotKey);
        try (registration) {
            SnapshotCacheService.SnapshotLookup<T> callerFallback = existing;
            if (registration.owner()) {
                SnapshotCacheService.SnapshotLookup<T> afterRegistration = cache.lookup(
                        snapshotKey, clock.instant(), request.freshTtl());
                if (afterRegistration.fresh()) {
                    registration.flight().completion().complete(cached(request, afterRegistration, false));
                } else {
                    SnapshotCacheService.SnapshotLookup<T> stale = afterRegistration.staleReadable()
                            ? afterRegistration : existing;
                    callerFallback = stale;
                    startRefresh(request, stale, registration.flight());
                }
            }
            return await(request, callerFallback, registration.flight());
        }
    }

    public <T> ProviderCallResult<T> peek(
            ProviderRequestKey key,
            AssetPriority priority,
            Duration freshTtl,
            String traceId) {
        ProviderCallRequest<T> request = new ProviderCallRequest<>(key, priority, freshTtl,
                freshTtl.multipliedBy(4), Duration.ofSeconds(1), traceId,
                () -> { throw new IllegalStateException("read-only snapshot peek must not call provider"); });
        ProviderSnapshotKey snapshotKey = key.snapshotKey();
        SnapshotCacheService.SnapshotLookup<T> lookup = cache.lookup(snapshotKey, clock.instant(), freshTtl);
        if (lookup.fresh()) return cached(request, lookup, false);
        if (lookup.staleReadable()) {
            return singleFlight.inFlight(snapshotKey)
                    ? cached(request, lookup, true, SnapshotFreshnessStatus.REFRESHING)
                    : cached(request, lookup, true);
        }
        if (singleFlight.inFlight(snapshotKey)) {
            Instant now = clock.instant();
            ProviderSnapshotMetadata metadata = metadata(request, null, now, now,
                    UnifiedSourceStatus.WAITING_SYNC, SnapshotFreshnessStatus.REFRESHING,
                    false, false, "SNAPSHOT_REFRESH_IN_PROGRESS", List.of("SNAPSHOT_REFRESH_IN_PROGRESS"));
            return audited(request, new ProviderCallResult<>(null, metadata,
                    budget.state(key.provider(), circuitBreaker.state(key.provider()))));
        }
        return failOrStale(request, lookup, UnifiedSourceStatus.WAITING_SYNC, "SNAPSHOT_NOT_CACHED");
    }

    private <T> void startRefresh(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderRefreshFlight<ProviderCallResult<T>> flight) {
        ProviderSnapshotKey snapshotKey = request.key().snapshotKey();
        String provider = snapshotKey.provider();
        if (!properties.isEnabled() || !properties.isExternalCallsEnabled()) {
            flight.completion().complete(failOrStale(request, stale, UnifiedSourceStatus.DISABLED,
                    "PROVIDER_CALL_DISABLED"));
            return;
        }
        if (!circuitBreaker.allowRequest(provider)) {
            healthRegistry.recordFailure(snapshotKey, UnifiedSourceStatus.DEGRADED, "PROVIDER_CIRCUIT_OPEN");
            flight.completion().complete(failOrStale(request, stale, UnifiedSourceStatus.DEGRADED,
                    "PROVIDER_CIRCUIT_OPEN"));
            return;
        }
        startAttempt(request, stale, flight, 1, 0, 0);
    }

    private <T> void startAttempt(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderRefreshFlight<ProviderCallResult<T>> flight,
            int attemptNumber,
            int used5xxRetries,
            int usedTimeoutRetries) {
        if (flight.completion().isDone()) return;
        String attemptId = request.traceId() + "-attempt-" + attemptNumber + "-" + UUID.randomUUID();
        AtomicBoolean timedOut = new AtomicBoolean();
        ProviderCallExecutor.TaskHandle<ProviderAdapterResponse<T>> handle;
        try {
            handle = callExecutor.submit(request.priority(),
                    () -> runPhysicalAttempt(request, attemptId, attemptNumber, timedOut));
        } catch (RejectedExecutionException rejected) {
            flight.completion().complete(finishResponse(request, stale,
                    ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0,
                            "PROVIDER_EXECUTOR_REJECTED", null)));
            return;
        }

        Runnable cancellation = () -> {
            if (timedOut.compareAndSet(false, true)) {
                healthRegistry.recordFailure(request.key().snapshotKey(), UnifiedSourceStatus.DEGRADED,
                        "PROVIDER_TIMEOUT_PHYSICAL_PENDING");
            }
            handle.cancelInterruptibly();
        };
        ScheduledFuture<?> timeout = callExecutor.schedule(cancellation, request.physicalAttemptTimeout());

        handle.completion().whenComplete((response, failure) -> {
            timeout.cancel(false);
            if (flight.completion().isDone()) return;

            ProviderAdapterResponse<T> effective = response;
            Throwable cause = unwrap(failure);
            if (timedOut.get()) {
                effective = ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                        "PROVIDER_TIMEOUT", null);
            } else if (cause != null) {
                effective = ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                        cause instanceof CancellationException ? "PROVIDER_CALL_CANCELLED" : "PROVIDER_CALL_FAILED",
                        null);
            }

            if (isTimeout(effective) && usedTimeoutRetries < request.maxRetryTimeout()) {
                scheduleRetry(request, stale, flight, attemptNumber + 1, used5xxRetries,
                        usedTimeoutRetries + 1);
                return;
            }
            if (isRetryable5xx(effective) && used5xxRetries < request.maxRetry5xx()) {
                scheduleRetry(request, stale, flight, attemptNumber + 1, used5xxRetries + 1,
                        usedTimeoutRetries);
                return;
            }
            flight.completion().complete(finishResponse(request, stale, effective));
        });
    }

    private <T> void scheduleRetry(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderRefreshFlight<ProviderCallResult<T>> flight,
            int attemptNumber,
            int used5xxRetries,
            int usedTimeoutRetries) {
        callExecutor.schedule(() -> startAttempt(request, stale, flight, attemptNumber,
                used5xxRetries, usedTimeoutRetries), boundedBackoff(attemptNumber));
    }

    private <T> ProviderAdapterResponse<T> runPhysicalAttempt(
            ProviderCallRequest<T> request,
            String attemptId,
            int attemptNumber,
            AtomicBoolean timedOut) {
        ProviderConcurrencyGuard.Lease lease = concurrencyGuard.tryAcquire(
                request.key().datasetType(), request.priority());
        if (lease == null) {
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0,
                    "PROVIDER_CONCURRENCY_REJECTED", null);
        }
        try (lease) {
            if (!budget.reserveAttempt(request.key(), request.priority(), request.effectiveProfile(),
                    attemptNumber > 1)) {
                return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0,
                        "PROVIDER_BUDGET_REJECTED", null);
            }
            auditAttempt(request, attemptId, attemptNumber, ProviderCallAuditPhase.PHYSICAL_ATTEMPT_STARTED,
                    UnifiedSourceStatus.WAITING_SYNC, "PHYSICAL_ATTEMPT_STARTED");
            ProviderAdapterResponse<T> response = null;
            String completionReason = "PROVIDER_RESPONSE_MALFORMED";
            UnifiedSourceStatus completionStatus = UnifiedSourceStatus.ERROR;
            try {
                response = request.adapterCall().get();
                if (timedOut.get()) {
                    completionReason = "PROVIDER_TIMEOUT_PHYSICAL_ENDED";
                } else if (response != null) {
                    completionReason = response.reasonCode();
                    completionStatus = response.sourceStatus() == null
                            ? UnifiedSourceStatus.ERROR : response.sourceStatus();
                }
                return response;
            } catch (RuntimeException failure) {
                completionReason = timedOut.get() ? "PROVIDER_TIMEOUT_PHYSICAL_ENDED" : "PROVIDER_CALL_FAILED";
                throw failure;
            } finally {
                auditAttempt(request, attemptId, attemptNumber,
                        ProviderCallAuditPhase.PHYSICAL_ATTEMPT_COMPLETED, completionStatus,
                        completionReason == null ? "PHYSICAL_ATTEMPT_COMPLETED" : completionReason);
            }
        }
    }

    private <T> ProviderCallResult<T> await(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderRefreshFlight<ProviderCallResult<T>> flight) {
        try {
            return flight.completion().get(request.callerWaitTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            return failOrStale(request, stale, UnifiedSourceStatus.DEGRADED, "PROVIDER_TIMEOUT");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return failOrStale(request, stale, UnifiedSourceStatus.ERROR, "PROVIDER_CALL_INTERRUPTED");
        } catch (ExecutionException failure) {
            return failOrStale(request, stale, UnifiedSourceStatus.ERROR, "PROVIDER_CALL_FAILED");
        }
    }

    private <T> ProviderCallResult<T> finishResponse(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderAdapterResponse<T> response) {
        ProviderSnapshotKey snapshotKey = request.key().snapshotKey();
        String provider = snapshotKey.provider();
        if (response != null && response.ready()) {
            circuitBreaker.recordSuccess(provider);
            healthRegistry.recordSuccess(snapshotKey, UnifiedSourceStatus.READY);
            Instant fetchTime = clock.instant();
            ProviderSnapshotMetadata metadata = metadata(request, response.providerDataTime(), fetchTime,
                    fetchTime.plus(request.freshTtl()), UnifiedSourceStatus.READY,
                    SnapshotFreshnessStatus.FRESH, false, false, null, List.of());
            cache.put(snapshotKey, response.payload(), metadata,
                    retentionPolicy.staleRetention(snapshotKey.datasetType()));
            return audited(request, new ProviderCallResult<>(response.payload(), metadata,
                    budget.state(provider, circuitBreaker.state(provider))));
        }
        if (response != null && response.sourceStatus() == UnifiedSourceStatus.EMPTY_CONFIRMED) {
            circuitBreaker.recordSuccess(provider);
            healthRegistry.recordSuccess(snapshotKey, UnifiedSourceStatus.EMPTY_CONFIRMED);
            Instant fetchTime = clock.instant();
            ProviderSnapshotMetadata metadata = metadata(request, response.providerDataTime(), fetchTime,
                    fetchTime.plus(request.freshTtl()), UnifiedSourceStatus.EMPTY_CONFIRMED,
                    SnapshotFreshnessStatus.FRESH, false, false, response.reasonCode(),
                    response.reasonCode() == null ? List.of() : List.of(response.reasonCode()));
            cache.put(snapshotKey, null, metadata,
                    retentionPolicy.staleRetention(snapshotKey.datasetType()));
            return audited(request, new ProviderCallResult<>(null, metadata,
                    budget.state(provider, circuitBreaker.state(provider))));
        }

        int status = response == null ? 0 : response.httpStatus();
        String reason = response == null || response.reasonCode() == null
                ? "PROVIDER_RESPONSE_MALFORMED" : response.reasonCode();
        if (status == 429) {
            budget.applyRetryAfter(provider, response.retryAfterSeconds() == null ? 60 : response.retryAfterSeconds());
        }
        ProviderFailureOrigin failureOrigin = ProviderFailureClassifier.classify(response);
        if (failureOrigin.affectsProviderCircuit()) circuitBreaker.recordFailure(provider);
        UnifiedSourceStatus sourceStatus = response == null ? UnifiedSourceStatus.ERROR : response.sourceStatus();
        if (failureOrigin.recordsRemoteHealthFailure()) {
            healthRegistry.recordFailure(snapshotKey,
                    sourceStatus == null ? UnifiedSourceStatus.ERROR : sourceStatus, reason);
        }
        return failOrStale(request, stale,
                sourceStatus == null ? UnifiedSourceStatus.ERROR : sourceStatus, reason);
    }

    private static boolean isRetryable5xx(ProviderAdapterResponse<?> response) {
        return response != null && response.httpStatus() >= 500 && response.httpStatus() <= 599;
    }

    private static boolean isTimeout(ProviderAdapterResponse<?> response) {
        return response != null && ("PROVIDER_TIMEOUT".equals(response.reasonCode())
                || "TIMEOUT".equals(response.reasonCode()));
    }

    private static Duration boundedBackoff(int attemptNumber) {
        long millis = Math.min(200L, 50L << Math.max(0, Math.min(2, attemptNumber - 2)));
        return Duration.ofMillis(millis);
    }

    private static Throwable unwrap(Throwable failure) {
        if (failure instanceof java.util.concurrent.CompletionException completion
                && completion.getCause() != null) return completion.getCause();
        return failure;
    }

    private <T> ProviderCallResult<T> cached(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> lookup,
            boolean fallback) {
        return cached(request, lookup, fallback,
                fallback ? SnapshotFreshnessStatus.STALE_READABLE : SnapshotFreshnessStatus.FRESH);
    }

    private <T> ProviderCallResult<T> cached(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> lookup,
            boolean fallback,
            SnapshotFreshnessStatus freshness) {
        ProviderSnapshotMetadata metadata = lookup.metadata().asCacheHit(
                freshness, fallback,
                lookup.metadata().fetchTime().plus(request.freshTtl()), clock.instant());
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
                SnapshotFreshnessStatus.UNAVAILABLE, false, false, reason, List.of(reason));
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
        Instant ageBasis = providerDataTime == null ? fetchTime : providerDataTime;
        long ageSeconds = ageBasis == null || fetchTime == null || fetchTime.isBefore(ageBasis)
                ? 0L : Duration.between(ageBasis, fetchTime).toSeconds();
        return new ProviderSnapshotMetadata(key.provider(), key.datasetType(), key.canonicalInstrumentId(),
                key.providerSymbol(), key.timeframe(), providerDataTime, fetchTime, expiresAt, ageSeconds,
                sourceStatus, freshness, request.traceId(), key.canonical(), key.sourceVersion(), cacheHit,
                fallback, errorCode, reasons);
    }

    private <T> ProviderCallResult<T> audited(ProviderCallRequest<T> request, ProviderCallResult<T> result) {
        ProviderSnapshotMetadata metadata = result.metadata();
        auditLog.record(new ProviderCallAuditEvent(request.traceId(), request.key().canonical(), request.priority(),
                request.baseProfile(), request.effectiveProfile(), request.profileReasonCodes(),
                request.frequencyMatrixVersion(), metadata.sourceStatus(), metadata.cacheHit(),
                metadata.fallbackUsed(), metadata.errorCode(), clock.instant()));
        return result;
    }

    private <T> void auditAttempt(ProviderCallRequest<T> request,
                                  String attemptId,
                                  int attemptNumber,
                                  ProviderCallAuditPhase phase,
                                  UnifiedSourceStatus status,
                                  String reasonCode) {
        auditLog.record(new ProviderCallAuditEvent(request.traceId(), request.key().canonical(), request.priority(),
                request.baseProfile(), request.effectiveProfile(), request.profileReasonCodes(),
                request.frequencyMatrixVersion(), status, false, false, reasonCode, clock.instant(),
                attemptId, attemptNumber, phase));
    }
}
