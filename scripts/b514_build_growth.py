from pathlib import Path
import re
import hashlib
import shutil

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
'''
new = '''    private fun openModule(key:String){
        currentModule=key
        if (key != "growth") {
            runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        }
        if (key == "analysis") return
'''
if old not in s:
    raise SystemExit('MainActivity Growth initial-render anchor not found')
MAIN.write_text(s.replace(old,new,1), encoding='utf-8')

s = LIVE.read_text(encoding='utf-8')
old = '''    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> {
        if (items.isEmpty()) return emptyList()
        if (items.any { it.referenceTimestamp <= 0L }) return emptyList()
        val expectedAnchor = currentGrowthAnchor(System.currentTimeMillis())
        if (items.any { it.referenceTimestamp != expectedAnchor }) return emptyList()
        return items
    }

    private fun currentGrowthAnchor(nowMillis: Long): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(BUCHAREST)
        var date = if (now.toLocalTime().isBefore(LocalTime.of(16, 0))) now.toLocalDate().minusDays(1) else now.toLocalDate()
        while (!OracleMarketCalendar.isTradingDay(date)) date = date.minusDays(1)
        return ZonedDateTime.of(date, LocalTime.of(16, 0), BUCHAREST).toInstant().toEpochMilli()
    }'''
new = '''    fun refresh(items: List<OracleGrowthRecommendation>): List<OracleGrowthRecommendation> {
        if (items.isEmpty()) return emptyList()
        return items.filter { it.referenceTimestamp > 0L }
    }'''
if old not in s:
    raise SystemExit('GrowthLiveData snapshot gate not found')
LIVE.write_text(s.replace(old,new,1), encoding='utf-8')

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
    raise SystemExit('Analysis changed during Growth patch')
print('B514 Growth-only patch applied; Analysis is byte-for-byte unchanged.')
