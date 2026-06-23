package org.example.trademodel.service.support;

public class ExternalContextImportResult<T> {
    private final T event;
    private final boolean deduplicated;
    private final String reasonCode;

    private ExternalContextImportResult(T event, boolean deduplicated, String reasonCode) {
        this.event = event;
        this.deduplicated = deduplicated;
        this.reasonCode = reasonCode;
    }

    public static <T> ExternalContextImportResult<T> imported(T event) {
        return new ExternalContextImportResult<>(event, false, "IMPORTED");
    }

    public static <T> ExternalContextImportResult<T> deduplicated(T event) {
        return new ExternalContextImportResult<>(event, true, "DEDUPLICATED_EXISTING_EVENT");
    }

    public T getEvent() { return event; }
    public boolean isDeduplicated() { return deduplicated; }
    public String getReasonCode() { return reasonCode; }
}
