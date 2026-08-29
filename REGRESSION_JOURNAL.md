# Oracle — Regression Journal

## Rule 001 — Build success is not visual success
- Symptom: APK build succeeded, but the application opened on a skeleton/loading screen instead of the Oracle UI.
- Evidence: user screenshot from 2026-08-29 showing `ORACLE` plus grey loading bars.
- Root-cause direction: later changes replaced the previously working `OracleNativeModule` visual shell.
- Known-good baseline: commit `88b5df79dbba1ba298c50b568daba164688605bd`.
- Recovery: restore `OracleNativeModule.kt` from that baseline before making further visual changes.
- Prevention: every UI iteration must preserve a known-good commit and must not be called ready solely because GitHub Actions is green.

## Rule 002 — Backtrack before adding another fix
When a new version regresses a previously working screen:
1. Identify the last known-good commit from actual evidence.
2. Compare the regression range, file by file.
3. Restore the smallest responsible change set.
4. Build.
5. Only then make one controlled visual change at a time.

## Current recovery
- Restored `OracleNativeModule.kt` from `88b5df7`.
- Recovery commit: `1e8e703d27148ef9d0cf560fc62ac22fbd220919`.
- Do not modify the restored shell until the startup/module behavior is confirmed.

## Automated regression cycle — 2026-08-29
- Build 95 candidate was rolled back to controlled UI commit `db09cda53312ee746f3f3d441b10926deebe2a3a` after the generated candidate contained invalid/truncated Kotlin.
- The rollback deliberately restores the last controlled Start implementation; no core, repository, Growth, or native shell changes are retained.
- CI was re-enabled for `ui/**` branches so every subsequent Start change is compiled before promotion.
- Current branch head: `d6c75b06cf78fbf56663b7385c1ce0afc4f2d589`.
