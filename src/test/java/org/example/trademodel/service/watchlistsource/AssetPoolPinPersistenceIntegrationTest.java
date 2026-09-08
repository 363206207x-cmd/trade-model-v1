package org.example.trademodel.service.watchlistsource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("core-regression")
@AutoConfigureMockMvc
@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:home_pin_contract;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false", "spring.sql.init.mode=always", "trade-model.auth.enabled=true"
})
class AssetPoolPinPersistenceIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(87000);
    private static final String SEQUENCE_PATH = "/api/asset-pool/home-pins/sequence";
    private static final String LOCAL_FIXTURE_PASSWORD = "pin-sequence-local-test-only";
    @Autowired AssetPoolService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwordEncoder;
    @MockBean AnalysisRunOrchestrator analysisRunOrchestrator;
    @MockBean MarketAssetCatalog marketAssetCatalog;
    private long owner;
    private long other;
    private List<String> members;

    @BeforeEach
    void createIsolatedLocalTestUsersAndPoolOnly() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:home_pin_contract");
        }
        owner = IDS.incrementAndGet();
        other = IDS.incrementAndGet();
        for (long id : new long[]{owner, other}) {
            jdbc.update("INSERT INTO tm_user(id,username,password_hash,created_at) VALUES(?,?,?,CURRENT_TIMESTAMP)",
                    id, "pin-contract-" + id, passwordEncoder.encode(LOCAL_FIXTURE_PASSWORD));
        }
        for (int i = 0; i < 2; i++) {
            String symbol = "PIN" + owner + i + "USDT";
            jdbc.update("INSERT INTO tm_asset(symbol,asset_name,source,status) VALUES(?,?,?,?)",
                    symbol, symbol, "TEST_ONLY", "ACTIVE");
            jdbc.update("INSERT INTO tm_asset_pool_item(owner_type,owner_id,asset_id,symbol,source_type,sort_order,ext_json) "
                            + "VALUES('USER',?,(SELECT id FROM tm_asset WHERE symbol=?),?,'USER_ADDED',?,?)",
                    owner, symbol, symbol, 100 + i, "{\"keep\":\"audit\"}");
        }
        members = service.listForUser(owner).stream().map(AssetPoolAssetDTO::symbol).toList();
        assertThat(members).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void pinEndpointsRequireCsrfAndUseAuthenticatedOwnerAndIdempotentDesiredState() throws Exception {
        String symbol = members.get(0);
        String path = "/api/asset-pool/" + symbol + "/home-pin";
        mvc.perform(post(path).with(user("pin-contract-" + owner)).contentType("application/json")
                .content("{\"pinned\":true}")).andExpect(status().isForbidden());
        assertThat(pins(owner)).isEmpty();
        mvc.perform(post(path).with(csrf()).contentType("application/json")
                .content("{\"pinned\":true}")).andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(user("pin-contract-" + owner)).with(csrf())
                .contentType("application/json").content("{\"pinned\":true}"))
                .andExpect(status().isOk());
        String saved = preferenceRows();
        mvc.perform(post(path).with(user("pin-contract-" + owner)).with(csrf())
                .contentType("application/json").content("{\"pinned\":true}"))
                .andExpect(status().isOk());
        assertThat(preferenceRows()).isEqualTo(saved);
        assertThat(pins(owner)).containsExactly(symbol);
        assertThat(pins(other)).isEmpty();
        mvc.perform(post("/api/asset-pool/home-pins/sequence").with(user("pin-contract-" + owner))
                .contentType("application/json").content("{\"symbols\":[\"" + symbol + "\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("pin-contract-" + owner)).with(csrf())
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @Test
    void sequenceUsesSessionOwnerLeavesOtherUsersUntouchedAndRepeatedPostsHaveNoSideEffects() throws Exception {
        var selected = members.subList(0, 3);
        selected.forEach(symbol -> service.setHomePin(owner, symbol, true));
        selected.forEach(symbol -> service.setHomePin(other, symbol, true));
        String otherBefore = preferenceRows(other);
        var businessBefore = nonPreferenceTableCounts();
        var desired = List.of(selected.get(2), selected.get(0), selected.get(1));
        String payload = json.writeValueAsString(Map.of("symbols", desired));

        mvc.perform(post(SEQUENCE_PATH).with(user("pin-contract-" + owner)).with(csrf())
                .contentType("application/json").content(payload)).andExpect(status().isOk());
        String saved = preferenceRows();
        for (int i = 0; i < 10; i++) {
            mvc.perform(post(SEQUENCE_PATH).with(user("pin-contract-" + owner)).with(csrf())
                    .contentType("application/json").content(payload)).andExpect(status().isOk());
            assertThat(preferenceRows()).isEqualTo(saved);
        }
        assertThat(pins(owner)).containsExactlyElementsOf(desired);
        assertThat(pins(other)).containsExactlyElementsOf(selected);
        assertThat(preferenceRows(other)).isEqualTo(otherBefore);
        assertThat(nonPreferenceTableCounts()).isEqualTo(businessBefore);
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @Test
    void sequenceRejectsAnonymousMissingCsrfAndForeignMembershipWithoutMutations() throws Exception {
        String ownerOnly = members.get(members.size() - 1);
        service.setHomePin(owner, ownerOnly, true);
        service.setHomePin(other, members.get(0), true);
        String ownerBefore = preferenceRows();
        String otherBefore = preferenceRows(other);
        var businessBefore = nonPreferenceTableCounts();
        String payload = json.writeValueAsString(Map.of("symbols", List.of(ownerOnly)));
        mvc.perform(post(SEQUENCE_PATH).with(csrf()).contentType("application/json").content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(SEQUENCE_PATH).with(user("pin-contract-" + owner))
                .contentType("application/json").content(payload)).andExpect(status().isForbidden());
        mvc.perform(post(SEQUENCE_PATH).with(user("pin-contract-" + other)).with(csrf())
                .contentType("application/json").content(payload)).andExpect(status().isBadRequest());
        assertThat(preferenceRows()).isEqualTo(ownerBefore);
        assertThat(preferenceRows(other)).isEqualTo(otherBefore);
        assertThat(nonPreferenceTableCounts()).isEqualTo(businessBefore);
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @Test
    void sequenceRejectsNonmembersDuplicatesOverSixAndMalformedPayloads() throws Exception {
        service.setHomePin(owner, members.get(0), true);
        String saved = preferenceRows();
        var businessBefore = nonPreferenceTableCounts();
        var invalid = List.of(
                json.writeValueAsString(Map.of("symbols", List.of("NOT_IN_THIS_POOL_USDT"))),
                json.writeValueAsString(Map.of("symbols", List.of(members.get(1)))),
                json.writeValueAsString(Map.of("symbols", List.of(members.get(0), members.get(0)))),
                json.writeValueAsString(Map.of("symbols", members.subList(0, 7))),
                "{}", "{\"symbols\":null}", "{\"symbols\":[null]}",
                "{\"symbols\":[]}", "{\"symbols\":[{}]}", "{\"symbols\":\"BTCUSDT\"}", "[");
        for (String payload : invalid) {
            mvc.perform(post(SEQUENCE_PATH).with(user("pin-contract-" + owner)).with(csrf())
                    .contentType("application/json").content(payload)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
            assertThat(preferenceRows()).isEqualTo(saved);
            assertThat(nonPreferenceTableCounts()).isEqualTo(businessBefore);
        }
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ownerId", "userId", "direction", "quantity", "price", "positionId",
            "leverage", "stopLoss", "takeProfit", "orderId", "exchangeOrderId", "unknownField"})
    void sequenceRejectsIdentityTradingAndUnknownFields(String field) throws Exception {
        service.setHomePin(owner, members.get(0), true);
        String before = preferenceRows();
        String otherBefore = preferenceRows(other);
        var businessBefore = nonPreferenceTableCounts();
        String payload = json.writeValueAsString(Map.of("symbols", List.of(members.get(0)), field, other));
        mvc.perform(post(SEQUENCE_PATH).with(user("pin-contract-" + owner)).with(csrf())
                .contentType("application/json").content(payload)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("invalid request"));
        assertThat(preferenceRows()).isEqualTo(before);
        assertThat(preferenceRows(other)).isEqualTo(otherBefore);
        assertThat(nonPreferenceTableCounts()).isEqualTo(businessBefore);
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @Test
    void unpublishedOldNameIsNotKeptAsACompatibilityRoute() throws Exception {
        String before = preferenceRows();
        var businessBefore = nonPreferenceTableCounts();
        mvc.perform(post("/api/asset-pool/home-pins/order").with(user("pin-contract-" + owner)).with(csrf())
                .contentType("application/json").content("{\"symbols\":[]}"))
                .andExpect(status().isNotFound());
        assertThat(preferenceRows()).isEqualTo(before);
        assertThat(nonPreferenceTableCounts()).isEqualTo(businessBefore);
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @Test
    void sequenceRestoresFromDatabaseAfterRefreshLogoutAndNewAuthenticatedSession() throws Exception {
        var selected = members.subList(0, 3);
        selected.forEach(symbol -> service.setHomePin(owner, symbol, true));
        var desired = List.of(selected.get(2), selected.get(0), selected.get(1));
        var businessBefore = nonPreferenceTableCounts();
        Cookie session = loginSession();
        mvc.perform(post(SEQUENCE_PATH).cookie(session).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("symbols", desired)))).andExpect(status().isOk());
        String saved = preferenceRows();
        assertThat(apiPins(session)).containsExactlyElementsOf(desired);
        assertThat(apiPins(session)).containsExactlyElementsOf(desired);
        mvc.perform(post("/logout").cookie(session).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/asset-pool").cookie(session)).andExpect(status().isUnauthorized());
        Cookie newSession = loginSession();
        assertThat(newSession.getValue()).isNotEqualTo(session.getValue());
        assertThat(apiPins(newSession)).containsExactlyElementsOf(desired);
        assertThat(preferenceRows()).isEqualTo(saved);
        assertThat(nonPreferenceTableCounts()).isEqualTo(businessBefore);
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @Test
    void pinsPersistInUserRowsInSavedOrderWithoutChangingMembershipOrOtherOwner() {
        var desired = List.of(members.get(3), members.get(1), members.get(0));
        desired.forEach(symbol -> service.setHomePin(owner, symbol, true));
        assertThat(pins(owner)).containsExactlyElementsOf(desired);
        var reordered = List.of(desired.get(2), desired.get(0), desired.get(1));
        service.reorderHomePins(owner, reordered);
        assertThat(pins(owner)).containsExactlyElementsOf(reordered);
        assertThat(service.listForUser(owner)).extracting(AssetPoolAssetDTO::symbol)
                .containsExactlyInAnyOrderElementsOf(members);
        assertThat(pins(other)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tm_asset_pool_item "
                + "WHERE owner_type='USER' AND owner_id=? AND ext_json LIKE '%homePinOrder%'", Integer.class, owner))
                .isEqualTo(3);
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    @Test
    void duplicatePinAndReorderAreNoOpsAndUnpinThenPinAppends() {
        var selected = members.subList(0, 3);
        selected.forEach(symbol -> service.setHomePin(owner, symbol, true));
        String before = preferenceRows();
        for (int i = 0; i < 10; i++) service.setHomePin(owner, selected.get(0), true);
        service.reorderHomePins(owner, selected);
        assertThat(preferenceRows()).isEqualTo(before);
        service.setHomePin(owner, selected.get(1), false);
        service.setHomePin(owner, selected.get(1), true);
        assertThat(pins(owner)).containsExactly(selected.get(0), selected.get(2), selected.get(1));
    }

    @Test
    void concurrentSeventhPinCannotExceedSixOrLoseTheSavedFive() throws Exception {
        members.subList(0, 5).forEach(symbol -> service.setHomePin(owner, symbol, true));
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var results = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (String symbol : members.subList(5, 7)) {
                results.add(executor.submit(() -> {
                    start.await();
                    try { service.setHomePin(owner, symbol, true); return true; }
                    catch (IllegalArgumentException full) { return false; }
                }));
            }
            start.countDown();
            int accepted = 0;
            for (var future : results) if (future.get(20, TimeUnit.SECONDS)) accepted++;
            assertThat(accepted).isEqualTo(1);
            assertThat(pins(owner)).hasSize(6).startsWith(members.subList(0, 5).toArray(String[]::new));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void invalidReorderOrForeignMembershipCannotMutatePreferences() {
        service.setHomePin(owner, members.get(0), true);
        String before = preferenceRows();
        assertThatThrownBy(() -> service.reorderHomePins(owner, List.of(members.get(0), members.get(0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.reorderHomePins(owner, List.of(members.get(1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.setHomePin(other, members.get(members.size() - 1), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(preferenceRows()).isEqualTo(before);
        assertThat(pins(other)).isEmpty();
    }

    @Test
    void removalClearsPinButPreservesPoolHistoryAndUnrelatedMetadata() {
        String symbol = members.get(members.size() - 1);
        service.setHomePin(owner, symbol, true);
        service.removeForUser(owner, symbol);
        assertThat(pins(owner)).doesNotContain(symbol);
        assertThat(service.listForUser(owner)).extracting(AssetPoolAssetDTO::symbol).doesNotContain(symbol);
        assertThat(jdbc.queryForObject("SELECT active FROM tm_asset_pool_item "
                + "WHERE owner_type='USER' AND owner_id=? AND symbol=?", Boolean.class, owner, symbol)).isFalse();
        String metadata = jdbc.queryForObject("SELECT ext_json FROM tm_asset_pool_item "
                + "WHERE owner_type='USER' AND owner_id=? AND symbol=?", String.class, owner, symbol);
        assertThat(metadata).contains("\"keep\":\"audit\"").doesNotContain("homePinOrder");
        verifyNoInteractions(analysisRunOrchestrator, marketAssetCatalog);
    }

    private List<String> pins(long user) {
        return service.listForUser(user).stream().filter(AssetPoolAssetDTO::homePinned)
                .sorted(java.util.Comparator.comparing(AssetPoolAssetDTO::homePinOrder))
                .map(AssetPoolAssetDTO::symbol).toList();
    }

    private String preferenceRows() {
        return preferenceRows(owner);
    }

    private String preferenceRows(long userId) {
        return jdbc.queryForList("SELECT * FROM tm_asset_pool_item "
                + "WHERE owner_type='USER' AND owner_id=? ORDER BY symbol", userId).toString();
    }

    private Map<String, Long> nonPreferenceTableCounts() {
        // The asserted in-memory datasource is the only database inspected. Cover all business tables,
        // including any future order table, while excluding only preference rows and session storage.
        var counts = new LinkedHashMap<String, Long>();
        var excluded = Set.of("TM_ASSET_POOL_ITEM", "SPRING_SESSION", "SPRING_SESSION_ATTRIBUTES");
        for (String table : jdbc.queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME", String.class)) {
            assertThat(table).matches("[A-Z0-9_]+");
            if (!excluded.contains(table)) counts.put(table, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class));
        }
        assertThat(counts).containsKeys("TM_USER_POSITION", "TM_REAL_POSITION", "TM_CHANNEL_DELIVERY",
                "TM_AI_CALL_LOG", "TM_ASYNC_TASK", "TM_ANALYSIS_RUN", "TM_HOT_RESET_EVENT");
        return counts;
    }

    private Cookie loginSession() throws Exception {
        var result = mvc.perform(formLogin().user("pin-contract-" + owner).password(LOCAL_FIXTURE_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("pin-contract-" + owner)).andReturn();
        Cookie session = result.getResponse().getCookie("JSESSIONID");
        assertThat(session).isNotNull();
        return session;
    }

    private List<String> apiPins(Cookie session) throws Exception {
        var result = mvc.perform(get("/api/asset-pool").cookie(session)).andExpect(status().isOk()).andReturn();
        var assets = json.treeToValue(json.readTree(result.getResponse().getContentAsString()).path("data"),
                AssetPoolAssetDTO[].class);
        return java.util.Arrays.stream(assets).filter(AssetPoolAssetDTO::homePinned)
                .sorted(java.util.Comparator.comparing(AssetPoolAssetDTO::homePinOrder))
                .map(AssetPoolAssetDTO::symbol).toList();
    }
}
