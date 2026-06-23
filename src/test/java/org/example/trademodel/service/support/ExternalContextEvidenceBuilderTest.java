package org.example.trademodel.service.support;

import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.service.MacroEventService;
import org.example.trademodel.service.NewsEventService;
import org.example.trademodel.vo.EvidenceItemVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalContextEvidenceBuilderTest {
    @Test
    void buildsTraceableEvidenceAndBlockingSnapshotForMajorNews() {
        LocalDateTime now = LocalDateTime.now();
        MacroEventDO macro = macro("macro-builder", now, 72, false);
        NewsEventDO news = news("news-builder", now, 90, true);
        ExternalContextEvidenceBuilder builder = new ExternalContextEvidenceBuilder(
                macroService(List.of(macro)), newsService(List.of(news)));

        ExternalContextEvidenceBundle bundle = builder.build("ana-builder", "BTCUSDT", "1h", now, "CRYPTO");

        assertThat(bundle.getEvidenceItems()).hasSize(2);
        assertThat(bundle.getEvidenceItems()).extracting(EvidenceItemVO::getExternalEventId)
                .contains("macro-builder", "news-builder");
        assertThat(bundle.getEvidenceItems()).allSatisfy(ev -> {
            assertThat(ev.getSourceProvider()).isNotBlank();
            assertThat(ev.getSourceReference()).isNotBlank();
            assertThat(ev.getSourceTraceId()).isNotBlank();
            assertThat(ev.getEventWindowStart()).isNotNull();
            assertThat(ev.getEventWindowEnd()).isNotNull();
        });
        assertThat(bundle.getSnapshot().isExternalContextBlocked()).isTrue();
        assertThat(bundle.getSnapshot().getRiskLevel()).isEqualTo("HIGH");
        assertThat(bundle.getSnapshot().getReasonCodes()).contains(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
    }

    @Test
    void missingSourceFailsClosedWithoutNormalEvidence() {
        LocalDateTime now = LocalDateTime.now();
        MacroEventDO macro = macro("macro-missing-source", now, 80, false);
        macro.setSourceTraceId(null);
        ExternalContextEvidenceBuilder builder = new ExternalContextEvidenceBuilder(macroService(List.of(macro)), newsService(List.of()));

        ExternalContextEvidenceBundle bundle = builder.build("ana-missing", "BTCUSDT", "1h", now, "CRYPTO");

        assertThat(bundle.getEvidenceItems()).isEmpty();
        assertThat(bundle.getSnapshot().isExternalContextBlocked()).isTrue();
        assertThat(bundle.getSnapshot().getSourceHealth()).isEqualTo(ExternalContextPolicy.SOURCE_HEALTH_BLOCKED);
        assertThat(bundle.getSnapshot().getReasonCodes()).contains(ExternalContextPolicy.REASON_MISSING_SOURCE);
    }

    @Test
    void duplicateEventProducesSingleEvidenceItem() {
        LocalDateTime now = LocalDateTime.now();
        MacroEventDO macro = macro("macro-dup", now, 70, false);
        ExternalContextEvidenceBuilder builder = new ExternalContextEvidenceBuilder(macroService(List.of(macro, macro)), newsService(List.of()));

        ExternalContextEvidenceBundle bundle = builder.build("ana-dup", "BTCUSDT", "1h", now, "CRYPTO");

        assertThat(bundle.getEvidenceItems()).hasSize(1);
    }

    private MacroEventDO macro(String id, LocalDateTime now, int score, boolean blocking) {
        MacroEventDO event = new MacroEventDO();
        fill(event, id, now, score, blocking);
        event.setEventType("RATE_DECISION");
        event.setTitle("Macro " + id);
        event.setStatus("ACTIVE");
        return event;
    }

    private NewsEventDO news(String id, LocalDateTime now, int score, boolean blocking) {
        NewsEventDO event = new NewsEventDO();
        fill(event, id, now, score, blocking);
        event.setHeadline("News " + id);
        event.setStatus("ACTIVE");
        return event;
    }

    private void fill(org.example.trademodel.entity.ExternalContextEventDO event, String id, LocalDateTime now, int score, boolean blocking) {
        event.setEventId(id);
        event.setAffectedSymbols("BTCUSDT");
        event.setMarketScope("CRYPTO");
        event.setEventTime(now.minusMinutes(1));
        event.setWindowStart(now.minusMinutes(5));
        event.setWindowEnd(now.plusMinutes(30));
        event.setImpactScore(score);
        event.setSeverity(score >= 85 ? "CRITICAL" : "HIGH");
        event.setDirection("NEUTRAL");
        event.setProvider("UNIT_TEST_PROVIDER");
        event.setSourceType("WIRE");
        event.setSourceReference("unit://" + id);
        event.setSourceTraceId("trace-" + id);
        event.setSourceEventId("source-" + id);
        event.setSourcePublishedAt(now.minusMinutes(10));
        event.setExecutionBlocking(blocking);
    }

    private MacroEventService macroService(List<MacroEventDO> events) {
        return new MacroEventService() {
            public ExternalContextImportResult<MacroEventDO> importEvent(ExternalContextImportRequest request) { return null; }
            public MacroEventDO findByEventId(String eventId) { return null; }
            public List<MacroEventDO> listRecent(int limit) { return events; }
            public List<MacroEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime) { return events; }
        };
    }

    private NewsEventService newsService(List<NewsEventDO> events) {
        return new NewsEventService() {
            public ExternalContextImportResult<NewsEventDO> importEvent(ExternalContextImportRequest request) { return null; }
            public NewsEventDO findByEventId(String eventId) { return null; }
            public List<NewsEventDO> listRecent(int limit) { return events; }
            public List<NewsEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime) { return events; }
        };
    }
}
