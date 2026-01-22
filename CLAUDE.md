# Claude Instructions for THC

## After Minecraft Version Updates

Run `gradle runClient` immediately after updating MC version to catch mixin breakages while the upgrade is the focus. Mixin injection targets change between versions and errors are much cheaper to fix with fresh context than weeks later.

## Critical Balance Values (DO NOT CHANGE)

These values have been explicitly tuned by the user. Never modify without explicit approval:

| File | Value | Setting |
|------|-------|---------|
| `PlayerAttackMixin.java` | `0.75f` | Melee damage multiplier (25% reduction) |
| `PlayerAttackMixin.java` | `2.0F` | Crit damage multiplier (double damage) |
| `PlayerAttackMixin.java` | `0.0f` | Sweeping edge disabled |

If a plan or task would change these values, **ask first**.

## Project Context

See `.planning/PROJECT.md` for full project context, requirements, and architecture.
