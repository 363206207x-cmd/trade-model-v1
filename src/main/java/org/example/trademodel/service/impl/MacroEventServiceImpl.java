package org.example.trademodel.service.impl;

import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.mapper.MacroEventMapper;
import org.example.trademodel.service.MacroEventService;
import org.example.trademodel.service.support.ExternalContextImportRequest;
import org.example.trademodel.service.support.ExternalContextImportResult;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MacroEventServiceImpl implements MacroEventService {
    private final MacroEventMapper mapper;

    public MacroEventServiceImpl(MacroEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ExternalContextImportResult<MacroEventDO> importEvent(ExternalContextImportRequest request) {
        MacroEventDO event = toEvent(request);
        MacroEventDO existing = mapper.selectByDedupeKey(event.getDedupeKey());
        if (existing != null) {
            return ExternalContextImportResult.deduplicated(existing);
        }
        try {
            mapper.insert(event);
        } catch (DuplicateKeyException ignored) {
            MacroEventDO deduped = mapper.selectByDedupeKey(event.getDedupeKey());
            return ExternalContextImportResult.deduplicated(deduped != null ? deduped : event);
        }
        return ExternalContextImportResult.imported(event);
    }

    @Override
    public MacroEventDO findByEventId(String eventId) {
        return mapper.selectByEventId(trim(eventId));
    }

    @Override
    public List<MacroEventDO> listRecent(int limit) {
        return mapper.selectRecent(Math.max(1, Math.min(limit, 200)));
    }

    @Override
    public List<MacroEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime) {
        LocalDateTime at = contextTime == null ? LocalDateTime.now() : contextTime;
        return mapper.selectWindowCandidates(at, 500).stream()
                .filter(event -> ExternalContextPolicy.matchesContextScope(
                        event.getAffectedSymbols(), event.getMarketScope(), symbol, marketScope))
                .filter(event -> {
                    String state = ExternalContextPolicy.windowState(event, at);
                    return ExternalContextPolicy.STATUS_ACTIVE.equals(state) || ExternalContextPolicy.STATUS_NEAR.equals(state);
                })
                .collect(Collectors.toList());
    }

    private MacroEventDO toEvent(ExternalContextImportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("macro event request is required");
        }
        MacroEventDO event = new MacroEventDO();
        event.setEventId(defaultText(request.getEventId(), "macro-" + UUID.randomUUID().toString().substring(0, 12)));
        event.setEventType(required(request.getEventType(), "eventType"));
        event.setTitle(required(request.getTitle(), "title"));
        event.setDescription(trim(request.getDescription()));
        copyCommon(request, event);
        if (request.getSourcePublishedAt() == null) {
            event.setSourcePublishedAt(event.getEventTime());
            event.setSourcePublishedAtReasonCode("MACRO_SOURCE_PUBLISHED_AT_FALLBACK_EVENT_TIME");
        } else {
            event.setSourcePublishedAt(request.getSourcePublishedAt());
            event.setSourcePublishedAtReasonCode("SOURCE_PUBLISHED_AT_PROVIDED");
        }
        event.setDedupeKey(defaultText(request.getDedupeKey(), buildDedupeKey("MACRO", event)));
        return event;
    }

    private void copyCommon(ExternalContextImportRequest request, MacroEventDO event) {
        event.setAffectedSymbols(trim(request.getAffectedSymbols()));
        event.setMarketScope(defaultText(request.getMarketScope(), "GLOBAL"));
        event.setEventTime(requiredTime(request.getEventTime(), "eventTime"));
        event.setWindowStart(requiredTime(request.getWindowStart(), "windowStart"));
        event.setWindowEnd(requiredTime(request.getWindowEnd(), "windowEnd"));
        if (event.getWindowStart().isAfter(event.getWindowEnd())) {
            throw new IllegalArgumentException("windowStart must be <= windowEnd");
        }
        int score = request.getImpactScore() == null ? 0 : request.getImpactScore();
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("impactScore must be between 0 and 100");
        }
        event.setImpactScore(score);
        event.setSeverity(normalizeAllowed(request.getSeverity(), "severity", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")));
        event.setDirection(normalizeAllowed(request.getDirection(), "direction", List.of("BULLISH", "BEARISH", "NEUTRAL", "UNKNOWN")));
        event.setProvider(required(request.getProvider(), "provider"));
        event.setSourceType(required(request.getSourceType(), "sourceType"));
        event.setSourceReference(required(request.getSourceReference(), "sourceReference"));
        event.setSourceTraceId(required(request.getSourceTraceId(), "sourceTraceId"));
        event.setSourceEventId(trim(request.getSourceEventId()));
        event.setSourceHash(trim(request.getSourceHash()));
        if (!ExternalContextPolicy.hasText(event.getSourceEventId()) && !ExternalContextPolicy.hasText(event.getSourceHash())) {
            throw new IllegalArgumentException("sourceEventId or sourceHash is required");
        }
        event.setStatus(normalizeAllowed(defaultText(request.getStatus(), "SCHEDULED"), "status", List.of("SCHEDULED", "ACTIVE", "EXPIRED", "CANCELLED")));
        event.setExecutionBlocking(Boolean.TRUE.equals(request.getExecutionBlocking()));
    }

    static String buildDedupeKey(String type, MacroEventDO event) {
        String sourceKey = ExternalContextPolicy.hasText(event.getSourceEventId()) ? event.getSourceEventId() : event.getSourceHash();
        return type + ":" + event.getProvider() + ":" + event.getSourceTraceId() + ":" + sourceKey;
    }

    private static String normalizeAllowed(String raw, String name, List<String> allowed) {
        String normalized = required(raw, name).toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException(name + " is invalid");
        }
        return normalized;
    }

    private static LocalDateTime requiredTime(LocalDateTime value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String required(String value, String name) {
        String text = trim(value);
        if (text == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return text;
    }

    private static String defaultText(String value, String fallback) {
        String text = trim(value);
        return text == null ? fallback : text;
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
