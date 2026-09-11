package org.example.trademodel.assetcard;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

/** Card-only synthetic facts. No Spring context, database, network, training or owner records. */
@org.junit.jupiter.api.Tag("core-regression")
class AssetCardRiskServiceTest {
    private static final Instant AT = Instant.parse("2026-09-10T08:00:00Z");
    private static final String RISK_VERSION = "ISOLATED_V42_RISK_FIXTURE";
    private final AssetCardRiskService service = new AssetCardRiskService();

    @Test void signedEvidenceKeepsNegativeValuesWithoutAbsoluteValueSubstitution() {
        assertTrue(metric(-0.003, "RATE").freshAt(AT));
        assertTrue(metric(-0.6, "RATIO").freshAt(AT));
    }

    @Test void highPercentileShockIsNotByItselfAHardInvalidation() {
        var shock = new AssetCardSnapshot.RiskItem("SHOCK", "ASSESSED", "HIGH", "return5m=-0.04",
                "BINANCE_SPOT", AT, "当前方向不利收益达到历史高风险分位", "LOG_RETURN");
        assertFalse(shock.invalidatesSignal());
    }

    @Test void eightUnknownAssessmentsCannotBecomeLowRisk() {
        var result = service.evaluate(input(Map.of(), Map.of(), null, false, false, null));
        assertEquals(8, result.risk().items().size());
        assertEquals(Set.of("CHASE","SHOCK","REVERSAL","CROWDING","LIQUIDATION","LIQUIDITY","EVENT","DATA"),
                new HashSet<>(result.risk().items().stream().map(AssetCardSnapshot.RiskItem::type).toList()));
        assertTrue(result.risk().items().stream().allMatch(i -> "UNKNOWN".equals(i.assessmentStatus()) && i.level() == null));
        assertNull(result.risk().overallLevel());
        assertFalse(result.invalidate());
        assertTrue(service.activeItems(result.risk()).isEmpty());
    }

    @Test void eachRiskUsesItsOwnEvidenceAndKnownHighDominatesOtherUnknowns() {
        var facts = completeFacts(); var distributions = distributions(facts);
        facts.put("structuralCenterDistanceAtr", metric(75, "ATR_MULTIPLE"));
        facts.put("volatility1m", metric(98, "LOG_RETURN_STD"));
        facts.remove("fundingRate");
        var result = service.evaluate(input(facts, distributions, event("NONE", AT), true, false, null));
        assertEquals("MEDIUM", item(result,"CHASE").level());
        assertEquals("HIGH", item(result,"SHOCK").level());
        assertEquals("UNKNOWN", item(result,"CROWDING").assessmentStatus());
        assertEquals("LOW", item(result,"LIQUIDATION").level());
        assertEquals("HIGH", result.risk().overallLevel());
        assertFalse(result.invalidate(), "An empirical HIGH SHOCK is not a hard safety condition");
        assertTrue(item(result,"CHASE").evidenceValue().contains("structuralCenterDistanceAtr=75.0"));
        assertNotEquals(item(result,"CHASE").source(), item(result,"SHOCK").source());
    }

    @Test void completeNoActiveRiskDistinguishesNoneLowAndUnknown() {
        var facts = completeFacts(); var distributions = distributions(facts);
        var result = service.evaluate(input(facts, distributions, event("NONE", AT), true, false, null));
        assertEquals("NONE", item(result,"EVENT").level());
        assertEquals("NONE", item(result,"DATA").level());
        assertEquals("LOW", item(result,"CHASE").level());
        assertTrue(result.risk().items().stream().allMatch(i -> "ASSESSED".equals(i.assessmentStatus())));
        assertEquals("LOW", result.risk().overallLevel());
        assertTrue(service.activeItems(result.risk()).isEmpty());
        facts.remove("depth25Bps");
        var incomplete = service.evaluate(input(facts, distributions, event("NONE", AT), true, false, null));
        assertEquals("UNKNOWN", item(incomplete,"LIQUIDITY").assessmentStatus());
        assertNull(incomplete.risk().overallLevel());
    }

