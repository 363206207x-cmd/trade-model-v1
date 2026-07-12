package org.example.trademodel.localreal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalRealDataStatusServiceTest {
    @Mock PersistedOhlcvBarMapper ohlcvMapper;
    @Mock AnalysisRunMapper analysisRunMapper;
    @Mock DecisionResultMapper decisionResultMapper;

    @Test
    void localRealStatusDoesNotExposeSecretsAndShowsAiDisabled() throws Exception {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        readiness.transition(LocalRealReadinessState.DEGRADED, "MARKET_WINDOW_INCOMPLETE");
        when(ohlcvMapper.countAllClosedBars()).thenReturn(0L);
        when(analysisRunMapper.countLocalRealSuccessfulSymbols()).thenReturn(0);
        LocalRealDataStatusService service = new LocalRealDataStatusService(
                readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper);

        Map<String, Object> status = service.status();
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(status).toLowerCase();

        assertThat(status.get("ai")).isEqualTo(Map.of("enabled", false, "status", "DISABLED"));
        assertThat(json).doesNotContain("api_key", "apikey", "password", "authorization", "prompt");
        assertThat(status).containsEntry("notAutoTrading", true).containsEntry("notOrderExecution", true);
    }
}
