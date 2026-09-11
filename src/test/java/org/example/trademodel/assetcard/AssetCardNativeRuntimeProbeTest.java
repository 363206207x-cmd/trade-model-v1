package org.example.trademodel.assetcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;

/** Pure contract fixtures are not Linux/native interoperability evidence. */
@Tag("core-regression")
class AssetCardNativeRuntimeProbeTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    @TempDir Path root;

    @Test void threeModesAreExplicitAndNoPasswordOrUnknownArgumentIsAccepted() {
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.Options.parse(new String[]{"--password","SENSITIVE_TEST_SENTINEL"}))
                .hasMessage("INVALID_ARGUMENTS");
        var output=new ByteArrayOutputStream();
        assertThat(AssetCardNativeRuntimeProbe.run(new String[]{"--password","SENSITIVE_TEST_SENTINEL"},new PrintStream(output))).isEqualTo(2);
        assertThat(output.toString()).contains("STATUS=FAIL","CODE=INVALID_ARGUMENTS","PRODUCTION_MODEL_READY=NO")
                .doesNotContain("SENSITIVE_TEST_SENTINEL","Exception");
    }

    @Test void checksumOnlyNeverMeansValidatedBundleAndDetectsChangedBytes() throws Exception {
        ObjectNode manifest=JSON.createObjectNode(); var files=manifest.putObject("files");
        for(String name:AssetCardNativeRuntimeProbe.BUNDLE_FILES) {
            byte[] data=("TEST_FIXTURE_ONLY_"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(root.resolve(name),data); files.put(name,sha(data));
        }
        byte[] bytes=JSON.writeValueAsBytes(manifest); Files.write(root.resolve("manifest.json"),bytes);
        assertThat(AssetCardNativeRuntimeProbe.checksums(root,sha(bytes),false).files()).hasSize(6);
        Files.writeString(root.resolve("long.ubj"),"CHANGED");
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.checksums(root,sha(bytes),false)).hasMessage("ARTIFACT_CHECKSUM_MISMATCH");
    }

    @Test void checksumsRejectSymlinksExtraFilesAndDuplicateJsonKeys() throws Exception {
        Files.writeString(root.resolve("native-fixture.json"),"{\"models\":{},\"models\":{}}");
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.checksums(root,sha(Files.readAllBytes(root.resolve("native-fixture.json"))),true))
                .hasMessage("INVALID_JSON");
        var link=root.resolve("link"); Files.createSymbolicLink(link,root);
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.checksums(link,"0".repeat(64),false)).hasMessage("INVALID_LOCAL_PATH");
    }

    @Test void fixedFloat32RowsAndIndependentBetaAreComparedWithoutChangingFormula() {
        ObjectNode side=JSON.createObjectNode();
        side.putObject("calibration").put("a",.8).put("b",1.3).put("c",-.2).put("epsilon",1e-7);
        float raw=.25f;
        side.putArray("expectedRaw").add((double)raw);
        double expected=AssetCardBetaCalibration.calibrate(raw,new AssetCardBetaCalibration.Parameters(.8,1.3,-.2,1e-7));
        side.putArray("expectedCalibrated").add(expected);
        assertThat(AssetCardNativeRuntimeProbe.compare(new float[][]{{raw}},side).raw()).isZero();
        assertThat(AssetCardNativeRuntimeProbe.compare(new float[][]{{raw}},side).calibrated()).isZero();
        side.withArray("expectedCalibrated").set(0,JSON.getNodeFactory().numberNode(expected+.01));
        assertThat(AssetCardNativeRuntimeProbe.compare(new float[][]{{raw}},side).calibrated()).isGreaterThan(.009);
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.compare(new float[][]{{Float.NaN}},side)).hasMessage("INVALID_PREDICTION");
    }

    @Test void fixtureIdentityCannotClaimRealHistoricalOrProductionReadiness() {
        ObjectNode manifest=JSON.createObjectNode().put("schemaVersion",1).put("kind","REAL_HISTORICAL").put("productionModelReady",true);
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.fixture(manifest,"a".repeat(40),"b".repeat(64))).hasMessage("INVALID_FIXTURE_IDENTITY");
    }

    @Test void floatEncodingRejectsInfinityAndNoncanonicalMissingPayloads() {
        assertThat(AssetCardNativeRuntimeProbe.float32("3e800000")).isEqualTo(.25f);
        assertThat(Float.isNaN(AssetCardNativeRuntimeProbe.float32("7fc00000"))).isTrue();
        for(String value:new String[]{"7f800000","ff800000","7fc00001","1.25","SENSITIVE"})
            assertThatThrownBy(()->AssetCardNativeRuntimeProbe.float32(value)).hasMessage("INVALID_FLOAT32_INPUT");
    }

    @Test void architectureAndJavaVersionCannotBeTreatedAsNativeSuccess() {
        assertThat(AssetCardNativeRuntimeProbe.supportedPlatform("Mac OS X","aarch64",17,false)).isFalse();
        assertThat(AssetCardNativeRuntimeProbe.supportedPlatform("Linux","aarch64",17,true)).isFalse();
        assertThat(AssetCardNativeRuntimeProbe.supportedPlatform("Linux","amd64",21,true)).isFalse();
        assertThat(AssetCardNativeRuntimeProbe.supportedPlatform("Linux","amd64",17,true)).isTrue();
    }

    @Test void fixtureChecksExactFeaturesCandidateVersionsPairAndFiniteExpectations() {
        ObjectNode valid=fixture();
        assertThat(AssetCardNativeRuntimeProbe.fixture(valid,"a".repeat(40),"b".repeat(64)).rows()).isEqualTo(1);
        for(String key:new String[]{"candidateSha","jarSha256","featureVersion","xgboostVersion","targetArch"}) {
            ObjectNode bad=valid.deepCopy(); bad.put(key,"MISMATCH");
            assertThatThrownBy(()->AssetCardNativeRuntimeProbe.fixture(bad,"a".repeat(40),"b".repeat(64))).hasMessage("FIXTURE_VERSION_MISMATCH");
        }
        ObjectNode bad=valid.deepCopy(); bad.put("maxAbsoluteError",.01);
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.fixture(bad,"a".repeat(40),"b".repeat(64))).hasMessage("INVALID_PARITY_TOLERANCE");
        ObjectNode extra=valid.deepCopy(); extra.put("validated",true);
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.fixture(extra,"a".repeat(40),"b".repeat(64))).hasMessage("INVALID_FIXTURE_FIELDS");
        ObjectNode same=valid.deepCopy(); ((ObjectNode)same.path("models").path("SHORT")).put("sha256","c".repeat(64));
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.fixture(same,"a".repeat(40),"b".repeat(64))).hasMessage("INDEPENDENT_FIXTURES_REQUIRED");
    }

    @Test void exactSameMetadataGateRejectsSwappedSidesAndProductionModelsInFixtureMode() {
        var spec=fixture().path("models").path("LONG");
        Map<String,String> valid=Map.of("asset_card_side","LONG","asset_card_data_kind","TEST_FIXTURE_ONLY",
                "asset_card_model_version","TEST_FIXTURE_LONG_MODEL_V1","asset_card_calibration_version","TEST_FIXTURE_LONG_BETA_V1",
                "asset_card_feature_version",AssetCardFeatureService.FEATURE_VERSION);
        var names=AssetCardFeatureService.FEATURE_NAMES.toArray(String[]::new);
        AssetCardNativeRuntimeProbe.verifyFixtureModel(valid,names,names.length,spec,"LONG");
        for(var mutation:Map.of("asset_card_side","SHORT","asset_card_data_kind","REAL_HISTORICAL","asset_card_model_version","WRONG",
                "asset_card_calibration_version","WRONG","asset_card_feature_version","WRONG").entrySet()) {
            var invalid=new HashMap<>(valid); invalid.put(mutation.getKey(),mutation.getValue());
            assertThatThrownBy(()->AssetCardNativeRuntimeProbe.verifyFixtureModel(invalid,names,names.length,spec,"LONG")).hasMessage("MODEL_IDENTITY_MISMATCH");
        }
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.verifyFixtureModel(valid,new String[]{"wrong"},names.length,spec,"LONG")).hasMessage("MODEL_IDENTITY_MISMATCH");
    }

    @Test void checksumManifestCannotSmuggleAdditionalPathsOrSymlinkArtifacts() throws Exception {
        ObjectNode manifest=JSON.createObjectNode(); var files=manifest.putObject("files");
        for(String name:AssetCardNativeRuntimeProbe.BUNDLE_FILES) { byte[] bytes=name.getBytes(); Files.write(root.resolve(name),bytes); files.put(name,sha(bytes)); }
        byte[] bytes=JSON.writeValueAsBytes(manifest); Files.write(root.resolve("manifest.json"),bytes);
        Files.writeString(root.resolve("extra"),"TEST_FIXTURE_ONLY");
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.checksums(root,sha(bytes),false)).hasMessage("UNEXPECTED_BUNDLE_FILES");
        Files.delete(root.resolve("extra")); Files.delete(root.resolve("long.ubj"));
        Files.createSymbolicLink(root.resolve("long.ubj"),root.resolve("short.ubj"));
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.checksums(root,sha(bytes),false)).hasMessage("INVALID_LOCAL_PATH");
    }

    @Test void shellMatrixRejectsMissingIdentityAndUnknownArgumentsWithoutCreatingModels() throws Exception {
        var process=new ProcessBuilder("bash","scripts/asset-card-native-staging-matrix.sh","--password","SENSITIVE_TEST_SENTINEL")
                .redirectErrorStream(true).start();
        assertThat(process.waitFor()).isEqualTo(2);
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(output).contains("STATUS=FAIL","CODE=INVALID_ARGUMENTS","PRODUCTION_MODEL_READY=NO")
                .doesNotContain("SENSITIVE_TEST_SENTINEL","NATIVE_STATUS=PASS");
    }

    @Test void oldDirtyOrMissingJarProvenanceCannotBorrowCurrentCandidateIdentity() {
        Properties receipt=new Properties();
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.verifyBuildIdentity(receipt,"a".repeat(40))).hasMessage("MISSING_BUILD_PROVENANCE");
        receipt.setProperty("git.commit.id.full","a".repeat(40)); receipt.setProperty("git.dirty","false");
        assertThat(AssetCardNativeRuntimeProbe.verifyBuildIdentity(receipt,"a".repeat(40))).isEqualTo("a".repeat(40));
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.verifyBuildIdentity(receipt,"b".repeat(40))).hasMessage("CANDIDATE_BUILD_MISMATCH");
        receipt.setProperty("git.dirty","true");
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.verifyBuildIdentity(receipt,"a".repeat(40))).hasMessage("UNVERIFIED_BUILD_PROVENANCE");
        receipt.setProperty("git.dirty","false"); receipt.setProperty("git.remote.origin.url","TEST_ONLY_FORBIDDEN_METADATA");
        assertThatThrownBy(()->AssetCardNativeRuntimeProbe.verifyBuildIdentity(receipt,"a".repeat(40))).hasMessage("MISSING_BUILD_PROVENANCE");
    }

    @Test void generatedMetadataMustMatchThisRealWorktreeNotTheMainCheckout() throws Exception {
        Path generated=Path.of("target/classes/git.properties");
        if(!Files.isRegularFile(generated)) {
            // Default (non-evidence) builds are allowed, but cannot pass the native candidate gate.
            assertThatThrownBy(()->AssetCardNativeRuntimeProbe.verifyBuildIdentity(new Properties(),"a".repeat(40)))
                    .hasMessage("MISSING_BUILD_PROVENANCE");
            return;
        }
        Properties identity=new Properties(); try(var in=Files.newInputStream(generated)) { identity.load(in); }
        assertThat(identity.stringPropertyNames()).containsExactlyInAnyOrder("git.commit.id.full","git.dirty");
        assertThat(identity.getProperty("git.commit.id.full")).isEqualTo(currentGit("rev-parse","HEAD").trim());
        assertThat(identity.getProperty("git.dirty")).isEqualTo(Boolean.toString(!currentGit("status","--porcelain").isBlank()));
    }

    private static String currentGit(String operation,String flag) throws Exception {
        var builder=new ProcessBuilder("git","-C",Path.of("").toRealPath().toString(),operation,flag).redirectErrorStream(true);
        // Independently resolve the directory under test instead of inheriting a build process's Git location.
        for(String key:new String[]{"GIT_DIR","GIT_WORK_TREE","GIT_COMMON_DIR","GIT_INDEX_FILE","GIT_OBJECT_DIRECTORY","GIT_ALTERNATE_OBJECT_DIRECTORIES","GIT_NAMESPACE"})
            builder.environment().remove(key);
        builder.environment().put("GIT_OPTIONAL_LOCKS","0");
        var process=builder.start(); assertThat(process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).isZero();
        return new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
    }

    private static ObjectNode fixture() {
        ObjectNode value=JSON.createObjectNode().put("schemaVersion",1).put("kind","TEST_FIXTURE_ONLY").put("productionModelReady",false)
                .put("candidateSha","a".repeat(40)).put("jarSha256","b".repeat(64)).put("targetOs","Linux").put("targetArch","x86_64")
                .put("javaMajor",17).put("xgboostVersion","2.1.4").put("featureVersion",AssetCardFeatureService.FEATURE_VERSION).put("maxAbsoluteError",1e-7);
        value.set("featureNames",JSON.valueToTree(AssetCardFeatureService.FEATURE_NAMES));
        var row=value.putArray("float32Rows").addArray(); AssetCardFeatureService.FEATURE_NAMES.forEach(n->row.add("3e800000"));
        var models=value.putObject("models");
        for(String side:new String[]{"LONG","SHORT"}) {
            var spec=models.putObject(side).put("file",side.toLowerCase()+".ubj").put("sha256",(side.equals("LONG")?"c":"d").repeat(64))
                    .put("modelVersion","TEST_FIXTURE_"+side+"_MODEL_V1").put("calibrationVersion","TEST_FIXTURE_"+side+"_BETA_V1");
            spec.putObject("calibration").put("a",side.equals("LONG")?1:2).put("b",1).put("c",0).put("epsilon",1e-7);
            spec.putArray("expectedRaw").add(.25); spec.putArray("expectedCalibrated").add(.25);
        }
        return value;
    }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
}
