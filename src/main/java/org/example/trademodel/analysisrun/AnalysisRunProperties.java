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
        private List<String> symbols = new ArrayList<>(List.of("BTCUSDT"));
        private List<String> timeframes = new ArrayList<>(List.of("1m"));

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getInitialDelayMs() { return initialDelayMs; }
        public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = Math.max(1000L, initialDelayMs); }
        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = Math.max(1000L, fixedDelayMs); }
        public List<String> getSymbols() { return symbols; }
        public void setSymbols(List<String> symbols) { this.symbols = normalize(symbols, List.of("BTCUSDT")); }
        public List<String> getTimeframes() { return timeframes; }
        public void setTimeframes(List<String> timeframes) { this.timeframes = normalize(timeframes, List.of("1m")); }

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
