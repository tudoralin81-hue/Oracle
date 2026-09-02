from pathlib import Path
import re

# B540: reproduce the Claude GROWTH loader/engine state at build time.
M = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
s = M.read_text(encoding='utf-8')
loader = '''    /**
     * B540 loading state (Requirement #6/#7/#11).
     *
     * Shows real progress as a percentage only ("DATE ÎNCĂRCATE: XX%") plus the
     * matching bar — the underlying counts (and therefore the size of the
     * monitored universe) are tracked internally but never rendered — an ETA
     * computed from actual throughput, and an investor quote that rotates in a
     * randomized, non-repeating order. If [OracleGrowthEngine] has already
     * finished with zero OHLCV received, this renders an explicit error state
     * instead — it never spins forever.
     */
    private fun addLoadingState() {
        val initial = OracleGrowthEngine.growthProgress()
        if (initial.phase == OracleGrowthPhase.NO_DATA) {
            addNoDataState(initial)
            return
        }
        val card = card(18)
        card.gravity = Gravity.CENTER
        val spinner = ImageView(host.root.context).apply {
            setImageResource(ro.alintudor.oracle.R.drawable.ic_oracle)
            contentDescription = "Oracle se calculează"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val rotation = ObjectAnimator.ofFloat(this, View.ROTATION, 0f, 360f).apply {
                duration = 1100L
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            rotation.start()
        }
        card.addView(spinner, LinearLayout.LayoutParams(host.dp(54), host.dp(54)).apply { gravity = Gravity.CENTER })
        card.addView(text("GROWTH", 17f, Typeface.DEFAULT_BOLD, green, 0, 10).apply { gravity = Gravity.CENTER })
        card.addView(text("Se calculează recomandările…", 13f, Typeface.DEFAULT, muted, 0, 5).apply { gravity = Gravity.CENTER })
        val progressLabel = text("DATE ÎNCĂRCATE: 0%", 12f, Typeface.DEFAULT_BOLD, cyan, 0, 10).apply { gravity = Gravity.CENTER }
        card.addView(progressLabel)
        val progressBar = ProgressBar(host.root.context, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0; isIndeterminate = false }
        card.addView(progressBar, LinearLayout.LayoutParams(-1, host.dp(9)).apply { setMargins(host.dp(10), host.dp(6), host.dp(10), host.dp(3)) })
        val etaLabel = text("Timp estimat: se calculează…", 10f, Typeface.DEFAULT_BOLD, green, 0, 5).apply { gravity = Gravity.CENTER }
        card.addView(etaLabel)
        var quoteOrder = loaderQuotes.indices.shuffled()
        var quotePos = 0
        val quoteLabel = text(loaderQuotes[quoteOrder[quotePos]], 10f, Typeface.DEFAULT, white, 0, 9).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.1f) }
        card.addView(quoteLabel)
        card.addView(text("Analiza se execută în fundal. Valorile apar numai după finalizarea calculului curent.", 9f, Typeface.DEFAULT, muted, 0, 9).apply { gravity = Gravity.CENTER })
        card.addView(text("Maxim țintă: 45 secunde", 9f, Typeface.DEFAULT_BOLD, muted, 0, 6).apply { gravity = Gravity.CENTER })
        host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(390)).apply { setMargins(0, 0, 0, host.dp(10)) })
        addBuildFooter()
        val handler = Handler(Looper.getMainLooper())
        val quoteRunnable = object : Runnable {
            override fun run() {
                quotePos++
                if (quotePos >= quoteOrder.size) {
                    val lastShown = quoteOrder.last()
                    var next = loaderQuotes.indices.shuffled()
                    if (next.size > 1 && next.first() == lastShown) next = next.toMutableList().apply { add(1, removeAt(0)) }
                    quoteOrder = next
                    quotePos = 0
                }
                quoteLabel.text = loaderQuotes[quoteOrder[quotePos]]
                handler.postDelayed(this, 15_000L)
            }
        }
        handler.postDelayed(quoteRunnable, 15_000L)
        val progressRunnable = object : Runnable {
            override fun run() {
                val p = OracleGrowthEngine.growthProgress()
                if (p.phase == OracleGrowthPhase.NO_DATA) { handler.removeCallbacksAndMessages(null); addNoDataState(p); return }
                val total = p.total.coerceAtLeast(1)
                val loaded = p.loaded.coerceIn(0, total)
                val pct = ((loaded * 100.0) / total).toInt().coerceIn(0, 100)
                progressBar.progress = pct
                progressLabel.text = "DATE ÎNCĂRCATE: $pct%"
                if (p.startedAtNanos > 0L) {
                    val elapsed = (System.nanoTime() - p.startedAtNanos).coerceAtLeast(1L) / 1_000_000_000.0
                    etaLabel.text = if (p.phase == OracleGrowthPhase.RUNNING) {
                        if (pct > 0) "Timp estimat: ~${formatEta((elapsed * (100 - pct) / pct).coerceAtLeast(0.0))}" else "Timp estimat: se calculează…"
                    } else "Analiza datelor: finalizată în ${String.format(Locale.US, "%.1f", elapsed)} s"
                }
                if (p.phase == OracleGrowthPhase.RUNNING) handler.postDelayed(this, 500L)
            }
        }
        handler.post(progressRunnable)
    }

'''
pattern = r'    /\*\*\n     \* B540 loading state \(Requirement #6/#7/#11\)\..*?\n    private fun addNoDataState'
s2, n = re.subn(pattern, loader + '    /** Requirement #6/#11: explicit, non-infinite error state when 0 OHLCV was received. */\n    private fun addNoDataState', s, count=1, flags=re.S)
if n != 1: raise SystemExit(f'loader replacement failed: {n}')
# Keep the existing history implementation, but make the arrow sit beside the title
# and make the entire header a reliable toggle target. Default is collapsed.
history_old = '''        header.addView(text("ULTIMELE RECOMANDĂRI", 15f, Typeface.DEFAULT_BOLD, cyan, 0, 0), LinearLayout.LayoutParams(0, -2, 1f))

        val download = TextView(host.root.context).apply {'''