    @Test void futureUnavailableAndExpiredMetricsNeverBecomeAssessed() {
        for (var invalid : List.of(
                new AssetCardRiskService.Metric(1,"ATR_MULTIPLE","TEST:CHASE",AT.plusSeconds(1),AT.plusSeconds(1),AT.plusSeconds(10)),
                new AssetCardRiskService.Metric(1,"ATR_MULTIPLE","TEST:CHASE",AT,AT.plusSeconds(1),AT.plusSeconds(10)),
                new AssetCardRiskService.Metric(1,"ATR_MULTIPLE","TEST:CHASE",AT.minusSeconds(20),AT.minusSeconds(20),AT.minusSeconds(1)),
                new AssetCardRiskService.Metric(1,"ATR_MULTIPLE","TEST:CHASE",AT,AT.minusSeconds(1),AT.plusSeconds(10)))) {
            var facts = completeFacts(); var distributions = distributions(facts);
            facts.put("extensionAtr", invalid);
            var result = service.evaluate(input(facts, distributions, null, false, false, null));
            assertEquals("UNKNOWN", item(result,"CHASE").assessmentStatus());
            assertNull(item(result,"CHASE").level());
        }
    }

    @Test void incompatibleUnitsAndFutureHistoryFailClosed() {
        var facts = completeFacts(); var distributions = distributions(facts);
        facts.put("spreadBps",metric(1,"QUOTE_CURRENCY"));
        assertEquals("UNKNOWN",item(service.evaluate(input(facts,distributions,null,false,false,null)),"LIQUIDITY").assessmentStatus());
        facts = completeFacts(); distributions = distributions(facts);
        distributions.put("extensionAtr", distribution("extensionAtr","ATR_MULTIPLE",true,AT.plusSeconds(1)));
        assertEquals("UNKNOWN",item(service.evaluate(input(facts,distributions,null,false,false,null)),"CHASE").assessmentStatus());
        distributions = distributions(facts);
        distributions.put("extensionAtr",distribution("extensionAtr","BASIS_POINTS",true,AT.minusSeconds(1)));
        assertEquals("UNKNOWN",item(service.evaluate(input(facts,distributions,null,false,false,null)),"CHASE").assessmentStatus());
    }

    @Test void structuralInvalidationAndSourceLossCannotManufactureOppositeDirection() {
        var result = service.evaluate(input(Map.of(),Map.of(),null,false,true,"现货价跌破信号时结构支撑"));
        assertEquals("HIGH",item(result,"REVERSAL").level());
        assertEquals("HIGH",item(result,"DATA").level());
        assertEquals("UNKNOWN",item(result,"CHASE").assessmentStatus());
        assertTrue(result.invalidate());
        assertEquals("现货价跌破信号时结构支撑",item(result,"REVERSAL").reason());
        assertEquals(0,Arrays.stream(AssetCardRiskService.Result.class.getRecordComponents())
                .filter(c->c.getName().toLowerCase(Locale.ROOT).contains("direction")).count());
    }

    @Test void eventKeepsItsActualGradeAndRequiresAnObservedFact() {
        for (String level : List.of("NONE","LOW","MEDIUM","HIGH")) {
            var fact = event(level,AT);
            var result = service.evaluate(input(Map.of(),Map.of(),fact,false,false,null));
            assertEquals(fact,item(result,"EVENT"));
            assertEquals(Set.of("MEDIUM","HIGH").contains(level)?level:null,result.risk().overallLevel());
        }
        var result = service.evaluate(input(Map.of(),Map.of(),event("HIGH",AT.plusSeconds(1)),false,false,null));
        assertEquals("UNKNOWN",item(result,"EVENT").assessmentStatus());
        assertNull(result.risk().overallLevel());
    }

