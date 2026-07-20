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
    private final LinkedHashMap<String, AttemptState> states = new LinkedHashMap<>(16, 0.75f, true);

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

    synchronized boolean isBlocked(String username) {
        Instant now = clock.instant();
        purgeExpired(now);
        AttemptState state = states.get(key(username));
        return state != null && state.blockedUntil != null && now.isBefore(state.blockedUntil);
    }

    synchronized FailureResult registerFailure(String username) {
        Instant now = clock.instant();
        purgeExpired(now);
        String key = key(username);
        AttemptState state = states.get(key);
        if (state == null) {
            ensureCapacity();
            state = new AttemptState(now);
            states.put(key, state);
        } else if (!now.isBefore(state.windowStartedAt.plus(failureWindow))) {
            state = new AttemptState(now);
            states.put(key, state);
        }

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

    public synchronized void reset(String username) {
        states.remove(key(username));
    }

    synchronized int trackedUsernameCount() {
        purgeExpired(clock.instant());
        return states.size();
    }

    synchronized int failureCount(String username) {
        purgeExpired(clock.instant());
        AttemptState state = states.get(key(username));
        return state == null ? 0 : state.failures;
    }

    private void purgeExpired(Instant now) {
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

    private void ensureCapacity() {
        while (states.size() >= maxTrackedUsernames) {
            Iterator<String> iterator = states.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
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
