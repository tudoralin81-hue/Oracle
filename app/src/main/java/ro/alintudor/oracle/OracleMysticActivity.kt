package ro.alintudor.oracle

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import ro.alintudor.oracle.core.OracleBootstrap
import ro.alintudor.oracle.core.OracleLocalProcessor
import ro.alintudor.oracle.core.OracleRepository
import ro.alintudor.oracle.core.snapshot
import ro.alintudor.oracle.nativeui.*
import kotlin.math.*

/** New Start experience. Module/data logic intentionally mirrors the stable activity. */
class OracleMysticActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var repository: OracleRepository
    private var currentModule: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val titles = linkedMapOf("portfolio" to "PORTFOLIO", "alerts" to "ALERTS", "news" to "NEWS", "growth" to "GROWTH", "knowledge" to "KNOWLEDGE", "analysis" to "ANALYSIS", "watchlist" to "WATCHLIST", "journal" to "JURNAL ACTIVITATE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = OracleRepository(this)
        window.statusBarColor = Color.rgb(3, 4, 12)
        window.navigationBarColor = Color.rgb(3, 4, 12)
        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(3, 4, 12)) }
        setContentView(root)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { handleBack() }
        }
        runCatching { OracleBootstrap.ensure(repository); showHub() }
            .onFailure { showFatalError("Pornirea Oracle a eșuat", it) }
    }

    private fun showHub() {
        currentModule = null
        root.removeAllViews()
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(Color.rgb(3, 4, 12)) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(2), dp(8), dp(22))
        }
        val hero = OracleMysticStartView(this) { openModule(it) }
        val heroHeight = (resources.displayMetrics.heightPixels * 0.70f).toInt().coerceAtLeast(dp(610))
        page.addView(hero, LinearLayout.LayoutParams(-1, heroHeight))
        page.addView(makeStatus(), LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(0, dp(8), 0, dp(8)) })
        scroll.addView(page)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun makeStatus() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), 0, dp(16), 0)
        setBackgroundColor(Color.rgb(8, 12, 25))
        addView(View(this@OracleMysticActivity).apply { setBackgroundColor(Color.rgb(63, 235, 137)) }, LinearLayout.LayoutParams(dp(8), dp(8)))
        addView(TextView(this@OracleMysticActivity).apply { text = "  ORACLE READY"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(this@OracleMysticActivity).apply { text = "LOCAL INTELLIGENCE"; textSize = 11f; setTextColor(Color.rgb(145, 154, 178)) })
    }

    private fun openModule(key: String) {
        currentModule = key
        runCatching { renderModule(key) }.onFailure { showModuleError(key, it) }
        if (key == "analysis") return
        Thread {
            val result = runCatching { OracleLocalProcessor.refresh(repository) }
            mainHandler.post {
                if (currentModule != key || isFinishing) return@post
                result.onSuccess { runCatching { renderModule(key) }.onFailure { showModuleError(key, it) } }
                    .onFailure { Toast.makeText(this, "Refresh local eșuat: ${it.message ?: it.javaClass.simpleName}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun renderModule(key: String) {
        root.removeAllViews()
        val host = OracleNativeModule(this, titles[key] ?: key.uppercase(), { showHub() }, { openModule(key) })
        root.addView(host.root, FrameLayout.LayoutParams(-1, -1))
        val data = repository.snapshot()
        when (key) {
            "portfolio" -> OraclePortfolioModule(host).render(data.positions)
            "alerts" -> OracleAlertsModule(host).render(data.alerts)
            "news" -> OracleNewsModule(host).render(data.news)
            "journal" -> OracleJournalModule(host).render(data.journal, data.history, data.alerts)
            "growth", "analysis", "watchlist", "knowledge" -> OracleSimpleModule(
                host,
                titles[key] ?: key.uppercase(),
                onWatchlistTickerClick = { ticker -> openWatchlistTicker(ticker) }
            ).render(actions = data.actions, knowledge = data.knowledge, positions = data.positions, history = data.history)
        }
    }

    private fun openWatchlistTicker(ticker: String) {
        val normalized = ticker.trim().uppercase(java.util.Locale.US)
        if (normalized.isBlank()) return
        OracleSimpleModule.setTickerDraft(normalized)
        openModule("analysis")
    }

    private fun handleBack() {
        if (currentModule != null) showHub() else finish()
    }

    private fun showModuleError(key: String, error: Throwable) {
        root.removeAllViews()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(32), dp(32), dp(32), dp(32)); setBackgroundColor(Color.rgb(3, 5, 12)) }
        box.addView(TextView(this).apply { text = "ORACLE  •  ${titles[key] ?: key.uppercase()}"; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) })
        box.addView(TextView(this).apply { text = "Modulul nu s-a putut încărca.\n\n${error.message ?: error.javaClass.simpleName}"; textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0, dp(24), 0, dp(24)) })
        box.addView(Button(this).apply { text = "REÎNCEARCĂ"; setOnClickListener { openModule(key) } })
        box.addView(Button(this).apply { text = "ÎNAPOI LA ORACLE"; setOnClickListener { showHub() } })
        root.addView(box, FrameLayout.LayoutParams(-1, -1))
    }

    private fun showFatalError(title: String, error: Throwable) {
        root.removeAllViews()
        root.addView(TextView(this).apply { text = "$title\n\n${error.message ?: error.javaClass.simpleName}"; textSize = 17f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setPadding(dp(32), dp(32), dp(32), dp(32)) }, FrameLayout.LayoutParams(-1, -1))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    override fun onBackPressed() { handleBack() }
}

