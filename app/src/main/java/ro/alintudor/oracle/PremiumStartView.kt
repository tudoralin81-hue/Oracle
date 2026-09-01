package ro.alintudor.oracle

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Oracle Start surface. UI-only: module implementations are not touched.
 * Navigation keys intentionally match the existing module contract.
 */
class PremiumStartView(context: Context, private val onModule: (String) -> Unit) : View(context) {
    private data class Card(val key: String, val title: String, val sub: String, val glyph: String)

    private val primary = listOf(
        Card("portfolio", "PORTFOLIO", "Your capital at a glance", "P"),
        Card("growth", "GROWTH", "Performance & trajectory", "G"),
        Card("analysis", "ANALYSIS", "Oracle decision engine", "A")
    )
    private val secondary = listOf(
        Card("watchlist", "WATCHLIST", "Track the next move", "W"),
        Card("alerts", "ALERTS", "Signals requiring attention", "!"),
        Card("news", "NEWS", "Catalysts & market events", "N"),
        Card("knowledge", "KNOWLEDGE", "Oracle intelligence base", "K"),
        Card("journal", "JOURNAL", "Activity & decisions", "J")
    )

    private val bg = Color.rgb(2, 5, 11)
    private val panel = Color.rgb(7, 12, 22)
    private val panel2 = Color.rgb(10, 17, 30)
    private val panel3 = Color.rgb(12, 22, 39)
    private val cyan = Color.rgb(54, 211, 255)
    private val cyanDim = Color.rgb(34, 103, 141)
    private val gold = Color.rgb(255, 204, 61)
    private val white = Color.rgb(246, 249, 255)
    private val muted = Color.rgb(135, 151, 177)
    private val line = Color.rgb(31, 50, 77)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hitAreas = ArrayList<Pair<RectF, String>>()
    private var pressed: String? = null
    private var started = SystemClock.uptimeMillis()

