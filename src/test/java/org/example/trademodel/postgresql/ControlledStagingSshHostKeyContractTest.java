package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingSshHostKeyContractTest {

    @TempDir
    Path tempDir;

    @Test
    void sshContractPinsHostKeyAndDisablesForwardingAndPasswords() throws Exception {
        String runner = P3hContractTestSupport.read(
                "scripts/controlled-staging-readonly-deployment-p3h.sh");
        String filter = P3hContractTestSupport.read("scripts/p3h-filter-known-hosts.sh");

        assertThat(runner).contains(
                "BatchMode=yes", "StrictHostKeyChecking=yes", "UserKnownHostsFile=",
                "IdentitiesOnly=yes", "IdentityAgent=none", "ForwardAgent=no",
                "ForwardX11=no", "ClearAllForwardings=yes", "PasswordAuthentication=no",
                "KbdInteractiveAuthentication=no", "ConnectTimeout=10",
                "ssh-keyscan -T 10", "known_hosts.candidates", "known_hosts.approved",
                "p3h-filter-known-hosts.sh", "BLOCKED_SSH_HOST_KEY_MISMATCH");
        assertThat(filter).contains("ssh-keygen -lf", "match_count", "mv \"${temporary_output}\"");
        assertThat(runner).doesNotContain("StrictHostKeyChecking=no", "UserKnownHostsFile=/dev/null");
    }

    @Test
    void multipleScannedKeysTrustOnlyApprovedLine() throws Exception {
        HostKey approved = generateHostKey("approved");
        HostKey unapproved = generateHostKey("unapproved");
        Path candidates = tempDir.resolve("candidates");
        Path filtered = tempDir.resolve("filtered");
        Files.writeString(candidates, approved.knownHostsLine() + "\n"
                + unapproved.knownHostsLine() + "\n", StandardCharsets.UTF_8);

        ScriptResult result = runFilter(candidates, approved.fingerprint(), filtered);

        assertThat(result.exitCode()).isZero();
        assertThat(Files.readAllLines(filtered, StandardCharsets.UTF_8))
                .containsExactly(approved.knownHostsLine());
    }

    @Test
    void unapprovedSecondKeyNeverEntersKnownHosts() throws Exception {
        HostKey approved = generateHostKey("expected");
        HostKey unapproved = generateHostKey("unexpected");
        Path candidates = tempDir.resolve("multi-candidates");
        Path filtered = tempDir.resolve("approved-only");
        Files.writeString(candidates, approved.knownHostsLine() + "\n"
                + unapproved.knownHostsLine() + "\n", StandardCharsets.UTF_8);

        ScriptResult result = runFilter(candidates, approved.fingerprint(), filtered);

        assertThat(result.exitCode()).isZero();
        assertThat(Files.readString(filtered, StandardCharsets.UTF_8))
                .contains(approved.knownHostsLine())
                .doesNotContain(unapproved.knownHostsLine());
    }

    @Test
    void zeroMatchingHostKeysFails() throws Exception {
        HostKey approved = generateHostKey("approved-zero");
        HostKey unapproved = generateHostKey("unapproved-zero");
        Path candidates = tempDir.resolve("zero-candidates");
        Path filtered = tempDir.resolve("zero-output");
        Files.writeString(candidates, unapproved.knownHostsLine() + "\n", StandardCharsets.UTF_8);

        ScriptResult result = runFilter(candidates, approved.fingerprint(), filtered);

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("BLOCKED_ZERO_MATCHES");
        assertThat(filtered).doesNotExist();
    }

    @Test
    void duplicateMatchingHostKeysFailsClosed() throws Exception {
        HostKey approved = generateHostKey("approved-duplicate");
        Path candidates = tempDir.resolve("duplicate-candidates");
        Path filtered = tempDir.resolve("duplicate-output");
        Files.writeString(candidates, approved.knownHostsLine() + "\n"
                + approved.knownHostsLine() + "\n", StandardCharsets.UTF_8);

        ScriptResult result = runFilter(candidates, approved.fingerprint(), filtered);

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("BLOCKED_MULTIPLE_MATCHES");
        assertThat(filtered).doesNotExist();
    }

    @Test
    void sshUsesFilteredKnownHostsFile() throws Exception {
        String runner = P3hContractTestSupport.read(
                "scripts/controlled-staging-readonly-deployment-p3h.sh");

        assertThat(runner).contains(
                "known_hosts=\"${TMP_DIR}/known_hosts.approved\"",
                "UserKnownHostsFile=${known_hosts}");
        assertThat(runner).doesNotContain("UserKnownHostsFile=${known_hosts_candidates}");
    }

    @Test
    void overlyBroadIdentityPermissionsFailBeforeNetwork() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        Path identity = Path.of(inputs.get("P3H_SSH_IDENTITY_FILE"));
        Files.setPosixFilePermissions(identity, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ));

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_SSH_IDENTITY_PERMISSIONS");
        assertThat(result.output()).doesNotContain(identity.toString());
    }

    @Test
    void attestedHostKeyMismatchFailsBeforeNetwork() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        Path attestation = Path.of(inputs.get("P3H_SERVER_ATTESTATION_FILE"));
        String content = Files.readString(attestation, StandardCharsets.UTF_8)
                .replace(inputs.get("P3H_SSH_HOST_KEY_SHA256"),
                        "SHA256:differentOfflinePinnedHostKey");
        Files.writeString(attestation, content, StandardCharsets.UTF_8);

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_INVALID_STAGING_ATTESTATION",
                "SERVER_ACCESS: NOT_ATTEMPTED");
        assertThat(result.output()).doesNotContain("ssh-keyscan");
    }

    private HostKey generateHostKey(String name) throws Exception {
        Path key = tempDir.resolve(name);
        Process generate = new ProcessBuilder(
                "ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-f", key.toString())
                .redirectErrorStream(true)
                .start();
        assertThat(generate.waitFor(10, TimeUnit.SECONDS)).isTrue();
        assertThat(generate.exitValue()).isZero();

        String[] publicKey = Files.readString(Path.of(key + ".pub"), StandardCharsets.UTF_8)
                .trim().split("\\s+");
        String line = "[stage.example.invalid]:22 " + publicKey[0] + " " + publicKey[1];

        Process inspect = new ProcessBuilder(
                "ssh-keygen", "-lf", Path.of(key + ".pub").toString(), "-E", "sha256")
                .redirectErrorStream(true)
                .start();
        assertThat(inspect.waitFor(10, TimeUnit.SECONDS)).isTrue();
        String output = new String(inspect.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(inspect.exitValue()).isZero();
        String fingerprint = output.trim().split("\\s+")[1];
        return new HostKey(line, fingerprint);
    }

    private ScriptResult runFilter(Path candidates, String fingerprint, Path output)
            throws Exception {
        Process process = new ProcessBuilder(
                "bash", "scripts/p3h-filter-known-hosts.sh",
                candidates.toString(), fingerprint, output.toString())
                .redirectErrorStream(true)
                .start();
        assertThat(process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS))
                .isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record HostKey(String knownHostsLine, String fingerprint) {
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
