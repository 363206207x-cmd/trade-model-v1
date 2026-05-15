package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BoundarySourceFieldsDTO {

    private String entrySourceField;
    private String stopSourceField;
    private String takeProfitSourceField;
    private String rrRule;
    private String dataSource;
    private BigDecimal dataQualityScore;
    private List<String> evidenceRefs = new ArrayList<>();

    public String getEntrySourceField() {
        return entrySourceField;
    }

    public void setEntrySourceField(String entrySourceField) {
        this.entrySourceField = entrySourceField;
    }

    public String getStopSourceField() {
        return stopSourceField;
    }

    public void setStopSourceField(String stopSourceField) {
        this.stopSourceField = stopSourceField;
    }

    public String getTakeProfitSourceField() {
        return takeProfitSourceField;
    }

    public void setTakeProfitSourceField(String takeProfitSourceField) {
        this.takeProfitSourceField = takeProfitSourceField;
    }

    public String getRrRule() {
        return rrRule;
    }

    public void setRrRule(String rrRule) {
        this.rrRule = rrRule;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(BigDecimal dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public List<String> getEvidenceRefs() {
        return evidenceRefs;
    }

    public void setEvidenceRefs(List<String> evidenceRefs) {
        this.evidenceRefs = evidenceRefs == null ? new ArrayList<>() : new ArrayList<>(evidenceRefs);
    }
}
