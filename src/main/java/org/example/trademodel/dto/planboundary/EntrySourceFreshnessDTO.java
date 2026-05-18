package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

/**
 * Freshness metadata for a future rule-owned entry candidate.
 */
public class EntrySourceFreshnessDTO {

    private String freshnessStatus;
    private String staleReasonCode;
    private String staleReasonText;
    private Long observedAtMs;
    private Long decisionCreateTimeMs;
    private List<String> missingFields = new ArrayList<>();

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getStaleReasonCode() {
        return staleReasonCode;
    }

    public void setStaleReasonCode(String staleReasonCode) {
        this.staleReasonCode = staleReasonCode;
    }

    public String getStaleReasonText() {
        return staleReasonText;
    }

    public void setStaleReasonText(String staleReasonText) {
        this.staleReasonText = staleReasonText;
    }

    public Long getObservedAtMs() {
        return observedAtMs;
    }

    public void setObservedAtMs(Long observedAtMs) {
        this.observedAtMs = observedAtMs;
    }

    public Long getDecisionCreateTimeMs() {
        return decisionCreateTimeMs;
    }

    public void setDecisionCreateTimeMs(Long decisionCreateTimeMs) {
        this.decisionCreateTimeMs = decisionCreateTimeMs;
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? new ArrayList<>() : new ArrayList<>(missingFields);
    }
}
