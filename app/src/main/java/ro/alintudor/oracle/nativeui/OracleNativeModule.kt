package ro.alintudor.oracle.nativeui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

/** Shared Oracle premium visual shell. Every module intentionally echoes the Oracle start-map language. */
class OracleNativeModule(
    private val context: Context,
    private val title: String,
    private val onRefresh: () -> Unit = {}
) {
    val root: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(1, 3, 8))
        setPadding(dp(10), dp(8), dp(10), 0)
    }
    val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(2), dp(8), dp(2), dp(28))
    }

    private val accent: Int = when (title.uppercase()) {
        "ALERTS" -> Color.rgb(255, 75, 40)
        "NEWS", "ANALYSIS" -> Color.rgb(25, 205, 255)
        "GROWTH" -> Color.rgb(145, 245, 35)
        "PORTFOLIO" -> Color.rgb(190, 65, 255)
        "KNOWLEDGE", "WATCHLIST", "JURNAL ACTIVITATE" -> Color.rgb(255, 210, 45)
        else -> Color.rgb(255, 205, 45)
    }

    init {
        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), dp(8))
        }
        header.addView(action("⌂", "Home", Color.rgb(255, 205, 45)) { (context as? android.app.Activity)?.onBackPressed() }, LinearLayout.LayoutParams(dp(46), dp(46)))

        val center = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        center.addView(TextView(context).apply {
            text = "ORACLE"
            textSize = 21f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        center.addView(TextView(context).apply {
            text = title
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .18f
            setTextColor(accent)
            gravity = Gravity.CENTER
        })
        header.addView(center, LinearLayout.LayoutParams(0, dp(46), 1f))
        header.addView(action("↻", "Refresh", Color.rgb(80, 210, 255)) { onRefresh() }, LinearLayout.LayoutParams(dp(46), dp(46)))
        root.addView(header)

        root.addView(ModuleBanner(context, title, accent), LinearLayout.LayoutParams(-1, dp(118)).apply {
            setMargins(dp(2), 0, dp(2), dp(6))
        })

        val rule = View(context).apply { setBackgroundColor(accent) }
        root.addView(rule, LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(dp(6), 0, dp(6), 0) })
        root.addView(ScrollView(context).apply {
            isFillViewport = false
            addView(content)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun action(symbol: String, description: String, buttonAccent: Int, click: () -> Unit): TextView = TextView(context).apply {
        text = symbol
        textSize = 22f
        gravity = Gravity.CENTER
        contentDescription = description
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(7, 10, 20), dp(13), buttonAccent, dp(1))
        isClickable = true
        isFocusable = true
        setOnClickListener { click() }
    }

    fun render() {
        content.removeAllViews()
        addCard(title, "Modul Oracle nativ")
    }

    fun addCard(heading: String, body: String) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(15))
            background = rounded(Color.rgb(7, 11, 22), dp(15), Color.rgb(42, 52, 76), dp(1))
            elevation = dp(2).toFloat()
        }
        val accentLine = View(context).apply { setBackgroundColor(accent) }
        card.addView(accentLine, LinearLayout.LayoutParams(dp(42), dp(3)).apply { setMargins(0, 0, 0, dp(10)) })
        card.addView(TextView(context).apply {
            text = heading.uppercase()
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .04f
            setTextColor(Color.WHITE)
        })
        card.addView(TextView(context).apply {
            text = body
            textSize = 14f
            setTextColor(Color.rgb(175, 182, 198))
            setPadding(0, dp(7), 0, 0)
        })
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, dp(10))
        content.addView(card, lp)
    }

    fun addSectionLabel(text: String, sectionAccent: Int = accent) {
        content.addView(TextView(context).apply {
            this.text = text.uppercase()
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .14f
            setTextColor(sectionAccent)
            setPadding(dp(5), dp(8), dp(5), dp(7))
        })
    }

    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    companion object {
        fun rounded(fill: Int, radius: Int, stroke: Int = Color.TRANSPARENT, strokeWidth: Int = 0) = GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (strokeWidth > 0) setStroke(strokeWidth, stroke)
        }
    }
}

private class ModuleBanner(context: Context, private val title: String, private val accent: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * .5f
        val cy = h * .54f
        val r = minOf(w, h) * .30f

        c.drawColor(Color.rgb(2, 4, 10))
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        paint.color = Color.argb(105, Color.red(accent), Color.green(accent), Color.blue(accent))
        for (i in 1..3) c.drawCircle(cx, cy, r * (1.0f + i * .48f), paint)

        paint.strokeWidth = 3f * density
        paint.color = accent
        c.drawCircle(cx, cy, r, paint)
        paint.strokeWidth = 1f * density
        paint.color = Color.argb(150, 255, 205, 45)
        c.drawCircle(cx, cy, r * 1.16f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(5, 8, 17)
        c.drawCircle(cx, cy, r * .88f, paint)
        paint.color = accent
        c.drawCircle(cx, cy - r * .82f, r * .055f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.WHITE
        paint.textSize = r * .34f
        c.drawText(title, cx, cy + r * .04f, paint)
        paint.textSize = r * .13f
        paint.color = Color.rgb(185, 192, 210)
        c.drawText("ORACLE INTELLIGENCE", cx, cy + r * .34f, paint)
        paint.textSize = r * .32f
        paint.color = accent
        c.drawText("›", cx, cy + r * .69f, paint)
    }
}
