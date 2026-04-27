package org.example.trademodel.vo;

import java.util.List;

/**
 * Step 3: 复盘首屏摘要（轻载）。
 * 仅保留首屏关键信息和明细可用性，不内嵌大列表。
 */
public class ReviewAggregateSummaryVO {

    private ReviewAggregateVO.ReviewRunSummary run;
    private ReviewAggregateVO.ReviewDecisionSummary decision;
    private ReviewAggregateVO.ReviewPlanSummary plan;
    private ReviewAggregateVO.ReviewClosureSummary reviewClosure;
    private List<DetailSectionMeta> detailSections;

    public ReviewAggregateVO.ReviewRunSummary getRun() {
        return run;
    }

    public void setRun(ReviewAggregateVO.ReviewRunSummary run) {
        this.run = run;
    }

    public ReviewAggregateVO.ReviewDecisionSummary getDecision() {
        return decision;
    }

    public void setDecision(ReviewAggregateVO.ReviewDecisionSummary decision) {
        this.decision = decision;
    }

    public ReviewAggregateVO.ReviewPlanSummary getPlan() {
        return plan;
    }

    public void setPlan(ReviewAggregateVO.ReviewPlanSummary plan) {
        this.plan = plan;
    }

    public ReviewAggregateVO.ReviewClosureSummary getReviewClosure() {
        return reviewClosure;
    }

    public void setReviewClosure(ReviewAggregateVO.ReviewClosureSummary reviewClosure) {
        this.reviewClosure = reviewClosure;
    }

    public List<DetailSectionMeta> getDetailSections() {
        return detailSections;
    }

    public void setDetailSections(List<DetailSectionMeta> detailSections) {
        this.detailSections = detailSections;
    }

    public static class DetailSectionMeta {
        private String section;
        private Integer total;
        private Integer recommendedLimit;

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }

        public Integer getRecommendedLimit() {
            return recommendedLimit;
        }

        public void setRecommendedLimit(Integer recommendedLimit) {
            this.recommendedLimit = recommendedLimit;
        }
    }
}
