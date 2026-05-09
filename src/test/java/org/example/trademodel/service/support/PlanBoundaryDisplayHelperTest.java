package org.example.trademodel.service.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PlanBoundaryDisplayHelperTest {

    @Test
    void null_blank_is_missing_per_context() {
        PlanBoundaryDisplayInfo pm =
                PlanBoundaryDisplayHelper.parse(null, PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(pm.parseStatus()).isEqualTo("MISSING");
        assertThat(pm.stateLabel()).isEqualTo("未返回");
        assertThat(pm.displayText()).isEqualTo("计划价位边界未返回，当前不参与数值监护。");

        PlanBoundaryDisplayInfo tr =
                PlanBoundaryDisplayHelper.parse("  ", PlanBoundaryDisplayContext.TRADE_REVIEW);
        assertThat(tr.displayText()).isEqualTo("计划价位边界未返回，复盘中无结构化边界快照。");

        PlanBoundaryDisplayInfo gen = PlanBoundaryDisplayHelper.parse("", PlanBoundaryDisplayContext.GENERIC);
        assertThat(gen.displayText()).isEqualTo("计划价位边界未返回，当前不参与数值监护。");
    }

    @Test
    void unstructured_preserves_source_confidence() {
        String json = "{\"boundaryParseStatus\":\"UNSTRUCTURED_TEXT_ONLY\",\"boundarySource\":\"AI_PLAN\",\"boundaryConfidence\":\"LOW\"}";
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse(json, PlanBoundaryDisplayContext.TRADE_REVIEW);
        assertThat(i.parseStatus()).isEqualTo("UNSTRUCTURED_TEXT_ONLY");
        assertThat(i.source()).isEqualTo("AI_PLAN");
        assertThat(i.confidence()).isEqualTo("LOW");
        assertThat(i.stateLabel()).isEqualTo("文本参考");
        assertThat(i.invalidPriceDirection()).isNull();
        assertThat(i.invalidPriceThreshold()).isNull();
    }

    @Test
    void partial_above_numeric_threshold() {
        String json = "{\"boundaryParseStatus\":\"PARTIAL\",\"invalidPriceDirection\":\"ABOVE\",\"invalidPriceThreshold\":78477.31}";
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse(json, PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(i.parseStatus()).isEqualTo("PARTIAL");
        assertThat(i.invalidPriceDirection()).isEqualTo("ABOVE");
        assertThat(i.invalidPriceThreshold()).isEqualByComparingTo(new BigDecimal("78477.31"));
    }

    @Test
    void partial_below_string_threshold() {
        String json = "{\"boundaryParseStatus\":\"PARTIAL\",\"invalidPriceDirection\":\"BELOW\",\"invalidPriceThreshold\":\"100.5\"}";
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse(json, PlanBoundaryDisplayContext.GENERIC);
        assertThat(i.invalidPriceDirection()).isEqualTo("BELOW");
        assertThat(i.invalidPriceThreshold()).isEqualByComparingTo(new BigDecimal("100.5"));
    }

    @Test
    void partial_invalid_direction_ignored() {
        String json =
                "{\"boundaryParseStatus\":\"PARTIAL\",\"invalidPriceDirection\":\"SIDEWAYS\",\"invalidPriceThreshold\":1}";
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse(json, PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(i.invalidPriceDirection()).isNull();
        assertThat(i.invalidPriceThreshold()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void invalid_declared_status() {
        String json = "{\"boundaryParseStatus\":\"INVALID\",\"boundarySource\":\"RULE_ENGINE\",\"boundaryConfidence\":\"LOW\"}";
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse(json, PlanBoundaryDisplayContext.TRADE_REVIEW);
        assertThat(i.parseStatus()).isEqualTo("INVALID");
        assertThat(i.source()).isEqualTo("RULE_ENGINE");
        assertThat(i.confidence()).isEqualTo("LOW");
        assertThat(i.stateLabel()).isEqualTo("结构无效");
    }

    @Test
    void illegal_json_fail_open_invalid() {
        assertThatCode(() -> PlanBoundaryDisplayHelper.parse("not-json", PlanBoundaryDisplayContext.POSITION_MONITOR))
                .doesNotThrowAnyException();
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse("not-json", PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(i.parseStatus()).isEqualTo("INVALID");
        assertThat(i.stateLabel()).isEqualTo("结构无效");
    }

    @Test
    void structured_with_invalid_price_reads_fields() {
        String json = "{\"boundaryParseStatus\":\"STRUCTURED\",\"boundarySource\":\"X\",\"boundaryConfidence\":\"HIGH\","
                + "\"invalidPriceDirection\":\"ABOVE\",\"invalidPriceThreshold\":99}";
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse(json, PlanBoundaryDisplayContext.TRADE_REVIEW);
        assertThat(i.parseStatus()).isEqualTo("STRUCTURED");
        assertThat(i.stateLabel()).isEqualTo("已结构化");
        assertThat(i.invalidPriceDirection()).isEqualTo("ABOVE");
        assertThat(i.invalidPriceThreshold()).isEqualByComparingTo(new BigDecimal("99"));
    }

    @Test
    void output_record_has_no_raw_json_or_trading_fields() {
        PlanBoundaryDisplayHelper.parse(
                "{\"boundaryParseStatus\":\"PARTIAL\",\"stopLossPrice\":1,\"markPrice\":2}",
                PlanBoundaryDisplayContext.GENERIC);
        assertThat(Arrays.stream(PlanBoundaryDisplayInfo.class.getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("planBoundaryJson", "stopLossPrice", "markPrice", "autoClose", "order");
    }

    @Test
    void null_context_defaults_like_generic() {
        PlanBoundaryDisplayInfo i = PlanBoundaryDisplayHelper.parse(null, null);
        assertThat(i.displayText()).isEqualTo("计划价位边界未返回，当前不参与数值监护。");
    }
}
