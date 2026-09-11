"""Deterministic synthetic unit fixtures; never production training evidence."""
import importlib.util
import math
import pathlib
import unittest
from unittest.mock import patch
import copy
import json
import tempfile
import io
import contextlib
import sys
import struct
import re

SPEC = importlib.util.spec_from_file_location("asset_card_model", pathlib.Path(__file__).with_name("asset_card_model.py"))
model = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(model)


def generate_native_fixture(output,candidate_sha,jar_sha256):
    """Explicit, disposable interoperability data; deliberately NOT a production bundle."""
    if not re.fullmatch(r"[0-9a-f]{40}",candidate_sha) or not re.fullmatch(r"[0-9a-f]{64}",jar_sha256):
        raise ValueError("Explicit candidate and JAR checksums required")
    root=pathlib.Path(output)
    if root.exists() or root.is_symlink(): raise ValueError("Refusing to overwrite fixture directory")
    import numpy as np
    import xgboost as xgb
    if xgb.__version__!=model.XGBOOST_VERSION: raise ValueError("Fixed XGBoost version required")
    width=len(model.FEATURE_NAMES)
    data=np.zeros((40,width),dtype=np.float32); data[:,0]=np.arange(40,dtype=np.float32)
    inputs=data[[0,19,39]].copy()
    # This missing value also exercises the common float32/NaN representation.
    inputs[0,-1]=np.nan
    result={"schemaVersion":1,"kind":"TEST_FIXTURE_ONLY","productionModelReady":False,
            "candidateSha":candidate_sha,"jarSha256":jar_sha256,"targetOs":"Linux","targetArch":"x86_64",
            "javaMajor":17,"xgboostVersion":model.XGBOOST_VERSION,"featureVersion":model.FEATURE_VERSION,
            "featureNames":model.FEATURE_NAMES,"maxAbsoluteError":1e-7,
            "float32Rows":[["7fc00000" if np.isnan(value) else struct.pack("!f",float(value)).hex() for value in row] for row in inputs],"models":{}}
    root.mkdir(mode=0o700)
    for side,cutoff in (("LONG",20),("SHORT",12)):
        labels=np.asarray([i>=cutoff if side=="LONG" else i<cutoff for i in range(40)],dtype=np.float32)
        training=xgb.DMatrix(data,label=labels,feature_names=model.FEATURE_NAMES,missing=np.nan)
        booster=xgb.train({"objective":"binary:logistic","device":"cpu","nthread":1,"max_depth":2,"eta":.2,"seed":7,"verbosity":0},training,num_boost_round=4)
        calibration=model.fit_beta(booster.predict(training).tolist(),labels.tolist())
        model_version="TEST_FIXTURE_"+side+"_MODEL_V1"; calibration_version="TEST_FIXTURE_"+side+"_BETA_V1"
        booster.set_attr(asset_card_data_kind="TEST_FIXTURE_ONLY",asset_card_side=side,
                         asset_card_model_version=model_version,asset_card_calibration_version=calibration_version,
                         asset_card_feature_version=model.FEATURE_VERSION)
        filename=side.lower()+".ubj"; booster.save_model(root/filename)
        # Reload the exact serialized UBJ being passed to Java, not an in-memory substitute.
        loaded=xgb.Booster(); loaded.load_model(root/filename)
        raw=loaded.predict(xgb.DMatrix(inputs,feature_names=model.FEATURE_NAMES,missing=np.nan)).tolist()
        result["models"][side]={"file":filename,"sha256":model.sha256(root/filename),"modelVersion":model_version,
                                "calibrationVersion":calibration_version,"calibration":calibration,
                                "expectedRaw":raw,"expectedCalibrated":[model.beta(value,calibration) for value in raw]}
    (root/"native-fixture.json").write_text(json.dumps(result,sort_keys=True,allow_nan=False)+"\n",encoding="utf-8")
    return result


