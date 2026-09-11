#!/usr/bin/env python3
"""Point-in-time offline dual-XGBoost asset-card pipeline. No network/database access.

CLI: status | verify-export MANIFEST | inspect MANIFEST | prepare MANIFEST OUTPUT | train MANIFEST OUTPUT_DIR
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
import random
import bisect
import re
from decimal import Decimal
from datetime import datetime, timezone

FEATURE_VERSION = "SPOT_CARD_FEATURES_V2_SIGNED_PIT"
SPOT_SOURCE_VERSION = "BINANCE_SPOT_PUBLIC_V1"
ATR_DEFINITION = "5m_TR_SMA14_FIXED_AT_SIGNAL"
XGBOOST_VERSION = "2.1.4"
INTERVALS = {"5m":300,"15m":900,"1h":3600,"4h":14400}
BAR_FEATURES = ["momentum6","atrRelative","volatility12","volumeRatio","slope12",
                "centerDistanceAtr","rangePosition","takerBuyFraction","tradeCount"]
EVIDENCE_FEATURES = ["spreadBps","depth10Bps","depth25Bps","bookImbalance","openInterest",
                     "fundingRate","longShortRatio","longLiquidation","shortLiquidation"]
FEATURE_NAMES = [f"{interval}.{feature}" for interval in INTERVALS for feature in BAR_FEATURES] + EVIDENCE_FEATURES
HORIZON = 14400
LABEL_DEFINITION = "ATR_FIRST_TOUCH_LONG_1_0.75_SHORT_SYMMETRIC_TIMEOUT_FAIL_1M_AMBIGUITY_EXCLUDED"
EXPORT_KIND = "ASSET_CARD_DB_EXPORT_V1"
EPSILON = 1e-7  # numeric endpoint protection, not a strength or release threshold
UNITS={**dict.fromkeys(("spotPrice","openInterest","longLiquidation","shortLiquidation","depth10Bps","depth25Bps"),"QUOTE_CURRENCY"),
       "fundingRate":"RATE","openInterestChange1h":"PERCENT","crowdingOpenInterestChange1h":"PERCENT",
       **dict.fromkeys(("longShortRatio","bookImbalance","liquidationImbalance","timeframeConflict"),"RATIO"),
       "logLongShortRatio":"LOG_RATIO","spreadBps":"BASIS_POINTS",
       **dict.fromkeys(("volatility1m","volatility5m"),"LOG_RETURN_STD"),
       **dict.fromkeys(("return1m","return5m","priceReturn1h"),"LOG_RETURN"),
       **dict.fromkeys(("structuralCenterDistanceAtr","extensionAtr","slope5m","slope1h","slope4h"),"ATR_MULTIPLE")}
DERIVATIVES={"openInterest","fundingRate","longShortRatio","longLiquidation","shortLiquidation","openInterestChange1h","logLongShortRatio","liquidationImbalance","crowdingOpenInterestChange1h"}
_DIRECTIONAL_RISK={"structuralCenterDistanceAtr","extensionAtr","return1m","return5m","volatility1m","volatility5m",
                   "slope5m","slope1h","slope4h","fundingRate","logLongShortRatio","crowdingOpenInterestChange1h",
                   "liquidationImbalance","spreadBps","depth10Bps","depth25Bps","bookImbalance"}
RISK_REQUIRED={"LONG":_DIRECTIONAL_RISK|{"longLiquidation"},"SHORT":_DIRECTIONAL_RISK|{"shortLiquidation"},
               "NON_DIRECTIONAL":{"volatility1m","volatility5m","spreadBps","depth10Bps","depth25Bps"}}


def instrument(symbol,derivative=False):
    import re
    if not isinstance(symbol,str) or not re.fullmatch(r"[A-Z0-9]{2,15}USDT",symbol): return None
    return "BINANCE:"+("PERPETUAL:LINEAR:" if derivative else "SPOT:NONE:")+symbol[:-4]+"/USDT"


def valid_observation(symbol,key,o,at):
    try:
        if not isinstance(o,dict) or not finite(o.get("value")) or not isinstance(o.get("source"),str): return False
        if not isinstance(o.get("sourceVersion"),str) or not o["sourceVersion"].strip() or o["sourceVersion"] in ("UNKNOWN","UNVERIFIED"): return False
        if key not in UNITS or o.get("unit")!=UNITS[key] or instrument(symbol,key in DERIVATIVES) is None or o.get("instrument")!=instrument(symbol,key in DERIVATIVES): return False
        if key in DERIVATIVES:
            dataset="COINGLASS_FUNDING" if key=="fundingRate" else "COINGLASS_LONG_SHORT_RATIO" if key in ("longShortRatio","logLongShortRatio") else "COINGLASS_LIQUIDATION" if key in ("longLiquidation","shortLiquidation","liquidationImbalance") else "COINGLASS_OPEN_INTEREST"
            if not o["source"].startswith("COINGLASS:"+dataset+":"): return False
        elif o["source"] not in ("BINANCE_SPOT","BINANCE_SPOT_AGG_TRADE","BINANCE_SPOT_TRADE","BINANCE_SPOT_DIFF_DEPTH","BINANCE_SPOT_STRUCTURE_AND_TRADE","BINANCE_SPOT_CLOSED_1M","BINANCE_SPOT_CLOSED_5M","BINANCE_SPOT_CLOSED_15M","BINANCE_SPOT_CLOSED_1H","BINANCE_SPOT_CLOSED_4H","BINANCE_SPOT_CLOSED_5M_1H_4H"): return False
        observed,available,expiry=(timestamp(o[k]) for k in ("observedAt","availableAt","expiresAt"))
        if not observed<=available<=at<=expiry: return False
        if key=="spotPrice": return o["value"]>0 and isinstance(o.get("observationId"),str) and bool(o["observationId"].strip())
        if key in ("spreadBps","depth10Bps","depth25Bps","openInterest","longLiquidation","shortLiquidation","volatility1m","volatility5m"): return o["value"]>=0
        if key in ("bookImbalance","liquidationImbalance"): return -1<=o["value"]<=1
        if key=="timeframeConflict": return 0<=o["value"]<=1
        return key!="longShortRatio" or o["value"]>0
    except (KeyError,TypeError,ValueError): return False


def timestamp(value):
    if isinstance(value, (int,float,Decimal)) and not isinstance(value,bool) and math.isfinite(value):
        result=Decimal(str(value))
        if result*1_000_000_000!=(result*1_000_000_000).to_integral_value(): raise ValueError("Nanosecond timestamp precision exceeded")
        return result
    if isinstance(value,str):
        result=datetime.fromisoformat(value.replace("Z","+00:00"))
        if result.tzinfo is None: raise ValueError("UTC/offset timestamp required")
        fraction=re.search(r"\.(\d+)(?:Z|[+-]\d\d:\d\d)$",value)
        if fraction and len(fraction.group(1))>9: raise ValueError("Nanosecond timestamp precision exceeded")
        return Decimal(int(result.replace(microsecond=0).timestamp()))+(Decimal("0."+fraction.group(1)) if fraction else Decimal(0))
    raise ValueError("Missing real timestamp")


def instant(value):
    value=timestamp(value); seconds=math.floor(value); nanos=int((value-seconds)*1_000_000_000)
    return datetime.fromtimestamp(seconds,timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")+("."+f"{nanos:09d}".rstrip("0") if nanos else "")+"Z"


def decode_json(text):
    times={"at","asOf","openTime","closeTime","observedAt","availableAt","expiresAt","signalAsOf","capturedAt","trainEnd","calibrationEnd","validationEnd","testEnd","trainedThrough","validatedThrough","validUntil","maturedAt","fromInclusive","toExclusive","availableAtCutoff","closed5mAt","labelEnd","labelAvailableAt"}
    def unique_object(pairs):
        result={}
        for key,value in pairs:
            if key in result: raise ValueError("Duplicate JSON identity key")
            result[key]=value
        return result
    def reject_constant(value): raise ValueError("Nonfinite JSON values are forbidden")
    def normalize(value,key=None):
        if isinstance(value,dict): return {k:normalize(v,k) for k,v in value.items()}
        if isinstance(value,list): return [normalize(v) for v in value]
        return value if not isinstance(value,Decimal) or key in times else float(value)
    return normalize(json.loads(text,parse_float=Decimal,object_pairs_hook=unique_object,parse_constant=reject_constant))


def json_default(value):
    if isinstance(value,Decimal): return instant(value)
    raise TypeError("Unsupported artifact value: "+type(value).__name__)


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
            if closed>as_of or available>as_of or available<closed or not opened<closed or abs(closed-opened-INTERVALS[interval])>Decimal(".001"): return []
            if any(not finite(bar.get(k)) for k in ("open","high","low","close","volume")): return []
            if bar["low"]<=0 or bar["high"]<max(bar["open"],bar["close"]) or bar["low"]>min(bar["open"],bar["close"]) or bar["volume"]<0: return []
            if bar.get("takerBuyBaseVolume") is not None and (not finite(bar["takerBuyBaseVolume"]) or not 0<=bar["takerBuyBaseVolume"]<=bar["volume"]): return []
            if bar.get("tradeCount") is not None and (type(bar["tradeCount"]) is not int or bar["tradeCount"]<0): return []
            if previous is not None and opened!=previous+INTERVALS[interval]: return []
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
            if not valid_observation(symbol,key,observation,as_of): raise ValueError()
            observations[key]={**observation,**{k:instant(timestamp(observation[k])) for k in ("observedAt","availableAt","expiresAt")}}; available.append(available_at)
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
        derived={"source":"BINANCE_SPOT_CLOSED_5M","observedAt":instant(timestamp(five[0][-1]["closeTime"])),"availableAt":instant(max(timestamp(b["availableAt"]) for b in five[0])),
                 "instrument":instrument(symbol),"sourceVersion":SPOT_SOURCE_VERSION,"expiresAt":instant(timestamp(five[0][-1]["closeTime"])+315),"observationId":None}
        center=(five[3]+five[4])/2; price=five[0][-1]["close"]
        observations["structuralCenterDistanceAtr"]={**derived,"value":(price-center)/atr,"unit":"ATR_MULTIPLE"}
        observations["extensionAtr"]={**derived,"value":(price-five[4] if price>five[4] else price-five[3] if price<five[3] else 0)/atr,"unit":"ATR_MULTIPLE"}
        observations["volatility5m"]={**derived,"value":five[1][2],"unit":"LOG_RETURN_STD"}
        observations["return5m"]={**derived,"value":math.log(price/five[0][-2]["close"]),"unit":"LOG_RETURN"}
    if "longShortRatio" in observations and observations["longShortRatio"]["value"]>0:
        observations["logLongShortRatio"]={**observations["longShortRatio"],"value":math.log(observations["longShortRatio"]["value"]),"unit":"LOG_RATIO"}
    if "longLiquidation" in observations and "shortLiquidation" in observations:
        long,short=observations["longLiquidation"],observations["shortLiquidation"]
        if long["value"]>=0 and short["value"]>=0 and long["instrument"]==short["instrument"] and long["sourceVersion"]==short["sourceVersion"]:
            total=long["value"]+short["value"]
            observations["liquidationImbalance"]={"value":(long["value"]-short["value"])/total if total>0 else 0,
                "source":long["source"]+"+"+short["source"],"observedAt":instant(max(timestamp(long["observedAt"]),timestamp(short["observedAt"]))),
                "availableAt":instant(max(timestamp(long["availableAt"]),timestamp(short["availableAt"]))),"instrument":long["instrument"],
                "sourceVersion":long["sourceVersion"],"unit":"RATIO","expiresAt":instant(min(timestamp(long["expiresAt"]),timestamp(short["expiresAt"]))),"observationId":None}
    for interval in ("5m","1h","4h"):
        if interval in values and values[interval][1][4] is not None:
            bars=values[interval][0]; closed=instant(timestamp(bars[-1]["closeTime"]))
            derived={"source":"BINANCE_SPOT_CLOSED_"+interval.upper(),"instrument":instrument(symbol),"sourceVersion":SPOT_SOURCE_VERSION,
                     "observedAt":closed,"availableAt":instant(max(timestamp(b["availableAt"]) for b in bars)),"expiresAt":instant(timestamp(closed)+INTERVALS[interval]+15),"observationId":None}
            observations["slope"+interval]={**derived,"unit":"ATR_MULTIPLE","value":values[interval][1][4]}
            if interval=="1h": observations["priceReturn1h"]={**derived,"unit":"LOG_RETURN","value":math.log(bars[-1]["close"]/bars[-2]["close"])}
    conflict_intervals=("5m","1h","4h")
    if all(interval in values and values[interval][1][4] is not None for interval in conflict_intervals):
        slopes=[values[interval][1][4] for interval in conflict_intervals]
        opposite=sum(1 for i in range(3) for j in range(i+1,3) if (slopes[i]<0<slopes[j]) or (slopes[j]<0<slopes[i]))
        observations["timeframeConflict"]={"value":opposite/3,"source":"BINANCE_SPOT_CLOSED_5M_1H_4H",
            "observedAt":instant(max(timestamp(values[interval][0][-1]["closeTime"]) for interval in conflict_intervals)),
            "availableAt":instant(max(timestamp(bar["availableAt"]) for interval in conflict_intervals for bar in values[interval][0])),
            "instrument":instrument(symbol),"sourceVersion":SPOT_SOURCE_VERSION,"unit":"RATIO","observationId":None,
            "expiresAt":instant(max(timestamp(values[interval][0][-1]["closeTime"]) for interval in conflict_intervals)+315)}
    return {"symbol":symbol,"closed5mAt":instant(timestamp(five[0][-1]["closeTime"])) if five else None,
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
        if timestamp(bar["openTime"])!=start+i*step or abs(timestamp(bar["closeTime"])-(start+(i+1)*step))>Decimal(".001"): return False
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
    sources=provenance.get("sources")
    if not isinstance(sources,list) or not sources: raise ValueError("Explicit Spot and CoinGlass source identities required")
    for source in sources:
        if any(not isinstance(source.get(k),str) or not source[k].strip() for k in ("provider","source","sourceVersion","instrument","unit")) or source["sourceVersion"] in ("UNKNOWN","UNVERIFIED"):
            raise ValueError("Incomplete historical source identity")
    if not {"BINANCE_SPOT","COINGLASS"}.issubset({s["provider"] for s in sources}): raise ValueError("Spot and CoinGlass provenance must be separate")


def registered_observation(o,provenance):
    return any(all(o.get(k)==source.get(k) for k in ("source","sourceVersion","instrument","unit")) for source in provenance["sources"])


def future_bar_valid(bar,symbol,captured_at,provenance):
    try:
        return (instrument(symbol) is not None and bar.get("instrument")==instrument(symbol) and bar.get("source") in ("BINANCE_SPOT","BINANCE_SPOT_CLOSED_1M","BINANCE_SPOT_CLOSED_5M")
                and isinstance(bar.get("sourceVersion"),str) and bool(bar["sourceVersion"].strip()) and bar["sourceVersion"] not in ("UNKNOWN","UNVERIFIED")
                and bar.get("unit")=="OHLCV" and registered_observation(bar,provenance)
                and timestamp(bar["closeTime"])<=timestamp(bar["availableAt"])<=captured_at)
    except (KeyError,TypeError,ValueError): return False


def sha256(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def _export_keys(value,required,optional=()):
    if not isinstance(value,dict) or not set(required)<=set(value) or set(value)-set(required)-set(optional):
        raise ValueError("Export fields must match the card-only schema; private/current/derived payloads are forbidden")


def _source_identity(value):
    keys=("provider","source","sourceVersion","instrument","unit")
    _export_keys(value,keys)
    if any(not isinstance(value[k],str) or not value[k].strip() for k in keys): raise ValueError("Incomplete source identity")
    provider="COINGLASS" if value["source"].startswith("COINGLASS:") else "BINANCE_SPOT" if value["source"].startswith("BINANCE_SPOT") else None
    if value["provider"]!=provider or value["sourceVersion"] in ("UNKNOWN","UNVERIFIED"):
        raise ValueError("Source provider/version mismatch")
    return tuple(value[k] for k in keys)


def _source_set(values):
    if not isinstance(values,list): raise ValueError("Explicit source identity list required")
    identities=[_source_identity(value) for value in values]
    if len(set(identities))!=len(identities): raise ValueError("Duplicate source identity")
    return set(identities)


def _observation_source(value):
    return _source_identity({**{key:value.get(key) for key in ("source","sourceVersion","instrument","unit")},
        "provider":"COINGLASS" if str(value.get("source","")).startswith("COINGLASS:") else "BINANCE_SPOT"})


def _export_observation(value,symbol,key,at,provenance):
    _export_keys(value,("value","source","sourceVersion","instrument","unit","observedAt","availableAt","expiresAt"),("observationId",))
    if not valid_observation(symbol,key,value,at) or not registered_observation(value,provenance):
        raise ValueError("Observation is unregistered, mixed, expired or unavailable at the original signal cutoff")
    if key=="spotPrice" and not re.fullmatch(r"0|[1-9][0-9]*",str(value.get("observationId",""))):
        raise ValueError("Actual immutable aggregate trade identity required")
    return _observation_source(value)


def validate_card_export(manifest,records):
    """Validate Java card-only file exports without DB access, trusting neither labels nor prepared features."""
    _export_keys(manifest,("schemaVersion","exportKind","symbol","featureVersion","labelDefinition","range","recordCount","sourceVersions","provenance"),
        ("files","modelVersion","calibrationVersion","thresholdVersion","releasePolicy","validUntil","thresholdCandidates","walkForward","xgboostParams","numBoostRound",
         "exclusions","modelMode","productionModelReady"))
    if type(manifest["schemaVersion"]) is not int or manifest["schemaVersion"]!=1 or manifest["exportKind"]!=EXPORT_KIND:
        raise ValueError("Unsupported card export schema")
    if "modelMode" in manifest and manifest["modelMode"]!="SHADOW" or "productionModelReady" in manifest and manifest["productionModelReady"] is not False:
        raise ValueError("An offline export cannot authorize model release or claim production readiness")
    if "exclusions" in manifest:
        exclusions=manifest["exclusions"]
        if not isinstance(exclusions,dict) or any(not isinstance(key,str) or not key.strip() or type(count) is not int or count<0 for key,count in exclusions.items()):
            raise ValueError("Export exclusions must be explicit nonnegative integer counts")
    symbol=manifest["symbol"]
    if instrument(symbol) is None or manifest["featureVersion"]!=FEATURE_VERSION or manifest["labelDefinition"]!=LABEL_DEFINITION:
        raise ValueError("Export symbol/feature/label identity mismatch")
    if type(manifest["recordCount"]) is not int or manifest["recordCount"]<0 or manifest["recordCount"]!=len(records):
        raise ValueError("Export record count mismatch")
    _export_keys(manifest["range"],("fromInclusive","toExclusive","availableAtCutoff"))
    start,stop,cutoff=(timestamp(manifest["range"][key]) for key in ("fromInclusive","toExclusive","availableAtCutoff"))
    provenance=manifest["provenance"]; validate_provenance(provenance)
    _export_keys(provenance,("kind","datasetVersion","source","availabilityBasis","capturedAt","sources"))
    captured=timestamp(provenance["capturedAt"])
    if not start<stop<=cutoff<=captured: raise ValueError("Invalid immutable export range/capture cutoff")
    sources=_source_set(provenance["sources"])
    if _source_set(manifest["sourceVersions"])!=sources: raise ValueError("Export source-version manifest mismatch")
    seen=set(); observed_sources=set()
    bar_fields=("openTime","closeTime","availableAt","open","high","low","close")
    bar_optional=("volume","takerBuyBaseVolume","tradeCount","symbol","interval","instrument","source","sourceVersion","unit")
    label_fields=("symbol","side","featureVersion","labelDefinition","signalTradeId","instrument","sourceVersion","signalAsOf","maturedAt","outcome","y")
    for record in records:
        _export_keys(record,("rawFrame","future5m","future1m","horizonTrade","labelResults","provenance"))
        row_provenance=record["provenance"]; validate_provenance(row_provenance)
        _export_keys(row_provenance,("kind","datasetVersion","source","availabilityBasis","capturedAt","sources"))
        row_sources=_source_set(row_provenance["sources"])
        if row_provenance["datasetVersion"]!=provenance["datasetVersion"] or not row_sources<=sources:
            raise ValueError("Mixed dataset/source versions")
        row_capture=timestamp(row_provenance["capturedAt"])
        if row_capture>captured: raise ValueError("Record capture exceeds the manifest capture")
        raw=record["rawFrame"]; _export_keys(raw,("symbol","signalAsOf","bars","evidence"))
        as_of=timestamp(raw["signalAsOf"]); end=as_of+HORIZON
        if raw["symbol"]!=symbol or not start<=as_of<stop or (symbol,as_of) in seen:
            raise ValueError("Duplicate or out-of-range symbol/signal identity")
        seen.add((symbol,as_of)); _export_keys(raw["bars"],INTERVALS)
        for interval,step in INTERVALS.items():
            bars=raw["bars"][interval]
            if not isinstance(bars,list): raise ValueError("Raw closed bars must be explicit")
            for bar in bars:
                _export_keys(bar,bar_fields,bar_optional)
                if not timestamp(bar["openTime"])<timestamp(bar["closeTime"])<=timestamp(bar["availableAt"])<=as_of:
                    raise ValueError("Raw bars cannot be backfilled across the original availability cutoff")
                if "symbol" in bar and bar["symbol"]!=symbol or "instrument" in bar and bar["instrument"]!=instrument(symbol):
                    raise ValueError("Mixed raw bar instrument")
                if "interval" in bar and bar["interval"]!=interval: raise ValueError("Mixed raw bar interval")
        if not isinstance(raw["evidence"],dict): raise ValueError("Raw evidence object required")
        actual_sources={_export_observation(value,symbol,key,as_of,row_provenance) for key,value in raw["evidence"].items()}
        price=raw["evidence"].get("spotPrice")
        if price is None: raise ValueError("Original signal trade is missing")
        frame=build_frame(raw)
        if not finite(frame["atr"]) or frame["atr"]<=0: raise ValueError("Original fixed-at-signal ATR unavailable")
        future_available=[end]
        for key,interval in (("future5m","5m"),("future1m","1m")):
            if not isinstance(record[key],list): raise ValueError("Explicit future path required")
            seen_bars=set()
            for bar in record[key]:
                _export_keys(bar,bar_fields,bar_optional)
                if not future_bar_valid(bar,symbol,min(cutoff,row_capture),row_provenance): raise ValueError("Unregistered or unavailable future bar")
                if "symbol" in bar and bar["symbol"]!=symbol or "interval" in bar and bar["interval"]!=interval: raise ValueError("Mixed future bar identity")
                opened=timestamp(bar["openTime"]); step=300 if interval=="5m" else 60
                if (bar["source"] not in ("BINANCE_SPOT","BINANCE_SPOT_CLOSED_"+interval.upper()) or opened%step!=0
                        or not contiguous([bar],opened,1,step) or timestamp(bar["closeTime"])>opened+step):
                    raise ValueError("Future path has an invalid OHLC, timeframe or closed UTC boundary")
                if opened in seen_bars: raise ValueError("Duplicate future bar identity")
                seen_bars.add(opened); future_available.append(timestamp(bar["availableAt"])); actual_sources.add(_observation_source(bar))
        horizon=record["horizonTrade"]
        if horizon is not None:
            actual_sources.add(_export_observation(horizon,symbol,"spotPrice",end,row_provenance))
            future_available.append(timestamp(horizon["availableAt"]))
        _export_keys(record["labelResults"],("LONG","SHORT"))
        for side in ("LONG","SHORT"):
            label=record["labelResults"][side]; _export_keys(label,label_fields)
            if (label["symbol"]!=symbol or label["side"]!=side or label["featureVersion"]!=FEATURE_VERSION
                    or label["labelDefinition"]!=LABEL_DEFINITION or label["instrument"]!=instrument(symbol)
                    or label["sourceVersion"]!=price["sourceVersion"] or type(label["signalTradeId"]) is not int
                    or label["signalTradeId"]<0 or str(label["signalTradeId"])!=price["observationId"]
                    or timestamp(label["signalAsOf"])!=as_of): raise ValueError("Label immutable identity mismatch")
            matured=timestamp(label["maturedAt"])
            if not max(future_available)<=matured<=min(cutoff,row_capture): raise ValueError("Label matured before evidence was actually available")
            y,outcome=first_touch(price["value"],frame["atr"],as_of,record["future5m"],record["future1m"],side)
            if label["outcome"]!=outcome or label["y"]!=y or label["y"] is not None and type(label["y"]) is not int:
                raise ValueError("Stored label disagrees with independent frozen first-touch reconstruction")
            if outcome=="TIMEOUT" and horizon is None: raise ValueError("Timeout requires a real point-in-time horizon trade")
        if actual_sources!=row_sources: raise ValueError("Record provenance must describe actual observation tuples, not declared unused providers")
        observed_sources.update(actual_sources)
    if records and observed_sources!=sources: raise ValueError("Manifest sources must be derived from the exported observations")


def _local_dataset_file(parent,descriptor):
    name=descriptor.get("path")
    if not isinstance(name,str) or not name or pathlib.Path(name).is_absolute() or ".." in pathlib.Path(name).parts:
        raise ValueError("Only manifest-local raw frame files are accepted")
    current=parent
    for part in pathlib.Path(name).parts:
        current=current/part
        if current.is_symlink(): raise ValueError("Dataset symlinks are forbidden")
    if not current.is_file() or not current.resolve().is_relative_to(parent): raise ValueError("Dataset file missing or outside manifest")
    return current


def read_dataset(manifest_path):
    path=pathlib.Path(manifest_path).resolve(); manifest=decode_json(path.read_text())
    validate_provenance(manifest["provenance"])
    records=[]
    if not manifest.get("files"): raise ValueError("No explicit historical files; actual sample count UNKNOWN")
    seen_files=set()
    for descriptor in manifest["files"]:
        data_path=_local_dataset_file(path.parent,descriptor)
        if data_path in seen_files or descriptor.get("kind")!="RAW_FRAMES": raise ValueError("Duplicate or non-raw historical file")
        seen_files.add(data_path); encoded=data_path.read_bytes()
        if hashlib.sha256(encoded).hexdigest()!=descriptor["sha256"]: raise ValueError("Historical file checksum mismatch")
        count=0
        for line in encoded.decode("utf-8").splitlines():
            if line.strip():
                record=decode_json(line); validate_provenance(record["provenance"])
                if record["provenance"]["datasetVersion"]!=manifest["provenance"]["datasetVersion"]: raise ValueError("Mixed dataset versions")
                if any(source not in manifest["provenance"]["sources"] for source in record["provenance"]["sources"]): raise ValueError("Record expands the manifest source identity allowlist")
                if "vector" in record or "features" in record or "currentSnapshot" in record: raise ValueError("Features must be rebuilt from genuine raw point-in-time inputs")
                records.append(record); count+=1
        if manifest.get("exportKind") is not None:
            _export_keys(descriptor,("path","kind","count","sha256"))
            if type(descriptor["count"]) is not int or descriptor["count"]!=count: raise ValueError("Export file record count mismatch")
    if manifest.get("exportKind") is not None: validate_card_export(manifest,records)
    elif any("labelResults" in record for record in records): raise ValueError("Card labels require their immutable export manifest identity")
    return manifest,records


def verify_export(manifest_path):
    path=pathlib.Path(manifest_path); digest=sha256(path)
    manifest,records=read_dataset(path)
    if manifest.get("exportKind")!=EXPORT_KIND or digest!=sha256(path): raise ValueError("Explicit unchanged card export manifest required")
    outcomes={side:{} for side in ("LONG","SHORT")}
    for record in records:
        for side in outcomes:
            outcome=record["labelResults"][side]["outcome"]
            outcomes[side][outcome]=outcomes[side].get(outcome,0)+1
    return {"exportKind":EXPORT_KIND,"manifestSha256":digest,"symbol":manifest["symbol"],"featureVersion":FEATURE_VERSION,
        "labelDefinition":LABEL_DEFINITION,"range":manifest["range"],"recordCount":len(records),"labelOutcomes":outcomes,
        "sourceVersions":manifest["sourceVersions"],"mode":"SHADOW","productionModelReady":False,
        "trainingReadiness":"UNVALIDATED_REQUIRES_EXPLICIT_COSTS_SPLITS_AND_REAL_MODEL_EVIDENCE"}


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
        if not future or any(not future_bar_valid(b,frame["symbol"],captured_at,record["provenance"]) for b in future):
            excluded["UNCLOSED_OR_UNCAPTURED_FUTURE"]=excluded.get("UNCLOSED_OR_UNCAPTURED_FUTURE",0)+1; continue
        if any(not future_bar_valid(b,frame["symbol"],captured_at,record["provenance"]) for b in record.get("future1m",[])):
            excluded["UNCLOSED_OR_UNCAPTURED_FUTURE"]=excluded.get("UNCLOSED_OR_UNCAPTURED_FUTURE",0)+1; continue
        # An actual Spot trade at the signal cutoff is required. A stale close cannot be an invented entry.
        price=frame["realInputs"].get("spotPrice")
        if price is None or not price["source"].startswith("BINANCE_SPOT") or not registered_observation(price,record["provenance"]) or timestamp(frame["signalAsOf"])-timestamp(price["observedAt"])>policy["maxSignalTradeAgeSeconds"]:
            excluded["MISSING_SIGNAL_TRADE"]=excluded.get("MISSING_SIGNAL_TRADE",0)+1; continue
        original=record["rawFrame"].get("evidence",{})
        if any(not registered_observation(o,record["provenance"]) for key,o in original.items() if key in frame["realInputs"]):
            excluded["UNREGISTERED_OBSERVATION_IDENTITY"]=excluded.get("UNREGISTERED_OBSERVATION_IDENTITY",0)+1; continue
        labels={}; returns={}; reject=None
        for side in ("LONG","SHORT"):
            label,reason=first_touch(price["value"],frame["atr"],frame["signalAsOf"],record.get("future5m",[]),record.get("future1m",[]),side)
            if label is None: reject=reason; break
            labels[side]=label
            if reason=="TIMEOUT":
                horizon=record.get("horizonTrade")
                try:
                    if not valid_observation(frame["symbol"],"spotPrice",horizon,end) or not registered_observation(horizon,record["provenance"]): raise ValueError()
                    observed,available=timestamp(horizon["observedAt"]),timestamp(horizon["availableAt"])
                    if not end-Decimal(str(policy["maxSignalTradeAgeSeconds"]))<=observed<=available<=end: raise ValueError()
                except (KeyError,TypeError,ValueError): reject="MISSING_POINT_IN_TIME_HORIZON_TRADE"; break
                gross=(horizon["value"]-price["value"])/price["value"]*(1 if side=="LONG" else -1)
            else: gross=frame["atr"]/price["value"] if reason=="TARGET" else -.75*frame["atr"]/price["value"]
            returns[side]=gross-policy["roundTripFeeRate"]-policy["roundTripSlippageRate"]
        if reject: excluded[reject]=excluded.get(reject,0)+1; continue
        vol=frame["vector"][FEATURE_NAMES.index("5m.volatility12")]
        # Volatility strata boundaries are frozen policy values, never fitted on test outcomes.
        cuts=policy["volatilityStrata"]
        bucket="LOW" if vol<cuts[0] else "MEDIUM" if vol<cuts[1] else "HIGH"
        # The target interval remains exactly four hours, but its label is not knowable
        # until every required surrounding closed bar (and persisted label) is available.
        label_available=max([end]+[timestamp(bar["availableAt"]) for bar in future]
            +[timestamp(bar["availableAt"]) for bar in record.get("future1m",[]) if first<=timestamp(bar["openTime"])<math.ceil(end/300)*300]
            +[timestamp(label["maturedAt"]) for label in record.get("labelResults",{}).values()])
        prepared.append({"symbol":frame["symbol"],"signalAsOf":timestamp(frame["signalAsOf"]),
                         "labelEnd":end,"labelAvailableAt":label_available,"vector":frame["vector"],
                         "labels":labels,"netReturns":returns,"regime":frame["fourHourTrend"],"volatility":bucket,
                         "realInputs":frame["realInputs"],"frame":frame,"missingPattern":missing_pattern(frame["vector"])})
    return sorted(prepared,key=lambda r:(r["signalAsOf"],r["symbol"])),excluded


def temporal_split(rows, boundaries):
    ends=[timestamp(boundaries[k]) for k in ("trainEnd","calibrationEnd","validationEnd","testEnd")]
    if any(b<=a for a,b in zip(ends,ends[1:])): raise ValueError("Strict chronological independent split boundaries required")
    if any("labelAvailableAt" not in row or timestamp(row["labelAvailableAt"])<timestamp(row["labelEnd"]) for row in rows):
        raise ValueError("Actual label availability at or after the fixed horizon is mandatory")
    parts=[]
    for i,end in enumerate(ends):
        start=-math.inf if i==0 else ends[i-1]+HORIZON
        parts.append([row for row in rows if row["signalAsOf"]>=start and row["labelEnd"]<=end and timestamp(row["labelAvailableAt"])<=end])
    if any(not part for part in parts): raise ValueError("Empty purged/4h-embargo split; no readiness shortcut")
    for left,right in zip(parts,parts[1:]):
        if max(r["labelEnd"] for r in left)+HORIZON>min(r["signalAsOf"] for r in right): raise ValueError("Overlapping label intervals or missing 4h embargo")
    return parts


def overlap_effective_samples(rows):
    """Sum of average label uniqueness for fixed 4h intervals, across simultaneous assets.
    For equal horizons this equals union duration / horizon; repeated rows cannot
    manufacture independent samples. Adjacent nonoverlapping intervals each count once.
    """
    intervals=sorted((timestamp(r["signalAsOf"]),timestamp(r["labelEnd"])) for r in rows)
    total=0; end=None
    for start,stop in intervals:
        if not math.isclose(stop-start,HORIZON,abs_tol=.001): raise ValueError("Invalid four-hour label identity")
        total+=max(0,stop-max(start,end if end is not None else start))
        end=stop if end is None else max(end,stop)
    return float(total/HORIZON)


def missing_pattern(vector):
    return "".join("1" if v is None else "0" for v in vector)


def population(rows,side):
    positive=sum(r["labels"][side] for r in rows)
    return {"count":len(rows),"positive":positive,"negative":len(rows)-positive,"effectiveSamples":overlap_effective_samples(rows)}


def population_pass(value,policy):
    return (value["positive"]>=policy["minPositiveSamples"] and value["negative"]>=policy["minNegativeSamples"]
            and value["positive"]+value["negative"]==value["count"] and policy["minEffectiveSamples"]<=value["effectiveSamples"]<=value["count"])


def time_block_intervals(rows,labels,probabilities,base_rate,policy):
    """Deterministic non-overlapping temporal block bootstrap. Never iid-row bootstrap."""
    blocks={}
    for i,row in enumerate(rows):
        block=math.floor(row["signalAsOf"]/policy["timeBlockSeconds"])
        # Purge labels crossing a bootstrap block boundary; adjacent resampled blocks then share no 4h outcomes.
        if row["labelEnd"]<=(block+1)*policy["timeBlockSeconds"]: blocks.setdefault(block,[]).append(i)
    ordered=[blocks[k] for k in sorted(blocks)]
    if len(ordered)<policy["minTimeBlocks"]: return {"blockCount":len(ordered),"intervals":{}}
    rng=random.Random(policy["bootstrapSeed"]); samples={k:[] for k in ("brier","ece","logLoss","hitRate")}
    for _ in range(policy["ciReplicates"]):
        indices=[i for _ in ordered for i in ordered[rng.randrange(len(ordered))]]
        m=metrics([labels[i] for i in indices],[probabilities[i] for i in indices],base_rate,policy["binCount"])
        for key in samples: samples[key].append(m[key])
    alpha=(1-policy["ciConfidence"])/2
    def quantile(values,q):
        values=sorted(values); position=(len(values)-1)*q; lo=math.floor(position); hi=math.ceil(position)
        return values[lo]+(values[hi]-values[lo])*(position-lo)
    return {"blockCount":len(ordered),"blockSeconds":policy["timeBlockSeconds"],"replicates":policy["ciReplicates"],
            "confidence":policy["ciConfidence"],"intervals":{k:{"lower":quantile(v,alpha),"upper":quantile(v,1-alpha)} for k,v in samples.items()}}


def uncertainty_pass(value,policy):
    try:
        if value["blockCount"]<policy["minTimeBlocks"] or value["blockSeconds"]!=policy["timeBlockSeconds"] or value["replicates"]!=policy["ciReplicates"] or value["confidence"]!=policy["ciConfidence"]: return False
        for key in ("brier","ece","logLoss","hitRate"):
            interval=value["intervals"][key]
            if not 0<=interval["lower"]<=interval["upper"] or interval["upper"]-interval["lower"]>policy["max"+key[0].upper()+key[1:]+"CiWidth"]: return False
        return True
    except (KeyError,TypeError,ValueError): return False


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
            "hitRate":sum(labels)/n,"positive":sum(labels),"negative":n-sum(labels),
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
        report["sides"][side]=metric_evidence(rows,side,raw[side],calibrated[side],base_rates[side],policy)
        for field,expected in (("symbol",policy["requiredAssets"]),("regime",policy["requiredRegimes"]),("volatility",policy["requiredVolatilityStrata"])):
            for value in expected:
                indices=[i for i,r in enumerate(rows) if r[field]==value]
                key=side+":"+field+":"+value
                report["strata"][key]={"count":0} if not indices else metric_evidence([rows[i] for i in indices],side,[raw[side][i] for i in indices],[calibrated[side][i] for i in indices],base_rates[side],policy)
    report["nonDirectional"]=nondirectional_evidence(rows,calibrated["LONG"],calibrated["SHORT"],thresholds,policy)
    return report


def metric_evidence(rows,side,raw,calibrated,base,policy):
    labels=[r["labels"][side] for r in rows]
    return {"raw":metrics(labels,raw,base,policy["binCount"]),"calibrated":metrics(labels,calibrated,base,policy["binCount"]),
            "population":population(rows,side),"uncertainty":time_block_intervals(rows,labels,calibrated,base,policy)}


def nondirectional_evidence(rows,longs,shorts,thresholds,policy):
    result={}; blocks={math.floor(r["signalAsOf"]/policy["timeBlockSeconds"]) for r in rows}
    for state in ("RANGE","WATCH"):
        indices=[]
        for i,row in enumerate(rows):
            if tier_name(longs[i],shorts[i],thresholds) or tier_name(shorts[i],longs[i],thresholds): continue
            is_range=abs(longs[i]-shorts[i])<=thresholds["rangeMaxGap"] and max(longs[i],shorts[i])<=thresholds["rangeMaxProbability"] and row["regime"]=="RANGE"
            if is_range==(state=="RANGE"): indices.append(i)
        coverage=[sum(math.floor(rows[i]["signalAsOf"]/policy["timeBlockSeconds"])==block for i in indices)/sum(math.floor(r["signalAsOf"]/policy["timeBlockSeconds"])==block for r in rows) for block in blocks]
        # A first-touch success on either side is a missed directional entry for this non-directional output.
        result[state]={"count":len(indices),"coverage":len(indices)/len(rows),"effectiveSamples":overlap_effective_samples([rows[i] for i in indices]),
                       "falseEntryRate":sum(rows[i]["labels"]["LONG"]==1 or rows[i]["labels"]["SHORT"]==1 for i in indices)/len(indices) if indices else None,
                       "coverageSwing":max(coverage)-min(coverage) if coverage else None,"selectionSplit":"FINAL_TEST_ONLY"}
    return result


def nondirectional_pass(report,policy):
    try:
        return all(report[s]["selectionSplit"]=="FINAL_TEST_ONLY" and report[s]["count"]>=policy["min"+s.title()+"TestSamples"]
                   and report[s]["effectiveSamples"]>=policy["minEffectiveSamples"] and report[s]["coverage"]>=policy["minNonDirectionalCoverage"]
                   and report[s]["falseEntryRate"]<=policy["max"+s.title()+"FalseEntryRate"] and report[s]["coverageSwing"]<=policy["maxNonDirectionalCoverageSwing"] for s in ("RANGE","WATCH"))
    except (KeyError,TypeError,ValueError): return False


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
        pairs=list(report["sides"].values())+list(report["strata"].values())
        return set(report["sides"])=={"LONG","SHORT"} and set(report["strata"])==expected_strata and set(report["tiers"])==expected_tiers and all(check_metric_pair(v,policy["minTestSamples"],policy) for v in report["sides"].values()) and all(check_metric_pair(v,policy["minStratumSamples"],policy) for v in report["strata"].values()) and all(v["count"]>=policy["minTierSamples"] and finite(v["netEdge"]) and v["netEdge"]>0 for v in report["tiers"].values()) and all(population_pass(v["population"],policy) and uncertainty_pass(v["uncertainty"],policy) for v in pairs) and nondirectional_pass(report["nonDirectional"],policy)
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
    if not isinstance(policy.get("riskMetrics"),dict) or set(policy["riskMetrics"])!=set(RISK_REQUIRED): raise ValueError("Explicit complete side risk metric policy required; missing coverage is UNKNOWN")
    for side,specs in policy["riskMetrics"].items():
        if side not in ("LONG","SHORT","NON_DIRECTIONAL") or not isinstance(specs,dict): raise ValueError("Side-specific risk policy required")
        if set(specs)!=RISK_REQUIRED[side]: raise ValueError("Missing asset-side-metric risk coverage")
        for metric,spec in specs.items():
            if spec.get("unit")!=UNITS.get(metric) or not isinstance(spec.get("higherIsWorse"),bool) or not 0<spec["mediumPercentile"]<spec["highPercentile"]<1 or type(spec.get("minSamples")) is not int or spec["minSamples"]<1: raise ValueError("Invalid asset-side-history risk distribution policy")
    for key in ("minPositiveSamples","minNegativeSamples","minTimeBlocks","ciReplicates","timeBlockSeconds","minRangeTestSamples","minWatchTestSamples"):
        if type(policy.get(key)) is not int or policy[key]<=0: raise ValueError("Explicit V42 sample policy required: "+key)
    if type(policy.get("bootstrapSeed")) is not int or policy["timeBlockSeconds"]<HORIZON or policy["minTimeBlocks"]<2 or policy["ciReplicates"]<2: raise ValueError("Independent time-block uncertainty policy required")
    for key in ("minEffectiveSamples","maxBrierCiWidth","maxEceCiWidth","maxLogLossCiWidth","maxHitRateCiWidth"):
        if not finite(policy.get(key)) or policy[key]<=0: raise ValueError("Explicit uncertainty/effective sample policy required: "+key)
    for key in ("ciConfidence","minNonDirectionalCoverage","maxNonDirectionalCoverageSwing","maxRangeFalseEntryRate","maxWatchFalseEntryRate","maxFeatureOutlierFraction","driftLowerQuantile","driftUpperQuantile"):
        if not finite(policy.get(key)) or not 0<policy[key]<1: raise ValueError("Explicit bounded coverage/drift policy required: "+key)
    if policy["driftLowerQuantile"]>=policy["driftUpperQuantile"]: raise ValueError("Ordered drift reference quantiles required")
    costs=policy.get("costProvenance",{})
    for key in ("source","sourceVersion","instrument","observedAt","availableAt","expiresAt","unit"):
        if not costs.get(key): raise ValueError("Real fee/slippage provenance required: "+key)
    if costs["unit"]!="RATE" or costs.get("kind")!="REAL_HISTORICAL" or costs["sourceVersion"] in ("UNKNOWN","UNVERIFIED") or not timestamp(costs["observedAt"])<=timestamp(costs["availableAt"])<=timestamp(costs["expiresAt"]): raise ValueError("Invalid fee/slippage identity")


def risk_distributions(train,policy):
    result={}
    for asset in policy["requiredAssets"]:
        entries={}
        for side,specs in policy["riskMetrics"].items():
          entries={}
          for key,spec in specs.items():
            observations=[]
            for r in train:
                if r["symbol"]!=asset: continue
                if key=="crowdingOpenInterestChange1h":
                    oi=r["realInputs"].get("openInterestChange1h"); price=r["realInputs"].get("priceReturn1h")
                    if oi and price and oi["value"]>0 and ((side=="LONG" and price["value"]>0) or (side=="SHORT" and price["value"]<0)): observations.append(oi)
                elif key in r["realInputs"]: observations.append(r["realInputs"][key])
            # Repeated cached snapshots are one observation, not inflated independent samples.
            unique={}
            for observation in observations:
                observation_key=(observation["source"],observation.get("instrument"),observation.get("sourceVersion"),timestamp(observation["observedAt"]))
                existing=unique.get(observation_key)
                if existing is not None and existing["value"]!=observation["value"]:
                    raise ValueError("Conflicting same-source historical risk observation")
                if existing is None or timestamp(observation["availableAt"])<timestamp(existing["availableAt"]): unique[observation_key]=observation
            values=sorted(o["value"] for o in unique.values())
            if len(values)<spec["minSamples"]: continue
            entries[key]={**spec,"symbol":asset,"side":side,"metricKey":key,"riskVersion":policy["riskVersion"],"sortedValues":values,"samples":len(values),"source":"REAL_HISTORICAL_TRAIN_ONLY",
                          "asOf":instant(max(timestamp(o["availableAt"]) for o in unique.values()))}
          result.setdefault(asset,{})[side]=entries
    return result


def train_fold(parts,manifest):
    validate_split_samples(parts,manifest["releasePolicy"])
    populations=validate_v42_populations(parts,manifest["releasePolicy"])
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
    report["populations"]=populations
    report["patternMetrics"]={}
    for pattern in sorted({missing_pattern(r["vector"]) for r in test}):
        indices=[i for i,r in enumerate(test) if missing_pattern(r["vector"])==pattern]
        report["patternMetrics"][pattern]={side:metric_evidence([test[i] for i in indices],side,[raw_test[side][i] for i in indices],
                                              [calibrated_test[side][i] for i in indices],bases[side],policy) for side in ("LONG","SHORT")}
    report["thresholdSelectionTiers"]=selection_tiers; report["rangeEvidence"]=range_evidence
    report["thresholds"]=thresholds; report["calibrators"]=calibrators
    report["splits"]=[{"name":name,"count":len(rows),"start":instant(min(r["signalAsOf"] for r in rows)),
                       "end":instant(max(r["signalAsOf"] for r in rows)),"labelEnd":instant(max(r["labelEnd"] for r in rows)),
                       "labelAvailableAt":instant(max(timestamp(r["labelAvailableAt"]) for r in rows))}
                      for name,rows in zip(("TRAIN","CALIBRATION","VALIDATION","TEST"),parts)]
    report["embargoSeconds"]=HORIZON
    report["passed"]=release_pass(report,policy)
    report["passed"]=report["passed"] and all(check_metric_pair(v,policy["minStratumSamples"],policy) and population_pass(v["population"],policy)
                             and uncertainty_pass(v["uncertainty"],policy) for pair in report["patternMetrics"].values() for v in pair.values())
    return models,calibrators,thresholds,report,risk_distributions(train,policy)


def validate_split_samples(parts,policy):
    if len(parts)!=4: raise ValueError("Four independent splits required")
    for rows,key in zip(parts,("minTrainSamples","minCalibrationSamples","minValidationSamples","minTestSamples")):
        if type(policy.get(key)) is not int or policy[key]<=0: raise ValueError("Explicit split sample policy required: "+key)
        if len(rows)<policy[key]: raise ValueError("Insufficient genuine independent split samples: "+key)


def validate_v42_populations(parts,policy):
    evidence=[]
    patterns={missing_pattern(r["vector"]) for r in parts[0]}
    for rows in parts:
        if {missing_pattern(r["vector"]) for r in rows}!=patterns: raise ValueError("Missing-pattern coverage differs across independent splits")
        strata={"ALL":rows}
        for field,key in (("symbol","requiredAssets"),("regime","requiredRegimes"),("volatility","requiredVolatilityStrata")):
            for value in policy[key]: strata[field+":"+value]=[r for r in rows if r[field]==value]
        for pattern in patterns: strata["missing:"+pattern]=[r for r in rows if missing_pattern(r["vector"])==pattern]
        report={key:{side:population(group,side) for side in ("LONG","SHORT")} for key,group in strata.items()}
        if any(not population_pass(p,policy) for group in report.values() for p in group.values()): raise ValueError("Insufficient side/outcome/effective population in independent split stratum")
        evidence.append(report)
    return evidence


def write_json(path,value):
    path.write_text(json.dumps(value,sort_keys=True,separators=(",",":"),allow_nan=False,default=json_default)+"\n")


def train_bundle(manifest,rows,excluded,output):
    validate_training_manifest(manifest)
    policy=manifest["releasePolicy"]; validate_policy(policy)
    if len(policy["requiredAssets"])!=1: raise ValueError("V42 raw-scale features require a per-asset bundle; cross-asset normalization is not inferred")
    if policy["costProvenance"]["instrument"]!=instrument(policy["requiredAssets"][0]): raise ValueError("Fee/slippage instrument mismatch")
    if any(not timestamp(policy["costProvenance"]["availableAt"])<=r["signalAsOf"]<=timestamp(policy["costProvenance"]["expiresAt"]) for r in rows): raise ValueError("Costs unavailable at actual historical signal cutoff")
    if not any(o.get("source","").startswith("COINGLASS:") for r in rows for o in r["realInputs"].values()): raise ValueError("No actual historical CoinGlass observations; declared provenance alone is not evidence")
    if any(row["symbol"] not in policy["requiredAssets"] or row["regime"] not in policy["requiredRegimes"] or row["volatility"] not in policy["requiredVolatilityStrata"] for row in rows):
        raise ValueError("Every actual asset/regime/volatility stratum must be covered by the frozen release policy")
    folds=manifest["walkForward"]
    if len(folds)<policy["minWalkForwardFolds"]: raise ValueError("Insufficient explicit temporal walk-forward folds")
    previous_test_end=-math.inf; reports=[]
    for bounds in folds:
        parts=temporal_split(rows,bounds)
        if min(r["signalAsOf"] for r in parts[-1])<=previous_test_end: raise ValueError("Walk-forward final test folds overlap")
        previous_test_end=max(timestamp(r["labelAvailableAt"]) for r in parts[-1])
        models,calibrators,thresholds,report,distributions=train_fold(parts,manifest)
        reports.append(report)
    required_sides={"LONG","SHORT","NON_DIRECTIONAL"}
    risk_complete=(set(policy["riskMetrics"])==required_sides and all(policy["riskMetrics"][s] for s in required_sides)
                   and all(set(distributions.get(asset,{}))==required_sides and all(set(distributions[asset][s])==set(policy["riskMetrics"][s]) for s in required_sides) for asset in policy["requiredAssets"]))
    def train_quantile(values,q):
        if not values: return None
        values=sorted(values); x=(len(values)-1)*q; lo=math.floor(x); hi=math.ceil(x)
        return values[lo]+(values[hi]-values[lo])*(x-lo)
    train=parts[0]
    lifecycle={"dataVersion":manifest["provenance"]["datasetVersion"],"riskVersion":policy["riskVersion"],
               "trainedThrough":report["splits"][1]["labelAvailableAt"],"validatedThrough":report["splits"][3]["labelAvailableAt"],"validUntil":manifest["validUntil"],
               "missingPatterns":sorted(report["patternMetrics"]),"maxFeatureOutlierFraction":policy["maxFeatureOutlierFraction"],
               "featureLower":[train_quantile([r["vector"][i] for r in train if r["vector"][i] is not None],policy["driftLowerQuantile"]) for i in range(len(FEATURE_NAMES))],
               "featureUpper":[train_quantile([r["vector"][i] for r in train if r["vector"][i] is not None],policy["driftUpperQuantile"]) for i in range(len(FEATURE_NAMES))]}
    if timestamp(lifecycle["validUntil"])<=timestamp(lifecycle["validatedThrough"]): raise ValueError("Bundle expires before independent validation is complete")
    out=pathlib.Path(output)
    if out.exists(): raise ValueError("Atomic bundle output must be a new directory; refusing overwrite")
    out.mkdir(parents=True)
    final_report={"dataKind":"REAL_HISTORICAL","datasetVersion":manifest["provenance"]["datasetVersion"],
                  "actualInputSamples":len(rows)+sum(excluded.values()),"actualUsableSamples":len(rows),"excluded":excluded,
                  "folds":reports,"final":reports[-1],"lifecycle":lifecycle,"riskMetricCoverageComplete":risk_complete,
                  "unassessedRiskCategories":{"NON_DIRECTIONAL":["CHASE","REVERSAL","CROWDING","LIQUIDATION"]},
                  "productionModelReady":all(r["passed"] for r in reports) and risk_complete,
                  "provenance":manifest["provenance"],"files":manifest["files"]}
    write_json(out/"validation.json",final_report)
    if not final_report["productionModelReady"]:
        write_json(out/"shadow.json",{"mode":"SHADOW","reason":"REAL_VALIDATION_GATES_FAILED","productionModelReady":False})
        return final_report
    for side in ("LONG","SHORT"):
        models[side].set_attr(asset_card_side=side,asset_card_model_version=manifest["modelVersion"],
                             asset_card_feature_version=FEATURE_VERSION,asset_card_calibration_version=manifest["calibrationVersion"],
                             asset_card_threshold_version=manifest["thresholdVersion"],asset_card_data_kind="REAL_HISTORICAL",
                             asset_card_dataset_version=manifest["provenance"]["datasetVersion"],asset_card_risk_version=policy["riskVersion"],
                             asset_card_trained_through=lifecycle["trainedThrough"],asset_card_valid_until=lifecycle["validUntil"])
        models[side].save_model(out/(side.lower()+".ubj"))
    write_json(out/"calibration.json",calibrators)
    write_json(out/"thresholds.json",thresholds)
    write_json(out/"risk-distributions.json",distributions)
    bundle={"schemaVersion":2,"dataKind":"REAL_HISTORICAL","featureVersion":FEATURE_VERSION,"atrDefinition":ATR_DEFINITION,"lifecycle":lifecycle,
            "featureNames":FEATURE_NAMES,"modelVersion":manifest["modelVersion"],"calibrationVersion":manifest["calibrationVersion"],
            "thresholdVersion":manifest["thresholdVersion"],"xgboostVersion":XGBOOST_VERSION,"horizonSeconds":HORIZON,
            "labelDefinition":LABEL_DEFINITION,
            "releasePolicy":policy,"files":{name:sha256(out/name) for name in ("long.ubj","short.ubj","calibration.json","thresholds.json","risk-distributions.json","validation.json")}}
    write_json(out/"manifest.json",bundle) # manifest is the atomic publication marker, written last
    return {"productionModelReady":True,"manifestSha256":sha256(out/"manifest.json"),"report":final_report}


def validate_training_manifest(manifest):
    validate_provenance(manifest["provenance"])
    for key in ("modelVersion","calibrationVersion","thresholdVersion"):
        if not isinstance(manifest.get(key),str) or not manifest[key].strip(): raise ValueError("Explicit nonblank atomic model version required: "+key)
    validate_policy(manifest["releasePolicy"])
    if not isinstance(manifest["releasePolicy"].get("riskVersion"),str) or not manifest["releasePolicy"]["riskVersion"].strip(): raise ValueError("Explicit directional risk version required")
    timestamp(manifest["validUntil"])
    if not isinstance(manifest.get("thresholdCandidates"),dict) or not manifest["thresholdCandidates"].get("tiers") or not manifest["thresholdCandidates"].get("range"):
        raise ValueError("Explicit validation-only threshold candidates required")


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command",choices=("status","verify-export","inspect","prepare","train"))
    parser.add_argument("manifest",nargs="?"); parser.add_argument("output",nargs="?")
    args=parser.parse_args()
    if args.command=="status":
        print(json.dumps({"mode":"SHADOW","productionModelReady":False,"actualSamples":"UNKNOWN","metrics":"UNKNOWN"})); return
    if not args.manifest: parser.error("An explicit real historical manifest is required")
    if args.command=="verify-export":
        if args.output: parser.error("verify-export is read-only and does not accept an output path")
        print(json.dumps(verify_export(args.manifest),sort_keys=True,allow_nan=False,default=json_default)); return
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
    else: print(json.dumps(train_bundle(manifest,rows,excluded,args.output),sort_keys=True,allow_nan=False,default=json_default))


if __name__ == "__main__": main()
