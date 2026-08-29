package ro.alintudor.oracle.nativeui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

/** Shared Oracle visual shell: same visual language as the start map. */
class OracleNativeModule(
    private val context: Context,
    private val title: String,
    private val onRefresh: () -> Unit = {}
) {
    val root: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.rgb(1, 3, 8))
        setPadding(dp(12), dp(12), dp(12), dp(12))
    }
    val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(10), 0, dp(18))
    }

    init {
        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(8))
        }
        header.addView(action("⌂", "Home", Color.rgb(255, 205, 45)) { (context as? android.app.Activity)?.onBackPressed() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val center = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        center.addView(TextView(context).apply { text = "ORACLE"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        center.addView(TextView(context).apply { text = title; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = .12f; setTextColor(Color.rgb(150,160,180)); gravity = Gravity.CENTER })
        header.addView(center, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(action("↻", "Refresh", Color.rgb(80, 210, 255)) { onRefresh() }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)

        val rule = View(context).apply { setBackgroundColor(Color.rgb(255, 205, 45)) }
        root.addView(rule, LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(dp(4), 0, dp(4), 0) })
        root.addView(ScrollView(context).apply { isFillViewport = true; addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun action(symbol: String, description: String, accent: Int, click: () -> Unit): TextView = TextView(context).apply {
        text = symbol
        textSize = 23f
        gravity = Gravity.CENTER
        contentDescription = description
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(8, 12, 24), dp(14), accent, dp(1))
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
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(Color.rgb(8, 12, 24), dp(16), Color.rgb(45, 55, 78), dp(1))
        }
        card.addView(TextView(context).apply {
            text = heading.uppercase()
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .05f
            setTextColor(Color.WHITE)
        })
        card.addView(TextView(context).apply {
            text = body
            textSize = 14f
            setTextColor(Color.rgb(175, 182, 198))
            setPadding(0, dp(8), 0, 0)
        })
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, dp(10))
        content.addView(card, lp)
    }

    fun addSectionLabel(text: String, accent: Int = Color.rgb(255, 205, 45)) {
        content.addView(TextView(context).apply {
            this.text = text.uppercase()
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .14f
            setTextColor(accent)
            setPadding(dp(5), dp(8), dp(5), dp(6))
        })
    }

    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    companion object {
        fun rounded(fill: Int, radius: Int, stroke: Int = Color.TRANSPARENT, strokeWidth: Int = 0) = GradientDrawable().apply {
            setColor(fill); cornerRadius = radius.toFloat()
            if (strokeWidth > 0) setStroke(strokeWidth, stroke)
        }
    }
}