class ModelTests(unittest.TestCase):
    def test_json_numeric_policy_arrays_remain_numbers_while_times_keep_nanoseconds(self):
        value=model.decode_json('{"signalAsOf":1767268800.000000001,"volatilityStrata":[0.01,0.02],"nested":[[0.5],{"value":0.25,"availableAt":1767268800.000000002}]}')
        self.assertTrue(all(model.finite(v) for v in value["volatilityStrata"]))
        self.assertTrue(model.finite(value["nested"][0][0]))
        self.assertEqual(model.instant(value["signalAsOf"]),"2026-01-01T12:00:00.000000001Z")
        self.assertEqual(model.instant(value["nested"][1]["availableAt"]),"2026-01-01T12:00:00.000000002Z")
        self.assertEqual(json.loads(json.dumps(value,default=model.json_default))["volatilityStrata"],[.01,.02])

    def test_v42_observation_identity_expiry_and_signed_value_contract(self):
        observation={"value":-.01,"source":"COINGLASS:COINGLASS_FUNDING:fixture","instrument":"BINANCE:PERPETUAL:LINEAR:BTC/USDT",
                     "sourceVersion":"TEST_SOURCE_V2","unit":"RATE","observedAt":90,"availableAt":95,"expiresAt":110}
        self.assertTrue(model.valid_observation("BTCUSDT","fundingRate",observation,100))
        for key,value in (("instrument","BINANCE:SPOT:NONE:BTC/USDT"),("unit","PERCENT"),("expiresAt",99),("sourceVersion","UNKNOWN"),("availableAt",89)):
            self.assertFalse(model.valid_observation("BTCUSDT","fundingRate",{**observation,key:value},100))
        self.assertNotIn("takerBuySellRatio",model.FEATURE_NAMES)
        self.assertEqual(model.FEATURE_VERSION,"SPOT_CARD_FEATURES_V2_SIGNED_PIT")

    def test_overlapping_labels_do_not_count_as_independent_samples(self):
        rows=[{"signalAsOf":i*300,"labelEnd":i*300+14400} for i in range(48)]
        self.assertLess(model.overlap_effective_samples(rows),3)
        self.assertEqual(model.overlap_effective_samples([{"signalAsOf":0,"labelEnd":14400},{"signalAsOf":14400,"labelEnd":28800}]),2)

    @staticmethod
    def path_bars(start=0,count=48,step=300):
        return [{"openTime":start+i*step,"closeTime":start+(i+1)*step,"open":100,"close":100,"high":100.5,"low":99.5} for i in range(count)]

    @staticmethod
    def feature_fixture(at=1767268800):
        raw={"symbol":"BTCUSDT","signalAsOf":at,"bars":{},"evidence":{}}
        for interval,step in model.INTERVALS.items():
            raw["bars"][interval]=[{"openTime":at-(24-i)*step,"closeTime":at-(23-i)*step,"availableAt":at-(23-i)*step,
                "open":100+i,"high":102+i,"low":99+i,"close":101+i,"volume":10+i,"takerBuyBaseVolume":5+i/2,"tradeCount":100+i} for i in range(24)]
        for key in ("spreadBps","depth10Bps","depth25Bps"):
            raw["evidence"][key]=ModelTests.observation(key,2,at)
        raw["evidence"]["spotPrice"]=ModelTests.observation("spotPrice",100,at)
        return raw

    @staticmethod
    def observation(key,value,at):
        return {"value":value,"source":"BINANCE_SPOT","observedAt":at,"availableAt":at,"expiresAt":at+60,
                "instrument":"BINANCE:SPOT:NONE:BTC/USDT","sourceVersion":"TEST_FIXTURE_V2","unit":model.UNITS[key],"observationId":"test-trade" if key=="spotPrice" else None}

    def test_beta_matches_java_equation_and_identity(self):
        self.assertAlmostEqual(model.beta(.2, {"a":1,"b":1,"c":0,"epsilon":1e-7}),.2)
        p = model.beta(.2,{"a":.8,"b":1.3,"c":-.2,"epsilon":1e-7})
        self.assertAlmostEqual(p, 1/(1+math.exp(-(.8*math.log(.2)-1.3*math.log(.8)-.2))))

    def test_first_touch_and_ambiguity_do_not_invent_outcomes(self):
        bars = self.path_bars()
        self.assertEqual(model.first_touch(100, 1, 0, bars, [], "LONG"), (0,"TIMEOUT"))
        bars[0].update(high=101.5,low=99.0)
        self.assertEqual(model.first_touch(100,1,0,bars,[],"LONG"),(None,"AMBIGUOUS"))
        minute=self.path_bars(count=5,step=60)
        minute[0]["high"]=101.5; minute[2]["low"]=99.0
        self.assertEqual(model.first_touch(100,1,0,bars,minute,"LONG"),(1,"TARGET"))
        minute[0]["low"]=99
        self.assertEqual(model.first_touch(100,1,0,bars,minute,"LONG"),(None,"AMBIGUOUS"))
        self.assertEqual(model.first_touch(100,1,0,bars[:-1],[],"LONG"),(None,"INCOMPLETE_HORIZON"))

    def test_actual_asof_boundary_minute_is_never_rounded_back(self):
        bars=self.path_bars(count=49); bars[0]["high"]=101.5
        minutes=self.path_bars(count=5,step=60); minutes[0]["high"]=101.5
        self.assertEqual(model.first_touch(100,1,5.123,bars,minutes,"LONG"),(None,"AMBIGUOUS"))
        minutes[0]["high"]=100.5; minutes[1]["high"]=101.5
        self.assertEqual(model.first_touch(100,1,5.123,bars,minutes,"LONG"),(1,"TARGET"))
        bars[0]["high"]=100.5; bars[-1]["high"]=101.5
        ending=self.path_bars(start=14400,count=5,step=60); ending[0]["high"]=101.5
        self.assertEqual(model.first_touch(100,1,5.123,bars,ending,"LONG"),(None,"AMBIGUOUS"))
        ending[0]["high"]=100.5; ending[1]["high"]=101.5
        self.assertEqual(model.first_touch(100,1,5.123,bars,ending,"LONG"),(0,"TIMEOUT"))
        # A partial end bar after the true horizon cannot turn the timeout into success.

    def test_short_barriers_and_unobserved_ohlc_fail_closed(self):
        bars=self.path_bars(); bars[1]["low"]=98.5
        self.assertEqual(model.first_touch(100,1,0,bars,[],"SHORT"),(1,"TARGET"))
        bars[1]["low"]=99.5; bars[1]["high"]=101
        self.assertEqual(model.first_touch(100,1,0,bars,[],"SHORT"),(0,"STOP"))
        bars[-1]["close"]=float("nan")
        self.assertEqual(model.first_touch(100,1,0,bars,[],"SHORT"),(None,"INCOMPLETE_HORIZON"))
        bars[-1]["close"]=100000
        self.assertEqual(model.first_touch(100,1,0,bars,[],"SHORT"),(None,"INCOMPLETE_HORIZON"))

    def test_future_label_bars_require_exact_opens_and_closed_duration_tolerance(self):
        bars=self.path_bars()
        bars[1]["openTime"]=model.timestamp("1970-01-01T00:05:00.000000001Z")
        self.assertEqual(model.first_touch(100,1,0,bars,[],"LONG"),(None,"INCOMPLETE_HORIZON"))
        bars=self.path_bars(); bars[1]["closeTime"]=model.timestamp("1970-01-01T00:09:59.998999999Z")
        self.assertEqual(model.first_touch(100,1,0,bars,[],"LONG"),(None,"INCOMPLETE_HORIZON"))

    def test_future_label_bar_lineage_is_registered_and_unit_bound(self):
        bar={**self.path_bars(count=1)[0],"availableAt":300,"instrument":model.instrument("BTCUSDT"),
             "source":"BINANCE_SPOT_CLOSED_5M","sourceVersion":"TEST_FIXTURE_V2","unit":"OHLCV"}
        provenance={"sources":[{k:bar[k] for k in ("source","sourceVersion","instrument","unit")}]}
        self.assertTrue(model.future_bar_valid(bar,"BTCUSDT",301,provenance))
        for key,value in (("unit","RATE"),("source","BINANCE_SPOT_FAKE"),("sourceVersion","UNREGISTERED"),("availableAt",302)):
            self.assertFalse(model.future_bar_valid({**bar,key:value},"BTCUSDT",301,provenance))
        self.assertFalse(model.future_bar_valid(bar,"BTCUSDT",301,{"sources":[]}))

    def test_timeout_requires_actual_point_in_time_horizon_trade(self):
        at=1767268800; cutoff=at+5.123
        raw=self.feature_fixture(at); raw["signalAsOf"]=cutoff
        raw["evidence"]["spotPrice"].update(observedAt=cutoff,availableAt=cutoff)
        record={"rawFrame":raw,"future5m":self.path_bars(at,49),"future1m":[],
                "provenance":{"kind":"SYNTHETIC_FIXTURE","capturedAt":model.instant(at+15000)}}
        for bar in record["future5m"]:
            bar.update(instrument=model.instrument("BTCUSDT"),source="BINANCE_SPOT",sourceVersion="TEST_FIXTURE_V2",unit="OHLCV",availableAt=bar["closeTime"])
        record["provenance"]["sources"]=[{k:o[k] for k in ("source","sourceVersion","instrument","unit")} for o in raw["evidence"].values()]
        record["provenance"]["sources"].append({k:record["future5m"][0][k] for k in ("source","sourceVersion","instrument","unit")})
        policy={"maxSignalTradeAgeSeconds":2,"roundTripFeeRate":.001,"roundTripSlippageRate":.001,"volatilityStrata":[.01,.02]}
        # Isolate preparation logic only; the real CLI validator rejects this fixture unconditionally.
        with self.assertRaises(ValueError): model.validate_provenance(record["provenance"])
        with patch.object(model,"validate_provenance",return_value=None):
            rows,excluded=model.prepare_records([record],policy)
            self.assertEqual(rows,[]); self.assertEqual(excluded,{"MISSING_POINT_IN_TIME_HORIZON_TRADE":1})
            record["horizonTrade"]={**self.observation("spotPrice",100.2,cutoff+14400-.5),"availableAt":cutoff+14400}
            rows,excluded=model.prepare_records([record],policy)
            self.assertEqual(excluded,{})
            self.assertEqual(rows[0]["signalAsOf"],model.timestamp(cutoff))
            self.assertAlmostEqual(rows[0]["netReturns"]["LONG"],0)
            self.assertEqual(rows[0]["labels"],{"LONG":0,"SHORT":0})
            record["horizonTrade"]["availableAt"]=cutoff+14400+.1
            self.assertEqual(model.prepare_records([record],policy)[1],{"MISSING_POINT_IN_TIME_HORIZON_TRADE":1})
            record["provenance"]["capturedAt"]=model.instant(at+100)
            self.assertEqual(model.prepare_records([record],policy)[1],{"UNCLOSED_OR_UNCAPTURED_FUTURE":1})

    def test_ece_is_weighted_actual_bin_calibration_error(self):
        report=model.metrics([1,0,1,0],[.9,.8,.2,.1],.5,5)
        self.assertGreater(report["ece"],0)
        self.assertAlmostEqual(report["brier"],(.01+.64+.64+.01)/4)
        self.assertEqual(sum(x["count"] for x in report["bins"]),4)

    def test_purge_and_embargo_separate_all_label_intervals(self):
        rows=[{"signalAsOf":i*3600,"labelEnd":i*3600+14400,"labelAvailableAt":i*3600+14400} for i in range(120)]
        parts=model.temporal_split(rows,{"trainEnd":30*3600,"calibrationEnd":60*3600,"validationEnd":90*3600,"testEnd":120*3600})
        for left,right in zip(parts,parts[1:]):
            self.assertTrue(max(x["labelEnd"] for x in left)+14400 <= min(x["signalAsOf"] for x in right))
        self.assertFalse(set(id(x) for x in parts[0]) & set(id(x) for x in parts[-1]))

    def test_synthetic_or_unknown_provenance_rejected(self):
        for kind in ("SYNTHETIC_FIXTURE","UNKNOWN",None):
            with self.assertRaises(ValueError): model.validate_provenance({"kind":kind})

    def test_future_features_missing_stay_missing_and_actual_feature_values_match_spec(self):
        at=1767268800
        raw=self.feature_fixture(at)
        raw["evidence"]["fundingRate"]={"value":.01,"source":"COINGLASS","observedAt":at-60,"availableAt":at+1}
        frame=model.build_frame(raw)
        self.assertTrue(frame["ready"])
        self.assertEqual(frame["atr"],3)
        self.assertEqual(frame["structuralSupport"],103)
        self.assertEqual(frame["structuralResistance"],125)
        self.assertAlmostEqual(frame["vector"][0],124/118-1)
        self.assertIsNone(frame["vector"][model.FEATURE_NAMES.index("fundingRate")])
        self.assertNotIn("fundingRate",frame["realInputs"])
        self.assertEqual(frame["fourHourTrend"],"LONG")

    def test_closed_higher_timeframes_and_availability_are_not_intrabar_features(self):
        raw=self.feature_fixture(); original=model.build_frame(raw)
        raw["signalAsOf"]+=10; raw["evidence"]["spotPrice"]["value"]=1000
        updated=model.build_frame(raw)
        self.assertEqual(original["realInputs"]["volatility5m"]["availableAt"],updated["realInputs"]["volatility5m"]["availableAt"])
        for interval in ("1h","4h"):
            indices=[i for i,name in enumerate(model.FEATURE_NAMES) if name.startswith(interval+".")]
            self.assertEqual([original["vector"][i] for i in indices],[updated["vector"][i] for i in indices])
            future=copy.deepcopy(raw)
            future["bars"][interval][-1]["availableAt"]=future["signalAsOf"]+1
            self.assertFalse(model.build_frame(future)["ready"])
            self.assertTrue(all(model.build_frame(future)["vector"][i] is None for i in indices))

    def test_timeframe_conflict_is_closed_slope_pair_ratio_not_risk_level(self):
        raw=self.feature_fixture()
        def slope(interval,sign):
            for i,bar in enumerate(raw["bars"][interval]):
                price=150+sign*i; bar.update(open=price,close=price,high=price+2,low=price-1)
        slope("1h",-1)
        self.assertEqual(model.build_frame(raw)["realInputs"]["timeframeConflict"]["value"],2/3)
        slope("1h",0); slope("4h",-1)
        self.assertEqual(model.build_frame(raw)["realInputs"]["timeframeConflict"]["value"],1/3)
        slope("4h",1)
        self.assertEqual(model.build_frame(raw)["realInputs"]["timeframeConflict"]["value"],0)
        del raw["bars"]["4h"]
        self.assertNotIn("timeframeConflict",model.build_frame(raw)["realInputs"])

    def test_each_split_has_its_own_explicit_minimum_and_versions_are_required(self):
        parts=[[{}]*5,[{}]*4,[{}]*3,[{}]*2]
        policy={"minTrainSamples":5,"minCalibrationSamples":4,"minValidationSamples":3,"minTestSamples":2}
        model.validate_split_samples(parts,policy)
        policy["minValidationSamples"]=4
        with self.assertRaisesRegex(ValueError,"minValidationSamples"): model.validate_split_samples(parts,policy)
        del policy["minValidationSamples"]
        with self.assertRaisesRegex(ValueError,"minValidationSamples"): model.validate_split_samples(parts,policy)
        with patch.object(model,"validate_provenance",return_value=None):
            with self.assertRaisesRegex(ValueError,"modelVersion"):
                model.validate_training_manifest({"provenance":{"kind":"SYNTHETIC_FIXTURE"},"modelVersion":" "})

    def test_band_edge_and_independent_tiers_cannot_be_filled_by_other_side(self):
        thresholds={"weak":{"minProbability":.55,"minGap":.05},"normal":{"minProbability":.65,"minGap":.1},"strong":{"minProbability":.75,"minGap":.2}}
        rows=[{"labels":{"LONG":1,"SHORT":0},"netReturns":{"LONG":.1,"SHORT":-.1}} for _ in range(6)]
        evidence=model.tier_evidence(rows,[.6,.6,.7,.7,.8,.8],[.1]*6,thresholds)
        self.assertEqual(evidence["LONG_weak"]["count"],2)
        self.assertEqual(evidence["SHORT_weak"]["count"],0)
        self.assertIsNone(evidence["SHORT_weak"]["netEdge"])
        self.assertIsNone(model.tier_name(.8,.8,thresholds))

    def test_duplicate_asof_risk_cache_is_not_independent_history(self):
        observation={"value":2,"source":"COINGLASS","observedAt":"2026-01-01T00:00:00Z","availableAt":"2026-01-01T00:01:00Z"}
        rows=[{"symbol":"BTCUSDT","realInputs":{"fundingRate":observation}}]*100
        policy={"requiredAssets":["BTCUSDT"],"riskVersion":"TEST_RISK","riskMetrics":{"LONG":{"fundingRate":{"unit":"RATE","higherIsWorse":True,"mediumPercentile":.8,"highPercentile":.95,"minSamples":2}}}}
        self.assertEqual(model.risk_distributions(rows,policy),{"BTCUSDT":{"LONG":{}}})
        rows.append({"symbol":"BTCUSDT","realInputs":{"fundingRate":{**observation,"availableAt":"2026-01-01T00:02:00Z"}}})
        self.assertEqual(model.risk_distributions(rows,policy),{"BTCUSDT":{"LONG":{}}})

    def test_risk_distribution_metric_keys_survive_deduplication_and_json_roundtrip(self):
        observations=[]
        for minute in (0,1):
            inputs={key:{"value":value+minute,"source":"SYNTHETIC_FIXTURE",
                         "observedAt":f"2026-01-01T00:0{minute}:00Z",
                         "availableAt":f"2026-01-01T00:0{minute}:01Z"}
                    for key,value in (("spreadBps",2),("depth10Bps",100))}
            observations.append({"symbol":"BTCUSDT","realInputs":inputs})
        policy={"requiredAssets":["BTCUSDT"],"riskVersion":"TEST_RISK","riskMetrics":{"LONG":{
            key:{"unit":unit,"higherIsWorse":adverse,"mediumPercentile":.8,
                 "highPercentile":.95,"minSamples":2}
            for key,unit,adverse in (("spreadBps","BASIS_POINTS",True),("depth10Bps","QUOTE_CURRENCY",False))}}}
        result=model.risk_distributions(observations+[observations[0]],policy)
        self.assertEqual(set(result["BTCUSDT"]["LONG"]),{"spreadBps","depth10Bps"})
        self.assertEqual(result["BTCUSDT"]["LONG"]["spreadBps"]["sortedValues"],[2,3])
        self.assertEqual(result["BTCUSDT"]["LONG"]["depth10Bps"]["sortedValues"],[100,101])
        self.assertEqual(result["BTCUSDT"]["LONG"]["depth10Bps"]["side"],"LONG")
        self.assertEqual(json.loads(json.dumps(result,allow_nan=False)),result)

    def test_population_counts_and_missing_strata_never_release(self):
        policy={"requiredAssets":["BTCUSDT"],"requiredRegimes":["LONG"],"requiredVolatilityStrata":["LOW"]}
        report={"splits":[{"count":40},{"count":35},{"count":30},{"count":30}],"sides":{},"strata":{},"tiers":{},"thresholdSelectionTiers":{},"rangeEvidence":{"count":3}}
        for side in ("LONG","SHORT"):
            report["sides"][side]={"raw":{"count":30},"calibrated":{"count":30}}
            for field,value in (("symbol","BTCUSDT"),("regime","LONG"),("volatility","LOW")):
                report["strata"][side+":"+field+":"+value]={"calibrated":{"count":30}}
            for tier in ("weak","normal","strong"):
                report["tiers"][side+"_"+tier]={"count":2}
                report["thresholdSelectionTiers"][side+"_"+tier]={"count":2}
        model.verify_population_counts(report,policy)
        invalid=copy.deepcopy(report); invalid["sides"]["LONG"]["raw"]["count"]=31
        with self.assertRaisesRegex(ValueError,"TEST_METRIC_POPULATION_MISMATCH"): model.verify_population_counts(invalid,policy)
        invalid=copy.deepcopy(report); invalid["strata"]["LONG:symbol:BTCUSDT"]["calibrated"]["count"]=31
        with self.assertRaisesRegex(ValueError,"STRATUM_POPULATION_MISMATCH"): model.verify_population_counts(invalid,policy)
        invalid=copy.deepcopy(report); invalid["tiers"]["LONG_strong"]["count"]=31
        with self.assertRaisesRegex(ValueError,"TIERS_EXCEED_INDEPENDENT_SPLIT_POPULATION"): model.verify_population_counts(invalid,policy)
        report["strata"].clear()
        self.assertFalse(model.release_pass(report,policy))

    def test_time_block_uncertainty_and_outcome_minima_fail_closed(self):
        rows=[{"signalAsOf":i*14400,"labelEnd":(i+1)*14400,"labels":{"LONG":i%2}} for i in range(12)]
        policy={"timeBlockSeconds":14400,"minTimeBlocks":3,"ciReplicates":40,"ciConfidence":.9,"bootstrapSeed":7,"binCount":2,
                "maxBrierCiWidth":1,"maxEceCiWidth":1,"maxLogLossCiWidth":3,"maxHitRateCiWidth":1,
                "minPositiveSamples":3,"minNegativeSamples":3,"minEffectiveSamples":3}
        labels=[r["labels"]["LONG"] for r in rows]; predictions=[.8 if y else .2 for y in labels]
        intervals=model.time_block_intervals(rows,labels,predictions,.5,policy)
        self.assertTrue(model.uncertainty_pass(intervals,policy))
        self.assertEqual(intervals,model.time_block_intervals(rows,labels,predictions,.5,policy))
        self.assertTrue(model.population_pass(model.population(rows,"LONG"),policy))
        self.assertFalse(model.population_pass({"count":12,"positive":12,"negative":0,"effectiveSamples":12},policy))
        self.assertFalse(model.uncertainty_pass({**intervals,"blockCount":1},policy))
        del intervals["intervals"]["brier"]
        self.assertFalse(model.uncertainty_pass(intervals,policy))

    def test_oi_risk_history_requires_same_side_price_build_and_own_version(self):
        rows=[]
        for i,(oi,price) in enumerate(((3,.1),(4,-.1),(-2,.1),(9,0))):
            rows.append({"symbol":"BTCUSDT","realInputs":{"openInterestChange1h":{"value":oi,"source":"COINGLASS:fixture","observedAt":i,"availableAt":i},"priceReturn1h":{"value":price}}})
        spec={"unit":"PERCENT","higherIsWorse":True,"mediumPercentile":.8,"highPercentile":.95,"minSamples":1}
        policy={"requiredAssets":["BTCUSDT"],"riskVersion":"TEST_RISK_V42","riskMetrics":{s:{"crowdingOpenInterestChange1h":spec} for s in ("LONG","SHORT")}}
        distributions=model.risk_distributions(rows,policy)["BTCUSDT"]
        self.assertEqual(distributions["LONG"]["crowdingOpenInterestChange1h"]["sortedValues"],[3])
        self.assertEqual(distributions["SHORT"]["crowdingOpenInterestChange1h"]["sortedValues"],[4])

    @staticmethod
    def v42_policy_fixture():
        policy={key:2 for key in ("minTrainSamples","minCalibrationSamples","minValidationSamples","minTestSamples","minStratumSamples","minTierSamples","minRangeSamples",
                "minProbabilityBandSamples","minOccupiedBands","minWalkForwardFolds","minPositiveSamples","minNegativeSamples","minTimeBlocks","minRangeTestSamples","minWatchTestSamples")}
        policy.update(version="TEST_POLICY_ONLY",riskVersion="TEST_RISK_ONLY",binCount=4,minEffectiveSamples=2,bootstrapSeed=7,timeBlockSeconds=86400,ciReplicates=40,ciConfidence=.9,
                maxEce=.3,maxLogLoss=2,minProbabilitySpread=.1,roundTripFeeRate=.001,roundTripSlippageRate=.001,maxSignalTradeAgeSeconds=2,
                requiredAssets=["BTCUSDT"],requiredRegimes=["LONG"],requiredVolatilityStrata=["LOW"],volatilityStrata=[.01,.02],
                maxBrierCiWidth=.5,maxEceCiWidth=.5,maxLogLossCiWidth=2,maxHitRateCiWidth=.5,minNonDirectionalCoverage=.1,maxNonDirectionalCoverageSwing=.8,
                maxRangeFalseEntryRate=.3,maxWatchFalseEntryRate=.3,maxFeatureOutlierFraction=.1,driftLowerQuantile=.01,driftUpperQuantile=.99,
                costProvenance={"kind":"REAL_HISTORICAL","source":"TEST_FIXTURE_NOT_REAL_FEES","sourceVersion":"TEST_VERSION","instrument":model.instrument("BTCUSDT"),"unit":"RATE","observedAt":1,"availableAt":1,"expiresAt":9999999999})
        policy["riskMetrics"]={s:{k:{"unit":model.UNITS[k],"higherIsWorse":True,"mediumPercentile":.8,"highPercentile":.95,"minSamples":2} for k in keys} for s,keys in model.RISK_REQUIRED.items()}
        return policy

    def test_release_policy_never_defaults_missing_v42_evidence_or_metric_coverage(self):
        # In-memory schema fixture only: no training invocation or production artifact is emitted.
        policy=self.v42_policy_fixture(); model.validate_policy(policy)
        for key in ("minNegativeSamples","ciConfidence","costProvenance","minEffectiveSamples","riskMetrics"):
            invalid=copy.deepcopy(policy); del invalid[key]
            with self.assertRaises((ValueError,KeyError)): model.validate_policy(invalid)
        invalid=copy.deepcopy(policy); del invalid["riskMetrics"]["SHORT"]["shortLiquidation"]
        with self.assertRaises(ValueError): model.validate_policy(invalid)
        invalid=copy.deepcopy(policy); invalid["costProvenance"]["kind"]="SYNTHETIC_FIXTURE"
        with self.assertRaises(ValueError): model.validate_policy(invalid)

    def test_all_four_splits_require_each_side_outcome_and_missing_pattern_coverage(self):
        policy=self.v42_policy_fixture()
        parts=[[{"signalAsOf":(j*20+i)*14400,"labelEnd":(j*20+i+1)*14400,"symbol":"BTCUSDT","regime":"LONG","volatility":"LOW","vector":[1.0],
                 "labels":{"LONG":i%2,"SHORT":1-i%2}} for i in range(12)] for j in range(4)]
        self.assertEqual(len(model.validate_v42_populations(parts,policy)),4)
        invalid=copy.deepcopy(parts)
        for row in invalid[1]: row["labels"]["SHORT"]=0
        with self.assertRaisesRegex(ValueError,"population"): model.validate_v42_populations(invalid,policy)
        invalid=copy.deepcopy(parts); invalid[3][0]["vector"]=[None]
        with self.assertRaisesRegex(ValueError,"Missing-pattern"): model.validate_v42_populations(invalid,policy)

    def test_range_and_watch_need_their_own_final_test_stable_coverage(self):
        policy=self.v42_policy_fixture()
        rows=[{"signalAsOf":i*14400,"labelEnd":(i+1)*14400,"regime":"RANGE" if i%2==0 else "LONG","labels":{"LONG":0,"SHORT":0}} for i in range(12)]
        thresholds={"weak":{"minProbability":.6,"minGap":.1},"normal":{"minProbability":.7,"minGap":.2},"strong":{"minProbability":.8,"minGap":.3},"rangeMaxGap":.05,"rangeMaxProbability":.5}
        evidence=model.nondirectional_evidence(rows,[.4]*12,[.4]*12,thresholds,policy)
        self.assertTrue(model.nondirectional_pass(evidence,policy))
        self.assertEqual(evidence["RANGE"]["selectionSplit"],"FINAL_TEST_ONLY")
        self.assertEqual(evidence["WATCH"]["count"],6)
        evidence["WATCH"]["coverageSwing"]=1
        self.assertFalse(model.nondirectional_pass(evidence,policy))

    def test_provenance_requires_both_actual_provider_identity_sets(self):
        source={"provider":"BINANCE_SPOT","source":"BINANCE_SPOT_AGG_TRADE","sourceVersion":"TEST","instrument":model.instrument("BTCUSDT"),"unit":"QUOTE_CURRENCY"}
        provenance={"kind":"REAL_HISTORICAL","datasetVersion":"TEST_SCHEMA_ONLY","source":"BINANCE_SPOT","availabilityBasis":"RECORDED_AT_INGESTION","capturedAt":100,"sources":[source]}
        with self.assertRaisesRegex(ValueError,"CoinGlass"): model.validate_provenance(provenance)
        provenance["sources"].append({**source,"provider":"COINGLASS","source":"COINGLASS:COINGLASS_FUNDING:fixture","instrument":model.instrument("BTCUSDT",True),"unit":"RATE"})
        model.validate_provenance(provenance)
        provenance["kind"]="SYNTHETIC_FIXTURE"
        with self.assertRaises(ValueError): model.validate_provenance(provenance)

    def test_ingestion_nanoseconds_survive_json_and_never_become_past_knowledge(self):
        text='{"value":-0.01,"source":"COINGLASS:COINGLASS_FUNDING:fixture","sourceVersion":"TEST","unit":"RATE","instrument":"BINANCE:PERPETUAL:LINEAR:BTC/USDT","observedAt":1767268800.000000001,"availableAt":1767268800.000000002,"expiresAt":1767268801}'
        o=model.decode_json(text)
        self.assertFalse(model.valid_observation("BTCUSDT","fundingRate",o,model.timestamp("2026-01-01T12:00:00.000000001Z")))
        self.assertTrue(model.valid_observation("BTCUSDT","fundingRate",o,model.timestamp("2026-01-01T12:00:00.000000002Z")))
        self.assertEqual(model.instant(o["availableAt"]),"2026-01-01T12:00:00.000000002Z")
        self.assertEqual(model.timestamp(model.instant(o["availableAt"])),o["availableAt"])

    def test_real_xgboost_and_independent_beta_numeric_smoke_test_fixture_only(self):
        if importlib.util.find_spec("xgboost") is None: self.skipTest("Install fixed offline test dependencies for JNI/Python numerical smoke")
        import numpy as np
        import xgboost as xgb
        self.assertEqual(xgb.__version__,model.XGBOOST_VERSION)
        data=np.arange(80,dtype=np.float32).reshape(40,2)
        calibrators={}; raw={}
        for side,cutoff in (("LONG",20),("SHORT",12)):
            labels=np.array([i>=cutoff if side=="LONG" else i<cutoff for i in range(40)],dtype=np.float32)
            booster=xgb.train({"objective":"binary:logistic","device":"cpu","nthread":1,"max_depth":2,"eta":.2,"seed":7},xgb.DMatrix(data,label=labels),num_boost_round=4)
            raw[side]=booster.predict(xgb.DMatrix(data)).tolist()
            self.assertEqual(len(raw[side]),40)
            calibrators[side]=model.fit_beta(raw[side],labels.tolist())
            self.assertTrue(all(0<=model.beta(p,calibrators[side])<=1 for p in raw[side]))
        self.assertNotEqual(raw["LONG"],raw["SHORT"])
        self.assertNotEqual(calibrators["LONG"],calibrators["SHORT"])

    def test_anticorrelated_beta_fit_cannot_silently_publish_collapsed_calibrator(self):
        if importlib.util.find_spec("scipy") is None: self.skipTest("Fixed offline SciPy test dependency required")
        with self.assertRaisesRegex(ValueError,"Invalid probability/independent monotone beta calibrator"):
            model.fit_beta([.1,.2,.8,.9],[1,1,0,0])


