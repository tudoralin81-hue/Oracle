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

# Fundamentals must remain raw company data. If Yahoo omits marketCap but supplies
# sharesOutstanding and current price, derive market cap from those live values.
real_data = Path('app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt')
r = real_data.read_text(encoding='utf-8')
old_summary = '''        OracleFundamentals(
            sector,industry,
            num(sd,"trailingPE")?:num(ks,"trailingPE"),
            num(sd,"forwardPE")?:num(ks,"forwardPE"),
            num(fd,"revenueGrowth"),num(fd,"earningsGrowth"),num(fd,"profitMargins"),
            num(fd,"operatingMargins"),num(fd,"returnOnEquity"),num(fd,"debtToEquity"),
            num(sd,"marketCap")?:num(price,"marketCap"), ""
        )'''
new_summary = '''        val marketCap = num(sd,"marketCap") ?: num(price,"marketCap") ?: run {
            val shares = num(ks,"sharesOutstanding") ?: num(ks,"impliedSharesOutstanding")
            val px = num(price,"regularMarketPrice") ?: num(price,"regularMarketPreviousClose")
            if (shares != null && px != null) shares * px else null
        }
        OracleFundamentals(
            sector,industry,
            num(sd,"trailingPE")?:num(ks,"trailingPE"),
            num(sd,"forwardPE")?:num(ks,"forwardPE"),
            num(fd,"revenueGrowth"),num(fd,"earningsGrowth"),num(fd,"profitMargins"),
            num(fd,"operatingMargins"),num(fd,"returnOnEquity"),num(fd,"debtToEquity"),
            marketCap, ""
        )'''
if old_summary in r:
    r = r.replace(old_summary, new_summary, 1)

old_quote = '''        OracleFundamentals(
            resolvedSector(ticker),q.optString("industry").takeIf{it.isNotBlank()},
            num(q,"trailingPE"),num(q,"forwardPE"),null,null,null,null,null,null,num(q,"marketCap"),""
        )'''
new_quote = '''        val marketCap = num(q,"marketCap") ?: run {
            val shares = num(q,"sharesOutstanding") ?: num(q,"impliedSharesOutstanding")
            val px = num(q,"regularMarketPrice") ?: num(q,"regularMarketPreviousClose")
            if (shares != null && px != null) shares * px else null
        }
        OracleFundamentals(
            resolvedSector(ticker),q.optString("industry").takeIf{it.isNotBlank()},
            num(q,"trailingPE"),num(q,"forwardPE"),null,null,null,null,null,null,marketCap,""
        )'''
if old_quote in r:
    r = r.replace(old_quote, new_quote, 1)

if 'sharesOutstanding' not in r or 'regularMarketPrice' not in r:
    raise SystemExit('Fundamentals market-cap fallback was not applied')
real_data.write_text(r, encoding='utf-8')

print('Analysis raw values + fundamentals market-cap fallback patch applied')
