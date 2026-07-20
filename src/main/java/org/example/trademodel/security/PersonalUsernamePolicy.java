package org.example.trademodel.security;

import java.util.Locale;

final class PersonalUsernamePolicy {
    static final int MAX_LENGTH = 64;

    private PersonalUsernamePolicy() {
    }

    static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isValid(String username) {
        if (username == null || username.isBlank() || username.length() > MAX_LENGTH) {
            return false;
        }
        return username.chars().noneMatch(Character::isISOControl);
    }
}
