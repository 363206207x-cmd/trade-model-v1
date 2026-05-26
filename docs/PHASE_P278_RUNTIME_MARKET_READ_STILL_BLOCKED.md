# P278 Runtime Market Read Still Blocked

P278 does not authorize runtime market reads.

Still blocked:

- `MarketQuoteClient` wiring
- `BinanceMarketQuoteClient` wiring
- runtime/live/external data read
- real scan loop
- production market-read adapter wiring
- scheduler/API/dashboard wiring for market reads

The future real scan input contract DTO may describe market-read requirement flags and data availability expectations, but it must not read data or call any market provider.

No runtime/live/external data may be read in P278. P278 is docs-only.