class NativeInteropFixtureTests(unittest.TestCase):
    def test_fixture_is_dual_ubj_float32_checksums_and_not_a_production_bundle(self):
        import struct
        with tempfile.TemporaryDirectory(prefix="asset-card-native-unit-") as folder:
            root=pathlib.Path(folder)/"fixture"
            result=generate_native_fixture(root,"a"*40,"b"*64)
            self.assertEqual(result["kind"],"TEST_FIXTURE_ONLY")
            self.assertFalse(result["productionModelReady"])
            self.assertFalse((root/"manifest.json").exists())
            self.assertEqual(result["featureNames"],model.FEATURE_NAMES)
            self.assertEqual(set(p.name for p in root.iterdir()),{"native-fixture.json","long.ubj","short.ubj"})
            self.assertNotEqual(result["models"]["LONG"]["sha256"],result["models"]["SHORT"]["sha256"])
            for side in ("LONG","SHORT"):
                spec=result["models"][side]
                self.assertEqual(model.sha256(root/spec["file"]),spec["sha256"])
                self.assertEqual(len(spec["expectedRaw"]),len(result["float32Rows"]))
                for raw,calibrated in zip(spec["expectedRaw"],spec["expectedCalibrated"]):
                    self.assertEqual(model.beta(raw,spec["calibration"]),calibrated)
            self.assertTrue(all(len(row)==len(model.FEATURE_NAMES) for row in result["float32Rows"]))
            self.assertEqual(struct.unpack("!f",bytes.fromhex(result["float32Rows"][1][0]))[0],19)
            with self.assertRaises(ValueError): generate_native_fixture(root,"a"*40,"b"*64)

    def test_fixture_refuses_unknown_candidate_or_jar_identity_before_writing(self):
        with tempfile.TemporaryDirectory(prefix="asset-card-native-unit-") as folder:
            root=pathlib.Path(folder)/"fixture"
            with self.assertRaises(ValueError): generate_native_fixture(root,"not-a-sha","b"*64)
            self.assertFalse(root.exists())


