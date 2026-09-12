# Asset-card live signal implementation evidence

## V42 PR #1302 final card-only continuation — 2026-09-12

This section supersedes earlier *current status* statements only; historical
implementation, runtime, budget and incident evidence below is retained.
No deployment or visible-page acceptance is claimed by this source update.

Gate PR #1303 Head `2024f1bc190bc88b55871f2e73047f82b34cf3a8` changed exactly
the five Owner-authorized gate/source files. Its manual and Ready-triggered
reviews completed without blocking findings, its exact-head quality checks
passed and workflow-contract passed twice. Actual squash merge:
`c21ecf1e273fa262584312ec7a1ace0d95889801`. The three matching assertions now
use here-strings, retaining their predicates, failure reasons and bounded
diagnostics. A 1 MiB early-match regression reproduces old `[141, 0]` pipeline
statuses and validates the corrected input path; 101 public outer-gate cases
retain the exact identities, 64 paths and historical-package rejection rules.
This does not reconstruct the request missing from the original CI log.

The existing business branch merged that main as
`17a01a588033396e8db02737cae08d809d8d7209`. Audited business Head
`ef8f0b098e56e15c2bc3c42db040c0360cfc9bed` remains an ancestor; all eleven
business files were byte-identical immediately after synchronization. No
force push, reset or replacement of prior implementation occurred.

Initial live-gate queries failed closed because GitHub evidence could not be
read: the captured exact source-PR request returned EOF. No permission logic,
authentication setting or query result was changed to recover admission.
After the real request succeeded, the unchanged business-worktree outer gate
reported `REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`,
`IMPLEMENTATION_ALLOWED=true`, `RESOLUTION_BLOCK_REASON=NONE`, count 64.
Product Source, task validation and complete workflow-contract then passed.

Current work is limited to card display correctness and the existing runtime
recovery/stop boundary. Final business test counts, candidate identity, remote
CI/review and deployment observations must be recorded after actual execution;
the historical results below are not substitute evidence for the new Head.
The initial trigger of the old 41-minute market stall remains UNKNOWN.
The old window and original budget ledger must not be reset or resumed.
SHADOW remains the model mode; production training/calibration samples,
production metrics and model readiness are not established by test fixtures.

### Preserved-worktree validation and non-destructive candidate build

The nine preserved WIP files remain within the same eleven-file PR scope and
unchanged 64-path allowlist. Card-only regressions cover both reachable renderers:
no price-source/time row or pinned-observation copy; no invalid ninth direction
with a percentage; independent signed risks, partial UNKNOWN and aggregate-only
rejection; one Beijing analysis clock unaffected by price updates. The Owner
preview never borrows canonical direction, confidence or risk. Legacy Mark/closed
bar values lack Spot trade/expiry proof and therefore cannot fill the main price.

Controlled-window persistence now shares bounded admission with the original
terminal lease. A stopped/expired window admits no new bar, trade, inference,
label, snapshot or retention write. Pending labels/cursors and original evidence
remain intact; only already-admitted finite IO may drain. This supersedes older
release text allowing unrestricted offline maturity after STOP. The real
single-connection PostgreSQL regression proves that a writer transaction does
not wait for the lease monitor while its storage check needs that connection.
Recovered Spot prices withdraw only the older disproved Spot-source DATA fault,
not other independent risks, prediction invalidation or the analysis clock.

No `clean`, target deletion or history rewrite was used. An initial native-profile
test run produced the common-checkout identity and correctly failed the provenance
guard (489 tests, one failure, one local ACL skip). The retained wrong metadata and
reports were preserved outside Git. The existing plugin was then run only in
`initialize`, with this worktree's real Git location; default `test` ran without
Git-location overrides or the identity profile. Generated identity was the actual
`17a01a588033396e8db02737cae08d809d8d7209`, `git.dirty=true`, not a release artifact.
The repeated focused run passed: 489 tests, zero failures/errors, one local
LinuxKit tmpfs POSIX-ACL skip requiring actual final-head Linux CI execution.
Python numerical tests: 49 passed. Frontend event/expiry/visibility/three-timezone
matrix, JS syntax and diff checks passed. Assertions were not weakened to hide
the identity failure. Candidate and merged-main builds must each use a new clean
isolated checkout and their own generated identity and SHA-256; full Maven,
packaged-JAR/CI/review and actual Staging evidence remain separate requirements.

### Exact candidate review and bounded recovery correction

Candidate `f993ff613aedd859fdd077781a4b72ab32a0c3d3` was built in a new,
initially target-free detached worktree, without clean. Its generated Git identity
matched that clean commit. Full Maven: 5,590 tests, zero failures/errors, 14 skips;
Python: 49 passed; frontend event/expiry/timezone matrix passed. The standard JAR
SHA-256 was `eb711dde7bd89ebbd432c696e85bbda2d26686ae1ce48a7d2b9ff45f144dc260`.
Fifteen loopback WebSocket scenarios loaded production classes extracted from that
JAR. Fourteen additional native-probe/real isolated credential tests passed. The
same JAR passed Linux x86_64 Java 17/XGBoost 2.1.4 LONG/SHORT and independent Beta
interop, maximum error zero; these were temporary fixture models, not production.

Both exact-head quality-gate runs (34688747384 and 34688745793) passed 1,594 tests
with two explicit opt-in skips; workflow-contract run 34688747462 passed. The real
Linux writer/ACL class ran all 14 tests without skips in both quality jobs. Local
full-suite skips were one LinuxKit tmpfs ACL capability case, twelve individually
controlled legacy P3/P3H/external PostgreSQL cases, and one external CoinGlass
smoke test. CI did not run the dedicated-Python and retained-rollback-JAR opt-ins;
those ran locally. No real external database or CoinGlass probe was substituted.

The exact f993ff61 review nevertheless found two P1 issues, so that candidate
remained Draft and was not merged/deployed. `V42-RECOVERED-RISK-VERSION` exposed a
same-version read-only risk withdrawal rejected by browser group watermarks.
`V42-INFERENCE-ADMISSION-LOSS` exposed a closed-bar audit silently lost after a
transient writer-admission timeout. New regressions first reproduced both failures.
The correction must retain real CAS versioning on background recovery and zero
GET writes; a verified newer trade may only withdraw the exact older Spot-source
DATA claim to UNKNOWN, preserving unrelated evidence and the analysis clock.
Deferred inference writes preserve immutable point-in-time evidence and the same
symbol/closed5mAt identity, have at most three total admissions, and never publish
a retried calculation as an on-time prediction. Exhaustion explicitly stops only
the card collection under the original ledger; no window/budget is restarted.
Final post-correction checks and review must be recorded against their own Head.
The post-review WIP focused suite passed 467 tests, zero failures/errors, one
local tmpfs ACL skip; Python 49 and the full frontend matrix passed. Product
Source, task validation, 101 outer-machine cases and workflow-contract passed.
The new frozen-evidence regression compares all nested values without numeric
tolerance and preserves exact Instant nanoseconds; it does not require equivalent
JSON spellings such as `100.0` and `100` to retain different node classes.
An exhausted write is explicitly UNPERSISTED: its pending payload is in memory,
not claimed as durable or automatically replayable after restart. The original
persisted bars and observations are not deleted. No server or new-window action
was performed for these local regressions.

## V42 stream recovery and stale-price correction — 2026-09-12

This is the same V42 card-only task, not a new model or algorithm. The business
branch was synchronized without loss with merged main
`34b943eece4f19c927b5d96aa50fe17e9025f182`; the preserved-history continuation
baseline is `9e0699fad12d9ab87731afb6522a90de74648746`. Before editing, the real
outer resolver reported AUTHORIZED_IMPLEMENTATION_PACKAGE,
IMPLEMENTATION_ALLOWED=true and RESOLUTION_BLOCK_REASON=NONE with no open PR.
The 64 implementation paths and their fingerprint are unchanged. A newly
created business PR requires its own exact continuation registration; the
already-merged #1300 is not permission for arbitrary future PRs.

Retained runtime evidence proves that recorded market observations stopped inside the approved
window and no second connection attempt occurred, while budget checks still
reported OPEN/RUNNING. It does not prove the first underlying disconnect cause:
the incident lacks a close/error timeline and contemporaneous Java queue trace.
The old collection window, persisted observations and original ledger are not
reset or resumed by this correction. A new release/window remains a separate
approval; no server, external Provider or Owner-data operation is performed.

Tests first reproduced the local-abort retained-socket path, retired handshake
single-flight failure, newer SOURCE_UNAVAILABLE/null reconciliation retaining
an old price, missing independently enforceable expiry, and fresh/late trade
recovery errors. Further substantive review identified stale signal/risk basis
promotion and retired-frame mutation races; those are tested as correctness
defects, not deferred as formatting concerns.

The correction retains the configured ten-second price TTL, emits
`priceValidUntil = actual trade time + configured TTL`, and revokes price at
that boundary. The browser uses a single local expiry timer even when SSE and
reconciliation fail; receipt time cannot extend validity. Only a genuinely new,
unexpired trade can restore price. Analysis display uses signalAsOf; PRICE and
connection events do not update that clock or manufacture risk/percentages.
Risk, signal, persistence and price remain separate fields. Java17 automatically
answers received Ping frames; no extra heartbeat or Provider frequency is added.

Validation is local/isolated only; no actual release/window is executed:

- Focused Service/MarketData/HomeUI: 112 tests, zero failures/errors/skips.
  The market class has 55 tests, including 10 real localhost WebSocket scenarios
  and four deterministic race scenarios. Initial failing tests were retained
  as regressions, not removed or weakened.
- Full Maven: 5,573 tests, zero failures, zero errors, 14 skips across 520 classes.
  Disposable PostgreSQL actually ran. Fixed Python/XGBoost 2.1.4 LONG/SHORT
  interoperability ran, with raw prediction delta zero and maximum beta delta
  2.20228566286e-20 (synthetic fixture only, not production model evidence).
  The exact retained pre-V24 JAR also started after V24 in an isolated database,
  retaining all 23 old migrations byte-for-byte and all new card tables/data.
- Python numerical suite: 49 tests passed. Frontend event matrix, UTC/Shanghai/
  New York subprocesses, JS syntax and diff checks passed. Two sliced-JS test
  fixtures now include the actual expiry dependency; zero existing assertions
  were removed. They verify exact expiry and expire-before-GET after visibility.
- Product Source, task validation, exact machine tests (including 64 public
  outer V42 cases), and workflow-contract passed. Before editing, clean-tree
  admission passed; during preserved WIP the unchanged outer clean-tree guard
  correctly reports BLOCKED_WORKTREE_DIRTY. This is not misreported as current
  clean admission or bypassed by editing the gate.
- Standard-JAR behavior test: the test-only runner verifies BOOT-INF/classes
  code source and bytes against the real JAR for MarketData/Service/Snapshot,
  then passes all 14 loopback/race scenarios under Java17. The precommit WIP JAR
  is explicitly dirty and not deployable; the clean final commit is packaged
  separately and receives its own SHA-256 and repeated runner receipt.
- Substantive local review closed V42-STALE-SAFETY-BASIS and
  V42-HANDSHAKE-OPEN-ORDER after reproduction/regression. Final remote Head CI
  and review are separate evidence, never inferred from this local review.

The exact 14 skipped tests are:

1. AssetCardDataSourceConfigurationTest.realLinuxSystemdAclAndReadOnlyMountPermitOnlyTheExactServiceIdentity:
   this local LinuxKit kernel lacks tmpfs POSIX ACL; required Linux CI must run it.
2. ControlledCurrentStateCloneFlywayActionTest.validatesOrMigratesOnlyAnApprovedLocalP3Database:
   no P3 controlled database action opt-in.
3–9. ControlledCurrentStateContentFingerprintTest: rollbackRestoresFingerprint,
   fingerprintOutputDoesNotContainRawModifiedValues, sameDataProducesMatchingFingerprint,
   sameRowCountTimeMutationIsDetected, sameRowCountPlanBoundaryMutationIsDetected,
   sessionTimezoneDoesNotChangeFingerprint, sameRowCountStatusMutationIsDetected:
   no approved P3 content database environment. These are not Owner-data checks.
10. ControlledGeneratedReleaseLikeFixtureFlywayTest.createsOnlyTheApprovedLocalGeneratedFixtureAtFlywayV6:
    no P3 generated-fixture opt-in.
11. ControlledGreenfieldFlywayV7ActionTest.migratesExactEmptyGreenfieldDatabaseFromV1ToV7AndRepeatsIdempotently:
    no P3-G database action opt-in.
12. ControlledP3hComposeOfflineSmokeTest.disposableComposeProvesBootstrapSecretsProxyAndReadOnlyRole:
    no legacy P3H Compose opt-in.
13. ControlledPostgreSqlFlywaySmokeTest.controlledExternalPostgreSqlFlywayMigrationsApplyWhenExplicitlyConfirmed:
    no external PostgreSQL environment; no persistent database connection attempted.
14. CoinGlassControlledSmokeTest.controlledSmokeUsesCoordinatorAndReturnsSanitizedSummary:
    external-call opt-in absent; no CoinGlass call attempted.

Earlier failed runs are not hidden: the first full attempt had four failures
(two missing JS fixture dependencies, old generated Git provenance, wrong Python
executable); the second had only the Git provenance failure. The final run used
the existing process-only Docker API 1.44, fixed Python, and actual worktree
metadata generated by the existing profile in a separate Git-bound process.
No dependency, Git identity assertion, test skip condition or system config was
changed to obtain the passing run. Final PR/clean artifact receipts are delivered
with the exact-head review; earlier sections below remain historical evidence.
MODEL_MODE=SHADOW; PRODUCTION_MODEL_READY=NO; CURRENT_PHASE_DONE=NO.

## PREPARED storage review correction — 2026-09-12

The exact a0f61b92 review found V42-RUNTIME-001: mandatory ReadWritePaths were
rendered in PREPARED while directory validation returned early unless ARMED.
Two new isolated regressions reproduced the false PASS for missing/unsafe
leaves. The correction checks existence, service UID/GID, 0700 mode, trusted
root0755 parent and absence of symlinks in both modes before writing a drop-in.
Only ARMED performs the database-device declaration check. No optional-path
prefix, directory auto-creation, quota default or permission relaxation was
introduced. Tests preserve an existing drop-in on refusal and keep PREPARED
independent of unapproved collection clocks/quotas. Final new-Head results are
recorded on #1300; prior a0f61b92 results below are historical, not new-Head CI.

Separately, the Owner-authorized Staging storage directories were created:
root:root0755 parent and UID999/GID9880700 collection/archive leaves on device
64770, matching PGDATA. The unique offline systemd probe
`v42-storage-preflight-a0f61b92-20260911T190430Z.service` used the actual service
identity and matched sandbox constraints; both leaves passed write/read/removal
of only fresh probe files, the parent was not writable, exit0. This did not
restart the application or claim the new application's writer was activated.
The old storage metadata and running PID658723 were unchanged.

The existing Chrome Dashboard was rechecked without clearing any site data.
Its stale visible page redirected to `/login`, matching zero active authenticated
database Sessions. Owner-only preview binding is therefore not yet established.
Recent bounded application logs contained no numeric quota facts. Shared-IP
headroom/account limits remain unproven; C1/C2 are unstarted, not filled with
guessed values. The old service remains active at 8d77902; no new deployment,
credential change, migration, model training or Owner-position operation occurred.

## V42 reviewed continuation and dedicated storage wiring — 2026-09-12

PR #1301 was re-reviewed without changing source Head
`e7670c495256559314db69154b3eafa28ab8e7e8`. The two ancestry/scope findings
had compared the gate Head with business PR #1300; the posted real-object
evidence instead binds #1300 to `f2a3903668d8e8129e18bace3d533b6273ebf8a6`.
All three source/merge/baseline ancestor checks passed. Its 23 business paths
are within the unchanged 64-path list; the four gate files remain separate.
The exact-Head re-review completed on 2026-09-11 18:32 UTC with no major issues,
and all three CI checks passed. No discussion was closed in lieu of review.
The actual #1301 squash merge is
`78a0c44738f1f583c6926d7a53e88bef32ed1fb2`.

Safe main synchronization preserves the entire original implementation and
audited-source ancestry. In the actual business checkout, not a test stub, the
resolver now reports `REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`,
`V42_ASSET_CARD_AUTHORIZATION_STATUS=AUTHORIZED`,
`IMPLEMENTATION_ALLOWED=true` and `RESOLUTION_BLOCK_REASON=NONE`, with #1300
identified as the authorized successor. Product Source, task validation and
the post-merge workflow contract passed. This is gate effectivity, not deployed
card or prediction acceptance.

The Owner-approved storage correction uses only
`/var/lib/rine-logic-asset-card` (root:root 0755) and its `collection`/`archive`
leaves (actual service UID/GID, 0700). The manifest explicitly binds both UID
and GID. Preflight continues to reject untrusted ancestors and symlinks, and
now verifies the exact leaf group and dedicated-parent mode/group. The card
drop-in adds only the two new leaves to `ReadWritePaths`; no base-unit path is
reset and no existing `/var/lib/rine-logic` content or permission is changed.
Historical paths and runtime observations below remain historical evidence.

The new regression first failed on the actual old `ReadWritePaths` line.
After the bounded patch, five storage/window focused cases and all 23 native
wiring tests passed (zero failures/errors/skips), including disposable
PostgreSQL. Python 49/49, the frontend event/time matrices, Shell/JS syntax
and diff checks also passed. Added
coverage preserves existing-directory contents/metadata and rejects missing
or wrong GID, wrong parent permissions and symlink ancestry. These isolated
fixtures do not prove the actual service sandbox can write the leaves: real
UID/GID, parent non-writability, same-PGDATA device and 20-GiB free-space checks
remain mandatory before the real start. Exact final candidate build/checksum,
full tests and CI results are recorded on the final #1300 Head; no earlier JAR
is relabelled as that candidate.

The latest read-only server observation at 2026-09-11 18:18:35 UTC still showed
`8d77902da7a66f26559a92a9646d99952f89e9b8`, active/running, readiness200,
PID658723 and no active systemd jobs. New card storage was not yet created;
old storage remained UID999:GID988 mode0750. Both existing `/var/lib` and actual
PGDATA report device64770, with 89,941,082,112 available bytes at observation.
These are not fresh release-time quota/storage evidence. C1 and C2 remain
unstarted; no missing shared-IP or account quota is filled by a template value.
MODEL_MODE=SHADOW; PRODUCTION_MODEL_READY=NO; ASSET_CARD_LIVE_READY=NO.

## V42 startup recovery and retained correctness fixes — 2026-09-11

This section records execution under the Owner's **one additional card-disabled
startup** approval, not a new collection allowance. Historical reports below
remain unchanged. The running release and the subsequent source correction are
different identities; a new source Head is not deployed by this approval.

### Actual controlled recovery

- Running merged main: `8d77902da7a66f26559a92a9646d99952f89e9b8`.
  Standard JAR SHA-256:
  `da5380a3034dd58369adf26a1b78adc7bd76b7c47ec25a0dc565cd9392a53304`.
  The candidate, uploaded and running bytes agree. Rollback remains exact
  `094b70a8ed31891999da0814fae5add09e2c4e08`, JAR SHA-256
  `b9e49308b0ce84e3d2033a07a571d009d8d1483b5770010fd7c74555dba0105b`.
- Prior attempt returned HTTP200 during `activating/start-post` and was wrongly
  rejected by an immediate `is-active` assertion. The corrected temporary
  controller is `/private/tmp/v42-startup-recovery-8d77902d.sh`; its exact diff
  against the previous controller is `/private/tmp/v42-startup-recovery-8d77902d.diff`.
  Controller SHA-256: `1cf6b2666d2d03535a56c2e2ee67f70b2a30a4b580765e9ccb2ed91b41c7ffc5`.
  This is a release-control artifact, not a hidden repository/business change.
- Fifteen local wait-state tests passed before execution. The existing unit's
  270-second startup timeout, `ExecStartPost` and exit semantics are unchanged;
  the controller has a finite 280-second observation budget and checks start-job
  exit, new invocation, stable Java PID, active/running and readiness together.
- Unique completed job:
  `v42-recovery-8d77902d-20260911T130300Z.service`, result success/exit0.
  Its log and source/diff are preserved under
  `/opt/rine-logic/releases/v42-recovery-8d77902d-20260911T130300Z/`.
  Observed transition: activating/start-post to active/running, same new PID
  `658723`, invocation `7595bd8e24fb4e2fba065652330806b4`, start-job exit0,
  readiness200; bounded start elapsed 8.698 seconds.
- V24 remains SUCCESS, checksum `-1944563506`; V23 remains SUCCESS,
  checksum `954499978`. No CREATE grant or migration replay occurred in this
  recovery. Independent connections confirmed migrator CREATE=false and
  SUPERUSER=false. Card enabled/external/writer remain false; no 40-card
  attachment has been installed and no collection window has started.

### Dedicated role, credentials and native runtime: distinguish each result

- Executed the approved three-new-table ACL convergence and writer bootstrap.
  Snapshot permits SELECT/INSERT/UPDATE, not DELETE; spot_bar and
  feature_history permit SELECT/INSERT/DELETE, not UPDATE. The old application
  role has none of these four rights on the three new tables. The exact role
  verifier passed; no old table/default ACL was changed.
- Protected Owner count/fingerprint stayed `28` /
  `8160d69e76965000e9eb7f749a13228a`; old default ACL fingerprint stayed
  `1b6b4aa26506b35a4dad6fe6552a9d81`; old non-card object ACL fingerprint stayed
  `c5ffb5cd9afbe47814d79557a6b293ac`. All three card table counts are still 0.
- Initial dedicated SCRAM credential was generated only in protected memory /
  root0400 candidate storage, set through the existing administration channel,
  verified by the exact running JAR through a new connection and atomically
  installed. Values were not printed, copied to ordinary environment/argv or
  hashed. Loopback HBA independently confirms `scram-sha-256`, not trust.
  `FRESH_CONNECTION=PASS`, `RESTART_REQUIRED=YES`; no restart followed.
- An independent non-Web systemd verifier under service UID999 correctly
  revealed a further runtime compatibility gap: systemd255 delivers root:root
  0440 credentials in a root:root 0550 read-only tmpfs, with a named ACL granting
  UID999 read (directory read/execute) and no other named user/group. The old
  Java expected-owner/0400 check rejects this. Root source-file verification is
  **not** claimed as service credential consumption. Actual application writer
  activation remains NOT_EXECUTED pending a reviewed source fix/new release.