    init { setBackgroundColor(bg); isFocusable = true }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, w, h, bg, Color.rgb(5, 11, 23), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint); paint.shader = null
        drawAmbient(c, w, h)
        drawHeader(c, w)
        drawHero(c, w)
        drawPrimary(c, w, h)
        drawSecondary(c, w, h)
        drawFooter(c, w, h)
        drawScan(c, w, h)
        postInvalidateDelayed(45L)
    }

    private fun drawAmbient(c: Canvas, w: Float, h: Float) {
        val t = (SystemClock.uptimeMillis() - started) / 1100.0
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(20, 54, 211, 255)
        c.drawCircle(w * .82f, h * .15f, dp(115f) + dp(10f) * sin(t).toFloat(), paint)
        paint.color = Color.argb(12, 255, 204, 61)
        c.drawCircle(w * .12f, h * .82f, dp(90f), paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f; paint.color = Color.argb(18, 74, 132, 183)
        val step = dp(32f)
        var x = 0f; while (x <= w) { c.drawLine(x, 0f, x, h, paint); x += step }
        var y = 0f; while (y <= h) { c.drawLine(0f, y, w, y, paint); y += step }
        paint.style = Paint.Style.FILL
    }

    private fun drawHeader(c: Canvas, w: Float) {
        val l = dp(22f)
        text(c, "ORACLE", l, dp(42f), 27f, white, true, .10f)
        text(c, "INTELLIGENCE TERMINAL", l, dp(62f), 9f, cyan, true, .18f)
        paint.color = gold; c.drawRect(l, dp(76f), l + dp(44f), dp(78f), paint)
        paint.color = Color.rgb(27, 42, 64); c.drawRect(l + dp(52f), dp(77f), w - l, dp(78f), paint)
        val pulse = ((sin((SystemClock.uptimeMillis() - started) / 360.0) + 1.0) * .5)
        paint.color = Color.argb((110 + pulse * 110).toInt(), 54, 211, 255); c.drawCircle(w - dp(35f), dp(40f), dp(4f), paint)
        text(c, "LIVE", w - dp(65f), dp(44f), 8f, muted, true, .10f)
    }

    private fun drawHero(c: Canvas, w: Float) {
        val l = dp(18f); val r = w - l; val top = dp(94f); val bottom = dp(202f)
        val box = RectF(l, top, r, bottom)
        rounded(c, box, panel, 20f); stroke(c, box, line, 1f, 20f)
        text(c, "COMMAND CENTER", l + dp(15f), top + dp(23f), 8f, muted, true, .16f)
        text(c, "SEE THE MARKET.", l + dp(15f), top + dp(55f), 23f, white, true, .01f)
        text(c, "THINK AHEAD.", l + dp(15f), top + dp(81f), 23f, cyan, true, .01f)
        text(c, "One starting point for your entire Oracle workflow.", l + dp(16f), top + dp(99f), 9f, muted, false, 0f)
        drawOrb(c, r - dp(67f), top + dp(52f))
        text(c, "SYSTEM", r - dp(112f), top + dp(91f), 7f, muted, true, .12f)
        text(c, "READY", r - dp(112f), top + dp(105f), 10f, white, true, .08f)
    }

    private fun drawOrb(c: Canvas, x: Float, y: Float) {
        val t = (SystemClock.uptimeMillis() - started) / 1000.0
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1f); paint.color = Color.argb(80, 54, 211, 255)
        c.drawCircle(x, y, dp(33f), paint); c.drawCircle(x, y, dp(24f), paint)
        paint.color = gold; paint.strokeWidth = dp(2f)
        val a = (t * 65.0).toFloat(); c.drawArc(RectF(x-dp(33f),y-dp(33f),x+dp(33f),y+dp(33f)),a,72f,false,paint)
        paint.color = cyan; paint.strokeWidth = dp(2.5f); c.drawArc(RectF(x-dp(24f),y-dp(24f),x+dp(24f),y+dp(24f)),-a,115f,false,paint)
        paint.style = Paint.Style.FILL; paint.color = cyan; c.drawCircle(x, y, dp(5f), paint)
    }

    private fun drawPrimary(c: Canvas, w: Float, h: Float) {
        hitAreas.clear()
        val l = dp(18f); val gap = dp(9f); val top = dp(216f); val cardW = (w - l * 2f - gap * 2f) / 3f
        val cardH = dp(105f)
        primary.forEachIndexed { i, card ->
            val rect = RectF(l + i * (cardW + gap), top, l + i * (cardW + gap) + cardW, top + cardH)
            drawPrimaryCard(c, rect, card, i == 2)
            hitAreas.add(rect to card.key)
        }
    }

    private fun drawPrimaryCard(c: Canvas, r: RectF, card: Card, accent: Boolean) {
        val active = pressed == card.key
        rounded(c, r, if (active) Color.rgb(13, 31, 50) else panel3, 17f)
        stroke(c, r, if (active) cyan else if (accent) Color.rgb(57, 91, 127) else line, if (active) 2f else 1f, 17f)
        paint.color = if (accent) gold else cyan; c.drawRect(r.left, r.top, r.left + dp(3f), r.bottom, paint)
        val cx = r.left + dp(26f); val cy = r.top + dp(28f)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1.5f); paint.color = if (accent) gold else cyanDim; c.drawCircle(cx, cy, dp(13f), paint)
        paint.style = Paint.Style.FILL; textCentered(c, card.glyph, cx, cy + dp(4f), 11f, white, true)
        text(c, card.title, r.left + dp(15f), r.top + dp(59f), 13f, white, true, .06f)
        text(c, card.sub, r.left + dp(15f), r.top + dp(78f), 8f, muted, false, 0f)
        text(c, "OPEN  ›", r.right - dp(52f), r.top + dp(96f), 7f, if (active) cyan else muted, true, .05f)
    }

    private fun drawSecondary(c: Canvas, w: Float, h: Float) {
        val l = dp(18f); val gap = dp(8f); val top = dp(334f); val cols = 2; val cardW = (w - l * 2f - gap) / 2f
        val bottom = h - dp(54f); val available = bottom - top; val rows = 3; val cardH = min(dp(70f), (available - gap * 2f) / rows)
        secondary.forEachIndexed { i, card ->
            val row = i / cols; val col = i % cols
            val rr = RectF(l + col * (cardW + gap), top + row * (cardH + gap), l + col * (cardW + gap) + cardW, top + row * (cardH + gap) + cardH)
            if (i == secondary.lastIndex) {
                rr.left = l; rr.right = w - l
            }
            drawSecondaryCard(c, rr, card); hitAreas.add(rr to card.key)
        }
        text(c, "QUICK ACCESS", l, top - dp(10f), 8f, muted, true, .15f)
    }

    private fun drawSecondaryCard(c: Canvas, r: RectF, card: Card) {
        val active = pressed == card.key
        rounded(c, r, if (active) Color.rgb(11, 27, 45) else panel, 14f)
        stroke(c, r, if (active) cyan else line, if (active) 1.7f else 1f, 14f)
        val cy = r.centerY(); paint.color = if (card.key == "alerts") gold else cyanDim
        c.drawCircle(r.left + dp(22f), cy, dp(9f), paint)
        textCentered(c, card.glyph, r.left + dp(22f), cy + dp(3.5f), 8f, bg, true)
        text(c, card.title, r.left + dp(40f), cy - dp(2f), 10f, white, true, .06f)
        text(c, card.sub, r.left + dp(40f), cy + dp(14f), 7f, muted, false, 0f)
        text(c, "›", r.right - dp(18f), cy + dp(4f), 17f, if (active) cyan else muted, false, 0f)
    }

    private fun drawFooter(c: Canvas, w: Float, h: Float) {
        val y = h - dp(22f)
        text(c, "ORACLE  •  PRECISION OVER NOISE", dp(19f), y, 7f, muted, true, .12f)
        text(c, "B514", w - dp(47f), y, 8f, gold, true, .08f)
    }

    private fun drawScan(c: Canvas, w: Float, h: Float) {
        val elapsed = SystemClock.uptimeMillis() - started
        val x = ((elapsed % 5200L) / 5200f) * (w + dp(120f)) - dp(60f)
        paint.color = Color.argb(24, 54, 211, 255); c.drawRect(x, 0f, x + dp(2f), h, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { pressed = hit(e.x, e.y); invalidate(); return true }
            MotionEvent.ACTION_UP -> { val k = hit(e.x, e.y); val p = pressed; pressed = null; invalidate(); if (k != null && k == p) { performClick(); onModule(k) }; return true }
            MotionEvent.ACTION_CANCEL -> { pressed = null; invalidate(); return true }
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
    private fun hit(x: Float, y: Float): String? = hitAreas.firstOrNull { it.first.contains(x, y) }?.second

    private fun rounded(c: Canvas, r: RectF, color: Int, radius: Float) { paint.style = Paint.Style.FILL; paint.color = color; c.drawRoundRect(r, dp(radius), dp(radius), paint) }
    private fun stroke(c: Canvas, r: RectF, color: Int, width: Float, radius: Float) { paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(width); paint.color = color; c.drawRoundRect(r, dp(radius), dp(radius), paint); paint.style = Paint.Style.FILL }
    private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean, spacing: Float) { paint.style = Paint.Style.FILL; paint.color = color; paint.textSize = dp(size); paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT; paint.letterSpacing = spacing; c.drawText(value, x, y, paint) }
    private fun textCentered(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean) { paint.textSize = dp(size); paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT; paint.color = color; c.drawText(value, x - paint.measureText(value) / 2f, y, paint) }
    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
