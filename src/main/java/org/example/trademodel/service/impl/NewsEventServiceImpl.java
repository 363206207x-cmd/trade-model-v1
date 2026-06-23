package org.example.trademodel.service.impl;

import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.mapper.NewsEventMapper;
import org.example.trademodel.service.NewsEventService;
import org.example.trademodel.service.support.ExternalContextImportRequest;
import org.example.trademodel.service.support.ExternalContextImportResult;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NewsEventServiceImpl implements NewsEventService {
    private final NewsEventMapper mapper;
    public NewsEventServiceImpl(NewsEventMapper mapper) { this.mapper = mapper; }

    @Override
    @Transactional
    public ExternalContextImportResult<NewsEventDO> importEvent(ExternalContextImportRequest request) {
        NewsEventDO event = toEvent(request);
        NewsEventDO existing = mapper.selectByDedupeKey(event.getDedupeKey());
        if (existing != null) { return ExternalContextImportResult.deduplicated(existing); }
        try { mapper.insert(event); } catch (DuplicateKeyException ignored) {
            NewsEventDO deduped = mapper.selectByDedupeKey(event.getDedupeKey());
            return ExternalContextImportResult.deduplicated(deduped != null ? deduped : event);
        }
        return ExternalContextImportResult.imported(event);
    }

    @Override public NewsEventDO findByEventId(String eventId) { return mapper.selectByEventId(trim(eventId)); }
    @Override public List<NewsEventDO> listRecent(int limit) { return mapper.selectRecent(Math.max(1, Math.min(limit, 200))); }

    @Override
    public List<NewsEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime) {
        LocalDateTime at = contextTime == null ? LocalDateTime.now() : contextTime;
        return mapper.selectWindowCandidates(at, 500).stream()
                .filter(event -> MacroEventServiceImpl.matchesSymbol(event.getAffectedSymbols(), symbol))
                .filter(event -> MacroEventServiceImpl.matchesMarketScope(event.getMarketScope(), marketScope))
                .filter(event -> {
                    String state = ExternalContextPolicy.windowState(event, at);
                    return ExternalContextPolicy.STATUS_ACTIVE.equals(state) || ExternalContextPolicy.STATUS_NEAR.equals(state);
                })
                .collect(Collectors.toList());
    }

    private NewsEventDO toEvent(ExternalContextImportRequest request) {
        if (request == null) { throw new IllegalArgumentException("news event request is required"); }
        NewsEventDO event = new NewsEventDO();
        event.setEventId(defaultText(request.getEventId(), "news-" + UUID.randomUUID().toString().substring(0, 12)));
        event.setHeadline(required(request.getHeadline(), "headline"));
        event.setSummary(trim(request.getSummary()));
        event.setAffectedSymbols(trim(request.getAffectedSymbols()));
        event.setMarketScope(defaultText(request.getMarketScope(), "GLOBAL"));
        event.setEventTime(requiredTime(request.getEventTime(), "eventTime"));
        event.setWindowStart(requiredTime(request.getWindowStart(), "windowStart"));
        event.setWindowEnd(requiredTime(request.getWindowEnd(), "windowEnd"));
        if (event.getWindowStart().isAfter(event.getWindowEnd())) { throw new IllegalArgumentException("windowStart must be <= windowEnd"); }
        int score = request.getImpactScore() == null ? 0 : request.getImpactScore();
        if (score < 0 || score > 100) { throw new IllegalArgumentException("impactScore must be between 0 and 100"); }
        event.setImpactScore(score);
        event.setSeverity(normalizeAllowed(request.getSeverity(), "severity", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")));
        event.setDirection(normalizeAllowed(request.getDirection(), "direction", List.of("BULLISH", "BEARISH", "NEUTRAL", "UNKNOWN")));
        event.setProvider(required(request.getProvider(), "provider"));
        event.setSourceType(required(request.getSourceType(), "sourceType"));
        event.setSourceReference(required(request.getSourceReference(), "sourceReference"));
        event.setSourceTraceId(required(request.getSourceTraceId(), "sourceTraceId"));
        event.setSourceEventId(trim(request.getSourceEventId()));
        event.setSourceHash(trim(request.getSourceHash()));
        event.setSourcePublishedAt(requiredTime(request.getSourcePublishedAt(), "sourcePublishedAt"));
        if (!ExternalContextPolicy.hasText(event.getSourceEventId()) && !ExternalContextPolicy.hasText(event.getSourceHash())) {
            throw new IllegalArgumentException("sourceEventId or sourceHash is required");
        }
        event.setStatus(normalizeAllowed(defaultText(request.getStatus(), "PUBLISHED"), "status", List.of("PUBLISHED", "ACTIVE", "EXPIRED", "RETRACTED")));
        event.setExecutionBlocking(Boolean.TRUE.equals(request.getExecutionBlocking()));
        event.setDedupeKey(defaultText(request.getDedupeKey(), buildDedupeKey(event)));
        return event;
    }

    private static String buildDedupeKey(NewsEventDO event) {
        String sourceKey = ExternalContextPolicy.hasText(event.getSourceEventId()) ? event.getSourceEventId() : event.getSourceHash();
        return "NEWS:" + event.getProvider() + ":" + event.getSourceTraceId() + ":" + sourceKey;
    }

    private static String normalizeAllowed(String raw, String name, List<String> allowed) {
        String normalized = required(raw, name).toUpperCase();
        if (allowed.contains(normalized) == false) { throw new IllegalArgumentException(name + " is invalid"); }
        return normalized;
    }
    private static LocalDateTime requiredTime(LocalDateTime value, String name) {
        if (value == null) { throw new IllegalArgumentException(name + " is required"); }
        return value;
    }
    private static String required(String value, String name) {
        String text = trim(value);
        if (text == null) { throw new IllegalArgumentException(name + " is required"); }
        return text;
    }
    private static String defaultText(String value, String fallback) { String text = trim(value); return text == null ? fallback : text; }
    private static String trim(String value) {
        if (value == null) { return null; }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
