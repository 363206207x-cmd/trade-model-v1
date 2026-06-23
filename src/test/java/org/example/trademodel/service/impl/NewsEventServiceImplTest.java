package org.example.trademodel.service.impl;

import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.service.NewsEventService;
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
class NewsEventServiceImplTest {
    @Autowired
    private NewsEventService service;

    @Test
    void importNewsEventRequiresSourcePublishedAtAndDeduplicates() {
        ExternalContextImportRequest request = request("news-dedupe", "ACTIVE", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));

        ExternalContextImportResult<NewsEventDO> first = service.importEvent(request);
        ExternalContextImportResult<NewsEventDO> second = service.importEvent(request);

        assertThat(first.isDeduplicated()).isFalse();
        assertThat(second.isDeduplicated()).isTrue();
        assertThat(first.getEvent().getProvider()).isEqualTo("UNIT_TEST_NEWS");
        assertThat(first.getEvent().getSourceReference()).isEqualTo("unit://news-dedupe");
        assertThat(first.getEvent().getSourceTraceId()).isEqualTo("trace-news-dedupe");
        assertThat(first.getEvent().getSourcePublishedAt()).isNotNull();
    }

    @Test
    void importNewsEventRejectsMissingSourcePublishedAt() {
        ExternalContextImportRequest request = request("news-missing-published", "ACTIVE", LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(30));
        request.setSourcePublishedAt(null);

        assertThatThrownBy(() -> service.importEvent(request)).hasMessageContaining("sourcePublishedAt is required");
    }

    @Test
    void candidatesExcludeRetractedExpiredAndFuturePublishedEvents() {
        LocalDateTime now = LocalDateTime.now();
        service.importEvent(request("news-active", "ACTIVE", now.minusMinutes(5), now.plusMinutes(30)));
        service.importEvent(request("news-retracted", "RETRACTED", now.minusMinutes(5), now.plusMinutes(30)));
        service.importEvent(request("news-expired", "EXPIRED", now.minusHours(3), now.minusHours(2)));
        service.importEvent(request("news-expired-current-window", "EXPIRED", now.minusMinutes(5), now.plusMinutes(30)));
        ExternalContextImportRequest futurePublished = request("news-future", "ACTIVE", now.minusMinutes(5), now.plusMinutes(30));
        futurePublished.setSourcePublishedAt(now.plusMinutes(5));
        service.importEvent(futurePublished);

        List<NewsEventDO> candidates = service.findWindowCandidates("BTCUSDT", "CRYPTO", now);

        assertThat(candidates).extracting(NewsEventDO::getEventId)
                .contains("news-active")
                .doesNotContain("news-retracted", "news-expired", "news-expired-current-window", "news-future");
    }

    @Test
    void candidatesDoNotTreatMissingMarketScopeAsWildcard() {
        LocalDateTime now = LocalDateTime.now();
        ExternalContextImportRequest equitiesNoSymbol = request("news-equities-no-symbol", "ACTIVE", now.minusMinutes(5), now.plusMinutes(30));
        equitiesNoSymbol.setAffectedSymbols(null);
        equitiesNoSymbol.setMarketScope("EQUITIES");
        service.importEvent(equitiesNoSymbol);

        ExternalContextImportRequest globalNoSymbol = request("news-global-no-symbol", "ACTIVE", now.minusMinutes(5), now.plusMinutes(30));
        globalNoSymbol.setAffectedSymbols(null);
        globalNoSymbol.setMarketScope("GLOBAL");
        service.importEvent(globalNoSymbol);

        ExternalContextImportRequest exactSymbol = request("news-exact-symbol-equities", "ACTIVE", now.minusMinutes(5), now.plusMinutes(30));
        exactSymbol.setAffectedSymbols("BTCUSDT");
        exactSymbol.setMarketScope("EQUITIES");
        service.importEvent(exactSymbol);

        List<NewsEventDO> candidates = service.findWindowCandidates("BTCUSDT", null, now);
        assertThat(candidates).extracting(NewsEventDO::getEventId)
                .contains("news-global-no-symbol", "news-exact-symbol-equities")
                .doesNotContain("news-equities-no-symbol");

        List<NewsEventDO> partialSymbolCandidates = service.findWindowCandidates("BTC", "CRYPTO", now);
        assertThat(partialSymbolCandidates).extracting(NewsEventDO::getEventId)
                .doesNotContain("news-exact-symbol-equities");
    }

    private ExternalContextImportRequest request(String id, String status, LocalDateTime windowStart, LocalDateTime windowEnd) {
        ExternalContextImportRequest r = new ExternalContextImportRequest();
        r.setEventId(id);
        r.setHeadline("News " + id);
        r.setAffectedSymbols("BTCUSDT,ETHUSDT");
        r.setMarketScope("CRYPTO");
        r.setEventTime(windowStart.plusMinutes(1));
        r.setWindowStart(windowStart);
        r.setWindowEnd(windowEnd);
        r.setImpactScore(90);
        r.setSeverity("CRITICAL");
        r.setDirection("BEARISH");
        r.setProvider("UNIT_TEST_NEWS");
        r.setSourceType("WIRE");
        r.setSourceReference("unit://" + id);
        r.setSourceTraceId("trace-" + id);
        r.setSourceEventId("source-" + id);
        r.setSourcePublishedAt(windowStart.minusMinutes(2));
        r.setStatus(status);
        r.setExecutionBlocking(true);
        r.setDedupeKey("dedupe-" + id);
        return r;
    }
}
