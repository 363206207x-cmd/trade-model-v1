package org.example.trademodel.service;

import org.example.trademodel.dto.req.CloseManualPositionReq;
import org.example.trademodel.entity.PositionMonitorRecordDO;
import org.example.trademodel.entity.PositionTradeResultDO;
import org.example.trademodel.vo.RealPositionVO;

public interface PositionTradeResultService {
    PositionTradeResultDO createFromClose(
            RealPositionVO position,
            CloseManualPositionReq req,
            PositionMonitorRecordDO latestMonitorRecord
    );
}
