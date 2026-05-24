package org.example.trademodel.service.watchlistsource;

import java.util.List;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;

public class DefaultWatchlistRuntimeSourceService implements WatchlistRuntimeSourceService {

    private static final String FIELD_REQUEST = "request";
    private static final String FIELD_GUARD_VALIDATOR = "guardValidator";
    private static final String FIELD_GUARD_RESULT = "guardResult";
    private static final String REASON_REQUEST_MISSING = "REQUEST_MISSING";
    private static final String REASON_RUNTIME_SOURCE_SERVICE_BLOCKED = "RUNTIME_SOURCE_SERVICE_BLOCKED";
    private static final String REASON_READ_ADAPTER_MISSING = "READ_ADAPTER_MISSING";
    private static final String REASON_GUARD_VALIDATOR_MISSING = "GUARD_VALIDATOR_MISSING";
    private static final String REASON_READ_RESULT_MISSING = "READ_RESULT_MISSING";
    private static final String REASON_READ_ADAPTER_READ_FAILED = "READ_ADAPTER_READ_FAILED";
    private static final String REASON_GUARD_RESULT_MISSING = "GUARD_RESULT_MISSING";

    private final RuleConfigWatchlistPoolReadAdapter readAdapter;
    private final WatchlistRuntimeSourceGuardValidator guardValidator;

    public DefaultWatchlistRuntimeSourceService(
            RuleConfigWatchlistPoolReadAdapter readAdapter,
            WatchlistRuntimeSourceGuardValidator guardValidator
    ) {
        this.readAdapter = readAdapter;
        this.guardValidator = guardValidator;
    }

    @Override
    public RuntimeSourceReadResultDTO readWatchlistRuntimeSource(RuntimeSourceReadRequestDTO request) {
        if (request == null) {
            return RuntimeSourceReadResultDTO.incomplete(
                    null,
                    List.of(FIELD_REQUEST),
                    List.of(REASON_REQUEST_MISSING, REASON_RUNTIME_SOURCE_SERVICE_BLOCKED)
            );
        }

        String symbol = request.getSymbol();
        if (readAdapter == null) {
            return RuntimeSourceReadResultDTO.sourceUnavailable(
                    symbol,
                    List.of(REASON_READ_ADAPTER_MISSING)
            );
        }

        if (guardValidator == null) {
            return RuntimeSourceReadResultDTO.incomplete(
                    symbol,
                    List.of(FIELD_GUARD_VALIDATOR),
                    List.of(REASON_GUARD_VALIDATOR_MISSING)
            );
        }

        RuntimeSourceReadResultDTO readResult;
        try {
            readResult = readAdapter.read(request);
        } catch (RuntimeException ex) {
            return RuntimeSourceReadResultDTO.sourceUnavailable(
                    symbol,
                    List.of(REASON_READ_ADAPTER_READ_FAILED)
            );
        }

        if (readResult == null) {
            return RuntimeSourceReadResultDTO.sourceUnavailable(
                    symbol,
                    List.of(REASON_READ_RESULT_MISSING)
            );
        }

        WatchlistRuntimeSourceDTO runtimeSource = readResult.getRuntimeSource();
        if (runtimeSource == null) {
            return readResult;
        }

        WatchlistRuntimeSourceDTO guardResult = guardValidator.validate(runtimeSource);
        if (guardResult == null) {
            return RuntimeSourceReadResultDTO.incomplete(
                    symbol,
                    List.of(FIELD_GUARD_RESULT),
                    List.of(REASON_GUARD_RESULT_MISSING)
            );
        }

        return RuntimeSourceReadResultDTO.fromRuntimeSource(guardResult);
    }
}
