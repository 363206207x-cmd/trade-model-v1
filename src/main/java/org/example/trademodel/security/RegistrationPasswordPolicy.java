package org.example.trademodel.security;

public final class RegistrationPasswordPolicy {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private RegistrationPasswordPolicy() {
    }

    public static void requireValid(String password) {
        int length = password == null ? 0 : password.length();
        if (length < MIN_LENGTH) {
            throw new IllegalArgumentException("password is shorter than 8 characters");
        }
        if (length > MAX_LENGTH) {
            throw new IllegalArgumentException("password is longer than 128 characters");
        }
    }
}
