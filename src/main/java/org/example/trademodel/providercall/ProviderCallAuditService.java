package org.example.trademodel.providercall;

import java.util.List;

public interface ProviderCallAuditService {
    void record(ProviderCallAuditEvent event);
    List<ProviderCallAuditEvent> snapshot();
}
