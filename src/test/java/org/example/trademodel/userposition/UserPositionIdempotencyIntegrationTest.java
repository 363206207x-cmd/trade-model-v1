package org.example.trademodel.userposition;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.entity.PersonalUserDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PersonalUserMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.vo.UserPositionVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Tag("core-regression")
class UserPositionIdempotencyIntegrationTest {

    @Autowired
    private UserPositionService service;
    @Autowired
    private UserPositionMapper userPositionMapper;
    @Autowired
    private PersonalUserMapper personalUserMapper;

    @Test
    void tenConcurrentOpenRetriesProduceExactlyOneOwnerPosition() throws Exception {
        Long userId = userId("idempotent-open-owner");
        CreateUserPositionReq request = openRequest("position-open:ten-concurrent");
        int requestCount = 10;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Future<UserPositionVO>> futures = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.manualOpenForUser(userId, request);
                }));
            }
            ready.await();
            start.countDown();
            Set<Long> ids = futures.stream().map(this::get).map(UserPositionVO::getId).collect(Collectors.toSet());

            assertThat(ids).hasSize(1);
            assertThat(userPositionMapper.listOpenByUserId(userId)).hasSize(1);
            UserPositionDO canonical = userPositionMapper.selectBySubmissionIdAndUserId(request.getSubmissionId(), userId);
            assertThat(canonical).isNotNull();
            assertThat(canonical.getId()).isEqualTo(ids.iterator().next());
            assertThat(canonical.getNotAutoTrading()).isTrue();
            assertThat(canonical.getNotOrderExecution()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sameSubmissionIdentityIsIsolatedByOwner() {
        Long firstOwner = userId("idempotent-isolation-a");
        Long secondOwner = userId("idempotent-isolation-b");
        CreateUserPositionReq request = openRequest("position-open:shared-owner-key");

        UserPositionVO first = service.manualOpenForUser(firstOwner, request);
        UserPositionVO second = service.manualOpenForUser(secondOwner, request);

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(userPositionMapper.listOpenByUserId(firstOwner)).extracting(UserPositionDO::getId).contains(first.getId());
        assertThat(userPositionMapper.listOpenByUserId(secondOwner)).extracting(UserPositionDO::getId).contains(second.getId());
    }

    @Test
    void twoConcurrentCloseRetriesProduceOneClosedStateAndNoOpenPosition() throws Exception {
        Long userId = userId("idempotent-close-owner");
        UserPositionVO opened = service.manualOpenForUser(userId, openRequest("position-open:for-close"));
        CloseUserPositionReq close = closeRequest("position-close:two-concurrent");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<UserPositionVO> first = executor.submit(() -> closeAfterReady(opened.getId(), userId, close, ready, start));
            Future<UserPositionVO> second = executor.submit(() -> closeAfterReady(opened.getId(), userId, close, ready, start));
            ready.await();
            start.countDown();

            assertThat(get(first).getStatus()).isEqualTo("CLOSED");
            assertThat(get(second).getStatus()).isEqualTo("CLOSED");
            assertThat(userPositionMapper.listOpenByUserId(userId)).isEmpty();
            UserPositionDO canonical = userPositionMapper.selectByIdAndUserId(opened.getId(), userId);
            assertThat(canonical.getCloseSubmissionId()).isEqualTo(close.getSubmissionId());
            assertThat(canonical.getNotAutoTrading()).isTrue();
            assertThat(canonical.getNotOrderExecution()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    private UserPositionVO closeAfterReady(Long positionId,
                                           Long userId,
                                           CloseUserPositionReq request,
                                           CountDownLatch ready,
                                           CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return service.manualCloseForUser(positionId, userId, request);
    }

    private <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private Long userId(String username) {
        PersonalUserDO user = new PersonalUserDO();
        user.setUsername(username);
        user.setPasswordHash("{noop}not-a-real-password");
        user.setCreatedAt(LocalDateTime.now());
        personalUserMapper.insert(user);
        return user.getId();
    }

    private static CreateUserPositionReq openRequest(String submissionId) {
        CreateUserPositionReq request = new CreateUserPositionReq();
        request.setSubmissionId(submissionId);
        request.setAssetSymbol("ETHUSDT");
        request.setSide("SHORT");
        request.setEntryPrice(new BigDecimal("2418.00"));
        request.setQuantity(new BigDecimal("0.25"));
        request.setLeverage(new BigDecimal("2"));
        request.setStopLoss(new BigDecimal("2460.00"));
        request.setTakeProfit(new BigDecimal("2320.00"));
        request.setOpenedAt(LocalDateTime.of(2026, 9, 3, 12, 0));
        request.setSourceType("MANUAL_INDEPENDENT");
        return request;
    }

    private static CloseUserPositionReq closeRequest(String submissionId) {
        CloseUserPositionReq request = new CloseUserPositionReq();
        request.setSubmissionId(submissionId);
        request.setClosePrice(new BigDecimal("2400.00"));
        request.setClosedAt(LocalDateTime.of(2026, 9, 3, 13, 0));
        request.setCloseReason("owner recorded close");
        return request;
    }
}
