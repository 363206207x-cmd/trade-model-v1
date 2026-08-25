package org.example.trademodel.uireview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UiReviewRuntimeGuardTest {
    @Test
    void acceptsIsolatedUiReviewRuntime() {
        MockEnvironment environment = reviewEnvironment();
        assertThatCode(() -> new UiReviewRuntimeGuard(environment)
                .run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void rejectsProdAndUiReviewTogether() {
        MockEnvironment environment = reviewEnvironment();
        environment.setActiveProfiles("ui-review", "prod");

        assertThatThrownBy(() -> new UiReviewRuntimeGuard(environment)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("UI_REVIEW_PROD_PROFILE_CONFLICT");
    }

    @Test
    void rejectsAnyEnabledExternalCapability() {
        MockEnvironment environment = reviewEnvironment()
                .withProperty("trade-model.provider-call.external-calls-enabled", "true");

        assertThatThrownBy(() -> new UiReviewRuntimeGuard(environment)
                .run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UI_REVIEW_EXTERNAL_CAPABILITY_ENABLED");
    }

    private MockEnvironment reviewEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("trade-model.ui-review.enabled", "true");
        environment.setActiveProfiles("ui-review");
        return environment;
    }
}
