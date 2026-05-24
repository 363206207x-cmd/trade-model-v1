package org.example.trademodel.service.watchlistsource;

import java.util.List;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;

public class DefaultWatchlistRuntimeSourceWiringAssembler implements WatchlistRuntimeSourceWiringAssembler {

    private static final String REASON_GUARD_MISSING = "GUARD_MISSING";
    private static final String REASON_NULL_GUARD = "NULL_GUARD";
    private static final String REASON_GUARD_RESULT_MISSING = "GUARD_RESULT_MISSING";
    private static final String FIELD_GUARD = "guard";
    private static final String FIELD_GUARD_RESULT = "guardResult";

    private final WatchlistRuntimeSourceGuardValidator guard;

    public DefaultWatchlistRuntimeSourceWiringAssembler() {
        this(new DefaultWatchlistRuntimeSourceGuardValidator());
    }

    public DefaultWatchlistRuntimeSourceWiringAssembler(WatchlistRuntimeSourceGuardValidator guard) {
        this.guard = guard;
    }

    @Override
    public WatchlistRuntimeSourceDTO assembleReviewOnlySource(WatchlistRuntimeSourceDTO source) {
        if (guard == null) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbolOf(source),
                    List.of(FIELD_GUARD),
                    List.of(REASON_GUARD_MISSING, REASON_NULL_GUARD)
            );
        }

        WatchlistRuntimeSourceDTO result = guard.validate(source);
        if (result == null) {
            return WatchlistRuntimeSourceDTO.incomplete(
                    symbolOf(source),
                    List.of(FIELD_GUARD_RESULT),
                    List.of(REASON_GUARD_RESULT_MISSING)
            );
        }
        return result;
    }

    private static String symbolOf(WatchlistRuntimeSourceDTO source) {
        return source == null ? null : source.getSymbol();
    }
}
