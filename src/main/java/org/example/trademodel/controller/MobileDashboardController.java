package org.example.trademodel.controller;

import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.vo.DashboardHomeVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class MobileDashboardController {
    static final int MOBILE_HOME_ASSET_LIMIT = 3;

    private final DashboardHomeService dashboardHomeService;

    public MobileDashboardController(DashboardHomeService dashboardHomeService) {
        this.dashboardHomeService = dashboardHomeService;
    }

    @GetMapping("/dashboard/mobile")
    public String mobileDashboard(
            @RequestParam(value = "selectedSymbol", required = false) String selectedSymbol,
            Model model) {
        DashboardHomeVO home = dashboardHomeService.getHome(selectedSymbol, MOBILE_HOME_ASSET_LIMIT, null);
        model.addAttribute("home", home);
        model.addAttribute("mobileAssets", mobileAssets(home, selectedSymbol));
        return "dashboard-mobile";
    }

    static List<DashboardHomeVO.AssetVO> mobileAssets(DashboardHomeVO home, String requestedSymbol) {
        if (home == null || home.getAssets() == null) {
            return List.of();
        }
        List<DashboardHomeVO.AssetVO> available = home.getAssets().stream()
                .filter(MobileDashboardController::isRenderableAsset)
                .toList();
        if (available.isEmpty()) {
            return List.of();
        }

        String normalizedRequest = normalizedSymbol(requestedSymbol);
        String selectedSymbol = normalizedRequest != null
                ? normalizedRequest : normalizedSymbol(home.getSelectedSymbol());
        DashboardHomeVO.AssetVO selected = selectedSymbol == null ? null : available.stream()
                .filter(asset -> selectedSymbol.equals(normalizedSymbol(asset.getRawSymbol())))
                .findFirst()
                .orElse(null);
        if (normalizedRequest != null && selected == null) {
            return List.of();
        }
        if (selected == null) {
            selected = available.get(0);
            home.setSelectedSymbol(normalizedSymbol(selected.getRawSymbol()));
        }

        List<DashboardHomeVO.AssetVO> visible = new ArrayList<>(
                available.subList(0, Math.min(MOBILE_HOME_ASSET_LIMIT, available.size())));
        if (selected != null && !visible.contains(selected)) {
            if (visible.size() >= MOBILE_HOME_ASSET_LIMIT) {
                visible.remove(visible.size() - 1);
            }
            visible.add(selected);
        }
        return List.copyOf(visible);
    }

    private static boolean isRenderableAsset(DashboardHomeVO.AssetVO asset) {
        return asset != null
                && normalizedSymbol(asset.getRawSymbol()) != null
                && !"DEFAULT_SLOT".equalsIgnoreCase(asset.getSlotType());
    }

    private static String normalizedSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT).replace("/", "");
    }
}
