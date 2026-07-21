package org.example.trademodel.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LoginAuditLoggerTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void usernameWithEqualsCannotInjectAuditField() {
        String sanitized = LoginAuditLogger.safeAuditValue("operator=outcome=SUCCESS");

        assertThat(sanitized)
                .isEqualTo("operator_u003D_outcome_u003D_SUCCESS")
                .doesNotContain("=");
    }

    @Test
    void usernameWithSpacesCannotInjectAuditField() {
        String sanitized = LoginAuditLogger.safeAuditValue("operator outcome=SUCCESS");

        assertThat(sanitized)
                .isEqualTo("operator_u0020_outcome_u003D_SUCCESS")
                .doesNotContain(" ");
    }

    @Test
    void unicodeLineSeparatorCannotCreateNewLogLine(CapturedOutput output) {
        new LoginAuditLogger(FIXED_CLOCK).failure("operator\u2028outcome=SUCCESS", "invalid_credentials");

        assertThat(output).doesNotContain("\u2028", "outcome=SUCCESS");
        assertThat(countOccurrences(output.toString(), "LOGIN_AUDIT")).isEqualTo(1);
    }

    @Test
    void carriageReturnLineFeedTabAreSanitized() {
        String sanitized = LoginAuditLogger.safeAuditValue("operator\r\noutcome\tSUCCESS");

        assertThat(sanitized)
                .isEqualTo("operator_u000D__u000A_outcome_u0009_SUCCESS")
                .doesNotContain("\r", "\n", "\t");
    }

    @Test
    void auditOutputDoesNotContainInjectedOutcomeOrReasonField(CapturedOutput output) {
        new LoginAuditLogger(FIXED_CLOCK).failure(
                "operator outcome=SUCCESS reason=credentials_valid",
                "invalid_credentials\noutcome=SUCCESS");

        assertThat(output)
                .contains("outcome=FAILURE")
                .doesNotContain("outcome=SUCCESS", "reason=credentials_valid")
                .doesNotContain("invalid_credentials\noutcome");
    }

    private static int countOccurrences(String value, String token) {
        return value.split(token, -1).length - 1;
    }
}
