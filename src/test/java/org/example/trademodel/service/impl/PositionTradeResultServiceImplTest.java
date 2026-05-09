package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.CloseManualPositionReq;
import org.example.trademodel.entity.PositionTradeResultDO;
import org.example.trademodel.vo.RealPositionVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PositionTradeResultServiceImplTest {

    private final PositionTradeResultServiceImpl service = new PositionTradeResultServiceImpl();

    @Test
    void long_calculates_realized_pnl_and_pct() {
        RealPositionVO position = basePosition("LONG", new BigDecimal("100"), new BigDecimal("2"));
        CloseManualPositionReq req = new CloseManualPositionReq();
        req.setExitPrice(new BigDecimal("110"));

        PositionTradeResultDO out = service.createFromClose(position, req, null);

        assertThat(out.getRealizedPnl()).isEqualByComparingTo(new BigDecimal("20.00000000"));
        assertThat(out.getRealizedPnlPct()).isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    void short_calculates_realized_pnl_and_pct() {
        RealPositionVO position = basePosition("SHORT", new BigDecimal("100"), new BigDecimal("2"));
        CloseManualPositionReq req = new CloseManualPositionReq();
        req.setExitPrice(new BigDecimal("90"));

        PositionTradeResultDO out = service.createFromClose(position, req, null);

        assertThat(out.getRealizedPnl()).isEqualByComparingTo(new BigDecimal("20.00000000"));
        assertThat(out.getRealizedPnlPct()).isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    void quantity_null_sets_realized_pnl_null_and_pct_present() {
        RealPositionVO position = basePosition("LONG", new BigDecimal("100"), null);
        CloseManualPositionReq req = new CloseManualPositionReq();
        req.setExitPrice(new BigDecimal("105"));

        PositionTradeResultDO out = service.createFromClose(position, req, null);

        assertThat(out.getRealizedPnl()).isNull();
        assertThat(out.getRealizedPnlPct()).isEqualByComparingTo(new BigDecimal("5.0000"));
    }

    private RealPositionVO basePosition(String side, BigDecimal avgOpenPrice, BigDecimal qty) {
        RealPositionVO vo = new RealPositionVO();
        vo.setPositionId("p-1");
        vo.setSymbol("BTCUSDT");
        vo.setPositionSide(side);
        vo.setAvgOpenPrice(avgOpenPrice);
        vo.setPositionQuantity(qty);
        return vo;
    }
}
