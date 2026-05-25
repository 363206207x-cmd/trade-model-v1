package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;

public interface WatchlistMarketReadAdapter {

    RuntimeSourceReadResultDTO readMarket(RuntimeSourceReadRequestDTO request);
}
