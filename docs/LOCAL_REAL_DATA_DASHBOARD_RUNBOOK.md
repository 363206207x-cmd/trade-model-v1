# 本地真实行情首页

这套本地模式以 Kraken Public OHLC 为主数据源读取真实、已闭合的 K 线，Binance Public Kline 仅作为可选回退，数据写入本机持久化数据库后再运行项目现有的证据、评分与决策链。首页展示的是实际写入的数据，不是 mock 数据。

内部六个资产会通过一次进程内缓存的 Kraken `AssetPairs` 响应解析 REST 标识：以 `wsname` 匹配展示身份，但 OHLC 请求使用 `altname`（缺失时才使用 result object key）。BTC 同时兼容 BTC/XBT，DOGE 同时兼容 DOGE/XDG。若 Kraken 明确不支持 BNB/USD 且 Binance 因 HTTP 451 地域限制不可用，只有 BNB 标记为数据源不可用，其余资产继续启动和分析。

只有 timeout、DNS、5xx、rate limit、provider unavailable，以及 AssetPairs 明确确认的 pair 不存在允许尝试 Binance。Kraken 本地映射或响应结构错误会直接 fail closed，不会通过 Binance 回退掩盖缺陷。

## 启动

```bash
bash scripts/start-local-real-data.sh
```

首次启动会为 6 个资产准备 4 个周期的数据。至少 5 个资产完成真实行情与规则分析时首页可进入就绪状态；不足 5 个时显示部分就绪或降级，不会用替代数据补齐。脚本会在首页数据准备完成后自动打开浏览器。

分析链的真实行情门禁由已持久化 K 线的 provenance 决定，不由 `local-real` Profile 名称决定。每个资产必须具备 5m、15m、1h、4h 四周期的 fresh、closed、quality OK 数据，并保留真实 provider、source endpoint、source trace 和 fetch time，才会进入 Evidence → Score → Decision。已有文件型 H2 窗口满足这些条件时，重启会直接复用并重新分析，不会为了进入分析而重复下载整批历史 K 线。

## 打开

访问：<http://127.0.0.1:8081/>

## 查看状态

```bash
bash scripts/status-local-real-data.sh
```

## 停止

```bash
bash scripts/stop-local-real-data.sh
```

数据保存在本机 `data/` 目录，停止或重启应用不会清空历史数据。

## 安全边界

- AI Provider 默认关闭，不读取 AI 密钥。
- 没有自动开仓、自动平仓、自动反手、下单或自动交易。
- 不发送外部 Push 或 Telegram。
- 仅绑定本机回环地址，不连接生产数据库。
- 行情接口不可用或数据不完整时会显示降级状态，不会生成替代数据。
- Binance 一旦返回 HTTP 451，会在当前进程内熔断，后续请求不会重复访问该受限来源。
