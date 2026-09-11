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
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
                                   String unit,boolean higherIsWorse,String symbol,String side,String metricKey,String riskVersion) {
        public RiskDistribution(List<Double> values,String source,Instant asOf,int samples,double medium,double high,int minimum,String unit,boolean higher) {
            this(values,source,asOf,samples,medium,high,minimum,unit,higher,null,null,null,null);
        }
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
            int lo=0,hi=samples;
            while(lo<hi) { int mid=(lo+hi)>>>1; if(sortedValues.get(mid)<value) lo=mid+1; else hi=mid; }
            int below=lo; hi=samples;
            while(lo<hi) { int mid=(lo+hi)>>>1; if(sortedValues.get(mid)<=value) lo=mid+1; else hi=mid; }
            int equal=lo-below;
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
    private volatile boolean closed;
    private volatile boolean drifted;
    private final ReentrantReadWriteLock resources=new ReentrantReadWriteLock();
    private Lifecycle lifecycle;
    public record Lifecycle(String dataVersion,String riskVersion,Instant trainedThrough,Instant validUntil,
                            Set<String> missingPatterns,List<Double> featureLower,List<Double> featureUpper,
                            double maxFeatureOutlierFraction) {
        public Lifecycle {
            require(dataVersion!=null && !dataVersion.isBlank() && riskVersion!=null && !riskVersion.isBlank()
                    && trainedThrough!=null && validUntil!=null && trainedThrough.isBefore(validUntil),"INVALID_MODEL_LIFECYCLE");
            missingPatterns=Set.copyOf(missingPatterns); featureLower=Collections.unmodifiableList(new ArrayList<>(featureLower));
            featureUpper=Collections.unmodifiableList(new ArrayList<>(featureUpper));
            require(!missingPatterns.isEmpty() && featureLower.size()==AssetCardFeatureService.FEATURE_NAMES.size()
                    && featureUpper.size()==featureLower.size() && maxFeatureOutlierFraction>0 && maxFeatureOutlierFraction<1,"MISSING_DRIFT_OR_PATTERN_COVERAGE");
            for(String pattern:missingPatterns) require(pattern.matches("[01]{"+featureLower.size()+"}"),"INVALID_MISSING_PATTERN");
            for(int i=0;i<featureLower.size();i++) require(featureLower.get(i)==null && featureUpper.get(i)==null
                    || featureLower.get(i)!=null && featureUpper.get(i)!=null && finite(featureLower.get(i)) && finite(featureUpper.get(i))
                    && featureLower.get(i)<=featureUpper.get(i),"INVALID_TRAINING_DRIFT_REFERENCE");
        }
    }

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
    public boolean validated() { return validatedAt(Instant.now()); }
    public boolean validatedAt(Instant at) { return !closed && !drifted && longModel!=null && shortModel!=null && lifecycle!=null
            && at!=null && !at.isBefore(lifecycle.trainedThrough()) && !at.isAfter(lifecycle.validUntil()); }
    public String reason() { return closed?"MODEL_CLOSED":drifted?"FEATURE_DISTRIBUTION_DRIFT_SHADOW":reason; }
    public Thresholds thresholds() { return thresholds; }
    public Map<String,Map<String,RiskDistribution>> riskDistributions() { return riskDistributions; }
    public Map<String,RiskDistribution> riskDistributions(String symbol,String side) {
        if(!validated()) return Map.of();
        Map<String,RiskDistribution> result=new HashMap<>();
        riskDistributions.getOrDefault(symbol,Map.of()).values().stream().filter(d->side.equals(d.side()) && Objects.equals(riskVersion(),d.riskVersion()))
                .forEach(d->result.put(d.metricKey(),d));
        return Map.copyOf(result);
    }
    public String riskVersion() { return lifecycle==null?null:lifecycle.riskVersion(); }
    public String dataVersion() { return lifecycle==null?null:lifecycle.dataVersion(); }
    public Lifecycle lifecycle() { return lifecycle; }
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
            var bundle=new AssetCardModelBundle(longBooster,shortBooster,longCalibration,shortCalibration,
                    requiredText(manifest,"modelVersion"),requiredText(manifest,"calibrationVersion"),requiredText(manifest,"thresholdVersion"),thresholds,distributions,validatedAssets,null);
            bundle.lifecycle=readLifecycle(manifest,report);
            require(bundle.validated(),"MODEL_EXPIRED_OR_NOT_YET_VALID");
            return bundle;
        } catch(Exception | LinkageError error) {
            safeDispose(longBooster); safeDispose(shortBooster);
            // No file contents, paths or native stack traces escape into a public card.
            return unavailable(error instanceof IllegalArgumentException?error.getMessage():"MODEL_BUNDLE_LOAD_FAILED");
        }
    }

    public Prediction predict(AssetCardFeatureService.Frame frame) {
        resources.readLock().lock();
        try { return predictWithResources(frame); }
        finally { resources.readLock().unlock(); }
    }
    private Prediction predictWithResources(AssetCardFeatureService.Frame frame) {
        if(!validated()) return noPrediction("MODEL_EXPIRED_OR_UNAVAILABLE");
        if(frame==null || !frame.ready()) return noPrediction("INSUFFICIENT_POINT_IN_TIME_FEATURES");
        if(frame.symbol()==null || !validatedAssets.contains(frame.symbol())) return noPrediction("ASSET_OUTSIDE_VALIDATED_COVERAGE");
        if(!validatedAt(frame.signalAsOf())) return noPrediction("MODEL_NOT_VALID_AT_SIGNAL_TIME");
        if(!AssetCardFeatureService.FEATURE_VERSION.equals(frame.featureVersion())
                || !AssetCardFeatureService.FEATURE_NAMES.equals(frame.featureNames())
                || frame.vector().size()!=AssetCardFeatureService.FEATURE_NAMES.size()
                || frame.signalAsOf()==null || frame.availableAt()==null || frame.availableAt().isAfter(frame.signalAsOf()))
            return noPrediction("FEATURE_CONTRACT_MISMATCH");
        StringBuilder pattern=new StringBuilder(); int observed=0,outliers=0;
        for(int i=0;i<frame.vector().size();i++) {
            Double value=frame.vector().get(i); pattern.append(value==null?'1':'0');
            if(value!=null) { observed++; Double lower=lifecycle.featureLower().get(i),upper=lifecycle.featureUpper().get(i);
                if(lower==null || upper==null || value<lower || value>upper) outliers++; }
        }
        if(!lifecycle.missingPatterns().contains(pattern.toString())) return noPrediction("MISSING_PATTERN_OUTSIDE_VALIDATED_COVERAGE");
        if(observed==0 || (double)outliers/observed>lifecycle.maxFeatureOutlierFraction()) { drifted=true; return noPrediction("FEATURE_DISTRIBUTION_DRIFT_SHADOW"); }
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
        finally { if(matrix!=null) try { matrix.dispose(); } catch(LinkageError unavailable) { drifted=true; } }
    }
    private Prediction noPrediction(String reason) { return new Prediction(false,null,null,null,null,modelVersion,
            calibrationVersion,thresholdVersion,null,List.of(reason==null?"MODEL_UNAVAILABLE":reason)); }
    @Override public void close() {
        resources.writeLock().lock();
        try {
        if(closed) return;
        closed=true;
        safeDispose(longModel); safeDispose(shortModel);
        } finally { resources.writeLock().unlock(); }
    }

    private static void verifyManifest(JsonNode m) {
        require(m.path("schemaVersion").asInt()==2 && "REAL_HISTORICAL".equals(requiredText(m,"dataKind")),"UNSUPPORTED_MODEL_MANIFEST");
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
        verifyV42Policy(p);
    }

    private static void verifyReport(JsonNode report,JsonNode policy) {
        require("REAL_HISTORICAL".equals(requiredText(report,"dataKind")),"NONREAL_VALIDATION_DATA");
        JsonNode provenance=report.path("provenance");
        require("REAL_HISTORICAL".equals(requiredText(provenance,"kind")) && "BINANCE_SPOT".equals(requiredText(provenance,"source"))
                && "RECORDED_AT_INGESTION".equals(requiredText(provenance,"availabilityBasis")),"UNVERIFIED_HISTORICAL_AVAILABILITY");
        require(requiredText(report,"datasetVersion").equals(requiredText(provenance,"datasetVersion")),"DATASET_VERSION_MISMATCH");
        Instant.parse(requiredText(provenance,"capturedAt"));
        Set<String> providers=new HashSet<>();
        require(provenance.path("sources").isArray() && !provenance.path("sources").isEmpty(),"MISSING_MULTI_SOURCE_PROVENANCE");
        for(JsonNode source:provenance.path("sources")) {
            for(String key:List.of("provider","source","sourceVersion","instrument","unit")) requiredText(source,key);
            require(!Set.of("UNKNOWN","UNVERIFIED").contains(source.path("sourceVersion").asText()),"UNVERIFIED_SOURCE_VERSION");
            providers.add(source.path("provider").asText());
        }
        require(providers.containsAll(Set.of("BINANCE_SPOT","COINGLASS")),"SPOT_AND_COINGLASS_LINEAGE_REQUIRED");
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
        verifyV42Fold(fold,policy);
    }

    static void verifyV42Policy(JsonNode p) {
        require(p.path("requiredAssets").size()==1,"PER_ASSET_BUNDLE_REQUIRED_FOR_RAW_SCALE_FEATURES");
        requiredText(p,"riskVersion");
        require(p.path("riskMetrics").isObject() && p.path("riskMetrics").size()==3,"INCOMPLETE_SIDE_RISK_POLICY");
        for(String side:List.of("LONG","SHORT","NON_DIRECTIONAL")) {
            Set<String> actual=new HashSet<>(); p.path("riskMetrics").path(side).fieldNames().forEachRemaining(actual::add);
            require(actual.equals(requiredRiskMetrics(side)),"INCOMPLETE_SIDE_RISK_POLICY:"+side);
            for(String metric:actual) require(Objects.equals(AssetCardFeatureService.observationUnit(metric),requiredText(p.path("riskMetrics").path(side).path(metric),"unit")),"RISK_METRIC_UNIT_MISMATCH");
        }
        for(String key:List.of("minPositiveSamples","minNegativeSamples","minTimeBlocks","ciReplicates","timeBlockSeconds","minRangeTestSamples","minWatchTestSamples")) positiveInt(p,key);
        require(p.path("bootstrapSeed").isIntegralNumber() && positiveInt(p,"timeBlockSeconds")>=14400
                && positiveInt(p,"minTimeBlocks")>=2 && positiveInt(p,"ciReplicates")>=2,"INVALID_TEMPORAL_UNCERTAINTY_POLICY");
        for(String key:List.of("minEffectiveSamples","maxBrierCiWidth","maxEceCiWidth","maxLogLossCiWidth","maxHitRateCiWidth")) require(number(p,key)>0,"MISSING_UNCERTAINTY_POLICY:"+key);
        for(String key:List.of("ciConfidence","minNonDirectionalCoverage","maxNonDirectionalCoverageSwing","maxRangeFalseEntryRate","maxWatchFalseEntryRate","maxFeatureOutlierFraction","driftLowerQuantile","driftUpperQuantile"))
            require(number(p,key)>0 && number(p,key)<1,"INVALID_BOUNDED_POLICY:"+key);
        require(number(p,"driftLowerQuantile")<number(p,"driftUpperQuantile"),"INVALID_DRIFT_QUANTILES");
        JsonNode costs=p.path("costProvenance");
        require("REAL_HISTORICAL".equals(requiredText(costs,"kind")) && "RATE".equals(requiredText(costs,"unit")),"REAL_COST_PROVENANCE_REQUIRED");
        requiredText(costs,"source"); String version=requiredText(costs,"sourceVersion");
        require(!Set.of("UNKNOWN","UNVERIFIED").contains(version),"UNVERIFIED_COST_SOURCE_VERSION");
        require(Objects.equals(AssetCardFeatureService.spotInstrument(p.path("requiredAssets").get(0).asText()),requiredText(costs,"instrument")),"COST_INSTRUMENT_MISMATCH");
        Instant observed=Instant.parse(requiredText(costs,"observedAt")),available=Instant.parse(requiredText(costs,"availableAt")),expires=Instant.parse(requiredText(costs,"expiresAt"));
        require(!available.isBefore(observed) && !expires.isBefore(available),"COST_PROVENANCE_TIMES");
    }
    static Set<String> requiredRiskMetrics(String side) {
        if("NON_DIRECTIONAL".equals(side)) return Set.of("volatility1m","volatility5m","spreadBps","depth10Bps","depth25Bps");
        require(Set.of("LONG","SHORT").contains(side),"INVALID_RISK_SIDE");
        Set<String> metrics=new HashSet<>(Set.of("structuralCenterDistanceAtr","extensionAtr","return1m","return5m","volatility1m","volatility5m",
                "slope5m","slope1h","slope4h","fundingRate","logLongShortRatio","crowdingOpenInterestChange1h","liquidationImbalance","spreadBps","depth10Bps","depth25Bps","bookImbalance"));
        metrics.add("LONG".equals(side)?"longLiquidation":"shortLiquidation"); return Set.copyOf(metrics);
    }
    static void verifyPopulation(JsonNode p,JsonNode policy) {
        int n=positiveInt(p,"count"),positive=positiveInt(p,"positive"),negative=positiveInt(p,"negative");
        require((long)positive+negative==n && positive>=positiveInt(policy,"minPositiveSamples") && negative>=positiveInt(policy,"minNegativeSamples")
                && number(p,"effectiveSamples")>=number(policy,"minEffectiveSamples") && number(p,"effectiveSamples")<=n,"INSUFFICIENT_INDEPENDENT_OUTCOME_POPULATION");
    }
    static void verifyUncertainty(JsonNode u,JsonNode policy) {
        require(positiveInt(u,"blockCount")>=positiveInt(policy,"minTimeBlocks") && positiveInt(u,"blockSeconds")==positiveInt(policy,"timeBlockSeconds")
                && positiveInt(u,"replicates")==positiveInt(policy,"ciReplicates") && number(u,"confidence")==number(policy,"ciConfidence"),"MISSING_TIME_BLOCK_UNCERTAINTY");
        for(String key:List.of("brier","ece","logLoss","hitRate")) {
            JsonNode band=u.path("intervals").path(key); double lower=number(band,"lower"),upper=number(band,"upper");
            require(lower>=0 && upper>=lower && upper-lower<=number(policy,"max"+Character.toUpperCase(key.charAt(0))+key.substring(1)+"CiWidth"),"UNVALIDATED_METRIC_UNCERTAINTY:"+key);
        }
    }
    private static void verifyV42Pair(JsonNode pair,JsonNode policy) {
        verifyPopulation(pair.path("population"),policy); verifyUncertainty(pair.path("uncertainty"),policy);
        require(positiveInt(pair.path("population"),"count")==positiveInt(pair.path("calibrated"),"count")
                && positiveInt(pair.path("population"),"positive")==positiveInt(pair.path("calibrated"),"positive")
                && positiveInt(pair.path("population"),"negative")==positiveInt(pair.path("calibrated"),"negative"),"OUTCOME_METRIC_POPULATION_MISMATCH");
    }
    private static void verifyV42Fold(JsonNode fold,JsonNode policy) {
        fold.path("sides").forEach(pair->verifyV42Pair(pair,policy));
        fold.path("strata").forEach(pair->verifyV42Pair(pair,policy));
        JsonNode patterns=fold.path("patternMetrics"); require(patterns.isObject() && !patterns.isEmpty(),"MISSING_PATTERN_CALIBRATION_COVERAGE");
        patterns.fields().forEachRemaining(pattern->{
            require(pattern.getKey().matches("[01]{"+AssetCardFeatureService.FEATURE_NAMES.size()+"}"),"INVALID_MISSING_PATTERN");
            for(String side:List.of("LONG","SHORT")) { JsonNode pair=pattern.getValue().path(side); verifyMetrics(pair,positiveInt(policy,"minStratumSamples"),policy); verifyV42Pair(pair,policy); }
        });
        JsonNode populations=fold.path("populations"); require(populations.isArray() && populations.size()==4,"INDEPENDENT_SPLIT_OUTCOME_EVIDENCE_REQUIRED");
        Set<String> expected=new HashSet<>(Set.of("ALL"));
        for(String field:List.of("symbol","regime","volatility")) { String key=field.equals("symbol")?"requiredAssets":field.equals("regime")?"requiredRegimes":"requiredVolatilityStrata";
            policy.path(key).forEach(value->expected.add(field+":"+value.asText())); }
        patterns.fieldNames().forEachRemaining(pattern->expected.add("missing:"+pattern));
        for(int i=0;i<4;i++) {
            JsonNode population=populations.get(i); Set<String> actual=new HashSet<>(); population.fieldNames().forEachRemaining(actual::add);
            require(actual.equals(expected),"INCOMPLETE_SPLIT_STRATIFIED_POPULATIONS");
            for(JsonNode group:population) for(String side:List.of("LONG","SHORT")) verifyPopulation(group.path(side),policy);
            for(String side:List.of("LONG","SHORT")) require(positiveInt(population.path("ALL").path(side),"count")==positiveInt(fold.path("splits").get(i),"count"),"SPLIT_POPULATION_MISMATCH");
            Instant available=Instant.parse(requiredText(policy.path("costProvenance"),"availableAt")),expires=Instant.parse(requiredText(policy.path("costProvenance"),"expiresAt"));
            require(!Instant.parse(fold.path("splits").get(i).path("start").asText()).isBefore(available)
                    && !Instant.parse(fold.path("splits").get(i).path("end").asText()).isAfter(expires),"COSTS_NOT_POINT_IN_TIME");
        }
        for(String state:List.of("RANGE","WATCH")) {
            JsonNode n=fold.path("nonDirectional").path(state); String title=state.equals("RANGE")?"Range":"Watch";
            require("FINAL_TEST_ONLY".equals(requiredText(n,"selectionSplit")) && positiveInt(n,"count")>=positiveInt(policy,"min"+title+"TestSamples")
                    && number(n,"effectiveSamples")>=number(policy,"minEffectiveSamples") && number(n,"effectiveSamples")<=positiveInt(n,"count")
                    && number(n,"coverage")>=number(policy,"minNonDirectionalCoverage") && number(n,"coverage")<=1
                    && number(n,"falseEntryRate")>=0 && number(n,"falseEntryRate")<=number(policy,"max"+title+"FalseEntryRate")
                    && number(n,"coverageSwing")>=0 && number(n,"coverageSwing")<=number(policy,"maxNonDirectionalCoverageSwing"),"UNVALIDATED_FINAL_"+state+"_COVERAGE");
        }
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
            require(asset.getValue().size()==3,"INCOMPLETE_SIDE_RISK_COVERAGE");
            for(String side:List.of("LONG","SHORT","NON_DIRECTIONAL")) {
            JsonNode sideNode=asset.getValue().path(side),sidePolicy=policy.path("riskMetrics").path(side);
            require(sideNode.isObject() && sidePolicy.isObject() && !sidePolicy.isEmpty() && sideNode.size()==sidePolicy.size(),"INCOMPLETE_SIDE_RISK_COVERAGE");
            sideNode.fields().forEachRemaining(metric->{
                JsonNode n=metric.getValue(),spec=sidePolicy.path(metric.getKey());
                List<Double> values=new ArrayList<>(); n.path("sortedValues").forEach(v->{ require(v.isNumber() && finite(v.asDouble()),"INVALID_RISK_VALUE");values.add(v.asDouble()); });
                require(n.path("higherIsWorse").isBoolean() && spec.path("higherIsWorse").isBoolean(),"MISSING_RISK_ORIENTATION");
                var distribution=new RiskDistribution(values,requiredText(n,"source"),Instant.parse(requiredText(n,"asOf")),
                        positiveInt(n,"samples"),number(n,"mediumPercentile"),number(n,"highPercentile"),positiveInt(n,"minSamples"),requiredText(n,"unit"),n.path("higherIsWorse").asBoolean(),
                        requiredText(n,"symbol"),requiredText(n,"side"),requiredText(n,"metricKey"),requiredText(n,"riskVersion"));
                require(asset.getKey().equals(distribution.symbol()) && side.equals(distribution.side()) && metric.getKey().equals(distribution.metricKey())
                        && requiredText(policy,"riskVersion").equals(distribution.riskVersion()),"RISK_SIDE_VERSION_IDENTITY_MISMATCH");
                require(distribution.mediumPercentile()==number(spec,"mediumPercentile") && distribution.highPercentile()==number(spec,"highPercentile")
                        && distribution.minSamples()==positiveInt(spec,"minSamples") && distribution.unit().equals(requiredText(spec,"unit"))
                        && distribution.higherIsWorse()==spec.path("higherIsWorse").asBoolean()
                        && !distribution.asOf().isAfter(trainEnd) && "REAL_HISTORICAL_TRAIN_ONLY".equals(distribution.source()),"UNVALIDATED_RISK_DISTRIBUTION");
                metrics.put(side+":"+metric.getKey(),distribution);
            });
            }
            result.put(asset.getKey(),Map.copyOf(metrics));
        });
        Set<String> expected=new HashSet<>(); policy.path("requiredAssets").forEach(n->expected.add(n.asText()));
        require(result.keySet().equals(expected),"INCOMPLETE_ASSET_RISK_COVERAGE");
        return Map.copyOf(result);
    }
    private static Lifecycle readLifecycle(JsonNode manifest,JsonNode report) {
        JsonNode life=manifest.path("lifecycle");
        require(life.equals(report.path("lifecycle")),"LIFECYCLE_NOT_BOUND_TO_VALIDATION");
        require(requiredText(life,"dataVersion").equals(requiredText(report,"datasetVersion"))
                && requiredText(life,"riskVersion").equals(requiredText(manifest.path("releasePolicy"),"riskVersion"))
                && number(life,"maxFeatureOutlierFraction")==number(manifest.path("releasePolicy"),"maxFeatureOutlierFraction"),"LIFECYCLE_DATA_OR_RISK_IDENTITY_MISMATCH");
        List<Double> lower=new ArrayList<>(),upper=new ArrayList<>();
        life.path("featureLower").forEach(v->lower.add(v.isNull()?null:v.asDouble(Double.NaN)));
        life.path("featureUpper").forEach(v->upper.add(v.isNull()?null:v.asDouble(Double.NaN)));
        Set<String> patterns=new HashSet<>(); life.path("missingPatterns").forEach(v->patterns.add(v.asText()));
        Set<String> tested=new HashSet<>(); report.path("final").path("patternMetrics").fieldNames().forEachRemaining(tested::add);
        require(patterns.equals(tested) && requiredText(life,"trainedThrough").equals(report.path("final").path("splits").get(1).path("labelEnd").asText())
                && requiredText(life,"validatedThrough").equals(report.path("final").path("splits").get(3).path("labelEnd").asText())
                && Instant.parse(requiredText(life,"validUntil")).isAfter(Instant.parse(requiredText(life,"validatedThrough")))
                && !Instant.now().isBefore(Instant.parse(requiredText(life,"validatedThrough"))),"LIFECYCLE_TEMPORAL_OR_PATTERN_MISMATCH");
        return new Lifecycle(requiredText(life,"dataVersion"),requiredText(life,"riskVersion"),Instant.parse(requiredText(life,"trainedThrough")),
                Instant.parse(requiredText(life,"validUntil")),patterns,lower,upper,number(life,"maxFeatureOutlierFraction"));
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
                && requiredText(manifest.path("lifecycle"),"riskVersion").equals(attributes.get("asset_card_risk_version"))
                && requiredText(manifest.path("lifecycle"),"trainedThrough").equals(attributes.get("asset_card_trained_through"))
                && requiredText(manifest.path("lifecycle"),"validUntil").equals(attributes.get("asset_card_valid_until"))
                && Arrays.asList(featureNames).equals(AssetCardFeatureService.FEATURE_NAMES),"MODEL_IDENTITY_OR_SIDE_MISMATCH");
    }
    private static Thresholds thresholds(JsonNode n) { return new Thresholds(tier(n.path("weak")),tier(n.path("normal")),tier(n.path("strong")),number(n,"rangeMaxGap"),number(n,"rangeMaxProbability")); }
    private static Tier tier(JsonNode n) { return new Tier(number(n,"minProbability"),number(n,"minGap")); }
    static int minimumSplitSamples(JsonNode policy,int split) {
        require(split>=0 && split<4,"INVALID_SPLIT_INDEX");
        return positiveInt(policy,List.of("minTrainSamples","minCalibrationSamples","minValidationSamples","minTestSamples").get(split));
    }
    private static double probability(float[][] values) { require(values!=null && values.length==1 && values[0].length==1 && finite(values[0][0]) && values[0][0]>=0 && values[0][0]<=1,"INVALID_BINARY_MODEL_OUTPUT");return values[0][0]; }
    private static void safeDispose(Booster model) { if(model!=null) try { model.dispose(); } catch(LinkageError unavailable) { /* Fail-closed cleanup must not crash application startup. */ } }
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
