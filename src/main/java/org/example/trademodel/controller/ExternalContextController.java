package org.example.trademodel.controller;

import org.example.trademodel.entity.ExternalContextEventDO;
import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.service.MacroEventService;
import org.example.trademodel.service.NewsEventService;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextImportRequest;
import org.example.trademodel.service.support.ExternalContextImportResult;
import org.example.trademodel.service.support.ExternalContextSnapshot;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/external-context")
public class ExternalContextController {
    private final MacroEventService macroEventService;
    private final NewsEventService newsEventService;
    private final ExternalContextEvidenceBuilder evidenceBuilder;

    public ExternalContextController(MacroEventService macroEventService,
                                     NewsEventService newsEventService,
                                     ExternalContextEvidenceBuilder evidenceBuilder) {
        this.macroEventService = macroEventService;
        this.newsEventService = newsEventService;
        this.evidenceBuilder = evidenceBuilder;
    }

    @PostMapping("/macro-events/import")
    public Map<String, Object> importMacro(@RequestBody ExternalContextImportRequest request) {
        return importResult(macroEventService.importEvent(request), "MACRO");
    }

    @PostMapping("/news-events/import")
    public Map<String, Object> importNews(@RequestBody ExternalContextImportRequest request) {
        return importResult(newsEventService.importEvent(request), "NEWS");
    }

    @GetMapping("/macro-events")
    public List<Map<String, Object>> macroEvents(@RequestParam(defaultValue = "100") int limit) {
        return macroEventService.listRecent(limit).stream().map(event -> eventMap(event, "MACRO")).toList();
    }

    @GetMapping("/news-events")
    public List<Map<String, Object>> newsEvents(@RequestParam(defaultValue = "100") int limit) {
        return newsEventService.listRecent(limit).stream().map(event -> eventMap(event, "NEWS")).toList();
    }

    @GetMapping("/current")
    public Map<String, Object> current(@RequestParam(required = false) String symbol,
                                       @RequestParam(required = false) String timeframe,
                                       @RequestParam(required = false) String marketScope,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime contextTime) {
        return snapshot(symbol, timeframe, marketScope, contextTime).toDashboardStatus();
    }

    @GetMapping("/events/{eventType}/{eventId}")
    public Map<String, Object> event(@PathVariable String eventType, @PathVariable String eventId) {
        if ("MACRO".equalsIgnoreCase(eventType)) {
            return eventMap(macroEventService.findByEventId(eventId), "MACRO");
        }
        if ("NEWS".equalsIgnoreCase(eventType)) {
            return eventMap(newsEventService.findByEventId(eventId), "NEWS");
        }
        throw new IllegalArgumentException("eventType must be MACRO or NEWS");
    }

    @GetMapping("/dashboard-status")
    public Map<String, Object> dashboardStatus(@RequestParam(required = false) String symbol,
                                               @RequestParam(required = false) String timeframe,
                                               @RequestParam(required = false) String marketScope) {
        Map<String, Object> status = snapshot(symbol, timeframe, marketScope, LocalDateTime.now()).toDashboardStatus();
        status.put("executionBoundary", "review-only; not executable; not auto trading; not order execution");
        status.put("latestEvent", status.get("latestExternalEventLabel"));
        status.put("eventWindow", String.valueOf(status.get("eventWindowStart")) + " ~ " + status.get("eventWindowEnd"));
        return status;
    }

    private ExternalContextSnapshot snapshot(String symbol, String timeframe, String marketScope, LocalDateTime contextTime) {
        return evidenceBuilder.buildSnapshot("external-context-current", symbol, timeframe,
                contextTime == null ? LocalDateTime.now() : contextTime, marketScope);
    }

    private static <T extends ExternalContextEventDO> Map<String, Object> importResult(ExternalContextImportResult<T> result,
                                                                                       String eventType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("deduplicated", result.isDeduplicated());
        map.put("reasonCode", result.getReasonCode());
        map.put("event", eventMap(result.getEvent(), eventType));
        return map;
    }

    private static Map<String, Object> eventMap(ExternalContextEventDO event, String eventType) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (event == null) {
            map.put("found", false);
            return map;
        }
        map.put("found", true);
        map.put("eventType", eventType);
        map.put("eventId", event.getEventId());
        map.put("label", event instanceof MacroEventDO macro ? macro.getTitle() : ((NewsEventDO) event).getHeadline());
        map.put("affectedSymbols", event.getAffectedSymbols());
        map.put("marketScope", event.getMarketScope());
        map.put("eventTime", event.getEventTime());
        map.put("windowStart", event.getWindowStart());
        map.put("windowEnd", event.getWindowEnd());
        map.put("impactScore", event.getImpactScore());
        map.put("severity", event.getSeverity());
        map.put("direction", event.getDirection());
        map.put("provider", event.getProvider());
        map.put("sourceType", event.getSourceType());
        map.put("sourceReference", event.getSourceReference());
        map.put("sourceTraceId", event.getSourceTraceId());
        map.put("sourceEventId", event.getSourceEventId());
        map.put("sourceHash", event.getSourceHash());
        map.put("sourcePublishedAt", event.getSourcePublishedAt());
        map.put("status", event.getStatus());
        map.put("executionBlocking", event.getExecutionBlocking());
        map.put("reviewOnly", true);
        map.put("manualReviewOnly", true);
        map.put("notTradeInstruction", true);
        map.put("notExecutable", true);
        map.put("notAutoTrading", true);
        map.put("notOrderExecution", true);
        map.put("notUserPositionCreation", true);
        map.put("notUserPositionMutation", true);
        map.put("notPushSend", true);
        map.put("notExternalChannel", true);
        map.put("notExternalFetch", true);
        return map;
    }
}
