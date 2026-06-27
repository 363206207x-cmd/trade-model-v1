package org.example.trademodel.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProdSmokeScriptHealthTest {

    @Test
    void prodSmokeScriptHasValidShellSyntax() throws Exception {
        Path script = Path.of("scripts", "prod-smoke.sh");
        Process process = new ProcessBuilder("bash", "-n", script.toString())
                .redirectErrorStream(true)
                .start();

        String output = readOutput(process);
        int exitCode = process.waitFor();

        assertThat(exitCode)
                .as(output)
                .isZero();
    }

    private static String readOutput(Process process) throws IOException {
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
