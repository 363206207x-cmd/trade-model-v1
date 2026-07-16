package org.example.trademodel.postgresql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

final class P3hContractTestSupport {

    static final Path RUNNER = Path.of(
            "scripts/controlled-staging-readonly-deployment-p3h.sh").toAbsolutePath();
    static final String CONFIRMATION =
            "I_CONFIRM_AUTHORIZED_NON_PRODUCTION_STAGING_DEPLOYMENT";
    static final String REBOOT_CONFIRMATION =
            "I_CONFIRM_CONTROLLED_STAGING_SERVER_REBOOT";

    private static final Set<String> P3H_ENVIRONMENT = Set.of(
            "P3H_CONFIRM", "P3H_SERVER_ATTESTATION_FILE",
            "P3H_SECRET_BACKEND_ATTESTATION_FILE", "P3H_SSH_HOST",
            "P3H_SSH_PORT", "P3H_SSH_USER", "P3H_SSH_IDENTITY_FILE",
            "P3H_SSH_HOST_KEY_SHA256", "P3H_STAGING_HOSTNAME", "P3H_TLS_MODE",
            "P3H_CA_BUNDLE_FILE", "P3H_SECRET_BACKEND", "P3H_SECRET_MOUNT_DIR",
            "P3H_RELEASE_OWNER_REFERENCE", "P3H_ROLLBACK_OWNER_REFERENCE",
            "P3H_INCIDENT_OWNER_REFERENCE", "P3H_REBOOT_CONFIRM",
            "P3H_KEEP_STAGING_RUNNING");

    private P3hContractTestSupport() {
    }

    static ScriptResult run(Map<String, String> overrides) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", RUNNER.toString());
        builder.directory(Path.of(".").toAbsolutePath().normalize().toFile());
        builder.redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        P3H_ENVIRONMENT.forEach(environment::remove);
        environment.putAll(new HashMap<>(overrides));

        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    static Map<String, String> completeLocalInputs(Path tempDir) throws IOException {
        Path identity = tempDir.resolve("identity");
        Files.writeString(identity, "offline-contract-key-material", StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(identity, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        String hostKey = "SHA256:8JxvMtK5qV6jG9sZ1dR4pN7wC2fL0aY3hB6uE9iQkT0";
        String hostname = "stage.example.invalid";
        Path serverAttestation = tempDir.resolve("server.attestation");
        Files.writeString(serverAttestation, """
                ENVIRONMENT_CLASS=CONTROLLED_STAGING
                PRODUCTION_TRAFFIC=NO
                PRODUCTION_DATABASE=NO
                PRODUCTION_SECRETS=NO
                AUTHORIZED_FOR_P3H=YES
                DISPOSABLE_OR_REBUILDABLE=YES
                LINUX_SERVER=YES
                EXPECTED_SSH_HOST_KEY_SHA256=%s
                EXPECTED_STAGING_HOSTNAME=%s
                SERVER_OWNER_REFERENCE=SRV-OWN-20260716-01
                APPROVAL_REFERENCE=P3H-OFFLINE-20260716-01
                """.formatted(hostKey, hostname), StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(serverAttestation, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        Path secretAttestation = tempDir.resolve("secret.attestation");
        Files.writeString(secretAttestation, """
                SECRET_BACKEND_CLASS=SYSTEMD_CREDENTIALS
                BACKEND_VERSION=systemd-255
                AUTHORIZED_FOR_P3H=YES
                PLAINTEXT_AT_REST=NO
                SECRETS_VERSIONED_OR_ROTATABLE=YES
                SECRET_MOUNT_IS_RUNTIME_ONLY=YES
                SECRET_OWNER_REFERENCE=SEC-OWN-20260716-01
                ROTATION_ALLOWED=YES
                """, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(secretAttestation, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        String identityPath = identity.toRealPath().toString();
        String serverAttestationPath = serverAttestation.toRealPath().toString();
        String secretAttestationPath = secretAttestation.toRealPath().toString();

        Map<String, String> inputs = new HashMap<>();
        inputs.put("P3H_CONFIRM", CONFIRMATION);
        inputs.put("P3H_SERVER_ATTESTATION_FILE", serverAttestationPath);
        inputs.put("P3H_SECRET_BACKEND_ATTESTATION_FILE", secretAttestationPath);
        inputs.put("P3H_SSH_HOST", "stage.example.invalid");
        inputs.put("P3H_SSH_PORT", "22");
        inputs.put("P3H_SSH_USER", "p3h-deploy");
        inputs.put("P3H_SSH_IDENTITY_FILE", identityPath);
        inputs.put("P3H_SSH_HOST_KEY_SHA256", hostKey);
        inputs.put("P3H_STAGING_HOSTNAME", hostname);
        inputs.put("P3H_TLS_MODE", "PUBLIC_CA");
        inputs.put("P3H_SECRET_BACKEND", "SYSTEMD_CREDENTIALS");
        inputs.put("P3H_SECRET_MOUNT_DIR", "/run/trade-model-p3h/secrets");
        inputs.put("P3H_RELEASE_OWNER_REFERENCE", "REL-OWN-20260716-01");
        inputs.put("P3H_ROLLBACK_OWNER_REFERENCE", "RBK-OWN-20260716-01");
        inputs.put("P3H_INCIDENT_OWNER_REFERENCE", "INC-OWN-20260716-01");
        inputs.put("P3H_REBOOT_CONFIRM", REBOOT_CONFIRMATION);
        inputs.put("P3H_KEEP_STAGING_RUNNING", "NO");
        return inputs;
    }

    static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    record ScriptResult(int exitCode, String output) {
    }
}
