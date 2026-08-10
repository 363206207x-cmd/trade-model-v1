package org.example.trademodel.decisionchain;

import java.util.List;

public record RuleValidationResult(boolean passed, List<String> reasons) {
    public static RuleValidationResult pass() { return new RuleValidationResult(true, List.of()); }
    public static RuleValidationResult blocked(List<String> reasons) {
        return new RuleValidationResult(false, reasons == null ? List.of("RULE_VALIDATION_BLOCKED") : List.copyOf(reasons));
    }
}
