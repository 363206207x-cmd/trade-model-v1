package org.example.trademodel.service;

import org.example.trademodel.entity.EventAssetRelationDO;
import org.example.trademodel.mapper.EventAssetRelationMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EventAssetRelationService {
    private static final Set<String> EVENT_TYPES = Set.of("MACRO", "INDUSTRY", "PROJECT", "HOT_RESET");
    private static final Set<String> RELATION_TYPES = Set.of(
            "AFFECTS_ASSET", "TRIGGERS_REVALIDATION", "CONTEXT_ONLY");

    private final EventAssetRelationMapper mapper;

    public EventAssetRelationService(EventAssetRelationMapper mapper) {
        this.mapper = mapper;
    }

    public EventAssetRelationDO record(EventAssetRelationDO row) {
        if (row == null || !EVENT_TYPES.contains(row.getEventType())
                || !RELATION_TYPES.contains(row.getRelationType())) {
            throw new IllegalArgumentException("event relation semantics are required");
        }
        if (!hasText(row.getEventId()) || !hasText(row.getSymbol()) || !hasText(row.getSourceReference())) {
            throw new IllegalArgumentException("eventId, symbol and sourceReference are required");
        }
        row.setRelationId(hasText(row.getRelationId()) ? row.getRelationId() : "event-relation-" + UUID.randomUUID());
        row.setCreatedAt(row.getCreatedAt() == null ? LocalDateTime.now(Clock.systemUTC()) : row.getCreatedAt());
        mapper.insert(row);
        return row;
    }

    public List<EventAssetRelationDO> listBySymbol(String symbol, int limit) {
        if (!hasText(symbol)) return List.of();
        return mapper.listBySymbol(symbol.trim().toUpperCase(), Math.max(1, Math.min(limit, 50)));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
