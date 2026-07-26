package org.example.trademodel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
public class AnalysisDetailController {
    private static final Pattern ANALYSIS_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    @GetMapping("/dashboard/analysis-detail")
    public ModelAndView analysisDetail(
            @RequestParam(value = "analysisId", required = false) String analysisId,
            @RequestParam(value = "selectedSymbol", required = false) String selectedSymbol,
            @RequestParam(value = "view", required = false) String view) {
        ModelAndView result = new ModelAndView("analysis-detail");
        result.addObject("mobileView", "mobile".equalsIgnoreCase(view));

        String normalizedAnalysisId = normalizeAnalysisId(analysisId);
        String normalizedSymbol = normalizeOptionalSymbol(selectedSymbol);
        if (normalizedAnalysisId == null || (hasText(selectedSymbol) && normalizedSymbol == null)) {
            result.setStatus(BAD_REQUEST);
            result.addObject("analysisId", "");
            result.addObject("selectedSymbol", "");
            return result;
        }

        result.addObject("analysisId", normalizedAnalysisId);
        result.addObject("selectedSymbol", normalizedSymbol == null ? "" : normalizedSymbol);
        return result;
    }

    static String normalizeAnalysisId(String analysisId) {
        if (!hasText(analysisId)) {
            return null;
        }
        String normalized = analysisId.trim();
        return ANALYSIS_ID_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private static String normalizeOptionalSymbol(String symbol) {
        return hasText(symbol) ? AssetDetailController.normalizeSymbol(symbol) : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
