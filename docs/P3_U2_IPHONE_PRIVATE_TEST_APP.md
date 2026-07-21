# P3-U2 iPhone Private Test App Foundation

## Status

- Phase: `P3-U2`
- Package: iPhone private test app foundation
- Branch: `codex/p3-u2-iphone-private-test-app`
- Base at branch creation: `b7fb33d543927b6f770d6092fd6f5df3751f3d57`
- Delivery state: local implementation validated; Draft PR required and
  unmerged work is not effective merged-main capability
- Production readiness: `BLOCKED`

## Scope Delivered

The package adds an iPhone-only Xcode project under `ios/TradeModelApp`:

1. SwiftUI application shell and safe-area-aware loading/configuration/error UI;
2. `WKWebView` backed by `WKWebsiteDataStore.default()`;
3. environment/Info.plist backend URL configuration with no default host;
4. private-LAN HTTP allowance for development and HTTPS-only production mode;
5. same-origin in-app navigation, system handling for external HTTPS,
   `mailto`, and `tel`, and fail-closed handling for unsafe navigation;
6. native loading, redacted network error, retry, refresh, back navigation, and
   pull-to-refresh states;
7. unit, security-contract, Xcode-project-contract, and minimal UI tests;
8. a shared Xcode Scheme with no committed Apple Team ID, profile, certificate,
   UDID, Apple account, or user-specific Xcode state.

## Existing Authentication Reuse

The app loads the configured origin's `/dashboard`. An unauthenticated backend
Session redirects to `/login`; a valid form login redirects back to the
Dashboard. The WebView owns the browser flow:

- native code does not read or save the password;
- native code does not construct or log `JSESSIONID`;
- native code does not construct a CSRF token;
- native code does not add JWT, API Key, Basic Auth, OAuth, or an auth bypass;
- logout remains the backend's CSRF-protected `POST /logout` flow;
- default persistent WebKit storage can reuse an unexpired Session after app
  relaunch.

No backend authentication, Java, schema, Flyway, Provider, Scheduler,
PositionMonitor, Review, AI, Push, Telegram, or trading code is changed.

## Security Contract

- Missing/malformed base URL: fail closed with a configuration screen.
- Loopback URL: rejected for physical-device-safe configuration.
- Credential-bearing URL: rejected.
- Development HTTP: accepted only for RFC1918 private IPv4 or `.local` hosts.
- Production: HTTPS required.
- ATS global arbitrary loads: disabled.
- Local network: declared narrowly with `NSAllowsLocalNetworking`.
- TLS challenge: default system handling only.
- Session storage: `WKWebsiteDataStore.default()`.
- Cross-origin Cookie forwarding: not implemented.
- External HTTPS: opened by the system.
- External HTTP, `javascript`, `file`, `data`, and unknown schemes: blocked.

## Controlled Validation

- Xcode 26.6 / Swift 6.3.3
- iPhoneOS SDK 26.5 and iPhoneSimulator SDK 26.5
- iOS Runtime 26.5 (`23F77`)
- iPhone 17 Pro Simulator
- Xcode Debug Simulator build: `PASS`
- Swift unit/security/project tests: 30 passed, 0 failed
- UI fail-closed launch test: 1 passed, 0 failed
- Simulator install/launch and missing-configuration visual check: `PASS`
- Loading, network-error, retry, redirect, and trusted-navigation behavior:
  deterministic XCTest evidence; no real backend or external host used
- Real iPhone installation/login/Session persistence: `NOT_RUN`
- Real server deployment: `NOT_RUN`

The first combined test attempt completed 30 tests and exposed one UI locator
failure. The locator was changed from an unstable SwiftUI container type to a
stable visible-title accessibility identifier; the unit and UI suites then
passed independently. No application safety rule was relaxed.

## Remaining Gates

1. Independent iOS project and security review of the Draft PR.
2. User-controlled Xcode installation on a real iPhone.
3. Real-device login, logout, Session expiry/relaunch, 375/390/430-point layout,
   dark-mode, rotation, and local-network acceptance evidence.
4. Authorized HTTPS server and reverse-proxy Session/CSRF evidence.
5. Normal merged-main validation before this package becomes effective.

P4, App Store/TestFlight/Ad Hoc distribution, deployment, live Provider/AI,
Telegram/external Push, orders, position mutation, and trading remain blocked.
