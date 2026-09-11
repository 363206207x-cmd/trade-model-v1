package org.example.trademodel.assetcard;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;

/** Non-Web, local-only standard-JAR verification. Never starts Spring or authorizes a model release. */
public final class AssetCardNativeRuntimeProbe {
    private AssetCardNativeRuntimeProbe() {}
    static final Set<String> BUNDLE_FILES=Set.of("long.ubj","short.ubj","calibration.json","thresholds.json","risk-distributions.json","validation.json");
    private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final String VERSION="2.1.4",NATIVE="lib/linux/x86_64/libxgboost4j.so";
    private static final Set<String> MODES=Set.of("--verify-manifest-checksums","--verify-bundle-only","--predict-fixture");
    record Options(String mode,Path appJar,String appJarSha,Path bundleDir,String manifestSha,String candidateSha) {
        static Options parse(String[] args) {
            require(args!=null && args.length>=9 && MODES.contains(args[0]),"INVALID_ARGUMENTS");
            Map<String,String> values=new HashMap<>();
            Set<String> names=Set.of("--app-jar","--app-jar-sha256","--bundle-dir","--manifest-sha256","--candidate-sha");
            for(int i=1;i<args.length;i+=2) require(i+1<args.length && names.contains(args[i])
                    && values.putIfAbsent(args[i],args[i+1])==null && !args[i+1].isBlank(),"INVALID_ARGUMENTS");
            require(values.keySet().containsAll(Set.of("--app-jar","--app-jar-sha256","--bundle-dir","--manifest-sha256")),"INVALID_ARGUMENTS");
            require(sha(values.get("--app-jar-sha256")) && sha(values.get("--manifest-sha256")),"INVALID_ARGUMENTS");
            String candidate=values.get("--candidate-sha");
            require(candidate==null || candidate.matches("[0-9a-f]{40}"),"INVALID_ARGUMENTS");
            require(!args[0].equals("--predict-fixture") || candidate!=null,"INVALID_ARGUMENTS");
            try { return new Options(args[0],Path.of(values.get("--app-jar")),values.get("--app-jar-sha256"),
                    Path.of(values.get("--bundle-dir")),values.get("--manifest-sha256"),candidate); }
            catch(InvalidPathException failure) { throw invalid("INVALID_ARGUMENTS"); }
        }
    }
    record Checked(JsonNode manifest,Map<String,byte[]> files) {}
    record Fixture(float[] data,int rows,int columns,JsonNode models,double tolerance) {}
    record Deltas(double raw,double calibrated) {}
    private record JarEvidence(String dependencySha,String nativeSha,String candidateSha) {}

    public static void main(String[] args) {
        // This isolated CLI reports fixed status codes, never native exception bodies or input paths.
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        System.exit(run(args,System.out));
    }

