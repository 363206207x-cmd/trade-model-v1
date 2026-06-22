package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account-risk")
public class AccountRiskController {
    private final UserPositionRiskAdapter userPositionRiskAdapter;

    public AccountRiskController(UserPositionRiskAdapter userPositionRiskAdapter) {
        this.userPositionRiskAdapter = userPositionRiskAdapter;
    }

    @GetMapping("/user-positions/current")
    public ApiResponse<UserPositionRiskResult> currentUserPositionRisk() {
        return ApiResponse.success(userPositionRiskAdapter.currentRisk());
    }
}
