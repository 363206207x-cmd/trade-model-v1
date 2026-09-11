package org.example.trademodel.assetcard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assumptions;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.Booster;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardModelBundleTest {
    @TempDir Path directory;
    @Test void registrySelectsOnlyTheExactValidatedAssetAndKeepsBundleIdentitiesIndependent() throws Exception {
        var btc=source("BTC"); var eth=source("ETH");
        try(var registry=registry(s->testBundle(s.equals(btc)?"BTCUSDT":"ETHUSDT",s.equals(btc)?"BTC":"ETH"))) {
            registry.reconcile(Map.of("BTCUSDT",btc,"ETHUSDT",eth));
            try(var b=registry.acquire("BTCUSDT"); var e=registry.acquire("ETHUSDT"); var missing=registry.acquire("SOLUSDT")) {
                assertThat(b.bundle().validatedAssets()).containsExactly("BTCUSDT");
                assertThat(e.bundle().validatedAssets()).containsExactly("ETHUSDT");
                assertThat(b.bundle().modelVersion()).isEqualTo("TEST_BTC_MODEL");
                assertThat(e.bundle().modelVersion()).isEqualTo("TEST_ETH_MODEL");
                assertThat(b.bundle().calibrationVersion()).isNotEqualTo(e.bundle().calibrationVersion());
                assertThat(b.bundle().thresholdVersion()).isNotEqualTo(e.bundle().thresholdVersion());
                assertThat(b.bundle().riskVersion()).isNotEqualTo(e.bundle().riskVersion());
                assertThat(missing.bundle().validated()).isFalse();
            }
        }
    }
    @Test void registryWrongAssetOrFailedReplacementRevokesOnlyThatSymbol() throws Exception {
        var btc=source("BTC"); var eth=source("ETH"); var bad=source("BAD");
        try(var registry=registry(s->{ if(s.equals(bad)) throw new UnsatisfiedLinkError("TEST_ONLY"); return testBundle(s.equals(btc)?"BTCUSDT":"ETHUSDT",s.equals(btc)?"BTC":"ETH"); })) {
            registry.reconcile(Map.of("BTCUSDT",btc,"ETHUSDT",eth));
            registry.reconcile(Map.of("BTCUSDT",bad,"ETHUSDT",eth));
            try(var b=registry.acquire("BTCUSDT"); var e=registry.acquire("ETHUSDT")) {
                assertThat(b.bundle().validated()).isFalse(); assertThat(b.bundle().thresholds()).isNull();
                assertThat(e.bundle().validated()).isTrue();
            }
            registry.reconcile(Map.of("BTCUSDT",eth,"ETHUSDT",eth));
            try(var b=registry.acquire("BTCUSDT"); var e=registry.acquire("ETHUSDT")) {
                assertThat(b.bundle().validated()).isFalse();
                assertThat(b.bundle().reason()).isEqualTo("BUNDLE_ASSET_IDENTITY_MISMATCH");
                assertThat(e.bundle().validated()).isTrue();
            }
        }
    }
    @Test void registryReplacementKeepsAnAcquiredVersionAliveWithoutBlockingOtherAssets() throws Exception {
        var old=source("OLD"); var replacement=source("NEW"); var eth=source("ETH");
        try(var registry=registry(s->testBundle(s.equals(eth)?"ETHUSDT":"BTCUSDT",s.equals(old)?"OLD":s.equals(replacement)?"NEW":"ETH"))) {
            registry.reconcile(Map.of("BTCUSDT",old,"ETHUSDT",eth));
            var held=registry.acquire("BTCUSDT"); var oldBundle=held.bundle();
            try {
                registry.reconcile(Map.of("BTCUSDT",replacement,"ETHUSDT",eth));
                assertThat(oldBundle.validated()).isTrue();
                try(var current=registry.acquire("BTCUSDT"); var other=registry.acquire("ETHUSDT")) {
                    assertThat(current.bundle().modelVersion()).isEqualTo("TEST_NEW_MODEL");
                    assertThat(other.bundle().validated()).isTrue();
                }
            } finally { held.close(); }
            assertThat(oldBundle.reason()).isEqualTo("MODEL_CLOSED");
            held.close(); // resource release is idempotent
            assertThatThrownBy(held::bundle).isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void expiredRemovedAndClosedRegistryNeverReturnAnotherAssetsModel() throws Exception {
        var btc=source("BTC"); var expired=source("EXPIRED");
        try(var registry=registry(s->{ var b=testBundle("BTCUSDT","BTC"); if(s.equals(expired)) expire(b); return b; })) {
            registry.reconcile(Map.of("BTCUSDT",expired));
            try(var lease=registry.acquire("BTCUSDT")) { assertThat(lease.bundle().validated()).isFalse(); }
            registry.reconcile(Map.of("BTCUSDT",btc));
            try(var lease=registry.acquire("BTCUSDT")) { expire(lease.bundle()); assertThat(lease.bundle().validated()).isFalse(); }
            registry.reconcile(Map.of("BTCUSDT",btc));
            try(var lease=registry.acquire("BTCUSDT")) { assertThat(lease.bundle().validated()).isTrue(); }
            registry.reconcile(Map.of());
            try(var lease=registry.acquire("BTCUSDT")) { assertThat(lease.bundle().validated()).isFalse(); }
            registry.close();
            registry.reconcile(Map.of("BTCUSDT",btc));
            try(var lease=registry.acquire("BTCUSDT")) { assertThat(lease.bundle().validated()).isFalse(); }
        }
    }
    @Test void configuredSourceMapIsImmutableExactAndShadowByDefault() {
        var properties=new AssetCardProperties(); var values=new HashMap<String,AssetCardModelBundle.Source>();
        values.put("btcusdt",source("BTC")); properties.setModelBundles(values); values.clear();
        assertThat(properties.getModelBundles()).containsOnlyKeys("BTCUSDT");
        assertThatThrownBy(()->properties.getModelBundles().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(properties.getModelMode()).isEqualTo(AssetCardProperties.ModelMode.SHADOW);
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getLabelMaturityBatchSize()).isEqualTo(128);
        assertThat(properties.getLabelMaturityInterval()).isEqualTo(java.time.Duration.ofSeconds(60));
        assertThat(properties.isTrainingExportEnabled()).isFalse();
        assertThat(properties.getTrainingExportDirectory()).isNull();
        assertThatThrownBy(()->properties.setModelBundles(Map.of("BTC*",source("BAD")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->properties.setModelBundles(Map.of("BTCUSDT",source("A"),"btcusdt",source("B")))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void lateReplacementCannotOverwriteNewerAssetConfigurationOrResurrectClosedRegistry() throws Exception {
        for(boolean closeDuringLoad:List.of(false,true)) {
            var slow=source("SLOW"); var fast=source("FAST"); var eth=source("ETH");
            var entered=new java.util.concurrent.CountDownLatch(1); var resume=new java.util.concurrent.CountDownLatch(1);
            var discarded=new java.util.concurrent.atomic.AtomicReference<AssetCardModelBundle>();
            var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
            try(var registry=registry(s->{
                var bundle=testBundle(s.equals(eth)?"ETHUSDT":"BTCUSDT",s.equals(slow)?"SLOW":s.equals(fast)?"FAST":"ETH");
                if(s.equals(slow)) {
                    discarded.set(bundle); entered.countDown();
                    try { if(!resume.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("TEST rendezvous timeout"); }
                    catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
                }
                return bundle;
            })) {
                var old=executor.submit(()->registry.reconcile(Map.of("BTCUSDT",slow,"ETHUSDT",eth)));
                try {
                    assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                    if(closeDuringLoad) registry.close();
                    else registry.reconcile(Map.of("BTCUSDT",fast)); // ETH removed by newer whole-map revision
                } finally { resume.countDown(); }
                old.get(5,java.util.concurrent.TimeUnit.SECONDS);
                try(var btc=registry.acquire("BTCUSDT"); var other=registry.acquire("ETHUSDT")) {
                    assertThat(btc.bundle().validated()).isEqualTo(!closeDuringLoad);
                    if(!closeDuringLoad) assertThat(btc.bundle().modelVersion()).isEqualTo("TEST_FAST_MODEL");
                    assertThat(other.bundle().validated()).isFalse();
                }
                assertThat(discarded.get().reason()).isEqualTo("MODEL_CLOSED");
            } finally { resume.countDown(); executor.shutdownNow(); }
        }
    }
    @Test void springConfigurationBindsPerAssetPathAndReviewedManifestWithoutEnablingExport() {
        var source=new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(Map.of(
                "trade-model.asset-card.model-bundles.btcusdt.path",directory.toString(),
                "trade-model.asset-card.model-bundles.btcusdt.expected-manifest-sha256","a".repeat(64)));
        var properties=new org.springframework.boot.context.properties.bind.Binder(source)
                .bind("trade-model.asset-card",org.springframework.boot.context.properties.bind.Bindable.of(AssetCardProperties.class)).get();
        assertThat(properties.getModelBundles()).containsOnlyKeys("BTCUSDT");
        assertThat(properties.getModelBundles().get("BTCUSDT").path()).isEqualTo(directory.toAbsolutePath());
        assertThat(properties.getModelBundles().get("BTCUSDT").expectedManifestSha256()).isEqualTo("a".repeat(64));
        assertThat(properties.isEnabled()).isFalse(); assertThat(properties.isTrainingExportEnabled()).isFalse();
    }
    @Test void publicRegistryNeverAcceptsMissingOrUncheckedBundleMetadata() {
        try(var registry=new AssetCardModelBundle.Registry()) {
            var results=registry.reconcile(Map.of("BTCUSDT",new AssetCardModelBundle.Source(directory,null),
                    "ETHUSDT",new AssetCardModelBundle.Source(directory.resolve("missing"),"0".repeat(64))));
            assertThat(results.values()).allMatch(result->!result.available());
            try(var btc=registry.acquire("BTCUSDT"); var eth=registry.acquire("ETHUSDT")) {
                assertThat(btc.bundle().thresholds()).isNull(); assertThat(eth.bundle().thresholds()).isNull();
                assertThat(btc.bundle().validatedAssets()).isEmpty(); assertThat(eth.bundle().validatedAssets()).isEmpty();
            }
        }
    }
    private AssetCardModelBundle.Source source(String name) { return new AssetCardModelBundle.Source(directory.resolve("TEST_ONLY_"+name),"0".repeat(64)); }
    private static AssetCardModelBundle.Registry registry(java.util.function.Function<AssetCardModelBundle.Source,AssetCardModelBundle> loader) throws Exception {
        var constructor=AssetCardModelBundle.Registry.class.getDeclaredConstructor(java.util.function.Function.class);
        constructor.setAccessible(true); return constructor.newInstance(loader); // isolated loader seam, never a public unvalidated install API
    }
    private static AssetCardModelBundle testBundle(String symbol,String version) {
        try {
            var constructor=AssetCardModelBundle.class.getDeclaredConstructor(Booster.class,Booster.class,
                    AssetCardBetaCalibration.Parameters.class,AssetCardBetaCalibration.Parameters.class,String.class,String.class,String.class,
                    AssetCardModelBundle.Thresholds.class,Map.class,Set.class,String.class);
            constructor.setAccessible(true);
            var parameters=new AssetCardBetaCalibration.Parameters(1,1,0,1e-7);
            var thresholds=new AssetCardModelBundle.Thresholds(new AssetCardModelBundle.Tier(.55,.05),new AssetCardModelBundle.Tier(.65,.1),new AssetCardModelBundle.Tier(.75,.2),.01,.5);
            var bundle=constructor.newInstance(mock(Booster.class),mock(Booster.class),parameters,parameters,"TEST_"+version+"_MODEL","TEST_"+version+"_CAL","TEST_"+version+"_THRESHOLD",thresholds,Map.of(),Set.of(symbol),null);
            setTestLifecycle(bundle);
            var field=AssetCardModelBundle.class.getDeclaredField("lifecycle"); field.setAccessible(true);
            var life=bundle.lifecycle(); field.set(bundle,new AssetCardModelBundle.Lifecycle("TEST_"+version+"_DATA","TEST_"+version+"_RISK",life.trainedThrough(),life.validUntil(),life.missingPatterns(),life.featureLower(),life.featureUpper(),life.maxFeatureOutlierFraction()));
            return bundle;
        } catch(Exception failure) { throw new AssertionError(failure); }
    }
    private static void expire(AssetCardModelBundle bundle) {
        try {
            var field=AssetCardModelBundle.class.getDeclaredField("lifecycle"); field.setAccessible(true); var life=bundle.lifecycle();
            field.set(bundle,new AssetCardModelBundle.Lifecycle(life.dataVersion(),life.riskVersion(),java.time.Instant.EPOCH,java.time.Instant.EPOCH.plusSeconds(1),life.missingPatterns(),life.featureLower(),life.featureUpper(),life.maxFeatureOutlierFraction()));
        } catch(Exception failure) { throw new AssertionError(failure); }
    }
    @Test void absentBundleDoesNotInventSamplesMetricsThresholdsOrConfidence() {
        try (var bundle = AssetCardModelBundle.load(directory, "0".repeat(64))) {
            assertThat(bundle.validated()).isFalse();
            assertThat(bundle.thresholds()).isNull();
            assertThat(bundle.riskDistributions()).isEmpty();
            assertThat(bundle.predict(null).pLong()).isNull();
            assertThat(bundle.predict(null).pShort()).isNull();
        }
    }
    @Test void uncheckedManifestAndInvalidTierOrderingAreRejected() throws Exception {
        Files.writeString(directory.resolve("manifest.json"), "{\"validated\":true}");
        assertThat(AssetCardModelBundle.load(directory, null).validated()).isFalse();
        assertThat(AssetCardModelBundle.load(directory, "0".repeat(64)).validated()).isFalse();
        assertThatThrownBy(() -> new AssetCardModelBundle.Thresholds(
                new AssetCardModelBundle.Tier(.7,.1),new AssetCardModelBundle.Tier(.6,.2),
                new AssetCardModelBundle.Tier(.8,.3),.05,.5)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void realEceIsRecomputedAndValidatedTrueCannotRescueCollapsedBands() throws Exception {
        var mapper=new ObjectMapper();
        var pair=mapper.readTree("""
                {"raw":{"count":2,"brier":0.16,"baseBrier":0.25},
                "calibrated":{"count":2,"brier":0.04,"baseBrier":0.25,"ece":0.2,"logLoss":0.2231435513,
                "probabilityMin":0.2,"probabilityMax":0.8,
                "bins":[{"lower":0,"upper":0.5,"count":1,"meanProbability":0.2,"observedRate":0},
                {"lower":0.5,"upper":1,"count":1,"meanProbability":0.8,"observedRate":1}]}}
                """);
        var policy=mapper.readTree("""
                {"binCount":2,"minProbabilityBandSamples":1,"minOccupiedBands":2,
                "maxEce":0.25,"maxLogLoss":0.5,"minProbabilitySpread":0.1}
                """);
        AssetCardModelBundle.verifyMetrics(pair,2,policy);
        ((ObjectNode)pair.path("calibrated")).put("ece",0);
        assertThatThrownBy(()->AssetCardModelBundle.verifyMetrics(pair,2,policy)).hasMessage("INCORRECT_BINNED_ECE");
        ((ObjectNode)pair.path("calibrated")).put("ece",.2).put("probabilityMin",.5).put("probabilityMax",.5).put("validated",true);
        assertThatThrownBy(()->AssetCardModelBundle.verifyMetrics(pair,2,policy)).hasMessage("COLLAPSED_CALIBRATION_CURVE");
    }
    @Test void assetDistributionPreservesDirectionAndNeverInventsMissingHistory() {
        var distribution=new AssetCardModelBundle.RiskDistribution(List.of(10.0,20.0,30.0,40.0),
                "TEST_FIXTURE",java.time.Instant.EPOCH,4,.75,.9,4,"QUOTE_CURRENCY",false);
        assertThat(distribution.adversePercentile(1)).isEqualTo(1);
        assertThat(distribution.adversePercentile(50)).isEqualTo(0);
        assertThat(AssetCardModelBundle.unavailable("NO_REAL_HISTORY").riskDistributions()).isEmpty();
    }
    @Test void independentValidationSplitMinimumHasNoImplicitTestMinimum() throws Exception {
        var policy=new ObjectMapper().readTree("{\"minTrainSamples\":5,\"minCalibrationSamples\":4,\"minValidationSamples\":3,\"minTestSamples\":2}");
        assertThat(AssetCardModelBundle.minimumSplitSamples(policy,2)).isEqualTo(3);
        assertThat(AssetCardModelBundle.minimumSplitSamples(policy,3)).isEqualTo(2);
        ((ObjectNode)policy).remove("minValidationSamples");
        assertThatThrownBy(()->AssetCardModelBundle.minimumSplitSamples(policy,2)).hasMessage("MISSING_POSITIVE_COUNT:minValidationSamples");
    }
    @Test void metricStrataAndTierPopulationsMustMatchTheIndependentTestSplit() {
        var mapper=new ObjectMapper();
        ObjectNode fold=mapper.createObjectNode();
        fold.putArray("splits").addObject().put("count",40);
        ((com.fasterxml.jackson.databind.node.ArrayNode)fold.path("splits")).addObject().put("count",35);
        ((com.fasterxml.jackson.databind.node.ArrayNode)fold.path("splits")).addObject().put("count",30);
        ((com.fasterxml.jackson.databind.node.ArrayNode)fold.path("splits")).addObject().put("count",30);
        var sides=fold.putObject("sides"); var strata=fold.putObject("strata");
        var tiers=fold.putObject("tiers"); var selected=fold.putObject("thresholdSelectionTiers");
        fold.putObject("rangeEvidence").put("count",3);
        for(String side:List.of("LONG","SHORT")) {
            var pair=sides.putObject(side); pair.putObject("raw").put("count",30); pair.putObject("calibrated").put("count",30);
            for(String key:List.of("symbol:BTCUSDT","regime:LONG","volatility:LOW")) strata.putObject(side+":"+key).putObject("calibrated").put("count",30);
            for(String tier:List.of("weak","normal","strong")) { tiers.putObject(side+"_"+tier).put("count",2); selected.putObject(side+"_"+tier).put("count",2); }
        }
        var policy=mapper.valueToTree(Map.of("requiredAssets",List.of("BTCUSDT"),"requiredRegimes",List.of("LONG"),"requiredVolatilityStrata",List.of("LOW")));
        AssetCardModelBundle.verifyPopulationCounts(fold,policy);
        ((ObjectNode)sides.path("LONG").path("raw")).put("count",31);
        assertThatThrownBy(()->AssetCardModelBundle.verifyPopulationCounts(fold,policy)).hasMessage("TEST_METRIC_POPULATION_MISMATCH");
        ((ObjectNode)sides.path("LONG").path("raw")).put("count",30);
        ((ObjectNode)strata.path("LONG:symbol:BTCUSDT").path("calibrated")).put("count",31);
        assertThatThrownBy(()->AssetCardModelBundle.verifyPopulationCounts(fold,policy)).hasMessage("STRATUM_POPULATION_MISMATCH");
        ((ObjectNode)strata.path("LONG:symbol:BTCUSDT").path("calibrated")).put("count",30);
        ((ObjectNode)tiers.path("LONG_strong")).put("count",31);
        assertThatThrownBy(()->AssetCardModelBundle.verifyPopulationCounts(fold,policy)).hasMessage("TIERS_EXCEED_INDEPENDENT_SPLIT_POPULATION");
    }
    @Test void modelSideAndEmbeddedMetadataMustMatchTheAtomicBundle() throws Exception {
        Map<String,String> attributes=new HashMap<>(Map.of("asset_card_side","LONG","asset_card_data_kind","REAL_HISTORICAL",
                "asset_card_model_version","TEST_FIXTURE_MODEL","asset_card_feature_version",AssetCardFeatureService.FEATURE_VERSION,
                "asset_card_calibration_version","TEST_FIXTURE_CAL","asset_card_threshold_version","TEST_FIXTURE_THRESHOLD",
                "asset_card_dataset_version","TEST_FIXTURE_DATA"));
        attributes.put("asset_card_risk_version","TEST_RISK");
        attributes.put("asset_card_trained_through","2025-01-01T00:00:00Z");
        attributes.put("asset_card_valid_until","2030-01-01T00:00:00Z");
        String[] names=AssetCardFeatureService.FEATURE_NAMES.toArray(String[]::new);
        var mapper=new ObjectMapper();
        var manifest=mapper.valueToTree(Map.of("modelVersion","TEST_FIXTURE_MODEL","featureVersion",AssetCardFeatureService.FEATURE_VERSION,
                "calibrationVersion","TEST_FIXTURE_CAL","thresholdVersion","TEST_FIXTURE_THRESHOLD"));
        var report=mapper.valueToTree(Map.of("datasetVersion","TEST_FIXTURE_DATA"));
        ((ObjectNode)manifest).putObject("lifecycle").put("riskVersion","TEST_RISK").put("trainedThrough","2025-01-01T00:00:00Z").put("validUntil","2030-01-01T00:00:00Z");
        AssetCardModelBundle.verifyModelMetadata(attributes,names,"LONG",manifest,report);
        assertThatThrownBy(()->AssetCardModelBundle.verifyModelMetadata(attributes,names,"SHORT",manifest,report)).hasMessage("MODEL_IDENTITY_OR_SIDE_MISMATCH");
        assertThatThrownBy(()->AssetCardModelBundle.verifyModelMetadata(attributes,new String[]{"wrongOrder"},"LONG",manifest,report)).hasMessage("MODEL_IDENTITY_OR_SIDE_MISMATCH");
        var synthetic=new HashMap<>(attributes); synthetic.put("asset_card_data_kind","SYNTHETIC_FIXTURE");
        assertThatThrownBy(()->AssetCardModelBundle.verifyModelMetadata(synthetic,names,"LONG",manifest,report)).hasMessage("MODEL_IDENTITY_OR_SIDE_MISMATCH");
        ((ObjectNode)manifest).put("calibrationVersion","DIFFERENT_CALIBRATOR");
        assertThatThrownBy(()->AssetCardModelBundle.verifyModelMetadata(attributes,names,"LONG",manifest,report)).hasMessage("MODEL_IDENTITY_OR_SIDE_MISMATCH");
    }
    @Test void unsupportedAssetFailsBeforeAnyNativeInference() throws Exception {
        Booster longModel=mock(Booster.class),shortModel=mock(Booster.class);
        var constructor=AssetCardModelBundle.class.getDeclaredConstructor(Booster.class,Booster.class,
                AssetCardBetaCalibration.Parameters.class,AssetCardBetaCalibration.Parameters.class,String.class,String.class,String.class,
                AssetCardModelBundle.Thresholds.class,Map.class,Set.class,String.class);
        constructor.setAccessible(true); // isolated test seam, never a production loader or exported fixture bundle
        var parameters=new AssetCardBetaCalibration.Parameters(1,1,0,1e-7);
        var thresholds=new AssetCardModelBundle.Thresholds(new AssetCardModelBundle.Tier(.55,.05),
                new AssetCardModelBundle.Tier(.65,.1),new AssetCardModelBundle.Tier(.75,.2),.01,.5);
        try(var bundle=constructor.newInstance(longModel,shortModel,parameters,parameters,"TEST_FIXTURE_MODEL","TEST_FIXTURE_CAL",
                "TEST_FIXTURE_THRESHOLD",thresholds,Map.of(),Set.of("BTCUSDT"),null)) {
            setTestLifecycle(bundle);
            var at=java.time.Instant.EPOCH;
            var frame=new AssetCardFeatureService.Frame("ETHUSDT",at,at,at,AssetCardFeatureService.FEATURE_VERSION,
                    AssetCardFeatureService.FEATURE_NAMES,Collections.nCopies(AssetCardFeatureService.FEATURE_NAMES.size(),null),
                    true,List.of(),"OBSERVATION","RANGE",1.0,99.0,101.0,Map.of());
            var result=bundle.predict(frame);
            assertThat(result.available()).isFalse();
            assertThat(result.pLong()).isNull();
            assertThat(result.reasons()).containsExactly("ASSET_OUTSIDE_VALIDATED_COVERAGE");
            var unknownPattern=new ArrayList<Double>(frame.vector()); unknownPattern.set(0,1.0);
            var missing=new AssetCardFeatureService.Frame("BTCUSDT",at,at,at,frame.featureVersion(),frame.featureNames(),unknownPattern,
                    true,List.of(),"OBSERVATION","RANGE",1.0,99.0,101.0,Map.of());
            assertThat(bundle.predict(missing).reasons()).containsExactly("MISSING_PATTERN_OUTSIDE_VALIDATED_COVERAGE");
            var outlier=new AssetCardFeatureService.Frame("BTCUSDT",at,at,at,frame.featureVersion(),frame.featureNames(),
                    Collections.nCopies(frame.featureNames().size(),1.0e12),true,List.of(),"OBSERVATION","RANGE",1.0,99.0,101.0,Map.of());
            assertThat(bundle.predict(outlier).reasons()).containsExactly("FEATURE_DISTRIBUTION_DRIFT_SHADOW");
            assertThat(bundle.validated()).isFalse();
            verifyNoInteractions(longModel,shortModel);
        }
    }
    private static void setTestLifecycle(AssetCardModelBundle bundle) throws Exception {
        int count=AssetCardFeatureService.FEATURE_NAMES.size();
        var field=AssetCardModelBundle.class.getDeclaredField("lifecycle"); field.setAccessible(true);
        field.set(bundle,new AssetCardModelBundle.Lifecycle("TEST_FIXTURE_DATA","TEST_RISK",java.time.Instant.EPOCH,java.time.Instant.parse("2100-01-01T00:00:00Z"),
                Set.of("0".repeat(count),"1".repeat(count)),Collections.nCopies(count,-1_000_000.0),Collections.nCopies(count,1_000_000.0),.5));
    }
    @Test void v42PopulationAndTimeBlockIntervalsCannotBeReplacedByRawCounts() throws Exception {
        var mapper=new ObjectMapper();
        var policy=mapper.readTree("""
                {"minPositiveSamples":2,"minNegativeSamples":2,"minEffectiveSamples":2,"minTimeBlocks":2,
                 "timeBlockSeconds":14400,"ciReplicates":40,"ciConfidence":0.9,
                 "maxBrierCiWidth":0.2,"maxEceCiWidth":0.2,"maxLogLossCiWidth":0.2,"maxHitRateCiWidth":0.2}
                """);
        ObjectNode population=mapper.createObjectNode().put("count",100).put("positive",50).put("negative",50).put("effectiveSamples",1);
        assertThatThrownBy(()->AssetCardModelBundle.verifyPopulation(population,policy)).hasMessage("INSUFFICIENT_INDEPENDENT_OUTCOME_POPULATION");
        population.put("effectiveSamples",3); AssetCardModelBundle.verifyPopulation(population,policy);
        population.put("negative",0);
        assertThatThrownBy(()->AssetCardModelBundle.verifyPopulation(population,policy)).isInstanceOf(IllegalArgumentException.class);
        ObjectNode uncertainty=mapper.createObjectNode().put("blockCount",3).put("blockSeconds",14400).put("replicates",40).put("confidence",.9);
        var intervals=uncertainty.putObject("intervals");
        for(String key:List.of("brier","ece","logLoss","hitRate")) intervals.putObject(key).put("lower",.1).put("upper",.2);
        AssetCardModelBundle.verifyUncertainty(uncertainty,policy);
        intervals.remove("ece");
        assertThatThrownBy(()->AssetCardModelBundle.verifyUncertainty(uncertainty,policy)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sideVersionDistributionUsesSignedMidrankAndBinaryBounds() {
        var distribution=new AssetCardModelBundle.RiskDistribution(List.of(-3.0,-1.0,-1.0,2.0),"TEST_FIXTURE",java.time.Instant.EPOCH,
                4,.75,.9,4,"RATE",true,"BTCUSDT","LONG","fundingRate","TEST_RISK");
        assertThat(distribution.adversePercentile(-1)).isEqualTo(.5);
        assertThat(distribution.adversePercentile(-4)).isZero();
        assertThat(distribution.adversePercentile(3)).isEqualTo(1);
        assertThat(distribution.side()).isEqualTo("LONG");
        assertThat(distribution.riskVersion()).isEqualTo("TEST_RISK");
    }
    @Test void pythonUbjsonDualModelsLoadAndPredictIdenticallyOnJava17TestFixtureOnly() throws Exception {
        String python=System.getProperty("assetCard.testPython");
        Assumptions.assumeTrue(python!=null,"Opt-in fixed Python/XGBoost test environment required; never real training data");
        assertThat(Runtime.version().feature()).as("Native interoperability must run on Java 17").isEqualTo(17);
        String script="""
                import json,sys,numpy as np,xgboost as x
                from pathlib import Path
                sys.path.insert(0,sys.argv[3])
                import asset_card_model as model
                assert x.__version__ == model.XGBOOST_VERSION == '2.1.4'
                root=Path(sys.argv[1]); width=int(sys.argv[2])
                a=np.zeros((40,width),dtype=np.float32); a[:,0]=np.arange(40)
                result={}
                for side,cutoff in [('long',20),('short',12)]:
                    labels=np.asarray([i>=cutoff if side=='long' else i<cutoff for i in range(40)],dtype=np.float32)
                    b=x.train({'objective':'binary:logistic','device':'cpu','nthread':1,'max_depth':2,'eta':.2,'seed':7,'verbosity':0},x.DMatrix(a,label=labels),num_boost_round=4)
                    calibrator=model.fit_beta(b.predict(x.DMatrix(a)).tolist(),labels.tolist())
                    b.save_model(root/(side+'.ubj'))
                    raw=b.predict(x.DMatrix(a[[0,19,39]])).tolist()
                    result[side]={'raw':raw,'calibrator':calibrator,'calibrated':[model.beta(p,calibrator) for p in raw]}
                print(json.dumps(result))
                """;
        int width=AssetCardFeatureService.FEATURE_NAMES.size();
        Process process=new ProcessBuilder(python,"-B","-c",script,directory.toString(),String.valueOf(width),
                Path.of("scripts").toAbsolutePath().toString()).redirectErrorStream(true).start();
        String output;
        try {
            assertThat(process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS)).as("Synthetic native fixture process completed").isTrue();
            output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            assertThat(process.exitValue()).as("Synthetic dual-model fixture generation succeeded").isZero();
        } finally { if(process.isAlive()) process.destroyForcibly().waitFor(); }
        var expected=new ObjectMapper().readTree(output);
        assertThat(expected.path("long").path("calibrator")).isNotEqualTo(expected.path("short").path("calibrator"));
        float[] values=new float[width*3]; values[width]=19; values[width*2]=39;
        DMatrix matrix=new DMatrix(values,3,width,Float.NaN);
        try {
            for(String side:List.of("long","short")) {
                var booster=XGBoost.loadModel(directory.resolve(side+".ubj").toString());
                try {
                    var node=expected.path(side); var parameters=node.path("calibrator");
                    var calibrator=new AssetCardBetaCalibration.Parameters(parameters.path("a").asDouble(),
                            parameters.path("b").asDouble(),parameters.path("c").asDouble(),parameters.path("epsilon").asDouble());
                    float[][] predictions=booster.predict(matrix);
                    assertThat(predictions.length).isEqualTo(3);
                    double rawDelta=0,calibratedDelta=0;
                    for(int row=0;row<3;row++) {
                        assertThat(predictions[row]).hasSize(1);
                        double raw=predictions[row][0],calibrated=AssetCardBetaCalibration.calibrate(raw,calibrator);
                        double expectedRaw=node.path("raw").get(row).asDouble(),expectedCalibrated=node.path("calibrated").get(row).asDouble();
                        assertThat(raw).isCloseTo(expectedRaw,within(1e-7));
                        assertThat(calibrated).isCloseTo(expectedCalibrated,within(1e-7));
                        rawDelta=Math.max(rawDelta,Math.abs(raw-expectedRaw));
                        calibratedDelta=Math.max(calibratedDelta,Math.abs(calibrated-expectedCalibrated));
                    }
                    System.out.printf(Locale.ROOT,"ASSET_CARD_NATIVE_INTEROP side=%s JAVA=17 XGBOOST=2.1.4 UBJ=PASS RAW_MAX_DELTA=%.12g BETA_MAX_DELTA=%.12g%n",side,rawDelta,calibratedDelta);
                } finally { booster.dispose(); }
            }
        } finally { matrix.dispose(); }
        // No production manifest is emitted: these files are explicitly synthetic test artifacts.
        try(var rejected=AssetCardModelBundle.load(directory,"0".repeat(64))) { assertThat(rejected.validated()).isFalse(); }
    }
}
