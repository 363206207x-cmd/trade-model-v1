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

    @ParameterizedTest
    @ValueSource(strings = {"abc", "user.name", "user_name", "user-name", "A1b"})
    void acceptsFrozenRegistrationFormat(String username) {
        assertThat(PersonalUsernamePolicy.isRegistrationValid(
                PersonalUsernamePolicy.normalize(username))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "user@example.com", "name with space", "用户", "a_very_long_registration_username_33"})
    void rejectsRegistrationValuesOutsideFrozenFormat(String username) {
        assertThat(PersonalUsernamePolicy.isRegistrationValid(
                PersonalUsernamePolicy.normalize(username))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"xuchao", "OWNER", "admin", "Administrator", "root", "system", "operator"})
    void reservesOwnerAndSystemIdentitiesFromPublicRegistration(String username) {
        assertThat(PersonalUsernamePolicy.isReservedRegistrationUsername(username)).isTrue();
    }
}
