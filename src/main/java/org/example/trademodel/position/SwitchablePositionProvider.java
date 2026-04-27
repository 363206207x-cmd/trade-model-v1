package org.example.trademodel.position;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SwitchablePositionProvider implements PositionProvider {

    private static final Logger log = LoggerFactory.getLogger(SwitchablePositionProvider.class);

    private final SimulatedPositionProvider simulatedPositionProvider;
    private final BinancePositionProvider binancePositionProvider;

    @Value("${position.provider.type:SIMULATED}")
    private String providerType;

    public SwitchablePositionProvider(SimulatedPositionProvider simulatedPositionProvider,
                                      BinancePositionProvider binancePositionProvider) {
        this.simulatedPositionProvider = simulatedPositionProvider;
        this.binancePositionProvider = binancePositionProvider;
    }

    @Override
    public PositionProviderResult fetchOpenPositions() {
        String type = normalizeType(providerType);
        if ("BINANCE".equals(type)) {
            log.info("[position-sync] provider.type=BINANCE");
            if (!binancePositionProvider.hasCredentials()) {
                String reason = "provider.type=BINANCE but credentials missing";
                log.warn("[position-sync] {}; fallback to SIMULATED", reason);
                return withDiagnostics(simulatedPositionProvider.fetchOpenPositions(), type, true, reason);
            }
            try {
                return withDiagnostics(binancePositionProvider.fetchOpenPositions(), type, false, null);
            } catch (Exception e) {
                String reason = "BINANCE provider failed: " + e.getMessage();
                log.error("[position-sync] BINANCE provider failed; fallback to SIMULATED err={}", e.getMessage());
                return withDiagnostics(simulatedPositionProvider.fetchOpenPositions(), type, true, reason);
            }
        }

        if (!"SIMULATED".equals(type)) {
            String reason = "unknown provider.type=" + providerType;
            log.warn("[position-sync] {} fallback to SIMULATED", reason);
            return withDiagnostics(simulatedPositionProvider.fetchOpenPositions(), type, true, reason);
        }
        log.info("[position-sync] provider.type=SIMULATED");
        return withDiagnostics(simulatedPositionProvider.fetchOpenPositions(), type, false, null);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "SIMULATED";
        }
        return type.trim().toUpperCase();
    }

    private PositionProviderResult withDiagnostics(PositionProviderResult sourceResult,
                                                   String configuredType,
                                                   boolean fallbackOccurred,
                                                   String fallbackReason) {
        return new PositionProviderResult(
                sourceResult.getSourceType(),
                sourceResult.getSourceName(),
                sourceResult.getOpenPositions(),
                configuredType,
                fallbackOccurred,
                fallbackReason
        );
    }
}
