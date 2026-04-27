package org.example.trademodel.service;

import org.example.trademodel.entity.UserConfigDO;

public interface UserConfigService {
    UserConfigDO getUserConfig(String userId);
    void saveUserConfig(UserConfigDO userConfig);
}
