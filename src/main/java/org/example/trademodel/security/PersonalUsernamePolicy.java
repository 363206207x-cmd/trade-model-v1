package org.example.trademodel.security;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class PersonalUsernamePolicy {
    public static final int MAX_LENGTH = 64;
    public static final int REGISTRATION_MIN_LENGTH = 3;
    public static final int REGISTRATION_MAX_LENGTH = 32;
    private static final Pattern SAFE_USERNAME = Pattern.compile("[a-z0-9._@-]+");
    private static final Pattern SAFE_REGISTRATION_USERNAME = Pattern.compile("[a-z0-9._-]+");
    private static final Set<String> RESERVED_REGISTRATION_USERNAMES = Set.of(
            "xuchao", "owner", "admin", "administrator", "root", "system", "operator");

    private PersonalUsernamePolicy() {
    }

    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String username) {
        if (username == null || username.isBlank() || username.length() > MAX_LENGTH) {
            return false;
        }
        return SAFE_USERNAME.matcher(username).matches();
    }

    public static boolean isRegistrationValid(String username) {
        if (username == null
                || username.length() < REGISTRATION_MIN_LENGTH
                || username.length() > REGISTRATION_MAX_LENGTH) {
            return false;
        }
        return SAFE_REGISTRATION_USERNAME.matcher(username).matches();
    }

    public static boolean isReservedRegistrationUsername(String username) {
        return RESERVED_REGISTRATION_USERNAMES.contains(normalize(username));
    }
}
