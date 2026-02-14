---
phase: quick-9
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/java/thc/playerclass/ClassManager.java
  - src/main/java/thc/playerclass/AllClassesCommand.java
  - src/main/kotlin/thc/THC.kt
  - src/main/kotlin/thc/item/BucklerItem.kt
  - src/main/kotlin/thc/bow/BreezeBowItem.kt
  - src/main/kotlin/thc/item/BlazeBowItem.kt
  - src/main/java/thc/mixin/SnowballHitMixin.java
  - src/main/java/thc/mixin/BoatPlacementMixin.java
  - src/main/java/thc/mixin/PlayerAttackMixin.java
  - src/main/java/thc/boon/BoonGate.java
autonomous: true

must_haves:
  truths:
    - "Running /allClasses toggles a mode where all is<Class> checks return true"
    - "Running /allClasses again turns the mode off"
    - "With allClasses active, any player can use Buckler, Blaze Bow, Breeze Bow, land boat, snowball effects"
    - "With allClasses active, damage multipliers and health still use the player's actual class"
    - "With allClasses off, ability gates work exactly as before"
  artifacts:
    - path: "src/main/java/thc/playerclass/ClassManager.java"
      provides: "isBastion(), isMelee(), isRanged(), isSupport() methods plus allClasses toggle state"
    - path: "src/main/java/thc/playerclass/AllClassesCommand.java"
      provides: "/allClasses command registration and toggle logic"
  key_links:
    - from: "BucklerItem.kt"
      to: "ClassManager.isBastion()"
      via: "boolean gate check"
      pattern: "ClassManager\\.isBastion"
    - from: "BreezeBowItem.kt"
      to: "ClassManager.isSupport()"
      via: "boolean gate check"
      pattern: "ClassManager\\.isSupport"
    - from: "BlazeBowItem.kt"
      to: "ClassManager.isRanged()"
      via: "boolean gate check"
      pattern: "ClassManager\\.isRanged"
    - from: "AllClassesCommand.java"
      to: "ClassManager.setAllClasses()"
      via: "toggle call"
      pattern: "ClassManager\\.setAllClasses\\|ClassManager\\.toggleAllClasses"
---

<objective>
Add `is<Class>(player)` API methods to ClassManager that respect an `/allClasses` override mode, then refactor all boolean class gate checks throughout the codebase to use the new API. Numeric multiplier checks (melee/ranged damage, health bonus) remain unchanged -- they should always use the actual class.

Purpose: Enables testing all class abilities without reselecting classes, and encapsulates class-checking logic behind a clean API.
Output: New `is<Class>()` methods on ClassManager, `/allClasses` toggle command, all gate checks refactored.
</objective>

<execution_context>
@/home/tack/.claude/get-shit-done/workflows/execute-plan.md
@/home/tack/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@src/main/java/thc/playerclass/ClassManager.java
@src/main/java/thc/playerclass/PlayerClass.java
@src/main/java/thc/playerclass/SelectClassCommand.java
@src/main/kotlin/thc/THC.kt
@src/main/kotlin/thc/item/BucklerItem.kt
@src/main/kotlin/thc/bow/BreezeBowItem.kt
@src/main/kotlin/thc/item/BlazeBowItem.kt
@src/main/java/thc/mixin/SnowballHitMixin.java
@src/main/java/thc/mixin/BoatPlacementMixin.java
@src/main/java/thc/mixin/PlayerAttackMixin.java
@src/main/java/thc/mixin/AbstractArrowMixin.java
@src/main/java/thc/boon/BoonGate.java
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add is<Class> API and /allClasses command to ClassManager</name>
  <files>
    src/main/java/thc/playerclass/ClassManager.java
    src/main/java/thc/playerclass/AllClassesCommand.java
    src/main/kotlin/thc/THC.kt
  </files>
  <action>
