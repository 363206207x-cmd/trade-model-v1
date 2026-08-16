# Fundamental AI v4.1 Telegram Security

Status: `IMPLEMENTATION_CANDIDATE_PENDING_INDEPENDENT_AUDIT`

## Secret Boundary

- Bot token and chat ID are runtime environment secrets only.
- Neither value is stored in the database, frontend, status API, docs, tests,
  screenshots, metrics tags, traces, or Git.
- The repository ignores `.config/`, `telegram.env`, and `*.telegram.env`.
- Tests use non-secret placeholders that do not resemble a real Bot API token.
- The operator private environment file was not read or used by implementation
  validation.

## HTTP And Error Sanitization

The Bot API token appears in the request URL path. The sole client owner does
not log the full URI. Provider descriptions and unexpected errors pass through
`TelegramSecretSanitizer` before persistence or observation. Only provider,
method, HTTP/error classification, provider message reference, recipient
fingerprint, attempt count, and timing are safe operational facts.

The Bot API call uses POST and plain-text content. Dynamic text is not inserted
as unescaped Telegram Markdown. Tests cover token/chat-ID removal, full-URL
removal, invalid JSON, and provider error bodies.

## Recipient And API Boundary

- The durable queue stores a one-way recipient fingerprint, never the chat ID.
- `GET /api/settings/notifications/telegram/status` is authenticated and
  owner-scoped.
- The message-scoped retry endpoint resolves ownership before changing an
  existing `FAILED` or `NOT_CONFIGURED` delivery to `QUEUED`.
- The status response excludes bot token, chat ID, recipient, request URL,
  cookie, session, and secret-file path.
- Existing UserConfig endpoints no longer expose or accept Telegram chat ID.
- No connectivity-test HTTP endpoint is added in this package; application
  live acceptance is a controlled merged-main operation.

## Link Safety

Links require public HTTPS, a public host, and exact same-host paths. Loopback,
private, HTTP, cross-host, arbitrary-path, and secret-bearing links are
rejected. Text-only delivery is the safe fallback when no valid public base URL
exists.

## Architecture Boundary

Only the Telegram dispatcher depends on `TelegramClient`. Business service
implementations emit canonical Messages through `HighValueAlertMessageService`
and cannot directly own Telegram HTTP behavior. Architecture tests enforce the
single-client owner and direct-call prohibition.

## Static Gates

- `TELEGRAM_TOKEN_LITERAL_IN_REPOSITORY_COUNT=0`
- `TELEGRAM_CHAT_ID_LITERAL_IN_REPOSITORY_COUNT=0`
- `TELEGRAM_SECRET_LOGGING_PATTERN_COUNT=0`
- `TELEGRAM_FULL_REQUEST_URL_LOG_PATTERN_COUNT=0`
- `AUTOMATIC_TRADING_CAPABILITY_COUNT=0`
