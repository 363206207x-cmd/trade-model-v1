package org.example.trademodel.service;

import org.example.trademodel.vo.RunBaselineVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RuntimeMetricService {

    private final Map<String, MetricState> metrics = new ConcurrentHashMap<>();

    public void recordDuration(String metricName, long durationMs) {
        if (metricName == null || metricName.trim().isEmpty()) {
            return;
        }
        MetricState state = metrics.computeIfAbsent(metricName.trim(), ignored -> new MetricState());
        state.lastDurationMs.set(durationMs);
        state.totalDurationMs.addAndGet(durationMs);
        state.sampleCount.incrementAndGet();
    }

    public Map<String, RunBaselineVO.RuntimeMetricSnapshot> snapshot() {
        Map<String, RunBaselineVO.RuntimeMetricSnapshot> out = new LinkedHashMap<>();
        for (Map.Entry<String, MetricState> entry : metrics.entrySet()) {
            MetricState state = entry.getValue();
            long sampleCount = state.sampleCount.get();
            RunBaselineVO.RuntimeMetricSnapshot snapshot = new RunBaselineVO.RuntimeMetricSnapshot();
            snapshot.setLastDurationMs(state.lastDurationMs.get());
            snapshot.setSampleCount(sampleCount);
            snapshot.setAvgDurationMs(sampleCount <= 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(state.totalDurationMs.get())
                    .divide(BigDecimal.valueOf(sampleCount), 2, RoundingMode.HALF_UP));
            out.put(entry.getKey(), snapshot);
        }
        return out;
    }

    private static final class MetricState {
        private final AtomicLong lastDurationMs = new AtomicLong();
        private final AtomicLong totalDurationMs = new AtomicLong();
        private final AtomicLong sampleCount = new AtomicLong();
    }
}
