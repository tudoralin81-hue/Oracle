# Growth V6 Enhanced

Branch: `growth-v6-enhanced`
Baseline: Build 234 / Oracle Growth V5.9.7

## Rules

1. Build 234 remains untouched and is the regression baseline.
2. No fabricated fundamentals, sector data, news or market data.
3. Missing data lowers confidence; it does not create fake information.
4. V5.9.7 weights remain the baseline until historical out-of-sample testing demonstrates a better alternative.
5. SHORT/MEDIUM/LONG remain independent horizons.
6. Enhanced indicators must be deterministic and reproducible from the same input snapshot.
7. Every material change must be recorded in `REGRESSION_JOURNAL.md`.
8. The 16:00 Europe/Bucharest daily snapshot/freeze contract remains unchanged.

## Enhancement targets

- Full Ichimoku component rather than a boolean flag.
- Full MACD line/signal/histogram and histogram slope where supported.
- More robust news relevance, freshness and catalyst scoring.
- Real fundamentals and sector/market factors only when authoritative data is available; otherwise neutral 50 with reduced confidence.
- Market-regime detection: bull, neutral, bear and high-volatility states.
- ATR-aware forecast using trend, momentum, volatility and regime rather than a fixed ATR multiplier alone.
- Confidence and data-quality scores.
- Historical walk-forward/backtest comparison against V5.9.7.
- Explicit PHP/original-vs-enhanced comparison tests.

## Acceptance criteria

Enhanced is not promoted to the main branch until:

- project compiles;
- regression tests pass;
- V5.9.7 baseline outputs remain reproducible;
- enhanced-vs-baseline comparison is documented;
- no hardcoded/fabricated market values are introduced;
- SHORT/MEDIUM/LONG outputs are stable under the same snapshot;
- no change is made to Build 234 itself.
