package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OraclePosition
import java.util.Locale

class OraclePortfolioModule(private val host: OracleNativeModule) {
    fun render(positions: List<OraclePosition>) {
        host.content.removeAllViews()
        host.addCard("PORTFOLIO", "Poziții, valoare, P&L, ponderi și risc")
        if (positions.isEmpty()) { host.addCard("Aștept date", "Nu există încă date de portofoliu în cache."); return }

        val value = positions.sumOf { it.marketValue }
        val invested = positions.sumOf { it.shares * it.avgCost }
        val pnl = positions.sumOf { it.pnl }
        val pnlPct = if (invested == 0.0) 0.0 else pnl / invested * 100.0
        val winners = positions.count { it.pnl > 0 }
        val losers = positions.count { it.pnl < 0 }
        val concentration = positions.maxOf { it.weight }
        val risk = when {
            concentration >= 50.0 -> "HIGH"
            concentration >= 35.0 -> "MEDIUM"
            else -> "CONTROLAT"
        }

        addMetricRow("Valoare portofoliu", money(value), "P&L", money(pnl) + "  (" + pct(pnlPct) + ")")
        addMetricRow("Câștigătoare", winners.toString(), "Pierzătoare", losers.toString())
        addMetricRow("Concentrare maximă", pct(concentration), "Risc", risk)

        positions.sortedByDescending { it.marketValue }.forEach { p -> addPosition(p) }
    }

    private fun addMetricRow(a:String,b:String,c:String,d:String) {
        val row = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4,4,4,12) }
        row.addView(metric(a,b), LinearLayout.LayoutParams(0,-2,1f))
        row.addView(metric(c,d), LinearLayout.LayoutParams(0,-2,1f))
        host.content.addView(row)
    }

    private fun metric(label:String,value:String) = TextView(host.root.context).apply {
        text = "$label\n$value"; textSize=15f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; setPadding(14,12,14,12); setBackgroundColor(Color.rgb(9,13,26))
    }

    private fun addPosition(p: OraclePosition) {
        val signal = when {
            p.pnlPercent <= -10.0 -> "WATCH / RISK"
            p.pnlPercent >= 15.0 -> "PROFIT"
            else -> "HOLD"
        }
        val card = TextView(host.root.context).apply {
            text = "${p.ticker}  •  $signal\n${"%.2f".format(Locale.US,p.currentPrice)} ${p.currency}  •  ${"%.2f".format(Locale.US,p.shares)} acțiuni\nAvg ${"%.2f".format(Locale.US,p.avgCost)}  •  P&L ${"%.2f".format(Locale.US,p.pnl)} (${pct(p.pnlPercent)})\nValoare ${money(p.marketValue)}  •  Pondere ${pct(p.weight)}"
            textSize=16f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER_VERTICAL; setPadding(18,18,18,18); setBackgroundColor(Color.rgb(9,13,26))
        }
        host.content.addView(card, LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,10) })
    }

    private fun money(v:Double) = String.format(Locale.US,"%,.2f",v)
    private fun pct(v:Double) = String.format(Locale.US,"%.2f%%",v)
}