    static int run(String[] args,PrintStream output) {
        Map<String,String> result=new LinkedHashMap<>();
        result.put("STATUS","FAIL"); result.put("CODE","NOT_EXECUTED");
        for(String key:List.of("CHECKSUM_STATUS","VERIFIED_BUNDLE_STATUS","NATIVE_STATUS","FIXTURE_PARITY_STATUS")) result.put(key,"NOT_EXECUTED");
        result.put("MODEL_MODE","SHADOW"); result.put("PRODUCTION_MODEL_READY","NO");
        result.put("STAGING_ACCEPTANCE","NOT_EXECUTED");
        int exit=2;
        try {
            Options options=Options.parse(args);
            JarEvidence jar=verifyJar(options.appJar(),options.appJarSha(),options.candidateSha());
            result.put("JAR_SHA256",options.appJarSha()); result.put("MANIFEST_SHA256",options.manifestSha());
            result.put("XGBOOST_JAVA_VERSION",VERSION); result.put("XGBOOST_JAR_SHA256",jar.dependencySha());
            result.put("CANDIDATE_SHA",jar.candidateSha()); result.put("CANDIDATE_PROVENANCE_STATUS","PASS");
            boolean test=options.mode().equals("--predict-fixture");
            Checked checked=checksums(options.bundleDir(),options.manifestSha(),test);
            result.put("CHECKSUM_STATUS","PASS");
            if(options.mode().equals("--verify-manifest-checksums")) {
                result.put("STATUS","PASS"); result.put("CODE","CHECKSUMS_VERIFIED"); exit=0;
            } else if(!supportedPlatform(System.getProperty("os.name"),System.getProperty("os.arch"),Runtime.version().feature(),Files.isRegularFile(Path.of("/proc/self/maps")))) {
                result.put("STATUS","NOT_EXECUTED"); result.put("CODE","LINUX_X86_64_JAVA17_REQUIRED"); exit=78;
            } else {
                result.put("OS","Linux"); result.put("ARCH","x86_64"); result.put("JAVA_MAJOR","17");
                result.put("JAVA_VERSION",Runtime.version().toString());
                result.put("RUNTIME_MODE","LOCAL_STANDARD_JAR");
                if(test) {
                    Fixture fixture=fixture(checked.manifest(),options.candidateSha(),options.appJarSha());
                    predict(fixture,checked.files(),result);
                    nativeEvidence(jar,result);
                    result.put("DATA_KIND","TEST_FIXTURE_ONLY"); result.put("FIXTURE_PARITY_STATUS","PASS");
                    result.put("CODE","TEST_FIXTURE_PARITY_VERIFIED");
                } else {
                    // The existing production gate verifies all real provenance, validation and versions.
                    try(AssetCardModelBundle bundle=AssetCardModelBundle.load(options.bundleDir(),options.manifestSha())) {
                        if(!bundle.validated()) throw new ProbeFailure(6,"BUNDLE_NOT_VALIDATED");
                        nativeEvidence(jar,result);
                        result.put("DATA_KIND","REAL_HISTORICAL"); result.put("VERIFIED_BUNDLE_STATUS","PASS");
                        result.put("CODE","REAL_BUNDLE_VERIFIED");
                    }
                }
                result.put("NATIVE_STATUS","PASS"); result.put("STATUS","PASS"); exit=0;
            }
        } catch(ProbeFailure failure) { exit=failure.exit; result.put("CODE",failure.getMessage());
            if(exit==4) result.put("NATIVE_STATUS","FAIL"); if(exit==5) result.put("FIXTURE_PARITY_STATUS","FAIL"); }
        catch(IllegalArgumentException failure) { result.put("CODE","INVALID_INPUT"); }
        catch(IOException failure) { exit=3; result.put("CODE","LOCAL_IO_FAILED"); }
        catch(LinkageError failure) { exit=4; result.put("CODE","NATIVE_LOAD_FAILED"); result.put("NATIVE_STATUS","FAIL"); }
        catch(Exception failure) { exit=4; result.put("CODE","NATIVE_OPERATION_FAILED"); result.put("NATIVE_STATUS","FAIL"); }
        result.put("EXIT_CODE",Integer.toString(exit));
        result.forEach((key,value)->output.println(key+"="+value)); return exit;
    }

    static boolean supportedPlatform(String os,String arch,int javaMajor,boolean procMaps) {
        return "Linux".equals(os) && Set.of("amd64","x86_64").contains(arch) && javaMajor==17 && procMaps;
    }

    static Checked checksums(Path directory,String expected,boolean fixture) throws IOException {
        require(sha(expected) && Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(directory),"INVALID_LOCAL_PATH");
        Path root=directory.toRealPath();
        String manifestName=fixture?"native-fixture.json":"manifest.json";
        byte[] bytes=read(root.resolve(manifestName),4*1024*1024);
        require(hash(bytes).equals(expected),"MANIFEST_CHECKSUM_MISMATCH");
        JsonNode manifest;
        try { manifest=JSON.readTree(bytes); require(manifest!=null && manifest.isObject(),"INVALID_JSON"); }
        catch(IOException failure) { throw invalid("INVALID_JSON"); }
        Set<String> expectedFiles=new HashSet<>(fixture?Set.of("long.ubj","short.ubj"):BUNDLE_FILES); expectedFiles.add(manifestName);
        try(var paths=Files.list(root)) {
            Set<String> actual=new HashSet<>(); paths.forEach(path->actual.add(path.getFileName().toString()));
            require(actual.equals(expectedFiles),"UNEXPECTED_BUNDLE_FILES");
        }
        Map<String,byte[]> payloads=new HashMap<>();
        if(!fixture) require(keys(manifest.path("files")).equals(BUNDLE_FILES),"INVALID_ARTIFACT_LIST");
        for(String name:fixture?Set.of("long.ubj","short.ubj"):BUNDLE_FILES) {
            JsonNode spec=fixture?manifest.path("models").path(name.startsWith("long")?"LONG":"SHORT"):null;
            if(fixture) require(name.equals(text(spec,"file")),"INVALID_ARTIFACT_LIST");
            String checksum=fixture?text(spec,"sha256"):text(manifest.path("files"),name);
            require(sha(checksum),"INVALID_ARTIFACT_CHECKSUM");
            byte[] content=read(root.resolve(name),128*1024*1024);
            require(hash(content).equals(checksum),"ARTIFACT_CHECKSUM_MISMATCH"); payloads.put(name,content);
        }
        return new Checked(manifest,Map.copyOf(payloads));
    }

