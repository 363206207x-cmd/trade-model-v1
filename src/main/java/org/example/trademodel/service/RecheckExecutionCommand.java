package org.example.trademodel.service;

/**
 * 统一重检执行指令：统一手动触发、调度触发、回放触发的运行语义。
 */
public class RecheckExecutionCommand {

    private String triggerSource;
    private String dispatchBatchId;
    private String dispatchInstructionId;
    private Integer retryAttempt;
    private Integer maxAttempts;
    private Integer retryBackoffMinutes;
    private Long replayFromLogId;

    public static RecheckExecutionCommand manual() {
        RecheckExecutionCommand cmd = new RecheckExecutionCommand();
        cmd.setTriggerSource("MANUAL");
        cmd.setRetryAttempt(1);
        return cmd;
    }

    public static RecheckExecutionCommand scheduled(String batchId,
                                                    String instructionId,
                                                    int retryAttempt,
                                                    int maxAttempts,
                                                    int retryBackoffMinutes) {
        RecheckExecutionCommand cmd = new RecheckExecutionCommand();
        cmd.setTriggerSource("SCHEDULED");
        cmd.setDispatchBatchId(batchId);
        cmd.setDispatchInstructionId(instructionId);
        cmd.setRetryAttempt(retryAttempt);
        cmd.setMaxAttempts(maxAttempts);
        cmd.setRetryBackoffMinutes(retryBackoffMinutes);
        return cmd;
    }

    public static RecheckExecutionCommand replay(String batchId,
                                                 String instructionId,
                                                 Long replayFromLogId) {
        RecheckExecutionCommand cmd = new RecheckExecutionCommand();
        cmd.setTriggerSource("REPLAY");
        cmd.setDispatchBatchId(batchId);
        cmd.setDispatchInstructionId(instructionId);
        cmd.setRetryAttempt(1);
        cmd.setReplayFromLogId(replayFromLogId);
        return cmd;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
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

    public Integer getRetryAttempt() {
        return retryAttempt;
    }

    public void setRetryAttempt(Integer retryAttempt) {
        this.retryAttempt = retryAttempt;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Integer getRetryBackoffMinutes() {
        return retryBackoffMinutes;
    }

    public void setRetryBackoffMinutes(Integer retryBackoffMinutes) {
        this.retryBackoffMinutes = retryBackoffMinutes;
    }

    public Long getReplayFromLogId() {
        return replayFromLogId;
    }

    public void setReplayFromLogId(Long replayFromLogId) {
        this.replayFromLogId = replayFromLogId;
    }
}
