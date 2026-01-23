# Claude Instructions for THC

## After Minecraft Version Updates

Run `gradle runClient` immediately after updating MC version to catch mixin breakages while the upgrade is the focus. Mixin injection targets change between versions and errors are much cheaper to fix with fresh context than weeks later.

## Critical Balance Values (DO NOT CHANGE)

These values have been explicitly tuned by the user. Never modify without explicit approval:

| File | Value | Setting |
|------|-------|---------|
| `PlayerAttackMixin.java` | `0.1875f` | Melee damage multiplier (81.25% reduction) |
| `PlayerAttackMixin.java` | `2.0F` | Crit damage multiplier (double damage) |
| `PlayerAttackMixin.java` | `0.0f` | Sweeping edge disabled |

If a plan or task would change these values, **ask first**.

## Git Safety Rules (MANDATORY)

### Before Starting Any Work
Run `git status` first. If there are uncommitted changes:
1. **STOP** and warn the user immediately
2. List the uncommitted files
3. Ask if they want to: commit them, stash them, or discard them
4. **Never** automatically stash or revert uncommitted changes

### After Completing Any Work
Always offer to commit completed work, even for "quick patches" outside GSD. Say:
> "Would you like me to commit these changes?"

If the user declines or the conversation is ending without a commit, warn:
> "Warning: These changes are uncommitted and could be lost."

### Never Silently Lose Work
- Never run `git stash`, `git checkout .`, `git reset`, or `git clean` without explicit user approval
- If a command would discard changes, explain what will be lost first

## Project Context

See `.planning/PROJECT.md` for full project context, requirements, and architecture.
