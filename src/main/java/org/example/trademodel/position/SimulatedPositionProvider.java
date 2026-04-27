package org.example.trademodel.position;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SimulatedPositionProvider implements PositionProvider {

    @Override
    public PositionProviderResult fetchOpenPositions() {
        LocalDateTime now = LocalDateTime.now();
        List<PositionSnapshot> positions = new ArrayList<>();

        PositionSnapshot btcLong = new PositionSnapshot();
        btcLong.setSymbol("BTCUSDT");
        btcLong.setPositionSide("LONG");
        btcLong.setAvgOpenPrice(new BigDecimal("63520.50"));
        btcLong.setPositionOpenTime(now.minusHours(4));
        btcLong.setPositionQuantity(new BigDecimal("0.015"));
        btcLong.setUnrealizedPnlPct(new BigDecimal("1.82"));
        btcLong.setMarkPrice(new BigDecimal("64678.20"));
        btcLong.setBreakEvenPrice(new BigDecimal("63610.00"));
        btcLong.setLiquidationPrice(new BigDecimal("58220.00"));
        positions.add(btcLong);

        PositionSnapshot ethShort = new PositionSnapshot();
        ethShort.setSymbol("ETHUSDT");
        ethShort.setPositionSide("SHORT");
        ethShort.setAvgOpenPrice(new BigDecimal("3120.80"));
        ethShort.setPositionOpenTime(now.minusHours(2));
        ethShort.setPositionQuantity(new BigDecimal("0.45"));
        ethShort.setUnrealizedPnlPct(new BigDecimal("-0.64"));
        ethShort.setMarkPrice(new BigDecimal("3140.76"));
        ethShort.setBreakEvenPrice(new BigDecimal("3116.20"));
        ethShort.setLiquidationPrice(new BigDecimal("3560.00"));
        positions.add(ethShort);

        return new PositionProviderResult("SIMULATED", "simulated-provider-v1", positions);
    }
}
