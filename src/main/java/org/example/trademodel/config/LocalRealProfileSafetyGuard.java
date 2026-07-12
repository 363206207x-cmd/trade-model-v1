package org.example.trademodel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("local-real")
public class LocalRealProfileSafetyGuard implements ApplicationRunner {
    private static final Set<String> LOOPBACK_ADDRESSES = Set.of("127.0.0.1", "localhost", "::1");
    private final Environment environment;

    public LocalRealProfileSafetyGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate(environment);
    }

    static void validate(Environment environment) {
        boolean authEnabled = environment.getProperty("trade-model.auth.enabled", Boolean.class, true);
        String address = environment.getProperty("server.address", "").trim().toLowerCase();
        if (!authEnabled && !LOOPBACK_ADDRESSES.contains(address)) {
            throw new IllegalStateException("local-real auth may be disabled only on an explicit loopback address");
        }
        if (environment.getProperty("trade-model.ai.enabled", Boolean.class, false)) {
            throw new IllegalStateException("local-real must keep AI providers disabled");
        }
        if (environment.getProperty("trade-model.providers.coinglass.enabled", Boolean.class, false)) {
            throw new IllegalStateException("local-real must keep CoinGlass disabled");
        }
    }
}
