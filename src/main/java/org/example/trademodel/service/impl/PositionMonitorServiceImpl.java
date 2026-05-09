package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PositionMonitorRecordDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PositionMonitorRecordMapper;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.PositionMonitorOpenRowVO;
import org.example.trademodel.vo.RealPositionVO;
import org.example.trademodel.vo.PositionMonitorOpenRowVO.LatestMonitorRecordVO;
import org.example.trademodel.service.support.PlanBoundaryDisplayContext;
import org.example.trademodel.service.support.PlanBoundaryDisplayHelper;
import org.example.trademodel.service.support.PlanBoundaryDisplayInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PositionMonitorServiceImpl implements PositionMonitorService {
    private static final Logger log = LoggerFactory.getLogger(PositionMonitorServiceImpl.class);

    private static final String MANUAL_SOURCE_TYPE = "MANUAL_INPUT";
    private static final String MANUAL_SOURCE_NAME = "USER_MANUAL";

    private static final String ENTRY_VALID = "VALID";
    private static final String ENTRY_WEAKENED = "WEAKENED";
    private static final String ENTRY_INVALIDATED = "INVALIDATED";

    private static final String DIR_SUPPORT_ORIGINAL = "SUPPORT_ORIGINAL";
    private static final String DIR_RANGE = "RANGE";
    private static final String DIR_AGAINST_ORIGINAL = "AGAINST_ORIGINAL";
    private static final String DIR_CONFLICT_EXPANDING = "CONFLICT_EXPANDING";

    private static final String REV_NONE = "NONE";
    private static final String REV_WEAK = "WEAK";
    private static final String REV_STRONG = "STRONG";

    private static final String RISK_LOW = "LOW";
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";
    private static final String RISK_EXTREME = "EXTREME";

    private static final String AI_SUPPORT = "SUPPORT";
    private static final String AI_MIXED = "MIXED";
    private static final String AI_AGAINST = "AGAINST";
    private static final String AI_UNKNOWN = "UNKNOWN";

    private static final String ACT_CONTINUE_HOLD = "CONTINUE_HOLD";
    private static final String ACT_NO_ADD = "NO_ADD";
    private static final String ACT_REDUCE_POSITION = "REDUCE_POSITION";
    private static final String ACT_TIGHTEN_STOP = "TIGHTEN_STOP";
    private static final String ACT_MOVE_STOP = "MOVE_STOP";
    private static final String ACT_PARTIAL_TAKE_PROFIT = "PARTIAL_TAKE_PROFIT";
    private static final String ACT_PLAN_INVALID_WAIT_CONFIRM = "PLAN_INVALID_WAIT_CONFIRM";
    private static final String ACT_CLOSE_AND_ENTER_REVIEW = "CLOSE_AND_ENTER_REVIEW";
    private static final int CONFLICT_LEVEL_3 = 3;
    private static final int CONFLICT_LEVEL_4 = 4;
    /**
     * Monitor 动作阈值：用于风险提示升级/等待确认，不是 confused 真值阈值，
     * 也不是自动平仓或反手触发阈值。
     */
    private static final int monitorRiskElevateThreshold = 70;
    /**
     * Monitor 动作阈值：用于更强阻断提示，不是 confused 真值阈值，
     * 也不是自动平仓或反手触发阈值。
     */
    private static final int monitorHardBlockThreshold = 85;
    /** Monitor 提示阈值：仅用于轻度困惑提示文案。 */
    private static final int monitorConfusedHintThreshold = 40;

    private static final String REVIEW_NOT_ENTERED = "NOT_ENTERED";
    private static final String REVIEW_READY_TO_REVIEW = "READY_TO_REVIEW";
    private static final String REVIEW_ENTERED = "ENTERED";
    private static final long HEARTBEAT_MINUTES = 30L;
    private static final String REVERSAL_HINT_WATCH = "反转观察";
    private static final String REVERSAL_HINT_WARNING = "反转预警";
    private static final String REVERSAL_HINT_STRONG_RISK = "强反转风险";
    private static final String REVERSAL_HINT_PLAN_INVALIDATED = "原计划失效型反转";

    private final RealPositionMapper realPositionMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final PositionMonitorRecordMapper positionMonitorRecordMapper;

    public PositionMonitorServiceImpl(RealPositionMapper realPositionMapper,
                                       DecisionResultMapper decisionResultMapper,
                                       ExecutionPlanMapper executionPlanMapper,
                                       AnalysisRunMapper analysisRunMapper,
                                       PositionMonitorRecordMapper positionMonitorRecordMapper) {
        this.realPositionMapper = realPositionMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.positionMonitorRecordMapper = positionMonitorRecordMapper;
    }

    @Override
    public PositionMonitorOpenRowVO run(String positionId) {
        return evaluateForPosition(positionId, true);
    }

    @Override
    public PositionMonitorOpenRowVO evaluateForPosition(String positionId, boolean forcePersist) {
        if (positionId == null || positionId.trim().isEmpty()) {
            throw new IllegalArgumentException("positionId must not be blank");
        }
        String normalizedPositionId = positionId.trim();

        RealPositionVO position = realPositionMapper.selectOpenPositionById(normalizedPositionId);
        if (position == null) {
            throw new IllegalArgumentException("持仓不存在或已关闭");
        }
        if (!MANUAL_SOURCE_TYPE.equals(position.getSourceType())
                || !MANUAL_SOURCE_NAME.equals(position.getSourceName())) {
            throw new IllegalArgumentException("不是手动持仓，无法触发监控复查");
        }

        LocalDateTime now = LocalDateTime.now();
        PositionMonitorRecordDO candidate = buildCandidateRecord(position, now);
        PositionMonitorRecordDO previous = positionMonitorRecordMapper.selectLatestByPositionId(normalizedPositionId);
        if (forcePersist || shouldPersist(previous, candidate, now)) {
            positionMonitorRecordMapper.insert(candidate);
            return buildOpenRow(position, candidate);
        }
        return buildOpenRow(position, previous != null ? previous : candidate);
    }

    @Override
    public void evaluateForSymbol(String symbol) {
        String normalizedSymbol = safeUpper(symbol);
        if (normalizedSymbol == null) {
            return;
        }
        List<RealPositionVO> positions = realPositionMapper.selectOpenManualPositionsBySymbol(normalizedSymbol);
        if (positions == null || positions.isEmpty()) {
            return;
        }
        for (RealPositionVO p : positions) {
            if (p == null || isBlank(p.getPositionId())) {
                continue;
            }
            try {
                evaluateForPosition(p.getPositionId(), false);
            } catch (Exception e) {
                log.warn("evaluateForSymbol skip positionId={} symbol={} err={}",
                        p.getPositionId(), normalizedSymbol, e.getMessage());
            }
        }
    }

    @Override
    public List<PositionMonitorOpenRowVO> listOpenManualPositions() {
        List<RealPositionVO> open = realPositionMapper.findOpenPositions();
        List<PositionMonitorOpenRowVO> out = new ArrayList<>();
        if (open == null || open.isEmpty()) {
            return out;
        }
        for (RealPositionVO p : open) {
            if (p == null) {
                continue;
            }
            if (!MANUAL_SOURCE_TYPE.equals(p.getSourceType())
                    || !MANUAL_SOURCE_NAME.equals(p.getSourceName())) {
                continue;
            }
            PositionMonitorRecordDO latest = positionMonitorRecordMapper.selectLatestByPositionId(p.getPositionId());
            PositionMonitorOpenRowVO row = new PositionMonitorOpenRowVO();
            row.setPositionId(p.getPositionId());
            row.setSymbol(p.getSymbol());
            row.setPositionSide(p.getPositionSide());
            row.setAvgOpenPrice(p.getAvgOpenPrice());
            row.setMarkPrice(p.getMarkPrice());
            row.setUnrealizedPnlPct(p.getUnrealizedPnlPct());
            row.setPositionQuantity(p.getPositionQuantity());
            row.setPositionOpenTime(p.getPositionOpenTime());

            if (latest == null) {
                row.setMonitorRecordAvailable(false);
                row.setLatestMonitorRecord(null);
            } else {
                row.setMonitorRecordAvailable(true);
                row.setLatestMonitorRecord(buildLatestMonitorRecordVo(latest));
            }
            out.add(row);
        }
        return out;
    }

    private static String safeUpper(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().toUpperCase();
        return t.isEmpty() ? null : t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        if (!isBlank(a)) {
            return a.trim();
        }
        return b;
    }

    private static String resolveEntryLogicState(DecisionResultVO decision,
                                                  String invalidConditionText,
                                                  boolean planPresent) {
        if (!isBlank(invalidConditionText)) {
            return ENTRY_INVALIDATED;
        }
        // 首版保守：plan 缺失时宁可弱化入场逻辑
        if (decision == null) {
            return ENTRY_WEAKENED;
        }
        if (!planPresent) {
            return ENTRY_WEAKENED;
        }
        if (Boolean.FALSE.equals(decision.getIsWorthOpening())) {
            return ENTRY_WEAKENED;
        }
        String multi = decision.getMultiTfConvergence();
        Integer conflictScore = decision.getAiConflictScore();
        if ("WEAK".equalsIgnoreCase(multi) && conflictScore != null && conflictScore >= 46) {
            return ENTRY_WEAKENED;
        }
        return ENTRY_VALID;
    }

    private static String resolveDirectionSupportState(String positionSide,
                                                        DecisionResultVO decision,
                                                        boolean invalidated) {
        if (invalidated) {
            return DIR_RANGE;
        }
        if (decision == null) {
            return DIR_RANGE;
        }
        String bias = decision.getMarketBiasHierarchy();
        String multi = decision.getMultiTfConvergence();
        Integer conflictScore = decision.getAiConflictScore();
        String conflictLevel = decision.getAiConflictLevel();

        if (isBlank(positionSide) || isBlank(bias)) {
            return DIR_RANGE;
        }

        boolean conflictExpanding = "WEAK".equalsIgnoreCase(multi)
                && (
                (conflictLevel != null
                        && (conflictLevel.contains("LEVEL_3") || conflictLevel.contains("LEVEL_4")))
                        || (conflictScore != null && conflictScore >= 70)
                );
        if (conflictExpanding) {
            return DIR_CONFLICT_EXPANDING;
        }

        boolean supportOriginal;
        if ("LONG".equalsIgnoreCase(positionSide)) {
            supportOriginal = bias.toUpperCase().contains("BULLISH");
        } else if ("SHORT".equalsIgnoreCase(positionSide)) {
            supportOriginal = bias.toUpperCase().contains("BEARISH");
        } else {
            return DIR_RANGE;
        }

        return supportOriginal ? DIR_SUPPORT_ORIGINAL : DIR_AGAINST_ORIGINAL;
    }

    private static String resolveAiSupportState(DecisionResultVO decision, boolean invalidated) {
        if (invalidated) {
            return AI_AGAINST;
        }
        if (decision == null) {
            return AI_UNKNOWN;
        }
        Integer score = decision.getAiConflictScore();
        if (score == null) {
            return AI_UNKNOWN;
        }
        if (score >= 70) {
            return AI_AGAINST;
        }
        if (score >= 40) {
            return AI_MIXED;
        }
        return Boolean.TRUE.equals(decision.getIsWorthOpening()) ? AI_SUPPORT : AI_MIXED;
    }

    private static String resolvePositionRiskLevel(DecisionResultVO decision,
                                                    boolean invalidated,
                                                    boolean planPresent) {
        if (invalidated) {
            return RISK_EXTREME;
        }
        if (decision == null) {
            return RISK_MEDIUM;
        }
        String risk = decision.getRiskLevel();
        String base;
        if (risk == null) {
            base = RISK_MEDIUM;
        } else if ("LOW".equalsIgnoreCase(risk)) {
            base = RISK_LOW;
        } else if ("MEDIUM".equalsIgnoreCase(risk)) {
            base = RISK_MEDIUM;
        } else if ("HIGH".equalsIgnoreCase(risk)) {
            base = RISK_HIGH;
        } else if ("EXTREME".equalsIgnoreCase(risk)) {
            base = RISK_EXTREME;
        } else {
            base = RISK_MEDIUM;
        }

        // 首版保守：plan 缺失时不把风险降档
        if (!planPresent) {
            if (RISK_LOW.equals(base)) {
                return RISK_MEDIUM;
            }
            return base;
        }

        Integer conflictScore = decision.getAiConflictScore();
        if (conflictScore != null && conflictScore >= 70 && !RISK_EXTREME.equals(base)) {
            return RISK_HIGH;
        }
        if ("WEAK".equalsIgnoreCase(decision.getMultiTfConvergence()) && !RISK_EXTREME.equals(base)) {
            if (RISK_LOW.equals(base)) {
                return RISK_MEDIUM;
            }
            return RISK_HIGH;
        }
        return base;
    }

    private static String resolveSystemSuggestedAction(String entryLogicState,
                                                        DecisionResultVO decision,
                                                        boolean planPresent,
                                                        boolean invalidated,
                                                        String directionSupportState,
                                                        String aiSupportState,
                                                        String positionRiskLevel) {
        if (ENTRY_INVALIDATED.equals(entryLogicState) || invalidated) {
            return ACT_PLAN_INVALID_WAIT_CONFIRM;
        }
        if (decision == null || !planPresent) {
            return ACT_PLAN_INVALID_WAIT_CONFIRM;
        }
        if (AI_AGAINST.equals(aiSupportState)) {
            return ACT_PLAN_INVALID_WAIT_CONFIRM;
        }
        if (RISK_EXTREME.equals(positionRiskLevel)) {
            return ACT_TIGHTEN_STOP;
        }
        if (DIR_AGAINST_ORIGINAL.equals(directionSupportState)) {
            return ACT_REDUCE_POSITION;
        }
        if (DIR_CONFLICT_EXPANDING.equals(directionSupportState)) {
            return ACT_TIGHTEN_STOP;
        }
        return Boolean.FALSE.equals(decision.getIsWorthOpening()) ? ACT_NO_ADD : ACT_CONTINUE_HOLD;
    }

    private static String resolveReversalState(String systemSuggestedAction) {
        if (ACT_CLOSE_AND_ENTER_REVIEW.equals(systemSuggestedAction)) {
            return REV_STRONG;
        }
        if (ACT_PLAN_INVALID_WAIT_CONFIRM.equals(systemSuggestedAction)) {
            return REV_STRONG;
        }
        if (ACT_CONTINUE_HOLD.equals(systemSuggestedAction)) {
            return REV_NONE;
        }
        // 其余保守动作均视为弱信号
        return REV_WEAK;
    }

    private static String resolveReviewEntryStatus(String systemSuggestedAction) {
        if (ACT_CLOSE_AND_ENTER_REVIEW.equals(systemSuggestedAction)) {
            return REVIEW_READY_TO_REVIEW;
        }
        return REVIEW_NOT_ENTERED;
    }

    private static final class ScoreTuneResult {
        final String riskLevel;
        final String action;

        ScoreTuneResult(String riskLevel, String action) {
            this.riskLevel = riskLevel;
            this.action = action;
        }
    }

    /**
     * confusedScore / dataQualityScore：仅抬风险与弱化建议，不单独给出平仓类动作。
     * invalidCondition / 失效链路优先于本段逻辑。
     */
    private static ScoreTuneResult applyScoreTuning(DecisionResultVO decision,
                                                     String entryLogicState,
                                                     String baseRisk,
                                                     String baseAction) {
        Integer confused = decision != null ? decision.getConfusedScore() : null;
        Integer dq = decision != null ? decision.getDataQualityScore() : null;
        Integer conflictScore = decision != null ? decision.getAiConflictScore() : null;
        int conflictLevel = decision != null
                ? parseAiConflictLevel(decision.getAiConflictLevel(), conflictScore)
                : 0;
        String assetState = decision != null ? parseAssetState(decision.getAssetStateSnapshot()) : null;

        String risk = baseRisk;
        if (confused != null && confused >= monitorRiskElevateThreshold) {
            risk = elevateRiskToMin(risk, RISK_HIGH);
        }
        if (confused != null && confused >= monitorHardBlockThreshold) {
            risk = elevateRiskToMin(risk, RISK_EXTREME);
        }
        if (conflictLevel >= CONFLICT_LEVEL_3) {
            risk = elevateRiskToMin(risk, RISK_HIGH);
        }
        if (conflictLevel >= CONFLICT_LEVEL_4) {
            risk = elevateRiskToMin(risk, RISK_EXTREME);
        }
        if ("HIGH_RISK".equals(assetState)) {
            risk = elevateRiskToMin(risk, RISK_HIGH);
        } else if ("CONFUSED".equals(assetState) || "INVALIDATED".equals(assetState)) {
            risk = elevateRiskToMin(risk, RISK_EXTREME);
        } else if ("COOLING".equals(assetState)) {
            risk = elevateRiskToMin(risk, RISK_MEDIUM);
        }
        if (dq != null && dq < 70) {
            risk = elevateRiskToMin(risk, RISK_HIGH);
        }
        if (dq != null && dq >= 70 && dq < 85) {
            risk = elevateRiskToMin(risk, RISK_MEDIUM);
        }

        String entry = safeUpper(entryLogicState);
        boolean preserveInvalidClose = ENTRY_INVALIDATED.equals(entry)
                || ACT_CLOSE_AND_ENTER_REVIEW.equals(safeUpper(baseAction));

        String tuned = baseAction;
        if (!preserveInvalidClose) {
            boolean preserveStrong = isStrongRiskAction(baseAction);
            if ("INVALIDATED".equals(assetState)) {
                tuned = ACT_PLAN_INVALID_WAIT_CONFIRM;
            } else if ("CONFUSED".equals(assetState) || conflictLevel >= CONFLICT_LEVEL_4
                    || (confused != null && confused >= monitorHardBlockThreshold)) {
                tuned = ACT_PLAN_INVALID_WAIT_CONFIRM;
            } else if ("HIGH_RISK".equals(assetState) && !preserveStrong) {
                tuned = ACT_TIGHTEN_STOP;
            } else if ("COOLING".equals(assetState) && !preserveStrong) {
                tuned = ACT_PLAN_INVALID_WAIT_CONFIRM;
            } else if ((confused != null && confused >= monitorRiskElevateThreshold) || conflictLevel >= CONFLICT_LEVEL_3) {
                tuned = tuneTowardWaitConfirm(tuned);
            }
            if (dq != null && dq < 70 && !preserveStrong) {
                tuned = tuneTowardWaitConfirm(tuned);
            }
            if (confused != null && confused > 0 && confused < monitorRiskElevateThreshold && !preserveStrong) {
                tuned = tuneMildNoAdd(tuned);
            }
            if (dq != null && dq >= 70 && dq < 85 && !preserveStrong) {
                tuned = tuneMildNoAdd(tuned);
            }
        }

        return new ScoreTuneResult(risk, tuned);
    }

    private static int parseAiConflictLevel(String aiConflictLevel, Integer aiConflictScore) {
        String s = safeUpper(aiConflictLevel);
        if (s != null) {
            if ("3".equals(s) || s.contains("LEVEL_3") || "L3".equals(s) || "LV3".equals(s)) {
                return CONFLICT_LEVEL_3;
            }
            if ("4".equals(s) || s.contains("LEVEL_4") || "L4".equals(s) || "LV4".equals(s)) {
                return CONFLICT_LEVEL_4;
            }
        }
        if (aiConflictScore != null && aiConflictScore >= monitorHardBlockThreshold) {
            return CONFLICT_LEVEL_4;
        }
        if (aiConflictScore != null && aiConflictScore >= monitorRiskElevateThreshold) {
            return CONFLICT_LEVEL_3;
        }
        return 0;
    }

    private static String parseAssetState(String assetStateSnapshot) {
        String s = safeUpper(assetStateSnapshot);
        if (s == null) {
            return null;
        }
        if (s.contains("CONFUSED")) {
            return "CONFUSED";
        }
        if (s.contains("HIGH_RISK")) {
            return "HIGH_RISK";
        }
        if (s.contains("INVALIDATED")) {
            return "INVALIDATED";
        }
        if (s.contains("COOLING")) {
            return "COOLING";
        }
        return null;
    }

    private static int riskRank(String level) {
        String s = safeUpper(level);
        if (RISK_EXTREME.equals(s)) {
            return 3;
        }
        if (RISK_HIGH.equals(s)) {
            return 2;
        }
        if (RISK_MEDIUM.equals(s)) {
            return 1;
        }
        if (RISK_LOW.equals(s)) {
            return 0;
        }
        return 1;
    }

    private static String riskFromRank(int rank) {
        if (rank >= 3) {
            return RISK_EXTREME;
        }
        if (rank == 2) {
            return RISK_HIGH;
        }
        if (rank == 1) {
            return RISK_MEDIUM;
        }
        return RISK_LOW;
    }

    private static String elevateRiskToMin(String current, String min) {
        return riskFromRank(Math.max(riskRank(current), riskRank(min)));
    }

    private static boolean isStrongRiskAction(String action) {
        String s = safeUpper(action);
        return ACT_REDUCE_POSITION.equals(s)
                || ACT_TIGHTEN_STOP.equals(s)
                || ACT_MOVE_STOP.equals(s)
                || ACT_PARTIAL_TAKE_PROFIT.equals(s);
    }

    private static String tuneTowardWaitConfirm(String action) {
        String s = safeUpper(action);
        if (ACT_CLOSE_AND_ENTER_REVIEW.equals(s)) {
            return action;
        }
        if (ACT_PLAN_INVALID_WAIT_CONFIRM.equals(s)) {
            return action;
        }
        if (isStrongRiskAction(action)) {
            return action;
        }
        return ACT_PLAN_INVALID_WAIT_CONFIRM;
    }

    private static String tuneMildNoAdd(String action) {
        String s = safeUpper(action);
        if (ACT_CLOSE_AND_ENTER_REVIEW.equals(s)) {
            return action;
        }
        if (isStrongRiskAction(action)) {
            return action;
        }
        if (ACT_PLAN_INVALID_WAIT_CONFIRM.equals(s)) {
            return action;
        }
        if (ACT_CONTINUE_HOLD.equals(s)) {
            return ACT_NO_ADD;
        }
        return action;
    }

    static String buildSupplementalNotes(DecisionResultVO decision, boolean analysisLinked) {
        if (decision == null) {
            return "";
        }
        Integer confused = decision.getConfusedScore();
        Integer dq = decision.getDataQualityScore();
        StringBuilder sb = new StringBuilder();
        if (confused != null && confused >= monitorConfusedHintThreshold) {
            sb.append("困惑度较高，建议人工确认后再处理。");
        } else if (confused != null && confused > 0 && confused < monitorConfusedHintThreshold) {
            sb.append("存在轻度困惑，建议人工复核。");
        }
        if (dq != null && dq < 70) {
            sb.append("数据质量不足，本次监控判断仅作参考。");
        } else if (dq != null && dq >= 70 && dq < 85) {
            sb.append("数据质量未达到强确认阈值，建议谨慎参考。");
        } else if (analysisLinked && dq == null) {
            sb.append("数据质量分未返回，结论仅供参考。");
        }
        return sb.toString();
    }

    /**
     * 只读解析 plan_boundary_json，生成 monitorSummary 补充句；与 {@link PlanBoundaryDisplayHelper} 同源展示语义。
     */
    static String buildBoundaryParseStatusNote(String planBoundaryJson) {
        return PlanBoundaryDisplayHelper.parse(planBoundaryJson, PlanBoundaryDisplayContext.POSITION_MONITOR).displayText();
    }

    static String mergeSupplementalAndBoundaryNotes(String supplementalNotes, String boundaryNote) {
        String b = boundaryNote == null ? "" : boundaryNote.trim();
        String s = supplementalNotes == null ? "" : supplementalNotes.trim();
        if (b.isEmpty()) {
            return s;
        }
        if (s.isEmpty()) {
            return b;
        }
        return s + " " + b;
    }

    static String buildMonitorSummary(boolean inputsInsufficient,
                                      String entryLogicState,
                                      String directionSupportState,
                                      String reversalState,
                                      String positionRiskLevel,
                                      String systemSuggestedAction,
                                      String supplementalNotes) {
        if (inputsInsufficient) {
            return "输入不足，已按保守口径生成监控记录";
        }
        String base = "监控摘要：入场逻辑" + mapEntryLogicStateZh(entryLogicState)
                + "，趋势/方向" + mapDirectionSupportStateZh(directionSupportState)
                + "，" + mapReversalStateZh(reversalState)
                + "，当前风险" + mapPositionRiskLevelZh(positionRiskLevel)
                + "，建议" + mapSystemSuggestedActionZh(systemSuggestedAction) + "。";
        if (supplementalNotes != null && !supplementalNotes.isBlank()) {
            return base + " " + supplementalNotes.trim();
        }
        return base;
    }

    private static String resolveReversalHintText(String entryLogicState,
                                                  String directionSupportState,
                                                  String reversalState,
                                                  String positionRiskLevel,
                                                  String systemSuggestedAction) {
        String entry = safeUpper(entryLogicState);
        String direction = safeUpper(directionSupportState);
        String reversal = safeUpper(reversalState);
        String risk = safeUpper(positionRiskLevel);
        String action = safeUpper(systemSuggestedAction);

        if (ENTRY_INVALIDATED.equals(entry) || ACT_PLAN_INVALID_WAIT_CONFIRM.equals(action)) {
            return REVERSAL_HINT_PLAN_INVALIDATED;
        }
        if (REV_STRONG.equals(reversal) || RISK_EXTREME.equals(risk)) {
            return REVERSAL_HINT_STRONG_RISK;
        }
        if (REV_WEAK.equals(reversal)
                && (RISK_HIGH.equals(risk)
                || DIR_AGAINST_ORIGINAL.equals(direction)
                || ACT_TIGHTEN_STOP.equals(action)
                || ACT_MOVE_STOP.equals(action))) {
            return REVERSAL_HINT_WARNING;
        }
        if (REV_WEAK.equals(reversal)) {
            return REVERSAL_HINT_WATCH;
        }
        return null;
    }

    static String appendReversalHint(String monitorSummary,
                                     String entryLogicState,
                                     String directionSupportState,
                                     String reversalState,
                                     String positionRiskLevel,
                                     String systemSuggestedAction) {
        String hint = resolveReversalHintText(entryLogicState,
                directionSupportState,
                reversalState,
                positionRiskLevel,
                systemSuggestedAction);
        if (hint == null || hint.isBlank()) {
            return monitorSummary;
        }
        String base = monitorSummary == null ? "" : monitorSummary.trim();
        if (base.contains(hint)) {
            return base;
        }
        if (base.isEmpty()) {
            return "提示：" + hint + "。";
        }
        return base + " 提示：" + hint + "。";
    }

    static boolean shouldPersist(PositionMonitorRecordDO previous, PositionMonitorRecordDO candidate, LocalDateTime now) {
        if (candidate == null || now == null) {
            return false;
        }
        if (previous == null) {
            return true;
        }
        if (!safeEquals(previous.getEntryLogicState(), candidate.getEntryLogicState())) {
            return true;
        }
        if (rankReversal(candidate.getReversalState()) > rankReversal(previous.getReversalState())) {
            return true;
        }
        if (rankRisk(candidate.getPositionRiskLevel()) > rankRisk(previous.getPositionRiskLevel())) {
            return true;
        }
        if (rankAiSupport(candidate.getAiSupportState()) > rankAiSupport(previous.getAiSupportState())) {
            return true;
        }
        if (!safeEquals(previous.getSystemSuggestedAction(), candidate.getSystemSuggestedAction())) {
            return true;
        }
        if (!REVIEW_READY_TO_REVIEW.equals(safeUpper(previous.getReviewEntryStatus()))
                && REVIEW_READY_TO_REVIEW.equals(safeUpper(candidate.getReviewEntryStatus()))) {
            return true;
        }
        LocalDateTime prevTime = previous.getMonitorTime();
        if (prevTime == null) {
            return true;
        }
        return Duration.between(prevTime, now).toMinutes() >= HEARTBEAT_MINUTES;
    }

    private PositionMonitorRecordDO buildCandidateRecord(RealPositionVO position, LocalDateTime now) {
        String symbol = safeUpper(position.getSymbol());
        String positionSide = safeUpper(position.getPositionSide());
        DecisionResultVO decision = null;
        if (symbol != null) {
            try {
                decision = decisionResultMapper.findLatestDecisionResultBaseBySymbol(symbol);
            } catch (Exception ignored) {
            }
        }
        boolean decisionPresent = decision != null && !isBlank(decision.getAnalysisId());
        String planId = null;
        String planInvalidCondition = null;
        String planBoundaryJson = null;
        boolean planPresent = false;
        if (decisionPresent) {
            try {
                var plan = executionPlanMapper.selectLatestByAnalysisIdTieBreak(decision.getAnalysisId());
                if (plan != null) {
                    planId = plan.getPlanId();
                    planInvalidCondition = plan.getInvalidCondition();
                    planBoundaryJson = plan.getPlanBoundaryJson();
                    planPresent = !isBlank(planId);
                }
            } catch (Exception ignored) {
            }
            try {
                AnalysisRunDO analysisRun = analysisRunMapper.selectById(decision.getAnalysisId());
                if (analysisRun != null && analysisRun.getDataQualityScore() != null) {
                    decision.setDataQualityScore(analysisRun.getDataQualityScore());
                }
            } catch (Exception ignored) {
            }
        }
        String invalidConditionText = firstNonBlank(
                decision != null ? decision.getInvalidCondition() : null,
                planInvalidCondition
        );
        boolean invalidated = !isBlank(invalidConditionText);
        String entryLogicState = resolveEntryLogicState(decision, invalidConditionText, planPresent);
        String directionSupportState = resolveDirectionSupportState(positionSide, decision, invalidated);
        String aiSupportState = resolveAiSupportState(decision, invalidated);
        String positionRiskLevel = resolvePositionRiskLevel(decision, invalidated, planPresent);
        String systemSuggestedAction = resolveSystemSuggestedAction(
                entryLogicState,
                decision,
                planPresent,
                invalidated,
                directionSupportState,
                aiSupportState,
                positionRiskLevel
        );
        ScoreTuneResult scoreTune = applyScoreTuning(decision, entryLogicState, positionRiskLevel, systemSuggestedAction);
        positionRiskLevel = scoreTune.riskLevel;
        systemSuggestedAction = scoreTune.action;
        String reversalState = resolveReversalState(systemSuggestedAction);
        String reviewEntryStatus = resolveReviewEntryStatus(systemSuggestedAction);
        boolean inputsInsufficient = !decisionPresent || !planPresent;
        String supplementalNotes = mergeSupplementalAndBoundaryNotes(
                buildSupplementalNotes(decision, decisionPresent),
                buildBoundaryParseStatusNote(planBoundaryJson));
        String monitorSummary = buildMonitorSummary(
                inputsInsufficient,
                entryLogicState,
                directionSupportState,
                reversalState,
                positionRiskLevel,
                systemSuggestedAction,
                supplementalNotes
        );
        monitorSummary = appendReversalHint(
                monitorSummary,
                entryLogicState,
                directionSupportState,
                reversalState,
                positionRiskLevel,
                systemSuggestedAction
        );

        PositionMonitorRecordDO record = new PositionMonitorRecordDO();
        record.setPositionMonitorRecordId("pmr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        record.setPositionId(position.getPositionId());
        record.setSymbol(symbol);
        record.setAnalysisId(decisionPresent ? decision.getAnalysisId() : null);
        record.setPlanId(planPresent ? planId : null);
        record.setMonitorTime(now);
        record.setEntryLogicState(entryLogicState);
        record.setDirectionSupportState(directionSupportState);
        record.setReversalState(reversalState);
        record.setPositionRiskLevel(positionRiskLevel);
        record.setAiSupportState(aiSupportState);
        record.setSystemSuggestedAction(systemSuggestedAction);
        record.setMonitorSummary(monitorSummary);
        record.setReviewEntryStatus(reviewEntryStatus);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private static int rankReversal(String state) {
        String s = safeUpper(state);
        if (REV_STRONG.equals(s)) return 2;
        if (REV_WEAK.equals(s)) return 1;
        return 0;
    }

    private static int rankRisk(String level) {
        String s = safeUpper(level);
        if (RISK_EXTREME.equals(s)) return 3;
        if (RISK_HIGH.equals(s)) return 2;
        if (RISK_MEDIUM.equals(s)) return 1;
        return 0;
    }

    private static int rankAiSupport(String state) {
        String s = safeUpper(state);
        if (AI_AGAINST.equals(s)) return 2;
        if (AI_MIXED.equals(s) || AI_UNKNOWN.equals(s)) return 1;
        return 0;
    }

    private static boolean safeEquals(String a, String b) {
        String aa = safeUpper(a);
        String bb = safeUpper(b);
        if (aa == null && bb == null) return true;
        if (aa == null || bb == null) return false;
        return aa.equals(bb);
    }

    private static String mapEntryLogicStateZh(String state) {
        String s = safeUpper(state);
        if (ENTRY_VALID.equals(s)) return "仍成立";
        if (ENTRY_WEAKENED.equals(s)) return "弱化";
        if (ENTRY_INVALIDATED.equals(s)) return "失效";
        return "待确认";
    }

    private static String mapDirectionSupportStateZh(String state) {
        String s = safeUpper(state);
        if (DIR_SUPPORT_ORIGINAL.equals(s)) return "仍支持原方向";
        if (DIR_RANGE.equals(s)) return "转为震荡";
        if (DIR_AGAINST_ORIGINAL.equals(s)) return "短线反向";
        if (DIR_CONFLICT_EXPANDING.equals(s)) return "多周期冲突扩大";
        return "待确认";
    }

    private static String mapReversalStateZh(String state) {
        String s = safeUpper(state);
        if (REV_NONE.equals(s)) return "无明显反转";
        if (REV_WEAK.equals(s)) return "弱反转";
        if (REV_STRONG.equals(s)) return "强反转";
        return "反转待确认";
    }

    private static String mapPositionRiskLevelZh(String level) {
        String s = safeUpper(level);
        if (RISK_LOW.equals(s)) return "低";
        if (RISK_MEDIUM.equals(s)) return "中";
        if (RISK_HIGH.equals(s)) return "高";
        if (RISK_EXTREME.equals(s)) return "极高";
        return "待确认";
    }

    private static String mapSystemSuggestedActionZh(String action) {
        String s = safeUpper(action);
        if (ACT_CONTINUE_HOLD.equals(s)) return "继续持有";
        if (ACT_NO_ADD.equals(s)) return "暂不加仓";
        if (ACT_REDUCE_POSITION.equals(s)) return "降低仓位";
        if (ACT_TIGHTEN_STOP.equals(s)) return "收紧止损";
        if (ACT_MOVE_STOP.equals(s)) return "移动止损";
        if (ACT_PARTIAL_TAKE_PROFIT.equals(s)) return "分批止盈";
        if (ACT_PLAN_INVALID_WAIT_CONFIRM.equals(s)) return "计划失效，等待人工确认";
        if (ACT_CLOSE_AND_ENTER_REVIEW.equals(s)) return "记录平仓并进入复盘";
        return "人工确认";
    }

    private PositionMonitorOpenRowVO buildOpenRow(RealPositionVO position, PositionMonitorRecordDO record) {
        PositionMonitorOpenRowVO row = new PositionMonitorOpenRowVO();
        row.setPositionId(position.getPositionId());
        row.setSymbol(position.getSymbol());
        row.setPositionSide(position.getPositionSide());
        row.setAvgOpenPrice(position.getAvgOpenPrice());
        row.setMarkPrice(position.getMarkPrice());
        row.setUnrealizedPnlPct(position.getUnrealizedPnlPct());
        row.setPositionQuantity(position.getPositionQuantity());
        row.setPositionOpenTime(position.getPositionOpenTime());

        row.setMonitorRecordAvailable(true);
        row.setLatestMonitorRecord(buildLatestMonitorRecordVo(record));
        return row;
    }

    private String resolvePlanBoundaryJsonForDisplay(PositionMonitorRecordDO record) {
        if (record == null) {
            return null;
        }
        String planId = record.getPlanId();
        if (isBlank(planId)) {
            return null;
        }
        try {
            ExecutionPlanDO plan = executionPlanMapper.selectByPlanId(planId.trim());
            return plan != null ? plan.getPlanBoundaryJson() : null;
        } catch (Exception e) {
            log.warn("resolvePlanBoundaryJsonForDisplay planId={} err={}", planId, e.getMessage());
            return null;
        }
    }

    private LatestMonitorRecordVO buildLatestMonitorRecordVo(PositionMonitorRecordDO record) {
        if (record == null) {
            return null;
        }
        LatestMonitorRecordVO vo = new LatestMonitorRecordVO();
        vo.setPositionMonitorRecordId(record.getPositionMonitorRecordId());
        vo.setAnalysisId(record.getAnalysisId());
        vo.setPlanId(record.getPlanId());
        vo.setMonitorTime(record.getMonitorTime());
        vo.setEntryLogicState(record.getEntryLogicState());
        vo.setDirectionSupportState(record.getDirectionSupportState());
        vo.setReversalState(record.getReversalState());
        vo.setPositionRiskLevel(record.getPositionRiskLevel());
        vo.setAiSupportState(record.getAiSupportState());
        vo.setSystemSuggestedAction(record.getSystemSuggestedAction());
        vo.setMonitorSummary(record.getMonitorSummary());
        vo.setReviewEntryStatus(record.getReviewEntryStatus());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());

        PlanBoundaryDisplayInfo bd = PlanBoundaryDisplayHelper.parse(
                resolvePlanBoundaryJsonForDisplay(record),
                PlanBoundaryDisplayContext.POSITION_MONITOR);
        vo.setBoundaryParseStatus(bd.parseStatus());
        vo.setBoundaryStateLabel(bd.stateLabel());
        vo.setBoundaryDisplayText(bd.displayText());
        vo.setBoundaryWarningText(bd.warningText());
        vo.setInvalidPriceDirection(bd.invalidPriceDirection());
        vo.setInvalidPriceThreshold(bd.invalidPriceThreshold());
        return vo;
    }
}

