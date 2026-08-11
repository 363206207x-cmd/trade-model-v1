package org.example.trademodel.controller;

import org.example.trademodel.common.GlobalExceptionHandler;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.UserPositionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class UserPositionControllerTest {
    @Mock
    private UserPositionService userPositionService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserPositionController(userPositionService, authenticatedUserIdResolver))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void manualOpenEndpointReturnsUserPositionSafetyFields() throws Exception {
        when(userPositionService.manualOpenForUser(eq(7L), any())).thenReturn(vo(11L, "OPEN"));

        mockMvc.perform(post("/api/user-positions/manual-open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "asset_symbol": "BTCUSDT",
                                  "side": "LONG",
                                  "entry_price": 100.50,
                                  "quantity": 0.25,
                                  "leverage": 2,
                                  "source_type": "MANUAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("11"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.sourceType").value("MANUAL_POSITION"))
                .andExpect(jsonPath("$.data.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true))
                .andExpect(jsonPath("$.data.notPositionSync").value(true))
                .andExpect(jsonPath("$.data.orderAction").doesNotExist())
                .andExpect(jsonPath("$.data.executionAction").doesNotExist())
                .andExpect(jsonPath("$.data.autoTradingAction").doesNotExist());
    }

    @Test
    void manualCloseEndpointReturnsClosedPositionWithSafetyFields() throws Exception {
        when(userPositionService.manualCloseForUser(eq(11L), eq(7L), any())).thenReturn(vo(11L, "CLOSED"));

        mockMvc.perform(post("/api/user-positions/11/manual-close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "close_price": 105.25,
                                  "close_reason": "manual exit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true))
                .andExpect(jsonPath("$.data.notPositionSync").value(true));
    }

    @Test
    void openPositionsEndpointReturnsOnlyServiceOpenRows() throws Exception {
        when(userPositionService.listOpenPositionsForUser(7L)).thenReturn(List.of(
                vo(1L, "OPEN"),
                vo(2L, "PARTIALLY_CLOSED")
        ));

        mockMvc.perform(get("/api/user-positions/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[1].status").value("PARTIALLY_CLOSED"))
                .andExpect(jsonPath("$.data[0].notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data[1].notAutoTrading").value(true));
    }

    @Test
    void getByIdReturnsNotFoundWhenPositionIsMissing() throws Exception {
        when(userPositionService.findByIdForUser(404L, 7L)).thenThrow(new UserPositionNotFoundException());

        mockMvc.perform(get("/api/user-positions/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getByIdSerializesLargePositionIdentityAsString() throws Exception {
        long positionId = 9_007_199_254_740_993L;
        when(userPositionService.findByIdForUser(positionId, 7L))
                .thenReturn(vo(positionId, "OPEN"));

        mockMvc.perform(get("/api/user-positions/{id}", positionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("9007199254740993"));
    }

    @Test
    void controllerReturnsBadRequestOnFailClosedValidation() throws Exception {
        when(userPositionService.manualOpenForUser(eq(7L), any()))
                .thenThrow(new IllegalArgumentException(
                        "source_type must be MANUAL_POSITION or SYSTEM_PLAN_POSITION"));

        mockMvc.perform(post("/api/user-positions/manual-open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "asset_symbol": "BTCUSDT",
                                  "side": "LONG",
                                  "entry_price": 100.50,
                                  "quantity": 0.25,
                                  "leverage": 2,
                                  "source_type": "PLAN_AUTO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("invalid request"));
    }

    private static UserPositionVO vo(Long id, String status) {
        UserPositionVO vo = new UserPositionVO();
        vo.setId(id);
        vo.setAssetSymbol("BTCUSDT");
        vo.setSide("LONG");
        vo.setStatus(status);
        vo.setEntryPrice(new BigDecimal("100.50"));
        vo.setQuantity(new BigDecimal("0.25"));
        vo.setLeverage(new BigDecimal("2"));
        vo.setOpenedAt(LocalDateTime.of(2026, 6, 22, 8, 30));
        vo.setSourceType("MANUAL_POSITION");
        vo.setManualReviewRequired(true);
        vo.setNotTradeInstruction(true);
        vo.setNotAutoTrading(true);
        vo.setNotOrderExecution(true);
        vo.setNotPositionSync(true);
        vo.setCreatedAt(LocalDateTime.of(2026, 6, 22, 8, 30));
        vo.setUpdatedAt(LocalDateTime.of(2026, 6, 22, 8, 30));
        if ("CLOSED".equals(status)) {
            vo.setClosedAt(LocalDateTime.of(2026, 6, 22, 9, 0));
            vo.setClosePrice(new BigDecimal("105.25"));
            vo.setCloseReason("manual exit");
        }
        return vo;
    }
}