    static Fixture fixture(JsonNode manifest,String candidate,String jar) {
        require(manifest.path("schemaVersion").isInt() && manifest.path("schemaVersion").asInt()==1
                && "TEST_FIXTURE_ONLY".equals(manifest.path("kind").asText()) && manifest.path("productionModelReady").isBoolean()
                && !manifest.path("productionModelReady").asBoolean(),"INVALID_FIXTURE_IDENTITY");
        require(keys(manifest).equals(Set.of("schemaVersion","kind","productionModelReady","candidateSha","jarSha256","targetOs","targetArch","javaMajor",
                "xgboostVersion","featureVersion","featureNames","maxAbsoluteError","float32Rows","models")),"INVALID_FIXTURE_FIELDS");
        require(candidate.equals(text(manifest,"candidateSha")) && jar.equals(text(manifest,"jarSha256"))
                && "Linux".equals(text(manifest,"targetOs")) && "x86_64".equals(text(manifest,"targetArch"))
                && manifest.path("javaMajor").isInt() && manifest.path("javaMajor").asInt()==17
                && VERSION.equals(text(manifest,"xgboostVersion")) && AssetCardFeatureService.FEATURE_VERSION.equals(text(manifest,"featureVersion"))
                && JSON.valueToTree(AssetCardFeatureService.FEATURE_NAMES).equals(manifest.path("featureNames")),"FIXTURE_VERSION_MISMATCH");
        double tolerance=number(manifest,"maxAbsoluteError");
        require(tolerance>0 && tolerance<=1e-7,"INVALID_PARITY_TOLERANCE");
        JsonNode rows=manifest.path("float32Rows"); int columns=AssetCardFeatureService.FEATURE_NAMES.size();
        require(rows.isArray() && rows.size()>0 && rows.size()<=128,"INVALID_FLOAT32_INPUT");
        float[] values=new float[rows.size()*columns];
        for(int r=0;r<rows.size();r++) {
            require(rows.get(r).isArray() && rows.get(r).size()==columns,"INVALID_FLOAT32_INPUT");
            for(int c=0;c<columns;c++) { require(rows.get(r).get(c).isTextual(),"INVALID_FLOAT32_INPUT"); values[r*columns+c]=float32(rows.get(r).get(c).asText()); }
        }
        JsonNode models=manifest.path("models"); require(keys(models).equals(Set.of("LONG","SHORT")),"INVALID_MODEL_PAIR");
        for(String side:List.of("LONG","SHORT")) {
            JsonNode spec=models.path(side);
            require(keys(spec).equals(Set.of("file","sha256","modelVersion","calibrationVersion","calibration","expectedRaw","expectedCalibrated"))
                    && (side.toLowerCase(Locale.ROOT)+".ubj").equals(text(spec,"file")) && sha(text(spec,"sha256")),"INVALID_MODEL_PAIR");
            require(text(spec,"modelVersion").matches("TEST_FIXTURE_[A-Z0-9_]+") && text(spec,"calibrationVersion").matches("TEST_FIXTURE_[A-Z0-9_]+"),"INVALID_FIXTURE_IDENTITY");
            parameters(spec.path("calibration"));
            for(String key:List.of("expectedRaw","expectedCalibrated")) {
                JsonNode predictions=spec.path(key); require(predictions.isArray() && predictions.size()==rows.size(),"INVALID_EXPECTED_PREDICTION");
                for(JsonNode p:predictions) require(p.isNumber() && Double.isFinite(p.asDouble()) && p.asDouble()>=0 && p.asDouble()<=1,"INVALID_EXPECTED_PREDICTION");
            }
        }
        require(!text(models.path("LONG"),"sha256").equals(text(models.path("SHORT"),"sha256"))
                && !models.path("LONG").path("calibration").equals(models.path("SHORT").path("calibration")),"INDEPENDENT_FIXTURES_REQUIRED");
        return new Fixture(values,rows.size(),columns,models,tolerance);
    }
    static float float32(String hex) {
        require(hex!=null && hex.matches("[0-9a-f]{8}"),"INVALID_FLOAT32_INPUT");
        float value=Float.intBitsToFloat((int)Long.parseLong(hex,16));
        require(Float.isFinite(value) || hex.equals("7fc00000"),"INVALID_FLOAT32_INPUT"); return value;
    }
    static Deltas compare(float[][] prediction,JsonNode spec) {
        JsonNode expected=spec.path("expectedRaw"),calibrated=spec.path("expectedCalibrated");
        require(prediction!=null && prediction.length==expected.size() && expected.size()==calibrated.size(),"INVALID_PREDICTION");
        var parameters=parameters(spec.path("calibration")); double rawMax=0,calMax=0;
        for(int r=0;r<prediction.length;r++) {
            require(prediction[r]!=null && prediction[r].length==1 && Float.isFinite(prediction[r][0]) && prediction[r][0]>=0 && prediction[r][0]<=1,"INVALID_PREDICTION");
            double raw=prediction[r][0]; rawMax=Math.max(rawMax,Math.abs(raw-expected.get(r).asDouble()));
            calMax=Math.max(calMax,Math.abs(AssetCardBetaCalibration.calibrate(raw,parameters)-calibrated.get(r).asDouble()));
        }
        return new Deltas(rawMax,calMax);
    }

