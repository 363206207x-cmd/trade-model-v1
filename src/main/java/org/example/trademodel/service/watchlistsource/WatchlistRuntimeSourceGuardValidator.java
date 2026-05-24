package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;

public interface WatchlistRuntimeSourceGuardValidator {

    WatchlistRuntimeSourceDTO validate(WatchlistRuntimeSourceDTO source);
}
