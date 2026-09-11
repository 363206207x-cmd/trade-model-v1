package org.example.trademodel.assetcard;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Pure, point-in-time Spot feature projection shared with scripts/asset_card_model.py. */
public final class AssetCardFeatureService {
    public static final String FEATURE_VERSION = "SPOT_CARD_FEATURES_V2_SIGNED_PIT";
    public static final String SPOT_SOURCE_VERSION = "BINANCE_SPOT_PUBLIC_V1";
    public static final String ATR_DEFINITION = "5m_TR_SMA14_FIXED_AT_SIGNAL";
    public static final List<String> INTERVALS = List.of("5m", "15m", "1h", "4h");
    private static final Map<String, Long> SECONDS = Map.of("5m",300L,"15m",900L,"1h",3600L,"4h",14400L);
    private static final List<String> BAR_FEATURES = List.of("momentum6", "atrRelative", "volatility12",
            "volumeRatio", "slope12", "centerDistanceAtr", "rangePosition", "takerBuyFraction", "tradeCount");
    public static final List<String> EVIDENCE_FEATURES = List.of("spreadBps", "depth10Bps", "depth25Bps",
            "bookImbalance", "openInterest", "fundingRate", "longShortRatio", "longLiquidation",
            "shortLiquidation");
    public static final List<String> FEATURE_NAMES;
    static {
        List<String> names = new ArrayList<>();
        for (String interval : INTERVALS) for (String feature : BAR_FEATURES) names.add(interval + "." + feature);
        names.addAll(EVIDENCE_FEATURES);
        FEATURE_NAMES = List.copyOf(names);
    }

