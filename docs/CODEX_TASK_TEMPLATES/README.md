# Codex Task Templates

These templates keep recurring runtime-slice prompts short and consistent.

Each template uses placeholders:

- `{module}`
- `{phase}`
- `{branch}`
- `{current_main}`
- `{next_allowed_action}`

Required sections in every template:

- task goal
- allowed changes
- forbidden changes
- required reads
- required checks
- output contract
- PR risk hint

Use `bash scripts/codex-next-task.sh` to render the next task from `docs/CODEX_NEXT_TASK.yml`.

Supported phases:

- `source_read`
- `design`
- `readiness_gate`
- `implementation`
- `verification`
- `visual_closure`
- `selection`
