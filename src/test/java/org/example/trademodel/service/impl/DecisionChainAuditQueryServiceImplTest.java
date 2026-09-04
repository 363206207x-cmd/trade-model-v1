package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.ConflictResolverResultMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanCandidateMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.vo.DecisionChainAuditVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DecisionChainAuditQueryServiceImplTest {

    @Test
    void aggregatesOwnedResponsibilityChainWithoutTreatingResolverOrValidationAsAiTrace() {
        Fixture fixture = new Fixture();
        when(fixture.candidates.selectById("candidate-1")).thenReturn(candidate());
        when(fixture.analyses.selectReadableByUser("analysis-1", 7L)).thenReturn(analysis());
        when(fixture.traces.queryOwned(eq(7L), eq("analysis-1"), any(), eq("candidate-1"),
                any(), any(), any(), any(), any(), eq(500))).thenReturn(List.of(
                trace("gpt-call", "GPT_FINAL"),
                trace("gemini-call", "GEMINI_REVIEW"),
                trace("grok-call", "GROK_CHALLENGE"),
                trace("not-ai-owner", "CONFLICT_RESOLVER")));
        when(fixture.conflicts.selectByCandidateId("candidate-1")).thenReturn(conflict());
        when(fixture.plans.selectLatestByCandidateId("candidate-1")).thenReturn(validatedPlan());
        when(fixture.opportunities.selectByOpportunityId("opportunity-1")).thenReturn(opportunity());
        when(fixture.evidence.listByAnalysisId("analysis-1")).thenReturn(List.of(evidence()));
        when(fixture.scores.listByAnalysisId("analysis-1")).thenReturn(List.of(score()));
        when(fixture.decisions.selectLatestByAnalysisId("analysis-1")).thenReturn(decision());
        when(fixture.pushes.listByAnalysisId("analysis-1")).thenReturn(List.of(push()));
        when(fixture.rechecks.selectByPushId(11L)).thenReturn(List.of(recheck()));
        when(fixture.positionLogs.listByAnalysisIdAndUserId("analysis-1", 7L))
                .thenReturn(List.of(monitorLog()));
        when(fixture.positions.listByFinalPlanIdAndUserId("final-1", 7L))
                .thenReturn(List.of(position()));
        when(fixture.reviews.listByAnalysisIdForUser("analysis-1", 7L)).thenReturn(List.of());

        Optional<DecisionChainAuditVO> result = fixture.service.queryForUser(
                7L, null, null, "candidate-1");

        assertThat(result).isPresent();
        DecisionChainAuditVO audit = result.orElseThrow();
        assertThat(audit.getAiTraces()).extracting(DecisionChainAuditVO.AiTraceStage::role)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(audit.getAiTraces()).allSatisfy(trace -> {
            assertThat(trace.inputSummary()).isEqualTo("AI 调用已完成");
            assertThat(trace.outputJson()).isNull();
        });
        assertThat(audit.getConflictResolver().getResolverResultId()).isEqualTo("resolver-1");
        assertThat(audit.getRuleValidation().validationResultId()).isEqualTo("validation-1");
        assertThat(audit.getFinalExecutionPlan().getPlanId()).isEqualTo("final-1");
        assertThat(audit.getEvidence()).extracting(EvidenceItemDO::getEvidenceId).containsExactly("evidence-1");
        assertThat(audit.getScores()).extracting(ScoreItemDO::getScoreId).containsExactly("score-1");
        assertThat(audit.getDecisionBundle().getDecisionId()).isEqualTo("decision-1");
        assertThat(audit.getPushes()).singleElement().satisfies(push ->
                assertThat(push.rechecks()).extracting(TmPushRecheckLogDO::getLogId).containsExactly(12L));
        assertThat(audit.getPositions()).singleElement().satisfies(position -> {
            assertThat(position.position().getId()).isEqualTo(21L);
            assertThat(position.monitorLogs()).extracting(PositionMonitorLogDO::getLogId).containsExactly(22L);
        });
        assertThat(audit.getOrderedStages()).extracting(DecisionChainAuditVO.StageStatus::owner)
                .containsExactly(
                        "AnalysisRun",
                        "EvidenceItem",
                        "ScoreItem",
                        "DecisionResult",
                        "AssetState/Opportunity",
                        "AITrace(GPT_FINAL)",
                        "ExecutionPlanCandidate",
                        "AITrace(GEMINI_REVIEW)",
                        "AITrace(GROK_CHALLENGE)",
                        "ConflictResolverResult",
                        "ExecutionPlan.validation",
                        "ExecutionPlan(final)",
                        "PushSnapshot/PushRecheckLog",
                        "UserPosition/PositionMonitorLog",
                        "ReviewResult");
        verify(fixture.analyses).selectReadableByUser("analysis-1", 7L);
    }

    @Test
    void hidesUnownedAnalysisAndNeverExposesUnvalidatedPlanAsFinal() {
        Fixture fixture = new Fixture();
        when(fixture.candidates.selectById("candidate-1")).thenReturn(candidate());
        when(fixture.analyses.selectReadableByUser("analysis-1", 8L)).thenReturn(null);

        assertThat(fixture.service.queryForUser(8L, null, null, "candidate-1")).isEmpty();

        when(fixture.analyses.selectReadableByUser("analysis-1", 7L)).thenReturn(analysis());
        when(fixture.traces.queryOwned(eq(7L), eq("analysis-1"), any(), eq("candidate-1"),
                any(), any(), any(), any(), any(), eq(500))).thenReturn(List.of());
        when(fixture.conflicts.selectByCandidateId("candidate-1")).thenReturn(conflict());
        ExecutionPlanDO blocked = validatedPlan();
        blocked.setRuleValidationStatus("BLOCKED");
        blocked.setFinalPlan(false);
        when(fixture.plans.selectLatestByCandidateId("candidate-1")).thenReturn(blocked);
        when(fixture.opportunities.selectByOpportunityId("opportunity-1")).thenReturn(opportunity());
        when(fixture.reviews.listByAnalysisIdForUser("analysis-1", 7L)).thenReturn(List.of());

        DecisionChainAuditVO audit = fixture.service.queryForUser(
                7L, null, null, "candidate-1").orElseThrow();
        assertThat(audit.getCandidate()).isNotNull();
        assertThat(audit.getRuleValidation().status()).isEqualTo("BLOCKED");
        assertThat(audit.getFinalExecutionPlan()).isNull();
    }

    @Test
    void failsClosedWhenNoSelectorOrInvalidOwnerIsProvided() {
        Fixture fixture = new Fixture();
        assertThatThrownBy(() -> fixture.service.queryForUser(7L, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("analysisId, traceId or candidateId");
        assertThatThrownBy(() -> fixture.service.queryForUser(null, "analysis-1", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    private static AnalysisRunDO analysis() {
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId("analysis-1");
        row.setStatus("SUCCESS");
        row.setSymbol("BTCUSDT");
        row.setTimeframe("5m");
        return row;
    }

    private static ExecutionPlanCandidateDO candidate() {
        ExecutionPlanCandidateDO row = new ExecutionPlanCandidateDO();
        row.setCandidateId("candidate-1");
        row.setAnalysisId("analysis-1");
        row.setOpportunityId("opportunity-1");
        row.setCandidateStatus("VALIDATED");
        return row;
    }

    private static AiCallLogDO trace(String callId, String role) {
        AiCallLogDO row = new AiCallLogDO();
        row.setCallId(callId);
        row.setTraceId("trace-1");
        row.setAnalysisId("analysis-1");
        row.setCandidateId("candidate-1");
        row.setAiRole(role);
        row.setCallStatus("SUCCESS");
        row.setRequestSummary("{\"accountRisk\":{\"grossNotional\":\"sensitive\"}}");
        row.setOutputPayload("{\"rawProviderPayload\":true}");
        return row;
    }

    private static ConflictResolverResultDO conflict() {
        ConflictResolverResultDO row = new ConflictResolverResultDO();
        row.setResolverResultId("resolver-1");
        return row;
    }

    private static ExecutionPlanDO validatedPlan() {
        ExecutionPlanDO row = new ExecutionPlanDO();
        row.setPlanId("final-1");
        row.setAnalysisId("analysis-1");
        row.setCandidateId("candidate-1");
        row.setOpportunityId("opportunity-1");
        row.setResolverResultId("resolver-1");
        row.setValidationResultId("validation-1");
        row.setRuleValidationStatus("PASS");
        row.setFinalPlan(true);
        return row;
    }

    private static AssetStateDO opportunity() {
        AssetStateDO row = new AssetStateDO();
        row.setOpportunityId("opportunity-1");
        row.setState(AssetStateEnum.CANDIDATE);
        return row;
    }

    private static EvidenceItemDO evidence() {
        EvidenceItemDO row = new EvidenceItemDO();
        row.setEvidenceId("evidence-1");
        row.setAnalysisId("analysis-1");
        return row;
    }

    private static ScoreItemDO score() {
        ScoreItemDO row = new ScoreItemDO();
        row.setScoreId("score-1");
        row.setAnalysisId("analysis-1");
        return row;
    }

    private static DecisionResult decision() {
        DecisionResult row = new DecisionResult();
        row.setDecisionId("decision-1");
        row.setAnalysisId("analysis-1");
        return row;
    }

    private static TmPushSnapshotDO push() {
        TmPushSnapshotDO row = new TmPushSnapshotDO();
        row.setPushId(11L);
        row.setAnalysisId("analysis-1");
        return row;
    }

    private static TmPushRecheckLogDO recheck() {
        TmPushRecheckLogDO row = new TmPushRecheckLogDO();
        row.setLogId(12L);
        row.setPushId(11L);
        return row;
    }

    private static UserPositionDO position() {
        UserPositionDO row = new UserPositionDO();
        row.setId(21L);
        row.setUserId(7L);
        row.setFinalPlanId("final-1");
        return row;
    }

    private static PositionMonitorLogDO monitorLog() {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setLogId(22L);
        row.setPositionId(21L);
        row.setAnalysisId("analysis-1");
        return row;
    }

    private static final class Fixture {
        private final AnalysisRunMapper analyses = mock(AnalysisRunMapper.class);
        private final ExecutionPlanCandidateMapper candidates = mock(ExecutionPlanCandidateMapper.class);
        private final ConflictResolverResultMapper conflicts = mock(ConflictResolverResultMapper.class);
        private final ExecutionPlanMapper plans = mock(ExecutionPlanMapper.class);
        private final AssetStateMapper opportunities = mock(AssetStateMapper.class);
        private final ReviewResultMapper reviews = mock(ReviewResultMapper.class);
        private final AiCallLogService traces = mock(AiCallLogService.class);
        private final EvidenceItemMapper evidence = mock(EvidenceItemMapper.class);
        private final ScoreItemMapper scores = mock(ScoreItemMapper.class);
        private final DecisionResultMapper decisions = mock(DecisionResultMapper.class);
        private final PushSnapshotMapper pushes = mock(PushSnapshotMapper.class);
        private final PushRecheckLogMapper rechecks = mock(PushRecheckLogMapper.class);
        private final UserPositionMapper positions = mock(UserPositionMapper.class);
        private final PositionMonitorLogMapper positionLogs = mock(PositionMonitorLogMapper.class);
        private final DecisionChainAuditQueryServiceImpl service = new DecisionChainAuditQueryServiceImpl(
                analyses, candidates, conflicts, plans, opportunities, reviews, traces,
                evidence, scores, decisions, pushes, rechecks, positions, positionLogs);
    }
}
