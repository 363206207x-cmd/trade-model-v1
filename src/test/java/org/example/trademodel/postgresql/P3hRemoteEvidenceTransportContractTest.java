package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class P3hRemoteEvidenceTransportContractTest {

    private static final Path BOUNDED_RUNNER = Path.of("scripts/p3h-bounded-process.py")
            .toAbsolutePath();
    private static final Path R1_RUNNER =
            Path.of("scripts/controlled-staging-readonly-deployment-p3h-r1.sh")
                    .toAbsolutePath();
    private static final String SOURCE_HEAD = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String ARCHIVE_SHA =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String APP_JAR_SHA =
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    private static final String APP_ARTIFACT_SHA =
            "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";

    @TempDir
    Path tempDir;

    private final AtomicInteger sequence = new AtomicInteger();

    @Test
    void boundedSupervisorPassesStdinByteForByte() throws Exception {
        byte[] payload = new byte[]{0, 1, 2, 10, 13, 31, 32, 65, 90, (byte) 255};
        Path input = writeBytes("binary-input", payload);
        Path output = tempDir.resolve("binary-output");

        ScriptResult result = runBounded(input, 5, 60, 60, List.of(
                "python3", "-c",
                "import pathlib,sys;pathlib.Path(sys.argv[1]).write_bytes(sys.stdin.buffer.read())",
                output.toString()));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(Files.readAllBytes(output)).containsExactly(payload);
    }

    @Test
    void boundedSupervisorPassesMultilineShellScriptToBashS() throws Exception {
        Path input = write("remote-script", """
                set -euo pipefail
                test "$1" = arg1
                test "$2" = arg2
                printf '%s\n' 'REMOTE_PREFLIGHT: PASS'
                """);
        Path fakeSsh = executable("fake-ssh", """
                #!/usr/bin/env bash
                set -euo pipefail
                shift
                exec "$@"
                """);

        ScriptResult result = runBounded(input, 5, 60, 60,
                List.of(fakeSsh.toString(), "controlled-target", "bash", "-s", "--", "arg1", "arg2"));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("REMOTE_PREFLIGHT: PASS");
    }

    @Test
    void backgroundExecutionCannotReplaceStdinWithDevNull() throws Exception {
        Path input = write("background-input", """
                set -euo pipefail
                printf '%s\n' 'REMOTE_PREFLIGHT: PASS'
                """);
        Path wrapper = executable("background-wrapper", """
                #!/usr/bin/env bash
                set -euo pipefail
                python3 "$1" \
                  --timeout-seconds 5 \
                  --global-start-epoch "$2" \
                  --global-timeout-seconds 60 \
                  --stage BACKGROUND_STDIN_TEST \
                  --operation-class REMOTE_PREFLIGHT \
                  --poll-seconds 1 \
                  --heartbeat-seconds 60 \
                  --term-grace-seconds 1 \
                  --stdin-file "$3" \
                  -- bash -s &
                child=$!
                wait "${child}"
                """);

        ScriptResult result = finish(new ProcessBuilder(
                "bash", wrapper.toString(), BOUNDED_RUNNER.toString(),
                String.valueOf(Instant.now().getEpochSecond()), input.toString())
                .redirectErrorStream(true).start(), Duration.ofSeconds(10));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("REMOTE_PREFLIGHT: PASS");
    }

    @Test
    void emptyStdinFileIsHandledDeterministically() throws Exception {
        Path input = write("empty-input", "");
        Path output = tempDir.resolve("empty-output");

        ScriptResult result = runBounded(input, 5, 60, 60,
                List.of("bash", "-c", "cat >\"$1\"", "_", output.toString()));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(Files.size(output)).isZero();
    }

    @Test
    void symlinkStdinFileFailsClosed() throws Exception {
        Path target = write("stdin-target", "safe");
        Path link = tempDir.resolve("stdin-link");
        Files.createSymbolicLink(link, target);

        ScriptResult result = runBounded(link, 5, 60, 60, List.of("cat"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("STDIN_FILE_STATUS: BLOCKED_SYMLINK");
        assertThat(result.output()).doesNotContain(target.toString(), "safe");
    }

    @Test
    void worldWritableStdinFileFailsClosed() throws Exception {
        Path input = write("world-writable-input", "safe");
        Files.setPosixFilePermissions(input, PosixFilePermissions.fromString("rw-rw-rw-"));

        ScriptResult result = runBounded(input, 5, 60, 60, List.of("cat"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("STDIN_FILE_STATUS: BLOCKED_UNSAFE_PERMISSIONS");
        assertThat(result.output()).doesNotContain("safe");
    }

    @Test
    void oversizedStdinFileFailsClosed() throws Exception {
        Path input = writeBytes("oversized-input", new byte[1024 * 1024 + 1]);

        ScriptResult result = runBounded(input, 5, 60, 60, List.of("cat"));

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("STDIN_FILE_STATUS: BLOCKED_OVERSIZED");
    }

    @Test
    void stdinContentNeverAppearsInHeartbeat() throws Exception {
        String sentinel = "stdin-content-must-never-appear";
        Path input = write("heartbeat-input", sentinel);

        ScriptResult result = runBounded(input, 5, 60, 1,
                List.of("bash", "-c", "sleep 2; cat >/dev/null"));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("PROCESS_STATE: RUNNING");
        assertThat(result.output()).doesNotContain(sentinel);
    }

    @Test
    void stdinTransportStillHonorsStageTimeout() throws Exception {
        Path input = write("timeout-input", "safe");

        ScriptResult result = runBounded(input, 1, 60, 60,
                List.of("bash", "-c", "cat >/dev/null; sleep 30"));

        assertThat(result.exitCode()).isEqualTo(124);
        assertThat(result.output()).contains("STAGE_TIMEOUT_TRIGGERED: YES");
    }

    @Test
    void stdinTransportStillKillsChildProcessGroup() throws Exception {
        Path input = write("tree-input", "safe");
        Path childPid = tempDir.resolve("stdin-child.pid");

        ScriptResult result = runBounded(input, 1, 60, 60, List.of(
                "bash", "-c", "cat >/dev/null; sleep 30 & echo $! >\"$1\"; wait", "_",
                childPid.toString()));

        assertThat(result.exitCode()).isEqualTo(124);
        long pid = Long.parseLong(Files.readString(childPid).trim());
        assertEventuallyStopped(pid);
    }

    @Test
    void boundedRunnerShellFunctionsHandleOptionalStdinUnderNounset() throws Exception {
        String productionScript = Files.readString(R1_RUNNER);
        String runtimeStart = "\ncase \"${P3H_TARGET_CLASS:-}\" in\n";
        int runtimeStartIndex = productionScript.indexOf(runtimeStart);
        assertThat(runtimeStartIndex).isPositive();

        Path stdin = write("shell-function-stdin", """
                set -euo pipefail
                test "$1" = arg1
                printf '%s\n' 'WITH_STDIN_SUPERVISOR: PASS'
                """);
        Path harness = executable("bounded-shell-functions",
                productionScript.substring(0, runtimeStartIndex) + """

                        BOUNDED_PROCESS_RUNNER="$1"
                        CURRENT_STAGE=optional-stdin-regression
                        RUN_START_EPOCH="$(date +%s)"
                        GLOBAL_TIMEOUT_SECONDS=30
                        POLL_INTERVAL_SECONDS=1
                        HEARTBEAT_INTERVAL_SECONDS=60
                        TERM_GRACE_SECONDS=1
                        run_bounded_process 5 NO_STDIN \
                          bash -c 'printf "%s\\n" "NO_STDIN_SUPERVISOR: PASS"'
                        run_bounded_with_stdin 5 WITH_STDIN "$2" bash -s -- arg1
                        """);

        ScriptResult result = finish(new ProcessBuilder(
                "bash", harness.toString(), BOUNDED_RUNNER.toString(), stdin.toString())
                .redirectErrorStream(true).start(), Duration.ofSeconds(15));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains(
                "NO_STDIN_SUPERVISOR: PASS",
                "WITH_STDIN_SUPERVISOR: PASS");
    }

    @Test
    void remotePreflightEvidenceContractPassesExactFixture() throws Exception {
        ScriptResult result = runPreflight(validPreflight());

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("P3H_EVIDENCE_REDACTION: PASS_EXACT");
        assertThat(Files.readString(result.sanitized())).isEqualTo(validPreflight());
    }

    @Test
    void remotePreflightMissingDuplicateUnknownAndMalformedEvidenceFailClosed() throws Exception {
        assertThat(runPreflight(validPreflight().replace("TIMEZONE: UTC\n", "")).exitCode())
                .isNotZero();
        assertThat(runPreflight(validPreflight() + "TIMEZONE: UTC\n").exitCode()).isNotZero();
        assertThat(runPreflight(validPreflight() + "RAW_RESPONSE: hidden\n").exitCode()).isNotZero();
        assertThat(runPreflight(validPreflight().replace("TIMEZONE: UTC", "TIMEZONE = UTC"))
                .exitCode()).isNotZero();
    }

    @Test
    void remotePreflightControlCharacterAndOversizedValueFailClosed() throws Exception {
        assertThat(runPreflight(validPreflight() + "RAW\u0001LINE: hidden\n").exitCode())
                .isNotZero();
        assertThat(runPreflight(validPreflight() + "UNKNOWN_KEY: " + "A".repeat(513) + "\n")
                .exitCode()).isNotZero();
    }

    @Test
    void exactHeadArtifactBuildEvidencePasses() throws Exception {
        ScriptResult result = runArtifact(validArtifactBuild());

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("P3H_ARTIFACT_BUILD_CONTRACT: PASS_EXACT");
    }

    @Test
    void artifactBuildEvidenceRejectsWrongHeadMissingOrUnknownFields() throws Exception {
        assertThat(runArtifact(validArtifactBuild().replace(
                "APP_ARTIFACT_SOURCE_HEAD: " + SOURCE_HEAD,
                "APP_ARTIFACT_SOURCE_HEAD: " + "f".repeat(40))).exitCode()).isNotZero();
        assertThat(runArtifact(validArtifactBuild().replace(
                "APP_JAR_SIZE_BYTES: 1024\n", "")).exitCode()).isNotZero();
        assertThat(runArtifact(validArtifactBuild() + "ABSOLUTE_PATH: hidden\n")
                .exitCode()).isNotZero();
    }

    @Test
    void zeroExitWithoutCompletionMarkerFails() throws Exception {
        Process remote = new ProcessBuilder("bash", "-c", "exit 0").start();
        assertThat(remote.waitFor(5, TimeUnit.SECONDS)).isTrue();
        assertThat(remote.exitValue()).isZero();

        assertThat(runAction("BUILD_APPLICATION_IMAGE", "").exitCode()).isNotZero();
    }

    @Test
    void emptyActionOutputFails() throws Exception {
        assertThat(runAction("BACKUP_RESTORE", "").exitCode()).isNotZero();
    }

    @Test
    void partialActionOutputFails() throws Exception {
        assertThat(runAction("BUILD_APPLICATION_IMAGE",
                "P3H_REMOTE_STAGE: APPLICATION_IMAGE_BUILD_PASS\n").exitCode()).isNotZero();
    }

    @Test
    void duplicateCompletionMarkerFails() throws Exception {
        assertThat(runAction("BUILD_APPLICATION_IMAGE", validBuild()
                + "P3H_REMOTE_STAGE: APPLICATION_IMAGE_BUILD_PASS\n").exitCode()).isNotZero();
    }

    @Test
    void duplicateSemanticEvidenceFails() throws Exception {
        assertThat(runAction("BACKUP_RESTORE", validBackupRestore()
                + "RESTORE_CONTENT: MATCH\n").exitCode()).isNotZero();
    }

    @Test
    void unknownEvidenceLineFails() throws Exception {
        String secret = "value-that-must-not-be-exposed";
        ScriptResult result = runAction("BUILD_APPLICATION_IMAGE",
                validBuild() + "RAW_RESPONSE: " + secret + "\n");

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("BLOCKED_UNKNOWN_KEY").doesNotContain(secret);
    }

    @Test
    void malformedEvidenceValueFails() throws Exception {
        assertThat(runAction("BUILD_APPLICATION_IMAGE",
                validBuild().replace("APP_IMAGE_USER: NON_ROOT_10001",
                        "APP_IMAGE_USER: root")).exitCode()).isNotZero();
    }

    @Test
    void validBuildEvidencePasses() throws Exception {
        assertActionPasses("BUILD_APPLICATION_IMAGE", validBuild());
    }

    @Test
    void validHeartbeatAndProgressEvidenceIsWhitelistedButNotPromoted() throws Exception {
        ScriptResult result = runAction("BUILD_APPLICATION_IMAGE", """
                P3H_LAB_STAGE: APPLICATION_IMAGE_BUILD
                STAGE_ELAPSED_SECONDS: 60
                GLOBAL_ELAPSED_SECONDS: 120
                PROCESS_STATE: RUNNING_PROGRESS
                DOCKER_OPERATION_CLASS: APPLICATION_IMAGE_BUILD
                P3H_PROGRESS_PROBE_STATUS: PASS
                VM_AVAILABLE_MEMORY_MB: 8192
                VM_AVAILABLE_DISK_GB: 30
                DOCKER_DAEMON: ACTIVE
                DNS_RESOLUTION: PASS
                REQUIRED_REGISTRY_CONNECTIVITY: PASS_BOUNDED
                """ + validBuild());

        assertThat(result.exitCode()).as(result.output()).isZero();
        String sanitized = Files.readString(result.sanitized());
        assertThat(sanitized).contains(validBuild().trim());
        assertThat(sanitized).doesNotContain(
                "P3H_LAB_STAGE", "PROCESS_STATE", "P3H_PROGRESS_PROBE_STATUS");
    }

    @Test
    void validInitialDeployEvidencePasses() throws Exception {
        assertActionPasses("INITIAL_DEPLOY", validInitialDeploy());
    }

    @Test
    void validBackupRestoreEvidencePasses() throws Exception {
        assertActionPasses("BACKUP_RESTORE", validBackupRestore());
    }

    @Test
    void validRotationEvidencePasses() throws Exception {
        assertActionPasses("ROTATE", validRotation());
    }

    @Test
    void validPostRebootEvidencePasses() throws Exception {
        assertActionPasses("POST_REBOOT_VERIFY", validPostReboot());
    }

    @Test
    void validCleanupEvidencePasses() throws Exception {
        assertActionPasses("CLEANUP", validCleanup());
    }

    @Test
    void finalEvidenceContractPassesOnlyCompleteExactFixture() throws Exception {
        ScriptResult result = runFinal(validFinalEvidence());

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("P3H_LAB_EVIDENCE_REDACTION: PASS_EXACT");
        String sanitized = Files.readString(result.sanitized());
        assertThat(sanitized).contains(
                "P3H_REMOTE_STAGE: BACKUP_RESTORE_PASS",
                "P3H_REMOTE_STAGE: ROTATION_PASS",
                "P3H_REMOTE_STAGE: POST_REBOOT_PASS",
                "P3H_REMOTE_STAGE: CLEANUP_PASS",
                "RESOURCE_CLEANUP: PASS",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
    }

    @Test
    void finalEvidenceMissingBackupRestoreRotationRebootOrCleanupFailsClosed() throws Exception {
        String complete = validFinalEvidence();
        assertThat(runFinal(complete.replace(validBackupRestore(), "")).exitCode()).isNotZero();
        assertThat(runFinal(complete.replace(validRotation(), "")).exitCode()).isNotZero();
        assertThat(runFinal(complete.replace(validPostReboot(), "")).exitCode()).isNotZero();
        assertThat(runFinal(complete.replace(validCleanup(), "")).exitCode()).isNotZero();
        assertThat(runFinal(complete.replace("RESOURCE_CLEANUP: PASS\n", "")).exitCode())
                .isNotZero();
    }

    @Test
    void finalEvidenceRejectsDuplicateUnknownAndMismatchedArchiveSha() throws Exception {
        String complete = validFinalEvidence();
        assertThat(runFinal(complete + "RESOURCE_CLEANUP: PASS\n").exitCode()).isNotZero();
        assertThat(runFinal(complete + "RAW_LOG: hidden\n").exitCode()).isNotZero();
        assertThat(runFinal(complete.replace(
                "SOURCE_ARCHIVE_REMOTE_SHA256: " + ARCHIVE_SHA,
                "SOURCE_ARCHIVE_REMOTE_SHA256: " + "c".repeat(64))).exitCode()).isNotZero();
        assertThat(runFinal(complete.replace(
                "APP_IMAGE_JAR_SHA256: " + APP_JAR_SHA,
                "APP_IMAGE_JAR_SHA256: " + "e".repeat(64))).exitCode()).isNotZero();
    }

    private void assertActionPasses(String action, String fixture) throws Exception {
        ScriptResult result = runAction(action, fixture);
        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("P3H_REMOTE_ACTION_EVIDENCE_CONTRACT: PASS_EXACT");
    }

    private ScriptResult runBounded(Path stdinFile, int stageTimeout, int globalTimeout,
                                    int heartbeat, List<String> command) throws Exception {
        List<String> args = new ArrayList<>(List.of(
                "python3", BOUNDED_RUNNER.toString(),
                "--timeout-seconds", String.valueOf(stageTimeout),
                "--global-start-epoch", String.valueOf(Instant.now().getEpochSecond()),
                "--global-timeout-seconds", String.valueOf(globalTimeout),
                "--stage", "STDIN_TRANSPORT_TEST",
                "--operation-class", "REMOTE_PREFLIGHT",
                "--poll-seconds", "1",
                "--heartbeat-seconds", String.valueOf(heartbeat),
                "--term-grace-seconds", "1",
                "--stdin-file", stdinFile.toString(),
                "--"));
        args.addAll(command);
        return finish(new ProcessBuilder(args).redirectErrorStream(true).start(),
                Duration.ofSeconds(10));
    }

    private ScriptResult runPreflight(String fixture) throws Exception {
        return runRedactor("scripts/p3h-server-evidence-redact.sh", fixture, List.of());
    }

    private ScriptResult runArtifact(String fixture) throws Exception {
        int current = sequence.incrementAndGet();
        Path raw = write("raw-" + current, fixture);
        Path sanitized = tempDir.resolve("sanitized-" + current);
        List<String> command = List.of(
                "python3", "scripts/p3h-evidence-contract.py",
                "--contract", "artifact",
                "--source-head", SOURCE_HEAD,
                "--input-file", raw.toString(),
                "--output-file", sanitized.toString(),
                "--status-key", "P3H_ARTIFACT_BUILD_CONTRACT");
        ScriptResult result = finish(
                new ProcessBuilder(command).redirectErrorStream(true).start(),
                Duration.ofSeconds(5));
        return new ScriptResult(result.exitCode(), result.output(), sanitized);
    }

    private ScriptResult runAction(String action, String fixture) throws Exception {
        int current = sequence.incrementAndGet();
        Path raw = write("raw-" + current, fixture);
        Path sanitized = tempDir.resolve("sanitized-" + current);
        List<String> command = new ArrayList<>(List.of(
                "bash", "scripts/p3h-remote-action-evidence-redact.sh",
                action, SOURCE_HEAD, raw.toString(), sanitized.toString()));
        if ("BUILD_APPLICATION_IMAGE".equals(action)) {
            command.add(APP_JAR_SHA);
            command.add(APP_ARTIFACT_SHA);
        }
        ScriptResult result = finish(
                new ProcessBuilder(command).redirectErrorStream(true).start(),
                Duration.ofSeconds(5));
        return new ScriptResult(result.exitCode(), result.output(), sanitized);
    }

    private ScriptResult runFinal(String fixture) throws Exception {
        return runRedactor("scripts/p3h-lab-evidence-redact.sh", fixture,
                List.of(SOURCE_HEAD));
    }

    private ScriptResult runRedactor(String script, String fixture, List<String> leadingArguments)
            throws Exception {
        int current = sequence.incrementAndGet();
        Path raw = write("raw-" + current, fixture);
        Path sanitized = tempDir.resolve("sanitized-" + current);
        List<String> command = new ArrayList<>(List.of("bash", script));
        command.addAll(leadingArguments);
        if (script.endsWith("p3h-remote-action-evidence-redact.sh")) {
            command.add(raw.toString());
            command.add(sanitized.toString());
        } else if (script.endsWith("p3h-lab-evidence-redact.sh")) {
            command = new ArrayList<>(List.of("bash", script, raw.toString(),
                    sanitized.toString(), SOURCE_HEAD));
        } else {
            command.add(raw.toString());
            command.add(sanitized.toString());
        }
        ScriptResult result = finish(new ProcessBuilder(command).redirectErrorStream(true).start(),
                Duration.ofSeconds(5));
        return new ScriptResult(result.exitCode(), result.output(), sanitized);
    }

    private String validPreflight() {
        return """
                REMOTE_PREFLIGHT: PASS
                LINUX_DISTRIBUTION: debian
                KERNEL_RELEASE: 6.1.0-arm64
                CPU_ARCHITECTURE: aarch64
                SYSTEMD_VERSION: 252
                DOCKER_ENGINE_VERSION: 28.3.3
                DOCKER_COMPOSE_VERSION: 2.38.2
                OPENSSL_VERSION: 3.0.17
                TIMEZONE: UTC
                TIME_SYNCHRONIZED: YES
                SECRET_BACKEND_CLASS: SYSTEMD_CREDENTIALS
                SECRET_MOUNT: RUNTIME_ONLY
                SECRET_MOUNT_RUNTIME_VERIFICATION: PASS_BACKEND_BOUND
                SECRET_MOUNT_FILESYSTEM: tmpfs
                SECRET_FILE_CONTRACT: PASS_NAMES_OWNERS_PERMISSIONS
                SUDO_NONINTERACTIVE: AVAILABLE
                AVAILABLE_DISK_KB: 40000000
                AVAILABLE_MEMORY_KB: 8000000
                """;
    }

    private String validArtifactBuild() {
        return """
                P3H_ARTIFACT_BUILD: PASS_EXACT_HEAD
                APP_ARTIFACT_SOURCE_HEAD: %s
                APP_JAR_SHA256: %s
                APP_JAR_SIZE_BYTES: 1024
                APP_ARTIFACT_ARCHIVE_SHA256: %s
                """.formatted(SOURCE_HEAD, APP_JAR_SHA, APP_ARTIFACT_SHA);
    }

    private String validBuild() {
        return """
                P3H_REMOTE_STAGE: APPLICATION_IMAGE_BUILD_PASS
                APP_ARTIFACT_SOURCE_HEAD: %s
                APP_JAR_SHA256: %s
                APP_ARTIFACT_ARCHIVE_SHA256: %s
                APP_ARTIFACT_REMOTE_SHA256: MATCH
                APP_JAR_REMOTE_SHA256: MATCH
                P3H_RUNTIME_IMAGE_PREFETCH: PASS_4_OF_4
                APP_IMAGE_REVISION: %s
                APP_IMAGE_JAR_SHA256: %s
                APP_IMAGE_USER: NON_ROOT_10001
                APP_IMAGE_JAR_CONTENT_SHA: MATCH
                APPLICATION_IMAGE_BUILD_MODE: RUNTIME_ONLY_PREBUILT_JAR
                """.formatted(
                SOURCE_HEAD, APP_JAR_SHA, APP_ARTIFACT_SHA, SOURCE_HEAD, APP_JAR_SHA);
    }

    private String validInitialDeploy() {
        return """
                P3H_REMOTE_STAGE: INITIAL_DEPLOY_PASS
                STAGING_FLYWAY: PASS_V1_TO_V7
                FLYWAY_REPEAT: ZERO_MIGRATIONS
                APPLICATION_DATABASE_ROLE: READ_ONLY
                READ_ONLY_WRITE_PROBE: DENIED
                TLS_1_2: PASS
                TLS_1_3: PASS
                HTTP_TO_HTTPS_REDIRECT: PASS
                UNKNOWN_HOST: REJECTED
                UNAUTHENTICATED_API: DENIED
                AUTHENTICATED_DASHBOARD: PASS
                EMPTY_DASHBOARD_FAIL_CLOSED: PASS
                RATE_LIMIT: PASS_429
                """;
    }

    private String validBackupRestore() {
        return """
                P3H_REMOTE_STAGE: BACKUP_RESTORE_PASS
                PROD_BACKUP_SCRIPT: PASS
                PROD_RESTORE_SCRIPT: PASS
                RESTORE_SCHEMA: MATCH
                RESTORE_CONTENT: MATCH
                """;
    }

    private String validRotation() {
        return """
                P3H_REMOTE_STAGE: ROTATION_PASS
                ADMIN_SECRET_ROTATION: PASS_V2_ACTIVE_V1_DENIED
                DATABASE_SECRET_ROTATION: PASS_V2_ACTIVE_V1_DENIED
                TLS_ROTATION: PASS
                SERVICE_RESTART: PASS
                """;
    }

    private String validPostReboot() {
        return """
                P3H_REMOTE_STAGE: POST_REBOOT_PASS
                VM_REBOOT_STATUS: PASS_ACTUAL_LINUX_VM_REBOOT
                V2_DATABASE_AFTER_REBOOT: PASS
                V1_DATABASE_AFTER_REBOOT: DENIED
                V2_ADMIN_AFTER_REBOOT: PASS
                V1_ADMIN_AFTER_REBOOT: DENIED
                POST_REBOOT_CONTENT_FINGERPRINT: MATCH
                SECRET_LEAK_CANDIDATE_COUNT: 0
                PROVIDER_EXTERNAL_CALLS: DISABLED
                AI_EXTERNAL_CALLS: DISABLED
                SCHEDULERS: DISABLED
                TRADING: DISABLED
                P3H_REMOTE_EXECUTION_IMPLEMENTATION: PASS_LOCAL_VM
                REAL_EXTERNAL_STAGING_STATUS: NOT_RUN
                P3H_RESULT: PARTIAL_LOCAL_VM_EVIDENCE
                P4_ALLOWED: NO
                PRODUCTION_READINESS: BLOCKED
                """;
    }

    private String validCleanup() {
        return "P3H_REMOTE_STAGE: CLEANUP_PASS\n";
    }

    private String validFinalEvidence() {
        return validPreflight()
                + "SOURCE_ARCHIVE_SHA256: " + ARCHIVE_SHA + "\n"
                + "SOURCE_ARCHIVE_REMOTE_SHA256: " + ARCHIVE_SHA + "\n"
                + validBuild()
                + validInitialDeploy()
                + validBackupRestore()
                + validRotation()
                + validPostReboot()
                + validCleanup()
                + "RESOURCE_CLEANUP: PASS\n";
    }

    private Path write(String name, String content) throws IOException {
        return writeBytes(name, content.getBytes(StandardCharsets.UTF_8));
    }

    private Path writeBytes(String name, byte[] content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, content);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        return path;
    }

    private Path executable(String name, String content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.writeString(path, content.stripIndent(), StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
        return path;
    }

    private void assertEventuallyStopped(long pid) throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                Thread.sleep(100);
            } else {
                return;
            }
        }
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
    }

    private ScriptResult finish(Process process, Duration timeout) throws Exception {
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8), null);
    }

    private record ScriptResult(int exitCode, String output, Path sanitized) {
    }
}
