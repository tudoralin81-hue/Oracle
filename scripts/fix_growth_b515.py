from pathlib import Path

MAIN = Path("app/src/main/java/ro/alintudor/oracle/MainActivity.kt")
LOCAL = Path("app/src/main/java/ro/alintudor/oracle/core/OracleLocalProcessor.kt")
GRADLE = Path("app/build.gradle")
GROWTH = Path("app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt")

# Growth must not trigger a full Oracle refresh every time the screen is reopened.
# Reopen is allowed to refresh only when the persisted 16:00 snapshot is missing/stale.
s = MAIN.read_text(encoding="utf-8")
old = '''    private fun openModule(key:String){
        currentModule=key
        runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        if (key == "analysis") return
        if (key == "knowledge") {
            if (OracleKnowledgeSync.isStale(this)) {
                OracleKnowledgeSync.refreshAsync(this) { ok, error ->
                    if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                    if (ok) runCatching { renderModule("knowledge", false) }.onFailure { showModuleError("knowledge", it) }
                    else if (error != null) Toast.makeText(this, "Knowledge refresh eșuat: $error", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        Thread{val result=runCatching{OracleLocalProcessor.refresh(repository)};mainHandler.post{if(currentModule!=key||isFinishing)return@post;result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}.onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()
    }
'''
new = '''    private fun openModule(key:String){
        currentModule=key
        runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        if (key == "analysis") return
        if (key == "growth") {
            // Growth is a frozen 16:00 snapshot. Reopening the screen must not
            // rerun the ranking while the current trading-day snapshot is valid.
            Thread{
                val snapshot = repository.snapshot()
                val anchor = currentGrowthAnchor(System.currentTimeMillis())
                val needsRefresh = snapshot.growth.isEmpty() || snapshot.growth.any { it.referenceTimestamp != anchor }
                if (!needsRefresh) return@Thread
                val result = runCatching { OracleLocalProcessor.refresh(repository) }
                mainHandler.post {
                    if (currentModule != key || isFinishing) return@post
                    result.onSuccess { runCatching { renderModule(key,false) }.onFailure { showModuleError(key,it) } }
                        .onFailure { e -> Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show() }
                }
            }.start()
            return
        }
        if (key == "knowledge") {
            if (OracleKnowledgeSync.isStale(this)) {
                OracleKnowledgeSync.refreshAsync(this) { ok, error ->
                    if (currentModule != "knowledge" || isFinishing) return@refreshAsync
                    if (ok) runCatching { renderModule("knowledge", false) }.onFailure { showModuleError("knowledge", it) }
                    else if (error != null) Toast.makeText(this, "Knowledge refresh eșuat: $error", Toast.LENGTH_LONG).show()
                }
            }
            return
        }
        Thread{val result=runCatching{OracleLocalProcessor.refresh(repository)};mainHandler.post{if(currentModule!=key||isFinishing)return@post;result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}.onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()
    }

    private fun currentGrowthAnchor(nowMillis: Long): Long {
        val zone = java.time.ZoneId.of("Europe/Bucharest")
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone)
        var date = if (now.toLocalTime().isBefore(java.time.LocalTime.of(16, 0))) now.toLocalDate().minusDays(1) else now.toLocalDate()
        while (!ro.alintudor.oracle.core.OracleMarketCalendar.isTradingDay(date)) date = date.minusDays(1)
        return java.time.ZonedDateTime.of(date, java.time.LocalTime.of(16, 0), zone).toInstant().toEpochMilli()
    }
'''
if old not in s:
    raise SystemExit("Expected B519 MainActivity openModule block not found")
MAIN.write_text(s.replace(old, new, 1), encoding="utf-8")

l = LOCAL.read_text(encoding="utf-8")
old_fun = "    fun refresh(repository: OracleRepository): OracleModuleData {"
new_fun = "    @Synchronized\n    fun refresh(repository: OracleRepository): OracleModuleData {"
if old_fun not in l:
    raise SystemExit("Expected OracleLocalProcessor.refresh signature not found")
LOCAL.write_text(l.replace(old_fun, new_fun, 1), encoding="utf-8")

g = GRADLE.read_text(encoding="utf-8")
if "versionCode 31" not in g or "versionName 'V6g-FINAL-B519'" not in g:
    raise SystemExit("Unexpected build identity; refusing to patch")
g = g.replace("versionCode 31", "versionCode 32", 1)
g = g.replace("versionName 'V6g-FINAL-B519'", "versionName 'V6g-FINAL-B515'", 1)
GRADLE.write_text(g, encoding="utf-8")

gw = GROWTH.read_text(encoding="utf-8")
if "B519" in gw:
    gw = gw.replace("BUILD B519 • V6g-FINAL", "BUILD B515 • V6g-FINAL")
    gw = gw.replace("V6g-FINAL-B519", "V6g-FINAL-B515")
GROWTH.write_text(gw, encoding="utf-8")

print("B515 Growth stability patch applied")