- Actual target-host offline native probe passed for this JAR: Linux x86_64,
  Java17.0.20, XGBoost2.1.4, libgomp.so.1 (installed package
  `14.2.0-4ubuntu2~24.04.1`), feature version `SPOT_CARD_FEATURES_V2_SIGNED_PIT`,
  45 features / 3 TEST_FIXTURE_ONLY rows. LONG/SHORT raw and Beta maximum errors
  were all 0, exit0. The first probe rejected an incorrectly mixed temporary
  bundle directory before inference; the second used the exact three-file
  fixture directory. Neither file-list verification nor model checks were
  weakened. Both logs remain under
  `/tmp/v42-native-probe-8d77902d.dFnZW5Yz/`; test models were deleted afterwards.
  No fixture was installed as a production bundle. Probe output's generic
  `LOCAL_STANDARD_JAR` label describes the probe mode; the recorded execution
  host is Staging. This is native interoperability only, not whole-system or
  production-model acceptance.

### Remaining hard execution stops

1. Existing `/var/lib/rine-logic` is UID999/GID988, mode0750. The approved fixed
   collection/archive paths require root-trusted ancestry in the current
   preflight. Merely creating root-owned children cannot satisfy that check.
   No ancestor was chowned/chmodded, no symlink/mount bypass was used, and no
   collection directory or ARMED configuration was installed. This exact
   storage-contract/environment mismatch still requires resolution.
2. PRICE transport identity, per-user API/event/replay delivery, actual model
   lifecycle times and explicit Owner-only SHADOW UI projection are being
   corrected in the existing 64-path business worktree. They are not part of
   the running 8d77902 JAR and require their own full regression, review,
   candidate identity and exact-version deployment approval.
3. C1 GET probes and C2 finite collection are NOT_EXECUTED. No quota/window was
   invented or consumed, no auto extension occurred, and no real model was
   trained. New-card browser/latency/mature-label acceptance remains
   NOT_EXECUTED. The old page and startup success are not substituted for it.

`MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`,
`ASSET_CARD_LIVE_READY=NO`, `CURRENT_PHASE_DONE=NO`.

### Retained source correction and renewed verification — 2026-09-12

The same business worktree/branch continues from `b054d05e3c827d0a24978e378aac4edf993c2a25`;
the merged release and audited-source ancestry are preserved. These corrections
are **not** the running 8d77902 JAR. No new release, collection window, model
training, AI/Telegram call or Owner-position operation was performed here.

- PRICE now uses the actual trade identity for transport while preserving the
  separate durable signal/risk snapshot identity; repeated trades update only
  price fields and never reload Home or write a price-only snapshot.
- Card events use authenticated per-user delivery and current membership, not
  global broadcast/cache replay. Reconnection resolves a current safe snapshot;
  stream cleanup preserves the existing generic SSE lifecycle callbacks.
- Java and Python lifecycle validation use actual `labelAvailableAt`, retain
  the four-hour embargo, bind `validatedThrough` in the model, and enforce the
  same half-open validity interval. No threshold/formula changed.
- A single explicitly configured, Session-verified Owner may inspect SHADOW
  card price/risk. Preview never enables a model, exposes unqualified direction
  or confidence, or falls back to an old probability. Same-version field-failure
  revocations are checked against the full identity and cannot restore stale
  values or erase unaffected fields.
- Protected source credentials still require their existing 0400/0600 channel.
  Only the exact systemd runtime path additionally accepts the independently
  observed root0440/root0550, exact service-UID ACL and read-only trusted tmpfs.
  The bounded isolated Python helper reads metadata only; Java checks identity
  before/after reading and rejects extra ACL entries, wrong UID/path/environment,
  writable mount and missing prerequisites. No default-connection fallback.

At the 2026-09-12 read-only recheck, Staging was still active/running with PID
658723, the same invocation, readiness200 and the same 8d77902 JAR checksum.
The card drop-in was absent. The already-used extra startup was not repeated.

Local frontend event matrix and Python **49 tests / zero skips** passed again.
The new real-tmpfs test found that Docker Desktop kernel
`6.12.76-linuxkit` explicitly has `CONFIG_TMPFS_POSIX_ACL` unset (ext4 ACL is
enabled). A named tmpfs ACL fails with EOPNOTSUPP before Java is exercised.
This is **not** runtime acceptance. Only this proven non-CI macOS environment
is reported as a local test skip; the tagged Linux CI test must actually run,
and unsupported/missing runtime there fails the check. Its fixture has no host
mounts or network, uses a disposable tmpfs, and drops UID/GID/capabilities before
the actual production helper and Java reader execute. Ordinary directory tests
are not substituted for systemd ACL evidence.

The first full rerun also exposed a generated Python bytecode cache to an old
UTF-8 source scanner. The cache was preserved outside the repository at
`/private/tmp/v42-python-cache-preserved.zmnAA4`; no source-scanner assertion was
changed or skipped. Subsequent commands disable bytecode generation. Final
full-suite counts, candidate JAR identity and exact-head CI are recorded after
their actual execution; earlier 8d77902/fc95 evidence is not relabelled.

The remaining storage decision is explicit, not a request for broad new
authority: prefer a new root-owned `/var/lib/rine-logic-asset-card` parent with
service-owned `collection` and `archive` children, instead of changing the old
service-owned `/var/lib/rine-logic` parent. These paths are **proposed only**;
they have not been created or substituted into the fixed-path runtime contract.
Owner approval must cover the exact path adjustment and subsequent new-version
release. Before any ARMED start, recheck the actual PGDATA/state/archive device,
free space, shared-IP and account quota, bind the single authenticated preview
account and freeze the absolute window. No missing value is defaulted into an
authorization. The current card-disabled 8d77902 service stays in place while
these stops are unresolved.

Final pre-commit local regression: **5,552 tests / 520 suites / 0 failures /
0 errors / 14 skips**, exit0. The 13 historical opt-in skips remain unchanged;
the fourteenth is the explicitly unexecuted Linux tmpfs ACL case above. It is
not counted as a pass and requires actual exact-head Linux CI execution before
this credential correction is accepted. Log:
`/private/tmp/v42-0912-full-maven-verified.log`. The existing build plugin
regenerated actual worktree `b054d05e... / dirty=true` before this run; no Git
location overrides were inherited by test processes. The retained old JAR
rollback still boots after V24 with data retained (this rerun's Docker carrier
is aarch64; the preceding x86_64 rollback evidence remains separately recorded).
Python49, the frontend matrix, shell/JS syntax, diff and added-diff secret-pattern
check (zero matches) pass. Product Source, task validation, workflow-contract
and exact machine self-tests pass, including 40 V42 outer cases. All 23 changed
paths belong to the unchanged 64-path list; no wildcard or gate-owner edits.

The first follow-up Head `905e941c1b5beca6aa0de481c38e1f0b706789be`
failed required CI run `34621655003` at synthetic tmpfs initialization (mount
errno13), before the real credential assertions. Docker's default AppArmor
profile denies that mount independently of SYS_ADMIN. The correction is only
the disposable, network-disabled, no-host-mount test container's AppArmor option;
host, server, production unit and credential validation policies are unchanged.
The reader still runs as UID999 with empty supplementary groups and zero
effective/permitted/ambient capabilities. All positive and rejection assertions
remain mandatory in Linux CI; the failed run is not acceptance evidence.

## V42 unified finite-SHADOW execution package — 2026-09-11 follow-up

本节取代下方历史预检中的“仅人工停采集”“归档没有生产调用者”和“旧JAR回滚待验证”三个未完成结论；历史数据盘点、服务器身份和历史测试结果原样保留。**本轮仅实现、隔离测试、提交与推送；本节所有真实执行命令仍待 Owner 对这一份执行包统一批准。** 不合并、不安装、不授予真实权限、不联网探测或采集、不训练。

### A. 审批对象、已有证明与执行前身份停点

- 开始时本地／origin／Draft PR #1295同为 `4a3e418512a7aa50b2eaae204c0f10686a8f7031`。登记main `8af9ef5b08723b6058d7320730830ea2686d29a4`；64条实际路径、FP `4262f151a513d7bec00bcc9f0614531d8cae537f`不变。最终提交及exact-head CI记录在本节验收记录及同一PR交付评论中；不得用4a或旧b87c测试冒充最终Head。
- 发布批准必须绑定最终PR完整Head；之后获取实际Squash merged-main SHA，比较全部业务树，再从该**干净精确merged-main**构建。尚未合并，因此现在没有可伪填的“部署SHA”。源代码允许推送不等于服务器执行权限。
- 回滚实物已找到并在本轮真正启动：`094b70a8ed31891999da0814fae5add09e2c4e08`，JAR SHA256 `b9e49308b0ce84e3d2033a07a571d009d8d1483b5770010fd7c74555dba0105b`。其本地原字节在 `/Users/xuchao/Documents/trade-model-v1/target/trade-model-v1-0.0.1-SNAPSHOT.jar`；服务器匹配旧release路径见下方历史身份表。未改写该实物。
- 最终候选与回滚制品都必须存在、hash匹配；再次只读核对固定SSH身份、无活动／不明发布进程、x86_64 Java17、主unit/drop-in/readiness脚本及当前运行JAR/三字段元数据。任一变化停止发布并列diff，不复用旧上传、不使用`--collect`判断状态。
- 最终包保留默认 `SHADOW`、无生产bundle链接、训练导出开关false；默认应用连接、旧调度开关、方向／风险阈值／置信度公式、64路径和通用门禁不变。

### B. 本轮补齐的真实执行链

1. **归档再删除。** `AssetCardService`后台每60秒调用`RetentionLifecycle`，每资产每轮最多128行；专用writer事务锁与写入共用`symbol`锁，验证同一批原始身份／数量／完整内容。不可变内容寻址JSON写入、fsync、原子命名、SHA256复核、完整恢复读取全部成功后，才按精确主键及旧内容删除。snapshot没有DELETE。旧业务表及ACL不参与此链。
2. **待成熟保护。** 任一LONG/SHORT未成熟INFERENCE的依赖保护优先于保留时长；原始输入完整冻结在rawFrame，未来1m/5m及实际成交依赖不能按年龄清理。本次固定采集窗口内`protectedFrom=EPOCH`，不删除任何卡片历史，避免净行数减少抵消新增量预算。窗口结束后仅经已验证档案清理。缺horizon/闭线/时点证据仍PENDING，重启后不会填false标签。
3. **可导出与恢复。** LabelPipeline从DB及已验证档案联合恢复INFERENCE、成交、闭线、成熟双侧标签；保留原始observedAt/availableAt，不用文件归档时间替换它们。DB中已无记录也能追溯与导出，不重插重复label。缺原始CG身份的样本保留原始审计，但在训练导出中明确剔除，不能以“档案恢复成功”冒充训练合格。
4. **失败关闭。** 目录、SHA、内容、读取、空间、事务或锁失败保留原DB数据，停止本次清理并报告状态；归档/存储异常停止卡片联网。单文件32MiB、每资产最多8192档／128MiB读取上限，超限是明确阻断，不静默略档。初次共享主机使用一个受保护共享档案目录；未证明独立主机各自本地磁盘的多实例归档安全。
5. **有限租约。** `CollectionLease`使用service-owned0700真实目录，0600账本和锁，严格身份hash、原子replace及文件/目录fsync。绝对开始/结束、符号、配额和累计计数写入同一身份；正常关闭后重启继续旧值，不能重获8h。已初始化账本丢失、损坏、权限不符、时钟倒退、存量异常减少均拒绝联网；第一个持久停止原因不会被后续动作抹掉。T0之前STOP也持久化；I/O异常在进程内锁存且优先记录停止标记，不读取旧OPEN恢复。未记录正常关闭的旧进程身份（包括SIGKILL或无法持久化停止）在新进程中返回UNVERIFIED_PREVIOUS_COLLECTION_PROCESS，保持终止，不能用自动重启绕过。
6. **停止隔离。** 到期判断在每个入站帧／请求上生效，250ms卡片watchdog关闭卡片WS、取消卡片REST、清理卡片排队任务；15秒测量DB/WAL/空间。未完成的少量已执行事务与只读证据驱动离线成熟可结束；不延长联网、不改其他业务调度。这里不声称所有数据库写入瞬间为0。
7. **来源隔离。** aggregate-All OI、coin加权但单位未证明的funding、多所USD liquidation不得冒充Binance单合约USDT。它们返回UNKNOWN及具体原因；仅精确Binance/pair/1m账户比值及其已证明无量纲转换可用。rawFrame与审计使用同一个Provider快照；旧历史不重写。`trainingEligible=false`与逐字段原因随INFERENCE保留，导出拒绝未证明记录。现有独立有效高中风险不被其他UNKNOWN覆盖。

### C. 两个不同批准项：21 GET探测与固定8h采集

**C1：21 GET只读探测，未执行。** 沿用下方完整端点表：BTCUSDT、ETHUSDT、XRPUSDT，Binance klines(1m×5)、aggTrades(最近60s最多100)、depth5000各3次；CoinGlass exchange-list、oi-weight-history(1m×1)、aggregated-liquidation(Binance/OKX/Bybit,1m×60)、global-long-short-account-ratio(Binance/pair,1m×1)各3次。host只允许`api.binance.com`和`open-api-v4.coinglass.com`。

- T只在批准执行前冻结一次UTC整分钟。Kline范围T−300000..T−1ms；aggTrade T−60000..T−1ms。其他参数、端点和预算与下方表完全相同。
- 总上限21GET／10分钟／并发1／请求起始间隔至少20秒；重试0、分页0、批量历史0、WS0。Binance权重总上限768；CG最多12请求、≤3rpm且≤已证账号rpm的80%，只用已有额度，不购买或升级。账户额度未确认则CG请求不执行，不能调用接口来猜额度。
- 401/403/418/429、provider非成功码、超时、配额未知/不足、跨host重定向或来源/单位冲突立即停剩余请求；一次失败也不得用重试补齐“21”。探测响应不写生产卡片表、不启动服务、认证头不存日志。

**C2：最多8h有限SHADOW，未执行、不是训练条件。** 精确符号固定为本次已核注册资产36项，运行时再与现有注册集合求交，不修改资产池成员、排序或用户选择：

```text
AAVEUSDT,ADAUSDT,ALGOUSDT,APTUSDT,ARBUSDT,ASTERUSDT,AVAXUSDT,BCHUSDT,BNBUSDT,BTCUSDT,DOGEUSDT,DOTUSDT,ETCUSDT,ETHUSDT,FETUSDT,FILUSDT,HBARUSDT,HYPERUSDT,ICPUSDT,INJUSDT,LINKUSDT,LTCUSDT,NEARUSDT,OPUSDT,POLUSDT,RUNEUSDT,SEIUSDT,SOLUSDT,SUIUSDT,TAOUSDT,TRXUSDT,UNIUSDT,VETUSDT,XLMUSDT,XRPUSDT,ZECUSDT
```

- 先完成PREPARED启动/全部前置，再把未来一个UTC整分钟记为`COLLECTION_STARTS_AT=T0`、`COLLECTION_ENDS_AT=T0+28800s`；两值在第二次启动**前**冻结在保护manifest，window ID=`v42-shadow-<批准Head前12位>-<T0 UTC数字>`。延迟启动只缩短可用时间；重启原样复用，不重新计算T0。不是从每次进程启动起算8小时。
- 共享IP额度证明必须在T0之前5分钟内完成。`SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE=1000`为卡片自身最大预算，不是假定账户全额度；`SHARED_IP_WEIGHT_LIMIT_PER_MINUTE`填写实际已核官方限制且≥2000。当前limit/余量未知，manifest维持0/NOT_CONFIGURED并拒绝ARMED；不能伪填常见6000。每个真实响应必须有有效权重头，达到实际全IP限制80%或头缺失即停止卡片。本任务不改变共享调用频率，其他业务同IP用量计入安全停止。

| 项目 | 工程验收固定预算／触发值 |
|---|---|
| WS范围 | 每symbol真实Spot aggTrade、depth@100ms、闭合1m/5m/15m/1h/4h；最多252stream；只读行情，无用户数据/交易stream |
| WS重连／控制 | 全窗连接尝试最多32，动态订退控制最多128；失败退避最高120秒；重启保留累计值；无无限23h延长 |
| REST初始化／恢复 | 仅现有depth5000；全窗最多288次／权重72000（250/次）；一分钟最多1000；约36初始＋252恢复的上限，不保证全部用满 |
| CoinGlass持续额外调用 | 0；仅peek已有缓存。未知29资产或不可信字段保持UNKNOWN；不另开scan或补历史 |
| 真实成交记录估算 | 每资产每秒最多最后一笔真实aggTrade，36×28800=1,036,800条；不是每笔成交全量，也不是独立训练样本 |
| DB新增监测阈值 | 三卡片表合计新增1,100,000行；初次不清理抵扣；15秒检查，异常存量下降停止 |
| 数据库体积／WAL | database增长3,221,225,472bytes；WAL差值8,589,934,592bytes。两者包括同库／同cluster其他业务，是保守全局停止阈值，不冒充卡片独占计量 |
| 尾批／离线余量 | 阈值之外预留50,000行、512MiB数据库空间、1GiB WAL预算供15秒检测尾批/离线标签；不是允许再联网。预算审批上限1,150,000行、3.5GiB DB、9GiB WAL；触发值仍上述更低值 |
| 空闲空间 | state/archive/PGDATA必须证明同设备；启动时及每15秒至少21,474,836,480bytes；原生配置档案写入也采用21,474,836,480bytes下限。不同盘或无法证明则禁止ARMED，不能拿/opt空闲量替代PG盘 |
| 保留配置 | Bar168h、Trade168h、Feature720h、Label2160h；每60秒最多128行/资产、先保护未成熟依赖再归档校验后删除；不是训练样本门槛 |
| 训练与模型 | 自动训练0、导出开关false、模型安装0；SHADOW，CANARY/ACTIVE禁止，真实新置信度不发布 |

上述行数、DB和WAL是监测停止阈值，不是逐INSERT硬配额。到期或到任一预算停止连接，不为缺失horizon继续采集。已齐证据允许离线成熟，未齐保持PENDING直到另行批准的数据准备；既不填写失败标签，也不擅自延长窗口。没有有限新窗口明确批准时不得删除账本、换ID或调大预算重新启动。

### D. 待批准的精确发布顺序、两次受控启动及回滚

所有命令只在批准后的Staging、固定SSH身份、`rine_logic_staging`执行。密码仅走既有受保护认证渠道，禁止argv/普通env/日志/secret hash。下列准备不在本轮执行。

1. **候选/回滚冻结。** 等最终exact-head CI通过后另批合并#1295；取得真实merged-main。先完整回归，再按本目录README的`asset-card-native-evidence`命令在干净精确提交构建标准JAR，提取实际git.properties（SHA一致、dirty=false）、本地sha256；新唯一release目录及上传文件名包含完整SHA＋UTC。上传完成远端sha256须逐字一致；不覆盖/复用中断文件。备份当前b9e493…JAR、三字段发布元数据和卡片drop-in存在/缺失事实；主unit、旧env与core调度不变。
2. **迁移前停点。** 只读新连接确认DB名/PG16版本、V23SUCCESS、V24未执行、三新表不存在、rine_migrator非super且publicCREATE=false。记录旧表结构/ACL/defaultACL/数据基线的受控指纹；不输出Owner数据。若已经V24SUCCESS则只核对checksum/表身份并NO-OP，不重复GRANT/迁移。任何不匹配停止。
3. **独立权限回收保护。** GRANT前准备唯一新命名的独立`systemd-run --on-active=45s`一次性回收看门狗，固定命令为既有本地管理员认证下单句`REVOKE CREATE ON SCHEMA public FROM rine_migrator;`、目标仅rine_logic_staging，默认autocommit。不使用`--collect`。确认该回收任务已排程，才允许独立提交`GRANT CREATE ON SCHEMA public TO rine_migrator;`。控制流程成功/失败/TERM都立即用新连接独立提交REVOKE，再另一新连接读CREATE=false、SUPERUSER=false；看门狗保留运行以覆盖控制进程丢失，重复REVOKE幂等。日志不包含凭据。
4. **受控启动一：迁移且卡片关闭。** 仅准确merged-main候选JAR替换current并同步三字段真实元数据，enabled/external/writer全部false，无模型；沿用原Spring Flyway设置只运行V24三表及既定索引。最多45秒临时权限窗口；锁超时/失败/权限回收不能确认即停止候选。成功后必须先确认独立回收，再允许角色/凭据步骤。真实系统断电不是finally测试证明：恢复后先禁继续发布、独立REVOKE及fresh验证，确认前不得重新启动候选；不把本地线程测试冒充真实systemd容灾。
5. **仅三新表ACL收敛＋专用角色。** V24SUCCESS后管理员执行已审脚本`asset-card-role-bootstrap.sql`，显式`-v card_reconcile_new_table_acl=true`。只撤销rine_app在三张新卡片表的SELECT/INSERT/UPDATE/DELETE，然后建立/验证rine_asset_card_writer。**不改旧default ACL，不撤销旧表权限。** 发现PUBLIC、成员关系、ownership、column grant或definer带来额外访问立即停止，不全库REVOKE。矩阵：snapshot S/I/U、禁止D；spot_bar与feature_history S/I/D、禁止U；其他业务表、序列、DDL、CREATE、TEMP、角色继承、额外权限全部拒绝。旧连接仍按原业务ACL读取。
6. **精确目录、凭据及PREPARED配置。** root-owned0700 `/etc/rine-logic/credentials`，密码候选root0400只走保护输入；source为`/etc/rine-logic/credentials/asset-card-db-password`。仅专用角色设置SCRAM认证，不把口令写入脚本。`asset-card-runtime-credentials.sh --prepare --candidate <同目录保护文件> --confirm PREPARE_OR_ROTATE_ASSET_CARD_CREDENTIAL_ONLY`调用候选标准JAR的独立新连接校验DB身份/全部权限，通过后才原子安装；失败保留旧inode、输出RESTART_REQUIRED，不自动重启。模型根`/opt/rine-logic/models/asset-card`root只读，**不安装生产模型current链接**。state和archive叶目录为rine-logic UID999、0700，可信root祖先，路径见manifest；既有40-asset-card.conf仅卡片附加源可更新。
7. **第二启动前停点。** PREPARED本身三开关false，不是已采集。先完成角色fresh验证、实际service读取凭据、原始JAR原生探针、源/共享IP/预算证明、PGDATA与state/archive同设备且空闲≥20GiB、全部真实manifest身份。安装脚本DRY_RUN先PASS。保护manifest填上C2精确绝对窗口及已核配额；ARMED只接受完整安全值。`asset-card-runtime-install.sh --apply --confirm INSTALL_ASSET_CARD_ARMED_WINDOW_ONLY`只写40-asset-card.conf，保留旧卡片配置和非秘密回滚清单，不自动reload/restart。
8. **受控启动二：同一JAR的有限SHADOW。** Owner一次批准此包后才执行一次daemon-reload/restart，enabled/external/writer=true、SHADOW、export=false、四个保留周期生效；任何旧全局/AI/Telegram/交易/持仓调度值不改。检查current exactSHA、active/readiness200、凭据身份、专用pool+默认pool、账本时间/预算和0/5m/10m/每小时/到期状态。不能因readiness200就宣布数据链通过。到期自动停止卡片网络；新启动继续同一账本。除失败回滚外，不额外启动候选或扩大窗口。
9. **失败回滚。** 在独立CREATE回收证据通过后，停止候选，恢复原JAR和原元数据b9e493…/094b70a8…，恢复之前卡片drop-in（原来不存在则仅停用本次新卡片附加配置，保留可恢复副本）。原main unit/core/env/Owner数据不改。daemon-reload/start旧服务并验证旧SHA/readiness200。**新表、迁移历史、采集数据、档案、停止账本保留**；不DROP、repair、关闭validate或删数据。角色/ACL不为回滚扩大到旧账号，旧Jar已实证不需要卡片表权限。

