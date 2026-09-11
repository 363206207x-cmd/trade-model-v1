package org.example.trademodel.postgresql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Local fake-root/disposable-DB contracts only; never an installer or a real credential fixture. */
@Tag("core-regression")
class NativeStagingAssetCardInfrastructureContractTest {
    private static final Path SOURCE = Path.of("deploy/native-staging").toAbsolutePath();
    private static final String WRITER_MAIN="org.example.trademodel.assetcard.AssetCardDataSourceConfiguration";
    @TempDir Path temporary;
    record Result(int code, String output) {}
    record Fixture(Path root, Path manifest) {}

    @Test void allNativeAttachmentsAreDefinedWithoutReplacingTheExternalBaseChain() throws Exception {
        for (String name : List.of("README.md", "rine-logic-asset-card.conf.template", "asset-card-role-bootstrap.sql",
                "asset-card-role-verify.sql", "asset-card-runtime-credentials.sh", "asset-card-model-install.sh",
                "asset-card-runtime-preflight.sh", "asset-card-runtime-manifest.template", "asset-card-runtime-install.sh"))
            assertThat(SOURCE.resolve(name)).isRegularFile();
        String dropin=Files.readString(SOURCE.resolve("rine-logic-asset-card.conf.template"));
        assertThat(dropin).contains("LoadCredential=", "ReadOnlyPaths=", "MODEL_MODE=SHADOW", "WRITER_ENABLED=@CARD_ENABLED@");
        assertThat(dropin).doesNotContain("ExecStart=", "EnvironmentFile=", "active.env", "ai.env");
        for (String name : List.of("asset-card-runtime-install.sh", "asset-card-model-install.sh", "asset-card-runtime-credentials.sh")) {
            String source=Files.readString(SOURCE.resolve(name));
            assertThat(source).doesNotContain("systemctl ", "daemon-reload", "PGPASSWORD=", "eval ");
        }
    }

    @Test void defaultDryRunPreservesFilesAndBaseHashMismatchFailsBeforeAnyInstaller() throws Exception {
        Fixture fixture=fixture();
        Result good=run("asset-card-runtime-install.sh", fixture);
        assertThat(good.code()).isZero();
        assertThat(good.output()).contains("ACTION=DRY_RUN", "SYSTEMD_CHANGE_EXECUTION=NO");
        assertThat(fixture.root().resolve("etc/systemd/system/rine-logic.service.d/40-asset-card.conf")).doesNotExist();
        Files.writeString(fixture.root().resolve("etc/systemd/system/rine-logic.service"), "CHANGED_TEST_BASE");
        Result bad=run("asset-card-runtime-install.sh", fixture);
        assertThat(bad.code()).isNotZero();
        assertThat(bad.output()).contains("BASE_IDENTITY_MISMATCH");
        assertThat(bad.output()).doesNotContain("CHANGED_TEST_BASE");
    }

    @Test void manifestIsDataNotShellAndUnknownOrDuplicateFieldsFailClosed() throws Exception {
        Fixture fixture=fixture();
        Files.writeString(fixture.manifest(), Files.readString(fixture.manifest())+"TARGET_ARCH=x86_64\n");
        assertThat(run("asset-card-runtime-install.sh",fixture).output()).contains("MANIFEST_INVALID");
        fixture=fixture();
        Files.writeString(fixture.manifest(),Files.readString(fixture.manifest())+"EVIL=$(touch /never-run)\n");
        assertThat(run("asset-card-runtime-install.sh",fixture).output()).contains("MANIFEST_INVALID");
    }

    @Test void releaseMetadataCannotRedirectHashingToAnArbitrarySecretFile() throws Exception {
        Fixture fixture=fixture();
        Files.writeString(fixture.manifest(), Files.readString(fixture.manifest())
                .replace("RELEASE_METADATA=/opt/rine-logic/current/deployment-metadata.txt", "RELEASE_METADATA=/opt/rine-logic/password-backup"));
        write(fixture.root().resolve("opt/rine-logic/password-backup"),"SYNTHETIC_SECRET_MUST_NOT_BE_HASHED","rw-------");
        Result denied=run("asset-card-runtime-preflight.sh",fixture);
        assertThat(denied.code()).isNotZero();
        assertThat(denied.output()).contains("RELEASE_METADATA_INVALID").doesNotContain("SYNTHETIC_SECRET_MUST_NOT_BE_HASHED");
    }

    @Test void deploymentMetadataRequiresExactThreeFieldsFullIdentitiesAndARealUtcInstant() throws Exception {
        Fixture fixture=fixture();
        String valid=Files.readString(fixture.root().resolve("opt/rine-logic/current/deployment-metadata.txt"));
        Result accepted=run("asset-card-runtime-install.sh",fixture);
        assertThat(accepted.code()).withFailMessage(accepted.output()).isZero();
        assertThat(accepted.output()).contains("ACTION=DRY_RUN");
        for(String malformed:List.of(
                valid.replace("a".repeat(40),"a".repeat(39)),
                valid.replace("a".repeat(40),"g".repeat(40)),
                valid.replaceAll("(?m)^ARTIFACT_SHA256=.*$","ARTIFACT_SHA256="+"b".repeat(63)),
                valid.replaceAll("(?m)^ARTIFACT_SHA256=.*$","ARTIFACT_SHA256="+"b".repeat(64)),
                valid.replace("2026-09-10T09:59:10Z","2026-09-10T09:59:10+08:00"),
                valid.replace("2026-09-10T09:59:10Z","2026-02-30T09:59:10Z"),
                valid.replace("2026-09-10T09:59:10Z","2025-02-29T09:59:10Z"),
                valid.replace("2026-09-10T09:59:10Z","2026-09-10T24:59:10Z"),
                valid.replace("2026-09-10T09:59:10Z","2026-09-10 09:59:10"),
                valid.replaceAll("(?m)^DEPLOYED_AT=.*\\n?",""),
                valid+"MERGED_MAIN_SHA="+"a".repeat(40)+"\n",
                valid+"UNKNOWN=value\n", valid+"\n", valid.replace("\n","\r\n"))) {
            replaceDeploymentMetadata(fixture,malformed);
            Result denied=run("asset-card-runtime-install.sh",fixture);
            assertThat(denied.code()).as("Invalid deployment metadata must fail closed").isNotZero();
            assertThat(denied.output()).contains("RELEASE_METADATA_INVALID");
            assertThat(fixture.root().resolve("etc/systemd/system/rine-logic.service.d/40-asset-card.conf")).doesNotExist();
        }
        replaceDeploymentMetadata(fixture,valid.replace("2026-09-10T09:59:10Z","2024-02-29T23:59:59Z"));
        assertThat(run("asset-card-runtime-install.sh",fixture).code()).isZero();
    }

    @Test void deploymentMetadataMustResolveBesideTheSameProtectedReleaseJar() throws Exception {
        Fixture fixture=fixture();
        Path current=fixture.root().resolve("opt/rine-logic/current");
        Path release=fixture.root().resolve("opt/rine-logic/releases/test-release");
        Files.createDirectories(release.getParent()); Files.move(current,release);
        Files.createSymbolicLink(current,Path.of("releases/test-release"));
        Result accepted=run("asset-card-runtime-install.sh",fixture);
        assertThat(accepted.code()).withFailMessage(accepted.output()).isZero();
        Path metadata=release.resolve("deployment-metadata.txt");
        Path outside=fixture.root().resolve("outside-metadata.txt");
        Files.move(metadata,outside); Files.createSymbolicLink(metadata,outside);
        Result denied=run("asset-card-runtime-install.sh",fixture);
        assertThat(denied.code()).isNotZero();
        assertThat(denied.output()).contains("SYMLINK_FORBIDDEN");
    }

