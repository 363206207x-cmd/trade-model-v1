package org.example.trademodel.stress.replay;

import java.time.Instant;
import java.util.List;
import java.util.Set;

record HistoricalFixtureValidation(int rowCount,
                                   Instant startTime,
                                   Instant endTime,
                                   Set<String> symbols,
                                   Set<String> timeframes,
                                   List<String> knownGaps) {
}
