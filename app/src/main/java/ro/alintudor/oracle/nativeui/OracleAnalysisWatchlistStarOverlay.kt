package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OracleWatchlistStore
import java.util.Locale

/**
 * Adds the Analysis-only favorite star without changing the existing Analysis cards.
 * The existing Watchlist store remains the single source of persistence.
 */
object OracleAnalysisWatchlistStarOverlay {
    private val tickerPattern = Regex("^[A-Z][A-Z0-9.-]{0,7}$")
    private val ignored = setOf(
        "ANALYSIS", "PORTFOLIO", "ACTIONS", "SIGNAL", "SUMMARY", "KNOWLEDGE",
        "GROWTH", "ALERTS", "NEWS", "WATCHLIST", "JURNAL", "ORACLE"
    )

    fun install(host: OracleNativeModule) {
        fun scan() {
            scanContent(host)
        }
        host.content.setOnHierarchyChangeListener(object : android.view.ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                child?.post { scan() }
            }
            override fun onChildViewRemoved(parent: View?, child: View?) = Unit
        })
        host.content.post { scan() }
    }

    private fun scanContent(host: OracleNativeModule) {
        for (i in 0 until host.content.childCount) {
            val card = host.content.getChildAt(i) as? LinearLayout ?: continue
            val row = card.getChildAt(0) as? LinearLayout ?: continue
            if (row.getTag(TAG_STAR) == true) continue

            val tickerView = row.getChildAt(0) as? TextView ?: continue
            val ticker = tickerView.text?.toString()?.trim()?.uppercase(Locale.US) ?: continue
            if (ticker.isBlank() || ticker in ignored || !tickerPattern.matches(ticker)) continue
            if (!looksLikeTickerRow(row)) continue

            val store = OracleWatchlistStore(host.root.context)
            var selected = store.load().any { it.equals(ticker, true) }
            val star = TextView(host.root.context).apply {
                textSize = 30f
                gravity = android.view.Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isClickable = true
                isFocusable = true
                setPadding(0, 0, 0, 0)
                contentDescription = if (selected) "Scoate $ticker din Watchlist" else "Adaugă $ticker în Watchlist"
                fun paint() {
                    text = if (selected) "★" else "☆"
                    setTextColor(if (selected) Color.rgb(255, 210, 45) else Color.rgb(125, 135, 155))
                }
                paint()
                setOnClickListener {
                    val current = store.load().toMutableList()
                    val present = current.any { it.equals(ticker, true) }
                    if (present) {
                        current.removeAll { it.equals(ticker, true) }
                        selected = false
                    } else {
                        current.add(ticker)
                        selected = true
                    }
                    store.save(current)
                    contentDescription = if (selected) "Scoate $ticker din Watchlist" else "Adaugă $ticker în Watchlist"
                    paint()
                }
            }
            row.addView(star, 1.coerceAtMost(row.childCount), LinearLayout.LayoutParams(host.dp(48), host.dp(42)))
            row.setTag(TAG_STAR, true)
        }
    }

    private fun looksLikeTickerRow(row: LinearLayout): Boolean {
        if (row.childCount < 2) return false
        val second = row.getChildAt(1)
        val secondText = (second as? TextView)?.text?.toString()?.trim().orEmpty()
        if (secondText == "›" || secondText == "→") return true
        if (secondText.contains("USD", true)) return true
        return row.isClickable && row.childCount <= 3
    }

    private const val TAG_STAR = 0x0A57A001
}
