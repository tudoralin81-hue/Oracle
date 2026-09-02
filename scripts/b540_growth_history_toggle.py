from pathlib import Path

# B540: history section is collapsed by default. The toggle is a single clickable
# title+arrow control placed immediately beside the title; PDF stays on the right.
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

        var expanded = false
        val toggle = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = "Extinde ultimele recomandări"
        }
        val title = text("ULTIMELE RECOMANDĂRI", 15f, Typeface.DEFAULT_BOLD, cyan, 0, 0)
        val arrow = TextView(host.root.context).apply {
            text = "⌄"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(cyan)
            setPadding(host.dp(4), 0, host.dp(8), host.dp(2))
        }
        toggle.addView(title, LinearLayout.LayoutParams(-2, -2))
        toggle.addView(arrow, LinearLayout.LayoutParams(host.dp(38), host.dp(40)))
        header.addView(toggle, LinearLayout.LayoutParams(0, host.dp(40), 1f))

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

        val allRows = mutableListOf<View>()
        fun addPlaceholder() {
            val row = historyRow(null)
            allRows += row
            rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
        }
        fun addEntry(item: OracleGrowthRecommendation) {
            val row = historyRow(item)
            allRows += row
            rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
        }

        val visible = all.take(6)
        visible.forEach { addEntry(it) }
        repeat(maxOf(0, 6 - visible.size)) { addPlaceholder() }
        all.drop(6).forEach { addEntry(it) }

        fun applyHistoryVisibility() {
            allRows.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
            arrow.text = if (expanded) "⌃" else "⌄"
            toggle.contentDescription = if (expanded) "Restrânge ultimele recomandări" else "Extinde ultimele recomandări"
        }
        toggle.setOnClickListener {
            expanded = !expanded
            applyHistoryVisibility()
        }
        applyHistoryVisibility()

        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })
    }
'''
s = s[:start] + new + s[end:]
s = s.replace('addView(text("02.09.2026 16:00", 10f, Typeface.DEFAULT, muted, 0, 2))', 'addView(text("01.09.2026 16:00", 10f, Typeface.DEFAULT, muted, 0, 2))')

# B540 final loader privacy/performance: show percentage only; never expose universe size.
s = s.replace('text("DATE ÎNCĂRCATE: 0 / ${initial.total}", 12f, Typeface.DEFAULT_BOLD, cyan, 0, 10)', 'text("PROGRES: 0%", 12f, Typeface.DEFAULT_BOLD, cyan, 0, 10)')
s = s.replace('max = initial.total.coerceAtLeast(1); progress = 0; isIndeterminate = false', 'max = 100; progress = 0; isIndeterminate = false')
s = s.replace('// Requirement #6: the visible counter steps in increments of 50;\n                // the engine tracks the exact count internally.\n                val shown = if (loaded >= total) total else (loaded / 50) * 50\n                progressBar.max = total\n                progressBar.progress = shown\n                progressLabel.text = "DATE ÎNCĂRCATE: $shown / $total"', '// B540 final: expose only a percentage; the monitored universe size remains private.\n                val shownPct = ((loaded.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)\n                progressBar.max = 100\n                progressBar.progress = shownPct\n                progressLabel.text = "PROGRES: $shownPct%"')
s = s.replace('"Maxim țintă: 20 secunde"', '"Maxim țintă: 45 secunde"')
s = s.replace('(${progress.loaded} / ${progress.total} simboluri primite).', 'Datele OHLCV necesare nu au fost primite.')
p.write_text(s, encoding='utf-8')

# B540 final: hard overall budget is 45 seconds.
e = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
es = e.read_text(encoding='utf-8')
es = es.replace('private const val TOTAL_BUDGET_NANOS = 19_000_000_000L // 1s buffer under the 20s target', 'private const val TOTAL_BUDGET_NANOS = 45_000_000_000L // hard 45s overall target')
e.write_text(es, encoding='utf-8')

print('B540 final loader: hard 45s budget; progress shown only as percentage; universe size hidden')