1. In ClassManager.java, add a private static boolean field `allClassesEnabled = false` and methods:
   - `public static boolean isBastion(ServerPlayer player)` - returns true if allClassesEnabled OR getClass(player) == PlayerClass.BASTION
   - `public static boolean isMelee(ServerPlayer player)` - returns true if allClassesEnabled OR getClass(player) == PlayerClass.MELEE
   - `public static boolean isRanged(ServerPlayer player)` - returns true if allClassesEnabled OR getClass(player) == PlayerClass.RANGED
   - `public static boolean isSupport(ServerPlayer player)` - returns true if allClassesEnabled OR getClass(player) == PlayerClass.SUPPORT
   - `public static void setAllClasses(boolean enabled)` - sets allClassesEnabled
   - `public static boolean isAllClassesEnabled()` - getter for state

   IMPORTANT: Do NOT make getClass() private yet -- it is still needed by numeric multiplier paths in PlayerAttackMixin (melee multiplier), AbstractArrowMixin (ranged multiplier), ServerPlayerMixin (health bonus), and THC.kt (revival speed). These use the actual class value, not a boolean gate, so they should NOT go through is<Class>().

   IMPORTANT: Do NOT change hasClass() visibility -- it is used in SelectClassCommand and THC.kt JOIN event.

2. Create AllClassesCommand.java in `src/main/java/thc/playerclass/`:
   - Follow the same pattern as SelectClassCommand.java (CommandRegistrationCallback.EVENT.register)
   - Register literal command "allClasses" (no arguments)
   - On execute: toggle ClassManager.setAllClasses(!ClassManager.isAllClassesEnabled())
   - Send actionbar message: "All classes: ENABLED" (green) or "All classes: DISABLED" (red)
   - Make it operator-only: add `.requires(source -> source.hasPermission(2))` to the command builder
   - Return 1 on success

3. In THC.kt onInitialize(), add `AllClassesCommand.register()` right after `SelectClassCommand.register()` (line 84). Add the import for `thc.playerclass.AllClassesCommand`.
  </action>
  <verify>Run `./gradlew build` -- project compiles with new API and command.</verify>
  <done>ClassManager has isBastion/isMelee/isRanged/isSupport methods that respect allClasses toggle. /allClasses command registered and toggles the mode with operator permission.</done>
</task>

<task type="auto">
  <name>Task 2: Refactor all boolean class gates to use is<Class> API</name>
  <files>
    src/main/kotlin/thc/item/BucklerItem.kt
    src/main/kotlin/thc/bow/BreezeBowItem.kt
    src/main/kotlin/thc/item/BlazeBowItem.kt
    src/main/java/thc/mixin/SnowballHitMixin.java
    src/main/java/thc/mixin/BoatPlacementMixin.java
    src/main/java/thc/mixin/PlayerAttackMixin.java
    src/main/java/thc/boon/BoonGate.java
  </files>
  <action>
Refactor each boolean class gate to use the new is<Class> API. In each file, replace the pattern of `ClassManager.getClass(player) != PlayerClass.X` with `ClassManager.is<X>(player)`. Remove unused `PlayerClass` imports where they are no longer needed.

**BucklerItem.kt (line 26-28):**
- Replace `val playerClass = ClassManager.getClass(player)` + `if (playerClass != PlayerClass.BASTION || boonLevel < 2)`
- With: `if (!ClassManager.isBastion(player) || boonLevel < 2)`
- Remove the `playerClass` variable (no longer needed)
- Remove `import thc.playerclass.PlayerClass`

**BreezeBowItem.kt (line 22-24):**
- Replace `val playerClass = ClassManager.getClass(player)` + `if (playerClass != PlayerClass.SUPPORT || boonLevel < 2)`
- With: `if (!ClassManager.isSupport(player) || boonLevel < 2)`
- Remove the `playerClass` variable
- Remove `import thc.playerclass.PlayerClass`

**BlazeBowItem.kt (line 22-24):**
- Replace `val playerClass = ClassManager.getClass(player)` + `if (playerClass != PlayerClass.RANGED || boonLevel < 2)`
- With: `if (!ClassManager.isRanged(player) || boonLevel < 2)`
- Remove the `playerClass` variable
- Remove `import thc.playerclass.PlayerClass`

**SnowballHitMixin.java (line 59-61):**
- Replace `PlayerClass playerClass = ClassManager.getClass(player);` + `if (playerClass != PlayerClass.BASTION)`
- With: `if (!ClassManager.isBastion(player))`
- Remove unused `import thc.playerclass.PlayerClass;`

