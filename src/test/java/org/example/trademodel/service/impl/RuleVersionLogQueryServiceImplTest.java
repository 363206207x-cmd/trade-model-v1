package org.example.trademodel.service.impl;

import org.example.trademodel.entity.RuleVersionLogDO;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.vo.ReviewAggregateVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleVersionLogQueryServiceImplTest {

    @Mock
    private RuleVersionLogMapper ruleVersionLogMapper;

    @Test
    void listByAnalysisId_fillsFieldsFromChangeSummaryWhenStructuredColumnsMissing() {
        RuleVersionLogQueryServiceImpl service = new RuleVersionLogQueryServiceImpl(ruleVersionLogMapper);

        RuleVersionLogDO row = new RuleVersionLogDO();
        row.setId("log-1");
        row.setAnalysisId(null);
        row.setRuleVersion("v8");
        row.setErrorType(null);
        row.setChangeCategory(null);
        row.setChangeSummary("REVIEW_FEEDBACK_SAVED;analysisId=a-100;ruleVersion=v8;errorType=MISS");
        row.setChangeDetail("detail");
        row.setOperator("SYSTEM");
        row.setRollbackFlag("N");
        row.setCreatedAt("2026-04-16 10:00:00");

        when(ruleVersionLogMapper.queryLogs("a-100", null, null, null, null, null, null, null, null, 20))
                .thenReturn(List.of(row));

        List<ReviewAggregateVO.RuleVersionLogSummary> out = service.listByAnalysisId("a-100", 0);

        assertThat(out).hasSize(1);
        ReviewAggregateVO.RuleVersionLogSummary item = out.get(0);
        assertThat(item.getAnalysisId()).isEqualTo("a-100");
        assertThat(item.getErrorType()).isEqualTo("MISS");
        assertThat(item.getChangeCategory()).isEqualTo("REVIEW_FEEDBACK_SAVED");
        assertThat(item.getFallbackMatched()).isTrue();
        verify(ruleVersionLogMapper).queryLogs("a-100", null, null, null, null, null, null, null, null, 20);
    }

    @Test
    void query_shouldTrimAndPassSearchFilters() {
        RuleVersionLogQueryServiceImpl service = new RuleVersionLogQueryServiceImpl(ruleVersionLogMapper);
        when(ruleVersionLogMapper.queryLogs("a-100", "v9", "SYSTEM", "N", "MISS",
                "REVIEW_FEEDBACK_SAVED", "hot reset", "2026-04-01 00:00:00", "2026-04-30 23:59:59", 50))
                .thenReturn(List.of());

        service.query(" a-100 ", " v9 ", " SYSTEM ", " N ", " MISS ",
                " REVIEW_FEEDBACK_SAVED ", " hot reset ", " 2026-04-01 00:00:00 ",
                " 2026-04-30 23:59:59 ", 999);

        verify(ruleVersionLogMapper).queryLogs("a-100", "v9", "SYSTEM", "N", "MISS",
                "REVIEW_FEEDBACK_SAVED", "hot reset", "2026-04-01 00:00:00", "2026-04-30 23:59:59", 50);
    }
}
