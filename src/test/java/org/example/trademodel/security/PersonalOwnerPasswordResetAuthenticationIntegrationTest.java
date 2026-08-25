package org.example.trademodel.security;

import java.sql.Connection;

import javax.sql.DataSource;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:owner-reset-auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=Base8!Aa",
        "trade-model.security.rate-limit.requests-per-minute=1000"
})
@DirtiesContext
class PersonalOwnerPasswordResetAuthenticationIntegrationTest {
    private static final String RESET_PASSWORD = "Test8!Aa";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PersonalUserMapper userMapper;

    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;

    @Test
    void committedResetAuthenticatesThroughTheRuntimeAuthenticationManager() throws Exception {
        PersonalUserDO before = userMapper.findById(1L);

        try (Connection connection = dataSource.getConnection()) {
            PersonalOwnerPasswordResetTool.resetExistingSingleOwner(
                    connection,
                    "xuchao",
                    RESET_PASSWORD.toCharArray(),
                    RESET_PASSWORD.toCharArray(),
                    passwordEncoder);
        }

        PersonalUserDO committed = userMapper.findById(1L);
        assertThat(committed.getId()).isEqualTo(before.getId());
        assertThat(committed.getUsername()).isEqualTo(before.getUsername());
        assertThat(committed.getRole()).isEqualTo("OWNER");
        assertThat(committed.getEnabled()).isTrue();
        assertThat(committed.getSessionVersion()).isEqualTo(before.getSessionVersion() + 1);

        Authentication authentication = authenticationConfiguration.getAuthenticationManager().authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("xuchao", RESET_PASSWORD));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("xuchao");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_OWNER");
    }
}
