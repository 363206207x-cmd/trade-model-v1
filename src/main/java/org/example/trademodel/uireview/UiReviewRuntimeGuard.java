package org.example.trademodel.uireview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Profile("ui-review")
@Component
public class UiReviewRuntimeGuard implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(UiReviewRuntimeGuard.class);
    private static final List<String> FORBIDDEN_TRUE_PROPERTIES = List.of(
            "trade-model.schedulers.enabled",
            "trade-model.provider-call.enabled",
            "trade-model.provider-call.external-calls-enabled",
            "trade-model.ohlcv.public-provider.enabled",
            "trade-model.ohlcv.public-provider.external-calls-enabled",
            "trade-model.providers.coinglass.enabled",
            "trade-model.providers.coinglass.external-calls-enabled",
            "trade-model.ai.enabled",
            "trade-model.ai.openai.enabled",
            "trade-model.ai.gemini.enabled",
            "trade-model.ai.xai.enabled");

    private final Environment environment;

    public UiReviewRuntimeGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase)) {
            throw new IllegalStateException("UI_REVIEW_PROD_PROFILE_CONFLICT");
        }
        if (!environment.getProperty("trade-model.ui-review.enabled", Boolean.class, false)) {
            throw new IllegalStateException("UI_REVIEW_PROPERTY_NOT_ENABLED");
        }
        FORBIDDEN_TRUE_PROPERTIES.stream()
                .filter(property -> environment.getProperty(property, Boolean.class, false))
                .findFirst()
                .ifPresent(property -> {
                    throw new IllegalStateException("UI_REVIEW_EXTERNAL_CAPABILITY_ENABLED:" + property);
                });
        log.info("UI_REVIEW_FIXTURE=ENABLED");
    }
}
