package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OracleNews

/** Rich native financial news feed. Existing news data is rendered without changing its source. */
class OracleNewsModule(private val host: OracleNativeModule) {
    fun render(news: List<OracleNews>) {
        host.content.removeAllViews()
        host.addCard("NEWS", "Știri economice, catalizatori și breaking news")
        if (news.isEmpty()) {
            host.addCard("AȘTEPT ȘTIRI", "Nu există încă știri în cache.")
            return
        }
        val breaking = news.count { it.breaking }
        addSummary(news.size, breaking)
        news.forEachIndexed { i, n -> addNews(i + 1, n) }
    }

    private fun addSummary(total: Int, breaking: Int) {
        val card = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(host.dp(15), host.dp(12), host.dp(15), host.dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(7, 11, 22))
                cornerRadius = host.dp(13).toFloat()
                setStroke(host.dp(1), Color.rgb(35, 44, 66))
            }
        }
        card.addView(
            TextView(host.root.context).apply {
                text = "◉"
                textSize = 25f
                setTextColor(Color.rgb(25, 205, 255))
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(host.dp(42), host.dp(42))
        )
        card.addView(
            TextView(host.root.context).apply {
                text = "TOTAL NEWS\n$total articole"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(host.dp(10), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        card.addView(
            TextView(host.root.context).apply {
                text = "BREAKING\n$breaking"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (breaking > 0) Color.rgb(255, 75, 60) else Color.rgb(145, 155, 176))
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(host.dp(80), -2)
        )
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, host.dp(10))
        })
    }

    private fun addNews(rank: Int, n: OracleNews) {
        val accent = if (n.breaking) Color.rgb(255, 75, 60) else Color.rgb(25, 205, 255)
        val card = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(15), host.dp(13), host.dp(14), host.dp(13))
            background = GradientDrawable().apply {
                setColor(Color.rgb(6, 10, 20))
                cornerRadius = host.dp(14).toFloat()
                setStroke(host.dp(1), accent)
            }
        }
        val top = LinearLayout(host.root.context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(
            TextView(host.root.context).apply {
                text = "%02d".format(rank)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accent)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(host.dp(32), host.dp(25))
        )
        top.addView(
            TextView(host.root.context).apply {
                text = n.ticker.ifBlank { "MARKET" }
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        top.addView(
            TextView(host.root.context).apply {
                text = if (n.breaking) "BREAKING" else "NEWS"
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accent)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(host.dp(68), host.dp(25))
        )
        card.addView(top)
        card.addView(TextView(host.root.context).apply {
            text = n.title
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(host.dp(32), host.dp(5), 0, 0)
        })
        card.addView(TextView(host.root.context).apply {
            text = n.source
            textSize = 11f
            setTextColor(Color.rgb(145, 155, 176))
            setPadding(host.dp(32), host.dp(5), 0, 0)
        })
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 0, 0, host.dp(9))
        })
    }
}
