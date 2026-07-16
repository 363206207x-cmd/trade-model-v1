package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingSecretStoreContractTest {

    @TempDir
    Path tempDir;

    @Test
    void onlyApprovedBackendsAndRuntimeMountsAreAccepted() throws Exception {
        String runner = P3hContractTestSupport.read(
                "scripts/controlled-staging-readonly-deployment-p3h.sh");

        assertThat(runner).contains(
                "SYSTEMD_CREDENTIALS)", "BLOCKED_BACKEND_NOT_IMPLEMENTED",
                "BLOCKED_UNSUPPORTED_SECRET_BACKEND", "BLOCKED_PLAINTEXT_SECRET_DIRECTORY",
                "case \"${P3H_SECRET_MOUNT_DIR}\"", "/run/*");
        assertThat(runner).doesNotContain("cat \"${P3H_SERVER_ATTESTATION_FILE}\"");

        String remotePreflight = P3hContractTestSupport.read("scripts/p3h-remote-preflight.sh");
        assertThat(remotePreflight).contains(
                "app_database_password_v1", "app_database_password_v2",
                "app_admin_password_v1", "app_admin_password_v2",
                "backup_reader_password", "recovery_owner_password",
                "binance_nonfunctional_key", "binance_nonfunctional_secret",
                "tls_certificate", "tls_private_key", "tls_ca_certificate",
                "BLOCKED_SECRET_FILE_PERMISSIONS", "BLOCKED_SECRET_FILE_OWNER",
                "SECRET_FILE_CONTRACT: PASS_NAMES_OWNERS_PERMISSIONS");
        assertThat(remotePreflight).doesNotContain("cat \"${secret_path}\"");
    }

    @Test
    void unsupportedBackendFailsBeforeAnySecretOrNetworkAccess() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.put("P3H_SECRET_BACKEND", "LOCAL_PLAINTEXT_FILE");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains(
                "P3H_RESULT: BLOCKED_UNSUPPORTED_SECRET_BACKEND",
                "SERVER_ACCESS: NOT_ATTEMPTED",
                "SECRET_ACCESS: NOT_ATTEMPTED");
    }

    @Test
    void nonRuntimeSecretDirectoryFailsClosed() throws Exception {
        Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
        inputs.put("P3H_SECRET_MOUNT_DIR", "/opt/trade-model-p3h/secrets");

        P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

        assertThat(result.output()).contains("P3H_RESULT: BLOCKED_PLAINTEXT_SECRET_DIRECTORY");
    }

    @Test
    void unimplementedBackendsFailClosedBeforeNetworkAccess() throws Exception {
        for (String backend : List.of("SOPS_AGE_TMPFS", "VAULT_AGENT", "CLOUD_SECRET_MANAGER_AGENT")) {
            Map<String, String> inputs = P3hContractTestSupport.completeLocalInputs(tempDir);
            inputs.put("P3H_SECRET_BACKEND", backend);

            P3hContractTestSupport.ScriptResult result = P3hContractTestSupport.run(inputs);

            assertThat(result.output()).contains(
                    "P3H_RESULT: BLOCKED_BACKEND_NOT_IMPLEMENTED",
                    "SERVER_ACCESS: NOT_ATTEMPTED",
                    "SECRET_ACCESS: NOT_ATTEMPTED");
        }
    }

    @Test
    void runtimeDirectoryOnPersistentFilesystemFails() throws Exception {
        String preflight = P3hContractTestSupport.read("scripts/p3h-remote-preflight.sh");
        String adapter = P3hContractTestSupport.read(
                "deploy/p3h/p3h-systemd-credentials-adapter.sh");

        assertThat(preflight).contains(
                "findmnt -n -T \"${secret_mount_dir}\" -o FSTYPE",
                "tmpfs|ramfs", "BLOCKED_SECRET_MOUNT_NOT_RUNTIME");
        assertThat(adapter).contains(
                "findmnt -n -T /run -o FSTYPE", "tmpfs|ramfs",
                "BLOCKED_RUNTIME_FILESYSTEM");
    }

    @Test
    void systemdUsesCredentialMountAndRuntimeMaterialization() throws Exception {
        String unit = P3hContractTestSupport.read("deploy/p3h/trade-model-p3h.service.template");
        String adapter = P3hContractTestSupport.read(
                "deploy/p3h/p3h-systemd-credentials-adapter.sh");

        assertThat(unit).contains(
                "LoadCredentialEncrypted=", "RuntimeDirectory=trade-model-p3h",
                "RuntimeDirectoryMode=0700", "p3h-systemd-credentials-adapter.sh");
        assertThat(unit).doesNotContain("docker compose up");
        assertThat(adapter).contains(
                "CREDENTIALS_DIRECTORY", "/run/credentials/*",
                "findmnt -n -T /run -o FSTYPE");
    }
}
