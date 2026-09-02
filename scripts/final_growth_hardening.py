from pathlib import Path

# Final B535 runtime hardening.
# fix_b535_growth_runtime.py already supplies the bounded 16/25s market scan
# and 15/12s news pass. This script leaves those calculations untouched.

# Direct cause of the device NPE: ConcurrentHashMap rejects null values.
p=Path('app/src/main/java/ro/alintudor/oracle/core/OracleRealData.kt')
s=p.read_text()
s=s.replace('ConcurrentHashMap<String, String?>()', 'ConcurrentHashMap<String, String>()')
s=s.replace('companyNameCache[symbol]=remote; return remote', 'remote?.let { companyNameCache[symbol]=it }; return remote')
p.write_text(s)

# Real launcher: Growth gets a 45s wall-clock timeout and never spins forever.
p=Path('app/src/main/java/ro/alintudor/oracle/OracleMysticActivity.kt')
s=p.read_text()
if 'future.get(45, TimeUnit.SECONDS)' not in s:
    if 'import java.util.concurrent.TimeUnit' not in s:
        s=s.replace('import java.util.concurrent.Executors','import java.util.concurrent.Executors\nimport java.util.concurrent.TimeUnit')
    if 'import java.util.concurrent.TimeoutException' not in s:
        s=s.replace('import java.util.concurrent.TimeUnit','import java.util.concurrent.TimeUnit\nimport java.util.concurrent.TimeoutException')
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
print('Final Growth hardening applied: null-safe company cache + 45s launcher timeout')