class CardExportTests(unittest.TestCase):
    """Synthetic files isolate export validation; real provenance rejection is never bypassed outside these tests."""
    @staticmethod
    def fixture():
        at=1767268800; end=at+model.HORIZON
        raw=ModelTests.feature_fixture(at)
        raw["evidence"]["fundingRate"]={**ModelTests.observation("fundingRate",.001,at),
            "source":"COINGLASS:COINGLASS_FUNDING:TEST_FIXTURE","instrument":model.instrument("BTCUSDT",True)}
        raw["evidence"]["spotPrice"]["observationId"]="41"
        future=ModelTests.path_bars(at)
        for bar in future:
            bar.update(availableAt=bar["closeTime"],instrument=model.instrument("BTCUSDT"),
                source="BINANCE_SPOT_CLOSED_5M",sourceVersion="TEST_FIXTURE_V2",unit="OHLCV")
        horizon={**ModelTests.observation("spotPrice",100.2,end-.5),"availableAt":end,"observationId":"83"}
        sources=[]
        for observation in list(raw["evidence"].values())+[future[0],horizon]:
            source={key:observation[key] for key in ("source","sourceVersion","instrument","unit")}
            source["provider"]="COINGLASS" if source["source"].startswith("COINGLASS:") else "BINANCE_SPOT"
            if source not in sources: sources.append(source)
        provenance={"kind":"SYNTHETIC_FIXTURE","datasetVersion":"TEST_EXPORT_ONLY","source":"BINANCE_SPOT",
            "availabilityBasis":"RECORDED_AT_INGESTION","capturedAt":model.instant(end+300),"sources":sources}
        labels={side:{"symbol":"BTCUSDT","side":side,"featureVersion":model.FEATURE_VERSION,
            "labelDefinition":"ATR_FIRST_TOUCH_LONG_1_0.75_SHORT_SYMMETRIC_TIMEOUT_FAIL_1M_AMBIGUITY_EXCLUDED",
            "signalTradeId":41,"instrument":model.instrument("BTCUSDT"),"sourceVersion":"TEST_FIXTURE_V2",
            "signalAsOf":model.instant(at),"maturedAt":model.instant(end+1),"outcome":"TIMEOUT","y":0}
            for side in ("LONG","SHORT")}
        record={"rawFrame":raw,"future5m":future,"future1m":[],"horizonTrade":horizon,
            "labelResults":labels,"provenance":copy.deepcopy(provenance)}
        manifest={"schemaVersion":1,"exportKind":"ASSET_CARD_DB_EXPORT_V1","symbol":"BTCUSDT",
            "featureVersion":model.FEATURE_VERSION,"labelDefinition":labels["LONG"]["labelDefinition"],
            "range":{"fromInclusive":model.instant(at),"toExclusive":model.instant(at+300),"availableAtCutoff":model.instant(end+300)},
            "recordCount":1,"sourceVersions":copy.deepcopy(sources),"provenance":provenance}
        return manifest,record

    @staticmethod
    def write_fixture(directory,manifest,records):
        folder=pathlib.Path(directory); data=folder/"records.jsonl"
        data.write_text("".join(json.dumps(row,sort_keys=True,allow_nan=False)+"\n" for row in records))
        manifest=copy.deepcopy(manifest)
        manifest["files"]=[{"path":"records.jsonl","kind":"RAW_FRAMES","count":len(records),"sha256":model.sha256(data)}]
        path=folder/"manifest.json"; path.write_text(json.dumps(manifest,sort_keys=True,allow_nan=False))
        return path

    def test_export_verification_is_repeatable_read_only_and_never_production_ready(self):
        manifest,record=self.fixture()
        with tempfile.TemporaryDirectory(prefix="asset-card-export-test-") as folder:
            path=self.write_fixture(folder,manifest,[record]); before={p.name:p.read_bytes() for p in pathlib.Path(folder).iterdir()}
            with patch.object(model,"validate_provenance",return_value=None):
                first=model.verify_export(path); second=model.verify_export(path)
            self.assertEqual(first,second); self.assertEqual(first["recordCount"],1)
            self.assertEqual(first["labelOutcomes"],{"LONG":{"TIMEOUT":1},"SHORT":{"TIMEOUT":1}})
            self.assertEqual(first["mode"],"SHADOW"); self.assertFalse(first["productionModelReady"])
            self.assertEqual(first["manifestSha256"],model.sha256(path))
            self.assertEqual(before,{p.name:p.read_bytes() for p in pathlib.Path(folder).iterdir()})
            with self.assertRaises(ValueError): model.verify_export(path)

    def test_export_statistics_are_shadow_only_and_cannot_claim_model_readiness(self):
        manifest,record=self.fixture()
        manifest.update(exclusions={"PENDING_HORIZON":2},modelMode="SHADOW",productionModelReady=False)
        with patch.object(model,"validate_provenance",return_value=None):
            model.validate_card_export(manifest,[record])
            for key,value in (("modelMode","ACTIVE"),("modelMode","CANARY"),("productionModelReady",True),
                    ("productionModelReady",0),("exclusions",{"PENDING":-1}),("exclusions",{"PENDING":True}),
                    ("exclusions",{"":1}),("exclusions",[])):
                with self.subTest(key=key,value=value),self.assertRaises(ValueError):
                    model.validate_card_export({**manifest,key:value},[record])

    def test_export_identity_label_claims_and_private_payloads_fail_closed(self):
        manifest,record=self.fixture()
        mutations=[("symbol","ETHUSDT"),("side","SHORT"),("featureVersion","OTHER"),("labelDefinition","OTHER"),
            ("signalTradeId",42),("instrument",model.instrument("ETHUSDT")),("sourceVersion","OTHER"),
            ("signalAsOf","2026-01-01T12:00:00.000000001Z"),("outcome","TARGET"),("y",1),("y",True)]
        with patch.object(model,"validate_provenance",return_value=None):
            for key,value in mutations:
                invalid=copy.deepcopy(record); invalid["labelResults"]["LONG"][key]=value
                with self.subTest(key=key,value=value),self.assertRaises(ValueError): model.validate_card_export(manifest,[invalid])
            for location,key in (("record","_runtime"),("rawFrame","userId"),("spotPrice","ownerId")):
                invalid=copy.deepcopy(record)
                target=invalid if location=="record" else invalid["rawFrame"] if location=="rawFrame" else invalid["rawFrame"]["evidence"]["spotPrice"]
                target[key]="PRIVATE_FIXTURE_MUST_REJECT"
                with self.subTest(location=location),self.assertRaises(ValueError): model.validate_card_export(manifest,[invalid])

    def test_export_range_counts_source_versions_and_duplicate_signals_are_bound(self):
        manifest,record=self.fixture()
        with patch.object(model,"validate_provenance",return_value=None):
            for key,value in (("symbol","ETHUSDT"),("featureVersion","OTHER"),("labelDefinition","OTHER"),("recordCount",2),("sourceVersions",[])):
                invalid={**manifest,key:value}
                with self.subTest(key=key),self.assertRaises(ValueError): model.validate_card_export(invalid,[record])
            invalid=copy.deepcopy(manifest); invalid["range"]["toExclusive"]=invalid["range"]["fromInclusive"]
            with self.assertRaises(ValueError): model.validate_card_export(invalid,[record])
            with self.assertRaises(ValueError): model.validate_card_export({**manifest,"recordCount":2},[record,copy.deepcopy(record)])
            invalid=copy.deepcopy(record); invalid["provenance"]["datasetVersion"]="OTHER"
            with self.assertRaises(ValueError): model.validate_card_export(manifest,[invalid])

    def test_export_never_backfills_availability_or_invents_horizon_trade(self):
        manifest,record=self.fixture(); at=record["rawFrame"]["signalAsOf"]
        with patch.object(model,"validate_provenance",return_value=None):
            invalid=copy.deepcopy(record); invalid["rawFrame"]["evidence"]["fundingRate"]["availableAt"]=at+1
            with self.assertRaises(ValueError): model.validate_card_export(manifest,[invalid])
            invalid=copy.deepcopy(record); invalid["rawFrame"]["bars"]["5m"][-1]["availableAt"]=at+1
            with self.assertRaises(ValueError): model.validate_card_export(manifest,[invalid])
            invalid=copy.deepcopy(record); invalid["labelResults"]["LONG"]["maturedAt"]=model.instant(at+model.HORIZON-1)
            with self.assertRaises(ValueError): model.validate_card_export(manifest,[invalid])
            for key,value in (("availableAt",at+model.HORIZON+.000000001),("observationId",None),("instrument",model.instrument("ETHUSDT"))):
                invalid=copy.deepcopy(record); invalid["horizonTrade"][key]=value
                if key=="availableAt": invalid["horizonTrade"][key]=model.timestamp(model.instant(at+model.HORIZON))+model.Decimal(".000000001")
                with self.subTest(key=key),self.assertRaises(ValueError): model.validate_card_export(manifest,[invalid])

    def test_export_recomputes_ambiguous_labels_instead_of_accepting_java_success(self):
        manifest,record=self.fixture(); at=record["rawFrame"]["signalAsOf"]
        record["future5m"][0].update(high=104,low=97)
        with patch.object(model,"validate_provenance",return_value=None):
            with self.assertRaises(ValueError): model.validate_card_export(manifest,[record])
            for side in ("LONG","SHORT"): record["labelResults"][side].update(y=None,outcome="AMBIGUOUS")
            model.validate_card_export(manifest,[record])
            minutes=ModelTests.path_bars(at,count=5,step=60)
            for minute in minutes:
                minute.update(availableAt=minute["closeTime"],instrument=model.instrument("BTCUSDT"),
                    source="BINANCE_SPOT_CLOSED_1M",sourceVersion="TEST_FIXTURE_V2",unit="OHLCV")
            minutes[0].update(high=104,low=97); record["future1m"]=minutes
            source={key:minutes[0][key] for key in ("source","sourceVersion","instrument","unit")}
            source["provider"]="BINANCE_SPOT"
            for sources in (record["provenance"]["sources"],manifest["provenance"]["sources"],manifest["sourceVersions"]):
                sources.append(copy.deepcopy(source))
            model.validate_card_export(manifest,[record])

    def test_export_future_bars_cannot_disguise_timeframe_or_malformed_closed_boundaries(self):
        manifest,record=self.fixture()
        with patch.object(model,"validate_provenance",return_value=None):
            for key,value in (("source","BINANCE_SPOT_CLOSED_1M"),("closeTime",record["future5m"][0]["openTime"]+60),
                    ("open",float("nan"))):
                invalid=copy.deepcopy(record); invalid["future5m"][0][key]=value
                # Register the claimed provider, so this tests timeframe consistency rather than only source membership.
                altered=copy.deepcopy(manifest)
                if key=="source":
                    source={name:invalid["future5m"][0][name] for name in ("source","sourceVersion","instrument","unit")}
                    source["provider"]="BINANCE_SPOT"
                    for sources in (invalid["provenance"]["sources"],altered["provenance"]["sources"],altered["sourceVersions"]):
                        sources.append(copy.deepcopy(source))
                if key!="source":
                    for side in ("LONG","SHORT"): invalid["labelResults"][side].update(y=None,outcome="INCOMPLETE_HORIZON")
                with self.subTest(key=key),self.assertRaises(ValueError): model.validate_card_export(altered,[invalid])

    def test_export_labels_cannot_bypass_binding_by_removing_the_export_marker(self):
        manifest,record=self.fixture(); manifest.pop("exportKind")
        with tempfile.TemporaryDirectory(prefix="asset-card-export-test-") as folder,patch.object(model,"validate_provenance",return_value=None):
            path=self.write_fixture(folder,manifest,[record])
            with self.assertRaises(ValueError): model.read_dataset(path)

    def test_prepared_label_availability_keeps_the_fixed_horizon_but_waits_for_partial_bar_maturity(self):
        manifest,record=self.fixture(); at=record["rawFrame"]["signalAsOf"]; signal=model.timestamp(at)+model.Decimal("5.123")
        record["rawFrame"]["signalAsOf"]=model.instant(signal)
        record["rawFrame"]["evidence"]["spotPrice"].update(observedAt=model.instant(signal),availableAt=model.instant(signal))
        record["future5m"].append({**record["future5m"][-1],"openTime":at+model.HORIZON,
            "closeTime":at+model.HORIZON+300,"availableAt":at+model.HORIZON+300})
        record["horizonTrade"].update(observedAt=model.instant(signal+model.HORIZON-model.Decimal(".5")),
            availableAt=model.instant(signal+model.HORIZON),expiresAt=model.instant(signal+model.HORIZON+10))
        for label in record["labelResults"].values():
            label.update(signalAsOf=model.instant(signal),maturedAt=model.instant(at+model.HORIZON+300))
        policy={"maxSignalTradeAgeSeconds":2,"roundTripFeeRate":.001,"roundTripSlippageRate":.001,"volatilityStrata":[.01,.02]}
        with patch.object(model,"validate_provenance",return_value=None):
            model.validate_card_export(manifest,[record])
            rows,excluded=model.prepare_records([record],policy)
        self.assertEqual(excluded,{})
        self.assertEqual(rows[0]["labelEnd"],signal+model.HORIZON)
        self.assertEqual(rows[0]["labelAvailableAt"],model.timestamp(at+model.HORIZON+300))
        self.assertGreater(rows[0]["labelAvailableAt"],rows[0]["labelEnd"])

    def test_every_temporal_fold_excludes_labels_not_yet_available_at_its_boundary(self):
        boundaries={"trainEnd":30*3600,"calibrationEnd":60*3600,"validationEnd":90*3600,"testEnd":120*3600}
        rows=[{"signalAsOf":i*3600,"labelEnd":i*3600+model.HORIZON,"labelAvailableAt":i*3600+model.HORIZON} for i in range(120)]
        delayed=[]
        for end in boundaries.values():
            row=next(row for row in rows if row["labelEnd"]==end)
            row["labelAvailableAt"]=model.timestamp(end)+model.Decimal(".000000001"); delayed.append(row)
        parts=model.temporal_split(rows,boundaries)
        for part,end in zip(parts,boundaries.values()):
            self.assertTrue(all(row["labelAvailableAt"]<=end for row in part))
            self.assertFalse(any(row in part for row in delayed))
        for left,right in zip(parts,parts[1:]):
            self.assertLessEqual(max(row["labelEnd"] for row in left)+model.HORIZON,min(row["signalAsOf"] for row in right))
        for value in (None,0):
            invalid=copy.deepcopy(rows)
            if value is None: invalid[0].pop("labelAvailableAt")
            else: invalid[0]["labelAvailableAt"]=value
            with self.subTest(availability=value),self.assertRaises(ValueError): model.temporal_split(invalid,boundaries)

    def test_export_file_checksums_counts_paths_and_symlinks_are_not_trusted(self):
        manifest,record=self.fixture()
        with tempfile.TemporaryDirectory(prefix="asset-card-export-test-") as folder,patch.object(model,"validate_provenance",return_value=None):
            path=self.write_fixture(folder,manifest,[record]); original=json.loads(path.read_text())
            for key,value in (("sha256","0"*64),("count",2),("path","../records.jsonl")):
                invalid=copy.deepcopy(original); invalid["files"][0][key]=value; path.write_text(json.dumps(invalid))
                with self.subTest(key=key),self.assertRaises(ValueError): model.verify_export(path)
            link=pathlib.Path(folder)/"linked.jsonl"; link.symlink_to(pathlib.Path(folder)/"records.jsonl")
            invalid=copy.deepcopy(original); invalid["files"][0]["path"]="linked.jsonl"; path.write_text(json.dumps(invalid))
            with self.assertRaises(ValueError): model.verify_export(path)

    def test_export_json_rejects_duplicate_identity_keys_and_nonfinite_values(self):
        for value in ('{"symbol":"BTCUSDT","symbol":"ETHUSDT"}', '{"value":NaN}', '{"value":Infinity}'):
            with self.assertRaises(ValueError): model.decode_json(value)

    def test_verify_export_cli_does_not_need_or_invent_training_costs(self):
        manifest,record=self.fixture()
        with tempfile.TemporaryDirectory(prefix="asset-card-export-test-") as folder:
            path=self.write_fixture(folder,manifest,[record]); output=io.StringIO()
            with patch.object(model,"validate_provenance",return_value=None),patch.object(model,"train_bundle") as train,\
                    patch("sys.argv",["asset_card_model.py","verify-export",str(path)]),contextlib.redirect_stdout(output):
                model.main()
            result=json.loads(output.getvalue()); self.assertFalse(result["productionModelReady"]); train.assert_not_called()
            with patch.object(model,"validate_provenance",return_value=None),self.assertRaises((ValueError,KeyError)):
                model.validate_training_manifest(manifest)


if __name__ == "__main__":
    if len(sys.argv)>1 and sys.argv[1]=="--generate-native-fixture":
        if len(sys.argv)!=5:
            print("TEST_FIXTURE_STATUS=FAIL\nCODE=INVALID_ARGUMENTS"); sys.exit(2)
        try:
            generate_native_fixture(sys.argv[2],sys.argv[3],sys.argv[4])
            print("TEST_FIXTURE_STATUS=PASS\nDATA_KIND=TEST_FIXTURE_ONLY\nPRODUCTION_MODEL_READY=NO")
        except Exception:
            print("TEST_FIXTURE_STATUS=FAIL\nCODE=FIXTURE_GENERATION_FAILED\nPRODUCTION_MODEL_READY=NO"); sys.exit(1)
    else: unittest.main()
