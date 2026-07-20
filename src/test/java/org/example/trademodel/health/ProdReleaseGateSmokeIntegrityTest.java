package org.example.trademodel.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ProdReleaseGateSmokeIntegrityTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CURRENT_APP_URL = "http://current-controlled-app:18085";

    @TempDir
    Path tempDir;

    private Path stubBin;
    private Path curlLog;

    @BeforeEach
    void setUp() throws Exception {
        stubBin = Files.createDirectory(tempDir.resolve("bin"));
        curlLog = tempDir.resolve("curl.log");
        writeExecutable(stubBin.resolve("docker"), """
                #!/usr/bin/env bash
                exit 0
                """);
        writeExecutable(stubBin.resolve("curl"), """
                #!/usr/bin/env bash
                set -euo pipefail
                output=""
                headers=""
                cookie_jar=""
                cookie_input=""
                data_file=""
                method="GET"
                url=""
                while [ "$#" -gt 0 ]; do
                  case "$1" in
                    -o|--output) output="$2"; shift 2 ;;
                    -w|--write-out) shift 2 ;;
                    --dump-header) headers="$2"; shift 2 ;;
                    --cookie-jar) cookie_jar="$2"; shift 2 ;;
                    --cookie) cookie_input="$2"; shift 2 ;;
                    --data-binary) data_file="${2#@}"; shift 2 ;;
                    --request) method="$2"; shift 2 ;;
                    --header|--connect-timeout|--max-time|--cacert) shift 2 ;;
                    --silent|--show-error) shift ;;
                    http://*|https://*) url="$1"; shift ;;
                    *) shift ;;
                  esac
                done
                [ -n "$output" ]
                [ -n "$url" ]
                printf '%s %s COOKIE=%s DATA=%s\n' "$method" "$url" \
                  "$([ -n "$cookie_input" ] && printf YES || printf NO)" \
                  "$([ -n "$data_file" ] && printf YES || printf NO)" >>"${CURL_LOG:?}"
                payload='{}'
                code=200
                location=""
                path="${url#*://}"
                path="/${path#*/}"
                case "$path:$method" in
                  /actuator/health:GET|/actuator/health/liveness:GET|/actuator/health/readiness:GET)
                    payload='{"status":"UP"}'
                    ;;
                  /login:GET)
                    if [ "${CURL_SMOKE_MODE:-VALID}" = "LOGIN_PAGE_FAIL" ]; then
                      code=500
                    elif [ "${CURL_SMOKE_MODE:-VALID}" = "MISSING_CSRF" ]; then
                      payload='<form action="/login" method="post"><input name="username"><input name="password"></form>'
                    else
                      payload='<form action="/login" method="post"><input type="hidden" name="_csrf" value="controlled-login-csrf"><input name="username"><input name="password"></form>'
                    fi
                    if [ -n "$cookie_jar" ]; then
                      printf '# Netscape HTTP Cookie File\nlocalhost\tFALSE\t/\tTRUE\t0\tJSESSIONID\tPREAUTH\n' >"$cookie_jar"
                    fi
                    ;;
                  /login:POST)
                    if [ "${CURL_SMOKE_MODE:-VALID}" = "LOGIN_FAIL" ] \
                        || [ -z "$data_file" ] \
                        || ! grep -q '_csrf=controlled-login-csrf' "$data_file" \
                        || ! grep -q 'username=controlled-user' "$data_file" \
                        || ! grep -q 'password=controlled-password' "$data_file"; then
                      code=302
                      location='/login?error=true'
                    else
                      code=302
                      location='/dashboard'
                      printf 'VALID\n' >"${CURL_SESSION_STATE:?}"
                      printf '# Netscape HTTP Cookie File\nlocalhost\tFALSE\t/\tTRUE\t0\tJSESSIONID\tAUTHED\n' >"$cookie_jar"
                    fi
                    ;;
                  /dashboard:GET)
                    if [ "$(cat "${CURL_SESSION_STATE:?}" 2>/dev/null || true)" != "VALID" ]; then
                      code=302
                      location='/login'
                    elif [ "${CURL_SMOKE_MODE:-VALID}" = "MISSING_LOGOUT_CSRF" ]; then
                      payload='<form action="/logout" method="post"></form>'
                    else
                      payload='<form action="/logout" method="post"><input type="hidden" name="_csrf" value="controlled-logout-csrf"></form>'
                    fi
                    ;;
                  /logout:POST)
                    if [ "${CURL_SMOKE_MODE:-VALID}" = "LOGOUT_FAIL" ] \
                        || [ -z "$data_file" ] \
                        || ! grep -q '_csrf=controlled-logout-csrf' "$data_file"; then
                      code=403
                    else
                      code=302
                      location='/login?logout=true'
                      printf 'INVALID\n' >"${CURL_SESSION_STATE:?}"
                    fi
                    ;;
                  /api/dashboard/home:GET)
                    if [ -z "$cookie_input" ]; then
                      code=401
                      payload='{"error":"unauthorized"}'
                    elif [ "${CURL_SMOKE_MODE:-VALID}" = "AUTH_LOGIN_PAGE" ]; then
                      payload='<form action="/login" method="post"></form>'
                    elif [ "$(cat "${CURL_SESSION_STATE:?}" 2>/dev/null || true)" != "VALID" ] \
                        && [ "${CURL_SMOKE_MODE:-VALID}" != "POST_LOGOUT_STILL_AUTH" ]; then
                      code=401
                      payload='{"error":"unauthorized"}'
                    elif [ "${CURL_DASHBOARD_MODE:-VALID}" = "INVALID" ]; then
                      payload='{"data":{"header":{}}}'
                    else
                      payload='{"data":{"header":{"dataSourceText":"controlled","aiStatus":"NOT_CALLED"},"systemState":{},"assets":[],"positions":[],"executionSuggestion":{},"aiDecision":{},"pushInbox":{"telegramStatus":"WAITING_SYNC"},"diagnostics":{"marketDataProvider":"NOT_CONFIGURED","aiProvider":"DISABLED","externalContextProvider":"NOT_CONFIGURED","providerReadiness":{"providers":[]}},"safety":{"notAutoTrading":true,"notOrderExecution":true}}}'
                    fi
                    ;;
                  /api/review/center:GET)
                    if [ "$(cat "${CURL_SESSION_STATE:?}" 2>/dev/null || true)" != "VALID" ]; then
                      code=401
                      payload='{"error":"unauthorized"}'
                    else
                      payload='{"data":{"summary":{},"positionReviews":[],"opportunityReviews":[],"pushReviews":[],"ruleFeedback":[]}}'
                    fi
                    ;;
                  *)
                    code=404
                    ;;
                esac
                if [ -n "$headers" ]; then
                  printf 'HTTP/1.1 %s Controlled\r\n' "$code" >"$headers"
                  if [ -n "$location" ]; then
                    printf 'Location: %s\r\n' "$location" >>"$headers"
                  fi
                  printf '\r\n' >>"$headers"
                fi
                printf '%s' "$payload" >"$output"
                printf '%s' "$code"
                """);
    }

    @Test
    void releaseGateForcesFetchAndValidateDespiteInheritedFetchPhase() throws Exception {
        Path canned = writeCannedResponses();

        ScriptResult result = runReleaseGate("FETCH", canned, "VALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(2);
        assertThat(result.output()).contains("PASS production smoke checks", "PASS production smoke");
        assertThat(result.output()).doesNotContain("LOCAL_CONTROLLED_SPLIT_ONLY");
        assertCurrentAppWasFetched();
    }

    @Test
    void releaseGateForcesFetchAndValidateDespiteInheritedValidatePhase() throws Exception {
        Path canned = writeCannedResponses();

        ScriptResult result = runReleaseGate("VALIDATE", canned, "VALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(2);
        assertThat(result.output()).contains("PASS production smoke checks", "PASS production smoke");
        assertCurrentAppWasFetched();
    }

    @Test
    void releaseGateClearsInheritedSmokeResponseDir() throws Exception {
        Path missingInheritedDirectory = tempDir.resolve("does-not-exist");

        ScriptResult result = runReleaseGate("FETCH_AND_VALIDATE", missingInheritedDirectory, "VALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(2);
        assertThat(result.output()).contains("PASS production smoke checks", "PASS production smoke");
        assertThat(result.output()).doesNotContain("smoke response directory must be");
    }

    @Test
    void releaseGateCannotPassWithCannedResponseArtifacts() throws Exception {
        Path canned = writeCannedResponses();

        ScriptResult result = runReleaseGate("VALIDATE", canned, "INVALID");

        assertThat(result.exitCode()).as(result.output()).isEqualTo(1);
        assertThat(result.output()).contains(
                "FAIL dashboard missing keys",
                "FAIL production smoke failed",
                "PRODUCTION_RELEASE_GATE: FAIL");
        assertCurrentAppWasFetched();
    }

    @Test
    void defaultProductionSmokeStillFetchesAndValidates() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/prod-smoke.sh")
                .redirectErrorStream(true);
        configureCommonEnvironment(builder);
        builder.environment().remove("SMOKE_PHASE");
        builder.environment().remove("SMOKE_RESPONSE_DIR");
        builder.environment().remove("SMOKE_SPLIT_PHASE_CONFIRM");
        builder.environment().put("APP_URL", CURRENT_APP_URL);
        builder.environment().put("TRADE_MODEL_SMOKE_USERNAME", "controlled-user");
        builder.environment().put("TRADE_MODEL_SMOKE_PASSWORD", "controlled-password");

        ScriptResult result = run(builder);

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains("PASS production smoke checks");
        assertThat(result.output()).doesNotContain("LOCAL_CONTROLLED_SPLIT_ONLY");
        assertCurrentAppWasFetched();
    }

    @Test
    void productionSmokeUsesFormLoginSessionCsrfAndNeverBasicAuth() throws Exception {
        String script = Files.readString(Path.of("scripts/prod-smoke.sh"), StandardCharsets.UTF_8);

        assertThat(script).contains(
                "TRADE_MODEL_SMOKE_USERNAME", "TRADE_MODEL_SMOKE_PASSWORD",
                "fetch_login_page", "extract_form_csrf", "perform_login",
                "fetch_authenticated_dashboard_page",
                "--cookie-jar \"$cookie_jar\"", "perform_logout",
                "assert_pre_logout_session_invalidated",
                "SESSION_AUTH_SMOKE: PASS_FORM_LOGIN_SESSION_CSRF");
        assertThat(script).doesNotContain("curl -u", "--user");
    }

    @Test
    void missingSmokeUsernameFailsFast() throws Exception {
        ScriptResult result = runSmoke("VALID", null, "controlled-password");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.output()).contains("FAIL smoke username missing");
    }

    @Test
    void missingSmokePasswordFailsFast() throws Exception {
        ScriptResult result = runSmoke("VALID", "controlled-user", null);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.output()).contains("FAIL smoke password missing");
    }

    @Test
    void loginPageAndCsrfFailuresFailClosed() throws Exception {
        ScriptResult loginPageFailure = runSmoke(
                "LOGIN_PAGE_FAIL", "controlled-user", "controlled-password");
        ScriptResult missingCsrf = runSmoke(
                "MISSING_CSRF", "controlled-user", "controlled-password");

        assertThat(loginPageFailure.exitCode()).isEqualTo(1);
        assertThat(loginPageFailure.output()).contains("FAIL /login returned HTTP 500");
        assertThat(missingCsrf.exitCode()).isEqualTo(1);
        assertThat(missingCsrf.output()).contains("did not provide the CSRF contract");
    }

    @Test
    void rejectedLoginAndLoginPageMasqueradingAsApiFailClosed() throws Exception {
        ScriptResult rejectedLogin = runSmoke(
                "LOGIN_FAIL", "controlled-user", "controlled-password");
        ScriptResult loginPageResponse = runSmoke(
                "AUTH_LOGIN_PAGE", "controlled-user", "controlled-password");

        assertThat(rejectedLogin.exitCode()).isEqualTo(1);
        assertThat(rejectedLogin.output()).contains("FAIL login was not accepted");
        assertThat(loginPageResponse.exitCode()).isEqualTo(1);
        assertThat(loginPageResponse.output()).contains("did not return authenticated JSON");
    }

    @Test
    void logoutFailureAndNonInvalidatedSessionFailClosed() throws Exception {
        ScriptResult logoutFailure = runSmoke(
                "LOGOUT_FAIL", "controlled-user", "controlled-password");
        ScriptResult liveOldSession = runSmoke(
                "POST_LOGOUT_STILL_AUTH", "controlled-user", "controlled-password");

        assertThat(logoutFailure.exitCode()).isEqualTo(1);
        assertThat(logoutFailure.output()).contains("FAIL logout returned HTTP 403");
        assertThat(liveOldSession.exitCode()).isEqualTo(1);
        assertThat(liveOldSession.output()).contains("pre-logout Session remained valid");
    }

    @Test
    void missingPostLoginLogoutCsrfFailsClosed() throws Exception {
        ScriptResult result = runSmoke(
                "MISSING_LOGOUT_CSRF", "controlled-user", "controlled-password");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.output()).contains(
                "FAIL authenticated dashboard did not provide the logout CSRF contract");
    }

    @Test
    void successfulSessionSmokeUsesCookiesAndCsrfWithoutLeakingValues() throws Exception {
        ScriptResult result = runSmoke("VALID", "controlled-user", "controlled-password");

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains(
                "SESSION_AUTH_SMOKE: PASS_FORM_LOGIN_SESSION_CSRF",
                "POST_LOGOUT_SESSION_INVALIDATION: PASS");
        assertThat(result.output()).doesNotContain(
                "controlled-password", "controlled-login-csrf", "controlled-logout-csrf",
                "JSESSIONID", "AUTHED");
        assertThat(Files.readString(curlLog, StandardCharsets.UTF_8)).contains(
                "POST " + CURRENT_APP_URL + "/login COOKIE=YES DATA=YES",
                "GET " + CURRENT_APP_URL + "/dashboard COOKIE=YES DATA=NO",
                "GET " + CURRENT_APP_URL + "/api/dashboard/home COOKIE=YES DATA=NO",
                "POST " + CURRENT_APP_URL + "/logout COOKIE=YES DATA=YES");
    }

    @Test
    void smokeTemporarySessionMaterialHasBoundedCleanupContract() throws Exception {
        String script = Files.readString(Path.of("scripts/prod-smoke.sh"), StandardCharsets.UTF_8);

        assertThat(script).contains(
                "umask 077", "runtime_dir=\"$(mktemp -d)\"", "chmod 700 \"$runtime_dir\"",
                "trap cleanup EXIT HUP INT TERM", "rm -rf \"$runtime_dir\"");
        assertThat(script).doesNotContain("curl -v", "set -x", "echo \"$csrf_token\"");
    }

    private ScriptResult runReleaseGate(String inheritedPhase,
                                        Path inheritedResponseDirectory,
                                        String dashboardMode) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/prod-release-gate.sh")
                .redirectErrorStream(true);
        configureCommonEnvironment(builder);
        Map<String, String> environment = builder.environment();
        environment.put("APP_URL", CURRENT_APP_URL);
        environment.put("TRADE_MODEL_SMOKE_USERNAME", "controlled-user");
        environment.put("TRADE_MODEL_SMOKE_PASSWORD", "controlled-password");
        environment.put("SMOKE_PHASE", inheritedPhase);
        environment.put("SMOKE_RESPONSE_DIR", inheritedResponseDirectory.toString());
        environment.put("SMOKE_SPLIT_PHASE_CONFIRM", "I_CONFIRM_LOCAL_CONTROLLED_SPLIT_SMOKE");
        environment.put("CURL_DASHBOARD_MODE", dashboardMode);
        environment.put("RELEASE_GATE_REQUIRE_DOCKER", "true");
        environment.put("RELEASE_GATE_REQUIRE_BACKUP", "false");
        environment.put("RELEASE_GATE_REQUIRE_PROVIDER_SMOKE", "false");
        return run(builder);
    }

    private ScriptResult runSmoke(String mode, String username, String password) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/prod-smoke.sh")
                .redirectErrorStream(true);
        configureCommonEnvironment(builder);
        Map<String, String> environment = builder.environment();
        environment.put("APP_URL", CURRENT_APP_URL);
        environment.put("CURL_SMOKE_MODE", mode);
        environment.put("SMOKE_PHASE", "FETCH_AND_VALIDATE");
        for (String key : List.of(
                "TRADE_MODEL_SMOKE_USERNAME", "TRADE_MODEL_SMOKE_PASSWORD",
                "SMOKE_AUTH_USERNAME", "SMOKE_AUTH_PASSWORD",
                "APP_ADMIN_USERNAME", "APP_ADMIN_PASSWORD")) {
            environment.remove(key);
        }
        if (username != null) {
            environment.put("TRADE_MODEL_SMOKE_USERNAME", username);
        }
        if (password != null) {
            environment.put("TRADE_MODEL_SMOKE_PASSWORD", password);
        }
        return run(builder);
    }

    private void configureCommonEnvironment(ProcessBuilder builder) {
        Map<String, String> environment = builder.environment();
        environment.put("PATH", stubBin + System.getProperty("path.separator") + environment.get("PATH"));
        environment.put("CURL_LOG", curlLog.toString());
        environment.put("CURL_SESSION_STATE", tempDir.resolve("session.state").toString());
    }

    private ScriptResult run(ProcessBuilder builder) throws Exception {
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private void assertCurrentAppWasFetched() throws Exception {
        String calls = Files.readString(curlLog, StandardCharsets.UTF_8);
        assertThat(calls).contains(
                CURRENT_APP_URL + "/actuator/health",
                CURRENT_APP_URL + "/actuator/health/liveness",
                CURRENT_APP_URL + "/actuator/health/readiness",
                "GET " + CURRENT_APP_URL + "/login",
                "POST " + CURRENT_APP_URL + "/login",
                "GET " + CURRENT_APP_URL + "/dashboard",
                CURRENT_APP_URL + "/api/dashboard/home",
                CURRENT_APP_URL + "/api/review/center",
                "POST " + CURRENT_APP_URL + "/logout");
    }

    private Path writeCannedResponses() throws Exception {
        Path directory = Files.createDirectories(tempDir.resolve("canned-responses"));
        for (String name : List.of("health.json", "liveness.json", "readiness.json")) {
            OBJECT_MAPPER.writeValue(directory.resolve(name).toFile(), Map.of("status", "UP"));
        }
        OBJECT_MAPPER.writeValue(directory.resolve("dashboard.json").toFile(), Map.of(
                "data", Map.of(
                        "header", Map.of("dataSourceText", "canned", "aiStatus", "NOT_CALLED"),
                        "systemState", Map.of(),
                        "assets", List.of(),
                        "positions", List.of(),
                        "executionSuggestion", Map.of(),
                        "aiDecision", Map.of(),
                        "pushInbox", Map.of("telegramStatus", "WAITING_SYNC"),
                        "diagnostics", Map.of(
                                "marketDataProvider", "NOT_CONFIGURED",
                                "aiProvider", "DISABLED",
                                "externalContextProvider", "NOT_CONFIGURED",
                                "providerReadiness", Map.of("providers", List.of())),
                        "safety", Map.of("notAutoTrading", true, "notOrderExecution", true))));
        OBJECT_MAPPER.writeValue(directory.resolve("review.json").toFile(), Map.of(
                "data", Map.of(
                        "summary", Map.of(),
                        "positionReviews", List.of(),
                        "opportunityReviews", List.of(),
                        "pushReviews", List.of(),
                        "ruleFeedback", List.of())));
        return directory;
    }

    private void writeExecutable(Path path, String content) throws Exception {
        Files.writeString(path, content.stripLeading(), StandardCharsets.UTF_8);
        assertThat(path.toFile().setExecutable(true)).isTrue();
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
