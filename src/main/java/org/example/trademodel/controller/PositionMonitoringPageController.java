package org.example.trademodel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Controller
public class PositionMonitoringPageController {
    private static final Pattern POSITION_ID_PATTERN = Pattern.compile("[1-9][0-9]{0,18}");

    @GetMapping("/dashboard/positions")
    public ModelAndView desktopPositionMonitoring(
            @RequestParam(value = "positionId", required = false) String positionId) {
        return positionMonitoring(positionId, false);
    }

    @GetMapping("/dashboard/mobile/positions")
    public ModelAndView mobilePositionMonitoring(
            @RequestParam(value = "positionId", required = false) String positionId) {
        return positionMonitoring(positionId, true);
    }

    private ModelAndView positionMonitoring(String positionId, boolean mobileView) {
        ModelAndView result = new ModelAndView("position-monitoring");
        String normalizedPositionId = normalizePositionId(positionId);
        boolean invalidPositionId = hasText(positionId) && normalizedPositionId == null;
        if (invalidPositionId) {
            result.setStatus(BAD_REQUEST);
        }
        result.addObject("requestedPositionId", normalizedPositionId == null ? "" : normalizedPositionId);
        result.addObject("invalidPositionId", invalidPositionId);
        result.addObject("mobileView", mobileView);
        return result;
    }

    static String normalizePositionId(String positionId) {
        if (!hasText(positionId)) {
            return null;
        }
        String normalized = positionId.trim();
        if (!POSITION_ID_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        try {
            return Long.parseLong(normalized) > 0 ? normalized : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
