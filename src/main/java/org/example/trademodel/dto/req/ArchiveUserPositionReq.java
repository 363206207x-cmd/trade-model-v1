package org.example.trademodel.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArchiveUserPositionReq {
    @JsonAlias({"submission_id", "idempotencyKey", "idempotency_key"})
    private String submissionId;
    private String reason;

    @JsonIgnore
    private final Map<String, Object> extraFields = new LinkedHashMap<>();

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @JsonAnySetter
    public void putExtraField(String name, Object value) { extraFields.put(name, value); }

    public Map<String, Object> getExtraFields() { return extraFields; }
}
