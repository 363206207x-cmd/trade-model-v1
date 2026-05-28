# Answer Format Contract

Every status, progress, task-handoff, and PR-review answer must use the fields below.

Use both English and Chinese labels. Do not answer only in English or only in Chinese.

```text
Current Mainline（当前主线）:
Current Block（当前模块）:
Current Level（当前层级）:
Done Criteria（完成标准）:
Current PR（当前 PR）:
Can Merge?（能否合并）:
Next Step（下一步）:
Remaining Steps（剩余步骤）:
Do Not Do（禁止事项）:
```

## Rules

- `Current PR（当前 PR）` must say open / merged / none.
- `Can Merge?（能否合并）` must mention risk level and approval rule.
- `Next Step（下一步）` must not skip an open required PR.
- `Do Not Do（禁止事项）` must include no auto-trading when trading paths are discussed.
- Open PR / branch / Issue does not count as done.
- Merged `main` is the only completed state.
