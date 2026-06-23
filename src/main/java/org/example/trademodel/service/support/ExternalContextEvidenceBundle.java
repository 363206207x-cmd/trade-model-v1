package org.example.trademodel.service.support;

import org.example.trademodel.vo.EvidenceItemVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExternalContextEvidenceBundle {
    private final ExternalContextSnapshot snapshot;
    private final List<EvidenceItemVO> evidenceItems;

    public ExternalContextEvidenceBundle(ExternalContextSnapshot snapshot, List<EvidenceItemVO> evidenceItems) {
        this.snapshot = snapshot == null ? new ExternalContextSnapshot() : snapshot;
        this.evidenceItems = evidenceItems == null ? new ArrayList<>() : new ArrayList<>(evidenceItems);
    }

    public ExternalContextSnapshot getSnapshot() { return snapshot; }
    public List<EvidenceItemVO> getEvidenceItems() { return Collections.unmodifiableList(evidenceItems); }
}
