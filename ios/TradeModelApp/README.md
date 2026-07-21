# Trade Model iPhone Private Test App

This is the P3-U2 iPhone-only SwiftUI + `WKWebView` foundation. It reuses the
existing Spring Security form-login, server-side Session Cookie, and CSRF
flow. It does not implement a second authentication protocol and does not
store credentials, Session IDs, or CSRF tokens in native code.

## Open And Build

1. Open `TradeModelApp.xcodeproj` in Xcode.
2. Select the `TradeModelApp` target and open **Signing & Capabilities**.
3. Leave **Automatically manage signing** enabled and select your own Apple
   Developer team locally. No Team ID or provisioning profile is committed.
4. Select an iPhone Simulator for local validation, or connect your own iPhone
   for a later private-device acceptance run.
5. The deployment target is iOS 17.0 and the target device family is iPhone.

## Configure A Development Backend

The app has no built-in server address. In Xcode, edit the active Scheme,
choose **Run > Arguments > Environment Variables**, and add:

```text
TRADE_MODEL_BASE_URL=http://192.168.x.x:8081
```

Replace the placeholder with the Mac's actual private LAN address. Do not use
`localhost`: on a physical iPhone it means the phone itself. One way to inspect
the current Wi-Fi address on the Mac is:

```bash
ipconfig getifaddr en0
```

Start Spring Boot with the repository-approved local configuration and confirm
that it listens on port `8081` and a LAN-reachable interface. Keep credentials
in the approved local secret mechanism; never add them to the Scheme, this
project, Git, or chat.

Development accepts explicitly configured private-LAN HTTP addresses or HTTPS.
Production mode accepts HTTPS only. The app rejects missing, malformed,
credential-bearing, loopback, and non-private HTTP URLs.

## Install On Your Own iPhone

1. Connect the iPhone to the Mac and approve the trust prompt.
2. Select that iPhone as the Xcode run destination.
3. Choose your own team under **Signing & Capabilities**.
4. Enable Developer Mode on the iPhone if iOS requests it, then restart and
   confirm the device prompt.
5. Ensure the iPhone and development Mac are on the same trusted LAN.
6. Set `TRADE_MODEL_BASE_URL` to the Mac's current private LAN URL.
7. Run from Xcode and accept the local-network permission prompt.

No real-device installation is claimed by this repository package. The user
must perform and record that acceptance separately.

## Device Acceptance Checklist

- The app opens without clipping around the Dynamic Island or Home Indicator.
- An unauthenticated Session reaches `/login` through the backend redirect.
- Valid form login reaches `/dashboard`.
- App background/foreground does not force a refresh.
- Reopening the app reuses an unexpired `JSESSIONID` from
  `WKWebsiteDataStore.default()`.
- Session expiry returns to `/login` normally.
- CSRF-protected `POST /logout` returns to login and invalidates the Session.
- Backend unavailability shows the native retry state without exposing a URL,
  credential, Cookie, response body, or internal error.
- Same-origin links stay in the app; external HTTPS, `mailto`, and `tel` use the
  system; untrusted HTTP and unsafe schemes are blocked.

## Future HTTPS Server

For a future deployed server, configure an HTTPS origin and build/run with the
production environment. Default system TLS validation remains enabled; this
project contains no trust-all handler and does not bypass certificate errors.
After a server is available, the app's daily use does not require the Mac, but
installing updates and renewing local-development signing still requires
Xcode and the user's Apple account.

## Explicitly Out Of Scope

- App Store, TestFlight, Ad Hoc, and enterprise distribution
- Android and iPad productization
- JWT, API key, Basic Auth, OAuth, native credential storage, or auth bypasses
- Provider, AI, Telegram, external Push, order execution, or trading behavior
- Production deployment or production-readiness approval
