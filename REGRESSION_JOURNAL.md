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
- `JURNAL ACTIVITATE` and portfolio export buttons must perform real local actions and must not be plain share/placeholder buttons.
- Export files must be written to the Android Downloads/Oracle folder on current Android versions.
- Prevention: test the portfolio interaction path after every portfolio change, not only compilation.

## Rule 004 — Excel export is the canonical journal/portfolio export model
- The supplied `AI-Stock-Oracle-Jurnal-Activitate` XLSX is the canonical export layout.
- Keep the exact 12 columns: `Data / Ora`, `Acțiune`, `Ticker`, `Acțiuni`, `Preț intrare`, `Preț vânzare`, `% la vânzare`, `Prognoză Oracle %`, `P/L realizat $`, `Rata de succes`, `ID poziție`, `Status`.
- Exported portfolio actions/positions are sourced from the persisted Oracle journal and current positions; never replace them with demo/static tickers.
- PDF must use the same columns, row order and footer semantics as the Excel model.
- The Excel export must be a real `.xlsx` file, not a renamed CSV.
- The total portfolio return must be displayed prominently at the top of both XLSX and PDF exports.
- The export must include `Status` with `ACTIVE` for current positions and `VÂNDUT` for closed positions.

## Rule 005 — Portfolio recommendations must not be recomputed from incomplete history
- Symptom: CRM, HOOD and MELI showed `RSI 0.0`, `Momentum 0.0%` and false `SELL` signals in Portfolio while the working Oracle analysis showed `HOLD`.
- Root cause: Portfolio recomputed technical indicators from a one-point local history and converted missing history into synthetic zeros.
- Required behavior: use the canonical Oracle analysis snapshot for seeded positions; preserve an existing valid action during local refresh; only compute a fallback action for genuinely new positions.
- Never turn missing technical data into a SELL signal.
- Regression check: seeded portfolio must remain `CRM HOLD 82/100`, `HOOD HOLD 95/100`, `MELI HOLD 95/100`, with the canonical technical snapshots loaded into Portfolio.

## Rule 006 — Missing support/resistance must never render as N/A when current price is available
- Symptom: CRM showed `Suport 20D N/A` and `Rezistență 20D N/A` even though the current price was available.
- Required behavior: if support/resistance is missing, zero, NaN or infinite, fall back to the position's current price.
- Prevention: Portfolio must never display `N/A` for these two fields when a valid current price exists.

## Rule 007 — Shared module shell must respect Android safe areas
- Symptom: module content could overlap the Android status/notification area at the top and the Android navigation area at the bottom.
- Required behavior: the shared `OracleNativeModule` shell applies the Android status-bar inset above the header and the navigation-bar inset below the module content.
- The module header is deliberately taller so `ORACLE` and the module title (including `PORTFOLIO`) remain fully visible above the divider.
- The scrollable module content has additional bottom space so the final controls/cards can be moved above Android navigation controls.
- This is a shared-shell fix and therefore applies to Portfolio, Alerts, News, Growth, Knowledge, Analysis, Watchlist and Journal.
- Do not reimplement per-module safe-area offsets unless a future device-specific regression proves it necessary.

## Current recovery
- Restored `OracleNativeModule.kt` from `88b5df7`.
- Recovery commit: `1e8e703d27148ef9d0cf560fc62ac22fbd220919`.
- Portfolio functional baseline includes explicit position summary, add-position flow, local journal view/export and real PDF/XLSX exports.
- Latest Portfolio V12 fixes preserve the canonical Oracle recommendations and technical indicators, use current-price fallback for missing support/resistance, and put the total portfolio return prominently in XLSX/PDF exports with active/sold status.
- Shared safe-area/header fix is now applied centrally in `OracleNativeModule.kt` so all modules receive the same top/bottom behavior.
- Do not modify the restored shell until startup/module behavior is confirmed.
