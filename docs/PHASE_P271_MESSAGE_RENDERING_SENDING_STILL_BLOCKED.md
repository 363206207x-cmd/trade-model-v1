# P271 Message Rendering Sending Still Blocked

## 1. Message Rendering Status

Message rendering remains blocked after P271.

P271 does not build final send text, templates, provider payloads, notification bodies, or channel-specific message formats.

## 2. Message Sending Status

Message sending remains blocked after P271.

P271 does not send external messages, local notifications, provider calls, webhooks, emails, Telegram messages, app notifications, or any other push output.

## 3. Blocked Message Behaviors

The following remain blocked:

- final message rendering
- provider payload rendering
- channel-specific formatting
- sendable message envelope
- message dispatch
- retry / backoff for sending
- delivery receipt handling
- audit queue consumption for send
- scheduler / API / dashboard-triggered send

## 4. Future Requirement

A future provider channel skeleton may only be disabled-by-default, fail-closed, audit-only, review-only, no-message-sending, no-credential, and no-live-provider-call.

It may represent internal review metadata only.

It must not create a sendable or renderable message.

## 5. Current Result

P271 only documents that message rendering and message sending remain blocked.
