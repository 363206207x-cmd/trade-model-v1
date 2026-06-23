package org.example.trademodel.service.impl;

import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.service.MacroEventService;
import org.example.trademodel.service.support.ExternalContextImportRequest;
import org.example.trademodel.service.support.ExternalContextImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MacroEventServiceImplTest {
    @Autowired
    private MacroEventService service;

    @Test
    void importMacroEvent_requiresSourceTraceAndDeduplicates() {
        ExternalContextImportRequest request = request("macro-dedupe", "ACTIVE", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        request.setSourcePublishedAt(null);

        ExternalContextImportResult<MacroEventDO> first = service.importEvent(request);
        ExternalContextImportResult<MacroEventDO> second = service.importEvent(request);

        assertThat(first.isDeduplicated()).isFalse();
        assertThat(second.isDeduplicated()).isTrue();
        assertThat(first.getEvent().getProvider()).isEqualTo("UNIT_TEST_PROVIDER");
        assertThat(first.getEvent().getSourceReference()).isEqualTo("unit://macro-dedupe");
        assertThat(first.getEvent().getSourceTraceId()).isEqualTo("trace-macro-dedupe");
        assertThat(first.getEvent().getSourcePublishedAt()).isEqualTo(first.getEvent().getEventTime());
        assertThat(first.getEvent().getSourcePublishedAtReasonCode()).isEqualTo("MACRO_SOURCE_PUBLISHED_AT_FALLBACK_EVENT_TIME");
    }

    @Test
    void importMacroEventRejectsMissingProviderSourceReferenceOrTrace() {
        ExternalContextImportRequest missingProvider = request("macro-missing-provider", "ACTIVE", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        missingProvider.setProvider(null);
        assertThatThrownBy(() -> service.importEvent(missingProvider)).hasMessageContaining("provider is required");

        ExternalContextImportRequest missingReference = request("macro-missing-reference", "ACTIVE", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        missingReference.setSourceReference(null);
        assertThatThrownBy(() -> service.importEvent(missingReference)).hasMessageContaining("sourceReference is required");

        ExternalContextImportRequest missingTrace = request("macro-missing-trace", "ACTIVE", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        missingTrace.setSourceTraceId(null);
        assertThatThrownBy(() -> service.importEvent(missingTrace)).hasMessageContaining("sourceTraceId is required");
    }

    @Test
    void windowCandidatesIncludeActiveAndNearButExcludeExpiredAndCancelled() {
        LocalDateTime now = LocalDateTime.now();
        service.importEvent(request("macro-active", "ACTIVE", now.minusMinutes(10), now.plusMinutes(10)));
        ExternalContextImportRequest near = request("macro-near", "SCHEDULED", now.plusMinutes(20), now.plusMinutes(40));
        near.setSourcePublishedAt(now.minusMinutes(1));
        service.importEvent(near);
        service.importEvent(request("macro-expired", "EXPIRED", now.minusHours(2), now.minusHours(1)));
        service.importEvent(request("macro-cancelled", "CANCELLED", now.minusMinutes(10), now.plusMinutes(10)));

        List<MacroEventDO> candidates = service.findWindowCandidates("BTCUSDT", "CRYPTO", now);

        assertThat(candidates).extracting(MacroEventDO::getEventId)
                .contains("macro-active", "macro-near")
                .doesNotContain("macro-expired", "macro-cancelled");
    }

    private ExternalContextImportRequest request(String id, String status, LocalDateTime windowStart, LocalDateTime windowEnd) {
        ExternalContextImportRequest r = new ExternalContextImportRequest();
        r.setEventId(id);
        r.setEventType("RATE_DECISION");
        r.setTitle("Macro " + id);
        r.setAffectedSymbols("BTCUSDT,ETHUSDT");
        r.setMarketScope("CRYPTO");
        r.setEventTime(windowStart.plusMinutes(1));
        r.setWindowStart(windowStart);
        r.setWindowEnd(windowEnd);
        r.setImpactScore(80);
        r.setSeverity("HIGH");
        r.setDirection("NEUTRAL");
        r.setProvider("UNIT_TEST_PROVIDER");
        r.setSourceType("CALENDAR");
        r.setSourceReference("unit://" + id);
        r.setSourceTraceId("trace-" + id);
        r.setSourceEventId("source-" + id);
        r.setSourcePublishedAt(windowStart.minusMinutes(5));
        r.setStatus(status);
        r.setDedupeKey("dedupe-" + id);
        return r;
    }
}
