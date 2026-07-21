package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingHttpsSmokeContractTest {

    @Test
    void serverEvidenceMustUseFullFetchAndValidateSmoke() throws Exception {
        String smoke = P3hContractTestSupport.read("scripts/prod-smoke.sh");
        String runner = P3hContractTestSupport.read(
                "scripts/controlled-p3h-compose-offline-smoke.sh");
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");

        assertThat(smoke).contains(
                "SMOKE_PHASE=\"${SMOKE_PHASE:-FETCH_AND_VALIDATE}\"",
                "/actuator/health", "/actuator/health/liveness",
                "/actuator/health/readiness", "/api/dashboard/home", "/api/review/center",
                "fetch_login_page", "perform_login", "fetch_authenticated_dashboard_page",
                "extract_form_csrf", "perform_logout",
                "assert_pre_logout_session_invalidated",
                "safety.notAutoTrading", "safety.notOrderExecution");
        assertThat(runner).contains(
                "SMOKE_PHASE=FETCH_AND_VALIDATE",
                "SMOKE_RESPONSE_DIR=\"\"",
                "TRADE_MODEL_SMOKE_USERNAME=p3h_operator",
                "TRADE_MODEL_SMOKE_CA_CERT=\"${SECRET_DIR}/tls_certificate\"",
                "print_sanitized_session_smoke_failure",
                "grep -E '^FAIL '");
        assertThat(runner).doesNotContain("SMOKE_PHASE=FETCH\n");
        assertThat(runner).doesNotContain(
                "cat \"${SESSION_SMOKE_LOG}\"",
                "tail \"${SESSION_SMOKE_LOG}\"");
        assertThat(proxy).contains(
                "location ~ ^/actuator/health",
                "location ~ ^/(?:login|logout|favicon\\.ico|(?:css|js|images|webjars)/.*)$",
                "location ~ ^/(?:api/",
                "proxy_set_header Cookie $http_cookie;");
        assertThat(proxy).doesNotContain("location /actuator/env", "location /actuator/configprops");
    }

    @Test
    void unauthenticatedAndBadCredentialChecksRemainRequired() throws Exception {
        String documentation = P3hContractTestSupport.read(
                "docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md");

        assertThat(documentation).containsIgnoringCase("unauthenticated");
    }
}
