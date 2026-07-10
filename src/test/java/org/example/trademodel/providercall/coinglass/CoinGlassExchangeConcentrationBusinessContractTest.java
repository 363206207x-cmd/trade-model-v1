package org.example.trademodel.providercall.coinglass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.derivatives.DerivativesBusinessInput;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesEvidenceItem;
import org.example.trademodel.derivatives.DerivativesEvidenceType;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoinGlassExchangeConcentrationBusinessContractTest {
    private static final Instant FETCH_TIME = Instant.parse("2026-07-10T10:00:00Z");

    @Test
    void providerRatioFlowsThroughProductionBusinessRiskContract() throws Exception {
        JsonNode response = fixture("open-interest-high-concentration.json");
        CoinGlassMappingResult<CoinGlassOpenInterestSnapshot> mapped = new CoinGlassV4ResponseValidator()
                .openInterest(response.get("data"), new CoinGlassSymbolMapper().map("BTCUSDT"), FETCH_TIME);

        assertThat(mapped.status()).isEqualTo(UnifiedSourceStatus.READY);
        assertThat(mapped.payload().exchangeConcentrationScore())
                .isGreaterThan(BigDecimal.ZERO)
                .isLessThanOrEqualTo(BigDecimal.ONE)
                .isEqualByComparingTo("0.90");

        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata(
                "COINGLASS",
                ProviderDatasetType.COINGLASS_OPEN_INTEREST,
                "BTCUSDT",
                "CURRENT",
                mapped.providerDataTime(),
                FETCH_TIME,
                FETCH_TIME.plusSeconds(60),
                UnifiedSourceStatus.READY,
                SnapshotFreshnessStatus.FRESH,
                "trace-concentration-contract",
                "fixture:open-interest-high-concentration",
                false,
                false,
                null,
                List.of());
        ProviderCallResult<CoinGlassOpenInterestSnapshot> openInterest =
                new ProviderCallResult<>(mapped.payload(), metadata, null);
        DerivativesRiskSnapshot snapshot = new CoinGlassDerivativesSnapshotAssembler(new CoinGlassProperties())
                .assemble("BTCUSDT", "trace-concentration-contract", openInterest, null, null, null)
                .payload();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.exchangeConcentrationScore()).isEqualByComparingTo("0.90");

        DerivativesBusinessAssessment assessment = new DerivativesBusinessIntegrationService(null).evaluate(
                new DerivativesBusinessInput(
                        "BTCUSDT",
                        "BULLISH",
                        new BigDecimal("110"),
                        new BigDecimal("100"),
                        true,
                        Map.of("5m", "BULLISH", "15m", "BULLISH", "1h", "BULLISH", "4h", "BULLISH"),
                        true,
                        90,
                        true,
                        false,
                        false,
                        null,
                        snapshot,
                        "trace-concentration-contract",
                        "analysis-concentration-contract",
                        "v1.0"));

        assertThat(assessment.evidence())
                .extracting(DerivativesEvidenceItem::evidenceType)
                .contains(DerivativesEvidenceType.EXCHANGE_CONCENTRATION_HIGH);
        assertThat(assessment.scoreDeltas().get(DerivativesBusinessIntegrationService.LIQUIDITY_SCORE)).isNegative();
        assertThat(assessment.scoreDeltas().get(DerivativesBusinessIntegrationService.CREDIBILITY_SCORE)).isNegative();
        assertThat(assessment.hotResetCandidate()).isTrue();
    }

    private static JsonNode fixture(String name) throws Exception {
        try (InputStream stream = CoinGlassExchangeConcentrationBusinessContractTest.class
                .getResourceAsStream("/provider/coinglass/v4/" + name)) {
            assertThat(stream).isNotNull();
            return new ObjectMapper().readTree(stream);
        }
    }
}
