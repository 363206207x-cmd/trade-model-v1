package org.example.trademodel.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptService {
    enum FailureResult {
        FAILURE_RECORDED,
        TEMPORARILY_BLOCKED
    }

    private final int failureThreshold;
    private final Duration failureWindow;
    private final Duration lockDuration;
    private final int maxTrackedUsernames;
    private final Clock clock;
    private final LinkedHashMap<String, AttemptState> knownUserStates = new LinkedHashMap<>();
    private final LinkedHashMap<String, AttemptState> unknownUsernameStates =
            new LinkedHashMap<>(16, 0.75f, true);
    private Instant knownStateCapacityBlockedUntil;

    @Autowired
    public LoginAttemptService(
            @Value("${trade-model.auth.login-attempts.failure-threshold:5}") int failureThreshold,
            @Value("${trade-model.auth.login-attempts.window-minutes:15}") long windowMinutes,
            @Value("${trade-model.auth.login-attempts.lock-minutes:15}") long lockMinutes,
            @Value("${trade-model.auth.login-attempts.max-tracked-usernames:1024}") int maxTrackedUsernames) {
        this(failureThreshold, Duration.ofMinutes(windowMinutes), Duration.ofMinutes(lockMinutes),
                maxTrackedUsernames, Clock.systemUTC());
    }

    LoginAttemptService(int failureThreshold,
                        Duration failureWindow,
                        Duration lockDuration,
                        int maxTrackedUsernames,
                        Clock clock) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.failureWindow = positive(failureWindow, Duration.ofMinutes(15));
        this.lockDuration = positive(lockDuration, Duration.ofMinutes(15));
        this.maxTrackedUsernames = Math.max(1, maxTrackedUsernames);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    synchronized boolean isKnownUserBlocked(String username) {
        Instant now = clock.instant();
        purgeExpired(knownUserStates, now);
        if (isKnownStateCapacityBlocked(now)) {
            return true;
        }
        AttemptState state = knownUserStates.get(key(username));
        return state != null && state.blockedUntil != null && now.isBefore(state.blockedUntil);
    }

    synchronized FailureResult registerKnownUserFailure(String username) {
        Instant now = clock.instant();
        purgeExpired(knownUserStates, now);
        if (isKnownStateCapacityBlocked(now)) {
            return FailureResult.TEMPORARILY_BLOCKED;
        }
        String key = key(username);
        AttemptState state = knownUserStates.get(key);
        if (state == null) {
            if (knownUserStates.size() >= maxTrackedUsernames) {
                knownStateCapacityBlockedUntil = now.plus(lockDuration);
                return FailureResult.TEMPORARILY_BLOCKED;
            }
            state = new AttemptState(now);
            knownUserStates.put(key, state);
        } else if (!now.isBefore(state.windowStartedAt.plus(failureWindow))) {
            state = new AttemptState(now);
            knownUserStates.put(key, state);
        }

        return registerFailure(state, now);
    }

    synchronized FailureResult registerUnknownUsernameFailure(String username) {
        Instant now = clock.instant();
        purgeExpired(unknownUsernameStates, now);
        String key = key(username);
        AttemptState state = unknownUsernameStates.get(key);
        if (state == null) {
            ensureUnknownCapacity();
            state = new AttemptState(now);
            unknownUsernameStates.put(key, state);
        } else if (!now.isBefore(state.windowStartedAt.plus(failureWindow))) {
            state = new AttemptState(now);
            unknownUsernameStates.put(key, state);
        }

        return registerFailure(state, now);
    }

    private FailureResult registerFailure(AttemptState state, Instant now) {

        if (state.blockedUntil != null && now.isBefore(state.blockedUntil)) {
            return FailureResult.TEMPORARILY_BLOCKED;
        }

        state.failures++;
        if (state.failures >= failureThreshold) {
            state.blockedUntil = now.plus(lockDuration);
            return FailureResult.TEMPORARILY_BLOCKED;
        }
        return FailureResult.FAILURE_RECORDED;
    }

    public synchronized void resetKnownUser(String username) {
        knownUserStates.remove(key(username));
    }

    synchronized void resetUnknownUsername(String username) {
        unknownUsernameStates.remove(key(username));
    }

    synchronized int knownUserStateCount() {
        purgeExpired(knownUserStates, clock.instant());
        return knownUserStates.size();
    }

    synchronized int unknownUsernameStateCount() {
        purgeExpired(unknownUsernameStates, clock.instant());
        return unknownUsernameStates.size();
    }

    synchronized int knownUserFailureCount(String username) {
        purgeExpired(knownUserStates, clock.instant());
        AttemptState state = knownUserStates.get(key(username));
        return state == null ? 0 : state.failures;
    }

    private void purgeExpired(Map<String, AttemptState> states, Instant now) {
        Iterator<Map.Entry<String, AttemptState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            AttemptState state = iterator.next().getValue();
            boolean lockExpired = state.blockedUntil != null && !now.isBefore(state.blockedUntil);
            boolean windowExpired = !now.isBefore(state.windowStartedAt.plus(failureWindow));
            if (lockExpired || (state.blockedUntil == null && windowExpired)) {
                iterator.remove();
            }
        }
    }

    private void ensureUnknownCapacity() {
        while (unknownUsernameStates.size() >= maxTrackedUsernames) {
            Iterator<String> iterator = unknownUsernameStates.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private boolean isKnownStateCapacityBlocked(Instant now) {
        if (knownStateCapacityBlockedUntil == null) {
            return false;
        }
        if (!now.isBefore(knownStateCapacityBlockedUntil)) {
            knownStateCapacityBlockedUntil = null;
            return false;
        }
        return true;
    }

    private static Duration positive(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }

    private static String key(String username) {
        String normalized = PersonalUsernamePolicy.normalize(username);
        return normalized.isEmpty() ? "<blank>" : normalized;
    }

    private static final class AttemptState {
        private final Instant windowStartedAt;
        private int failures;
        private Instant blockedUntil;

        private AttemptState(Instant windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }
    }
}
