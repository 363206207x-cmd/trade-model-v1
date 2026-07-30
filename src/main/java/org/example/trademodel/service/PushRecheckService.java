package org.example.trademodel.service;

import org.example.trademodel.vo.PushRecheckLogItemVO;
import org.example.trademodel.vo.PushRecheckOpsOverviewVO;
import org.example.trademodel.vo.PushRecheckReplaySummaryVO;

import java.math.BigDecimal;
import java.util.List;

public interface PushRecheckService {
    default RecheckResult recheck(Long pushId, BigDecimal currentPrice) {
        return recheck(pushId, currentPrice, RecheckExecutionCommand.manual());
    }

    RecheckResult recheck(Long pushId, BigDecimal currentPrice, RecheckExecutionCommand command);

    /**
     * Disabled raw read. Persisted rows have no authoritative owner relation,
     * so this boundary rejects before repository access.
     */
    PushRecheckLogItemVO getLatestLog(Long pushId);

    /**
     * Disabled raw read. Persisted rows have no authoritative owner relation,
     * so this boundary rejects before repository access.
     */
    List<PushRecheckLogItemVO> listLogs(Long pushId);

    /**
     * Disabled user replay boundary. Scheduled internal execution does not use
     * this method.
     */
    List<RecheckResult> replayByDispatch(String dispatchBatchId, String dispatchInstructionId);

    /**
     * Disabled global replay-summary boundary.
     */
    PushRecheckReplaySummaryVO summarizeReplayByDispatch(String dispatchBatchId, String dispatchInstructionId);

    /**
     * Disabled global operations boundary.
     */
    PushRecheckOpsOverviewVO getOpsOverview(String dispatchBatchId,
                                            String dispatchInstructionId,
                                            Integer auditLimit,
                                            Integer logLimit);
}
