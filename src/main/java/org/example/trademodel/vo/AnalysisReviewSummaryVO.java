package org.example.trademodel.vo;

import java.time.LocalDateTime;

/**
 * Analysis 级复盘摘要（{@code tm_review_result}），用于工作台 detail。
 * 不包含交易级（trade）复盘或规则改进建议状态。
 */
public class AnalysisReviewSummaryVO {

    private String reviewStatus;
    private String reviewStatusText;
    private Boolean reviewCompleted;
    private Boolean reviewHasContent;
    private LocalDateTime reviewUpdatedAt;
    private String reviewEntryUrl;

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewStatusText() {
        return reviewStatusText;
    }

    public void setReviewStatusText(String reviewStatusText) {
        this.reviewStatusText = reviewStatusText;
    }

    public Boolean getReviewCompleted() {
        return reviewCompleted;
    }

    public void setReviewCompleted(Boolean reviewCompleted) {
        this.reviewCompleted = reviewCompleted;
    }

    public Boolean getReviewHasContent() {
        return reviewHasContent;
    }

    public void setReviewHasContent(Boolean reviewHasContent) {
        this.reviewHasContent = reviewHasContent;
    }

    public LocalDateTime getReviewUpdatedAt() {
        return reviewUpdatedAt;
    }

    public void setReviewUpdatedAt(LocalDateTime reviewUpdatedAt) {
        this.reviewUpdatedAt = reviewUpdatedAt;
    }

    public String getReviewEntryUrl() {
        return reviewEntryUrl;
    }

    public void setReviewEntryUrl(String reviewEntryUrl) {
        this.reviewEntryUrl = reviewEntryUrl;
    }
}