    @Test void onlyThreeActiveItemsAreOrderedBySeverityInvalidationAndFreshness() {
        var items = List.of(
                new AssetCardSnapshot.RiskItem("CHASE","ASSESSED","HIGH","1","TEST:CHASE",AT,"chase"),
                new AssetCardSnapshot.RiskItem("EVENT","ASSESSED","MEDIUM","event","TEST:EVENT",AT,"event"),
                new AssetCardSnapshot.RiskItem("DATA","ASSESSED","HIGH","lost","TEST:DATA",AT.minusSeconds(1),"loss","SOURCE_STATE",true),
                new AssetCardSnapshot.RiskItem("SHOCK","ASSESSED","HIGH","1","TEST:SHOCK",AT,"shock"),
                new AssetCardSnapshot.RiskItem("LIQUIDITY","ASSESSED","HIGH","1","TEST:BOOK",AT.minusSeconds(2),"depth","QUOTE_CURRENCY",true),
                AssetCardSnapshot.RiskItem.unknown(AssetCardSnapshot.RiskType.CROWDING,"missing"));
        var active = service.activeItems(new AssetCardSnapshot.Risk("HIGH",items,AT));
        assertEquals(3,active.size());
        assertEquals(List.of("DATA","LIQUIDITY","CHASE"),active.stream().map(AssetCardSnapshot.RiskItem::type).toList());
        assertTrue(active.stream().allMatch(i->"ASSESSED".equals(i.assessmentStatus()) && "HIGH".equals(i.level())));
        assertEquals(6,items.size());
    }

    @Test void opportunityAndCanonicalScoresAreNotRiskInputs() {
        assertTrue(Arrays.stream(AssetCardRiskService.Input.class.getRecordComponents())
                .noneMatch(c->c.getName().matches("(?i).*(opportunity|confidence|canonical|score).*")));
        var facts = completeFacts(); var distributions = distributions(facts);
        var baseline = service.evaluate(input(facts,distributions,event("NONE",AT),true,false,null));
        facts.put("opportunityScore",metric(100,"SCORE"));
        facts.put("finalConfidence",metric(100,"PERCENT"));
        distributions.put("opportunityScore",distribution("opportunityScore","SCORE",true,AT.minusSeconds(1)));
        assertEquals(baseline,service.evaluate(input(facts,distributions,event("NONE",AT),true,false,null)));
    }

    @Test void eightDirectionsMapToThreeSidesAndNeutralStatesNeverHaveNumericConfidence() {
        for (var direction : AssetCardSnapshot.Direction.values()) {
            var expected = direction.longSide() ? AssetCardSnapshot.SignalSide.LONG
                    : direction.shortSide() ? AssetCardSnapshot.SignalSide.SHORT : AssetCardSnapshot.SignalSide.NON_DIRECTIONAL;
            assertEquals(expected,direction.signalSide());
            var signal = signal(direction);
            if (!direction.directional()) assertNull(signal.calibratedConfidence());
        }
    }

    @Test void identicalSignedFactsHaveOppositeAdverseMeaningsForLongAndShort() {
        Map<String,String> riskType = Map.ofEntries(
                Map.entry("structuralCenterDistanceAtr","CHASE"),Map.entry("extensionAtr","CHASE"),
                Map.entry("fundingRate","CROWDING"),Map.entry("logLongShortRatio","CROWDING"),
                Map.entry("liquidationImbalance","LIQUIDATION"),Map.entry("bookImbalance","LIQUIDITY"),
                Map.entry("return1m","SHOCK"),Map.entry("return5m","SHOCK"),
                Map.entry("slope5m","REVERSAL"),Map.entry("slope1h","REVERSAL"),Map.entry("slope4h","REVERSAL"));
        for (var entry : riskType.entrySet()) for (double extreme : List.of(-99.0,99.0)) {
            var facts = centeredFacts();
            facts.put(entry.getKey(),metric(extreme,facts.get(entry.getKey()).unit()));
            var longResult = service.evaluate(input(AssetCardSnapshot.Direction.LONG,facts,
                    sideDistributions(facts,AssetCardSnapshot.SignalSide.LONG),event("NONE",AT),true,false,null));
            var shortResult = service.evaluate(input(AssetCardSnapshot.Direction.SHORT,facts,
                    sideDistributions(facts,AssetCardSnapshot.SignalSide.SHORT),event("NONE",AT),true,false,null));
            boolean adverseLong = higherForLong(entry.getKey()) == (extreme > 0);
            assertEquals(adverseLong ? "HIGH" : "LOW",item(longResult,entry.getValue()).level(),entry.getKey()+" LONG "+extreme);
            assertEquals(adverseLong ? "LOW" : "HIGH",item(shortResult,entry.getValue()).level(),entry.getKey()+" SHORT "+extreme);
            assertFalse(longResult.invalidate());
            assertFalse(shortResult.invalidate());
        }
    }

