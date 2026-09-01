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

/** B514 native Start screen. No image asset is used. Protected modules are untouched. */
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

    private data class Module(
        val key: String,
        val title: String,
        val subtitle: String,
        val color: Int
    )

    private val modules = listOf(
        Module("portfolio", "PORTFOLIO", "OVERVIEW", Color.rgb(205, 65, 255)),
        Module("watchlist", "WATCHLIST", "TRACK & FOCUS", Color.rgb(250, 202, 55)),
        Module("analysis", "ANALYSIS", "CHARTS & TOOLS", Color.rgb(35, 215, 255)),
        Module("growth", "GROWTH", "FUTURE SCAN", Color.rgb(120, 248, 50)),
        Module("alerts", "ALERTS", "STAY AHEAD", Color.rgb(255, 68, 45)),
        Module("news", "NEWS", "MARKET PULSE", Color.rgb(38, 212, 255)),
        Module("knowledge", "KNOWLEDGE", "LEARN & EVOLVE", Color.rgb(248, 202, 55)),
        Module("stock", "STOCK", "INTELLIGENCE", Color.rgb(202, 68, 255))
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val widthPx = width.toFloat()
        val heightPx = height.toFloat()
        val scale = min(widthPx / 720f, heightPx / 1180f).coerceAtLeast(0.42f)
        val offsetX = (widthPx - 720f * scale) / 2f

        fun sx(v: Float): Float = offsetX + v * scale
        fun sy(v: Float): Float = v * scale
        fun ss(v: Float): Float = v * scale

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(2, 3, 5)
        paint.alpha = 255
        canvas.drawRect(0f, 0f, widthPx, heightPx, paint)

        val centerX = sx(360f)
        val eyeY = sy(286f)

        // Fine mystical geometry, deliberately thin and rectangular UI around it.
        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 55
        paint.strokeWidth = ss(0.55f)
        for (i in 0 until 12) {
            canvas.drawCircle(centerX, eyeY, ss(116f + i * 18f), paint)
        }
        for (i in 0 until 24) {
            val angle = i * Math.PI / 12.0
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            canvas.drawLine(
                centerX + c * ss(96f), eyeY + s * ss(96f),
                centerX + c * ss(330f), eyeY + s * ss(330f), paint
            )
        }

        drawTopButton(canvas, sx(58f), sy(54f), ss(29f), false)
        drawTopButton(canvas, sx(662f), sy(54f), ss(29f), true)

        drawCenteredText(canvas, "STOCK INTELLIGENCE", centerX, sy(23f), ss(9f), gold, Typeface.DEFAULT_BOLD, 0.08f)
        drawSigil(canvas, centerX, sy(82f), ss(30f))
        drawCenteredText(canvas, "ORACLE", centerX, sy(138f), ss(31f), brightGold, Typeface.SERIF, 0.17f)
        drawCenteredText(canvas, "STOCK INTELLIGENCE", centerX, sy(163f), ss(9.5f), gold, Typeface.DEFAULT, 0.25f)

        drawEye(canvas, centerX, eyeY, ss(142f), brightGold)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.alpha = 255
        canvas.drawCircle(centerX, eyeY, ss(51f), paint)
        paint.style = Paint.Style.STROKE
        paint.color = brightGold
        paint.alpha = 230
        paint.strokeWidth = ss(2f)
        canvas.drawCircle(centerX, eyeY, ss(57f), paint)
        paint.strokeWidth = ss(0.7f)
        paint.alpha = 125
        for (i in 0 until 24) {
            val angle = i * Math.PI / 12.0
            canvas.drawLine(
                centerX + cos(angle).toFloat() * ss(65f), eyeY + sin(angle).toFloat() * ss(65f),
                centerX + cos(angle).toFloat() * ss(145f), eyeY + sin(angle).toFloat() * ss(145f), paint
            )
        }

        drawCenteredText(canvas, "SEE MORE.  KNOW FIRST.", centerX, sy(500f), ss(10.5f), white, Typeface.DEFAULT, 0.27f)
        paint.color = gold
        paint.alpha = 190
        paint.strokeWidth = ss(0.7f)
        canvas.drawLine(sx(220f), sy(522f), sx(500f), sy(522f), paint)
        drawDiamond(canvas, centerX, sy(522f), ss(4f), brightGold)

        hits.clear()
        val cardLeft = 20f
        val cardTop = 546f
        val cardWidth = 164f
        val cardHeight = 132f
        val gap = 12f

        for (index in modules.indices) {
            val column = index % 4
            val row = index / 4
            val left = sx(cardLeft + column * (cardWidth + gap))
            val top = sy(cardTop + row * (cardHeight + gap))
            val rect = RectF(left, top, left + ss(cardWidth), top + ss(cardHeight))
            hits += rect to modules[index].key
            drawModuleCard(canvas, rect, modules[index], scale)
        }

        drawStatusPanel(canvas, ::sx, ::sy, ::ss)

        drawCenteredText(canvas, "ORACLE", centerX, sy(960f), ss(20f), gold, Typeface.SERIF, 0.34f)
        drawCenteredText(canvas, "SEE MORE.  KNOW FIRST.", centerX, sy(986f), ss(8f), muted, Typeface.DEFAULT, 0.22f)
        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 150
        paint.strokeWidth = ss(0.6f)
        canvas.drawLine(sx(285f), sy(1007f), sx(435f), sy(1007f), paint)
        drawDiamond(canvas, centerX, sy(1007f), ss(3f), gold)
        drawCenteredText(canvas, "357AT2026", centerX, sy(1050f), ss(11f), brightGold, Typeface.DEFAULT_BOLD, 0.16f)
    }

    private fun drawStatusPanel(
        canvas: Canvas,
        sx: (Float) -> Float,
        sy: (Float) -> Float,
        ss: (Float) -> Float
    ) {
        val panel = RectF(sx(20f), sy(834f), sx(700f), sy(922f))
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(5, 6, 8)
        paint.alpha = 245
        canvas.drawRoundRect(panel, ss(9f), ss(9f), paint)
        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 180
        paint.strokeWidth = ss(0.8f)
        canvas.drawRoundRect(panel, ss(9f), ss(9f), paint)

        drawMiniEye(canvas, sx(72f), sy(878f), ss(24f), Color.rgb(105, 235, 88))
        drawLeftText(canvas, "ORACLE READY", sx(112f), sy(872f), ss(14f), white, Typeface.DEFAULT_BOLD)
        drawLeftText(canvas, "Market Intelligence Active", sx(112f), sy(895f), ss(9f), Color.rgb(70, 218, 105), Typeface.DEFAULT)

        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 190
        paint.strokeWidth = ss(0.9f)
        canvas.drawCircle(sx(378f), sy(878f), ss(24f), paint)
        for (i in -2..2) {
            val x = sx(378f + i * 6f)
            val half = sy(10f + kotlin.math.abs(i) * 2f)
            canvas.drawLine(x, sy(878f) - half, x, sy(878f) + half, paint)
        }
        drawLeftText(canvas, "LOCAL INTELLIGENCE", sx(416f), sy(872f), ss(13f), white, Typeface.DEFAULT_BOLD)
        drawLeftText(canvas, "Synced & Protected", sx(416f), sy(895f), ss(9f), Color.rgb(130, 200, 125), Typeface.DEFAULT)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(65, 228, 95)
        paint.alpha = 255
        canvas.drawCircle(sx(675f), sy(880f), ss(8f), paint)
    }

    private fun drawTopButton(canvas: Canvas, x: Float, y: Float, radius: Float, gear: Boolean) {
        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 220
        paint.strokeWidth = radius * 0.035f
        canvas.drawRoundRect(RectF(x - radius, y - radius, x + radius, y + radius), radius * 0.2f, radius * 0.2f, paint)
        paint.strokeWidth = radius * 0.075f
        if (!gear) {
            for (i in -1..1) {
                canvas.drawLine(x - radius * 0.35f, y + i * radius * 0.22f, x + radius * 0.35f, y + i * radius * 0.22f, paint)
            }
        } else {
            canvas.drawCircle(x, y, radius * 0.25f, paint)
            canvas.drawCircle(x, y, radius * 0.42f, paint)
            for (i in 0 until 8) {
                val angle = i * Math.PI / 4.0
                canvas.drawLine(
                    x + cos(angle).toFloat() * radius * 0.45f,
                    y + sin(angle).toFloat() * radius * 0.45f,
                    x + cos(angle).toFloat() * radius * 0.58f,
                    y + sin(angle).toFloat() * radius * 0.58f,
                    paint
                )
            }
        }
    }

    private fun drawSigil(canvas: Canvas, x: Float, y: Float, radius: Float) {
        paint.style = Paint.Style.STROKE
        paint.color = brightGold
        paint.alpha = 240
        paint.strokeWidth = radius * 0.06f
        path.reset()
        path.moveTo(x - radius * 0.75f, y)
        path.lineTo(x, y - radius * 0.5f)
        path.lineTo(x + radius * 0.75f, y)
        path.lineTo(x, y + radius * 0.5f)
        path.close()
        canvas.drawPath(path, paint)
        canvas.drawCircle(x, y, radius * 0.18f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, radius * 0.055f, paint)
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.alpha = 235
        paint.strokeWidth = radius * 0.018f
        path.reset()
        path.moveTo(x - radius, y)
        path.cubicTo(x - radius * 0.58f, y - radius * 0.56f, x + radius * 0.58f, y - radius * 0.56f, x + radius, y)
        path.cubicTo(x + radius * 0.58f, y + radius * 0.56f, x - radius * 0.58f, y + radius * 0.56f, x - radius, y)
        canvas.drawPath(path, paint)
    }

    private fun drawModuleCard(canvas: Canvas, rect: RectF, module: Module, scale: Float) {
        fun ss(v: Float): Float = v * scale
        val x = rect.centerX()
        val y = rect.top + rect.height() * 0.37f
        val iconRadius = min(rect.width(), rect.height()) * 0.25f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(3, 4, 6)
        paint.alpha = 247
        canvas.drawRoundRect(rect, ss(8f), ss(8f), paint)

        paint.style = Paint.Style.STROKE
        paint.color = gold
        paint.alpha = 205
        paint.strokeWidth = ss(0.8f)
        canvas.drawRoundRect(rect, ss(8f), ss(8f), paint)

        paint.color = module.color
        paint.alpha = 245
        paint.strokeWidth = ss(1f)
        canvas.drawCircle(x, y, iconRadius, paint)
        canvas.drawCircle(x, y, iconRadius * 0.78f, paint)
        canvas.drawCircle(x, y, iconRadius * 0.58f, paint)

        when (module.key) {
            "watchlist" -> drawEye(canvas, x, y, iconRadius * 0.78f, module.color)
            "portfolio" -> canvas.drawRect(x - iconRadius * 0.45f, y - iconRadius * 0.38f, x + iconRadius * 0.45f, y + iconRadius * 0.38f, paint)
            "analysis", "growth" -> {
                path.reset()
                path.moveTo(x - iconRadius * 0.6f, y + iconRadius * 0.35f)
                path.lineTo(x - iconRadius * 0.18f, y)
                path.lineTo(x + iconRadius * 0.05f, y + iconRadius * 0.16f)
                path.lineTo(x + iconRadius * 0.58f, y - iconRadius * 0.48f)
                canvas.drawPath(path, paint)
            }
            "alerts" -> canvas.drawArc(RectF(x - iconRadius * 0.45f, y - iconRadius * 0.43f, x + iconRadius * 0.45f, y + iconRadius * 0.43f), 210f, 120f, false, paint)
            "news" -> {
                canvas.drawRect(x - iconRadius * 0.46f, y - iconRadius * 0.43f, x + iconRadius * 0.46f, y + iconRadius * 0.43f, paint)
                for (i in -1..1) canvas.drawLine(x - iconRadius * 0.27f, y + i * iconRadius * 0.18f, x + iconRadius * 0.27f, y + i * iconRadius * 0.18f, paint)
            }
            "knowledge" -> {
                canvas.drawRect(x - iconRadius * 0.48f, y - iconRadius * 0.43f, x, y + iconRadius * 0.43f, paint)
                canvas.drawRect(x, y - iconRadius * 0.43f, x + iconRadius * 0.48f, y + iconRadius * 0.43f, paint)
            }
            else -> drawSigil(canvas, x, y, iconRadius * 0.75f)
        }

        drawCenteredText(canvas, module.title, x, rect.top + rect.height() * 0.71f, ss(11.5f), white, Typeface.DEFAULT, 0.01f)
        drawCenteredText(canvas, module.subtitle, x, rect.top + rect.height() * 0.84f, ss(7.5f), muted, Typeface.DEFAULT, 0.03f)
        paint.color = module.color
        paint.alpha = 220
        paint.strokeWidth = ss(0.6f)
        canvas.drawLine(x - ss(20f), rect.bottom - ss(11f), x + ss(20f), rect.bottom - ss(11f), paint)
        drawDiamond(canvas, x, rect.bottom - ss(11f), ss(2.4f), module.color)
    }

    private fun drawMiniEye(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        drawEye(canvas, x, y, radius, color)
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = radius * 0.07f
        canvas.drawCircle(x, y, radius * 0.24f, paint)
    }

    private fun drawDiamond(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.alpha = 220
        paint.strokeWidth = 0.7f * resources.displayMetrics.density
        path.reset()
        path.moveTo(x, y - radius)
        path.lineTo(x + radius, y)
        path.lineTo(x, y + radius)
        path.lineTo(x - radius, y)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawCenteredText(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface, spacing: Float) {
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = color
        paint.textSize = size
        paint.typeface = typeface
        paint.textAlign = Paint.Align.CENTER
        paint.letterSpacing = spacing
        canvas.drawText(value, x, y, paint)
    }

    private fun drawLeftText(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface) {
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = color
        paint.textSize = size
        paint.typeface = typeface
        paint.textAlign = Paint.Align.LEFT
        paint.letterSpacing = 0f
        canvas.drawText(value, x, y, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            for ((rect, key) in hits) {
                if (rect.contains(event.x, event.y)) {
                    onModule(key)
                    return true
                }
            }
        }
        return true
    }
}
