package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
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
                "return 308 https://$host$request_uri", "ssl_protocols TLSv1.2 TLSv1.3",
                "Strict-Transport-Security \"max-age=86400\"",
                "proxy_set_header Host $host", "proxy_set_header X-Forwarded-Proto https",
                "proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for",
                "server_tokens off", "client_max_body_size 1m", "limit_req_status 429");
        assertThat(smoke).contains(
                "--cacert", "-verify_hostname", "-verify_return_error", "-CAfile",
                "-tls1_2", "-tls1_3", "-tls1_1",
                "TLS_1_0_REJECTED", "TLS_1_1_REJECTED", "BLOCKED_CERTIFICATE_HOSTNAME",
                "elapsed_ticks", "return 124");
        assertThat(smoke).doesNotContain("curl -k", "--insecure");
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

    private void writeExecutable(Path path, String content) throws Exception {
        Files.writeString(path, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE));
    }
}
