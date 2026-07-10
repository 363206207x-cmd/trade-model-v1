package org.example.trademodel.controller;

import jakarta.validation.Valid;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.providercall.profile.ScanProfileResponse;
import org.example.trademodel.providercall.profile.ScanProfileService;
import org.example.trademodel.providercall.profile.ScanProfileUpdateRequest;
import org.example.trademodel.providercall.profile.RuntimeScanProfileResponse;
import org.example.trademodel.providercall.profile.RuntimeScanProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/scan-profile")
public class ScanProfileController {
    private final ScanProfileService service;
    private final RuntimeScanProfileService runtimeService;

    public ScanProfileController(ScanProfileService service, RuntimeScanProfileService runtimeService) {
        this.service = service;
        this.runtimeService = runtimeService;
    }

    @GetMapping
    public ApiResponse<ScanProfileResponse> get(Authentication authentication) {
        return ApiResponse.success(service.get(requireAuthenticated(authentication)));
    }

    @PutMapping
    public ApiResponse<ScanProfileResponse> update(Authentication authentication,
                                                   @Valid @RequestBody ScanProfileUpdateRequest request) {
        return ApiResponse.success(service.update(requireAuthenticated(authentication), request));
    }

    @GetMapping("/runtime")
    public ApiResponse<RuntimeScanProfileResponse> runtime(Authentication authentication,
                                                           @RequestParam String symbol) {
        requireAuthenticated(authentication);
        return ApiResponse.success(runtimeService.get(symbol));
    }

    private static String requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("authenticated operator is required");
        }
        return authentication.getName();
    }
}
