package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicKlineFetchResult;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Public Binance spot K-line adapter. No credential, account, position, or order surface is used. */
@Service
public class BinancePublicOhlcvProvider implements PublicOhlcvProvider {
    static final String SOURCE_ENDPOINT = "/api/v3/klines";
    private static final Set<String> SUPPORTED_TIMEFRAMES = Set.of("5m", "15m", "1h", "4h");

    private final RealMarketDataFetcherService fetcher;
    private final boolean providerEnabled;
    private final boolean externalCallsEnabled;

    public BinancePublicOhlcvProvider(
            RealMarketDataFetcherService fetcher,
            @Value("${trade-model.ohlcv.public-provider.enabled:false}") boolean providerEnabled,
            @Value("${trade-model.ohlcv.public-provider.external-calls-enabled:false}") boolean externalCallsEnabled
    ) {
        this.fetcher = fetcher;
        this.providerEnabled = providerEnabled;
        this.externalCallsEnabled = externalCallsEnabled;
    }

    @Override
    public PublicOhlcvProviderResult fetchClosedBars(
            String symbol,
            String timeframe,
            int limit,
            String ingestionRunId
    ) {
        if (!providerEnabled) {
            return result(OhlcvSourceState.DISABLED, "PUBLIC_OHLCV_PROVIDER_DISABLED", null);
        }
        if (!externalCallsEnabled) {
            return result(OhlcvSourceState.NOT_CONFIGURED, "PUBLIC_OHLCV_EXTERNAL_CALL_NOT_ENABLED", null);
        }
        if (symbol == null || symbol.isBlank() || !SUPPORTED_TIMEFRAMES.contains(timeframe)) {
            return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_REQUEST_INVALID", null);
        }
        if (limit <= 0 || limit > 500) {
            return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_LIMIT_INVALID", null);
        }

        PublicKlineFetchResult fetched = fetcher.fetchKlinesDetailed(symbol, timeframe, limit);
        if (fetched == null) {
            return result(OhlcvSourceState.ERROR, "PUBLIC_OHLCV_PROVIDER_RESULT_MISSING", null);
        }
        if (fetched.sourceState() != OhlcvSourceState.READY) {
            return result(fetched.sourceState(), fetched.reasonCode(), null);
        }

        List<OhlcvBarInput> bars = new ArrayList<>();
        try {
            for (String[] row : fetched.rows()) {
                if (row == null || row.length < 11) {
                    return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_ROW_MALFORMED", null);
                }
                long closeTimeMs = Long.parseLong(row[6]);
                if (closeTimeMs >= fetched.fetchTime().toEpochMilli()) {
                    continue;
                }
                bars.add(new OhlcvBarInput(
                        symbol.trim().toUpperCase(Locale.ROOT),
                        timeframe,
                        Long.parseLong(row[0]),
                        closeTimeMs,
                        decimal(row[1]),
                        decimal(row[2]),
                        decimal(row[3]),
                        decimal(row[4]),
                        decimal(row[5]),
                        decimal(row[7]),
                        Long.parseLong(row[8]),
                        decimal(row[9]),
                        decimal(row[10]),
                        true));
            }
        } catch (RuntimeException e) {
            return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_ROW_PARSE_FAILED", null);
        }
        if (bars.isEmpty()) {
            return result(OhlcvSourceState.WAITING_SYNC, "PUBLIC_OHLCV_NO_CLOSED_BAR", null);
        }

        OhlcvIngestionBatch batch = new OhlcvIngestionBatch(
                "BINANCE_PUBLIC",
                "SPOT",
                SOURCE_ENDPOINT,
                OhlcvSourceState.READY,
                fetched.fetchTime(),
                "binance-public-kline-v1",
                1,
                ingestionRunId,
                ingestionRunId,
                bars);
        return result(OhlcvSourceState.READY, null, batch);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static PublicOhlcvProviderResult result(
            OhlcvSourceState state,
            String reason,
            OhlcvIngestionBatch batch
    ) {
        return new PublicOhlcvProviderResult(state, reason, batch);
    }
}
