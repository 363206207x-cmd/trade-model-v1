package org.example.trademodel.entity;

public class NewsEventDO extends ExternalContextEventDO {
    private String headline;
    private String summary;

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