history_new = '''        val title = text("ULTIMELE RECOMANDĂRI", 15f, Typeface.DEFAULT_BOLD, cyan, 0, 0)
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

        val arrow = TextView(host.root.context).apply {
            text = "⌄"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(cyan)
            setPadding(0, 0, 0, host.dp(2))
            isClickable = true
            isFocusable = true
            contentDescription = "Extinde sau restrânge ultimele recomandări"
        }
        header.addView(arrow, LinearLayout.LayoutParams(host.dp(38), host.dp(40)))

        val download = TextView(host.root.context).apply {'''
s2, n2 = s2.replace(history_old, history_new, 1), s2.count(history_old)
if n2 != 1: raise SystemExit(f'header replacement failed: {n2}')
old_arrow_block = '''        val arrow = TextView(host.root.context).apply {
            text = "⌄"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(cyan)
            setPadding(0, 0, 0, host.dp(2))
            isClickable = true
            isFocusable = true
        }
        header.addView(arrow, LinearLayout.LayoutParams(host.dp(38), host.dp(40)))
        card.addView(header)'''
new_arrow_block = '''        card.addView(header)'''
if old_arrow_block not in s2: raise SystemExit('duplicate arrow block not found')
s2 = s2.replace(old_arrow_block, new_arrow_block, 1)
old_listener = '''        var expanded = false
        arrow.setOnClickListener {
            expanded = !expanded
            arrow.text = if (expanded) "⌃" else "⌄"
            expandedViews.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
        }'''
new_listener = '''        var expanded = false
        fun applyHistoryVisibility() {
            arrow.text = if (expanded) "⌃" else "⌄"
            arrow.contentDescription = if (expanded) "Restrânge ultimele recomandări" else "Extinde ultimele recomandări"
            expandedViews.forEach { it.visibility = if (expanded) View.VISIBLE else View.GONE }
        }
        header.setOnClickListener {
            expanded = !expanded
            applyHistoryVisibility()
        }
        arrow.setOnClickListener {
            expanded = !expanded
            applyHistoryVisibility()
        }
        applyHistoryVisibility()'''
if old_listener not in s2: raise SystemExit('toggle listener block not found')
s2 = s2.replace(old_listener, new_listener, 1)
M.write_text(s2, encoding='utf-8')
E = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
es = E.read_text(encoding='utf-8')
es = es.replace('private const val TOTAL_BUDGET_NANOS = 19_000_000_000L // 1s buffer under the 20s target','private const val TOTAL_BUDGET_NANOS = 44_000_000_000L // 1s buffer under the 45s target')
es = es.replace('private const val SCAN_BUDGET_NANOS = 13_000_000_000L','private const val SCAN_BUDGET_NANOS = 30_000_000_000L // scaled with TOTAL_BUDGET_NANOS so OHLCV fetch keeps its ~68% share of the run')
E.write_text(es, encoding='utf-8')
print('B540 GROWTH loader/engine patch + history arrow toggle applied')
