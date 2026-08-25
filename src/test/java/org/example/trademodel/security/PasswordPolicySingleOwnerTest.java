package org.example.trademodel.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicySingleOwnerTest {

    @Test
    void bootstrapGeneratorAndPreflightDelegateToCanonicalPolicyWithoutShellRegex() throws Exception {
        String bootstrap = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/security/PersonalUserBootstrap.java"));
        String generator = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/security/RuntimePasswordTool.java"));
        String preflight = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/config/TargetRuntimePreflight.java"));
        String generatorScript = Files.readString(Path.of("scripts/generate-runtime-password.sh"));
        String preflightScript = Files.readString(Path.of("scripts/target-runtime-preflight.sh"));

        assertThat(bootstrap).contains("InitialPasswordPolicy.validate(initialPassword, username)");
        assertThat(generator).contains("InitialPasswordPolicy.validate(password,");
        assertThat(preflight).contains("InitialPasswordPolicy.validate(");
        assertThat(generatorScript).contains("RuntimePasswordTool generate")
                .doesNotContain("PASSWORD_REGEX", "MIN_PASSWORD_LENGTH");
        assertThat(preflightScript).contains("TargetRuntimePreflight")
                .doesNotContain("PASSWORD_REGEX", "MIN_PASSWORD_LENGTH");
    }
}
