package org.example.trademodel.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.example.trademodel.service.PlanReadinessService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.PlanReadinessReasonVO;
import org.example.trademodel.vo.PlanReadinessSourceFieldVO;
import org.example.trademodel.vo.PlanReadinessVO;
import org.springframework.stereotype.Service;

/**
 * Read-model derivation for plan reference context only. Not a trading signal.
 */
@Service
public class PlanReadinessServiceImpl implements PlanReadinessService {

    private static final String RULE_VERSION = "planReadiness.rules.v1";

    private static final String STATUS_READY = "READY_FOR_REVIEW";
    private static final String STATUS_WATCH = "WATCH_ONLY";
    private static final String STATUS_WAIT = "WAIT_FOR_CONFIRMATION";
    private static final String STATUS_BLOCKED = "BLOCKED";

    /** UI weight: higher = closer to review-ready (not trading strength). */
    private static final int LEVEL_BLOCKED = 1;
    private static final int LEVEL_WATCH = 2;
    private static final int LEVEL_WAIT = 3;
    private static final int LEVEL_READY = 4;

    private static final int DATA_QUALITY_THRESHOLD = 70;
    /**
     * Readiness action threshold only: used to downgrade/wait confirmation.
     * It does not redefine backend aiConflict/confused truth thresholds.
     */
    private static final int readinessConflictActionThreshold = 60;
    /**
     * Readiness action floor only: used to downgrade/wait confirmation.
     * It does not redefine backend aiConflict/confused truth thresholds.
     */
    private static final int readinessConflictLevelActionFloor = 3;

    private static final Pattern CONFLICT_LEVEL_PATTERN = Pattern.compile("(?i)L(\\d+)");

    @Override
    public PlanReadinessVO derive(DecisionResultVO decision) {
        if (decision == null) {
            return null;
        }

        PlanReadinessVO vo = new PlanReadinessVO();
        vo.setRuleVersion(RULE_VERSION);
        vo.setSourceFields(buildSourceFields(decision));
        vo.ensureCollectionsNonNull();

        Integer dqs = decision.getDataQualityScore();
        if (dqs != null && dqs < DATA_QUALITY_THRESHOLD) {
            return fill(vo, STATUS_BLOCKED, "暂不具备计划参考条件", LEVEL_BLOCKED,
                    "数据质量不足",
                    List.of(reason("DATA_QUALITY_LOW", "数据质量不足")));
        }

        Integer confused = decision.getConfusedScore();
        if (confused != null && confused > 0) {
            return fill(vo, STATUS_WAIT, "等待确认", LEVEL_WAIT,
                    "存在困惑状态，需等待确认",
                    List.of(reason("CONFUSED", "存在困惑状态")));
        }

        if (isHighConflict(decision)) {
            return fill(vo, STATUS_WAIT, "等待确认", LEVEL_WAIT,
                    "AI 冲突较高，需等待确认",
                    List.of(reason("AI_CONFLICT_HIGH", "AI 冲突较高")));
        }

        if (!"FULL".equals(decision.getReadModelTruthStatus())) {
            return fill(vo, STATUS_WATCH, "仅观察", LEVEL_WATCH,
                    "读模型未完整，仅适合观察",
                    List.of(reason("READ_MODEL_PARTIAL", "读模型未完整")));
        }

        if (Boolean.FALSE.equals(decision.getIsWorthOpening())) {
            return fill(vo, STATUS_WATCH, "仅观察", LEVEL_WATCH,
                    "当前不满足人工关注开仓条件",
                    List.of(reason("NOT_WORTH_OPENING_CONTEXT", "不满足人工关注开仓条件")));
        }

        if (!planCoreFieldsComplete(decision)) {
            return fill(vo, STATUS_WATCH, "仅观察", LEVEL_WATCH,
                    "计划字段不完整，仅适合观察",
                    List.of(reason("PLAN_FIELDS_INCOMPLETE", "计划核心字段不完整")));
        }

        if (Boolean.TRUE.equals(decision.getIsWorthOpening())) {
            return fill(vo, STATUS_READY, "可进入人工复核", LEVEL_READY,
                    "计划字段较完整，可进入人工复核",
                    new ArrayList<>());
        }

        return fill(vo, STATUS_WATCH, "仅观察", LEVEL_WATCH,
                "当前仅适合观察",
                List.of(reason("WATCH_ONLY_DEFAULT", "默认仅观察")));
    }

    private static PlanReadinessVO fill(PlanReadinessVO vo,
                                        String status,
                                        String text,
                                        int level,
                                        String primary,
                                        List<PlanReadinessReasonVO> blocking) {
        vo.setReadinessStatus(status);
        vo.setReadinessText(text);
        vo.setReadinessLevel(level);
        vo.setPrimaryReason(primary);
        vo.setBlockingReasons(blocking != null ? blocking : new ArrayList<>());
        vo.ensureCollectionsNonNull();
        return vo;
    }

    private static PlanReadinessReasonVO reason(String code, String message) {
        PlanReadinessReasonVO r = new PlanReadinessReasonVO();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    private static boolean isHighConflict(DecisionResultVO decision) {
        Integer score = decision.getAiConflictScore();
        if (score != null && score >= readinessConflictActionThreshold) {
            return true;
        }
        String level = decision.getAiConflictLevel();
        if (level == null || level.isBlank()) {
            return false;
        }
        Matcher m = CONFLICT_LEVEL_PATTERN.matcher(level.trim());
        if (m.find()) {
            try {
                int n = Integer.parseInt(m.group(1));
                return n >= readinessConflictLevelActionFloor;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean planCoreFieldsComplete(DecisionResultVO d) {
        return !isBlank(d.getEntryZone())
                && !isBlank(d.getStopLoss())
                && !isBlank(d.getTakeProfitRules())
                && !isBlank(d.getValidPeriod())
                && !isBlank(d.getInvalidCondition());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static List<PlanReadinessSourceFieldVO> buildSourceFields(DecisionResultVO d) {
        List<PlanReadinessSourceFieldVO> list = new ArrayList<>();
        add(list, "isWorthOpening", d.getIsWorthOpening());
        add(list, "recommendedAction", d.getRecommendedAction());
        add(list, "dataQualityScore", d.getDataQualityScore());
        add(list, "aiConflictLevel", d.getAiConflictLevel());
        add(list, "aiConflictScore", d.getAiConflictScore());
        add(list, "confusedScore", d.getConfusedScore());
        add(list, "readModelTruthStatus", d.getReadModelTruthStatus());
        add(list, "validPeriod", d.getValidPeriod());
        add(list, "invalidCondition", d.getInvalidCondition());
        add(list, "entryZone", d.getEntryZone());
        add(list, "stopLoss", d.getStopLoss());
        add(list, "takeProfitRules", d.getTakeProfitRules());
        return list;
    }

    private static void add(List<PlanReadinessSourceFieldVO> list, String name, Object value) {
        PlanReadinessSourceFieldVO f = new PlanReadinessSourceFieldVO();
        f.setFieldName(name);
        f.setValueSummary(value == null ? "null" : String.valueOf(value));
        list.add(f);
    }
}
