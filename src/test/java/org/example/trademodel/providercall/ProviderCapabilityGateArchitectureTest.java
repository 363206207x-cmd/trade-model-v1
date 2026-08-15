package org.example.trademodel.providercall;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderCapabilityGateArchitectureTest {
    private static final Path ROOT = Path.of("src/main/java/org/example/trademodel");
    private static final Set<String> DIRECT_CLIENT_OWNERS = Set.of(
            "market/client/MarketQuoteClient.java",
            "market/client/OpenInterestClient.java",
            "market/client/PerpFundingRateClient.java",
            "market/client/impl/BinanceMarketQuoteClient.java",
            "market/client/impl/BinanceUsdtMOpenInterestClient.java",
            "market/client/impl/BinanceUsdtMPerpFundingClient.java",
            "providercall/snapshot/MarketPriceSnapshotService.java",
            "providercall/snapshot/BinanceDerivativesSnapshotService.java");
    private static final Set<String> RAW_PUBLIC_HTTP_OWNERS = Set.of(
            "service/RealMarketDataFetcherService.java",
            "market/client/impl/BinancePublicOhlcvProvider.java",
            "market/client/impl/KrakenPublicOhlcvProvider.java",
            "market/client/impl/KrakenPairResolver.java");

    @Test
    void productionBusinessServicesAndControllersCannotDependOnDirectProviderClients() throws Exception {
        assertOnlyOwnedBy("MarketQuoteClient", DIRECT_CLIENT_OWNERS);
        assertOnlyOwnedBy("OpenInterestClient", DIRECT_CLIENT_OWNERS);
        assertOnlyOwnedBy("PerpFundingRateClient", DIRECT_CLIENT_OWNERS);
    }

    @Test
    void rawOhlcvTransportCallsRemainInsideProviderAndDirectoryOwners() throws Exception {
        assertOnlyOwnedBy("fetchKlinesDetailed(", RAW_PUBLIC_HTTP_OWNERS);
        assertOnlyOwnedBy("fetchPublicJson(", RAW_PUBLIC_HTTP_OWNERS);
    }

    @Test
    void everyProductionMarketAdapterConsumesTheUnifiedCapabilityOwner() throws Exception {
        for (String file : List.of(
                "market/client/impl/RoutedPublicOhlcvProvider.java",
                "providercall/snapshot/CoordinatedOhlcvSnapshotService.java",
                "providercall/snapshot/MarketPriceSnapshotService.java",
                "providercall/snapshot/BinanceDerivativesSnapshotService.java",
                "providercall/scan/DefaultProviderDatasetRefreshPort.java")) {
            assertThat(Files.readString(ROOT.resolve(file))).as(file)
                    .contains("ProviderCapabilityRegistry");
        }
    }

    private static void assertOnlyOwnedBy(String token, Set<String> allowed) throws Exception {
        try (var files = Files.walk(ROOT)) {
            for (Path file : files.filter(value -> value.toString().endsWith(".java")).toList()) {
                String relative = ROOT.relativize(file).toString();
                if (!Files.readString(file).contains(token)) continue;
                assertThat(allowed).as("direct provider token in " + relative).contains(relative);
            }
        }
    }
}
