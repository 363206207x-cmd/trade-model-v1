#!/usr/bin/env python3
"""Point-in-time offline dual-XGBoost asset-card pipeline. No network/database access.

CLI: status | inspect MANIFEST | prepare MANIFEST OUTPUT | train MANIFEST OUTPUT_DIR
MANIFEST explicitly identifies REAL_HISTORICAL JSONL raw-frame files by SHA256.
Each row has rawFrame (Java RawFrame JSON), future5m, future1m and provenance.
No prepared feature vectors, current snapshots or synthetic production inputs are accepted.
The manifest also supplies temporal split boundaries, walkForward boundaries,
XGBoost hyperparameters, thresholdCandidates and a frozen releasePolicy. No defaults
for direction thresholds, sample adequacy, transaction costs or risk percentiles exist.
"""
import argparse
import hashlib
import itertools
import json
import math
import pathlib
from datetime import datetime, timezone

FEATURE_VERSION = "SPOT_CARD_FEATURES_V1"
ATR_DEFINITION = "5m_TR_SMA14_FIXED_AT_SIGNAL"
XGBOOST_VERSION = "2.1.4"
INTERVALS = {"5m":300,"15m":900,"1h":3600,"4h":14400}
BAR_FEATURES = ["momentum6","atrRelative","volatility12","volumeRatio","slope12",
                "centerDistanceAtr","rangePosition","takerBuyFraction","tradeCount"]
EVIDENCE_FEATURES = ["spreadBps","depth10Bps","depth25Bps","bookImbalance","openInterest",
                     "fundingRate","longShortRatio","longLiquidation","shortLiquidation","takerBuySellRatio"]
FEATURE_NAMES = [f"{interval}.{feature}" for interval in INTERVALS for feature in BAR_FEATURES] + EVIDENCE_FEATURES
HORIZON = 14400
EPSILON = 1e-7  # numeric endpoint protection, not a strength or release threshold


def timestamp(value):
    if isinstance(value, (int,float)) and math.isfinite(value):
        return float(value)
    if isinstance(value,str):
        result=datetime.fromisoformat(value.replace("Z","+00:00"))
        if result.tzinfo is None: raise ValueError("UTC/offset timestamp required")
        return result.timestamp()
    raise ValueError("Missing real timestamp")


def instant(value):
    return datetime.fromtimestamp(value,timezone.utc).isoformat().replace("+00:00","Z")


def finite(value):
    return isinstance(value,(float,int)) and not isinstance(value,bool) and math.isfinite(value)


def beta(raw, parameters):
    a,b,c,epsilon=(parameters[k] for k in ("a","b","c","epsilon"))
    if not all(finite(v) for v in (raw,a,b,c,epsilon)) or not 0 <= raw <= 1 or a < 0 or b < 0 or a+b == 0 or not 0 < epsilon < .5:
        raise ValueError("Invalid probability/independent monotone beta calibrator")
    p=max(epsilon,min(1-epsilon,raw))
    logit=a*math.log(p)-b*math.log1p(-p)+c
    return 1/(1+math.exp(-logit)) if logit>=0 else math.exp(logit)/(1+math.exp(logit))


def valid_bars(values, interval, as_of):
    if not isinstance(values,list) or len(values)<24: return []
    values=values[-24:]
    previous=None
    for bar in values:
        try:
            opened,closed,available=(timestamp(bar[k]) for k in ("openTime","closeTime","availableAt"))
            if closed>as_of or available>as_of or available<closed or not opened<closed or abs(closed-opened-INTERVALS[interval])>.00101: return []
            if any(not finite(bar.get(k)) for k in ("open","high","low","close","volume")): return []
            if bar["low"]<=0 or bar["high"]<max(bar["open"],bar["close"]) or bar["low"]>min(bar["open"],bar["close"]) or bar["volume"]<0: return []
            if bar.get("takerBuyBaseVolume") is not None and (not finite(bar["takerBuyBaseVolume"]) or not 0<=bar["takerBuyBaseVolume"]<=bar["volume"]): return []
            if bar.get("tradeCount") is not None and (not finite(bar["tradeCount"]) or bar["tradeCount"]<0): return []
            if previous is not None and abs(opened-previous-INTERVALS[interval])>1e-6: return []
            previous=opened
        except (KeyError,TypeError,ValueError): return []
    if as_of-timestamp(values[-1]["closeTime"])>INTERVALS[interval]+15: return []
    return values


def bar_features(bars):
    last=bars[-1]
    atr=sum(max(bars[i]["high"]-bars[i]["low"],abs(bars[i]["high"]-bars[i-1]["close"]),abs(bars[i]["low"]-bars[i-1]["close"])) for i in range(len(bars)-14,len(bars)))/14
    support=min(b["low"] for b in bars[-20:]); resistance=max(b["high"] for b in bars[-20:])
    returns=[math.log(bars[i]["close"]/bars[i-1]["close"]) for i in range(len(bars)-12,len(bars))]
    mean_return=sum(returns)/12; mean_price=sum(b["close"] for b in bars[-12:])/12
    volatility=math.sqrt(sum((r-mean_return)**2 for r in returns)/12)
    slope=sum((i-5.5)*(b["close"]-mean_price) for i,b in enumerate(bars[-12:]))/143
    volume=sum(b["volume"] for b in bars[-21:-1])/20
    features=[last["close"]/bars[-7]["close"]-1,atr/last["close"],volatility,
              last["volume"]/volume if volume>0 else None,slope/atr if atr>0 else None,
              (last["close"]-(support+resistance)/2)/atr if atr>0 else None,
              (last["close"]-support)/(resistance-support) if resistance>support else None,
              last["takerBuyBaseVolume"]/last["volume"] if last["volume"]>0 and last.get("takerBuyBaseVolume") is not None else None,
              last.get("tradeCount")]
    return features,atr,support,resistance


