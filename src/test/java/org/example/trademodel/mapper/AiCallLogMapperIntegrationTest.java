package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class AiCallLogMapperIntegrationTest {
    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2035, 1, 1, 0, 0);
    private static final LocalDateTime SINCE = LocalDateTime.of(2034, 1, 1, 0, 0);

    @Autowired
    private AiCallLogMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void sumChargeableCostCountsStartedReservationForDailyAndAnalysisBudgets() {
        insert("started-reservation", "analysis-started", "STARTED", "0.60", "0.00");

        assertThat(mapper.sumChargeableCostSince(SINCE)).isEqualByComparingTo("0.60");
        assertThat(mapper.sumChargeableCostByAnalysisId("analysis-started")).isEqualByComparingTo("0.60");
    }

    @Test
    void sumChargeableCostUsesActualSuccessCostAndFailedReservationFallback() {
        insert("success-actual", "analysis-completed", "SUCCESS", "0.60", "0.20");
        insert("failed-reserved", "analysis-completed", "FAILED", "0.40", "0.00");

        assertThat(mapper.sumChargeableCostByAnalysisId("analysis-completed")).isEqualByComparingTo("0.60");
    }

    @Test
    void sumChargeableCostDoesNotChargeLocalSkippedStatuses() {
        insert("budget-blocked", "analysis-skipped", "BUDGET_BLOCKED", "0.90", "0.00");
        insert("disabled", "analysis-skipped", "DISABLED", "0.90", "0.00");
        insert("not-configured", "analysis-skipped", "NOT_CONFIGURED", "0.90", "0.00");
        insert("rate-limited", "analysis-skipped", "RATE_LIMITED", "0.90", "0.00");

        assertThat(mapper.sumChargeableCostByAnalysisId("analysis-skipped")).isEqualByComparingTo("0");
    }

    private void insert(String callId, String analysisId, String status, String reservedCost, String calculatedCost) {
        jdbcTemplate.update("""
                INSERT INTO tm_ai_call_log(
                  call_id, analysis_id, provider_name, ai_role, call_status, started_at,
                  reserved_cost_usd, calculated_cost_usd
                ) VALUES (?, ?, 'OPENAI', 'GPT_RULE_REVIEW', ?, ?, ?, ?)
                """,
                callId,
                analysisId,
                status,
                Timestamp.valueOf(STARTED_AT),
                new BigDecimal(reservedCost),
                new BigDecimal(calculatedCost));
    }
}
