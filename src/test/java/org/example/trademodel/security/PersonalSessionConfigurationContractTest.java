package org.example.trademodel.security;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalSessionConfigurationContractTest {

    @Test
    void sessionCookieAndFormSecurityContractAreExplicit() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));
        String production = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        String security = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/config/SecurityConfig.java"));

        assertThat(application).contains(
                "timeout: ${TRADE_MODEL_SESSION_TIMEOUT:30m}",
                "http-only: true",
                "same-site: lax",
                "secure: ${TRADE_MODEL_SESSION_COOKIE_SECURE:false}",
                "org.example.trademodel.mapper.PersonalUserMapper: INFO");
        assertThat(production).contains("secure: ${TRADE_MODEL_SESSION_COOKIE_SECURE:true}");
        assertThat(security).contains(
                ".formLogin(",
                ".loginPage(\"/login\")",
                ".loginProcessingUrl(\"/login\")",
                ".sessionFixation(fixation -> fixation.migrateSession())",
                ".logoutUrl(\"/logout\")");
        assertThat(security).doesNotContain(".httpBasic(", "SessionCreationPolicy.STATELESS");
    }

    @Test
    void responsiveLoginAndCsrfFetchContractsArePresent() throws Exception {
        String login = Files.readString(Path.of("src/main/resources/templates/login.html"));
        String css = Files.readString(Path.of("src/main/resources/static/css/login.css"));
        String dashboard = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));
        String review = Files.readString(Path.of("src/main/resources/templates/review.html"));
        String reviewJs = Files.readString(Path.of("src/main/resources/static/js/review-page.js"));

        assertThat(login).contains(
                "width=device-width, initial-scale=1",
                "autocomplete=\"username\"",
                "autocomplete=\"current-password\"",
                "th:action=\"@{/login}\"",
                "method=\"post\"");
        assertThat(login).doesNotContain("注册", "OAuth", "JWT");
        assertThat(css).contains("font-size: 16px", "width: min(100%, 392px)", "overflow-wrap: anywhere");
        assertThat(dashboard).contains("name=\"_csrf\"", "csrfHeaders(", "th:action=\"@{/logout}\"");
        assertThat(review).contains("name=\"_csrf\"", "name=\"_csrf_header\"");
        assertThat(reviewJs).contains("csrfHeaders(", "headers: csrfHeaders(");
    }

    @Test
    void h2AndPostgresqlSchemasSharePersonalUserContract() throws Exception {
        String h2 = Files.readString(Path.of("src/main/resources/schema.sql"));
        String postgres = Files.readString(Path.of(
                "src/main/resources/db/migration/V8__personal_user_session_authentication.sql"));

        for (String field : new String[]{"id", "username", "password_hash", "created_at", "last_login_at"}) {
            assertThat(h2).contains(field);
            assertThat(postgres).contains(field);
        }
        assertThat(h2).contains("CREATE TABLE IF NOT EXISTS tm_user", "UNIQUE (username)");
        assertThat(postgres).contains("CREATE TABLE IF NOT EXISTS tm_user", "UNIQUE (username)",
                "TIMESTAMP WITHOUT TIME ZONE");
    }
}