    @Test void fakeRootCannotReadAnOutsideManifestOrAnUnprotectedManifest() throws Exception {
        Fixture fixture=fixture();
        Path outside=temporary.resolve("outside.manifest");
        write(outside,"THIS_MUST_NOT_BE_PARSED\n","rw-------");
        assertThat(run("asset-card-runtime-install.sh",new Fixture(fixture.root(),outside)).output()).contains("TEST_ROOT_INVALID");
        Files.setPosixFilePermissions(fixture.manifest(),PosixFilePermissions.fromString("rw-rw-rw-"));
        assertThat(run("asset-card-runtime-install.sh",fixture).output()).contains("PERMISSION_INVALID");
    }

    @Test void credentialChecksNeverReadPrintOrHashTheSecretAndRejectUnsafeMetadata() throws Exception {
        Fixture fixture=fixture(); Path secret=fixture.root().resolve("etc/rine-logic/credentials/asset-card-db-password");
        Result good=run("asset-card-runtime-credentials.sh",fixture);
        assertThat(good.code()).isZero();
        assertThat(good.output()).contains("ACTION=CHECK_ONLY", "CREDENTIAL_METADATA=PASS");
        assertThat(good.output()).doesNotContain("SYNTHETIC_PASSWORD_SENTINEL",sha(secret));
        Files.writeString(secret, "");
        assertThat(run("asset-card-runtime-credentials.sh",fixture).output()).contains("CREDENTIAL_METADATA_INVALID");
        Files.writeString(secret, "SYNTHETIC_PASSWORD_SENTINEL");
        Files.setPosixFilePermissions(secret,PosixFilePermissions.fromString("rw-r--r--"));
        assertThat(run("asset-card-runtime-credentials.sh",fixture).output()).contains("CREDENTIAL_METADATA_INVALID");
        Files.delete(secret); Files.createSymbolicLink(secret,fixture.root().resolve("app.jar"));
        assertThat(run("asset-card-runtime-credentials.sh",fixture).output()).contains("SYMLINK_FORBIDDEN");
    }

    @Test void credentialMetadataRejectsWrongExpectedOwnerWithoutChangingFileOwnership() throws Exception {
        Fixture fixture=fixture();
        Path secret=fixture.root().resolve("etc/rine-logic/credentials/asset-card-db-password");
        String actualOwner=Files.getAttribute(secret,"unix:uid").toString();
        String wrongOwner=Long.toString(Long.parseLong(actualOwner)+1);
        var process=new ProcessBuilder("bash","-c","source \"$1\"; ROOT_UID=\"$2\"; native_credential_metadata \"$3\"",
                "local-metadata-fixture",SOURCE.resolve("asset-card-runtime-preflight.sh").toString(),wrongOwner,secret.toString())
                .redirectErrorStream(true).start();
        assertThat(process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).isNotZero();
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(output).contains("CREDENTIAL_METADATA_INVALID").doesNotContain("SYNTHETIC_PASSWORD_SENTINEL",sha(secret));
        assertThat(Files.getAttribute(secret,"unix:uid").toString()).isEqualTo(actualOwner);
        assertThat(Files.readString(secret)).isEqualTo("SYNTHETIC_PASSWORD_SENTINEL");
    }

    @Test void failedFreshCredentialVerificationPreservesOldCredentialAndDoesNotRestart() throws Exception {
        Fixture fixture=fixture(); Path old=fixture.root().resolve("etc/rine-logic/credentials/asset-card-db-password");
        Path candidate=old.getParent().resolve("candidate-password"); write(candidate,"SYNTHETIC_NEW_PASSWORD", "rw-------");
        Result denied=run("asset-card-runtime-credentials.sh",fixture,"--rotate","--candidate",candidate.toString(),
                "--confirm","PREPARE_OR_ROTATE_ASSET_CARD_CREDENTIAL_ONLY");
        assertThat(denied.code()).isNotZero();
        assertThat(denied.output()).contains("FRESH_CREDENTIAL_VERIFICATION_FAILED");
        assertThat(Files.readString(old)).isEqualTo("SYNTHETIC_PASSWORD_SENTINEL");
        assertThat(denied.output()).doesNotContain("SYNTHETIC_NEW_PASSWORD", "SYNTHETIC_PASSWORD_SENTINEL");
        try (var paths=Files.walk(fixture.root())) {
            assertThat(paths.filter(p->p.getFileName().toString().contains("pending-credential")).toList()).isEmpty();
        }
    }

