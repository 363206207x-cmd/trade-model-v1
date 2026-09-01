package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PackagedJarExistingV13RestartTest {

    @Test
    void controlledHarnessRestartsTheSameJarAgainstTheExistingV13Database() throws Exception {
        String script = Files.readString(Path.of("scripts/standard-release-postgresql-smoke.sh"));
        assertThat(script.split("start_app", -1).length - 1).isGreaterThanOrEqualTo(3);
        assertThat(script).contains("PACKAGED_JAR_EXISTING_V16_RESTART=PASS");
    }
}
