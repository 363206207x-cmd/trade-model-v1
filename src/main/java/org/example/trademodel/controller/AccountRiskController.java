package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account-risk")
public class AccountRiskController {
    private final UserPositionRiskAdapter userPositionRiskAdapter;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public AccountRiskController(UserPositionRiskAdapter userPositionRiskAdapter,
                                 AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.userPositionRiskAdapter = userPositionRiskAdapter;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @GetMapping("/user-positions/current")
    public ApiResponse<UserPositionRiskResult> currentUserPositionRisk() {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ApiResponse.success(userPositionRiskAdapter.currentRiskForUser(userId));
    }
}
