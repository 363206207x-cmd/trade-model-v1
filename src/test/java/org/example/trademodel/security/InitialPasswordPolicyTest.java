package org.example.trademodel.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InitialPasswordPolicyTest {

    @Test
    void acceptsExactlyEightCharactersWithoutCompositionRequirements() {
        assertThat(InitialPasswordPolicy.validate("abcdefgh", "another").accepted()).isTrue();
        assertThat(InitialPasswordPolicy.validate("12ab cd!", "another").accepted()).isTrue();
        assertThat(InitialPasswordPolicy.minimumLength()).isEqualTo(8);
        assertThat(InitialPasswordPolicy.maximumLength()).isEqualTo(8);
    }

    @Test
    void rejectsSevenNineBlankAndSurroundingWhitespace() {
        assertThat(InitialPasswordPolicy.validate("1234567").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_TOO_SHORT);
        assertThat(InitialPasswordPolicy.validate("123456789").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_TOO_LONG);
        assertThat(InitialPasswordPolicy.validate("        ").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_MISSING);
        assertThat(InitialPasswordPolicy.validate(" abcdefg").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_SURROUNDING_WHITESPACE);
        assertThat(InitialPasswordPolicy.validate("abcdefg ").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_SURROUNDING_WHITESPACE);
    }

    @Test
    void rejectsKnownDefaultsAndUsernameEquality() {
        assertThat(InitialPasswordPolicy.validate("password").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_UNSAFE_VALUE);
        assertThat(InitialPasswordPolicy.validate("12345678").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_UNSAFE_VALUE);
        assertThat(InitialPasswordPolicy.validate("UserName", "username").reasonCode())
                .isEqualTo(InitialPasswordPolicy.ReasonCode.PASSWORD_MATCHES_USERNAME);
    }
}
