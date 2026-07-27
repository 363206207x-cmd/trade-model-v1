package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user-positions")
public class UserPositionController {
    private final UserPositionService userPositionService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public UserPositionController(UserPositionService userPositionService,
                                  AuthenticatedUserIdResolver authenticatedUserIdResolver) {
        this.userPositionService = userPositionService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @PostMapping("/manual-open")
    public ResponseEntity<ApiResponse<UserPositionVO>> manualOpen(@RequestBody CreateUserPositionReq request) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userPositionService.manualOpenForUser(userId, request)));
    }

    @PostMapping("/{id}/manual-close")
    public ResponseEntity<ApiResponse<UserPositionVO>> manualClose(@PathVariable Long id,
                                                                   @RequestBody CloseUserPositionReq request) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userPositionService.manualCloseForUser(id, userId, request)));
    }

    @GetMapping("/open")
    public ApiResponse<List<UserPositionVO>> openPositions() {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ApiResponse.success(userPositionService.listOpenPositionsForUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserPositionVO>> getById(@PathVariable Long id) {
        Long userId = authenticatedUserIdResolver.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userPositionService.findByIdForUser(id, userId)));
    }
}
