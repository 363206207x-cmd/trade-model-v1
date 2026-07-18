package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingLocalLimaLabContractTest {

    @TempDir
    Path tempDir;

    @Test
    void bootstrapUsesDedicatedIdentityWithoutSearchingExistingSshMaterial() throws Exception {
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");

        assertThat(bootstrap).contains(
                "identity/p3h-lab1-ed25519",
                "ssh-keygen -q -t ed25519",
                "P3H-LAB1-USER-AUTH-20260717");
        assertThat(bootstrap).doesNotContain(
                "${HOME}/.ssh", "~/.ssh", "find ~/.ssh", "find \"${HOME}/.ssh\"");
    }

    @Test
    void limaTemplateIsLinuxVmWithNoRepositoryMount() throws Exception {
        String template = read("deploy/p3h/lima/p3h-lab.yaml");

        assertThat(template).contains(
                "template:debian-12", "cpus: 4", "memory: 8GiB",
                "disk: 40GiB", "mounts: []", "systemctl", "systemd-creds",
                "https://download.docker.com/linux/debian", "docker-compose-plugin",
                "--no-install-recommends",
                "sudo docker info", "sudo docker compose version");
        assertThat(template).doesNotContain(".runtime", ".env", "dump", "backup");
    }

    @Test
    void bootstrapIsBoundedAndCleansOnlyOwnedPartialLab() throws Exception {
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");

        assertThat(bootstrap).contains(
                "run_bounded 5100 limactl start",
                "--timeout=75m",
                "P3H_LAB_DESTROY_CONFIRM=I_CONFIRM_DESTROY_LOCAL_P3H_LAB1",
                "P3H-LAB1-USER-AUTH-20260717",
                "BLOCKED_LIMA_START_TIMEOUT_OR_FAILURE",
                "P3H_LAB_BOOTSTRAP_FAILED_STAGE",
                "sed -n '/^P3H_LAB_PROVISION:/p'");
    }

    @Test
    void localNtpUsesOwnedHostOnlySourceAndIsRemovedWithTheLab() throws Exception {
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");
        String provision = read("deploy/p3h/lima/p3h-lab-provision-linux.sh");
        String destroy = read("scripts/p3h-lab-destroy.sh");
        String server = read("scripts/p3h-lab-local-ntp-server.py");

        assertThat(bootstrap).contains(
                "launchctl submit", "org.example.trademodel.p3h-lab1-ntp",
                "install -m 0700 \"${LOCAL_NTP_SERVER}\" \"${local_ntp_runtime}\"",
                "--bind=0.0.0.0",
                "--owner-token=P3H-LAB1-USER-AUTH-20260717");
        assertThat(provision).contains(
                "NTP=host.lima.internal", "FallbackNTP=",
                "NTPSynchronized");
        assertThat(destroy).contains(
                "BLOCKED_UNOWNED_LOCAL_NTP_PROCESS",
                "launchctl remove \"${LOCAL_NTP_LABEL}\"");
        assertThat(server).contains(
                "P3H_LAB_LOCAL_NTP: READY",
                "127.0.0.0/8", "192.168.5.0/24");
        assertThat(server).doesNotContain("print(request", "print(address");
        assertThat(server).contains("server.recvfrom(512)");
    }

    @Test
    void externalTargetNeverFallsBackToLocalLab() throws Exception {
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");

        assertThat(runner).contains(
                "AUTHORIZED_EXTERNAL_STAGING)",
                "controlled-staging-readonly-deployment-p3h.sh",
                "BLOCKED_LAB_ATTESTATION_FOR_EXTERNAL_TARGET");
        assertThat(runner.indexOf("AUTHORIZED_EXTERNAL_STAGING)"))
                .isLessThan(runner.indexOf("LOCAL_LIMA_LAB)"));
    }

    @Test
    void localLabCanNeverClaimExternalStagingPass() throws Exception {
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(runner + remote).contains("REAL_EXTERNAL_STAGING_STATUS: NOT_RUN");
        assertThat(runner + remote).doesNotContain(
                "REAL_EXTERNAL_STAGING_STATUS: PASS",
                "P3H_RESULT: PASS_EXTERNAL_STAGING");
    }

    @Test
    void defaultRunnerIsNoCallAndFailClosed() throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "bash", "scripts/controlled-staging-readonly-deployment-p3h-r1.sh");
        builder.redirectErrorStream(true);
        builder.environment().remove("P3H_TARGET_CLASS");
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);

        assertThat(finished).isTrue();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.exitValue()).isZero();
        assertThat(output).contains(
                "SKIPPED_TARGET_CLASS_REQUIRED",
                "REAL_EXTERNAL_STAGING_STATUS: NOT_RUN",
                "P4_ALLOWED: NO",
                "PRODUCTION_READINESS: BLOCKED");
    }

    @Test
    void secretsUseSystemdCredentialsAndFileInputsOnly() throws Exception {
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");
        String holder = read("deploy/p3h/lima/p3h-lab-credential-holder.service");
        String materializer = read("deploy/p3h/lima/p3h-lab-materialize-credentials.sh");
        String mount = read("deploy/p3h/lima/p3h-lab-credential-runtime.mount.template");
        String seal = read("deploy/p3h/lima/p3h-lab-credential-seal.service");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(bootstrap).contains(
                "systemd-creds encrypt", "PLAINTEXT_SECRET_AT_REST: NO",
                "sudo install -d -m 0755 /usr/local/libexec",
                "P3H_LAB_CREDENTIAL_UNIT_STATUS:",
                "--property=ExecMainStatus",
                "encrypt_stream tls_certificate tls_certificate_v1",
                "encrypt_stream tls_private_key tls_private_key_v1",
                "encrypt_stream tls_certificate tls_certificate_v2_active",
                "encrypt_stream tls_private_key tls_private_key_v2_active");
        assertThat(bootstrap).doesNotContain(
                "encrypt_stream tls_certificate_v1 <",
                "encrypt_stream tls_private_key_v1 <");
        assertThat(holder).contains(
                "LoadCredentialEncrypted=", "User=p3h-deploy",
                "RequiresMountsFor=/run/credentials/p3hlab1",
                "NoNewPrivileges=yes", "UMask=0077");
        assertThat(holder).doesNotContain(
                "ProtectSystem=strict", "ReadWritePaths=");
        assertThat(bootstrap + remote).contains(
                "systemd-escape --path --suffix=mount");
        assertThat(materializer).contains(
                "${CREDENTIALS_DIRECTORY}", "/run/credentials/p3hlab1",
                "install -m 0400");
        assertThat(mount).contains(
                "Type=tmpfs", "mode=0700", "nodev,nosuid,noexec");
        assertThat(seal).contains(
                "mount -t tmpfs -o remount,ro,nodev,nosuid,noexec tmpfs",
                "CAP_SYS_ADMIN", "NoNewPrivileges=yes");
        assertThat(seal).doesNotContain("ProtectHome=yes");
        assertThat(remote).contains(
                "PROD_DATASOURCE_PASSWORD_FILE=/credentials/backup_reader_password",
                "RESTORE_DATASOURCE_PASSWORD_FILE=/credentials/recovery_owner_password");
        assertThat(remote).doesNotContain(
                "PROD_DATASOURCE_PASSWORD=", "RESTORE_DATASOURCE_PASSWORD=",
                "--env APP_ADMIN_PASSWORD=", "--env OPENAI_API_KEY=");
    }

    @Test
    void tlsNeverUsesInsecureCurlAndPinsApprovedHostname() throws Exception {
        String all = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh")
                + read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(all).contains(
                "--cacert", "--resolve", "trade-staging.lab.test",
                "-verify_hostname");
        assertThat(all).doesNotContain("curl -k", "--insecure");
    }

    @Test
    void hostKeyRequiresConsoleAndNetworkAgreement() throws Exception {
        String bootstrap = read("scripts/p3h-lab-bootstrap-macos.sh");
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");

        assertThat(bootstrap + runner).contains(
                "limactl shell", "ssh-keyscan", "p3h-filter-known-hosts.sh",
                "BLOCKED_CONSOLE_NETWORK_HOST_KEY_MISMATCH",
                "BLOCKED_REBOOT_NETWORK_HOST_KEY");
    }

    @Test
    void exactSourceArchiveMustMatchRemoteHashAndImageRevision() throws Exception {
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(runner).contains(
                "git -C \"${ROOT_DIR}\" archive", "archive_sha", "remote_sha",
                "BLOCKED_REMOTE_ARCHIVE_SHA");
        assertThat(remote).contains(
                "org.opencontainers.image.revision", "BLOCKED_IMAGE_REVISION",
                "expected_release=\"/opt/trade-model-p3h/releases/${SOURCE_HEAD}\"",
                "[ -L \"${ROOT}\" ]",
                "[ \"$(readlink -f \"${ROOT}\")\" = \"${expected_release}\" ]");
    }

    @Test
    void remotePreflightPreservesInputAcrossBoundedBackgroundExecution() throws Exception {
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");

        assertThat(runner).contains(
                "run_bounded_with_stdin()",
                "\"$@\" <\"${input_file}\" &",
                "run_bounded_with_stdin 180 \"${ROOT_DIR}/scripts/p3h-remote-preflight.sh\"",
                "BLOCKED_REMOTE_PREFLIGHT_EVIDENCE");
        assertThat(runner).doesNotContain(
                "<\"${ROOT_DIR}/scripts/p3h-remote-preflight.sh\" >\"${remote_preflight}\"");
    }

    @Test
    void remoteFailureEvidencePreservesOnlySanitizedReasonCode() throws Exception {
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");
        String composeStart = read("deploy/p3h/p3h-compose-start.sh");

        assertThat(runner).contains(
                "persist_remote_failure_evidence()",
                "$2 ~ /^BLOCKED_[A-Z0-9_]+$/",
                "P3H_REMOTE_FAILURE_REASON: ${reason}",
                "SANITIZED_FAILURE_EVIDENCE_SHA256: ${failure_sha}",
                "P3H_REMOTE_STAGE: BLOCKED_REMOTE_STAGE_TIMEOUT",
                "run_remote_stage 12600 INITIAL_DEPLOY BLOCKED_INITIAL_DEPLOY");
        assertThat(runner).doesNotContain(
                "cat \"${stage_output}\" >&2",
                "tail \"${stage_output}\"");
        assertThat(remote).contains(
                "trap unexpected_failure ERR",
                "BLOCKED_UNEXPECTED_${CURRENT_REMOTE_STEP}",
                "IMAGE_BUILD_ATTEMPT_TIMEOUT_SECONDS=3600",
                "IMAGE_BUILD_MAX_ATTEMPTS=2",
                "IMAGE_BUILD_FAILURE_CATEGORY=UNKNOWN",
                "P3H_IMAGE_BUILD_RETRY: BOUNDED_CACHE_REUSE_${IMAGE_BUILD_FAILURE_CATEGORY}",
                "TIMEOUT|NETWORK|RATE_LIMIT",
                "MAVEN|STORAGE|UNKNOWN",
                "BLOCKED_IMAGE_BUILD_TIMEOUT",
                "service_start_failure_reason()",
                "$1 == \"P3H_COMPOSE_FAILED_STEP\"",
                "$1 == \"P3H_COMPOSE_START\"",
                "detail=ARCHITECTURE_MISMATCH",
                "no (flyway )?database( plugin)? found to handle",
                "detail=UNSUPPORTED_DATABASE",
                "detail=DATABASE_PERMISSION",
                "detail=MIGRATION_FILES",
                "detail=MIGRATION_SQL",
                "rm -f \"${journal_file}\"",
                "fingerprint_failure_reason()",
                "BLOCKED_INITIAL_FINGERPRINT_$(fingerprint_failure_reason",
                "BLOCKED_STEADY_FINGERPRINT_$(fingerprint_failure_reason",
                "BLOCKED_CONTENT_AFTER_REBOOT_$(fingerprint_failure_reason",
                "return 81",
                "return 85",
                "current-state-clone-fingerprint.sql",
                "current-state-clone-content-fingerprint.sql",
                "p3h_backup_reader",
                "validate_prebuild_compose_config()",
                "P3H_COMPOSE_CONFIG_CHECK_ONLY=true",
                "BLOCKED_PREBUILD_${config_status}",
                "validate_systemd_sandbox_compose_config()",
                "systemd-run --unit=\"${transient_unit}\" --wait --collect --pipe --quiet",
                "--property=SupplementaryGroups=docker",
                "--property=ProtectSystem=strict",
                "--property=ProtectHome=yes",
                "--property=RuntimeDirectory=trade-model-p3h-prebuild",
                "--property=Environment=DOCKER_CONFIG=/run/trade-model-p3h-prebuild/docker-config",
                "BLOCKED_PREBUILD_SYSTEMD_${config_status}",
                "start_service_or_block INITIAL_SERVICE",
                "start_service_or_block STEADY_SERVICE");
        assertThat(remote).doesNotContain(
                "IMAGE_BUILD_MAX_ATTEMPTS=3",
                "while true",
                "cat \"${build_log}\"",
                "tail \"${build_log}\"",
                "cat \"${journal_file}\"",
                "tail \"${journal_file}\"",
                "row_to_json(t)",
                "journalctl --unit=trade-model-p3h.service --boot --no-pager -o cat >&2");
        assertThat(composeStart).contains(
                "P3H_COMPOSE_CONFIG_CHECK_ONLY:-false",
                "P3H_COMPOSE_CONFIG_CHECK: PASS",
                "BLOCKED_COMPOSE_CONFIG_${compose_config_category}",
                "compose_config_category=FILE_PERMISSION",
                "compose_config_category=SECRET_FILE_PERMISSION",
                "compose_config_category=DOCKER_SOCKET_PERMISSION",
                "compose_config_category=DOCKER_CONFIG_PERMISSION",
                "compose_config_category=RELEASE_FILE_PERMISSION",
                "compose_config_category=MISSING_FILE",
                "compose_config_category=INVALID_SCHEMA");
        assertThat(composeStart).doesNotContain(
                "cat \"${compose_config_log}\"",
                "tail \"${compose_config_log}\"");
        assertThat(read("deploy/p3h/lima/trade-model-p3h-lab.service.template"))
                .contains(
                        "SupplementaryGroups=docker",
                        "RuntimeDirectory=trade-model-p3h",
                        "Environment=DOCKER_CONFIG=/run/trade-model-p3h/docker-config");
        assertThat(composeStart).contains(
                "install -d -m 0700 \"${DOCKER_CONFIG}\"",
                "BLOCKED_DOCKER_CONFIG_SCOPE",
                "BLOCKED_DOCKER_CONFIG_DIRECTORY");
    }

    @Test
    void rebootMustBeActualVmRebootAndPreserveOnlyV2() throws Exception {
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(runner).contains(
                "sudo systemctl reboot", "/proc/sys/kernel/random/boot_id",
                "BLOCKED_BOOT_ID_UNCHANGED");
        assertThat(remote).contains(
                "PASS_ACTUAL_LINUX_VM_REBOOT",
                "activate_tls_v2_credentials()",
                "BLOCKED_TLS_CREDENTIAL_ACTIVATION",
                "tls_certificate_v2_active.cred",
                "cmp -s \"${CREDENTIALS}/tls_certificate\" \"${CREDENTIALS}/tls_certificate_v2\"",
                "V2_DATABASE_AFTER_REBOOT: PASS",
                "V1_DATABASE_AFTER_REBOOT: DENIED",
                "V2_ADMIN_AFTER_REBOOT: PASS",
                "V1_ADMIN_AFTER_REBOOT: DENIED");
        assertThat(runner + remote).doesNotContain("SERVER_REBOOT: PASS_PHYSICAL_SERVER");
    }

    @Test
    void rateLimitProbeMustExceedConfiguredRequestBudget() throws Exception {
        String compose = read("deploy/p3h/docker-compose.p3h.yml");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(compose).contains("TRADE_MODEL_SECURITY_RATE_LIMIT_RPM: \"120\"");
        assertThat(remote).contains("for request_index in $(seq 1 140)");
    }

    @Test
    void adminCredentialProbeIsBoundedAndReportsOnlySanitizedStatusCategory() throws Exception {
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(remote).contains(
                "await_auth_expectation()",
                "for attempt in $(seq 1 8)",
                "if [ \"${attempt}\" -lt 8 ]",
                "sleep 2",
                "429|TRANSPORT_DNS|TRANSPORT_CONNECT|TRANSPORT_TIMEOUT)",
                "auth_code_category()",
                "HTTP_401", "HTTP_403", "HTTP_429", "HTTP_5XX",
                "TRANSPORT_CONFIG", "TRANSPORT_CONNECT", "TRANSPORT_TIMEOUT",
                "TRANSPORT_TLS", "TRANSPORT_CERTIFICATE", "TRANSPORT_CA_FILE",
                "TRANSPORT_CREDENTIAL_FILE", "TRANSPORT_RUNTIME",
                "BLOCKED_V1_ADMIN_PRECHECK_$(auth_code_category",
                "BLOCKED_V2_ADMIN_PREACTIVATED_$(auth_code_category");
        assertThat(remote).contains(
                "--output /dev/null --write-out '%{http_code}'",
                "2>/dev/null",
                "connect-timeout = 2",
                "max-time = 5");
        assertThat(remote).doesNotContain(
                "auth_response_body", "curl --verbose", "set -x");
    }

    @Test
    void evidenceChecksUseOwnedRuntimeAndCannotMaskEarlierFailuresWithCleanup() throws Exception {
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(remote).contains(
                "SERVICE_RUNTIME=/run/trade-model-p3h",
                "${SERVICE_RUNTIME}/p3h-lab-auth.XXXXXX",
                "${SERVICE_RUNTIME}/p3h-lab-smoke.XXXXXX",
                "${SERVICE_RUNTIME}/p3h-lab-backup.XXXXXX",
                "${SERVICE_RUNTIME}/p3h-lab-journal.XXXXXX",
                "run_checked_or_block()",
                "trap - ERR",
                "set -euo pipefail",
                "run_checked_or_block BLOCKED_HTTPS_SMOKE https_smoke V1",
                "run_checked_or_block BLOCKED_BACKUP_RESTORE backup_restore",
                "run_checked_or_block BLOCKED_TLS_CREDENTIAL_ACTIVATION",
                "run_checked_or_block BLOCKED_POST_ROTATION_SMOKE https_smoke V2",
                "run_checked_or_block BLOCKED_HTTPS_AFTER_REBOOT https_smoke V2");
        assertThat(remote).doesNotContain(
                "mktemp /run/p3h-lab-auth.",
                "mktemp -d /run/p3h-lab-smoke.",
                "mktemp -d /run/p3h-lab-backup.",
                "mktemp /run/p3h-lab-journal.",
                "https_smoke V1 || blocked",
                "https_smoke V2 || blocked",
                "backup_restore || blocked",
                "activate_tls_v2_credentials || blocked");
    }

    @Test
    void cleanupCanDeleteOnlyExactLabResources() throws Exception {
        String destroy = read("scripts/p3h-lab-destroy.sh");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");

        assertThat(destroy).contains(
                "VM_NAME=trade-model-p3h-staging-lab",
                "${HOME}/.local/share/trade-model-p3h-lab1",
                "BLOCKED_UNOWNED_VM", "BLOCKED_UNOWNED_DIRECTORY");
        assertThat(remote).contains(
                "label=com.docker.compose.project=${PROJECT}",
                "/etc/credstore.encrypted/trade-model-p3h");
        assertThat(destroy).doesNotContain("limactl delete --all", "${HOME}/.ssh");
    }

    @Test
    void sanitizedEvidenceDropsUnknownLinesAndValues() throws Exception {
        Path raw = tempDir.resolve("raw.txt");
        Path sanitized = tempDir.resolve("sanitized.txt");
        String secret = "do-not-expose-this-secret";
        Files.writeString(raw, """
                STAGING_FLYWAY: PASS_V1_TO_V7
                APPLICATION_DATABASE_ROLE: READ_ONLY
                READ_ONLY_WRITE_PROBE: DENIED
                SECRET_LEAK_CANDIDATE_COUNT: 0
                P4_ALLOWED: NO
                PRODUCTION_READINESS: BLOCKED
                RAW_RESPONSE: %s
                """.formatted(secret), StandardCharsets.UTF_8);

        Process process = new ProcessBuilder(
                "bash", "scripts/p3h-lab-evidence-redact.sh",
                raw.toString(), sanitized.toString()).redirectErrorStream(true).start();
        assertThat(process.waitFor(5, TimeUnit.SECONDS)).isTrue();

        assertThat(process.exitValue()).isZero();
        assertThat(Files.readString(sanitized)).doesNotContain(secret, "RAW_RESPONSE");
    }

    @Test
    void everyTerminalStatusKeepsP4AndProductionBlocked() throws Exception {
        String runner = read("scripts/controlled-staging-readonly-deployment-p3h-r1.sh");
        String remote = read("deploy/p3h/lima/p3h-lab-r1-remote.sh");
        String docs = read("docs/P3H_LOCAL_LINUX_VM_STAGING_LAB1.md");

        assertThat(runner + remote + docs).contains(
                "P4_ALLOWED: NO", "PRODUCTION_READINESS: BLOCKED");
        assertThat(runner + remote + docs).doesNotContain(
                "P4_ALLOWED: YES", "PRODUCTION_READINESS: READY");
    }

    private String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
