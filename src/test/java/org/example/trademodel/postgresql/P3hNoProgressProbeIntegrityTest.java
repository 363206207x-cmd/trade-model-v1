package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class P3hNoProgressProbeIntegrityTest {

    @TempDir
    Path tempDir;

    @Test
    void dockerSystemDfHangCannotBlockTimeout() throws Exception {
        ProbeResult result = runProbeHarness("SYSTEM_DF", 2, 20);

        assertThat(result.elapsed()).isLessThan(Duration.ofSeconds(10));
        assertThat(result.output()).contains(
                "P3H_PROGRESS_PROBE_STATUS: PROBE_TIMEOUT",
                "BOUNDED_DOCKER_FAILURE: NO_PROGRESS_TIMEOUT",
                "OPERATION_STATUS: 126");
    }

    @Test
    void buildkitDuHangCannotBlockTimeout() throws Exception {
        ProbeResult result = runProbeHarness("BUILDKIT_DU", 2, 20);

        assertThat(result.elapsed()).isLessThan(Duration.ofSeconds(10));
        assertThat(result.output()).contains(
                "P3H_PROGRESS_PROBE_STATUS: FILESYSTEM_PROBE_TIMEOUT",
                "OPERATION_STATUS: 126");
    }

    @Test
    void containerdDuHangCannotBlockTimeout() throws Exception {
        ProbeResult result = runProbeHarness("CONTAINERD_DU", 2, 20);

        assertThat(result.elapsed()).isLessThan(Duration.ofSeconds(10));
        assertThat(result.output()).contains(
                "P3H_PROGRESS_PROBE_STATUS: FILESYSTEM_PROBE_TIMEOUT",
                "OPERATION_STATUS: 126");
    }

    @Test
    void probeTimeoutDoesNotResetProgressClock() throws Exception {
        ProbeResult result = runProbeHarness("SYSTEM_DF", 2, 20);

        assertThat(result.output()).contains("BOUNDED_DOCKER_FAILURE: NO_PROGRESS_TIMEOUT");
        assertThat(result.elapsed()).isLessThan(Duration.ofSeconds(10));
    }

    @Test
    void noProgressTerminatesProcessGroup() throws Exception {
        ProbeResult result = runProbeHarness("SYSTEM_DF", 2, 20);

        assertThat(result.output()).contains("OPERATION_STATUS: 126");
        assertStopped(result.mainPid());
        assertStopped(result.childPid());
    }

    @Test
    void globalTimeoutStillWins() throws Exception {
        ProbeResult result = runProbeHarness("NONE", 20, 2);

        assertThat(result.output()).contains(
                "BOUNDED_DOCKER_FAILURE: GLOBAL_TIMEOUT",
                "OPERATION_STATUS: 125");
        assertThat(result.output()).doesNotContain("BOUNDED_DOCKER_FAILURE: NO_PROGRESS_TIMEOUT");
    }

    private ProbeResult runProbeHarness(String hangingProbe, int noProgressSeconds,
                                        int globalSeconds) throws Exception {
        String remote = Files.readString(
                Path.of("deploy/p3h/lima/p3h-lab-r1-remote.sh"), StandardCharsets.UTF_8);
        int boundary = remote.indexOf("bounded_build_preflight() {");
        assertThat(boundary).isPositive();
        Path bin = Files.createDirectories(tempDir.resolve(
                "bin-" + hangingProbe + "-" + System.nanoTime()));
        installStubs(bin);
        Path operationMainPid = tempDir.resolve(hangingProbe + "-main-" + System.nanoTime());
        Path operationChildPid = tempDir.resolve(hangingProbe + "-child-" + System.nanoTime());
        String operationBody = "NONE".equals(hangingProbe) ? """
                #!/usr/bin/env bash
                printf '%s\n' "$$" >"${OPERATION_MAIN_PID}"
                printf '%s\n' "$$" >"${OPERATION_CHILD_PID}"
                exec sleep 30
                """ : """
                #!/usr/bin/env bash
                trap '' TERM
                printf '%s\n' "$$" >"${OPERATION_MAIN_PID}"
                (
                  trap '' TERM
                  printf '%s\n' "$$" >"${OPERATION_CHILD_PID}"
                  while :; do sleep 1; done
                ) &
                wait
                """;
        Path operation = executable(
                tempDir.resolve("operation-" + System.nanoTime() + ".sh"), operationBody);
        Path output = tempDir.resolve("docker-output-" + System.nanoTime());
        Path harness = executable(tempDir.resolve("probe-harness-" + System.nanoTime() + ".sh"),
                remote.substring(0, boundary) + """

                trap - ERR EXIT
                NO_PROGRESS_TIMEOUT_SECONDS="${TEST_NO_PROGRESS_SECONDS}"
                PROGRESS_PROBE_TIMEOUT_SECONDS=1
                POLL_INTERVAL_SECONDS=1
                HEARTBEAT_INTERVAL_SECONDS=1
                TERM_GRACE_SECONDS=1
                R1_START_EPOCH="$(date +%s)"
                R1_GLOBAL_TIMEOUT_SECONDS="${TEST_GLOBAL_SECONDS}"
                if run_docker_bounded OFFLINE_PROBE_TEST OFFLINE_PROBE \
                    20 test-target "${TEST_DOCKER_OUTPUT}" \
                    bash "${TEST_OPERATION_SCRIPT}"; then
                  operation_status=0
                else
                  operation_status=$?
                fi
                echo "OPERATION_STATUS: ${operation_status}"
                echo "BOUNDED_DOCKER_FAILURE: ${BOUNDED_DOCKER_FAILURE}"
                exit 0
                """);

        ProcessBuilder builder = new ProcessBuilder("bash", harness.toString())
                .redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        env.put("PATH", bin + ":" + env.get("PATH"));
        env.put("HANG_PROBE", hangingProbe);
        env.put("TEST_NO_PROGRESS_SECONDS", String.valueOf(noProgressSeconds));
        env.put("TEST_GLOBAL_SECONDS", String.valueOf(globalSeconds));
        env.put("TEST_DOCKER_OUTPUT", output.toString());
        env.put("TEST_OPERATION_SCRIPT", operation.toString());
        env.put("OPERATION_MAIN_PID", operationMainPid.toString());
        env.put("OPERATION_CHILD_PID", operationChildPid.toString());
        long started = System.nanoTime();
        Process process = builder.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(3, TimeUnit.SECONDS);
        }
        String processOutput = new String(
                process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(finished).as("probe harness did not finish\n%s", processOutput).isTrue();
        assertThat(process.exitValue()).as(processOutput).isZero();
        return new ProbeResult(
                processOutput,
                Duration.ofNanos(System.nanoTime() - started),
                readPid(operationMainPid),
                readPid(operationChildPid));
    }

    private void installStubs(Path bin) throws Exception {
        executable(bin.resolve("timeout"), """
                #!/usr/bin/env python3
                import os, signal, subprocess, sys, time
                args = sys.argv[1:]
                while args and args[0].startswith("--"):
                    args.pop(0)
                seconds = float(args.pop(0).rstrip("s"))
                process = subprocess.Popen(args, start_new_session=True)
                try:
                    raise SystemExit(process.wait(timeout=seconds))
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGTERM)
                    time.sleep(0.2)
                    if process.poll() is None:
                        os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
                    raise SystemExit(124)
                """);
        executable(bin.resolve("setsid"), """
                #!/usr/bin/env python3
                import os, sys
                os.setsid()
                os.execvp(sys.argv[1], sys.argv[1:])
                """);
        executable(bin.resolve("docker"), """
                #!/usr/bin/env bash
                set -euo pipefail
                hang() { trap '' TERM; while :; do sleep 1; done; }
                case "${1:-}:${2:-}" in
                  image:inspect)
                    if [ "${HANG_PROBE}" = IMAGE_INSPECT ]; then hang; fi
                    exit 1
                    ;;
                  system:df)
                    if [ "${HANG_PROBE}" = SYSTEM_DF ]; then hang; fi
                    printf '%s\n' 'Images|1|1MB'
                    ;;
                  info:*) exit 0 ;;
                  *) exit 0 ;;
                esac
                """);
        executable(bin.resolve("sudo"), "#!/usr/bin/env bash\nexec \"$@\"\n");
        executable(bin.resolve("du"), """
                #!/usr/bin/env bash
                set -euo pipefail
                hang() { trap '' TERM; while :; do sleep 1; done; }
                case "$*" in
                  *buildkit*) if [ "${HANG_PROBE}" = BUILDKIT_DU ]; then hang; fi ;;
                  *containerd*) if [ "${HANG_PROBE}" = CONTAINERD_DU ]; then hang; fi ;;
                esac
                printf '%s\n' '1 /stub'
                """);
        executable(bin.resolve("sha256sum"), """
                #!/usr/bin/env bash
                cat >/dev/null
                printf '%s\n' 'fixed-probe-fingerprint  -'
                """);
    }

    private Path executable(Path path, String content) throws IOException {
        Files.writeString(path, content.stripIndent(), StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
        return path;
    }

    private long readPid(Path path) throws Exception {
        for (int attempt = 0; attempt < 30 && !Files.exists(path); attempt++) {
            Thread.sleep(100);
        }
        return Long.parseLong(Files.readString(path).trim());
    }

    private void assertStopped(long pid) throws Exception {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                Thread.sleep(100);
            } else {
                return;
            }
        }
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
    }

    private record ProbeResult(String output, Duration elapsed, long mainPid, long childPid) {
    }
}
