package org.example.trademodel.dto.planboundary;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundaryCandidateDTOTest {

    @Test
    void boundaryStatusEnumValuesAreComplete() {
        assertThat(BoundaryStatusEnum.values()).containsExactly(
                BoundaryStatusEnum.VALID,
                BoundaryStatusEnum.INCOMPLETE,
                BoundaryStatusEnum.INVALID,
                BoundaryStatusEnum.WATCH_ONLY
        );
    }

    @Test
    void boundaryEntryTypeEnumValuesAreComplete() {
        assertThat(BoundaryEntryTypeEnum.values()).containsExactly(
                BoundaryEntryTypeEnum.BREAKOUT,
                BoundaryEntryTypeEnum.PULLBACK,
                BoundaryEntryTypeEnum.REJECTION,
                BoundaryEntryTypeEnum.WATCH_ONLY
        );
    }

    @Test
    void boundaryStopTypeEnumValuesAreComplete() {
        assertThat(BoundaryStopTypeEnum.values()).containsExactly(
                BoundaryStopTypeEnum.STRUCTURE_INVALIDATION,
                BoundaryStopTypeEnum.ATR_BUFFER,
                BoundaryStopTypeEnum.SWING_BREAK,
                BoundaryStopTypeEnum.INVALID
        );
    }

    @Test
    void incompleteFactorySetsSafeDefaultsAndReason() {
        BoundaryCandidateDTO candidate =
                BoundaryCandidateDTO.incomplete("BTCUSDT", "15m", "MISSING_KLINE_WINDOW");

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(candidate.getTimeframe()).isEqualTo("15m");
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).extracting(BoundaryBlockingReasonDTO::getText)
                .containsExactly("MISSING_KLINE_WINDOW");
    }

    @Test
    void watchOnlyFactorySetsStatus() {
        BoundaryCandidateDTO candidate =
                BoundaryCandidateDTO.watchOnly("ETHUSDT", "1h", "EVENT_WINDOW");

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
    }

    @Test
    void invalidFactorySetsStatus() {
        BoundaryCandidateDTO candidate =
                BoundaryCandidateDTO.invalid("SOLUSDT", "4h", "DIRECTION_BOUNDARY_CONFLICT");

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INVALID);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
    }

    @Test
    void validFactoryCreatesCandidateWithSafeDefaults() {
        BoundaryEntryDTO entry = sampleEntry();
        BoundaryStopDTO stop = sampleStop();
        BoundaryTakeProfitLevelDTO tp = sampleTakeProfitLevel();
        BoundarySourceFieldsDTO sourceFields = sampleSourceFields();

        BoundaryCandidateDTO candidate = BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                stop,
                List.of(tp),
                sourceFields,
                new BigDecimal("88.50")
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.VALID);
        assertThat(candidate.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(candidate.getTimeframe()).isEqualTo("1h");
        assertThat(candidate.getEntry()).isSameAs(entry);
        assertThat(candidate.getStop()).isSameAs(stop);
        assertThat(candidate.getTakeProfitLevels()).containsExactly(tp);
        assertThat(candidate.getSourceFields()).isSameAs(sourceFields);
        assertThat(candidate.getDataQualityScore()).isEqualByComparingTo("88.50");
        assertThat(candidate.getBlockingReasons()).isEmpty();
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
    }

    @Test
    void validFactoryDefensivelyCopiesTakeProfitLevels() {
        List<BoundaryTakeProfitLevelDTO> takeProfitLevels = new ArrayList<>();
        takeProfitLevels.add(sampleTakeProfitLevel());

        BoundaryCandidateDTO candidate = BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                sampleEntry(),
                sampleStop(),
                takeProfitLevels,
                sampleSourceFields(),
                new BigDecimal("90.00")
        );

        takeProfitLevels.add(sampleTakeProfitLevel());

        assertThat(candidate.getTakeProfitLevels()).hasSize(1);
    }

    @Test
    void validFactoryRejectsMissingRequiredFields() {
        BoundaryEntryDTO entry = sampleEntry();
        BoundaryStopDTO stop = sampleStop();
        List<BoundaryTakeProfitLevelDTO> takeProfitLevels = List.of(sampleTakeProfitLevel());
        BoundarySourceFieldsDTO sourceFields = sampleSourceFields();
        BigDecimal dataQualityScore = new BigDecimal("90.00");

        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                " ",
                "1h",
                entry,
                stop,
                takeProfitLevels,
                sourceFields,
                dataQualityScore
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                " ",
                entry,
                stop,
                takeProfitLevels,
                sourceFields,
                dataQualityScore
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                null,
                stop,
                takeProfitLevels,
                sourceFields,
                dataQualityScore
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                null,
                takeProfitLevels,
                sourceFields,
                dataQualityScore
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                stop,
                null,
                sourceFields,
                dataQualityScore
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                stop,
                List.of(),
                sourceFields,
                dataQualityScore
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                stop,
                takeProfitLevels,
                null,
                dataQualityScore
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                stop,
                takeProfitLevels,
                sourceFields,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void candidateCarriesEntryStopTakeProfitAndSourceFields() {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryType(BoundaryEntryTypeEnum.PULLBACK);
        entry.setEntryZoneLow(new BigDecimal("100.10"));
        entry.setEntryZoneHigh(new BigDecimal("101.20"));
        entry.setEntrySourceFields(List.of("swingLowRef", "atrValue"));

        BoundaryStopDTO stop = new BoundaryStopDTO();
        stop.setStopType(BoundaryStopTypeEnum.STRUCTURE_INVALIDATION);
        stop.setStopLoss(new BigDecimal("96.80"));
        stop.setStopSourceFields(List.of("swingLowRef"));

        BoundaryTakeProfitLevelDTO tp = new BoundaryTakeProfitLevelDTO();
        tp.setLevel(1);
        tp.setPrice(new BigDecimal("108.00"));
        tp.setRr(new BigDecimal("2.00"));

        BoundarySourceFieldsDTO sourceFields = new BoundarySourceFieldsDTO();
        sourceFields.setTimeframe("1h");
        sourceFields.setAtrValue(new BigDecimal("1.23"));
        sourceFields.setEvidenceRefs(List.of("kline:1h:last-48"));

        BoundaryCandidateDTO candidate = new BoundaryCandidateDTO();
        candidate.setBoundaryStatus(BoundaryStatusEnum.VALID);
        candidate.setEntry(entry);
        candidate.setStop(stop);
        candidate.setTakeProfitLevels(List.of(tp));
        candidate.setSourceFields(sourceFields);

        assertThat(candidate.getEntry().getEntryZoneLow()).isEqualByComparingTo("100.10");
        assertThat(candidate.getStop().getStopLoss()).isEqualByComparingTo("96.80");
        assertThat(candidate.getTakeProfitLevels()).hasSize(1);
        assertThat(candidate.getSourceFields().getEvidenceRefs()).containsExactly("kline:1h:last-48");
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
    }

    @Test
    void numericSourceCarriesTraceableFields() {
        BoundaryNumericSourceDTO numericSource = new BoundaryNumericSourceDTO();
        numericSource.setSourceType("rr_ladder");
        numericSource.setSourceValue(new BigDecimal("108.00"));
        numericSource.setSourceTimeframe("1h");
        numericSource.setSourceReason("TP ladder level from prior resistance");
        numericSource.setSourceField("resistanceRef");
        numericSource.setSourceRef("source:resistance:1h");

        assertThat(numericSource.getSourceType()).isEqualTo("rr_ladder");
        assertThat(numericSource.getSourceValue()).isEqualByComparingTo("108.00");
        assertThat(numericSource.getSourceTimeframe()).isEqualTo("1h");
        assertThat(numericSource.getSourceReason()).isEqualTo("TP ladder level from prior resistance");
        assertThat(numericSource.getSourceField()).isEqualTo("resistanceRef");
        assertThat(numericSource.getSourceRef()).isEqualTo("source:resistance:1h");
    }

    @Test
    void numericSourceDoesNotExposeExecutionActionNames() {
        List<String> fieldNames = Stream.of(BoundaryNumericSourceDTO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());
        List<String> methodNames = Stream.of(BoundaryNumericSourceDTO.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toList());

        assertThat(fieldNames).doesNotContain(
                "order" + "Id",
                "order" + "Side",
                "execution" + "Id",
                "auto" + "Trade",
                "place" + "Order",
                "close" + "Position",
                "reverse" + "Position"
        );
        assertThat(methodNames).doesNotContain(
                "get" + "OrderId",
                "set" + "OrderId",
                "get" + "ExecutionId",
                "set" + "ExecutionId",
                "enable" + "AutoTrade",
                "place" + "Order",
                "close" + "Position",
                "reverse" + "Position"
        );
    }

    @Test
    void takeProfitLevelCarriesTraceabilityFields() {
        BoundaryNumericSourceDTO numericSource = new BoundaryNumericSourceDTO();
        numericSource.setSourceType("resistance");
        numericSource.setSourceValue(new BigDecimal("108.00"));
        numericSource.setSourceTimeframe("1h");
        numericSource.setSourceReason("prior resistance target");
        numericSource.setSourceField("resistanceRef");
        numericSource.setSourceRef("source:resistance:1h");

        BoundaryTakeProfitLevelDTO tp = new BoundaryTakeProfitLevelDTO();
        tp.setLevel(1);
        tp.setPrice(new BigDecimal("108.00"));
        tp.setRr(new BigDecimal("2.00"));
        tp.setSource("resistance");
        tp.setReason("first target from resistance");
        tp.setNumericSource(numericSource);
        tp.setSourceTimeframe("1h");
        tp.setSourceRef("source:resistance:1h");
        tp.setPartialRatio(new BigDecimal("0.50"));
        tp.setAllocationRatio(new BigDecimal("0.50"));

        assertThat(tp.getLevel()).isEqualTo(1);
        assertThat(tp.getPrice()).isEqualByComparingTo("108.00");
        assertThat(tp.getRr()).isEqualByComparingTo("2.00");
        assertThat(tp.getSource()).isEqualTo("resistance");
        assertThat(tp.getReason()).isEqualTo("first target from resistance");
        assertThat(tp.getNumericSource().getSourceField()).isEqualTo("resistanceRef");
        assertThat(tp.getSourceTimeframe()).isEqualTo("1h");
        assertThat(tp.getSourceRef()).isEqualTo("source:resistance:1h");
        assertThat(tp.getPartialRatio()).isEqualByComparingTo("0.50");
        assertThat(tp.getAllocationRatio()).isEqualByComparingTo("0.50");
    }

    @Test
    void candidateFieldsDoNotExposeTradingActionNames() {
        List<String> fieldNames = Stream.of(BoundaryCandidateDTO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        assertThat(fieldNames).doesNotContain(
                "order" + "Id",
                "api" + "Key",
                "sec" + "ret",
                "exchange" + "Account",
                "auto" + "Order",
                "auto" + "Open",
                "auto" + "Close",
                "auto" + "Reverse"
        );
    }

    @Test
    void candidateFactoryMethodsDoNotExposeTradingActionNames() {
        List<String> methodNames = Stream.of(BoundaryCandidateDTO.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toList());

        assertThat(methodNames).doesNotContain(
                "execute" + "Order",
                "place" + "Order",
                "close" + "Position",
                "reverse" + "Position",
                "auto" + "Trade"
        );
    }

    private BoundaryEntryDTO sampleEntry() {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryType(BoundaryEntryTypeEnum.PULLBACK);
        entry.setEntryZoneLow(new BigDecimal("100.10"));
        entry.setEntryZoneHigh(new BigDecimal("101.20"));
        entry.setEntrySource("support");
        entry.setEntryTimeframe("1h");
        entry.setEntryReason("pullback into support");
        entry.setEntrySourceFields(List.of("supportRef", "atrValue"));
        return entry;
    }

    private BoundaryStopDTO sampleStop() {
        BoundaryStopDTO stop = new BoundaryStopDTO();
        stop.setStopType(BoundaryStopTypeEnum.STRUCTURE_INVALIDATION);
        stop.setStopLoss(new BigDecimal("96.80"));
        stop.setStopSource("swing_low");
        stop.setStopTimeframe("1h");
        stop.setStopReason("structure invalidation below swing low");
        stop.setStopSourceFields(List.of("swingLowRef"));
        return stop;
    }

    private BoundaryTakeProfitLevelDTO sampleTakeProfitLevel() {
        BoundaryNumericSourceDTO numericSource = new BoundaryNumericSourceDTO();
        numericSource.setSourceType("resistance");
        numericSource.setSourceValue(new BigDecimal("108.00"));
        numericSource.setSourceTimeframe("1h");
        numericSource.setSourceReason("prior resistance target");
        numericSource.setSourceField("resistanceRef");
        numericSource.setSourceRef("source:resistance:1h");

        BoundaryTakeProfitLevelDTO tp = new BoundaryTakeProfitLevelDTO();
        tp.setLevel(1);
        tp.setPrice(new BigDecimal("108.00"));
        tp.setRr(new BigDecimal("2.00"));
        tp.setSource("resistance");
        tp.setReason("first target from resistance");
        tp.setNumericSource(numericSource);
        tp.setSourceTimeframe("1h");
        tp.setSourceRef("source:resistance:1h");
        tp.setPartialRatio(new BigDecimal("0.50"));
        tp.setAllocationRatio(new BigDecimal("0.50"));
        return tp;
    }

    private BoundarySourceFieldsDTO sampleSourceFields() {
        BoundarySourceFieldsDTO sourceFields = new BoundarySourceFieldsDTO();
        sourceFields.setTimeframe("1h");
        sourceFields.setAtrValue(new BigDecimal("1.23"));
        sourceFields.setDataQualityScore(new BigDecimal("88.50"));
        sourceFields.setEvidenceRefs(List.of("kline:1h:last-48"));
        return sourceFields;
    }
}
