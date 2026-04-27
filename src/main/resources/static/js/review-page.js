(function () {
    'use strict';

    var analysisId = window.__REVIEW_ANALYSIS_ID__ || '';
    var banner = document.getElementById('status-banner');
    var root = document.getElementById('review-root');
    var closureRoot = document.getElementById('review-closure-root');
    var entryContext = document.getElementById('review-entry-context');
    var currentAggregate = null;

    /**
     * 复盘错误类型中文标签（与 ReviewErrorType / PROJECT_SPEC 二十一（四）一致）；仅用于展示，接口与保存仍为原始码值。
     */
    var REVIEW_ERROR_TYPE_LABEL_ZH = {
        DATA_ISSUE: '数据缺失/延迟/异常',
        RULE_TOO_LOOSE: '规则过松',
        RULE_TOO_STRICT: '规则过严',
        TF_CONFLICT_MISJUDGED: '多周期冲突误判',
        EVENT_RISK_UNDERWEIGHT: '事件权重低估',
        LEVERAGE_RISK_UNDERWEIGHT: '杠杆风险低估',
        LIQUIDITY_TRAP_MISSED: '插针/流动性扫荡漏判',
        AI_OVERRULE_BIAS: 'AI 辅助结论偏移',
        PLAN_EXECUTION_MISMATCH: '计划和市场演化不匹配',
        UNKNOWN: '未知原因'
    };

    function formatReviewErrorTypeForDisplay(raw) {
        if (raw === null || raw === undefined) {
            return null;
        }
        var code = String(raw).trim();
        if (!code) {
            return '';
        }
        var zh = REVIEW_ERROR_TYPE_LABEL_ZH[code];
        if (!zh) {
            return code;
        }
        return zh + '（' + code + '）';
    }

    function text(s) {
        if (s === null || s === undefined) return '—';
        return String(s);
    }

    function textOrDash(s) {
        if (s === null || s === undefined) return '—';
        var t = String(s).trim();
        return t ? t : '—';
    }

    /** 与 Dashboard detail `displayMiniField` 等价：空/空白 -> — */
    function displayMarketEnvMiniField(raw) {
        var trim = raw != null ? String(raw).trim() : '';
        return trim ? trim : '—';
    }

    /** 与 Dashboard `mapMarketEnvironmentSourceType` 一致 */
    function mapMarketEnvironmentSourceType(sourceTypeRaw) {
        var sourceType = sourceTypeRaw != null ? String(sourceTypeRaw).trim() : '';
        if (!sourceType) return '—';
        if (sourceType === 'BINANCE_24H_HEURISTIC') return '24h 启发式';
        if (sourceType === 'BINANCE_SPOT_PERP_MIN_HEURISTIC') return '现货+资金费率启发式';
        if (sourceType === 'BINANCE_USDM_OI_MIN_HEURISTIC') return '现货+未平仓启发式';
        if (sourceType === 'BINANCE_SPOT_PERP_OI_MIN_HEURISTIC') return '现货+资金费率+未平仓启发式';
        if (sourceType === 'PLACEHOLDER_FALLBACK') return '占位回退';
        return sourceType;
    }

    /** 极小只读块：与 Dashboard s1 marketEnvironmentMini 同构四字段；me 为 null 时字段均为 — */
    function renderMarketEnvironmentMini(me) {
        var box = el('div', { className: 'review-market-env-mini' });
        box.appendChild(el('p', { className: 'review-me-title', textContent: '市场环境（analysis 对齐快照）' }));
        var dl = el('dl', { className: 'mini-kv' });
        var pairs = [
            ['摘要', displayMarketEnvMiniField(me && me.summary)],
            ['环境类型', displayMarketEnvMiniField(me && me.environmentType)],
            ['风险模式', displayMarketEnvMiniField(me && me.riskMode)],
            ['来源', mapMarketEnvironmentSourceType(me && me.sourceType)]
        ];
        pairs.forEach(function (row) {
            dl.appendChild(el('dt', { textContent: row[0] }));
            dl.appendChild(el('dd', { textContent: row[1] }));
        });
        box.appendChild(dl);
        box.appendChild(el('p', {
            className: 'structured-evidence-hint muted',
            textContent: '仅用于本次 analysis 对齐展示，不代表完整市场环境模块'
        }));
        return box;
    }

    function el(tag, attrs, children) {
        var n = document.createElement(tag);
        if (attrs) {
            Object.keys(attrs).forEach(function (k) {
                if (k === 'className') n.className = attrs[k];
                else if (k === 'textContent') n.textContent = attrs[k];
                else n.setAttribute(k, attrs[k]);
            });
        }
        (children || []).forEach(function (c) {
            if (c) n.appendChild(c);
        });
        return n;
    }

    function section(title, bodyEl, sectionId) {
        var s = el('section', { className: 'section anchor-target' });
        if (sectionId) {
            s.id = sectionId;
        }
        s.appendChild(el('h2', { textContent: title }));
        s.appendChild(bodyEl);
        return s;
    }

    /** 无数据区块：标题 + 简短说明 */
    function emptyPanel(lead, detail) {
        var p = el('p', { className: 'empty-panel' });
        p.appendChild(el('strong', { textContent: lead }));
        if (detail) {
            p.appendChild(document.createTextNode(' ' + detail));
        }
        return p;
    }

    function kvRows(pairs) {
        var dl = el('dl', { className: 'kv' });
        pairs.forEach(function (p) {
            dl.appendChild(el('dt', { textContent: p[0] }));
            var dd = el('dd');
            if (p.length > 2 && p[2] === 'node' && p[3]) {
                dd.appendChild(p[3]);
            } else {
                dd.textContent = p[1];
                if (p.length > 2 && p[2] === 'raw') {
                    dd.className = 'raw-json';
                }
            }
            dl.appendChild(dd);
        });
        return dl;
    }

    function parseJsonObject(str) {
        if (str === null || str === undefined || str === '') {
            return null;
        }
        var raw = String(str).trim();
        if (!raw || raw === 'null') {
            return null;
        }
        try {
            var parsed = JSON.parse(raw);
            return (parsed && typeof parsed === 'object') ? parsed : null;
        } catch (e) {
            return null;
        }
    }

    function toListByDelimiters(str) {
        if (str === null || str === undefined) return [];
        var s = String(str).trim();
        if (!s || s === '—') return [];
        if (s.indexOf('\n') >= 0) {
            var lines = s.split('\n').map(function (x) { return x.trim(); }).filter(Boolean);
            return lines.length > 0 ? lines : [s];
        }
        if (s.indexOf(';') >= 0 || s.indexOf('；') >= 0) {
            var semis = s.split(/[;；]/).map(function (x) { return x.trim(); }).filter(Boolean);
            return semis.length > 1 ? semis : [s];
        }
        return [s];
    }

    function renderSimpleList(items, cls) {
        if (!items || items.length === 0) {
            return el('span', { textContent: '—' });
        }
        var ul = el('ul', { className: cls || 'simple-list' });
        items.forEach(function (it) {
            ul.appendChild(el('li', { textContent: String(it) }));
        });
        return ul;
    }

    function renderTagList(items, cls) {
        if (!items || items.length === 0) {
            return el('span', { textContent: '—' });
        }
        var wrap = el('div', { className: cls || 'tag-list' });
        items.forEach(function (it) {
            wrap.appendChild(el('span', { className: 'tag-chip', textContent: String(it) }));
        });
        return wrap;
    }

    function isFilledReviewState(s) {
        if (!s) return false;
        return [s.errorType, s.actualOutcome, s.adjustmentSuggestion].some(function (v) {
            return v !== null && v !== undefined && String(v).trim() !== '';
        });
    }

    function buildReviewCompletionFromState(s) {
        if (!s) {
            return {
                status: 'EMPTY',
                completed: false,
                hasContent: false,
                updateTime: null,
                summary: '未填写人工复盘结论'
            };
        }
        var filled = isFilledReviewState(s);
        var summary = filled ? '已填写人工复盘结论' : '未填写人工复盘结论';
        if (s.updateTime) {
            summary += '，最近更新时间=' + text(s.updateTime);
        }
        return {
            status: filled ? 'FILLED' : 'EMPTY',
            completed: filled,
            hasContent: filled,
            updateTime: s.updateTime || null,
            summary: summary
        };
    }

    function buildEntryGuidanceFromCompletion(completion) {
        if (completion && completion.hasContent) {
            return '当前已存在人工复盘结论；本区录入时优先核对本次结论是否仍与下方事实链一致，再决定是否补充修正。';
        }
        return '请先根据闭环总览确认结论、执行链阶段与偏差信号来源，再结合下方证据区填写 errorType / actualOutcome / adjustmentSuggestion。';
    }

    function jumpToAnchor(anchor) {
        if (!anchor) return;
        var target = document.getElementById(anchor);
        if (!target) return;
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        try {
            history.replaceState(null, '', '#' + anchor);
        } catch (err) { /* ignore */ }
    }

    function renderObjectPreview(obj) {
        if (!obj || typeof obj !== 'object') {
            return el('span', { textContent: '—' });
        }
        var keys = Object.keys(obj);
        if (keys.length === 0) {
            return el('span', { textContent: '—' });
        }
        var dl = el('dl', { className: 'mini-kv' });
        keys.slice(0, 10).forEach(function (k) {
            dl.appendChild(el('dt', { textContent: k }));
            var v = obj[k];
            var textValue;
            if (v === null || v === undefined) textValue = '—';
            else if (typeof v === 'object') textValue = JSON.stringify(v);
            else textValue = String(v);
            dl.appendChild(el('dd', { textContent: textValue }));
        });
        if (keys.length > 10) {
            dl.appendChild(el('dt', { textContent: '...' }));
            dl.appendChild(el('dd', { textContent: '其余字段见下方原始 JSON' }));
        }
        return dl;
    }

    function parseNumberLike(v) {
        if (v === null || v === undefined || v === '') return null;
        var n = Number(v);
        return Number.isFinite(n) ? n : null;
    }

    function formatDelta(current, base) {
        if (current === null || base === null) {
            return '暂无对比值';
        }
        var d = current - base;
        var s = d > 0 ? '+' : '';
        return s + String(d);
    }

    function createDetails(title, innerNode, openByDefault) {
        var details = el('details', { className: 'simple-details' });
        if (openByDefault) details.open = true;
        details.appendChild(el('summary', { textContent: title }));
        var body = el('div', { className: 'simple-details-body' });
        if (innerNode) body.appendChild(innerNode);
        details.appendChild(body);
        return details;
    }

    function renderSignalCards(items) {
        var grid = el('div', { className: 'decision-summary-grid' });
        items.forEach(function (pair) {
            var card = el('div', { className: 'signal-card' });
            card.appendChild(el('div', { className: 'signal-k', textContent: pair[0] }));
            card.appendChild(el('div', { className: 'signal-v', textContent: pair[1] }));
            grid.appendChild(card);
        });
        return grid;
    }

    function renderAuditLineList(lines) {
        if (!lines || lines.length === 0) {
            return el('span', { textContent: '—' });
        }
        var ul = el('ul', { className: 'simple-list' });
        lines.forEach(function (line) {
            ul.appendChild(el('li', { textContent: line }));
        });
        return ul;
    }

    function buildDecisionAuditLines(d, explanationObj, assetStateObj, reasons, evidenceItems) {
        var lines = [];
        lines.push('reviewReasons ' + reasons.length + ' 条，evidenceSummary ' + evidenceItems.length + ' 条。');
        if (d.multiTfConvergence || d.aiConflictLevel || d.confusedScore !== null && d.confusedScore !== undefined) {
            lines.push('多周期/冲突/困惑快照：multiTf=' + textOrDash(d.multiTfConvergence)
                + '，aiConflict=' + textOrDash(d.aiConflictLevel)
                + '，confusedScore=' + textOrDash(d.confusedScore) + '。');
        }
        if (explanationObj && typeof explanationObj === 'object') {
            var eKeys = Object.keys(explanationObj);
            if (eKeys.length > 0) {
                lines.push('explanationJson 首层字段：' + eKeys.slice(0, 6).join(', ') + (eKeys.length > 6 ? ' ...' : '') + '。');
            }
        }
        if (assetStateObj && typeof assetStateObj === 'object') {
            var stateLike = assetStateObj.state || assetStateObj.assetState || assetStateObj.status;
            var confusedLike = assetStateObj.confusedScore;
            if (stateLike !== undefined || confusedLike !== undefined) {
                lines.push('assetStateSnapshot 关键快照：state=' + textOrDash(stateLike) + '，confusedScore=' + textOrDash(confusedLike) + '。');
            }
        }
        if (d.validPeriod || d.invalidCondition) {
            lines.push('有效期/失效条件：validPeriod=' + textOrDash(d.validPeriod) + '，invalidCondition=' + textOrDash(d.invalidCondition) + '。');
        }
        return lines;
    }

    /** 库内多为 JSON 数组字符串；失败则当作单行文本。 */
    function parseReviewReasons(str) {
        if (str === null || str === undefined || str === '') {
            return [];
        }
        var s = String(str).trim();
        if (!s || s === 'null') {
            return [];
        }
        try {
            var v = JSON.parse(s);
            if (Array.isArray(v)) {
                return v.map(function (x) {
                    return String(x);
                });
            }
            return [String(v)];
        } catch (e) {
            return [s];
        }
    }

    /** Missed reason_json：合法 JSON 则最小格式化，否则原样（与 failReasonJson 列策略一致）。 */
    function tryPrettyJsonString(s) {
        if (s === null || s === undefined) {
            return '';
        }
        var raw = String(s).trim();
        if (!raw) {
            return '';
        }
        try {
            return JSON.stringify(JSON.parse(raw), null, 2);
        } catch (e) {
            return raw;
        }
    }

    /** 合法 JSON 则缩进 2 格；否则原样字符串（不做树形展开）。 */
    function formatExplanationJson(str) {
        if (str === null || str === undefined || str === '') {
            return null;
        }
        var s = String(str).trim();
        if (!s) {
            return null;
        }
        try {
            return JSON.stringify(JSON.parse(s), null, 2);
        } catch (e) {
            return s;
        }
    }

    function renderRun(run) {
        if (!run) {
            return section('1. Run 摘要', emptyPanel('暂无 Run 数据', '聚合结果中未包含 run 行。'), 'sec-run');
        }
        return section('1. Run 摘要', kvRows([
            ['analysisId', text(run.analysisId)],
            ['symbol', text(run.symbol)],
            ['timeframe', text(run.timeframe)],
            ['analysisTime', text(run.analysisTime)],
            ['ruleVersion', text(run.ruleVersion)],
            ['dataQualityScore', text(run.dataQualityScore)],
            ['traceId', text(run.traceId)],
            ['status', text(run.status)]
        ]), 'sec-run');
    }

    function renderClosureSummary(summary) {
        if (!summary) {
            return section('0. 复盘闭环总览', emptyPanel('暂无闭环摘要', '聚合结果中未包含 reviewClosure。'), 'sec-closure');
        }
        var reviewCompletion = summary.reviewCompletion || {};
        var deviationSignals = Array.isArray(summary.deviationSignals) ? summary.deviationSignals : [];
        var deviationSourceTags = Array.isArray(summary.deviationSourceTags) ? summary.deviationSourceTags : [];
        var nextFocus = Array.isArray(summary.nextFocus) ? summary.nextFocus : [];
        var keyFacts = Array.isArray(summary.keyFacts) ? summary.keyFacts : [];
        var wrap = el('div');
        wrap.appendChild(el('p', {
            className: 'closure-lead',
            textContent: '先看这块确认本次分析目前走到哪一步、偏差信号来自哪里，以及人工复盘是否已经沉淀。'
        }));
        wrap.appendChild(renderSignalCards([
            ['当前阶段', textOrDash(summary.stageLabel)],
            ['Decision 结论', textOrDash(summary.decisionConclusion)],
            ['执行链概况', textOrDash(summary.executionHeadline)],
            ['人工复盘状态', textOrDash(reviewCompletion.summary)]
        ]));
        wrap.appendChild(kvRows([
            ['人工复盘状态码', '', 'node', el('span', { id: 'closure-review-status', textContent: textOrDash(reviewCompletion.status) })],
            ['人工复盘是否已填写', '', 'node', el('span', { id: 'closure-review-completed', textContent: text(reviewCompletion.completed) })],
            ['人工复盘是否有内容', '', 'node', el('span', { id: 'closure-review-has-content', textContent: text(reviewCompletion.hasContent) })],
            ['最近更新时间', '', 'node', el('span', { id: 'closure-review-update', textContent: text(reviewCompletion.updateTime) })],
            ['状态说明', '', 'node', el('span', { id: 'closure-review-summary', textContent: textOrDash(reviewCompletion.summary) })]
        ]));
        wrap.appendChild(el('div', { className: 'subhead', textContent: '偏差信号来源（最小归纳标签）' }));
        wrap.appendChild(renderTagList(deviationSourceTags, 'tag-list'));
        wrap.appendChild(el('div', { className: 'subhead', textContent: '偏差信号来源（最小归纳）' }));
        wrap.appendChild(renderSimpleList(deviationSignals, 'closure-list'));
        wrap.appendChild(el('div', { className: 'subhead', textContent: '本轮建议先核对' }));
        wrap.appendChild(renderSimpleList(nextFocus, 'closure-list'));
        wrap.appendChild(el('div', { className: 'subhead', textContent: '人工复盘重点参考事实块' }));
        wrap.appendChild(renderFactRefs(keyFacts));
        return section('0. 复盘闭环总览', wrap, 'sec-closure');
    }

    function renderFactRefs(items) {
        if (!items || items.length === 0) {
            return el('span', { textContent: '—' });
        }
        var list = el('div', { className: 'fact-ref-list' });
        items.forEach(function (item) {
            var card = el('button', { type: 'button', className: 'fact-ref-card' });
            card.appendChild(el('div', { className: 'fact-ref-label', textContent: textOrDash(item.label) }));
            card.appendChild(el('div', { className: 'fact-ref-reason', textContent: textOrDash(item.reason) }));
            card.addEventListener('click', function () {
                jumpToAnchor(item.anchor);
            });
            list.appendChild(card);
        });
        return list;
    }

    /** 与 Dashboard detail s2 证据明细一致的标签映射（只读展示）。 */
    function evidenceDirectionLabel(dir) {
        var d = dir != null ? String(dir).trim().toUpperCase() : "";
        if (d === "BULLISH") return "偏多";
        if (d === "BEARISH") return "偏空";
        if (d === "NEUTRAL") return "中性";
        return "";
    }

    function evidenceSourceSecondaryLabel(src) {
        var s = src != null ? String(src).trim() : "";
        if (!s) return "";
        var u = s.toUpperCase();
        if (u === "SYSTEM_GENERATED") return "系统";
        if (u === "MARKET_HEURISTIC") return "启发式";
        if (u === "MANUAL_INPUT") return "手动";
        return s;
    }

    function renderStructuredEvidenceTop3List(items) {
        var list = Array.isArray(items) ? items : [];
        if (!list.length) {
            return el('p', { className: 'empty-hint', textContent: '结构化证据暂无' });
        }
        var ul = el('ul', { className: 'decision-evidence-list structured-evidence-top3' });
        list.slice(0, 3).forEach(function (item) {
            var typeRaw = item && item.evidenceType != null ? String(item.evidenceType).trim() : '';
            var descRaw = item && item.description != null ? String(item.description).trim() : '';
            var dirRaw = item && item.direction != null ? String(item.direction).trim() : '';
            var srcRaw = item && item.source != null ? String(item.source).trim() : '';
            var typeText = typeRaw ? typeRaw : '—';
            var descText = descRaw ? descRaw : '—';
            var dirLabel = evidenceDirectionLabel(dirRaw);
            var srcSecondary = evidenceSourceSecondaryLabel(srcRaw);

            var li = el('li');
            if (dirLabel) {
                li.appendChild(el('span', { className: 'muted evidence-dir-tag', textContent: dirLabel }));
                li.appendChild(document.createTextNode(' '));
            }
            li.appendChild(el('span', { className: 'muted', textContent: '[' + typeText + ']' }));
            li.appendChild(document.createTextNode(' ' + descText));
            if (srcSecondary) {
                li.appendChild(document.createTextNode(' '));
                li.appendChild(el('span', { className: 'muted evidence-src-muted', textContent: srcSecondary }));
            }
            ul.appendChild(li);
        });
        return ul;
    }

    function renderStructuredEvidenceSection(evidenceTopItems) {
        var wrap = el('div', { className: 'structured-evidence-wrap' });
        wrap.appendChild(el('div', { className: 'subhead', textContent: '结构化证据（前3条）' }));
        wrap.appendChild(renderStructuredEvidenceTop3List(evidenceTopItems || []));
        wrap.appendChild(el('p', {
            className: 'structured-evidence-hint muted',
            textContent: '说明：evidenceSummary 为决策摘要；以下为同次 analysis 落库的结构化证据 top3（只读）。'
        }));
        return wrap;
    }

    /** 与 Dashboard detail s2 renderScoreTopItems 一致（只读）：scoreType：scoreValue */
    function renderStructuredScoreTop3List(items) {
        var list = Array.isArray(items) ? items : [];
        if (!list.length) {
            return el('p', { className: 'empty-hint', textContent: '评分明细暂无' });
        }
        var ul = el('ul', { className: 'decision-evidence-list structured-score-top3' });
        list.slice(0, 3).forEach(function (item) {
            var typeRaw = item && item.scoreType != null ? String(item.scoreType).trim() : '';
            var valueRaw = item != null ? item.scoreValue : null;
            var typeText = typeRaw ? typeRaw : '—';
            var valueText = valueRaw !== null && valueRaw !== undefined && String(valueRaw).trim() !== ''
                ? String(valueRaw).trim()
                : '—';
            ul.appendChild(el('li', { textContent: typeText + '： ' + valueText }));
        });
        return ul;
    }

    function renderStructuredScoreSection(scoreTopItems) {
        var wrap = el('div', { className: 'structured-score-wrap' });
        wrap.appendChild(el('div', { className: 'subhead', textContent: '评分明细（前3条）' }));
        wrap.appendChild(renderStructuredScoreTop3List(scoreTopItems || []));
        wrap.appendChild(el('p', {
            className: 'structured-evidence-hint muted',
            textContent: '说明：本块为 tm_score_item 的落库 top3，不代表八大评分已完成。'
        }));
        return wrap;
    }

    function renderEntryContext(summary) {
        if (!entryContext) return;
        entryContext.innerHTML = '';
        if (!summary) {
            return;
        }
        var facts = Array.isArray(summary.keyFacts) ? summary.keyFacts : [];
        var signalItems = [
            ['当前结论', textOrDash(summary.decisionConclusion)],
            ['执行链阶段', textOrDash(summary.stageLabel)],
            ['执行链概况', textOrDash(summary.executionHeadline)]
        ];
        entryContext.appendChild(el('p', {
            className: 'entry-context-lead',
            textContent: textOrDash(summary.entryGuidance)
        }));
        entryContext.appendChild(renderSignalCards(signalItems));
        entryContext.appendChild(el('div', { className: 'subhead', textContent: '偏差信号来源' }));
        entryContext.appendChild(renderTagList(summary.deviationSourceTags || [], 'tag-list'));
        if (facts.length > 0) {
            entryContext.appendChild(el('div', { className: 'subhead', textContent: '建议先对照这些事实块后再录入' }));
            entryContext.appendChild(renderFactRefs(facts));
        }
    }

    function renderDecision(d, evidenceTopItems, scoreTopItems, marketEnvironment) {
        if (!d) {
            return section('2. Decision 摘要', emptyPanel('暂无 Decision 数据', '聚合结果中未包含 decision 行。'), 'sec-decision');
        }
        var expl = formatExplanationJson(d.explanationJson);
        var reasons = parseReviewReasons(d.reviewReasons);
        var explanationObj = parseJsonObject(d.explanationJson);
        var assetStateObj = parseJsonObject(d.assetStateSnapshot);
        var evidenceItems = toListByDelimiters(d.evidenceSummary);

        var summaryNode = renderSignalCards([
            ['结论', textOrDash(d.conclusionSummary)],
            ['开仓价值', text(d.isWorthOpening)],
            ['采纳状态', text(d.isAdopted)],
            ['优先级', text(d.actionPriority)],
            ['置信 / 风险', text(d.confidenceLevel) + ' / ' + text(d.riskLevel)],
            ['冲突等级 / 分', text(d.aiConflictLevel) + ' / ' + text(d.aiConflictScore)],
            ['计划模式', text(d.aiPlanMode)],
            ['confusedScore', text(d.confusedScore)]
        ]);
        var auditNode = renderAuditLineList(
            buildDecisionAuditLines(d, explanationObj, assetStateObj, reasons, evidenceItems)
        );

        var reasonsNode = renderSimpleList(reasons, 'decision-review-reasons');
        var evidenceNode = renderSimpleList(evidenceItems, 'decision-evidence-list');
        var explanationPreviewNode = renderObjectPreview(explanationObj);
        var assetStatePreviewNode = renderObjectPreview(assetStateObj);

        var wrap = el('div');
        wrap.appendChild(renderMarketEnvironmentMini(marketEnvironment));
        wrap.appendChild(renderStructuredEvidenceSection(evidenceTopItems));
        wrap.appendChild(renderStructuredScoreSection(scoreTopItems));
        wrap.appendChild(el('div', { className: 'subhead', textContent: '关键信号（复盘速读）' }));
        wrap.appendChild(summaryNode);
        wrap.appendChild(el('div', { className: 'subhead', textContent: '审计线索（二级解释）' }));
        wrap.appendChild(auditNode);
        wrap.appendChild(el('div', { className: 'subhead', textContent: '解释与证据（结构化速览）' }));
        wrap.appendChild(kvRows([
            ['reviewReasons（条目）', '', 'node', reasonsNode],
            ['evidenceSummary（条目）', '', 'node', evidenceNode],
            ['explanationJson（首层字段）', '', 'node', explanationPreviewNode],
            ['assetStateSnapshot（首层字段）', '', 'node', assetStatePreviewNode]
        ]));
        var rawNode = kvRows([
            ['decisionId', text(d.decisionId)],
            ['symbol', text(d.symbol)],
            ['marketBiasHierarchy', text(d.marketBiasHierarchy)],
            ['tradeType', text(d.tradeType)],
            ['confidenceLevel', text(d.confidenceLevel)],
            ['riskLevel', text(d.riskLevel)],
            ['actionPriority', text(d.actionPriority)],
            ['conclusionSummary', text(d.conclusionSummary)],
            ['isWorthOpening', text(d.isWorthOpening)],
            ['multiTfConvergence', text(d.multiTfConvergence)],
            ['isAdopted', text(d.isAdopted)],
            ['validPeriod', text(d.validPeriod)],
            ['invalidCondition', text(d.invalidCondition)],
            ['evidenceSummary', textOrDash(d.evidenceSummary), 'raw'],
            ['explanationJson', expl !== null ? expl : '—', 'raw'],
            ['reviewReasons', '', 'node', reasonsNode],
            ['assetStateSnapshot', textOrDash(d.assetStateSnapshot), 'raw'],
            ['aiConflictLevel', text(d.aiConflictLevel)],
            ['aiConflictScore', text(d.aiConflictScore)],
            ['aiPlanMode', text(d.aiPlanMode)],
            ['confusedScore', text(d.confusedScore)],
            ['createTime', text(d.createTime)]
        ]);
        wrap.appendChild(createDetails('原始字段（完整兜底）', rawNode, false));
        return section('2. Decision 摘要', wrap, 'sec-decision');
    }

    function renderGovernanceSummary(summary) {
        if (!summary) {
            return section('A2. 治理输入摘要（人工结论层）', emptyPanel('暂无治理输入摘要', '聚合结果中未包含 governanceSummary。'), 'sec-governance-summary');
        }
        return section('A2. 治理输入摘要（人工结论层）', kvRows([
            ['governanceStatus', text(summary.governanceStatus)],
            ['governanceActionHint', textOrDash(summary.governanceActionHint)],
            ['hasReviewContent', text(summary.hasReviewContent)],
            ['primaryIssueType', text(formatReviewErrorTypeForDisplay(summary.primaryIssueType))],
            ['latestReviewUpdatedAt', text(summary.latestReviewUpdatedAt)],
            ['linkedRuleLogId', text(summary.linkedRuleLogId)],
            ['linkedRuleLogCreatedAt', text(summary.linkedRuleLogCreatedAt)],
            ['linkedRuleLogChangeCategory', text(summary.linkedRuleLogChangeCategory)]
        ]), 'sec-governance-summary');
    }

    function renderPlan(p) {
        if (!p) {
            return section('3. Plan 摘要', emptyPanel('暂无 Plan 数据', '聚合结果中未包含 plan 行。'), 'sec-plan');
        }
        return section('3. Plan 摘要', kvRows([
            ['planId', text(p.planId)],
            ['recommendedAction', text(p.recommendedAction)],
            ['entryZone', text(p.entryZone)],
            ['stopLoss', text(p.stopLoss)],
            ['takeProfitRules', text(p.takeProfitRules)],
            ['leverageSuggestion', text(p.leverageSuggestion)],
            ['positionSuggestion', text(p.positionSuggestion)],
            ['createTime', text(p.createTime)]
        ]), 'sec-plan');
    }

    function renderPushRecheck(list) {
        if (!list || list.length === 0) {
            return section('4. Push / Recheck 摘要', emptyPanel('暂无 Push 记录', '本 analysis 未关联 push 或列表为空。'), 'sec-push');
        }
        var frag = document.createDocumentFragment();
        list.forEach(function (bundle, idx) {
            var push = bundle.push || {};
            var rechecks = bundle.rechecks || [];
            var block = el('div', { className: 'push-block' });
            block.appendChild(el('div', { className: 'subhead', textContent: 'Push #' + (idx + 1) }));
            var latest = rechecks.length > 0 ? rechecks[rechecks.length - 1] : null;
            var digest = el('div', { className: 'push-digest' });
            digest.appendChild(el('span', { className: 'digest-item', textContent: '状态: ' + text(push.pushStatus) }));
            digest.appendChild(el('span', { className: 'digest-item', textContent: 'Recheck 数: ' + rechecks.length }));
            digest.appendChild(el('span', {
                className: 'digest-item',
                textContent: '最近 Recheck: ' + (latest ? (text(latest.recheckStatus) + ' @ ' + text(latest.recheckTime)) : '—')
            }));
            block.appendChild(digest);
            var riskGate = text(push.accountRiskAllowed) + ' / ' + text(push.riskLevelSnapshot);
            var drift = '—';
            if (latest) {
                var latestDrift = parseNumberLike(latest.priceDriftRatio);
                var latestSlip = parseNumberLike(latest.currentSlippageEstimation);
                if (latestDrift !== null && latestSlip !== null) {
                    drift = String(latestDrift) + ' / ' + String(latestSlip);
                }
            }
            var pConfused = parseNumberLike(push.confusedScoreSnapshot);
            var rConfused = latest ? parseNumberLike(latest.currentConfusedScore) : null;
            var scoreDelta = formatDelta(rConfused, pConfused);
            var pQuality = parseNumberLike(push.dataQualityScoreSnapshot);
            var rQuality = latest ? parseNumberLike(latest.currentDataQualityScore) : null;
            var qualityDelta = formatDelta(rQuality, pQuality);
            var latestFailObj = latest ? parseJsonObject(latest.failReasonJson) : null;
            var latestFailKeys = latestFailObj ? Object.keys(latestFailObj).slice(0, 4).join(', ') : '—';
            block.appendChild(el('div', { className: 'subhead', textContent: 'Push 解释速览' }));
            block.appendChild(renderSignalCards([
                ['风险闸门', riskGate],
                ['触发上下文', text(push.planModeSnapshot) + ' / ' + text(push.causeEffectAlignmentSnapshot)],
                ['执行/质量', text(push.executionFeasibilitySnapshot) + ' / ' + text(push.dataQualityScoreSnapshot)],
                ['困惑分变化(最新-快照)', scoreDelta],
                ['质量分变化(最新-快照)', qualityDelta],
                ['漂移/滑点(最新)', drift],
                ['最新复扫状态', latest ? text(latest.recheckStatus) : '—'],
                ['最新失败原因键', latestFailKeys]
            ]));
            block.appendChild(kvRows([
                ['pushId', text(push.pushId)],
                ['symbol', text(push.symbol)],
                ['timeframe', text(push.timeframe)],
                ['pushType', text(push.pushType)],
                ['pushStatus', text(push.pushStatus)],
                ['pushCreateTime', text(push.pushCreateTime)],
                ['createTime', text(push.createTime)],
                ['ruleVersion', text(push.ruleVersion)],
                ['triggerPrice', text(push.triggerPrice)],
                ['planModeSnapshot', text(push.planModeSnapshot)],
                ['causeEffectAlignmentSnapshot', text(push.causeEffectAlignmentSnapshot)],
                ['executionFeasibilitySnapshot', text(push.executionFeasibilitySnapshot)],
                ['dataQualityScoreSnapshot', text(push.dataQualityScoreSnapshot)],
                ['confusedScoreSnapshot', text(push.confusedScoreSnapshot)],
                ['accountRiskSnapshotId', text(push.accountRiskSnapshotId)],
                ['accountRiskAllowed', text(push.accountRiskAllowed)],
                ['riskLevelSnapshot', text(push.riskLevelSnapshot)],
                ['riskReasonCode', text(push.riskReasonCode)],
                ['riskReasonText', text(push.riskReasonText)],
                ['positionExposure', text(push.positionExposure)],
                ['maxAllowedExposure', text(push.maxAllowedExposure)],
                ['snapshotSource', text(push.snapshotSource)],
                ['snapshotVersion', text(push.snapshotVersion)],
                ['expiresAt', text(push.expiresAt)],
                ['traceId', text(push.traceId)]
            ]));
            var rawPushJsonNode = kvRows([
                ['entryZoneJson', text(push.entryZoneJson), 'raw'],
                ['stopZoneJson', text(push.stopZoneJson), 'raw'],
                ['invalidationConditionJson', text(push.invalidationConditionJson), 'raw']
            ]);
            block.appendChild(createDetails('Push 原始 JSON（兜底）', rawPushJsonNode, false));
            if (rechecks.length === 0) {
                block.appendChild(emptyPanel('本条 Push 无 Recheck 日志', '尚未产生复扫记录。'));
            } else {
                block.appendChild(el('div', { className: 'subhead', textContent: 'Recheck' }));
                block.appendChild(el('p', {
                    className: 'empty',
                    textContent: '说明：currentSlippageEstimation 为本次 recheck 基于价格漂移的估算；currentAccountRiskAllowed 来自 push.accountRiskSnapshotId 对应风险快照。Round2 可解释字段见本 Push 的 riskReason*/positionExposure/maxAllowedExposure/snapshot*。'
                }));
                var table = el('table', { className: 'simple' });
                var thead = el('thead');
                var hr = el('tr');
                ['logId', 'recheckTime', 'recheckStatus', 'currentPrice', 'priceDriftRatio', 'currentSlippageEstimation', 'currentDataQualityScore', 'currentConfusedScore', 'currentAccountRiskAllowed', 'failReasonJson', 'createTime'].forEach(function (h) {
                    hr.appendChild(el('th', { textContent: h }));
                });
                thead.appendChild(hr);
                table.appendChild(thead);
                var tbody = el('tbody');
                rechecks.forEach(function (r) {
                    var tr = el('tr');
                    ['logId', 'recheckTime', 'recheckStatus', 'currentPrice', 'priceDriftRatio', 'currentSlippageEstimation', 'currentDataQualityScore', 'currentConfusedScore', 'currentAccountRiskAllowed', 'failReasonJson', 'createTime'].forEach(function (k) {
                        var td = el('td');
                        if (k === 'failReasonJson') {
                            var fr = r[k];
                            if (fr === null || fr === undefined || String(fr).trim() === '') {
                                td.textContent = '—';
                            } else {
                                td.className = 'raw-json';
                                td.textContent = tryPrettyJsonString(fr);
                            }
                        } else {
                            td.textContent = text(r[k]);
                        }
                        tr.appendChild(td);
                    });
                    tbody.appendChild(tr);
                });
                table.appendChild(tbody);
                block.appendChild(table);
            }
            frag.appendChild(block);
        });
        var wrap = el('div');
        wrap.appendChild(frag);
        return section('4. Push / Recheck 摘要', wrap, 'sec-push');
    }

    function renderMissed(rows) {
        if (!rows || rows.length === 0) {
            return section('5a. Missed 摘要', emptyPanel('暂无 Missed 记录', '未写入 missed 或列表为空。'), 'sec-missed');
        }
        var wrap = el('div');
        wrap.appendChild(el('p', {
            className: 'empty',
            textContent: '说明：优先展示 reasonView（读取侧解释层），reasonJson 继续保留原样兜底。'
        }));
        var table = el('table', { className: 'simple' });
        var thead = el('thead');
        var hr = el('tr');
        ['missedId', 'analysisId', 'decisionId', 'symbol', 'bizDate', 'ruleVersion', 'traceId', 'reasonView', 'reasonJson', 'createTime'].forEach(function (h) {
            hr.appendChild(el('th', { textContent: h }));
        });
        thead.appendChild(hr);
        table.appendChild(thead);
        var tbody = el('tbody');
        rows.forEach(function (m) {
            var tr = el('tr');
            tr.appendChild(el('td', { textContent: text(m.missedId) }));
            tr.appendChild(el('td', { textContent: text(m.analysisId) }));
            tr.appendChild(el('td', { textContent: text(m.decisionId) }));
            tr.appendChild(el('td', { textContent: text(m.symbol) }));
            tr.appendChild(el('td', { textContent: text(m.bizDate) }));
            tr.appendChild(el('td', { textContent: text(m.ruleVersion) }));
            tr.appendChild(el('td', { textContent: text(m.traceId) }));
            var reasonViewCell = el('td');
            var rv = m.reasonView;
            if (!rv) {
                reasonViewCell.textContent = '—';
            } else {
                reasonViewCell.className = 'raw-json';
                reasonViewCell.textContent = tryPrettyJsonString(JSON.stringify({
                    version: rv.version,
                    rule: rv.rule,
                    whyMissed: rv.whyMissed,
                    facts: rv.facts || {},
                    refs: rv.refs || {},
                    parseStatus: rv.parseStatus
                }));
            }
            tr.appendChild(reasonViewCell);
            var reasonCell = el('td');
            if (m.reasonJson === null || m.reasonJson === undefined || String(m.reasonJson).trim() === '') {
                reasonCell.textContent = '—';
            } else {
                reasonCell.className = 'raw-json';
                reasonCell.textContent = tryPrettyJsonString(m.reasonJson);
            }
            tr.appendChild(reasonCell);
            tr.appendChild(el('td', { textContent: text(m.createTime) }));
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        wrap.appendChild(table);
        return section('5a. Missed 摘要', wrap, 'sec-missed');
    }

    function renderRuleVersionLogs(rows) {
        if (!rows || rows.length === 0) {
            return section('5d. 规则版本审计链', emptyPanel('暂无规则版本日志', '本 analysis 未命中规则版本审计日志。'), 'sec-rule-version-log');
        }
        var wrap = el('div');
        wrap.appendChild(el('p', {
            className: 'empty',
            textContent: '说明：优先展示结构化字段；fallbackMatched=true 表示该条日志有字段来自 changeSummary 兼容补齐。'
        }));
        var table = el('table', { className: 'simple' });
        var thead = el('thead');
        var hr = el('tr');
        ['linkedReview', 'createdAt', 'changeCategory', 'ruleVersion', 'errorType', 'operator', 'rollbackFlag', 'fallbackMatched', 'changeSummary', 'changeDetail'].forEach(function (h) {
            hr.appendChild(el('th', { textContent: h }));
        });
        thead.appendChild(hr);
        table.appendChild(thead);
        var tbody = el('tbody');
        rows.forEach(function (item) {
            var tr = el('tr');
            if (item.linkedToLatestReview === true) {
                tr.style.background = '#eff6ff';
            }
            ['linkedToLatestReview', 'createdAt', 'changeCategory', 'ruleVersion', 'errorType', 'operator', 'rollbackFlag', 'fallbackMatched', 'changeSummary', 'changeDetail'].forEach(function (k) {
                var td = el('td');
                if (k === 'linkedToLatestReview') {
                    td.textContent = item[k] === true ? 'YES (本次 review 关联)' : '—';
                } else if (k === 'errorType') {
                    td.textContent = text(formatReviewErrorTypeForDisplay(item[k]));
                } else {
                    td.textContent = text(item[k]);
                }
                if (k === 'changeSummary' || k === 'changeDetail') {
                    td.className = 'raw-json';
                }
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        wrap.appendChild(table);
        attachJsonPanels(wrap);
        return section('5d. 规则版本审计链', wrap, 'sec-rule-version-log');
    }

    function hrExpl(title, body) {
        if (!body || String(body).trim() === '') {
            return null;
        }
        var wrap = el('div');
        wrap.appendChild(el('div', { className: 'hr-k', textContent: title }));
        wrap.appendChild(el('p', { textContent: String(body) }));
        return wrap;
    }

    function renderHotReset(h) {
        if (!h) {
            return section('5b. Hot Reset（当前资产行）', emptyPanel('暂无 Hot Reset 行', '无 tm_asset_state 行：symbol 为空或该标的尚未落权威表。'), 'sec-hot-reset');
        }
        var intro = el('div', { className: 'hr-callout' });
        intro.appendChild(el('p', { className: 'hr-lead', textContent: '本节展示的是该标的在 tm_asset_state 的当前一行（含最近一次 hot_reset_*），不是仅属于本 analysis 的独立事件存档。' }));
        var a = hrExpl('数据来源与边界', h.scopeExplanationZh);
        var b = hrExpl('与本次复盘 / decision 的关系', h.relationToThisAnalysisZh);
        var c = hrExpl('preResetState / postResetState 含义', h.prePostStateMeaningZh);
        var d = hrExpl('与 missed.reasonJson 的旁证（只读）', h.missedRelationHintZh);
        [a, b, c, d].forEach(function (node) {
            if (node) intro.appendChild(node);
        });

        var sub = el('p', { className: 'subhead', textContent: '当前行字段（与聚合接口 hotReset 一致）' });
        var frag = document.createDocumentFragment();
        frag.appendChild(intro);
        frag.appendChild(sub);
        frag.appendChild(kvRows([
            ['semanticScope', text(h.semanticScope)],
            ['symbol（标的）', text(h.symbol)],
            ['state（当前枚举）', text(h.state)],
            ['confusedScore（当前行）', text(h.confusedScore)],
            ['hotResetFlag', text(h.hotResetFlag)],
            ['hotResetTriggerType', text(h.hotResetTriggerType)],
            ['hotResetTriggerValue', text(h.hotResetTriggerValue)],
            ['hotResetTime（最近一次写入时间）', text(h.hotResetTime)],
            ['preResetState（该次 Hot Reset 前）', text(h.preResetState)],
            ['postResetState（该次 Hot Reset 后）', text(h.postResetState)],
            ['lastUpdateTime（行更新时间）', text(h.lastUpdateTime)]
        ]));
        frag.appendChild(el('p', { className: 'subhead', textContent: '本次 analysis 事件语义（tm_hot_reset_event）' }));
        frag.appendChild(kvRows([
            ['analysisEventRecorded', text(h.analysisEventRecorded)],
            ['eventId', text(h.analysisEventId)],
            ['traceId', text(h.analysisEventTraceId)],
            ['triggerType（事件类别）', text(h.analysisEventTriggerType)],
            ['triggerReasonCode（触发原因码）', text(h.analysisEventTriggerReasonCode)],
            ['triggerReasonText（触发原因说明）', text(h.analysisEventTriggerReasonText)],
            ['triggerValue', text(h.analysisEventTriggerValue)],
            ['decisionId', text(h.analysisEventDecisionId)],
            ['decisionState（触发时决策状态）', text(h.analysisEventDecisionState)],
            ['preState（资产状态切换前状态）', text(h.analysisEventPreState)],
            ['postState（资产状态切换后状态）', text(h.analysisEventPostState)],
            ['confusedScoreSnapshot', text(h.analysisEventConfusedScoreSnapshot)],
            ['multiTimeframeAlignedSnapshot', text(h.analysisEventMultiTimeframeAlignedSnapshot)],
            ['eventVersion', text(h.analysisEventVersion)],
            ['eventTime', text(h.analysisEventTime)]
        ]));
        return section('5b. Hot Reset（当前资产行）', frag, 'sec-hot-reset');
    }

    function renderAlerts(rows) {
        if (!rows || rows.length === 0) {
            return section('5c. Alerts 摘要', emptyPanel('暂无告警', '本 analysis 时间范围内未命中告警记录。'), 'sec-alerts');
        }
        var explainFn = window.AlertExplain && window.AlertExplain.explainAlert;
        var wrap = el('div');
        wrap.appendChild(el('p', {
            className: 'empty',
            textContent: '说明：展示层统一为「命中原因 + 当前状态 + 抑制/冷却依据」，不改变 OPEN / SUPPRESSED 真值判定。'
        }));
        var table = el('table', { className: 'simple' });
        var thead = el('thead');
        var hr = el('tr');
        ['id', 'alertType', 'typeLabel', 'alertLevel', 'status', 'why', 'cooldown', 'suppress', 'cooldownUntil', 'suppressReasonRaw', 'createdAt'].forEach(function (h) {
            hr.appendChild(el('th', { textContent: h }));
        });
        thead.appendChild(hr);
        table.appendChild(thead);
        var tbody = el('tbody');
        rows.forEach(function (a) {
            var explain = explainFn ? explainFn(a) : null;
            var typeLabel = explain ? explain.alertTypeLabel : text(a.alertType);
            var why = explain ? explain.why : text(a.alertMessage);
            var cooldown = explain ? (explain.cooldownState + ' · ' + explain.cooldownDetail) : text(a.cooldownUntil);
            var suppress = explain ? explain.suppressDetail : text(a.suppressReason);
            var tr = el('tr');
            [
                text(a.id),
                text(a.alertType),
                typeLabel,
                text(a.alertLevel),
                text(a.status),
                why,
                cooldown,
                suppress,
                text(a.cooldownUntil),
                text(a.suppressReason),
                text(a.createdAt)
            ].forEach(function (cell) {
                tr.appendChild(el('td', { textContent: cell }));
            });
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        wrap.appendChild(table);
        return section('5c. Alerts 摘要', wrap, 'sec-alerts');
    }

    /** 为较长的 JSON/文本单元格增加展开/收起（不改数据，仅展示） */
    function attachJsonPanels(scope) {
        if (!scope) return;
        var nodes = scope.querySelectorAll('dd.raw-json, td.raw-json');
        nodes.forEach(function (cell) {
            if (cell.dataset.enhanced === '1') {
                return;
            }
            var t = cell.textContent || '';
            if (t.length < 160) {
                return;
            }
            cell.dataset.enhanced = '1';
            cell.classList.add('json-collapsible');
            cell.classList.remove('raw-json');
            var textContent = t;
            cell.textContent = '';
            var toolbar = el('div', { className: 'json-toolbar' });
            toolbar.appendChild(el('span', { className: 'json-label', textContent: '长内容（JSON/文本）' }));
            var btn = el('button', { type: 'button', textContent: '展开' });
            var inner = el('div', { className: 'raw-json' });
            inner.textContent = textContent;
            btn.addEventListener('click', function () {
                cell.classList.toggle('is-expanded');
                btn.textContent = cell.classList.contains('is-expanded') ? '收起' : '展开';
            });
            toolbar.appendChild(btn);
            cell.appendChild(toolbar);
            cell.appendChild(inner);
        });
    }

    function populateNav() {
        var nav = document.getElementById('review-nav');
        if (!nav) {
            return;
        }
        nav.innerHTML = '';
        nav.appendChild(el('span', { className: 'review-nav-label', textContent: '快速跳转' }));
        var links = [
            ['录入', '#sec-review-entry'],
            ['总览', '#sec-closure'],
            ['Run', '#sec-run'],
            ['治理输入摘要', '#sec-governance-summary'],
            ['Decision', '#sec-decision'],
            ['Plan', '#sec-plan'],
            ['Push', '#sec-push'],
            ['Missed', '#sec-missed'],
            ['Hot Reset', '#sec-hot-reset'],
            ['Alerts', '#sec-alerts'],
            ['规则版本审计链', '#sec-rule-version-log']
        ];
        links.forEach(function (item) {
            var a = el('a', { href: item[1], textContent: item[0] });
            a.addEventListener('click', function (e) {
                var id = item[1].slice(1);
                var target = document.getElementById(id);
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    try {
                        history.replaceState(null, '', item[1]);
                    } catch (err) { /* ignore */ }
                }
            });
            nav.appendChild(a);
        });
        nav.className = 'review-nav visible';
    }

    function renderAll(data) {
        currentAggregate = data || {};
        root.innerHTML = '';
        if (closureRoot) {
            closureRoot.innerHTML = '';
            closureRoot.appendChild(renderClosureSummary(currentAggregate.reviewClosure));
            closureRoot.style.display = 'block';
        }
        renderEntryContext(currentAggregate.reviewClosure);
        root.appendChild(renderRun(data.run));
        root.appendChild(renderGovernanceSummary(data.governanceSummary));
        root.appendChild(renderDecision(data.decision, data.evidenceTopItems, data.scoreTopItems, data.marketEnvironment));
        root.appendChild(renderPlan(data.plan));
        root.appendChild(renderPushRecheck(data.pushRecheck));
        root.appendChild(renderMissed(data.missed));
        root.appendChild(renderHotReset(data.hotReset));
        root.appendChild(renderAlerts(data.alerts));
        root.appendChild(renderRuleVersionLogs(data.ruleVersionLogs));
        attachJsonPanels(root);
        var rr = document.getElementById('readonly-region');
        if (rr) {
            rr.style.display = 'block';
        }
        populateNav();
    }

    function showError(msg) {
        banner.className = 'banner error';
        banner.textContent = msg;
        banner.style.display = 'block';
        root.style.display = 'none';
        var rr = document.getElementById('readonly-region');
        if (rr) {
            rr.style.display = 'none';
        }
        if (closureRoot) {
            closureRoot.style.display = 'none';
            closureRoot.innerHTML = '';
        }
        var nav = document.getElementById('review-nav');
        if (nav) {
            nav.className = 'review-nav';
            nav.innerHTML = '';
        }
    }

    function showLoaded() {
        banner.style.display = 'none';
    }

    var entryEl = document.getElementById('review-entry');

    function showReviewEntry() {
        if (entryEl) entryEl.style.display = 'block';
    }

    function applyReviewState(s) {
        var et = document.getElementById('rf-error-type');
        var ao = document.getElementById('rf-actual-outcome');
        var adj = document.getElementById('rf-adjustment');
        if (!et || !ao || !adj) return;
        if (!s) {
            et.value = '';
            ao.value = '';
            adj.value = '';
            syncClosureReviewStatus(null);
            return;
        }
        et.value = s.errorType != null ? String(s.errorType) : '';
        ao.value = s.actualOutcome != null ? String(s.actualOutcome) : '';
        adj.value = s.adjustmentSuggestion != null ? String(s.adjustmentSuggestion) : '';
        syncClosureReviewStatus(s);
    }

    function syncClosureReviewStatus(s) {
        var statusNode = document.getElementById('closure-review-status');
        var completedNode = document.getElementById('closure-review-completed');
        var hasContentNode = document.getElementById('closure-review-has-content');
        var updateNode = document.getElementById('closure-review-update');
        var summaryNode = document.getElementById('closure-review-summary');
        if (!statusNode || !completedNode || !hasContentNode || !updateNode || !summaryNode) {
            return;
        }
        var completion = buildReviewCompletionFromState(s);
        statusNode.textContent = textOrDash(completion.status);
        completedNode.textContent = text(completion.completed);
        hasContentNode.textContent = text(completion.hasContent);
        updateNode.textContent = text(completion.updateTime);
        summaryNode.textContent = textOrDash(completion.summary);
        if (currentAggregate && currentAggregate.reviewClosure) {
            currentAggregate.reviewClosure.reviewCompletion = completion;
            currentAggregate.reviewClosure.entryGuidance = buildEntryGuidanceFromCompletion(completion);
            renderEntryContext(currentAggregate.reviewClosure);
        }
    }

    function wireSaveOnce() {
        var btn = document.getElementById('rf-save');
        var msg = document.getElementById('rf-save-msg');
        if (!btn || btn.dataset.wired === '1') return;
        btn.dataset.wired = '1';
        btn.addEventListener('click', function () {
            if (msg) msg.textContent = '';
            var payload = {
                analysisId: analysisId,
                errorType: document.getElementById('rf-error-type').value,
                actualOutcome: document.getElementById('rf-actual-outcome').value,
                adjustmentSuggestion: document.getElementById('rf-adjustment').value
            };
            fetch('/api/review/save', {
                method: 'POST',
                headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }).then(function (res) {
                return res.json().then(function (body) {
                    return { ok: res.ok, status: res.status, body: body };
                });
            }).then(function (pack) {
                var b = pack.body;
                if (!pack.ok || b.code !== 200) {
                    var err = b && b.msg ? b.msg : ('HTTP ' + pack.status);
                    if (msg) msg.textContent = '保存失败：' + err;
                    return;
                }
                applyReviewState(b.data);
                if (msg) msg.textContent = '已保存';
            }).catch(function () {
                if (msg) msg.textContent = '保存失败：网络或解析错误';
            });
        });
    }

    if (!analysisId) {
        showError('缺少 analysisId');
        return;
    }

    fetch('/api/review/aggregate/' + encodeURIComponent(analysisId), {
        headers: { 'Accept': 'application/json' }
    }).then(function (res) {
        return res.json().then(function (body) {
            return { ok: res.ok, status: res.status, body: body };
        });
    }).then(function (pack) {
        var body = pack.body;
        if (!pack.ok || body.code !== 200) {
            var errMsg = body && body.msg ? body.msg : ('HTTP ' + pack.status);
            showError(errMsg);
            return;
        }
        fetch('/api/review/state/' + encodeURIComponent(analysisId), {
            headers: { 'Accept': 'application/json' }
        }).then(function (res) {
            return res.json().then(function (b) {
                return { ok: res.ok, body: b };
            });
        }).then(function (sp) {
            showLoaded();
            showReviewEntry();
            renderAll(body.data || {});
            if (sp.ok && sp.body && sp.body.code === 200) {
                applyReviewState(sp.body.data);
            } else {
                applyReviewState(null);
            }
            wireSaveOnce();
        }).catch(function () {
            showLoaded();
            showReviewEntry();
            renderAll(body.data || {});
            applyReviewState(null);
            wireSaveOnce();
        });
    }).catch(function () {
        showError('网络或解析失败');
    });
})();