def build_frame(raw):
    as_of=timestamp(raw["signalAsOf"]); symbol=raw["symbol"]
    values={}; vector=[]; reasons=[]; available=[]; observations={}
    for interval in INTERVALS:
        bars=valid_bars(raw.get("bars",{}).get(interval,[]),interval,as_of)
        if not bars:
            vector.extend([None]*len(BAR_FEATURES)); reasons.append("MISSING_OR_INVALID_CLOSED_BARS:"+interval)
        else:
            features,atr,support,resistance=bar_features(bars)
            values[interval]=(bars,features,atr,support,resistance); vector.extend(features)
            available.extend(timestamp(b["availableAt"]) for b in bars)
    for key,observation in raw.get("evidence",{}).items():
        try:
            observed_at=timestamp(observation["observedAt"]); available_at=timestamp(observation["availableAt"])
            if not finite(observation.get("value")) or not observation.get("source") or not observed_at<=available_at<=as_of: raise ValueError()
            observations[key]=observation; available.append(available_at)
        except (KeyError,TypeError,ValueError): reasons.append("FUTURE_OR_INVALID_EVIDENCE:"+key)
    vector.extend(observations[k]["value"] if k in observations else None for k in EVIDENCE_FEATURES)
    for key in ("spreadBps","depth10Bps","depth25Bps"):
        if key not in observations or observations[key]["value"]<0: reasons.append("MISSING_OR_INVALID_CORE_EVIDENCE:"+key)
    five=values.get("5m"); atr=five[2] if five else None
    one="INSUFFICIENT_DATA"; four="INSUFFICIENT_DATA"
    if "4h" in values:
        slope=values["4h"][1][4]
        four="INSUFFICIENT_DATA" if slope is None else "LONG" if slope>0 else "SHORT" if slope<0 else "RANGE"
    if "1h" in values and "4h" in values and values["1h"][1][0] is not None and values["4h"][1][4] is not None:
        alignment=values["1h"][1][0]*values["4h"][1][4]
        one="CONFLICT" if alignment<0 else "OPPORTUNITY" if alignment>0 else "OBSERVATION"
    if five and atr is not None and atr>0:
        derived={"source":"BINANCE_SPOT_CLOSED_5M","observedAt":five[0][-1]["closeTime"],"availableAt":instant(max(timestamp(b["availableAt"]) for b in five[0]))}
        center=(five[3]+five[4])/2; price=five[0][-1]["close"]
        observations["structuralCenterDistanceAtr"]={**derived,"value":abs(price-center)/atr}
        observations["extensionAtr"]={**derived,"value":max(0,price-five[4],five[3]-price)/atr}
        observations["volatility5m"]={**derived,"value":five[1][2]}
    for source_key,target_key in (("fundingRate","absFundingRate"),("bookImbalance","absBookImbalance"),("openInterestChange1h","absOpenInterestChange1h")):
        if source_key in observations: observations[target_key]={**observations[source_key],"value":abs(observations[source_key]["value"])}
    if "longShortRatio" in observations and observations["longShortRatio"]["value"]>0:
        observations["absLogLongShortRatio"]={**observations["longShortRatio"],"value":abs(math.log(observations["longShortRatio"]["value"]))}
    if "longLiquidation" in observations and "shortLiquidation" in observations:
        long,short=observations["longLiquidation"],observations["shortLiquidation"]
        if long["value"]>=0 and short["value"]>=0:
            total=long["value"]+short["value"]
            observations["absLiquidationImbalance"]={"value":abs(long["value"]-short["value"])/total if total>0 else 0,
                "source":long["source"]+"+"+short["source"],"observedAt":instant(max(timestamp(long["observedAt"]),timestamp(short["observedAt"]))),
                "availableAt":instant(max(timestamp(long["availableAt"]),timestamp(short["availableAt"])))}
    conflict_intervals=("5m","1h","4h")
    if all(interval in values and values[interval][1][4] is not None for interval in conflict_intervals):
        slopes=[values[interval][1][4] for interval in conflict_intervals]
        opposite=sum(1 for i in range(3) for j in range(i+1,3) if (slopes[i]<0<slopes[j]) or (slopes[j]<0<slopes[i]))
        observations["timeframeConflict"]={"value":opposite/3,"source":"BINANCE_SPOT_CLOSED_5M_1H_4H",
            "observedAt":instant(max(timestamp(values[interval][0][-1]["closeTime"]) for interval in conflict_intervals)),
            "availableAt":instant(max(timestamp(bar["availableAt"]) for interval in conflict_intervals for bar in values[interval][0]))}
    return {"symbol":symbol,"closed5mAt":five[0][-1]["closeTime"] if five else None,
            "signalAsOf":instant(as_of),"availableAt":instant(max(available)) if available else None,
            "featureVersion":FEATURE_VERSION,"featureNames":FEATURE_NAMES,"vector":vector,
            "ready":len(values)==4 and atr is not None and atr>0 and not any(x.startswith("MISSING_OR_INVALID_CORE_EVIDENCE") for x in reasons),
            "reasons":reasons,"oneHourState":one,"fourHourTrend":four,"atr":atr,
            "structuralSupport":five[3] if five else None,"structuralResistance":five[4] if five else None,
            "realInputs":observations}


