package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingTlsProxyContractTest {

    @TempDir
    Path tempDir;

    @Test
    void proxyRequiresVerifiedTlsRedirectHstsAndForwardedHeaders() throws Exception {
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");
        String smoke = P3hContractTestSupport.read("scripts/p3h-tls-smoke.sh");

        assertThat(proxy).contains(
                "return 308 https://${P3H_STAGING_HOSTNAME}$request_uri",
                "ssl_protocols TLSv1.2 TLSv1.3",
                "Strict-Transport-Security \"max-age=86400\"",
                "proxy_set_header Host ${P3H_STAGING_HOSTNAME}",
                "proxy_set_header X-Forwarded-Proto https",
                "proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for",
                "server_tokens off", "client_max_body_size 1m", "limit_req_status 429");
        assertThat(smoke).contains(
                "--cacert", "-verify_hostname", "-verify_return_error", "-CAfile",
                "-tls1_2", "-tls1_3", "-tls1_1",
                "TLS_1_0_REJECTED", "TLS_1_1_REJECTED", "BLOCKED_CERTIFICATE_HOSTNAME",
                "elapsed_ticks", "return 124");
        assertThat(smoke).doesNotContain("curl -k", "--insecure");
        assertThat(proxy).doesNotContain(
                "https://$host$request_uri", "proxy_set_header Host $host");
    }

    @Test
    void internalCaSmokePassesCaBundleToEveryVerifiedProbeAndRejectsLegacyTls() throws Exception {
        Path fakeBin = Files.createDirectory(tempDir.resolve("strict-bin"));
        Path opensslArguments = tempDir.resolve("openssl-arguments.txt");
        Path caBundle = tempDir.resolve("staging-ca.pem");
        Files.writeString(caBundle, "offline-ca-fixture", StandardCharsets.UTF_8);
        writeExecutable(fakeBin.resolve("curl"), "#!/usr/bin/env bash\nprintf '200'\n");
        writeExecutable(fakeBin.resolve("openssl"), """
                #!/usr/bin/env bash
                printf '%s\\n' "$*" >> "$P3H_TEST_OPENSSL_ARGUMENTS"
                if [[ "$*" == "s_client -help" ]]; then
                  printf '%s\\n' '-tls1_3'
                  exit 0
                fi
                case " $* " in
                  *" -tls1 "*|*" -tls1_1 "*) exit 1 ;;
                  *) exit 0 ;;
                esac
                """);

        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/p3h-tls-smoke.sh");
        builder.redirectErrorStream(true);
        builder.environment().put("PATH", fakeBin + ":" + builder.environment().get("PATH"));
        builder.environment().put("P3H_HTTPS_BASE_URL", "https://stage.example.invalid");
        builder.environment().put("P3H_STAGING_HOSTNAME", "stage.example.invalid");
        builder.environment().put("P3H_TLS_MODE", "INTERNAL_CA");
        builder.environment().put("P3H_CA_BUNDLE_FILE", caBundle.toString());
        builder.environment().put("P3H_TEST_OPENSSL_ARGUMENTS", opensslArguments.toString());

        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        assertThat(finished).isTrue();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String arguments = Files.readString(opensslArguments, StandardCharsets.UTF_8);

        assertThat(process.exitValue()).as("sanitized output: %s", output).isZero();
        assertThat(output).contains(
                "TLS_1_0_REJECTED: PASS", "TLS_1_1_REJECTED: PASS",
                "TLS_1_3: PASS", "HEALTH_AND_CERTIFICATE_TARGET: MATCH",
                "P3H_TLS_SMOKE: PASS_VERIFIED");
        assertThat(arguments).contains("-verify_return_error", "-CAfile " + caBundle);
    }

    @Test
    void accessLogFormatDoesNotRecordSensitiveHeadersOrQueryStrings() throws Exception {
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");
        String logFormat = proxy.substring(proxy.indexOf("log_format p3h_safe"),
                proxy.indexOf("server {"));

        assertThat(logFormat).contains("$uri", "$request_id", "$p3h_client_class");
        assertThat(logFormat).doesNotContain(
                "$http_authorization", "$http_cookie", "$request_uri", "$args");
    }

    @Test
    void certificateHostnameMismatchFailsClosedWithoutNetwork() throws Exception {
        Path fakeBin = Files.createDirectory(tempDir.resolve("bin"));
        writeExecutable(fakeBin.resolve("curl"), "#!/usr/bin/env bash\nprintf '200'\n");
        writeExecutable(fakeBin.resolve("openssl"), "#!/usr/bin/env bash\nexit 1\n");

        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/p3h-tls-smoke.sh");
        builder.redirectErrorStream(true);
        builder.environment().put("PATH", fakeBin + ":" + builder.environment().get("PATH"));
        builder.environment().put("P3H_HTTPS_BASE_URL", "https://stage.example.invalid");
        builder.environment().put("P3H_STAGING_HOSTNAME", "stage.example.invalid");
        builder.environment().put("P3H_TLS_MODE", "PUBLIC_CA");

        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        assertThat(finished).isTrue();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(process.exitValue()).as("sanitized output: %s", output).isEqualTo(2);
        assertThat(output).isEqualTo("P3H_TLS_SMOKE: BLOCKED_CERTIFICATE_HOSTNAME\n");
    }

    @Test
    void hostHeaderInjectionCannotChangeRedirectTarget() throws Exception {
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");

        assertThat(proxy).contains("return 308 https://${P3H_STAGING_HOSTNAME}$request_uri");
        assertThat(proxy).doesNotContain("https://$host$request_uri");
    }

    @Test
    void unknownHttpHostIsRejected() throws Exception {
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");

        assertThat(proxy).contains(
                "listen 8080 default_server", "server_name _", "return 444");
    }

    @Test
    void unknownHttpsHostIsRejected() throws Exception {
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");
        String offlineSmoke = P3hContractTestSupport.read(
                "scripts/controlled-p3h-compose-offline-smoke.sh");

        assertThat(proxy).contains(
                "listen 8443 ssl default_server", "ssl_reject_handshake on");
        assertThat(offlineSmoke).contains(
                "-servername unapproved.invalid",
                "BLOCKED_UNKNOWN_HTTPS_HOST_ACCEPTED",
                "UNKNOWN_HTTPS_HOST_REJECTED: PASS");
    }

    @Test
    void approvedHostRedirectsToApprovedHostname() throws Exception {
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");

        assertThat(proxy).contains(
                "server_name ${P3H_STAGING_HOSTNAME}",
                "return 308 https://${P3H_STAGING_HOSTNAME}$request_uri");
    }

    @Test
    void approvedHostIsForwardedUpstream() throws Exception {
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");

        assertThat(proxy).contains("proxy_set_header Host ${P3H_STAGING_HOSTNAME}");
        assertThat(proxy).doesNotContain("proxy_set_header Host $host");
    }

    @Test
    void healthAndCertificateTargetsMustMatch() throws Exception {
        String smoke = P3hContractTestSupport.read("scripts/p3h-tls-smoke.sh");

        assertThat(smoke).contains(
                "parsed.hostname == expected_host", "actual_port == approved_port",
                "${hostname}:${https_port}", "HEALTH_AND_CERTIFICATE_TARGET: MATCH");
    }

    @Test
    void differentBaseUrlHostnameFails() throws Exception {
        assertTargetBindingFailure("https://other.example.invalid", Map.of());
    }

    @Test
    void baseUrlWithUserInfoFails() throws Exception {
        assertTargetBindingFailure("https://user@stage.example.invalid", Map.of());
    }

    @Test
    void baseUrlWithQueryFails() throws Exception {
        assertTargetBindingFailure("https://stage.example.invalid?target=other", Map.of());
    }

    @Test
    void serverTls13FailureIsNotReportedAsClientUnsupported() throws Exception {
        Path fakeBin = Files.createDirectory(tempDir.resolve("tls13-bin"));
        writeExecutable(fakeBin.resolve("curl"), "#!/usr/bin/env bash\nprintf '200'\n");
        writeExecutable(fakeBin.resolve("openssl"), """
                #!/usr/bin/env bash
                if [[ "$*" == "s_client -help" ]]; then
                  printf '%s\\n' '-tls1_3'
                  exit 0
                fi
                case " $* " in
                  *" -tls1_3 "*) exit 1 ;;
                  *) exit 0 ;;
                esac
                """);

        ScriptOutput result = runTlsSmoke(fakeBin,
                "https://stage.example.invalid", Map.of());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).contains("TLS_1_3: FAIL");
        assertThat(result.output()).doesNotContain("TLS_1_3: ENVIRONMENT_NOT_SUPPORTED");
    }

    private void assertTargetBindingFailure(String baseUrl, Map<String, String> extraEnvironment)
            throws Exception {
        Path fakeBin = Files.createDirectory(tempDir.resolve("target-bin-"
                + Integer.toUnsignedString(baseUrl.hashCode())));
        writeExecutable(fakeBin.resolve("curl"), "#!/usr/bin/env bash\nexit 99\n");
        writeExecutable(fakeBin.resolve("openssl"), "#!/usr/bin/env bash\nexit 99\n");

        ScriptOutput result = runTlsSmoke(fakeBin, baseUrl, extraEnvironment);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).isEqualTo("P3H_TLS_SMOKE: BLOCKED_TARGET_BINDING\n");
    }

    private ScriptOutput runTlsSmoke(Path fakeBin, String baseUrl,
                                     Map<String, String> extraEnvironment) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/p3h-tls-smoke.sh");
        builder.redirectErrorStream(true);
        builder.environment().put("PATH", fakeBin + ":" + builder.environment().get("PATH"));
        builder.environment().put("P3H_HTTPS_BASE_URL", baseUrl);
        builder.environment().put("P3H_STAGING_HOSTNAME", "stage.example.invalid");
        builder.environment().put("P3H_TLS_MODE", "PUBLIC_CA");
        builder.environment().putAll(extraEnvironment);
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        assertThat(finished).isTrue();
        return new ScriptOutput(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private void writeExecutable(Path path, String content) throws Exception {
        Files.writeString(path, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
    }

    private record ScriptOutput(int exitCode, String output) {
    }
}
