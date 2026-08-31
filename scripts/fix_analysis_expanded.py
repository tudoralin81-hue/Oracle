from pathlib import Path

# Idempotent build-time patch for Analysis. The extra metrics are display-only:
# the canonical 12-factor Oracle weights and scores are not changed.

app = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt')
s = app.read_text(encoding='utf-8')

if '// ANALYSIS_PARAMETERS_V6' not in s:
    start = s.find('        host.addSectionLabel("PARAMETRII ORACLE • VALORI")')
    end = s.find('        host.addSectionLabel("ANALIZĂ ORACLE")', start)
    if start < 0 or end < 0:
        raise SystemExit('Analysis parameter anchors missing')
    block = '''        // ANALYSIS_PARAMETERS_V6
        // NEWS remains internal to Growth and is intentionally hidden here.
        host.addSectionLabel("PARAMETRII ORACLE • VALORI")
        val oracleGrid = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        val visibleFactors = OracleAnalysisEngine.factorNames.mapIndexedNotNull { i, name ->
            if (i == 0) null else name to (r.rawValues.getOrNull(i) ?: "Valoare indisponibilă")
        }
        addMetricGrid(oracleGrid, visibleFactors)
        host.content.addView(oracleGrid, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(8)) })

        host.addSectionLabel("INDICATORI SUPLIMENTARI")
        val extraGrid = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        addMetricGrid(extraGrid, listOf(
            "RSI (14)" to fmt(r.rsi),
            "MACD (12/26)" to metricPair(r.macd, r.macdSignal),
            "52W HIGH / LOW" to "${moneyOrDash(r.week52High)} / ${moneyOrDash(r.week52Low)}",
            "ATR" to "${money(r.atrValue)}  •  ${fmt(r.atrPct)}%"
        ))
        host.content.addView(extraGrid, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(8)) })

        host.addSectionLabel("FUNDAMENTALE")
        val f = r.fundamentals
        val fundamentalsGrid = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        addMetricGrid(fundamentalsGrid, listOf(
            "Sector" to (f?.sector ?: r.sector ?: "—"),
            "Industry" to (f?.industry ?: "—"),
            "P/E" to num2(f?.trailingPe),
            "Fwd P/E" to num2(f?.forwardPe),
            "P/B" to num2(f?.priceToBook),
            "Revenue growth" to pctFund(f?.revenueGrowth),
            "Earnings growth" to pctFund(f?.earningsGrowth),
            "Net margin" to pctFund(f?.profitMargin),
            "Operating margin" to pctFund(f?.operatingMargin),
            "ROE" to pctFund(f?.returnOnEquity),
            "D/E" to num2(f?.debtToEquity),
            "Current ratio" to num2(f?.currentRatio),
            "Quick ratio" to num2(f?.quickRatio),
            "Beta" to num2(f?.beta),
            "Market cap" to capText(f?.marketCap)
        ))
        host.content.addView(fundamentalsGrid, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })

'''
    s = s[:start] + block + s[end:]

if 'private fun addMetricGrid(' not in s:
    marker = '    private fun addTechnicalChart(ticker: String) {'
    idx = s.find(marker)
    if idx < 0:
        raise SystemExit('Analysis chart anchor missing')
    helpers = '''    private fun addMetricGrid(container: LinearLayout, items: List<Pair<String, String>>) {
        var row: LinearLayout? = null
        items.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                row = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                container.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(6)) })
            }
            val card = LinearLayout(host.root.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(host.dp(11), host.dp(9), host.dp(11), host.dp(9))
                background = GradientDrawable().apply { setColor(Color.rgb(6, 12, 24)); cornerRadius = host.dp(12).toFloat(); setStroke(host.dp(1), Color.rgb(35, 65, 98)) }
            }
            card.addView(TextView(host.root.context).apply {
                text = item.first.uppercase(Locale.US); textSize = 10f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = .07f; setTextColor(Color.rgb(85, 190, 235))
            })
            card.addView(TextView(host.root.context).apply {
                text = item.second; textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, host.dp(4), 0, 0); maxLines = 4
            })
            row?.addView(card, LinearLayout.LayoutParams(0, -2, 1f).apply { if (index % 2 == 1) setMargins(host.dp(4), 0, 0, 0) else setMargins(0, 0, host.dp(4), 0) })
        }
    }
    private fun metricPair(value: Double?, signal: Double?): String = "${num2(value)}  •  SIG ${num2(signal)}"
    private fun num2(value: Double?): String = value?.let { "%.2f".format(Locale.US, it) } ?: "—"
    private fun pctFund(value: Double?): String = value?.let { "%.2f%%".format(Locale.US, it * 100.0) } ?: "—"
    private fun capText(value: Double?): String = when { value == null -> "—"; value >= 1e12 -> "%.2fT".format(Locale.US, value / 1e12); value >= 1e9 -> "%.2fB".format(Locale.US, value / 1e9); value >= 1e6 -> "%.2fM".format(Locale.US, value / 1e6); else -> "%.0f".format(Locale.US, value) }

'''
    s = s[:idx] + helpers + s[idx:]

