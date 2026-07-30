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
     * Legacy raw read. Persisted rows have no authoritative owner relation, so
     * this service boundary always fails closed with null.
     */
    PushRecheckLogItemVO getLatestLog(Long pushId);

    /**
     * Legacy raw read. Persisted rows have no authoritative owner relation, so
     * this service boundary always fails closed with an empty list.
     */
    List<PushRecheckLogItemVO> listLogs(Long pushId);

    /**
     * 最小回放链路：按批次或指令定位历史重检日志，并使用历史价格重放。
     */
    List<RecheckResult> replayByDispatch(String dispatchBatchId, String dispatchInstructionId);

    /**
     * 回放结果最小摘要：按批次或指令聚合 recheck 日志，返回运营查询所需最小字段。
     */
    PushRecheckReplaySummaryVO summarizeReplayByDispatch(String dispatchBatchId, String dispatchInstructionId);

    /**
     * 运维最小总览：拼接配置、配置审计、回放摘要与最近执行日志。
     */
    PushRecheckOpsOverviewVO getOpsOverview(String dispatchBatchId,
                                            String dispatchInstructionId,
                                            Integer auditLimit,
                                            Integer logLimit);
}
