package org.example.trademodel.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class ReviewCenterTemplateTest {
    private static final Path REVIEW_TEMPLATE = Path.of("src/main/resources/templates/review.html");

    @Test
    void reviewDashboardFetchesReadonlyCenterApiOnly() throws Exception {
        String html = Files.readString(REVIEW_TEMPLATE);

        assertThat(html).contains("/api/review/center");
        assertThat(html).doesNotContain("/api/review/aggregate/dashboard");
        assertThat(html).contains("review-center-positions-body");
        assertThat(html).contains("review-center-opportunities-body");
        assertThat(html).contains("review-center-pushes-body");
        assertThat(html).contains("review-center-rules-body");
        assertThat(html).contains("暂无持仓复盘记录");
        assertThat(html).contains("暂无机会复盘记录");
        assertThat(html).contains("暂无推送复盘记录");
        assertThat(html).contains("暂无规则反馈记录");
    }

    @Test
    void visibleDomContainsNoPositionSourceUnverified() throws Exception {
        String html = Files.readString(REVIEW_TEMPLATE)
                + Files.readString(Path.of("src/main/resources/templates/dashboard.html"));

        assertThat(html).doesNotContain("POSITION_SOURCE_UNVERIFIED");
    }
}
