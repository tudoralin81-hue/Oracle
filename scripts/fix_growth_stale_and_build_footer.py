from pathlib import Path

GROWTH = Path("app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt")
MAIN = Path("app/src/main/java/ro/alintudor/oracle/MainActivity.kt")

g = GROWTH.read_text(encoding="utf-8")
old = '''    fun render(items: List<OracleGrowthRecommendation>, fallbackNews: List<OracleNews> = emptyList()) {
        host.content.removeAllViews()
        if (items.isEmpty()) {'''
new = '''    fun render(items: List<OracleGrowthRecommendation>, fallbackNews: List<OracleNews> = emptyList()) {
        host.content.removeAllViews()
        // Never render a stale trading-day snapshot. Growth is a daily snapshot,
        // not live data. Before the current session is available, show no cards.
        val validItems = items.filter { it.referenceTimestamp == currentGrowthAnchor() }
        if (validItems.isEmpty()) {
            host.addCard("GROWTH", "Se încarcă snapshot-ul Growth al sesiunii curente…")
            addBuildFooter()
            return
        }
'''
if old in g:
    g = g.replace(old, new, 1)

# Remove the old empty-list body if this script is re-run on an unpatched source.
g = g.replace('''        if (items.isEmpty()) {
            host.addCard("GROWTH", "Nu există încă un snapshot Growth local. Refresh va afișa ultimul rezultat Oracle disponibil.")
            return
        }

''', '', 1)
g = g.replace('''        addSummary(items)

        val ordered = listOf("SHORT", "MEDIUM", "LONG").mapNotNull { horizon ->
            items.firstOrNull { it.horizon.equals(horizon, true) }
        }''', '''        addSummary(validItems)

        val ordered = listOf("SHORT", "MEDIUM", "LONG").mapNotNull { horizon ->
            validItems.firstOrNull { it.horizon.equals(horizon, true) }
        }''', 1)
g = g.replace('''        addHistory(items)
    }

    private fun addSummary''', '''        addHistory(validItems)
        addBuildFooter()
    }

    private fun currentGrowthAnchor(): Long {
        val zone = java.time.ZoneId.of("Europe/Bucharest")
        val now = java.time.ZonedDateTime.now(zone)
        var date = if (now.toLocalTime().isBefore(java.time.LocalTime.of(16, 0))) now.toLocalDate().minusDays(1) else now.toLocalDate()
        while (!ro.alintudor.oracle.core.OracleMarketCalendar.isTradingDay(date)) date = date.minusDays(1)
        return java.time.ZonedDateTime.of(date, java.time.LocalTime.of(16, 0), zone).toInstant().toEpochMilli()
    }

    private fun addBuildFooter() {
        host.content.addView(text("BUILD ${ro.alintudor.oracle.BuildConfig.VERSION_NAME}", 9f, Typeface.DEFAULT_BOLD, Color.rgb(120, 135, 160), host.dp(4), 10), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(18)) })
    }

    private fun addSummary''', 1)
# Remove any hardcoded footer left by the previous B517 script.
g = g.replace('''        host.content.addView(text("BUILD V6g-FINAL-B517", 9f, Typeface.DEFAULT_BOLD, Color.rgb(120, 135, 160), host.dp(4), 10), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(18)) })
''', '', 1)
GROWTH.write_text(g, encoding="utf-8")

m = MAIN.read_text(encoding="utf-8")
old_open = '''    private fun openModule(key:String){
        currentModule=key
        runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}'''
new_open = '''    private fun openModule(key:String){
        currentModule=key
        // Growth must never flash the previous trading-day recommendations.
        // Render its loading state first; only the async refresh may publish cards.
        if (key == "growth") {
            runCatching {
                root.removeAllViews()
                val host = OracleNativeModule(this, titles[key] ?: "GROWTH", { showHub() }, { refreshModule(key) })
                root.addView(host.root, FrameLayout.LayoutParams(-1, -1))
                OracleGrowthModule(host).render(emptyList(), emptyList())
            }.onFailure { showModuleError(key, it) }
        } else {
            runCatching{renderModule(key,false)}.onFailure{showModuleError(key,it)}
        }'''
if old_open in m:
    m = m.replace(old_open, new_open, 1)
MAIN.write_text(m, encoding="utf-8")
print("B518 Growth stale-render + dynamic build footer patch applied")
