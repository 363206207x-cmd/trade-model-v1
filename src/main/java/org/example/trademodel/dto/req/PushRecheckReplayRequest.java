package org.example.trademodel.dto.req;

public class PushRecheckReplayRequest {

    private String dispatchBatchId;
    private String dispatchInstructionId;

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