    @Test void realCredentialCliAuthenticatesFreshConnectionsAndFailedRotationPreservesTheWorkingCredential() throws Exception {
        boolean docker;
        try { docker= DockerClientFactory.instance().isDockerAvailable(); } catch (RuntimeException unavailable) { docker=false; }
        assumeTrue(docker,"Disposable PostgreSQL unavailable; real credential integration not executed and no external fallback");
        try(var database=new PostgreSQLContainer<>("postgres:16-alpine")
                .withEnv("POSTGRES_HOST_AUTH_METHOD","scram-sha-256")
                .withCommand("postgres","-c","password_encryption=scram-sha-256")) {
            database.start();
            for(String file:List.of("asset-card-role-bootstrap.sql","asset-card-role-verify.sql"))
                database.copyFileToContainer(MountableFile.forHostPath(SOURCE.resolve(file)),"/tmp/"+file);
            database.copyFileToContainer(MountableFile.forHostPath(Path.of("src/main/resources/db/migration/V24__asset_card_live_signal.sql")),"/tmp/actual-v24.sql");
            assertThat(file(database,"actual-v24.sql").getExitCode()).isZero();
            assertThat(sql(database,"REVOKE TEMP ON DATABASE test FROM PUBLIC; CREATE TABLE public.existing_private_business(id int);"
                    +"INSERT INTO public.existing_private_business VALUES (42); CREATE SEQUENCE public.existing_private_sequence;").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-bootstrap.sql").getExitCode()).isZero();
            String oldPassword="LOCAL_ONLY_OLD_"+java.util.UUID.randomUUID();
            String badPassword="LOCAL_ONLY_BAD_"+java.util.UUID.randomUUID();
            String newPassword="LOCAL_ONLY_NEW_"+java.util.UUID.randomUUID();
            setDisposableWriterPassword(database,oldPassword);
            String databaseBaseline=sql(database,"SELECT oid,relacl::text FROM pg_class WHERE relname IN ('existing_private_business','existing_private_sequence') ORDER BY oid").getStdout();
            Fixture fixture=realCredentialFixture(database);
            Path current=fixture.root().resolve("etc/rine-logic/credentials/asset-card-db-password");
            write(current,oldPassword,"rw-------");
            Result initial=realCredentialCli(fixture,current,"test",Files.getAttribute(current,"unix:uid").toString());
            assertSafeCredentialOutput(initial,oldPassword,badPassword,newPassword);
            assertThat(initial.code()).isZero();
            assertThat(initial.output()).contains("ASSET_CARD_WRITER_VERIFY=PASS");

            // A new process has no old connection pool: the wrong password must fail real SCRAM authentication.
            Path candidate=current.getParent().resolve("candidate-password");
            write(candidate,badPassword,"rw-------");
            Result badAuth=realCredentialCli(fixture,candidate,"test",Files.getAttribute(candidate,"unix:uid").toString());
            assertSafeCredentialOutput(badAuth,oldPassword,badPassword,newPassword);
            assertThat(badAuth.code()).isNotZero();
            assertThat(badAuth.output()).contains("ASSET_CARD_WRITER_VERIFY=FAIL");
            Result failedRotation=run("asset-card-runtime-credentials.sh",fixture,"--rotate","--candidate",candidate.toString(),
                    "--confirm","PREPARE_OR_ROTATE_ASSET_CARD_CREDENTIAL_ONLY");
            assertSafeCredentialOutput(failedRotation,oldPassword,badPassword,newPassword);
            assertThat(failedRotation.code()).isNotZero();
            assertThat(failedRotation.output()).contains("FRESH_CREDENTIAL_VERIFICATION_FAILED").doesNotContain("FRESH_CONNECTION=PASS");
            assertThat(Files.readString(current).equals(oldPassword)).as("Failed authentication preserves the old credential bytes").isTrue();
            assertThat(realCredentialCli(fixture,current,"test",Files.getAttribute(current,"unix:uid").toString()).code()).isZero();
            try(var paths=Files.list(current.getParent())) {
                assertThat(paths.filter(p->p.getFileName().toString().contains(".previous.")).toList()).isEmpty();
            }

            Result badDatabase=realCredentialCli(fixture,current,"wrong_expected_database",Files.getAttribute(current,"unix:uid").toString());
            assertSafeCredentialOutput(badDatabase,oldPassword,badPassword,newPassword);
            assertThat(badDatabase.code()).isNotZero();
            Result badOwner=realCredentialCli(fixture,current,"test",Long.toString(((Number)Files.getAttribute(current,"unix:uid")).longValue()+1));
            assertSafeCredentialOutput(badOwner,oldPassword,badPassword,newPassword);
            assertThat(badOwner.code()).isNotZero();

            // Authentication succeeds but an extra card-table privilege must still reject rotation.
            write(candidate,oldPassword,"rw-------");
            assertThat(sql(database,"GRANT DELETE ON public.tm_asset_card_snapshot TO rine_asset_card_writer").getExitCode()).isZero();
            Result badAcl=run("asset-card-runtime-credentials.sh",fixture,"--rotate","--candidate",candidate.toString(),
                    "--confirm","PREPARE_OR_ROTATE_ASSET_CARD_CREDENTIAL_ONLY");
            assertSafeCredentialOutput(badAcl,oldPassword,badPassword,newPassword);
            assertThat(badAcl.code()).isNotZero();
            assertThat(badAcl.output()).contains("FRESH_CREDENTIAL_VERIFICATION_FAILED");
            assertThat(Files.readString(current).equals(oldPassword)).as("Failed ACL verification preserves the old credential bytes").isTrue();
            assertThat(sql(database,"REVOKE DELETE ON public.tm_asset_card_snapshot FROM rine_asset_card_writer").getExitCode()).isZero();
            assertThat(realCredentialCli(fixture,current,"test",Files.getAttribute(current,"unix:uid").toString()).code()).isZero();

            // Only this disposable database's admin provisions the new password; the installer never changes roles.
            setDisposableWriterPassword(database,newPassword);
            write(candidate,newPassword,"rw-------");
            Result changed=run("asset-card-runtime-credentials.sh",fixture,"--rotate","--candidate",candidate.toString(),
                    "--confirm","PREPARE_OR_ROTATE_ASSET_CARD_CREDENTIAL_ONLY");
            assertSafeCredentialOutput(changed,oldPassword,badPassword,newPassword);
            assertThat(changed.code()).isZero();
            assertThat(changed.output()).contains("FRESH_CONNECTION=PASS","RESTART_REQUIRED=YES","HOT_ROTATION=NOT_CLAIMED","SYSTEMD_CHANGE_EXECUTION=NO");
            assertThat(Files.readString(current).equals(newPassword)).as("Successful verified rotation installs the new credential bytes").isTrue();
            assertThat(candidate).doesNotExist();
            assertThat(realCredentialCli(fixture,current,"test",Files.getAttribute(current,"unix:uid").toString()).code()).isZero();
            try(var paths=Files.list(current.getParent())) {
                List<Path> backups=paths.filter(p->p.getFileName().toString().contains(".previous.")).toList();
                assertThat(backups).hasSize(1);
                assertThat(Files.readString(backups.get(0)).equals(oldPassword)).as("Protected rollback inode retains the prior credential bytes").isTrue();
                assertThat(Files.getPosixFilePermissions(backups.get(0))).isEqualTo(PosixFilePermissions.fromString("rw-------"));
            }
            assertThat(sql(database,"SELECT oid,relacl::text FROM pg_class WHERE relname IN ('existing_private_business','existing_private_sequence') ORDER BY oid").getStdout()).isEqualTo(databaseBaseline);
            assertThat(sql(database,"SELECT id FROM public.existing_private_business").getStdout()).isEqualTo("42\n");
            assertThat(sql(database,"SELECT (SELECT count(*) FROM public.tm_asset_card_snapshot)+(SELECT count(*) FROM public.tm_asset_card_spot_bar)+(SELECT count(*) FROM public.tm_asset_card_feature_history)").getStdout()).isEqualTo("0\n");
            assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isZero();
        }
    }

    @Test void unqualifiedModelNeverCreatesAProductionPointerEvenWithApplyConfirmation() throws Exception {
        Fixture fixture=fixture(); Path candidate=fixture.root().resolve("model-candidate"); Files.createDirectory(candidate);
        write(candidate.resolve("manifest.json"),"TEST_FIXTURE_NOT_REAL_PRODUCTION", "r--------");
        Result denied=run("asset-card-model-install.sh",fixture,"--source",candidate.toString(),"--bundle-sha256",sha(candidate.resolve("manifest.json")),
                "--apply","--confirm","INSTALL_ASSET_CARD_MODEL_ONLY");
        assertThat(denied.code()).isNotZero();
        assertThat(denied.output()).contains("MODEL_NOT_QUALIFIED");
        assertThat(fixture.root().resolve("opt/rine-logic/models/asset-card/current")).doesNotExist();
    }

    @Test void fakeQualifiedModelRoutingUsesImmutableBundlesAndRetainsAnAtomicRollbackPointer() throws Exception {
        Fixture fixture=fixture();
        // This fake process tests filesystem routing only, not real-manifest qualification or native readiness.
        write(fixture.root().resolve("usr/bin/java"),"#!/bin/sh\nprintf '%s\\n' VERIFIED_BUNDLE_STATUS=PASS DATA_KIND=REAL_HISTORICAL\n","rwx------");
        Path first=modelFixture(fixture,"first"), second=modelFixture(fixture,"second");
        String firstSha=sha(first.resolve("manifest.json")), secondSha=sha(second.resolve("manifest.json"));
        Path modelRoot=fixture.root().resolve("opt/rine-logic/models/asset-card");
        try {
            Result dry=run("asset-card-model-install.sh",fixture,"--source",first.toString(),"--bundle-sha256",firstSha);
            assertThat(dry.code()).isZero();
            assertThat(modelRoot.resolve("current")).doesNotExist();
            for(Path source:List.of(first,second)) {
                Result applied=run("asset-card-model-install.sh",fixture,"--source",source.toString(),"--bundle-sha256",sha(source.resolve("manifest.json")),
                        "--apply","--confirm","INSTALL_ASSET_CARD_MODEL_ONLY");
                assertThat(applied.code()).withFailMessage(applied.output()).isZero();
                assertThat(applied.output()).contains("PRODUCTION_MODEL_READY=NO", "MODEL_MODE=SHADOW", "SYSTEMD_CHANGE_EXECUTION=NO");
            }
            assertThat(Files.readSymbolicLink(modelRoot.resolve("current")).toString()).isEqualTo("bundles/"+secondSha);
            assertThat(Files.readSymbolicLink(modelRoot.resolve("previous")).toString()).isEqualTo("bundles/"+firstSha);
            assertThat(Files.readString(modelRoot.resolve("bundles/"+firstSha+"/manifest.json"))).isEqualTo("SYNTHETIC_MODEL_FIXTURE_first");
            assertThat(Files.getPosixFilePermissions(modelRoot.resolve("bundles/"+secondSha))).isEqualTo(PosixFilePermissions.fromString("r-xr-xr-x"));
            assertThat(Files.getPosixFilePermissions(modelRoot.resolve("bundles/"+secondSha+"/long.ubj"))).isEqualTo(PosixFilePermissions.fromString("r--r--r--"));
            try(var paths=Files.list(modelRoot.resolve("bundles"))) {
                assertThat(paths.filter(p->p.getFileName().toString().startsWith(".pending.")).toList()).isEmpty();
            }
        } finally {
            // These are this test's immutable fixtures only; make them removable by @TempDir cleanup.
            if(Files.isDirectory(modelRoot.resolve("bundles"))) try(var paths=Files.list(modelRoot.resolve("bundles"))) {
                for(Path path:paths.toList()) if(Files.isDirectory(path)) Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rwx------"));
            }
        }
    }

    @Test void fakeRootDropinInstallRequiresConfirmationAndPreservesTheBaseAndPreviousCardDropin() throws Exception {
        Fixture fixture=fixture();
        Path base=fixture.root().resolve("etc/systemd/system/rine-logic.service"); String oldBase=Files.readString(base);
        Path card=fixture.root().resolve("etc/systemd/system/rine-logic.service.d/40-asset-card.conf");
        write(card,"SYNTHETIC_PREVIOUS_CARD_DROPIN","rw-r--r--");
        assertThat(run("asset-card-runtime-install.sh",fixture,"--apply").output()).contains("EXPLICIT_CONFIRMATION_REQUIRED");
        Result applied=run("asset-card-runtime-install.sh",fixture,"--apply","--confirm","INSTALL_ASSET_CARD_DROPIN_ONLY");
        assertThat(applied.code()).withFailMessage(applied.output()).isZero();
        assertThat(Files.readString(base)).isEqualTo(oldBase);
        assertThat(Files.readString(card)).contains("MODEL_MODE=SHADOW", "WRITER_ENABLED=false", "LoadCredential=").doesNotContain("ExecStart=", "@WRITER_JDBC_URL@");
        assertThat(applied.output()).contains("RESTART_REQUIRED=YES", "SYSTEMD_CHANGE_EXECUTION=NO");
        try(var paths=Files.list(card.getParent())) {
            List<Path> backups=paths.filter(p->p.getFileName().toString().startsWith("40-asset-card.conf.previous.")).toList();
            assertThat(backups).hasSize(1);
            assertThat(Files.readString(backups.get(0))).isEqualTo("SYNTHETIC_PREVIOUS_CARD_DROPIN");
        }
    }

    @Test void preparedAttachmentBindsAllCardSwitchesOffAndHasNoImplicitWindowOrRestart() throws Exception {
        Fixture fixture=fixture();
        Result applied=run("asset-card-runtime-install.sh",fixture,"--apply","--confirm","INSTALL_ASSET_CARD_DROPIN_ONLY");
        assertThat(applied.code()).withFailMessage(applied.output()).isZero();
        String rendered=Files.readString(cardDropin(fixture));
        assertThat(rendered).contains("ASSET_CARD_ENABLED=false", "EXTERNAL_CALLS_ENABLED=false", "WRITER_ENABLED=false", "RETENTION_ENABLED=false")
                .doesNotContain("COLLECTION_WINDOW_", "NOT_CONFIGURED", "@CARD_ENABLED@", "ExecStart=", "active.env", "ai.env");
        var properties=bindRenderedEnvironment(rendered);
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isExternalCallsEnabled()).isFalse();
        assertThat(properties.isWriterEnabled()).isFalse();
        assertThat(properties.isRetentionEnabled()).isFalse();
        assertThat(properties.getCollectionWindow().valid()).isFalse();
        assertThat(properties.getCollectionWindow().getStartsAt()).isNull();
        assertThat(properties.getModelMode()).isEqualTo(org.example.trademodel.assetcard.AssetCardProperties.ModelMode.SHADOW);
        assertThat(applied.output()).contains("CARD_STARTUP_MODE=PREPARED", "RESTART_REQUIRED=YES", "SYSTEMD_CHANGE_EXECUTION=NO");
    }

