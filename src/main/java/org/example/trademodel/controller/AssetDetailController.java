package org.example.trademodel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
public class AssetDetailController {
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[A-Z0-9]{2,32}");

    @GetMapping("/dashboard/asset-detail")
    public ModelAndView assetDetail(
            @RequestParam(value = "selectedSymbol", required = false) String selectedSymbol,
            @RequestParam(value = "view", required = false) String view) {
        ModelAndView result = new ModelAndView("asset-detail");
        result.addObject("mobileView", "mobile".equalsIgnoreCase(view));
        String normalizedSymbol = normalizeSymbol(selectedSymbol);
        if (normalizedSymbol == null) {
            result.setStatus(BAD_REQUEST);
            result.addObject("selectedSymbol", "");
            return result;
        }
        result.addObject("selectedSymbol", normalizedSymbol);
        return result;
    }

    static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT).replace("/", "");
        if (!SYMBOL_PATTERN.matcher(normalized).matches() || "DEFAULT_SLOT".equals(normalized)) {
            return null;
        }
        return normalized;
    }
}
