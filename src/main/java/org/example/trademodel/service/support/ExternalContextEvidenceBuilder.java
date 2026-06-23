package org.example.trademodel.service.support;

import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.entity.ExternalContextEventDO;
import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.service.MacroEventService;
import org.example.trademodel.service.NewsEventService;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ExternalContextEvidenceBuilder {
    private final MacroEventService macroEventService;
    private final NewsEventService newsEventService;
    public ExternalContextEvidenceBuilder(MacroEventService macroEventService, NewsEventService newsEventService) {
        this.macroEventService = macroEventService;
        this.newsEventService = newsEventService;
    }

    public ExternalContextEvidenceBundle build(String analysisId, String symbol, String timeframe, LocalDateTime contextTime, String marketScope) {
        LocalDateTime at = contextTime == null ? LocalDateTime.now() : contextTime;
        ExternalContextSnapshot snapshot = new ExternalContextSnapshot();
        List<EvidenceItemVO> evidence = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        List<MacroEventDO> macroEvents = macroEventService == null ? List.of() : macroEventService.findWindowCandidates(symbol, marketScope, at);
        List<NewsEventDO> newsEvents = newsEventService == null ? List.of() : newsEventService.findWindowCandidates(symbol, marketScope, at);
        for (MacroEventDO event : macroEvents) { applyEvent(snapshot, evidence, emitted, analysisId, symbol, marketScope, event, "MACRO", label(event), at); }
        for (NewsEventDO event : newsEvents) { applyEvent(snapshot, evidence, emitted, analysisId, symbol, marketScope, event, "NEWS", label(event), at); }
        return new ExternalContextEvidenceBundle(snapshot, evidence);
    }

    public ExternalContextSnapshot buildSnapshot(String analysisId, String symbol, String timeframe, LocalDateTime contextTime, String marketScope) {
        return build(analysisId, symbol, timeframe, contextTime, marketScope).getSnapshot();
    }

    public static EventImpactInputVO toEventImpactInput(EventImpactInputVO input, ExternalContextSnapshot snapshot) {
        EventImpactInputVO target = input == null ? new EventImpactInputVO() : input;
        if (snapshot == null) { return target; }
        target.setExternalContextStatus(snapshot.getStatus());
        target.setActiveExternalEventCount(snapshot.getActiveExternalEventCount());
        target.setActiveMacroEventCount(snapshot.getActiveMacroEventCount());
        target.setActiveNewsEventCount(snapshot.getActiveNewsEventCount());
        target.setExternalContextRiskLevel(snapshot.getRiskLevel());
        target.setExternalContextBlocked(snapshot.isExternalContextBlocked());
        target.setExternalEventIds(snapshot.getExternalEventIds());
        target.setExternalContextReasonCodes(snapshot.getReasonCodes());
        target.setNextExternalEventTime(snapshot.getNextExternalEventTime());
        target.setLatestExternalEventTime(snapshot.getLatestExternalEventTime());
        target.setLatestExternalEventLabel(snapshot.getLatestExternalEventLabel());
        target.setExternalEventWindowStart(snapshot.getEventWindowStart());
        target.setExternalEventWindowEnd(snapshot.getEventWindowEnd());
        target.setExternalContextSourceHealth(snapshot.getSourceHealth());
        return target;
    }

    private static void applyEvent(ExternalContextSnapshot snapshot, List<EvidenceItemVO> evidence, Set<String> emitted,
                                   String analysisId, String symbol, String marketScope, ExternalContextEventDO event,
                                   String eventType, String label, LocalDateTime at) {
        if (!ExternalContextPolicy.matchesContextScope(event == null ? null : event.getAffectedSymbols(),
                event == null ? null : event.getMarketScope(), symbol, marketScope)) {
            return;
        }
        String state = ExternalContextPolicy.windowState(event, at);
        if (ExternalContextPolicy.STATUS_CANCELLED.equals(state) || ExternalContextPolicy.STATUS_RETRACTED.equals(state)
                || ExternalContextPolicy.STATUS_EXPIRED.equals(state)) { return; }
        snapshot.addEventId(eventType + ":" + event.getEventId());
        snapshot.setLatestExternalEventLabel(label);
        snapshot.setLatestExternalEventTime(event.getEventTime());
        snapshot.setEventWindowStart(event.getWindowStart());
        snapshot.setEventWindowEnd(event.getWindowEnd());
        if (ExternalContextPolicy.STATUS_ACTIVE.equals(state)) {
            snapshot.setActiveExternalEventCount(snapshot.getActiveExternalEventCount() + 1);
            if ("MACRO".equals(eventType)) { snapshot.setActiveMacroEventCount(snapshot.getActiveMacroEventCount() + 1); }
            else { snapshot.setActiveNewsEventCount(snapshot.getActiveNewsEventCount() + 1); }
        } else if (event.getWindowStart() == null) {
            return;
        } else if (snapshot.getNextExternalEventTime() == null || event.getWindowStart().isBefore(snapshot.getNextExternalEventTime())) {
            snapshot.setNextExternalEventTime(event.getWindowStart());
        }
        if (ExternalContextPolicy.hasCompleteSource(event) == false) {
            snapshot.setSourceHealth(ExternalContextPolicy.SOURCE_HEALTH_BLOCKED);
            snapshot.setStatus("BLOCKED");
            snapshot.setExternalContextBlocked(true);
            snapshot.setRiskLevel(ExternalContextPolicy.RISK_HIGH);
            snapshot.addReason(ExternalContextPolicy.REASON_MISSING_SOURCE);
            return;
        }
        if (ExternalContextPolicy.isHighImpact(event)) {
            snapshot.setRiskLevel(ExternalContextPolicy.RISK_HIGH);
            snapshot.addReason(ExternalContextPolicy.REASON_HIGH_IMPACT_REVIEW);
        }
        if (ExternalContextPolicy.isBlocking(event, state)) {
            snapshot.setStatus("BLOCKED");
            snapshot.setExternalContextBlocked(true);
            snapshot.setRiskLevel(ExternalContextPolicy.RISK_HIGH);
            snapshot.addReason(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        }
        String key = eventType + ":" + event.getEventId();
        if (emitted.add(key)) { evidence.add(toEvidence(analysisId, event, eventType, label, state)); }
    }

    private static EvidenceItemVO toEvidence(String analysisId, ExternalContextEventDO event, String eventType, String label, String state) {
        EvidenceItemVO ev = new EvidenceItemVO();
        ev.setEvidenceId("ev-ext-" + Integer.toHexString((String.valueOf(analysisId) + eventType + event.getEventId()).hashCode()));
        ev.setEvidenceType("MACRO".equals(eventType) ? EvidenceTypeConstants.MACRO : EvidenceTypeConstants.NEWS);
        ev.setDirection(EvidenceTypeConstants.normalizeEvidenceDirection(event.getDirection()));
        ev.setSource(EvidenceTypeConstants.EVIDENCE_SOURCE_EXTERNAL_CONTEXT);
        ev.setSourceProvider(event.getProvider());
        ev.setSourceReference(event.getSourceReference());
        ev.setSourceTraceId(event.getSourceTraceId());
        ev.setExternalEventId(event.getEventId());
        ev.setExternalEventType(eventType);
        ev.setEventWindowStart(event.getWindowStart());
        ev.setEventWindowEnd(event.getWindowEnd());
        ev.setImpactScore(event.getImpactScore());
        ev.setSeverity(event.getSeverity());
        ev.setStrength(event.getImpactScore() == null ? null : event.getImpactScore().doubleValue());
        ev.setConfidence(80.0);
        ev.setDescription(String.format(Locale.ROOT, "External context %s event [%s] provider=%s impactScore=%s severity=%s windowState=%s sourceTraceId=%s", eventType, label, event.getProvider(), event.getImpactScore(), event.getSeverity(), state, event.getSourceTraceId()));
        return ev;
    }

    private static String label(MacroEventDO event) { return event == null ? "macro" : event.getTitle(); }
    private static String label(NewsEventDO event) { return event == null ? "news" : event.getHeadline(); }
}
