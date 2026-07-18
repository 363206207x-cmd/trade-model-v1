package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class P3hBootstrapTimeoutAndPgpassIntegrityTest {

    private static final Path BOUNDED_RUNNER = Path.of("scripts/p3h-bounded-process.py")
            .toAbsolutePath();

    @TempDir
    Path tempDir;

    @Test
    void limaInternalTimeoutCannotPreemptBootstrapContract() throws Exception {
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");

        assertThat(bootstrap).contains(
                "VM_BOOTSTRAP_TIMEOUT_SECONDS=1200",
                "LIMA_INTERNAL_TIMEOUT=21m",
                "--timeout=\"${LIMA_INTERNAL_TIMEOUT}\"");
        assertThat(bootstrap).doesNotContain("--timeout=15m");
    }

    @Test
    void minimalVmStartDoesNotInstallDocker() throws Exception {
        String template = read("deploy/p3h/lima/p3h-lab.yaml");

        assertThat(template).contains("provision: []", "minimal Linux VM");
        assertThat(template).doesNotContain(
                "apt-get", "docker-ce", "download.docker.com", "docker compose");
    }

    @Test
    void guestDockerProvisionIsSeparateBoundedStage() throws Exception {
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");
        String provision = read("deploy/p3h/lima/p3h-lab-provision-linux.sh");

        assertThat(bootstrap).contains(
                "GUEST_PROVISION_TIMEOUT_SECONDS=1800",
                "GUEST_PACKAGE_AND_DOCKER_PROVISION",
                "COPY_DEDICATED_PUBLIC_KEY",
                "VM_CONSOLE_AVAILABLE");
        assertThat(provision).contains(
                "BLOCKED_GUEST_DNS", "BLOCKED_GUEST_APT_UPDATE",
                "BLOCKED_DOCKER_REPOSITORY", "BLOCKED_DOCKER_PACKAGE_INSTALL",
                "BLOCKED_DOCKER_DAEMON_START", "BLOCKED_DOCKER_COMPOSE_MISSING");
    }

    @Test
    void guestProvisionFailureHasSanitizedCategory() throws Exception {
        String provision = read("deploy/p3h/lima/p3h-lab-provision-linux.sh");
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");

        assertThat(provision).contains(
                "P3H_LAB_PROVISION_STAGE:", "P3H_LAB_PROVISION_ELAPSED_SECONDS:",
                "P3H_LAB_PROVISION_EXIT_CODE:", ">>\"${provision_log}\" 2>&1");
        assertThat(bootstrap).contains(
                "GUEST_PROVISION_FAILURE_CATEGORY:", "GUEST_PROVISION_STAGE:",
                "GUEST_PROVISION_ELAPSED_SECONDS:", "GUEST_PROVISION_EXIT_CODE:");
        assertThat(bootstrap).doesNotContain("cat \"${guest_provision_log}\"");
        assertThat(provision).doesNotContain(
                "echo \"HOSTNAME:", "echo \"TIMEZONE:", "echo \"TIME_SYNCHRONIZED:",
                "echo \"DEPLOYMENT_USER:", "echo \"PASSWORD_AUTHENTICATION:");
    }

    @Test
    void hungDirectChildIsKilledByGlobalDeadline() throws Exception {
        ProcessTreeEvidence evidence = runHungProcessTree("DIRECT_CHILD", true);

        assertThat(evidence.result().exitCode()).isEqualTo(125);
        assertThat(evidence.result().output()).contains("GLOBAL_TIMEOUT_TRIGGERED: YES");
        assertProcessTreeStopped(evidence);
    }

    @Test
    void hungLimaCommandCannotOutliveSupervisor() throws Exception {
        ScriptResult result = runBounded(
                1, 60, "LIMA_COMMAND", List.of("bash", "-c", "trap '' TERM; sleep 30"), null, 300);

        assertThat(result.exitCode()).isEqualTo(124);
        assertThat(result.output()).contains("DOCKER_OPERATION_CLASS: LIMA_COMMAND");
    }

    @Test
    void hungSshCommandCannotOutliveSupervisor() throws Exception {
        ScriptResult result = runBounded(
                1, 60, "SSH_COMMAND", List.of("bash", "-c", "trap '' TERM; sleep 30"), null, 300);

        assertThat(result.exitCode()).isEqualTo(124);
        assertThat(result.output()).contains("DOCKER_OPERATION_CLASS: SSH_COMMAND");
    }

    @Test
    void termEscalatesToKillAfterGrace() throws Exception {
        ScriptResult result = runBounded(
                1, 60, "TERM_KILL", List.of("bash", "-c", "trap '' TERM; while :; do sleep 1; done"),
                null, 300);

        assertThat(result.exitCode()).isEqualTo(124);
        assertThat(result.output()).contains("TERM_ESCALATED_TO_KILL: YES");
    }

    @Test
    void unrelatedProcessGroupIsNotTouched() throws Exception {
        Process unrelated = new ProcessBuilder("sleep", "30").start();
        try {
            ProcessTreeEvidence evidence = runHungProcessTree("ISOLATION", true);

            assertThat(evidence.result().exitCode()).isEqualTo(125);
            assertProcessTreeStopped(evidence);
            assertThat(unrelated.isAlive()).isTrue();
        } finally {
            unrelated.destroyForcibly();
            unrelated.waitFor(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void hungCleanupCommandIsBounded() throws Exception {
        Path cleanup = executable("cleanup-hang.sh", """
                #!/usr/bin/env bash
                trap '' TERM
                while :; do sleep 1; done
                """);
        long started = System.nanoTime();
        ScriptResult result = runBounded(
                60, 60, "CLEANUP_TEST", List.of("bash", "-c", "exit 2"), cleanup, 1);

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(8));
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains(
                "SUPERVISOR_CLEANUP_STATUS: FAIL", "SUPERVISOR_CLEANUP_TIMEOUT: YES");
    }

    @Test
    void hungLimaStopIsBounded() throws Exception {
        DestroyEvidence evidence = runDestroyStub(true, false);

        assertThat(evidence.elapsed()).isLessThan(Duration.ofSeconds(12));
        assertThat(evidence.result().exitCode()).isNotZero();
        assertThat(evidence.result().output()).contains(
                "P3H_LAB_DESTROY: FAIL_LIMA_STOP", "LAB_VM_CLEANUP: FAIL");
    }

    @Test
    void hungLimaDeleteIsBounded() throws Exception {
        DestroyEvidence evidence = runDestroyStub(false, true);

        assertThat(evidence.elapsed()).isLessThan(Duration.ofSeconds(12));
        assertThat(evidence.result().exitCode()).isNotZero();
        assertThat(evidence.result().output()).contains(
                "P3H_LAB_DESTROY: FAIL_LIMA_DELETE", "LAB_VM_CLEANUP: FAIL");
    }

    @Test
    void cleanupFailureCannotPrintPass() throws Exception {
        DestroyEvidence evidence = runDestroyStub(false, true);

        assertThat(evidence.result().output()).contains("RESOURCE_CLEANUP: FAIL");
        assertThat(evidence.result().output()).doesNotContain("RESOURCE_CLEANUP: PASS");
    }

    @Test
    void cleanupPreservesUnrelatedVm() throws Exception {
        DestroyEvidence evidence = runDestroyStub(false, false);

        assertThat(evidence.result().exitCode()).as(evidence.result().output()).isZero();
        assertThat(Files.readString(evidence.commandLog())).contains(
                "delete --force trade-model-p3h-staging-lab");
        assertThat(Files.readString(evidence.commandLog())).doesNotContain("unrelated-vm");
    }

    @Test
    void cleanupPreservesUnrelatedFiles() throws Exception {
        Path unrelated = tempDir.resolve("unrelated-owner-data");
        Files.writeString(unrelated, "preserve", StandardCharsets.UTF_8);

        DestroyEvidence evidence = runDestroyStub(false, false);

        assertThat(evidence.result().exitCode()).as(evidence.result().output()).isZero();
        assertThat(unrelated).exists();
        assertThat(Files.readString(unrelated)).isEqualTo("preserve");
    }

    @Test
    void backupPasswordWithColonPasses() throws Exception {
        assertPgpassRoundTrip(false, "colon:password");
    }

    @Test
    void backupPasswordWithBackslashPasses() throws Exception {
        assertPgpassRoundTrip(false, "slash\\password");
    }

    @Test
    void restorePasswordWithColonPasses() throws Exception {
        assertPgpassRoundTrip(true, "colon:password");
    }

    @Test
    void restorePasswordWithBackslashPasses() throws Exception {
        assertPgpassRoundTrip(true, "slash\\password");
    }

    @Test
    void embeddedNewlinePasswordFailsClosed() throws Exception {
        ScriptResult backup = runDatabaseScript(false, "line-one\nline-two", false);
        ScriptResult restore = runDatabaseScript(true, "line-one\nline-two", false);

        assertThat(backup.exitCode()).isNotZero();
        assertThat(restore.exitCode()).isNotZero();
        assertThat(backup.output() + restore.output()).contains("Invalid ");
    }

    @Test
    void emptyPasswordFileFailsClosed() throws Exception {
        ScriptResult backup = runDatabaseScript(false, "", false);
        ScriptResult restore = runDatabaseScript(true, "", false);

        assertThat(backup.exitCode()).isNotZero();
        assertThat(restore.exitCode()).isNotZero();
    }

    @Test
    void passwordNeverAppearsInOutput() throws Exception {
        String password = "never-print-colon:and\\slash";
        ScriptResult backup = runDatabaseScript(false, password, true);
        ScriptResult restore = runDatabaseScript(true, password, true);

        assertThat(backup.output() + restore.output()).doesNotContain(password);
    }

    private void assertPgpassRoundTrip(boolean restore, String password) throws Exception {
        ScriptResult result = runDatabaseScript(restore, password, true);

        assertThat(result.exitCode()).isZero();
        Path capture = tempDir.resolve(restore ? "restore.pgpass" : "backup.pgpass");
        assertThat(Files.readString(capture)).isEqualTo(
                "db\\:host:54\\:32:trade\\:model:user\\\\name:"
                        + password.replace("\\", "\\\\").replace(":", "\\:") + "\n");
    }

    private ScriptResult runDatabaseScript(boolean restore, String password, boolean requireSuccess)
            throws Exception {
        String prefix = restore ? "restore" : "backup";
        Path bin = Files.createDirectories(tempDir.resolve(prefix + "-bin"));
        Path capture = tempDir.resolve(prefix + ".pgpass");
        Path passwordFile = tempDir.resolve(prefix + "-password");
        Files.writeString(passwordFile, password, StandardCharsets.UTF_8);
        setMode(passwordFile, "rw-------");
        Path backupFile = tempDir.resolve("fixture.dump");
        Files.writeString(backupFile, "fixture", StandardCharsets.UTF_8);
        executable(bin.resolve("pg_dump"), """
                #!/usr/bin/env bash
                set -euo pipefail
                cp "${PGPASSFILE}" "${PGPASS_CAPTURE}"
                for arg in "$@"; do
                  case "${arg}" in --file=*) : >"${arg#--file=}" ;; esac
                done
                """);
        executable(bin.resolve("pg_restore"), """
                #!/usr/bin/env bash
                set -euo pipefail
                cp "${PGPASSFILE}" "${PGPASS_CAPTURE}"
                """);
        executable(bin.resolve("psql"), """
                #!/usr/bin/env bash
                set -euo pipefail
                cp "${PGPASSFILE}" "${PGPASS_CAPTURE}"
                """);

        List<String> command = List.of("bash", restore
                ? "scripts/prod-restore.sh" : "scripts/prod-backup.sh");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        env.put("PATH", bin + ":" + env.get("PATH"));
        env.put("PGPASS_CAPTURE", capture.toString());
        env.remove("PGPASSWORD");
        env.remove("PROD_DATASOURCE_PASSWORD");
        env.remove("RESTORE_DATASOURCE_PASSWORD");
        if (restore) {
            env.put("RESTORE_DATASOURCE_HOST", "db:host");
            env.put("RESTORE_DATASOURCE_PORT", "54:32");
            env.put("RESTORE_DATASOURCE_DATABASE", "trade:model");
            env.put("RESTORE_DATASOURCE_USERNAME", "user\\name");
            env.put("RESTORE_DATASOURCE_PASSWORD_FILE", passwordFile.toString());
            env.put("RESTORE_BACKUP_FILE", backupFile.toString());
            env.put("RESTORE_CONFIRM", "I_UNDERSTAND_RESTORE_CAN_OVERWRITE_DATA");
        } else {
            env.put("PROD_DATASOURCE_HOST", "db:host");
            env.put("PROD_DATASOURCE_PORT", "54:32");
            env.put("PROD_DATASOURCE_DATABASE", "trade:model");
            env.put("PROD_DATASOURCE_USERNAME", "user\\name");
            env.put("PROD_DATASOURCE_PASSWORD_FILE", passwordFile.toString());
            env.put("BACKUP_DIR", tempDir.resolve("backup-output").toString());
            env.put("BACKUP_FILE", tempDir.resolve("backup-output/result.dump").toString());
        }
        ScriptResult result = finish(builder.start(), Duration.ofSeconds(10));
        if (requireSuccess) {
            assertThat(result.exitCode()).isZero();
        }
        return result;
    }

    private ProcessTreeEvidence runHungProcessTree(String operationClass, boolean globalTimeout)
            throws Exception {
        Path mainPid = tempDir.resolve(operationClass + "-main.pid");
        Path childPid = tempDir.resolve(operationClass + "-child.pid");
        Path grandPid = tempDir.resolve(operationClass + "-grand.pid");
        String grandCode = "import os,signal,sys,time;"
                + "signal.signal(signal.SIGTERM, signal.SIG_IGN);"
                + "open(sys.argv[1],'w').write(str(os.getpid()));time.sleep(60)";
        String childCode = "import os,signal,subprocess,sys;"
                + "signal.signal(signal.SIGTERM, signal.SIG_IGN);"
                + "open(sys.argv[1],'w').write(str(os.getpid()));"
                + "p=subprocess.Popen([sys.executable,'-c',sys.argv[3],sys.argv[2]]);p.wait()";
        String mainCode = "import os,signal,subprocess,sys;"
                + "signal.signal(signal.SIGTERM, signal.SIG_IGN);"
                + "open(sys.argv[1],'w').write(str(os.getpid()));"
                + "p=subprocess.Popen([sys.executable,'-c',sys.argv[4],sys.argv[2],sys.argv[3],sys.argv[5]]);p.wait()";
        List<String> command = List.of(
                "python3", "-c", mainCode, mainPid.toString(), childPid.toString(),
                grandPid.toString(), childCode, grandCode);
        ScriptResult result = runBounded(
                globalTimeout ? 60 : 1, globalTimeout ? 3 : 60,
                operationClass, command, null, 300);
        return new ProcessTreeEvidence(result, pid(mainPid), pid(childPid), pid(grandPid));
    }

    private void assertProcessTreeStopped(ProcessTreeEvidence evidence) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (!alive(evidence.mainPid()) && !alive(evidence.childPid())
                    && !alive(evidence.grandPid())) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(alive(evidence.mainPid())).isFalse();
        assertThat(alive(evidence.childPid())).isFalse();
        assertThat(alive(evidence.grandPid())).isFalse();
    }

    private ScriptResult runBounded(int stageTimeout, int globalTimeout, String operationClass,
                                    List<String> command, Path cleanup, int cleanupTimeout)
            throws Exception {
        List<String> args = new ArrayList<>(List.of(
                "python3", BOUNDED_RUNNER.toString(),
                "--timeout-seconds", String.valueOf(stageTimeout),
                "--global-start-epoch", String.valueOf(Instant.now().getEpochSecond()),
                "--global-timeout-seconds", String.valueOf(globalTimeout),
                "--stage", "OFFLINE_TIMEOUT_TEST",
                "--operation-class", operationClass,
                "--poll-seconds", "1",
                "--heartbeat-seconds", "60",
                "--term-grace-seconds", "1"));
        if (cleanup != null) {
            args.addAll(List.of(
                    "--cleanup-script", cleanup.toString(),
                    "--cleanup-timeout-seconds", String.valueOf(cleanupTimeout)));
        }
        args.add("--");
        args.addAll(command);
        return finish(new ProcessBuilder(args).redirectErrorStream(true).start(), Duration.ofSeconds(12));
    }

    private DestroyEvidence runDestroyStub(boolean hangStop, boolean hangDelete) throws Exception {
        Path home = Files.createDirectories(tempDir.resolve(
                "home-" + hangStop + "-" + hangDelete + "-" + System.nanoTime()));
        Path labRoot = Files.createDirectories(home.resolve(".local/share/trade-model-p3h-lab1"));
        Files.writeString(labRoot.resolve("lab-owned-by-p3h-lab1"),
                "P3H-LAB1-USER-AUTH-20260717\n", StandardCharsets.UTF_8);
        Path bin = Files.createDirectories(home.resolve("bin"));
        Path vmState = home.resolve("vm-present");
        Files.writeString(vmState, "yes", StandardCharsets.UTF_8);
        Path commandLog = home.resolve("commands.log");
        Files.writeString(commandLog, "", StandardCharsets.UTF_8);
        executable(bin.resolve("launchctl"), "#!/usr/bin/env bash\nexit 1\n");
        executable(bin.resolve("limactl"), """
                #!/usr/bin/env bash
                set -euo pipefail
                printf '%s\n' "$*" >>"${LIMA_COMMAND_LOG}"
                case "${1:-}" in
                  list)
                    [ -f "${LIMA_VM_STATE_FILE}" ] && printf '%s\n' trade-model-p3h-staging-lab
                    printf '%s\n' unrelated-vm
                    ;;
                  stop)
                    if [ "${LIMA_HANG_STOP:-NO}" = YES ]; then
                      trap '' TERM
                      while :; do sleep 1; done
                    fi
                    ;;
                  delete)
                    if [ "${LIMA_HANG_DELETE:-NO}" = YES ]; then
                      trap '' TERM
                      while :; do sleep 1; done
                    fi
                    rm -f "${LIMA_VM_STATE_FILE}"
                    ;;
                esac
                """);
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/p3h-lab-destroy.sh")
                .redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        env.put("HOME", home.toString());
        env.put("PATH", bin + ":" + env.get("PATH"));
        env.put("P3H_LAB_DESTROY_CONFIRM", "I_CONFIRM_DESTROY_LOCAL_P3H_LAB1");
        env.put("P3H_OFFLINE_TIMEOUT_TEST", "I_CONFIRM_OFFLINE_TIMEOUT_STUBS");
        env.put("LIMA_VM_STATE_FILE", vmState.toString());
        env.put("LIMA_COMMAND_LOG", commandLog.toString());
        env.put("LIMA_HANG_STOP", hangStop ? "YES" : "NO");
        env.put("LIMA_HANG_DELETE", hangDelete ? "YES" : "NO");
        long started = System.nanoTime();
        ScriptResult result = finish(builder.start(), Duration.ofSeconds(15));
        return new DestroyEvidence(result, Duration.ofNanos(System.nanoTime() - started), commandLog);
    }

    private Path executable(String name, String content) throws IOException {
        return executable(tempDir.resolve(name), content);
    }

    private Path executable(Path path, String content) throws IOException {
        Files.writeString(path, content.stripIndent(), StandardCharsets.UTF_8);
        setMode(path, "rwx------");
        return path;
    }

    private void setMode(Path path, String ignored) throws IOException {
        Files.setPosixFilePermissions(path, ignored.startsWith("rwx")
                ? Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE)
                : Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    }

    private ScriptResult finish(Process process, Duration timeout) throws Exception {
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private long pid(Path path) throws Exception {
        for (int attempt = 0; attempt < 20 && !Files.exists(path); attempt++) {
            Thread.sleep(50);
        }
        return Long.parseLong(Files.readString(path).trim());
    }

    private boolean alive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private record ScriptResult(int exitCode, String output) {
    }

    private record ProcessTreeEvidence(ScriptResult result, long mainPid, long childPid, long grandPid) {
    }

    private record DestroyEvidence(ScriptResult result, Duration elapsed, Path commandLog) {
    }
}