    @Test void armedAttachmentRequiresDistinctConfirmationAndBindsTheExactFiniteJavaPlan() throws Exception {
        Fixture fixture=armedFixture();
        String base=Files.readString(fixture.root().resolve("etc/systemd/system/rine-logic.service"));
        assertThat(run("asset-card-runtime-install.sh",fixture,"--apply","--confirm","INSTALL_ASSET_CARD_DROPIN_ONLY").output())
                .contains("EXPLICIT_CONFIRMATION_REQUIRED");
        assertThat(cardDropin(fixture)).doesNotExist();
        Result applied=run("asset-card-runtime-install.sh",fixture,"--apply","--confirm","INSTALL_ASSET_CARD_ARMED_WINDOW_ONLY");
        assertThat(applied.code()).withFailMessage(applied.output()).isZero();
        assertThat(applied.output()).contains("CARD_STARTUP_MODE=ARMED", "DATABASE_FILESYSTEM_EVIDENCE=REVIEWED_DECLARATION_ONLY",
                "VISIBLE_COLLECTION_DIRECTORIES_DEVICE_MATCH=YES", "RESTART_REQUIRED=YES", "SYSTEMD_CHANGE_EXECUTION=NO");
        assertThat(Files.readString(fixture.root().resolve("etc/systemd/system/rine-logic.service"))).isEqualTo(base);
        var properties=bindRenderedEnvironment(Files.readString(cardDropin(fixture)));
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isExternalCallsEnabled()).isTrue();
        assertThat(properties.isWriterEnabled()).isTrue();
        assertThat(properties.isRetentionEnabled()).isTrue();
        assertThat(properties.isTrainingExportEnabled()).isFalse();
        assertThat(properties.getModelMode()).isEqualTo(org.example.trademodel.assetcard.AssetCardProperties.ModelMode.SHADOW);
        assertThat(properties.getBarRetention()).isEqualTo(java.time.Duration.ofHours(168));
        assertThat(properties.getFeatureRetention()).isEqualTo(java.time.Duration.ofHours(720));
        assertThat(properties.getTradeRetention()).isEqualTo(java.time.Duration.ofHours(168));
        assertThat(properties.getLabelRetention()).isEqualTo(java.time.Duration.ofHours(2160));
        assertThat(properties.getRetentionBatchSize()).isEqualTo(128);
        assertThat(properties.getArchiveDirectory()).isEqualTo(Path.of("/var/lib/rine-logic/asset-card/archive"));
        assertThat(properties.getArchiveMinimumFreeBytes()).isEqualTo(21_474_836_480L);
        var plan=properties.getCollectionWindow();
        assertThat(plan.valid()).isTrue();
        assertThat(plan.getId()).isEqualTo("LOCAL_FIXTURE_WINDOW");
        assertThat(plan.getStartsAt()).isEqualTo(java.time.Instant.parse(manifestValue(fixture,"COLLECTION_STARTS_AT")));
        assertThat(plan.getEndsAt()).isEqualTo(plan.getStartsAt().plus(java.time.Duration.ofHours(8)));
        assertThat(plan.getSymbols()).containsExactlyInAnyOrder("BTCUSDT","ETHUSDT","XRPUSDT");
        assertThat(plan.getStateDirectory()).isEqualTo(Path.of("/var/lib/rine-logic/asset-card/collection"));
        assertThat(plan.getSharedIpWeightAllowancePerMinute()).isEqualTo(500);
        assertThat(plan.getSharedIpWeightLimitPerMinute()).isEqualTo(6000);
        assertThat(plan.getSharedIpHeadroomConfirmedAt()).isEqualTo(java.time.Instant.parse(manifestValue(fixture,"SHARED_IP_HEADROOM_CONFIRMED_AT")));
        assertThat(plan.getMaximumRestRequests()).isEqualTo(288);
        assertThat(plan.getMaximumRestWeight()).isEqualTo(72_000);
        assertThat(plan.getMaximumConnectionAttempts()).isEqualTo(32);
        assertThat(plan.getMaximumControlMessages()).isEqualTo(128);
        assertThat(plan.getMaximumNewRows()).isEqualTo(1_100_000);
        assertThat(plan.getMaximumDatabaseGrowthBytes()).isEqualTo(3_221_225_472L);
        assertThat(plan.getMaximumWalGrowthBytes()).isEqualTo(8_589_934_592L);
        assertThat(plan.getMinimumFreeBytes()).isEqualTo(21_474_836_480L);
        assertThat(properties.getWriter().getMaximumPoolSize()).isEqualTo(2);
        assertThat(properties.getWriter().getConnectionTimeout()).isEqualTo(java.time.Duration.ofSeconds(5));
    }

    @Test void unverifiedQuotaInvalidWindowOrExpandedBudgetCannotInstallAnArmedAttachment() throws Exception {
        Fixture fixture=armedFixture(); String valid=Files.readString(fixture.manifest());
        var invalid=java.util.Map.ofEntries(
                java.util.Map.entry("CARD_STARTUP_MODE","ACTIVE"),
                java.util.Map.entry("COLLECTION_WINDOW_ID","NOT_CONFIGURED"),
                java.util.Map.entry("COLLECTION_STARTS_AT","2026-02-30T12:00:00Z"),
                java.util.Map.entry("COLLECTION_ENDS_AT",java.time.Instant.parse(manifestValue(fixture,"COLLECTION_STARTS_AT")).plusSeconds(28801).toString()),
                java.util.Map.entry("SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE","0"),
                java.util.Map.entry("SHARED_IP_WEIGHT_LIMIT_PER_MINUTE","999"),
                java.util.Map.entry("SHARED_IP_HEADROOM_CONFIRMED_AT","NOT_CONFIGURED"),
                java.util.Map.entry("COLLECTION_SYMBOLS","BTCUSDT,BTCUSDT"),
                java.util.Map.entry("MAXIMUM_REST_REQUESTS","289"),
                java.util.Map.entry("MAXIMUM_REST_WEIGHT","72001"),
                java.util.Map.entry("MAXIMUM_CONNECTION_ATTEMPTS","33"),
                java.util.Map.entry("MAXIMUM_CONTROL_MESSAGES","129"),
                java.util.Map.entry("MAXIMUM_NEW_ROWS","1100001"),
                java.util.Map.entry("MAXIMUM_DATABASE_GROWTH_BYTES","3221225473"),
                java.util.Map.entry("MAXIMUM_WAL_GROWTH_BYTES","8589934593"),
                java.util.Map.entry("MINIMUM_FREE_BYTES","21474836479"),
                java.util.Map.entry("COLLECTION_STATE_DIRECTORY","/tmp/escape"),
                java.util.Map.entry("STORAGE_SAME_FILESYSTEM_VERIFIED","NO"),
                java.util.Map.entry("DATABASE_FILESYSTEM_DEVICE","999999999"));
        for(var entry:invalid.entrySet()) {
            Files.writeString(fixture.manifest(),valid); replaceManifestValue(fixture,entry.getKey(),entry.getValue());
            Result denied=run("asset-card-runtime-install.sh",fixture,"--apply","--confirm","INSTALL_ASSET_CARD_ARMED_WINDOW_ONLY");
            assertThat(denied.code()).as("Invalid %s must fail closed",entry.getKey()).isNotZero();
            assertThat(cardDropin(fixture)).doesNotExist();
        }
        Files.writeString(fixture.manifest(),valid); replaceManifestValue(fixture,"COLLECTION_SYMBOLS","UNREGISTEREDUSDT");
        assertThat(run("asset-card-runtime-install.sh",fixture).output()).contains("COLLECTION_SYMBOLS_INVALID");
        Files.writeString(fixture.manifest(),valid); replaceManifestValue(fixture,"SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE","0500");
        assertThat(run("asset-card-runtime-install.sh",fixture).output()).contains("COLLECTION_QUOTA_UNVERIFIED");
        Files.writeString(fixture.manifest(),valid);
        Files.setPosixFilePermissions(fixture.root().resolve("var/lib/rine-logic/asset-card/archive"),PosixFilePermissions.fromString("rwxrwxrwx"));
        assertThat(run("asset-card-runtime-install.sh",fixture).output()).contains("COLLECTION_DIRECTORY_UNSAFE");
    }

    @Test void repeatedInstallationPreservesAbsoluteClocksAndStoppedLedgerAndExpiredPlansCannotOverwrite() throws Exception {
        Fixture fixture=armedFixture();
        Path ledger=fixture.root().resolve("var/lib/rine-logic/asset-card/collection/LOCAL_FIXTURE_WINDOW.json");
        write(ledger,"{\"status\":\"STOPPED\",\"requests\":288,\"fixtureOnly\":true}\n","rw-------");
        String beforeLedger=Files.readString(ledger);
        for(int i=0;i<2;i++) {
            Result applied=run("asset-card-runtime-install.sh",fixture,"--apply","--confirm","INSTALL_ASSET_CARD_ARMED_WINDOW_ONLY");
            assertThat(applied.code()).withFailMessage(applied.output()).isZero();
            assertThat(Files.readString(ledger)).isEqualTo(beforeLedger);
            var plan=bindRenderedEnvironment(Files.readString(cardDropin(fixture))).getCollectionWindow();
            assertThat(plan.getStartsAt()).isEqualTo(java.time.Instant.parse(manifestValue(fixture,"COLLECTION_STARTS_AT")));
            assertThat(plan.getEndsAt()).isEqualTo(java.time.Instant.parse(manifestValue(fixture,"COLLECTION_ENDS_AT")));
        }
        String oldDropin=Files.readString(cardDropin(fixture));
        java.time.Instant ended=java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).minusSeconds(60);
        replaceManifestValue(fixture,"COLLECTION_ENDS_AT",ended.toString());
        replaceManifestValue(fixture,"COLLECTION_STARTS_AT",ended.minusSeconds(28800).toString());
        replaceManifestValue(fixture,"SHARED_IP_HEADROOM_CONFIRMED_AT",ended.minusSeconds(28860).toString());
        Result denied=run("asset-card-runtime-install.sh",fixture,"--apply","--confirm","INSTALL_ASSET_CARD_ARMED_WINDOW_ONLY");
        assertThat(denied.code()).isNotZero();
        assertThat(denied.output()).contains("COLLECTION_WINDOW_EXPIRED");
        assertThat(Files.readString(cardDropin(fixture))).isEqualTo(oldDropin);
        assertThat(Files.readString(ledger)).isEqualTo(beforeLedger);
    }

    @Test void bootstrapAndVerifyProveExactEffectivePrivilegesWithoutChangingOldAcls() throws Exception {
        boolean docker;
        try { docker= DockerClientFactory.instance().isDockerAvailable(); } catch (RuntimeException unavailable) { docker=false; }
        assumeTrue(docker,"Disposable PostgreSQL unavailable; no external database fallback");
        try (var database=new PostgreSQLContainer<>("postgres:16-alpine")) {
            database.start();
            for (String name : List.of("asset-card-role-bootstrap.sql","asset-card-role-verify.sql"))
                database.copyFileToContainer(MountableFile.forHostPath(SOURCE.resolve(name)),"/tmp/"+name);
            assertThat(sql(database,"CREATE TABLE public.tm_asset_card_snapshot(id int); CREATE TABLE public.tm_asset_card_spot_bar(id int);"
                    +"CREATE TABLE public.tm_asset_card_feature_history(id int); CREATE TABLE public.existing_private_business(id int);"
                    +"CREATE SEQUENCE public.existing_private_sequence; CREATE ROLE historical_reader NOLOGIN;").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-bootstrap.sql").getExitCode()).isNotZero(); // Default PUBLIC TEMP is forbidden.
            assertThat(sql(database,"SELECT count(*) FROM pg_roles WHERE rolname='rine_asset_card_writer'").getStdout()).contains("0");
            assertThat(sql(database,"REVOKE TEMP ON DATABASE test FROM PUBLIC;").getExitCode()).isZero(); // Local disposable fixture only.
            String before=sql(database,"SELECT oid,relacl::text FROM pg_class WHERE relname IN ('existing_private_business','existing_private_sequence') ORDER BY oid").getStdout();
            String createDefiner="CREATE FUNCTION public.fixture_private_business_count() RETURNS bigint LANGUAGE sql SECURITY DEFINER "
                    +"AS 'SELECT count(*) FROM public.existing_private_business'";
            assertThat(sql(database,createDefiner).getExitCode()).isZero();
            String functionAcl=sql(database,"SELECT proacl::text FROM pg_proc WHERE proname='fixture_private_business_count'").getStdout();
            var refusedBootstrap=file(database,"asset-card-role-bootstrap.sql");
            assertThat(refusedBootstrap.getExitCode()).isNotZero();
            assertThat(refusedBootstrap.getStderr()).contains("PREEXISTING_SECURITY_DEFINER_ACCESS");
            assertThat(sql(database,"SELECT count(*) FROM pg_roles WHERE rolname='rine_asset_card_writer'").getStdout()).isEqualTo("0\n");
            assertThat(sql(database,"SELECT proacl::text FROM pg_proc WHERE proname='fixture_private_business_count'").getStdout()).isEqualTo(functionAcl);
            assertThat(sql(database,"DROP FUNCTION public.fixture_private_business_count()").getExitCode()).isZero(); // Remove only this temporary attack fixture.
            assertThat(file(database,"asset-card-role-bootstrap.sql").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-bootstrap.sql").getExitCode()).isZero(); // Existing role: verify, not ALTER.
            assertThat(sql(database,"SELECT oid,relacl::text FROM pg_class WHERE relname IN ('existing_private_business','existing_private_sequence') ORDER BY oid").getStdout()).isEqualTo(before);
            assertThat(sql(database,createDefiner).getExitCode()).isZero();
            // The card role cannot read the table directly, but this PUBLIC routine delegates its owner's access.
            assertThat(sql(database,"SET ROLE rine_asset_card_writer; SELECT public.fixture_private_business_count()").getStdout()).endsWith("0\n");
            functionAcl=sql(database,"SELECT proacl::text FROM pg_proc WHERE proname='fixture_private_business_count'").getStdout();
            var refusedVerify=file(database,"asset-card-role-verify.sql");
            assertThat(refusedVerify.getExitCode()).isNotZero();
            assertThat(refusedVerify.getStderr()).contains("ASSET_CARD_SECURITY_DEFINER_ACCESS_FORBIDDEN");
            assertThat(sql(database,"SELECT proacl::text FROM pg_proc WHERE proname='fixture_private_business_count'").getStdout()).isEqualTo(functionAcl);
            assertThat(sql(database,"SELECT oid,relacl::text FROM pg_class WHERE relname IN ('existing_private_business','existing_private_sequence') ORDER BY oid").getStdout()).isEqualTo(before);
            assertThat(sql(database,"DROP FUNCTION public.fixture_private_business_count()").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isZero();
            for (String table:List.of("tm_asset_card_snapshot","tm_asset_card_spot_bar","tm_asset_card_feature_history")) {
                String denied=table.equals("tm_asset_card_snapshot")?"DELETE":"UPDATE";
                assertThat(sql(database,"GRANT "+denied+" ON public."+table+" TO rine_asset_card_writer").getExitCode()).isZero();
                assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isNotZero();
                assertThat(sql(database,"REVOKE "+denied+" ON public."+table+" FROM rine_asset_card_writer").getExitCode()).isZero();
            }
            assertThat(sql(database,"GRANT SELECT ON public.tm_asset_card_snapshot TO rine_asset_card_writer WITH GRANT OPTION").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isNotZero();
            assertThat(sql(database,"REVOKE GRANT OPTION FOR SELECT ON public.tm_asset_card_snapshot FROM rine_asset_card_writer").getExitCode()).isZero();
            for(String grant:List.of("SELECT ON public.existing_private_business", "SELECT(id) ON public.existing_private_business", "USAGE ON SEQUENCE public.existing_private_sequence")) {
                assertThat(sql(database,"GRANT "+grant+" TO PUBLIC").getExitCode()).isZero();
                assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isNotZero();
                assertThat(sql(database,"REVOKE "+grant+" FROM PUBLIC").getExitCode()).isZero();
            }
            assertThat(sql(database,"ALTER TABLE public.tm_asset_card_snapshot RENAME TO stored_card_snapshot;"
                    +"REVOKE ALL ON public.stored_card_snapshot FROM rine_asset_card_writer;"
                    +"CREATE VIEW public.tm_asset_card_snapshot AS SELECT id FROM public.existing_private_business;"
                    +"GRANT SELECT,INSERT,UPDATE ON public.tm_asset_card_snapshot TO rine_asset_card_writer").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isNotZero();
            assertThat(sql(database,"DROP VIEW public.tm_asset_card_snapshot; ALTER TABLE public.stored_card_snapshot RENAME TO tm_asset_card_snapshot;"
                    +"GRANT SELECT,INSERT,UPDATE ON public.tm_asset_card_snapshot TO rine_asset_card_writer").getExitCode()).isZero();
            assertThat(sql(database,"GRANT historical_reader TO rine_asset_card_writer").getExitCode()).isZero();
            assertThat(file(database,"asset-card-role-verify.sql").getExitCode()).isNotZero();
            String attributes=sql(database,"SELECT rolname,rolinherit FROM pg_roles WHERE rolname='rine_asset_card_writer'").getStdout();
            assertThat(file(database,"asset-card-role-bootstrap.sql").getExitCode()).isNotZero();
            assertThat(sql(database,"SELECT rolname,rolinherit FROM pg_roles WHERE rolname='rine_asset_card_writer'").getStdout()).isEqualTo(attributes);
        }
    }

    private static org.testcontainers.containers.Container.ExecResult file(PostgreSQLContainer<?> db,String file) throws Exception {
        return db.execInContainer("psql","-X","-v","ON_ERROR_STOP=1","-U",db.getUsername(),"-d",db.getDatabaseName(),"-f","/tmp/"+file);
    }
    private static org.testcontainers.containers.Container.ExecResult sql(PostgreSQLContainer<?> db,String sql) throws Exception {
        return db.execInContainer("psql","-X","-v","ON_ERROR_STOP=1","-U",db.getUsername(),"-d",db.getDatabaseName(),"-Atc",sql);
    }
    private static void setDisposableWriterPassword(PostgreSQLContainer<?> database,String password) throws Exception {
        // Generated local-only credential: never put it in a command argument, log or environment variable.
        try(var connection=java.sql.DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword());
            var statement=connection.createStatement()) {
            statement.execute("ALTER ROLE rine_asset_card_writer PASSWORD '"+password.replace("'","''")+"'");
        }
    }
    private Fixture realCredentialFixture(PostgreSQLContainer<?> database) throws Exception {
        Fixture fixture=fixture();
        String jdbcUrl="jdbc:postgresql://"+database.getHost()+":"+database.getMappedPort(5432)+"/"+database.getDatabaseName();
        Path jar=fixture.root().resolve("opt/rine-logic/current/app.jar");
        String suppliedJar=System.getProperty("assetCard.credential.integration-jar");
        Path actualJava=Path.of(System.getProperty("java.home"),"bin","java").toRealPath();
        String wrapper;
        if(suppliedJar!=null) {
            Path sourceJar=Path.of(suppliedJar).toAbsolutePath().normalize();
            assertThat(sourceJar).isRegularFile();
            assertThat(Files.isSymbolicLink(sourceJar)).isFalse();
            verifyCredentialJarIdentity(sourceJar);
            Files.copy(sourceJar,jar,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.setPosixFilePermissions(jar,PosixFilePermissions.fromString("rw-r--r--"));
            wrapper="#!/bin/sh\nexec "+shellQuote(actualJava.toString())+" \"$@\"\n";
            System.out.println("ASSET_CARD_CREDENTIAL_RUNTIME_MODE=STANDARD_JAR\nASSET_CARD_CREDENTIAL_JAR_SHA256="+sha(jar));
        } else {
            String classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
            assertThat(Thread.currentThread().getContextClassLoader().getResource(WRITER_MAIN.replace('.','/')+".class")).isNotNull();
            // The ordinary test lifecycle precedes repackage. This adapter changes only the launcher,
            // then executes the current production CLI in a real child JVM; no auth or output is stubbed.
            wrapper="#!/bin/sh\n[ \"$1\" = '-Dloader.main="+WRITER_MAIN+"' ] && [ \"$2\" = '-cp' ] && "
                    +"[ \"$4\" = 'org.springframework.boot.loader.launch.PropertiesLauncher' ] || exit 64\nshift 4\nexec "
                    +shellQuote(actualJava.toString())+" -cp "+shellQuote(classpath)+" "+WRITER_MAIN+" \"$@\"\n";
            System.out.println("ASSET_CARD_CREDENTIAL_RUNTIME_MODE=COMPILED_CLASSES_NOT_JAR_EVIDENCE");
        }
        write(fixture.root().resolve("usr/bin/java"),wrapper,"rwx------");
        String metadata=Files.readString(fixture.root().resolve("opt/rine-logic/current/deployment-metadata.txt"))
                .replaceAll("(?m)^ARTIFACT_SHA256=.*$","ARTIFACT_SHA256="+sha(jar));
        replaceDeploymentMetadata(fixture,metadata);
        String manifest=Files.readString(fixture.manifest()).replace("WRITER_JDBC_URL=jdbc:postgresql://127.0.0.1:1/test","WRITER_JDBC_URL="+jdbcUrl)
                .replaceAll("(?m)^APP_JAR_SHA256=.*$","APP_JAR_SHA256="+sha(jar));
        Files.writeString(fixture.manifest(),manifest);
        return fixture;
    }
    private static void verifyCredentialJarIdentity(Path jar) throws Exception {
        try(var archive=new java.util.jar.JarFile(jar.toFile())) {
            assertThat(archive.getJarEntry("org/springframework/boot/loader/launch/PropertiesLauncher.class")).isNotNull();
            String classResource=WRITER_MAIN.replace('.','/')+".class";
            var entry=archive.getJarEntry("BOOT-INF/classes/"+classResource);
            assertThat(entry).isNotNull();
            try(var fromJar=archive.getInputStream(entry);var compiled=Thread.currentThread().getContextClassLoader().getResourceAsStream(classResource)) {
                assertThat(compiled).isNotNull();
                assertThat(fromJar.readAllBytes()).as("Standard JAR must contain the current compiled writer CLI, not a stale artifact").isEqualTo(compiled.readAllBytes());
            }
            for(var entries=archive.entries();entries.hasMoreElements();) {
                var nested=entries.nextElement();
                if(!nested.getName().startsWith("BOOT-INF/classes/"+classResource.replace(".class","$")) || !nested.getName().endsWith(".class")) continue;
                String resource=nested.getName().substring("BOOT-INF/classes/".length());
                try(var fromJar=archive.getInputStream(nested);var compiled=Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                    assertThat(compiled).isNotNull();
                    assertThat(fromJar.readAllBytes()).as("Nested writer implementation must match the current compiled candidate").isEqualTo(compiled.readAllBytes());
                }
            }
            var metadata=archive.getJarEntry("BOOT-INF/classes/git.properties");
            assertThat(metadata).isNotNull();
            var identity=new java.util.Properties();
            try(var input=archive.getInputStream(metadata)) { identity.load(input); }
            var git=new ProcessBuilder("git","rev-parse","HEAD").directory(Path.of(".").toRealPath().toFile());
            git.environment().remove("GIT_DIR"); git.environment().remove("GIT_WORK_TREE");
            var process=git.start();
            assertThat(process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).isZero();
            String head=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();
            assertThat(identity.getProperty("git.commit.id.full")).isEqualTo(head);
            assertThat(identity.getProperty("git.dirty")).isEqualTo("false");
        }
    }
    private Result realCredentialCli(Fixture fixture,Path credential,String expectedDatabase,String expectedOwner) throws Exception {
        String jdbcUrl=Files.readAllLines(fixture.manifest()).stream().filter(line->line.startsWith("WRITER_JDBC_URL=")).findFirst().orElseThrow().substring("WRITER_JDBC_URL=".length());
        var process=new ProcessBuilder(fixture.root().resolve("usr/bin/java").toString(),"-Dloader.main="+WRITER_MAIN,"-cp",
                fixture.root().resolve("opt/rine-logic/current/app.jar").toString(),"org.springframework.boot.loader.launch.PropertiesLauncher",
                "verify","--jdbc-url",jdbcUrl,"--expected-database",expectedDatabase,"--credential-file",credential.toString(),"--credential-owner",expectedOwner)
                .redirectErrorStream(true).start();
        return awaitLocalProcess(process);
    }
    private static void assertSafeCredentialOutput(Result result,String... passwords) {
        assertThat(java.util.Arrays.stream(passwords).noneMatch(result.output()::contains)).as("Credential output must not expose local-only password values").isTrue();
        assertThat(result.output()).doesNotContain("Exception", "org.postgresql", "jdbc:postgresql:", "Started TradeModel");
    }
    private static String shellQuote(String value) { return "'"+value.replace("'","'\\''")+"'"; }
    private Fixture fixture() throws Exception {
        Path root=Files.createTempDirectory(temporary,"native-fake-").toRealPath();
        Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));
        write(root.resolve(".asset-card-test-root"),"TEST_FIXTURE_ONLY\n","rw-------");
        String[] paths={"etc/systemd/system/rine-logic.service","etc/systemd/system/rine-logic.service.d/20-core-loop-schedulers.conf",
                "usr/local/sbin/rine-logic-wait-ready","opt/rine-logic/current/app.jar","opt/rine-logic/current/deployment-metadata.txt"};
        for(String path:paths) write(root.resolve(path),"SYNTHETIC_NON_SECRET_BASE\n","rw-r--r--");
        write(root.resolve(paths[4]),"MERGED_MAIN_SHA="+"a".repeat(40)+"\nARTIFACT_SHA256="+sha(root.resolve(paths[3]))
                +"\nDEPLOYED_AT=2026-09-10T09:59:10Z\n","rw-r--r--");
        write(root.resolve("etc/rine-logic/credentials/asset-card-db-password"),"SYNTHETIC_PASSWORD_SENTINEL","rw-------");
        write(root.resolve("usr/bin/java"),"#!/bin/sh\nprintf '%s\\n' ASSET_CARD_WRITER_VERIFY=FAIL VERIFIED_BUNDLE_STATUS=FAIL\nexit 2\n","rwx------");
        Files.createDirectories(root.resolve("opt/rine-logic/models/asset-card"));
        String template=Files.readString(SOURCE.resolve("asset-card-runtime-manifest.template"));
        String uid=Files.getAttribute(root,"unix:uid").toString();
        template=template.replace("NATIVE_SYSTEMD_JAR_RUNTIME","TEST_FIXTURE_ONLY")
                .replace("REPLACE_ROOT_UID",uid).replace("REPLACE_SERVICE_UID",String.valueOf(Integer.parseInt(uid)+1))
                .replace("REPLACE_JAR_SHA256",sha(root.resolve(paths[3])))
                .replace("REPLACE_MAIN_UNIT_SHA256",sha(root.resolve(paths[0])))
                .replace("REPLACE_SCHEDULER_SHA256",sha(root.resolve(paths[1])))
                .replace("REPLACE_READY_SHA256",sha(root.resolve(paths[2])))
                .replace("REPLACE_RELEASE_METADATA_SHA256",sha(root.resolve(paths[4])))
                .replace("REPLACE_RELEASE_METADATA_FORMAT","KEY_VALUE_V1")
                .replace("REPLACE_DATABASE_NAME","test").replace("REPLACE_NON_SECRET_JDBC_URL","jdbc:postgresql://127.0.0.1:1/test");
        Path manifest=root.resolve("runtime.manifest"); write(manifest,template,"rw-------");
        return new Fixture(root,manifest);
    }
    private Fixture armedFixture() throws Exception {
        Fixture fixture=fixture();
        java.time.Instant confirmed=java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        replaceManifestValue(fixture,"CARD_STARTUP_MODE","ARMED");
        replaceManifestValue(fixture,"COLLECTION_WINDOW_ID","LOCAL_FIXTURE_WINDOW");
        replaceManifestValue(fixture,"COLLECTION_STARTS_AT",confirmed.plusSeconds(60).toString());
        replaceManifestValue(fixture,"COLLECTION_ENDS_AT",confirmed.plusSeconds(60+28800).toString());
        replaceManifestValue(fixture,"COLLECTION_SYMBOLS","BTCUSDT,ETHUSDT,XRPUSDT");
        replaceManifestValue(fixture,"SHARED_IP_WEIGHT_ALLOWANCE_PER_MINUTE","500");
        replaceManifestValue(fixture,"SHARED_IP_WEIGHT_LIMIT_PER_MINUTE","6000");
        replaceManifestValue(fixture,"SHARED_IP_HEADROOM_CONFIRMED_AT",confirmed.toString());
        for(String leaf:List.of("collection","archive")) {
            Path path=fixture.root().resolve("var/lib/rine-logic/asset-card/"+leaf);
            Files.createDirectories(path); Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rwx------"));
        }
        replaceManifestValue(fixture,"STORAGE_SAME_FILESYSTEM_VERIFIED","YES");
        replaceManifestValue(fixture,"DATABASE_FILESYSTEM_DEVICE",Files.getAttribute(fixture.root(),"unix:dev").toString());
        return fixture;
    }
    private static Path cardDropin(Fixture fixture) { return fixture.root().resolve("etc/systemd/system/rine-logic.service.d/40-asset-card.conf"); }
    private static String manifestValue(Fixture fixture,String key) throws Exception {
        return Files.readAllLines(fixture.manifest()).stream().filter(line->line.startsWith(key+"=")).findFirst().orElseThrow().substring(key.length()+1);
    }
    private static void replaceManifestValue(Fixture fixture,String key,String value) throws Exception {
        String contents=Files.readString(fixture.manifest());
        assertThat(contents.lines().filter(line->line.startsWith(key+"=")).count()).isEqualTo(1);
        Files.writeString(fixture.manifest(),contents.replaceAll("(?m)^"+java.util.regex.Pattern.quote(key)+"=.*$",
                java.util.regex.Matcher.quoteReplacement(key+"="+value)));
    }
    private static org.example.trademodel.assetcard.AssetCardProperties bindRenderedEnvironment(String dropin) {
        java.util.Map<String,Object> variables=new java.util.LinkedHashMap<>();
        dropin.lines().filter(line->line.startsWith("Environment=\"")).forEach(line->{
            String pair=line.substring("Environment=\"".length(),line.length()-1); int separator=pair.indexOf('=');
            assertThat(variables.put(pair.substring(0,separator),pair.substring(separator+1))).isNull();
        });
        var environment=new org.springframework.core.env.StandardEnvironment();
        environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new org.springframework.core.env.SystemEnvironmentPropertySource("systemEnvironment",variables));
        var properties=new org.example.trademodel.assetcard.AssetCardProperties();
        org.springframework.boot.context.properties.bind.Binder.get(environment)
                .bind("trade-model.asset-card",org.springframework.boot.context.properties.bind.Bindable.ofInstance(properties));
        return properties;
    }
    private void replaceDeploymentMetadata(Fixture fixture,String content) throws Exception {
        Path metadata=fixture.root().resolve("opt/rine-logic/current/deployment-metadata.txt");
        Files.writeString(metadata,content);
        Files.writeString(fixture.manifest(),Files.readString(fixture.manifest())
                .replaceAll("(?m)^RELEASE_METADATA_SHA256=.*$","RELEASE_METADATA_SHA256="+sha(metadata)));
    }
    private Path modelFixture(Fixture fixture,String identity) throws Exception {
        Path directory=fixture.root().resolve("fixture-model-"+identity);
        for(String file:List.of("manifest.json","long.ubj","short.ubj","calibration.json","thresholds.json","risk-distributions.json","validation.json"))
            write(directory.resolve(file),"SYNTHETIC_MODEL_FIXTURE_"+identity,"r--------");
        return directory;
    }
    private Result run(String script,Fixture fixture,String... extra) throws Exception {
        var command=new java.util.ArrayList<>(List.of("bash",SOURCE.resolve(script).toString(),"--manifest",fixture.manifest().toString(),"--test-root",fixture.root().toString()));
        command.addAll(List.of(extra)); var process=new ProcessBuilder(command).redirectErrorStream(true).start();
        return awaitLocalProcess(process);
    }
    private static Result awaitLocalProcess(Process process) throws Exception {
        try {
            assertThat(process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            return new Result(process.exitValue(),new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            if(process.isAlive()) {
                // The rotation shell may still be waiting for its fresh-connection JVM.
                // Terminate that child too; a timeout remains a failed test, never a skipped check.
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS);
            }
            process.getOutputStream().close();
            process.getInputStream().close();
            process.getErrorStream().close();
        }
    }
    private static void write(Path path,String value,String mode) throws Exception {
        Files.createDirectories(path.getParent()); Files.writeString(path,value); Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(mode));
    }
    private static String sha(Path path) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
}
