package org.example.trademodel.vo;

import java.util.Map;

public class MissedReasonViewVO {

    private String version;
    private String rule;
    private String whyMissed;
    private Map<String, Object> facts;
    private Map<String, Object> refs;
    private String parseStatus;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getWhyMissed() {
        return whyMissed;
    }

    public void setWhyMissed(String whyMissed) {
        this.whyMissed = whyMissed;
    }

    public Map<String, Object> getFacts() {
        return facts;
    }

    public void setFacts(Map<String, Object> facts) {
        this.facts = facts;
    }

    public Map<String, Object> getRefs() {
        return refs;
    }

    public void setRefs(Map<String, Object> refs) {
        this.refs = refs;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }
}
