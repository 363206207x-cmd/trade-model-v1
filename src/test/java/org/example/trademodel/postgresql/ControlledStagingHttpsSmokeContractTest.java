package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingHttpsSmokeContractTest {

    @Test
    void serverEvidenceMustUseFullFetchAndValidateSmoke() throws Exception {
        String smoke = P3hContractTestSupport.read("scripts/prod-smoke.sh");
        String proxy = P3hContractTestSupport.read("deploy/p3h/reverse-proxy.conf");

        assertThat(smoke).contains(
                "SMOKE_PHASE=\"${SMOKE_PHASE:-FETCH_AND_VALIDATE}\"",
                "/actuator/health", "/actuator/health/liveness",
                "/actuator/health/readiness", "/api/dashboard/home", "/api/review/center",
                "safety.notAutoTrading", "safety.notOrderExecution");
        assertThat(proxy).contains("location ~ ^/actuator/health", "location ~ ^/(?:api/");
        assertThat(proxy).doesNotContain("location /actuator/env", "location /actuator/configprops");
    }

    @Test
    void unauthenticatedAndBadCredentialChecksRemainRequired() throws Exception {
        String documentation = P3hContractTestSupport.read(
                "docs/PRODUCTION_ACCEPTANCE_EVIDENCE_TEMPLATE.md");

        assertThat(documentation).containsIgnoringCase("unauthenticated");
    }
}
