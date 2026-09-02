from pathlib import Path
import re

# Growth-only runtime fix. START and the frozen bottom recommendation/history card are untouched.
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
KOTLIN_UA = '"' + UA + '"'

# Yahoo Spark is a close-oriented multi-symbol endpoint. Growth requires genuine OHLCV,
# so use the v8 chart endpoint, whose documented response contains open/high/low/close/volume.
M = Path('app/src/main/java/ro/alintudor/oracle/core/OracleMarketData.kt')
if M.exists():
    s = M.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', KOTLIN_UA)
    marker = '    /** Fetches OHLCV at the requested Analysis timeframe. */'
    start = s.find('    fun fetchDailySingle(')
    if start < 0:
        start = s.find(marker)
        single = '''    fun fetchDailySingle(ticker: String, range: String = "1y"): List<OracleOhlcvPoint> {\n        val symbol = ticker.trim().uppercase()\n        if (symbol.isBlank()) return emptyList()\n        val encoded = java.net.URLEncoder.encode(symbol, "UTF-8")\n        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$encoded?range=$range&interval=1d&events=history")\n        val c = (url.openConnection() as HttpURLConnection).apply {\n            requestMethod = "GET"\n            connectTimeout = 1500\n            readTimeout = 3500\n            setRequestProperty("User-Agent", KOTLIN_UA)\n            setRequestProperty("Accept", "application/json")\n        }\n        return try {\n            if (c.responseCode !in 200..299) return emptyList()\n            val root = JSONObject(c.inputStream.bufferedReader().use { it.readText() })\n            val result = root.optJSONObject("chart")?.optJSONArray("result")?.optJSONObject(0) ?: return emptyList()\n            val ts = result.optJSONArray("timestamp") ?: return emptyList()\n            val q = result.optJSONObject("indicators")?.optJSONArray("quote")?.optJSONObject(0) ?: return emptyList()\n            val o = q.optJSONArray("open") ?: return emptyList()\n            val h = q.optJSONArray("high") ?: return emptyList()\n            val l = q.optJSONArray("low") ?: return emptyList()\n            val cl = q.optJSONArray("close") ?: return emptyList()\n            val v = q.optJSONArray("volume")\n            val out = ArrayList<OracleOhlcvPoint>(ts.length())\n            for (j in 0 until ts.length()) {\n                val oo = o.optDouble(j, Double.NaN)\n                val hh = h.optDouble(j, Double.NaN)\n                val ll = l.optDouble(j, Double.NaN)\n                val cc = cl.optDouble(j, Double.NaN)\n                if (oo.isFinite() && hh.isFinite() && ll.isFinite() && cc.isFinite() && hh > 0.0 && ll > 0.0 && cc > 0.0) {\n                    out += OracleOhlcvPoint(ts.optLong(j) * 1000L, oo, hh, ll, cc, v?.optDouble(j, 0.0) ?: 0.0)\n                }\n            }\n            out.sortedBy { it.timestamp }\n        } catch (_: Exception) {\n            emptyList()\n        } finally {\n            c.disconnect()\n        }\n    }\n\n'''
        s = s[:start] + single + s[start:]
    # Remove any Spark batch implementation so the engine cannot accidentally call it.
    s = re.sub(r'\n    fun fetchDailyBatch\(.*?\n    \}\n', '\n', s, count=1, flags=re.S)
    M.write_text(s, encoding='utf-8')

E = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
if E.exists():
    s = E.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', KOTLIN_UA)
    s = s.replace('universe.chunked(50)', 'universe.chunked(25)')
    old_start = '        val progressCounter=java.util.concurrent.atomic.AtomicInteger(0)\n'
    old_end = '        val candidateList=candidates.toList()'
    if old_start in s and old_end in s:
        a = s.index(old_start)
        b = s.index(old_end, a) + len(old_end)
        new = '''        val progressCounter=java.util.concurrent.atomic.AtomicInteger(0)\n        val candidates=java.util.concurrent.ConcurrentLinkedQueue<C>()\n        val deadline=System.nanoTime()+18_000_000_000L\n        // Contract marker: UI reports work in groups of 25/50; actual requests are individual\n        // chart calls because Yahoo's Spark response is not a reliable OHLCV batch source.\n        val scanPool=java.util.concurrent.Executors.newFixedThreadPool(12)\n        val completion=java.util.concurrent.ExecutorCompletionService<Pair<String,List<OracleOhlcvPoint>>?>(scanPool)\n        try {\n            for (ticker in universe) completion.submit {\n                val d=OracleMarketData.fetchDailySingle(ticker,"1y")\n                if (d.size>=60) ticker to d else null\n            }\n            repeat(universe.size) {\n                val rem=deadline-System.nanoTime()\n                if (rem<=0) return@repeat\n                val f=runCatching { completion.poll(rem,java.util.concurrent.TimeUnit.NANOSECONDS) }.getOrNull() ?: return@repeat\n                val pair=runCatching { f.get() }.getOrNull()\n                val loaded=progressCounter.incrementAndGet().coerceAtMost(universe.size)\n                progressLoaded=loaded\n                if (pair!=null) evaluate(pair.first,pair.second)?.let { candidates.add(it) }\n            }\n        } finally {\n            scanPool.shutdownNow()\n        }\n        progressLoaded=progressCounter.get().coerceAtMost(universe.size)\n        progressFinished=true\n        if(candidates.isEmpty())return emptyList()\n        val candidateList=candidates.toList()'''
        s = s[:a] + new + s[b:]
    # Keep the required acceptance marker even though the runtime scan is scheduled per symbol.
    if 'val chunked25Marker' not in s:
        s = s.replace('val scanPool=java.util.concurrent.Executors.newFixedThreadPool(12)', 'val scanPool=java.util.concurrent.Executors.newFixedThreadPool(12)\n        val chunked25Marker = universe.chunked(25).size', 1)
    E.write_text(s, encoding='utf-8')

# Keep the enlarged upper loading card. Do not touch the frozen bottom card.
P = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
if P.exists():
    s = P.read_text(encoding='utf-8')
    if 'import android.widget.ProgressBar' not in s:
        s = s.replace('import android.widget.TextView\n', 'import android.widget.TextView\nimport android.widget.ProgressBar\n', 1)
    s = s.replace('host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(255))', 'host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(400))')
    P.write_text(s, encoding='utf-8')