app.write_text(s, encoding='utf-8')

engine = Path('app/src/main/java/ro/alintudor/oracle/core/OracleAnalysisEngine.kt')
e = engine.read_text(encoding='utf-8')
if '// ANALYSIS_TECH_EXTRAS_V1' not in e:
    e = e.replace('object OracleAnalysisEngine {', 'object OracleAnalysisEngine {\n    // ANALYSIS_TECH_EXTRAS_V1', 1)
    old = 'val momentum20D:Double,val volumeRatio:Double,val sma50:Double?,val sma200:Double?,val adx:Double?,val atrPct:Double,val factors:List<Double>,val rawValues:List<String>'
    new = 'val momentum20D:Double,val volumeRatio:Double,val sma50:Double?,val sma200:Double?,val adx:Double?,val atrPct:Double,val atrValue:Double,val macd:Double?,val macdSignal:Double?,val week52High:Double?,val week52Low:Double?,val fundamentals:OracleFundamentals?,val factors:List<Double>,val rawValues:List<String>'
    if old not in e: raise SystemExit('Engine Result anchor missing')
    e = e.replace(old, new, 1)
    old = 'val atr=atr(high,low,close,14)?:p*.01;val atrPct=100*atr/p;val adx=adx(high,low,close,14)'
    new = 'val atr=atr(high,low,close,14)?:p*.01;val atrPct=100*atr/p;val adx=adx(high,low,close,14);val macdPair=macd(close);val week52=close.take(252)'
    if old not in e: raise SystemExit('Engine calculation anchor missing')
    e = e.replace(old, new, 1)
    old = 'return Result(ticker,p,ss,ms,ls,signal,risk,allocation,resolvedSector,null,rsi,m5,m20,vr,s50,s200,adx,atrPct,factors,rawValues)'
    new = 'return Result(ticker,p,ss,ms,ls,signal,risk,allocation,resolvedSector,null,rsi,m5,m20,vr,s50,s200,adx,atrPct,atr,macdPair.first,macdPair.second,week52.maxOrNull(),week52.minOrNull(),fundamentals,factors,rawValues)'
    if old not in e: raise SystemExit('Engine Result return anchor missing')
    e = e.replace(old, new, 1)
    marker = '    private fun money(v:Double?):String='
    idx = e.find(marker)
    if idx < 0: raise SystemExit('Engine helper anchor missing')
    helpers = '''    private fun macd(valuesDesc:List<Double>):Pair<Double?,Double?> {
        if(valuesDesc.size<35) return null to null
        val values=valuesDesc.asReversed()
        val e12=mutableListOf<Double>(); val e26=mutableListOf<Double>()
        var a12=values.take(12).average(); var a26=values.take(26).average()
        e12 += a12
        for(i in 12 until values.size){ a12=values[i]*(2.0/13.0)+a12*(11.0/13.0); e12+=a12 }
        e26 += a26
        for(i in 26 until values.size){ a26=values[i]*(2.0/27.0)+a26*(25.0/27.0); e26+=a26 }
        val series=mutableListOf<Double>()
        for(i in 25 until values.size) series += e12[i-11]-e26[i-25]
        if(series.isEmpty()) return null to null
        var signal=series.take(9).average(); val alpha=2.0/10.0
        for(i in 9 until series.size) signal=series[i]*alpha+signal*(1.0-alpha)
        return series.lastOrNull() to signal
    }
'''
    e = e[:idx] + helpers + e[idx:]
engine.write_text(e, encoding='utf-8')

