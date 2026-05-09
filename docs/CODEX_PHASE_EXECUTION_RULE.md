# CODEX_PHASE_EXECUTION_RULE — Codex 阶段执行规则

## 1. 固定工作流

Codex 每一轮必须按以下流程执行：

1. 读取规则
2. 只读审计
3. 输出差距
4. 提出一个最小交付物
5. 等用户确认
6. 只实施这个最小交付物
7. 编译验证
8. 运行必要单测
9. 更新收口文档
10. 输出下一步最小动作

涉及方向输出、候选计划、执行建议、推送、持仓监控、复盘时，必须先审计以下治理边界是否受影响：

- AI conflict
- confused state
- Push Recheck
- Missed Opportunity
- Hot Reset

如果护栏未覆盖完整方案，必须先更新护栏再实施 Java。

不允许在上述治理边界未冻结前推进：

- 生产 `VALID`
- mapper
- Assembler
- `plan_boundary_json`
- 方向性推送
- 自动化执行链路

## 2. 读取规则阶段

每轮先读取：

- AGENTS.md
- docs/V1_FRAMEWORK_LOCK.md
- docs/CODEX_PHASE_EXECUTION_RULE.md
- docs/PHASE_POSITION_MONITOR_FREEZE_INDEX.md，如果存在
- 与本轮任务相关的最近 closure 文档
- 涉及治理边界时，读取完整方案或最新治理 closure

未读取规则前，不允许修改代码。

## 3. 只读审计阶段

只读审计阶段允许：

- 读取文件
- 搜索代码
- 查看文档
- 输出差距

只读审计阶段禁止：

- 修改文件
- 新增文件
- 删除文件
- 运行编译
- 改代码
- 改页面
- 改 schema

审计输出必须包含：

1. 当前阶段判断
2. 已读取到的关键规则
3. 扫描到的相关文件
4. 已实现
5. 部分实现
6. 后端-only
7. 前端-only
8. 冲突
9. 未实现
10. 下一步最小实施建议
11. 预计修改文件
12. 本轮不做什么
13. 验收命令

涉及方向、候选、推送、持仓监控、复盘时，审计输出还必须说明是否影响：

- AI conflict
- confused state
- Push Recheck
- Missed Opportunity
- Hot Reset

## 4. 最小交付物规则

每轮只能做一个最小交付物。

最小交付物必须满足：

- 文件范围小
- 可编译
- 可验证
- 可回滚
- 不跨多个业务方向
- 不重构无关模块
- 不修改首页 UI，除非本轮明确要求
- 不改变 JSON 契约，除非本轮明确要求
- 不引入新依赖，除非本轮明确要求
- 不绕过 AI conflict / confused state / Push Recheck / Missed Opportunity / Hot Reset 治理边界

## 5. 实施阶段规则

进入实施前必须得到用户明确确认。

实施阶段允许：

- 修改本轮指定文件
- 新增本轮指定文件
- 运行 ./mvnw clean compile
- 运行本轮相关测试

实施阶段禁止：

- 修改项目目录以外文件
- 联网安装依赖
- 大规模删除
- 大重构
- 修改无关模块
- 自动下单
- 自动平仓
- 把候选机会当成真实持仓
- 在治理边界未冻结前推进生产 `VALID`、mapper、Assembler、`plan_boundary_json`、方向性推送或自动化执行链路
- 让 AI 直接触发下单、平仓、`triggered`、`VALID` 或可执行计划
- 让 Loader context 足量直接推出 candidate `VALID`

## 6. 编译验证规则

默认编译命令：

./mvnw clean compile

如 Maven 因网络或 ~/.m2 权限失败，必须说明失败原因，不得私自联网下载或修改工作区外文件。

如果编译失败，只允许修复本轮引入的问题。

## 7. 收口文档规则

每轮实施成功后，必须新增或更新一个收口文档。

收口文档必须包含：

1. 当前阶段
2. 本轮目标
3. 修改文件
4. 实现内容
5. 未实现内容
6. 明确未做
7. 编译结果
8. 测试结果
9. 风险
10. 下一步最小动作

收口文档优先放在 docs/ 目录。

命名建议：

docs/PHASE_<主题>_<动作>_CLOSURE.md

## 8. 输出格式规则

每轮最终输出必须包含：

一、当前阶段
二、本轮目标
三、修改文件清单
四、实现内容
五、未实现内容
六、编译结果
七、测试结果
八、是否触碰禁止事项
九、本轮明确未做
十、下一步最小动作
十一、是否影响 AI conflict
十二、是否影响 confused state
十三、是否影响 Push Recheck
十四、是否影响 Missed Opportunity
十五、是否影响 Hot Reset
