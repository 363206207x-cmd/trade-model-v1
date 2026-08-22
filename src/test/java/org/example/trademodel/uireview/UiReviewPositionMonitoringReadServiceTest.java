package org.example.trademodel.uireview;

import org.example.trademodel.service.PositionMonitoringProjectionService;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.DashboardHomeVO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UiReviewPositionMonitoringReadServiceTest {
    private final UiReviewPositionMonitoringReadService service =
            new UiReviewPositionMonitoringReadService();

    @Test
    void homeListAndDetailShareTheSameThreePositionIdentities() {
        List<DashboardHomeVO.PositionVO> home = service.homeTopThree(null);
        PositionMonitoringProjectionService.CollectionProjection list = service.listForUser(1L);

        assertThat(home).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactly(7101L, 7102L, 7103L);
        assertThat(list.positions()).hasSize(3);
        assertThat(list.activeCount()).isEqualTo(3);
        assertThat(list.accountRiskCoverageState()).isEqualTo("COMPLETE");
        assertThat(service.aggregate().getActiveCount()).isEqualTo(list.activeCount());
        assertThat(service.aggregate().getHighestTrustedRisk()).isEqualTo("EXTREME");

        for (PositionMonitoringProjectionService.ItemProjection row : list.positions()) {
            PositionMonitoringProjectionService.ItemProjection detail =
                    service.findForUser(1L, row.position().getId());
            assertThat(detail.position().getId()).isEqualTo(row.position().getId());
            assertThat(detail.monitor().getPositionId()).isEqualTo(row.position().getId());
            assertThat(detail.position().getAssetSymbol()).isEqualTo(row.monitor().getSymbol());
            assertThat(detail.position().getSide()).isEqualTo(row.monitor().getDirection());
            assertThat(detail.position().getSourceType()).isEqualTo(row.monitor().getSourceType());
        }
    }

    @Test
    void eachDetailIsIsolatedAndUnknownIdFailsClosed() {
        assertDetail(7101L, "BTCUSDT", "LONG", "SYSTEM_PLAN_POSITION");
        assertDetail(7102L, "ETHUSDT", "SHORT", "MANUAL_INDEPENDENT");
        assertDetail(7103L, "SOLUSDT", "LONG", "SYSTEM_PLAN_POSITION");
        assertThatThrownBy(() -> service.findForUser(1L, 7999L))
                .isInstanceOf(UserPositionNotFoundException.class);
    }

    @Test
    void uiReviewSourceIsProfileLimitedAndReadOnly() {
        try (AnnotationConfigApplicationContext normal = new AnnotationConfigApplicationContext()) {
            normal.register(UiReviewPositionMonitoringReadService.class);
            normal.refresh();
            assertThat(normal.getBeansOfType(UiReviewPositionMonitoringReadService.class)).isEmpty();
        }
        try (AnnotationConfigApplicationContext review = new AnnotationConfigApplicationContext()) {
            review.getEnvironment().setActiveProfiles("ui-review");
            review.register(UiReviewPositionMonitoringReadService.class);
            review.refresh();
            assertThat(review.getBeansOfType(UiReviewPositionMonitoringReadService.class)).hasSize(1);
        }
        assertThat(service.historyForUser(1L, 100).positions()).isEmpty();
    }

    private void assertDetail(long id, String symbol, String side, String sourceType) {
        PositionMonitoringProjectionService.ItemProjection detail = service.findForUser(1L, id);
        assertThat(detail.position().getId()).isEqualTo(id);
        assertThat(detail.monitor().getPositionId()).isEqualTo(id);
        assertThat(detail.position().getAssetSymbol()).isEqualTo(symbol);
        assertThat(detail.monitor().getSymbol()).isEqualTo(symbol);
        assertThat(detail.position().getSide()).isEqualTo(side);
        assertThat(detail.position().getSourceType()).isEqualTo(sourceType);
        assertThat(detail.position().getEntryPrice()).isNotNull();
        assertThat(detail.position().getOpenedAt()).isNotNull();
    }
}
