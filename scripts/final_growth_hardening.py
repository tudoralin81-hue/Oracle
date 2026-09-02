from pathlib import Path
import re

# 1) Fix the device NPE: ConcurrentHashMap cannot store null values.
p=Path('app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt')
s=p.read_text()
s=s.replace('ConcurrentHashMap<String, String?>()', 'ConcurrentHashMap<String, String>()')
s=s.replace('companyNameCache[symbol]=remote; return remote', 'remote?.let { companyNameCache[symbol]=it }; return remote')
p.write_text(s)

# 2) Replace either known intermediate B535 scan with final 16-worker/25s bounded scan.
p=Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
s=p.read_text()
patterns=[
re.compile(r'(?s)val byTicker=seed\.associateBy\{it\.ticker\.uppercase\(Locale\.US\)\}.*?executor\.invokeAll\(tasks, 18, TimeUnit\.SECONDS\).*?executor\.shutdownNow\(\)'),
re.compile(r'(?s)val byTicker=seed\.associateBy\{it\.ticker\.uppercase\(Locale\.US\)\}.*?scanExecutor\.shutdownNow\(\)')]
block='''val byTicker=seed.associateBy{it.ticker.uppercase(Locale.US)}
        val candidates=java.util.Collections.synchronizedList(mutableListOf<C>())
        val tickers=universe.distinct()
        val scanExecutor=Executors.newFixedThreadPool(16)
        try {
            val futures=tickers.map { ticker ->
                scanExecutor.submit(Callable {
                    runCatching { OracleMarketData.fetchDaily(ticker,"1y") }.getOrDefault(emptyList())
                        .takeIf { it.size >= 60 }
                        ?.let { candles -> evaluate(ticker,candles) }
                })
            }
            futures.forEach { f -> runCatching { f.get(25,TimeUnit.SECONDS) }.getOrNull()?.let { candidates+=it } }
        } finally { scanExecutor.shutdownNow() }'''
for pat in patterns:
    if pat.search(s):
        s=pat.sub(block,s,count=1)
        break
else:
    raise SystemExit('Growth scan anchor missing')

# 3) Bound news enrichment to 15 workers / 12s whenever the generated news map exists.
news_pat=re.compile(r'(?m)^\s*val newsMap=.*$')
m=news_pat.search(s)
if m:
    news='''        val newsMap=mutableMapOf<String,Int>()
        val newsExecutor=Executors.newFixedThreadPool(15)
        try {
            val newsFutures=top15.map { ticker -> ticker to newsExecutor.submit(Callable { runCatching { newsScore(ticker) }.getOrDefault(0) }) }
            newsFutures.forEach { (ticker,f) -> newsMap[ticker]=runCatching { f.get(12,TimeUnit.SECONDS) }.getOrDefault(0) }
        } finally { newsExecutor.shutdownNow() }'''
    s=s[:m.start()]+news+s[m.end():]
p.write_text(s)

# 4) Real launcher: Growth calculation gets a 45s wall-clock timeout and never spins forever.
p=Path('app/src/main/java/ro/alintudor/oracle/OracleMysticActivity.kt')
s=p.read_text()
if 'future.get(45, TimeUnit.SECONDS)' not in s:
    if 'import java.util.concurrent.TimeUnit' not in s:
        s=s.replace('import java.util.concurrent.Executors','import java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit\nimport java.util.concurrent.TimeoutException')
    if 'import java.util.concurrent.Callable' not in s:
        s=s.replace('import java.util.concurrent.Executors','import java.util.concurrent.Executors\nimport java.util.concurrent.Callable')
    anchor='        if (key == "analysis") return\n'
    if anchor not in s:
        raise SystemExit('Mystic growth anchor missing')
    growth='''        if (key == "growth") {
            Thread {
                val executor = Executors.newSingleThreadExecutor()
                val future = executor.submit(Callable { OracleLocalProcessor.refreshGrowthOnly(repository) })
                try {
                    future.get(45, TimeUnit.SECONDS)
                    mainHandler.post {
                        if (currentModule != "growth" || isFinishing) return@post
                        runCatching { renderModule("growth") }.onFailure { showModuleError("growth", it) }
                    }
                } catch (e: TimeoutException) {
                    mainHandler.post {
                        if (currentModule != "growth" || isFinishing) return@post
                        showGrowthCalculationError(TimeoutException("Calculul Growth a depășit limita de 45 de secunde."))
                    }
                } catch (e: Throwable) {
                    mainHandler.post {
                        if (currentModule != "growth" || isFinishing) return@post
                        showGrowthCalculationError(e)
                    }
                } finally { executor.shutdown() }
            }.start()
            return
        }
'''
    s=s.replace(anchor,anchor+growth,1)
p.write_text(s)
print('Final Growth hardening applied: NPE cache fix + 16/25s scan + 15/12s news + 45s launcher timeout')
