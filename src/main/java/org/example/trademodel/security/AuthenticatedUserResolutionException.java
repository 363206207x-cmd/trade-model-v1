package org.example.trademodel.security;

public class AuthenticatedUserResolutionException extends RuntimeException {
    public AuthenticatedUserResolutionException() {
        super("authentication required");
    }
}
