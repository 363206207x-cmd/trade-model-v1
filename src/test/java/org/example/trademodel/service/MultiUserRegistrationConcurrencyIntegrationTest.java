package org.example.trademodel.service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:multi-user-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "trade-model.auth.enabled=true",
        "trade-model.auth.initial-username=xuchao",
        "trade-model.auth.initial-password=owner-concurrency-secret"
})
@DirtiesContext
class MultiUserRegistrationConcurrencyIntegrationTest {
    @Autowired
    private MultiUserAccountService accountService;

    @Autowired
    private PersonalUserMapper userMapper;

    @Test
    void databaseGuardAllowsExactlyOneConcurrentRegistrationIntoLastSlot() throws Exception {
        for (int index = 1; index <= 8; index++) {
            accountService.register("existing_" + index, "12345678");
        }
        assertThat(userMapper.countEnabled()).isEqualTo(9);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = () -> register("contender_a");
            Callable<Boolean> second = () -> register("contender_b");
            List<Boolean> outcomes = executor.invokeAll(List.of(first, second)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();

            assertThat(outcomes).containsExactlyInAnyOrder(true, false);
            assertThat(userMapper.countEnabled()).isEqualTo(10);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean register(String username) {
        try {
            accountService.register(username, "12345678");
            return true;
        } catch (IllegalStateException expectedAtCapacity) {
            return false;
        }
    }
}
