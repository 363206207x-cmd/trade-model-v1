package org.example.trademodel.service;

import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.service.support.ExternalContextImportRequest;
import org.example.trademodel.service.support.ExternalContextImportResult;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsEventService {
    ExternalContextImportResult<NewsEventDO> importEvent(ExternalContextImportRequest request);
    NewsEventDO findByEventId(String eventId);
    List<NewsEventDO> listRecent(int limit);
    List<NewsEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime);
}
