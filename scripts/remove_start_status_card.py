from pathlib import Path

p = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
s = p.read_text(encoding='utf-8')
old = '        page.addView(status,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(8),0,dp(8))})\n'
if old not in s:
    raise SystemExit('Expected Start status-card line not found')
s = s.replace(old, '', 1)
if 'ORACLE READY' in s or 'LOCAL INTELLIGENCE' in s:
    raise SystemExit('Status-card text still present in MainActivity.kt')
if 'OracleHeroView(this){ openModule(it) }' not in s:
    raise SystemExit('Approved rectangular OracleHeroView Start is not present')
p.write_text(s, encoding='utf-8')
print('Removed exactly one Start status-card line; kept approved rectangular Start.')
