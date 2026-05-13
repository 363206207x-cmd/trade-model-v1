package org.example.trademodel.dto.planboundary;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
}
