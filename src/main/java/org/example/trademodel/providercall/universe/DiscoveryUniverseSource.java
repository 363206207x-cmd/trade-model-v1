package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.List;

public interface DiscoveryUniverseSource {
    List<CanonicalInstrumentId> currentDiscoveryUniverse();
}
