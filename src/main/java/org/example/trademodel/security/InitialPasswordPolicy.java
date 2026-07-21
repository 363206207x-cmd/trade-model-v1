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

    public static boolean isUnsafe(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < MIN_LENGTH || UNSAFE_VALUES.contains(normalized)) {
            return true;
        }
        if (normalized.startsWith("<") && normalized.endsWith(">")) {
            return true;
        }
        return TEMPLATE_PREFIXES.stream().anyMatch(normalized::startsWith);
    }
}
