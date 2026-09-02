from pathlib import Path

# Growth UI history toggle and labels.
p = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
s = p.read_text(encoding='utf-8')
start = s.index('    private fun addHistory(')
brace = s.index('{', start)
depth = 0
end = None
for i in range(brace, len(s)):
    if s[i] == '{':
        depth += 1
    elif s[i] == '}':
        depth -= 1
        if depth == 0:
            end = i + 1
            break
if end is None:
    raise SystemExit('Could not locate addHistory end')

new = '''    private fun addHistory(entries: List<OracleGrowthRecommendation>) {
        val card = card(12)
        val header = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = text("ULTIMELE RECOMANDĂRI", 15f, Typeface.DEFAULT_BOLD, cyan, 0, 0)
        header.addView(title, LinearLayout.LayoutParams(-2, -2))
        val arrow = TextView(host.root.context).apply {
            text = "⌄"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(cyan)
            setPadding(host.dp(4), 0, host.dp(8), host.dp(2))
            isClickable = true
            isFocusable = true
            contentDescription = "Extinde sau restrânge ultimele recomandări"
        }
        header.addView(arrow, LinearLayout.LayoutParams(host.dp(38), host.dp(40)))
        header.addView(View(host.root.context), LinearLayout.LayoutParams(0, 1, 1f))
        val download = TextView(host.root.context).apply {
            text = "⇩  PDF"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(8, 15, 28), cyan, 1, 10)
            setPadding(host.dp(13), host.dp(8), host.dp(13), host.dp(8))
            isClickable = true
            isFocusable = true
            contentDescription = "Descarcă jurnalul Growth în PDF"
            setOnClickListener {
                val path = journalStore.exportPdf()
                if (path != null) Toast.makeText(host.root.context, "Jurnalul Growth a fost salvat în Downloads.", Toast.LENGTH_LONG).show()
                else Toast.makeText(host.root.context, "Nu există recomandări pentru export.", Toast.LENGTH_SHORT).show()
            }
        }
        header.addView(download, LinearLayout.LayoutParams(host.dp(94), host.dp(40)))
        card.addView(header)

        val all = entries
            .filter { it.referenceTimestamp > 0L && it.referenceTimestamp >= startHistoryTimestamp() }
            .sortedWith(compareByDescending<OracleGrowthRecommendation> { it.referenceTimestamp }
                .thenBy { horizonOrder(it.horizon) }.thenBy { it.ticker })
        val rows = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        card.addView(rows)

        val summaryViews = mutableListOf<View>()
        fun addSummaryEntry(item: OracleGrowthRecommendation) {
            val row = historyRow(item)
            summaryViews += row
            rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
        }
        fun addPlaceholder() {
            val row = historyRow(null, "31.08.2026 16:00")
            summaryViews += row
            rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
        }
        all.take(6).forEach(::addSummaryEntry)
        repeat(maxOf(0, 6 - minOf(6, all.size))) { addPlaceholder() }

        val olderViews = all.drop(6).map { item ->
            historyRow(item).also { row ->
                row.visibility = View.GONE
                rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
            }
        }
        var expanded = true
        fun applyHistoryVisibility() {
            summaryViews.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
            olderViews.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
            arrow.text = if (expanded) "⌃" else "⌄"
            arrow.contentDescription = if (expanded) "Restrânge ultimele recomandări" else "Extinde ultimele recomandări"
        }
        arrow.setOnClickListener { expanded = !expanded; applyHistoryVisibility() }
        title.setOnClickListener { expanded = !expanded; applyHistoryVisibility() }
        applyHistoryVisibility()
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })
    }'''
s = s[:start] + new + s[end:]
s = s.replace('private fun historyRow(item: OracleGrowthRecommendation?): LinearLayout {', 'private fun historyRow(item: OracleGrowthRecommendation?, placeholderDate: String? = null): LinearLayout {')
s = s.replace('addView(text("—", 10f, Typeface.DEFAULT, muted, 0, 2))', 'addView(text(placeholderDate ?: "—", 10f, Typeface.DEFAULT, muted, 0, 2))')
if '"RSG" -> "Republic Services, Inc."' not in s:
    s = s.replace('            "CRM" -> "Salesforce, Inc."\n', '            "CRM" -> "Salesforce, Inc."\n            "RSG" -> "Republic Services, Inc."\n')
