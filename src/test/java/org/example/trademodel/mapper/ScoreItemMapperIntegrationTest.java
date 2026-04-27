package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.vo.ScoreBriefVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class ScoreItemMapperIntegrationTest {

    @Autowired
    private ScoreItemMapper scoreItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectTop3BriefByAnalysisId_returnsAtMostThreeOrderedByScoreIdDesc() {
        jdbcTemplate.update(
                "INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value, weight, direction, description) VALUES (?,?,?,?,?,?,?)",
                "sc-001", "ana-sc-top3", "A", 61.0, 1.0, "UP", "desc-1");
        jdbcTemplate.update(
                "INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value, weight, direction, description) VALUES (?,?,?,?,?,?,?)",
                "sc-002", "ana-sc-top3", "B", 62.0, 1.0, "UP", "desc-2");
        jdbcTemplate.update(
                "INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value, weight, direction, description) VALUES (?,?,?,?,?,?,?)",
                "sc-003", "ana-sc-top3", "C", 63.0, 1.0, "UP", "desc-3");
        jdbcTemplate.update(
                "INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value, weight, direction, description) VALUES (?,?,?,?,?,?,?)",
                "sc-004", "ana-sc-top3", "D", 64.0, 1.0, "UP", "desc-4");

        List<ScoreBriefVO> rows = scoreItemMapper.selectTop3BriefByAnalysisId("ana-sc-top3");

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getScoreType()).isEqualTo("D");
        assertThat(rows.get(1).getScoreType()).isEqualTo("C");
        assertThat(rows.get(2).getScoreType()).isEqualTo("B");
        assertThat(rows).extracting(ScoreBriefVO::getScoreValue).containsExactly(64.0, 63.0, 62.0);
    }
}
