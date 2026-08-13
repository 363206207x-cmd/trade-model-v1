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

    @Value("${position.provider.type:DISABLED}")
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
                log.warn("[position-sync] {}; fail closed", reason);
                return unavailable(type, true, reason);
            }
            try {
                return withDiagnostics(binancePositionProvider.fetchOpenPositions(), type, false, null);
            } catch (Exception e) {
                String reason = "BINANCE provider failed: " + e.getMessage();
                log.error("[position-sync] BINANCE provider failed; fail closed err={}", e.getMessage());
                return unavailable(type, true, reason);
            }
        }

        if ("SIMULATED".equals(type)) {
            log.warn("[position-sync] provider.type=SIMULATED is non-authoritative and disabled");
            return withDiagnostics(simulatedPositionProvider.fetchOpenPositions(), type, false, null);
        }
        if ("DISABLED".equals(type)) {
            return unavailable(type, false, "POSITION_PROVIDER_DISABLED");
        }
        String reason = "unknown provider.type=" + providerType;
        log.warn("[position-sync] {}; fail closed", reason);
        return unavailable(type, true, reason);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "DISABLED";
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
                fallbackOccurred || sourceResult.isFallbackOccurred(),
                fallbackReason != null ? fallbackReason : sourceResult.getFallbackReason(),
                sourceResult.isAuthoritativeSnapshot()
                        && !fallbackOccurred
                        && !sourceResult.isFallbackOccurred()
        );
    }

    private PositionProviderResult unavailable(String configuredType,
                                               boolean fallbackOccurred,
                                               String reason) {
        return new PositionProviderResult(
                "UNAVAILABLE",
                "fail-closed-position-provider",
                java.util.List.of(),
                configuredType,
                fallbackOccurred,
                reason,
                false
        );
    }
}
