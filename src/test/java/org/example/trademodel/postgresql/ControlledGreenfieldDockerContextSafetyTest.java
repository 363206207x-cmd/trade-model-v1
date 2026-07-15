package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ControlledGreenfieldDockerContextSafetyTest {

    private static final Path CHECKER = Path.of(
            "scripts/check-docker-context-safety.sh").toAbsolutePath();

    @TempDir
    Path tempDir;

    @Test
    void dockerContextExcludesRuntimeDumpAttestationAndEnv() throws Exception {
        String dockerIgnore = Files.readString(Path.of(".dockerignore"), StandardCharsets.UTF_8);

        assertThat(dockerIgnore).contains(
                ".runtime", ".runtime/**", "backups", "backups/**",
                "*.dump", "*.attestation", ".env", ".env.*", "*.env", "!.env.example");
    }

    @Test
    void applicationImageBuildUsesExactCommittedGitArchive() throws Exception {
        String runner = Files.readString(Path.of(
                "scripts/controlled-greenfield-first-boot-rehearsal-p3g.sh"), StandardCharsets.UTF_8);
        String dockerfile = Files.readString(Path.of("Dockerfile"), StandardCharsets.UTF_8);

        assertThat(runner).contains(
                "git status --porcelain",
                "git archive --format=tar --output=\"${ARCHIVE_TAR}\" \"${CURRENT_HEAD}\"",
                "scripts/check-docker-context-safety.sh",
                "--label \"org.opencontainers.image.revision=${CURRENT_HEAD}\"",
                "APP_IMAGE_SOURCE_HEAD: ${CURRENT_HEAD}");
        assertThat(runner).doesNotContain("docker build .");
        assertThat(dockerfile).contains(
                "--mount=type=cache,target=/root/.m2,sharing=locked",
                "-Dmaven.test.skip=true package",
                "COPY --from=build /workspace/target/*.jar /app/app.jar");
        assertThat(runner).contains(
                "--mount \"type=bind,src=${ARCHIVE_CONTEXT},dst=/repo,readonly\"",
                "--mount \"type=bind,src=${MAVEN_DISTRIBUTION_DIR},dst=/opt/maven,readonly\"",
                "--mount \"type=bind,src=${MAVEN_REPOSITORY_DIR},dst=/maven-repository,readonly\"",
                "find /root/.m2/repository -name _remote.repositories -delete",
                "P3G_CONTROLLED_POSTGRESQL_JDBC_URL=jdbc:postgresql://127.0.0.1:${P3G_POSTGRES_PORT}/${PRIMARY_DATABASE}");
        assertThat(runner).doesNotContain(".m2/settings.xml");
    }

    @Test
    void untrackedRuntimeEvidenceNeverEntersBuildContext() throws Exception {
        Path safeContext = tempDir.resolve("safe-context");
        Files.createDirectories(safeContext);
        Files.writeString(safeContext.resolve("Dockerfile"), "FROM scratch\n");
        ScriptResult safe = run(safeContext);
        assertThat(safe.exitCode()).isZero();
        assertThat(safe.output()).contains("DOCKER_CONTEXT_SAFETY: PASS_EXACT_ARCHIVE_CONTEXT");

        Path unsafeContext = tempDir.resolve("unsafe-context");
        Files.createDirectories(unsafeContext.resolve(".runtime"));
        Files.writeString(unsafeContext.resolve("Dockerfile"), "FROM scratch\n");
        Files.writeString(unsafeContext.resolve(".runtime/evidence.dump"), "not-a-real-dump");
        ScriptResult unsafe = run(unsafeContext);
        assertThat(unsafe.exitCode()).isEqualTo(2);
        assertThat(unsafe.output()).contains("DOCKER_CONTEXT_SAFETY: BLOCKED_FORBIDDEN_PATH");
    }

    @Test
    void secretEnvAndAttestationAreRejectedWithoutPrintingTheirPaths() throws Exception {
        Path context = tempDir.resolve("secret-context");
        Files.createDirectories(context);
        Files.writeString(context.resolve("Dockerfile"), "FROM scratch\n");
        Files.writeString(context.resolve("secret.env"), "SECRET=value\n");
        Files.writeString(context.resolve("release.attestation"), "private evidence\n");

        ScriptResult result = run(context);

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.output()).isEqualTo("DOCKER_CONTEXT_SAFETY: BLOCKED_FORBIDDEN_PATH\n");
        assertThat(result.output()).doesNotContain("SECRET", "release.attestation", context.toString());
    }

    private ScriptResult run(Path context) throws Exception {
        Process process = new ProcessBuilder("bash", CHECKER.toString(), context.toString())
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        assertThat(finished).isTrue();
        return new ScriptResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