    public record Bar(Instant openTime, Instant closeTime, Instant availableAt, double open, double high,
                      double low, double close, double volume, Double takerBuyBaseVolume, Long tradeCount) {}
    public record Observation(Double value, String source, Instant observedAt, Instant availableAt,
                              String instrument, String sourceVersion, String unit, Instant expiresAt,
                              String observationId) {
        /** Legacy records remain readable but cannot satisfy the V42 provenance gate. */
        public Observation(Double value,String source,Instant observedAt,Instant availableAt) {
            this(value,source,observedAt,availableAt,null,null,null,null,null);
        }
        public Observation(Double value,String source,Instant observedAt,Instant availableAt,String instrument,
                           String sourceVersion,String unit,Instant expiresAt) {
            this(value,source,observedAt,availableAt,instrument,sourceVersion,unit,expiresAt,null);
        }
    }
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
            if (!usableObservation(input.symbol(),entry.getKey(),observation,input.signalAsOf())) reasons.add("FUTURE_OR_INVALID_EVIDENCE:" + entry.getKey());
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
            observations.put("structuralCenterDistanceAtr", derived(input.symbol(),(price-(support+resistance)/2)/atr,"ATR_MULTIPLE",closed,fiveAvailable));
            observations.put("extensionAtr", derived(input.symbol(),(price>resistance?price-resistance:price<support?price-support:0)/atr,"ATR_MULTIPLE",closed,fiveAvailable));
            observations.put("volatility5m", derived(input.symbol(),vectors.get("5m").get(2),"LOG_RETURN_STD",closed,fiveAvailable));
            observations.put("return5m",derived(input.symbol(),Math.log(price/five.get(five.size()-2).close()),"LOG_RETURN",closed,fiveAvailable));
        }
        Observation longShort=observations.get("longShortRatio");
        if(longShort!=null && longShort.value()>0)
            observations.put("logLongShortRatio",new Observation(Math.log(longShort.value()),longShort.source(),longShort.observedAt(),longShort.availableAt(),
                    longShort.instrument(),longShort.sourceVersion(),"LOG_RATIO",longShort.expiresAt()));
        Observation longLiq=observations.get("longLiquidation"),shortLiq=observations.get("shortLiquidation");
        if(longLiq!=null && shortLiq!=null && longLiq.value()>=0 && shortLiq.value()>=0) {
            double sum=longLiq.value()+shortLiq.value();
            if(Objects.equals(longLiq.instrument(),shortLiq.instrument()) && Objects.equals(longLiq.sourceVersion(),shortLiq.sourceVersion()))
                observations.put("liquidationImbalance",new Observation(sum==0?0:(longLiq.value()-shortLiq.value())/sum,
                    longLiq.source()+"+"+shortLiq.source(),max(longLiq.observedAt(),shortLiq.observedAt()),max(longLiq.availableAt(),shortLiq.availableAt()),
                    longLiq.instrument(),longLiq.sourceVersion(),"RATIO",longLiq.expiresAt().isBefore(shortLiq.expiresAt())?longLiq.expiresAt():shortLiq.expiresAt()));
        }
        for(String interval:List.of("5m","1h","4h")) if(vectors.containsKey(interval) && vectors.get(interval).get(4)!=null) {
            List<Bar> intervalBars=validBars.get(interval); Bar last=intervalBars.get(intervalBars.size()-1);
            Instant known=intervalBars.stream().map(Bar::availableAt).max(Instant::compareTo).orElseThrow();
            observations.put("slope"+interval,derived(input.symbol(),vectors.get(interval).get(4),"ATR_MULTIPLE",last.closeTime(),known,interval));
            if("1h".equals(interval)) observations.put("priceReturn1h",derived(input.symbol(),Math.log(last.close()/intervalBars.get(intervalBars.size()-2).close()),"LOG_RETURN",last.closeTime(),known,interval));
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
            observations.put("timeframeConflict",new Observation(oppositePairs/3.0,"BINANCE_SPOT_CLOSED_5M_1H_4H",observed,known,
                    spotInstrument(input.symbol()),SPOT_SOURCE_VERSION,"RATIO",observed.plusSeconds(315)));
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
                    || Duration.between(bar.openTime(),bar.closeTime()).minusMillis(millis).abs().compareTo(Duration.ofMillis(1))>0
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
        if (Duration.between(previous.closeTime(),asOf).compareTo(Duration.ofMillis(millis+15_000))>0) return List.of();
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
    public static String spotInstrument(String symbol) { return instrument(symbol,false); }
    private static String instrument(String symbol,boolean derivative) {
        if(symbol==null || !symbol.matches("[A-Z0-9]{2,15}USDT")) return null;
        return "BINANCE:"+(derivative?"PERPETUAL:LINEAR:":"SPOT:NONE:")+symbol.substring(0,symbol.length()-4)+"/USDT";
    }
    public static String observationUnit(String key) {
        return switch(key) {
            case "spotPrice","openInterest","longLiquidation","shortLiquidation","depth10Bps","depth25Bps" -> "QUOTE_CURRENCY";
            case "fundingRate" -> "RATE";
            case "openInterestChange1h","crowdingOpenInterestChange1h" -> "PERCENT";
            case "longShortRatio","bookImbalance","liquidationImbalance","timeframeConflict" -> "RATIO";
            case "logLongShortRatio" -> "LOG_RATIO";
            case "spreadBps" -> "BASIS_POINTS";
            case "volatility1m","volatility5m" -> "LOG_RETURN_STD";
            case "return1m","return5m","priceReturn1h" -> "LOG_RETURN";
            case "structuralCenterDistanceAtr","extensionAtr","slope5m","slope1h","slope4h" -> "ATR_MULTIPLE";
            default -> null;
        };
    }
    public static boolean usableObservation(String symbol,String key,Observation o,Instant at) {
        if(o==null || at==null || o.value()==null || !Double.isFinite(o.value()) || o.source()==null
                || o.sourceVersion()==null || o.sourceVersion().isBlank() || Set.of("UNKNOWN","UNVERIFIED").contains(o.sourceVersion())
                || o.unit()==null || !o.unit().equals(observationUnit(key)) || o.observedAt()==null || o.availableAt()==null || o.expiresAt()==null
                || o.observedAt().isAfter(at) || o.availableAt().isAfter(at) || o.availableAt().isBefore(o.observedAt())
                || o.expiresAt().isBefore(at) || o.expiresAt().isBefore(o.availableAt())) return false;
        boolean derivative=Set.of("openInterest","fundingRate","longShortRatio","longLiquidation","shortLiquidation",
                "openInterestChange1h","logLongShortRatio","liquidationImbalance","crowdingOpenInterestChange1h").contains(key);
        if(!Objects.equals(instrument(symbol,derivative),o.instrument()) || o.instrument()==null) return false;
        if(derivative) {
            String dataset=switch(key) { case "fundingRate" -> "COINGLASS_FUNDING"; case "longShortRatio","logLongShortRatio" -> "COINGLASS_LONG_SHORT_RATIO";
                case "longLiquidation","shortLiquidation","liquidationImbalance" -> "COINGLASS_LIQUIDATION"; default -> "COINGLASS_OPEN_INTEREST"; };
            if(!o.source().startsWith("COINGLASS:"+dataset+":")) return false;
        } else if(!Set.of("BINANCE_SPOT","BINANCE_SPOT_AGG_TRADE","BINANCE_SPOT_TRADE","BINANCE_SPOT_DIFF_DEPTH","BINANCE_SPOT_STRUCTURE_AND_TRADE",
                "BINANCE_SPOT_CLOSED_1M","BINANCE_SPOT_CLOSED_5M","BINANCE_SPOT_CLOSED_15M","BINANCE_SPOT_CLOSED_1H","BINANCE_SPOT_CLOSED_4H","BINANCE_SPOT_CLOSED_5M_1H_4H").contains(o.source())) return false;
        if("spotPrice".equals(key)) return o.value()>0 && o.observationId()!=null && !o.observationId().isBlank();
        if(Set.of("spreadBps","depth10Bps","depth25Bps","openInterest","longLiquidation","shortLiquidation","volatility1m","volatility5m").contains(key)) return o.value()>=0;
        if(Set.of("bookImbalance","liquidationImbalance").contains(key)) return o.value()>=-1 && o.value()<=1;
        if("timeframeConflict".equals(key)) return o.value()>=0 && o.value()<=1;
        return !"longShortRatio".equals(key) || o.value()>0;
    }
    private static Observation derived(String symbol,double value,String unit,Instant observed,Instant available) {
        return derived(symbol,value,unit,observed,available,"5m");
    }
    private static Observation derived(String symbol,double value,String unit,Instant observed,Instant available,String interval) {
        return new Observation(value,"BINANCE_SPOT_CLOSED_"+interval.toUpperCase(Locale.ROOT),observed,available,spotInstrument(symbol),SPOT_SOURCE_VERSION,unit,observed.plusSeconds(SECONDS.get(interval)+15));
    }
    private static Instant max(Instant a,Instant b) { return a==null || b.isAfter(a)?b:a; }
}
