package org.example.trademodel.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionProfileSafetyGuard implements ApplicationRunner {

    private static final Set<String> UNSAFE_ADMIN_PASSWORDS = Set.of(
            "PASSWORD",
            "ADMIN",
            "CHANGE-ME",
            "CHANGEME",
            "123456",
            "DEV-LOCAL-PASSWORD"
    );

    private static final Set<String> ALLOWED_ACTUATOR_EXPOSURE = Set.of("HEALTH");

    private final Environment environment;

    public ProductionProfileSafetyGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate(environment);
    }

    static void validate(Environment environment) {
        List<String> errors = new ArrayList<>();

        String datasourceUrl = property(environment, "spring.datasource.url");
        if (isBlank(datasourceUrl)) {
            errors.add("production datasource URL missing");
        } else if (isH2MemoryUrl(datasourceUrl)) {
            errors.add("H2 memory database is not allowed in prod");
        }

        String datasourceUsername = property(environment, "spring.datasource.username");
        if (isBlank(datasourceUsername)) {
            errors.add("production datasource username missing");
        }

        String datasourcePassword = property(environment, "spring.datasource.password");
        if (isBlank(datasourcePassword)) {
            errors.add("production datasource password missing");
        }

        if (isTrue(property(environment, "spring.h2.console.enabled"))) {
            errors.add("H2 console must be disabled in prod");
        }

        String providerType = normalized(property(environment, "position.provider.type"));
        if (isBlank(providerType)) {
            errors.add("production position provider type missing");
        } else if ("SIMULATED".equals(providerType)) {
            errors.add("simulated provider is not allowed in prod");
        } else if (!"BINANCE".equals(providerType)) {
            errors.add("unsupported production position provider type: " + providerType);
        }

        if ("BINANCE".equals(providerType)) {
            if (isBlank(property(environment, "binance.api.key"))) {
                errors.add("Binance API key missing for production position provider");
            }
            if (isBlank(property(environment, "binance.api.secret"))) {
                errors.add("Binance API secret missing for production position provider");
            }
        }

        validateExplicitAiProvider(environment, errors, "OpenAI", "trade-model.ai.openai");
        validateExplicitAiProvider(environment, errors, "Gemini", "trade-model.ai.gemini");
        validateExplicitAiProvider(environment, errors, "xAI", "trade-model.ai.xai");

        String serverAddress = normalizedAddress(property(environment, "server.address"));
        if (isPublicBind(serverAddress) && !isTrue(property(environment, "trade-model.production.allow-public-bind"))) {
            errors.add("production public server bind requires trade-model.production.allow-public-bind=true");
        }

        String actuatorExposure = property(environment, "management.endpoints.web.exposure.include");
        if (hasUnsafeActuatorExposure(actuatorExposure)) {
            errors.add("production actuator web exposure must be limited to health");
        }

        String adminUsername = property(environment, "trade-model.auth.admin-username");
        if (isBlank(adminUsername)) {
            errors.add("production admin username missing");
        }

        String adminPassword = property(environment, "trade-model.auth.admin-password");
        if (isBlank(adminPassword)) {
            errors.add("production admin password missing");
        } else if (isUnsafeAdminPassword(adminPassword)) {
            errors.add("production admin password uses an unsafe default value");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe prod profile config: " + String.join("; ", errors));
        }
    }

    private static String property(Environment environment, String key) {
        try {
            return environment.getProperty(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isH2MemoryUrl(String datasourceUrl) {
        return normalized(datasourceUrl).startsWith("JDBC:H2:MEM");
    }

    private static boolean isPublicBind(String serverAddress) {
        return "0.0.0.0".equals(serverAddress) || "::".equals(serverAddress);
    }

    private static boolean isTrue(String value) {
        return "true".equalsIgnoreCase(trim(value));
    }

    private static boolean isUnsafeAdminPassword(String value) {
        return UNSAFE_ADMIN_PASSWORDS.contains(normalized(value));
    }

    private static boolean hasUnsafeActuatorExposure(String exposure) {
        if (isBlank(exposure)) {
            return false;
        }
        for (String rawEndpoint : exposure.split(",")) {
            String endpoint = normalized(rawEndpoint);
            if (!ALLOWED_ACTUATOR_EXPOSURE.contains(endpoint)) {
                return true;
            }
        }
        return false;
    }

    private static void validateExplicitAiProvider(Environment environment,
                                                   List<String> errors,
                                                   String displayName,
                                                   String propertyPrefix) {
        if (!isTrue(property(environment, propertyPrefix + ".enabled"))) {
            return;
        }
        if (isBlank(property(environment, propertyPrefix + ".api-key"))) {
            errors.add(displayName + " API key missing for explicitly enabled production AI provider");
        }
        if (isBlank(property(environment, propertyPrefix + ".model"))) {
            errors.add(displayName + " model missing for explicitly enabled production AI provider");
        }
        if (isBlank(property(environment, propertyPrefix + ".base-url"))) {
            errors.add(displayName + " base URL missing for explicitly enabled production AI provider");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalized(String value) {
        return trim(value).toUpperCase(Locale.ROOT);
    }

    private static String normalizedAddress(String value) {
        return trim(value).toLowerCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
