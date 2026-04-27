package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.MissedReasonViewParser;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.service.PushRecheckStatusContract;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.RuleVersionLogQueryService;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.example.trademodel.vo.ReviewAggregateDetailVO;
import org.example.trademodel.vo.ReviewAggregateSummaryVO;
import org.example.trademodel.vo.ReviewAggregateVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 组装 {@link ReviewAggregateVO}：从各表读摘要字段；Push/Recheck 中 JSON 列原样映射至 VO，不做解析。
 * Hot Reset 区：仅对 missed.reasonJson 做最小只读解析以生成旁证文案，不改动规则与落库。
 */
@Service
public class ReviewAggregateServiceImpl implements ReviewAggregateService {

    private static final ObjectMapper JSON = new ObjectMapper();
    static final int DETAIL_LIMIT_DEFAULT = 20;
    static final int DETAIL_LIMIT_MAX = 50;
    static final int DETAIL_RECHECK_PER_PUSH_MAX = 5;
    static final String SECTION_PUSH_RECHECK = "pushRecheck";
    static final String SECTION_MISSED = "missed";
    static final String SECTION_ALERTS = "alerts";
    static final String SECTION_RULE_VERSION_LOGS = "ruleVersionLogs";
    static final String SECTION_HOT_RESET = "hotReset";
    private static final String CHANGE_CATEGORY_REVIEW_FEEDBACK_SAVED = "REVIEW_FEEDBACK_SAVED";

    private final AnalysisRunMapper analysisRunMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    private final AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final MissedOpportunityMapper missedOpportunityMapper;
    private final MonitorAlertMapper monitorAlertMapper;
    private final AssetStateMapper assetStateMapper;
    private final HotResetEventMapper hotResetEventMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final RuleVersionLogQueryService ruleVersionLogQueryService;
    private final EvidenceService evidenceService;
    private final ScoreService scoreService;

    public ReviewAggregateServiceImpl(
            AnalysisRunMapper analysisRunMapper,
            DecisionResultMapper decisionResultMapper,
            ExecutionPlanMapper executionPlanMapper,
            PushSnapshotMapper pushSnapshotMapper,
            MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper,
            AccountRiskSnapshotMapper accountRiskSnapshotMapper,
            PushRecheckLogMapper pushRecheckLogMapper,
            MissedOpportunityMapper missedOpportunityMapper,
            MonitorAlertMapper monitorAlertMapper,
            AssetStateMapper assetStateMapper,
            HotResetEventMapper hotResetEventMapper,
            ReviewResultMapper reviewResultMapper,
            RuleVersionLogQueryService ruleVersionLogQueryService,
            EvidenceService evidenceService,
            ScoreService scoreService) {
        this.analysisRunMapper = analysisRunMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.marketEnvironmentSnapshotMapper = marketEnvironmentSnapshotMapper;
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.missedOpportunityMapper = missedOpportunityMapper;
        this.monitorAlertMapper = monitorAlertMapper;
        this.assetStateMapper = assetStateMapper;
        this.hotResetEventMapper = hotResetEventMapper;
        this.reviewResultMapper = reviewResultMapper;
        this.ruleVersionLogQueryService = ruleVersionLogQueryService;
        this.evidenceService = evidenceService;
        this.scoreService = scoreService;
    }

    @Override
    public Optional<ReviewAggregateVO> getAggregateByAnalysisId(String analysisId) {
        AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
        if (run == null) {
            return Optional.empty();
        }

        ReviewAggregateVO vo = new ReviewAggregateVO();
        vo.setRun(toRunSummary(run));

        DecisionResult dr = decisionResultMapper.selectLatestByAnalysisId(analysisId);
        vo.setDecision(dr == null ? null : toDecisionSummary(dr));

        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(analysisId);
        vo.setPlan(plan == null ? null : toPlanSummary(plan));
        vo.setMarketEnvironment(toMarketEnvironmentSummary(marketEnvironmentSnapshotMapper.selectByAnalysisId(analysisId)));

        List<EvidenceBriefVO> evidenceTopItems = evidenceService.listTopEvidenceBriefByAnalysisId(analysisId);
        vo.setEvidenceTopItems(evidenceTopItems != null ? evidenceTopItems : Collections.emptyList());

        List<ScoreBriefVO> scoreTopItems = scoreService.listTopScoreBriefByAnalysisId(analysisId);
        vo.setScoreTopItems(scoreTopItems != null ? scoreTopItems : Collections.emptyList());

        List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck = buildPushRecheck(analysisId);
        vo.setPushRecheck(pushRecheck);
        org.example.trademodel.entity.ReviewResultDO reviewResult = reviewResultMapper.selectByAnalysisId(analysisId);
        List<ReviewAggregateVO.RuleVersionLogSummary> ruleVersionLogs = ruleVersionLogQueryService.listByAnalysisId(analysisId, 20);
        ReviewAggregateVO.RuleVersionLogSummary linkedReviewLog = markLatestReviewLinkedLog(ruleVersionLogs);
        vo.setRuleVersionLogs(ruleVersionLogs);
        List<MissedOpportunityDO> missedRows = missedOpportunityMapper.listByAnalysisId(analysisId);
        List<ReviewAggregateVO.ReviewMissedSummary> missed = toMissedList(missedRows);
        vo.setMissed(missed);
        ReviewAggregateVO.ReviewHotResetSummary hotReset = toHotReset(run, dr, missedRows);
        vo.setHotReset(hotReset);
        List<ReviewAggregateVO.ReviewAlertSummary> alerts = toAlertList(monitorAlertMapper.listByAnalysisId(analysisId));
        vo.setAlerts(alerts);
        vo.setReviewClosure(buildReviewClosure(run, dr, plan, pushRecheck, missed, hotReset, alerts, reviewResult));
        vo.setGovernanceSummary(buildGovernanceSummary(reviewResult, linkedReviewLog));

        return Optional.of(vo);
    }

