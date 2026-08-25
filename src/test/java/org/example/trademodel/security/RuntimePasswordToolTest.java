package org.example.trademodel.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class RuntimePasswordToolTest {

    @Test
    void generatedPasswordsAlwaysUseTheExactCanonicalLength() {
        SecureRandom random = new SecureRandom();
        for (int attempt = 0; attempt < 200; attempt++) {
            String value = RuntimePasswordTool.generate(8, random);
            assertThat(value).hasSize(8);
            assertThat(InitialPasswordPolicy.validate(value).accepted()).isTrue();
        }
    }

    @Test
    void generatorRejectsEveryRequestedLengthOtherThanEight() {
        SecureRandom random = new SecureRandom();
        assertThatThrownBy(() -> RuntimePasswordTool.generate(7, random))
                .hasMessage("PASSWORD_LENGTH_MUST_EQUAL_8");
        assertThatThrownBy(() -> RuntimePasswordTool.generate(9, random))
                .hasMessage("PASSWORD_LENGTH_MUST_EQUAL_8");
    }

    @Test
    void generatorNeverPrintsAPasswordWhenNoPrivateFileIsProvided(CapturedOutput output) {
        assertThatThrownBy(() -> RuntimePasswordTool.main(new String[]{"generate"}))
                .hasMessage("PASSWORD_ENV_FILE_REQUIRED");
        assertThat(output).doesNotContain("RUNTIME_PASSWORD=", "PASSWORD_DISPLAYED_ONCE");
    }

    @Test
    void envFileIsCreatedOnceWithOwnerOnlyPermissionsAndNoTemporaryFile(@TempDir Path directory,
                                                                        CapturedOutput output)
            throws Exception {
        Path target = directory.resolve("runtime-password.env");

        RuntimePasswordTool.main(new String[]{"generate", "--env-file", target.toString()});

        assertThat(Files.readString(target)).startsWith("TRADE_MODEL_INITIAL_PASSWORD='")
                .endsWith("'\n");
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(target)))
                .isEqualTo("rw-------");
        try (var files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactly("runtime-password.env");
        }
        assertThat(output).contains("PASSWORD_ENV_FILE=CREATED", "PASSWORD_ENV_FILE_PERMISSION=0600")
                .doesNotContain("TRADE_MODEL_INITIAL_PASSWORD='");
    }
}
