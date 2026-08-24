# RINE LOGIC Owner Login Runbook

## Login

- URL: `https://rine-staging.tailf2f07d.ts.net/login`
- Username: `xuchao`
- Store the credential only in the Owner's password manager after a successful private Staging login.
- Public registration and public password recovery are not available.

## Root-only password recovery

1. Connect to `rine-staging` through verified Tailscale SSH.
2. Run `sudo /usr/local/sbin/rine-logic-reset-owner-password` from an interactive terminal.
3. Enter the new password twice at the hidden prompts.
4. Confirm only `OWNER_PASSWORD_RESET=PASS` is printed.
5. Restart `rine-logic.service` and verify login with `xuchao`.

The command rejects missing or duplicate accounts, never creates an account,
does not accept a password argument, and updates only the existing configured
Owner. It also keeps the root-owned bootstrap secret aligned because the
production preflight still requires that existing setting.

## Recovery and backup notes

Create and verify a PostgreSQL custom-format backup before an identity or
credential operation. Preserve the stable `tm_user.id`, verify exactly one
account after recovery, and test the offsite copy through an isolated restore.
Never place a database dump, password, password hash, cookie, or CSRF token in
the repository or an audit report.
