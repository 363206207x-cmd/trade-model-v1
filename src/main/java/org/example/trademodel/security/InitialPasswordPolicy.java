package org.example.trademodel.security;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class InitialPasswordPolicy {
    private static final int MIN_LENGTH = 12;
    private static final Set<String> UNSAFE_VALUES = Set.of(
            "PASSWORD",
            "ADMIN",
            "CHANGE-ME",
            "CHANGEME",
            "CHANGE_THIS",
            "123456",
            "DEV-LOCAL-PASSWORD",
            "PLACEHOLDER",
            "EXAMPLE-PASSWORD",
            "SAMPLE-PASSWORD",
            "DEFAULT-PASSWORD",
            "SECRET-PASSWORD",
            "LONG-LOCAL-SECRET",
            "YOUR-PASSWORD",
            "YOUR-SECRET"
    );
    private static final List<String> TEMPLATE_PREFIXES = List.of(
            "REPLACE-WITH-",
            "REPLACE_WITH_",
            "CHANGE-ME-",
            "CHANGE_ME_",
            "CHANGE-THIS-",
            "CHANGE_THIS_",
            "PLACEHOLDER-",
            "PLACEHOLDER_",
            "EXAMPLE-",
            "EXAMPLE_",
            "SAMPLE-",
            "SAMPLE_",
            "DEFAULT-",
            "DEFAULT_"
    );

    private InitialPasswordPolicy() {
    }

    public enum ReasonCode {
        NONE,
        PASSWORD_MISSING,
        PASSWORD_TOO_SHORT,
        PASSWORD_UNSAFE_VALUE,
        PASSWORD_TEMPLATE_VALUE
    }

    public record Validation(boolean accepted, ReasonCode reasonCode) {
        static Validation pass() {
            return new Validation(true, ReasonCode.NONE);
        }

        static Validation reject(ReasonCode reasonCode) {
            return new Validation(false, reasonCode);
        }
    }

    public static Validation validate(String value) {
        if (value == null || value.isBlank()) {
            return Validation.reject(ReasonCode.PASSWORD_MISSING);
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < MIN_LENGTH) {
            return Validation.reject(ReasonCode.PASSWORD_TOO_SHORT);
        }
        if (UNSAFE_VALUES.contains(normalized)) {
            return Validation.reject(ReasonCode.PASSWORD_UNSAFE_VALUE);
        }
        if (normalized.startsWith("<") && normalized.endsWith(">")) {
            return Validation.reject(ReasonCode.PASSWORD_TEMPLATE_VALUE);
        }
        if (TEMPLATE_PREFIXES.stream().anyMatch(normalized::startsWith)) {
            return Validation.reject(ReasonCode.PASSWORD_TEMPLATE_VALUE);
        }
        return Validation.pass();
    }

    public static boolean isUnsafe(String value) {
        return !validate(value).accepted();
    }

    public static int minimumLength() {
        return MIN_LENGTH;
    }
}
