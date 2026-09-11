package org.example.trademodel.assetcard;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.Tag("core-regression")
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
            evidence.put(key,spot(key,2.0,at));
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
            evidence.put(key,spot(key,2.0,at));
        var raw=new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,evidence);
        var javaFrame=new AssetCardFeatureService().build(raw);
        var mapper=new ObjectMapper().findAndRegisterModules();
        String script="import importlib.util,json,sys; s=importlib.util.spec_from_file_location('card','scripts/asset_card_model.py'); m=importlib.util.module_from_spec(s); s.loader.exec_module(m); print(json.dumps(m.build_frame(m.decode_json(sys.argv[1]))))";
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
            evidence.put(key,spot(key,2.0,at));
        var service=new AssetCardFeatureService();
        var initial=service.build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,bars,evidence));
        evidence.put("spotPrice",spot("spotPrice",1000.0,at.plusSeconds(10)));
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
    private AssetCardFeatureService.Observation spot(String key,double value,Instant observed) {
        return new AssetCardFeatureService.Observation(value,"BINANCE_SPOT",observed,observed,
                AssetCardFeatureService.spotInstrument("BTCUSDT"),"TEST_FIXTURE_V2",AssetCardFeatureService.observationUnit(key),observed.plusSeconds(60),"spotPrice".equals(key)?"test-trade":null);
    }
    @Test void v42ObservationRequiresCompleteIdentityUnitExpiryAndRetainsSignedFunding() {
        var o=new AssetCardFeatureService.Observation(-.01,"COINGLASS:COINGLASS_FUNDING:test",at,at,
                "BINANCE:PERPETUAL:LINEAR:BTC/USDT","TEST_SOURCE_V2","RATE",at.plusSeconds(60));
        assertThat(AssetCardFeatureService.usableObservation("BTCUSDT","fundingRate",o,at)).isTrue();
        assertThat(AssetCardFeatureService.usableObservation("ETHUSDT","fundingRate",o,at)).isFalse();
        assertThat(AssetCardFeatureService.usableObservation("BTCUSDT","fundingRate",o,at.plusSeconds(61))).isFalse();
        assertThat(AssetCardFeatureService.usableObservation("BTCUSDT","fundingRate",new AssetCardFeatureService.Observation(-.01,o.source(),at,at),at)).isFalse();
        var frame=new AssetCardFeatureService().build(new AssetCardFeatureService.RawFrame("BTCUSDT",at,Map.of(),Map.of("fundingRate",o)));
        assertThat(frame.realInputs().get("fundingRate").value()).isEqualTo(-.01);
        assertThat(frame.realInputs()).doesNotContainKey("absFundingRate");
        assertThat(frame.featureNames()).doesNotContain("takerBuySellRatio");
        assertThat(frame.featureVersion()).isEqualTo("SPOT_CARD_FEATURES_V2_SIGNED_PIT");
    }
    @Test void javaAndPythonRejectTheSameObservationIdentitySourceUnitAndTimeCorruption() throws Exception {
        var mapper=new ObjectMapper().findAndRegisterModules();
        var original=new AssetCardFeatureService.Observation(-.01,"COINGLASS:COINGLASS_FUNDING:fixture",at,at,
                "BINANCE:PERPETUAL:LINEAR:BTC/USDT","TEST_SOURCE_V2","RATE",at.plusSeconds(60));
        List<AssetCardFeatureService.Observation> observations=new ArrayList<>(List.of(original));
        for(var corruption:Map.of("source","COINGLASS:COINGLASS_OPEN_INTEREST:fixture","sourceVersion","UNKNOWN",
                "instrument","BINANCE:SPOT:NONE:BTC/USDT","unit","PERCENT").entrySet()) {
            com.fasterxml.jackson.databind.node.ObjectNode node=mapper.valueToTree(original); node.put(corruption.getKey(),corruption.getValue());
            observations.add(mapper.treeToValue(node,AssetCardFeatureService.Observation.class));
        }
        observations.add(new AssetCardFeatureService.Observation(-.01,original.source(),at,at,original.instrument(),original.sourceVersion(),"RATE",at.minusSeconds(1)));
        observations.add(new AssetCardFeatureService.Observation(-.01,original.source(),at,at.plusSeconds(1),original.instrument(),original.sourceVersion(),"RATE",at.plusSeconds(60)));
        observations.add(new AssetCardFeatureService.Observation(-.01,original.source(),at,at));
        List<Boolean> expected=observations.stream().map(o->AssetCardFeatureService.usableObservation("BTCUSDT","fundingRate",o,at)).toList();
        String script="import json,sys; sys.path.insert(0,'scripts'); import asset_card_model as m; x=m.decode_json(sys.argv[1]); print(json.dumps([m.valid_observation('BTCUSDT','fundingRate',o,m.timestamp(x['at'])) for o in x['observations']]))";
        Process process=new ProcessBuilder("python3","-B","-c",script,mapper.writeValueAsString(Map.of("at",at,"observations",observations))).redirectErrorStream(true).start();
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
        assertThat(mapper.readTree(output)).isEqualTo(mapper.valueToTree(expected));
        assertThat(expected).containsExactly(true,false,false,false,false,false,false,false);
    }
}
