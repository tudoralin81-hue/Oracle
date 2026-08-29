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

## Rule 003 — Portfolio data and actions must be functional, not decorative
- Portfolio must show the real stored tickers, share counts and market values; do not replace them with demo/static rows.
- Portfolio must support adding another local position without changing existing positions.
- `JURNAL ACTIVITATE`, `DESCARCĂ EXCEL` and `DESCARCĂ PDF` must perform real local actions and must not be plain share/placeholder buttons.
- Export files must be written to the Android Downloads/Oracle folder on current Android versions.
- Prevention: test the portfolio interaction path after every portfolio change, not only compilation.

## Rule 004 — Excel export is the canonical journal/portfolio export model
- The supplied `AI-Stock-Oracle-Jurnal-Activitate` XLSX is the canonical export layout.
- Keep the exact 12 columns: `Data / Ora`, `Acțiune`, `Ticker`, `Acțiuni`, `Preț intrare`, `Preț vânzare`, `% la vânzare`, `Prognoză Oracle %`, `P/L realizat $`, `Rata de succes`, `ID poziție`, `Status`.
- Exported portfolio actions/positions are sourced from the persisted Oracle journal and current positions; never replace them with demo/static tickers.
- PDF must use the same columns, row order and footer semantics as the Excel model.
- The Excel export must be a real `.xlsx` file, not a renamed CSV.

## Current recovery
- Restored `OracleNativeModule.kt` from `88b5df7`.
- Recovery commit: `1e8e703d27148ef9d0cf560fc62ac22fbd220919`.
- Portfolio functional baseline now includes explicit position summary, add-position flow, local journal view/export and real PDF/XLSX exports.
- Do not modify the restored shell until the startup/module behavior is confirmed.
