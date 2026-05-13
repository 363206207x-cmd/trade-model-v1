package org.example.trademodel.dto.planboundary;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeKlineContextDTOTest {

    @Test
    void runtimeKlineContextStatusEnumValuesAreComplete() {
        assertThat(RuntimeKlineContextStatusEnum.values()).containsExactly(
                RuntimeKlineContextStatusEnum.FRESH,
                RuntimeKlineContextStatusEnum.STALE,
                RuntimeKlineContextStatusEnum.UNKNOWN
        );
    }

    @Test
    void missingFactorySetsUnknownStatusAndBlockingReason() {
        RuntimeKlineContextDTO context =
                RuntimeKlineContextDTO.missing("BTCUSDT", "15m", "MISSING_KLINE_WINDOW");

        assertThat(context.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(context.getTimeframe()).isEqualTo("15m");
        assertThat(context.getStaleStatus()).isEqualTo(RuntimeKlineContextStatusEnum.UNKNOWN);
        assertThat(context.getBlockingReasons()).containsExactly("MISSING_KLINE_WINDOW");
        assertThat(context.getGeneratedAt()).isNotNull();
    }

    @Test
    void staleFactorySetsStaleStatusAndBlockingReason() {
        RuntimeKlineContextDTO context =
                RuntimeKlineContextDTO.stale("ETHUSDT", "1h", "KLINE_STALE");

        assertThat(context.getStaleStatus()).isEqualTo(RuntimeKlineContextStatusEnum.STALE);
        assertThat(context.getBlockingReasons()).containsExactly("KLINE_STALE");
        assertThat(context.getGeneratedAt()).isNotNull();
    }

    @Test
    void freshFactorySetsFreshStatus() {
        RuntimeKlineContextDTO context = RuntimeKlineContextDTO.fresh("SOLUSDT", "4h");

        assertThat(context.getSymbol()).isEqualTo("SOLUSDT");
        assertThat(context.getTimeframe()).isEqualTo("4h");
        assertThat(context.getStaleStatus()).isEqualTo(RuntimeKlineContextStatusEnum.FRESH);
        assertThat(context.getBlockingReasons()).isEmpty();
        assertThat(context.getGeneratedAt()).isNotNull();
    }

    @Test
    void contextCarriesOhlcvKlineItemsMissingFieldsAndBlockingReasons() {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setOpenTime(LocalDateTime.parse("2026-05-13T10:00:00"));
        item.setCloseTime(LocalDateTime.parse("2026-05-13T10:15:00"));
        item.setOpen(new BigDecimal("100.10"));
        item.setHigh(new BigDecimal("102.20"));
        item.setLow(new BigDecimal("99.80"));
        item.setClose(new BigDecimal("101.40"));
        item.setVolume(new BigDecimal("12345.67"));
        item.setSourceType("OKX");

        RuntimeKlineContextDTO context = RuntimeKlineContextDTO.fresh("BTCUSDT", "15m");
        context.setKlineWindowStart(LocalDateTime.parse("2026-05-13T09:00:00"));
        context.setKlineWindowEnd(LocalDateTime.parse("2026-05-13T10:15:00"));
        context.setKlineCount(6);
        context.setLatestOpen(new BigDecimal("100.10"));
        context.setLatestHigh(new BigDecimal("102.20"));
        context.setLatestLow(new BigDecimal("99.80"));
        context.setLatestClose(new BigDecimal("101.40"));
        context.setLatestVolume(new BigDecimal("12345.67"));
        context.setPreviousClose(new BigDecimal("100.00"));
        context.setHighestHigh(new BigDecimal("105.00"));
        context.setLowestLow(new BigDecimal("98.00"));
        context.setAverageVolume(new BigDecimal("10000.00"));
        context.setDataQualityScore(95);
        context.setMissingFields(List.of("volume"));
        context.setBlockingReasons(List.of("MISSING_VOLUME"));
        context.setKlineItems(List.of(item));

        assertThat(context.getLatestClose()).isEqualByComparingTo("101.40");
        assertThat(context.getAverageVolume()).isEqualByComparingTo("10000.00");
        assertThat(context.getKlineCount()).isEqualTo(6);
        assertThat(context.getDataQualityScore()).isEqualTo(95);
        assertThat(context.getMissingFields()).containsExactly("volume");
        assertThat(context.getBlockingReasons()).containsExactly("MISSING_VOLUME");
        assertThat(context.getKlineItems()).hasSize(1);
        assertThat(context.getKlineItems().get(0).getHigh()).isEqualByComparingTo("102.20");
    }

    @Test
    void contextFieldsDoNotExposeTradingActionNames() {
        List<String> fieldNames = Stream.of(RuntimeKlineContextDTO.class.getDeclaredFields())
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
