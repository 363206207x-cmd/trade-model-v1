package org.example.trademodel.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalUsernamePolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "operator",
            "user.name",
            "user_name",
            "user-name",
            "user@example.com"
    })
    void acceptsOnlyDocumentedAsciiUsernameCharacters(String username) {
        assertThat(PersonalUsernamePolicy.isValid(PersonalUsernamePolicy.normalize(username))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "operator outcome=SUCCESS",
            "operator=admin",
            "operator\nadmin",
            "operator\radmin",
            "operator\tadmin",
            "operator\u0085admin",
            "operator\u2028admin",
            "operator\u2029admin",
            "operator\u200Badmin",
            "操作员"
    })
    void rejectsSeparatorsControlsUnicodeAndInvisibleCharacters(String username) {
        assertThat(PersonalUsernamePolicy.isValid(PersonalUsernamePolicy.normalize(username))).isFalse();
    }
}
