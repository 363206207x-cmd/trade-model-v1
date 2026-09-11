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
    @TempDir Path temporary;
    record Result(int code, String output) {}
    record Fixture(Path root, Path manifest) {}

    @Test void allNativeAttachmentsAreDefinedWithoutReplacingTheExternalBaseChain() throws Exception {
        for (String name : List.of("README.md", "rine-logic-asset-card.conf.template", "asset-card-role-bootstrap.sql",
                "asset-card-role-verify.sql", "asset-card-runtime-credentials.sh", "asset-card-model-install.sh",
                "asset-card-runtime-preflight.sh", "asset-card-runtime-manifest.template", "asset-card-runtime-install.sh"))
            assertThat(SOURCE.resolve(name)).isRegularFile();
        String dropin=Files.readString(SOURCE.resolve("rine-logic-asset-card.conf.template"));
        assertThat(dropin).contains("LoadCredential=", "ReadOnlyPaths=", "MODEL_MODE=SHADOW", "WRITER_ENABLED=false");
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
                .replace("RELEASE_METADATA=/opt/rine-logic/release-metadata.json", "RELEASE_METADATA=/opt/rine-logic/password-backup"));
        write(fixture.root().resolve("opt/rine-logic/password-backup"),"SYNTHETIC_SECRET_MUST_NOT_BE_HASHED","rw-------");
        Result denied=run("asset-card-runtime-preflight.sh",fixture);
        assertThat(denied.code()).isNotZero();
        assertThat(denied.output()).contains("RELEASE_METADATA_INVALID").doesNotContain("SYNTHETIC_SECRET_MUST_NOT_BE_HASHED");
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
    private Fixture fixture() throws Exception {
        Path root=Files.createTempDirectory(temporary,"native-fake-").toRealPath();
        Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));
        write(root.resolve(".asset-card-test-root"),"TEST_FIXTURE_ONLY\n","rw-------");
        String[] paths={"etc/systemd/system/rine-logic.service","etc/systemd/system/rine-logic.service.d/20-core-loop-schedulers.conf",
                "usr/local/sbin/rine-logic-wait-ready","opt/rine-logic/current/app.jar","opt/rine-logic/release-metadata.json"};
        for(String path:paths) write(root.resolve(path),"SYNTHETIC_NON_SECRET_BASE\n","rw-r--r--");
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
                .replace("REPLACE_RELEASE_METADATA_FORMAT","JSON_V1")
                .replace("REPLACE_DATABASE_NAME","test").replace("REPLACE_NON_SECRET_JDBC_URL","jdbc:postgresql://127.0.0.1:1/test");
        Path manifest=root.resolve("runtime.manifest"); write(manifest,template,"rw-------");
        return new Fixture(root,manifest);
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
        assertThat(process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        return new Result(process.exitValue(),new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
    }
    private static void write(Path path,String value,String mode) throws Exception {
        Files.createDirectories(path.getParent()); Files.writeString(path,value); Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(mode));
    }
    private static String sha(Path path) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
}