    @Override
    public Optional<ReviewAggregateSummaryVO> getAggregateSummaryByAnalysisId(String analysisId) {
        AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
        if (run == null) {
            return Optional.empty();
        }

        ReviewAggregateSummaryVO vo = new ReviewAggregateSummaryVO();
        vo.setRun(toRunSummary(run));

        DecisionResult dr = decisionResultMapper.selectLatestByAnalysisId(analysisId);
        vo.setDecision(dr == null ? null : toDecisionSummary(dr));

        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(analysisId);
        vo.setPlan(plan == null ? null : toPlanSummary(plan));

        List<TmPushSnapshotDO> pushRows = pushSnapshotMapper.listByAnalysisId(analysisId);
        List<MissedOpportunityDO> missedRows = missedOpportunityMapper.listByAnalysisId(analysisId);
        List<MonitorAlertDO> alertRows = monitorAlertMapper.listByAnalysisId(analysisId);
        ReviewAggregateVO.ReviewHotResetSummary hotReset = toHotReset(run, dr, missedRows);

        vo.setReviewClosure(buildReviewClosure(
                run,
                dr,
                plan,
                Collections.emptyList(),
                Collections.emptyList(),
                hotReset,
                Collections.emptyList(),
                reviewResultMapper.selectByAnalysisId(analysisId)));
        vo.setDetailSections(buildDetailSectionsMeta(pushRows.size(), missedRows.size(), alertRows.size()));
        return Optional.of(vo);
    }

    @Override
    public Optional<ReviewAggregateDetailVO> getAggregateDetailByAnalysisId(String analysisId, String section, int limit) {
        AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
        if (run == null) {
            return Optional.empty();
        }

        String normalizedSection = normalizeSection(section);
        int appliedLimit = clampLimit(limit);
        ReviewAggregateDetailVO vo = new ReviewAggregateDetailVO();
        vo.setAnalysisId(analysisId);
        vo.setSection(normalizedSection);
        vo.setLimitApplied(appliedLimit);

        switch (normalizedSection) {
            case SECTION_PUSH_RECHECK:
                fillPushRecheckDetail(vo, analysisId, appliedLimit);
                break;
            case SECTION_MISSED:
                fillMissedDetail(vo, analysisId, appliedLimit);
                break;
            case SECTION_ALERTS:
                fillAlertsDetail(vo, analysisId, appliedLimit);
                break;
            case SECTION_RULE_VERSION_LOGS:
                fillRuleVersionLogsDetail(vo, analysisId, appliedLimit);
                break;
            case SECTION_HOT_RESET:
                fillHotResetDetail(vo, run, analysisId);
                break;
            default:
                throw new IllegalArgumentException("unsupported detail section: " + normalizedSection);
        }

        return Optional.of(vo);
    }

    private static int clampLimit(int rawLimit) {
        if (rawLimit <= 0) {
            return DETAIL_LIMIT_DEFAULT;
        }
        return Math.min(rawLimit, DETAIL_LIMIT_MAX);
    }

