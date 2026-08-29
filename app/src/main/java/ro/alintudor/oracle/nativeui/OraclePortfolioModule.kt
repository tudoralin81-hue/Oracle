package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.OraclePosition
import java.util.Locale

/** Rich native portfolio dashboard. Calculations remain owned by the existing core snapshot. */
class OraclePortfolioModule(private val host: OracleNativeModule) {
    fun render(positions: List<OraclePosition>) {
        host.content.removeAllViews()
        host.addCard("PORTFOLIO", "Poziții, valoare, P/L, ponderi și risc")
        if (positions.isEmpty()) { host.addCard("AȘTEPT DATE", "Nu există încă date de portofoliu în cache."); return }

        val value = positions.sumOf { it.marketValue }
        val invested = positions.sumOf { it.shares * it.avgCost }
        val pnl = positions.sumOf { it.pnl }
        val pnlPct = if (invested == 0.0) 0.0 else pnl / invested * 100.0
        val winners = positions.count { it.pnl > 0 }
        val losers = positions.count { it.pnl < 0 }
        val concentration = positions.maxOf { it.weight }
        val risk = when { concentration >= 50.0 -> "HIGH"; concentration >= 35.0 -> "MEDIUM"; else -> "CONTROLAT" }

        addHero(value, pnl, pnlPct, positions.size)
        addMetricRow("CÂȘTIGĂTOARE", winners.toString(), "PIERZĂTOARE", losers.toString())
        addMetricRow("CONCENTRARE MAX.", pct(concentration), "RISC", risk)
        positions.sortedByDescending { it.marketValue }.forEachIndexed { i, p -> addPosition(i + 1, p) }
    }

    private fun addHero(value: Double, pnl: Double, pnlPct: Double, count: Int) {
        val card = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(18), host.dp(16), host.dp(18), host.dp(16))
            background = GradientDrawable().apply { setColor(Color.rgb(7, 11, 22)); cornerRadius = host.dp(16).toFloat(); setStroke(host.dp(1), Color.rgb(92, 72, 28)) }
        }
        val top = LinearLayout(host.root.context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(host.root.context).apply { text = "◔"; textSize = 34f; setTextColor(Color.rgb(255, 210, 55)); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(host.dp(48), host.dp(48)))
        val mid = LinearLayout(host.root.context).apply { orientation = LinearLayout.VERTICAL; setPadding(host.dp(12), 0, 0, 0) }
        mid.addView(TextView(host.root.context).apply { text = "TOTAL PORTOFOLIU • $count ACȚIUNI"; textSize = 11f; setTextColor(Color.rgb(155, 166, 188)); letterSpacing = .04f })
        mid.addView(TextView(host.root.context).apply { text = money(value); textSize = 23f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0, host.dp(3), 0, 0) })
        top.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(host.root.context).apply { text = "${if (pnlPct >= 0) "+" else ""}${pct(pnlPct)}"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (pnl >= 0) Color.rgb(145,245,35) else Color.rgb(255,80,65)); gravity = Gravity.CENTER })
        card.addView(top)
        card.addView(TextView(host.root.context).apply { text = "P/L  ${money(pnl)}"; textSize = 13f; setTextColor(Color.rgb(175,183,201)); setPadding(host.dp(60), host.dp(6), 0, 0) })
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(10)) })
    }

    private fun addMetricRow(a:String,b:String,c:String,d:String) {
        val row = LinearLayout(host.root.context).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(metric(a,b), LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(0,0,host.dp(5),host.dp(8)) })
        row.addView(metric(c,d), LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(host.dp(5),0,0,host.dp(8)) })
        host.content.addView(row)
    }

    private fun metric(label:String,value:String) = LinearLayout(host.root.context).apply {
        orientation = LinearLayout.VERTICAL; setPadding(host.dp(14),host.dp(12),host.dp(14),host.dp(12))
        background = GradientDrawable().apply { setColor(Color.rgb(7,11,22)); cornerRadius=host.dp(12).toFloat(); setStroke(host.dp(1),Color.rgb(35,44,66)) }
        addView(TextView(host.root.context).apply { text=label; textSize=10f; setTextColor(Color.rgb(145,155,176)) })
        addView(TextView(host.root.context).apply { text=value; textSize=16f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setPadding(0,host.dp(3),0,0) })
    }

    private fun addPosition(rank:Int,p:OraclePosition) {
        val positive = p.pnl >= 0
        val accent = if (positive) Color.rgb(145,245,35) else Color.rgb(255,80,65)
        val signal = when { p.pnlPercent <= -10.0 -> "WATCH / RISK"; p.pnlPercent >= 15.0 -> "PROFIT"; else -> "HOLD" }
        val card = LinearLayout(host.root.context).apply { orientation=LinearLayout.VERTICAL; setPadding(host.dp(15),host.dp(13),host.dp(12),host.dp(13)); background=GradientDrawable().apply{setColor(Color.rgb(6,10,20));cornerRadius=host.dp(15).toFloat();setStroke(host.dp(1),Color.rgb(42,52,76))} }
        val top=LinearLayout(host.root.context).apply{gravity=Gravity.CENTER_VERTICAL}
        top.addView(TextView(host.root.context).apply{text="%02d".format(rank);textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(34),host.dp(30)))
        top.addView(TextView(host.root.context).apply{text=p.ticker;textSize=20f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f))
        top.addView(TextView(host.root.context).apply{text=signal;textSize=10f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent);gravity=Gravity.CENTER},LinearLayout.LayoutParams(host.dp(82),host.dp(30)))
        card.addView(top)
        card.addView(TextView(host.root.context).apply{text="${money(p.marketValue)} ${p.currency}   •   ${pct(p.weight)} PONDERE";textSize=13f;setTextColor(Color.rgb(175,183,201));setPadding(host.dp(34),host.dp(5),0,0)})
        val line=LinearLayout(host.root.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(host.dp(34),host.dp(9),0,0)}
        line.addView(metricInline("PREȚ",String.format(Locale.US,"%.2f",p.currentPrice)),LinearLayout.LayoutParams(0,-2,1f))
        line.addView(metricInline("AVG",String.format(Locale.US,"%.2f",p.avgCost)),LinearLayout.LayoutParams(0,-2,1f))
        line.addView(metricInline("P/L","${pct(p.pnlPercent)}"),LinearLayout.LayoutParams(0,-2,1f))
        card.addView(line)
        host.content.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,host.dp(9))})
    }
    private fun metricInline(label:String,value:String)=TextView(host.root.context).apply{text="$label\n$value";textSize=11f;setTextColor(Color.WHITE);setPadding(0,0,4,0)}
    private fun money(v:Double)=String.format(Locale.US,"%,.2f",v)
    private fun pct(v:Double)=String.format(Locale.US,"%.2f%%",v)
}
