package org.example.trademodel.service.impl;

import org.example.trademodel.entity.PushRecheckDispatchConfigAuditDO;
import org.example.trademodel.entity.PushRecheckDispatchConfigDO;
import org.example.trademodel.mapper.PushRecheckDispatchConfigAuditMapper;
import org.example.trademodel.mapper.PushRecheckDispatchConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushRecheckDispatchConfigServiceImplTest {

    @Mock
    private PushRecheckDispatchConfigMapper configMapper;
    @Mock
    private PushRecheckDispatchConfigAuditMapper auditMapper;

    @InjectMocks
    private PushRecheckDispatchConfigServiceImpl service;

    @Test
    void loadOrInit_shouldInsertMissingDefaults() {
        when(configMapper.selectAll()).thenReturn(List.of(), List.of(
                row("limit", 50),
                row("maxAttempts", 3),
                row("minRetryMinutes", 5)
        ));

        Map<String, Integer> config = service.loadOrInit(50, 3, 5);

        verify(configMapper, times(3)).insert(any(PushRecheckDispatchConfigDO.class));
        assertThat(config.get("limit")).isEqualTo(50);
        assertThat(config.get("maxAttempts")).isEqualTo(3);
        assertThat(config.get("minRetryMinutes")).isEqualTo(5);
    }

    @Test
    void updateConfig_shouldPersistAndAuditChangedFieldsOnly() {
        when(configMapper.selectAll()).thenReturn(
                List.of(row("limit", 50), row("maxAttempts", 3), row("minRetryMinutes", 5)),
                List.of(row("limit", 80), row("maxAttempts", 3), row("minRetryMinutes", 10))
        );

        Map<String, Integer> updated = service.updateConfig(80, null, 10, "tester", "API");

        verify(configMapper, times(2)).updateValue(any(), anyInt(), any(), any(), any());
        verify(auditMapper, times(2)).insert(any(PushRecheckDispatchConfigAuditDO.class));
        ArgumentCaptor<PushRecheckDispatchConfigAuditDO> cap = ArgumentCaptor.forClass(PushRecheckDispatchConfigAuditDO.class);
        verify(auditMapper, times(2)).insert(cap.capture());
        assertThat(cap.getAllValues()).extracting(PushRecheckDispatchConfigAuditDO::getConfigKey)
                .containsExactlyInAnyOrder("limit", "minRetryMinutes");
        assertThat(updated.get("limit")).isEqualTo(80);
        assertThat(updated.get("maxAttempts")).isEqualTo(3);
        assertThat(updated.get("minRetryMinutes")).isEqualTo(10);
    }

    private static PushRecheckDispatchConfigDO row(String key, Integer value) {
        PushRecheckDispatchConfigDO row = new PushRecheckDispatchConfigDO();
        row.setConfigKey(key);
        row.setConfigValue(value);
        return row;
    }
}
