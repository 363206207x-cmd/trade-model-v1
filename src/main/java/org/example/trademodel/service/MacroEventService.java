package org.example.trademodel.service;

import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.service.support.ExternalContextImportRequest;
import org.example.trademodel.service.support.ExternalContextImportResult;

import java.time.LocalDateTime;
import java.util.List;

public interface MacroEventService {
    ExternalContextImportResult<MacroEventDO> importEvent(ExternalContextImportRequest request);
    MacroEventDO findByEventId(String eventId);
    List<MacroEventDO> listRecent(int limit);
    List<MacroEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime);
}
