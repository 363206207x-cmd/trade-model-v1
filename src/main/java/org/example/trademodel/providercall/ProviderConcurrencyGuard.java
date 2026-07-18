package org.example.trademodel.providercall;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProviderConcurrencyGuard {
    private final int maxProviderCalls;
    private final int maxAiCalls;
    private final int maxQueuedCalls;
    private final int reservedPrioritySlots;
    private int activeProviderCalls;
    private int activeAiCalls;
    private int rejectedCalls;

    @Autowired
    public ProviderConcurrencyGuard(ProviderCallProperties properties) {
        this(properties.getMaxConcurrentProviderCalls(), properties.getMaxConcurrentAiCalls(),
                properties.getMaxQueuedCalls(), properties.getReservedPrioritySlots());
    }

    public ProviderConcurrencyGuard(int maxProviderCalls, int maxAiCalls,
                                    int maxQueuedCalls, int reservedPrioritySlots) {
        this.maxProviderCalls = Math.max(1, maxProviderCalls);
        this.maxAiCalls = Math.max(1, maxAiCalls);
        this.maxQueuedCalls = Math.max(1, maxQueuedCalls);
        this.reservedPrioritySlots = Math.max(0, Math.min(this.maxProviderCalls - 1, reservedPrioritySlots));
    }

    public synchronized Lease tryAcquire(ProviderDatasetType datasetType, AssetPriority priority) {
        boolean ai = datasetType == ProviderDatasetType.AI_REVIEW;
        int priorityLimit = switch (priority) {
            case P3_DISCOVERY -> Math.max(1, maxProviderCalls - reservedPrioritySlots);
            case P1_WATCHLIST -> Math.max(1, maxProviderCalls - Math.max(1, reservedPrioritySlots / 2));
            case P2_CANDIDATE, P0_POSITION -> maxProviderCalls;
        };
        if (activeProviderCalls >= priorityLimit || (ai && activeAiCalls >= maxAiCalls)) {
            rejectedCalls++;
            return null;
        }
        activeProviderCalls++;
        if (ai) activeAiCalls++;
        return new Lease(this, ai);
    }

    public synchronized ConcurrencyState state() {
        return new ConcurrencyState(maxProviderCalls, maxAiCalls, maxQueuedCalls,
                activeProviderCalls, activeAiCalls, rejectedCalls, reservedPrioritySlots);
    }

    private synchronized void release(boolean ai) {
        activeProviderCalls = Math.max(0, activeProviderCalls - 1);
        if (ai) activeAiCalls = Math.max(0, activeAiCalls - 1);
    }

    public static final class Lease implements AutoCloseable {
        private final ProviderConcurrencyGuard owner;
        private final boolean ai;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Lease(ProviderConcurrencyGuard owner, boolean ai) {
            this.owner = owner;
            this.ai = ai;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) owner.release(ai);
        }
    }

    public record ConcurrencyState(
            int maxProviderCalls,
            int maxAiCalls,
            int maxQueuedCalls,
            int activeProviderCalls,
            int activeAiCalls,
            int rejectedCalls,
            int reservedPrioritySlots
    ) {
    }
}
