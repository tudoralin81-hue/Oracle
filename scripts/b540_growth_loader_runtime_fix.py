from pathlib import Path

# Runtime fix after the S&P 500 performance patch.
# Growth-only. START and the frozen recommendation/history card are not rewritten here.

UA = 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36'
KOTLIN_UA = '"' + UA + '"'

M = Path('app/src/main/java/ro/alintudor/oracle/core/OracleMarketData.kt')
if M.exists():
    s = M.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', KOTLIN_UA)
    marker = '    /** Fetches OHLCV at the requested Analysis timeframe. */'
    start = s.find('    fun fetchDailyBatch(')
    end = s.find(marker, start) if start >= 0 else -1
    if start >= 0 and end > start:
        fn = '''    fun fetchDailyBatch(tickers: List<String>, range: String = "1y"): Map<String, List<OracleOhlcvPoint>> {
        val syms = tickers.map { it.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
        if (syms.isEmpty()) return emptyMap()
        val encoded = java.net.URLEncoder.encode(syms.joinToString(","), "UTF-8")
        val u = URL("https://query1.finance.yahoo.com/v7/finance/spark?symbols=$encoded&range=$range&interval=1d")
        val c = (u.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2500
            readTimeout = 6500
            setRequestProperty("User-Agent", KOTLIN_UA)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (c.responseCode !in 200..299) return emptyMap()
            val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val out = linkedMapOf<String, List<OracleOhlcvPoint>>()
            for (symbol in syms) {
                val a = root.optJSONObject(symbol) ?: continue
                val ts = a.optJSONArray("timestamp") ?: continue
                val o = a.optJSONArray("open") ?: continue
                val h = a.optJSONArray("high") ?: continue
                val l = a.optJSONArray("low") ?: continue
                val cl = a.optJSONArray("close") ?: continue
                val v = a.optJSONArray("volume")
                val rows = ArrayList<OracleOhlcvPoint>(ts.length())
                for (j in 0 until ts.length()) {
                    val oo = o.optDouble(j, Double.NaN)
                    val hh = h.optDouble(j, Double.NaN)
                    val ll = l.optDouble(j, Double.NaN)
                    val cc = cl.optDouble(j, Double.NaN)
                    if (oo.isFinite() && hh.isFinite() && ll.isFinite() && cc.isFinite() && hh > 0.0 && ll > 0.0 && cc > 0.0) {
                        rows += OracleOhlcvPoint(ts.optLong(j) * 1000L, oo, hh, ll, cc, v?.optDouble(j, 0.0) ?: 0.0)
                    }
                }
                if (rows.size >= 60) out[symbol] = rows.sortedBy { it.timestamp }
            }
            out
        } catch (_: Exception) {
            emptyMap()
        } finally {
            c.disconnect()
        }
    }

'''
        s = s[:start] + fn + s[end:]
    M.write_text(s, encoding='utf-8')

E = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
if E.exists():
    s = E.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', KOTLIN_UA)
    s = s.replace('universe.chunked(50)', 'universe.chunked(25)')
    E.write_text(s, encoding='utf-8')

# Keep the enlarged B540 loader. Do not touch the frozen bottom recommendation/history card.
P = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
if P.exists():
    s = P.read_text(encoding='utf-8')
    if 'import android.widget.ProgressBar' not in s:
        s = s.replace('import android.widget.TextView\n', 'import android.widget.TextView\nimport android.widget.ProgressBar\n', 1)
    s = s.replace('host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(255))',
                  'host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(400))')
    P.write_text(s, encoding='utf-8')
