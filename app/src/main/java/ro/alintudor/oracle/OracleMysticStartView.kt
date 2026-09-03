package ro.alintudor.oracle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Native Start B515: vector-only, responsive, seven-module composition.
 * B541: premium pass — layered background gradient + vignette, an ambient
 * halo behind the eye sigil, a shimmering title, and glass-style module
 * cards (gradient fill, soft outer glow, top sheen). Layout, hit-testing
 * and every existing animation timing are unchanged.
 */
class OracleMysticStartView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val hit = mutableListOf<Pair<RectF, String>>()
    private var sx = 1f; private var sy = 1f; private var ox = 0f; private var oy = 0f
    private val gold = Color.rgb(255, 205, 55); private val white = Color.rgb(245, 241, 231); private val green = Color.rgb(60, 255, 85)
    private val modules = listOf(
        M("portfolio", "PORTFOLIO", "OVERVIEW", Color.rgb(220, 55, 255)),
        M("watchlist", "WATCHLIST", "TRACK & FOCUS", Color.rgb(255, 205, 35)),
        M("analysis", "ANALYSIS", "CHARTS & TOOLS", Color.rgb(20, 220, 255)),
        M("growth", "GROWTH", "FUTURE SCAN", Color.rgb(120, 255, 45)),
        M("alerts", "ALERTS", "STAY AHEAD", Color.rgb(255, 65, 45)),
        M("news", "NEWS", "MARKET PULSE", Color.rgb(25, 205, 255)),
        M("knowledge", "KNOWLEDGE", "LEARN & EVOLVE", Color.rgb(255, 210, 45))
    )
    private data class M(val key: String, val title: String, val sub: String, val color: Int)
    private fun X(v: Float) = ox + v * sx; private fun Y(v: Float) = oy + v * sy; private fun S(v: Float) = v * min(sx, sy)

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val wide = w / h > 1.18f
        val dw = if (wide) 1280f else 720f; val dh = if (wide) 800f else 1120f
        sx = w / dw; sy = h / dh; ox = 0f; oy = 0f

        background(c, w, h)
        val time = System.nanoTime() / 1_000_000_000.0
        val cx = X(dw * .5f); val eyeY = Y(if (wide) 185f else 255f); val eyeR = S(if (wide) 135f else 126f)

        stars(c, w, h, time)
        halo(c, cx, eyeY, eyeR * 1.65f, gold, time)
        grid(c, cx, eyeY, S(if (wide) 118f else 112f), S(18f))
        sigil(c, cx, Y(if (wide) 31f else 54f), S(20f), gold)
        titleGlow(c, "ORACLE", cx, Y(if (wide) 72f else 100f), S(if (wide) 34f else 31f), time)
        text(c, "STOCK INTELLIGENCE", cx, Y(if (wide) 99f else 127f), S(9f), gold, Typeface.DEFAULT, .25f, true)
        eye(c, cx, eyeY, eyeR, time)
        text(c, "SEE MORE.  KNOW FIRST.", cx, Y(if (wide) 330f else 430f), S(10.5f), white, Typeface.DEFAULT, .25f, true)
        line(c, X(if (wide) 385f else 220f), Y(if (wide) 348f else 449f), X(if (wide) 895f else 500f), Y(if (wide) 348f else 449f), gold, 125, .7f)
        diamond(c, cx, Y(if (wide) 348f else 449f), S(4f), gold)

        hit.clear()
        if (wide) drawCards(c, 145f, 385f, 225f, 110f, 18f, time, true)
        else drawCards(c, 18f, 475f, 162f, 118f, 10f, time, false)

        text(c, "357AT2026", cx, Y(if (wide) 775f else 1090f), S(10f), gold, Typeface.DEFAULT_BOLD, .18f, true)
        vignette(c, w, h)
        postInvalidateDelayed(32L)
    }

    /** Rich vertical gradient instead of a flat fill — cheap, adds depth. */
    private fun background(c: Canvas, w: Float, h: Float) {
        p.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(6, 8, 16), Color.rgb(1, 1, 3), Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL; p.alpha = 255
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    /** Subtle cinematic edge-darkening drawn last, on top of everything. */
    private fun vignette(c: Canvas, w: Float, h: Float) {
        val cx = w * .5f; val cy = h * .46f
        val radius = max(w, h) * .82f
        p.shader = RadialGradient(cx, cy, radius, Color.argb(0, 0, 0, 0), Color.argb(95, 0, 0, 0), Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    /** Soft ambient glow behind the eye sigil; breathes slowly with time. */
    private fun halo(c: Canvas, x: Float, y: Float, r: Float, color: Int, time: Double) {
        val q = (.5 + .5 * sin(time * .55)).toFloat()
        val alpha = (34 + 20 * q).toInt()
        p.shader = RadialGradient(x, y, r, Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)), Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP)
        p.style = Paint.Style.FILL
        c.drawCircle(x, y, r, p)
        p.shader = null
    }

    /** ORACLE title: a soft halo pass, then a slow shimmer band sweeps the fill. */
    private fun titleGlow(c: Canvas, s: String, x: Float, y: Float, size: Float, time: Double) {
        p.style = Paint.Style.STROKE; p.strokeWidth = S(2.4f)
        p.color = gold; p.alpha = 60
        p.textSize = size; p.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER; p.letterSpacing = .18f
        c.drawText(s, x, y, p)

        val sweep = ((time * .16) % 1.0).toFloat()
        val band = size * 6f
        val start = x - band * 1.5f + sweep * band * 3f
        p.style = Paint.Style.FILL; p.alpha = 255
        p.shader = LinearGradient(
            start, y - size, start + band, y + size * .25f,
            intArrayOf(gold, Color.rgb(255, 246, 214), gold),
            floatArrayOf(0f, .5f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawText(s, x, y, p)
        p.shader = null
    }

    private fun stars(c: Canvas, w: Float, h: Float, time: Double) { p.style = Paint.Style.FILL; for (i in 0 until 70) { val x = ((i * 83 + 41) % 1000) / 1000f * w; val y = ((i * 149 + 17) % 1000) / 1000f * h; val q = (.5 + .5 * sin(time * .7 + i)).toFloat(); p.color = Color.argb((35 + 75 * q).toInt(), 210, 190, 80); c.drawCircle(x, y, S(.65f + (i % 3) * .35f), p) } }
    private fun grid(c: Canvas, cx: Float, cy: Float, first: Float, step: Float) { p.style = Paint.Style.STROKE; p.strokeWidth = S(.55f); p.color = Color.argb(48, 205, 175, 65); for (i in 0 until 14) c.drawCircle(cx, cy, first + i * step, p); for (i in 0 until 32) { val a = i * Math.PI / 16.0; val dx = cos(a).toFloat(); val dy = sin(a).toFloat(); c.drawLine(cx + dx * (first - S(16f)), cy + dy * (first - S(16f)), cx + dx * (first + S(255f)), cy + dy * (first + S(255f)), p) } }
    private fun eye(c: Canvas, x: Float, y: Float, r: Float, time: Double) { val q = (.5 + .5 * sin(time * 1.25)).toFloat(); p.style = Paint.Style.STROKE; path.reset(); path.moveTo(x - r, y); path.cubicTo(x - r * .58f, y - r * .55f, x + r * .58f, y - r * .55f, x + r, y); path.cubicTo(x + r * .58f, y + r * .55f, x - r * .58f, y + r * .55f, x - r, y); p.color = gold; p.alpha = (180 + 70 * q).toInt(); p.strokeWidth = S(2f); c.drawPath(path, p); p.color = green; p.alpha = (55 + 90 * q).toInt(); p.strokeWidth = S(1.2f); c.drawCircle(x, y, r * (.48f + .035f * q), p); p.alpha = (160 + 90 * q).toInt(); p.strokeWidth = S(2f); c.drawCircle(x, y, r * .29f, p); p.style = Paint.Style.FILL; p.color = Color.rgb(2, 10, 4); p.alpha = 255; c.drawCircle(x, y, r * .275f, p); p.color = green; p.alpha = (165 + 90 * q).toInt(); c.drawCircle(x, y, r * (.09f + .035f * q), p); p.color = Color.argb((30 + 80 * q).toInt(), 60, 255, 85); c.drawCircle(x, y, r * (.15f + .05f * q), p); p.style = Paint.Style.STROKE; p.color = Color.rgb(255, 105, 35); p.alpha = (70 + 90 * q).toInt(); p.strokeWidth = S(.8f); for (i in 0 until 28) { val a = i * Math.PI / 14.0; val inn = r * .40f; val out = r * (.56f + .05f * q); c.drawLine(x + cos(a).toFloat() * inn, y + sin(a).toFloat() * inn, x + cos(a).toFloat() * out, y + sin(a).toFloat() * out, p) } }
    private fun drawCards(c: Canvas, left: Float, top: Float, cw: Float, ch: Float, gap: Float, time: Double, wide: Boolean) { for (i in modules.indices) { val col: Int; val row: Int; if (i < 4) { col = i; row = 0 } else { col = i - 4; row = 1 }; val count = if (row == 0) 4 else 3; val rowW = count * cw + (count - 1) * gap; val rowLeft = if (row == 0) left else left + (4 * cw + 3 * gap - rowW) / 2f; val l = rowLeft + col * (cw + gap); val t = top + row * (ch + gap); val r = RectF(X(l), Y(t), X(l + cw), Y(t + ch)); hit += r to modules[i].key; card(c, r, modules[i], time, i, wide) } }

    /** Glass-style module card: soft accent glow, gradient fill, thin sheen highlight. */
    private fun card(c: Canvas, r: RectF, m: M, time: Double, index: Int, wide: Boolean) {
        val q = (.5 + .5 * sin(time * 1.1 + index * .53)).toFloat()
        val cx = r.centerX(); val cy = r.top + r.height() * .39f; val rr = min(r.width(), r.height()) * .255f

        p.style = Paint.Style.FILL; p.color = m.color; p.alpha = (16 + 14 * q).toInt()
        val glow = RectF(r.left - S(3f), r.top - S(3f), r.right + S(3f), r.bottom + S(3f))
        c.drawRoundRect(glow, S(12f), S(12f), p)

        p.shader = LinearGradient(r.left, r.top, r.left, r.bottom, Color.rgb(15, 19, 32), Color.rgb(2, 3, 6), Shader.TileMode.CLAMP)
        p.alpha = 255
        c.drawRoundRect(r, S(10f), S(10f), p)
        p.shader = null

        p.style = Paint.Style.STROKE; p.color = m.color; p.alpha = (155 + 95 * q).toInt(); p.strokeWidth = S(1.15f)
        c.drawRoundRect(r, S(10f), S(10f), p)

        p.color = Color.argb(55, 255, 255, 255); p.strokeWidth = S(1f)
        c.drawLine(r.left + S(10f), r.top + S(6f), r.right - S(10f), r.top + S(6f), p)

        p.color = m.color
        p.alpha = (35 + 95 * q).toInt(); p.strokeWidth = S(1f); c.drawCircle(cx, cy, rr * (1.16f + .06f * q), p)
        p.alpha = (100 + 130 * q).toInt(); c.drawCircle(cx, cy, rr, p)
        p.alpha = 90; c.drawCircle(cx, cy, rr * .78f, p)
        p.alpha = 255; p.strokeWidth = S(1.8f)
        when (m.key) {
            "watchlist" -> miniEye(c, cx, cy, rr * .72f, m.color)
            "portfolio" -> { c.drawRect(cx - rr * .5f, cy - rr * .38f, cx + rr * .5f, cy + rr * .38f, p); c.drawCircle(cx + rr * .22f, cy + rr * .17f, rr * .16f, p) }
            "analysis" -> { path.reset(); path.moveTo(cx - rr * .58f, cy + rr * .35f); path.lineTo(cx - rr * .2f, cy); path.lineTo(cx, cy + rr * .12f); path.lineTo(cx + rr * .56f, cy - rr * .5f); c.drawPath(path, p) }
            "growth" -> { path.reset(); path.moveTo(cx - rr * .6f, cy + rr * .34f); path.lineTo(cx - rr * .2f, cy + .05f); path.lineTo(cx + rr * .04f, cy + .18f); path.lineTo(cx + rr * .58f, cy - rr * .5f); c.drawPath(path, p) }
            "alerts" -> { c.drawArc(RectF(cx - rr * .46f, cy - rr * .48f, cx + rr * .46f, cy + rr * .42f), 210f, 120f, false, p); c.drawLine(cx - rr * .2f, cy + rr * .42f, cx + rr * .2f, cy + rr * .42f, p) }
            "news" -> { c.drawRect(cx - rr * .46f, cy - rr * .44f, cx + rr * .46f, cy + rr * .44f, p); for (j in -1..1) c.drawLine(cx - rr * .28f, cy + j * rr * .19f, cx + rr * .28f, cy + j * rr * .19f, p) }
            "knowledge" -> { c.drawRect(cx - rr * .48f, cy - rr * .44f, cx, cy + rr * .44f, p); c.drawRect(cx, cy - rr * .44f, cx + rr * .48f, cy + rr * .44f, p) }
        }
        text(c, m.title, cx, r.top + r.height() * .73f, S(if (wide) 11.5f else 11f), white, Typeface.DEFAULT, .01f, true)
        text(c, m.sub, cx, r.top + r.height() * .88f, S(if (wide) 7.7f else 7.2f), m.color, Typeface.DEFAULT, .02f, true)
        p.color = m.color; p.alpha = (130 + 115 * q).toInt(); p.strokeWidth = S(1f)
        c.drawLine(cx - S(32f), r.bottom - S(12f), cx + S(32f), r.bottom - S(12f), p)
        diamond(c, cx, r.bottom - S(12f), S(3.3f), m.color)
    }

    private fun miniEye(c: Canvas, x: Float, y: Float, r: Float, color: Int) { p.style = Paint.Style.STROKE; p.color = color; p.alpha = 235; p.strokeWidth = S(1.5f); path.reset(); path.moveTo(x - r, y); path.cubicTo(x - r * .55f, y - r * .48f, x + r * .55f, y - r * .48f, x + r, y); path.cubicTo(x + r * .55f, y + r * .48f, x - r * .55f, y + r * .48f, x - r, y); c.drawPath(path, p); p.style = Paint.Style.FILL; c.drawCircle(x, y, r * .16f, p) }
    private fun sigil(c: Canvas, x: Float, y: Float, r: Float, color: Int) { p.style = Paint.Style.STROKE; p.color = color; p.alpha = 220; p.strokeWidth = S(1.2f); c.drawCircle(x, y, r * .45f, p); c.drawCircle(x, y, r * .14f, p); c.drawLine(x, y - r * .45f, x, y - r * .8f, p); c.drawLine(x - r * .65f, y, x - r * .25f, y, p); c.drawLine(x + r * .25f, y, x + r * .65f, y, p) }
    private fun diamond(c: Canvas, x: Float, y: Float, r: Float, color: Int) { p.style = Paint.Style.STROKE; p.color = color; p.alpha = 220; p.strokeWidth = S(.8f); path.reset(); path.moveTo(x, y - r); path.lineTo(x + r, y); path.lineTo(x, y + r); path.lineTo(x - r, y); path.close(); c.drawPath(path, p) }
    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, alpha: Int, width: Float) { p.style = Paint.Style.STROKE; p.color = color; p.alpha = alpha; p.strokeWidth = S(width); c.drawLine(x1, y1, x2, y2, p) }
    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface, spacing: Float, bold: Boolean) { p.style = Paint.Style.FILL; p.color = color; p.alpha = 255; p.textSize = size; p.typeface = if (bold) Typeface.create(typeface, Typeface.BOLD) else typeface; p.textAlign = Paint.Align.CENTER; p.letterSpacing = spacing; c.drawText(s, x, y, p) }
    override fun onTouchEvent(e: MotionEvent): Boolean { if (e.actionMasked == MotionEvent.ACTION_UP) { for ((r, key) in hit) if (r.contains(e.x, e.y)) { performClick(); onModule(key); return true } }; return true }
    override fun performClick(): Boolean { super.performClick(); return true }
}