    static void verifyFixtureModel(Map<String,String> attributes,String[] featureNames,long featureCount,JsonNode spec,String side) {
        require(attributes!=null && featureNames!=null && side.equals(attributes.get("asset_card_side"))
                && "TEST_FIXTURE_ONLY".equals(attributes.get("asset_card_data_kind"))
                && text(spec,"modelVersion").equals(attributes.get("asset_card_model_version"))
                && text(spec,"calibrationVersion").equals(attributes.get("asset_card_calibration_version"))
                && AssetCardFeatureService.FEATURE_VERSION.equals(attributes.get("asset_card_feature_version"))
                && Arrays.asList(featureNames).equals(AssetCardFeatureService.FEATURE_NAMES)
                && featureCount==AssetCardFeatureService.FEATURE_NAMES.size(),"MODEL_IDENTITY_MISMATCH");
    }

    private static void predict(Fixture fixture,Map<String,byte[]> files,Map<String,String> result) throws Exception {
        DMatrix matrix=null; double maximumError=0;
        try {
            matrix=new DMatrix(fixture.data(),fixture.rows(),fixture.columns(),Float.NaN);
            for(String side:List.of("LONG","SHORT")) {
                Booster model=null;
                try {
                    JsonNode spec=fixture.models().path(side); model=XGBoost.loadModel(files.get(side.toLowerCase(Locale.ROOT)+".ubj"));
                    verifyFixtureModel(model.getAttrs(),model.getFeatureNames(),model.getNumFeature(),spec,side);
                    JsonNode version=JSON.readTree(model.toByteArray("json")).path("version");
                    require(version.equals(JSON.valueToTree(List.of(2,1,4))),"NATIVE_VERSION_MISMATCH");
                    Deltas delta=compare(model.predict(matrix),spec);
                    result.put(side+"_MODEL_SHA256",text(spec,"sha256")); result.put(side+"_MODEL_VERSION",text(spec,"modelVersion"));
                    result.put(side+"_CALIBRATION_VERSION",text(spec,"calibrationVersion"));
                    result.put(side+"_RAW_MAX_ERROR",Double.toString(delta.raw())); result.put(side+"_BETA_MAX_ERROR",Double.toString(delta.calibrated()));
                    maximumError=Math.max(maximumError,Math.max(delta.raw(),delta.calibrated()));
                    result.put("MAX_ABSOLUTE_ERROR",Double.toString(maximumError));
                    if(delta.raw()>fixture.tolerance() || delta.calibrated()>fixture.tolerance()) throw new ProbeFailure(5,"PARITY_MISMATCH");
                } finally { if(model!=null) model.dispose(); }
            }
            result.put("XGBOOST_NATIVE_VERSION",VERSION); result.put("FEATURE_VERSION",AssetCardFeatureService.FEATURE_VERSION);
            result.put("FEATURE_COUNT",Integer.toString(fixture.columns())); result.put("FIXTURE_ROWS",Integer.toString(fixture.rows()));
        } finally { if(matrix!=null) matrix.dispose(); }
    }

