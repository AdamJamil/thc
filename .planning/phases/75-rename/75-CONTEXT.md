# Phase 75: Rename - Context

**Gathered:** 2026-02-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace all Tank references with Bastion throughout codebase and UI. Code refactoring, UI text updates, and command registration changes. No new functionality.

</domain>

<decisions>
## Implementation Decisions

### Command handling
- Only `/selectClass bastion` should work
- No backward-compatible alias for `/selectClass tank`
- Clean break from the old name

### Player data
- No migration needed — assume no existing worlds with saved Tank class data
- Fresh implementation targeting new worlds only

### Class presentation
- No flavor text or class description changes required
- Simple name substitution: "Tank" → "Bastion" wherever it appears

### Claude's Discretion
- Whether to rename internal code variables/classes or just user-facing strings
- File/class naming conventions for renamed code

</decisions>

<specifics>
## Specific Ideas

No specific requirements — straightforward find-and-replace refactoring.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 75-rename*
*Context gathered: 2026-02-03*
