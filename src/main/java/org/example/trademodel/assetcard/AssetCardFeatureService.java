package org.example.trademodel.assetcard;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Pure, point-in-time Spot feature projection shared with scripts/asset_card_model.py. */
public final class AssetCardFeatureService {
    public static final String FEATURE_VERSION = "SPOT_CARD_FEATURES_V1";
    public static final String ATR_DEFINITION = "5m_TR_SMA14_FIXED_AT_SIGNAL";
    public static final List<String> INTERVALS = List.of("5m", "15m", "1h", "4h");
    private static final Map<String, Long> SECONDS = Map.of("5m",300L,"15m",900L,"1h",3600L,"4h",14400L);
    private static final List<String> BAR_FEATURES = List.of("momentum6", "atrRelative", "volatility12",
            "volumeRatio", "slope12", "centerDistanceAtr", "rangePosition", "takerBuyFraction", "tradeCount");
    public static final List<String> EVIDENCE_FEATURES = List.of("spreadBps", "depth10Bps", "depth25Bps",
            "bookImbalance", "openInterest", "fundingRate", "longShortRatio", "longLiquidation",
            "shortLiquidation", "takerBuySellRatio");
    public static final List<String> FEATURE_NAMES;
    static {
        List<String> names = new ArrayList<>();
        for (String interval : INTERVALS) for (String feature : BAR_FEATURES) names.add(interval + "." + feature);
        names.addAll(EVIDENCE_FEATURES);
        FEATURE_NAMES = List.copyOf(names);
    }

    public record Bar(Instant openTime, Instant closeTime, Instant availableAt, double open, double high,
                      double low, double close, double volume, Double takerBuyBaseVolume, Long tradeCount) {}
    public record Observation(Double value, String source, Instant observedAt, Instant availableAt) {}
    public record RawFrame(String symbol, Instant signalAsOf, Map<String,List<Bar>> bars,
                           Map<String,Observation> evidence) {}
    public record Frame(String symbol, Instant closed5mAt, Instant signalAsOf, Instant availableAt,
                        String featureVersion, List<String> featureNames, List<Double> vector,
                        boolean ready, List<String> reasons, String oneHourState, String fourHourTrend,
                        Double atr, Double structuralSupport, Double structuralResistance,
                        Map<String,Observation> realInputs) {
        public Frame {
            featureNames = List.copyOf(featureNames);
            vector = Collections.unmodifiableList(new ArrayList<>(vector)); // missing values are JSON null, never zero/NaN
            reasons = List.copyOf(reasons);
            realInputs = Collections.unmodifiableMap(new LinkedHashMap<>(realInputs));
        }
    }

