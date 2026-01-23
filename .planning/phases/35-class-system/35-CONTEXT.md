# Phase 35: Class System - Context

**Gathered:** 2026-01-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement `/selectClass <tank|melee|ranged|support>` command that permanently assigns a role with health/damage modifiers. Selection only works in base chunks. Class persists across sessions.

</domain>

<decisions>
## Implementation Decisions

### Stat Modifiers (from spec)
- Tank: +1 heart max health, x2.5 melee damage, x1 ranged damage
- Melee: +0.5 hearts max health, x4 melee damage, x1 ranged damage
- Ranged: no health change, x1 melee damage, x5 ranged damage
- Support: no health change, x1 melee damage, x3 ranged damage

### Selection Feedback
- Success: Chat message + centered title announcement
- Title shows class name prominently

### Error Feedback
- Actionbar text (temporary message above hotbar) for errors
- Errors: wrong location, already has class, invalid class name

### Class Query
- No query command needed
- Players remember their class, no `/class` or `/selectClass` info mode

### On Join Behavior
- No notification when joining
- Modifiers apply silently

### Claude's Discretion
- Title duration and formatting
- Exact error message wording
- Attachment key naming conventions

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 35-class-system*
*Context gathered: 2026-01-22*
