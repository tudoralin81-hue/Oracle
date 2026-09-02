from pathlib import Path
import re

# Growth-only runtime fix. START and the frozen bottom recommendation/history card are untouched.
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'

M = Path('app/src/main/java/ro/alintudor/oracle/core/OracleMarketData.kt')
if M.exists():
    s = M.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', UA)
    marker = '    /** Fetches OHLCV at the requested Analysis timeframe. */'
    # Remove any previous generated batch/single adapters, then install one known-good Spark adapter.
    s = re.sub(r'\n    fun fetchDailySingle\(.*?\n    \}\n', '\n', s, count=1, flags=re.S)
    s = re.sub(r'\n    fun fetchDailyBatch\(.*?\n    \}\n', '\n', s, count=1, flags=re.S)
    i = s.find(marker)
    if i < 0:
        raise SystemExit('MarketData marker not found')
    batch = '''    fun fetchDailyBatch(tickers: List<String>, range: String = "1y"): Map<String, List<OracleOhlcvPoint>> {
        val syms = tickers.map { it.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
        if (syms.isEmpty()) return emptyMap()
        val symbolList = syms.joinToString(",")
        val url = URL("https://query1.finance.yahoo.com/v7/finance/spark?symbols=$symbolList&range=$range&interval=1d&indicators=open,high,low,close,volume&includePrePost=false&corsDomain=finance.yahoo.com&.tsrc=finance")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2000
            readTimeout = 6000
            setRequestProperty("User-Agent", "__UA__")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (c.responseCode !in 200..299) return emptyMap()
            val result = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                .optJSONObject("spark")?.optJSONArray("result") ?: return emptyMap()
            buildMap {
                for (x in 0 until result.length()) {
                    val item = result.optJSONObject(x) ?: continue
                    val ticker = item.optString("symbol").uppercase()
                    val response = item.optJSONArray("response")?.optJSONObject(0) ?: continue
                    val ts = response.optJSONArray("timestamp") ?: continue
                    val q = response.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0) ?: continue
                    val o = q.optJSONArray("open") ?: continue
                    val h = q.optJSONArray("high") ?: continue
                    val l = q.optJSONArray("low") ?: continue
                    val cl = q.optJSONArray("close") ?: continue
                    val v = q.optJSONArray("volume")
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
                    if (rows.size >= 60) put(ticker, rows.sortedBy { it.timestamp })
                }
            }
        } catch (_: Exception) {
            emptyMap()
        } finally {
            c.disconnect()
        }
    }

'''.replace('__UA__', UA)
    s = s[:i] + batch + s[i:]
    M.write_text(s, encoding='utf-8')

E = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
if E.exists():
    s = E.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', UA)
    # Spark is reliable when requests are kept small; 10 symbols also gives granular progress.
    s = s.replace('universe.chunked(50)', 'universe.chunked(10)')
    s = s.replace('universe.chunked(25)', 'universe.chunked(10)')
    # Replace the single-symbol 18s scan, if present, with one global batched scan.
    old_start = '        val progressCounter=java.util.concurrent.atomic.AtomicInteger(0)\n'
    old_end = '        val candidateList=candidates.toList()'
    if old_start in s and old_end in s:
        a = s.index(old_start)
        b = s.index(old_end, a) + len(old_end)
        new = '''        val progressCounter=java.util.concurrent.atomic.AtomicInteger(0)
        val candidates=java.util.concurrent.ConcurrentLinkedQueue<C>()
        val deadline=System.nanoTime()+18_000_000_000L
        val scanPool=java.util.concurrent.Executors.newFixedThreadPool(12)
        try {
            val futures=universe.chunked(10).map { batch ->
                scanPool.submit {
                    val data=OracleMarketData.fetchDailyBatch(batch,"1y")
                    val loaded=progressCounter.addAndGet(data.size).coerceAtMost(universe.size)
                    progressLoaded=loaded
                    for ((ticker,candles) in data) {
                        if (System.nanoTime() < deadline && candles.size >= 60) evaluate(ticker,candles)?.let { candidates.add(it) }
                    }
                }
            }
            futures.forEach { f ->
                val rem=deadline-System.nanoTime()
                if (rem > 0) runCatching { f.get(rem,java.util.concurrent.TimeUnit.NANOSECONDS) }
            }
        } finally { scanPool.shutdownNow() }
        progressLoaded=progressCounter.get().coerceAtMost(universe.size)
        progressFinished=true
        if(candidates.isEmpty())return emptyList()
        val candidateList=candidates.toList()'''
        s = s[:a] + new + s[b:]
    E.write_text(s, encoding='utf-8')

# Keep the enlarged upper loading card. Do not touch the frozen bottom card.
P = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
if P.exists():
    s = P.read_text(encoding='utf-8')
    if 'import android.widget.ProgressBar' not in s:
        s = s.replace('import android.widget.TextView\n', 'import android.widget.TextView\nimport android.widget.ProgressBar\n', 1)
    s = s.replace('host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(255))', 'host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(400))')
    P.write_text(s, encoding='utf-8')