(function (global) {
    'use strict';

    var ALERT_TYPE_LABELS = {
        HIGH_RISK_DECISION: '高风险决策',
        DATA_QUALITY_INSUFFICIENT: '数据质量不足',
        AI_CONFLICT_ELEVATED: '多模型冲突升高',
        MULTI_TF_WEAK: '多周期收敛偏弱',
        OPEN_BLOCKED_BY_CONFLICT: '开仓被冲突阻断',
        CONFLUENCE_BREAKDOWN: '收敛破裂'
    };

    function textOrDash(v) {
        if (v === null || v === undefined) return '—';
        var s = String(v).trim();
        return s ? s : '—';
    }

    function parseTimeLike(value) {
        if (!value) return null;
        if (value instanceof Date) return isNaN(value.getTime()) ? null : value;
        if (typeof value === 'number') {
            var dn = new Date(value);
            return isNaN(dn.getTime()) ? null : dn;
        }
        if (typeof value === 'string') {
            var raw = value.trim();
            if (!raw) return null;
            var d = new Date(raw.replace(' ', 'T'));
            return isNaN(d.getTime()) ? null : d;
        }
        return null;
    }

    function extractWindowMinutes(reason, key) {
        if (!reason) return null;
        var re = new RegExp(key + ':(\\d+)m', 'i');
        var m = String(reason).match(re);
        return m ? Number(m[1]) : null;
    }

    function formatMinutes(minutes) {
        if (minutes === null || minutes === undefined || !isFinite(minutes)) return '';
        if (minutes < 60) return Math.max(1, Math.round(minutes)) + ' 分钟';
        var h = Math.floor(minutes / 60);
        var m = Math.round(minutes % 60);
        if (m <= 0) return h + ' 小时';
        return h + ' 小时 ' + m + ' 分钟';
    }

    function buildCooldownInfo(row, nowDate) {
        var cooldownUntil = row && row.cooldownUntil;
        if (!cooldownUntil) {
            return {
                state: 'NO_COOLDOWN',
                label: '未设置冷却截止',
                detail: '未设置冷却截止',
                until: '—'
            };
        }
        var untilDate = parseTimeLike(cooldownUntil);
        var until = textOrDash(cooldownUntil);
        if (!untilDate) {
            return {
                state: 'UNKNOWN',
                label: '冷却截止时间解析失败',
                detail: '冷却截止：' + until,
                until: until
            };
        }
        var now = nowDate || new Date();
        var diffMinutes = (untilDate.getTime() - now.getTime()) / 60000;
        if (diffMinutes > 0) {
            return {
                state: 'COOLING',
                label: '冷却中',
                detail: '冷却中，剩余约 ' + formatMinutes(diffMinutes),
                until: until
            };
        }
        return {
            state: 'PASSED',
            label: '已过冷却',
            detail: '已过冷却，冷却截止：' + until,
            until: until
        };
    }

    function explainSuppressReason(reason) {
        var raw = textOrDash(reason);
        if (raw === '—') {
            return { key: 'NONE', title: '未提供抑制原因', detail: '未提供抑制原因', raw: raw };
        }
        if (raw.toUpperCase().indexOf('THROTTLE_DB') >= 0) {
            var dbM = extractWindowMinutes(raw, 'THROTTLE_DB');
            return {
                key: 'THROTTLE_DB',
                title: '数据库窗口抑制',
                detail: '数据库窗口抑制（' + (dbM ? dbM + ' 分钟内已有同类 OPEN 告警' : '同类 OPEN 告警已存在') + '）',
                raw: raw
            };
        }
        if (raw.toUpperCase().indexOf('SEMANTIC_SIMILAR_RECENT') >= 0) {
            var semM = extractWindowMinutes(raw, 'SEMANTIC_SIMILAR_RECENT');
            return {
                key: 'SEMANTIC_SIMILAR_RECENT',
                title: '近窗语义相似抑制',
                detail: '近窗语义相似抑制（' + (semM ? semM + ' 分钟内已有相近告警' : '近窗内已有相近告警') + '）',
                raw: raw
            };
        }
        return { key: 'OTHER', title: '抑制', detail: '抑制原因：' + raw, raw: raw };
    }

    function explainStatus(row, nowDate) {
        var status = textOrDash(row && row.status).toUpperCase();
        var suppress = explainSuppressReason(row && row.suppressReason);
        var cooldown = buildCooldownInfo(row, nowDate);
        if (status === 'SUPPRESSED') {
            return {
                statusLabel: '已抑制',
                statusDetail: suppress.detail,
                suppress: suppress,
                cooldown: cooldown
            };
        }
        if (status === 'OPEN') {
            return {
                statusLabel: '已触发',
                statusDetail: cooldown.detail,
                suppress: suppress,
                cooldown: cooldown
            };
        }
        return {
            statusLabel: status || '未知状态',
            statusDetail: '状态未知：请结合 status / suppressReason / cooldownUntil',
            suppress: suppress,
            cooldown: cooldown
        };
    }

    function typeLabel(alertType) {
        var raw = textOrDash(alertType);
        return ALERT_TYPE_LABELS[raw] || raw;
    }

    function explainAlert(row, nowDate) {
        var hitReason = textOrDash(row && row.alertMessage);
        var type = textOrDash(row && row.alertType);
        var status = explainStatus(row, nowDate);
        return {
            alertType: type,
            alertTypeLabel: typeLabel(type),
            hitReason: hitReason,
            statusLabel: status.statusLabel,
            statusDetail: status.statusDetail,
            suppressDetail: status.suppress.detail,
            suppressKey: status.suppress.key,
            suppressRaw: status.suppress.raw,
            cooldownState: status.cooldown.label,
            cooldownDetail: status.cooldown.detail,
            cooldownUntil: status.cooldown.until,
            // 三段式 why 骨架
            why: '命中原因：' + hitReason
                + '；当前状态：' + status.statusLabel + '（' + status.statusDetail + '）'
                + '；抑制/冷却依据：' + (status.statusLabel === '已抑制' ? status.suppress.detail : status.cooldown.detail)
        };
    }

    global.AlertExplain = {
        ALERT_TYPE_LABELS: ALERT_TYPE_LABELS,
        textOrDash: textOrDash,
        typeLabel: typeLabel,
        explainSuppressReason: explainSuppressReason,
        buildCooldownInfo: buildCooldownInfo,
        explainStatus: explainStatus,
        explainAlert: explainAlert
    };
})(window);
