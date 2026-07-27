package org.example.trademodel.risk;

/**
 * Read-only account risk view over manual UserPosition facts.
 * Future monitor code can consume this interface without depending on a controller.
 */
public interface UserPositionRiskAdapter {
    UserPositionRiskResult currentRiskForUser(Long userId);

    UserPositionRiskResult currentRiskForSystem();
}
