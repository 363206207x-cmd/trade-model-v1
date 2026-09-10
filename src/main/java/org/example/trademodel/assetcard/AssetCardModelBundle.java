package org.example.trademodel.assetcard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** One immutable, locally verified pair of models and independent calibrators. No runtime training. */
public final class AssetCardModelBundle implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> FILES = Set.of("long.ubj","short.ubj","calibration.json","thresholds.json",
            "risk-distributions.json","validation.json");
    private static final String LABEL = "ATR_FIRST_TOUCH_LONG_1_0.75_SHORT_SYMMETRIC_TIMEOUT_FAIL_1M_AMBIGUITY_EXCLUDED";
    public record Tier(double minProbability, double minGap) {
        public Tier { require(finite(minProbability) && minProbability>0 && minProbability<1
                && finite(minGap) && minGap>0 && minGap<1,"INVALID_TIER"); }
    }
    public record Thresholds(Tier weak,Tier normal,Tier strong,double rangeMaxGap,double rangeMaxProbability) {
        public Thresholds {
            require(weak!=null && normal!=null && strong!=null,"MISSING_THRESHOLDS");
            require(weak.minProbability()<normal.minProbability() && normal.minProbability()<strong.minProbability()
                    && weak.minGap()<normal.minGap() && normal.minGap()<strong.minGap(),"UNORDERED_THRESHOLDS");
            require(finite(rangeMaxGap) && rangeMaxGap>=0 && rangeMaxGap<weak.minGap()
                    && finite(rangeMaxProbability) && rangeMaxProbability>0 && rangeMaxProbability<weak.minProbability(),"INVALID_RANGE_THRESHOLDS");
        }
    }
    public record RiskDistribution(List<Double> sortedValues,String source,Instant asOf,int samples,
                                   double mediumPercentile,double highPercentile,int minSamples,
                                   String unit,boolean higherIsWorse) {
        public RiskDistribution {
            sortedValues=List.copyOf(sortedValues);
            require(samples==sortedValues.size() && minSamples>0 && samples>=minSamples,"RISK_SAMPLE_COUNT");
            require(source!=null && !source.isBlank() && asOf!=null && unit!=null && !unit.isBlank(),"RISK_PROVENANCE");
            require(finite(mediumPercentile) && finite(highPercentile) && mediumPercentile>0
                    && mediumPercentile<highPercentile && highPercentile<1,"RISK_PERCENTILES");
            for(int i=0;i<sortedValues.size();i++) require(sortedValues.get(i)!=null && finite(sortedValues.get(i))
                    && (i==0 || sortedValues.get(i)>=sortedValues.get(i-1)),"RISK_DISTRIBUTION_ORDER");
        }
        /** Midrank empirical CDF, inverted only when the bundle says lower values are worse. */
        public double adversePercentile(double value) {
            require(finite(value),"INVALID_RISK_OBSERVATION");
            int below=0,equal=0;
            for(double sample:sortedValues) { if(sample<value) below++; else if(sample==value) equal++; }
            double percentile=(below+equal*.5)/samples;
            return higherIsWorse?percentile:1-percentile;
        }
    }
    public record Prediction(boolean available,Double rawLong,Double rawShort,Double pLong,Double pShort,
                             String modelVersion,String calibrationVersion,String thresholdVersion,
                             Thresholds thresholds,List<String> reasons) {
        public Prediction { reasons=List.copyOf(reasons); }
    }

    private final Booster longModel;
    private final Booster shortModel;
    private final AssetCardBetaCalibration.Parameters longCalibration;
    private final AssetCardBetaCalibration.Parameters shortCalibration;
    private final String modelVersion,calibrationVersion,thresholdVersion,reason;
    private final Thresholds thresholds;
    private final Map<String,Map<String,RiskDistribution>> riskDistributions;
    private final Set<String> validatedAssets;
    private boolean closed;

    private AssetCardModelBundle(Booster longModel,Booster shortModel,AssetCardBetaCalibration.Parameters longCalibration,
                                 AssetCardBetaCalibration.Parameters shortCalibration,String modelVersion,String calibrationVersion,
                                 String thresholdVersion,Thresholds thresholds,Map<String,Map<String,RiskDistribution>> riskDistributions,Set<String> validatedAssets,String reason) {
        this.longModel=longModel; this.shortModel=shortModel; this.longCalibration=longCalibration; this.shortCalibration=shortCalibration;
        this.modelVersion=modelVersion; this.calibrationVersion=calibrationVersion; this.thresholdVersion=thresholdVersion;
        this.thresholds=thresholds; this.riskDistributions=Map.copyOf(riskDistributions); this.reason=reason;
        this.validatedAssets=Set.copyOf(validatedAssets);
    }
    public static AssetCardModelBundle unavailable(String reason) {
        return new AssetCardModelBundle(null,null,null,null,null,null,null,null,Map.of(),Set.of(),reason==null?"MODEL_UNAVAILABLE":reason);
    }
    public synchronized boolean validated() { return !closed && longModel!=null && shortModel!=null; }
    public String reason() { return closed?"MODEL_CLOSED":reason; }
    public Thresholds thresholds() { return thresholds; }
    public Map<String,Map<String,RiskDistribution>> riskDistributions() { return riskDistributions; }
    public String modelVersion() { return modelVersion; }
    public String calibrationVersion() { return calibrationVersion; }
    public String thresholdVersion() { return thresholdVersion; }
    public Set<String> validatedAssets() { return validatedAssets; }

    /** Caller supplies a reviewed manifest SHA256, not a checksum read from the untrusted directory itself. */
    public static AssetCardModelBundle load(Path directory,String expectedManifestSha256) {
        Booster longBooster=null,shortBooster=null;
        try {
            require(directory!=null && Files.isDirectory(directory) && !Files.isSymbolicLink(directory),"MISSING_BUNDLE");
            require(expectedManifestSha256!=null && expectedManifestSha256.matches("[0-9a-f]{64}"),"MISSING_TRUSTED_MANIFEST_CHECKSUM");
            Path root=directory.toRealPath();
            byte[] manifestBytes=read(root,"manifest.json",4*1024*1024);
            require(hash(manifestBytes).equals(expectedManifestSha256),"MANIFEST_CHECKSUM_MISMATCH");
            JsonNode manifest=JSON.readTree(manifestBytes);
            verifyManifest(manifest);
            Map<String,byte[]> payloads=new HashMap<>();
            for(String name:FILES) {
                byte[] bytes=read(root,name,128*1024*1024);
                require(hash(bytes).equals(requiredText(manifest.path("files"),name)),"ARTIFACT_CHECKSUM_MISMATCH:"+name);
                payloads.put(name,bytes);
            }
            JsonNode policy=manifest.path("releasePolicy"), report=JSON.readTree(payloads.get("validation.json"));
            verifyReport(report,policy);
            var thresholdDocument=JSON.readTree(payloads.get("thresholds.json"));
            require(thresholdDocument.equals(report.path("final").path("thresholds")),"THRESHOLDS_NOT_BOUND_TO_VALIDATION");
            var thresholds=thresholds(thresholdDocument);
            var calibrators=JSON.readTree(payloads.get("calibration.json"));
            require(calibrators.equals(report.path("final").path("calibrators")),"CALIBRATORS_NOT_BOUND_TO_VALIDATION");
            var longCalibration=calibration(calibrators.path("LONG"));
            var shortCalibration=calibration(calibrators.path("SHORT"));
            var distributions=distributions(JSON.readTree(payloads.get("risk-distributions.json")),policy,
                    Instant.parse(report.path("final").path("splits").get(0).path("end").asText()));
            // Native loading happens only after all independent provenance/calibration/validation gates.
            longBooster=XGBoost.loadModel(payloads.get("long.ubj"));
            shortBooster=XGBoost.loadModel(payloads.get("short.ubj"));
            require(longBooster.getNumFeature()==AssetCardFeatureService.FEATURE_NAMES.size()
                    && shortBooster.getNumFeature()==AssetCardFeatureService.FEATURE_NAMES.size(),"MODEL_FEATURE_COUNT_MISMATCH");
            verifyModelIdentity(longBooster,"LONG",manifest,report);
            verifyModelIdentity(shortBooster,"SHORT",manifest,report);
            Set<String> validatedAssets=new HashSet<>();
            policy.path("requiredAssets").forEach(n->validatedAssets.add(n.asText()));
            return new AssetCardModelBundle(longBooster,shortBooster,longCalibration,shortCalibration,
                    requiredText(manifest,"modelVersion"),requiredText(manifest,"calibrationVersion"),requiredText(manifest,"thresholdVersion"),thresholds,distributions,validatedAssets,null);
        } catch(Exception | LinkageError error) {
            if(longBooster!=null) longBooster.dispose();
            if(shortBooster!=null) shortBooster.dispose();
            // No file contents, paths or native stack traces escape into a public card.
            return unavailable(error instanceof IllegalArgumentException?error.getMessage():"MODEL_BUNDLE_LOAD_FAILED");
        }
    }

    public synchronized Prediction predict(AssetCardFeatureService.Frame frame) {
        if(!validated()) return noPrediction(reason());
        if(frame==null || !frame.ready()) return noPrediction("INSUFFICIENT_POINT_IN_TIME_FEATURES");
        if(frame.symbol()==null || !validatedAssets.contains(frame.symbol())) return noPrediction("ASSET_OUTSIDE_VALIDATED_COVERAGE");
        if(!AssetCardFeatureService.FEATURE_VERSION.equals(frame.featureVersion())
                || !AssetCardFeatureService.FEATURE_NAMES.equals(frame.featureNames())
                || frame.vector().size()!=AssetCardFeatureService.FEATURE_NAMES.size()
                || frame.signalAsOf()==null || frame.availableAt()==null || frame.availableAt().isAfter(frame.signalAsOf()))
            return noPrediction("FEATURE_CONTRACT_MISMATCH");
        DMatrix matrix=null;
        try {
            float[] data=new float[frame.vector().size()];
            for(int i=0;i<data.length;i++) {
                Double value=frame.vector().get(i);
                require(value==null || finite(value) && Float.isFinite(value.floatValue()),"NONFINITE_FEATURE");
                data[i]=value==null?Float.NaN:value.floatValue();
            }
            matrix=new DMatrix(data,1,data.length,Float.NaN);
            double rawLong=probability(longModel.predict(matrix)),rawShort=probability(shortModel.predict(matrix));
            return new Prediction(true,rawLong,rawShort,AssetCardBetaCalibration.calibrate(rawLong,longCalibration),
                    AssetCardBetaCalibration.calibrate(rawShort,shortCalibration),modelVersion,calibrationVersion,thresholdVersion,thresholds,List.of());
        } catch(Exception | LinkageError error) { return noPrediction("MODEL_INFERENCE_UNAVAILABLE"); }
        finally { if(matrix!=null) matrix.dispose(); }
    }
    private Prediction noPrediction(String reason) { return new Prediction(false,null,null,null,null,modelVersion,
            calibrationVersion,thresholdVersion,null,List.of(reason==null?"MODEL_UNAVAILABLE":reason)); }
    @Override public synchronized void close() {
        if(closed) return;
        closed=true;
        if(longModel!=null) longModel.dispose();
        if(shortModel!=null) shortModel.dispose();
    }

    private static void verifyManifest(JsonNode m) {
        require(m.path("schemaVersion").asInt()==1 && "REAL_HISTORICAL".equals(requiredText(m,"dataKind")),"UNSUPPORTED_MODEL_MANIFEST");
        require(AssetCardFeatureService.FEATURE_VERSION.equals(requiredText(m,"featureVersion"))
                && AssetCardFeatureService.ATR_DEFINITION.equals(requiredText(m,"atrDefinition")),"FEATURE_VERSION_MISMATCH");
        List<String> names=new ArrayList<>(); m.path("featureNames").forEach(n->names.add(n.asText()));
        require(names.equals(AssetCardFeatureService.FEATURE_NAMES),"FEATURE_ORDER_MISMATCH");
        require("2.1.4".equals(requiredText(m,"xgboostVersion")) && m.path("horizonSeconds").asInt()==14400
                && LABEL.equals(requiredText(m,"labelDefinition")),"MODEL_RUNTIME_OR_LABEL_MISMATCH");
        Set<String> files=new HashSet<>(); m.path("files").fieldNames().forEachRemaining(files::add);
        require(files.equals(FILES),"INCOMPLETE_ATOMIC_BUNDLE");
        requiredText(m,"modelVersion"); requiredText(m,"calibrationVersion"); requiredText(m,"thresholdVersion");
        JsonNode p=m.path("releasePolicy"); requiredText(p,"version");
        for(String key:List.of("minTrainSamples","minCalibrationSamples","minValidationSamples","minTestSamples","minStratumSamples","minTierSamples",
                "minRangeSamples","minProbabilityBandSamples","minOccupiedBands","binCount","minWalkForwardFolds")) positiveInt(p,key);
        for(String key:List.of("maxEce","maxLogLoss","minProbabilitySpread","roundTripFeeRate","roundTripSlippageRate","maxSignalTradeAgeSeconds"))
            require(number(p,key)>0,"MISSING_FROZEN_RELEASE_POLICY:"+key);
        require(positiveInt(p,"binCount")>=2 && positiveInt(p,"minOccupiedBands")>=2
                && positiveInt(p,"minOccupiedBands")<=positiveInt(p,"binCount"),"COLLAPSED_CALIBRATION_POLICY");
        require(positiveInt(p,"minWalkForwardFolds")>=2,"MULTIPLE_WALK_FORWARD_FOLDS_REQUIRED");
        for(String key:List.of("requiredAssets","requiredRegimes","requiredVolatilityStrata")) {
            require(p.path(key).isArray() && !p.path(key).isEmpty(),"MISSING_COVERAGE_POLICY:"+key);
            Set<String> unique=new HashSet<>();
            for(JsonNode value:p.path(key)) require(value.isTextual() && !value.asText().isBlank() && unique.add(value.asText()),"INVALID_COVERAGE_POLICY:"+key);
        }
    }

    private static void verifyReport(JsonNode report,JsonNode policy) {
        require("REAL_HISTORICAL".equals(requiredText(report,"dataKind")),"NONREAL_VALIDATION_DATA");
        JsonNode provenance=report.path("provenance");
        require("REAL_HISTORICAL".equals(requiredText(provenance,"kind")) && "BINANCE_SPOT".equals(requiredText(provenance,"source"))
                && "RECORDED_AT_INGESTION".equals(requiredText(provenance,"availabilityBasis")),"UNVERIFIED_HISTORICAL_AVAILABILITY");
        require(requiredText(report,"datasetVersion").equals(requiredText(provenance,"datasetVersion")),"DATASET_VERSION_MISMATCH");
        Instant.parse(requiredText(provenance,"capturedAt"));
        require(report.path("actualInputSamples").canConvertToInt() && report.path("actualUsableSamples").canConvertToInt()
                && report.path("actualInputSamples").asInt()>=report.path("actualUsableSamples").asInt()
                && report.path("actualUsableSamples").asInt()>0,"UNKNOWN_REAL_SAMPLE_COUNTS");
        require(report.path("files").isArray() && !report.path("files").isEmpty(),"MISSING_REAL_DATA_MANIFEST");
        for(JsonNode file:report.path("files")) require("RAW_FRAMES".equals(requiredText(file,"kind"))
                && requiredText(file,"sha256").matches("[0-9a-f]{64}"),"MISSING_SOURCE_CHECKSUM");
        JsonNode folds=report.path("folds");
        require(folds.isArray() && folds.size()>=positiveInt(policy,"minWalkForwardFolds"),"MISSING_WALK_FORWARD_VALIDATION");
        Instant lastTestEnd=null;
        for(JsonNode fold:folds) {
            verifyFold(fold,policy);
            long splitSamples=0;
            for(JsonNode split:fold.path("splits")) splitSamples+=positiveInt(split,"count");
            require(splitSamples<=report.path("actualUsableSamples").asInt(),"SPLITS_EXCEED_REAL_USABLE_SAMPLES");
            JsonNode test=fold.path("splits").get(3);
            Instant testStart=Instant.parse(requiredText(test,"start"));
            require(lastTestEnd==null || testStart.isAfter(lastTestEnd),"OVERLAPPING_WALK_FORWARD_TESTS");
            lastTestEnd=Instant.parse(requiredText(test,"labelEnd"));
        }
        require(report.path("final").equals(folds.get(folds.size()-1)),"FINAL_REPORT_NOT_LAST_FOLD");
        // Do not trust productionModelReady / passed / validated booleans; every gate above is recomputed.
    }

    private static void verifyFold(JsonNode fold,JsonNode policy) {
        require(fold.path("embargoSeconds").asInt()==14400,"MISSING_FOUR_HOUR_EMBARGO");
        JsonNode splits=fold.path("splits"); require(splits.isArray() && splits.size()==4,"INDEPENDENT_SPLITS_REQUIRED");
        Instant previousEnd=null;
        String[] names={"TRAIN","CALIBRATION","VALIDATION","TEST"};
        for(int i=0;i<4;i++) {
            JsonNode split=splits.get(i); require(names[i].equals(requiredText(split,"name")),"INVALID_SPLIT_ORDER");
            Instant start=Instant.parse(requiredText(split,"start")),end=Instant.parse(requiredText(split,"end")),labelEnd=Instant.parse(requiredText(split,"labelEnd"));
            require(!start.isAfter(end) && !end.plusSeconds(14400).isAfter(labelEnd)
                    && (previousEnd==null || !start.isBefore(previousEnd.plusSeconds(14400))),"TEMPORAL_LEAKAGE_OR_MISSING_EMBARGO");
            int minimum=minimumSplitSamples(policy,i);
            require(positiveInt(split,"count")>=minimum,"INSUFFICIENT_SPLIT_SAMPLES"); previousEnd=labelEnd;
        }
        JsonNode sides=fold.path("sides"); require(sides.size()==2,"DUAL_MODEL_METRICS_REQUIRED");
        for(String side:List.of("LONG","SHORT")) {
            verifyMetrics(sides.path(side),positiveInt(policy,"minTestSamples"),policy);
            for(String field:List.of("symbol","regime","volatility")) {
                String key=field.equals("symbol")?"requiredAssets":field.equals("regime")?"requiredRegimes":"requiredVolatilityStrata";
                for(JsonNode value:policy.path(key)) verifyMetrics(fold.path("strata").path(side+":"+field+":"+value.asText()),positiveInt(policy,"minStratumSamples"),policy);
            }
        }
        verifyTiers(fold.path("tiers"),policy);
        verifyTiers(fold.path("thresholdSelectionTiers"),policy);
        require(positiveInt(fold.path("rangeEvidence"),"count")>=positiveInt(policy,"minRangeSamples")
                && "VALIDATION_ONLY".equals(requiredText(fold.path("rangeEvidence"),"selectionSplit")),"UNVALIDATED_RANGE_THRESHOLDS");
        verifyPopulationCounts(fold,policy);
    }

    static void verifyPopulationCounts(JsonNode fold,JsonNode policy) {
        require(fold.path("splits").isArray() && fold.path("splits").size()==4,"INDEPENDENT_SPLITS_REQUIRED");
        int testCount=positiveInt(fold.path("splits").get(3),"count");
        int validationCount=positiveInt(fold.path("splits").get(2),"count");
        for(String side:List.of("LONG","SHORT")) {
            require(positiveInt(fold.path("sides").path(side).path("raw"),"count")==testCount
                    && positiveInt(fold.path("sides").path(side).path("calibrated"),"count")==testCount,"TEST_METRIC_POPULATION_MISMATCH");
            for(String field:List.of("symbol","regime","volatility")) {
                String key=field.equals("symbol")?"requiredAssets":field.equals("regime")?"requiredRegimes":"requiredVolatilityStrata";
                long count=0;
                for(JsonNode value:policy.path(key)) count+=positiveInt(fold.path("strata").path(side+":"+field+":"+value.asText()).path("calibrated"),"count");
                require(count==testCount,"STRATUM_POPULATION_MISMATCH");
            }
        }
        long testTiers=0,validationTiers=0;
        for(String side:List.of("LONG","SHORT")) for(String tier:List.of("weak","normal","strong")) {
            testTiers+=positiveInt(fold.path("tiers").path(side+"_"+tier),"count");
            validationTiers+=positiveInt(fold.path("thresholdSelectionTiers").path(side+"_"+tier),"count");
        }
        require(testTiers<=testCount && validationTiers+positiveInt(fold.path("rangeEvidence"),"count")<=validationCount,"TIERS_EXCEED_INDEPENDENT_SPLIT_POPULATION");
    }

    static void verifyMetrics(JsonNode pair,int minimum,JsonNode policy) {
        JsonNode raw=pair.path("raw"),cal=pair.path("calibrated");
        int count=positiveInt(cal,"count"); require(count>=minimum && count==positiveInt(raw,"count"),"INSUFFICIENT_CALIBRATION_METRICS");
        double brier=number(cal,"brier"),rawBrier=number(raw,"brier"),base=number(cal,"baseBrier");
        require(brier>=0 && brier<rawBrier && brier<base && rawBrier<=1 && base<=1
                && Math.abs(base-number(raw,"baseBrier"))<1e-12,"BRIER_NOT_BETTER_THAN_RAW_AND_BASE");
        require(number(cal,"logLoss")>=0 && number(cal,"logLoss")<=number(policy,"maxLogLoss"),"INVALID_LOGLOSS");
        JsonNode bins=cal.path("bins"); require(bins.isArray() && bins.size()==positiveInt(policy,"binCount"),"MISSING_CALIBRATION_CURVE");
        double ece=0; int total=0,occupied=0;
        for(int i=0;i<bins.size();i++) {
            JsonNode bin=bins.get(i); require(bin.path("count").isIntegralNumber() && bin.path("count").asInt()>=0,"INVALID_BAND_COUNT");
            int n=bin.path("count").asInt();
            require(Math.abs(number(bin,"lower")-(double)i/bins.size())<1e-12
                    && Math.abs(number(bin,"upper")-(double)(i+1)/bins.size())<1e-12,"INVALID_BIN_EDGES");
            if(n>0) {
                double mean=number(bin,"meanProbability"),observed=number(bin,"observedRate");
                require(mean>=number(bin,"lower") && mean<=number(bin,"upper") && observed>=0 && observed<=1,"INVALID_CALIBRATION_BIN");
                ece+=n*Math.abs(mean-observed); total+=n;
                if(n>=positiveInt(policy,"minProbabilityBandSamples")) occupied++;
            } else require(bin.path("meanProbability").isNull() && bin.path("observedRate").isNull(),"FABRICATED_EMPTY_BIN");
        }
        require(total==count && Math.abs(ece/count-number(cal,"ece"))<1e-10
                && number(cal,"ece")<=number(policy,"maxEce"),"INCORRECT_BINNED_ECE");
        require(occupied>=positiveInt(policy,"minOccupiedBands") && number(cal,"probabilityMin")>=0 && number(cal,"probabilityMax")<=1
                && number(cal,"probabilityMax")-number(cal,"probabilityMin")>=number(policy,"minProbabilitySpread"),"COLLAPSED_CALIBRATION_CURVE");
    }
    private static void verifyTiers(JsonNode tiers,JsonNode policy) {
        require(tiers.size()==6,"INDEPENDENT_SIX_TIER_EVIDENCE_REQUIRED");
        for(String side:List.of("LONG","SHORT")) for(String tier:List.of("weak","normal","strong")) {
            JsonNode evidence=tiers.path(side+"_"+tier);
            require(positiveInt(evidence,"count")>=positiveInt(policy,"minTierSamples") && number(evidence,"netEdge")>0,"TIER_LACKS_SAMPLES_OR_AFTER_COST_EDGE");
            require(number(evidence,"meanProbability")>=0 && number(evidence,"meanProbability")<=1
                    && number(evidence,"observedRate")>=0 && number(evidence,"observedRate")<=1,"INVALID_TIER_PROBABILITIES");
        }
    }
    private static Map<String,Map<String,RiskDistribution>> distributions(JsonNode node,JsonNode policy,Instant trainEnd) {
        require(node.isObject(),"INVALID_RISK_DISTRIBUTIONS");
        Map<String,Map<String,RiskDistribution>> result=new HashMap<>();
        node.fields().forEachRemaining(asset->{
            Set<String> covered=new HashSet<>(); policy.path("requiredAssets").forEach(n->covered.add(n.asText()));
            require(covered.contains(asset.getKey()),"RISK_ASSET_OUTSIDE_VALIDATED_COVERAGE");
            Map<String,RiskDistribution> metrics=new HashMap<>();
            asset.getValue().fields().forEachRemaining(metric->{
                JsonNode n=metric.getValue(),spec=policy.path("riskMetrics").path(metric.getKey());
                List<Double> values=new ArrayList<>(); n.path("sortedValues").forEach(v->{ require(v.isNumber() && finite(v.asDouble()),"INVALID_RISK_VALUE");values.add(v.asDouble()); });
                require(n.path("higherIsWorse").isBoolean() && spec.path("higherIsWorse").isBoolean(),"MISSING_RISK_ORIENTATION");
                var distribution=new RiskDistribution(values,requiredText(n,"source"),Instant.parse(requiredText(n,"asOf")),
                        positiveInt(n,"samples"),number(n,"mediumPercentile"),number(n,"highPercentile"),positiveInt(n,"minSamples"),requiredText(n,"unit"),n.path("higherIsWorse").asBoolean());
                require(distribution.mediumPercentile()==number(spec,"mediumPercentile") && distribution.highPercentile()==number(spec,"highPercentile")
                        && distribution.minSamples()==positiveInt(spec,"minSamples") && distribution.unit().equals(requiredText(spec,"unit"))
                        && distribution.higherIsWorse()==spec.path("higherIsWorse").asBoolean()
                        && !distribution.asOf().isAfter(trainEnd) && "REAL_HISTORICAL_TRAIN_ONLY".equals(distribution.source()),"UNVALIDATED_RISK_DISTRIBUTION");
                metrics.put(metric.getKey(),distribution);
            });
            result.put(asset.getKey(),Map.copyOf(metrics));
        });
        return Map.copyOf(result);
    }
    private static AssetCardBetaCalibration.Parameters calibration(JsonNode n) { return new AssetCardBetaCalibration.Parameters(number(n,"a"),number(n,"b"),number(n,"c"),number(n,"epsilon")); }
    static void verifyModelIdentity(Booster model,String side,JsonNode manifest,JsonNode report) throws Exception {
        verifyModelMetadata(model.getAttrs(),model.getFeatureNames(),side,manifest,report);
    }
    static void verifyModelMetadata(Map<String,String> attributes,String[] featureNames,String side,JsonNode manifest,JsonNode report) {
        require(attributes!=null && featureNames!=null && side.equals(attributes.get("asset_card_side"))
                && "REAL_HISTORICAL".equals(attributes.get("asset_card_data_kind"))
                && requiredText(manifest,"modelVersion").equals(attributes.get("asset_card_model_version"))
                && requiredText(manifest,"featureVersion").equals(attributes.get("asset_card_feature_version"))
                && requiredText(manifest,"calibrationVersion").equals(attributes.get("asset_card_calibration_version"))
                && requiredText(manifest,"thresholdVersion").equals(attributes.get("asset_card_threshold_version"))
                && requiredText(report,"datasetVersion").equals(attributes.get("asset_card_dataset_version"))
                && Arrays.asList(featureNames).equals(AssetCardFeatureService.FEATURE_NAMES),"MODEL_IDENTITY_OR_SIDE_MISMATCH");
    }
    private static Thresholds thresholds(JsonNode n) { return new Thresholds(tier(n.path("weak")),tier(n.path("normal")),tier(n.path("strong")),number(n,"rangeMaxGap"),number(n,"rangeMaxProbability")); }
    private static Tier tier(JsonNode n) { return new Tier(number(n,"minProbability"),number(n,"minGap")); }
    static int minimumSplitSamples(JsonNode policy,int split) {
        require(split>=0 && split<4,"INVALID_SPLIT_INDEX");
        return positiveInt(policy,List.of("minTrainSamples","minCalibrationSamples","minValidationSamples","minTestSamples").get(split));
    }
    private static double probability(float[][] values) { require(values!=null && values.length==1 && values[0].length==1 && finite(values[0][0]) && values[0][0]>=0 && values[0][0]<=1,"INVALID_BINARY_MODEL_OUTPUT");return values[0][0]; }
    private static byte[] read(Path directory,String name,int maximum) throws Exception {
        Path file=directory.resolve(name); require(!Files.isSymbolicLink(file) && Files.isRegularFile(file)
                && Files.size(file)>0 && Files.size(file)<=maximum,"MISSING_OR_INVALID_BUNDLE_ARTIFACT:"+name);
        return Files.readAllBytes(file);
    }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String requiredText(JsonNode n,String key) { require(n.path(key).isTextual() && !n.path(key).asText().isBlank(),"MISSING_BUNDLE_FIELD:"+key);return n.path(key).asText(); }
    private static int positiveInt(JsonNode n,String key) { require(n.path(key).isIntegralNumber() && n.path(key).canConvertToInt() && n.path(key).asInt()>0,"MISSING_POSITIVE_COUNT:"+key);return n.path(key).asInt(); }
    private static double number(JsonNode n,String key) { require(n.path(key).isNumber() && finite(n.path(key).asDouble()),"MISSING_NUMERIC_EVIDENCE:"+key);return n.path(key).asDouble(); }
    private static boolean finite(double value) { return Double.isFinite(value); }
    private static void require(boolean condition,String reason) { if(!condition) throw new IllegalArgumentException(reason); }
}
