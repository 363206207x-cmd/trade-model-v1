package org.example.trademodel.dto.planboundary;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryCandidateDTOTest {

    @Test
    void validFactoryShouldCreateValidCandidate() {
        BoundaryEntryDTO entry = validEntry();
        BoundaryStopDTO stop = validStop();
        BoundaryTakeProfitLevelDTO takeProfit = validTakeProfitLevel();
        BoundarySourceFieldsDTO sourceFields = validSourceFields();
        BigDecimal dataQualityScore = BigDecimal.valueOf(90);

        BoundaryCandidateDTO candidate = BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                stop,
                List.of(takeProfit),
                sourceFields,
                dataQualityScore
        );

        assertEquals("BTCUSDT", candidate.getSymbol());
        assertEquals("1h", candidate.getTimeframe());
        assertEquals(BoundaryStatusEnum.VALID, candidate.getBoundaryStatus());
        assertEquals(entry, candidate.getEntry());
        assertEquals(stop, candidate.getStop());
        assertEquals(List.of(takeProfit), candidate.getTakeProfitLevels());
        assertEquals(sourceFields, candidate.getSourceFields());
        assertEquals(dataQualityScore, candidate.getDataQualityScore());
        assertTrue(candidate.isManualReviewRequired());
        assertTrue(candidate.isNotTradeInstruction());
        assertTrue(candidate.getBlockingReasons().isEmpty());
    }

    @Test
    void validFactoryShouldRejectMissingRequiredFields() {
        BoundaryEntryDTO entry = validEntry();
        BoundaryStopDTO stop = validStop();
        List<BoundaryTakeProfitLevelDTO> takeProfitLevels = List.of(validTakeProfitLevel());
        BoundarySourceFieldsDTO sourceFields = validSourceFields();
        BigDecimal dataQualityScore = BigDecimal.valueOf(90);

        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid(null, "1h", entry, stop, takeProfitLevels, sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid(" ", "1h", entry, stop, takeProfitLevels, sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", null, entry, stop, takeProfitLevels, sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", " ", entry, stop, takeProfitLevels, sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", "1h", null, stop, takeProfitLevels, sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", "1h", entry, null, takeProfitLevels, sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", "1h", entry, stop, null, sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", "1h", entry, stop, List.of(), sourceFields, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", "1h", entry, stop, takeProfitLevels, null, dataQualityScore));
        assertThrows(IllegalArgumentException.class, () ->
                BoundaryCandidateDTO.valid("BTCUSDT", "1h", entry, stop, takeProfitLevels, sourceFields, null));
    }

    @Test
    void validFactoryShouldDefensivelyCopyTakeProfitLevels() {
        List<BoundaryTakeProfitLevelDTO> takeProfitLevels = new ArrayList<>();
        takeProfitLevels.add(validTakeProfitLevel());

        BoundaryCandidateDTO candidate = BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                validEntry(),
                validStop(),
                takeProfitLevels,
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        takeProfitLevels.add(validTakeProfitLevel());

        assertEquals(1, candidate.getTakeProfitLevels().size());
    }

    private BoundaryEntryDTO validEntry() {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryType("pullback");
        entry.setEntryPrice(BigDecimal.valueOf(68000));
        entry.setEntryZoneLow(BigDecimal.valueOf(67800));
        entry.setEntryZoneHigh(BigDecimal.valueOf(68200));
        entry.setNumericSourceType("support");
        entry.setNumericSourceValue(BigDecimal.valueOf(68000));
        entry.setSourceTimeframe("1h");
        entry.setReason("support retest");
        return entry;
    }

    private BoundaryStopDTO validStop() {
        BoundaryStopDTO stop = new BoundaryStopDTO();
        stop.setStopType("structure_invalidated");
        stop.setStopPrice(BigDecimal.valueOf(66800));
        stop.setStopZoneLow(BigDecimal.valueOf(66600));
        stop.setStopZoneHigh(BigDecimal.valueOf(67000));
        stop.setNumericSourceType("swing_low");
        stop.setNumericSourceValue(BigDecimal.valueOf(66800));
        stop.setSourceTimeframe("1h");
        stop.setReason("recent swing low");
        return stop;
    }

    private BoundaryTakeProfitLevelDTO validTakeProfitLevel() {
        BoundaryTakeProfitLevelDTO takeProfit = new BoundaryTakeProfitLevelDTO();
        takeProfit.setLevel(1);
        takeProfit.setPrice(BigDecimal.valueOf(70400));
        takeProfit.setRr(BigDecimal.valueOf(2));
        takeProfit.setSource("rr_ladder");
        takeProfit.setNumericSourceType("rr_ladder");
        takeProfit.setNumericSourceValue(BigDecimal.valueOf(70400));
        takeProfit.setSourceTimeframe("1h");
        takeProfit.setSourceRef("tp-1");
        takeProfit.setPartialRatio(BigDecimal.valueOf(0.5));
        takeProfit.setAllocationRatio(BigDecimal.valueOf(0.5));
        takeProfit.setReason("2R target");
        return takeProfit;
    }

    private BoundarySourceFieldsDTO validSourceFields() {
        BoundarySourceFieldsDTO sourceFields = new BoundarySourceFieldsDTO();
        sourceFields.setEntrySourceField("supportLevel");
        sourceFields.setStopSourceField("swingLow");
        sourceFields.setTakeProfitSourceField("rrLadder");
        sourceFields.setRrRule("min_rr_2");
        sourceFields.setDataSource("runtimeKlineContext");
        sourceFields.setDataQualityScore(BigDecimal.valueOf(90));
        sourceFields.setEvidenceRefs(List.of("kline-window-1"));
        return sourceFields;
    }
}
