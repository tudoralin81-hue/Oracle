from pathlib import Path
import re
import shutil

# B514 FINAL APK #81: this script is Growth-only.
# Analysis is now sourced directly from current main and must remain byte-for-byte unchanged.
# SOURCE_OF_TRUTH_MAIN_V1
# VALIDATION_GATE_V4
ANALYSIS = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleAnalysisModules.kt')
MAIN = Path('app/src/main/java/ro/alintudor/oracle/MainActivity.kt')
LIVE = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthLiveData.kt')
GROWTH = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
GRADLE = Path('app/build.gradle')
BEFORE = Path('/tmp/oracle_analysis_before.kt')

shutil.copyfile(ANALYSIS, BEFORE)

s = MAIN.read_text(encoding='utf-8')
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
        if (key != "growth") {
            runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        }
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
        if (key == "growth") {
            refreshGrowthUntilCurrent(0)
            return
        }
        Thread{val result=runCatching{OracleLocalProcessor.refresh(repository)};mainHandler.post{if(currentModule!=key||isFinishing)return@post;result.onSuccess{runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}}.onFailure{e->Toast.makeText(this,"Refresh local eșuat: ${e.message?:e.javaClass.simpleName}",Toast.LENGTH_LONG).show()}}}.start()
    }

    private fun refreshGrowthUntilCurrent(attempt:Int) {
        if (currentModule != "growth" || isFinishing) return
        Thread {
            val result = runCatching { OracleLocalProcessor.refresh(repository) }
            mainHandler.post {
                if (currentModule != "growth" || isFinishing) return@post
                val data = result.getOrNull()
                val current = data?.growth?.let { OracleGrowthLiveData.refresh(it) }.orEmpty()
                if (current.isNotEmpty()) {
                    runCatching { renderModule("growth", false) }.onFailure { showModuleError("growth", it) }
                } else if (attempt < 24) {
                    if (attempt == 0) runCatching { renderModule("growth", false) }.onFailure { showModuleError("growth", it) }
                    mainHandler.postDelayed({ refreshGrowthUntilCurrent(attempt + 1) }, 5000L)
                } else if (result.isFailure) {
                    Toast.makeText(this, "Growth refresh eșuat: ${result.exceptionOrNull()?.message ?: "eroare"}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
'''
if old not in s:
    raise SystemExit('MainActivity openModule anchor not found')
MAIN.write_text(s.replace(old,new,1), encoding='utf-8')

# GrowthLiveData already rejects stale snapshots; keep that gate unchanged.

s = GROWTH.read_text(encoding='utf-8')
old = '''        if (items.isEmpty()) {
            host.addCard("GROWTH", "Nu există încă un snapshot Growth local. Refresh va afișa ultimul rezultat Oracle disponibil.")
            return
        }'''
new = '''        if (items.isEmpty()) {
            host.addCard("GROWTH", "Se încarcă snapshot-ul Growth al sesiunii curente…")
            addBuildFooter()
            return
        }'''
if old not in s:
    raise SystemExit('Growth empty-state anchor not found')
s = s.replace(old,new,1)
old = '''        addNews(ordered, fallbackNews)
        addHistory(items)
    }
'''
new = '''        addNews(ordered, fallbackNews)
        addHistory(items)
        addBuildFooter()
    }

    private fun addBuildFooter() {
        host.content.addView(text("BUILD B514 • V6g-FINAL", 9f, Typeface.DEFAULT_BOLD, Color.rgb(125, 135, 155), host.dp(4), 8), LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, host.dp(18))
        })
    }
'''
if old not in s:
    raise SystemExit('Growth footer insertion point not found')
GROWTH.write_text(s.replace(old,new,1), encoding='utf-8')

s = GRADLE.read_text(encoding='utf-8')
s = re.sub(r'versionCode\s+\d+', 'versionCode 35', s, count=1)
s = re.sub(r"versionName\s+'[^']+'", "versionName 'V6g-FINAL-B514'", s, count=1)
GRADLE.write_text(s, encoding='utf-8')

if ANALYSIS.read_bytes() != BEFORE.read_bytes():
    raise SystemExit('B514 Growth-only patch changed Analysis; this is forbidden.')
print('B518 stale-render guard + retry applied; Analysis is byte-for-byte unchanged.')