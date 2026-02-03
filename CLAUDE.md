# Claude Instructions for THC

## MC 1.21+ Item Model System (CRITICAL)

**When adding new items, you MUST create THREE files:**

1. **Item Definition** - `assets/thc/items/<name>.json`
   ```json
   {
     "model": {
       "type": "minecraft:model",
       "model": "thc:item/<name>"
     }
   }
   ```

2. **Model** - `assets/thc/models/item/<name>.json`
   ```json
   {
     "parent": "minecraft:item/generated",
     "textures": {
       "layer0": "thc:item/<name>"
     }
   }
   ```

3. **Texture** - `assets/thc/textures/item/<name>.png`

**Without the `items/*.json` file, the item icon will NOT display.** This is different from older MC versions which only needed `models/item/`.

For armor worn textures, also need:
- `assets/thc/equipment/<material>.json` - equipment definition
- `assets/thc/textures/entity/equipment/humanoid/<material>.png` - worn texture

## Gradle Build Errors

If you see **"Gradle could not start your build"** with IO errors, it means the user is already running Gradle (e.g., `runClient`). **STOP IMMEDIATELY** and notify the user. Do not:
- Retry the build
- Kill Java processes
- Delete .gradle directories
- Attempt any other troubleshooting

Just tell the user: "Gradle is already running. Let me know when it's free."

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

### After Completing Any Work — COMMIT IMMEDIATELY

**THIS IS THE MOST IMPORTANT RULE IN THIS FILE.**

After ANY code change that builds successfully, you MUST:

1. **STOP EVERYTHING** — Do not answer questions, do not investigate other issues, do not continue the conversation
2. **COMMIT THE CHANGES** — Run git add and git commit immediately
3. **THEN continue** — Only after committing may you proceed to anything else

This is NOT optional. This is NOT "offer to commit." This is: **COMMIT NOW.**

Uncommitted code is **LOST CODE**. Context windows end. Sessions crash. Work vanishes. Every minute code sits uncommitted is a minute it can be lost forever.

**The pattern is:**
```
1. Make code change
2. Verify it compiles
3. IMMEDIATELY commit (do not pass go, do not collect $200)
4. Only then continue conversation
```

**If you find yourself about to say anything other than "Let me commit these changes" after a successful build, STOP. You are about to violate this rule.**

If the user explicitly declines a commit, warn LOUDLY:
> "⚠️ WARNING: These changes are NOT committed. If this session ends, the work WILL BE LOST. Are you sure?"

### Never Silently Lose Work
- Never run `git stash`, `git checkout .`, `git reset`, or `git clean` without explicit user approval
- If a command would discard changes, explain what will be lost first

## Project Context

See `.planning/PROJECT.md` for full project context, requirements, and architecture.
