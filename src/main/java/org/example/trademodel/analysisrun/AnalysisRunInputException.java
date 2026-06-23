package org.example.trademodel.analysisrun;

public class AnalysisRunInputException extends IllegalArgumentException {
    private final String reasonCode;

    public AnalysisRunInputException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
