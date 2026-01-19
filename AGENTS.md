# Repository Guidelines

## Project Structure & Module Organization
- `src/main/kotlin/thc` and `src/main/java/thc` contain common (shared) mod code and mixins (e.g., `thc.mixin`).
- `src/client/kotlin/thc` and `src/client/java/thc` contain client-only code and mixins (e.g., `thc.mixin.client`).
- `src/main/resources` contains mod resources such as `fabric.mod.json` and `thc.mixins.json`.
- `src/client/resources` contains client resources such as `thc.client.mixins.json`.
- `src/main/resources/assets/thc` contains mod assets (e.g., `icon.png`).
- `run/` is used by Fabric Loom for local run configs and logs; `build/` is Gradle output.

## Build, Test, and Development Commands
- `./gradlew build` compiles, runs checks, and produces the mod JARs in `build/libs`.
- `./gradlew runClient` launches the Minecraft client with the mod loaded.
- `./gradlew runServer` launches a local Minecraft server with the mod loaded.
- `./gradlew runGametest` runs the headless GameTest suite (writes a report to `build/reports/gametest.xml`).
- `./gradlew runSmokeServer` starts a server and auto-shuts down after a brief smoke test (`thc.smokeTest`).
- `./gradlew runDatagen` generates data resources into the configured output directories.

## Coding Style & Naming Conventions
- Kotlin and Java code lives under the `thc` package; keep package names aligned with directory paths.
- Use standard Kotlin/Java formatting (4-space indentation, braces on the same line).
- Name mixin classes with a descriptive suffix (e.g., `*Mixin`) and keep them in `thc.mixin` or `thc.mixin.client`.

## Testing Guidelines
- Fabric GameTests live under `src/main/java/thc/gametest` and are discovered in dev/gametest runs.
- Use `./gradlew runGametest` for mixin/boot smoke coverage, and `./gradlew runSmokeServer` for a fast server startup check.
- When adding or modifying existing functionality, always run smoke tests (`./gradlew runSmokeServer`, and `./gradlew runGametest` if it exercises the changed behavior).

## Commit & Pull Request Guidelines
- No git history is available in this checkout, so there are no established commit conventions to follow.
- In pull requests, include a short summary, list of key changes, and any relevant run commands (for example, `./gradlew runClient`).
- If you change mod metadata, update `gradle.properties` and/or `src/main/resources/fabric.mod.json` accordingly.

## Configuration Notes
- Versioning and dependency coordinates live in `gradle.properties` (e.g., `mod_version`, `minecraft_version`).
- Java/Kotlin targets are set to Java 21 in `build.gradle`; keep code compatible with that level.
