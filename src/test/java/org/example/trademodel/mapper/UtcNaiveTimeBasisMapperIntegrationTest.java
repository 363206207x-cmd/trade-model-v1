package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class UtcNaiveTimeBasisMapperIntegrationTest {

    @Autowired
    private DecisionResultMapper decisionResultMapper;

    @Autowired
    private PushRecheckLogMapper pushRecheckLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void decisionAtUtcMidnightBelongsToCorrectUtcDay() {
        insertDecision("utc-day-before", "2026-07-13 23:59:59");
        insertDecision("utc-day-start", "2026-07-14 00:00:00");
        insertDecision("utc-day-end", "2026-07-14 23:59:59");
        insertDecision("utc-day-after", "2026-07-15 00:00:00");

        int count = decisionResultMapper.countDecisionsInRange(
                LocalDateTime.parse("2026-07-14T00:00:00"),
                LocalDateTime.parse("2026-07-15T00:00:00"));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void recheckWindowIncludesExactStartAndAsOf() {
        insertRecheck("UTC_BOUNDARY_INCLUSIVE", "2026-07-14 11:30:00");
        insertRecheck("UTC_BOUNDARY_INCLUSIVE", "2026-07-14 12:00:00");

        Integer count = pushRecheckLogMapper.countByStatusInWindow(
                "UTC_BOUNDARY_INCLUSIVE",
                LocalDateTime.parse("2026-07-14T11:30:00"),
                LocalDateTime.parse("2026-07-14T12:00:00"));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void recheckWindowExcludesBeforeStartAndAfterAsOf() {
        insertRecheck("UTC_BOUNDARY_EXCLUSIVE", "2026-07-14 11:29:59");
        insertRecheck("UTC_BOUNDARY_EXCLUSIVE", "2026-07-14 11:59:59");
        insertRecheck("UTC_BOUNDARY_EXCLUSIVE", "2026-07-14 12:00:01");

        Integer count = pushRecheckLogMapper.countByStatusInWindow(
                "UTC_BOUNDARY_EXCLUSIVE",
                LocalDateTime.parse("2026-07-14T11:30:00"),
                LocalDateTime.parse("2026-07-14T12:00:00"));

        assertThat(count).isEqualTo(1);
    }

    private void insertDecision(String id, String createTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) "
                        + "VALUES (?,?,?, CAST(? AS TIMESTAMP))",
                id, "analysis-" + id, "BTCUSDT", createTime);
    }

    private void insertRecheck(String status, String createTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_push_recheck_log(push_id, recheck_status, create_time) "
                        + "VALUES (?,?, CAST(? AS TIMESTAMP))",
                991L, status, createTime);
    }
}
