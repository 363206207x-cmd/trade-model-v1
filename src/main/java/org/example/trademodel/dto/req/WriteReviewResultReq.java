package org.example.trademodel.dto.req;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存复盘：客户端提交字段；id / 时间戳由服务端写入。
 */
public class WriteReviewResultReq {

    @NotBlank
    private String analysisId;
    private String errorType;
    private String actualOutcome;
    private String adjustmentSuggestion;

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public String getAdjustmentSuggestion() {
        return adjustmentSuggestion;
    }

    public void setAdjustmentSuggestion(String adjustmentSuggestion) {
        this.adjustmentSuggestion = adjustmentSuggestion;
    }
}
