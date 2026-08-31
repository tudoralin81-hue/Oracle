from pathlib import Path

app = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt')
s = app.read_text(encoding='utf-8')

# Preserve the resolved sector display. The sector-resolution step may already have
# applied this change, so both states are accepted.
old_sector = 'text = "${companyName(r.ticker)}   •   Sector: ${sector(r.ticker)}"'
new_sector = 'text = "${companyName(r.ticker)}   •   Sector: ${r.sector ?: "Sector indisponibil"}"'
if old_sector in s:
    s = s.replace(old_sector, new_sector, 1)
elif new_sector not in s:
    raise SystemExit('Analysis sector display anchor not found')

# Always replace the complete parameter loop. The previous script only replaced it
# when rawValues was absent, which allowed News to leak back into Analysis.
start_marker = '        OracleAnalysisEngine.factorNames.forEachIndexed { i, n ->'
end_marker = '        host.content.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })'
start = s.find(start_marker)
end = s.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit('Analysis parameter block anchors not found')
end += len(end_marker)

new_block = '''        OracleAnalysisEngine.factorNames.forEachIndexed { i, n ->
            // NEWS remains an internal Growth factor but is intentionally hidden from Analysis.
            if (i == 0) return@forEachIndexed
            val row = LinearLayout(host.root.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(host.dp(14), host.dp(10), host.dp(12), host.dp(10))
                background = GradientDrawable().apply {
                    setColor(if (i % 2 == 0) Color.rgb(7, 12, 23) else Color.rgb(10, 16, 29))
                    cornerRadius = host.dp(9).toFloat()
                }
            }
            row.addView(TextView(host.root.context).apply {
                text = n
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(215, 222, 235))
            }, LinearLayout.LayoutParams(0, -2, .32f))
            row.addView(TextView(host.root.context).apply {
                text = r.rawValues.getOrNull(i) ?: "Valoare indisponibilă"
                textSize = 12.5f
                setTextColor(Color.WHITE)
                gravity = Gravity.END
            }, LinearLayout.LayoutParams(0, -2, .68f))
            grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(3)) })
        }
        host.content.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })'''

s = s[:start] + new_block + s[end:]

if '// ANALYSIS_RAW_VALUES_V4' not in s:
    s = s.replace('// ANALYSIS_RAW_VALUES_V3', '// ANALYSIS_RAW_VALUES_V4', 1)

app.write_text(s, encoding='utf-8')

# Oracle scores are internal only. Analysis must show the real indicator/value.
engine = Path('app/src/main/java/ro/alintudor/oracle/core/OracleAnalysisEngine.kt')
e = engine.read_text(encoding='utf-8')
old_rr = '"ATR %.2f%% • scor R/R %.1f/100".format(Locale.US,atrPct,rr)'
new_rr = '"ATR %.2f%%".format(Locale.US,atrPct)'
if old_rr in e:
    e = e.replace(old_rr, new_rr, 1)
old_adx = '"ADX(14) ${money(adx)} • scor Oracle %.1f/100".format(Locale.US,adxScore)'
new_adx = '"ADX(14) ${money(adx)}"'
if old_adx in e:
    e = e.replace(old_adx, new_adx, 1)
if new_rr not in e:
    raise SystemExit('Analysis R/R raw-value anchor not found in OracleAnalysisEngine.kt')
if new_adx not in e:
    raise SystemExit('Analysis ADX raw-value anchor not found in OracleAnalysisEngine.kt')
engine.write_text(e, encoding='utf-8')

print('Analysis display patch applied: News hidden, sector preserved, R/R and ADX Oracle scores hidden')
