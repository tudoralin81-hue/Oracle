from pathlib import Path

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

        // The toggle belongs immediately beside the section title.
        // It is CLOSED by default; only older rows are hidden/revealed.
        val arrow = TextView(host.root.context).apply {
            text = "⌄"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(cyan)
            setPadding(host.dp(4), 0, host.dp(8), host.dp(2))
            isClickable = true
            isFocusable = true
            contentDescription = "Extinde ultimele recomandări"
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

        fun addPlaceholder() {
            val row = historyRow(null)
            rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
        }
        fun addEntry(item: OracleGrowthRecommendation) {
            val row = historyRow(item)
            rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
        }

        val visible = all.take(6)
        visible.forEach { addEntry(it) }
        repeat(maxOf(0, 6 - visible.size)) { addPlaceholder() }

        val expandedViews = all.drop(6).map { item ->
            val row = historyRow(item)
            row.visibility = View.GONE
            rows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, host.dp(6), 0, 0) })
            row
        }

        var expanded = false
        fun applyHistoryVisibility() {
            expandedViews.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
            arrow.text = if (expanded) "⌃" else "⌄"
            arrow.contentDescription = if (expanded) "Restrânge ultimele recomandări" else "Extinde ultimele recomandări"
        }
        arrow.setOnClickListener {
            expanded = !expanded
            applyHistoryVisibility()
        }
        title.setOnClickListener {
            expanded = !expanded
            applyHistoryVisibility()
        }
        // Explicit initial state: collapsed.
        applyHistoryVisibility()

        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })
    }
'''
s = s[:start] + new + s[end:]
p.write_text(s, encoding='utf-8')
print('B540 GROWTH history toggle fixed: arrow beside title, closed by default')
