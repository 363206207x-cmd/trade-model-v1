package org.example.trademodel.vo;

import java.util.List;

public class PositionMonitorActionAdviceVO {
    private String actionCode;
    private String actionText;
    private List<String> reasonCodes;
    private String reasonText;
    private Boolean manualOnly;
    private Boolean notTradeInstruction;
    private List<String> riskNotes;
    private String disclaimerText;

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public List<String> getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        this.reasonText = reasonText;
    }

    public Boolean getManualOnly() {
        return manualOnly;
    }

    public void setManualOnly(Boolean manualOnly) {
        this.manualOnly = manualOnly;
    }

    public Boolean getNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(Boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
    }

    public List<String> getRiskNotes() {
        return riskNotes;
    }

    public void setRiskNotes(List<String> riskNotes) {
        this.riskNotes = riskNotes;
    }

    public String getDisclaimerText() {
        return disclaimerText;
    }

    public void setDisclaimerText(String disclaimerText) {
        this.disclaimerText = disclaimerText;
    }
}
