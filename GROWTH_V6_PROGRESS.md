# Growth V6 Enhanced — Progress

Baseline: Build 234 (`bc06a387d3a9ece099804f04ade83ed39e87b120`)
Branch: `growth-v6-enhanced`

## Implemented

- Safe enhancement layer over the existing Build 234 Growth engine.
- Cross-factor agreement scoring for trend, momentum, volume, Ichimoku, and ADX.
- Horizon-specific ranking adjustment for SHORT / MEDIUM / LONG.
- Breakout-without-volume penalty.
- Trend/momentum divergence penalty.
- Low risk/reward penalty.
- Strong ADX + trend confirmation bonus.
- Regime-aware forecast scaling.
- Conservative allocation caps for weak/low-score setups.
- No fabricated fundamentals, sector values, news or prices.
- Existing 16:00 Europe/Bucharest snapshot freeze remains unchanged.
- CI now builds `growth-v6-enhanced` automatically.

## Validation

- Kotlin engine + V6 enhancement layer compile successfully with isolated JVM stubs.
- Full Android/Gradle build must be validated by GitHub Actions on the branch.
- This is an enhancement stage, not yet a claim of proven out-of-sample superiority.

## Promotion rule

Do not merge into `main` until the branch build passes and the 234-vs-V6 comparison is documented.
