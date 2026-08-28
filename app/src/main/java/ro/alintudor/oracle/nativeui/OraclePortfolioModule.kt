package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import ro.alintudor.oracle.core.OraclePosition

class OraclePortfolioModule(private val host: OracleNativeModule) {
    fun render(positions: List<OraclePosition>) {
        host.content.removeAllViews()
        host.addCard("PORTFOLIO", "Poziții, valoare, P&L și ponderi")
        if (positions.isEmpty()) { host.addCard("Aștept date", "Nu există încă date de portofoliu în cache."); return }
        positions.forEach { p ->
            val card = TextView(host.root.context).apply {
                text = "${p.ticker}   ${"%.2f".format(p.currentPrice)} ${p.currency}\n${"%.2f".format(p.shares)} acțiuni  •  Avg ${"%.2f".format(p.avgCost)}\nP&L ${"%.2f".format(p.pnl)} (${"%.2f".format(p.pnlPercent)}%)  •  ${"%.1f".format(p.weight)}%"
                textSize=16f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER_VERTICAL; setPadding(18,18,18,18); setBackgroundColor(Color.rgb(9,13,26))
            }
            host.content.addView(card, android.widget.LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,10) })
        }
    }
}