private class OracleMysticStartView(context: android.content.Context, private val onModule: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodes = listOf(
        Node("portfolio", "PORTFOLIO", Color.rgb(194, 75, 255), 0f),
        Node("news", "NEWS", Color.rgb(0, 215, 255), 52f),
        Node("knowledge", "KNOWLEDGE", Color.rgb(255, 210, 45), 104f),
        Node("watchlist", "WATCHLIST", Color.rgb(255, 220, 45), 156f),
        Node("analysis", "ANALYSIS", Color.rgb(30, 205, 255), 208f),
        Node("growth", "GROWTH", Color.rgb(140, 245, 45), 260f),
        Node("alerts", "ALERTS", Color.rgb(255, 72, 42), 312f)
    )
    private data class Node(val key: String, val label: String, val color: Int, val angle: Float)
    private var centers = mutableMapOf<String, Pair<Float, Float>>()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val d = resources.displayMetrics.density
        val cx = w * .5f; val cy = h * .47f; val radius = min(w, h) * .205f; val orbit = min(w, h) * .315f; val nr = min(w, h) * .082f
        c.drawColor(Color.rgb(2, 3, 10))
        paint.style = Paint.Style.FILL
        for (i in 0 until 95) {
            val x = ((i * 193 + 41) % 997) / 997f * w
            val y = ((i * 79 + 17) % 991) / 991f * h
            val a = 30 + (i % 6) * 12
            paint.color = Color.argb(a, 220, 190, 115)
            c.drawCircle(x, y, (.45f + (i % 4) * .38f) * d, paint)
        }
        paint.style = Paint.Style.STROKE
        for (i in 1..8) {
            paint.strokeWidth = if (i == 1) 1.8f * d else .65f * d
            paint.color = Color.argb(72 - i * 5, 196, 157, 62)
            c.drawCircle(cx, cy, orbit * (.55f + i * .17f), paint)
        }
        paint.strokeWidth = .55f * d; paint.color = Color.argb(80, 204, 169, 78)
        for (a in floatArrayOf(0f, 45f, 90f, 135f)) {
            val q = Math.toRadians(a.toDouble()); val dx = cos(q).toFloat() * w; val dy = sin(q).toFloat() * h
            c.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, paint)
        }
        centers.clear()
        for (n in nodes) {
            val a = Math.toRadians((n.angle - 90f).toDouble())
            val x = cx + cos(a).toFloat() * orbit; val y = cy + sin(a).toFloat() * orbit
            centers[n.key] = x to y
            paint.strokeWidth = .8f * d; paint.color = Color.argb(115, 205, 171, 74)
            c.drawLine(cx, cy, x, y, paint)
            c.drawCircle(x, y, 2.2f * d, paint)
        }
        drawSigil(c, cx, cy, radius, d)
        for (n in nodes) {
            val pos = centers[n.key]!!
            drawNode(c, pos.first, pos.second, nr, n, d)
        }
        drawHeader(c, w, h, d)
    }

    private fun drawSigil(c: Canvas, cx: Float, cy: Float, r: Float, d: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        for (i in 5 downTo 1) {
            paint.strokeWidth = (1f + i * .8f) * d
            paint.color = Color.argb(18 + i * 5, 255, 191, 50)
            c.drawCircle(cx, cy, r * (1.02f + i * .055f), paint)
        }
        paint.strokeWidth = 8f * d
        paint.color = Color.rgb(247, 186, 45)
        c.drawCircle(cx, cy, r, paint)
        paint.strokeWidth = 1.4f * d; paint.color = Color.rgb(255, 222, 94)
        c.drawCircle(cx, cy, r * .86f, paint)
        c.drawCircle(cx, cy, r * 1.16f, paint)
        paint.color = Color.argb(115, 218, 173, 64); paint.strokeWidth = .7f * d
        for (i in 1..3) c.drawCircle(cx, cy, r * (1.30f + i * .13f), paint)
        val eye = Path(); eye.moveTo(cx - r * .28f, cy - r * .22f); eye.cubicTo(cx - r * .10f, cy - r * .43f, cx + r * .10f, cy - r * .43f, cx + r * .28f, cy - r * .22f); eye.cubicTo(cx + r * .10f, cy - r * .02f, cx - r * .10f, cy - r * .02f, cx - r * .28f, cy - r * .22f)
        paint.strokeWidth = 2f * d; paint.color = Color.rgb(255, 204, 52); c.drawPath(eye, paint); c.drawCircle(cx, cy - r * .22f, r * .075f, paint)
        paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); paint.color = Color.WHITE; paint.textSize = r * .25f
        c.drawText("ORACLE", cx, cy + r * .12f, paint)
        paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = r * .078f; paint.color = Color.rgb(255, 207, 61)
        c.drawText("STOCK INTELLIGENCE", cx, cy + r * .28f, paint)
        val graph = Path(); graph.moveTo(cx - r * .48f, cy + r * .52f)
        val pts = arrayOf(.0f to .10f, .12f to .02f, .23f to .17f, .34f to .08f, .45f to .28f, .56f to .19f, .68f to .42f, .80f to .30f, .92f to .52f, 1f to .68f)
        for ((x, y) in pts) graph.lineTo(cx - r * .48f + x * r * .96f, cy + r * .52f - y * r * .30f)
        paint.strokeWidth = 1.5f * d; paint.color = Color.rgb(255, 197, 39); c.drawPath(graph, paint)
        paint.textSize = r * .22f; c.drawText("↗", cx, cy - r * .36f, paint)
    }

    private fun drawNode(c: Canvas, x: Float, y: Float, r: Float, n: Node, d: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.2f * d; paint.color = Color.argb(45, Color.red(n.color), Color.green(n.color), Color.blue(n.color)); c.drawCircle(x, y, r * 1.16f, paint)
        paint.strokeWidth = 2.4f * d; paint.color = n.color; c.drawCircle(x, y, r, paint)
        paint.style = Paint.Style.FILL; paint.color = Color.argb(242, 5, 8, 18); c.drawCircle(x, y, r * .965f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.1f * d; paint.color = Color.argb(105, Color.red(n.color), Color.green(n.color), Color.blue(n.color)); c.drawCircle(x, y, r * .82f, paint)
        paint.style = Paint.Style.FILL; paint.color = n.color; c.drawCircle(x, y - r * .73f, r * .035f, paint)
        drawNodeIcon(c, x, y - r * .28f, r * .27f, n.key, n.color, d)
        paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = r * .22f; paint.color = n.color
        c.drawText(n.label, x, y + r * .16f, paint)
        paint.textSize = r * .085f; paint.color = Color.WHITE
        val desc = when (n.key) { "portfolio" -> "Performanță și poziții"; "alerts" -> "Semnale și evenimente"; "news" -> "Știri financiare"; "growth" -> "Acțiuni cu potențial"; "knowledge" -> "Idei și documentație"; "analysis" -> "Analiză detaliată"; else -> "Acțiuni favorite" }
        c.drawText(desc, x, y + r * .39f, paint)
        paint.textSize = r * .27f; c.drawText("›", x, y + r * .70f, paint)
    }

    private fun drawNodeIcon(c: Canvas, x: Float, y: Float, s: Float, key: String, color: Int, d: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.7f * d; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND; paint.color = color
        when (key) {
            "portfolio" -> { c.drawCircle(x, y, s * .62f, paint); c.drawLine(x, y, x, y - s * .62f, paint); c.drawLine(x, y, x + s * .48f, y + s * .28f, paint) }
            "alerts" -> { c.drawArc(x - s * .48f, y - s * .35f, x + s * .48f, y + s * .42f, 205f, 130f, false, paint); c.drawLine(x - s * .58f, y + s * .42f, x + s * .58f, y + s * .42f, paint); c.drawCircle(x, y + s * .62f, s * .07f, paint) }
            "news" -> { c.drawRect(x - s * .58f, y - s * .55f, x + s * .58f, y + s * .55f, paint); c.drawLine(x - s * .35f, y - s * .2f, x + s * .35f, y - s * .2f, paint); c.drawLine(x - s * .35f, y, x + s * .35f, y, paint); c.drawLine(x - s * .35f, y + s * .2f, x + s * .18f, y + s * .2f, paint) }
            "growth" -> { val q = Path(); q.moveTo(x - s * .62f, y + s * .35f); q.lineTo(x - s * .18f, y - s * .04f); q.lineTo(x + s * .04f, y + s * .10f); q.lineTo(x + s * .62f, y - s * .55f); c.drawPath(q, paint); c.drawLine(x + s * .35f, y - s * .55f, x + s * .62f, y - s * .55f, paint); c.drawLine(x + s * .62f, y - s * .55f, x + s * .62f, y - s * .27f, paint) }
            "knowledge" -> { c.drawRect(x - s * .58f, y - s * .58f, x - s * .04f, y + s * .58f, paint); c.drawRect(x + s * .04f, y - s * .58f, x + s * .58f, y + s * .58f, paint) }
            "analysis" -> { c.drawLine(x - s * .6f, y + s * .55f, x - s * .6f, y - s * .58f, paint); c.drawLine(x - s * .6f, y + s * .55f, x + s * .62f, y + s * .55f, paint); val q = Path(); q.moveTo(x - s * .48f, y + s * .22f); q.lineTo(x - s * .12f, y - s * .05f); q.lineTo(x + s * .08f, y + s * .08f); q.lineTo(x + s * .55f, y - s * .45f); c.drawPath(q, paint) }
            "watchlist" -> { val q = Path(); q.moveTo(x - s * .7f, y); q.cubicTo(x - s * .28f, y - s * .62f, x + s * .28f, y - s * .62f, x + s * .7f, y); q.cubicTo(x + s * .28f, y + s * .62f, x - s * .28f, y + s * .62f, x - s * .7f, y); c.drawPath(q, paint); c.drawCircle(x, y, s * .18f, paint) }
        }
    }

    private fun drawHeader(c: Canvas, w: Float, h: Float, d: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.3f * d; paint.color = Color.rgb(129, 105, 40)
        c.drawRoundRect(4 * d, 8 * d, 52 * d, 54 * d, 11 * d, 11 * d, paint); c.drawRoundRect(w - 52 * d, 8 * d, w - 4 * d, 54 * d, 11 * d, 11 * d, paint)
        paint.strokeWidth = 2.2f * d
        for (i in 0..2) c.drawLine(15 * d, (24 + i * 8) * d, 41 * d, (24 + i * 8) * d, paint)
        c.drawArc(w - 39 * d, 17 * d, w - 17 * d, 40 * d, -55f, 285f, false, paint); c.drawLine(w - 17 * d, 17 * d, w - 17 * d, 25 * d, paint)
        paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); paint.color = Color.WHITE; paint.textSize = min(w, h) * .033f
        c.drawText("ORACLE", w * .5f, 27 * d, paint); paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = min(w, h) * .016f; paint.color = Color.rgb(176, 150, 73); c.drawText("STOCK INTELLIGENCE", w * .5f, 47 * d, paint)
        paint.textSize = min(w, h) * .012f; paint.color = Color.argb(150, 205, 182, 113); c.drawText("THE INTELLIGENCE CIRCLE", w * .5f, h - 12 * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val hit = min(width, height).toFloat() * .082f * 1.45f
        for ((key, pos) in centers) {
            if (hypot(event.x - pos.first, event.y - pos.second) <= hit) { onModule(key); return true }
        }
        return true
    }
}
