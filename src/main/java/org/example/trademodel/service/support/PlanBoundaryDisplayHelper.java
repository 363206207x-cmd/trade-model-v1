package org.example.trademodel.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * Read-only parsing of {@code plan_boundary_json} into display fields. Does not compare prices or influence decisions.
 */
public final class PlanBoundaryDisplayHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String WARNING_TEXT = "未启用价格比较；不自动止损；不自动平仓。";

    private PlanBoundaryDisplayHelper() {}

    /**
     * Parses boundary JSON for UI read models. Fail-open: never throws for malformed input.
     */
    public static PlanBoundaryDisplayInfo parse(String planBoundaryJson, PlanBoundaryDisplayContext context) {
        PlanBoundaryDisplayContext ctx = context != null ? context : PlanBoundaryDisplayContext.GENERIC;
        if (planBoundaryJson == null || planBoundaryJson.trim().isEmpty()) {
            return missing(ctx);
        }
        try {
            JsonNode root = MAPPER.readTree(planBoundaryJson);
            if (root == null || !root.isObject()) {
                return invalidStructural(ctx, null);
            }
            JsonNode statusNode = root.get("boundaryParseStatus");
            if (statusNode == null || statusNode.isNull() || !statusNode.isTextual()) {
                return invalidStructural(ctx, root);
            }
            String rawStatus = statusNode.asText();
            if (rawStatus == null || rawStatus.trim().isEmpty()) {
                return invalidStructural(ctx, root);
            }
            String status = rawStatus.trim();
            return switch (status) {
                case "UNSTRUCTURED_TEXT_ONLY" -> unstructured(root, ctx);
                case "PARTIAL" -> partial(root, ctx);
                case "INVALID" -> invalidDeclared(root, ctx);
                case "STRUCTURED" -> structured(root, ctx);
                default -> invalidStructural(ctx, root);
            };
        } catch (Exception e) {
            return invalidStructural(ctx, null);
        }
    }

    private static PlanBoundaryDisplayInfo missing(PlanBoundaryDisplayContext ctx) {
        return new PlanBoundaryDisplayInfo(
                "MISSING",
                null,
                null,
                "未返回",
                missingDisplayText(ctx),
                WARNING_TEXT,
                null,
                null);
    }

    private static String missingDisplayText(PlanBoundaryDisplayContext ctx) {
        return switch (ctx) {
            case POSITION_MONITOR -> "计划价位边界未返回，当前不参与数值监护。";
            case TRADE_REVIEW -> "计划价位边界未返回，复盘中无结构化边界快照。";
            case GENERIC -> "计划价位边界未返回，当前不参与数值监护。";
        };
    }

    private static PlanBoundaryDisplayInfo unstructured(JsonNode root, PlanBoundaryDisplayContext ctx) {
        return new PlanBoundaryDisplayInfo(
                "UNSTRUCTURED_TEXT_ONLY",
                textOrNull(root, "boundarySource"),
                textOrNull(root, "boundaryConfidence"),
                "文本参考",
                unstructuredDisplayText(ctx),
                WARNING_TEXT,
                null,
                null);
    }

    private static String unstructuredDisplayText(PlanBoundaryDisplayContext ctx) {
        return switch (ctx) {
            case POSITION_MONITOR -> "计划价位边界当前仅为文本参考，尚未参与数值监护。";
            case TRADE_REVIEW -> "计划价位边界当前仅为文本参考，复盘时不作为数值触发依据。";
            case GENERIC -> "计划价位边界当前仅为文本参考，尚未参与数值监护。";
        };
    }

    private static PlanBoundaryDisplayInfo partial(JsonNode root, PlanBoundaryDisplayContext ctx) {
        String dir = readInvalidPriceDirection(root.get("invalidPriceDirection"));
        BigDecimal thr = readInvalidPriceThreshold(root.get("invalidPriceThreshold"));
        return new PlanBoundaryDisplayInfo(
                "PARTIAL",
                textOrNull(root, "boundarySource"),
                textOrNull(root, "boundaryConfidence"),
                "部分结构化",
                partialDisplayText(ctx),
                WARNING_TEXT,
                dir,
                thr);
    }

    private static String partialDisplayText(PlanBoundaryDisplayContext ctx) {
        return switch (ctx) {
            case POSITION_MONITOR -> "计划价位边界部分结构化，本阶段仅记录状态，尚未启用价格比较。";
            case TRADE_REVIEW -> "计划价位边界部分结构化，复盘中仅作为结构失效参考，不代表已启用价格比较。";
            case GENERIC -> "计划价位边界部分结构化，尚未启用价格比较。";
        };
    }

    private static PlanBoundaryDisplayInfo invalidDeclared(JsonNode root, PlanBoundaryDisplayContext ctx) {
        return new PlanBoundaryDisplayInfo(
                "INVALID",
                textOrNull(root, "boundarySource"),
                textOrNull(root, "boundaryConfidence"),
                "结构无效",
                invalidStatusDisplayText(ctx),
                WARNING_TEXT,
                null,
                null);
    }

    private static PlanBoundaryDisplayInfo structured(JsonNode root, PlanBoundaryDisplayContext ctx) {
        String dir = readInvalidPriceDirection(root.get("invalidPriceDirection"));
        BigDecimal thr = readInvalidPriceThreshold(root.get("invalidPriceThreshold"));
        return new PlanBoundaryDisplayInfo(
                "STRUCTURED",
                textOrNull(root, "boundarySource"),
                textOrNull(root, "boundaryConfidence"),
                "已结构化",
                structuredDisplayText(ctx),
                WARNING_TEXT,
                dir,
                thr);
    }

    private static String structuredDisplayText(PlanBoundaryDisplayContext ctx) {
        return switch (ctx) {
            case POSITION_MONITOR -> "计划价位边界已结构化，但本阶段仍未启用价格比较。";
            case TRADE_REVIEW -> "计划价位边界已结构化，复盘中仅作为边界快照，不代表已启用价格比较。";
            case GENERIC -> "计划价位边界已结构化，但未启用价格比较。";
        };
    }

    /**
     * Illegal JSON, non-object root, missing/empty status, or unknown status value.
     */
    private static PlanBoundaryDisplayInfo invalidStructural(PlanBoundaryDisplayContext ctx, JsonNode root) {
        return new PlanBoundaryDisplayInfo(
                "INVALID",
                root != null && root.isObject() ? textOrNull(root, "boundarySource") : null,
                root != null && root.isObject() ? textOrNull(root, "boundaryConfidence") : null,
                "结构无效",
                invalidStatusDisplayText(ctx),
                WARNING_TEXT,
                null,
                null);
    }

    private static String invalidStatusDisplayText(PlanBoundaryDisplayContext ctx) {
        return switch (ctx) {
            case POSITION_MONITOR -> "计划价位边界结构无效，当前仅按文本参考。";
            case TRADE_REVIEW -> "计划价位边界结构无效，复盘时仅按文本记录参考。";
            case GENERIC -> "计划价位边界结构无效，当前仅按文本参考。";
        };
    }

    private static String textOrNull(JsonNode root, String field) {
        if (root == null || !root.isObject()) {
            return null;
        }
        JsonNode n = root.get(field);
        if (n == null || n.isNull() || !n.isTextual()) {
            return null;
        }
        String t = n.asText();
        if (t == null) {
            return null;
        }
        t = t.trim();
        return t.isEmpty() ? null : t;
    }

    private static String readInvalidPriceDirection(JsonNode node) {
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String u = node.asText().trim().toUpperCase();
        if ("ABOVE".equals(u) || "BELOW".equals(u)) {
            return u;
        }
        return null;
    }

    private static BigDecimal readInvalidPriceThreshold(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            if (node.isNumber()) {
                return node.decimalValue();
            }
            if (node.isTextual()) {
                String t = node.asText().trim();
                if (t.isEmpty()) {
                    return null;
                }
                return new BigDecimal(t);
            }
        } catch (Exception ignored) {
            // fail-open
        }
        return null;
    }
}
