package org.example.trademodel.service;

import org.example.trademodel.TradeModelApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:multi-user-registration-disabled;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=owner-disabled-registration-secret",
        "registration.enabled=false"
})
@Transactional
class MultiUserRegistrationDisabledIntegrationTest {
    @Autowired
    private MultiUserAccountService accountService;

    @Test
    void disabledRegistrationFailsClosedWithoutChangingTheOwner() {
        assertThat(accountService.registrationAvailability().open()).isFalse();
        assertThat(accountService.registrationAvailability().enabledAccounts()).isEqualTo(1);
        assertThatThrownBy(() -> accountService.register("blocked_user", "12345678"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
        assertThat(accountService.listAccounts())
                .singleElement()
                .satisfies(owner -> {
                    assertThat(owner.id()).isEqualTo(1L);
                    assertThat(owner.username()).isEqualTo("xuchao");
                    assertThat(owner.role()).isEqualTo("OWNER");
                });
    }
}
