package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.service.UserPositionService;
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

    public UserPositionController(UserPositionService userPositionService) {
        this.userPositionService = userPositionService;
    }

    @PostMapping("/manual-open")
    public ResponseEntity<ApiResponse<UserPositionVO>> manualOpen(@RequestBody CreateUserPositionReq request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(userPositionService.manualOpen(request)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @PostMapping("/{id}/manual-close")
    public ResponseEntity<ApiResponse<UserPositionVO>> manualClose(@PathVariable Long id,
                                                                   @RequestBody CloseUserPositionReq request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(userPositionService.manualClose(id, request)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }

    @GetMapping("/open")
    public ApiResponse<List<UserPositionVO>> openPositions() {
        return ApiResponse.success(userPositionService.listOpenPositions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserPositionVO>> getById(@PathVariable Long id) {
        try {
            UserPositionVO position = userPositionService.findById(id);
            if (position == null) {
                return ResponseEntity.status(404).body(ApiResponse.notFound("UserPosition not found: " + id));
            }
            return ResponseEntity.ok(ApiResponse.success(position));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(ex.getMessage()));
        }
    }
}
