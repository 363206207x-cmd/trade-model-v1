package org.example.trademodel.service.watchlistsource;

import java.util.List;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;

public class DefaultWatchlistMarketReadAdapter implements WatchlistMarketReadAdapter {

    private static final String FIELD_REQUEST = "request";
    private static final String FIELD_SYMBOL = "symbol";
    private static final String FIELD_WATCHLIST_POOL_ONLY = "watchlistPoolOnly";

    private static final String REASON_MARKET_READ_REQUEST_MISSING = "MARKET_READ_REQUEST_MISSING";
    private static final String REASON_WATCHLIST_POOL_ONLY_REQUIRED = "WATCHLIST_POOL_ONLY_REQUIRED";
    private static final String REASON_SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String REASON_MARKET_READ_ADAPTER_NO_OP = "MARKET_READ_ADAPTER_NO_OP";
    private static final String REASON_MARKET_CLIENT_NOT_CONNECTED = "MARKET_CLIENT_NOT_CONNECTED";
    private static final String REASON_MARKET_READ_ADAPTER_FAILED = "MARKET_READ_ADAPTER_FAILED";

    @Override
    public RuntimeSourceReadResultDTO readMarket(RuntimeSourceReadRequestDTO request) {
        try {
            if (request == null) {
                return RuntimeSourceReadResultDTO.incomplete(
                        null,
                        List.of(FIELD_REQUEST),
                        List.of(REASON_MARKET_READ_REQUEST_MISSING)
                );
            }

            String symbol = request.getSymbol();
            if (!Boolean.TRUE.equals(request.getWatchlistPoolOnly())) {
                return RuntimeSourceReadResultDTO.incomplete(
                        symbol,
                        List.of(FIELD_WATCHLIST_POOL_ONLY),
                        List.of(REASON_WATCHLIST_POOL_ONLY_REQUIRED)
                );
            }

            if (symbol == null || symbol.isBlank()) {
                return RuntimeSourceReadResultDTO.incomplete(
                        symbol,
                        List.of(FIELD_SYMBOL),
                        List.of(REASON_SYMBOL_MISSING)
                );
            }

            return RuntimeSourceReadResultDTO.sourceUnavailable(
                    symbol,
                    List.of(REASON_MARKET_READ_ADAPTER_NO_OP, REASON_MARKET_CLIENT_NOT_CONNECTED)
            );
        } catch (RuntimeException ex) {
            String symbol = request == null ? null : request.getSymbol();
            return RuntimeSourceReadResultDTO.sourceUnavailable(
                    symbol,
                    List.of(REASON_MARKET_READ_ADAPTER_FAILED)
            );
        }
    }
}
