package org.example.trademodel.vo;

public class ScoreItemVO {
    private String scoreId;
    private String scoreType;      // 趋势结构分、资金推动分、杠杆风险分...
    private Double scoreValue;     // 0-100
    private Double weight;
    private String direction;
    private String description;

    public String getScoreId() { return scoreId; }
    public void setScoreId(String scoreId) { this.scoreId = scoreId; }
    public String getScoreType() { return scoreType; }
    public void setScoreType(String scoreType) { this.scoreType = scoreType; }
    public Double getScoreValue() { return scoreValue; }
    public void setScoreValue(Double scoreValue) { this.scoreValue = scoreValue; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
