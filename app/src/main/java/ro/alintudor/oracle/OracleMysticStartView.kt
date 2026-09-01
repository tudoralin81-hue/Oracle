package ro.alintudor.oracle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** B514 native Start UI only. Protected modules are untouched. */
class OracleMysticStartView(
    context: Context,
    private val onModule: (String) -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val hits = mutableListOf<Pair<RectF, String>>()
    private val gold = Color.rgb(238, 190, 60)
    private val brightGold = Color.rgb(255, 215, 82)
    private val white = Color.rgb(244, 239, 226)
    private val muted = Color.rgb(151, 143, 126)
    private val green = Color.rgb(65, 245, 92)

    private val modules = listOf(
        Module("portfolio", "PORTFOLIO", "OVERVIEW", Color.rgb(220, 60, 255)),
        Module("watchlist", "WATCHLIST", "TRACK & FOCUS", Color.rgb(255, 205, 35)),
        Module("analysis", "ANALYSIS", "CHARTS & TOOLS", Color.rgb(25, 220, 255)),
        Module("growth", "GROWTH", "FUTURE SCAN", Color.rgb(115, 255, 45)),
        Module("alerts", "ALERTS", "STAY AHEAD", Color.rgb(255, 55, 40)),
        Module("news", "NEWS", "MARKET PULSE", Color.rgb(35, 215, 255)),
        Module("knowledge", "KNOWLEDGE", "LEARN & EVOLVE", Color.rgb(255, 205, 45)),
        Module("stock", "STOCK", "INTELLIGENCE", Color.rgb(220, 65, 255))
    )

    private data class Module(val key: String, val title: String, val subtitle: String, val color: Int)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val wide = w / h > 1.15f
        val designW = if (wide) 1120f else 720f
        val designH = if (wide) 900f else 1080f
        val scale = min(w / designW, h / designH)
        val ox = (w - designW * scale) * 0.5f
        val oy = (h - designH * scale) * 0.5f

        fun px(v: Float): Float = ox + v * scale
        fun py(v: Float): Float = oy + v * scale
        fun ps(v: Float): Float = v * scale

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(1, 2, 4)
        paint.alpha = 255
        c.drawRect(0f, 0f, w, h, paint)

        val t = System.nanoTime() / 1000000000.0
        val cx = px(designW * 0.5f)
        val eyeY = py(if (wide) 235f else 285f)
        val eyeR = ps(if (wide) 112f else 132f)

        drawBackgroundGrid(c, cx, eyeY, ps(if (wide) 110f else 105f), ps(18f), scale)
        drawCentered(c, "ORACLE", cx, py(if (wide) 78f else 105f), ps(31f), brightGold, Typeface.SERIF, 0.18f)
        drawCentered(c, "STOCK INTELLIGENCE", cx, py(if (wide) 102f else 130f), ps(9f), gold, Typeface.DEFAULT, 0.25f)
        drawSigil(c, cx, py(if (wide) 38f else 57f), ps(22f), brightGold)
        drawAnimatedEye(c, cx, eyeY, eyeR, t)
        drawCentered(c, "SEE MORE.  KNOW FIRST.", cx, py(if (wide) 382f else 465f), ps(10.5f), white, Typeface.DEFAULT, 0.27f)

        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 180
        paint.strokeWidth = ps(0.7f)
        c.drawLine(px(if (wide) 365f else 215f), py(if (wide) 401f else 486f), px(if (wide) 755f else 505f), py(if (wide) 401f else 486f), paint)
        drawDiamond(c, cx, py(if (wide) 401f else 486f), ps(4f), brightGold)

        hits.clear()
        if (wide) {
            drawWideCards(c, px, py, ps, t)
            drawStatus(c, ::px, ::py, ::ps, t, true)
            drawFooter(c, cx, ::py, ::ps, 865f)
        } else {
            drawPhoneCards(c, px, py, ps, t)
            drawStatus(c, ::px, ::py, ::ps, t, false)
            drawFooter(c, cx, ::py, ::ps, 1042f)
        }
        postInvalidateDelayed(32L)
    }

    private fun drawBackgroundGrid(c: Canvas, cx: Float, cy: Float, first: Float, step: Float, time: Double, scale: Float) {
        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 30
        paint.strokeWidth = 0.55f * scale
        for (i in 0 until 14) c.drawCircle(cx, cy, first + i * step, paint)
        for (i in 0 until 32) {
            val a = i * Math.PI / 16.0
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            val inner = first - 15f * scale
            val outer = first + 230f * scale
            c.drawLine(cx + dx * inner, cy + dy * inner, cx + dx * outer, cy + dy * outer, paint)
        }
    }

    private fun drawAnimatedEye(c: Canvas, x: Float, y: Float, r: Float, time: Double) {
        val pulse = (0.5 + 0.5 * sin(time * 1.7)).toFloat()
        paint.style = Paint.Style.STROKE
        path.reset()
        path.moveTo(x - r, y)
        path.cubicTo(x - r * 0.58f, y - r * 0.55f, x + r * 0.58f, y - r * 0.55f, x + r, y)
        path.cubicTo(x + r * 0.58f, y + r * 0.55f, x - r * 0.58f, y + r * 0.55f, x - r, y)
        paint.color = brightGold
        paint.alpha = (175 + 75 * pulse).toInt()
        paint.strokeWidth = r * 0.018f
        c.drawPath(path, paint)
        paint.alpha = (45 + 85 * pulse).toInt()
        paint.strokeWidth = r * 0.012f
        c.drawCircle(x, y, r * (0.48f + 0.035f * pulse), paint)
        paint.color = green
        paint.alpha = (135 + 120 * pulse).toInt()
        paint.strokeWidth = r * 0.018f
        c.drawCircle(x, y, r * (0.30f + 0.025f * pulse), paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(2, 12, 5)
        paint.alpha = 255
        c.drawCircle(x, y, r * 0.28f, paint)
        paint.color = Color.rgb(70, 255, 95)
        c.drawCircle(x, y, r * (0.095f + 0.035f * pulse), paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(255, 110, 35)
        paint.alpha = (75 + 90 * pulse).toInt()
        paint.strokeWidth = r * 0.009f
        for (i in 0 until 28) {
            val a = i * Math.PI / 14.0
            val inner = r * 0.40f
            val outer = r * (0.56f + 0.055f * pulse)
            c.drawLine(x + cos(a).toFloat() * inner, y + sin(a).toFloat() * inner, x + cos(a).toFloat() * outer, y + sin(a).toFloat() * outer, paint)
        }
    }

    private fun drawPhoneCards(c: Canvas, x: (Float) -> Float, y: (Float) -> Float, s: (Float) -> Float, time: Double) {
        val left = 20f
        val top = 510f
        val cw = 164f
        val ch = 120f
        val gap = 12f
        for (i in modules.indices) {
            val col = i % 4
            val row = i / 4
            val l = left + col * (cw + gap)
            val q = top + row * (ch + gap)
            val r = RectF(x(l), y(q), x(l + cw), y(q + ch))
            hits += r to modules[i].key
            drawCard(c, r, modules[i], s, time, i)
        }
    }

    private fun drawWideCards(c: Canvas, x: (Float) -> Float, y: (Float) -> Float, s: (Float) -> Float, time: Double) {
        val cw = 235f
        val ch = 112f
        val gap = 14f
        val left = 130f
        val top = 435f
        for (i in modules.indices) {
            val col = i % 4
            val row = i / 4
            val l = left + col * (cw + gap)
            val q = top + row * (ch + gap)
            val r = RectF(x(l), y(q), x(l + cw), y(q + ch))
            hits += r to modules[i].key
            drawCard(c, r, modules[i], s, time, i)
        }
    }

    private fun drawCard(c: Canvas, r: RectF, m: Module, s: (Float) -> Float, time: Double, index: Int) {
        val pulse = (0.5 + 0.5 * sin(time * 1.35 + index * 0.48)).toFloat()
        val cx = r.centerX()
        val cy = r.top + r.height() * 0.37f
        val ir = min(r.width(), r.height()) * 0.25f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(2, 4, 7)
        paint.alpha = 250
        c.drawRoundRect(r, s(9f), s(9f), paint)
        paint.style = Paint.Style.STROKE
        paint.color = m.color
        paint.alpha = (150 + 95 * pulse).toInt()
        paint.strokeWidth = s(1.25f)
        c.drawRoundRect(r, s(9f), s(9f), paint)
        paint.alpha = (40 + 90 * pulse).toInt()
        paint.strokeWidth = s(1f)
        c.drawCircle(cx, cy, ir * (1.06f + 0.10f * pulse), paint)
        paint.alpha = (120 + 120 * pulse).toInt()
        paint.strokeWidth = s(1.1f)
        c.drawCircle(cx, cy, ir, paint)
        paint.alpha = 90
        c.drawCircle(cx, cy, ir * 0.78f, paint)
        paint.alpha = 255
        paint.strokeWidth = s(1.9f)
        when (m.key) {
            "watchlist" -> drawEyeIcon(c, cx, cy, ir * 0.70f, m.color)
            "portfolio" -> {
                c.drawRect(cx - ir * 0.45f, cy - ir * 0.35f, cx + ir * 0.45f, cy + ir * 0.35f, paint)
                c.drawCircle(cx + ir * 0.22f, cy + ir * 0.15f, ir * 0.17f, paint)
            }
            "analysis", "growth" -> {
                path.reset()
                path.moveTo(cx - ir * 0.60f, cy + ir * 0.35f)
                path.lineTo(cx - ir * 0.20f, cy)
                path.lineTo(cx + ir * 0.02f, cy + ir * 0.15f)
                path.lineTo(cx + ir * 0.58f, cy - ir * 0.48f)
                c.drawPath(path, paint)
            }
            "alerts" -> {
                c.drawArc(RectF(cx - ir * 0.48f, cy - ir * 0.45f, cx + ir * 0.48f, cy + ir * 0.45f), 210f, 120f, false, paint)
                c.drawLine(cx - ir * 0.22f, cy + ir * 0.40f, cx + ir * 0.22f, cy + ir * 0.40f, paint)
            }
            "news" -> {
                c.drawRect(cx - ir * 0.46f, cy - ir * 0.43f, cx + ir * 0.46f, cy + ir * 0.43f, paint)
                for (j in -1..1) c.drawLine(cx - ir * 0.28f, cy + j * ir * 0.18f, cx + ir * 0.28f, cy + j * ir * 0.18f, paint)
            }
            "knowledge" -> {
                c.drawRect(cx - ir * 0.48f, cy - ir * 0.43f, cx, cy + ir * 0.43f, paint)
                c.drawRect(cx, cy - ir * 0.43f, cx + ir * 0.48f, cy + ir * 0.43f, paint)
            }
            else -> drawSigil(c, cx, cy, ir * 0.70f, m.color)
        }
        drawCentered(c, m.title, cx, r.top + r.height() * 0.72f, s(11.5f), white, Typeface.DEFAULT, 0.01f)
        drawCentered(c, m.subtitle, cx, r.top + r.height() * 0.87f, s(7.8f), m.color, Typeface.DEFAULT, 0.02f)
        paint.color = m.color
        paint.alpha = (130 + 110 * pulse).toInt()
        paint.strokeWidth = s(1f)
        paint.style = Paint.Style.STROKE
        c.drawLine(cx - s(35f), r.bottom - s(13f), cx + s(35f), r.bottom - s(13f), paint)
        drawDiamond(c, cx, r.bottom - s(13f), s(3.5f), m.color)
    }

    private fun drawStatus(c: Canvas, x: (Float) -> Float, y: (Float) -> Float, s: (Float) -> Float, time: Double, wide: Boolean) {
        val pulse = (0.5 + 0.5 * sin(time * 0.85)).toFloat()
        val top = if (wide) 715f else 800f
        val bottom = if (wide) 795f else 886f
        val r = RectF(x(if (wide) 130f else 20f), y(top), x(if (wide) 990f else 700f), y(bottom))
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(3, 6, 8)
        paint.alpha = 250
        c.drawRoundRect(r, s(10f), s(10f), paint)
        paint.style = Paint.Style.STROKE
        paint.color = green
        paint.alpha = (120 + 110 * pulse).toInt()
        paint.strokeWidth = s(1.1f)
        c.drawRoundRect(r, s(10f), s(10f), paint)
        val baseX = if (wide) 190f else 72f
        val textX = if (wide) 235f else 112f
        val localX = if (wide) 630f else 416f
        val signalX = if (wide) 585f else 378f
        val dotX = if (wide) 955f else 675f
        drawMiniEye(c, x(baseX), y(top + 42f), s(24f), green, pulse)
        drawLeft(c, "ORACLE READY", x(textX), y(top + 37f), s(14f), white, Typeface.DEFAULT_BOLD)
        drawLeft(c, "Market Intelligence Active", x(textX), y(top + 61f), s(9f), green, Typeface.DEFAULT)
        paint.color = gold
        paint.alpha = (150 + 90 * pulse).toInt()
        paint.strokeWidth = s(0.9f)
        c.drawCircle(x(signalX), y(top + 42f), s(24f + 3f * pulse), paint)
        for (i in -2..2) {
            val xx = x(signalX + i * 6f)
            val half = s(9f + kotlin.math.abs(i) * 2f)
            c.drawLine(xx, y(top + 42f) - half, xx, y(top + 42f) + half, paint)
        }
        drawLeft(c, "LOCAL INTELLIGENCE", x(localX), y(top + 37f), s(13f), white, Typeface.DEFAULT_BOLD)
        drawLeft(c, "Synced & Protected", x(localX), y(top + 61f), s(9f), green, Typeface.DEFAULT)
        paint.style = Paint.Style.FILL
        paint.color = green
        paint.alpha = 255
        c.drawCircle(x(dotX), y(top + 42f), s(7f + 3f * pulse), paint)
    }

    private fun drawFooter(c: Canvas, cx: Float, y: (Float) -> Float, s: (Float) -> Float, base: Float) {
        drawCentered(c, "ORACLE", cx, y(base - 60f), s(20f), gold, Typeface.SERIF, 0.34f)
        drawCentered(c, "SEE MORE.  KNOW FIRST.", cx, y(base - 35f), s(8f), muted, Typeface.DEFAULT, 0.22f)
        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 145
        paint.strokeWidth = s(0.6f)
        c.drawLine(cx - s(75f), y(base - 15f), cx + s(75f), y(base - 15f), paint)
        drawDiamond(c, cx, y(base - 15f), s(3f), gold)
        drawCentered(c, "357AT2026", cx, y(base + 27f), s(11f), brightGold, Typeface.DEFAULT_BOLD, 0.16f)
    }

    private fun drawMiniEye(c: Canvas, x: Float, y: Float, r: Float, color: Int, pulse: Float) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.alpha = (165 + 80 * pulse).toInt()
        paint.strokeWidth = r * 0.10f
        path.reset()
        path.moveTo(x - r, y)
        path.cubicTo(x - r * 0.55f, y - r * 0.55f, x + r * 0.55f, y - r * 0.55f, x + r, y)
        path.cubicTo(x + r * 0.55f, y + r * 0.55f, x - r * 0.55f, y + r * 0.55f, x - r, y)
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        c.drawCircle(x, y, r * (0.18f + 0.05f * pulse), paint)
    }

    private fun drawEyeIcon(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.alpha = 255
        paint.strokeWidth = r * 0.09f
        path.reset()
        path.moveTo(x - r, y)
        path.cubicTo(x - r * 0.55f, y - r * 0.55f, x + r * 0.55f, y - r * 0.55f, x + r, y)
        path.cubicTo(x + r * 0.55f, y + r * 0.55f, x - r * 0.55f, y + r * 0.55f, x - r, y)
        c.drawPath(path, paint)
        c.drawCircle(x, y, r * 0.23f, paint)
    }

    private fun drawSigil(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.alpha = 245
        paint.strokeWidth = r * 0.065f
        path.reset()
        path.moveTo(x - r * 0.75f, y)
        path.lineTo(x, y - r * 0.5f)
        path.lineTo(x + r * 0.75f, y)
        path.lineTo(x, y + r * 0.5f)
        path.close()
        c.drawPath(path, paint)
        c.drawCircle(x, y, r * 0.18f, paint)
    }

    private fun drawDiamond(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.alpha = 230
        paint.strokeWidth = r * 0.32f
        path.reset()
        path.moveTo(x, y - r)
        path.lineTo(x + r, y)
        path.lineTo(x, y + r)
        path.lineTo(x - r, y)
        path.close()
        c.drawPath(path, paint)
    }

    private fun drawCentered(c: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface, spacing: Float) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 255
        paint.typeface = typeface
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        paint.letterSpacing = spacing
        c.drawText(text, x, y, paint)
        paint.letterSpacing = 0f
    }

    private fun drawLeft(c: Canvas, text: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 255
        paint.typeface = typeface
        paint.textSize = size
        paint.textAlign = Paint.Align.LEFT
        c.drawText(text, x, y, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) {
            for ((r, key) in hits) {
                if (r.contains(e.x, e.y)) {
                    onModule(key)
                    performClick()
                    return true
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
