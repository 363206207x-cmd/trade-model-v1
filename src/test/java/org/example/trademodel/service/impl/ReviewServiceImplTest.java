package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.ReviewStateVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewResultMapper reviewResultMapper;

    @Mock
    private AnalysisRunMapper analysisRunMapper;

    @Mock
    private RuleVersionLogMapper ruleVersionLogMapper;

    @Mock
    private UserPositionMapper userPositionMapper;

    @Test
    void saveOrUpdate_insertBranch_writesRuleVersionLog_whenRuleVersionMissingAndErrorTypeBlank() {
        ReviewServiceImpl service = service();

        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId(" a-1 ");
        req.setErrorType("   ");
        req.setActualOutcome(null);
        req.setAdjustmentSuggestion(" ");

        ReviewResultDO savedRow = reviewRow("r-1", "a-1", null, null, null);
        when(reviewResultMapper.selectByAnalysisId("a-1"))
                .thenReturn(null)
                .thenReturn(savedRow);
        when(reviewResultMapper.insert(any(ReviewResultDO.class))).thenReturn(1);

        // rule_version 回查不到
        when(analysisRunMapper.selectById("a-1")).thenReturn(null);
        when(ruleVersionLogMapper.insert(any(RuleVersionLogDO.class))).thenReturn(1);

        ReviewStateVO vo = service.saveOrUpdate(req);
        assertThat(vo).isNotNull();

        ArgumentCaptor<RuleVersionLogDO> captor = ArgumentCaptor.forClass(RuleVersionLogDO.class);
        verify(ruleVersionLogMapper, times(1)).insert(captor.capture());

        RuleVersionLogDO log = captor.getValue();
        assertThat(log.getAnalysisId()).isEqualTo("a-1");
        assertThat(log.getRuleVersion()).isNull();
        assertThat(log.getErrorType()).isNull();
        assertThat(log.getChangeCategory()).isEqualTo("REVIEW_FEEDBACK_SAVED");
        assertThat(log.getOperator()).isEqualTo("SYSTEM");
        assertThat(log.getChangeSummary())
                .startsWith("REVIEW_FEEDBACK_SAVED;analysisId=a-1;ruleVersion=MISSING;errorType=NULL;operator=SYSTEM;time=");
        assertThat(log.getChangeDetail()).contains("errorType=NULL");
        assertThat(log.getChangeDetail()).contains("actualOutcome=NULL");
        assertThat(log.getChangeDetail()).contains("adjustmentSuggestion=NULL");
    }

    @Test
    void saveOrUpdate_updateBranch_writesRuleVersionLog_eachSave() {
        ReviewServiceImpl service = service();

        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId("a-2");
        req.setErrorType("DATA_ISSUE");
        req.setActualOutcome("OUTCOME");
        req.setAdjustmentSuggestion("SUGGEST");

        ReviewResultDO existingRow = reviewRow("r-old", "a-2", "OLD", "OLD_OUTCOME", "OLD_SUGGEST");
        ReviewResultDO savedRow = reviewRow("r-new", "a-2", "DATA_ISSUE", "OUTCOME", "SUGGEST");

        when(reviewResultMapper.selectByAnalysisId("a-2"))
                .thenReturn(existingRow)
                .thenReturn(savedRow);
        when(reviewResultMapper.updateContentByAnalysisId(any(ReviewResultDO.class))).thenReturn(1);

        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("a-2");
        run.setRuleVersion("v2");
        when(analysisRunMapper.selectById("a-2")).thenReturn(run);
        when(ruleVersionLogMapper.insert(any(RuleVersionLogDO.class))).thenReturn(1);

        ReviewStateVO vo = service.saveOrUpdate(req);
        assertThat(vo).isNotNull();

        ArgumentCaptor<RuleVersionLogDO> captor = ArgumentCaptor.forClass(RuleVersionLogDO.class);
        verify(ruleVersionLogMapper, times(1)).insert(captor.capture());
        RuleVersionLogDO log = captor.getValue();

        assertThat(log.getAnalysisId()).isEqualTo("a-2");
        assertThat(log.getRuleVersion()).isEqualTo("v2");
        assertThat(log.getErrorType()).isEqualTo("DATA_ISSUE");
        assertThat(log.getChangeCategory()).isEqualTo("REVIEW_FEEDBACK_SAVED");
        assertThat(log.getChangeSummary())
                .startsWith("REVIEW_FEEDBACK_SAVED;analysisId=a-2;ruleVersion=v2;errorType=DATA_ISSUE;operator=SYSTEM;time=");
        assertThat(log.getChangeDetail()).contains("errorType=DATA_ISSUE");
        assertThat(log.getChangeDetail()).contains("actualOutcome=OUTCOME");
        assertThat(log.getChangeDetail()).contains("adjustmentSuggestion=SUGGEST");

        verify(reviewResultMapper, never()).insert(any(ReviewResultDO.class));
    }

    @Test
    void saveOrUpdate_invalidErrorType_throwsBeforeDbAndSkipsAudit() {
        ReviewServiceImpl service = service();

        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId("a-bad");
        req.setErrorType("NOT_IN_VOCABULARY");
        req.setActualOutcome("x");
        req.setAdjustmentSuggestion("y");

        assertThrows(IllegalArgumentException.class, () -> service.saveOrUpdate(req));

        verify(reviewResultMapper, never()).insert(any());
        verify(reviewResultMapper, never()).selectByAnalysisId(any());
        verify(reviewResultMapper, never()).updateContentByAnalysisId(any());
        verify(ruleVersionLogMapper, never()).insert(any());
    }

    @Test
    void saveOrUpdate_insertBranch_allowedErrorType_writesRuleVersionLog() {
        ReviewServiceImpl service = service();

        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId("a-ok");
        req.setErrorType("UNKNOWN");
        req.setActualOutcome("O");
        req.setAdjustmentSuggestion("A");

        ReviewResultDO savedRow = reviewRow("r-ok", "a-ok", "UNKNOWN", "O", "A");
        when(reviewResultMapper.selectByAnalysisId("a-ok"))
                .thenReturn(null)
                .thenReturn(savedRow);
        when(reviewResultMapper.insert(any(ReviewResultDO.class))).thenReturn(1);

        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("a-ok");
        run.setRuleVersion("v9");
        when(analysisRunMapper.selectById("a-ok")).thenReturn(run);
        when(ruleVersionLogMapper.insert(any(RuleVersionLogDO.class))).thenReturn(1);

        ReviewStateVO vo = service.saveOrUpdate(req);
        assertThat(vo).isNotNull();
        assertThat(vo.getErrorType()).isEqualTo("UNKNOWN");

        ArgumentCaptor<RuleVersionLogDO> captor = ArgumentCaptor.forClass(RuleVersionLogDO.class);
        verify(ruleVersionLogMapper, times(1)).insert(captor.capture());
        RuleVersionLogDO log = captor.getValue();
        assertThat(log.getChangeSummary())
                .startsWith("REVIEW_FEEDBACK_SAVED;analysisId=a-ok;ruleVersion=v9;errorType=UNKNOWN;operator=SYSTEM;time=");
        assertThat(log.getChangeDetail()).contains("errorType=UNKNOWN");
    }

    @Test
    void userPositionSaveUsesExactOwnerPositionAndServerGeneratedScope() {
        ReviewServiceImpl service = service();
        UserPositionDO position = new UserPositionDO();
        position.setId(31L);
        position.setUserId(17L);
        position.setStatus("CLOSED");
        when(userPositionMapper.selectByIdAndUserId(31L, 17L)).thenReturn(position);

        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId("shared-analysis");
        req.setErrorType("RULE_TOO_STRICT");
        req.setActualOutcome("OWNER_A");
        req.setAdjustmentSuggestion("owner-a-only");

        ReviewResultDO saved = reviewRow(
                "review-owner-a", "shared-analysis", "RULE_TOO_STRICT", "OWNER_A", "owner-a-only");
        saved.setUserId(17L);
        saved.setUserPositionId(31L);
        saved.setReviewScopeKey("USER:17:POSITION:31");
        when(reviewResultMapper.selectByUserPositionScope(
                "shared-analysis", 17L, 31L, "USER:17:POSITION:31"))
                .thenReturn(null)
                .thenReturn(saved);

        ReviewStateVO result = service.saveOrUpdateForUserPosition(17L, 31L, req);

        assertThat(result.getReviewId()).isEqualTo("review-owner-a");
        ArgumentCaptor<ReviewResultDO> rowCaptor = ArgumentCaptor.forClass(ReviewResultDO.class);
        verify(reviewResultMapper).insert(rowCaptor.capture());
        assertThat(rowCaptor.getValue()).satisfies(row -> {
            assertThat(row.getAnalysisId()).isEqualTo("shared-analysis");
            assertThat(row.getUserId()).isEqualTo(17L);
            assertThat(row.getUserPositionId()).isEqualTo(31L);
            assertThat(row.getReviewScopeKey()).isEqualTo("USER:17:POSITION:31");
        });
        verify(reviewResultMapper, never()).selectByAnalysisId("shared-analysis");
        verify(reviewResultMapper, never()).updateContentByAnalysisId(any());
        verify(ruleVersionLogMapper, never()).insert(any());
    }

    @Test
    void userPositionSaveRejectsNonOwnerBeforeReviewLookupOrWrite() {
        ReviewServiceImpl service = service();
        when(userPositionMapper.selectByIdAndUserId(41L, 17L)).thenReturn(null);
        WriteReviewResultReq req = new WriteReviewResultReq();
        req.setAnalysisId("shared-analysis");

        assertThrows(UserPositionNotFoundException.class,
                () -> service.saveOrUpdateForUserPosition(17L, 41L, req));

        verify(reviewResultMapper, never()).selectByUserPositionScope(any(), any(), any(), any());
        verify(reviewResultMapper, never()).insert(any());
        verify(reviewResultMapper, never()).updateContentByUserPositionScope(any());
        verify(ruleVersionLogMapper, never()).insert(any());
    }

    private ReviewServiceImpl service() {
        return new ReviewServiceImpl(
                reviewResultMapper, analysisRunMapper, ruleVersionLogMapper, userPositionMapper);
    }

    private static ReviewResultDO reviewRow(String id,
                                            String analysisId,
                                            String errorType,
                                            String actualOutcome,
                                            String adjustmentSuggestion) {
        ReviewResultDO row = new ReviewResultDO();
        row.setId(id);
        row.setAnalysisId(analysisId);
        row.setErrorType(errorType);
        row.setActualOutcome(actualOutcome);
        row.setAdjustmentSuggestion(adjustmentSuggestion);
        row.setCreateTime(null);
        row.setUpdateTime(null);
        return row;
    }
}
