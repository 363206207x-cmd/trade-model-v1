package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.AssetPoolItemDO;
import org.example.trademodel.mapper.AssetPoolItemMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class AssetPoolTrackingStoppedIntegrationTest {

    @Autowired
    private AssetPoolService assetPoolService;

    @Autowired
    private AssetPoolItemMapper assetPoolItemMapper;

    @Test
    void removingAndToppingUpDefaultPersistsCanonicalTrackingState() {
        long userId = 99001L;

        assetPoolService.removeForUser(userId, "ETHUSDT");

        AssetPoolItemDO stopped = assetPoolItemMapper.selectByOwnerAndSymbol("USER", userId, "ETHUSDT");
        assertThat(stopped).isNotNull();
        assertThat(stopped.getActive()).isFalse();
        assertThat(stopped.getWatchStatus()).isEqualTo("TRACKING_STOPPED");
        assertThat(assetPoolService.listForUser(userId))
                .noneMatch(asset -> "ETHUSDT".equals(asset.symbol()));

        assetPoolService.topUpDefaults(userId);

        AssetPoolItemDO restored = assetPoolItemMapper.selectByOwnerAndSymbol("USER", userId, "ETHUSDT");
        assertThat(restored.getActive()).isTrue();
        assertThat(restored.getWatchStatus()).isEqualTo("OBSERVING");
        assertThat(assetPoolService.listForUser(userId))
                .anyMatch(asset -> "ETHUSDT".equals(asset.symbol()));
    }
}