    public Frame build(RawFrame input) {
        Objects.requireNonNull(input, "rawFrame");
        Objects.requireNonNull(input.signalAsOf(), "signalAsOf");
        Objects.requireNonNull(input.symbol(), "symbol");
        List<String> reasons = new ArrayList<>();
        Map<String,Observation> observations = new LinkedHashMap<>();
        Map<String,List<Bar>> validBars = new HashMap<>();
        Map<String,List<Double>> vectors = new HashMap<>();
        List<Double> vector = new ArrayList<>();
        Instant available = null;
        for (String interval : INTERVALS) {
            List<Bar> values = input.bars() == null ? List.of() : input.bars().getOrDefault(interval, List.of());
            List<Bar> selected = validateBars(values, interval, input.signalAsOf());
            if (selected.isEmpty()) {
                reasons.add("MISSING_OR_INVALID_CLOSED_BARS:" + interval);
                vector.addAll(Collections.nCopies(BAR_FEATURES.size(), null));
            } else {
                validBars.put(interval, selected);
                List<Double> features = barFeatures(selected);
                vectors.put(interval, features);
                vector.addAll(features);
                for (Bar bar : selected) available = max(available, bar.availableAt());
            }
        }
        if (input.evidence() != null) for (var entry : input.evidence().entrySet()) {
            Observation observation = entry.getValue();
            if (!usable(observation, input.signalAsOf())) reasons.add("FUTURE_OR_INVALID_EVIDENCE:" + entry.getKey());
            else {
                observations.put(entry.getKey(), observation);
                available = max(available, observation.availableAt());
            }
        }
        for (String key : EVIDENCE_FEATURES) {
            Observation evidence = observations.get(key);
            vector.add(evidence == null ? null : evidence.value());
        }
        for (String key : List.of("spreadBps", "depth10Bps", "depth25Bps")) {
            Observation evidence = observations.get(key);
            if (evidence == null || evidence.value() < 0) reasons.add("MISSING_OR_INVALID_CORE_EVIDENCE:" + key);
        }
        List<Bar> five = validBars.get("5m");
        Double atr = five == null ? null : atr(five);
        Double support = five == null ? null : support(five);
        Double resistance = five == null ? null : resistance(five);
        Instant closed = five == null ? null : five.get(five.size()-1).closeTime();
        if (five != null && atr != null && atr > 0) {
            Instant fiveAvailable=five.stream().map(Bar::availableAt).max(Instant::compareTo).orElseThrow();
            double price = five.get(five.size()-1).close();
            observations.put("structuralCenterDistanceAtr", new Observation(Math.abs(price-(support+resistance)/2)/atr,
                    "BINANCE_SPOT_CLOSED_5M",closed,fiveAvailable));
            observations.put("extensionAtr", new Observation(Math.max(0,Math.max(price-resistance,support-price))/atr,
                    "BINANCE_SPOT_CLOSED_5M",closed,fiveAvailable));
            observations.put("volatility5m", new Observation(vectors.get("5m").get(2),"BINANCE_SPOT_CLOSED_5M",closed,fiveAvailable));
        }
        absolute(observations,"fundingRate","absFundingRate");
        absolute(observations,"bookImbalance","absBookImbalance");
        absolute(observations,"openInterestChange1h","absOpenInterestChange1h");
        Observation longShort=observations.get("longShortRatio");
        if(longShort!=null && longShort.value()>0)
            observations.put("absLogLongShortRatio",new Observation(Math.abs(Math.log(longShort.value())),longShort.source(),longShort.observedAt(),longShort.availableAt()));
        Observation longLiq=observations.get("longLiquidation"),shortLiq=observations.get("shortLiquidation");
        if(longLiq!=null && shortLiq!=null && longLiq.value()>=0 && shortLiq.value()>=0) {
            double sum=longLiq.value()+shortLiq.value();
            observations.put("absLiquidationImbalance",new Observation(sum==0?0:Math.abs(longLiq.value()-shortLiq.value())/sum,
                    longLiq.source()+"+"+shortLiq.source(),max(longLiq.observedAt(),shortLiq.observedAt()),max(longLiq.availableAt(),shortLiq.availableAt())));
        }
        List<String> conflictIntervals=List.of("5m","1h","4h");
        if(conflictIntervals.stream().allMatch(interval->vectors.containsKey(interval) && vectors.get(interval).get(4)!=null)) {
            int oppositePairs=0;
            for(int i=0;i<3;i++) for(int j=i+1;j<3;j++)
                if(Math.signum(vectors.get(conflictIntervals.get(i)).get(4))*Math.signum(vectors.get(conflictIntervals.get(j)).get(4))<0) oppositePairs++;
            Instant observed=null,known=null;
            for(String interval:conflictIntervals) {
                List<Bar> intervalBars=validBars.get(interval);
                observed=max(observed,intervalBars.get(intervalBars.size()-1).closeTime());
                for(Bar bar:intervalBars) known=max(known,bar.availableAt());
            }
            observations.put("timeframeConflict",new Observation(oppositePairs/3.0,"BINANCE_SPOT_CLOSED_5M_1H_4H",observed,known));
        }
        String four = trend(vectors.get("4h"));
        String one = "INSUFFICIENT_DATA";
        if (vectors.containsKey("1h") && vectors.containsKey("4h") && vectors.get("1h").get(0)!=null && vectors.get("4h").get(4)!=null) {
            double hourMomentum = vectors.get("1h").get(0), fourSlope = vectors.get("4h").get(4);
            one = hourMomentum * fourSlope < 0 ? "CONFLICT" : hourMomentum * fourSlope > 0 ? "OPPORTUNITY" : "OBSERVATION";
        }
        boolean ready = validBars.size() == INTERVALS.size() && atr != null && atr > 0
                && reasons.stream().noneMatch(s -> s.startsWith("MISSING_OR_INVALID_CORE_EVIDENCE"));
        return new Frame(input.symbol(), closed,input.signalAsOf(),available,FEATURE_VERSION,FEATURE_NAMES,vector,
                ready,reasons,one,four,atr,support,resistance,observations);
    }