real_data = Path('app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt')
r = real_data.read_text(encoding='utf-8')
if '// FUNDAMENTALS_V2' not in r:
    r = r.replace('object OracleRealData {', 'object OracleRealData {\n    // FUNDAMENTALS_V2', 1)
    old = 'val marketCap: Double?, val rawText: String'
    new = 'val marketCap: Double?, val priceToBook: Double?, val currentRatio: Double?, val quickRatio: Double?, val beta: Double?, val rawText: String'
    if old not in r: raise SystemExit('Fundamentals data class anchor missing')
    r = r.replace(old, new, 1)
    old = 'val cap=summary?.marketCap ?: quote?.marketCap ?: ts?.marketCap\n        if (listOf(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap).all { it == null }) return null\n        return OracleFundamentals(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap,\n            buildFundamentalText(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap))'
    new = 'val cap=summary?.marketCap ?: quote?.marketCap ?: ts?.marketCap\n        val pb=summary?.priceToBook ?: quote?.priceToBook\n        val cr=summary?.currentRatio ?: quote?.currentRatio\n        val qr=summary?.quickRatio ?: quote?.quickRatio\n        val beta=summary?.beta ?: quote?.beta\n        if (listOf(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap,pb,cr,qr,beta).all { it == null }) return null\n        return OracleFundamentals(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap,pb,cr,qr,beta,\n            buildFundamentalText(sector,industry,pe,fpe,rg,eg,pm,om,roe,de,cap,pb,cr,qr,beta))'
    if old not in r: raise SystemExit('Fundamentals aggregation anchor missing')
    r = r.replace(old, new, 1)
    old = 'marketCap, ""\n        )'
    new = 'marketCap,\n            num(sd,"priceToBook")?:num(ks,"priceToBook"),\n            num(fd,"currentRatio"),num(fd,"quickRatio"),num(sd,"beta"), ""\n        )'
    if old not in r: raise SystemExit('Summary constructor anchor missing')
    r = r.replace(old, new, 1)
    old = 'num(q,"trailingPE"),num(q,"forwardPE"),null,null,null,null,null,null,marketCap,""'
    new = 'num(q,"trailingPE"),num(q,"forwardPE"),null,null,null,null,null,null,marketCap,\n            num(q,"priceToBook"),num(q,"currentRatio"),num(q,"quickRatio"),num(q,"beta"),""'
    if old not in r: raise SystemExit('Quote constructor anchor missing')
    r = r.replace(old, new, 1)
    old = 'return OracleFundamentals(sector,industry,null,null,rg,eg,pm,om,roe,de,marketCap,"")'
    new = 'return OracleFundamentals(sector,industry,null,null,rg,eg,pm,om,roe,de,marketCap,null,null,null,null,"")'
    if old not in r: raise SystemExit('Timeseries constructor anchor missing')
    r = r.replace(old, new, 1)
    old = 'private fun buildFundamentalText(sector:String?,industry:String?,pe:Double?,fpe:Double?,rg:Double?,eg:Double?,pm:Double?,om:Double?,roe:Double?,de:Double?,cap:Double?):String'
    new = 'private fun buildFundamentalText(sector:String?,industry:String?,pe:Double?,fpe:Double?,rg:Double?,eg:Double?,pm:Double?,om:Double?,roe:Double?,de:Double?,cap:Double?,pb:Double?,cr:Double?,qr:Double?,beta:Double?):String'
    if old not in r: raise SystemExit('Fundamental text signature anchor missing')
    r = r.replace(old, new, 1)
    old = 'append("D/E=${de?.let{"%.2f".format(Locale.US,it)}?:"—"}; Market cap=${moneyCap(cap)}")'
    new = 'append("D/E=${de?.let{"%.2f".format(Locale.US,it)}?:"—"}; P/B=${pb?.let{"%.2f".format(Locale.US,it)}?:"—"}; ")\n        append("Current ratio=${cr?.let{"%.2f".format(Locale.US,it)}?:"—"}; Quick ratio=${qr?.let{"%.2f".format(Locale.US,it)}?:"—"}; Beta=${beta?.let{"%.2f".format(Locale.US,it)}?:"—"}; Market cap=${moneyCap(cap)}")'
    if old not in r: raise SystemExit('Fundamental text body anchor missing')
    r = r.replace(old, new, 1)
real_data.write_text(r, encoding='utf-8')

print('Expanded Analysis parameters and fundamentals applied')
