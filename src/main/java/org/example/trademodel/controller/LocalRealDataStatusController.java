package org.example.trademodel.controller;

import org.example.trademodel.localreal.LocalRealDataStatusService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("local-real")
@RequestMapping("/api/local-real")
public class LocalRealDataStatusController {
    private final LocalRealDataStatusService statusService;

    public LocalRealDataStatusController(LocalRealDataStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return statusService.status();
    }
}
