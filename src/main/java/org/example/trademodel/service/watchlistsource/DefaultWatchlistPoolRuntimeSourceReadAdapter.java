package org.example.trademodel.service.watchlistsource;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;

public class DefaultWatchlistPoolRuntimeSourceReadAdapter implements WatchlistPoolRuntimeSourceReadAdapter {

    private static final String FIELD_REQUEST = "request";
    private static final String REASON_READ_ADAPTER_NOT_IMPLEMENTED = "READ_ADAPTER_NOT_IMPLEMENTED";
    private static final String REASON_REQUEST_MISSING = "REQUEST_MISSING";
    private static final String REASON_REQUEST_INCOMPLETE = "REQUEST_INCOMPLETE";
    private static final String REASON_NO_RUNTIME_READ_IMPLEMENTED = "NO_RUNTIME_READ_IMPLEMENTED";

    @Override
    public RuntimeSourceReadResultDTO read(RuntimeSourceReadRequestDTO request) {
        if (request == null) {
            return RuntimeSourceReadResultDTO.incomplete(
                    null,
                    List.of(FIELD_REQUEST),
                    List.of(REASON_READ_ADAPTER_NOT_IMPLEMENTED, REASON_REQUEST_MISSING)
            );
        }

        List<String> missingFields = request.getMissingFields();
        if (!missingFields.isEmpty()) {
            return RuntimeSourceReadResultDTO.incomplete(
                    request.getSymbol(),
                    missingFields,
                    withReasons(
                            request.getBlockingReasons(),
                            REASON_READ_ADAPTER_NOT_IMPLEMENTED,
                            REASON_REQUEST_INCOMPLETE
                    )
            );
        }

        return RuntimeSourceReadResultDTO.sourceUnavailable(
                request.getSymbol(),
                withReasons(
                        request.getBlockingReasons(),
                        REASON_READ_ADAPTER_NOT_IMPLEMENTED,
                        REASON_NO_RUNTIME_READ_IMPLEMENTED
                )
        );
    }

    private static List<String> withReasons(
            List<String> baseReasons,
            String firstReason,
            String secondReason
    ) {
        List<String> resolvedReasons = new ArrayList<>();
        if (baseReasons != null) {
            resolvedReasons.addAll(baseReasons);
        }
        addIfAbsent(resolvedReasons, firstReason);
        addIfAbsent(resolvedReasons, secondReason);
        return resolvedReasons;
    }

    private static void addIfAbsent(List<String> values, String value) {
        if (value != null && !values.contains(value)) {
            values.add(value);
        }
    }
}