### E. 停止点及恢复动作

| 停止条件 | 自动／执行控制动作 | 允许恢复方式 |
|---|---|---|
| Head/JAR/hash/SSH/main unit或元数据不一致 | 不替换、不GRANT、不启动 | 重新只读列实际差异，审批不自动转移到新身份 |
| 迁移失败／控制任务中断 | 控制流程及独立watchdog回收CREATE；验证后恢复旧JAR | 保留V24已成功对象；Flyway失败事务不修history，不重复权限试探 |
| CREATE回收无法确认 | 停候选及所有后续步骤 | 只核验回收；未确认前不继续ACL/凭据/联网或网页验收 |
| PUBLIC/旧defaultACL异常扩大 | 专用验证拒绝，停在新对象步骤 | 只列精确权限来源；不动旧ACL，不用旧账号fallback写入 |
| 凭据/专用连接失败 | 新候选连接关闭；不替换旧凭据、不回退应用pool | 修正保护候选后再新连接verify；需要restart明确标注 |
| quota未知、401/403/418/429、80%sharedIP、budget耗尽 | 持久终止原因、abort卡片WS、cancel卡片REST、清队列 | 原窗口保持终止；不换ID/续时/增额；另批才可新窗口 |
| 到期 | 同上；已接收有限尾批可完成，离线成熟仅用既有证据 | 未成熟保持PENDING；不自动联网补标签 |
| 空间/DB/WAL异常、档案/校验失败 | 停新采集及本轮清理，原证据保留 | 修复存储/校验原因并只读证明；不得先删未验档数据腾空间 |
| CoinGlass来源/单位/窗口不明 | 仅该证据UNKNOWN，训练资格false | 不猜测补齐、不改算法；未来精确探测/来源证明另有结果才可接纳 |

### F. 本地验证与真实剩余事项

- 隔离PG16.14（缓存官方镜像）复制Staging16.15的defaultACL条件；补丁级版本不同如实记录。V24只新增3表／既定索引，成功、锁超时失败及中断路径独立COMMIT回收、新连接CREATE=false。真实专用writer、Hikari池1、advisory锁、WAL/DB测量、归档读回及精确删除通过。
- 本轮实际旧JAR在隔离Linux x86_64真正启动readiness200；V1–V23迁移字节23/23一致，原Flyway验证规则将V24保留为未来版本并NO-OP。V24 history、新三表及测试行、旧ACL/defaultACL/全行指纹、原JAR字节均未变；没有关闭校验或模拟旧main。
- 本轮最终完整Maven：**5533 tests／520 suites／0 failures／0 errors／13 skips，exit0**。日志`/private/tmp/v42-release-full-maven-final.log`，SHA256 `4dee0f5276dd1e57598b78dd69470b272c896efcb2edf29944650b9c89238e1a`。停止边界focused：Market41＋Service43＝84，0失败/错误/跳过；Native19＋V24五项＝24，0失败/错误/跳过。V24实际控制进程SIGKILL=137后，独立回收者新连接REVOKE+COMMIT，另一连接CREATE=false；这是隔离看门狗证明，不是已在Staging安装。最终Head CI在同一PR交付评论核对，不复用旧Head。
- Python45／0失败／0跳过、前端事件矩阵与北京时间矩阵PASS；JS/shell语法、diff及高可信秘密模式扫描PASS（新增diff匹配0）。Product Source、任务校验、workflow-contract含V42真实外层自测PASS。实际工作区在提交前如实显示BLOCKED_WORKTREE_DIRTY；不改门禁，提交后须重新核对真实干净外层IMPLEMENTATION_ALLOWED=true。
- 测试日志：停止focused SHA256 `4f348b832af27a73a63151d0787d001a51712ca10cccaffc444a7a8ad45ec888`；Native/V24 `2a7ca51b0f748a7fd2fbb1530baf0d1f060e8ee1f1bdb36346c35612b2c003d2`；Python `3fdf9eb8e8dfeba61877454bee54ca1f29f7c7126ec5da8c9dffce90cfb3b519`；前端 `879db747162704ebb94474505b2fd308be5d014e30652e4e0c4140eed8848fc2`；时间 `e9a510a787e647a096b2083c7986197039935ba94517158eb384bfb1eea62326`。
- 本轮13项skip未新增或放宽：ControlledCurrentStateContentFingerprintTest七项（rollbackRestoresFingerprint、fingerprintOutputDoesNotContainRawModifiedValues、sameDataProducesMatchingFingerprint、sameRowCountTimeMutationIsDetected、sameRowCountPlanBoundaryMutationIsDetected、sessionTimezoneDoesNotChangeFingerprint、sameRowCountStatusMutationIsDetected），因P3 content-fingerprint显式环境未启用；ControlledCurrentStateCloneFlywayActionTest一项，P3受控DB未启用；ControlledGeneratedReleaseLikeFixtureFlywayTest一项，generated fixture环境未启用；ControlledGreenfieldFlywayV7ActionTest一项，P3-G未启用；ControlledPostgreSqlFlywaySmokeTest一项，真实外部PG环境未授权；ControlledP3hComposeOfflineSmokeTest一项，独立P3-H opt-in未启用；CoinGlassControlledSmokeTest一项，COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS不存在。新卡片PG、旧JAR回滚、原生fixture互操作均实际本地执行，不以skip冒充通过。
- 真实服务器同设备/实际共享IP限额及CG账号额度目前**未重新执行验证**，是发布第二启动／C1的明确前置值；源实现已有拒绝机制，不是假称已通过。真实SHADOW、原生服务器probe、重启断流、4h真实标签和页面新模型验收仍NOT_EXECUTED。
- 原始样本、成熟标签、有效非重叠4h簇、Brier/ECE/LogLoss、RANGE/WATCH及时间外结果仍NOT_AVAILABLE；8h只是管道工程验收，不具备训练或上线百分比资格。CG不合格不妨碍SHADOW冻结原始证据/合格标签成熟，但禁止它进入训练。

本轮实际20条改动如下，均在64内；完整业务分支相对origin/main仍为60条变更，范围外0、重复路径0、白名单/门禁变更0。算法及旧canonical源文件本轮无diff：

```text
deploy/native-staging/README.md
deploy/native-staging/asset-card-role-bootstrap.sql
deploy/native-staging/asset-card-runtime-install.sh
deploy/native-staging/asset-card-runtime-manifest.template
deploy/native-staging/asset-card-runtime-preflight.sh
deploy/native-staging/rine-logic-asset-card.conf.template
docs/evidence/asset_card_live_signal/IMPLEMENTATION_AND_ACCEPTANCE.md
src/main/java/org/example/trademodel/assetcard/AssetCardEvidenceService.java
src/main/java/org/example/trademodel/assetcard/AssetCardFeatureService.java
src/main/java/org/example/trademodel/assetcard/AssetCardMarketDataService.java
src/main/java/org/example/trademodel/assetcard/AssetCardProperties.java
src/main/java/org/example/trademodel/assetcard/AssetCardService.java
src/main/java/org/example/trademodel/mapper/AssetCardMapper.java
src/test/java/org/example/trademodel/assetcard/AssetCardEvidenceServiceTest.java
src/test/java/org/example/trademodel/assetcard/AssetCardFeatureServiceTest.java
src/test/java/org/example/trademodel/assetcard/AssetCardMarketDataServiceTest.java
src/test/java/org/example/trademodel/assetcard/AssetCardServiceTest.java
src/test/java/org/example/trademodel/mapper/AssetCardMapperIntegrationTest.java
src/test/java/org/example/trademodel/postgresql/NativeStagingAssetCardInfrastructureContractTest.java
src/test/java/org/example/trademodel/postgresql/V24AssetCardLiveSignalMigrationContractTest.java
```

审批请一次覆盖D1–D9及分别列项的C1/C2（可以明确不批准其中某项，未批准项保持不执行）；所有前置停点继续有效。不是授权无限重试、外部数据批量下载、其他业务变更、模型训练、CANARY/ACTIVE或Production。

```text
BUSINESS_PR_1295_STATE=DRAFT_UNMERGED
MODEL_MODE=SHADOW
PRODUCTION_MODEL_READY=NO
REAL_STAGING_NATIVE_ACCEPTANCE=NOT_EXECUTED
REAL_DATABASE_PERMISSION_CHANGE=NO
BUSINESS_PR_MERGE_EXECUTED=NO
DEPLOY_EXECUTED=NO
EXTERNAL_PROBE_EXECUTED=NO
NEW_LIVE_COLLECTION_STARTED=NO
REAL_MODEL_TRAINING_EXECUTED=NO
ASSET_CARD_LIVE_READY=NO
CURRENT_PHASE_DONE=NO
```

## V42 Staging read-only preflight and SHADOW release preparation — 2026-09-11

本节是本轮只读结果与待批准方案；下面此前的本地集成、原生预测和历史测试记录保持原样，不视为真实 Staging 验收。本轮不安装依赖、不改变服务器、ACL、数据或开关，不合并、不部署、不启动采集或训练。

### 结论与本轮边界

- Staging 的现有 native systemd/JAR 链可访问：x86_64、Java 17、service active/running、readiness 200、libgomp.so.1 已存在。它仍运行 094b70a8 的旧卡片版本，不是 V42。
- 已登记观察资产为36项；以 tm_asset_pool_item 的 active symbol 并集限定所有 SQL，未扩展交易所资产范围、未读取 Owner/持仓行。此并集包含首页可选集合，但不是对某个登录用户此刻可见卡片子集的截图证明。本轮浏览器连接只返回 browser 对象，未取得标签内容；未枚举其他窗口/历史/域名，未清理会话。下一阶段仍须核对实际展示资产。
- 36项均有 Binance Spot 5m/15m/1h/4h记录，共111,866根，来源分组内原始行数=去重闭线数。1m为现有表确实没有记录；三张 V24 表则是“表未创建”，不能记成0行。训练、校准、成熟标签和独立4小时有效样本量均 NOT_AVAILABLE。
- 不能开始合格真实训练。K线不等于包含当时可用时间、真实盘口/成交终点、完整衍生品来源、费用/滑点和成熟标签的训练样本。现有部分CG衍生记录可用于调查，不可直接当原始市场观测或独立样本。
- 已确认发布前需要处理的事项：V24未部署、writer账号/凭据/目录未创建；旧元数据路径与模板不同（本轮只修源预检）；默认ACL会向 rine_app 授予新表DML；部分5m存量过期；CG聚合来源与内部单交易所身份/单位存在证明缺口。没有发现必须等待“90天”的证据。
- 本轮必要接线修复只改 manifest、preflight、README、现有基础设施测试，并更新本证据文件。64条清单、门禁逻辑、算法、其他业务模块均不变。

### 1. 代码、授权、制品与实际运行身份

| 项目 | 本轮核验结果 |
|---|---|
| 保留业务基线 / PR #1295 | b87c18a55be8f84ebe8ab374a0a917f34439d1e0；开始时本地/远端/PR一致，Draft、未合并，60条整分支改动均在64内 |
| 登记实际合并 / origin/main | #1299 / 8af9ef5b08723b6058d7320730830ea2686d29a4；重新fetch后相同 |
| 来源祖先 | 2921a4a98254a4bd88f3138ed4eb2e0487b3956b及8af9ef5均为业务Head祖先 |
| 实际门禁 | V42真实外层 AUTHORIZED_IMPLEMENTATION_PACKAGE、IMPLEMENTATION_ALLOWED=true、RESOLUTION_BLOCK_REASON=NONE、64路径，FP 4262f151a513d7bec00bcc9f0614531d8cae537f |
| 保留本地标准JAR | target/trade-model-v1-0.0.1-SNAPSHOT.jar；SHA256 85d30085cbf0c2b24618d9c79448c2bbcb821cc1775cdccd3ec7efcc809c966a；嵌入真实b87c18a…、git.dirty=false；本轮未重打包、未将其改标成后续Head |
| 实际运行 / 元数据SHA | MERGED_MAIN_SHA=094b70a8ed31891999da0814fae5add09e2c4e08 |
| 实际JAR / 校验值 | /opt/rine-logic/current/app.jar；b9e49308b0ce84e3d2033a07a571d009d8d1483b5770010fd7c74555dba0105b |
| 实际发布元数据 | /opt/rine-logic/current/deployment-metadata.txt；KEY_VALUE_V1，只有 MERGED_MAIN_SHA / ARTIFACT_SHA256 / DEPLOYED_AT；2026-09-10T09:59:10Z |
| 元数据本身SHA256 | ab27773fb9d1a853e3404273215c2c3b9111caddb038dad58804b9f583a06ee9；JAR hash字段与实物一致；不假装这是JAR内Git证明（旧JAR无git.properties） |
| 匹配的保留发布制品 | /opt/rine-logic/releases/card-pool-094b70a8-20260910T094500Z/app-094b70a8-20260910T094500Z.upload.jar；同一 b9e49308… 校验值 |
| 不可误用的旧制品 | 093000Z目录app.jar为6bfd9841…，与当前不符；不选作“当前版本”回滚包，不覆盖/复用旧上传 |
| 当前提交后的新候选 | 本轮变更Head在PR更新中单独记录；未来只有精确merged-main的新构建可以部署。b87c的JAR不可被重标为新Head或merged-main制品 |

真实外层同时保留全局历史提醒 MAIN_BEHIND_ORIGIN / P0_0_DONE_PENDING_MERGED_MAIN；没有改写它们来宣称业务完成。当前V42权限与历史主工作区提醒分开报告。业务PR合并与部署权限仍为false。

### 2. 实际 Staging 启动链、权限与当前开关

已验证 SSH 固定主机身份及 Tailscale 目标在线。仅访问 rine-staging；未输出秘密文件正文/哈希、口令、令牌或全量环境。

| 非敏感事实 | 实际结果 |
|---|---|
| 服务 | rine-logic.service active/running；User/Group=rine-logic，UID999；MainPID452233，启动2026-09-10 09:59:02 UTC（只读观察时刻） |
| Java / systemd | /usr/bin/java → Java17.0.20，x86_64；systemd255，支持LoadCredential能力；这不是凭据已接通的证明 |
| 主unit | /etc/systemd/system/rine-logic.service，root:root 0600；SHA256 04c8f91f7df6139433f4fd5d4d1e06fedac449f3b867443fb10c81b1ada4bf2d |
| 唯一现有drop-in | /etc/systemd/system/rine-logic.service.d/20-core-loop-schedulers.conf，root:root 0644；SHA256 3f4a6a756958bc0e2730a766d549afc9121376a8d4e40c49fbb3f5de994812ab |
| readiness脚本 | /usr/local/sbin/rine-logic-wait-ready，root、gid988、0750；SHA256 41b22da418fdf86d6cf7723714f0b312066624bf2398d36542796d78de0f522c；localhost:8081/actuator/health/readiness=200 |
| 基础目录 | /opt/rine-logic/current为root0755真实目录，而非symlink；主unit、旧env文件及core scheduler不由卡片工具替换 |
| 待创建 | /opt/rine-logic/models/asset-card、/etc/rine-logic/credentials、40-asset-card.conf均未安装 |
| 原生库 | /lib/x86_64-linux-gnu/libgomp.so.1存在；未安装/升级；本轮没用真实服务器做XGBoost预测 |
| 发布任务 | 未发现活动/过渡发布进程；历史failed rine-deploy-runtime-truth-0d6a1e36-20260907T133100Z.service PID=0/ControlPID=0，退出1，9月7日结束；未collect/reset-failed/清理 |
| 磁盘（/opt所在FS） | total120,217,645,056bytes、available90,360,766,464bytes；不是数据库占用/写入增长测量 |
| 数据库 | rine_logic_staging，PostgreSQL16.15，localhost:5432，SCRAM-SHA-256；public owner=rine_migrator |
| 迁移 | Spring Flyway enabled=true、user=rine_migrator；最新V23 SUCCESS，V24未执行；旧JAR没有V24和AssetCardProperties |
| 当前连接 | pg_stat_activity观察到10个rine_app idle连接；不读取密码或环境秘密正文 |

实际进程中的非敏感总开关：

```text
TRADE_MODEL_PROVIDER_CALL_ENABLED=true
TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED=true
TRADE_MODEL_COINGLASS_ENABLED=true
TRADE_MODEL_COINGLASS_EXTERNAL_CALLS_ENABLED=true
TRADE_MODEL_SCHEDULERS_ENABLED=true
TRADE_MODEL_PRODUCTION_SCHEDULER_POLICY=EXPLICIT_OPT_IN
SPRING_FLYWAY_ENABLED=true
TRADE_MODEL_ASSET_CARD_ENABLED=UNSET_IN_PROCESS_ENV
TRADE_MODEL_ASSET_CARD_EXTERNAL_CALLS_ENABLED=UNSET_IN_PROCESS_ENV
TRADE_MODEL_ASSET_CARD_WRITER_ENABLED=UNSET_IN_PROCESS_ENV
TRADE_MODEL_ASSET_CARD_MODEL_MODE=UNSET_IN_PROCESS_ENV
```

卡片类根本不在运行JAR中，因此不是“SHADOW已运行”；SHADOW只是候选默认/下一步唯一允许模式。只启动候选时默认enabled/external/writer=false，modelMode=SHADOW。当前prod是Staging的Spring profile，不表示本轮访问或部署了Production。上述现有全局开关不得在本任务中扩大或重新启用其他子调度器。

数据库核验均 BEGIN READ ONLY，statement_timeout=8或10秒、lock_timeout=1秒、有限聚合结果、COMMIT；没有DML/DDL/ACL执行。两个只读补充命令曾在启动前收到自动审核技术错误原文：
“Automatic approval review failed: Selected model is at capacity. Please try a different model.”
复核其只读语句范围后，同一命令分别只重新送审一次，获准执行；没有改路径、SQL或工具规避审核。无明确危险hunk被绕过。

有效权限事实：

- rine_app：LOGIN、非SUPERUSER，INHERIT但无实际角色成员关系；public CREATE=false，DB CREATE/TEMP=false。既有表上SELECT/INSERT/UPDATE各45、DELETE44；Spring Session两表另有REFERENCES/TRIGGER/TRUNCATE。**真实native默认应用角色不是P3-H只读角色**；本轮不撤销它的旧权限，不把这点隐藏。默认读取与卡片writer隔离仍须下一阶段实际证明。
- rine_migrator：非SUPERUSER，public CREATE=false；DB CREATE/TEMP=true；无实际角色成员关系。不得把它用作writer。
- PUBLIC：本轮未发现database ACL、public schema ACL、用户域表/列/序列授权或可调用SECURITY DEFINER额外权限。实际writer尚不存在，不能声称writer权限验收PASS。
- **确认阻断：** rine_migrator在public的默认表ACL把SIUD授予rine_app；默认序列ACL授予SELECT/UPDATE/USAGE。V24没有序列，但新表会继承旧表默认授权，不能据“V24无GRANT”推断默认账号不能写卡片表。
- 最小选项：不改旧default ACL/旧表/旧账号。V24成功后、卡片写入前，单独审核并仅收回rine_app对三张新卡片表的继承对象授权，再授予专用writer精确矩阵。若Owner要求旧角色对新表保留只读，可保留SELECT、仅撤销新表I/U/D；若要求卡片独占则仅三表SIUD全撤。**两者均为待批准选择，本轮未执行。** 不能对整个DB执行REVOKE或修改默认连接权限。
- 不需要为了当前PUBLIC情况执行全库REVOKE。执行窗口仍应重新核验，发现新增PUBLIC/角色成员冲突则停止。

### 3. 数据覆盖清单与实际可用性

K线与CG状态聚合观察基准为2026-09-11T09:22:54.231646Z；随后衍生证据聚合约09:33–09:36Z。不是跨SQL全局原子行情快照；每条已读查询在只读事务内完成，不能把其时间误差当作数据变化证据。

Binance表：tm_persisted_ohlcv_bar。下表均BINANCE_PUBLIC / SPOT；144组均closed=true、quality=OK、source=READY，sourceVersion、endpoint、trace、provenanceVersion、fetch_time/ingested_at齐全，去重以(symbol,provider,market,timeframe,open_time)计。stored FRESH不是当前TTL验收。每个周期“内部缺口”不含开始前/结束后的未覆盖区间。

- **36项的1m：** 表存在、该周期原始/去重行数均0、时间范围/覆盖天数不可得；旧scheduler只写5m/15m/1h/4h。不能生成四小时完整1m标签路径。
- **XRP5m缺口：** 2026-08-26T15:15:00Z、15:20:00Z两根缺失，下一根15:25。其余已存范围内部连续，但不保证历史前端或当前尾部完整。
- **陈旧尾部：** ADA、AVAX所有4周期停在9月10日；BCH、BNB的5m停在9月10日20:34/20:39，其他周期较新。不能直接当当前完整5m特征帧。其余资产在本次09:14–09:19闭线附近有数据，下一次激活仍需重新验证TTL、各24根连续历史。
- 另有KRAKEN/SPOT历史（5m1703、15m970、1h693、4h624根），不混入下表，不冒充Binance Spot。USDT符号标识也不能将Kraken来源改名。
- fetch/ingested为旧表UTC-naive字段；查询会话UTC，卡片Mapper解释UTC并要求两者不晚于cutoff。8月7日起的4h条目可能到8月24才首次获取；它们可用于那之后的回看窗口，不能伪称8月7日当时已经可用。当前聚合只证明元数据完整，逐样本PIT可用性尚未验证。