    @Test void oiCannotRaiseCrowdingWithoutTheSameSidePriceMovement() {
        var facts = completeFacts(); facts.put("openInterestChange1h",metric(99,"PERCENT"));
        var history = distributions(facts);
        assertEquals("HIGH",item(service.evaluate(input(facts,history,null,true,false,null)),"CROWDING").level());
        facts.put("priceReturn1h",metric(-.01,"LOG_RETURN"));
        assertEquals("LOW",item(service.evaluate(input(facts,history,null,true,false,null)),"CROWDING").level());
        facts.remove("priceReturn1h");
        assertEquals("UNKNOWN",item(service.evaluate(input(facts,history,null,true,false,null)),"CROWDING").assessmentStatus());
    }

    @Test void distributionAssetSideMetricAndVersionMustAllMatch() {
        var facts = completeFacts();
        var baseline = distributions(facts).get("fundingRate");
        for (String mismatch : List.of("symbol","side","metric","version")) {
            var history = distributions(facts);
            history.put("fundingRate",new AssetCardModelBundle.RiskDistribution(baseline.sortedValues(),baseline.source(),baseline.asOf(),
                    baseline.samples(),baseline.mediumPercentile(),baseline.highPercentile(),baseline.minSamples(),baseline.unit(),baseline.higherIsWorse(),
                    mismatch.equals("symbol")?"ETHUSDT":"BTCUSDT",mismatch.equals("side")?"SHORT":"LONG",
                    mismatch.equals("metric")?"logLongShortRatio":"fundingRate",mismatch.equals("version")?"OTHER_VERSION":RISK_VERSION));
            assertEquals("UNKNOWN",item(service.evaluate(input(facts,history,null,true,false,null)),"CROWDING").assessmentStatus(),mismatch);
        }
    }

    @Test void rangeAndWatchDoNotInheritLongEvidenceOrOldStructuralInvalidation() {
        var facts = completeFacts(); facts.put("fundingRate",metric(99,"RATE"));
        for (var direction : List.of(AssetCardSnapshot.Direction.RANGE,AssetCardSnapshot.Direction.WATCH)) {
            var result = service.evaluate(input(direction,facts,distributions(facts),event("NONE",AT),true,false,"旧多头结构条件"));
            for (String type : List.of("CHASE","REVERSAL","CROWDING","LIQUIDATION"))
                assertEquals("UNKNOWN",item(result,type).assessmentStatus(),type);
            assertFalse(result.invalidate());
            assertNull(result.risk().overallLevel());
            assertTrue(result.risk().matchesBasis(signal(direction)));
            assertFalse(result.risk().matchesBasis(signal(AssetCardSnapshot.Direction.LONG)));
        }
    }

