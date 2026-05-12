package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.vo.PushWatchlistConfigAuditVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class PushWatchlistConfigAuditMapperTest {

    @Autowired
    private PushWatchlistConfigAuditMapper pushWatchlistConfigAuditMapper;

    @Test
    void insertAndSelectRecent_roundTripsWatchlistAuditFields() {
        LocalDateTime createTime = LocalDateTime.of(2026, 5, 12, 12, 0, 0);
        PushWatchlistConfigAuditVO row = new PushWatchlistConfigAuditVO();
        row.setRuleKey("push.watchlist.symbols");
        row.setBeforeSymbols("BTCUSDT");
        row.setAfterSymbols("BTCUSDT,ETHUSDT");
        row.setBeforeEnabled(Boolean.FALSE);
        row.setAfterEnabled(Boolean.TRUE);
        row.setChangedBy("tester");
        row.setChangeReason("add ETH");
        row.setSource("API");
        row.setTraceId("trace-test-001");
        row.setRuleVersion("p1-test");
        row.setCreateTime(createTime);

        int inserted = pushWatchlistConfigAuditMapper.insert(row);

        assertThat(inserted).isEqualTo(1);
        List<PushWatchlistConfigAuditVO> rows = pushWatchlistConfigAuditMapper.selectRecent(10);
        assertThat(rows).isNotEmpty();
        PushWatchlistConfigAuditVO loaded = rows.stream()
                .filter(item -> "trace-test-001".equals(item.getTraceId()))
                .findFirst()
                .orElseThrow();

        assertThat(loaded.getRuleKey()).isEqualTo("push.watchlist.symbols");
        assertThat(loaded.getBeforeSymbols()).isEqualTo("BTCUSDT");
        assertThat(loaded.getAfterSymbols()).isEqualTo("BTCUSDT,ETHUSDT");
        assertThat(loaded.getBeforeEnabled()).isFalse();
        assertThat(loaded.getAfterEnabled()).isTrue();
        assertThat(loaded.getChangedBy()).isEqualTo("tester");
        assertThat(loaded.getChangeReason()).isEqualTo("add ETH");
        assertThat(loaded.getSource()).isEqualTo("API");
        assertThat(loaded.getTraceId()).isEqualTo("trace-test-001");
        assertThat(loaded.getRuleVersion()).isEqualTo("p1-test");
        assertThat(loaded.getCreateTime()).isEqualTo(createTime);
    }
}
