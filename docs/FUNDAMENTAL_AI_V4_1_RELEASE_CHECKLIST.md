# Fundamental AI v4.1 Release Checklist

The maximum pre-deployment outcome is
`READY_AFTER_MERGED_MAIN_VALIDATION`. Checkboxes are evidence requirements, not
an assertion that production deployment has occurred.

## Code And Contract

- [ ] Independent re-audit approves the exact PR head.
- [ ] PR #1179 required checks pass and the approved head is merged to `main`.
- [ ] Local `main` equals `origin/main`; worktree is clean.
- [ ] Product Source Gate, Workflow Contract, authorization and duplicate
      skeleton guard pass on merged `main`.
- [ ] Full Maven, focused remediation and PostgreSQL V1-to-V13 smoke pass.
- [ ] Mobile changes are absent and automatic trading capability count is zero.

## Artifact And Configuration

- [ ] Java 17 repeatable artifact checksum recorded.
- [ ] No local absolute-path runtime dependency or production fixture path.
- [ ] `LOCAL_TRUSTED` or `PUBLIC_REVERSE_PROXY` mode recorded.
- [ ] Public mode HTTPS, proxy headers, secure cookie and security headers
      verified.
- [ ] Required secrets injected from the secret store; missing values fail
      closed; rotation owner recorded.
- [ ] Scheduler master switch, each enabled scheduler, and approval variables
      reviewed.
- [ ] Log destination, retention and rotation configured.

## Data And Recovery

- [ ] Pre-release PostgreSQL backup identifier, checksum and restore-readiness
      evidence recorded.
- [ ] Flyway V13 confirmed; migration failure keeps readiness down.
- [ ] Previous artifact remains available.
- [ ] Six-step rollback reviewed by the Rollback Decision Owner.

## Smoke And Owners

- [ ] Liveness, readiness and provider health checked independently.
- [ ] Login, CSRF, Session, authenticated Home and logout smoke pass.
- [ ] Fourteen Desktop routes and canonical Home smoke pass.
- [ ] Dynamic Top6 and Position/Plan/AI fail-closed behavior pass.
- [ ] Target runtime decision chain is either PASS with trace IDs or explicitly
      blocked by named missing configuration; controlled evidence is not
      relabeled as live evidence.
- [ ] Release Owner recorded.
- [ ] Rollback Decision Owner recorded.
- [ ] Backup confirmer and smoke confirmer recorded.

Final pre-deployment state: `READY_AFTER_MERGED_MAIN_VALIDATION` only after all
applicable checks are complete. `DEPLOYED` and `PRODUCTION_EFFECTIVE` require a
separate authorized deployment and runtime acceptance.
