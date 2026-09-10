package org.example.trademodel.assetcard;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

class AssetCardFeatureServiceTest {
    private final Instant at = Instant.parse("2026-01-01T12:00:00Z");
    @Test void missingHistoryIsUnknownNotZeroAndFutureAvailabilityIsRejected() {
        var input = new AssetCardFeatureService.RawFrame("BTCUSDT", at, Map.of(), Map.of(
                "fundingRate", new AssetCardFeatureService.Observation(.01, "COINGLASS", at.minusSeconds(60), at.plusSeconds(1))));
        var frame = new AssetCardFeatureService().build(input);
        assertThat(frame.ready()).isFalse();
        assertThat(frame.atr()).isNull();
        assertThat(frame.vector()).containsNull();
        assertThat(frame.realInputs()).doesNotContainKey("fundingRate");
        assertThat(frame.reasons()).contains("FUTURE_OR_INVALID_EVIDENCE:fundingRate");
    }
    @Test void fixedFeatureDefinitionIsDeterministicAndRequiresClosedContiguousBars() {
        Map<String,List<AssetCardFeatureService.Bar>> bars = new HashMap<>();
        for (var interval : Map.of("5m", 300, "15m", 900, "1h", 3600, "4h", 14400).entrySet()) {
            List<AssetCardFeatureService.Bar> values = new ArrayList<>();
            for (int i=0;i<24;i++) {
                Instant close = at.minusSeconds((long)(23-i)*interval.getValue());
                values.add(new AssetCardFeatureService.Bar(close.minusSeconds(interval.getValue()), close,
                        close, 100+i, 102+i, 99+i, 101+i, 10+i, 5.0+i/2.0, 100L+i));
            }
            bars.put(interval.getKey(), values);
        }
        Map<String,AssetCardFeatureService.Observation> evidence = new HashMap<>();
        for (String key : List.of("spreadBps", "depth10Bps", "depth25Bps"))
            evidence.put(key,new AssetCardFeatureService.Observation(2.0,"BINANCE_SPOT",at,at));
        var frame = new AssetCardFeatureService().build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,evidence));
        assertThat(frame.ready()).isTrue();
        assertThat(frame.atr()).isEqualTo(3.0);
        assertThat(frame.structuralSupport()).isEqualTo(103.0);
        assertThat(frame.structuralResistance()).isEqualTo(125.0);
        assertThat(frame.vector().get(frame.featureNames().indexOf("5m.momentum6"))).isCloseTo(124.0/118-1,within(1e-12));
        assertThat(frame.fourHourTrend()).isEqualTo("LONG");
        var broken = new ArrayList<>(bars.get("5m")); broken.remove(20); bars.put("5m", broken);
        assertThat(new AssetCardFeatureService().build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,evidence)).ready()).isFalse();
    }
    @Test void javaAndOfflinePythonFeatureVectorsUseExactlyTheSameDefinition() throws Exception {
        Map<String,List<AssetCardFeatureService.Bar>> bars=new LinkedHashMap<>();
        for(var interval:Map.of("5m",300,"15m",900,"1h",3600,"4h",14400).entrySet()) {
            List<AssetCardFeatureService.Bar> values=new ArrayList<>();
            for(int i=0;i<24;i++) {
                Instant close=at.minusSeconds((long)(23-i)*interval.getValue());
                values.add(new AssetCardFeatureService.Bar(close.minusSeconds(interval.getValue()),close,close,
                        100+i,102+i,99+i,101+i,10+i,5.0+i/2.0,100L+i));
            }
            bars.put(interval.getKey(),values);
        }
        Map<String,AssetCardFeatureService.Observation> evidence=new HashMap<>();
        for(String key:List.of("spreadBps","depth10Bps","depth25Bps"))
            evidence.put(key,new AssetCardFeatureService.Observation(2.0,"BINANCE_SPOT",at,at));
        var raw=new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,evidence);
        var javaFrame=new AssetCardFeatureService().build(raw);
        var mapper=new ObjectMapper().findAndRegisterModules();
        String script="import importlib.util,json,sys; s=importlib.util.spec_from_file_location('card','scripts/asset_card_model.py'); m=importlib.util.module_from_spec(s); s.loader.exec_module(m); print(json.dumps(m.build_frame(json.loads(sys.argv[1]))))";
        Process process=new ProcessBuilder("python3","-B","-c",script,mapper.writeValueAsString(raw)).redirectErrorStream(true).start();
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
        var python=mapper.readTree(output);
        assertThat(python.path("featureNames").toString()).isEqualTo(mapper.valueToTree(javaFrame.featureNames()).toString());
        for(int i=0;i<javaFrame.vector().size();i++) {
            if(javaFrame.vector().get(i)==null) assertThat(python.path("vector").get(i).isNull()).isTrue();
            else assertThat(python.path("vector").get(i).asDouble()).isCloseTo(javaFrame.vector().get(i),within(1e-12));
        }
        assertThat(python.path("oneHourState").asText()).isEqualTo(javaFrame.oneHourState());
        assertThat(python.path("fourHourTrend").asText()).isEqualTo(javaFrame.fourHourTrend());
        assertThat(python.path("realInputs").path("structuralCenterDistanceAtr").path("value").asDouble())
                .isCloseTo(javaFrame.realInputs().get("structuralCenterDistanceAtr").value(),within(1e-12));
        assertThat(python.path("realInputs").path("timeframeConflict").path("value").asDouble())
                .isEqualTo(javaFrame.realInputs().get("timeframeConflict").value());
    }
    @Test void intrabarPriceCannotChangeClosedOneHourOrFourHourFeaturesAndAvailabilityCannotBeBackdated() {
        Map<String,List<AssetCardFeatureService.Bar>> bars=new HashMap<>();
        for(var interval:Map.of("5m",300,"15m",900,"1h",3600,"4h",14400).entrySet()) {
            List<AssetCardFeatureService.Bar> values=new ArrayList<>();
            for(int i=0;i<24;i++) {
                Instant close=at.minusSeconds((long)(23-i)*interval.getValue());
                values.add(new AssetCardFeatureService.Bar(close.minusSeconds(interval.getValue()),close,close,
                        100+i,102+i,99+i,101+i,10+i,5.0+i/2.0,100L+i));
            }
            bars.put(interval.getKey(),values);
        }
        Map<String,AssetCardFeatureService.Observation> evidence=new HashMap<>();
        for(String key:List.of("spreadBps","depth10Bps","depth25Bps"))
            evidence.put(key,new AssetCardFeatureService.Observation(2.0,"BINANCE_SPOT",at,at));
        var service=new AssetCardFeatureService();
        var initial=service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,evidence));
        evidence.put("spotPrice",new AssetCardFeatureService.Observation(1000.0,"BINANCE_SPOT",at.plusSeconds(10),at.plusSeconds(10)));
        var intrabar=service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at.plusSeconds(10),bars,evidence));
        assertThat(intrabar.signalAsOf()).isEqualTo(at.plusSeconds(10));
        assertThat(intrabar.closed5mAt()).isEqualTo(at);
        assertThat(intrabar.realInputs().get("volatility5m").availableAt()).isEqualTo(initial.realInputs().get("volatility5m").availableAt());
        for(String interval:List.of("1h","4h")) {
            for(int i=0;i<initial.featureNames().size();i++) if(initial.featureNames().get(i).startsWith(interval+"."))
                assertThat(intrabar.vector().get(i)).isEqualTo(initial.vector().get(i));
            var modified=new HashMap<>(bars);
            var higher=new ArrayList<>(bars.get(interval)); var last=higher.get(23);
            higher.set(23,new AssetCardFeatureService.Bar(last.openTime(),last.closeTime(),at.plusSeconds(11),
                    last.open(),last.high(),last.low(),last.close(),last.volume(),last.takerBuyBaseVolume(),last.tradeCount()));
            modified.put(interval,higher);
            var unavailable=service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at.plusSeconds(10),modified,evidence));
            assertThat(unavailable.ready()).isFalse();
            assertThat(unavailable.reasons()).contains("MISSING_OR_INVALID_CLOSED_BARS:"+interval);
            var available=service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at.plusSeconds(11),modified,evidence));
            assertThat(available.ready()).isTrue();
        }
    }
    @Test void timeframeConflictCountsOppositeClosedSlopeSignsNotRiskGrades() {
        Map<String,List<AssetCardFeatureService.Bar>> bars=new HashMap<>();
        bars.put("5m",slopeBars(300,1)); bars.put("15m",slopeBars(900,1));
        bars.put("1h",slopeBars(3600,-1)); bars.put("4h",slopeBars(14400,1));
        var service=new AssetCardFeatureService();
        var mixed=service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,Map.of()));
        assertThat(mixed.realInputs().get("timeframeConflict").value()).isEqualTo(2.0/3);
        assertThat(mixed.realInputs().get("timeframeConflict").source()).isEqualTo("BINANCE_SPOT_CLOSED_5M_1H_4H");
        bars.put("1h",slopeBars(3600,0)); bars.put("4h",slopeBars(14400,-1));
        assertThat(service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,Map.of())).realInputs()
                .get("timeframeConflict").value()).isEqualTo(1.0/3);
        bars.put("4h",slopeBars(14400,1));
        assertThat(service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,Map.of())).realInputs()
                .get("timeframeConflict").value()).isZero();
        bars.remove("4h");
        assertThat(service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,Map.of())).realInputs())
                .doesNotContainKey("timeframeConflict");
    }
    private List<AssetCardFeatureService.Bar> slopeBars(long seconds,int sign) {
        List<AssetCardFeatureService.Bar> values=new ArrayList<>();
        for(int i=0;i<24;i++) {
            Instant close=at.minusSeconds((23-i)*seconds); double price=150+sign*i;
            values.add(new AssetCardFeatureService.Bar(close.minusSeconds(seconds),close,close,
                    price,price+2,price-1,price,10,5.0,100L));
        }
        return values;
    }
}
