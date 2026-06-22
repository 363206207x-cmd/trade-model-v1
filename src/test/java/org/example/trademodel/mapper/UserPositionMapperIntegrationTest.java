package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.UserPositionDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class UserPositionMapperIntegrationTest {
    @Autowired
    private UserPositionMapper userPositionMapper;

    @Test
    void insertSelectAndManualClosePersistManualSafetyFields() {
        UserPositionDO row = row("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 0));

        userPositionMapper.insert(row);

        assertThat(row.getId()).isNotNull();
        UserPositionDO persisted = userPositionMapper.selectById(row.getId());
        assertThat(persisted.getAssetSymbol()).isEqualTo("BTCUSDT");
        assertThat(persisted.getSide()).isEqualTo("LONG");
        assertThat(persisted.getStatus()).isEqualTo("OPEN");
        assertThat(persisted.getEntryPrice()).isEqualByComparingTo("100.50000000");
        assertThat(persisted.getSourceType()).isEqualTo("MANUAL");
        assertThat(persisted.getManualReviewRequired()).isTrue();
        assertThat(persisted.getNotTradeInstruction()).isTrue();
        assertThat(persisted.getNotAutoTrading()).isTrue();
        assertThat(persisted.getNotOrderExecution()).isTrue();
        assertThat(persisted.getNotPositionSync()).isTrue();

        int updated = userPositionMapper.manualClose(
                row.getId(),
                LocalDateTime.of(2026, 6, 22, 9, 0),
                new BigDecimal("105.25"),
                "manual close",
                LocalDateTime.of(2026, 6, 22, 9, 0)
        );

        assertThat(updated).isEqualTo(1);
        UserPositionDO closed = userPositionMapper.selectById(row.getId());
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getClosePrice()).isEqualByComparingTo("105.25000000");
        assertThat(closed.getCloseReason()).isEqualTo("manual close");
    }

    @Test
    void listOpenPositionsExcludesClosedRows() {
        userPositionMapper.insert(row("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 0)));
        userPositionMapper.insert(row("ETHUSDT", "PARTIALLY_CLOSED", LocalDateTime.of(2026, 6, 22, 8, 5)));
        userPositionMapper.insert(row("SOLUSDT", "CLOSED", LocalDateTime.of(2026, 6, 22, 8, 10)));

        List<UserPositionDO> rows = userPositionMapper.listOpenPositions();

        assertThat(rows).extracting(UserPositionDO::getStatus)
                .contains("OPEN", "PARTIALLY_CLOSED")
                .doesNotContain("CLOSED");
        assertThat(rows).extracting(UserPositionDO::getAssetSymbol)
                .doesNotContain("SOLUSDT");
    }

    private static UserPositionDO row(String symbol, String status, LocalDateTime openedAt) {
        UserPositionDO row = new UserPositionDO();
        row.setAssetSymbol(symbol);
        row.setSide("LONG");
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100.50"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setLeverage(new BigDecimal("2"));
        row.setStopLoss(new BigDecimal("95.00"));
        row.setTakeProfit(new BigDecimal("120.00"));
        row.setOpenedAt(openedAt);
        if ("CLOSED".equals(status)) {
            row.setClosedAt(openedAt.plusHours(1));
            row.setClosePrice(new BigDecimal("105.25"));
            row.setCloseReason("closed fixture");
        }
        row.setSourceType("MANUAL");
        row.setSourceRefId("manual-test");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        row.setCreatedAt(openedAt);
        row.setUpdatedAt(openedAt);
        return row;
    }
}
