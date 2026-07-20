package org.example.trademodel.security;

import java.util.Locale;
import java.util.regex.Pattern;

final class PersonalUsernamePolicy {
    static final int MAX_LENGTH = 64;
    private static final Pattern SAFE_USERNAME = Pattern.compile("[a-z0-9._@-]+");

    private PersonalUsernamePolicy() {
    }

    static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isValid(String username) {
        if (username == null || username.isBlank() || username.length() > MAX_LENGTH) {
            return false;
        }
        return SAFE_USERNAME.matcher(username).matches();
    }
}
