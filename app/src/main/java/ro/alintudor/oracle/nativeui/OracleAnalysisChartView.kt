package ro.alintudor.oracle.nativeui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import ro.alintudor.oracle.core.OracleMarketData
import ro.alintudor.oracle.core.OracleOhlcvPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Real OHLCV technical chart for Analysis. Data comes from the same market-data adapter as Oracle. */
class OracleAnalysisChartView(context: Context, private val ticker: String) : View(context) {
    private val bg = Color.rgb(3, 7, 14)
    private val grid = Color.rgb(30, 39, 55)
    private val text = Color.rgb(190, 200, 220)
    private val green = Color.rgb(55, 235, 95)
    private val red = Color.rgb(255, 75, 75)
    private val blue = Color.rgb(35, 175, 255)
    private val gold = Color.rgb(255, 190, 45)
    private val purple = Color.rgb(160, 100, 255)
    private val paints = Paint(Paint.ANTI_ALIAS_FLAG)
    private var data: List<OracleOhlcvPoint> = emptyList()
    private var visible = 100
    private var offset = 0
    private var mode = "1D"
    private var showBB = true
    private var showMA = true
    private var showIchi = false
    private var showRSI = true
    private var showADX = true
    private var loading = true
    private var selectedIndex = -1
    private var downX = 0f
    private var downY = 0f
    private var lastOffset = 0
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            visible = (visible / detector.scaleFactor).toInt().coerceIn(35, 180)
            clampOffset()
            selectedIndex = -1
            invalidate()
            return true
        }
    })

    init {
        setBackgroundColor(bg)
        loadMode("1D")
    }

    private fun loadMode(requested: String) {
        loading = true
        invalidate()
        Thread {
            val fetched = runCatching { OracleMarketData.fetchForMode(ticker, requested) }.getOrDefault(emptyList())
            post {
                if (mode == requested) {
                    data = fetched
                    loading = false
                    clampOffset()
                    selectedIndex = -1
                    invalidate()
                }
            }
        }.start()
    }

    fun setMode(value: String) {
        val next = when (value) { "30M", "1H", "1D" -> value; else -> "1D" }
        if (next == mode && data.isNotEmpty()) return
        mode = next
        visible = 100
        offset = 0
        selectedIndex = -1
        loadMode(next)
    }

    fun toggleIndicator(name: String) {
        when (name) {
            "BB" -> showBB = !showBB
            "MA/EMA" -> showMA = !showMA
            "ICHI" -> showIchi = !showIchi
            "RSI" -> showRSI = !showRSI
            "ADX" -> showADX = !showADX
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastOffset = offset
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && data.isNotEmpty()) {
                    val step = width.toFloat() / max(1, visible)
                    offset = (lastOffset + ((downX - event.x) / max(4f, step)).toInt()).coerceIn(0, max(0, data.size - visible))
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (abs(event.x - downX) < 18f && abs(event.y - downY) < 18f) {
                    selectCandle(event.x)
                }
                return true
            }
        }
        return true
    }

    private fun selectCandle(x: Float) {
        if (data.isEmpty()) return
        val start = max(0, data.size - visible - offset)
        val end = min(data.size, start + visible)
        val count = end - start
        if (count <= 0) return
        val step = width.toFloat() / count
        val local = (x / step).toInt().coerceIn(0, count - 1)
        selectedIndex = local
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        paints.style = Paint.Style.FILL
        paints.color = bg
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints)
        if (loading) {
            label(c, "Se încarcă datele reale pentru $ticker • $mode…", 18f, 34f, text, 14f)
            return
        }
        if (data.size < 10) {
            label(c, "Nu există suficiente date OHLCV pentru $ticker • $mode.", 18f, 34f, red, 14f)
            return
        }

        val start = max(0, data.size - visible - offset)
        val end = min(data.size, start + visible)
        val d = data.subList(start, end)
        val topH = height * 0.55f
        val volTop = topH + 8f
        val volH = height * 0.14f
        val rsiTop = volTop + volH + 8f
        val rsiH = if (showADX) height * 0.12f else height * 0.20f
        val adxTop = rsiTop + rsiH + 8f
        val adxH = height - adxTop - 8f

        drawGrid(c, 0f, 56f, topH, 5)
        drawCandles(c, d, 8f, 56f, width - 8f, topH)
        drawVolume(c, d, volTop, volH)
        if (showRSI) drawOscillator(c, d, rsiTop, rsiH, false)
        if (showADX) drawOscillator(c, d, adxTop, max(20f, adxH), true)
        drawHeader(c, d)
    }

    private fun drawHeader(c: Canvas, d: List<OracleOhlcvPoint>) {
        label(c, "$ticker  •  $mode", 12f, 20f, Color.WHITE, 15f)
        if (d.isNotEmpty()) {
            label(c, "${money(d.last().close)}", width - 88f, 20f, text, 11f)
        }
        if (selectedIndex in d.indices) {
            val p = d[selectedIndex]
            val info = "O ${money(p.open)}  H ${money(p.high)}  L ${money(p.low)}  C ${money(p.close)}  •  ${dateTime(p.timestamp)}"
            label(c, info, 12f, 40f, if (p.close >= p.open) green else red, 10f)
        } else {
            label(c, "Atinge o lumânare pentru OHLC + volum", 12f, 40f, text, 10f)
        }
    }

    private fun drawGrid(c: Canvas, l: Float, top: Float, b: Float, rows: Int) {
        paints.strokeWidth = 1f
        paints.color = grid
        for (i in 0..rows) {
            val y = top + (b - top) * i / rows
            c.drawLine(l, y, width.toFloat(), y, paints)
        }
        for (i in 0..8) {
            val x = width * i / 8f
            c.drawLine(x, top, x, b, paints)
        }
    }

    private fun drawCandles(c: Canvas, d: List<OracleOhlcvPoint>, left: Float, top: Float, right: Float, bottom: Float) {
        val minP = d.minOf { it.low }
        val maxP = d.maxOf { it.high }
        val span = max(0.0001, maxP - minP)
        val step = (right - left) / max(1, d.size)
        val bodyW = max(2f, step * .62f)
        fun y(v: Double): Float = bottom - ((v - minP) / span * (bottom - top - 18f)).toFloat()

        val closes = d.map { it.close }
        if (showBB) {
            val upper = mutableListOf<Float>()
            val mid = mutableListOf<Float>()
            val lower = mutableListOf<Float>()
            for (i in d.indices) {
                val a = closes.subList(max(0, i - 19), i + 1)
                val m = a.average()
                val sd = sqrt(a.sumOf { (it - m) * (it - m) } / a.size)
                upper += y(m + 2 * sd)
                mid += y(m)
                lower += y(m - 2 * sd)
            }
            lineSeries(c, upper, blue, step, left)
            lineSeries(c, mid, Color.rgb(80, 110, 170), step, left)
            lineSeries(c, lower, blue, step, left)
        }
        if (showMA) {
            lineSeries(c, moving(closes, 10).map { y(it) }, gold, step, left)
            lineSeries(c, ema(closes, 10).map { y(it) }, purple, step, left)
        }
        if (showIchi) {
            val t = mutableListOf<Float>(); val k = mutableListOf<Float>(); val sa = mutableListOf<Float>(); val sb = mutableListOf<Float>()
            for (i in d.indices) {
                val h9 = d.subList(max(0, i - 8), i + 1).maxOf { it.high }
                val l9 = d.subList(max(0, i - 8), i + 1).minOf { it.low }
                val h26 = d.subList(max(0, i - 25), i + 1).maxOf { it.high }
                val l26 = d.subList(max(0, i - 25), i + 1).minOf { it.low }
                val h52 = d.subList(max(0, i - 51), i + 1).maxOf { it.high }
                val l52 = d.subList(max(0, i - 51), i + 1).minOf { it.low }
                val tv = (h9 + l9) / 2
                val kv = (h26 + l26) / 2
                t += y(tv); k += y(kv); sa += y((tv + kv) / 2); sb += y((h52 + l52) / 2)
            }
            lineSeries(c, t, Color.rgb(255, 90, 130), step, left)
            lineSeries(c, k, blue, step, left)
            lineSeries(c, sa, Color.rgb(80, 210, 150), step, left)
            lineSeries(c, sb, Color.rgb(210, 100, 210), step, left)
        }

        d.forEachIndexed { i, p ->
            val x = left + (i + .5f) * step
            val yo = y(p.open); val yc = y(p.close); val yh = y(p.high); val yl = y(p.low)
            paints.color = if (p.close >= p.open) green else red
            paints.strokeWidth = 1.5f
            c.drawLine(x, yh, x, yl, paints)
            c.drawRect(x - bodyW / 2, min(yo, yc), x + bodyW / 2, max(yo, yc).coerceAtLeast(min(yo, yc) + 1.5f), paints)
        }

        drawTrendAnalysis(c, d, left, top, right, bottom, minP, maxP, span, step)

        if (selectedIndex in d.indices) {
            val x = left + (selectedIndex + .5f) * step
            val p = d[selectedIndex]
            paints.color = Color.argb(180, 255, 255, 255)
            paints.strokeWidth = 1f
            c.drawLine(x, top, x, bottom, paints)
            paints.color = if (p.close >= p.open) green else red
            c.drawCircle(x, y(p.close), 3.5f, paints)
        }

        label(c, "BB(20,2)", 12f, top + 14f, blue, 10f)
        if (showMA) label(c, "EMA10 / MA10", 82f, top + 14f, gold, 10f)
        if (showIchi) label(c, "Ichimoku", 178f, top + 14f, Color.rgb(80, 210, 150), 10f)
        label(c, money(maxP), width - 82f, top + 28f, text, 10f)
        label(c, money(minP), width - 82f, bottom - 6f, text, 10f)
    }

    private fun drawTrendAnalysis(c: Canvas, d: List<OracleOhlcvPoint>, left: Float, top: Float, right: Float, bottom: Float, minP: Double, maxP: Double, span: Double, step: Float) {
        val n = min(50, d.size)
        if (n < 12) return
        val start = d.size - n
        var sumX = 0.0; var sumY = 0.0; var sumXX = 0.0; var sumXY = 0.0
        for (i in 0 until n) {
            val x = i.toDouble(); val yy = d[start + i].close
            sumX += x; sumY += yy; sumXX += x * x; sumXY += x * yy
        }
        val den = n * sumXX - sumX * sumX
        if (abs(den) < 0.000001) return
        val slope = (n * sumXY - sumX * sumY) / den
        val intercept = (sumY - slope * sumX) / n
        var err = 0.0
        for (i in 0 until n) {
            val r = d[start + i].close - (intercept + slope * i)
            err += r * r
        }
        val sd = sqrt(err / n)
        fun y(v: Double): Float = bottom - ((v - minP) / span * (bottom - top - 18f)).toFloat()
        fun x(i: Int): Float = left + (i + .5f) * step

        val bullish = slope > 0.0
        val trendColor = if (bullish) Color.rgb(30, 210, 150) else Color.rgb(255, 95, 95)
        paints.color = trendColor
        paints.strokeWidth = 2.2f
        c.drawLine(x(0), y(intercept), x(n - 1), y(intercept + slope * (n - 1)), paints)
        paints.strokeWidth = 1.1f
        paints.color = Color.argb(190, trendColor shr 16 and 255, trendColor shr 8 and 255, trendColor and 255)
        c.drawLine(x(0), y(intercept + sd), x(n - 1), y(intercept + slope * (n - 1) + sd), paints)
        c.drawLine(x(0), y(intercept - sd), x(n - 1), y(intercept + slope * (n - 1) - sd), paints)

        val support = d.takeLast(min(30, d.size)).minOf { it.low }
        val resistance = d.takeLast(min(30, d.size)).maxOf { it.high }
        paints.color = Color.argb(150, 80, 180, 255)
        paints.strokeWidth = 1f
        c.drawLine(left, y(support), right, y(support), paints)
        paints.color = Color.argb(150, 255, 190, 45)
        c.drawLine(left, y(resistance), right, y(resistance), paints)
        label(c, "SUPORT ${money(support)}", 12f, y(support) - 4f, Color.rgb(80, 180, 255), 8f)
        label(c, "REZIST. ${money(resistance)}", 12f, y(resistance) + 10f, gold, 8f)

        val lastX = x(n - 1)
        val futureBars = min(12, max(4f, (right - lastX) / max(1f, step)).toInt())
        val target = intercept + slope * (n - 1 + futureBars)
        val endX = min(right - 6f, lastX + futureBars * step)
        val endY = y(target)
        paints.color = trendColor
        paints.strokeWidth = 2.5f
        c.drawLine(lastX, y(intercept + slope * (n - 1)), endX, endY, paints)
        c.drawLine(endX, endY, endX - 10f, endY + if (bullish) 7f else -7f, paints)
        c.drawLine(endX, endY, endX - 10f, endY + if (bullish) -7f else 7f, paints)
        label(c, if (bullish) "TREND ASCENDENT" else "TREND DESCENDENT", 12f, top + 30f, trendColor, 10f)
    }

    private fun drawVolume(c: Canvas, d: List<OracleOhlcvPoint>, top: Float, h: Float) {
        val maxV = max(1.0, d.maxOf { it.volume })
        val step = width / max(1, d.size).toFloat()
        val bw = max(2f, step * .65f)
        d.forEachIndexed { i, p ->
            val x = (i + .5f) * step
            val bh = (p.volume / maxV * h).toFloat()
            paints.color = if (p.close >= p.open) Color.rgb(20, 145, 55) else Color.rgb(145, 25, 30)
            c.drawRect(x - bw / 2, top + h - bh, x + bw / 2, top + h, paints)
        }
        label(c, "VOLUME", 12f, top + 12f, text, 10f)
        if (selectedIndex in d.indices) label(c, "VOL ${volumeText(d[selectedIndex].volume)}", 82f, top + 12f, Color.WHITE, 10f)
        label(c, "MAX ${volumeText(maxV)}", width - 96f, top + 12f, text, 8f)
    }

    private fun drawOscillator(c: Canvas, d: List<OracleOhlcvPoint>, top: Float, h: Float, adxMode: Boolean) {
        paints.color = grid; paints.strokeWidth = 1f
        c.drawLine(0f, top, width.toFloat(), top, paints)
        c.drawLine(0f, top + h, width.toFloat(), top + h, paints)
        val vals = if (adxMode) adx(d.map { it.high }, d.map { it.low }, d.map { it.close }, 14) else rsi(d.map { it.close }, 14)
        lineSeries(c, vals.map { top + h - (it / 100f) * h }, if (adxMode) gold else blue, width / max(1, d.size).toFloat(), 0f)
        label(c, if (adxMode) "ADX(14)" else "RSI(14)", 12f, top + 12f, if (adxMode) gold else blue, 10f)
        if (!adxMode) {
            label(c, "70", width - 28f, top + h * .3f, text, 8f)
            label(c, "30", width - 28f, top + h * .7f, text, 8f)
        }
    }

    private fun lineSeries(c: Canvas, values: List<Float>, color: Int, step: Float, left: Float) {
        paints.color = color; paints.style = Paint.Style.STROKE; paints.strokeWidth = 1.4f
        var prev = Float.NaN
        values.forEachIndexed { i, v ->
            if (v.isFinite()) {
                val x = left + (i + .5f) * step
                if (prev.isFinite()) c.drawLine(left + (i - .5f) * step, prev, x, v, paints)
                prev = v
            }
        }
        paints.style = Paint.Style.FILL
    }

    private fun moving(v: List<Double>, n: Int): List<Double> {
        val o = mutableListOf<Double>()
        v.indices.forEach { i -> o += v.subList(max(0, i - n + 1), i + 1).average() }
        return o
    }

    private fun ema(v: List<Double>, n: Int): List<Double> {
        if (v.isEmpty()) return emptyList()
        val o = mutableListOf(v.first()); val k = 2.0 / (n + 1)
        for (i in 1 until v.size) o += v[i] * k + o.last() * (1 - k)
        return o
    }

    private fun rsi(v: List<Double>, n: Int): List<Float> {
        val out = MutableList(v.size) { 50f }
        for (i in v.indices) {
            val a = v.subList(max(1, i - n + 1), i + 1); var g = 0.0; var l = 0.0
            for (j in 1 until a.size) { val d = a[j] - a[j - 1]; if (d >= 0) g += d else l -= d }
            val rg = g / max(1, a.size - 1); val rl = l / max(1, a.size - 1)
            out[i] = (if (rl == 0.0) 100.0 else 100 - 100 / (1 + rg / rl)).toFloat()
        }
        return out
    }

    private fun adx(h: List<Double>, l: List<Double>, c: List<Double>, n: Int): List<Float> {
        val out = MutableList(c.size) { 35f }
        if (c.size < 3) return out
        for (i in 1 until c.size) {
            val s = max(0, i - n + 1); var tr = 0.0; var pd = 0.0; var md = 0.0
            for (j in max(1, s) until i + 1) {
                tr += maxOf(h[j] - l[j], abs(h[j] - c[j - 1]), abs(l[j] - c[j - 1]))
                val up = h[j] - h[j - 1]; val dn = l[j - 1] - l[j]
                if (up > dn && up > 0) pd += up
                if (dn > up && dn > 0) md += dn
            }
            val pi = if (tr > 0) 100 * pd / tr else 0.0; val mi = if (tr > 0) 100 * md / tr else 0.0
            out[i] = (if (pi + mi > 0) 100 * abs(pi - mi) / (pi + mi) else 0.0).toFloat()
        }
        return out
    }

    private fun clampOffset() { offset = offset.coerceIn(0, max(0, data.size - visible)) }
    private fun label(c: Canvas, s: String, x: Float, y: Float, color: Int, size: Float) { paints.color = color; paints.textSize = size; paints.typeface = android.graphics.Typeface.DEFAULT_BOLD; c.drawText(s, x.coerceIn(4f, max(4f, width - 4f)), y, paints) }
    private fun money(v: Double) = "%.2f".format(Locale.US, v)
    private fun volumeText(v: Double): String = when { v >= 1_000_000_000 -> "%.2fB".format(Locale.US, v / 1e9); v >= 1_000_000 -> "%.2fM".format(Locale.US, v / 1e6); v >= 1_000 -> "%.1fK".format(Locale.US, v / 1e3); else -> "%.0f".format(Locale.US, v) }
    private fun dateTime(ts: Long): String = SimpleDateFormat(if (mode == "1D") "dd MMM yyyy" else "dd MMM HH:mm", Locale.US).format(Date(ts))
}
