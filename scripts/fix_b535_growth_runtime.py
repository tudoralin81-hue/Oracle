from pathlib import Path

ROOT = Path('.')

# Idempotent B535 runtime patch. The B535 generator has several source variants;
# this patch only applies a change when that change is not already present.

# 1) Growth must calculate independently when the Growth module is opened.
p = ROOT / 'app/src/main/java/ro/alintudor/oracle/MainActivity.kt'
s = p.read_text()
if 'OracleLocalProcessor.refreshGrowthOnly(repository)' not in s:
    old = '        if (key == "analysis") return\n        if (key == "knowledge") {'
    new = '''        if (key == "growth") {\n            Thread {\n                val result = runCatching { OracleLocalProcessor.refreshGrowthOnly(repository) }\n                mainHandler.post {\n                    if (currentModule != "growth" || isFinishing) return@post\n                    result.onSuccess { runCatching { renderModule("growth", false) }.onFailure { showModuleError("growth", it) } }\n                        .onFailure { e -> showModuleError("growth", e) }\n                }\n            }.start()\n            return\n        }\n        if (key == "analysis") return\n        if (key == "knowledge") {'''
    if old in s:
        p.write_text(s.replace(old, new, 1))

# 2) The Growth engine originally fetched the full universe serially.
p = ROOT / 'app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt'
s = p.read_text()
if 'Executors.newFixedThreadPool' not in s:
    if 'java.util.concurrent.Callable' not in s:
        s = s.replace('import kotlin.math.sqrt\n', 'import kotlin.math.sqrt\nimport java.util.concurrent.Callable\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit\n')
    old = '''        val byTicker=seed.associateBy{it.ticker.uppercase(Locale.US)}\n        val candidates=mutableListOf<C>()\n        for(ticker in universe.distinct()){val candles=OracleMarketData.fetchDaily(ticker,"1y");if(candles.size<60)continue;evaluate(ticker,candles)?.let{candidates+=it}}'''
    new = '''        val byTicker=seed.associateBy{it.ticker.uppercase(Locale.US)}\n        val candidates=mutableListOf<C>()\n        val tickers=universe.distinct()\n        val executor=Executors.newFixedThreadPool(12)\n        try {\n            val futures=tickers.map { ticker -> executor.submit(Callable { ticker to OracleMarketData.fetchDaily(ticker,"1y") }) }\n            futures.forEach { future -> runCatching { future.get(25, TimeUnit.SECONDS) }.getOrNull()?.let { (ticker,candles) -> if(candles.size>=60) evaluate(ticker,candles)?.let { candidates += it } } }\n        } finally { executor.shutdownNow() }'''
    if old in s:
        p.write_text(s.replace(old, new, 1))

print('B535 Growth runtime patch applied')
