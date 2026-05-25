package org.example.trademodel.service.watchlistscan;

import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.service.watchlistsource.WatchlistRuntimeSourceService;

public class DefaultLowFrequencyWatchlistScanOrchestrator implements LowFrequencyWatchlistScanOrchestrator {

    private static final String REASON_DISABLED_BY_DEFAULT =
            "LOW_FREQUENCY_SCAN_ORCHESTRATOR_DISABLED_BY_DEFAULT";
    private static final String REASON_REQUEST_MISSING = "REQUEST_MISSING";
    private static final String REASON_ORCHESTRATOR_BLOCKED = "ORCHESTRATOR_BLOCKED";
    private static final String REASON_WATCHLIST_POOL_ONLY_REQUIRED = "WATCHLIST_POOL_ONLY_REQUIRED";
    private static final String REASON_RUNTIME_SOURCE_SERVICE_MISSING = "RUNTIME_SOURCE_SERVICE_MISSING";
    private static final String REASON_RUNTIME_SOURCE_SERVICE_FAILED = "RUNTIME_SOURCE_SERVICE_FAILED";
    private static final String REASON_SCAN_RESULT_ASSEMBLER_MISSING = "SCAN_RESULT_ASSEMBLER_MISSING";
    private static final String REASON_SCAN_RESULT_MISSING = "SCAN_RESULT_MISSING";
    private static final String REASON_ORCHESTRATOR_ASSEMBLY_FAILED = "ORCHESTRATOR_ASSEMBLY_FAILED";

    private final WatchlistRuntimeSourceService runtimeSourceService;
    private final WatchlistScanResultAssembler scanResultAssembler;
    private final boolean enabled;

    public DefaultLowFrequencyWatchlistScanOrchestrator(
            WatchlistRuntimeSourceService runtimeSourceService,
            WatchlistScanResultAssembler scanResultAssembler,
            boolean enabled
    ) {
        this.runtimeSourceService = runtimeSourceService;
        this.scanResultAssembler = scanResultAssembler;
        this.enabled = enabled;
    }

    @Override
    public WatchlistScanResultDTO scanSingleSymbol(RuntimeSourceReadRequestDTO request) {
        String symbol = request == null ? null : request.getSymbol();

        if (!enabled) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_DISABLED_BY_DEFAULT)
            );
        }

        if (request == null) {
            return WatchlistScanResultDTO.incomplete(
                    null,
                    List.of(REASON_REQUEST_MISSING, REASON_ORCHESTRATOR_BLOCKED)
            );
        }

        if (!Boolean.TRUE.equals(request.getWatchlistPoolOnly())) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_WATCHLIST_POOL_ONLY_REQUIRED)
            );
        }

        if (runtimeSourceService == null) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_RUNTIME_SOURCE_SERVICE_MISSING)
            );
        }

        if (scanResultAssembler == null) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_SCAN_RESULT_ASSEMBLER_MISSING)
            );
        }

        RuntimeSourceReadResultDTO runtimeResult;
        try {
            runtimeResult = runtimeSourceService.readWatchlistRuntimeSource(request);
        } catch (RuntimeException ex) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_RUNTIME_SOURCE_SERVICE_FAILED)
            );
        }

        try {
            WatchlistScanResultDTO scanResult = scanResultAssembler.assemble(runtimeResult);
            if (scanResult == null) {
                return WatchlistScanResultDTO.incomplete(
                        symbol,
                        List.of(REASON_SCAN_RESULT_MISSING)
                );
            }
            return scanResult;
        } catch (RuntimeException ex) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_ORCHESTRATOR_ASSEMBLY_FAILED)
            );
        }
    }
}
