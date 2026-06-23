package org.example.trademodel.service.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DirectAssemblerBypassGuardTest {
    @Test
    void legacyDirectAssemblerEntryAndReflectionSaveBypassAreDisabled() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImpl.java"));

        assertThat(source).contains("public AssetAnalysisVO assemble(String symbol, String timeframe)");
        assertThat(source).contains("throw new IllegalStateException(\"DIRECT_ASSEMBLER_ENTRY_DISABLED\")");
        assertThat(source).doesNotContain("LEGACY_DIRECT_ASSEMBLE");
        assertThat(source).doesNotContain("LEGACY_REFLECTION_SAVE");
        assertThat(source).doesNotContain("LocalDateTime analysisTime = LocalDateTime.now();\n        AnalysisExecutionContext context = new AnalysisExecutionContext");
    }
}
