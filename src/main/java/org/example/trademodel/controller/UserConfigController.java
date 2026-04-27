package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-config")
public class UserConfigController {

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("user config controller ok");
    }
}