    private static List<Bar> validateBars(List<Bar> bars, String interval, Instant asOf) {
        if (bars == null || bars.size() < 24) return List.of();
        List<Bar> selected = bars.subList(bars.size()-24,bars.size());
        long millis = SECONDS.get(interval)*1000;
        Bar previous = null;
        for (Bar bar : selected) {
            if (bar == null || bar.openTime()==null || bar.closeTime()==null || bar.availableAt()==null
                    || bar.closeTime().isAfter(asOf) || bar.availableAt().isAfter(asOf)
                    || bar.availableAt().isBefore(bar.closeTime()) || !bar.openTime().isBefore(bar.closeTime())
                    || Math.abs(Duration.between(bar.openTime(),bar.closeTime()).toMillis()-millis)>1
                    || !Double.isFinite(bar.open()) || !Double.isFinite(bar.high()) || !Double.isFinite(bar.low())
                    || !Double.isFinite(bar.close()) || !Double.isFinite(bar.volume())
                    || bar.low()<=0 || bar.high()<Math.max(bar.open(),bar.close())
                    || bar.low()>Math.min(bar.open(),bar.close()) || bar.volume()<0
                    || (bar.takerBuyBaseVolume()!=null && (!Double.isFinite(bar.takerBuyBaseVolume())
                    || bar.takerBuyBaseVolume()<0 || bar.takerBuyBaseVolume()>bar.volume()))
                    || (bar.tradeCount()!=null && bar.tradeCount()<0)) return List.of();
            if (previous != null && !bar.openTime().equals(previous.openTime().plusMillis(millis))) return List.of();
            previous=bar;
        }
        if (Duration.between(previous.closeTime(),asOf).toMillis()>millis+15_000) return List.of();
        return List.copyOf(selected);
    }

    private static List<Double> barFeatures(List<Bar> b) {
        int n=b.size(); Bar last=b.get(n-1); double atr=atr(b), support=support(b), resistance=resistance(b);
        double[] returns=new double[12];
        double sum=0,volume=0,meanPrice=0;
        for(int i=0;i<12;i++) { returns[i]=Math.log(b.get(n-12+i).close()/b.get(n-13+i).close()); sum+=returns[i]; meanPrice+=b.get(n-12+i).close(); }
        double variance=0; for(double r:returns) variance+=Math.pow(r-sum/12,2);
        double slope=0; for(int i=0;i<12;i++) slope+=(i-5.5)*(b.get(n-12+i).close()-meanPrice/12);
        slope/=143; // sum((0..11 - 5.5)^2)
        for(int i=n-21;i<n-1;i++) volume+=b.get(i).volume();
        return Arrays.asList(last.close()/b.get(n-7).close()-1,atr/last.close(),Math.sqrt(variance/12),
                volume>0?last.volume()/(volume/20):null,atr>0?slope/atr:null,
                atr>0?(last.close()-(support+resistance)/2)/atr:null,
                resistance>support?(last.close()-support)/(resistance-support):null,
                last.volume()>0 && last.takerBuyBaseVolume()!=null?last.takerBuyBaseVolume()/last.volume():null,
                last.tradeCount()==null?null:last.tradeCount().doubleValue());
    }
    private static double atr(List<Bar> b) {
        double total=0; for(int i=b.size()-14;i<b.size();i++) total+=Math.max(b.get(i).high()-b.get(i).low(),
                Math.max(Math.abs(b.get(i).high()-b.get(i-1).close()),Math.abs(b.get(i).low()-b.get(i-1).close())));
        return total/14;
    }
    private static double support(List<Bar> b) { return b.subList(b.size()-20,b.size()).stream().mapToDouble(Bar::low).min().orElseThrow(); }
    private static double resistance(List<Bar> b) { return b.subList(b.size()-20,b.size()).stream().mapToDouble(Bar::high).max().orElseThrow(); }
    private static String trend(List<Double> f) { return f==null||f.get(4)==null?"INSUFFICIENT_DATA":f.get(4)>0?"LONG":f.get(4)<0?"SHORT":"RANGE"; }
    private static boolean usable(Observation o,Instant at) { return o!=null && o.value()!=null && Double.isFinite(o.value())
            && o.source()!=null && !o.source().isBlank() && o.observedAt()!=null && o.availableAt()!=null
            && !o.observedAt().isAfter(at) && !o.availableAt().isAfter(at) && !o.availableAt().isBefore(o.observedAt()); }
    private static Instant max(Instant a,Instant b) { return a==null || b.isAfter(a)?b:a; }
    private static void absolute(Map<String,Observation> inputs,String sourceKey,String targetKey) {
        Observation source=inputs.get(sourceKey);
        if(source!=null) inputs.put(targetKey,new Observation(Math.abs(source.value()),source.source(),source.observedAt(),source.availableAt()));
    }
}
