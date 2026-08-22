package org.example.trademodel.uireview;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UiReviewAssetPoolServiceTest {
    @Test
    void reviewMembershipIsInMemoryAndSupportsAllThreeSearchActionStates() {
        UiReviewAssetPoolService service = new UiReviewAssetPoolService();

        assertThat(service.listForUser(1L)).hasSize(6);
        assertThat(service.searchMarket("aave", 8)).singleElement()
                .satisfies(asset -> assertThat(asset.symbol()).isEqualTo("AAVEUSDT"));
        assertThat(service.listForUser(1L)).extracting("symbol").doesNotContain("AAVEUSDT");

        service.addForUser(1L, "AAVEUSDT", true);

        assertThat(service.listForUser(1L)).extracting("symbol").contains("AAVEUSDT");
        assertThat(service.addForUser(1L, "AAVEUSDT", true).symbol()).isEqualTo("AAVEUSDT");
        assertThat(service.listForUser(1L)).hasSize(7);
    }
}
