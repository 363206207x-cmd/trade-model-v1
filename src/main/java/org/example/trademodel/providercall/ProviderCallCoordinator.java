package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
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
import java.util.concurrent.atomic.AtomicReference;

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
                return awaitOwnerResult(request, callerFallback, registration.flight());
            }
            return awaitAndRewrapWaiterResult(request, callerFallback, registration.flight());
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
        if (!properties.isEnabled() || !properties.isExternalCallsEnabled()) {
            flight.completion().complete(failOrStale(request, stale, UnifiedSourceStatus.DISABLED,
                    "PROVIDER_CALL_DISABLED"));
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
        ProviderCircuitPermit permit = circuitBreaker.tryAcquire(request.key().provider());
        if (!permit.acquired()) {
            completeCircuitOpen(request, stale, flight);
            return;
        }
        if (flight.completion().isDone()) {
            permit.releaseWithoutRemoteAttempt();
            return;
        }
        String attemptId = request.traceId() + "-attempt-" + attemptNumber + "-" + UUID.randomUUID();
        AttemptExecutionState executionState = new AttemptExecutionState();
        ProviderCallExecutor.TaskHandle<ProviderAdapterResponse<T>> handle;
        try {
            handle = callExecutor.submit(request.priority(),
                    () -> runPhysicalAttempt(request, attemptId, attemptNumber, executionState));
        } catch (RejectedExecutionException rejected) {
            ProviderAdapterResponse<T> response = ProviderAdapterResponse.failed(
                    UnifiedSourceStatus.DEGRADED, 0, "PROVIDER_EXECUTOR_REJECTED", null);
            settleCircuitPermit(permit, response);
            flight.completion().complete(finishResponse(request, stale, response));
            return;
        }

        Runnable cancellation = () -> {
            AttemptExecutionPhase timeoutPhase = executionState.requestTimeout();
            if (timeoutPhase == AttemptExecutionPhase.REMOTE_TIMEOUT_REQUESTED) {
                healthRegistry.recordFailure(request.key().snapshotKey(), UnifiedSourceStatus.DEGRADED,
                        "PROVIDER_TIMEOUT_PHYSICAL_PENDING");
            }
            if (timeoutPhase != null) handle.cancelInterruptibly();
        };
        ScheduledFuture<?> timeout;
        try {
            timeout = callExecutor.schedule(cancellation, request.physicalAttemptTimeout());
        } catch (RejectedExecutionException supervisorUnavailable) {
            handle.cancelInterruptibly();
            observeAttemptCompletion(request, stale, flight, attemptNumber, used5xxRetries,
                    usedTimeoutRetries, executionState, handle, null, permit);
            return;
        }

        observeAttemptCompletion(request, stale, flight, attemptNumber, used5xxRetries,
                usedTimeoutRetries, executionState, handle, timeout, permit);
    }

    private <T> void observeAttemptCompletion(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderRefreshFlight<ProviderCallResult<T>> flight,
            int attemptNumber,
            int used5xxRetries,
            int usedTimeoutRetries,
            AttemptExecutionState executionState,
            ProviderCallExecutor.TaskHandle<ProviderAdapterResponse<T>> handle,
            ScheduledFuture<?> timeout,
            ProviderCircuitPermit permit) {

        handle.completion().whenComplete((response, failure) -> {
            if (timeout != null) timeout.cancel(false);

            ProviderAdapterResponse<T> effective = response;
            Throwable cause = unwrap(failure);
            if (executionState.cancelledBeforeRemote()) {
                effective = ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0,
                        executionState.localTimeoutReason(), null);
            } else if (executionState.remoteTimeoutRequested()) {
                effective = ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                        "PROVIDER_TIMEOUT", null);
            } else if (cause != null) {
                effective = ProviderAdapterResponse.failed(UnifiedSourceStatus.ERROR, 0,
                        cause instanceof CancellationException ? "PROVIDER_CALL_CANCELLED" : "PROVIDER_CALL_FAILED",
                        null);
            }
            applyRetryAfter(request.key().provider(), effective);
            settleCircuitPermit(permit, effective);
            if (flight.completion().isDone()) return;

            boolean circuitAllowsRetry = circuitBreaker.state(request.key().provider()) != ProviderCircuitState.OPEN;
            if (circuitAllowsRetry && isTimeout(effective)
                    && usedTimeoutRetries < request.maxRetryTimeout()) {
                scheduleRetry(request, stale, flight, attemptNumber + 1, used5xxRetries,
                        usedTimeoutRetries + 1, effective);
                return;
            }
            if (circuitAllowsRetry && isRetryable5xx(effective)
                    && used5xxRetries < request.maxRetry5xx()) {
                scheduleRetry(request, stale, flight, attemptNumber + 1, used5xxRetries + 1,
                        usedTimeoutRetries, effective);
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
            int usedTimeoutRetries,
            ProviderAdapterResponse<T> terminalResponse) {
        try {
            callExecutor.schedule(() -> startAttempt(request, stale, flight, attemptNumber,
                    used5xxRetries, usedTimeoutRetries), boundedBackoff(attemptNumber));
        } catch (RejectedExecutionException retrySupervisorUnavailable) {
            if (!flight.completion().isDone()) {
                flight.completion().complete(finishResponse(request, stale, terminalResponse));
            }
        }
    }

    private <T> ProviderAdapterResponse<T> runPhysicalAttempt(
            ProviderCallRequest<T> request,
            String attemptId,
            int attemptNumber,
            AttemptExecutionState executionState) {
        if (!executionState.beginLocalAdmission()) return executionState.localTimeoutResponse();

        boolean attemptStartAudited = false;
        String completionReason = "PROVIDER_RESPONSE_MALFORMED";
        UnifiedSourceStatus completionStatus = UnifiedSourceStatus.ERROR;
        try {
            ProviderConcurrencyGuard.Lease lease = concurrencyGuard.tryAcquire(
                    request.key().datasetType(), request.priority());
            if (lease == null) {
                return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0,
                        "PROVIDER_CONCURRENCY_REJECTED", null);
            }
            try (lease) {
                if (executionState.cancelledBeforeRemote()) return executionState.localTimeoutResponse();
                if (!budget.reserveAttempt(request.key(), request.priority(), request.effectiveProfile(),
                        attemptNumber > 1)) {
                    return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0,
                            "PROVIDER_BUDGET_REJECTED", null);
                }
                if (executionState.cancelledBeforeRemote()) return executionState.localTimeoutResponse();
                auditAttempt(request, attemptId, attemptNumber, ProviderCallAuditPhase.PHYSICAL_ATTEMPT_STARTED,
                        UnifiedSourceStatus.WAITING_SYNC, "PHYSICAL_ATTEMPT_STARTED");
                attemptStartAudited = true;
                if (!executionState.beginRemoteAttempt()) {
                    completionReason = executionState.localTimeoutReason();
                    completionStatus = UnifiedSourceStatus.DEGRADED;
                    return executionState.localTimeoutResponse();
                }

                try {
                    ProviderAdapterResponse<T> response = request.adapterCall().get();
                    if (executionState.completeRemoteAttempt()) {
                        if (response != null) {
                            completionReason = response.reasonCode();
                            completionStatus = response.sourceStatus() == null
                                    ? UnifiedSourceStatus.ERROR : response.sourceStatus();
                        }
                    } else if (executionState.remoteTimeoutRequested()) {
                        completionReason = "PROVIDER_TIMEOUT_PHYSICAL_ENDED";
                    }
                    return response;
                } catch (RuntimeException failure) {
                    if (executionState.completeRemoteAttempt()) {
                        completionReason = "PROVIDER_CALL_FAILED";
                    } else if (executionState.remoteTimeoutRequested()) {
                        completionReason = "PROVIDER_TIMEOUT_PHYSICAL_ENDED";
                    }
                    throw failure;
                }
            }
        } finally {
            executionState.completeLocalAttempt();
            if (attemptStartAudited) {
                auditAttempt(request, attemptId, attemptNumber,
                        ProviderCallAuditPhase.PHYSICAL_ATTEMPT_COMPLETED, completionStatus,
                        completionReason == null ? "PHYSICAL_ATTEMPT_COMPLETED" : completionReason);
            }
        }
    }

    enum AttemptExecutionPhase {
        QUEUED,
        LOCAL_ADMISSION,
        REMOTE_IN_FLIGHT,
        REMOTE_TIMEOUT_REQUESTED,
        CANCELLED_BEFORE_REMOTE,
        COMPLETED
    }

    static final class AttemptExecutionState {
        private static final String QUEUE_TIMEOUT = "PROVIDER_EXECUTOR_QUEUE_TIMEOUT";
        private static final String PRE_REMOTE_TIMEOUT = "PROVIDER_PRE_REMOTE_TIMEOUT";

        private final AtomicReference<AttemptExecutionPhase> phase =
                new AtomicReference<>(AttemptExecutionPhase.QUEUED);
        private final AtomicReference<String> localTimeoutReason = new AtomicReference<>();

        boolean beginLocalAdmission() {
            return phase.compareAndSet(AttemptExecutionPhase.QUEUED, AttemptExecutionPhase.LOCAL_ADMISSION);
        }

        boolean beginRemoteAttempt() {
            return phase.compareAndSet(AttemptExecutionPhase.LOCAL_ADMISSION,
                    AttemptExecutionPhase.REMOTE_IN_FLIGHT);
        }

        AttemptExecutionPhase requestTimeout() {
            while (true) {
                AttemptExecutionPhase current = phase.get();
                if (current == AttemptExecutionPhase.QUEUED) {
                    if (cancelBeforeRemote(current, QUEUE_TIMEOUT)) {
                        return AttemptExecutionPhase.CANCELLED_BEFORE_REMOTE;
                    }
                    continue;
                }
                if (current == AttemptExecutionPhase.LOCAL_ADMISSION) {
                    if (cancelBeforeRemote(current, PRE_REMOTE_TIMEOUT)) {
                        return AttemptExecutionPhase.CANCELLED_BEFORE_REMOTE;
                    }
                    continue;
                }
                if (current == AttemptExecutionPhase.REMOTE_IN_FLIGHT) {
                    if (phase.compareAndSet(current, AttemptExecutionPhase.REMOTE_TIMEOUT_REQUESTED)) {
                        return AttemptExecutionPhase.REMOTE_TIMEOUT_REQUESTED;
                    }
                    continue;
                }
                return null;
            }
        }

        boolean completeRemoteAttempt() {
            return phase.compareAndSet(AttemptExecutionPhase.REMOTE_IN_FLIGHT,
                    AttemptExecutionPhase.COMPLETED);
        }

        void completeLocalAttempt() {
            phase.compareAndSet(AttemptExecutionPhase.LOCAL_ADMISSION, AttemptExecutionPhase.COMPLETED);
        }

        boolean cancelledBeforeRemote() {
            return phase.get() == AttemptExecutionPhase.CANCELLED_BEFORE_REMOTE;
        }

        boolean remoteTimeoutRequested() {
            return phase.get() == AttemptExecutionPhase.REMOTE_TIMEOUT_REQUESTED;
        }

        String localTimeoutReason() {
            String reason = localTimeoutReason.get();
            return reason == null ? PRE_REMOTE_TIMEOUT : reason;
        }

        <T> ProviderAdapterResponse<T> localTimeoutResponse() {
            return ProviderAdapterResponse.failed(UnifiedSourceStatus.DEGRADED, 0,
                    localTimeoutReason(), null);
        }

        private boolean cancelBeforeRemote(AttemptExecutionPhase expected, String reason) {
            localTimeoutReason.compareAndSet(null, reason);
            if (phase.compareAndSet(expected, AttemptExecutionPhase.CANCELLED_BEFORE_REMOTE)) return true;
            if (phase.get() != AttemptExecutionPhase.CANCELLED_BEFORE_REMOTE) {
                localTimeoutReason.compareAndSet(reason, null);
            }
            return false;
        }
    }

    private <T> ProviderCallResult<T> awaitOwnerResult(
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

    private <T> ProviderCallResult<T> awaitAndRewrapWaiterResult(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> callerFallback,
            ProviderRefreshFlight<ProviderCallResult<T>> flight) {
        try {
            ProviderCallResult<T> sharedResult = flight.completion().get(
                    request.callerWaitTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return adaptSharedFlightResultForCaller(request, callerFallback, sharedResult);
        } catch (TimeoutException timeout) {
            return failOrStale(request, callerFallback, UnifiedSourceStatus.DEGRADED, "PROVIDER_TIMEOUT");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return failOrStale(request, callerFallback, UnifiedSourceStatus.ERROR,
                    "PROVIDER_CALL_INTERRUPTED");
        } catch (ExecutionException failure) {
            return failOrStale(request, callerFallback, UnifiedSourceStatus.ERROR, "PROVIDER_CALL_FAILED");
        }
    }

    private <T> ProviderCallResult<T> adaptSharedFlightResultForCaller(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> callerFallback,
            ProviderCallResult<T> sharedResult) {
        SnapshotCacheService.SnapshotLookup<T> callerLookup = cache.lookup(
                request.key().snapshotKey(), clock.instant(), request.freshTtl());
        if (callerLookup.fresh()) return cached(request, callerLookup, false);
        if (callerLookup.staleReadable()) return cached(request, callerLookup, true);

        ProviderSnapshotMetadata sharedMetadata = sharedResult == null ? null : sharedResult.metadata();
        if (sharedMetadata != null && (sharedMetadata.sourceStatus() == UnifiedSourceStatus.READY
                || sharedMetadata.sourceStatus() == UnifiedSourceStatus.EMPTY_CONFIRMED)) {
            return failOrStale(request, null, UnifiedSourceStatus.DEGRADED,
                    "SNAPSHOT_EXPIRED_BEFORE_CALLER_REWRAP");
        }
        UnifiedSourceStatus status = sharedMetadata == null || sharedMetadata.sourceStatus() == null
                ? UnifiedSourceStatus.ERROR : sharedMetadata.sourceStatus();
        String reason = sharedMetadata == null || sharedMetadata.errorCode() == null
                ? "PROVIDER_CALL_FAILED" : sharedMetadata.errorCode();
        return failOrStale(request, callerFallback, status, reason);
    }

    private <T> ProviderCallResult<T> finishResponse(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderAdapterResponse<T> response) {
        ProviderSnapshotKey snapshotKey = request.key().snapshotKey();
        String provider = snapshotKey.provider();
        if (response != null && response.ready()) {
            Duration retention = validDatasetRetention(snapshotKey);
            if (retention == null) {
                return failOrStale(request, stale, UnifiedSourceStatus.ERROR,
                        "PROVIDER_SNAPSHOT_RETENTION_INVALID");
            }
            Instant fetchTime = clock.instant();
            Instant expiresAt = boundedExpiry(fetchTime, request.freshTtl(), retention);
            if (expiresAt == null || !expiresAt.isAfter(fetchTime)) {
                return failOrStale(request, stale, UnifiedSourceStatus.ERROR,
                        "PROVIDER_SNAPSHOT_RETENTION_INVALID");
            }
            healthRegistry.recordSuccess(snapshotKey, UnifiedSourceStatus.READY);
            ProviderSnapshotMetadata metadata = metadata(request, response.providerDataTime(), fetchTime,
                    expiresAt, UnifiedSourceStatus.READY,
                    SnapshotFreshnessStatus.FRESH, false, false, null, List.of());
            cache.put(snapshotKey, response.payload(), metadata, retention);
            return audited(request, new ProviderCallResult<>(response.payload(), metadata,
                    budget.state(provider, circuitBreaker.state(provider))));
        }
        if (response != null && response.sourceStatus() == UnifiedSourceStatus.EMPTY_CONFIRMED) {
            Duration retention = validDatasetRetention(snapshotKey);
            if (retention == null) {
                return failOrStale(request, stale, UnifiedSourceStatus.ERROR,
                        "PROVIDER_SNAPSHOT_RETENTION_INVALID");
            }
            Instant fetchTime = clock.instant();
            Instant expiresAt = boundedExpiry(fetchTime, request.freshTtl(), retention);
            if (expiresAt == null || !expiresAt.isAfter(fetchTime)) {
                return failOrStale(request, stale, UnifiedSourceStatus.ERROR,
                        "PROVIDER_SNAPSHOT_RETENTION_INVALID");
            }
            healthRegistry.recordSuccess(snapshotKey, UnifiedSourceStatus.EMPTY_CONFIRMED);
            ProviderSnapshotMetadata metadata = metadata(request, response.providerDataTime(), fetchTime,
                    expiresAt, UnifiedSourceStatus.EMPTY_CONFIRMED,
                    SnapshotFreshnessStatus.FRESH, false, false, response.reasonCode(),
                    response.reasonCode() == null ? List.of() : List.of(response.reasonCode()));
            cache.put(snapshotKey, null, metadata, retention);
            return audited(request, new ProviderCallResult<>(null, metadata,
                    budget.state(provider, circuitBreaker.state(provider))));
        }

        String reason = response == null || response.reasonCode() == null
                ? "PROVIDER_RESPONSE_MALFORMED" : response.reasonCode();
        ProviderFailureOrigin failureOrigin = ProviderFailureClassifier.classify(response);
        UnifiedSourceStatus sourceStatus = response == null ? UnifiedSourceStatus.ERROR : response.sourceStatus();
        if (failureOrigin.recordsRemoteHealthFailure()) {
            healthRegistry.recordFailure(snapshotKey,
                    sourceStatus == null ? UnifiedSourceStatus.ERROR : sourceStatus, reason);
        }
        return failOrStale(request, stale,
                sourceStatus == null ? UnifiedSourceStatus.ERROR : sourceStatus, reason);
    }

    private void applyRetryAfter(String provider, ProviderAdapterResponse<?> response) {
        if (response != null && response.httpStatus() == 429) {
            budget.applyRetryAfter(provider,
                    response.retryAfterSeconds() == null ? 60 : response.retryAfterSeconds());
        }
    }

    private static void settleCircuitPermit(ProviderCircuitPermit permit, ProviderAdapterResponse<?> response) {
        if (response != null && (response.ready()
                || response.sourceStatus() == UnifiedSourceStatus.EMPTY_CONFIRMED)) {
            permit.recordSuccess();
            return;
        }
        ProviderFailureOrigin origin = ProviderFailureClassifier.classify(response);
        if (origin.affectsProviderCircuit()) {
            permit.recordRemoteFailure();
        } else if (origin == ProviderFailureOrigin.REMOTE_RATE_LIMIT
                || origin == ProviderFailureOrigin.REMOTE_AUTH) {
            permit.recordRemoteReachable();
        } else {
            permit.releaseWithoutRemoteAttempt();
        }
    }

    private <T> void completeCircuitOpen(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            ProviderRefreshFlight<ProviderCallResult<T>> flight) {
        ProviderSnapshotKey snapshotKey = request.key().snapshotKey();
        healthRegistry.recordFailure(snapshotKey, UnifiedSourceStatus.DEGRADED, "PROVIDER_CIRCUIT_OPEN");
        flight.completion().complete(failOrStale(request, stale, UnifiedSourceStatus.DEGRADED,
                "PROVIDER_CIRCUIT_OPEN"));
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
        ProviderSnapshotMetadata metadata = callerCacheMetadata(request, lookup.metadata(), freshness, fallback);
        return audited(request, new ProviderCallResult<>(lookup.payload(), metadata,
                budget.state(request.key().provider(), circuitBreaker.state(request.key().provider()))));
    }

    private <T> ProviderSnapshotMetadata callerCacheMetadata(
            ProviderCallRequest<T> request,
            ProviderSnapshotMetadata stored,
            SnapshotFreshnessStatus freshness,
            boolean fallback) {
        ProviderRequestKey key = request.key();
        Instant asOf = clock.instant();
        Instant ageBasis = stored.providerDataTime() == null ? stored.fetchTime() : stored.providerDataTime();
        long ageSeconds = ageBasis == null || asOf.isBefore(ageBasis)
                ? 0L : Duration.between(ageBasis, asOf).toSeconds();
        return new ProviderSnapshotMetadata(key.provider(), key.datasetType(), key.canonicalInstrumentId(),
                key.providerSymbol(), key.timeframe(), stored.providerDataTime(), stored.fetchTime(),
                boundedExpiry(key.snapshotKey(), stored.fetchTime(), request.freshTtl()), ageSeconds,
                fallback ? UnifiedSourceStatus.STALE : stored.sourceStatus(), freshness, request.traceId(),
                key.canonical(), key.sourceVersion(), true, fallback, stored.errorCode(), stored.reasonCodes());
    }

    private Instant boundedExpiry(
            ProviderSnapshotKey snapshotKey,
            Instant fetchTime,
            Duration requestedFreshTtl) {
        Duration retention = validDatasetRetention(snapshotKey);
        if (retention == null) return fetchTime;
        return boundedExpiry(fetchTime, requestedFreshTtl, retention);
    }

    private static Instant boundedExpiry(
            Instant fetchTime,
            Duration requestedFreshTtl,
            Duration retention) {
        if (fetchTime == null || retention == null || retention.isZero() || retention.isNegative()) {
            return fetchTime;
        }
        Instant retentionExpiry;
        try {
            retentionExpiry = fetchTime.plus(retention);
        } catch (DateTimeException | ArithmeticException invalidRetentionBoundary) {
            return fetchTime;
        }
        if (requestedFreshTtl == null || requestedFreshTtl.isZero() || requestedFreshTtl.isNegative()) {
            return fetchTime;
        }
        try {
            Instant requestedExpiry = fetchTime.plus(requestedFreshTtl);
            return requestedExpiry.isBefore(retentionExpiry) ? requestedExpiry : retentionExpiry;
        } catch (DateTimeException | ArithmeticException invalidRequestedExpiry) {
            return retentionExpiry;
        }
    }

    private Duration validDatasetRetention(ProviderSnapshotKey snapshotKey) {
        if (snapshotKey == null || snapshotKey.datasetType() == null) return null;
        try {
            Duration retention = retentionPolicy.staleRetention(snapshotKey.datasetType());
            return retention == null || retention.isZero() || retention.isNegative() ? null : retention;
        } catch (RuntimeException invalidRetentionPolicy) {
            return null;
        }
    }

    private <T> ProviderCallResult<T> failOrStale(
            ProviderCallRequest<T> request,
            SnapshotCacheService.SnapshotLookup<T> stale,
            UnifiedSourceStatus status,
            String reason) {
        if (!ProviderFailureClassifier.isRegionRestricted(0, reason)
                && stale != null && stale.staleReadable()) {
            return cached(request, stale, true);
        }
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
