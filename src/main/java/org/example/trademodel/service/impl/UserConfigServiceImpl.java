package org.example.trademodel.service.impl;

import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.UserConfigMapper;
import org.example.trademodel.service.UserConfigService;
import org.springframework.stereotype.Service;

@Service
public class UserConfigServiceImpl implements UserConfigService {

    private final UserConfigMapper userConfigMapper;

    public UserConfigServiceImpl(UserConfigMapper userConfigMapper) {
        this.userConfigMapper = userConfigMapper;
    }

    @Override
    public UserConfigDO getUserConfig(String userId) {
        return userConfigMapper.findByUserId(userId);
    }

    @Override
    public void saveUserConfig(UserConfigDO userConfig) {
        userConfigMapper.saveOrUpdate(userConfig);
    }
}