def contiguous(bars, start, count, step):
    if len(bars)!=count: return False
    for i,bar in enumerate(bars):
        if any(not finite(bar.get(k)) for k in ("open","high","low","close")): return False
        if bar["low"]<=0 or bar["high"]<max(bar["open"],bar["close"]) or bar["low"]>min(bar["open"],bar["close"]): return False
        if abs(timestamp(bar["openTime"])-(start+i*step))>1e-6 or abs(timestamp(bar["closeTime"])-(start+(i+1)*step))>.00101: return False
    return True


def first_touch(entry, atr, as_of, future5m, future1m, side):
    """First touch in [actual signal cutoff, cutoff+4h), not a rounded bar timestamp.
    Complete surrounding closed 5m coverage is mandatory. Partial boundary bars
    are resolved with closed 1m; a touch inside a partial boundary 1m is ambiguous.
    """
    if side not in ("LONG","SHORT") or not finite(entry) or entry<=0 or not finite(atr) or atr<=0: raise ValueError("Invalid fixed-at-signal barriers")
    start=timestamp(as_of); end=start+HORIZON
    first=math.floor(start/300)*300
    count=math.ceil((end-first)/300)
    bars=[b for b in future5m if first<=timestamp(b["openTime"])<end]
    if not contiguous(bars,first,count,300): return None,"INCOMPLETE_HORIZON"
    target=entry+atr if side=="LONG" else entry-atr
    stop=entry-.75*atr if side=="LONG" else entry+.75*atr
    def touches(bar):
        return (bar["high"]>=target,bar["low"]<=stop) if side=="LONG" else (bar["low"]<=target,bar["high"]>=stop)
    for bar in bars:
        target_hit,stop_hit=touches(bar)
        opened=timestamp(bar["openTime"])
        partial=opened<start or opened+300>end
        if target_hit and stop_hit or partial and (target_hit or stop_hit):
            minutes=[b for b in future1m if opened<=timestamp(b["openTime"])<opened+300]
            if not contiguous(minutes,opened,5,60): return None,"AMBIGUOUS"
            if not math.isclose(max(m["high"] for m in minutes),bar["high"],rel_tol=1e-10) or not math.isclose(min(m["low"] for m in minutes),bar["low"],rel_tol=1e-10): return None,"AMBIGUOUS"
            for minute in minutes:
                minute_start=timestamp(minute["openTime"])
                if minute_start+60<=start or minute_start>=end: continue
                t,s=touches(minute)
                if (minute_start<start or minute_start+60>end) and (t or s): return None,"AMBIGUOUS"
                if t and s: return None,"AMBIGUOUS"
                if t: return 1,"TARGET"
                if s: return 0,"STOP"
            if partial: continue # touches existed only outside the actual 4h interval
            return None,"AMBIGUOUS"
        if target_hit: return 1,"TARGET"
        if stop_hit: return 0,"STOP"
    return 0,"TIMEOUT"


def validate_provenance(provenance):
    if provenance.get("kind")!="REAL_HISTORICAL" or not all(provenance.get(k) for k in ("datasetVersion","source","availabilityBasis","capturedAt")):
        raise ValueError("Only explicit real historical provenance is allowed; samples/metrics otherwise UNKNOWN")
    if provenance["source"]!="BINANCE_SPOT" or provenance["availabilityBasis"]!="RECORDED_AT_INGESTION":
        raise ValueError("Spot lineage and original recorded availability are mandatory; no current snapshot backfill")
    timestamp(provenance["capturedAt"])


