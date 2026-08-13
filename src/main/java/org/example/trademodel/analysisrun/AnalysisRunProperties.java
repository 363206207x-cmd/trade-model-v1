package org.example.trademodel.analysisrun;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "trade-model.analysis")
public class AnalysisRunProperties {
    private Scheduler scheduler = new Scheduler();
    private Idempotency idempotency = new Idempotency();

    public Scheduler getScheduler() { return scheduler; }
    public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler != null ? scheduler : new Scheduler(); }
    public Idempotency getIdempotency() { return idempotency; }
    public void setIdempotency(Idempotency idempotency) { this.idempotency = idempotency != null ? idempotency : new Idempotency(); }

    public static class Scheduler {
        private boolean enabled = false;
        private long initialDelayMs = 60000L;
        private long fixedDelayMs = 60000L;
        /**
         * Legacy configuration surface retained for compatible binding only.
         * Persistent analysis scheduling is sourced exclusively from Asset Pool.
         */
        private List<String> symbols = new ArrayList<>();
        private List<String> timeframes = new ArrayList<>(List.of("5m", "15m", "1h", "4h"));
        private List<String> requiredMarketTimeframes = new ArrayList<>();
        private int requiredClosedBars;
        private long observingIntervalSeconds = 900L;
        private long candidateIntervalSeconds = 300L;
        private long waitingTriggerIntervalSeconds = 120L;
        private long triggeredIntervalSeconds = 60L;
        private long highRiskIntervalSeconds = 60L;
        private long invalidatedIntervalSeconds = 900L;
        private long coolingIntervalSeconds = 900L;
        private long confusedIntervalSeconds = 120L;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getInitialDelayMs() { return initialDelayMs; }
        public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = Math.max(1000L, initialDelayMs); }
        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = Math.max(1000L, fixedDelayMs); }
        public List<String> getSymbols() { return symbols; }
        public void setSymbols(List<String> symbols) { this.symbols = normalize(symbols, List.of()); }
        public List<String> getTimeframes() { return timeframes; }
        public void setTimeframes(List<String> timeframes) {
            this.timeframes = normalize(timeframes, List.of("5m", "15m", "1h", "4h"));
        }
        public List<String> getRequiredMarketTimeframes() { return requiredMarketTimeframes; }
        public void setRequiredMarketTimeframes(List<String> values) {
            this.requiredMarketTimeframes = normalize(values, List.of());
        }
        public int getRequiredClosedBars() { return requiredClosedBars; }
        public void setRequiredClosedBars(int requiredClosedBars) {
            this.requiredClosedBars = Math.max(0, requiredClosedBars);
        }
        public long getObservingIntervalSeconds() { return observingIntervalSeconds; }
        public void setObservingIntervalSeconds(long value) { this.observingIntervalSeconds = value; }
        public long getCandidateIntervalSeconds() { return candidateIntervalSeconds; }
        public void setCandidateIntervalSeconds(long value) { this.candidateIntervalSeconds = value; }
        public long getWaitingTriggerIntervalSeconds() { return waitingTriggerIntervalSeconds; }
        public void setWaitingTriggerIntervalSeconds(long value) { this.waitingTriggerIntervalSeconds = value; }
        public long getTriggeredIntervalSeconds() { return triggeredIntervalSeconds; }
        public void setTriggeredIntervalSeconds(long value) { this.triggeredIntervalSeconds = value; }
        public long getHighRiskIntervalSeconds() { return highRiskIntervalSeconds; }
        public void setHighRiskIntervalSeconds(long value) { this.highRiskIntervalSeconds = value; }
        public long getInvalidatedIntervalSeconds() { return invalidatedIntervalSeconds; }
        public void setInvalidatedIntervalSeconds(long value) { this.invalidatedIntervalSeconds = value; }
        public long getCoolingIntervalSeconds() { return coolingIntervalSeconds; }
        public void setCoolingIntervalSeconds(long value) { this.coolingIntervalSeconds = value; }
        public long getConfusedIntervalSeconds() { return confusedIntervalSeconds; }
        public void setConfusedIntervalSeconds(long value) { this.confusedIntervalSeconds = value; }

        public boolean cadenceConfigured() {
            return observingIntervalSeconds > 0
                    && candidateIntervalSeconds > 0
                    && waitingTriggerIntervalSeconds > 0
                    && triggeredIntervalSeconds > 0
                    && highRiskIntervalSeconds > 0
                    && invalidatedIntervalSeconds > 0
                    && coolingIntervalSeconds > 0
                    && confusedIntervalSeconds > 0;
        }

        public long intervalSeconds(String state) {
            return switch (state == null ? "OBSERVING" : state.trim().toUpperCase()) {
                case "CANDIDATE" -> candidateIntervalSeconds;
                case "WAITING_TRIGGER" -> waitingTriggerIntervalSeconds;
                case "TRIGGERED" -> triggeredIntervalSeconds;
                case "HIGH_RISK" -> highRiskIntervalSeconds;
                case "INVALIDATED" -> invalidatedIntervalSeconds;
                case "COOLING" -> coolingIntervalSeconds;
                case "CONFUSED" -> confusedIntervalSeconds;
                default -> observingIntervalSeconds;
            };
        }

        private static List<String> normalize(List<String> raw, List<String> defaults) {
            List<String> out = new ArrayList<>();
            if (raw != null) {
                for (String value : raw) {
                    if (value == null) {
                        continue;
                    }
                    for (String part : value.split(",")) {
                        String t = part.trim();
                        if (!t.isEmpty()) {
                            out.add(t);
                        }
                    }
                }
            }
            return out.isEmpty() ? new ArrayList<>(defaults) : out;
        }
    }

    public static class Idempotency {
        private long leaseSeconds = 120L;
        private int maxRecoveryAttempts = 3;

        public long getLeaseSeconds() { return leaseSeconds; }
        public void setLeaseSeconds(long leaseSeconds) { this.leaseSeconds = Math.max(10L, leaseSeconds); }
        public int getMaxRecoveryAttempts() { return maxRecoveryAttempts; }
        public void setMaxRecoveryAttempts(int maxRecoveryAttempts) { this.maxRecoveryAttempts = Math.max(1, maxRecoveryAttempts); }
    }
}
