package org.example.trademodel.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLoggingFilterTest {

    @Test
    void reportsUnhandledFailureAsInternalServerErrorWhenResponseStillLooksSuccessful() {
        assertThat(AccessLoggingFilter.statusForLog(200, false)).isEqualTo(500);
    }

    @Test
    void preservesExplicitErrorStatusAndNormalCompletionStatus() {
        assertThat(AccessLoggingFilter.statusForLog(503, false)).isEqualTo(503);
        assertThat(AccessLoggingFilter.statusForLog(302, true)).isEqualTo(302);
        assertThat(AccessLoggingFilter.statusForLog(200, true)).isEqualTo(200);
    }
}
