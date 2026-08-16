package org.example.trademodel.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramLinkPolicyTest {

    @Test
    void publicHttpsLinksUseOnlyCanonicalRecheckAndPositionPaths() {
        TelegramProperties properties = new TelegramProperties();
        properties.setPublicBaseUrl("https://app.example.test/workspace/");
        TelegramLinkPolicy policy = new TelegramLinkPolicy(properties);

        assertThat(policy.pushRecheckLink("push 41"))
                .isEqualTo("https://app.example.test/recheck/push%2041");
        assertThat(policy.positionDetailLink(91L))
                .isEqualTo("https://app.example.test/positions/91");
    }

    @Test
    void localPrivateHttpAndCrossHostLinksFailClosed() {
        for (String base : new String[]{
                "http://app.example.test", "https://localhost:8080", "https://127.0.0.1",
                "https://10.0.0.4", "https://172.16.0.4", "https://192.168.1.4", "https://service.local"}) {
            TelegramProperties properties = new TelegramProperties();
            properties.setPublicBaseUrl(base);
            assertThat(new TelegramLinkPolicy(properties).pushRecheckLink("41")).isNull();
        }

        TelegramProperties properties = new TelegramProperties();
        properties.setPublicBaseUrl("https://app.example.test");
        TelegramLinkPolicy policy = new TelegramLinkPolicy(properties);
        assertThat(policy.safeLink("https://attacker.example/recheck/41")).isNull();
        assertThat(policy.safeLink("/recheck/41?token=forbidden")).isNull();
    }
}
