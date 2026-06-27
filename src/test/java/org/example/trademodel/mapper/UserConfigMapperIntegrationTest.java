package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.UserConfigDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class UserConfigMapperIntegrationTest {

    @Autowired
    private UserConfigMapper userConfigMapper;

    @Test
    void defaultH2SaveOrUpdateInsertsAndUpdatesSameUserId() {
        userConfigMapper.saveOrUpdate(row("pdr-2c2a-user", "LOW", "GPT", "WEB", 15));
        userConfigMapper.saveOrUpdate(row("pdr-2c2a-user", "HIGH", "GEMINI", "WEB,TELEGRAM", 30));

        UserConfigDO persisted = userConfigMapper.findByUserId("pdr-2c2a-user");
        assertThat(persisted.getUserId()).isEqualTo("pdr-2c2a-user");
        assertThat(persisted.getRiskPreference()).isEqualTo("HIGH");
        assertThat(persisted.getAiModelPreference()).isEqualTo("GEMINI");
        assertThat(persisted.getNotifyChannels()).isEqualTo("WEB,TELEGRAM");
        assertThat(persisted.getCooldownMinutes()).isEqualTo(30);
    }

    private static UserConfigDO row(String userId, String riskPreference, String model,
                                    String channels, Integer cooldownMinutes) {
        UserConfigDO row = new UserConfigDO();
        row.setUserId(userId);
        row.setRiskPreference(riskPreference);
        row.setAiModelPreference(model);
        row.setNotifyChannels(channels);
        row.setCooldownMinutes(cooldownMinutes);
        return row;
    }
}
