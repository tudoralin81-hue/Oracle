from pathlib import Path

# Runtime fix after the S&P 500 performance patch.
# Growth-only. START and the frozen recommendation/history card are not rewritten here.

UA = 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36'

M = Path('app/src/main/java/ro/alintudor/oracle/core/OracleMarketData.kt')
if M.exists():
    s = M.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', UA)
    M.write_text(s, encoding='utf-8')

E = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
if E.exists():
    s = E.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', UA)
    s = s.replace('universe.chunked(50)', 'universe.chunked(25)')
    E.write_text(s, encoding='utf-8')

# Keep the already-generated B540 loader implementation; only make its card taller and
# ensure the ProgressBar import exists. Do not regenerate addLoadingState() here.
P = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
if P.exists():
    s = P.read_text(encoding='utf-8')
    if 'import android.widget.ProgressBar' not in s:
        s = s.replace('import android.widget.TextView\n', 'import android.widget.TextView\nimport android.widget.ProgressBar\n', 1)
    s = s.replace('host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(255))',
                  'host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(400))')
    P.write_text(s, encoding='utf-8')