| 资产 | 周期 | 最早open UTC | 最新close所在分钟 UTC | 覆盖天数 | 原始/去重 | 缺口段/缺根 | 首次fetch → 最新available UTC |
|---|---|---|---|---:|---:|---:|---|
| AAVEUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| AAVEUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:06Z |
| AAVEUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 08:05Z |
| AAVEUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:14Z | 4.6 | 1325/1325 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| ADAUSDT | 15m | 2026-08-23 09:00Z | 2026-09-10 20:29Z | 18.48 | 1774/1774 | 0/0 | 2026-08-24 10:02Z → 2026-09-10 20:30Z |
| ADAUSDT | 1h | 2026-08-20 06:00Z | 2026-09-10 19:59Z | 21.58 | 518/518 | 0/0 | 2026-08-24 10:02Z → 2026-09-10 20:00Z |
| ADAUSDT | 4h | 2026-08-07 16:00Z | 2026-09-10 19:59Z | 34.17 | 205/205 | 0/0 | 2026-08-24 10:02Z → 2026-09-10 20:00Z |
| ADAUSDT | 5m | 2026-08-24 01:45Z | 2026-09-10 20:39Z | 17.79 | 5123/5123 | 0/0 | 2026-08-24 10:02Z → 2026-09-10 20:41Z |
| ALGOUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| ALGOUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:06Z |
| ALGOUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 08:05Z |
| ALGOUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:14Z | 4.6 | 1325/1325 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| APTUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| APTUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:06Z |
| APTUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 08:05Z |
| APTUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:14Z | 4.6 | 1325/1325 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| ARBUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| ARBUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:06Z |
| ARBUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 08:05Z |
| ARBUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:14Z | 4.6 | 1325/1325 | 0/0 | 2026-09-07 03:11Z → 2026-09-11 09:18Z |
| ASTERUSDT | 15m | 2026-09-06 01:15Z | 2026-09-11 09:14Z | 5.33 | 512/512 | 0/0 | 2026-09-07 02:16Z → 2026-09-11 09:18Z |
| ASTERUSDT | 1h | 2026-09-02 22:00Z | 2026-09-11 08:59Z | 8.46 | 203/203 | 0/0 | 2026-09-07 02:16Z → 2026-09-11 09:06Z |
| ASTERUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 02:16Z → 2026-09-11 08:05Z |
| ASTERUSDT | 5m | 2026-09-06 17:55Z | 2026-09-11 09:14Z | 4.64 | 1336/1336 | 0/0 | 2026-09-07 02:16Z → 2026-09-11 09:18Z |
| AVAXUSDT | 15m | 2026-09-06 02:00Z | 2026-09-10 20:29Z | 4.77 | 458/458 | 0/0 | 2026-09-07 03:12Z → 2026-09-10 20:31Z |
| AVAXUSDT | 1h | 2026-09-02 23:00Z | 2026-09-10 19:59Z | 7.87 | 189/189 | 0/0 | 2026-09-07 03:12Z → 2026-09-10 20:01Z |
| AVAXUSDT | 4h | 2026-08-21 08:00Z | 2026-09-10 19:59Z | 20.5 | 123/123 | 0/0 | 2026-09-07 03:12Z → 2026-09-10 20:01Z |
| AVAXUSDT | 5m | 2026-09-06 18:50Z | 2026-09-10 20:34Z | 4.07 | 1173/1173 | 0/0 | 2026-09-07 03:12Z → 2026-09-10 20:37Z |
| BCHUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:12Z → 2026-09-11 09:19Z |
| BCHUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:12Z → 2026-09-11 09:01Z |
| BCHUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:12Z → 2026-09-11 08:00Z |
| BCHUSDT | 5m | 2026-09-06 18:50Z | 2026-09-10 20:34Z | 4.07 | 1173/1173 | 0/0 | 2026-09-07 03:12Z → 2026-09-10 20:37Z |
| BNBUSDT | 15m | 2026-08-23 09:00Z | 2026-09-11 09:14Z | 19.01 | 1825/1825 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:16Z |
| BNBUSDT | 1h | 2026-08-20 06:00Z | 2026-09-11 08:59Z | 22.12 | 531/531 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:00Z |
| BNBUSDT | 4h | 2026-08-07 16:00Z | 2026-09-11 07:59Z | 34.67 | 208/208 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 08:00Z |
| BNBUSDT | 5m | 2026-08-24 01:45Z | 2026-09-10 20:39Z | 17.79 | 5123/5123 | 0/0 | 2026-08-24 10:02Z → 2026-09-10 20:41Z |
| BTCUSDT | 15m | 2026-08-23 09:00Z | 2026-09-11 09:14Z | 19.01 | 1825/1825 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:16Z |
| BTCUSDT | 1h | 2026-08-20 06:00Z | 2026-09-11 08:59Z | 22.12 | 531/531 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:00Z |
| BTCUSDT | 4h | 2026-08-07 16:00Z | 2026-09-11 07:59Z | 34.67 | 208/208 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 08:00Z |
| BTCUSDT | 5m | 2026-08-24 01:45Z | 2026-09-11 09:19Z | 18.32 | 5275/5275 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:21Z |
| DOGEUSDT | 15m | 2026-09-05 13:30Z | 2026-09-11 09:14Z | 5.82 | 559/559 | 0/0 | 2026-09-06 14:42Z → 2026-09-11 09:16Z |
| DOGEUSDT | 1h | 2026-09-02 10:00Z | 2026-09-11 08:59Z | 8.96 | 215/215 | 0/0 | 2026-09-06 14:42Z → 2026-09-11 09:00Z |
| DOGEUSDT | 4h | 2026-08-20 20:00Z | 2026-09-11 07:59Z | 21.5 | 129/129 | 0/0 | 2026-09-06 14:42Z → 2026-09-11 08:00Z |
| DOGEUSDT | 5m | 2026-09-06 06:20Z | 2026-09-11 09:19Z | 5.12 | 1476/1476 | 0/0 | 2026-09-06 14:42Z → 2026-09-11 09:21Z |
| DOTUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:12Z → 2026-09-11 09:19Z |
| DOTUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:12Z → 2026-09-11 09:01Z |
| DOTUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:12Z → 2026-09-11 08:00Z |
| DOTUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:14Z | 4.6 | 1325/1325 | 0/0 | 2026-09-07 03:12Z → 2026-09-11 09:19Z |
| ETCUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| ETCUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:02Z |
| ETCUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 08:01Z |
| ETCUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| ETHUSDT | 15m | 2026-08-23 09:00Z | 2026-09-11 09:14Z | 19.01 | 1825/1825 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:16Z |
| ETHUSDT | 1h | 2026-08-20 06:00Z | 2026-09-11 08:59Z | 22.12 | 531/531 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:00Z |
| ETHUSDT | 4h | 2026-08-07 16:00Z | 2026-09-11 07:59Z | 34.67 | 208/208 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 08:01Z |
| ETHUSDT | 5m | 2026-08-24 01:45Z | 2026-09-11 09:19Z | 18.32 | 5275/5275 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:20Z |
| FETUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| FETUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:02Z |
| FETUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 08:01Z |
| FETUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| FILUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| FILUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:02Z |
| FILUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 08:01Z |
| FILUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| HBARUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| HBARUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:02Z |
| HBARUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 08:01Z |
| HBARUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| HYPERUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| HYPERUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:02Z |
| HYPERUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 08:01Z |
| HYPERUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:13Z → 2026-09-11 09:20Z |
| ICPUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:15Z |
| ICPUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:03Z |
| ICPUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 08:02Z |
| ICPUSDT | 5m | 2026-09-06 18:45Z | 2026-09-11 09:19Z | 4.61 | 1327/1327 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:21Z |
| INJUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:15Z |
| INJUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:03Z |
| INJUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 08:02Z |
| INJUSDT | 5m | 2026-09-06 18:45Z | 2026-09-11 09:19Z | 4.61 | 1327/1327 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:21Z |
| LINKUSDT | 15m | 2026-09-06 01:15Z | 2026-09-11 09:14Z | 5.33 | 512/512 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 09:15Z |
| LINKUSDT | 1h | 2026-09-02 22:00Z | 2026-09-11 08:59Z | 8.46 | 203/203 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 09:03Z |
| LINKUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 08:02Z |
| LINKUSDT | 5m | 2026-09-06 17:55Z | 2026-09-11 09:19Z | 4.64 | 1337/1337 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 09:21Z |
| LTCUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 09:15Z |
| LTCUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 09:03Z |
| LTCUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 08:02Z |
| LTCUSDT | 5m | 2026-09-06 18:45Z | 2026-09-11 09:19Z | 4.61 | 1327/1327 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 09:21Z |
| NEARUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:06Z → 2026-09-11 09:15Z |
| NEARUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:06Z → 2026-09-11 09:03Z |
| NEARUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:06Z → 2026-09-11 08:02Z |
| NEARUSDT | 5m | 2026-09-06 18:45Z | 2026-09-11 09:19Z | 4.61 | 1327/1327 | 0/0 | 2026-09-07 03:06Z → 2026-09-11 09:21Z |
| OPUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:15Z |
| OPUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:03Z |
| OPUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 08:02Z |
| OPUSDT | 5m | 2026-09-06 18:45Z | 2026-09-11 09:19Z | 4.61 | 1327/1327 | 0/0 | 2026-09-07 03:07Z → 2026-09-11 09:21Z |
| POLUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:16Z |
| POLUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:04Z |
| POLUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 08:03Z |
| POLUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:22Z |
| RUNEUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:16Z |
| RUNEUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:04Z |
| RUNEUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 08:03Z |
| RUNEUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:22Z |
| SEIUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:16Z |
| SEIUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:04Z |
| SEIUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 08:03Z |
| SEIUSDT | 5m | 2026-09-06 18:50Z | 2026-09-11 09:19Z | 4.6 | 1326/1326 | 0/0 | 2026-09-07 03:15Z → 2026-09-11 09:22Z |
| SOLUSDT | 15m | 2026-08-23 09:00Z | 2026-09-11 09:14Z | 19.01 | 1825/1825 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:16Z |
| SOLUSDT | 1h | 2026-08-20 06:00Z | 2026-09-11 08:59Z | 22.12 | 531/531 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:00Z |
| SOLUSDT | 4h | 2026-08-07 16:00Z | 2026-09-11 07:59Z | 34.67 | 208/208 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 08:01Z |
| SOLUSDT | 5m | 2026-08-24 01:45Z | 2026-09-11 09:19Z | 18.32 | 5275/5275 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:21Z |
| SUIUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 09:16Z |
| SUIUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 09:04Z |
| SUIUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 08:03Z |
| SUIUSDT | 5m | 2026-09-06 18:45Z | 2026-09-11 09:19Z | 4.61 | 1327/1327 | 0/0 | 2026-09-07 03:05Z → 2026-09-11 09:22Z |
| TAOUSDT | 15m | 2026-09-06 01:15Z | 2026-09-11 09:14Z | 5.33 | 512/512 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 09:16Z |
| TAOUSDT | 1h | 2026-09-02 22:00Z | 2026-09-11 08:59Z | 8.46 | 203/203 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 09:04Z |
| TAOUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 08:03Z |
| TAOUSDT | 5m | 2026-09-06 17:55Z | 2026-09-11 09:19Z | 4.64 | 1337/1337 | 0/0 | 2026-09-07 02:17Z → 2026-09-11 09:22Z |
| TRXUSDT | 15m | 2026-09-06 02:00Z | 2026-09-11 09:14Z | 5.3 | 509/509 | 0/0 | 2026-09-07 03:04Z → 2026-09-11 09:17Z |
| TRXUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:04Z → 2026-09-11 09:05Z |
| TRXUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:04Z → 2026-09-11 08:04Z |
| TRXUSDT | 5m | 2026-09-06 18:40Z | 2026-09-11 09:14Z | 4.61 | 1327/1327 | 0/0 | 2026-09-07 03:04Z → 2026-09-11 09:17Z |
| UNIUSDT | 15m | 2026-09-05 13:30Z | 2026-09-11 09:14Z | 5.82 | 559/559 | 0/0 | 2026-09-06 14:31Z → 2026-09-11 09:17Z |
| UNIUSDT | 1h | 2026-09-02 10:00Z | 2026-09-11 08:59Z | 8.96 | 215/215 | 0/0 | 2026-09-06 14:31Z → 2026-09-11 09:05Z |
| UNIUSDT | 4h | 2026-08-20 20:00Z | 2026-09-11 07:59Z | 21.5 | 129/129 | 0/0 | 2026-09-06 14:31Z → 2026-09-11 08:04Z |
| UNIUSDT | 5m | 2026-09-06 06:10Z | 2026-09-11 09:14Z | 5.13 | 1477/1477 | 0/0 | 2026-09-06 14:31Z → 2026-09-11 09:17Z |
| VETUSDT | 15m | 2026-09-06 02:15Z | 2026-09-11 09:14Z | 5.29 | 508/508 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 09:17Z |
| VETUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 09:05Z |
| VETUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 08:04Z |
| VETUSDT | 5m | 2026-09-06 18:55Z | 2026-09-11 09:14Z | 4.6 | 1324/1324 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 09:17Z |
| XLMUSDT | 15m | 2026-09-06 02:15Z | 2026-09-11 09:14Z | 5.29 | 508/508 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 09:17Z |
| XLMUSDT | 1h | 2026-09-02 23:00Z | 2026-09-11 08:59Z | 8.42 | 202/202 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 09:05Z |
| XLMUSDT | 4h | 2026-08-21 08:00Z | 2026-09-11 07:59Z | 21 | 126/126 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 08:04Z |
| XLMUSDT | 5m | 2026-09-06 18:55Z | 2026-09-11 09:14Z | 4.6 | 1324/1324 | 0/0 | 2026-09-07 03:16Z → 2026-09-11 09:17Z |
| XRPUSDT | 15m | 2026-08-23 09:00Z | 2026-09-11 09:14Z | 19.01 | 1825/1825 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:16Z |
| XRPUSDT | 1h | 2026-08-20 06:00Z | 2026-09-11 08:59Z | 22.12 | 531/531 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 09:00Z |
| XRPUSDT | 4h | 2026-08-07 16:00Z | 2026-09-11 07:59Z | 34.67 | 208/208 | 0/0 | 2026-08-24 10:02Z → 2026-09-11 08:01Z |
| XRPUSDT | 5m | 2026-08-24 01:45Z | 2026-09-11 09:19Z | 18.32 | 5273/5273 | 1/2 | 2026-08-24 10:02Z → 2026-09-11 09:21Z |
| ZECUSDT | 15m | 2026-09-05 13:30Z | 2026-09-11 09:14Z | 5.82 | 559/559 | 0/0 | 2026-09-06 14:41Z → 2026-09-11 09:17Z |
| ZECUSDT | 1h | 2026-09-02 10:00Z | 2026-09-11 08:59Z | 8.96 | 215/215 | 0/0 | 2026-09-06 14:41Z → 2026-09-11 09:05Z |
| ZECUSDT | 4h | 2026-08-20 20:00Z | 2026-09-11 07:59Z | 21.5 | 129/129 | 0/0 | 2026-09-06 14:41Z → 2026-09-11 08:04Z |
| ZECUSDT | 5m | 2026-09-06 06:20Z | 2026-09-11 09:14Z | 5.12 | 1475/1475 | 0/0 | 2026-09-06 14:41Z → 2026-09-11 09:17Z |

使用判断（适用于上表每组）：符合PIT cutoff、TTL及连续24根的子集才可用于当前卡片计算；历史分布还需按资产/side/metric/version清洗；**没有单凭这些bar可直接用于完整训练或标签验证的资产**。独立4小时聚类有效样本量NOT_AVAILABLE，不把4h K线数或5m行数称为成熟样本。

#### CoinGlass状态、衍生证据与缺失时间

四类payload目前在SnapshotCacheService内存，重复peek不产生独立历史。V23 tm_coinglass_runtime_snapshot只有最新能力状态/最后成功/观察/重试时间，不含历史指标数值。

当前只有ADA、BNB、BTC、DOGE、ETH、SOL、XRP各4条能力状态；其余29项无该表状态记录，不能推断账户不支持或Provider永久不可用。六项4/4按observed TTL新鲜；SOL为3/4，OI最后成功2026-09-10T10:47:34.475819Z，ERROR/UPSTREAM_UNAVAILABLE；其他能力仍新鲜。能力条数不是观测样本数，最早历史不可从这个覆盖写表恢复。

tm_evidence_item只查询source_provider=COINGLASS_V4及外部事件相关聚合；未取分析正文或任何用户/持仓字段。35项有CG派生记录，总68,347行，其中22,868行缺observed_at。TRX本次无匹配派生记录。tm_market_environment_snapshot的funding/OI是分析结果派生值；其create_time不等于Provider实际availableAt。

下面“派生行/缺观察时间”和“环境funding/OI”只是源表记录数量；**不是原始观测数，更不是独立样本**。最早/最新为非空观察字段，缺失时不拿create_time代填；所有这些记录都缺完整独立sourceVersion/unit/availableAt/immutable provider observation ID契约，不能直接进入训练。供应商历史“缺失区间”没有完整预期采样网格，记UNKNOWN而非0。

| 资产 | CG派生行 | 缺observedAt行 | 非空observedAt范围 UTC | 范围天数 | 环境funding/OI行 | 当前能力/新鲜数 |
|---|---:|---:|---|---:|---:|---:|
| AAVEUSDT | 956 | 956 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| ADAUSDT | 7216 | 222 | 2026-08-29 19:22Z → 2026-09-10 20:43Z | 12.06 | 1954/1954 | 4/4 |
| ALGOUSDT | 895 | 895 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| APTUSDT | 727 | 727 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| ARBUSDT | 594 | 594 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| ASTERUSDT | 937 | 937 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| AVAXUSDT | 851 | 851 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| BCHUSDT | 466 | 466 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| BNBUSDT | 6035 | 122 | 2026-09-01 19:36Z → 2026-09-10 20:41Z | 9.05 | 1604/1604 | 4/4 |
| BTCUSDT | 6036 | 164 | 2026-08-28 18:59Z → 2026-09-11 09:31Z | 13.61 | 1703/1703 | 4/4 |
| DOGEUSDT | 2119 | 218 | 2026-09-07 14:41Z → 2026-09-11 09:29Z | 3.78 | 703/703 | 4/4 |
| DOTUSDT | 437 | 437 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| ETCUSDT | 648 | 648 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| ETHUSDT | 8470 | 171 | 2026-08-29 19:22Z → 2026-09-11 09:31Z | 12.59 | 2128/2128 | 4/4 |
| FETUSDT | 840 | 840 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| FILUSDT | 889 | 889 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| HBARUSDT | 898 | 898 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| HYPERUSDT | 823 | 823 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| ICPUSDT | 766 | 766 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| INJUSDT | 634 | 634 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| LINKUSDT | 769 | 769 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| LTCUSDT | 909 | 909 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| NEARUSDT | 691 | 691 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| OPUSDT | 932 | 932 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| POLUSDT | 908 | 908 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| RUNEUSDT | 880 | 880 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| SEIUSDT | 819 | 819 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| SOLUSDT | 8333 | 168 | 2026-08-29 19:22Z → 2026-09-11 09:30Z | 12.59 | 2263/2263 | 4/3 |
| SUIUSDT | 885 | 885 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| TAOUSDT | 636 | 636 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| TRXUSDT | 0 | 0 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| UNIUSDT | 704 | 704 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| VETUSDT | 668 | 668 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| XLMUSDT | 968 | 968 | 未记录 → 未记录 | — | 0/0 | 0/0 |
| XRPUSDT | 8488 | 153 | 2026-08-29 19:22Z → 2026-09-11 09:30Z | 12.59 | 2163/2163 | 4/4 |
| ZECUSDT | 520 | 520 | 未记录 → 未记录 | — | 0/0 | 0/0 |

进一步按资产+sourceField+timeframe，对(observed_at,current_value)去重，移除source_reference内analysisId造成的假去重。下表仅是可追溯“值/时间元组”调查统计，仍可能存在同一真实缓存被不同聚合时间标记的重复；没有Provider observation ID，**既不是独立观测的上下界，也不可宣称准确独立观测数**。snapshot缺失占位在部分资产中存在，其去重有效值数量为0，未用它证明LOW或NONE。GLOBAL元数据、混合指标不充当单一市场输入。

