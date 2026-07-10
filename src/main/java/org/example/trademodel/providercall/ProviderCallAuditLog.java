package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ProviderCallAuditLog {
    private final CopyOnWriteArrayList<ProviderCallAuditEvent> events = new CopyOnWriteArrayList<>();

    public void record(ProviderCallAuditEvent event) {
        events.add(event);
    }

    public List<ProviderCallAuditEvent> snapshot() {
        return List.copyOf(events);
    }
}