if 'private fun displaySector(' not in s:
    marker = '    private fun horizonOrder(horizon: String)'
    helper = '''    private fun displaySector(ticker:String, stored:String):String {
        val value=stored.trim()
        if (value.isNotBlank() && value != "—") return value
        return when(ticker.uppercase(Locale.US)) {
            "RSG" -> "Industrials"
            "CF" -> "Materials"
            "LNG" -> "Energy"
            "CRM", "NOW", "ORCL" -> "Technology"
            else -> "—"
        }
    }

'''
    s = s.replace(marker, helper + marker)
s = s.replace('identity.addView(text(item.sector.ifBlank { "—" }, 9f, Typeface.DEFAULT_BOLD, Color.rgb(150, 170, 205), 0, 2))', 'identity.addView(text(displaySector(item.ticker, item.sector), 9f, Typeface.DEFAULT_BOLD, Color.rgb(150, 170, 205), 0, 2))')
s = s.replace('BUILD B536 • GROWTH', 'BUILD B538 • GROWTH')
p.write_text(s, encoding='utf-8')

# Archive the previous Growth snapshot at T0 rollover before it is replaced.
p = Path('app/src/main/java/ro/alintudor/oracle/OracleMysticActivity.kt')
s = p.read_text(encoding='utf-8')
if 'import ro.alintudor.oracle.core.OracleGrowthJournalStore' not in s:
    s = s.replace('import ro.alintudor.oracle.core.OracleBootstrap\n', 'import ro.alintudor.oracle.core.OracleBootstrap\nimport ro.alintudor.oracle.core.OracleGrowthJournalStore\n')
old = '''            Thread {
                runCatching { OracleLocalProcessor.refreshGrowthOnly(repository) }
            }.start()'''
old_one_line = '        Thread { runCatching { OracleLocalProcessor.refreshGrowthOnly(repository) } }.start()'
newwarm = '''            Thread {
                runCatching {
                    val growthJournal = OracleGrowthJournalStore(applicationContext)
                    val previous = repository.cachedGrowth()
                    val refreshed = OracleLocalProcessor.refreshGrowthOnly(repository)
                    if (previous.isNotEmpty() && refreshed.isNotEmpty() &&
                        previous.first().referenceTimestamp != refreshed.first().referenceTimestamp) {
                        growthJournal.record(previous)
                    }
                    growthJournal.record(refreshed)
                }
            }.start()'''
if old in s:
    s = s.replace(old, newwarm)
elif old_one_line in s:
    one_line_new = '''        Thread {
            runCatching {
                val growthJournal = OracleGrowthJournalStore(applicationContext)
                val previous = repository.cachedGrowth()
                val refreshed = OracleLocalProcessor.refreshGrowthOnly(repository)
                if (previous.isNotEmpty() && refreshed.isNotEmpty() &&
                    previous.first().referenceTimestamp != refreshed.first().referenceTimestamp) {
                    growthJournal.record(previous)
                }
                growthJournal.record(refreshed)
            }
        }.start()'''
    s = s.replace(old_one_line, one_line_new)
else:
    raise SystemExit('Launcher warm-up block not found')
p.write_text(s, encoding='utf-8')

# PDF exports only the Growth journal window starting 31.08.2026.
p = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthJournalStore.kt')
s = p.read_text(encoding='utf-8')
oldpdf = '''        val entries = load()
        if (entries.isEmpty()) return null'''
newpdf = '''        val entries = load()
            .filter { it.referenceTimestamp > 0L && it.referenceTimestamp >= startHistoryTimestamp() }
            .sortedWith(compareBy<OracleGrowthRecommendation> { it.referenceTimestamp }
                .thenBy { horizonOrder(it.horizon) }.thenBy { it.ticker })
        if (entries.isEmpty()) return null'''
if oldpdf in s:
    s = s.replace(oldpdf, newpdf)
if 'private fun startHistoryTimestamp()' not in s:
    marker = '    private fun key(item: OracleGrowthRecommendation)'
    helper = '''    private fun startHistoryTimestamp(): Long {
        val f = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ro", "RO"))
        f.timeZone = zone
        return f.parse("31.08.2026 00:00")?.time ?: 0L
    }

'''
    s = s.replace(marker, helper + marker)
p.write_text(s, encoding='utf-8')