    @Test void currentRiskBindingRejectsDifferentDirectionClockAndLegacyUnboundRecords() {
        var result = service.evaluate(input(completeFacts(),distributions(completeFacts()),event("NONE",AT),true,false,null));
        assertEquals(AssetCardSnapshot.SignalSide.LONG,result.risk().riskBasisSide());
        assertEquals(AssetCardSnapshot.Direction.LONG,result.risk().riskBasisDirection());
        assertEquals(AT.minusSeconds(300),result.risk().riskBasisSignalAsOf());
        assertEquals(AT,result.risk().riskMarketAsOf());
        assertEquals(RISK_VERSION,result.risk().riskVersion());
        assertTrue(result.risk().matchesBasis(signal(AssetCardSnapshot.Direction.LONG)));
        assertFalse(result.risk().matchesBasis(signal(AssetCardSnapshot.Direction.WEAK_LONG)));
        var differentRun = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG,"VALID",null,.7,.4,"1h","4h",AT);
        assertFalse(result.risk().matchesBasis(differentRun));
        assertFalse(new AssetCardSnapshot.Risk("HIGH",result.risk().items(),AT).matchesBasis(signal(AssetCardSnapshot.Direction.LONG)));
        var pending = AssetCardSnapshot.Risk.unknownFor(signal(AssetCardSnapshot.Direction.SHORT),RISK_VERSION,"正在重算当前方向风险");
        assertTrue(pending.matchesBasis(signal(AssetCardSnapshot.Direction.SHORT)));
        assertTrue(pending.items().stream().allMatch(i->"UNKNOWN".equals(i.assessmentStatus())));
        assertNull(pending.riskMarketAsOf());
    }

    @Test void mismatchedInputSideAndFutureSignalIdentityAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new AssetCardRiskService.Input("BTCUSDT",AT,
                AssetCardSnapshot.SignalSide.SHORT,AssetCardSnapshot.Direction.LONG,AT,RISK_VERSION,Map.of(),Map.of(),null,false,false,null));
        assertThrows(IllegalArgumentException.class,()->new AssetCardRiskService.Input("BTCUSDT",AT,
                AssetCardSnapshot.SignalSide.LONG,AssetCardSnapshot.Direction.LONG,AT.plusSeconds(1),RISK_VERSION,Map.of(),Map.of(),null,false,false,null));
    }

    @Test void directionalRiskCannotMatchOrEvaluateWithoutASignalClock() {
        var signal = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG,"VALID",null,.7,.4,"1h","4h",null);
        var risk = new AssetCardSnapshot.Risk("HIGH",List.of(),AT,AssetCardSnapshot.SignalSide.LONG,
                AssetCardSnapshot.Direction.LONG,null,AT,RISK_VERSION);
        assertFalse(risk.matchesBasis(signal));
        var result = service.evaluate(new AssetCardRiskService.Input("BTCUSDT",AT,AssetCardSnapshot.SignalSide.LONG,
                AssetCardSnapshot.Direction.LONG,null,RISK_VERSION,completeFacts(),distributions(completeFacts()),
                event("HIGH",AT),true,true,"structural breach"));
        assertNull(result.risk().overallLevel());
        assertTrue(result.risk().items().stream().allMatch(i->"UNKNOWN".equals(i.assessmentStatus())));
        assertFalse(result.invalidate());
        var startup = service.evaluate(new AssetCardRiskService.Input("BTCUSDT",AT,AssetCardSnapshot.SignalSide.NON_DIRECTIONAL,
                null,null,RISK_VERSION,Map.of(),Map.of(),null,false,true,null));
        assertEquals("HIGH",item(startup,"DATA").level());
        assertTrue(startup.invalidate(),"First startup health must not require an invented directional signal timestamp");
    }

    @Test void sourceLossIsSideNeutralHighAndCannotInventMarketObservationTime() {
        var result = service.evaluate(input(AssetCardSnapshot.Direction.WATCH,Map.of(),Map.of(),null,false,true,null));
        assertEquals("HIGH",result.risk().overallLevel());
        assertTrue(result.invalidate());
        assertTrue(item(result,"DATA").hardInvalidation());
        assertNull(result.risk().riskMarketAsOf());
        assertEquals(AT,item(result,"DATA").asOf());
    }

    @Test void nonRiskInputsCannotAdvanceTheRiskMarketClock() {
        var facts = completeFacts();
        facts.replaceAll((key,fact)->new AssetCardRiskService.Metric(fact.value(),fact.unit(),fact.source(),
                AT.minusSeconds(2),AT.minusSeconds(2),AT.plusSeconds(10)));
        var history = distributions(facts);
        var before = service.evaluate(input(facts,history,event("NONE",AT),true,false,null));
        facts.put("opportunityScore",metric(100,"SCORE"));
        facts.put("finalConfidence",metric(100,"PERCENT"));
        facts.put("absFundingRate",metric(99,"RATE"));
        var after = service.evaluate(input(facts,history,event("NONE",AT),true,false,null));
        assertEquals(AT.minusSeconds(2),after.risk().riskMarketAsOf());
        assertEquals(before,after);
    }

    @Test void wrongTailPolicyAndImpossibleUnsignedMetricsFailClosed() {
        var facts = completeFacts();
        var history = distributions(facts);
        history.put("return1m",distribution("return1m","LOG_RETURN",true,AT.minusSeconds(1)));
        assertEquals("UNKNOWN",item(service.evaluate(input(facts,history,null,true,false,null)),"SHOCK").assessmentStatus());
        history = distributions(facts);
        facts.put("depth10Bps",metric(-1,"QUOTE_CURRENCY"));
        assertEquals("UNKNOWN",item(service.evaluate(input(facts,history,null,true,false,null)),"LIQUIDITY").assessmentStatus());
    }

    @Test void legacyInputWithoutRiskIdentityNeverBecomesCurrentRisk() {
        var facts = completeFacts();
        var legacy = new AssetCardRiskService.Input("BTCUSDT",AT,facts,distributions(facts),event("HIGH",AT),true,true,"old breach");
        var result = service.evaluate(legacy);
        assertFalse(result.risk().matchesBasis(signal(AssetCardSnapshot.Direction.LONG)));
        assertTrue(result.risk().items().stream().allMatch(i->"UNKNOWN".equals(i.assessmentStatus())));
        assertFalse(result.invalidate());
    }

    private static AssetCardSnapshot.Signal signal(AssetCardSnapshot.Direction direction) {
        return new AssetCardSnapshot.Signal(direction,"VALID",null,.7,.4,"1h","4h",AT.minusSeconds(300));
    }

    private static boolean higherForLong(String key) {
        return Set.of("structuralCenterDistanceAtr","extensionAtr","fundingRate","logLongShortRatio","liquidationImbalance").contains(key);
    }

    private static boolean signed(String key) {
        return higherForLong(key) || Set.of("return1m","return5m","slope5m","slope1h","slope4h","bookImbalance").contains(key);
    }

    private static Map<String,AssetCardRiskService.Metric> centeredFacts() {
        var facts = completeFacts();
        facts.replaceAll((key,fact)->signed(key)?metric(0,fact.unit()):fact);
        return facts;
    }

    private static Map<String,AssetCardModelBundle.RiskDistribution> sideDistributions(Map<String,AssetCardRiskService.Metric> facts,AssetCardSnapshot.SignalSide side) {
        Map<String,AssetCardModelBundle.RiskDistribution> result = new HashMap<>();
        facts.forEach((rawKey,fact)-> {
            String key = "openInterestChange1h".equals(rawKey)?"crowdingOpenInterestChange1h":rawKey;
            boolean higher = signed(key) ? (side==AssetCardSnapshot.SignalSide.LONG)==higherForLong(key) : !key.startsWith("depth");
            var values = IntStream.rangeClosed(signed(key)?-100:0,100).mapToObj(i->(double)i).toList();
            result.put(key,new AssetCardModelBundle.RiskDistribution(values,"ISOLATED_SIGNED_HISTORY",AT.minusSeconds(1),values.size(),
                    .70,.90,100,fact.unit(),higher,"BTCUSDT",side.name(),key,RISK_VERSION));
        });
        return result;
    }

    private AssetCardRiskService.Input input(Map<String,AssetCardRiskService.Metric> facts,
                Map<String,AssetCardModelBundle.RiskDistribution> distributions,AssetCardSnapshot.RiskItem event,
                boolean complete,boolean lost,String breach) {
        return input(AssetCardSnapshot.Direction.LONG, facts, distributions, event, complete, lost, breach);
    }
    private AssetCardRiskService.Input input(AssetCardSnapshot.Direction direction, Map<String,AssetCardRiskService.Metric> facts,
                Map<String,AssetCardModelBundle.RiskDistribution> distributions,AssetCardSnapshot.RiskItem event,
                boolean complete,boolean lost,String breach) {
        return new AssetCardRiskService.Input("BTCUSDT",AT,direction.signalSide(),direction,AT.minusSeconds(300),
                RISK_VERSION,facts,distributions,event,complete,lost,breach);
    }
    private static AssetCardSnapshot.RiskItem item(AssetCardRiskService.Result result,String type) {
        return result.risk().items().stream().filter(i->type.equals(i.type())).findFirst().orElseThrow();
    }
    private static AssetCardSnapshot.RiskItem event(String level,Instant at) {
        return new AssetCardSnapshot.RiskItem("EVENT","ASSESSED",level,"isolated-event","TEST:EVENT",at,"已核实事件证据");
    }
    private static AssetCardRiskService.Metric metric(double value,String unit) {
        return new AssetCardRiskService.Metric(value,unit,"TEST:"+unit,AT,AT,AT.plusSeconds(10));
    }
    private static Map<String,AssetCardRiskService.Metric> completeFacts() {
        Map<String,AssetCardRiskService.Metric> facts = new LinkedHashMap<>();
        Map.of("structuralCenterDistanceAtr","ATR_MULTIPLE","extensionAtr","ATR_MULTIPLE",
                "volatility1m","LOG_RETURN_STD","volatility5m","LOG_RETURN_STD",
                "fundingRate","RATE","logLongShortRatio","LOG_RATIO",
                "openInterestChange1h","PERCENT","liquidationImbalance","RATIO")
                .forEach((key,unit)->facts.put(key,metric(1,unit)));
        Map.of("return1m","LOG_RETURN","return5m","LOG_RETURN","slope5m","ATR_MULTIPLE",
                "slope1h","ATR_MULTIPLE","slope4h","ATR_MULTIPLE","bookImbalance","RATIO")
                .forEach((key,unit)->facts.put(key,metric(99,unit)));
        facts.put("priceReturn1h",metric(.01,"LOG_RETURN"));
        for(String key:List.of("longLiquidation","shortLiquidation")) facts.put(key,metric(1,"QUOTE_CURRENCY"));
        for(String key:List.of("depth10Bps","depth25Bps")) facts.put(key,metric(99,"QUOTE_CURRENCY"));
        facts.put("spreadBps",metric(1,"BASIS_POINTS"));
        return facts;
    }
    private static Map<String,AssetCardModelBundle.RiskDistribution> distributions(Map<String,AssetCardRiskService.Metric> facts) {
        Map<String,AssetCardModelBundle.RiskDistribution> result = new HashMap<>();
        facts.forEach((key,fact)-> {
            String distributionKey = "openInterestChange1h".equals(key) ? "crowdingOpenInterestChange1h" : key;
            boolean higher = !key.startsWith("depth") && !Set.of("return1m","return5m","slope5m","slope1h","slope4h","bookImbalance").contains(key);
            result.put(distributionKey,distribution(distributionKey,fact.unit(),higher,AT.minusSeconds(1)));
        });
        return result;
    }
    private static AssetCardModelBundle.RiskDistribution distribution(String key,String unit,boolean higher,Instant at) {
        // Test-only percentile fixture. These values cannot create a production bundle.
        return new AssetCardModelBundle.RiskDistribution(IntStream.range(0,100).mapToObj(i->(double)i).toList(),
                "TEST_HISTORICAL_DISTRIBUTION",at,100,.70,.90,100,unit,higher,
                "BTCUSDT","LONG",key,RISK_VERSION);
    }
}
