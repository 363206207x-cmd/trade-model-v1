import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

const contractPath = "src/main/resources/static/js/frontend-contract.js";
const workspacePath = "src/main/resources/static/js/workspace.js";
const sandbox = { window: {} };
vm.createContext(sandbox);
vm.runInContext(fs.readFileSync(contractPath, "utf8"), sandbox, { filename: contractPath });

const contract = sandbox.window.TradeModelFrontendContract;
assert.ok(contract, "TradeModelFrontendContract must load");

assert.equal(contract.reviewResultLabel("APPROVE"), "通过");
assert.equal(contract.reviewResultLabel("DOWNGRADE"), "降级");
assert.equal(contract.reviewResultLabel("REJECT_CANDIDATE"), "拒绝候选");
assert.equal(contract.reviewResultLabel("RISK_WARNING"), "风险警告");
assert.equal(contract.reviewResultLabel("REJECT"), "当前不可查看");
assert.equal(contract.reviewResultLabel(), "当前不可查看");

const validPath = {
  triggerCondition: "跌破结构支撑",
  causalPath: "结构破坏后流动性转弱",
  invalidatingEvidence: "重新站稳并恢复成交"
};
assert.deepEqual(
  JSON.parse(JSON.stringify(contract.failurePathView("FOUND", [validPath]))),
  {
    valid: true,
    state: "FOUND",
    label: "已发现可验证失败路径",
    paths: [validPath],
    failClosed: false
  }
);
assert.equal(contract.failurePathView("FOUND", []).valid, false);
assert.equal(contract.failurePathView("FOUND", [{}]).valid, false);
assert.equal(
  contract.failurePathView("NO_VERIFIABLE_FAILURE_PATH", []).label,
  "未发现可验证失败路径"
);
assert.equal(contract.failurePathView("NONE_FOUND", [validPath]).valid, false);
assert.equal(contract.failurePathView("SOURCE_UNAVAILABLE", [validPath]).valid, false);

const found = [{ state: "FOUND", size: 1 }];
const none = [{ state: "NONE_FOUND", size: 0 }];
assert.equal(contract.roleGate("READY", true, found).renderMode, "READY");
assert.equal(contract.roleGate("PARTIAL", true, none).renderMode, "PARTIAL");
for (const state of ["FALLBACK", "UNAVAILABLE", "ERROR"]) {
  const gated = contract.roleGate(state, true, found);
  assert.equal(gated.allowed, false, `${state} must hide old payload`);
  assert.equal(gated.renderMode, "FAIL_CLOSED");
}
assert.equal(contract.roleGate("READY", false, found).allowed, false);
assert.equal(contract.roleGate("READY", undefined, found).allowed, false);
assert.equal(contract.roleGate("READY", true, [{ state: "FOUND", size: 0 }]).allowed, false);
assert.equal(contract.roleGate("READY", true, [{ state: "SOURCE_UNAVAILABLE", size: 1 }]).allowed, false);
assert.equal(contract.roleGate("READY", true,
  [{ state: "NO_VERIFIABLE_FAILURE_PATH", size: 0 }]).allowed, false);
assert.equal(contract.roleGate("READY", true,
  [{ state: "NO_VERIFIABLE_FAILURE_PATH", size: 0, failurePath: true }]).allowed, true);

const preview = contract.analysisModeGate("ANALYSIS_PREVIEW");
assert.equal(preview.valid, true);
assert.equal(preview.candidateAllowed, false);
assert.equal(preview.candidateReviewAllowed, false);
assert.equal(preview.opportunityFailurePathsAllowed, false);
const opportunity = contract.analysisModeGate("OPPORTUNITY_DECISION");
assert.equal(opportunity.valid, true);
assert.equal(opportunity.candidateAllowed, true);
assert.equal(opportunity.candidateReviewAllowed, true);
assert.equal(opportunity.opportunityFailurePathsAllowed, true);
for (const value of [undefined, null, "", "UNKNOWN"]) {
  const unknown = contract.analysisModeGate(value);
  assert.equal(unknown.valid, false);
  assert.equal(unknown.mode, null);
  assert.equal(unknown.candidateAllowed, false);
}

const taskCases = [
  [{ state: "QUEUED", taskType: "ANALYSIS_PREVIEW" }, true, false, true, "排队中"],
  [{ state: "RUNNING", taskType: "ANALYSIS_PREVIEW" }, true, false, true, "执行中"],
  [{ state: "SUCCEEDED", taskType: "ANALYSIS_PREVIEW" }, false, false, false, "已完成"],
  [{ state: "FAILED", taskType: "ANALYSIS_PREVIEW" }, false, true, false, "失败"],
  [{ state: "PARTIAL", taskType: "ANALYSIS_PREVIEW" }, false, true, false, "分析失败"]
];
for (const [input, active, retryable, cancellable, displayLabel] of taskCases) {
  const view = contract.asyncTaskView(input);
  assert.equal(view.active, active);
  assert.equal(view.retryable, retryable);
  assert.equal(view.cancellable, cancellable);
  assert.equal(view.displayLabel, displayLabel);
}
const unavailableTask = contract.asyncTaskView({
  state: "FAILED",
  taskType: "ANALYSIS_PREVIEW",
  errorCode: "AUTHORITATIVE_OHLCV_UNAVAILABLE",
  errorMessage: "internal detail must not be shown"
});
assert.equal(unavailableTask.active, false);
assert.equal(unavailableTask.failureText, "可信市场数据尚未就绪，分析未完成");
const historicalTerminalTasks = [
  { state: "PARTIAL", taskType: "ANALYSIS_PREVIEW" },
  { state: "PARTIAL", taskType: "ANALYSIS_PREVIEW" }
];
assert.equal(historicalTerminalTasks.map(contract.asyncTaskView).filter((view) => view.active).length, 0);

const workspace = fs.readFileSync(workspacePath, "utf8");
for (const call of ["reviewResultLabel(review)", "failurePathView(", "roleGate(", "analysisModeGate(", "asyncTaskView(task)"]) {
  assert.ok(workspace.includes(call), `workspace.js must call ${call}`);
}
assert.ok(!workspace.includes("REJECT: \"拒绝候选\""), "legacy REJECT must not remain a formal mapping");
assert.ok(workspace.includes('analysisMode === "ANALYSIS_PREVIEW"'));
assert.ok(workspace.includes('analysisMode === "OPPORTUNITY_DECISION"'));

console.log("FRONTEND_CONTRACT_STATE_MATRIX=PASS");
