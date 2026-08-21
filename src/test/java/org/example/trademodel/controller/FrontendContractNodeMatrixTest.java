package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class FrontendContractNodeMatrixTest {

    @Test
    void productionFrontendContractPassesExecutableNodeStateMatrix() throws Exception {
        Process process = new ProcessBuilder("node", "scripts/frontend-contract-state-matrix.mjs")
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(completed).as(output).isTrue();
        assertThat(process.exitValue()).as(output).isZero();
        assertThat(output).contains("FRONTEND_CONTRACT_STATE_MATRIX=PASS");
    }
}
