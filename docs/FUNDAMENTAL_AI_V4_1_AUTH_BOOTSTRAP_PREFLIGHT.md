# Fundamental AI v4.1 Auth Bootstrap And Preflight

Status: `IMPLEMENTED_PENDING_INDEPENDENT_AUDIT`

## Single Policy Owner

`InitialPasswordPolicy.validate` is the sole application password decision.
Bootstrap, password validation preflight and generated-password verification
all call it. No shell regex duplicates the policy.

Canonical reasons are `PASSWORD_MISSING`, `PASSWORD_TOO_SHORT`,
`PASSWORD_UNSAFE_VALUE`, and `PASSWORD_TEMPLATE_VALUE`.

## Bootstrap States

`USERNAME_MISSING`, `PASSWORD_MISSING`, `PASSWORD_POLICY_REJECTED`,
`BOOTSTRAP_READY`, `USER_ALREADY_EXISTS`, and `BOOTSTRAP_FAILED`.

- An existing database user is never overwritten and does not depend on the
  initial password remaining configured.
- A new user is created only after username and password policy validation.
- Missing/rejected credentials keep readiness DOWN while liveness remains UP.
- Logs contain only state and reason code, never credential values.

## Secure Password Tool

`scripts/generate-runtime-password.sh` uses Java `SecureRandom` through the
application-owned tool. Default length is 24 and the generated value is
validated by the canonical policy.

- With no file option, the value is displayed once with a password-manager
  warning.
- `--env-file <path>` creates a new 0600 file atomically and refuses overwrite.
- Temporary files are removed on both success and failure.

Password-only validation reads `TRADE_MODEL_INITIAL_PASSWORD` and outputs only
`PASSWORD_POLICY=PASS` or a rejected state plus canonical reason code.

Existing login/session/logout behavior remains owned by Spring Security and is
covered by the existing integration suite.
