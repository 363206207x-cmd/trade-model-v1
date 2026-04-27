package org.example.trademodel.dto.req;

public class PushRecheckDispatchConfigRequest {

    private Integer limit;
    private Integer maxAttempts;
    private Integer minRetryMinutes;

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Integer getMinRetryMinutes() {
        return minRetryMinutes;
    }

    public void setMinRetryMinutes(Integer minRetryMinutes) {
        this.minRetryMinutes = minRetryMinutes;
    }
}
