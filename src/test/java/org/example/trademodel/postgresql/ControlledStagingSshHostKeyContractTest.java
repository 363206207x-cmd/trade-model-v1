package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingSshHostKeyContractTest {

    @TempDir
    Path tempDir;

    @Test
    void sshContractPinsHostKeyAndDisablesForwardingAndPasswords() throws Exception {
        String runner = P3hContractTestSupport.read(
                "scripts/controlled-staging-readonly-deployment-p3h.sh");

        assertThat(runner).contains(
                "BatchMode=yes", "StrictHostKeyChecking=yes", "UserKnownHostsFile=",
                "IdentitiesOnly=yes", "IdentityAgent=none", "ForwardAgent=no",
                "ForwardX11=no", "ClearAllForwardings=yes", "PasswordAuthentication=no",
                "KbdInteractiveAuthentication=no", "ConnectTimeout=10",
                "ssh-keyscan -T 10", "ssh-keygen -lf", "BLOCKED_SSH_HOST_KEY_MISMATCH");
        assertThat(runner).doesNotContain("StrictHostKeyChecking=no", "UserKnownHostsFile=/dev/null");
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
}
