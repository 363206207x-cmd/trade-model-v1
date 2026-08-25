package org.example.trademodel.security;

public final class RegistrationPasswordPolicy {
    public static final int MIN_LENGTH = InitialPasswordPolicy.minimumLength();
    public static final int MAX_LENGTH = InitialPasswordPolicy.maximumLength();

    private RegistrationPasswordPolicy() {
    }

    public static void requireValid(String password) {
        requireValid(password, null);
    }

    public static void requireValid(String password, String username) {
        InitialPasswordPolicy.Validation validation = InitialPasswordPolicy.validate(password, username);
        if (!validation.accepted()) {
            throw new IllegalArgumentException("password policy rejected: " + validation.reasonCode().name());
        }
    }
}
