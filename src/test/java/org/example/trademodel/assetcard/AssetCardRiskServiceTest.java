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
    private final AssetCardRiskService service = new AssetCardRiskService();

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
        facts.remove("absFundingRate");
        var result = service.evaluate(input(facts, distributions, event("NONE", AT), true, false, null));
        assertEquals("MEDIUM", item(result,"CHASE").level());
        assertEquals("HIGH", item(result,"SHOCK").level());
        assertEquals("UNKNOWN", item(result,"CROWDING").assessmentStatus());
        assertEquals("LOW", item(result,"LIQUIDATION").level());
        assertEquals("HIGH", result.risk().overallLevel());
        assertTrue(result.invalidate());
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
        distributions.put("extensionAtr", distribution("ATR_MULTIPLE",true,AT.plusSeconds(1)));
        assertEquals("UNKNOWN",item(service.evaluate(input(facts,distributions,null,false,false,null)),"CHASE").assessmentStatus());
        distributions = distributions(facts);
        distributions.put("extensionAtr",distribution("BASIS_POINTS",true,AT.minusSeconds(1)));
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
                new AssetCardSnapshot.RiskItem("DATA","ASSESSED","HIGH","lost","TEST:DATA",AT.minusSeconds(1),"loss"),
                new AssetCardSnapshot.RiskItem("SHOCK","ASSESSED","HIGH","1","TEST:SHOCK",AT,"shock"),
                new AssetCardSnapshot.RiskItem("LIQUIDITY","ASSESSED","HIGH","1","TEST:BOOK",AT.minusSeconds(2),"depth"),
                AssetCardSnapshot.RiskItem.unknown(AssetCardSnapshot.RiskType.CROWDING,"missing"));
        var active = service.activeItems(new AssetCardSnapshot.Risk("HIGH",items,AT));
        assertEquals(3,active.size());
        assertEquals(List.of("SHOCK","DATA","LIQUIDITY"),active.stream().map(AssetCardSnapshot.RiskItem::type).toList());
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
        distributions.put("opportunityScore",distribution("SCORE",true,AT.minusSeconds(1)));
        assertEquals(baseline,service.evaluate(input(facts,distributions,event("NONE",AT),true,false,null)));
    }

    private AssetCardRiskService.Input input(Map<String,AssetCardRiskService.Metric> facts,
                Map<String,AssetCardModelBundle.RiskDistribution> distributions,AssetCardSnapshot.RiskItem event,
                boolean complete,boolean lost,String breach) {
        return new AssetCardRiskService.Input("BTCUSDT",AT,facts,distributions,event,complete,lost,breach);
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
                "timeframeConflict","RATIO","absFundingRate","RATE","absLogLongShortRatio","LOG_RATIO",
                "absOpenInterestChange1h","PERCENT","absLiquidationImbalance","RATIO","absBookImbalance","RATIO")
                .forEach((key,unit)->facts.put(key,metric(1,unit)));
        for(String key:List.of("longLiquidation","shortLiquidation")) facts.put(key,metric(1,"QUOTE_CURRENCY"));
        for(String key:List.of("depth10Bps","depth25Bps")) facts.put(key,metric(99,"QUOTE_CURRENCY"));
        facts.put("spreadBps",metric(1,"BASIS_POINTS"));
        return facts;
    }
    private static Map<String,AssetCardModelBundle.RiskDistribution> distributions(Map<String,AssetCardRiskService.Metric> facts) {
        Map<String,AssetCardModelBundle.RiskDistribution> result = new HashMap<>();
        facts.forEach((key,fact)->result.put(key,distribution(fact.unit(),!key.startsWith("depth"),AT.minusSeconds(1))));
        return result;
    }
    private static AssetCardModelBundle.RiskDistribution distribution(String unit,boolean higher,Instant at) {
        // Test-only percentile fixture. These values cannot create a production bundle.
        return new AssetCardModelBundle.RiskDistribution(IntStream.range(0,100).mapToObj(i->(double)i).toList(),
                "TEST_HISTORICAL_DISTRIBUTION",at,100,.70,.90,100,unit,higher);
    }
}