**BoatPlacementMixin.java (line 70-73):**
- Replace `PlayerClass playerClass = ClassManager.getClass(sp);` + `if (playerClass != PlayerClass.BASTION || boonLevel < 5)`
- With: `if (!ClassManager.isBastion(sp) || boonLevel < 5)`
- Remove unused `import thc.playerclass.PlayerClass;`

**BoonGate.java (line 22-24):**
- Replace `PlayerClass playerClass = ClassManager.getClass(player);` + `if (playerClass != PlayerClass.BASTION)`
- With: `if (!ClassManager.isBastion(player))`
- Remove unused `import thc.playerclass.PlayerClass;`

**DO NOT TOUCH these files -- they use numeric class values, not boolean gates:**
- PlayerAttackMixin.java lines 38-41 (melee multiplier uses `playerClass.getMeleeMultiplier()`)
- AbstractArrowMixin.java lines 88-91 (ranged multiplier uses `playerClass.getRangedMultiplier()`)
- ServerPlayerMixin.java (health bonus uses `playerClass.getHealthBonus()`)
- THC.kt lines 322-327 (revival speed uses `playerClass == PlayerClass.SUPPORT` for a numeric rate -- but wait, this IS a boolean gate for the SUPPORT class giving faster revival speed. Refactor this one too: replace `val playerClass = ClassManager.getClass(reviver)` + `if (playerClass == PlayerClass.SUPPORT)` with `if (ClassManager.isSupport(reviver))`. Remove the `playerClass` local variable. Keep both `ClassManager` and `PlayerClass` imports in THC.kt since PlayerClass is still used elsewhere in the file -- actually check: after this refactor, grep the remaining THC.kt file for PlayerClass usage. If none remain, remove the import.)

**PlayerAttackMixin.java sweeping edge (line 77):** This uses `BoonGate.hasStage3Boon()` which internally checks Bastion. Since we are updating BoonGate.java itself in this task, the sweeping edge path is covered transitively. No changes needed in PlayerAttackMixin.java.
  </action>
  <verify>
1. `./gradlew build` succeeds
2. Grep confirms no remaining `getClass(` calls in the refactored files (BucklerItem, BreezeBowItem, BlazeBowItem, SnowballHitMixin, BoatPlacementMixin, BoonGate)
3. Grep confirms `getClass(` still exists in ClassManager.java (the method), PlayerAttackMixin.java (melee multiplier), AbstractArrowMixin.java (ranged multiplier), ServerPlayerMixin.java (health)
  </verify>
  <done>All boolean class gates use is<Class>() API. Numeric multiplier paths untouched. When /allClasses is toggled on, all ability gates (buckler, bows, snowball, boat, sweeping edge) pass for any class. Damage multipliers and health still respect actual class.</done>
</task>

</tasks>

<verification>
1. `./gradlew build` passes
2. `grep -rn "ClassManager.isBastion\|ClassManager.isMelee\|ClassManager.isRanged\|ClassManager.isSupport" src/` shows all refactored call sites
3. `grep -rn "ClassManager.getClass" src/` shows ONLY: ClassManager.java (definition), PlayerAttackMixin.java, AbstractArrowMixin.java, ServerPlayerMixin.java, and THC.kt (only if revival speed was left as-is)
4. No remaining `PlayerClass.BASTION` comparisons in gate files (only in numeric multiplier files and ClassManager itself)
</verification>

<success_criteria>
- /allClasses command registered, operator-only, toggles mode with feedback message
- ClassManager.isBastion/isMelee/isRanged/isSupport all return true when allClasses enabled
- All boolean ability gates (buckler, breeze bow, blaze bow, snowball, boat placement, sweeping edge via BoonGate) use is<Class> API
- Numeric multiplier paths (melee damage, ranged damage, health bonus) unchanged -- still use getClass() directly
- Project builds successfully
</success_criteria>

<output>
After completion, create `.planning/quick/9-add-allclasses-override-command-with-is-/9-SUMMARY.md`
</output>
