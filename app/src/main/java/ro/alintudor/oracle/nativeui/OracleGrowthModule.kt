package ro.alintudor.oracle.nativeui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import ro.alintudor.oracle.core.OracleGrowthRecommendation
import ro.alintudor.oracle.core.OracleNews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Growth module. Visual structure follows the approved mobile Growth mockup,
 * while values remain the persisted Oracle snapshot and are never recalculated here.
 */
class OracleGrowthModule(private val host: OracleNativeModule) {
    private val bg = Color.rgb(6, 10, 20)
    private val panel = Color.rgb(7, 14, 28)
    private val border = Color.rgb(49, 82, 125)
    private val muted = Color.rgb(165, 174, 195)
    private val cyan = Color.rgb(75, 225, 255)
    private val orange = Color.rgb(255, 160, 25)
    private val green = Color.rgb(105, 245, 35)
    private val red = Color.rgb(255, 80, 90)
    private val white = Color.WHITE

    fun render(items: List<OracleGrowthRecommendation>, fallbackNews: List<OracleNews> = emptyList()) {
        host.content.removeAllViews()
        if (items.isEmpty()) {
            addLoadingState()
            return
        }

        // GrowthBanner from the shared module shell is the single Growth hero.
        // Do not add a second Growth banner here.
        addSummary(items)

        val ordered = listOf("SHORT", "MEDIUM", "LONG").mapNotNull { horizon ->
            items.firstOrNull { it.horizon.equals(horizon, true) }
        }
        if (ordered.isNotEmpty()) addRecommendations(ordered, fallbackNews)
        addNews(ordered, fallbackNews)
        addHistory(items)
        addBuildFooter()
    }

    private fun addLoadingState() {
        val card = card(18)
        card.gravity = Gravity.CENTER
        val spinner = ProgressBar(host.root.context).apply { isIndeterminate = true }
        card.addView(spinner, LinearLayout.LayoutParams(host.dp(54), host.dp(54)).apply { gravity = Gravity.CENTER })
        card.addView(text("GROWTH", 17f, Typeface.DEFAULT_BOLD, green, 0, 10).apply { gravity = Gravity.CENTER })
        card.addView(text("Se calculează recomandările…", 13f, Typeface.DEFAULT, muted, 0, 5).apply { gravity = Gravity.CENTER })
        card.addView(text("Analiza se execută în fundal. Valorile apar numai după finalizarea calculului curent.", 10f, Typeface.DEFAULT, muted, 0, 7).apply { gravity = Gravity.CENTER })
        host.content.addView(card, LinearLayout.LayoutParams(-1, host.dp(190)).apply { setMargins(0, 0, 0, host.dp(10)) })
        addBuildFooter()
    }

