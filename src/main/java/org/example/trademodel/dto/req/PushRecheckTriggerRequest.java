package org.example.trademodel.dto.req;

import java.math.BigDecimal;

/**
 * POST 触发二次校验时的入参：必须提供当前价（与 {@link org.example.trademodel.service.PushRecheckService#recheck} 一致）。
 */
public class PushRecheckTriggerRequest {

    private BigDecimal currentPrice;
    private String dispatchBatchId;
    private String dispatchInstructionId;

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getDispatchBatchId() {
        return dispatchBatchId;
    }

    public void setDispatchBatchId(String dispatchBatchId) {
        this.dispatchBatchId = dispatchBatchId;
    }

    public String getDispatchInstructionId() {
        return dispatchInstructionId;
    }

    public void setDispatchInstructionId(String dispatchInstructionId) {
        this.dispatchInstructionId = dispatchInstructionId;
    }
}
