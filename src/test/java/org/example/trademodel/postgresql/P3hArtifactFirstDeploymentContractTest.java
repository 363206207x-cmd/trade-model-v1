package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class P3hArtifactFirstDeploymentContractTest {

    private static final String SOURCE_HEAD = "a".repeat(40);

    @TempDir
    Path tempDir;

    @Test
    void exactHeadArtifactBuilderCreatesApprovedThreeFileArchive() throws Exception {
        Path fakeRepo = Files.createDirectories(tempDir.resolve("repository"));
        Path scripts = Files.createDirectories(fakeRepo.resolve("scripts"));
        Path fakeSource = Files.createDirectories(tempDir.resolve("archived-source"));
        Path fakeBin = Files.createDirectories(tempDir.resolve("bin"));
        Path fakeJava = Files.createDirectories(tempDir.resolve("java17/bin"));
        Path output = tempDir.resolve("artifact-output");

        Files.copy(Path.of("scripts/p3h-build-exact-application-artifact.sh"),
                scripts.resolve("p3h-build-exact-application-artifact.sh"));
        Files.copy(Path.of("scripts/p3h-bounded-process.py"),
                scripts.resolve("p3h-bounded-process.py"));
        executable(scripts.resolve("p3h-build-exact-application-artifact.sh"));
        executable(scripts.resolve("p3h-bounded-process.py"));
        prepareArchivedSource(fakeSource);
        executable(fakeJava.resolve("java"), """
                #!/usr/bin/env bash
                printf '%s\n' 'openjdk version "17.0.99"'
                """);
        executable(fakeBin.resolve("git"), """
                #!/usr/bin/env bash
                set -euo pipefail
                args="$*"
                case "${args}" in
                  *'branch --show-current'*) printf '%s\n' 'codex/p3h-local-vm-staging-lab1' ;;
                  *'rev-parse HEAD'*) printf '%s\n' "${FAKE_SOURCE_HEAD}" ;;
                  *'rev-parse origin/codex/p3h-local-vm-staging-lab1'*)
                    printf '%s\n' "${FAKE_SOURCE_HEAD}"
                    ;;
                  *'status --porcelain --untracked-files=normal'*) : ;;
                  *'archive --format=tar'*)
                    output=''
                    for argument in "$@"; do
                      case "${argument}" in --output=*) output="${argument#--output=}" ;; esac
                    done
                    test -n "${output}"
                    tar -cf "${output}" -C "${FAKE_ARCHIVE_SOURCE}" .
                    ;;
                  *) exit 2 ;;
                esac
                """);

        ProcessBuilder builder = new ProcessBuilder(
                "bash", scripts.resolve("p3h-build-exact-application-artifact.sh").toString())
                .redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.put("PATH", fakeBin + ":" + environment.get("PATH"));
        environment.put("FAKE_SOURCE_HEAD", SOURCE_HEAD);
        environment.put("FAKE_ARCHIVE_SOURCE", fakeSource.toString());
        environment.put("P3H_EXPECTED_HEAD", SOURCE_HEAD);
        environment.put("P3H_ARTIFACT_BUILD_CONFIRM",
                "I_CONFIRM_BUILD_EXACT_HEAD_APPLICATION_ARTIFACT");
        environment.put("P3H_ARTIFACT_OUTPUT_DIR", output.toString());
        environment.put("P3H_JAVA17_HOME", fakeJava.getParent().toString());

        ProcessResult result = finish(builder.start(), Duration.ofSeconds(20));

        assertThat(result.exitCode()).as(result.output()).isZero();
        assertThat(result.output()).contains(
                "P3H_ARTIFACT_BUILD: PASS_EXACT_HEAD",
                "APP_ARTIFACT_SOURCE_HEAD: " + SOURCE_HEAD,
                "APP_JAR_SHA256:",
                "APP_ARTIFACT_ARCHIVE_SHA256:");
        Path archive = output.resolve("p3h-application-artifact-" + SOURCE_HEAD + ".tar");
        assertThat(archive).isRegularFile();
        ProcessResult listing = finish(new ProcessBuilder("tar", "-tf", archive.toString())
                .redirectErrorStream(true).start(), Duration.ofSeconds(5));
        assertThat(listing.output().lines().toList()).containsExactly(
                "app.jar", "Dockerfile.runtime.p3h", "artifact-metadata.txt");
        ProcessResult metadata = finish(new ProcessBuilder(
                "tar", "-xOf", archive.toString(), "artifact-metadata.txt")
                .redirectErrorStream(true).start(), Duration.ofSeconds(5));
        assertThat(metadata.output()).contains(
                "SOURCE_HEAD=" + SOURCE_HEAD,
                "JAVA_VERSION=17.0.99",
                "MAVEN_VERSION=3.9.9");
        assertThat(metadata.output()).doesNotContain(
                fakeRepo.toString(), fakeSource.toString(), "API_KEY", "PASSWORD");
    }

    @Test
    void runtimeOnlyDockerfileIsPinnedNonRootAndBuildFree() throws Exception {
        String dockerfile = Files.readString(
                Path.of("deploy/p3h/Dockerfile.runtime.p3h"), StandardCharsets.UTF_8);

        assertThat(dockerfile).contains(
                "eclipse-temurin:17-jre-jammy@sha256:",
                "ARG VCS_REF",
                "ARG APP_JAR_SHA256",
                "org.opencontainers.image.revision",
                "org.example.trademodel.app-jar-sha256",
                "COPY --chown=10001:10001 app.jar /app/app.jar",
                "USER 10001:10001");
        assertThat(dockerfile).doesNotContain(
                "mvnw", "maven", "COPY src", "COPY .mvn", "RUN ./mvnw");
    }

    @Test
    void hostRunnerOrdersArtifactBuildTransferPrefetchAndRuntimeBuild() throws Exception {
        String runner = Files.readString(
                Path.of("scripts/controlled-staging-readonly-deployment-p3h-r1.sh"),
                StandardCharsets.UTF_8);
        String remote = Files.readString(
                Path.of("deploy/p3h/lima/p3h-lab-r1-remote.sh"), StandardCharsets.UTF_8);

        assertThat(runner.indexOf("set_stage remote-preflight"))
                .isLessThan(runner.indexOf("set_stage exact-source-archive"));
        assertThat(runner.indexOf("set_stage exact-source-archive"))
                .isLessThan(runner.indexOf("set_stage application-artifact-build-on-host"));
        assertThat(runner.indexOf("set_stage application-artifact-build-on-host"))
                .isLessThan(runner.indexOf("set_stage application-artifact-upload"));
        assertThat(runner.indexOf("set_stage application-artifact-upload"))
                .isLessThan(runner.indexOf("set_stage application-image-build"));
        assertThat(runner).contains("APP_ARTIFACT_BUILD: PASS_EXACT_HEAD");
        assertThat(remote.indexOf("CURRENT_REMOTE_STEP=APPLICATION_ARTIFACT_SHA_VERIFY"))
                .isLessThan(remote.indexOf("CURRENT_REMOTE_STEP=RUNTIME_IMAGE_PREFETCH"));
        assertThat(remote.indexOf("CURRENT_REMOTE_STEP=RUNTIME_IMAGE_PREFETCH"))
                .isLessThan(remote.indexOf("CURRENT_REMOTE_STEP=IMAGE_BUILD"));
    }

    private void prepareArchivedSource(Path source) throws IOException {
        Path wrapper = Files.createDirectories(source.resolve(".mvn/wrapper"));
        Files.writeString(wrapper.resolve("maven-wrapper.properties"), """
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
                """, StandardCharsets.UTF_8);
        Path deploy = Files.createDirectories(source.resolve("deploy/p3h"));
        Files.copy(Path.of("deploy/p3h/Dockerfile.runtime.p3h"),
                deploy.resolve("Dockerfile.runtime.p3h"));
        executable(source.resolve("mvnw"), """
                #!/usr/bin/env bash
                set -euo pipefail
                mkdir -p target
                python3 - <<'PY'
                from pathlib import Path
                import zipfile
                target = Path("target/trade-model-v1-test.jar")
                with zipfile.ZipFile(target, "w") as jar:
                    jar.writestr(
                        "META-INF/MANIFEST.MF",
                        "Main-Class: org.springframework.boot.loader.launch.JarLauncher\\n"
                        "Start-Class: org.example.trademodel.TradeModelApplication\\n",
                    )
                    jar.writestr("BOOT-INF/", b"")
                    jar.writestr("BOOT-INF/classes/application.yml", b"safe-placeholder-only")
                    jar.writestr(
                        "org/springframework/boot/loader/launch/JarLauncher.class",
                        b"offline-fixture",
                    )
                PY
                """);
    }

    private Path executable(Path path) throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
        return path;
    }

    private Path executable(Path path, String content) throws IOException {
        Files.writeString(path, content.stripIndent(), StandardCharsets.UTF_8);
        return executable(path);
    }

    private ProcessResult finish(Process process, Duration timeout) throws Exception {
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(3, TimeUnit.SECONDS);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(finished).as(output).isTrue();
        return new ProcessResult(process.exitValue(), output);
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
