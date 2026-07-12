package org.example.trademodel.dto.ohlcv;

import java.time.Instant;
import java.util.List;

public record PublicKlineFetchResult(
        OhlcvSourceState sourceState,
        String reasonCode,
        Instant fetchTime,
        List<String[]> rows,
        int httpStatus
) {
    public PublicKlineFetchResult(OhlcvSourceState sourceState, String reasonCode,
                                  Instant fetchTime, List<String[]> rows) {
        this(sourceState, reasonCode, fetchTime, rows, 0);
    }

    public PublicKlineFetchResult {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