    static String verifyBuildIdentity(Properties identity,String expectedCandidate) {
        require(identity!=null && identity.stringPropertyNames().equals(Set.of("git.commit.id.full","git.dirty")),"MISSING_BUILD_PROVENANCE");
        String candidate=identity.getProperty("git.commit.id.full");
        require(candidate!=null && candidate.matches("[0-9a-f]{40}") && "false".equals(identity.getProperty("git.dirty")),"UNVERIFIED_BUILD_PROVENANCE");
        require(expectedCandidate==null || expectedCandidate.equals(candidate),"CANDIDATE_BUILD_MISMATCH");
        return candidate;
    }

    private static JarEvidence verifyJar(Path supplied,String expected,String expectedCandidate) throws IOException {
        Path jar=supplied.toRealPath(); require(Files.isRegularFile(jar) && hashFile(jar).equals(expected),"JAR_CHECKSUM_MISMATCH");
        try(JarFile archive=new JarFile(jar.toFile())) {
            require(archive.getEntry("org/springframework/boot/loader/launch/PropertiesLauncher.class")!=null,"STANDARD_BOOT_JAR_REQUIRED");
            Properties identity=new Properties();
            identity.load(new ByteArrayInputStream(entry(archive,"BOOT-INF/classes/git.properties")));
            String candidate=verifyBuildIdentity(identity,expectedCandidate);
            for(Class<?> owner:List.of(AssetCardNativeRuntimeProbe.class,AssetCardBetaCalibration.class,AssetCardFeatureService.class)) {
                String name=owner.getName().replace('.','/')+".class";
                require(Arrays.equals(entry(archive,"BOOT-INF/classes/"+name),resource(owner,"/"+name)),"CANDIDATE_CLASS_ORIGIN_MISMATCH");
            }
            byte[] dependency=entry(archive,"BOOT-INF/lib/xgboost4j_2.12-"+VERSION+".jar");
            byte[] nativeBytes=null,metadata=null,loadedClass=null;
            try(var zip=new ZipInputStream(new ByteArrayInputStream(dependency))) {
                for(var item=zip.getNextEntry();item!=null;item=zip.getNextEntry()) {
                    if(item.getName().equals(NATIVE)) nativeBytes=zip.readAllBytes();
                    if(item.getName().equals("META-INF/maven/ml.dmlc/xgboost4j_2.12/pom.properties")) metadata=zip.readAllBytes();
                    if(item.getName().equals("ml/dmlc/xgboost4j/java/XGBoost.class")) loadedClass=zip.readAllBytes();
                }
            }
            require(nativeBytes!=null && metadata!=null && Arrays.equals(loadedClass,resource(XGBoost.class,"/ml/dmlc/xgboost4j/java/XGBoost.class")),"CANDIDATE_NATIVE_ORIGIN_MISMATCH");
            Properties properties=new Properties(); properties.load(new ByteArrayInputStream(metadata));
            require(VERSION.equals(properties.getProperty("version")),"JAVA_XGBOOST_VERSION_MISMATCH");
            return new JarEvidence(hash(dependency),hash(nativeBytes),candidate);
        }
    }
    private static void nativeEvidence(JarEvidence expected,Map<String,String> result) throws IOException {
        Set<Path> jni=new HashSet<>(),gomp=new HashSet<>();
        try(var lines=Files.lines(Path.of("/proc/self/maps"))) {
            lines.forEach(line->{ String[] fields=line.split("\\s+",6); if(fields.length!=6 || !fields[5].startsWith("/")) return;
                String name=Path.of(fields[5]).getFileName().toString();
                if(name.matches("libxgboost4j.*\\.so")) jni.add(Path.of(fields[5]));
                if(name.matches("libgomp\\.so\\.1(?:\\.[0-9]+)*")) gomp.add(Path.of(fields[5])); });
        }
        require(jni.size()==1 && gomp.size()==1,"LOADED_NATIVE_LIBRARIES_UNVERIFIED");
        require(hashFile(jni.iterator().next()).equals(expected.nativeSha()),"LOADED_NATIVE_CHECKSUM_MISMATCH");
        result.put("XGBOOST_NATIVE_SHA256",expected.nativeSha()); result.put("XGBOOST_NATIVE_VERSION",VERSION);
        result.put("LIBGOMP_SONAME","libgomp.so.1");
        result.put("LIBGOMP_SHA256",hashFile(gomp.iterator().next())); result.put("LIBGOMP_PACKAGE_VERSION","UNKNOWN");
    }

