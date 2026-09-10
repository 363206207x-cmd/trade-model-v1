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

class AssetCardModelBundleTest {
    @TempDir Path directory;
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
        Map<String,String> attributes=Map.of("asset_card_side","LONG","asset_card_data_kind","REAL_HISTORICAL",
                "asset_card_model_version","TEST_FIXTURE_MODEL","asset_card_feature_version",AssetCardFeatureService.FEATURE_VERSION,
                "asset_card_calibration_version","TEST_FIXTURE_CAL","asset_card_threshold_version","TEST_FIXTURE_THRESHOLD",
                "asset_card_dataset_version","TEST_FIXTURE_DATA");
        String[] names=AssetCardFeatureService.FEATURE_NAMES.toArray(String[]::new);
        var mapper=new ObjectMapper();
        var manifest=mapper.valueToTree(Map.of("modelVersion","TEST_FIXTURE_MODEL","featureVersion",AssetCardFeatureService.FEATURE_VERSION,
                "calibrationVersion","TEST_FIXTURE_CAL","thresholdVersion","TEST_FIXTURE_THRESHOLD"));
        var report=mapper.valueToTree(Map.of("datasetVersion","TEST_FIXTURE_DATA"));
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
            var at=java.time.Instant.EPOCH;
            var frame=new AssetCardFeatureService.Frame("ETHUSDT",at,at,at,AssetCardFeatureService.FEATURE_VERSION,
                    AssetCardFeatureService.FEATURE_NAMES,Collections.nCopies(AssetCardFeatureService.FEATURE_NAMES.size(),null),
                    true,List.of(),"OBSERVATION","RANGE",1.0,99.0,101.0,Map.of());
            var result=bundle.predict(frame);
            assertThat(result.available()).isFalse();
            assertThat(result.pLong()).isNull();
            assertThat(result.reasons()).containsExactly("ASSET_OUTSIDE_VALIDATED_COVERAGE");
            verifyNoInteractions(longModel,shortModel);
        }
    }
    @Test void pythonUbjsonDualModelsLoadAndPredictIdenticallyOnJava17TestFixtureOnly() throws Exception {
        String python=System.getProperty("assetCard.testPython");
        Assumptions.assumeTrue(python!=null,"Opt-in fixed Python/XGBoost test environment required; never real training data");
        String script="""
                import json,sys,numpy as np,xgboost as x
                from pathlib import Path
                root=Path(sys.argv[1]); width=int(sys.argv[2])
                a=np.zeros((40,width),dtype=np.float32); a[:,0]=np.arange(40)
                result={}
                for side,period in [('long',3),('short',5)]:
                    labels=np.asarray([i%period==0 for i in range(40)],dtype=np.float32)
                    b=x.train({'objective':'binary:logistic','device':'cpu','nthread':1,'max_depth':2,'eta':.2,'seed':7},x.DMatrix(a,label=labels),num_boost_round=4)
                    b.save_model(root/(side+'.ubj'))
                    result[side]=float(b.predict(x.DMatrix(a[-1:]))[0])
                print(json.dumps(result))
                """;
        Process process=new ProcessBuilder(python,"-B","-c",script,directory.toString(),String.valueOf(AssetCardFeatureService.FEATURE_NAMES.size())).redirectErrorStream(true).start();
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
        var expected=new ObjectMapper().readTree(output);
        float[] values=new float[AssetCardFeatureService.FEATURE_NAMES.size()]; values[0]=39;
        DMatrix matrix=new DMatrix(values,1,values.length,Float.NaN);
        try {
            for(String side:List.of("long","short")) {
                var booster=XGBoost.loadModel(directory.resolve(side+".ubj").toString());
                try { assertThat((double)booster.predict(matrix)[0][0]).isCloseTo(expected.path(side).asDouble(),within(1e-7)); }
                finally { booster.dispose(); }
            }
        } finally { matrix.dispose(); }
        // No production manifest is emitted: these files are explicitly synthetic test artifacts.
        assertThat(AssetCardModelBundle.load(directory,"0".repeat(64)).validated()).isFalse();
    }
}
