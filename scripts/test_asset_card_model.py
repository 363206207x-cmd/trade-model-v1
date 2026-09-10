"""Deterministic synthetic unit fixtures; never production training evidence."""
import importlib.util
import math
import pathlib
import unittest
from unittest.mock import patch
import copy

SPEC = importlib.util.spec_from_file_location("asset_card_model", pathlib.Path(__file__).with_name("asset_card_model.py"))
model = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(model)


class ModelTests(unittest.TestCase):
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
            raw["evidence"][key]={"value":2,"source":"BINANCE_SPOT","observedAt":at,"availableAt":at}
        raw["evidence"]["spotPrice"]={"value":100,"source":"BINANCE_SPOT","observedAt":at,"availableAt":at}
        return raw

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

    def test_timeout_requires_actual_point_in_time_horizon_trade(self):
        at=1767268800; cutoff=at+5.123
        raw=self.feature_fixture(at); raw["signalAsOf"]=cutoff
        raw["evidence"]["spotPrice"].update(observedAt=cutoff,availableAt=cutoff)
        record={"rawFrame":raw,"future5m":self.path_bars(at,49),"future1m":[],
                "provenance":{"kind":"SYNTHETIC_FIXTURE","capturedAt":model.instant(at+15000)}}
        policy={"maxSignalTradeAgeSeconds":2,"roundTripFeeRate":.001,"roundTripSlippageRate":.001,"volatilityStrata":[.01,.02]}
        # Isolate preparation logic only; the real CLI validator rejects this fixture unconditionally.
        with self.assertRaises(ValueError): model.validate_provenance(record["provenance"])
        with patch.object(model,"validate_provenance",return_value=None):
            rows,excluded=model.prepare_records([record],policy)
            self.assertEqual(rows,[]); self.assertEqual(excluded,{"MISSING_POINT_IN_TIME_HORIZON_TRADE":1})
            record["horizonTrade"]={"value":100.2,"source":"BINANCE_SPOT","observedAt":cutoff+14400-.5,"availableAt":cutoff+14400}
            rows,excluded=model.prepare_records([record],policy)
            self.assertEqual(excluded,{})
            self.assertEqual(rows[0]["signalAsOf"],cutoff)
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
        rows=[{"signalAsOf":i*3600,"labelEnd":i*3600+14400} for i in range(120)]
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
        policy={"requiredAssets":["BTCUSDT"],"riskMetrics":{"fundingRate":{"unit":"RATE","higherIsWorse":True,"mediumPercentile":.8,"highPercentile":.95,"minSamples":2}}}
        self.assertEqual(model.risk_distributions(rows,policy),{})
        rows.append({"symbol":"BTCUSDT","realInputs":{"fundingRate":{**observation,"availableAt":"2026-01-01T00:02:00Z"}}})
        self.assertEqual(model.risk_distributions(rows,policy),{})

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

    def test_real_xgboost_and_independent_beta_numeric_smoke_test_fixture_only(self):
        if importlib.util.find_spec("xgboost") is None: self.skipTest("Install fixed offline test dependencies for JNI/Python numerical smoke")
        import numpy as np
        import xgboost as xgb
        self.assertEqual(xgb.__version__,model.XGBOOST_VERSION)
        data=np.arange(80,dtype=np.float32).reshape(40,2)
        labels=np.array([i%3==0 for i in range(40)],dtype=np.float32)
        booster=xgb.train({"objective":"binary:logistic","device":"cpu","nthread":1,"max_depth":2,"eta":.2,"seed":7},xgb.DMatrix(data,label=labels),num_boost_round=4)
        predictions=booster.predict(xgb.DMatrix(data)).tolist()
        self.assertEqual(len(predictions),40)
        long=model.fit_beta(predictions,labels.tolist())
        short=model.fit_beta(predictions,(1-labels).tolist())
        self.assertNotEqual(long,short)
        self.assertTrue(all(0<=model.beta(p,long)<=1 for p in predictions))


if __name__ == "__main__": unittest.main()
