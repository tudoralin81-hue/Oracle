from pathlib import Path

# Runtime fix applied after the existing S&P 500 performance patch.
# Do not touch START or the frozen recommendation/history card implementation.

M = Path('app/src/main/java/ro/alintudor/oracle/core/OracleMarketData.kt')
if M.exists():
    s = M.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36')
    M.write_text(s, encoding='utf-8')

E = Path('app/src/main/java/ro/alintudor/oracle/core/OracleGrowthEngine.kt')
if E.exists():
    s = E.read_text(encoding='utf-8')
    s = s.replace('Oracle-Stock-Intelligence/1.0', 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36')
    # Keep 50-company visible progress steps, but use smaller network batches so one oversized
    # Spark response cannot zero out the whole scan.
    s = s.replace('universe.chunked(50)', 'universe.chunked(25)')
    E.write_text(s, encoding='utf-8')

P = Path('app/src/main/java/ro/alintudor/oracle/nativeui/OracleGrowthModule.kt')
s = P.read_text(encoding='utf-8')
if 'import android.widget.ProgressBar' not in s:
    s = s.replace('import android.widget.TextView\n', 'import android.widget.TextView\nimport android.widget.ProgressBar\n', 1)
start = s.index('    private fun addLoadingState()')
brace = s.index('{', start)
depth = 0
end = None
for i in range(brace, len(s)):
    if s[i] == '{': depth += 1
    elif s[i] == '}':
        depth -= 1
        if depth == 0:
            end = i + 1
            break
if end is None:
    raise SystemExit('addLoadingState end not found')
new = r'''    private fun addLoadingState() {
        val p0 = ro.alintudor.oracle.core.OracleGrowthEngine.growthProgress()
        val finishedAtRender = p0.size >= 4 && p0[3] == 1L
        val card = card(18)
        card.gravity = Gravity.CENTER
        val spinner = ImageView(host.root.context).apply {
            setImageResource(ro.alintudor.oracle.R.drawable.ic_oracle)
            contentDescription = "Oracle se calculează"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            ObjectAnimator.ofFloat(this, View.ROTATION, 0f, 360f).apply {
                duration = 1100L
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
        card.addView(spinner, LinearLayout.LayoutParams(host.dp(56), host.dp(56)).apply { gravity = Gravity.CENTER })
        card.addView(text("GROWTH", 20f, Typeface.DEFAULT_BOLD, green, 0, 8).apply { gravity = Gravity.CENTER })
        card.addView(text("Se calculează recomandările…", 14f, Typeface.DEFAULT, muted, 0, 5).apply { gravity = Gravity.CENTER })
        val progressLabel = text("DATE ÎNCĂRCATE: 0 / 500", 13f, Typeface.DEFAULT_BOLD, cyan, 0, 5).apply { gravity = Gravity.CENTER }
        card.addView(progressLabel)
        val progressBar = ProgressBar(host.root.context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 500
            progress = 0
            isIndeterminate = false
        }
        card.addView(progressBar, LinearLayout.LayoutParams(-1, host.dp(10)).apply { setMargins(host.dp(8), host.dp(3), host.dp(8), host.dp(4)) })
        val etaLabel = text("Timp estimat: se calculează…", 12f, Typeface.DEFAULT_BOLD, green, 0, 4).apply { gravity = Gravity.CENTER }
        card.addView(etaLabel)
        val quoteLabel = text("\"Price is what you pay; value is what you get.\"\n— Benjamin Graham", 14f, Typeface.DEFAULT, white, 0, 7).apply {
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.12f)
        }
        card.addView(quoteLabel)
        card.addView(text("Analiza S&P 500 • date OHLCV reale • procesare paralelizată", 10f, Typeface.DEFAULT, muted, 0, 4).apply { gravity = Gravity.CENTER })
        val diagnostic = text(if (finishedAtRender) "Datele nu au fost primite. Se verifică sursa de piață…" else "Analiza se execută în fundal.", 10f, Typeface.DEFAULT_BOLD, if (finishedAtRender) orange else muted, 0, 4).apply { gravity = Gravity.CENTER }
        card.addView(diagnostic)
        val quotes = listOf(
            "\"Price is what you pay; value is what you get.\"\n— Benjamin Graham",
            "\"Rule No. 1: Never lose money. Rule No. 2: Never forget Rule No. 1.\"\n— Warren Buffett",
            "\"The most important quality for an investor is temperament, not intellect.\"\n— Warren Buffett",
            "\"It's only when the tide goes out that you learn who's been swimming naked.\"\n— Warren Buffett",
            "\"In the short run, the market is a voting machine, but in the long run it is a weighing machine.\"\n— Benjamin Graham",
            "\"The intelligent investor is a realist who sells to optimists and buys from pessimists.\"\n— Benjamin Graham",
            "\"Invert, always invert.\"\n— Charlie Munger",
            "\"Behind every stock is a company. Find out what it's doing.\"\n— Peter Lynch",
            "\"The four most dangerous words in investing are: this time it's different.\"\n— Sir John Templeton"
        )
        val handler = Handler(Looper.getMainLooper())
        var quoteIndex = 0
        val quoteRunnable = object : Runnable {
            override fun run() {
                quoteIndex = (quoteIndex + 1) % quotes.size
                quoteLabel.text = quotes[quoteIndex]
                handler.postDelayed(this, 15_000L)
            }
        }
        handler.postDelayed(quoteRunnable, 15_000L)
        val progressRunnable = object : Runnable {
            override fun run() {
                val p = ro.alintudor.oracle.core.OracleGrowthEngine.growthProgress()
                val total = p[1].toInt().coerceAtLeast(1)
                val loaded = p[0].toInt().coerceIn(0, total)
                val shown = if (loaded >= total) total else (loaded / 50) * 50
                progressBar.progress = shown
                progressLabel.text = "DATE ÎNCĂRCATE: $shown / ${"%,d".format(Locale.US, total)}"
                val started = p[2]
                if (started > 0L && shown > 0 && !isFinished(p)) {
                    val elapsed = (System.nanoTime() - started).coerceAtLeast(1L) / 1_000_000_000.0
                    val eta = (elapsed * (total - shown) / shown).coerceAtLeast(0.0)
                    etaLabel.text = "Timp estimat: ~${formatEta(eta)}"
                } else if (isFinished(p)) {
                    val elapsed = if (started > 0L) (System.nanoTime() - started).coerceAtLeast(0L) / 1_000_000_000.0 else 0.0
                    etaLabel.text = "Analiza datelor: finalizată în ${String.format(Locale.US, "%.1f", elapsed)} s"
                    diagnostic.text = if (loaded == 0) "⚠ 0/500 date OHLCV — sursa de piață nu a răspuns" else "✓ Date OHLCV primite: $loaded / $total"
                    diagnostic.setTextColor(if (loaded == 0) orange else green)
                }
                if (!isFinished(p)) handler.postDelayed(this, 500L)
            }
        }
        handler.post(progressRunnable)
        card.addView(text("Maxim țintă: 20 secunde", 9f, Typeface.DEFAULT_BOLD, muted, 0, 3).apply { gravity = Gravity.CENTER })
        host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(400)).apply { setMargins(0, 0, 0, host.dp(10)) })
        addBuildFooter()
    }

    private fun isFinished(p: LongArray): Boolean = p.size >= 4 && p[3] == 1L

    private fun formatEta(seconds: Double): String {
        val rounded = kotlin.math.ceil(seconds).toInt().coerceAtLeast(0)
        return if (rounded < 60) "$rounded sec" else "${rounded / 60} min ${rounded % 60} sec"
    }'''
s = s[:start] + new + s[end:]
P.write_text(s, encoding='utf-8')
