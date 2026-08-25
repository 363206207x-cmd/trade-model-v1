# TRINE LOGIC Multi-User Account Registration Source Mapping

| Frozen requirement | Existing owner | Authorized closure |
|---|---|---|
| User identity and credentials | Existing `tm_user`, user entity/mapper/service, BCrypt and form-login chain | Extend the same owner with role, active-state and registration lifecycle |
| Session and CSRF | Existing Spring Security Session and CSRF configuration | Preserve and add force-logout/disabled-user enforcement |
| Private data | Existing watch pool, config, UserPosition, Analysis, Final plan, Message and audit owners | Add authenticated-user ownership columns/queries only where absent |
| Shared market facts | Existing provider and persisted OHLCV owners | Keep global and never copy per user |
| Owner administration | Existing authenticated `xuchao` identity | Add OWNER-only user lifecycle controls without a second Owner |
| Home and task routes | Existing approved Desktop templates/controllers | Bind registration/account administration only; Home structure is frozen |
| Existing Owner data | `tm_user.id=1` and its current linked records | Backfill to user 1 with backup, rollback and ambiguity guards |

The current v4.1 Product Source and delivery/safety contracts remain
authoritative. This package records an implementation permission boundary and
does not redefine trading, AI, position, plan, message or provider semantics.
