package ro.alintudor.oracle

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Premium Start/Home shell. Navigation only; Oracle modules remain untouched. */
class PremiumStartView(context: Context, private val onOpen: (String) -> Unit) : ScrollView(context) {
    private val density = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()
    private fun bg(color: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); stroke?.let { setStroke(dp(1), it) }
    }
    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(context).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    init {
        isFillViewport = true
        setBackgroundColor(Color.rgb(1, 3, 8))
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(30))
        }

        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(tv("ORACLE", 30f, Color.WHITE, true))
        brand.addView(tv("AI STOCK INTELLIGENCE", 10f, Color.rgb(120, 145, 180), true))
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(tv("●  LIVE", 11f, Color.rgb(50, 220, 135), true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(82), dp(36)))
        page.addView(top)
        page.addView(tv("Your market command center", 14f, Color.rgb(160, 170, 190)).apply { setPadding(0, dp(4), 0, dp(16)) })

        val pulse = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16)); background = bg(Color.rgb(7, 14, 28), 18, Color.rgb(35, 60, 95))
        }
        pulse.addView(tv("MARKET PULSE", 11f, Color.rgb(255, 210, 60), true))
        pulse.addView(tv("Oracle is ready", 22f, Color.WHITE, true).apply { setPadding(0, dp(5), 0, dp(2)) })
        pulse.addView(tv("Your command center for analysis, opportunities and portfolio decisions.", 12f, Color.rgb(150, 165, 190)))
        page.addView(pulse, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })

        page.addView(tv("QUICK ACCESS", 11f, Color.rgb(125, 150, 190), true).apply { setPadding(dp(4), dp(3), 0, dp(9)) })
        val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val rows = listOf(
            listOf(Triple("analysis", "ANALYSIS", "Deep stock analysis"), Triple("watchlist", "WATCHLIST", "Your saved tickers")),
            listOf(Triple("growth", "GROWTH", "Return & contribution"), Triple("portfolio", "PORTFOLIO", "Positions & P/L")),
            listOf(Triple("news", "NEWS", "Catalysts & events"), Triple("alerts", "ALERTS", "Active signals"))
        )
        rows.forEach { pair ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { index, item ->
                row.addView(card(item.first, item.second, item.third), LinearLayout.LayoutParams(0, dp(106), 1f).apply { if (index == 0) setMargins(0, 0, dp(7), dp(8)) else setMargins(0, 0, 0, dp(8)) })
            }
            grid.addView(row)
        }
        page.addView(grid)

        val bottom = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(13), dp(14), dp(13)); background = bg(Color.rgb(7, 11, 21), 15) }
        bottom.addView(tv("ORACLE READY", 12f, Color.rgb(50, 220, 135), true), LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(tv("B514 • STABLE BASE", 10f, Color.rgb(120, 135, 160), true))
        page.addView(bottom, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(8), 0, 0) })
        addView(page, LayoutParams(-1, -2))
    }

    private fun card(key: String, title: String, subtitle: String): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(12), dp(10), dp(10)); background = bg(Color.rgb(8, 13, 25), 16, Color.rgb(32, 52, 82))
        isClickable = true; isFocusable = true; elevation = dp(2).toFloat()
        setOnClickListener { onOpen(key) }
        addView(tv(title, 16f, Color.WHITE, true))
        addView(tv(subtitle, 11f, Color.rgb(145, 160, 185)).apply { setPadding(0, dp(4), 0, 0) })
        addView(tv("OPEN  ›", 10f, Color.rgb(105, 160, 235), true).apply { setPadding(0, dp(9), 0, 0) })
    }
}