    private static String normalizeSection(String section) {
        if (section == null || section.isBlank()) {
            return SECTION_PUSH_RECHECK;
        }
        String normalized = section.trim();
        if (SECTION_PUSH_RECHECK.equals(normalized)
                || SECTION_MISSED.equals(normalized)
                || SECTION_ALERTS.equals(normalized)
                || SECTION_RULE_VERSION_LOGS.equals(normalized)
                || SECTION_HOT_RESET.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("unsupported detail section: " + section);
    }

    private List<ReviewAggregateSummaryVO.DetailSectionMeta> buildDetailSectionsMeta(int pushCount, int missedCount, int alertCount) {
        List<ReviewAggregateSummaryVO.DetailSectionMeta> out = new ArrayList<>();
        out.add(sectionMeta(SECTION_PUSH_RECHECK, pushCount, DETAIL_LIMIT_DEFAULT));
        out.add(sectionMeta(SECTION_MISSED, missedCount, DETAIL_LIMIT_DEFAULT));
        out.add(sectionMeta(SECTION_ALERTS, alertCount, DETAIL_LIMIT_DEFAULT));
        out.add(sectionMeta(SECTION_RULE_VERSION_LOGS, DETAIL_LIMIT_DEFAULT, DETAIL_LIMIT_DEFAULT));
        out.add(sectionMeta(SECTION_HOT_RESET, 1, 1));
        return out;
    }

    private static ReviewAggregateSummaryVO.DetailSectionMeta sectionMeta(String section, int total, int recommendedLimit) {
        ReviewAggregateSummaryVO.DetailSectionMeta m = new ReviewAggregateSummaryVO.DetailSectionMeta();
        m.setSection(section);
        m.setTotal(total);
        m.setRecommendedLimit(recommendedLimit);
        return m;
    }

    private void fillPushRecheckDetail(ReviewAggregateDetailVO vo, String analysisId, int limit) {
        List<TmPushSnapshotDO> pushRows = pushSnapshotMapper.listByAnalysisId(analysisId);
        int total = pushRows == null ? 0 : pushRows.size();
        vo.setTotal(total);
        if (total == 0) {
            vo.setPushRecheck(Collections.emptyList());
            vo.setTruncated(Boolean.FALSE);
            return;
        }
        int toIndex = Math.min(limit, total);
        List<ReviewAggregateVO.ReviewPushWithRecheck> out = new ArrayList<>(toIndex);
        for (TmPushSnapshotDO s : pushRows.subList(0, toIndex)) {
            ReviewAggregateVO.ReviewPushWithRecheck bundle = new ReviewAggregateVO.ReviewPushWithRecheck();
            TmAccountRiskSnapshotDO risk = s.getAccountRiskSnapshotId() == null
                    ? null
                    : accountRiskSnapshotMapper.selectById(s.getAccountRiskSnapshotId());
            bundle.setPush(toPushSummary(s, risk));
            List<TmPushRecheckLogDO> logs = pushRecheckLogMapper.selectByPushId(s.getPushId());
            if (logs == null || logs.isEmpty()) {
                bundle.setRechecks(Collections.emptyList());
            } else {
                int recheckToIndex = Math.min(DETAIL_RECHECK_PER_PUSH_MAX, logs.size());
                bundle.setRechecks(toRecheckList(logs.subList(0, recheckToIndex)));
            }
            out.add(bundle);
        }
        vo.setPushRecheck(out);
        vo.setTruncated(total > toIndex);
    }

    private void fillMissedDetail(ReviewAggregateDetailVO vo, String analysisId, int limit) {
        List<MissedOpportunityDO> rows = missedOpportunityMapper.listByAnalysisId(analysisId);
        int total = rows == null ? 0 : rows.size();
        vo.setTotal(total);
        if (total == 0) {
            vo.setMissed(Collections.emptyList());
            vo.setTruncated(Boolean.FALSE);
            return;
        }
        int toIndex = Math.min(limit, total);
        vo.setMissed(toMissedList(rows.subList(0, toIndex)));
        vo.setTruncated(total > toIndex);
    }

    private void fillAlertsDetail(ReviewAggregateDetailVO vo, String analysisId, int limit) {
        List<MonitorAlertDO> rows = monitorAlertMapper.listByAnalysisId(analysisId);
        int total = rows == null ? 0 : rows.size();
        vo.setTotal(total);
        if (total == 0) {
            vo.setAlerts(Collections.emptyList());
            vo.setTruncated(Boolean.FALSE);
            return;
        }
        int toIndex = Math.min(limit, total);
        vo.setAlerts(toAlertList(rows.subList(0, toIndex)));
        vo.setTruncated(total > toIndex);
    }

    private void fillRuleVersionLogsDetail(ReviewAggregateDetailVO vo, String analysisId, int limit) {
        List<ReviewAggregateVO.RuleVersionLogSummary> logs = ruleVersionLogQueryService.listByAnalysisId(analysisId, limit);
        vo.setRuleVersionLogs(logs == null ? Collections.emptyList() : logs);
        vo.setTotal(logs == null ? 0 : logs.size());
        vo.setTruncated(Boolean.FALSE);
    }

    private void fillHotResetDetail(ReviewAggregateDetailVO vo, AnalysisRunDO run, String analysisId) {
        DecisionResult dr = decisionResultMapper.selectLatestByAnalysisId(analysisId);
        List<MissedOpportunityDO> missedRows = missedOpportunityMapper.listByAnalysisId(analysisId);
        vo.setHotReset(toHotReset(run, dr, missedRows));
        vo.setTotal(vo.getHotReset() == null ? 0 : 1);
        vo.setTruncated(Boolean.FALSE);
    }

    private static ReviewAggregateVO.ReviewClosureSummary buildReviewClosure(
            AnalysisRunDO run,
            DecisionResult decision,
            ExecutionPlanDO plan,
            List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck,
            List<ReviewAggregateVO.ReviewMissedSummary> missed,
            ReviewAggregateVO.ReviewHotResetSummary hotReset,
            List<ReviewAggregateVO.ReviewAlertSummary> alerts,
            org.example.trademodel.entity.ReviewResultDO reviewResult) {
        ReviewAggregateVO.ReviewClosureSummary s = new ReviewAggregateVO.ReviewClosureSummary();
        s.setStageLabel(buildStageLabel(plan, pushRecheck));
        s.setDecisionConclusion(buildDecisionConclusion(decision));
        s.setExecutionHeadline(buildExecutionHeadline(pushRecheck));
        s.setDeviationSignals(buildDeviationSignals(pushRecheck, missed, hotReset, alerts));
        s.setDeviationSourceTags(buildDeviationSourceTags(pushRecheck, missed, hotReset, alerts));
        s.setReviewCompletion(buildReviewCompletion(reviewResult));
        s.setEntryGuidance(buildEntryGuidance(reviewResult));
        s.setNextFocus(buildNextFocus(pushRecheck, missed, hotReset, alerts, reviewResult));
        s.setKeyFacts(buildKeyFacts(decision, plan, pushRecheck, missed, hotReset, alerts));
        return s;
    }

    private static ReviewAggregateVO.GovernanceSummary buildGovernanceSummary(
            org.example.trademodel.entity.ReviewResultDO reviewResult,
            ReviewAggregateVO.RuleVersionLogSummary linkedReviewLog) {
        ReviewAggregateVO.GovernanceSummary summary = new ReviewAggregateVO.GovernanceSummary();
        boolean hasContent = reviewResult != null && hasReviewContent(reviewResult);
        summary.setHasReviewContent(hasContent);
        summary.setPrimaryIssueType(reviewResult == null ? null : nullIfBlank(reviewResult.getErrorType()));
        summary.setLatestReviewUpdatedAt(reviewResult == null ? null : reviewResult.getUpdateTime());
        summary.setGovernanceStatus(hasContent ? "READY_FOR_GOVERNANCE_INPUT" : "REVIEW_NOTE_PENDING");
        summary.setGovernanceActionHint(buildGovernanceActionHint(reviewResult, hasContent));
        if (linkedReviewLog != null) {
            summary.setLinkedRuleLogId(linkedReviewLog.getId());
            summary.setLinkedRuleLogCreatedAt(linkedReviewLog.getCreatedAt());
            summary.setLinkedRuleLogChangeCategory(linkedReviewLog.getChangeCategory());
        }
        return summary;
    }

    private static String buildGovernanceActionHint(org.example.trademodel.entity.ReviewResultDO reviewResult, boolean hasContent) {
        if (!hasContent) {
            return "仅有事实链，尚未形成人工治理输入（请补充 review 结论）。";
        }
        String suggestion = reviewResult == null ? null : nullIfBlank(reviewResult.getAdjustmentSuggestion());
        String outcome = reviewResult == null ? null : nullIfBlank(reviewResult.getActualOutcome());
        if (suggestion != null) {
            return "建议动作已形成：优先按 adjustmentSuggestion 作为治理输入候选。";
        }
        if (outcome != null) {
            return "已记录实际结果：建议先观察并补全调整建议后再推进规则动作。";
        }
        return "已形成最小人工结论：当前更偏记录归档，可继续补充治理建议。";
    }

    private static ReviewAggregateVO.RuleVersionLogSummary markLatestReviewLinkedLog(List<ReviewAggregateVO.RuleVersionLogSummary> logs) {
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        ReviewAggregateVO.RuleVersionLogSummary linked = null;
        for (ReviewAggregateVO.RuleVersionLogSummary log : logs) {
            if (log == null) {
                continue;
            }
            boolean matched = CHANGE_CATEGORY_REVIEW_FEEDBACK_SAVED.equals(log.getChangeCategory());
            log.setLinkedToLatestReview(matched && linked == null);
            if (matched && linked == null) {
                linked = log;
            }
        }
        return linked;
    }

    private static String buildStageLabel(ExecutionPlanDO plan, List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck) {
        if (pushRecheck != null && !pushRecheck.isEmpty()) {
            return "已进入 Push / Recheck 复盘阶段";
        }
        if (plan != null) {
            return "已形成 Plan，待观察执行链";
        }
        return "以 Run / Decision 复盘为主";
    }

    private static String buildDecisionConclusion(DecisionResult decision) {
        if (decision == null) {
            return "暂无 Decision 结论";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("结论=").append(nullToDash(decision.getConclusionSummary()));
        sb.append("；是否值得开仓=").append(boolText(decision.getIsWorthOpening()));
        sb.append("；采纳状态=").append(boolText(decision.getIsAdopted()));
        return sb.toString();
    }

    private static String buildExecutionHeadline(List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck) {
        if (pushRecheck == null || pushRecheck.isEmpty()) {
            return "暂无 Push 记录，执行链尚未展开。";
        }
        int pushCount = pushRecheck.size();
        int recheckCount = 0;
        int blockedCount = 0;
        int terminatedCount = 0;
        int waitingCount = 0;
        int passCount = 0;
        for (ReviewAggregateVO.ReviewPushWithRecheck item : pushRecheck) {
            List<ReviewAggregateVO.ReviewRecheckSummary> rechecks = item.getRechecks();
            if (rechecks == null) {
                continue;
            }
            recheckCount += rechecks.size();
            for (ReviewAggregateVO.ReviewRecheckSummary recheck : rechecks) {
                PushRecheckStatusContract.ReviewTag tag = resolveReviewTag(recheck);
                if (tag == PushRecheckStatusContract.ReviewTag.BLOCKED) {
                    blockedCount++;
                }
                if (tag == PushRecheckStatusContract.ReviewTag.TERMINATED) {
                    terminatedCount++;
                }
                if (tag == PushRecheckStatusContract.ReviewTag.WAITING) {
                    waitingCount++;
                }
                if (tag == PushRecheckStatusContract.ReviewTag.PASS) {
                    passCount++;
                }
            }
        }
        return "Push " + pushCount + " 条，Recheck " + recheckCount + " 条"
                + "；通过 " + passCount + " 条"
                + "，等待 " + waitingCount + " 条"
                + "，阻断 " + blockedCount + " 条"
                + "，终止 " + terminatedCount + " 条。";
    }

    private static List<String> buildDeviationSignals(
            List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck,
            List<ReviewAggregateVO.ReviewMissedSummary> missed,
            ReviewAggregateVO.ReviewHotResetSummary hotReset,
            List<ReviewAggregateVO.ReviewAlertSummary> alerts) {
        List<String> out = new ArrayList<>();
        String recheckSignal = buildRecheckSignal(pushRecheck);
        if (recheckSignal != null) {
            out.add(recheckSignal);
        }
        if (missed != null && !missed.isEmpty()) {
            out.add("存在 Missed 记录 " + missed.size() + " 条。");
        }
        if (hotReset != null && Boolean.TRUE.equals(hotReset.getAnalysisEventRecorded())) {
            out.add("本 analysis 命中 Hot Reset 事件。");
        }
        String alertSignal = buildAlertSignal(alerts);
        if (alertSignal != null) {
            out.add(alertSignal);
        }
        if (out.isEmpty()) {
            out.add("当前未归纳出明显偏差信号，请结合下方证据区复核。");
        }
        return out;
    }

    private static List<String> buildDeviationSourceTags(
            List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck,
            List<ReviewAggregateVO.ReviewMissedSummary> missed,
            ReviewAggregateVO.ReviewHotResetSummary hotReset,
            List<ReviewAggregateVO.ReviewAlertSummary> alerts) {
        List<String> out = new ArrayList<>();
        String recheckSignal = buildRecheckSignal(pushRecheck);
        if (recheckSignal != null) {
            out.add("Recheck 阻断 / 等待 / 漂移");
        }
        if (missed != null && !missed.isEmpty()) {
            out.add("Missed");
        }
        if (hotReset != null && Boolean.TRUE.equals(hotReset.getAnalysisEventRecorded())) {
            out.add("Hot Reset");
        }
        if (alerts != null && !alerts.isEmpty()) {
            out.add("Alerts");
        }
        if (out.isEmpty()) {
            out.add("暂无明确偏差信号");
        }
        return out;
    }

    private static String buildRecheckSignal(List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck) {
        if (pushRecheck == null || pushRecheck.isEmpty()) {
            return null;
        }
        int blocked = 0;
        int terminated = 0;
        int drifted = 0;
        int waiting = 0;
        for (ReviewAggregateVO.ReviewPushWithRecheck item : pushRecheck) {
            List<ReviewAggregateVO.ReviewRecheckSummary> rechecks = item.getRechecks();
            if (rechecks == null) {
                continue;
            }
            for (ReviewAggregateVO.ReviewRecheckSummary recheck : rechecks) {
                PushRecheckStatusContract.ReviewTag tag = resolveReviewTag(recheck);
                if (tag == PushRecheckStatusContract.ReviewTag.BLOCKED) {
                    blocked++;
                }
                if (tag == PushRecheckStatusContract.ReviewTag.TERMINATED) {
                    terminated++;
                }
                if (hasMeaningfulDrift(recheck)) {
                    drifted++;
                }
                if (tag == PushRecheckStatusContract.ReviewTag.WAITING) {
                    waiting++;
                }
            }
        }
        if (blocked == 0 && drifted == 0 && waiting == 0 && terminated == 0) {
            return null;
        }
        if (blocked > 0 || terminated > 0) {
            String base = "Recheck 出现阻断 " + blocked + " 条、终止 " + terminated + " 条。";
            if (drifted > 0) {
                base += "其中价格漂移 " + drifted + " 条。";
            }
            if (waiting > 0) {
                base += "仍有等待 " + waiting + " 条。";
            }
            return base;
        }
        if (waiting > 0) {
            return "Recheck 处于等待 " + waiting + " 条。";
        }
        return "Recheck 检测到价格漂移 " + drifted + " 条。";
    }

    private static String buildAlertSignal(List<ReviewAggregateVO.ReviewAlertSummary> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return null;
        }
        int open = 0;
        int suppressed = 0;
        for (ReviewAggregateVO.ReviewAlertSummary alert : alerts) {
            if ("OPEN".equalsIgnoreCase(alert.getStatus())) {
                open++;
            } else if ("SUPPRESSED".equalsIgnoreCase(alert.getStatus())) {
                suppressed++;
            }
        }
        if (open == 0 && suppressed == 0) {
            return "存在 Alerts 记录 " + alerts.size() + " 条。";
        }
        return "存在 Alerts：OPEN " + open + " 条，SUPPRESSED " + suppressed + " 条。";
    }

    private static ReviewAggregateVO.ReviewCompletionSummary buildReviewCompletion(org.example.trademodel.entity.ReviewResultDO reviewResult) {
        ReviewAggregateVO.ReviewCompletionSummary s = new ReviewAggregateVO.ReviewCompletionSummary();
        if (reviewResult == null) {
            s.setStatus("EMPTY");
            s.setCompleted(Boolean.FALSE);
            s.setHasContent(Boolean.FALSE);
            s.setSummary("未填写人工复盘结论");
            return s;
        }
        s.setUpdateTime(reviewResult.getUpdateTime());
        boolean hasContent = hasReviewContent(reviewResult);
        s.setStatus(hasContent ? "FILLED" : "EMPTY");
        s.setCompleted(hasContent);
        s.setHasContent(hasContent);
        if (hasContent) {
            s.setSummary("已填写人工复盘结论，最近更新时间=" + reviewResult.getUpdateTime());
        } else if (reviewResult.getUpdateTime() != null) {
            s.setSummary("未填写人工复盘结论，最近更新时间=" + reviewResult.getUpdateTime());
        } else {
            s.setSummary("未填写人工复盘结论");
        }
        return s;
    }

    private static boolean hasReviewContent(org.example.trademodel.entity.ReviewResultDO reviewResult) {
        return hasText(reviewResult.getErrorType())
                || hasText(reviewResult.getActualOutcome())
                || hasText(reviewResult.getAdjustmentSuggestion());
    }

    private static String buildEntryGuidance(org.example.trademodel.entity.ReviewResultDO reviewResult) {
        if (reviewResult != null && hasReviewContent(reviewResult)) {
            return "当前已存在人工复盘结论；本区录入时优先核对本次结论是否仍与下方事实链一致，再决定是否补充修正。";
        }
        return "请先根据闭环总览确认结论、执行链阶段与偏差信号来源，再结合下方证据区填写 errorType / actualOutcome / adjustmentSuggestion。";
    }

    private static List<String> buildNextFocus(
            List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck,
            List<ReviewAggregateVO.ReviewMissedSummary> missed,
            ReviewAggregateVO.ReviewHotResetSummary hotReset,
            List<ReviewAggregateVO.ReviewAlertSummary> alerts,
            org.example.trademodel.entity.ReviewResultDO reviewResult) {
        List<String> out = new ArrayList<>();
        if (reviewResult == null || !hasReviewContent(reviewResult)) {
            out.add("先完成人工复盘录入，沉淀 errorType / actualOutcome / adjustmentSuggestion。");
        }
        if (buildRecheckSignal(pushRecheck) != null) {
            out.add("优先核对 Push / Recheck 的阻断、等待/漂移与失败原因。");
        }
        if (missed != null && !missed.isEmpty()) {
            out.add("对照 Missed.reasonView，确认这次机会为何未落地。");
        }
        if (hotReset != null && Boolean.TRUE.equals(hotReset.getAnalysisEventRecorded())) {
            out.add("核对 Hot Reset 事件与本次 decision / state 之间的关系。");
        }
        if (alerts != null && !alerts.isEmpty()) {
            out.add("对照 Alerts 的 OPEN / SUPPRESSED 状态，确认是否存在告警侧偏差。");
        }
        if (out.isEmpty()) {
            out.add("按下方事实区核对 Decision、Plan 与执行链是否一致。");
        }
        return out;
    }

    private static List<ReviewAggregateVO.ReviewFactRef> buildKeyFacts(
            DecisionResult decision,
            ExecutionPlanDO plan,
            List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck,
            List<ReviewAggregateVO.ReviewMissedSummary> missed,
            ReviewAggregateVO.ReviewHotResetSummary hotReset,
            List<ReviewAggregateVO.ReviewAlertSummary> alerts) {
        List<ReviewAggregateVO.ReviewFactRef> out = new ArrayList<>();
        if (decision != null) {
            out.add(factRef("Decision", "sec-decision", "先确认本次分析的核心结论、开仓价值与失效条件。"));
        }
        if (plan != null) {
            out.add(factRef("Plan", "sec-plan", "核对推荐动作、入场区、止损和止盈规则是否与结论一致。"));
        }
        if (pushRecheck != null && !pushRecheck.isEmpty()) {
            out.add(factRef("Push / Recheck", "sec-push", "核对执行链走到哪一步，以及是否出现阻断、等待、失败或价格漂移。"));
        }
        if (missed != null && !missed.isEmpty()) {
            out.add(factRef("Missed", "sec-missed", "确认本次机会为何未落地，是否属于明确偏差来源。"));
        }
        if (hotReset != null && Boolean.TRUE.equals(hotReset.getAnalysisEventRecorded())) {
            out.add(factRef("Hot Reset", "sec-hot-reset", "确认是否存在重置事件，以及它与本次 analysis 的关系。"));
        }
        if (alerts != null && !alerts.isEmpty()) {
            out.add(factRef("Alerts", "sec-alerts", "对照 OPEN / SUPPRESSED 告警，确认是否存在监控侧异常信号。"));
        }
        if (out.isEmpty()) {
            out.add(factRef("Run", "sec-run", "当前主要依据 Run 基础信息做复盘起点判断。"));
        }
        return out;
    }

    private static boolean hasBlockingRecheck(ReviewAggregateVO.ReviewRecheckSummary recheck) {
        if (recheck == null || recheck.getRecheckStatus() == null) {
            return false;
        }
        PushRecheckStatusContract.ReviewTag tag = resolveReviewTag(recheck);
        return tag == PushRecheckStatusContract.ReviewTag.BLOCKED
                || tag == PushRecheckStatusContract.ReviewTag.TERMINATED;
    }

    private static boolean isValidWaitingRecheck(ReviewAggregateVO.ReviewRecheckSummary recheck) {
        if (recheck == null || recheck.getRecheckStatus() == null) {
            return false;
        }
        return resolveReviewTag(recheck) == PushRecheckStatusContract.ReviewTag.WAITING;
    }

    private static boolean hasMeaningfulDrift(ReviewAggregateVO.ReviewRecheckSummary recheck) {
        // 只认 DRIFTED 枚举，避免 price_drift_ratio 在阈值内也可能非 0 的噪声。
        if (recheck == null || recheck.getRecheckStatus() == null) {
            return false;
        }
        return PushRecheckStatusContract.tryParseRecheckStatus(recheck.getRecheckStatus()) == RecheckStatusEnum.DRIFTED;
    }

    private static PushRecheckStatusContract.ReviewTag resolveReviewTag(ReviewAggregateVO.ReviewRecheckSummary recheck) {
        if (recheck == null) {
            return PushRecheckStatusContract.ReviewTag.BLOCKED;
        }
        return PushRecheckStatusContract.toReviewTagByRecheckRaw(recheck.getRecheckStatus());
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ReviewAggregateVO.ReviewFactRef factRef(String label, String anchor, String reason) {
        ReviewAggregateVO.ReviewFactRef ref = new ReviewAggregateVO.ReviewFactRef();
        ref.setLabel(label);
        ref.setAnchor(anchor);
        ref.setReason(reason);
        return ref;
    }

    private static String boolText(Boolean value) {
        if (value == null) {
            return "—";
        }
        return Boolean.TRUE.equals(value) ? "是" : "否";
    }

    private List<ReviewAggregateVO.ReviewPushWithRecheck> buildPushRecheck(String analysisId) {
        List<TmPushSnapshotDO> snaps = pushSnapshotMapper.listByAnalysisId(analysisId);
        if (snaps == null || snaps.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewAggregateVO.ReviewPushWithRecheck> out = new ArrayList<>(snaps.size());
        for (TmPushSnapshotDO s : snaps) {
            ReviewAggregateVO.ReviewPushWithRecheck bundle = new ReviewAggregateVO.ReviewPushWithRecheck();
            TmAccountRiskSnapshotDO risk = s.getAccountRiskSnapshotId() == null
                    ? null
                    : accountRiskSnapshotMapper.selectById(s.getAccountRiskSnapshotId());
            bundle.setPush(toPushSummary(s, risk));
            bundle.setRechecks(toRecheckList(pushRecheckLogMapper.selectByPushId(s.getPushId())));
            out.add(bundle);
        }
        return out;
    }

    private static ReviewAggregateVO.ReviewRunSummary toRunSummary(AnalysisRunDO r) {
        ReviewAggregateVO.ReviewRunSummary s = new ReviewAggregateVO.ReviewRunSummary();
        s.setAnalysisId(r.getAnalysisId());
        s.setSymbol(r.getSymbol());
        s.setTimeframe(r.getTimeframe());
        s.setAnalysisTime(r.getAnalysisTime());
        s.setRuleVersion(r.getRuleVersion());
        s.setDataQualityScore(r.getDataQualityScore());
        s.setTraceId(r.getTraceId());
        s.setStatus(r.getStatus());
        return s;
    }

    private static ReviewAggregateVO.ReviewDecisionSummary toDecisionSummary(DecisionResult d) {
        ReviewAggregateVO.ReviewDecisionSummary s = new ReviewAggregateVO.ReviewDecisionSummary();
        s.setDecisionId(d.getDecisionId());
        s.setSymbol(d.getSymbol());
        s.setMarketBiasHierarchy(d.getMarketBiasHierarchy());
        s.setTradeType(d.getTradeType());
        s.setConfidenceLevel(d.getConfidenceLevel());
        s.setRiskLevel(d.getRiskLevel());
        s.setActionPriority(d.getActionPriority());
        s.setConclusionSummary(d.getConclusionSummary());
        s.setIsWorthOpening(d.getIsWorthOpening());
        s.setMultiTfConvergence(d.getMultiTfConvergence());
        s.setIsAdopted(d.getIsAdopted());
        s.setValidPeriod(d.getValidPeriod());
        s.setInvalidCondition(d.getInvalidCondition());
        s.setEvidenceSummary(d.getEvidenceSummary());
        s.setExplanationJson(d.getExplanationJson());
        s.setReviewReasons(d.getReviewReasons());
        s.setAiConflictLevel(d.getAiConflictLevel());
        s.setAiConflictScore(d.getAiConflictScore());
        s.setAiPlanMode(d.getAiPlanMode());
        s.setConfusedScore(d.getConfusedScore());
        s.setAssetStateSnapshot(d.getAssetStateSnapshot());
        s.setCreateTime(d.getCreateTime());
        return s;
    }

    private static ReviewAggregateVO.ReviewPlanSummary toPlanSummary(ExecutionPlanDO p) {
        ReviewAggregateVO.ReviewPlanSummary s = new ReviewAggregateVO.ReviewPlanSummary();
        s.setPlanId(p.getPlanId());
        s.setPlanMode(p.getPlanMode());
        s.setRecommendedAction(p.getRecommendedAction());
        s.setEntryZone(p.getEntryZone());
        s.setStopLoss(p.getStopLoss());
        s.setTakeProfitRules(p.getTakeProfitRules());
        s.setLeverageSuggestion(p.getLeverageSuggestion());
        s.setPositionSuggestion(p.getPositionSuggestion());
        s.setCreateTime(p.getCreateTime());
        return s;
    }

    private static ReviewAggregateVO.ReviewMarketEnvironmentSummary toMarketEnvironmentSummary(MarketEnvironmentSnapshotDO row) {
        if (row == null) {
            return null;
        }
        ReviewAggregateVO.ReviewMarketEnvironmentSummary s = new ReviewAggregateVO.ReviewMarketEnvironmentSummary();
        s.setSummary(row.getSummary());
        s.setSourceType(row.getSourceType());
        s.setEnvironmentType(row.getEnvironmentType());
        s.setRiskMode(row.getRiskMode());
        return s;
    }

    private static ReviewAggregateVO.ReviewPushSummary toPushSummary(TmPushSnapshotDO p, TmAccountRiskSnapshotDO risk) {
        ReviewAggregateVO.ReviewPushSummary s = new ReviewAggregateVO.ReviewPushSummary();
        s.setPushId(p.getPushId());
        s.setAnalysisId(p.getAnalysisId());
        s.setSymbol(p.getSymbol());
        s.setTimeframe(p.getTimeframe());
        s.setPushType(p.getPushType());
        s.setPushStatus(p.getPushStatus());
        s.setPushCreateTime(p.getPushCreateTime());
        s.setRuleVersion(p.getRuleVersion());
        s.setTriggerPrice(p.getTriggerPrice());
        s.setEntryZoneJson(p.getEntryZoneJson());
        s.setStopZoneJson(p.getStopZoneJson());
        s.setInvalidationConditionJson(p.getInvalidationConditionJson());
        s.setPlanModeSnapshot(p.getPlanModeSnapshot());
        s.setCauseEffectAlignmentSnapshot(p.getCauseEffectAlignmentSnapshot());
        s.setExecutionFeasibilitySnapshot(p.getExecutionFeasibilitySnapshot());
        s.setDataQualityScoreSnapshot(p.getDataQualityScoreSnapshot());
        s.setConfusedScoreSnapshot(p.getConfusedScoreSnapshot());
        s.setAccountRiskSnapshotId(p.getAccountRiskSnapshotId());
        if (risk != null) {
            s.setAccountRiskAllowed(risk.getRiskAllowed());
            s.setRiskLevelSnapshot(risk.getRiskLevelSnapshot());
            s.setRiskReasonCode(risk.getRiskReasonCode());
            s.setRiskReasonText(risk.getRiskReasonText());
            s.setPositionExposure(risk.getPositionExposure());
            s.setMaxAllowedExposure(risk.getMaxAllowedExposure());
            s.setSnapshotSource(risk.getSnapshotSource());
            s.setSnapshotVersion(risk.getSnapshotVersion());
        }
        s.setExpiresAt(p.getExpiresAt());
        s.setTraceId(p.getTraceId());
        s.setCreateTime(p.getCreateTime());
        return s;
    }

    private static List<ReviewAggregateVO.ReviewRecheckSummary> toRecheckList(List<TmPushRecheckLogDO> logs) {
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewAggregateVO.ReviewRecheckSummary> out = new ArrayList<>(logs.size());
        for (TmPushRecheckLogDO l : logs) {
            out.add(toRecheckSummary(l));
        }
        return out;
    }

    private static ReviewAggregateVO.ReviewRecheckSummary toRecheckSummary(TmPushRecheckLogDO l) {
        ReviewAggregateVO.ReviewRecheckSummary s = new ReviewAggregateVO.ReviewRecheckSummary();
        s.setLogId(l.getLogId());
        s.setPushId(l.getPushId());
        s.setRecheckTime(l.getRecheckTime());
        s.setRecheckStatus(l.getRecheckStatus());
        s.setCurrentPrice(l.getCurrentPrice());
        s.setPriceDriftRatio(l.getPriceDriftRatio());
        s.setCurrentSlippageEstimation(l.getCurrentSlippageEstimation());
        s.setCurrentDataQualityScore(l.getCurrentDataQualityScore());
        s.setCurrentConfusedScore(l.getCurrentConfusedScore());
        s.setCurrentAccountRiskAllowed(l.getCurrentAccountRiskAllowed());
        s.setFailReasonJson(l.getFailReasonJson());
        s.setCreateTime(l.getCreateTime());
        return s;
    }

    private static List<ReviewAggregateVO.ReviewMissedSummary> toMissedList(List<MissedOpportunityDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewAggregateVO.ReviewMissedSummary> out = new ArrayList<>(rows.size());
        for (MissedOpportunityDO m : rows) {
            ReviewAggregateVO.ReviewMissedSummary s = new ReviewAggregateVO.ReviewMissedSummary();
            s.setMissedId(m.getMissedId());
            s.setDecisionId(m.getDecisionId());
            s.setAnalysisId(m.getAnalysisId());
            s.setSymbol(m.getSymbol());
            s.setBizDate(m.getBizDate());
            s.setReasonJson(m.getReasonJson());
            s.setReasonView(MissedReasonViewParser.parse(m.getReasonJson()));
            s.setRuleVersion(m.getRuleVersion());
            s.setTraceId(m.getTraceId());
            s.setCreateTime(m.getCreateTime());
            out.add(s);
        }
        return out;
    }

    private ReviewAggregateVO.ReviewHotResetSummary toHotReset(
            AnalysisRunDO run, DecisionResult decision, List<MissedOpportunityDO> missedRows) {
        if (run == null || run.getSymbol() == null || run.getSymbol().isBlank()) {
            return null;
        }
        String symbol = run.getSymbol().trim();
        AssetStateDO row = assetStateMapper.selectBySymbol(symbol);
        if (row == null) {
            return null;
        }
        ReviewAggregateVO.ReviewHotResetSummary s = new ReviewAggregateVO.ReviewHotResetSummary();
        s.setSymbol(row.getSymbol());
        s.setState(row.getState() != null ? row.getState().name() : null);
        s.setConfusedScore(row.getConfusedScore());
        s.setHotResetFlag(row.getHotResetFlag());
        s.setHotResetTriggerType(row.getHotResetTriggerType());
        s.setHotResetTriggerValue(row.getHotResetTriggerValue());
        s.setHotResetTime(row.getHotResetTime());
        s.setPreResetState(row.getPreResetState());
        s.setPostResetState(row.getPostResetState());
        s.setLastUpdateTime(row.getLastUpdateTime());
        HotResetEventDO event = hotResetEventMapper.selectLatestByAnalysisId(run.getAnalysisId());
        s.setAnalysisEventRecorded(event != null);
        if (event != null) {
            s.setAnalysisEventId(event.getEventId());
            s.setAnalysisEventTraceId(event.getTraceId());
            s.setAnalysisEventTriggerType(event.getTriggerType());
            s.setAnalysisEventTriggerValue(event.getTriggerValue());
            s.setAnalysisEventDecisionId(event.getDecisionId());
            s.setAnalysisEventDecisionState(event.getDecisionState());
            s.setAnalysisEventConfusedScoreSnapshot(event.getConfusedScoreSnapshot());
            s.setAnalysisEventMultiTimeframeAlignedSnapshot(event.getMultiTimeframeAlignedSnapshot());
            s.setAnalysisEventTriggerReasonCode(event.getTriggerReasonCode());
            s.setAnalysisEventTriggerReasonText(event.getTriggerReasonText());
            s.setAnalysisEventVersion(event.getEventVersion());
            s.setAnalysisEventTime(event.getEventTime());
            s.setAnalysisEventPreState(event.getPreState());
            s.setAnalysisEventPostState(event.getPostState());
        }

        s.setSemanticScope(ReviewAggregateVO.ReviewHotResetSummary.SEMANTIC_SCOPE_CURRENT_ROW_BY_SYMBOL);
        s.setScopeExplanationZh(
                "数据来自 tm_asset_state 按本 run 的 symbol 查询的「当前一行」。"
                        + "其中 state、confusedScore 会随后续分析被覆盖更新；"
                        + "hot_reset_* 与 pre/post 记录的是该表上最近一次 Hot Reset 写入的元数据，"
                        + "不是按 analysisId 单独归档的事件流水，也不能单独还原「仅属于本次 analysis」的完整时序。");
        s.setPrePostStateMeaningZh(
                "preResetState：触发最近一次 Hot Reset 时，该标的在权威表中的资产状态（枚举名）。"
                        + " postResetState：Hot Reset 写入后目标状态（主链常见为 OBSERVING）。"
                        + " 二者描述的是「那一次 Hot Reset」的前后边界，不是本区其它字段的通用释义。");
        s.setRelationToThisAnalysisZh(buildHotResetRelationZh(run, decision, row));
        s.setMissedRelationHintZh(buildMissedHotResetHintZh(missedRows));
        return s;
    }

    private static String buildHotResetRelationZh(AnalysisRunDO run, DecisionResult decision, AssetStateDO row) {
        StringBuilder sb = new StringBuilder();
        sb.append("本复盘 analysisId=").append(nullToEmpty(run.getAnalysisId()));
        sb.append("；symbol=").append(nullToEmpty(run.getSymbol()));
        sb.append("。本节与「本次 decision」的关系：decision.assetStateSnapshot / confusedScore 是决策落库时的快照；");
        sb.append("本节为权威表当前行，二者不是同一对象。");
        if (decision != null) {
            Integer dcs = decision.getConfusedScore();
            Integer rcs = row.getConfusedScore();
            if (dcs != null && rcs != null && !dcs.equals(rcs)) {
                sb.append(" 当前行 confusedScore=").append(rcs).append(" 与本次 decision 的 ").append(dcs).append(" 不一致时，通常表示行在后续运行中被更新。");
            } else if (dcs != null && rcs != null) {
                sb.append(" 当前行 confusedScore 与本次 decision 一致或可比，仍不代表「仅本次 analysis」的冻结历史。");
            }
        }
        if (row.getHotResetTime() == null) {
            sb.append(" hotResetTime 为空：可能尚未发生 Hot Reset，或该列尚未写入；其它字段仍表示当前行快照。");
        } else if (run.getAnalysisTime() != null) {
            if (row.getHotResetTime().isBefore(run.getAnalysisTime())) {
                sb.append(" hotResetTime 早于本次 analysisTime：该次 Hot Reset 发生在本次分析时间之前，不要把本节误读为「本次 analysis 专属刚发生事件」。");
            } else {
                sb.append(" hotResetTime 不早于本次 analysisTime：可能与本次或之后流水线写入有关，但仍仅代表当前库内状态，非独立审计流水。");
            }
        }
        return sb.toString();
    }

    private static String buildMissedHotResetHintZh(List<MissedOpportunityDO> missedRows) {
        if (missedRows == null || missedRows.isEmpty()) {
            return "本 analysis 下无 missed 记录：无法从 missed.reasonJson 读取旁证；不据此推断 Hot Reset 是否命中，请结合 decision 与主链。";
        }
        Boolean would = tryParseHotResetWouldFire(missedRows.get(0).getReasonJson());
        if (Boolean.FALSE.equals(would)) {
            return "存在 missed 记录：reasonJson.facts.hotResetWouldFire=false 与「主链未因 Hot Reset 跳过 missed」一致；"
                    + "与本节展示的当前资产行无逐条绑定。";
        }
        if (Boolean.TRUE.equals(would)) {
            return "存在 missed 记录：reasonJson.facts.hotResetWouldFire=true（若与主链预期不符请核对数据）；仅只读旁证。";
        }
        return "存在 missed 记录：reasonJson 中未能解析 facts.hotResetWouldFire；请以 decision 与主链为准。";
    }

    private static Boolean tryParseHotResetWouldFire(String reasonJson) {
        if (reasonJson == null || reasonJson.isBlank()) {
            return null;
        }
        try {
            JsonNode n = JSON.readTree(reasonJson);
            JsonNode f = n.path("facts").path("hotResetWouldFire");
            if (f.isBoolean()) {
                return f.booleanValue();
            }
        } catch (Exception ignored) {
            // 保持 null，由文案提示无法解析
        }
        return null;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static List<ReviewAggregateVO.ReviewAlertSummary> toAlertList(List<MonitorAlertDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewAggregateVO.ReviewAlertSummary> out = new ArrayList<>(rows.size());
        for (MonitorAlertDO a : rows) {
            ReviewAggregateVO.ReviewAlertSummary s = new ReviewAggregateVO.ReviewAlertSummary();
            s.setId(a.getId());
            s.setAlertType(a.getAlertType());
            s.setAlertLevel(a.getAlertLevel());
            s.setAlertMessage(a.getAlertMessage());
            s.setStatus(a.getStatus());
            s.setCooldownUntil(a.getCooldownUntil());
            s.setSuppressReason(a.getSuppressReason());
            s.setCreatedAt(a.getCreatedAt());
            out.add(s);
        }
        return out;
    }
}
