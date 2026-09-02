from pathlib import Path

ROOT = Path('.')

# 1) Growth must be calculated independently when the Growth module is opened.
p = ROOT / 'app/src/main/java/ro/alintudor/oracle/MainActivity.kt'
s = p.read_text()
old = '''        if (key == "analysis") return\n        if (key == "knowledge") {'''
new = '''        if (key == "growth") {\n            Thread {\n                val result = runCatching { OracleLocalProcessor.refreshGrowthOnly(repository) }\n                mainHandler.post {\n                    if (currentModule != "growth" || isFinishing) return@post\n                    result.onSuccess {\n                        runCatching { renderModule("growth", false) }\n                            .onFailure { showModuleError("growth", it) }\n                    }.onFailure { e ->\n                        showModuleError("growth", e)\n                    }\n                }\n            }.start()\n            return\n        }\n        if (key == "analysis") return\n        if (key == "knowledge") {'''
if old not in s:
    raise SystemExit('MainActivity anchor not found')
p.write_text(s.replace(old, new, 1))

# 2) The Growth engine previously downloaded ~250 tickers strictly serially.
#    That could leave the UI on the loading state for many minutes. Keep the
#    exact same per-ticker calculations, but fetch the independent OHLCV series
#    concurrently, with bounded workers.
p = ROOT / 'app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt'
s = p.read_text()
if 'java.util.concurrent.Callable' not in s:
    s = s.replace('import kotlin.math.sqrt\n', 'import kotlin.math.sqrt\nimport java.util.concurrent.Callable\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit\n')
old_loop = '''        val byTicker=seed.associateBy{it.ticker.uppercase(Locale.US)}\n        val candidates=mutableListOf<C>()\n        for(ticker in universe.distinct()){val candles=OracleMarketData.fetchDaily(ticker,"1y");if(candles.size<60)continue;evaluate(ticker,candles)?.let{candidates+=it}}'''
new_loop = '''        val byTicker=seed.associateBy{it.ticker.uppercase(Locale.US)}\n        val candidates=mutableListOf<C>()\n        val tickers=universe.distinct()\n        val executor=Executors.newFixedThreadPool(12)\n        try {\n            val futures=tickers.map { ticker ->\n                executor.submit(Callable { ticker to OracleMarketData.fetchDaily(ticker,"1y") })\n            }\n            futures.forEach { future ->\n                runCatching { future.get(25, TimeUnit.SECONDS) }.getOrNull()?.let { (ticker,candles) ->\n                    if(candles.size>=60) evaluate(ticker,candles)?.let { candidates += it }\n                }\n            }\n        } finally {\n            executor.shutdownNow()\n        }'''
if old_loop not in s:
    raise SystemExit('Growth engine loop anchor not found')
s = s.replace(old_loop, new_loop, 1)
# Use the real sector directory and resolve company name only for the 3 selected picks.
old_meta = 'company=meta?.company?:pick.ticker,sector=sector?:"US",score=score'
new_meta = 'company=meta?.company?.takeIf{it.isNotBlank()}?:OracleMarketData.companyName(pick.ticker)?:pick.ticker,sector=OracleRealData.resolvedSector(pick.ticker,sector)?:"US",score=score'
if old_meta not in s:
    raise SystemExit('Growth metadata anchor not found')
s = s.replace(old_meta, new_meta, 1)
p.write_text(s)

# 3) Resolve company names through Yahoo quote data for only the final picks.
p = ROOT / 'app/src/main/java/ro/alintudor/oracle/core/OracleMarketData.kt'
s = p.read_text()
anchor = '''    /** Backward-compatible daily feed used by existing Oracle components. */\n    fun fetchDaily(ticker: String, range: String = "6mo"): List<OracleOhlcvPoint> = fetch(ticker, range, "1d")\n'''
insert = '''    /** Backward-compatible daily feed used by existing Oracle components. */\n    fun fetchDaily(ticker: String, range: String = "6mo"): List<OracleOhlcvPoint> = fetch(ticker, range, "1d")\n\n    /** Returns the full company name for the final Growth recommendations. */\n    fun companyName(ticker: String): String? {\n        val symbol = ticker.trim().uppercase()\n        if (symbol.isBlank()) return null\n        val url = URL("https://query1.finance.yahoo.com/v7/finance/quote?symbols=$symbol")\n        val connection = (url.openConnection() as HttpURLConnection).apply {\n            requestMethod = "GET"\n            connectTimeout = 4_000\n            readTimeout = 6_000\n            setRequestProperty("User-Agent", "Oracle-Stock-Intelligence/1.0")\n            setRequestProperty("Accept", "application/json")\n        }\n        return try {\n            if (connection.responseCode !in 200..299) return null\n            val body = connection.inputStream.bufferedReader().use { it.readText() }\n            val results = JSONObject(body).optJSONObject("quoteResponse")?.optJSONArray("result") ?: return null\n            if (results.length() == 0) return null\n            val o = results.optJSONObject(0) ?: return null\n            o.optString("longName").takeIf { it.isNotBlank() }\n                ?: o.optString("shortName").takeIf { it.isNotBlank() }\n        } catch (_: Exception) {\n            null\n        } finally {\n            connection.disconnect()\n        }\n    }\n'''
if anchor not in s:
    raise SystemExit('MarketData anchor not found')
s = s.replace(anchor, insert, 1)
p.write_text(s)

print('B535 Growth runtime patch applied')
