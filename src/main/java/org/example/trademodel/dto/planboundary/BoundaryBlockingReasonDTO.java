package org.example.trademodel.dto.planboundary;

public class BoundaryBlockingReasonDTO {
    private String code;
    private String text;
    private String field;
    private String severity;

    public static BoundaryBlockingReasonDTO of(String code, String text) {
        BoundaryBlockingReasonDTO reason = new BoundaryBlockingReasonDTO();
        reason.setCode(code);
        reason.setText(text);
        return reason;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