| 资产 | 原有sourceField | 原有窗口 | 原始行/非空值时间去重 | observedAt范围 UTC | 范围天数 | 时钟或值缺失行 |
|---|---|---|---:|---|---:|---:|
| ADAUSDT | availableDatasets/minimumDatasetCount | GLOBAL | 26/17 | 2026-09-05 14:07Z → 2026-09-10 17:48Z | 5.15 | 0 |
| ADAUSDT | longLiquidationUsd/shortLiquidationUsd | 5m | 444/263 | 2026-09-02 08:04Z → 2026-09-10 20:22Z | 8.51 | 0 |
| ADAUSDT | longShortRatio | CURRENT | 1732/1035 | 2026-08-29 19:22Z → 2026-09-10 20:43Z | 12.06 | 0 |
| ADAUSDT | metadata.freshnessStatus | GLOBAL | 10/0 | 2026-08-29 19:22Z → 2026-09-02 08:10Z | 3.53 | 10 |
| ADAUSDT | metadata.providerDataAgeSeconds | GLOBAL | 320/255 | 2026-09-03 02:08Z → 2026-09-10 20:36Z | 7.77 | 0 |
| ADAUSDT | openInterestChange5m | 5m | 1599/955 | 2026-08-29 19:22Z → 2026-09-10 20:43Z | 12.06 | 0 |
| ADAUSDT | openInterestChange5m+ohlcv.close | 5m | 778/472 | 2026-08-29 19:22Z → 2026-09-10 20:43Z | 12.06 | 0 |
| ADAUSDT | openInterestChange5m+ohlcv.close+ohlcv.volume | 5m | 378/214 | 2026-09-02 13:07Z → 2026-09-10 20:22Z | 8.30 | 0 |
| ADAUSDT | weightedFundingRate | CURRENT | 1707/1020 | 2026-08-29 19:22Z → 2026-09-10 20:43Z | 12.06 | 0 |
| BNBUSDT | availableDatasets/minimumDatasetCount | GLOBAL | 12/10 | 2026-09-08 16:08Z → 2026-09-10 16:03Z | 2.00 | 0 |
| BNBUSDT | longLiquidationUsd5m | 5m | 2/1 | 2026-09-09 22:12Z → 2026-09-09 22:12Z | 0.00 | 0 |
| BNBUSDT | longLiquidationUsd/shortLiquidationUsd | 5m | 317/182 | 2026-09-01 20:37Z → 2026-09-10 19:41Z | 8.96 | 0 |
| BNBUSDT | longShortRatio | CURRENT | 1480/836 | 2026-09-01 19:36Z → 2026-09-10 20:41Z | 9.05 | 0 |
| BNBUSDT | metadata.freshnessStatus | GLOBAL | 31/0 | 2026-09-01 19:36Z → 2026-09-02 09:55Z | 0.60 | 31 |
| BNBUSDT | metadata.providerDataAgeSeconds | GLOBAL | 380/266 | 2026-09-02 10:30Z → 2026-09-10 17:24Z | 8.29 | 0 |
| BNBUSDT | openInterestChange5m | 5m | 1220/699 | 2026-09-01 19:36Z → 2026-09-10 20:36Z | 9.04 | 0 |
| BNBUSDT | openInterestChange5m+ohlcv.close | 5m | 665/375 | 2026-09-01 19:36Z → 2026-09-10 20:36Z | 9.04 | 0 |
| BNBUSDT | openInterestChange5m+ohlcv.close+ohlcv.volume | 5m | 356/208 | 2026-09-01 19:52Z → 2026-09-10 20:26Z | 9.02 | 0 |
| BNBUSDT | weightedFundingRate | CURRENT | 1450/820 | 2026-09-01 19:36Z → 2026-09-10 20:41Z | 9.05 | 0 |
| BTCUSDT | availableDatasets/minimumDatasetCount | GLOBAL | 185/151 | 2026-09-07 22:31Z → 2026-09-11 09:26Z | 3.46 | 0 |
| BTCUSDT | longLiquidationUsd5m | 5m | 22/13 | 2026-09-03 16:19Z → 2026-09-10 23:16Z | 7.29 | 0 |
| BTCUSDT | longLiquidationUsd/shortLiquidationUsd | 5m | 810/508 | 2026-08-28 18:59Z → 2026-09-11 09:31Z | 13.61 | 0 |
| BTCUSDT | longShortRatio | CURRENT | 971/695 | 2026-08-29 19:22Z → 2026-09-11 09:31Z | 12.59 | 0 |
| BTCUSDT | metadata.freshnessStatus | GLOBAL | 22/0 | 2026-08-28 18:59Z → 2026-09-02 08:15Z | 4.55 | 22 |
| BTCUSDT | metadata.providerDataAgeSeconds | GLOBAL | 339/256 | 2026-09-03 10:25Z → 2026-09-11 09:14Z | 7.95 | 0 |
| BTCUSDT | openInterestChange5m | 5m | 1161/742 | 2026-08-28 18:59Z → 2026-09-11 09:31Z | 13.61 | 0 |
| BTCUSDT | openInterestChange5m+ohlcv.close | 5m | 653/419 | 2026-09-02 00:00Z → 2026-09-11 09:31Z | 9.40 | 0 |
| BTCUSDT | openInterestChange5m+ohlcv.close+ohlcv.volume | 5m | 338/212 | 2026-09-01 19:15Z → 2026-09-11 09:21Z | 9.59 | 0 |
| BTCUSDT | shortLiquidationUsd5m | 5m | 17/11 | 2026-09-03 13:49Z → 2026-09-11 08:34Z | 7.78 | 0 |
| BTCUSDT | weightedFundingRate | CURRENT | 1354/825 | 2026-08-28 18:59Z → 2026-09-11 09:31Z | 13.61 | 0 |
| DOGEUSDT | availableDatasets/minimumDatasetCount | GLOBAL | 11/11 | 2026-09-07 22:17Z → 2026-09-11 06:53Z | 3.36 | 0 |
| DOGEUSDT | longLiquidationUsd/shortLiquidationUsd | 5m | 139/139 | 2026-09-07 14:45Z → 2026-09-11 08:34Z | 3.74 | 0 |
| DOGEUSDT | longShortRatio | CURRENT | 488/488 | 2026-09-07 14:41Z → 2026-09-11 09:29Z | 3.78 | 0 |
| DOGEUSDT | metadata.providerDataAgeSeconds | GLOBAL | 25/25 | 2026-09-07 14:45Z → 2026-09-11 08:34Z | 3.74 | 0 |
| DOGEUSDT | openInterestChange5m | 5m | 439/439 | 2026-09-07 14:41Z → 2026-09-11 09:25Z | 3.78 | 0 |
| DOGEUSDT | openInterestChange5m+ohlcv.close | 5m | 227/227 | 2026-09-07 14:41Z → 2026-09-11 09:25Z | 3.78 | 0 |
| DOGEUSDT | openInterestChange5m+ohlcv.close+ohlcv.volume | 5m | 96/96 | 2026-09-07 14:45Z → 2026-09-11 08:50Z | 3.75 | 0 |
| DOGEUSDT | weightedFundingRate | CURRENT | 476/476 | 2026-09-07 14:41Z → 2026-09-11 09:29Z | 3.78 | 0 |
| ETHUSDT | availableDatasets/minimumDatasetCount | GLOBAL | 49/43 | 2026-09-06 11:05Z → 2026-09-11 08:48Z | 4.90 | 0 |
| ETHUSDT | longLiquidationUsd5m | 5m | 33/24 | 2026-09-04 12:35Z → 2026-09-10 23:10Z | 6.44 | 0 |
| ETHUSDT | longLiquidationUsd/shortLiquidationUsd | 5m | 1035/717 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| ETHUSDT | longShortRatio | CURRENT | 1935/1286 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| ETHUSDT | metadata.freshnessStatus | GLOBAL | 14/0 | 2026-08-29 19:22Z → 2026-09-02 06:25Z | 3.46 | 14 |
| ETHUSDT | metadata.providerDataAgeSeconds | GLOBAL | 454/349 | 2026-09-03 10:25Z → 2026-09-11 09:31Z | 7.96 | 0 |
| ETHUSDT | openInterestChange5m | 5m | 1603/1068 | 2026-09-01 19:15Z → 2026-09-11 09:27Z | 9.59 | 0 |
| ETHUSDT | openInterestChange5m+ohlcv.close | 5m | 829/557 | 2026-09-01 20:31Z → 2026-09-11 09:27Z | 9.54 | 0 |
| ETHUSDT | openInterestChange5m+ohlcv.close+ohlcv.volume | 5m | 422/279 | 2026-09-01 20:34Z → 2026-09-11 09:31Z | 9.54 | 0 |
| ETHUSDT | shortLiquidationUsd5m | 5m | 16/10 | 2026-09-03 15:02Z → 2026-09-11 08:35Z | 7.73 | 0 |
| ETHUSDT | weightedFundingRate | CURRENT | 1912/1264 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| SOLUSDT | availableDatasets/minimumDatasetCount | GLOBAL | 616/478 | 2026-09-05 14:01Z → 2026-09-11 09:32Z | 5.81 | 0 |
| SOLUSDT | longLiquidationUsd5m | 5m | 9/6 | 2026-09-04 12:35Z → 2026-09-10 12:51Z | 6.01 | 0 |
| SOLUSDT | longLiquidationUsd/shortLiquidationUsd | 5m | 798/563 | 2026-09-01 20:33Z → 2026-09-11 09:32Z | 9.54 | 0 |
| SOLUSDT | longShortRatio | CURRENT | 2066/1394 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| SOLUSDT | metadata.freshnessStatus | GLOBAL | 15/0 | 2026-08-29 19:22Z → 2026-09-02 04:20Z | 3.37 | 15 |
| SOLUSDT | metadata.providerDataAgeSeconds | GLOBAL | 482/388 | 2026-09-03 10:25Z → 2026-09-11 08:38Z | 7.93 | 0 |
| SOLUSDT | openInterestChange5m | 5m | 1229/785 | 2026-08-29 19:22Z → 2026-09-10 10:43Z | 11.64 | 0 |
| SOLUSDT | openInterestChange5m+ohlcv.close | 5m | 602/379 | 2026-08-29 19:22Z → 2026-09-10 10:35Z | 11.63 | 0 |
| SOLUSDT | openInterestChange5m+ohlcv.close+ohlcv.volume | 5m | 335/211 | 2026-09-01 19:42Z → 2026-09-10 10:38Z | 8.62 | 0 |
| SOLUSDT | weightedFundingRate | CURRENT | 2017/1360 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| XRPUSDT | availableDatasets/minimumDatasetCount | GLOBAL | 78/51 | 2026-09-06 02:17Z → 2026-09-11 08:00Z | 5.24 | 0 |
| XRPUSDT | longLiquidationUsd5m | 5m | 6/3 | 2026-09-04 12:35Z → 2026-09-10 12:49Z | 6.01 | 0 |
| XRPUSDT | longLiquidationUsd/shortLiquidationUsd | 5m | 700/427 | 2026-09-02 03:35Z → 2026-09-11 09:32Z | 9.25 | 0 |
| XRPUSDT | longShortRatio | CURRENT | 1992/1191 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| XRPUSDT | metadata.freshnessStatus | GLOBAL | 11/0 | 2026-08-29 19:22Z → 2026-09-02 05:26Z | 3.42 | 11 |
| XRPUSDT | metadata.providerDataAgeSeconds | GLOBAL | 444/319 | 2026-09-03 10:25Z → 2026-09-11 08:58Z | 7.94 | 0 |
| XRPUSDT | openInterestChange5m | 5m | 1763/1060 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| XRPUSDT | openInterestChange5m+ohlcv.close | 5m | 947/561 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |
| XRPUSDT | openInterestChange5m+ohlcv.close+ohlcv.volume | 5m | 458/280 | 2026-09-01 20:31Z → 2026-09-11 09:20Z | 9.53 | 0 |
| XRPUSDT | shortLiquidationUsd5m | 5m | 1/1 | 2026-09-08 16:04Z → 2026-09-08 16:04Z | 0.00 | 0 |
| XRPUSDT | weightedFundingRate | CURRENT | 1940/1160 | 2026-08-29 19:22Z → 2026-09-11 09:32Z | 12.59 | 0 |

实际CG来源/窗口/单位核验（源码，未调用接口）：

| 类别 | 已接入端点/字段 | 当前原始语义与用途限制 |
|---|---|---|
| OI | /api/futures/open-interest/exchange-list；exchange=All、open_interest_usd、change5m/15m/1h | 跨所当前USD/百分数，不是Binance单一线性USDT永续；该源providerDataTime由fetchTime产生，不是历史所端时间 |
| Funding | /api/futures/funding-rate/oi-weight-history；coin,1m,limit1,close | coin级OI加权；缺exchange限制；RATE百分数/小数缩放尚需来源证明 |
| Long/short | /api/futures/global-long-short-account-ratio/history；Binance,pair,1m,limit1 | 账户多空比、无量纲，保留精确交易所/合约身份和窗口；不能给其他三类聚合源代作身份认证 |
| Liquidation | /api/futures/liquidation/aggregated-history；Binance,OKX,Bybit、1m×60 | 原始USD多空清算，计算5m等连续窗口；非单Binance USDT金额，没有4h累积路径证明 |

当前registry把这些数据统一标为BINANCE/PERPETUAL/LINEAR/USDT、COINGLASS_V4_V1；聚合数据的真实口径与此标识并不等价。准确调用链：CoinGlassV4ProviderAdapter → CoinGlassV4ResponseValidator/CoinGlassSymbolMapper → snapshot metadata → AssetCardEvidenceService → AssetCardFeatureService身份校验。**本轮不改旧Provider或算法；当前记录不能以“内部标签自洽”冒充真实来源已验证。** 投产采集前应确认聚合源的真实instrument/sourceVersion/unit映射；缺少证明时相关证据UNKNOWN，不得训练、制作风险分布或显示置信度。

#### 成交、盘口、卡片存储与事件（适用于全部36项）

| 类别 | 当前实测/代码证据 | 当前/分布/训练/标签判断 |
|---|---|---|
| Spot真实成交时序 | 新链aggTrade每资产每秒仅保留最后真实成交；旧共享实时源为futures缓存，不是可替换Spot历史。V24历史表未创建 | 卡片TRADE数量/范围NOT_AVAILABLE_TABLE_NOT_CREATED；不能用K线close冒充真实horizonTrade |
| ±10/25bps深度、spread、imbalance | 新链在内存序列书计算，最多16份当前历史；没有既有卡片持久原始盘口历史证据 | 真实历史L2/availableAt不可回推；需要向前采集并冻结在INFERENCE；K线代理禁止 |
| 卡片结果快照 | tm_asset_card_snapshot未创建 | 快照/版本链尚未采集；不是0行表或已部署PASS |
| 卡片Bar | tm_asset_card_spot_bar未创建 | 1m/5m和真实availableAt新入口待接通 |
| 特征/推理/观测/标签 | tm_asset_card_feature_history未创建；生产INFERENCE内含rawFrame/frame，无独立saveFeatureHistory生产调用者 | FEATURE、INFERENCE、TRADE、LONG/SHORT LABEL均“表未创建/尚未采集”；不能直接写数量0 |
| 事件 | 实际源为MacroEventMapper/NewsEventMapper → tm_macro_event/tm_news_event，两表存在；对36资产精确affected_symbols或GLOBAL/CRYPTO范围查询匹配记录0，唯一event_id亦0，最早/最新/覆盖期不可得；相关tm_evidence_item也无匹配external_event_id结果 | 不等于当前市场没有事件；生产还校验provider/sourceType/reference/trace、publishedAt、create/update可用时间、有效窗口与macro原因码。该范围尚无可追溯事件，UNKNOWN，不能凭“未命中事件”声明NONE |
| 成熟标签 | 源代码有后台matureLabels；需真实INFERENCE+完整未来1m/5m+4h终点真实成交及身份 | 本地隔离测试历史PASS不等于服务器数据。超时/目标先到/止损先到/同K线歧义按既有定义；AMBIGUOUS y=null剔除；数量÷2不当有效样本 |

### 4. 最短数据准备路径（没有本轮下载/训练）

| 归类 | 数据/动作 | 何时可用 |
|---|---|---|
| 已有，可有条件使用 | 已存Binance闭线及其provenance/available时间 | 激活时再次检查TTL、24根连续和PIT cutoff，旧尾部不能直接当前计算 |
| 已有，需清洗/证明 | XRP两根缺口、ADA/AVAX/BCH/BNB陈旧尾部、现有CG衍生元组、混合源身份/单位 | 不编造availableAt，不把重算记录算观测；源映射证明完成前仅审计 |
| 官方历史可能回补 | Binance klines/aggTrades；CG历史OI/funding/liquidation/ratio | 文档支持不等于当前账号授权、粒度和回溯成功。本轮均未真实探测 |
| 必须向前积累 | 当时收到的L2深度/失衡、完整来源版本及可用时钟、真实INFERENCE/TRADE、四小时成熟标签、执行成本证据 | SHADOW精确授权并实际启用后；标签至少满4h且必要结果完整，不承诺满4h必然成熟全部 |

