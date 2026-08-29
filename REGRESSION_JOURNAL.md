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

## Rule 003 — Responsive sizing must not mix px and dp
- The start hero previously calculated its height by mixing `widthPixels` with `dp(20)` and `dp(560)`.
- This produced inconsistent visual scaling between phone/tablet densities.
- New controlled change uses the actual screen height in pixels for the hero container and keeps drawing coordinates normalized to the view size.
- Module shell and Growth module are intentionally untouched.

## Current recovery / known-good base
- Restored `OracleNativeModule.kt` from `88b5df7`.
- Recovery commit: `1e8e703d27148ef9d0cf560fc62ac22fbd220919`.
- Do not modify the restored shell unless a new regression is independently confirmed.

## Controlled UI iteration — 2026-08-29
- Commit: `db09cda53312ee746f3f3d441b10926deebe2a3a`.
- Scope: `MainActivity.kt` only.
- Intent: bring the Oracle start graphic closer to the supplied reference while preserving navigation, module rendering, Growth, journal and the recovered native shell.
- Changes: responsive hero height, constellation/orbital background, gold Oracle core, module icons, cleaner node proportions, and reference-style top controls.
- Rollback target if visual testing fails: `1e8e703d27148ef9d0cf560fc62ac22fbd220919`.
