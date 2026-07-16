package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingP3HPreflightContractTest {

    @TempDir
    Path tempDir;

    @Test
    void missingConfirmationBlocksBeforeNetworkOrSecretAccess() throws Exception {
        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(Map.of());

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_MISSING_CONTROLLED_STAGING_INPUT",
                "SERVER_ACCESS: NOT_ATTEMPTED",
                "SECRET_ACCESS: NOT_ATTEMPTED",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
        assertThat(result.output()).doesNotContain("stage.example", "ssh-keyscan");
    }

    @Test
    void missingAttestationBlocksBeforeNetworkOrSecretAccess() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.remove("P3H_SERVER_ATTESTATION_FILE");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_MISSING_CONTROLLED_STAGING_INPUT",
                "SERVER_ACCESS: NOT_ATTEMPTED",
                "SECRET_ACCESS: NOT_ATTEMPTED");
        assertThat(result.output()).doesNotContain("ssh-keyscan", "ssh ");
    }

    @Test
    void nonStagingAttestationFailsBeforeNetwork() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        Path attestation = Path.of(inputs.get("P3H_SERVER_ATTESTATION_FILE"));
        String invalid = Files.readString(attestation, StandardCharsets.UTF_8)
                .replace("ENVIRONMENT_CLASS=CONTROLLED_STAGING", "ENVIRONMENT_CLASS=PRODUCTION");
        Files.writeString(attestation, invalid, StandardCharsets.UTF_8);

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_INVALID_STAGING_ATTESTATION",
                "SERVER_ACCESS: NOT_ATTEMPTED");
    }

    @Test
    void productionIndicatorFailsBeforeAttestationOrNetwork() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.put("P3H_STAGING_HOSTNAME", "production.example.invalid");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_PRODUCTION_INDICATOR");
        assertThat(result.output()).doesNotContain(inputs.get("P3H_STAGING_HOSTNAME"));
    }

    @Test
    void missingApprovalReferenceFails() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        replaceAttestation(inputs, "APPROVAL_REFERENCE=P3H-OFFLINE-20260716-01\n", "");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_INVALID_STAGING_ATTESTATION");
        assertThat(result.output()).contains("SERVER_ACCESS: NOT_ATTEMPTED");
    }

    @Test
    void duplicateOwnerReferenceFails() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        replaceAttestation(inputs, "SERVER_OWNER_REFERENCE=SRV-OWN-20260716-01\n",
                "SERVER_OWNER_REFERENCE=SRV-OWN-20260716-01\n"
                        + "SERVER_OWNER_REFERENCE=SRV-OWN-20260716-02\n");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_INVALID_STAGING_ATTESTATION");
    }

    @Test
    void placeholderOwnerReferenceFails() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.put("P3H_RELEASE_OWNER_REFERENCE", "TBD");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_INVALID_OWNER_REFERENCE");
        assertThat(result.output()).contains("SERVER_ACCESS: NOT_ATTEMPTED");
    }

    @Test
    void parentSymlinkAttestationFails() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        Path realParent = Files.createDirectory(tempDir.resolve("real-attestation-parent"));
        Path realAttestation = realParent.resolve("server.attestation");
        Files.copy(Path.of(inputs.get("P3H_SERVER_ATTESTATION_FILE")), realAttestation);
        Files.setPosixFilePermissions(realAttestation, privatePermissions());
        Path linkedParent = tempDir.resolve("linked-attestation-parent");
        Files.createSymbolicLink(linkedParent, realParent);
        inputs.put("P3H_SERVER_ATTESTATION_FILE",
                linkedParent.resolve("server.attestation").toAbsolutePath().toString());

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_INVALID_STAGING_ATTESTATION");
    }

    @Test
    void parentSymlinkIdentityFails() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        Path realParent = Files.createDirectory(tempDir.resolve("real-identity-parent"));
        Path realIdentity = realParent.resolve("identity");
        Files.copy(Path.of(inputs.get("P3H_SSH_IDENTITY_FILE")), realIdentity);
        Files.setPosixFilePermissions(realIdentity, privatePermissions());
        Path linkedParent = tempDir.resolve("linked-identity-parent");
        Files.createSymbolicLink(linkedParent, realParent);
        inputs.put("P3H_SSH_IDENTITY_FILE",
                linkedParent.resolve("identity").toAbsolutePath().toString());

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_INVALID_SSH_IDENTITY");
    }

    private void replaceAttestation(Map<String, String> inputs, String current, String replacement)
            throws Exception {
        Path attestation = Path.of(inputs.get("P3H_SERVER_ATTESTATION_FILE"));
        Files.writeString(attestation,
                Files.readString(attestation, StandardCharsets.UTF_8).replace(current, replacement),
                StandardCharsets.UTF_8);
    }

    private Set<PosixFilePermission> privatePermissions() {
        return Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    }
}
