package org.example.trademodel.service.impl;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.service.DecisionChainAuditQueryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class DecisionChainAuditPreviewIntegrationTest {
    private static final String ANALYSIS_ID = "analysis-preview-audit-chain";
    private static final String TRACE_ID = "trace-preview-audit-chain";
    private static final String REQUEST_ID = "request-preview-audit-chain";
    private static final long USER_ID = 84001L;

    @Autowired
    private DecisionChainAuditQueryService auditQueryService;

    @Autowired
    private AnalysisRunMapper analysisRunMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void readsUserOwnedPreviewWithoutPromotingCandidateOrFinalPlan() {
        LocalDateTime now = LocalDateTime.of(2038, 4, 5, 6, 7, 8);
        jdbcTemplate.update("""
                INSERT INTO tm_analysis_run(
                  analysis_id, symbol, timeframe, analysis_time, rule_version,
                  owner_type, owner_id, preview, analysis_mode, data_quality_score,
                  trace_id, request_id, status, started_at, completed_at
                ) VALUES (?, 'ETHUSDT', '5m', ?, 'v4.1', 'USER', ?, TRUE,
                  'ANALYSIS_PREVIEW', 55, ?, ?, 'SUCCESS', ?, ?)
                """, ANALYSIS_ID, Timestamp.valueOf(now), USER_ID, TRACE_ID, REQUEST_ID,
                Timestamp.valueOf(now), Timestamp.valueOf(now.plusSeconds(1)));

        for (int index = 1; index <= 5; index++) {
            jdbcTemplate.update("""
                    INSERT INTO tm_evidence_item(evidence_id, analysis_id, evidence_type, description, create_time)
                    VALUES (?, ?, 'MARKET', ?, ?)
                    """, "preview-evidence-" + index, ANALYSIS_ID, "evidence-" + index, Timestamp.valueOf(now));
        }
        for (int index = 1; index <= 8; index++) {
            jdbcTemplate.update("""
                    INSERT INTO tm_score_item(score_id, analysis_id, score_type, score_value)
                    VALUES (?, ?, ?, ?)
                    """, "preview-score-" + index, ANALYSIS_ID, "SCORE_" + index, 50 + index);
        }
        insertTrace("preview-gpt", "GPT_FINAL", false, false, now);
        insertTrace("preview-gemini", "GEMINI_REVIEW", true, true, now.plusNanos(1_000_000));
        insertTrace("preview-grok", "GROK_CHALLENGE", true, true, now.plusNanos(2_000_000));

        var audit = auditQueryService.queryForUser(USER_ID, ANALYSIS_ID, null, null).orElseThrow();

        assertThat(audit.getAnalysis().preview()).isTrue();
        assertThat(audit.getAnalysis().analysisMode()).isEqualTo("ANALYSIS_PREVIEW");
        assertThat(audit.getEvidence()).hasSize(5);
        assertThat(audit.getScores()).hasSize(8);
        assertThat(audit.getAiTraces())
                .extracting(trace -> trace.role())
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(audit.getOpportunity()).isNull();
        assertThat(audit.getCandidate()).isNull();
        assertThat(audit.getConflictResolver()).isNull();
        assertThat(audit.getFinalExecutionPlan()).isNull();
        assertThat(analysisRunMapper.selectReadableByTraceId(TRACE_ID, USER_ID).getAnalysisId())
                .isEqualTo(ANALYSIS_ID);
        assertThat(analysisRunMapper.selectReadableByRequestId(REQUEST_ID, USER_ID).getAnalysisId())
                .isEqualTo(ANALYSIS_ID);
    }

    private void insertTrace(String callId, String role, boolean reviewOnly,
                             boolean notExecutionPlanCreation, LocalDateTime startedAt) {
        jdbcTemplate.update("""
                INSERT INTO tm_ai_call_log(
                  call_id, analysis_id, trace_id, request_id, provider_name, model_name,
                  ai_role, call_status, started_at, completed_at, fallback_flag, fallback_reason,
                  request_hash, contract_type, output_payload, review_only,
                  not_execution_plan_creation
                ) VALUES (?, ?, ?, ?, 'RULE_PATH', 'contract-gate', ?, 'INVALID_RESPONSE',
                  ?, ?, TRUE, 'AI_INPUT_CONTRACT_BLOCKED', ?, 'DECISION_CHAIN_V4_1', ?, ?, ?)
                """, callId, ANALYSIS_ID, TRACE_ID, REQUEST_ID, role,
                Timestamp.valueOf(startedAt), Timestamp.valueOf(startedAt.plusNanos(500_000)),
                "hash-" + callId, "{\"roleState\":\"FALLBACK\"}", reviewOnly,
                notExecutionPlanCreation);
    }
}
