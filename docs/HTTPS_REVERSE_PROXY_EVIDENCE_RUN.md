# HTTPS Reverse Proxy Evidence Run

Branch: `codex/pdr-live13-https-reverse-proxy-evidence`
Base main commit: `60a40764`
Package: PDR-LIVE13 HTTPS Reverse Proxy Evidence
Status: CONTROLLED EVIDENCE RECORDED
Production deployment readiness: BLOCKED

## Scope

This package records controlled HTTPS / reverse proxy production-readiness evidence. It is not production deployment, does not access a production server, does not access a production database, does not print or commit secrets, and does not add trading runtime behavior.

## Sources Inspected

- `src/main/resources/application-prod.yml`
- `docker-compose.yml`
- `scripts/prod-smoke.sh`
- `docs/SECRETS_AND_ACCESS_HARDENING.md`
- `docs/SECRETS_HTTPS_ACCESS_EVIDENCE_RUN.md`
- `docs/ACCESS_AUDIT_RATE_LIMIT_EVIDENCE_RUN.md`
- `docs/PRODUCTION_READINESS_RUNBOOK.md`
- `docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md`
- `docs/RELEASE_EVIDENCE_BUNDLE_CURRENT_STATUS.md`

No `.env` file was read. No production host, production database, TLS certificate, private key, API key, cookie, authorization header, or datasource secret was inspected or printed.

## Evidence Status Summary

| Area | Status | Evidence / Finding |
|---|---|---|
| HTTPS / reverse proxy | DOCUMENTED_WITH_CONFIG | This package records a safe template-only reverse proxy configuration pattern and explicit smoke/evidence checklist. No real server proxy was accessed, so this is not PASS. |
| TLS termination | DOCUMENTED_WITH_CONFIG | Template requires a real certificate path managed outside the repository. No certificate material is committed or inspected. |
| HTTP-to-HTTPS redirect | DOCUMENTED_WITH_CONFIG | Template includes a port 80 redirect to HTTPS. No real redirect smoke was run. |
| HSTS | DOCUMENTED_WITH_CONFIG | Template includes an HSTS header that must be enabled only after HTTPS is stable for the target host. No browser/server HSTS evidence exists. |
| Proxy / forwarded headers | DOCUMENTED_WITH_CONFIG | Template forwards `Host`, `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Forwarded-Host`. `application-prod.yml` does not currently set `server.forward-headers-strategy`; this remains an implementation/evidence item before production approval. |
| Actuator exposure behind proxy | GUARD_PASS / TEMPLATE_REQUIRED | Existing application tests prove only minimal health/liveness/readiness are exposed by the app. The proxy template restricts intended public actuator paths to health endpoints, but no real proxy route smoke was run. |
| Auth smoke through proxy | MISSING_EVIDENCE | `scripts/prod-smoke.sh` can be pointed at an HTTPS `APP_URL`, but no controlled HTTPS reverse-proxy endpoint was run in this package. |
| Access logging behind proxy | APP_GUARD_PASS / PROXY_EVIDENCE_MISSING | PDR-LIVE12 proves sanitized application `ACCESS_LOG`. Reverse proxy access-log retention/aggregation and real forwarded-client-IP evidence remain missing. |
| Auth audit logging behind proxy | APP_GUARD_PASS / PROXY_EVIDENCE_MISSING | PDR-LIVE12 proves auth failure audit logging at the app boundary. Real proxy-path auth failure smoke remains missing. |
| Rate limiting behind proxy | APP_GUARD_PASS / PROXY_EVIDENCE_MISSING | PDR-LIVE12 proves application-level 429 guard. Proxy-level abuse protection remains documented but not evidenced. |

## Template-Only Reverse Proxy Configuration

The following Nginx-style configuration is a reference template only. It is not a production secret, does not include certificate material, and must be adapted and reviewed by the release owner before use.

```nginx
# TEMPLATE ONLY - NOT A PRODUCTION SECRET
# Replace YOUR_HOST and certificate paths in a controlled server environment.
# Keep the application bound to 127.0.0.1:8081 or another private interface.

limit_req_zone $binary_remote_addr zone=trade_model_api:10m rate=120r/m;

server {
    listen 80;
    server_name YOUR_HOST;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name YOUR_HOST;

    ssl_certificate /etc/letsencrypt/live/YOUR_HOST/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/YOUR_HOST/privkey.pem;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;

    access_log /var/log/nginx/trade-model-access.log;
    error_log /var/log/nginx/trade-model-error.log warn;

    location = /actuator/health {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
    }

    location ~ ^/actuator/health/(liveness|readiness)$ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
    }

    location / {
        limit_req zone=trade_model_api burst=60 nodelay;
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header Connection "";
    }
}
```

## TLS Termination Checklist

