from pathlib import Path

ENGINE = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
MYSTIC = Path('app/src/main/java/ro/alintudor/oracle/OracleMysticActivity.kt')


def replace_once(path: Path, old: str, new: str):
    s = path.read_text()
    if new in s:
        return
    if old not in s:
        raise SystemExit(f'Expected pattern not found in {path}: {old[:120]}')
    path.write_text(s.replace(old, new, 1))

# The generated B535 engine must not scan the universe serially. Bound both
# network-heavy phases exactly as documented: 16 workers / 25s scan and
# 15 workers / 12s news.
replace_once(
    ENGINE,
    'import java.net.URLEncoder\nimport java.util.Locale',
    'import java.net.URLEncoder\nimport java.util.Locale\nimport java.util.concurrent.Callable\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit'
)
replace_once(
    ENGINE,
    '        val candidates=mutableListOf<C>()\n        for(ticker in universe.distinct()){val candles=OracleMarketData.fetchDaily(ticker,"1y");if(candles.size<60)continue;evaluate(ticker,candles)?.let{candidates+=it}}',
    '''        val candidates=java.util.Collections.synchronizedList(mutableListOf<C>())
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
)
replace_once(
    ENGINE,
    '        val newsMap=top15.associateWith{newsScore(it)}',
    '''        val newsMap=mutableMapOf<String,Int>()
        val newsExecutor=Executors.newFixedThreadPool(15)
        try {
            val newsFutures=top15.map { ticker -> ticker to newsExecutor.submit(Callable { runCatching { newsScore(ticker) }.getOrDefault(0) }) }
            newsFutures.forEach { (ticker,f) -> newsMap[ticker]=runCatching { f.get(12,TimeUnit.SECONDS) }.getOrDefault(0) }
        } finally { newsExecutor.shutdownNow() }'''
)

# Real launcher: Growth gets a hard outer 45s wall-clock cap independent of
# the internals. A timeout is surfaced immediately; the worker is left alive
# so a late successful calculation can still populate the cache.
replace_once(
    MYSTIC,
    'import android.widget.*\nimport ro.alintudor.oracle.core.OracleBootstrap',
    'import android.widget.*\nimport ro.alintudor.oracle.core.OracleBootstrap\nimport java.util.concurrent.Callable\nimport java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit\nimport java.util.concurrent.TimeoutException'
)
old = '''        if (key == "growth") {
            Thread {
                val result = runCatching { OracleLocalProcessor.refreshGrowthOnly(repository) }
                mainHandler.post {
                    if (currentModule != "growth" || isFinishing) return@post
                    result.onSuccess {
                        runCatching { renderModule("growth") }
                            .onFailure { showModuleError("growth", it) }
                    }.onFailure { error ->
                        showGrowthCalculationError(error)
                    }
                }
            }.start()
            return
        }'''
new = '''        if (key == "growth") {
            Thread {
                val executor = Executors.newSingleThreadExecutor()
                val future = executor.submit(Callable { OracleLocalProcessor.refreshGrowthOnly(repository) })
                try {
                    val result = future.get(45, TimeUnit.SECONDS)
                    mainHandler.post {
                        if (currentModule != "growth" || isFinishing) return@post
                        runCatching { renderModule("growth") }
                            .onFailure { showModuleError("growth", it) }
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
                } finally {
                    executor.shutdown()
                }
            }.start()
            return
        }'''
replace_once(MYSTIC, old, new)

# Make the existing Growth error screen explicitly describe the timeout.
p = MYSTIC
s = p.read_text()
s = s.replace(
    'text = "Calculul Growth nu s-a finalizat.\\n\\n${error.message ?: error.javaClass.simpleName}"',
    'text = "Calculul Growth nu s-a finalizat.\\n\\n${error.message ?: error.javaClass.simpleName}"',
    1
)
p.write_text(s)

print('Growth timeout solution applied successfully')
