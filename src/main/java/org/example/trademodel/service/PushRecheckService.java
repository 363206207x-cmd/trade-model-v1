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

    /** 按 log_id 倒序取第一条，无记录时返回 null */
    PushRecheckLogItemVO getLatestLog(Long pushId);

    /** 同 pushId 下全部日志，新在前（与 Mapper 排序一致） */
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
