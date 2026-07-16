package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledStagingSecretMaterializationContractTest {

    @Test
    void appUserReadsConfigTreeSecrets() throws Exception {
        String runner = runner();

        assertThat(runner).contains(
                "compose exec -T app bash -c",
                "test -r /run/secrets/config/spring.datasource.password");
    }

    @Test
    void unrelatedUidCannotReadSecrets() throws Exception {
        assertThat(runner()).contains(
                "--user 10002:10002 app",
                "test ! -r /run/secrets/config/spring.datasource.password");
    }

    @Test
    void secretFilesAreNotWorldReadable() throws Exception {
        String materializer = P3hContractTestSupport.read("deploy/p3h/p3h-secret-materializer.sh");

        assertThat(materializer).contains(
                "chmod 400", "${app_uid}:${app_gid}:400", "P3H_SECRET_TARGET_MODE: 0400");
        assertThat(materializer).doesNotContain("chmod 444", "chmod 0444");
    }

    @Test
    void secretValuesAbsentFromDockerInspect() throws Exception {
        assertThat(runner()).contains(
                "docker inspect", "BLOCKED_SECRET_VALUE_EXPOSURE",
                "SECRET_VALUES_IN_DOCKER_INSPECT: ABSENT");
    }

    @Test
    void secretValuesAbsentFromProcessArguments() throws Exception {
        assertThat(runner()).contains(
                "compose ps --quiet", "docker top \"${running_container_id}\"",
                "done <\"${running_container_ids}\"",
                "SECRET_VALUES_IN_PROCESS_ARGUMENTS: ABSENT");
        assertThat(runner()).doesNotContain("compose top >");
    }

    @Test
    void appStillRunsAsNonRoot() throws Exception {
        String dockerfile = P3hContractTestSupport.read("deploy/p3h/Dockerfile.p3h");
        String compose = P3hContractTestSupport.read("deploy/p3h/docker-compose.p3h.yml");

        assertThat(dockerfile).contains("--uid 10001", "--gid 10001", "USER app");
        assertThat(compose).doesNotContain("app:\n    user: \"0:0\"");
    }

    private String runner() throws Exception {
        return P3hContractTestSupport.read("scripts/controlled-p3h-compose-offline-smoke.sh");
    }
}