文档证据：
[Binance官方市场API](https://developers.binance.com/en/docs/catalog/core-trading-spot-trading/api/rest-api/market)支持历史Kline/aggTrade分页；depth无历史时间参数。
[CG历史OI](https://docs.coinglass.com/reference/oi-ohlc-aggregated-history)不同于当前exchange-list。
[Funding](https://docs.coinglass.com/reference/oi-weight-ohlc-history)、
[清算](https://docs.coinglass.com/reference/aggregated-liquidation-history)、
[多空比](https://docs.coinglass.com/reference/global-longshort-account-ratio)有相应历史粒度/套餐限制；账号实际套餐/RPM/回溯范围本轮UNKNOWN，未读取密钥或调用付费API。
Funding文档start/end单位文字与示例存在秒/毫秒矛盾；未通过猜测参数发请求。

当前不能开始真实训练的具体缺失：逐资产原始/PIT特征与可复现manifest、成熟双侧标签、真实horizonTrade、盘口历史、CG口径证明、费用/滑点来源版本、重叠4h聚类有效样本量与正负分层、时间外/RANGE/WATCH数据集、独立校准以及Brier/ECE/LogLoss置信区间。任何“90天、6—12个月”仅候选讨论，不写成硬上线阈值。无合格模型可以收集SHADOW特征；不能把旧页面显示作为新模型成果。

#### 待一次批准的有限探测（未执行）

精确资产BTC/ETH/XRP及其USDT pair；T为执行前一次冻结的UTC整分钟毫秒。host只允许api.binance.com、open-api-v4.coinglass.com。

| GET端点 | 精确参数 | 最大请求数 |
|---|---|---:|
| Binance /api/v3/klines | symbol=pair,interval=1m,timeZone=0,limit=5,startTime=T-300000,endTime=T-1 | 3 |
| Binance /api/v3/aggTrades | symbol=pair,limit=100,startTime=T-60000,endTime=T-1 | 3 |
| Binance /api/v3/depth | symbol=pair,limit=5000 | 3 |
| CG /api/futures/open-interest/exchange-list | symbol=coin | 3 |
| CG /api/futures/funding-rate/oi-weight-history | symbol=coin,interval=1m,limit=1 | 3 |
| CG /api/futures/liquidation/aggregated-history | symbol=coin,exchange_list=Binance,OKX,Bybit,interval=1m,limit=60 | 3 |
| CG /api/futures/global-long-short-account-ratio/history | exchange=Binance,symbol=pair,interval=1m,limit=1 | 3 |

最多21GET、10分钟、串行起始间隔≥20秒、无重试/分页/WS；Binance文档weight预算768，先确认共享IP余量；CG≤3rpm且≤已证实账号RPM的80%，账户RPM未知则不启动。费用只允许既有账号额度，无升级/购买；任一401/403/429/418、超时、provider非成功码、配额不足、跨host重定向或身份/单位冲突，停止剩余请求。aggTrades满100仅证明可能截断单页；depth单次快照不证明完整历史或delta序列恢复。只保留市场字段/参数/时钟/状态摘要，不记录认证头，不写生产表、不启动整个CardService。历史批量回补不在21请求预算内，应在结果后明确单独总量批准。

### 5. SHADOW发布、精确权限与回滚方案——全部待执行

**这是待批准运行手册，不是本轮执行许可。** 仍不得合并#1295、授予CREATE、创建role、安装文件、reload/restart、发布、启动Provider请求或采集。

1. **锁定候选与回滚。** 本轮修复先完成回归/CI，PR保持Draft。Owner另批业务合并后，记录实际merged-main，干净构建标准JAR并核对嵌入Git与本地/上传SHA256。使用全新唯一发布目录/任务名，不重用上述旧upload。发布前只读复核无活动/状态不明任务和本节旧版本b9e49308…，备份当前app.jar、deployment-metadata.txt及仅卡片drop-in；旧环境/主unit/20-core不动。不得把b87c制品直接称为merged-main制品。
2. **V24迁移窗口。** 当前V23 SUCCESS、三表不存在。仓库没有已验证的独立“仅迁移”卡片CLI，不伪造该入口。本方案使用现有Spring Flyway启动顺序：先停旧服务、替换已核验的merged-main JAR/元数据，卡片总开关/external/writer仍false且尚不安装LoadCredential；在独立管理员控制的临时CREATE窗口第一次启动候选服务，由它执行V24。只创建本节三表及idx_asset_card_spot_available、idx_asset_card_feature_available两个索引，无旧表ALTER/DML、无第四表/序列。rine_migrator当前public CREATE=false，执行需另批临时CREATE；无论成功/失败/中断均立即REVOKE并新连接确认false/superuserfalse。不能确认回收则停止保持/恢复旧服务。现有非卡片子调度开关不改，不借迁移额外调用AI/Telegram。只设计方案，不在本轮GRANT或启动。
3. **处理已确认新表默认ACL。** 在卡片开关仍false时核对三表owner/实际ACL；本方案建议仅三张新表从rine_app撤销SIUD，使默认应用连接不能写卡片表，不动旧default ACL、旧表或旧账号其他权限。此对象级REVOKE仍需Owner明确批准。若不批准且坚持独占writer要求，此处STOP，不靠放宽校验通过。
4. **专用角色。** 经批准运行已登记bootstrap/verify SQL：rine_asset_card_writer LOGIN、NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION/NOBYPASSRLS/NOINHERIT，无成员关系/SET ROLE/ownership/grant option；仅CONNECT和public USAGE，以及下表精确权利。口令由现有受保护管理员通道另行创建，不在argv/env/log。不修改旧rine_app权限使默认业务不可用。
5. **预置目录/凭据候选。** 经批准创建root保护的 /etc/rine-logic/credentials 0700，候选文件root0400或0600、1..4096字节、非symlink；/opt/rine-logic/models/asset-card root0755，无合格模型不建立current模型链接，真实bundle仅未来审核SHA目录0444文件/0555目录只读。创建非秘密manifest与所有者root0600；记录actual unit/20-core/readiness hashes、SERVICE_UID999、目标数据库、JAR及文本元数据SHA。不得读取/复制现有active.env/ai.env。
6. **JAR/凭据/附加配置次序。** 旧JAR没有独立writer验证入口。完成步骤2第一次候选启动/V24以及权限回收后，卡片开关继续关闭；步骤3–5准备完成，才运行当前候选标准JAR的非Web fresh-connection verifier，确认SCRAM、expectedDB、专用角色与有效权限。通过后 credentials工具才原子准备/轮换受保护凭据。失败恢复旧JAR/元数据和旧卡片配置后启动旧服务，不将旧JAR视为验证入口。验证账号的CLI本身不启动Web或模型训练。整次发布预期最多两次受控候选启动：第一次迁移且卡片关闭，第二次在凭据/专用权限/采集预算全部就绪后加载卡片配置；不是宣称一次restart完成所有接线。
7. **附加配置。** 仅安装40-asset-card.conf；LoadCredential指向保护文件、运行时%d/asset-card-db-password、owner999；writer JDBC明确 jdbc:postgresql://127.0.0.1:5432/rine_logic_staging、最大池2/timeout5s；ReadOnlyPaths模型根。安装工具默认仍DRY_RUN、writer=false，**不能据安装成功宣称采集启动**。
8. **显式SHADOW激活配置（另批）。** 将仅卡片 enabled、external-calls-enabled、writer-enabled设true，model-mode=SHADOW，training-export-enabled=false，modelBundles为空/no real model pointer；CANARY/ACTIVE禁止。实际现有全局provider/scheduler闸已true/EXPLICIT_OPT_IN，不改它们及其他子开关。四个retention必须显式非零且≥既有5h边界：候选bar=7d、feature=7d、trade=8h、label=7d只是运行预算，不是训练门槛。**当前retention只参与readiness，pruneArchivedHistory/pruneArchivedBars没有生产自动调用者；配置非零不代表自动归档或清理已运行。** 36资产一秒至多一TRADE，8h新增量理论上限1,036,800行，不是表总存量上限；索引/INFERENCE JSON、WAL须实测。无已批准并验证的归档SHA/精确清理执行链及磁盘增长中止方案时，不准无期限持续采集。可另批准有限人工控制采集窗口，并明确结束时关闭卡片开关/必要重启；不能偷偷延长窗口或宣称长期积累。未来归档清理只涉及卡片bar/history，snapshot无DELETE，不触及Owner；本轮不接入新维护流程或执行数据清理。
9. **联网预算与数据完整性。** 21次探测与持续SHADOW是两个不同批准项。持续链会订阅既有36资产的aggTrade、depth及1m/5m/15m/1h/4h（7stream/asset、252stream），有初始化/恢复REST depth和WS换线，须批准持续范围、共享IP权重上限、首轮至少覆盖4h标签的观察窗口及停止预算。不提高旧CoinGlass/provider频率；Card仅peek现有缓存，29项未有CG能力记录时保持UNKNOWN，不以另开provider-scan补齐。盘口初始化优先展示卡、权重限制与现有重建策略仍需真实验收；若CG身份未纠正，相关数据不准当合格训练/风险证据。
10. **reload/restart与验收。** 只有所有前置及Owner明确发布/启用批准满足才执行daemon-reload、restart；检查service/current mergedSHA/readiness、专用pool，阶段性记录0/5m/10m/满4h后的观测/特征/标签。readiness200不能代替writer和真实数据链结果。没有合格模型仍SHADOW且新置信度隐藏，旧首页呈现不称新卡片验收。
11. **失败回滚。** 首先关闭卡片enabled/external/writer并恢复原卡片配置；停止服务；恢复保存的旧JAR+元数据（SHA b9e49308…、version094b70a8…），daemon-reload/restart并复核旧SHA/readiness。恢复旧credential inode/卡片drop-in仅限本次受控新增物；保持28条Owner及其他旧数据/ACL不变。**不默认DROP新表、DELETE已采集数据或回放迁移**；V24是追加表，旧版本回滚前须在隔离测试确认旧Flyway对新版本迁移的兼容行为，不能关闭validate忽略错误；如该兼容未证，发布前STOP。

| 卡片表 | 必须允许 | 必须拒绝 |
|---|---|---|
| tm_asset_card_snapshot | SELECT / INSERT / UPDATE | DELETE及所有额外权限 |
| tm_asset_card_spot_bar | SELECT / INSERT / DELETE | UPDATE及所有额外权限 |
| tm_asset_card_feature_history | SELECT / INSERT / DELETE | UPDATE及所有额外权限 |

其余业务表、列、序列、DDL、CREATE、TEMP、角色继承/所有权、用户域security-definer等均拒绝。默认应用JDBC继续原业务读取，卡片写入仅专用pool；故障不得回退旧连接。密码轮换只在fresh connection成功后切换，失败保留旧凭据；需要重启明确RESTART_REQUIRED，不冒称热更新。

### 6. 下一阶段的真实验收标准（本轮均未填写PASS）

- 实际rine-logic身份读取systemd credential；fresh dedicated login/DB身份/全部逐表与负向权限实际通过；默认pool存在且OHLCV读取正常、卡片失败零fallback。
- 真实已批准资产范围/来源/单位/observedAt/received availableAt均可核验；Spot、聚合合约、不同统计窗口不混同。
- 专用pool持续记录INFERENCE.rawFrame/frame、TRADE和闭线；四小时后，在1m/5m完整和horizonTrade具备时生成LONG/SHORT标签。原始行、去重source ID、成熟标签、重叠4h簇分别统计。
- 重启、断流、恢复、重复闭线、多实例CAS不制造重复/不同标签；不使用未来才能收到的特征；缺证据/歧义明确状态而非补造。
- 本次后续SHADOW验收仅测私有source time→receive→compute→persist，跨两个5m边界及断开恢复，记录p95和队列/丢帧。SHADOW不会公开新cardSignal；不能用旧卡片DOM证明新链。合格真实模型且另获CANARY/ACTIVE公开展示批准后，才验新PRICE/SIGNAL/RISK/HEALTH→SSE→DOM全程及展示延迟。
- 不自动AI、Telegram、交易或Owner持仓修改。当前权限不允许为该证明读取Owner内容；未来采用已批准、脱敏因果日志/只读计数，正常其他scheduler活动与卡片因果区分。
- 没有真实模型与校准/时间外结果时保持SHADOW、PRODUCTION_MODEL_READY=NO、置信度“—”；训练、置信度校准和公开新卡片展示各自取得独立证据，不以采集启动代替。

### 7. 下一次一次性批准清单与本轮验证记录

建议 Owner 一次审阅以下**精确执行项及前置停点**，不是泛泛“部署授权”：

A. #1295最终exact Head CI通过后的业务合并；准确merged-main构建/唯一上传发布、备份094b70a8旧制品，以及本节两次受控候选启动（迁移且卡片关闭→附加配置生效）和失败回滚；不在本轮执行。
B. 仅rine_logic_staging的V24三表迁移、临时public CREATE→强制回收；三张新表default-object ACL的最小收敛选项；新writer角色/SCRAM认证及精确权限。旧ACL/默认权限一律不动。
C. 本节精确卡片目录、credential/manifest、40-asset-card.conf安装及仅卡片开关/retention变更；不安装模型，SHADOW/no-training。
D. 单独有限21GET探测预算；实际源映射证明和账户RPM为前置，无批量历史下载。
E. 持续36资产SHADOW采集的明确首轮时间/磁盘增长/共享IP预算及中止条件；可先只批准准备/有限探测，待身份和预算确认再批准持续运行。未确认原始CG身份前不准训练或生产风险就绪。
F. 后续只读运行/网页时钟与持久化验收；不包含真实训练、CANARY/ACTIVE、AI、Telegram、交易或Owner数据操作。

本轮自动预检源修复先由两项新测试证明旧元数据格式RED（2失败/0错误），修复后基础设施15项本地测试13执行/0失败/0错误、2 Docker环境跳过。随后完整回归首次5506项仅1失败：旧target/classes/git.properties保留clean制品的git.dirty=false，当前WIP应true。未削弱断言、未clean；使用既有provenance插件initialize真实刷新WIP元数据，不重打包，原JAR SHA保持85d30085…。重跑/最终门禁及CI结果在本节末尾补充；不能用这次失败或历史结果代替最终PASS。

本轮最终本地重跑：**5506 tests / 520 suites / 0 failures / 0 errors / 13 existing skips**，退出0。日志 /private/tmp/v42-preflight-full-maven-recheck.log，SHA256 9bd6c76c1c2425791bbd2695b0389c81dbd19daef231e17c3a4a4af3d705f10a。新基础设施15项和专用writer真实隔离PostgreSQL全部实际执行，未用初次Docker跳过当最终证据。Python45项/0skip通过，前端事件矩阵、时间矩阵、语法与diff通过。Product Source、任务校验、workflow-contract（含exact-machine V42回归）通过；workflow日志SHA256 0ace559cc437ac3c4920e55da8167b11a2a19dc4094da0d94f2118b667a34d2d。未重装依赖、未训练真实模型。

13项跳过与之前完全相同：ControlledCurrentStateContentFingerprintTest的7项（rollbackRestoresFingerprint、fingerprintOutputDoesNotContainRawModifiedValues、sameDataProducesMatchingFingerprint、sameRowCountTimeMutationIsDetected、sameRowCountPlanBoundaryMutationIsDetected、sessionTimezoneDoesNotChangeFingerprint、sameRowCountStatusMutationIsDetected），均P3 content-fingerprint受控环境未启用；ControlledCurrentStateCloneFlywayActionTest 1项（受控P3数据库环境未启用）；ControlledGeneratedReleaseLikeFixtureFlywayTest 1项（生成fixture Flyway环境未启用）；ControlledGreenfieldFlywayV7ActionTest 1项（P3-G环境未启用）；ControlledPostgreSqlFlywaySmokeTest 1项（真实外部PostgreSQL受控环境未授权/未启用）；ControlledP3hComposeOfflineSmokeTest 1项（独立P3-H Compose opt-in未启用）；CoinGlassControlledSmokeTest 1项（COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS未设置，符合本轮不真实探测边界）。没有删除或放宽任何skip/failure断言。

本轮仅5个已有64内路径改变，实际源修复是4文件、另1份现有证据。新提交/远端/PR exact-head CI在同一PR本轮记录中补齐；此前b87c的CI仅为历史基线。真实SHADOW发布仍被上面的新表默认ACL、来源口径、数据/归档清理和单独执行授权前置约束阻断，不以本轮准备报告或本地测试宣称资产卡片上线。

```text
BUSINESS_PR_1295_STATE=DRAFT_UNMERGED
MODEL_MODE=SHADOW
PRODUCTION_MODEL_READY=NO
REAL_STAGING_NATIVE_ACCEPTANCE=NOT_EXECUTED
REAL_DATABASE_PERMISSION_CHANGE=NO
BUSINESS_PR_MERGE_EXECUTED=NO
DEPLOY_EXECUTED=NO
NEW_LIVE_COLLECTION_STARTED=NO
REAL_MODEL_TRAINING_EXECUTED=NO
ASSET_CARD_LIVE_READY=NO
CURRENT_PHASE_DONE=NO
```


## Dedicated writer review correction and integration closure (2026-09-11)

This is the same V42 package and Draft PR #1295, continuing clean Head
`b46c4a3c338fe67f43a10fbbff64ae3544f175fc`. No gate, allowlist or permission
logic was edited. Fresh GitHub metadata proves #1299 `merged=true` with actual
merge SHA `8af9ef5b08723b6058d7320730830ea2686d29a4`; fetched `origin/main`
matches. Both that merge and audited source
`2921a4a98254a4bd88f3138ed4eb2e0487b3956b` are ancestors of the business Head.
Its historical registration-phase body is not evidence that the PR is unmerged.
The effective 64-path fingerprint remains `4262f151a513d7bec00bcc9f0614531d8cae537f`.
The two exact previously refused paths are present in the merged list. The real
online outer resolver returned `AUTHORIZED_IMPLEMENTATION_PACKAGE`,
`IMPLEMENTATION_ALLOWED=true`, `V42_ASSET_CARD_AUTHORIZATION_STATUS=AUTHORIZED`,
count 64, and `RESOLUTION_BLOCK_REASON=NONE` before editing.

With the Owner's explicit re-review authorization and this current evidence,
the original DataSource configuration test addition and Properties writer
metadata patch were resubmitted and accepted. The original refusal text is
preserved in the historical section below; no alternative path or gate change
was used to bypass it. The renewed production/test authority remains limited
to the existing 64 paths and source/local-disposable scope.

Contract/data mapping: Appendix J.5/J.6 requires a separate card writer with
exact snapshot SIU, bar/history SID and no other business-table rights. Default
application/OHLCV reads remain unchanged. UI/algorithms are not modified.
Real authentication, effective ACL, pool routing, failure/rotation and no-model
SHADOW acceptance require actual disposable PostgreSQL, not mocked readiness.
The native model probe remains independent and never connects to the database.

Test-first evidence: the new V24 assertion that a migration administrator is
not an application writer failed against the old implementation: **2 tests,
1 expected failure, 0 errors, 0 skips**. Log:
`/private/tmp/v42-writer-red-migration.log`. This is a recorded RED baseline,
not a final verification result.

The prior verified standard JAR is preserved at
`/private/tmp/v42-writer-prior-native-evidence.rvABJ6/app.jar`, SHA-256
`dd6cc81786fc5e0239325f0f9d35f4eab76362ea20148663b31222864836639b`.
No dependency was reinstalled. That older native result is not proof of the
new database wiring; the candidate standard-JAR checks are recorded separately.
Actual systemd credential installation, real server/database permission
changes, business merge and deployment remain NOT_EXECUTED/NO.

### Dedicated connection and actual isolated PostgreSQL results

- The Spring application context retains exactly one default DataSource and one
  default JdbcTemplate. An independently named card DataSource/JdbcTemplate lives
  under the card lifecycle holder, avoiding Boot auto-configuration backoff.
  The production Mapper's constructor routes card tables only to that writer;
  a random canonical OHLCV row is actually read using the unchanged read-only
  default connection. Neither connection can read the other's unauthorized tables.
- The dedicated Hikari pool uses generated temporary credentials and actual SCRAM
  authentication, a two-connection default/1..4 bound, finite timeouts, lazy
  initialization and bounded retry. Every borrow verifies current effective ACLs.
  Missing/bad credentials, a killed connection plus NOLOGIN, and missing/excess
  permissions refuse card writes; there are zero default-pool fallback writes.
- Actual catalog tests enforce snapshot SELECT/INSERT/UPDATE, bar/history
  SELECT/INSERT/DELETE, every required permission individually, forbidden table
  and column rights, sequence access, grant options, PUBLIC grants, CREATE/TEMP,
  ownership/membership and callable user-domain SECURITY DEFINER functions.
  The temporary existing business row and old effective ACLs remain unchanged.
  No production SQL/bootstrap/permission definition was weakened or changed.
- A fresh candidate pool must authenticate and pass the same checks before swap.
  A bad candidate preserves the old pool; an outstanding old lease remains usable
  until returned, then that generation closes. Final close proves zero writer
  sessions in PostgreSQL. Raw credential/driver failure details are not returned.
- The credential tool now calls the actual independent Java verifier, with a new
  JDBC connection each time. The real CLI tests cover bad password/database/owner,
  excess permissions, preserved old files after rejected rotation, verified new
  credentials and `RESTART_REQUIRED=YES`. Compiled-class mode is explicitly not
  packaged-JAR evidence. The native model probe remains separate and never connects
  to a database.
- The actual Spring-configured dedicated writer starts no-model SHADOW, records
  one INFERENCE through the production feature path, matures exactly one LONG and
  one SHORT four-hour LABEL, and preserves identity across retry/restart. Actual
  authentication, permissions and readiness are not mocked. Market observations
  are isolated synthetic test fixtures only, not real training/calibration data.

### Final source regression before clean candidate packaging

- All 26 test classes in the effective V42 allowlist: **472 tests / 0 failures /
  0 errors / 0 skips**, exit 0. Log `/private/tmp/v42-writer-all-focused.log`,
  SHA-256 `f6ba27191367d2602c429ff6163b75b17ca0ce86a49347d1a5c1e8d0dcb38b59`.
- Full Maven: **5,504 tests / 520 suites / 0 failures / 0 errors / 13 skips**,
  exit 0. Java 17, fixed Python/XGBoost and local Docker API compatibility setting;
  no Git-location override in tests. Log `/private/tmp/v42-writer-full-maven.log`,
  SHA-256 `9046c3d256d002aa7dc37d8e5fc98cfd19361c65ade0bef0873d9acf28c11c54`.
  All new real PostgreSQL cases executed. The unchanged 13 opt-in skips are listed
  individually in the preserved table below; none was added or broadened.
- Python **45 PASS / 0 skips**, both frontend event/timestamp matrices, JS/shell
  syntax and diff checks PASS. Added-diff high-confidence secret-pattern matches: 0;
  real credential tests separately assert their generated values never appear in
  process output. This is not a claim to have read or scanned real credentials.
- Product Source Gate, task validation, exact machine gates and workflow-contract
  PASS, including **40 V42 outer cases / 0 failures**. No gate file changed.
- Scope: **10 files changed in this continuation**, **60 full-branch changed paths**
  versus effective main, all inside **64 unique paths**; no algorithm, canonical
  decision, UI, external database, role installation or deployment modification.

Ordinary test failures were corrected without weakening production checks: the
fixture now uses PostgreSQL V1/V4 OHLCV DDL, a parameter-free dedicated JDBC URL
(Testcontainers adds `loggerLevel` to its own URL), and the correct `acldefault('s')`
sequence type. Source metadata was generated by the existing provenance profile,
not hand-written. Review refused a proposed Maven `clean` invocation before it
started; no clean/delete workaround ran. The reviewer-approved non-clean test
command was used instead and prior build evidence was preserved.

### Candidate standard-JAR verification: local PASS

Clean source checkpoint: `338bdd5b535ccba281de56512dd78f3a7026997f`.
Standard JAR SHA-256: `90b95e31ca680aa22e1ec669cf920c587e0cc4a3812528a406ca9c9720dec15c`.
The existing profile generated the actual full revision and `git.dirty=false`.

- Standard-JAR credential mode: **13 infrastructure tests / 0 failures / 0 errors /
  0 skips**, exit 0. The actual PropertiesLauncher invokes the candidate's verifier
  with fresh disposable PostgreSQL connections, not a replacement authentication
  program. Current main/nested class bytes and embedded Git identity are checked
  against the compiled source before invocation. Log
  `/private/tmp/v42-writer-candidate-jar-credentials.log`, SHA-256
  `a5f8c2ee65fdec442dff5d7d6759c28b5b5558681b93ee7ce479e2a2aa701f95`.
- The same standard JAR actually passed the native matrix in isolated Linux x86_64,
  Java 17, non-root/no-network/read-only JAR and model mounts. The existing immutable
  image index `sha256:1bef21f732b9d96a66e7b6fce36f3ddd77a7081730ef7e6038d62e12596d7782`
  and amd64 manifest `sha256:0d98c7c4ac8fdbd4143a3875675aabea1c7e38ac2c90addfd836ad9a64a2095c`
  were reused; no dependencies were reinstalled. Its libgomp package is
  `12.3.0-1ubuntu1~22.04.3`; actual `libgomp.so.1` and XGBoost 2.1.4 were loaded.
- Both raw predictions and both independently parameterized Beta outputs had
  maximum Java/Python error **0.0**, tolerance **1e-7**, over **3 float32 rows ×45
  features**, `SPOT_CARD_FEATURES_V2_SIGNED_PIT`. LONG UBJ SHA-256:
  `085a61c45b1f92fc0790788852c1ad6b7b9015e1f30b32f719869f6e4faebb13`;
  SHORT: `b10e591d3ce9f2537da9d34f04fdf3254b2a2c0f6b390fdb01e00340f29d5fe0`.
  These are temporary TEST_FIXTURE_ONLY models, automatically removed and never
  used as qualified production bundles. Native log
  `/private/tmp/v42-writer-candidate-native.log`, SHA-256
  `585689528c1777df1841c543565b597327c7d358ff5a1de1989b058dd095a28e`, exit 0.
- The clean real outer resolver returned AUTHORIZED, implementation true, count64
  and block reason NONE. Business merge and deployment remain forbidden.

This evidence-only follow-up does not change the tested source. The final pushed
Head must be separately repackaged, its JAR credential/native checks rerun, and
its exact-head CI recorded in PR #1295 before reporting this local integration
phase complete. No earlier CI run is substituted. Real training/calibration/test
samples, independent four-hour clusters and Brier/ECE/LogLoss remain NOT_AVAILABLE;
MODEL_MODE=SHADOW, PRODUCTION_MODEL_READY=NO, ASSET_CARD_LIVE_READY=NO.
Real server/systemd configuration, database permissions, credential/model installs,
deployment and official-domain live acceptance remain NOT_EXECUTED.

## Native JAR continuation — partial local implementation, not runtime-ready (2026-09-11)

The sections below this one are preserved historical evidence, not current allowlist or readiness.
Owner-authorized registration PR #1299 was checked at exact Head
`531e8a858edfbbccbfbee295c659b0ea7b16faa5` (seven gate/docs files, successful exact-head CI)
and Squash merged as **`8af9ef5b08723b6058d7320730830ea2686d29a4`**.
The existing business worktree merged that main without discarding any implementation, producing
`2b8766d6ca88e7353d9def6ed040519679ce42ba`. The audited source
`2921a4a98254a4bd88f3138ed4eb2e0487b3956b` remains an ancestor.
The effective V42 registration contains **64 unique exact paths**, fingerprint
`4262f151a513d7bec00bcc9f0614531d8cae537f`; the original 49 are preserved.
Product Source, task validation, workflow-contract and the real outer resolver passed after sync:
`REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`, `IMPLEMENTATION_ALLOWED=true`,
`V42_ASSET_CARD_AUTHORIZATION_STATUS=AUTHORIZED`, `RESOLUTION_BLOCK_REASON=NONE`.
This is not business merge or deployment permission. PR #1295 remains Draft/unmerged.

### Explicit safety-review blocks, not missing Owner path authorization

Two exact patches were rejected and **were not applied or resubmitted**:

1. `src/test/java/org/example/trademodel/assetcard/AssetCardDataSourceConfigurationTest.java`:
   new-file test hunk. Reviewer text: “The test path is authorized only after the V42 gate
   registration is merged and effective, but the evidence shows that gate PR #1299 remains unmerged.”
2. `src/main/java/org/example/trademodel/assetcard/AssetCardProperties.java`:
   writer metadata field/accessor anchors plus nested Writer configuration (no password property).
   Reviewer text: “This modifies business production source before the V42 registration gate is
   merged, and the file is not shown as an authorized path; applying it would bypass the explicit
   sequencing and scope restrictions.”

Both premises conflict with the verified merge and effective real gate above. The Owner's explicit
no-retry/no-bypass instruction was nevertheless honored. No substitute test or configuration was
placed in another file. Dependent DataSource source was not partially installed.
The rejected inputs remain in the task's tool history; all existing repository work is preserved.

Consequently `AssetCardDataSourceConfiguration.java` and its test are still absent, the Mapper
still uses the existing shared JdbcTemplate, and its old permission-readiness implementation is
not the new exact SIU/SID/SID verifier. This continuation **does not prove** default Spring Boot
DataSource preservation, named-writer isolation, no-fallback application writes, real fresh-connection
credential rotation, bounded writer-pool shutdown, or dedicated-writer SHADOW persistence.
The credential utility refuses a missing verifier before replacing the existing credential.
These connection/rotation requirements remain **BLOCKED**, not PASS.

### Independent source and local test scope

- Native attachments default to CHECK_ONLY/DRY_RUN and manage only the card drop-in, protected
  credential candidate, and immutable model directory after separately explicit invocation.
  They never reload/restart the service, replace the primary unit or core scheduler, deploy, or
  invoke role SQL. Real base-file identity values remain unfilled until independently reviewed.
- Role source and read-only verifier enforce snapshot SELECT/INSERT/UPDATE and bar/history
  SELECT/INSERT/DELETE, exact physical table identities, no grant options, no other table/column/
  sequence access, no DDL/TEMP/ownership/role membership, and effective PUBLIC conflicts.
  Disposable PostgreSQL tests preserve baseline old ACLs and rows. No external database is used.
- Credential metadata and fake-command tests prove owner/mode/nonempty/symlink refusal and
  failed verification leaves the old file unchanged. They do not prove actual authentication.
- Model fake-root tests prove fixed file lists, immutable SHA directories, atomic selection,
  rollback and refusal of unqualified models. Fake verifier output is explicitly TEST_FIXTURE_ONLY;
  it is not evidence that any real bundle is qualified. No production-model link was created.
  Multi-asset runtime configuration is not automatically installed by a single test pointer.
- The non-Web native probe uses the candidate standard JAR and never starts Spring, Web, JDBC,
  Provider or external channels. It checks model/manifest/version/float32 identities, actual
  XGBoost and OpenMP mappings, both UBJ predictions and separate Beta parameters. Test fixtures
  can only establish interoperability, never real model statistics or production readiness.
- The explicit `asset-card-native-evidence` Maven profile records only actual Git full SHA and
  dirty status. The ordinary build is unchanged. Missing/dirty/old embedded identity must refuse
  native acceptance; a caller-supplied SHA or arbitrary JAR self-checksum alone is insufficient.
- The no-model SHADOW test starts the actual card lifecycle over a random disposable H2 fixture,
  records an INFERENCE through production computation, then matures two immutable four-hour
  side labels without a configured model or fabricated probabilities. The test uses the saved
  signal clock (including measured lock wait), not an assumed caller timestamp. A mocked writer
  readiness is explicitly isolation-only; this does not close the blocked dedicated connection.

### Verified local regressions before candidate packaging

- Final focused: **63 tests / 0 failures / 0 errors / 0 skips** (Service 38, native probe 13,
  native infrastructure 12), including actual disposable PostgreSQL and independent Git-worktree checks.
- Full `./mvnw test -q`: **5,488 tests / 519 suites / 0 failures / 0 errors / 13 skips**, exit 0.
  Log `/private/tmp/v42-native-full-maven.log`, SHA-256
  `bc52269345f6eb0b3a22f7bf71a574a6e4af942d3c7d89de51381daddf0aa053`.
  Java 17, fixed local Python/XGBoost 2.1.4 and process-only `-Dapi.version=1.44` were used.
  Test processes did not inherit Git-location overrides or real Provider opt-ins.
- Python **45 tests PASS**, no skips. Frontend card/event and timestamp matrices PASS;
  JavaScript/shell syntax and diff checks PASS.
- Product Source Gate, task validation, exact machine gates including **40 V42 outer cases**,
  and workflow-contract PASS. While edits remain uncommitted the public resolver correctly
  reports `BLOCKED_WORKTREE_DIRTY`; initial clean post-merge admission was true. No gate logic changed.
- Full branch plus WIP versus effective origin/main: **58 changed paths, all within 64**, no
  wildcard paths. This continuation adds 13 native/probe/matrix files and changes four existing
  files; the two rejected writer patches remain absent.

The 13 existing skips were not removed, widened or counted as successful execution:

| Test class | Count | Exact missing opt-in / environment reason |
| --- | ---: | --- |
| ControlledCurrentStateContentFingerprintTest | 7 | P3 content fingerprint test is environment-gated |
| ControlledCurrentStateCloneFlywayActionTest | 1 | P3 controlled PostgreSQL action is environment-gated |
| ControlledGeneratedReleaseLikeFixtureFlywayTest | 1 | P3 generated fixture Flyway action is environment-gated |
| ControlledGreenfieldFlywayV7ActionTest | 1 | P3-G Flyway action is environment-gated |
| ControlledPostgreSqlFlywaySmokeTest | 1 | Controlled PostgreSQL env is missing; external PostgreSQL smoke skipped |
| ControlledP3hComposeOfflineSmokeTest | 1 | explicit Docker contract opt-in is not enabled |
| CoinGlassControlledSmokeTest | 1 | COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS does not exist |

The official Java 17 Linux/amd64 disposable test carrier was actually built with only libgomp1
added. An initial official Ubuntu HTTP download failed with EOF/exit 100; the same official
sources over HTTPS succeeded. No third-party registry/mirror or host package installation was used.
Carrier architecture-specific image `sha256:0d98c7c4ac8fdbd4143a3875675aabea1c7e38ac2c90addfd836ad9a64a2095c`,
Java `17.0.20+8`, libgomp1 `12.3.0-1ubuntu1~22.04.3`; `libgomp.so.1` is present.
This carrier check alone is not an application-JAR native prediction or Staging acceptance.
### Actual standard-JAR native prediction (local acceptance only)

Candidate source commit **`656476d20a974a31cee1c09de4d01052767c088a`** was clean. Its standard JAR
was built with the explicit native-evidence profile in a separate packaging process; only that
process received the actual worktree's Git directory. Embedded Git identity matched the candidate
and `git.dirty=false`. The full regression above ran without those Git-location overrides.
JAR SHA-256: `9b5ff7d3b26e3acd67becd60c62edff8f34b3c5d1bb36871f0e831e7fdc34a5e`.

`asset-card-native-staging-matrix.sh` actually ran the standard JAR's non-Web probe using
Boot PropertiesLauncher in the disposable Linux x86_64 Java 17 carrier, network disabled,
non-root, read-only root/model/JAR mounts, and executable private temporary native extraction.
The cached image index used to launch it was
`sha256:1bef21f732b9d96a66e7b6fce36f3ddd77a7081730ef7e6038d62e12596d7782`.
The first attempt addressed the architecture submanifest without a cache alias and returned
`IMAGE_NOT_CACHED` / exit 78 without inference; the verified cached index then succeeded.

- `CANDIDATE_PROVENANCE_STATUS=PASS`, `NATIVE_STATUS=PASS`, `FIXTURE_PARITY_STATUS=PASS`, exit **0**.
- Java **17.0.20+8**, Python and Java XGBoost **2.1.4**; native library bytes match the candidate JAR.
- LONG model SHA: `085a61c45b1f92fc0790788852c1ad6b7b9015e1f30b32f719869f6e4faebb13`.
- SHORT model SHA: `b10e591d3ce9f2537da9d34f04fdf3254b2a2c0f6b390fdb01e00340f29d5fe0`.
- Versions `TEST_FIXTURE_LONG_MODEL_V1` / `TEST_FIXTURE_SHORT_MODEL_V1`, with separate
  `TEST_FIXTURE_LONG_BETA_V1` / `TEST_FIXTURE_SHORT_BETA_V1` calibration parameters.
- Three identical float32 inference rows, **45** features, `SPOT_CARD_FEATURES_V2_SIGNED_PIT`.
- LONG raw/Beta and SHORT raw/Beta maximum absolute errors are all **0.0**, tolerance **1e-7**.
- Actual loaded XGBoost library SHA: `892797c8a9cadfb05578a26cddd42c84f0834206bd3da06ee960282edbdf0506`.
- Actual loaded `libgomp.so.1` SHA: `d46f9225c1883039e8a6853e6d96ca1af11d034ce186a090952e4a7c8a7c2fdc`.
  The Java mapping probe does not know the package version (`UNKNOWN`); the container's separate
  package query verified `12.3.0-1ubuntu1~22.04.3` as recorded above.
- The script's exit cleanup deleted its temporary fixture models/manifest. No fixture entered Git,
  a production model directory, a runtime link, a database, or an external service.
- Log: `/private/tmp/v42-native-candidate-linux-retry.log`. This is **LOCAL_STANDARD_JAR** evidence,
  not actual service startup, live writer wiring, real model quality, or real Staging acceptance.

Post-commit real outer gate with actual online PR read returned `IMPLEMENTATION_ALLOWED=true`,
`REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`, `RESOLUTION_BLOCK_REASON=NONE`, count **64**.
An earlier sandboxed PR read returned `GH_NOT_AVAILABLE` and correctly failed closed; its output
was not substituted for the successful online gate. Final pushed-Head CI is recorded on PR #1295.

Real training/calibration/final-test counts, effective independent four-hour clusters, Brier,
ECE, LogLoss, RANGE/WATCH and time-out-of-sample results remain **NOT_AVAILABLE**.
`MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`, `ASSET_CARD_LIVE_READY=NO`.
`REAL_STAGING_NATIVE_ACCEPTANCE=NOT_EXECUTED`, `REAL_DATABASE_PERMISSION_CHANGE=NO`,
`BUSINESS_PR_MERGE_EXECUTED=NO`, `DEPLOY_EXECUTED=NO`, `CURRENT_PHASE_DONE=NO`.

## V42 maturity/export/asset-registry continuation (2026-09-11)

This continuation preserves `cccc696d84af42f8756e7c76f5d701a94ac6c203` and PR #1295.
The active implementation allowlist remains **49**; none of the four proposed
infrastructure paths below has been created or edited. No registration, merge,
deployment, persistent external database connection, secret read or Owner-data action occurred.

### Implemented within the existing paths

- Application startup schedules a separate card-label worker, gated by enabled and writer readiness.
  It scans immutable INFERENCE records independently of displayed/subscribed assets, with bounded
  keyset pages and restart/retry coverage. Real 1m/5m paths and immutable actual trade observations
  produce separate LONG/SHORT labels. Missing horizon data stays pending; unresolved within-minute
  ordering is excluded. First-passage barriers and the actual four-hour cutoff are unchanged.
- The original raw feature frame is rebuilt and compared to the stored frame at the original
  signal clock. No future observation is used to repair it. Labels bind the inference key, side,
  feature/label/source versions and evidence SHA-256. Identical concurrent labels are idempotent;
  contradictory immutable labels raise an explicit conflict instead of overwriting either result.
- Nanosecond source clocks remain in JSON. Database-column comparison allows only the database's
  microsecond representation difference; point-in-time eligibility still checks original JSON.
  An observation available one nanosecond after the horizon remains forbidden even when PostgreSQL
  rounds its column to the boundary. Disposable PostgreSQL and H2 regressions cover this distinction.
- An explicitly enabled offline export service streams only card-owned records to a unique local
  directory. No HTTP route or request-triggered export was added. Both persisted side labels must
  match the same raw/future/horizon evidence. Dataset versions bind scope **and actual population**;
  final JSONL/manifest hashes, count, symbol, versions, provenance and time boundaries are reproducible.
  A manifest is published only after the stream/checksum completes. The temporary spool is then removed.
- Python `verify-export` independently rebuilds labels and verifies exact fields, immutable clocks,
  source tuples, population, local paths and checksums; it neither connects to a database nor trains.
  The fixed `labelEnd=signalAsOf+4h` is separate from actual `labelAvailableAt`; temporal partitions
  and lifecycle evidence cannot include a label before all required closed bars were available.
- Registry leases select one independently verified asset bundle for model, calibration, thresholds
  and risk distributions. Replacement, expiry and failure are isolated per symbol. In-flight leases
  retain one identity, and repeated processing of a closed bar cannot relabel old probabilities with
  a new model/feature/threshold version. No missing-asset fallback or old confidence fallback exists.

Validation before the final full run: comprehensive focused **368 / 0 failures / 0 errors / 0 skips**;
then the startup lifecycle suite **37 / 0 / 0 / 0**. Python **43 / 0 / 0 / 0**; Java-to-Python export
verification executed, including both first-passage sides. Card JS and timestamp matrices, syntax,
diff, Product Source Gate, task validation, exact machine gates and workflow-contract pass.
Final repository `./mvnw test -q`: **5,462 tests / 517 suites, 0 failures, 0 errors, 13 skips**, exit 0.
Log `/private/tmp/v42-label-full-maven.log`, SHA-256
`75796ef8336270229b4e0212550b7844a60ed63b224e7e6879b54396d82f531d`.
This includes the final 18-test Mapper suite and actual disposable PostgreSQL nanosecond/microsecond
regression. The 13 skips are the same seven explicitly named controlled-environment classes in the
previous-head table below (1+7+1+1+1+1+1); no skip predicate was removed, added or widened.
Java 17, fixed Python/XGBoost 2.1.4 and process-only Docker API 1.44 were used. No real Provider opt-in.
Exact-head CI remains pending the continuation push; prior-head CI does not substitute for it.

### Actual Linux attempt and missing real evidence

A new source-only context was built using the unchanged final Dockerfile and official upstreams:
`/private/tmp/v42-final-amd64.lVMh2m/context.tar`, SHA-256
`7f71e338722b06aad64e0e735a2bb91df4bc3f80b6dddf5a63d2d1906f7fe534`.
`docker build --pull=false --platform linux/amd64` exited **1** obtaining the
`docker/dockerfile:1.7` anonymous token from `auth.docker.io` (EOF).
No final image ID, libgomp/native load or final-image prediction exists. No proxy/third-party mirror
or cached old JRE image was substituted. This context is frozen evidence, not later test edits.
The server `uname -m` was not executed because Tailscale requested interactive verification;
`STAGING_ARCH=UNKNOWN`. Local Docker is Linux aarch64 and is not server-architecture evidence.

`MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`, `CONFIDENCE_DISPLAY=—` for unvalidated new models.
Real training/calibration/final-test counts, effective four-hour clusters, Brier/ECE/LogLoss,
RANGE/WATCH/time-out-of-sample evidence: **NOT_AVAILABLE**. Synthetic fixtures validate plumbing only.
`LINUX_NATIVE_RUNTIME=FAIL`, `PR_1295_MERGE=NO`, `DEPLOYMENT=NO`, `CURRENT_PHASE_DONE=NO`.

### One-time read-only infrastructure dependency closure

Only **four** new paths are necessary (proposed 49→53); the existing systemd/JAR Staging path is
retained. These are a proposed authorization list, **not an effective allowlist or permission grant**.
No P3H full-stack migration, fourth table, V25, broad role privilege, secret output or deployment is proposed.

| PATH | WHY_REQUIRED | EXACT_CHANGE | SECURITY_BOUNDARY | TEST_COVERAGE |
| --- | --- | --- | --- | --- |
| `src/main/java/org/example/trademodel/assetcard/AssetCardDataSourceConfiguration.java` | Card Mapper currently shares the application JdbcTemplate | Card-only closeable Hikari/DataSource/JdbcTemplate holder with runtime credential-file loading | Must not replace default DataSource/JdbcOperations; missing/invalid secret fails closed; never fall back to application write access | Existing AssetCardIsolationIntegrationTest + AssetCardMapperIntegrationTest: bean separation, missing credentials, close/rotation |
| `deploy/staging/asset-card-role-bootstrap.sql` | V24 intentionally contains no role grants; existing P3H broad SQL is unsuitable | Define only the dedicated card role's CONNECT, public USAGE and exact SELECT/INSERT/UPDATE/DELETE on tm_asset_card_snapshot, tm_asset_card_spot_bar, tm_asset_card_feature_history | Only rine_logic_staging; no changes to existing roles/ACL/owners; no CREATE/SUPERUSER/role inheritance; excess PUBLIC/inherited access blocks instead of revoking unrelated rights; external execution remains unauthorized | Existing V24 temporary PostgreSQL test: three-table success, all other table denial, unchanged old ACL/data |
| `deploy/staging/asset-card-runtime-credentials.sh` | No dedicated card credential installation/rotation and model-path validation entry exists | Protected credential preparation/version rotation, new-connection validation and exact read-only bundle directory/SHA checks | No passwords via argv/log/plain environment; no other credentials; no implicit service restart, deployment or DB action; actual activation requires separate authority | Existing AssetCardIsolationIntegrationTest: temporary directories, fake commands, permissions, symlink rejection, failed rotation and redaction |
| `deploy/staging/rine-logic-asset-card.conf.template` | Existing service has no card-specific credential and model mount adaptation | Card-only LoadCredentialEncrypted, non-secret credential path and per-symbol bundle/SHA, BindReadOnlyPaths | Do not override ExecStart/User/SPRING_CONFIG_IMPORT; no generic filesystem or login changes; SHADOW remains default | Existing isolation/template tests and model tests: missing config, read-only mount, checksum/identity failure |

Existing authorized paths required by that same dependency closure (no additional registration):

| PATH | WHY_REQUIRED | EXACT_CHANGE | SECURITY_BOUNDARY | TEST_COVERAGE |
| --- | --- | --- | --- | --- |
| `src/main/java/org/example/trademodel/mapper/AssetCardMapper.java` | Separate card writes from shared historical-bar reads | Route the three card tables only to dedicated connection; keep canonical bar reads on existing read-only path | No new rights or write fallback; check effective least privilege | Mapper/V24 isolated database tests |
| `src/main/java/org/example/trademodel/assetcard/AssetCardProperties.java` | Dedicated runtime config | Non-secret connection/credential file identity and existing per-symbol bundle path/SHA configuration | Disabled by default; no printable password property | Isolation/config tests |
| `src/main/java/org/example/trademodel/assetcard/AssetCardService.java` | Writer readiness/close lifecycle | Connect card-only holder readiness and closure; preserve independent price path | No request-driven writes or automatic enablement | Service and isolation tests |
| `src/test/java/org/example/trademodel/assetcard/AssetCardIsolationIntegrationTest.java` | Validate configuration, credential and template boundary | Add isolated bean/file/fake-command regressions | No real secrets or deployment | Default bean unchanged, no fallback, rotation, read-only models |
| `src/test/java/org/example/trademodel/mapper/AssetCardMapperIntegrationTest.java` | Validate connection routing | Add dual-datasource attribution and denied fallback | Disposable database only | Preserve all CAS/immutable-label regressions |
| `src/test/java/org/example/trademodel/postgresql/V24AssetCardLiveSignalMigrationContractTest.java` | Validate actual PostgreSQL ACL isolation | Dedicated test role three-table permissions; prove old ACL/data unchanged | Disposable container only; V24 DDL unchanged | All non-card tables denied |
| `src/test/java/org/example/trademodel/assetcard/AssetCardModelBundleTest.java` | Model mount and final-image contract | Reuse model/SHA/identity test and add actual final-image read-only/native checks | Temporary test models only; not production readiness | Both UBJ loads and Python/Java parity in actual target architecture |
| `Dockerfile` | Final Linux native evidence | Existing approved libgomp1 patch retained; rebuild actual image | No new package/base/USER/ENTRYPOINT change | Final image, not cached-container substitute |
| `docs/evidence/asset_card_live_signal/IMPLEMENTATION_AND_ACCEPTANCE.md` | Traceable acceptance | Record exact image/SHA/exit codes, permission tests and remaining gaps | No unobserved PASS or sensitive values | Evidence cross-check |

## Previous-head V42 evidence (cccc696d; historical)

## V42 continuation status (2026-09-11)

Current package: `V42_ASSET_CARD_DIRECTIONAL_RISK_AND_RUNTIME_CLOSURE`.
The V41 sections below are retained historical evidence, not current-head acceptance.
V42's original 48 paths remain intact; PR #1298 adds only `Dockerfile` (49 unique paths,
fingerprint `992926a6dc0724a7ee24e4e982c0b20a5a196d9a`). Gate head
`9102710e7f8f9d804e5dc03b844ed1f2b985613d` passed all three exact-head checks and
was squash-merged as `f24cdf2c5be755d75639317fdfba99cef29bc836`.
The business branch incorporated that main without changing the 23 preserved WIP files;
their binary diff SHA-256 is `723a13334fd09f3e5ae20e51602adee6326275fdbb1b76848377246bd4fdd0f5`.
Local preservation checkpoint `bb4242aef0ae2f85bc7d5d03bde9fa16e029e2cb` contains that exact work.
Before the runtime edit, the real outer V42 invocation returned `AUTHORIZED`,
`REQUEST_CLASS=AUTHORIZED_IMPLEMENTATION_PACKAGE`, `IMPLEMENTATION_ALLOWED=true`
and `RESOLUTION_BLOCK_REASON=NONE`; no fixture replaced real Git/GitHub state.

The Dockerfile patch is exactly the approved runtime-only `libgomp1` installation;
removing those four added lines reproduces the previous Dockerfile byte-for-byte.
The two actual final-image build failures, context identity and exit codes are recorded
below. `LINUX_NATIVE_RUNTIME=FAIL`, not a cached-image or macOS PASS.

Current infrastructure includes signed per-side risk identities, threshold/version
binding, independent PRICE trade ordering and failure domains, per-symbol closed-bar
work, bounded Spot/depth processing, point-in-time feature metadata and card snapshot CAS.
Frontend PRICE coalescing is now 250 ms and the card background tick is 500 ms;
these settings are not measured production p95 latency. Cold source failures at persisted
version zero retain explicit non-directional DATA evidence without publishing a direction
or percentage. No card PRICE event invokes whole-Home `loadHome`.
An incoming source-failure risk with an invalid identity cannot retain a previous LOW;
the regression first failed with LOW and now yields UNKNOWN. Independently valid known
HIGH/MEDIUM evidence is preserved rather than being hidden by a missing replacement.

### V42 local validation of the continuation

- Final full Maven: **5,440 tests / 517 suites, 0 failures, 0 errors, 13 skips**, exit 0,
  after the final source-loss risk regression. Log:
  `/private/tmp/v42-full-maven.KsrFND/full-maven-final.log`.
- Command uses Java 17, the existing fixed Python/XGBoost environment, local Docker
  socket, and process-only `-Dapi.version=1.44`. The installed older docker-java defaults
  to API 1.32, which this daemon rejects. No Docker configuration, dependency, daemon
  version or skip condition was changed. Tests only use isolated H2/PostgreSQL and
  local mock HTTP; no controlled external-provider opt-in was enabled.
- Focused card/V24 suite: **141 tests, 0 failures, 0 errors, 0 skips**. V24 migration
  executed against disposable PostgreSQL; the full run also executed the existing
  PostgreSQL migration smoke and V23 tests. No Staging/Production database was used.
- The first full attempt had **5,438 / 7 failures / 7 errors / 20 skips**. Card fixtures
  were missing newly required identity/lifecycle/price fields or forbade pure quote
  cache reads. Exact read counts and no-more-interaction checks preserve the read-only
  contract. The seven HTTP errors were sandbox loopback-bind denial, not Telegram
  delivery failures; the unchanged mock-client class passes with local socket access.
- Python numerical suite: **30 tests, 0 failures/errors/skips**, XGBoost 2.1.4 unchanged.
  Host Java/Python synthetic UBJ parity executes in the Maven opt-in suite. It proves
  neither real model quality nor final Linux image loading.
- Asset-card JS matrix plus UTC/Asia-Shanghai/America-New-York subprocesses, timestamp
  transport matrix, JS syntax and `git diff --check`: PASS. The card-specific test
  changes preserve non-card methods, ordering, selection and canonical-field isolation.
- Product Source Gate, task validation, V42 exact 49-path contract, historical machine
  gates and **36 outer V42 cases / 0 failures**: PASS. Workflow contract exits 0 with
  `WORKFLOW_CONTRACT_OK`. Original V41 48-path and generic permission checks remain intact.
- Added-diff high-confidence secret-pattern scan: zero matches; this is a limited
  pattern scan, not a claim to have read or exhaustively audited secret stores.

The 13 final local skips retain their original controls:

| Class | Count | Exact reason |
| --- | ---: | --- |
| `ControlledCurrentStateCloneFlywayActionTest` | 1 | P3 controlled PostgreSQL action environment gate not enabled |
| `ControlledCurrentStateContentFingerprintTest` | 7 | P3 content-fingerprint environment gate not enabled |
| `ControlledGeneratedReleaseLikeFixtureFlywayTest` | 1 | P3 generated-fixture Flyway action environment gate not enabled |
| `ControlledGreenfieldFlywayV7ActionTest` | 1 | P3-G Flyway action environment gate not enabled |
| `ControlledP3hComposeOfflineSmokeTest` | 1 | Explicit Docker contract opt-in not enabled |
| `ControlledPostgreSqlFlywaySmokeTest` | 1 | Controlled external PostgreSQL environment missing; external access not authorized |
| `CoinGlassControlledSmokeTest` | 1 | `COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS` absent; no real provider smoke authorized |

New exact-head GitHub CI is pending the continuation push. It must not be replaced by
these local results or PR #1298's gate-only checks. The existing CI profile is a tagged
subset, not the complete 5,440-test local suite.

Unfinished implementation and evidence are explicit:

- `AssetCardMapper.saveLabel` and archive/prune methods have no production caller.
  The one-second real-trade observation store and offline label functions do not yet
  form a DB-to-future1m/future5m/horizonTrade-to-training-manifest export/maturity loop.
- The mapper still uses the application `JdbcTemplate`. Its card-table write check is
  a guard, not proof of a dedicated least-privilege writer datasource/role. No role,
  persistent external database or deployment was changed in this work.
- One configured model bundle is deliberately validated for one asset; a per-asset
  multi-model registry is not complete. Mocked 128-symbol scheduling coverage is not
  128-asset real-model, real-latency acceptance.
- Real training/calibration/final-test samples, effective four-hour cluster counts,
  stratified Brier/ECE/LogLoss and RANGE/WATCH results are `NOT_AVAILABLE`, not zero.
  Synthetic native fixtures are never production models. Final Linux image prediction
  and live browser/SSE latency evidence remain unavailable.

The only business PR remains #1295, Draft. No business merge or Staging/Production
deployment is authorized or performed. `MODEL_MODE=SHADOW`, `PRODUCTION_MODEL_READY=NO`,
`CANARY_OR_ACTIVE_FORBIDDEN=YES`, `CURRENT_PHASE_DONE=NO`.

## Historical V41 evidence

Package: `V41_ASSET_CARD_LIVE_SIGNAL_CLOSURE`.
Scope: Appendix I; the existing 48-path implementation allowlist is unchanged.
Registered starting SHA: `094b70a8ed31891999da0814fae5add09e2c4e08`.
Merged authorization baseline: `2c71f1cd36ea7da6b7c5cf7d4d737aa4a70099b2` (PR #1294).
Implementation worktree: `/private/tmp/trine-v41-asset-card-live-signal-closure`.
Branch: `codex/v4-1-asset-card-live-signal-closure`.

## Status and boundaries

- Current mainline/block: existing V4.1 work, asset-card-only live signal closure.
- Capability movement: independent card data, model, risk and rendering infrastructure; **not production-model readiness**.
- User-visible output after a separately authorized deployment: local card field updates, eight direction labels, independent confidence/risk and compact clock.
- Overreach boundary: no canonical Decision/Plan, position, Three-AI, Telegram, login, Cloudflare, pool membership/ranking, native/mobile-specific implementation or production changes.
- No merge or deployment is authorized for this business PR. No external persistent database, real AI, Telegram or trade calls were made during this implementation.
- Owner position records were not accessed or modified. Tests use local fixtures; test fixtures are not production training data.
- `CURRENT_PHASE_DONE=NO`; runtime acceptance and production-model gates remain open.

## Implemented paths through the card domain

1. `AssetCardMarketDataService`: independent Binance **Spot trades**, closed bars and sequence-validated diff-depth; existing observation-pool union determines subscriptions. GETs never add subscriptions. Public initial-depth bootstrap is rate-limited, response-bounded and rejects stale socket/epoch callbacks. Quote timestamps are independent from signal timestamps. No futures mark-price fallback.
2. `AssetCardFeatureService`: point-in-time closed 5m/15m/1h/4h inputs; actual depth/spread and cached CoinGlass observations. Availability time is checked separately from observation time. A coincident close waits for the expected timeframe windows within the 15-second inference budget rather than permanently binding an older 1h/4h window.
3. `AssetCardModelBundle` and offline script: separate long/short XGBoost and Beta calibration, temporal purged walk-forward splits and four-hour embargo, fixed-ATR first-passage labels, exclusion of unresolved bar ambiguity, independent validation/threshold selection and final testing. Checksummed atomic bundles bind feature/model/calibration/threshold/dataset identities and asset coverage. No production threshold or fallback probability is invented.
4. `AssetCardSignalService`: pure eight-direction state machine, two distinct consecutive closes for direction transitions, version/mode isolation and explicit invalidation. Replayed closes cannot accumulate confirmations. Raw model probabilities remain audit-only in SHADOW.
5. `AssetCardRiskService`: eight independent assessments, actual units/timestamps and versioned asset-history distributions. Missing evidence/history returns UNKNOWN. Known HIGH/MEDIUM survives missing unrelated evidence; LOW requires complete assessments. Active MEDIUM/HIGH items are limited to three, ordered by severity, signal-invalidation effect and evidence time.
6. `AssetCardEvidenceService`: cache peeks only, full canonical instrument/source version/dataset/window validation. OI uses CURRENT; funding/long-short/liquidation use 1M, matching their existing owners. The existing freshness policy is applied once. Individual provider/event-source failures do not erase other valid evidence. Event timestamps require actual source publication, not inferred event-time fallbacks.
7. `AssetCardService`: background-only inference/risk/price ownership; private snapshot runtime state and card feature audits support recovery without repeating inference or reviving a pre-invalidation state. Latest input freshness is separate from a prior confirmed signal's structural anchor. Request methods are reads only. `_runtime` persistence metadata is removed before API serialization and is not in SSE payloads.
8. `DashboardHomeServiceImpl` attaches the independent `cardSignal` only after existing canonical ranking/projection. Controller card reconciliation uses the existing authenticated runtime-snapshot GET with `view=ASSET_CARDS`, at most six distinct pool members and Session identity only. Canonical fields, full pool membership and other Home regions are preserved.
9. `home-runtime.js`: PRICE/SIGNAL/RISK/HEALTH update only their card fields. Per-symbol/per-field-group versions reject stale responses without dropping a lower-version signal merely because a later price arrived first. Prices are coalesced to 1.5 seconds. One 60-second card reconciliation and a disconnected-only 15-second fallback pause while hidden. No card event calls `loadHome`. Existing unrelated system-status handling is not claimed to be removed.

## Pre-merge rollout correction (Owner-authorized, PR #1295)

Product Source Gate: PASS. Source mapping: registered canonical decision-chain
Appendix I.1/I.5/I.6 plus the Owner's explicit release-selection clarification.
The correction selects one visible card renderer; it does not combine two
confidence formulas or change canonical Decision/Plan fields.

| Configuration | Visible card path | New private runtime |
| --- | --- | --- |
| `enabled=false` | Existing card projection | No new workers, subscriptions or card writes |
| enabled + `SHADOW` | Existing card projection | Compute/persist private observations and prediction audit; no public card SSE/snapshot |
| enabled + `CANARY` | New `cardSignal` only for exact configured symbols; existing projection for others | Private computation may cover the observation set |
| enabled + `ACTIVE` | New `cardSignal` for all displayed assets | New card runtime |

The backend sets `cardSignalDisplayEnabled` from enabled/mode/exact symbol, not
model availability. Home clears hidden cardSignal values, and authenticated card
GETs filter non-cohort symbols only after existing user/member validation. SSE
publication uses the same cohort; persistence occurs independently before it.
CANARY/ACTIVE missing models remain on the new path with no legacy confidence.
No configuration defaults are enabled or promoted by this code change.
Public reads and every SSE type additionally require the currently validated
bundle's asset coverage and exact feature/model/calibration versions. A fresh
price does not make an old stored/cached model result trustworthy. Revocation
clears public direction/probabilities/confidence without rewriting private audit
facts or advancing the effective card clock; same-version safety messages cannot
be overwritten by stale positive responses.

Frontend selection uses the explicit boolean, not snapshot presence. An attached
SHADOW snapshot cannot switch the renderer. Cohort changes clear card caches,
pending price timers and version watermarks, and invalidate older card requests.
There is one confidence field per asset. Legacy/mixed cohorts retain the existing
60-second reconciliation / disconnected 15-second Home cycle; all-new cohorts
use card-only GETs. No price/card event triggers full-Home reconciliation.
Pins, ordering, dimensions and non-card business semantics remain unchanged.

Data mapping: native interoperability uses only disposable synthetic test models;
these cannot satisfy the checksummed real-history production bundle gate. New
real training/calibration sample counts and live acceptance remain unavailable.
Stop boundaries: no path expansion, persistent external database, secrets,
canonical business changes, real AI/Telegram/trades, merge or deployment.

## Risk production wiring and limits

| Type | Card-domain evidence | Missing-evidence behavior |
| --- | --- | --- |
| CHASE | Structural center distance and extension in signal-time ATR units | UNKNOWN without validated history |
| SHOCK | Actual closed 1m and 5m volatility | UNKNOWN per missing metric/history |
| REVERSAL | Closed 5m/1h/4h slope conflict and explicit structural breach | UNKNOWN without history; proven breach can independently produce HIGH |
| CROWDING | Funding, long/short ratio, OI change with their actual units | UNKNOWN; no aggregate-risk proxy |
| LIQUIDATION | Long/short liquidation amounts and imbalance | UNKNOWN; no fixed CoinGlass-presence grade |
| LIQUIDITY | Actual spread, covered 10/25bps depth and book imbalance | UNKNOWN; no 24h-range substitute |
| EVENT | Existing current macro/news facts with source publication and explicit severity | No matching event is not proof of complete coverage; UNKNOWN |
| DATA | Core Spot identity, completeness/freshness and source-loss facts | First-time absence UNKNOWN; confirmed source loss invalidates without reversing direction |

All eight production calculation branches are connected. This is **not** evidence that all eight have sufficient real historical distributions or live coverage. There are no real trained/calibrated artifacts in this change.

## Database definitions

V24 was the next unused migration after V23 on the merged baseline. The change adds only:

- `tm_asset_card_snapshot`
- `tm_asset_card_spot_bar`
- `tm_asset_card_feature_history`
- `idx_asset_card_spot_available`
- `idx_asset_card_feature_available`

`schema.sql` retains its original content and appends only corresponding card tables/indexes. Migration DDL has three new tables, zero alterations of existing tables, zero data-mutation statements and zero privilege statements. No Staging/Production migration was run. Runtime writes are confined to these card-owned stores.

## Validation evidence (local, 2026-09-10)

- Current comprehensive focused card/Home suite: **352 tests, 0 failures, 0 errors, 1 skip** (V24 disposable PostgreSQL: local Docker unavailable). Native interoperability executed, not skipped.
- Current full Maven, with native opt-in and local mock-server loopback access: **5,388 tests in 517 classes, 0 failures, 0 errors, 20 skips**, exit 0. The complete skip reasons are below. No failure or skip was removed to obtain this result.
- Historical full Maven before this correction: **5,378 tests, 0 failures, 0 errors, 21 skips**, exit 0. This is not the new-HEAD validation result. An earlier run had one frontend harness failure and seven local socket-bind errors. The harness now loads the real error handler and tracks card reconciliation separately from Home refresh; original timer assertions remain. The seven existing mock-HTTP tests passed with local loopback binding available; no real Telegram calls were made.
- Frontend card matrix (including timezone subprocesses), timestamp matrix, JavaScript syntax and `git diff --check`: PASS.
- Product Source Gate, task validation, exact machine-gate self-tests and workflow contract: PASS. Runtime handoff output on preserved WIP can still say `BLOCKED_WORKTREE_DIRTY`; this is reported, not bypassed or rewritten as a clean-tree PASS.
- Owner-authorized prerequisite: only `libomp` 22.1.8 was installed from its Homebrew bottle after confirming absence. Automatic updates/cleanup/dependent upgrades were disabled; no sudo, other package, Python version change or security-setting change. An initial TLS download failure was retried successfully.
- Fixed Python 3.12.14 / XGBoost **2.1.4** / NumPy 2.1.3 / SciPy 1.14.1: **18 tests PASS, 0 failures, 0 errors, 0 skips**. The previous inverse-label fixture correctly failed monotone calibration; the numerical test now trains two genuinely separate synthetic models and fits each side's own calibrator. Rejection of a collapsed anticorrelated fit remains tested. A separate red/green regression fixes risk-distribution metric keys being overwritten by observation-deduplication tuple keys; no scoring formula/threshold changed.
- Java 17 enhanced native suite (`AssetCardModelBundleTest`, `AssetCardBetaCalibrationTest` with `-DassetCard.testPython=...`): **11 tests PASS, 0 failures, 0 errors, 0 skips**. Python actually trains `long.ubj` and `short.ubj`; Java XGBoost4J **2.1.4** loads both and predicts three identical float32 input rows. Maximum raw delta: **0** for both sides. Independent fitted Beta maximum delta: **0** long, **2.20228566286e-20** short, below **1e-7**. The test asserts Java 17/Python XGBoost 2.1.4 and distinct calibrator parameters. JUnit `@TempDir` deletes test artifacts; no production manifest/model is emitted or committed. An initial AssertJ two-dimensional-array assertion compilation error was corrected to the same exact row-count assertion before this passing run.
- CI coverage correction: the repository's existing `ci` profile selects `smoke | core-regression`, not the full suite. The card calculation/market/risk/model, Mapper/V24 and Home projection/rendering tests now carry the existing `core-regression` tag. No workflow, global gate or existing assertion was removed or relaxed. The earlier 1,090-test CI run is not presented as card-test coverage; the updated exact-head run must include these tests.
- Exact `3f7857ef9236ed6da43f7618bbd55517a2dde139` CI ran **1,394 tests, 1 failure, 0 errors, 1 native opt-in skip**; workflow-contract passed. The failure exposed nanosecond epoch timestamps being rounded through a floating-point JSON tree. The regression now deliberately uses a nine-digit fractional instant on every OS and retains exact timestamp equality. Card storage uses a private mapper copy with decimal tree reads, preserving precision without changing the shared application mapper; the test explicitly verifies that isolation. The strengthened regression was reproduced RED locally, then the comprehensive 352-test focused suite passed. The final full-Maven count above is revalidated after this correction before the follow-up commit.

### V42 final Linux native-image attempt (2026-09-11)

The Dockerfile-path gate is effective through PR #1298 / `f24cdf2c5be755d75639317fdfba99cef29bc836`. The exact runtime-only `libgomp1` insertion was captured with business checkpoint `bb4242aef0ae2f85bc7d5d03bde9fa16e029e2cb`; the build stage, Java versions, `USER app` and application entrypoint were preserved. No host dependency installation or business application startup occurred.

Frozen source-only build context: `/private/tmp/asset-card-final-image.K61wfu/context.tar`, SHA-256 `d56173d0c1d7e6ace47699c38ee9d06c89ee3f59627f8eb61ecfa7add5edc110`; Dockerfile SHA-256 `b0d5daff86401ba3257bbc070a6d26e421425c67be60bda50a804b89675b87d0`. Later worktree edits are not attributed to this context. The local Docker daemon reports Linux aarch64; the requested image platform was `linux/arm64`.

Both the first attempt and the single authorized identical retry exited **1**, before `apt-get`, Maven compilation or native inference:

```sh
docker build --pull=false --platform linux/arm64 --progress=plain -t trine-asset-card-v42-native-test:local - < /private/tmp/asset-card-final-image.K61wfu/context.tar
```

Both failed at Dockerfile line 1 while resolving `docker/dockerfile:1.7`: `failed to fetch anonymous token: Get "https://auth.docker.io/token?scope=repository%3Adocker%2Fdockerfile%3Apull&service=registry.docker.io": EOF`. The frontend, base-image aliases, Docker configuration and credentials were not changed; no third attempt was made. Logs: `/private/tmp/asset-card-final-image.K61wfu/build-1.log` (SHA-256 `be7e67cdd215109dcb5cb94faa2b21b8396f45a3066252cdbdf6c554b38ebb1b`) and `build-2.log` (SHA-256 `c23624e51404c7b1fafeda68daf0baf6821f69cb669865c7300fe51183a97e71`).

Final image ID, application-JAR hash, installed `libgomp1` version and final-image long/short raw/Beta deltas are **NOT_AVAILABLE**. The exact local test-image tag is absent. No new UBJ fixture or production model was generated, so no new model artifact requires deletion. The earlier cached-JRE `libgomp.so.1` failure and macOS Java/Python fixture parity are diagnostic evidence only, not a substitute for the required final image. `FINAL_LINUX_NATIVE_PARITY=BLOCKED`; `MODEL_MODE=SHADOW`; `PRODUCTION_MODEL_READY=NO`; real training/calibration/final-test samples and metrics remain `UNKNOWN`. Current phase is NOT DONE.

### Historical V41 local skip reasons (20)

The former native interoperability skip is closed by the successful opt-in run.

| Test class / methods | Count | Reason |
| --- | ---: | --- |
| `AnalysisIdempotencyGuardPostgreSqlIntegrationTest`: `tenSequentialRetriesReturnOneCanonicalAnalysisRun`, `concurrentRetriesAreAtomicAndDoNotAbortTransactions(int)`, `differentKeysCreateDifferentRowsAndPostConflictTransactionsStayHealthy`, `sameKeyWithDifferentNormalizedPayloadFailsClosed` | 4 | Docker unavailable; existing `disabledWithoutDocker` policy |
| `PostgreSqlFlywayMigrationSmokeTest.postgreSqlCurrentMigrationRuntimeTest` | 1 | Docker/Testcontainers unavailable |
| `V23CoinGlassRuntimeSnapshotMigrationContractTest.disposablePostgresqlV23RequiresTemporaryCreateAndPreservesExistingRowsAndPrivileges` | 1 | Disposable PostgreSQL unavailable; no external database fallback |
| `V24AssetCardLiveSignalMigrationContractTest.postgresMigrationPreservesLegacyRowsAndDoesNotChangePrivileges` | 1 | Disposable Docker PostgreSQL unavailable; no external database fallback |
| `ControlledGeneratedReleaseLikeFixtureFlywayTest.createsOnlyTheApprovedLocalGeneratedFixtureAtFlywayV6` | 1 | P3 generated-fixture Flyway environment gate not enabled |
| `ControlledPostgreSqlFlywaySmokeTest.controlledExternalPostgreSqlFlywayMigrationsApplyWhenExplicitlyConfirmed` | 1 | Controlled external PostgreSQL environment missing; external DB access not authorized |
| `ControlledP3hComposeOfflineSmokeTest.disposableComposeProvesBootstrapSecretsProxyAndReadOnlyRole` | 1 | Explicit Docker contract opt-in not enabled |
| `ControlledGreenfieldFlywayV7ActionTest.migratesExactEmptyGreenfieldDatabaseFromV1ToV7AndRepeatsIdempotently` | 1 | P3-G Flyway action environment gate not enabled |
| `ControlledCurrentStateCloneFlywayActionTest.validatesOrMigratesOnlyAnApprovedLocalP3Database` | 1 | P3 controlled PostgreSQL action environment gate not enabled |
| `ControlledCurrentStateContentFingerprintTest`: `rollbackRestoresFingerprint`, `fingerprintOutputDoesNotContainRawModifiedValues`, `sameDataProducesMatchingFingerprint`, `sameRowCountTimeMutationIsDetected`, `sameRowCountPlanBoundaryMutationIsDetected`, `sessionTimezoneDoesNotChangeFingerprint`, `sameRowCountStatusMutationIsDetected` | 7 | P3 controlled content-fingerprint environment gate not enabled |
| `CoinGlassControlledSmokeTest.controlledSmokeUsesCoordinatorAndReturnsSanitizedSummary` | 1 | `COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS` unset; real external smoke not authorized |

## Remaining evidence and release limitations

`MODEL_MODE=SHADOW` (default; feature disabled until a separately authorized runtime configuration).
`PRODUCTION_MODEL_READY=NO`.
Real training/calibration samples: no real training performed in this work; counts NOT_AVAILABLE.
Real Brier/ECE/LogLoss: NOT_AVAILABLE, not values derived from test fixtures.
Real price/model/risk latency: NOT_MEASURED. The 1.5-second renderer, one-second background tick and bounded closed-bar wait are tested mechanisms, not live SLA evidence.
PostgreSQL/container checks need an available Docker/PostgreSQL environment; local skips are not a migration acceptance claim. Previous exact `7016ce88c501e23dba61b443dab42926617c1d0e` CI ran V24 PostgreSQL 2/2 and the existing PostgreSQL smoke 1/1 successfully; new-HEAD CI must be verified separately.
Business PR: #1295, must remain Draft. New-HEAD CI is pending at evidence commit; results will be recorded on this same PR after the push. Merge: not executed. Staging/Production deployment: not executed.
No official-domain acceptance is claimed for undeployed code.

## Files in this pre-merge correction

All 16 paths remain within the unchanged 48-path allowlist:

- `src/main/java/org/example/trademodel/assetcard/AssetCardProperties.java`
- `src/main/java/org/example/trademodel/assetcard/AssetCardService.java`
- `src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java`
- `src/main/java/org/example/trademodel/vo/DashboardHomeVO.java`
- `src/main/resources/static/js/home-runtime.js`
- `scripts/asset_card_model.py`
- `scripts/test_asset_card_model.py`
- `scripts/asset-card-runtime-matrix.mjs`
- `src/test/java/org/example/trademodel/assetcard/AssetCardServiceTest.java`
- `src/test/java/org/example/trademodel/assetcard/AssetCardIsolationIntegrationTest.java`
- `src/test/java/org/example/trademodel/assetcard/AssetCardModelBundleTest.java`
- `src/test/java/org/example/trademodel/controller/DashboardHomeControllerTest.java`
- `src/test/java/org/example/trademodel/controller/HomeUiReviewRuntimeContractTest.java`
- `src/test/java/org/example/trademodel/controller/GlobalFrozenUiAlignmentContractTest.java`
- `src/test/java/org/example/trademodel/service/impl/DashboardHomeServiceImplTest.java`
- `docs/evidence/asset_card_live_signal/IMPLEMENTATION_AND_ACCEPTANCE.md`

The two existing UI-contract files only opt their existing new-card fixtures into
the explicit cohort; original assertions and non-card methods are unchanged.
No Native APP, mobile-specific, canonical Plan/position/AI/Telegram, migration,
gate-owner or product-source contract file is changed by this correction.
