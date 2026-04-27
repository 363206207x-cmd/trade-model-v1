package org.example.trademodel.service.impl;

import org.example.trademodel.vo.ReviewAggregateVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewAggregateServiceImplTest {

    private static boolean callHasBlockingRecheck(String recheckStatus) throws Exception {
        ReviewAggregateVO.ReviewRecheckSummary r = new ReviewAggregateVO.ReviewRecheckSummary();
        r.setRecheckStatus(recheckStatus);

        Method m = ReviewAggregateServiceImpl.class.getDeclaredMethod(
                "hasBlockingRecheck",
                ReviewAggregateVO.ReviewRecheckSummary.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, r);
    }

    private static String callBuildRecheckSignal(List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck) throws Exception {
        Method m = ReviewAggregateServiceImpl.class.getDeclaredMethod(
                "buildRecheckSignal",
                List.class);
        m.setAccessible(true);
        return (String) m.invoke(null, pushRecheck);
    }

    @SuppressWarnings("unchecked")
    private static ReviewAggregateVO.RuleVersionLogSummary callMarkLatestReviewLinkedLog(List<ReviewAggregateVO.RuleVersionLogSummary> logs) throws Exception {
        Method m = ReviewAggregateServiceImpl.class.getDeclaredMethod(
                "markLatestReviewLinkedLog",
                List.class);
        m.setAccessible(true);
        return (ReviewAggregateVO.RuleVersionLogSummary) m.invoke(null, logs);
    }

    private static ReviewAggregateVO.GovernanceSummary callBuildGovernanceSummary(
            org.example.trademodel.entity.ReviewResultDO reviewResult,
            ReviewAggregateVO.RuleVersionLogSummary linkedLog) throws Exception {
        Method m = ReviewAggregateServiceImpl.class.getDeclaredMethod(
                "buildGovernanceSummary",
                org.example.trademodel.entity.ReviewResultDO.class,
                ReviewAggregateVO.RuleVersionLogSummary.class);
        m.setAccessible(true);
        return (ReviewAggregateVO.GovernanceSummary) m.invoke(null, reviewResult, linkedLog);
    }

    private static int callClampLimit(int limit) throws Exception {
        Method m = ReviewAggregateServiceImpl.class.getDeclaredMethod("clampLimit", int.class);
        m.setAccessible(true);
        return (int) m.invoke(null, limit);
    }

    private static String callNormalizeSection(String section) throws Exception {
        Method m = ReviewAggregateServiceImpl.class.getDeclaredMethod("normalizeSection", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, section);
    }

    @Test
    void hasBlockingRecheck_validExecutable_isNotBlocking() throws Exception {
        assertThat(callHasBlockingRecheck("VALID_EXECUTABLE")).isFalse();
    }

    @Test
    void hasBlockingRecheck_validWaiting_isNotBlocking() throws Exception {
        assertThat(callHasBlockingRecheck("VALID_WAITING")).isFalse();
    }

    @Test
    void hasBlockingRecheck_drifted_isBlocking() throws Exception {
        assertThat(callHasBlockingRecheck("DRIFTED")).isTrue();
    }

    @Test
    void buildRecheckSignal_waitingOnly_returnsWaitingMessage() throws Exception {
        ReviewAggregateVO.ReviewRecheckSummary r = new ReviewAggregateVO.ReviewRecheckSummary();
        r.setRecheckStatus("VALID_WAITING");

        ReviewAggregateVO.ReviewPushWithRecheck bundle = new ReviewAggregateVO.ReviewPushWithRecheck();
        bundle.setRechecks(Arrays.asList(r));

        String signal = callBuildRecheckSignal(Arrays.asList(bundle));
        assertThat(signal).contains("等待");
    }

    @Test
    void buildRecheckSignal_drifted_containsBlockedAndDrift() throws Exception {
        ReviewAggregateVO.ReviewRecheckSummary r = new ReviewAggregateVO.ReviewRecheckSummary();
        r.setRecheckStatus("DRIFTED");

        ReviewAggregateVO.ReviewPushWithRecheck bundle = new ReviewAggregateVO.ReviewPushWithRecheck();
        bundle.setRechecks(Arrays.asList(r));

        String signal = callBuildRecheckSignal(Arrays.asList(bundle));
        assertThat(signal).contains("阻断");
        assertThat(signal).contains("价格漂移");
    }

    @Test
    void clampLimit_outOfRange_isGuarded() throws Exception {
        assertThat(callClampLimit(-1)).isEqualTo(20);
        assertThat(callClampLimit(0)).isEqualTo(20);
        assertThat(callClampLimit(10)).isEqualTo(10);
        assertThat(callClampLimit(500)).isEqualTo(50);
    }

    @Test
    void normalizeSection_blank_defaultsToPushRecheck() throws Exception {
        assertThat(callNormalizeSection(null)).isEqualTo("pushRecheck");
        assertThat(callNormalizeSection("   ")).isEqualTo("pushRecheck");
        assertThat(callNormalizeSection("alerts")).isEqualTo("alerts");
    }

    @Test
    void normalizeSection_invalid_throws() {
        assertThatThrownBy(() -> callNormalizeSection("foo"))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .rootCause()
                .hasMessageContaining("unsupported detail section");
    }

    @Test
    void markLatestReviewLinkedLog_marksFirstMatchedLogOnly() throws Exception {
        ReviewAggregateVO.RuleVersionLogSummary first = new ReviewAggregateVO.RuleVersionLogSummary();
        first.setId("l1");
        first.setChangeCategory("REVIEW_FEEDBACK_SAVED");
        ReviewAggregateVO.RuleVersionLogSummary second = new ReviewAggregateVO.RuleVersionLogSummary();
        second.setId("l2");
        second.setChangeCategory("REVIEW_FEEDBACK_SAVED");
        ReviewAggregateVO.RuleVersionLogSummary third = new ReviewAggregateVO.RuleVersionLogSummary();
        third.setId("l3");
        third.setChangeCategory("RULE_PUBLISHED");

        ReviewAggregateVO.RuleVersionLogSummary linked = callMarkLatestReviewLinkedLog(Arrays.asList(first, second, third));
        assertThat(linked).isSameAs(first);
        assertThat(first.getLinkedToLatestReview()).isTrue();
        assertThat(second.getLinkedToLatestReview()).isFalse();
        assertThat(third.getLinkedToLatestReview()).isFalse();
    }

    @Test
    void buildGovernanceSummary_withContentAndSuggestion_returnsGovernanceReady() throws Exception {
        org.example.trademodel.entity.ReviewResultDO row = new org.example.trademodel.entity.ReviewResultDO();
        row.setErrorType("DRIFT");
        row.setActualOutcome("价格漂移");
        row.setAdjustmentSuggestion("下调触发阈值");
        row.setUpdateTime(LocalDateTime.of(2026, 4, 17, 10, 0));

        ReviewAggregateVO.RuleVersionLogSummary linkedLog = new ReviewAggregateVO.RuleVersionLogSummary();
        linkedLog.setId("log-1");
        linkedLog.setCreatedAt("2026-04-17 10:01:00");
        linkedLog.setChangeCategory("REVIEW_FEEDBACK_SAVED");

        ReviewAggregateVO.GovernanceSummary summary = callBuildGovernanceSummary(row, linkedLog);
        assertThat(summary.getGovernanceStatus()).isEqualTo("READY_FOR_GOVERNANCE_INPUT");
        assertThat(summary.getHasReviewContent()).isTrue();
        assertThat(summary.getPrimaryIssueType()).isEqualTo("DRIFT");
        assertThat(summary.getGovernanceActionHint()).contains("adjustmentSuggestion");
        assertThat(summary.getLinkedRuleLogId()).isEqualTo("log-1");
    }
}