    private static AssetCardBetaCalibration.Parameters parameters(JsonNode node) {
        require(keys(node).equals(Set.of("a","b","c","epsilon")),"INVALID_BETA_PARAMETERS");
        try { return new AssetCardBetaCalibration.Parameters(number(node,"a"),number(node,"b"),number(node,"c"),number(node,"epsilon")); }
        catch(IllegalArgumentException failure) { throw invalid("INVALID_BETA_PARAMETERS"); }
    }
    private static Set<String> keys(JsonNode node) { Set<String> result=new HashSet<>(); if(node!=null && node.isObject()) node.fieldNames().forEachRemaining(result::add); return result; }
    private static String text(JsonNode node,String key) { require(node!=null && node.path(key).isTextual() && !node.path(key).asText().isBlank(),"INVALID_MANIFEST_FIELD"); return node.path(key).asText(); }
    private static double number(JsonNode node,String key) { require(node.path(key).isNumber() && Double.isFinite(node.path(key).asDouble()),"INVALID_MANIFEST_NUMBER"); return node.path(key).asDouble(); }
    private static byte[] read(Path path,int maximum) throws IOException { require(!Files.isSymbolicLink(path) && Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS) && Files.size(path)>0 && Files.size(path)<=maximum,"INVALID_LOCAL_PATH"); return Files.readAllBytes(path); }
    private static byte[] resource(Class<?> owner,String name) throws IOException { try(InputStream in=owner.getResourceAsStream(name)) { require(in!=null,"MISSING_CANDIDATE_CLASS"); return in.readAllBytes(); } }
    private static byte[] entry(JarFile jar,String name) throws IOException { var entry=jar.getJarEntry(name); require(entry!=null && entry.getSize()>0 && entry.getSize()<256L*1024*1024,"MISSING_CANDIDATE_ENTRY"); try(var in=jar.getInputStream(entry)) { return in.readAllBytes(); } }
    private static boolean sha(String value) { return value!=null && value.matches("[0-9a-f]{64}"); }
    private static String hash(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch(Exception failure) { throw invalid("HASH_UNAVAILABLE"); } }
    private static String hashFile(Path path) throws IOException {
        try { MessageDigest digest=MessageDigest.getInstance("SHA-256"); try(var in=Files.newInputStream(path)) { byte[] buffer=new byte[65536]; int count; while((count=in.read(buffer))!=-1) digest.update(buffer,0,count); } return HexFormat.of().formatHex(digest.digest()); }
        catch(java.security.NoSuchAlgorithmException failure) { throw invalid("HASH_UNAVAILABLE"); }
    }
    private static void require(boolean condition,String code) { if(!condition) throw invalid(code); }
    private static ProbeFailure invalid(String code) { return new ProbeFailure(2,code); }
    private static final class ProbeFailure extends IllegalArgumentException {
        private final int exit;
        private ProbeFailure(int exit,String code) { super(code); this.exit=exit; }
    }
}