Status: DOCUMENTED_WITH_CONFIG.

1. A real TLS certificate must be provisioned outside the repository.
2. Certificate private keys must never be committed, pasted into docs, or printed in smoke output.
3. Certificate renewal evidence must be recorded before production approval.
4. This package did not inspect certificates and did not access a server.

## HTTP To HTTPS Redirect Checklist

Status: DOCUMENTED_WITH_CONFIG.

1. The template redirects port 80 to HTTPS with HTTP 301.
2. Release evidence still needs redacted `curl -I http://HOST/...` output proving the redirect on the intended entrypoint.
3. No redirect smoke was run in this package.

## HSTS Checklist

Status: DOCUMENTED_WITH_CONFIG.

1. The template includes `Strict-Transport-Security`.
2. HSTS must be enabled only after HTTPS and redirect behavior are stable for the intended host.
3. No real HSTS header evidence exists yet.

## Proxy Header / Forwarded Header Checklist

Status: DOCUMENTED_WITH_CONFIG with app follow-up required.

1. The template forwards `Host`, `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Forwarded-Host`.
2. `application-prod.yml` currently does not explicitly set `server.forward-headers-strategy`.
3. A later implementation or release-evidence package should decide whether to set `server.forward-headers-strategy=framework` or rely entirely on proxy logs.
4. Real forwarded-client-IP and HTTPS scheme behavior is not evidenced yet.

## Actuator Exposure Policy Behind Proxy

Status: GUARD_PASS / TEMPLATE_REQUIRED.

1. Existing app-level tests prove public actuator exposure is limited to minimal health/liveness/readiness.
2. The template exposes only `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` as intended health routes.
3. Sensitive actuator routes such as env, beans, configprops, mappings, and loggers must remain unavailable through the proxy.
4. Real proxy route smoke remains missing.

## Auth Smoke Through Proxy Status

Status: MISSING_EVIDENCE.

Required future evidence:

1. `APP_URL=https://YOUR_HOST bash scripts/prod-smoke.sh` with credentials sourced from server env and redacted output.
2. HTTP 200 for `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` over HTTPS.
3. Authenticated HTTP 200 for `/api/dashboard/home` and `/api/review/center` over HTTPS.
4. HTTP 401 for protected routes without credentials.
5. Confirmation that no password, cookie, token, datasource URL, API key, or authorization header appears in logs or evidence.

No proxy smoke was run in this package.

## Access Logging / Auth Audit / Rate Limit Behind Proxy Status

Status: APP_GUARD_PASS / PROXY_EVIDENCE_MISSING.

PDR-LIVE12 already proves app-level access logging, auth audit logging, sensitive-data redaction, and rate limiting. LIVE13 records how those controls should sit behind HTTPS termination, but proxy-level log retention, forwarded-IP accuracy, and proxy-level rate-limit evidence remain missing.

## Safety Confirmation

- No production server was accessed.
- No production DB was accessed.
- No real secrets were printed.
- No real secrets were committed.
- No `.env` was committed.
- No certificate or private key material was committed.
- No destructive operation was run.
- No orders were placed.
- No order execution path was introduced.
- No auto-open, auto-close, or auto-reverse behavior was introduced.
- No auto-trading behavior was introduced.
- No external Push was sent.
- No fake positions were created.
- No fake review records were created.

## Production Readiness Decision

Production readiness remains BLOCKED.

HTTPS / reverse proxy evidence moves from `DOCUMENTED_NOT_EVIDENCED` to `DOCUMENTED_WITH_CONFIG` because this package records a concrete template-only reverse proxy configuration and evidence checklist. It is still not `GUARD_PASS` or `PASS` because no controlled HTTPS reverse-proxy endpoint was run, no certificate/redirect/HSTS smoke was collected, and no real proxy auth smoke exists.

## Remaining Blockers

1. Controlled HTTPS reverse-proxy smoke through the intended entrypoint is missing.
2. TLS certificate provisioning and renewal evidence is missing.
3. HTTP-to-HTTPS redirect evidence is missing.
4. HSTS header evidence is missing.
5. Forwarded header / client-IP behavior is not evidenced, and app-level forwarded-header strategy remains undecided.
6. Reverse-proxy access-log retention/aggregation evidence is missing.
7. Secrets manager integration and credential rotation evidence remain missing.
8. AI provider PASS evidence or release-owner waiver remains missing.
9. Real server smoke and release-owner approval remain missing.

## Next Recommendation

Recommended next package: `PDR-LIVE14 Secrets Manager / Credential Rotation Evidence`, or a controlled real-server HTTPS reverse-proxy smoke package if an approved non-production server endpoint is available. Production deployment must remain blocked until a later release-gate package records complete PASS evidence and explicit approval.