def sha256(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def read_dataset(manifest_path):
    path=pathlib.Path(manifest_path).resolve(); manifest=json.loads(path.read_text())
    validate_provenance(manifest["provenance"])
    records=[]
    if not manifest.get("files"): raise ValueError("No explicit historical files; actual sample count UNKNOWN")
    for descriptor in manifest["files"]:
        data_path=(path.parent/descriptor["path"]).resolve()
        if not data_path.is_relative_to(path.parent) or data_path.is_symlink() or descriptor.get("kind")!="RAW_FRAMES": raise ValueError("Only manifest-local raw frame files are accepted")
        if sha256(data_path)!=descriptor["sha256"]: raise ValueError("Historical file checksum mismatch")
        for line in data_path.read_text().splitlines():
            if line.strip():
                record=json.loads(line); validate_provenance(record["provenance"])
                if record["provenance"]["datasetVersion"]!=manifest["provenance"]["datasetVersion"]: raise ValueError("Mixed dataset versions")
                if "vector" in record or "features" in record or "currentSnapshot" in record: raise ValueError("Features must be rebuilt from genuine raw point-in-time inputs")
                records.append(record)
    return manifest,records


def prepare_records(records, policy):
    prepared=[]; excluded={}; seen=set()
    for record in records:
        validate_provenance(record["provenance"])
        frame=build_frame(record["rawFrame"])
        key=(frame["symbol"],frame["signalAsOf"])
        if key in seen: raise ValueError("Duplicate symbol/signal timestamp")
        seen.add(key)
        if not frame["ready"]:
            excluded["INSUFFICIENT_POINT_IN_TIME_FEATURES"]=excluded.get("INSUFFICIENT_POINT_IN_TIME_FEATURES",0)+1; continue
        end=timestamp(frame["signalAsOf"])+HORIZON
        first=math.floor(timestamp(frame["signalAsOf"])/300)*300
        future=[b for b in record.get("future5m",[]) if first<=timestamp(b["openTime"])<end]
        captured_at=timestamp(record["provenance"]["capturedAt"])
        if not future or any(timestamp(b["closeTime"])>captured_at for b in future):
            excluded["UNCLOSED_OR_UNCAPTURED_FUTURE"]=excluded.get("UNCLOSED_OR_UNCAPTURED_FUTURE",0)+1; continue
        if any(timestamp(b["closeTime"])>captured_at for b in record.get("future1m",[])):
            excluded["UNCLOSED_OR_UNCAPTURED_FUTURE"]=excluded.get("UNCLOSED_OR_UNCAPTURED_FUTURE",0)+1; continue
        # An actual Spot trade at the signal cutoff is required. A stale close cannot be an invented entry.
        price=frame["realInputs"].get("spotPrice")
        if price is None or price["source"]!="BINANCE_SPOT" or timestamp(frame["signalAsOf"])-timestamp(price["observedAt"])>policy["maxSignalTradeAgeSeconds"]:
            excluded["MISSING_SIGNAL_TRADE"]=excluded.get("MISSING_SIGNAL_TRADE",0)+1; continue
        labels={}; returns={}; reject=None
        for side in ("LONG","SHORT"):
            label,reason=first_touch(price["value"],frame["atr"],frame["signalAsOf"],record.get("future5m",[]),record.get("future1m",[]),side)
            if label is None: reject=reason; break
            labels[side]=label
            if reason=="TIMEOUT":
                horizon=record.get("horizonTrade")
                try:
                    if horizon is None or horizon.get("source")!="BINANCE_SPOT" or not finite(horizon.get("value")) or horizon["value"]<=0: raise ValueError()
                    observed,available=timestamp(horizon["observedAt"]),timestamp(horizon["availableAt"])
                    if not end-policy["maxSignalTradeAgeSeconds"]<=observed<=available<=end: raise ValueError()
                except (KeyError,TypeError,ValueError): reject="MISSING_POINT_IN_TIME_HORIZON_TRADE"; break
                gross=(horizon["value"]-price["value"])/price["value"]*(1 if side=="LONG" else -1)
            else: gross=frame["atr"]/price["value"] if reason=="TARGET" else -.75*frame["atr"]/price["value"]
            returns[side]=gross-policy["roundTripFeeRate"]-policy["roundTripSlippageRate"]
        if reject: excluded[reject]=excluded.get(reject,0)+1; continue
        vol=frame["vector"][FEATURE_NAMES.index("5m.volatility12")]
        # Volatility strata boundaries are frozen policy values, never fitted on test outcomes.
        cuts=policy["volatilityStrata"]
        bucket="LOW" if vol<cuts[0] else "MEDIUM" if vol<cuts[1] else "HIGH"
        prepared.append({"symbol":frame["symbol"],"signalAsOf":timestamp(frame["signalAsOf"]),
                         "labelEnd":timestamp(frame["signalAsOf"])+HORIZON,"vector":frame["vector"],
                         "labels":labels,"netReturns":returns,"regime":frame["fourHourTrend"],"volatility":bucket,
                         "realInputs":frame["realInputs"],"frame":frame})
    return sorted(prepared,key=lambda r:(r["signalAsOf"],r["symbol"])),excluded


def temporal_split(rows, boundaries):
    ends=[timestamp(boundaries[k]) for k in ("trainEnd","calibrationEnd","validationEnd","testEnd")]
    if any(b<=a for a,b in zip(ends,ends[1:])): raise ValueError("Strict chronological independent split boundaries required")
    parts=[]
    for i,end in enumerate(ends):
        start=-math.inf if i==0 else ends[i-1]+HORIZON
        parts.append([row for row in rows if row["signalAsOf"]>=start and row["labelEnd"]<=end])
    if any(not part for part in parts): raise ValueError("Empty purged/4h-embargo split; no readiness shortcut")
    for left,right in zip(parts,parts[1:]):
        if max(r["labelEnd"] for r in left)+HORIZON>min(r["signalAsOf"] for r in right): raise ValueError("Overlapping label intervals or missing 4h embargo")
    return parts


def metrics(labels, probabilities, base_rate, bin_count):
    if not labels or len(labels)!=len(probabilities) or not 0<=base_rate<=1 or bin_count<2: raise ValueError("Real nonempty validation predictions required")
    if any(y not in (0,1) for y in labels) or any(not finite(p) or not 0<=p<=1 for p in probabilities): raise ValueError("Invalid binary labels/probabilities")
    bins=[]; n=len(labels)
    for i in range(bin_count):
        indices=[j for j,p in enumerate(probabilities) if min(int(p*bin_count),bin_count-1)==i]
        count=len(indices)
        bins.append({"lower":i/bin_count,"upper":(i+1)/bin_count,"count":count,
                     "meanProbability":sum(probabilities[j] for j in indices)/count if count else None,
                     "observedRate":sum(labels[j] for j in indices)/count if count else None})
    return {"count":n,"brier":sum((p-y)**2 for p,y in zip(probabilities,labels))/n,
            "baseBrier":sum((base_rate-y)**2 for y in labels)/n,
            "ece":sum(b["count"]*abs(b["meanProbability"]-b["observedRate"]) for b in bins if b["count"])/n,
            "logLoss":-sum(y*math.log(max(EPSILON,p))+(1-y)*math.log(max(EPSILON,1-p)) for y,p in zip(labels,probabilities))/n,
            "probabilityMin":min(probabilities),"probabilityMax":max(probabilities),"bins":bins}


def tier_name(p,other,thresholds):
    for name in ("strong","normal","weak"):
        t=thresholds[name]
        if p>=t["minProbability"] and p-other>=t["minGap"]: return name
    return None


def tier_evidence(rows,long_prob,short_prob,thresholds):
    evidence={}
    for side,prob,other in (("LONG",long_prob,short_prob),("SHORT",short_prob,long_prob)):
        for tier in ("weak","normal","strong"):
            indices=[i for i in range(len(rows)) if tier_name(prob[i],other[i],thresholds)==tier]
            evidence[side+"_"+tier]={"count":len(indices),"netEdge":sum(rows[i]["netReturns"][side] for i in indices)/len(indices) if indices else None,
                                      "meanProbability":sum(prob[i] for i in indices)/len(indices) if indices else None,
                                      "observedRate":sum(rows[i]["labels"][side] for i in indices)/len(indices) if indices else None}
    return evidence


def choose_thresholds(rows,long_prob,short_prob,candidates,policy):
    best=None; best_score=-math.inf
    for weak,normal,strong in itertools.combinations(sorted(candidates["tiers"],key=lambda x:x["minProbability"]),3):
        if not (0<weak["minProbability"]<normal["minProbability"]<strong["minProbability"]<1 and 0<weak["minGap"]<normal["minGap"]<strong["minGap"]<1): continue
        for range_candidate in candidates["range"]:
            if not 0<=range_candidate["maxGap"]<weak["minGap"] or not 0<range_candidate["maxProbability"]<weak["minProbability"]: continue
            thresholds={"weak":weak,"normal":normal,"strong":strong,"rangeMaxGap":range_candidate["maxGap"],"rangeMaxProbability":range_candidate["maxProbability"]}
            tiers=tier_evidence(rows,long_prob,short_prob,thresholds)
            if any(v["count"]<policy["minTierSamples"] or v["netEdge"] is None or v["netEdge"]<=0 for v in tiers.values()): continue
            range_indices=[i for i in range(len(rows)) if abs(long_prob[i]-short_prob[i])<=thresholds["rangeMaxGap"] and max(long_prob[i],short_prob[i])<=thresholds["rangeMaxProbability"] and rows[i]["regime"]=="RANGE"]
            if len(range_indices)<policy["minRangeSamples"]: continue
            score=sum(v["netEdge"]*v["count"] for v in tiers.values())+sum(1 for i in range_indices if rows[i]["labels"]["LONG"]==0 and rows[i]["labels"]["SHORT"]==0)/len(range_indices)
            if score>best_score: best=(thresholds,tiers,{"count":len(range_indices),"selectionSplit":"VALIDATION_ONLY"}); best_score=score
    if best is None: raise ValueError("No validation-derived strictly ordered thresholds have independent six-tier positive after-cost edge")
    return best


def fit_beta(raw,labels):
    import numpy as np
    from scipy.optimize import minimize
    p=np.clip(np.asarray(raw),EPSILON,1-EPSILON); y=np.asarray(labels)
    if len(set(labels))!=2: raise ValueError("Calibration split must contain both outcomes")
    x=np.column_stack((np.log(p),-np.log1p(-p),np.ones(len(p))))
    def loss(params):
        z=x@params
        return float(np.mean(np.logaddexp(0,z)-y*z))
    result=minimize(loss,[1,1,0],method="L-BFGS-B",bounds=[(0,None),(0,None),(None,None)])
    if not result.success: raise ValueError("Independent beta fit did not converge")
    params={"a":float(result.x[0]),"b":float(result.x[1]),"c":float(result.x[2]),"epsilon":EPSILON}
    beta(.5,params)
    return params


def evaluate(rows,raw,calibrated,base_rates,thresholds,policy):
    report={"sides":{},"strata":{},"tiers":tier_evidence(rows,calibrated["LONG"],calibrated["SHORT"],thresholds)}
    for side in ("LONG","SHORT"):
        labels=[r["labels"][side] for r in rows]
        report["sides"][side]={"raw":metrics(labels,raw[side],base_rates[side],policy["binCount"]),
                              "calibrated":metrics(labels,calibrated[side],base_rates[side],policy["binCount"])}
        for field,expected in (("symbol",policy["requiredAssets"]),("regime",policy["requiredRegimes"]),("volatility",policy["requiredVolatilityStrata"])):
            for value in expected:
                indices=[i for i,r in enumerate(rows) if r[field]==value]
                key=side+":"+field+":"+value
                report["strata"][key]={"count":0} if not indices else {
                    "raw":metrics([labels[i] for i in indices],[raw[side][i] for i in indices],base_rates[side],policy["binCount"]),
                    "calibrated":metrics([labels[i] for i in indices],[calibrated[side][i] for i in indices],base_rates[side],policy["binCount"])}
    return report


def check_metric_pair(pair,minimum,policy):
    if "raw" not in pair or "calibrated" not in pair: return False
    raw,cal=pair["raw"],pair["calibrated"]
    occupied=[b for b in cal["bins"] if b["count"]>=policy["minProbabilityBandSamples"]]
    return cal["count"]>=minimum and cal["brier"]<raw["brier"] and cal["brier"]<cal["baseBrier"] and cal["ece"]<=policy["maxEce"] and cal["logLoss"]<=policy["maxLogLoss"] and len(occupied)>=policy["minOccupiedBands"] and cal["probabilityMax"]-cal["probabilityMin"]>=policy["minProbabilitySpread"]


def release_pass(report,policy):
    try:
        verify_population_counts(report,policy)
        expected_strata={side+":"+field+":"+value for side in ("LONG","SHORT") for field,key in
                         (("symbol","requiredAssets"),("regime","requiredRegimes"),("volatility","requiredVolatilityStrata")) for value in policy[key]}
        expected_tiers={side+"_"+tier for side in ("LONG","SHORT") for tier in ("weak","normal","strong")}
        return set(report["sides"])=={"LONG","SHORT"} and set(report["strata"])==expected_strata and set(report["tiers"])==expected_tiers and all(check_metric_pair(v,policy["minTestSamples"],policy) for v in report["sides"].values()) and all(check_metric_pair(v,policy["minStratumSamples"],policy) for v in report["strata"].values()) and all(v["count"]>=policy["minTierSamples"] and finite(v["netEdge"]) and v["netEdge"]>0 for v in report["tiers"].values())
    except (KeyError,TypeError,ValueError): return False


def verify_population_counts(report,policy):
    if len(report.get("splits",[]))!=4: raise ValueError("Four independent populations required")
    test_count=report["splits"][3]["count"]; validation_count=report["splits"][2]["count"]
    if type(test_count) is not int or test_count<=0 or type(validation_count) is not int or validation_count<=0: raise ValueError("Unknown independent population counts")
    for side in ("LONG","SHORT"):
        if report["sides"][side]["raw"]["count"]!=test_count or report["sides"][side]["calibrated"]["count"]!=test_count:
            raise ValueError("TEST_METRIC_POPULATION_MISMATCH")
        for field,key in (("symbol","requiredAssets"),("regime","requiredRegimes"),("volatility","requiredVolatilityStrata")):
            if sum(report["strata"][side+":"+field+":"+value]["calibrated"]["count"] for value in policy[key])!=test_count:
                raise ValueError("STRATUM_POPULATION_MISMATCH")
    tiers=[side+"_"+tier for side in ("LONG","SHORT") for tier in ("weak","normal","strong")]
    if sum(report["tiers"][key]["count"] for key in tiers)>test_count or sum(report["thresholdSelectionTiers"][key]["count"] for key in tiers)+report["rangeEvidence"]["count"]>validation_count:
        raise ValueError("TIERS_EXCEED_INDEPENDENT_SPLIT_POPULATION")


def validate_policy(policy):
    for k in ("minTrainSamples","minCalibrationSamples","minValidationSamples","minTestSamples","minStratumSamples","minTierSamples","minRangeSamples","minProbabilityBandSamples","minOccupiedBands","binCount","minWalkForwardFolds"):
        if type(policy.get(k)) is not int or policy[k]<=0: raise ValueError("Explicit frozen positive sample policy required: "+k)
    if policy["binCount"]<2 or not 2<=policy["minOccupiedBands"]<=policy["binCount"]: raise ValueError("Non-collapsed calibration curve policy required")
    for k in ("maxEce","maxLogLoss","minProbabilitySpread","roundTripFeeRate","roundTripSlippageRate","maxSignalTradeAgeSeconds"):
        if not finite(policy.get(k)) or policy[k]<=0: raise ValueError("Explicit positive metric/cost policy required: "+k)
    for k in ("requiredAssets","requiredRegimes","requiredVolatilityStrata"):
        if not isinstance(policy.get(k),list) or not policy[k] or len(set(policy[k]))!=len(policy[k]): raise ValueError("Explicit coverage policy required: "+k)
    if not policy.get("version") or len(policy.get("volatilityStrata",[]))!=2 or not 0<policy["volatilityStrata"][0]<policy["volatilityStrata"][1]: raise ValueError("Versioned frozen volatility strata required")
    if policy["minWalkForwardFolds"]<2: raise ValueError("Multiple temporal walk-forward folds required")
    if not isinstance(policy.get("riskMetrics"),dict): raise ValueError("Explicit risk distribution definitions required (empty means UNKNOWN)")
    for spec in policy["riskMetrics"].values():
        if not spec.get("unit") or not isinstance(spec.get("higherIsWorse"),bool) or not 0<spec["mediumPercentile"]<spec["highPercentile"]<1 or type(spec.get("minSamples")) is not int or spec["minSamples"]<1: raise ValueError("Invalid asset-history risk distribution policy")


def risk_distributions(train,policy):
    result={}
    for asset in policy["requiredAssets"]:
        entries={}
        for key,spec in policy["riskMetrics"].items():
            observations=[r["realInputs"][key] for r in train if r["symbol"]==asset and key in r["realInputs"]]
            # Repeated cached snapshots are one observation, not inflated independent samples.
            unique={}
            for observation in observations:
                key=(observation["source"],timestamp(observation["observedAt"]))
                existing=unique.get(key)
                if existing is not None and existing["value"]!=observation["value"]:
                    raise ValueError("Conflicting same-source historical risk observation")
                if existing is None or timestamp(observation["availableAt"])<timestamp(existing["availableAt"]): unique[key]=observation
            values=sorted(o["value"] for o in unique.values())
            if len(values)<spec["minSamples"]: continue
            entries[key]={**spec,"sortedValues":values,"samples":len(values),"source":"REAL_HISTORICAL_TRAIN_ONLY",
                          "asOf":instant(max(timestamp(o["availableAt"]) for o in unique.values()))}
        if entries: result[asset]=entries
    return result


def train_fold(parts,manifest):
    validate_split_samples(parts,manifest["releasePolicy"])
    import numpy as np
    import xgboost as xgb
    if xgb.__version__!=XGBOOST_VERSION: raise ValueError("Python/JVM XGBoost version mismatch")
    train,calibration,validation,test=parts; policy=manifest["releasePolicy"]
    params=dict(manifest["xgboostParams"])
    if params.get("objective")!="binary:logistic" or params.get("device")!="cpu" or any(k in params for k in ("early_stopping_rounds","evals")): raise ValueError("Fixed CPU binary objective without test early stopping required")
    rounds=manifest["numBoostRound"]
    if type(rounds) is not int or rounds<=0: raise ValueError("Explicit offline-frozen boosting rounds required")
    def matrix(rows,label=None):
        return xgb.DMatrix(np.asarray([[float("nan") if v is None else v for v in r["vector"]] for r in rows],dtype=np.float32),
                           label=label,feature_names=FEATURE_NAMES,missing=np.nan)
    models={}; calibrators={}; bases={}; raw_validation={}; calibrated_validation={}; raw_test={}; calibrated_test={}
    for side in ("LONG","SHORT"):
        labels=[r["labels"][side] for r in train]
        if len(set(labels))!=2: raise ValueError("Training split lacks both outcomes")
        bases[side]=sum(labels)/len(labels)
        models[side]=xgb.train(params,matrix(train,labels),num_boost_round=rounds)
        calibrators[side]=fit_beta(models[side].predict(matrix(calibration)).tolist(),[r["labels"][side] for r in calibration])
        raw_validation[side]=models[side].predict(matrix(validation)).tolist()
        calibrated_validation[side]=[beta(p,calibrators[side]) for p in raw_validation[side]]
    thresholds,selection_tiers,range_evidence=choose_thresholds(validation,calibrated_validation["LONG"],calibrated_validation["SHORT"],manifest["thresholdCandidates"],policy)
    # Final test is touched only after model, calibrators and thresholds are frozen.
    for side in ("LONG","SHORT"):
        raw_test[side]=models[side].predict(matrix(test)).tolist()
        calibrated_test[side]=[beta(p,calibrators[side]) for p in raw_test[side]]
    report=evaluate(test,raw_test,calibrated_test,bases,thresholds,policy)
    report["thresholdSelectionTiers"]=selection_tiers; report["rangeEvidence"]=range_evidence
    report["thresholds"]=thresholds; report["calibrators"]=calibrators
    report["splits"]=[{"name":name,"count":len(rows),"start":instant(min(r["signalAsOf"] for r in rows)),
                       "end":instant(max(r["signalAsOf"] for r in rows)),"labelEnd":instant(max(r["labelEnd"] for r in rows))}
                      for name,rows in zip(("TRAIN","CALIBRATION","VALIDATION","TEST"),parts)]
    report["embargoSeconds"]=HORIZON
    report["passed"]=release_pass(report,policy)
    return models,calibrators,thresholds,report,risk_distributions(train,policy)


def validate_split_samples(parts,policy):
    if len(parts)!=4: raise ValueError("Four independent splits required")
    for rows,key in zip(parts,("minTrainSamples","minCalibrationSamples","minValidationSamples","minTestSamples")):
        if type(policy.get(key)) is not int or policy[key]<=0: raise ValueError("Explicit split sample policy required: "+key)
        if len(rows)<policy[key]: raise ValueError("Insufficient genuine independent split samples: "+key)


def write_json(path,value):
    path.write_text(json.dumps(value,sort_keys=True,separators=(",",":"),allow_nan=False)+"\n")


def train_bundle(manifest,rows,excluded,output):
    validate_training_manifest(manifest)
    policy=manifest["releasePolicy"]; validate_policy(policy)
    if any(row["symbol"] not in policy["requiredAssets"] or row["regime"] not in policy["requiredRegimes"] or row["volatility"] not in policy["requiredVolatilityStrata"] for row in rows):
        raise ValueError("Every actual asset/regime/volatility stratum must be covered by the frozen release policy")
    folds=manifest["walkForward"]
    if len(folds)<policy["minWalkForwardFolds"]: raise ValueError("Insufficient explicit temporal walk-forward folds")
    previous_test_end=-math.inf; reports=[]
    for bounds in folds:
        parts=temporal_split(rows,bounds)
        if min(r["signalAsOf"] for r in parts[-1])<=previous_test_end: raise ValueError("Walk-forward final test folds overlap")
        previous_test_end=max(r["labelEnd"] for r in parts[-1])
        models,calibrators,thresholds,report,distributions=train_fold(parts,manifest)
        reports.append(report)
    out=pathlib.Path(output)
    if out.exists(): raise ValueError("Atomic bundle output must be a new directory; refusing overwrite")
    out.mkdir(parents=True)
    final_report={"dataKind":"REAL_HISTORICAL","datasetVersion":manifest["provenance"]["datasetVersion"],
                  "actualInputSamples":len(rows)+sum(excluded.values()),"actualUsableSamples":len(rows),"excluded":excluded,
                  "folds":reports,"final":reports[-1],"productionModelReady":all(r["passed"] for r in reports),
                  "provenance":manifest["provenance"],"files":manifest["files"]}
    write_json(out/"validation.json",final_report)
    if not final_report["productionModelReady"]:
        write_json(out/"shadow.json",{"mode":"SHADOW","reason":"REAL_VALIDATION_GATES_FAILED","productionModelReady":False})
        return final_report
    for side in ("LONG","SHORT"):
        models[side].set_attr(asset_card_side=side,asset_card_model_version=manifest["modelVersion"],
                             asset_card_feature_version=FEATURE_VERSION,asset_card_calibration_version=manifest["calibrationVersion"],
                             asset_card_threshold_version=manifest["thresholdVersion"],asset_card_data_kind="REAL_HISTORICAL",
                             asset_card_dataset_version=manifest["provenance"]["datasetVersion"])
        models[side].save_model(out/(side.lower()+".ubj"))
    write_json(out/"calibration.json",calibrators)
    write_json(out/"thresholds.json",thresholds)
    write_json(out/"risk-distributions.json",distributions)
    bundle={"schemaVersion":1,"dataKind":"REAL_HISTORICAL","featureVersion":FEATURE_VERSION,"atrDefinition":ATR_DEFINITION,
            "featureNames":FEATURE_NAMES,"modelVersion":manifest["modelVersion"],"calibrationVersion":manifest["calibrationVersion"],
            "thresholdVersion":manifest["thresholdVersion"],"xgboostVersion":XGBOOST_VERSION,"horizonSeconds":HORIZON,
            "labelDefinition":"ATR_FIRST_TOUCH_LONG_1_0.75_SHORT_SYMMETRIC_TIMEOUT_FAIL_1M_AMBIGUITY_EXCLUDED",
            "releasePolicy":policy,"files":{name:sha256(out/name) for name in ("long.ubj","short.ubj","calibration.json","thresholds.json","risk-distributions.json","validation.json")}}
    write_json(out/"manifest.json",bundle) # manifest is the atomic publication marker, written last
    return {"productionModelReady":True,"manifestSha256":sha256(out/"manifest.json"),"report":final_report}


def validate_training_manifest(manifest):
    validate_provenance(manifest["provenance"])
    for key in ("modelVersion","calibrationVersion","thresholdVersion"):
        if not isinstance(manifest.get(key),str) or not manifest[key].strip(): raise ValueError("Explicit nonblank atomic model version required: "+key)
    validate_policy(manifest["releasePolicy"])
    if not isinstance(manifest.get("thresholdCandidates"),dict) or not manifest["thresholdCandidates"].get("tiers") or not manifest["thresholdCandidates"].get("range"):
        raise ValueError("Explicit validation-only threshold candidates required")


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command",choices=("status","inspect","prepare","train"))
    parser.add_argument("manifest",nargs="?"); parser.add_argument("output",nargs="?")
    args=parser.parse_args()
    if args.command=="status":
        print(json.dumps({"mode":"SHADOW","productionModelReady":False,"actualSamples":"UNKNOWN","metrics":"UNKNOWN"})); return
    if not args.manifest: parser.error("An explicit real historical manifest is required")
    manifest,records=read_dataset(args.manifest)
    if args.command=="inspect":
        print(json.dumps({"dataKind":"REAL_HISTORICAL","rawRecords":len(records),"symbols":sorted({r["rawFrame"]["symbol"] for r in records}),
                          "availableCoverage":"UNVALIDATED_UNTIL_PREPARE","productionModelReady":False},sort_keys=True)); return
    validate_training_manifest(manifest)
    rows,excluded=prepare_records(records,manifest["releasePolicy"])
    if not args.output: parser.error("Explicit new output path required")
    if args.command=="prepare":
        path=pathlib.Path(args.output)
        if path.exists(): raise ValueError("Refusing to overwrite prepared history")
        write_json(path,{"featureVersion":FEATURE_VERSION,"rows":rows,"excluded":excluded,"productionModelReady":False})
    else: print(json.dumps(train_bundle(manifest,rows,excluded,args.output),sort_keys=True,allow_nan=False))


if __name__ == "__main__": main()