    private fun addSummary(items: List<OracleGrowthRecommendation>) {
        val card = card(14)
        card.addView(text("RECOMANDĂRILE DE CREȘTERE", 18f, Typeface.DEFAULT_BOLD, green, 0, 0))
        card.addView(text("Oracle Growth • snapshot zilnic 16:00", 13f, Typeface.DEFAULT, muted, 0, 5))
        val line = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, host.dp(12), 0, 0)
        }
        line.addView(metric("ORIZONTURI", items.map { it.horizon }.distinct().size.toString(), cyan), LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(metric("RECOMANDĂRI", items.size.toString(), orange), LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(metric("ANCHOR", formatT0(items.first().referenceTimestamp), white), LinearLayout.LayoutParams(0, -2, 1.55f))
        card.addView(line)
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })
    }

    private fun addRecommendations(items: List<OracleGrowthRecommendation>, news: List<OracleNews>) {
        val section = TextView(host.root.context).apply {
            text = "SUMAR RECOMANDĂRI ACTIVE"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            setPadding(host.dp(4), host.dp(3), host.dp(4), host.dp(7))
        }
        host.content.addView(section)
        items.forEach { addRecommendationCard(it, news) }
    }

    private fun addRecommendationCard(item: OracleGrowthRecommendation, news: List<OracleNews>) {
        val accent = when (item.horizon.uppercase(Locale.US)) { "SHORT" -> cyan; "MEDIUM" -> orange; else -> green }
        val card = card(12).apply { background = rounded(bg, accent, 1, 15) }
        val top = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val left = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        left.addView(text(horizonLabel(item.horizon), 13f, Typeface.DEFAULT_BOLD, accent, 0, 0))
        left.addView(text(horizonRange(item.horizon), 11f, Typeface.DEFAULT, muted, 0, 3))
        top.addView(left, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text(formatT0(item.referenceTimestamp), 10f, Typeface.DEFAULT, muted, 0, 0))
        card.addView(top)

        val identity = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, host.dp(10), 0, host.dp(8)) }
        val ticker = text(item.ticker, 30f, Typeface.DEFAULT_BOLD, white, 0, 0)
        identity.addView(ticker, LinearLayout.LayoutParams(host.dp(120), -2))
        val company = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        company.addView(text(item.company, 15f, Typeface.DEFAULT_BOLD, white, 0, 0))
        company.addView(text(item.sector, 11f, Typeface.DEFAULT_BOLD, Color.rgb(150, 170, 205), 0, 4))
        identity.addView(company, LinearLayout.LayoutParams(0, -2, 1f))
        identity.addView(text("›", 28f, Typeface.DEFAULT, accent, 0, 0))
        card.addView(identity)
        card.addView(divider())

        val metrics = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, host.dp(7), 0, host.dp(4)) }
        metrics.addView(metric("SCOR", "${item.score}/100", cyan), LinearLayout.LayoutParams(0, -2, 1f))
        metrics.addView(metric("SEMNAL", compactSignal(item.signal), orange), LinearLayout.LayoutParams(0, -2, 1.15f))
        metrics.addView(metric("RISC", item.risk, riskColor(item.risk)), LinearLayout.LayoutParams(0, -2, 1f))
        metrics.addView(metric("ALOCARE", "${format(item.allocationMax)}%", orange), LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(metrics)

        val lower = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, host.dp(5), 0, host.dp(4)) }
        val forecast = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        forecast.addView(text("Potențial estimat", 10f, Typeface.DEFAULT, muted, 0, 0))
        forecast.addView(text(signedPct(item.forecastPct), 22f, Typeface.DEFAULT_BOLD, green, 0, 2))
        lower.addView(forecast, LinearLayout.LayoutParams(0, -2, 1.15f))
        val momentum = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL }
        momentum.addView(text("Momentum", 10f, Typeface.DEFAULT, muted, 0, 0))
        momentum.addView(text("5D: ${signedPct(item.momentum5D)}", 11f, Typeface.DEFAULT_BOLD, cyan, 0, 2))
        momentum.addView(text("20D: ${signedPct(item.momentum20D)}", 11f, Typeface.DEFAULT_BOLD, cyan, 0, 2))
        lower.addView(momentum, LinearLayout.LayoutParams(0, -2, 1.1f))
        lower.addView(SparklineView(host.root.context, accent), LinearLayout.LayoutParams(host.dp(112), host.dp(52)))
        card.addView(lower)
        addCompactWeights(card, item.weights)

        val linked = news.firstOrNull { it.ticker.equals(item.ticker, true) }
        val newsTitle = if (item.newsTitle.isNotBlank()) item.newsTitle else linked?.title.orEmpty()
        val source = if (item.newsSource.isNotBlank()) item.newsSource else linked?.source.orEmpty()
        if (newsTitle.isNotBlank()) {
            card.addView(text("▣  ${if (source.isBlank()) "NEWS" else source}", 10f, Typeface.DEFAULT_BOLD, cyan, 0, 5))
            card.addView(text(newsTitle, 11f, Typeface.DEFAULT, white, 0, 4))
        }
        card.addView(text("Datele sunt informative și nu constituie recomandări de investiții.", 9f, Typeface.DEFAULT, Color.rgb(125, 135, 155), 0, 8))
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(9)) })
    }

    private fun addCompactWeights(parent: LinearLayout, weights: List<Int>) {
        if (weights.isEmpty()) return
        parent.addView(text("Ponderi", 10f, Typeface.DEFAULT_BOLD, white, 0, 5))
        val names = listOf("News", "BO", "Trend", "Mom", "Vol", "S/R", "Fund", "BB", "Ichimoku", "Mkt", "R/R", "ADX")
        val grid = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL; setPadding(0, host.dp(2), 0, host.dp(1)) }
        val columns = 6
        for (r in 0 until 2) {
            val row = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            for (c in 0 until columns) {
                val i = r * columns + c
                val cell = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(host.dp(2), host.dp(2), host.dp(2), host.dp(2)) }
                cell.addView(text(names[i], 8f, Typeface.DEFAULT, muted, 0, 0), LinearLayout.LayoutParams(0, -2, 1f))
                cell.addView(text(weights.getOrNull(i)?.takeIf { it > 0 }?.toString() ?: "—", 9f, Typeface.DEFAULT_BOLD, cyan, 0, 0))
                row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
            }
            grid.addView(row)
        }
        parent.addView(grid)
    }

    private fun addNews(items: List<OracleGrowthRecommendation>, fallbackNews: List<OracleNews>) {
        val recent = items.mapNotNull { item ->
            val n = fallbackNews.firstOrNull { it.ticker.equals(item.ticker, true) }
            if (n != null) n else if (item.newsTitle.isNotBlank()) OracleNews(item.ticker, item.newsTitle, item.newsSource, "", item.referenceTimestamp, false) else null
        }.distinctBy { it.ticker }
        if (recent.isEmpty()) return
        val card = card(12)
        card.addView(text("ȘTIRI & CATALIZATORI RECENȚI", 15f, Typeface.DEFAULT_BOLD, green, 0, 0))
        recent.forEach { n ->
            card.addView(text("▣  ${n.title}", 11f, Typeface.DEFAULT, white, 0, 7))
            card.addView(text("${formatT0(n.publishedAt)} • ${n.source}", 9f, Typeface.DEFAULT, muted, host.dp(18), 2))
        }
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(8)) })
    }

    private fun addHistory(items: List<OracleGrowthRecommendation>) {
        val label = TextView(host.root.context).apply { text = "JURNAL GROWTH • ISTORIC FORECAST"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = .05f; setTextColor(cyan); setPadding(host.dp(4), host.dp(5), host.dp(4), host.dp(7)) }
        host.content.addView(label)
        items.sortedBy { it.referenceTimestamp }.forEach { item ->
            val row = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8)); background = rounded(bg, Color.rgb(34, 43, 65), 1, 11) }
            row.addView(text(item.ticker, 13f, Typeface.DEFAULT_BOLD, white, 0, 0), LinearLayout.LayoutParams(host.dp(58), -2))
            row.addView(text("${item.horizon}\nT0 ${formatT0(item.referenceTimestamp)}", 9f, Typeface.DEFAULT, muted, 0, 0), LinearLayout.LayoutParams(0, -2, 1.35f))
            row.addView(text("Forecast\n${signedPct(item.forecastPct)}", 10f, Typeface.DEFAULT_BOLD, green, 0, 0), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(text("Scor\n${item.score}/100", 10f, Typeface.DEFAULT_BOLD, cyan, 0, 0), LinearLayout.LayoutParams(0, -2, .8f))
            host.content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(6)) })
        }
        host.content.addView(text("Istoricul este append-only: o schimbare de forecast nu rescrie T0.", 9f, Typeface.DEFAULT, Color.rgb(125, 135, 155), host.dp(4), 2), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(15)) })
    }

    private fun addBuildFooter() {
        host.content.addView(text("BUILD B514 • V6g-FINAL", 9f, Typeface.DEFAULT_BOLD, Color.rgb(125, 135, 155), host.dp(4), 8), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(18)) })
    }

    private fun horizonLabel(horizon: String) = when (horizon.uppercase(Locale.US)) { "SHORT" -> "●  TERMEN SCURT"; "MEDIUM" -> "●  TERMEN MEDIU"; else -> "●  TERMEN LUNG" }
    private fun horizonRange(horizon: String) = when (horizon.uppercase(Locale.US)) { "SHORT" -> "1–10 zile bursiere"; "MEDIUM" -> "2–12 săptămâni"; else -> "3–12 luni" }
    private fun compactSignal(signal: String) = signal.replace("STRONG ", "STRONG\n").trim()
    private fun riskColor(risk: String): Int = if (risk.contains("RID", true)) red else orange
    private fun signedPct(v: Double) = if (v >= 0) "+${format(v)}%" else "${format(v)}%"
    private fun format(v: Double) = "%.1f".format(Locale.US, v)

    private fun formatT0(timestamp: Long): String {
        if (timestamp <= 0L) return "—"
        val f = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ro", "RO")); f.timeZone = TimeZone.getTimeZone("Europe/Bucharest"); return f.format(Date(timestamp))
    }

    private fun card(padding: Int) = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(padding), host.dp(padding), host.dp(padding), host.dp(padding)); background = rounded(bg, border, 1, 16) }
    private fun rounded(fill: Int, stroke: Int, strokeWidth: Int, radius: Int) = GradientDrawable().apply { setColor(fill); if (strokeWidth > 0) setStroke(host.dp(strokeWidth), stroke); cornerRadius = host.dp(radius).toFloat() }
    private fun divider() = View(host.root.context).apply { setBackgroundColor(Color.rgb(35, 48, 70)); layoutParams = LinearLayout.LayoutParams(-1, host.dp(1)) }
    private fun metric(label: String, value: String, color: Int) = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(2), host.dp(2), host.dp(2), host.dp(2)); gravity = Gravity.CENTER; addView(text(label, 8f, Typeface.DEFAULT, muted, 0, 0).apply { gravity = Gravity.CENTER }); addView(text(value, 13f, Typeface.DEFAULT_BOLD, color, 0, 3).apply { gravity = Gravity.CENTER }) }
    private fun text(value: String, size: Float, typeface: Typeface, color: Int, left: Int, top: Int) = TextView(host.root.context).apply { text = value; textSize = size; this.typeface = typeface; setTextColor(color); setPadding(left, top, 0, 0) }

    private class SparklineView(context: android.content.Context, private val lineColor: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas); paint.color = lineColor; val p = Path(); val pts = floatArrayOf(.02f,.78f, .14f,.55f, .25f,.67f, .37f,.44f, .48f,.57f, .61f,.31f, .73f,.46f, .86f,.20f, .98f,.05f)
            for (i in pts.indices step 2) { val x = pts[i] * width; val y = pts[i + 1] * height; if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
            canvas.drawPath(p, paint); canvas.drawCircle(width * .98f, height * .05f, 4f, paint)
        }
    }
}
