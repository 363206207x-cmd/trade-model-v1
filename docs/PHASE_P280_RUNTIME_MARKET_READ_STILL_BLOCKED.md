# P280 Runtime Market Read Still Blocked

P280 keeps runtime market reads blocked.

## Still Blocked

The following remain blocked:

- `MarketQuoteClient` wiring.
- `BinanceMarketQuoteClient` wiring.
- Runtime/live/external data reads.
- Market-read adapter production wiring.
- Real scan loop.
- Production scan output assembly from live market data.

P280 does not read market data and does not authorize any future validator to read market data.

## Boundary

A future `RealScanInputContractGuardValidator` may validate only DTO safety / Watchlist Pool proof / fail-closed states / review-only flags. It must not call market clients or request live data.

Display Slots / 默认六币 cannot be scan universe or batch universe. Watchlist Pool remains the scan candidate boundary.
