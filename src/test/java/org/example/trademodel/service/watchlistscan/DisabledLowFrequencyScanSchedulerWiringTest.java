package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class DisabledLowFrequencyScanSchedulerWiringTest {

    @Test
    void disabledDefaultFailsClosed() {
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring();
        RuntimeSourceReadRequestDTO request = watchlistRequest("BTCUSDT");

        WatchlistScanResultDTO result = wiring.runOnce(request);

        assertThat(wiring.isEnabled()).isFalse();
        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getBlockingReasons()).contains("SCHEDULER_WIRING_DISABLED_BY_DEFAULT");
        assertSafeDefaults(result);
    }

    @Test
    void nullRequestFailsClosed() {
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring(
                request -> WatchlistScanResultDTO.reviewOnly("BTCUSDT", List.of("SHOULD_NOT_RUN")),
                true
        );

        WatchlistScanResultDTO result = wiring.runOnce(null);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getSymbol()).isNull();
        assertThat(result.getBlockingReasons()).contains("REQUEST_MISSING", "SCHEDULER_WIRING_BLOCKED");
        assertSafeDefaults(result);
    }

    @Test
    void nonWatchlistPoolOnlyRequestFailsClosed() {
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring(
                request -> WatchlistScanResultDTO.reviewOnly("BTCUSDT", List.of("SHOULD_NOT_RUN")),
                true
        );

        WatchlistScanResultDTO result = wiring.runOnce(requestWithWatchlistPoolOnly("BTCUSDT", false));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getBlockingReasons()).contains("WATCHLIST_POOL_ONLY_REQUIRED");
        assertSafeDefaults(result);
    }

    @Test
    void missingOrchestratorFailsClosed() {
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring(null, true);

        WatchlistScanResultDTO result = wiring.runOnce(watchlistRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("ORCHESTRATOR_MISSING");
        assertSafeDefaults(result);
    }

    @Test
    void orchestratorExceptionFailsClosed() {
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring(
                request -> {
                    throw new IllegalStateException("boom");
                },
                true
        );

        WatchlistScanResultDTO result = wiring.runOnce(watchlistRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("ORCHESTRATOR_FAILED");
        assertSafeDefaults(result);
    }

    @Test
    void orchestratorNullResultFailsClosed() {
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring(
                request -> null,
                true
        );

        WatchlistScanResultDTO result = wiring.runOnce(watchlistRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("ORCHESTRATOR_RESULT_MISSING");
        assertSafeDefaults(result);
    }

    @Test
    void normalOrchestratorResultIsReturnedSafely() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(
                WatchlistScanResultDTO.reviewOnly("ETHUSDT", List.of("REVIEW_ONLY_FROM_ORCHESTRATOR"))
        );
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring(
                orchestrator,
                true
        );
        RuntimeSourceReadRequestDTO request = watchlistRequest("ETHUSDT");

        WatchlistScanResultDTO result = wiring.runOnce(request);

        assertThat(orchestrator.capturedRequest).isSameAs(request);
        assertThat(result).isSameAs(orchestrator.result);
        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).contains("REVIEW_ONLY_FROM_ORCHESTRATOR");
        assertSafeDefaults(result);
    }

    @Test
    void unsafeOrchestratorResultFailsClosed() {
        DisabledLowFrequencyScanSchedulerWiring wiring = new DisabledLowFrequencyScanSchedulerWiring(
                request -> WatchlistScanResultDTO.candidateAttentionReviewOnly(
                        "BTCUSDT",
                        List.of("UNAUTHORIZED_CANDIDATE_ATTENTION")
                ),
                true
        );

        WatchlistScanResultDTO result = wiring.runOnce(watchlistRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("ORCHESTRATOR_RESULT_UNSAFE");
        assertSafeDefaults(result);
    }

    @Test
    void declaresNoBatchOrScheduledMethods() {
        Set<String> methodNames = Arrays.stream(DisabledLowFrequencyScanSchedulerWiring.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methodNames).contains("runOnce");
        assertThat(methodNames).noneMatch(name -> name.toLowerCase().contains("batch"));
        assertThat(Arrays.stream(DisabledLowFrequencyScanSchedulerWiring.class.getDeclaredMethods()))
                .noneMatch(method -> method.isAnnotationPresent(Scheduled.class));
    }

    @Test
    void declaresNoForbiddenRuntimeFields() {
        Set<String> fieldTypeNames = Arrays.stream(DisabledLowFrequencyScanSchedulerWiring.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName)
                .collect(Collectors.toSet());

        assertThat(fieldTypeNames).noneMatch(name -> name.contains("MarketQuoteClient"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("BinanceMarketQuoteClient"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("Controller"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("Scheduler"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("PushRecheckService"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("PushSnapshotService"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("ExternalRuntimeService"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("RuntimeDataClient"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("DataSource"));
        assertThat(fieldTypeNames).noneMatch(name -> name.contains("JdbcTemplate"));
    }

    private static RuntimeSourceReadRequestDTO watchlistRequest(String symbol) {
        return RuntimeSourceReadRequestDTO.forWatchlistPool(
                symbol,
                "P246_TEST",
                "disabled scheduler wiring skeleton test"
        );
    }

    private static RuntimeSourceReadRequestDTO requestWithWatchlistPoolOnly(
            String symbol,
            Boolean watchlistPoolOnly
    ) {
        try {
            Constructor<RuntimeSourceReadRequestDTO> constructor =
                    RuntimeSourceReadRequestDTO.class.getDeclaredConstructor(
                            String.class,
                            Boolean.class,
                            String.class,
                            String.class,
                            List.class,
                            List.class
                    );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    symbol,
                    watchlistPoolOnly,
                    "P246_TEST",
                    "disabled scheduler wiring skeleton test",
                    List.of(),
                    List.of()
            );
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to build request", ex);
        }
    }

    private static void assertSafeDefaults(WatchlistScanResultDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getCandidateAttentionAllowed()).isFalse();
        assertThat(result.getPromoteToHomeAllowed()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
    }

    private static final class CapturingOrchestrator implements LowFrequencyWatchlistScanOrchestrator {

        private final WatchlistScanResultDTO result;
        private RuntimeSourceReadRequestDTO capturedRequest;

        private CapturingOrchestrator(WatchlistScanResultDTO result) {
            this.result = result;
        }

        @Override
        public WatchlistScanResultDTO scanSingleSymbol(RuntimeSourceReadRequestDTO request) {
            this.capturedRequest = request;
            return result;
        }
    }
}
