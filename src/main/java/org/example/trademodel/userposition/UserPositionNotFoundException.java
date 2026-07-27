package org.example.trademodel.userposition;

public class UserPositionNotFoundException extends RuntimeException {
    public UserPositionNotFoundException() {
        super("UserPosition not found");
    }
}
