package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.entity.PersonalUserDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class UserPositionMapperIntegrationTest {
    @Autowired
    private UserPositionMapper userPositionMapper;
    @Autowired
    private PersonalUserMapper personalUserMapper;

    @Test
    void insertSelectAndManualClosePersistManualSafetyFields() {
        Long userId = userId("mapper-owner-a");
        UserPositionDO row = row("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 0));
        row.setUserId(userId);

        userPositionMapper.insert(row);

        assertThat(row.getId()).isNotNull();
        UserPositionDO persisted = userPositionMapper.selectByIdAndUserId(row.getId(), userId);
        assertThat(persisted.getUserId()).isEqualTo(userId);
        assertThat(persisted.getAssetSymbol()).isEqualTo("BTCUSDT");
        assertThat(persisted.getSide()).isEqualTo("LONG");
        assertThat(persisted.getStatus()).isEqualTo("OPEN");
        assertThat(persisted.getEntryPrice()).isEqualByComparingTo("100.50000000");
        assertThat(persisted.getSourceType()).isEqualTo("MANUAL_INDEPENDENT");
        assertThat(persisted.getManualReviewRequired()).isTrue();
        assertThat(persisted.getNotTradeInstruction()).isTrue();
        assertThat(persisted.getNotAutoTrading()).isTrue();
        assertThat(persisted.getNotOrderExecution()).isTrue();
        assertThat(persisted.getNotPositionSync()).isTrue();

        int updated = userPositionMapper.manualCloseByIdAndUserId(
                row.getId(),
                userId,
                LocalDateTime.of(2026, 6, 22, 9, 0),
                new BigDecimal("105.25"),
                "manual close",
                LocalDateTime.of(2026, 6, 22, 9, 0)
        );

        assertThat(updated).isEqualTo(1);
        UserPositionDO closed = userPositionMapper.selectByIdAndUserId(row.getId(), userId);
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getClosePrice()).isEqualByComparingTo("105.25000000");
        assertThat(closed.getCloseReason()).isEqualTo("manual close");
    }

    @Test
    void listOpenPositionsExcludesClosedRows() {
        Long userId = userId("mapper-owner-b");
        UserPositionDO open = row("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 0));
        open.setUserId(userId);
        UserPositionDO partial = row("ETHUSDT", "PARTIALLY_CLOSED", LocalDateTime.of(2026, 6, 22, 8, 5));
        partial.setUserId(userId);
        UserPositionDO closed = row("SOLUSDT", "CLOSED", LocalDateTime.of(2026, 6, 22, 8, 10));
        closed.setUserId(userId);
        userPositionMapper.insert(open);
        userPositionMapper.insert(partial);
        userPositionMapper.insert(closed);

        List<UserPositionDO> rows = userPositionMapper.listOpenByUserId(userId);

        assertThat(rows).extracting(UserPositionDO::getStatus)
                .containsExactly("PARTIALLY_CLOSED", "OPEN")
                .doesNotContain("CLOSED");
        assertThat(rows).extracting(UserPositionDO::getAssetSymbol)
                .doesNotContain("SOLUSDT");
    }

    @Test
    void ownerQueriesIsolateSameSymbolAndRejectMissingOwner() {
        Long userA = userId("mapper-owner-isolation-a");
        Long userB = userId("mapper-owner-isolation-b");
        UserPositionDO ownedA = row("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 0));
        ownedA.setUserId(userA);
        UserPositionDO ownedB = row("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 1));
        ownedB.setUserId(userB);
        UserPositionDO missingOwner = row("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 2));
        UserPositionDO partialA = row(
                "BTCUSDT", "PARTIALLY_CLOSED", LocalDateTime.of(2026, 6, 22, 8, 3));
        partialA.setUserId(userA);
        UserPositionDO partialB = row(
                "BTCUSDT", "PARTIALLY_CLOSED", LocalDateTime.of(2026, 6, 22, 8, 4));
        partialB.setUserId(userB);
        userPositionMapper.insert(ownedA);
        userPositionMapper.insert(ownedB);
        assertThatThrownBy(() -> userPositionMapper.insert(missingOwner))
                .isInstanceOf(RuntimeException.class);
        userPositionMapper.insert(partialA);
        userPositionMapper.insert(partialB);

        assertThat(userPositionMapper.listOpenByUserId(userA))
                .extracting(UserPositionDO::getId)
                .containsExactly(partialA.getId(), ownedA.getId());
        assertThat(userPositionMapper.listOpenByUserId(userB))
                .extracting(UserPositionDO::getId)
                .containsExactly(partialB.getId(), ownedB.getId());
        assertThat(userPositionMapper.selectByIdAndUserId(ownedB.getId(), userA)).isNull();
        assertThat(userPositionMapper.listClaimedOpenForSystemMonitoring())
                .extracting(UserPositionDO::getId)
                .contains(ownedA.getId(), ownedB.getId(), partialA.getId(), partialB.getId());
    }

    @Test
    void conditionalCloseRequiresExactOwnerAndActiveState() {
        Long userA = userId("mapper-close-owner-a");
        Long userB = userId("mapper-close-owner-b");
        UserPositionDO ownedA = row("ETHUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 0));
        ownedA.setUserId(userA);
        UserPositionDO partialA = row(
                "BTCUSDT", "PARTIALLY_CLOSED", LocalDateTime.of(2026, 6, 22, 8, 1));
        partialA.setUserId(userA);
        userPositionMapper.insert(ownedA);
        userPositionMapper.insert(partialA);
        LocalDateTime closedAt = LocalDateTime.of(2026, 6, 22, 9, 0);

        assertThat(close(ownedA.getId(), userB, closedAt)).isZero();
        assertThat(close(partialA.getId(), userB, closedAt)).isZero();
        assertThat(close(ownedA.getId(), userA, closedAt)).isEqualTo(1);
        assertThat(close(partialA.getId(), userA, closedAt)).isEqualTo(1);
        assertThat(close(ownedA.getId(), userA, closedAt.plusMinutes(1))).isZero();
        assertThat(userPositionMapper.selectByIdAndUserId(ownedA.getId(), userA).getStatus())
                .isEqualTo("CLOSED");
        assertThat(userPositionMapper.selectByIdAndUserId(partialA.getId(), userA).getStatus())
                .isEqualTo("CLOSED");
    }

    @Test
    void ownerForeignKeyRejectsUnknownCanonicalUser() {
        UserPositionDO row = row("XRPUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 0));
        row.setUserId(Long.MAX_VALUE);

        assertThatThrownBy(() -> userPositionMapper.insert(row))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("FK_TM_USER_POSITION_USER");
    }

    private int close(Long positionId, Long userId, LocalDateTime closedAt) {
        return userPositionMapper.manualCloseByIdAndUserId(
                positionId, userId, closedAt, new BigDecimal("105.25"), "manual close", closedAt);
    }

    private Long userId(String username) {
        PersonalUserDO user = new PersonalUserDO();
        user.setUsername(username);
        user.setPasswordHash("{noop}not-a-real-password");
        user.setCreatedAt(LocalDateTime.now());
        personalUserMapper.insert(user);
        return user.getId();
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
        row.setSourceType("MANUAL_INDEPENDENT");
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
