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

/** Native B514 Start only. Protected modules are not modified. */
class OracleMysticStartView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val hit = mutableListOf<Pair<RectF, String>>()
    private var sc = 1f
    private var ox = 0f
    private var oy = 0f
    private val gold = Color.rgb(238, 190, 60)
    private val brightGold = Color.rgb(255, 215, 82)
    private val green = Color.rgb(65, 245, 92)
    private val white = Color.rgb(244, 239, 226)
    private val muted = Color.rgb(151, 143, 126)

    private data class M(val key: String, val title: String, val sub: String, val color: Int)
    private val modules = listOf(
        M("portfolio", "PORTFOLIO", "OVERVIEW", Color.rgb(220, 60, 255)),
        M("watchlist", "WATCHLIST", "TRACK & FOCUS", Color.rgb(255, 205, 35)),
        M("analysis", "ANALYSIS", "CHARTS & TOOLS", Color.rgb(25, 220, 255)),
        M("growth", "GROWTH", "FUTURE SCAN", Color.rgb(115, 255, 45)),
        M("alerts", "ALERTS", "STAY AHEAD", Color.rgb(255, 55, 40)),
        M("news", "NEWS", "MARKET PULSE", Color.rgb(35, 215, 255)),
        M("knowledge", "KNOWLEDGE", "LEARN & EVOLVE", Color.rgb(255, 205, 45)),
        M("stock", "STOCK", "INTELLIGENCE", Color.rgb(220, 65, 255))
    )

    private fun X(v: Float) = ox + v * sc
    private fun Y(v: Float) = oy + v * sc
    private fun S(v: Float) = v * sc

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val wide = w / h > 1.15f
        val dw = if (wide) 1120f else 720f
        val dh = if (wide) 900f else 1080f
        sc = min(w / dw, h / dh)
        ox = (w - dw * sc) * 0.5f
        oy = (h - dh * sc) * 0.5f
        p.style = Paint.Style.FILL
        p.color = Color.rgb(1, 2, 4)
        p.alpha = 255
        c.drawRect(0f, 0f, w, h, p)

        val time = System.nanoTime() / 1000000000.0
        val cx = X(dw * 0.5f)
        val eyeY = Y(if (wide) 235f else 285f)
        val eyeR = S(if (wide) 112f else 132f)
        drawGrid(c, cx, eyeY, S(if (wide) 110f else 105f), S(18f))
        text(c, "ORACLE", cx, Y(if (wide) 78f else 105f), S(31f), brightGold, Typeface.SERIF, 0.18f, true)
        text(c, "STOCK INTELLIGENCE", cx, Y(if (wide) 102f else 130f), S(9f), gold, Typeface.DEFAULT, 0.25f, true)
        sigil(c, cx, Y(if (wide) 38f else 57f), S(22f), brightGold)
        eye(c, cx, eyeY, eyeR, time)
        text(c, "SEE MORE.  KNOW FIRST.", cx, Y(if (wide) 382f else 465f), S(10.5f), white, Typeface.DEFAULT, 0.27f, true)
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 180
        p.strokeWidth = S(0.7f)
        c.drawLine(X(if (wide) 365f else 215f), Y(if (wide) 401f else 486f), X(if (wide) 755f else 505f), Y(if (wide) 401f else 486f), p)
        diamond(c, cx, Y(if (wide) 401f else 486f), S(4f), brightGold)

        hit.clear()
        if (wide) cards(c, 130f, 435f, 235f, 112f, 14f, time) else cards(c, 20f, 510f, 164f, 120f, 12f, time)
        status(c, wide, time)
        footer(c, cx, if (wide) 865f else 1042f)
        postInvalidateDelayed(32L)
    }

    private fun drawGrid(c: Canvas, cx: Float, cy: Float, first: Float, step: Float) {
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 30
        p.strokeWidth = S(0.55f)
        for (i in 0 until 14) c.drawCircle(cx, cy, first + i * step, p)
        for (i in 0 until 32) {
            val a = i * Math.PI / 16.0
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            c.drawLine(cx + dx * (first - S(15f)), cy + dy * (first - S(15f)), cx + dx * (first + S(230f)), cy + dy * (first + S(230f)), p)
        }
    }

    private fun eye(c: Canvas, x: Float, y: Float, r: Float, time: Double) {
        val q = (0.5 + 0.5 * sin(time * 1.7)).toFloat()
        p.style = Paint.Style.STROKE
        path.reset()
        path.moveTo(x - r, y)
        path.cubicTo(x - r * 0.58f, y - r * 0.55f, x + r * 0.58f, y - r * 0.55f, x + r, y)
        path.cubicTo(x + r * 0.58f, y + r * 0.55f, x - r * 0.58f, y + r * 0.55f, x - r, y)
        p.color = brightGold
        p.alpha = (175 + 75 * q).toInt()
        p.strokeWidth = r * 0.018f
        c.drawPath(path, p)
        p.alpha = (45 + 85 * q).toInt()
        p.strokeWidth = r * 0.012f
        c.drawCircle(x, y, r * (0.48f + 0.035f * q), p)
        p.color = green
        p.alpha = (135 + 120 * q).toInt()
        p.strokeWidth = r * 0.018f
        c.drawCircle(x, y, r * (0.30f + 0.025f * q), p)
        p.style = Paint.Style.FILL
        p.color = Color.rgb(2, 12, 5)
        p.alpha = 255
        c.drawCircle(x, y, r * 0.28f, p)
        p.color = Color.rgb(70, 255, 95)
        c.drawCircle(x, y, r * (0.095f + 0.035f * q), p)
        p.style = Paint.Style.STROKE
        p.color = Color.rgb(255, 110, 35)
        p.alpha = (75 + 90 * q).toInt()
        p.strokeWidth = r * 0.009f
        for (i in 0 until 28) {
            val a = i * Math.PI / 14.0
            val inner = r * 0.40f
            val outer = r * (0.56f + 0.055f * q)
            c.drawLine(x + cos(a).toFloat() * inner, y + sin(a).toFloat() * inner, x + cos(a).toFloat() * outer, y + sin(a).toFloat() * outer, p)
        }
    }

    private fun cards(c: Canvas, left: Float, top: Float, cw: Float, ch: Float, gap: Float, time: Double) {
        for (i in modules.indices) {
            val col = i % 4
            val row = i / 4
            val l = left + col * (cw + gap)
            val t = top + row * (ch + gap)
            val r = RectF(X(l), Y(t), X(l + cw), Y(t + ch))
            hit += r to modules[i].key
            card(c, r, modules[i], time, i)
        }
    }

    private fun card(c: Canvas, r: RectF, m: M, time: Double, i: Int) {
        val q = (0.5 + 0.5 * sin(time * 1.35 + i * 0.48)).toFloat()
        val cx = r.centerX()
        val cy = r.top + r.height() * 0.37f
        val ir = min(r.width(), r.height()) * 0.25f
        p.style = Paint.Style.FILL
        p.color = Color.rgb(2, 4, 7)
        p.alpha = 250
        c.drawRoundRect(r, S(9f), S(9f), p)
        p.style = Paint.Style.STROKE
        p.color = m.color
        p.alpha = (150 + 95 * q).toInt()
        p.strokeWidth = S(1.25f)
        c.drawRoundRect(r, S(9f), S(9f), p)
        p.alpha = (40 + 90 * q).toInt()
        p.strokeWidth = S(1f)
        c.drawCircle(cx, cy, ir * (1.06f + 0.10f * q), p)
        p.alpha = (120 + 120 * q).toInt()
        p.strokeWidth = S(1.1f)
        c.drawCircle(cx, cy, ir, p)
        p.alpha = 90
        c.drawCircle(cx, cy, ir * 0.78f, p)
        p.alpha = 255
        p.strokeWidth = S(1.9f)
        when (m.key) {
            "watchlist" -> miniEye(c, cx, cy, ir * 0.70f, m.color)
            "portfolio" -> {
                c.drawRect(cx - ir * 0.45f, cy - ir * 0.35f, cx + ir * 0.45f, cy + ir * 0.35f, p)
                c.drawCircle(cx + ir * 0.22f, cy + ir * 0.15f, ir * 0.17f, p)
            }
            "analysis", "growth" -> {
                path.reset()
                path.moveTo(cx - ir * 0.60f, cy + ir * 0.35f)
                path.lineTo(cx - ir * 0.20f, cy)
                path.lineTo(cx + ir * 0.02f, cy + ir * 0.15f)
                path.lineTo(cx + ir * 0.58f, cy - ir * 0.48f)
                c.drawPath(path, p)
            }
            "alerts" -> {
                c.drawArc(RectF(cx - ir * 0.48f, cy - ir * 0.45f, cx + ir * 0.48f, cy + ir * 0.45f), 210f, 120f, false, p)
                c.drawLine(cx - ir * 0.22f, cy + ir * 0.40f, cx + ir * 0.22f, cy + ir * 0.40f, p)
            }
            "news" -> {
                c.drawRect(cx - ir * 0.46f, cy - ir * 0.43f, cx + ir * 0.46f, cy + ir * 0.43f, p)
                for (j in -1..1) c.drawLine(cx - ir * 0.28f, cy + j * ir * 0.18f, cx + ir * 0.28f, cy + j * ir * 0.18f, p)
            }
            "knowledge" -> {
                c.drawRect(cx - ir * 0.48f, cy - ir * 0.43f, cx, cy + ir * 0.43f, p)
                c.drawRect(cx, cy - ir * 0.43f, cx + ir * 0.48f, cy + ir * 0.43f, p)
            }
            else -> sigil(c, cx, cy, ir * 0.70f, m.color)
        }
        text(c, m.title, cx, r.top + r.height() * 0.72f, S(11.5f), white, Typeface.DEFAULT, 0.01f, true)
        text(c, m.sub, cx, r.top + r.height() * 0.87f, S(7.8f), m.color, Typeface.DEFAULT, 0.02f, true)
        p.color = m.color
        p.alpha = (130 + 110 * q).toInt()
        p.strokeWidth = S(1f)
        c.drawLine(cx - S(35f), r.bottom - S(13f), cx + S(35f), r.bottom - S(13f), p)
        diamond(c, cx, r.bottom - S(13f), S(3.5f), m.color)
    }

    private fun status(c: Canvas, wide: Boolean, time: Double) {
        val q = (0.5 + 0.5 * sin(time * 0.85)).toFloat()
        val top = if (wide) 715f else 800f
        val bottom = if (wide) 795f else 886f
        val left = if (wide) 130f else 20f
        val right = if (wide) 990f else 700f
        val r = RectF(X(left), Y(top), X(right), Y(bottom))
        p.style = Paint.Style.FILL
        p.color = Color.rgb(3, 6, 8)
        p.alpha = 250
        c.drawRoundRect(r, S(10f), S(10f), p)
        p.style = Paint.Style.STROKE
        p.color = green
        p.alpha = (120 + 110 * q).toInt()
        p.strokeWidth = S(1.1f)
        c.drawRoundRect(r, S(10f), S(10f), p)
        val ex = if (wide) 190f else 72f
        val tx = if (wide) 235f else 112f
        val lx = if (wide) 630f else 416f
        val sx = if (wide) 585f else 378f
        val dx = if (wide) 955f else 675f
        miniEye(c, X(ex), Y(top + 42f), S(24f), green)
        textLeft(c, "ORACLE READY", X(tx), Y(top + 37f), S(14f), white, Typeface.DEFAULT_BOLD)
        textLeft(c, "Market Intelligence Active", X(tx), Y(top + 61f), S(9f), green, Typeface.DEFAULT)
        p.color = gold
        p.alpha = (150 + 90 * q).toInt()
        p.strokeWidth = S(0.9f)
        c.drawCircle(X(sx), Y(top + 42f), S(24f + 3f * q), p)
        for (i in -2..2) {
            val xx = X(sx + i * 6f)
            val half = S(9f + kotlin.math.abs(i) * 2f)
            c.drawLine(xx, Y(top + 42f) - half, xx, Y(top + 42f) + half, p)
        }
        textLeft(c, "LOCAL INTELLIGENCE", X(lx), Y(top + 37f), S(13f), white, Typeface.DEFAULT_BOLD)
        textLeft(c, "Synced & Protected", X(lx), Y(top + 61f), S(9f), green, Typeface.DEFAULT)
        p.style = Paint.Style.FILL
        p.color = green
        p.alpha = 255
        c.drawCircle(X(dx), Y(top + 42f), S(7f + 3f * q), p)
    }

    private fun footer(c: Canvas, cx: Float, base: Float) {
        text(c, "ORACLE", cx, Y(base - 60f), S(20f), gold, Typeface.SERIF, 0.34f, true)
        text(c, "SEE MORE.  KNOW FIRST.", cx, Y(base - 35f), S(8f), muted, Typeface.DEFAULT, 0.22f, true)
        p.style = Paint.Style.STROKE
        p.color = gold
        p.alpha = 145
        p.strokeWidth = S(0.6f)
        c.drawLine(cx - S(75f), Y(base - 15f), cx + S(75f), Y(base - 15f), p)
        diamond(c, cx, Y(base - 15f), S(3f), gold)
        text(c, "357AT2026", cx, Y(base + 27f), S(11f), brightGold, Typeface.DEFAULT_BOLD, 0.16f, true)
    }

    private fun miniEye(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.alpha = 240
        p.strokeWidth = r * 0.10f
        path.reset()
        path.moveTo(x - r, y)
        path.cubicTo(x - r * 0.55f, y - r * 0.55f, x + r * 0.55f, y - r * 0.55f, x + r, y)
        path.cubicTo(x + r * 0.55f, y + r * 0.55f, x - r * 0.55f, y + r * 0.55f, x - r, y)
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        c.drawCircle(x, y, r * 0.22f, p)
    }

    private fun sigil(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.alpha = 245
        p.strokeWidth = r * 0.065f
        path.reset()
        path.moveTo(x - r * 0.75f, y)
        path.lineTo(x, y - r * 0.5f)
        path.lineTo(x + r * 0.75f, y)
        path.lineTo(x, y + r * 0.5f)
        path.close()
        c.drawPath(path, p)
        c.drawCircle(x, y, r * 0.18f, p)
    }

    private fun diamond(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.color = color
        p.alpha = 230
        p.strokeWidth = maxOf(S(0.6f), r * 0.32f)
        path.reset()
        path.moveTo(x, y - r)
        path.lineTo(x + r, y)
        path.lineTo(x, y + r)
        path.lineTo(x - r, y)
        path.close()
        c.drawPath(path, p)
    }

    private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, face: Typeface, spacing: Float, centered: Boolean) {
        p.style = Paint.Style.FILL
        p.color = color
        p.alpha = 255
        p.typeface = face
        p.textSize = size
        p.textAlign = if (centered) Paint.Align.CENTER else Paint.Align.LEFT
        p.letterSpacing = spacing
        c.drawText(value, x, y, p)
        p.letterSpacing = 0f
    }

    private fun textLeft(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, face: Typeface) {
        text(c, value, x, y, size, color, face, 0f, false)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) {
            for ((r, key) in hit) if (r.contains(e.x, e.y)) {
                onModule(key)
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
