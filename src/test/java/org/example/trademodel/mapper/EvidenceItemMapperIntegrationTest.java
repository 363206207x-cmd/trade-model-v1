package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.vo.EvidenceBriefVO;
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
class EvidenceItemMapperIntegrationTest {

    @Autowired
    private EvidenceItemMapper evidenceItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectTop3BriefByAnalysisId_returnsAtMostThreeOrderedByCreateTimeDescThenIdDesc() {
        jdbcTemplate.update(
                "INSERT INTO tm_evidence_item(evidence_id, analysis_id, evidence_type, description, direction, source, create_time) VALUES (?,?,?,?,?,?, TIMESTAMP '2025-01-01 00:00:00')",
                "ev-001", "ana-ev-top3", "A", "desc-1", null, null);
        jdbcTemplate.update(
                "INSERT INTO tm_evidence_item(evidence_id, analysis_id, evidence_type, description, direction, source, create_time) VALUES (?,?,?,?,?,?, TIMESTAMP '2025-01-01 00:00:00')",
                "ev-002", "ana-ev-top3", "B", "desc-2", null, null);
        jdbcTemplate.update(
                "INSERT INTO tm_evidence_item(evidence_id, analysis_id, evidence_type, description, direction, source, create_time) VALUES (?,?,?,?,?,?, TIMESTAMP '2025-01-02 00:00:00')",
                "ev-003", "ana-ev-top3", "C", "desc-3", null, null);
        jdbcTemplate.update(
                "INSERT INTO tm_evidence_item(evidence_id, analysis_id, evidence_type, description, direction, source, create_time) VALUES (?,?,?,?,?,?, TIMESTAMP '2025-01-03 00:00:00')",
                "ev-004", "ana-ev-top3", "D", "desc-4", "BULLISH", "SYSTEM_GENERATED");

        List<EvidenceBriefVO> rows = evidenceItemMapper.selectTop3BriefByAnalysisId("ana-ev-top3");

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getEvidenceType()).isEqualTo("D");
        assertThat(rows.get(0).getDirection()).isEqualTo("BULLISH");
        assertThat(rows.get(0).getSource()).isEqualTo("SYSTEM_GENERATED");
        assertThat(rows.get(1).getEvidenceType()).isEqualTo("C");
        assertThat(rows.get(2).getEvidenceType()).isEqualTo("B");
        assertThat(rows).extracting(EvidenceBriefVO::getDescription).containsExactly("desc-4", "desc-3", "desc-2");
    }
}
