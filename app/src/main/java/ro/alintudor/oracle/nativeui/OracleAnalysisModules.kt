package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import ro.alintudor.oracle.core.*
import kotlin.math.abs

/** Native presentation for all Oracle modules. All calculations stay local. */
class OracleSimpleModule(private val host: OracleNativeModule, private val moduleTitle: String) {
    fun render(actions: List<OracleAction> = emptyList(), knowledge: List<OracleKnowledgeItem> = emptyList(), positions: List<OraclePosition> = emptyList(), history: List<OracleHistoryPoint> = emptyList()) {
        host.content.removeAllViews()
        val normalized = OracleAnalytics.normalize(positions)
        val computedActions = OracleAnalytics.actions(normalized, history)
        when (moduleTitle) {
            "GROWTH" -> renderGrowth(normalized, history)
            "ANALYSIS" -> renderAnalysis(normalized, history)
            "WATCHLIST" -> renderWatchlist(normalized, history)
            "KNOWLEDGE" -> renderKnowledge(knowledge)
            "ACTIONS" -> renderActions(if (computedActions.isNotEmpty()) computedActions else actions)
            else -> renderActions(if (computedActions.isNotEmpty()) computedActions else actions)
        }
    }

    private fun renderGrowth(positions: List<OraclePosition>, history: List<OracleHistoryPoint>) {
        host.addCard("GROWTH", "Randament, trend local și contribuție la portofoliu")
        if (positions.isEmpty()) return emptyState()
        val trends = OracleAnalytics.trends(history).associateBy { it.ticker }
        positions.sortedByDescending { it.pnlPercent }.forEachIndexed { i, p ->
            val t = trends[p.ticker]
            addItem("${i + 1}. ${p.ticker}", "P/L ${fmt(p.pnlPercent)}%  •  Trend ${t?.direction ?: "N/A"} ${t?.let { "(${fmt(it.changePct)}%)" } ?: ""}\nValoare ${fmtMoney(p.marketValue)}  •  Pondere ${fmt(p.weight)}%")
        }
    }

    private fun renderAnalysis(positions: List<OraclePosition>, history: List<OracleHistoryPoint>) {
        host.addCard("ANALYSIS", "Snapshot calculat local de Oracle")
        if (positions.isEmpty()) return emptyState()
        val s = OracleAnalytics.summary(positions)
        host.addCard("PORTFOLIO", "Valoare ${fmtMoney(s.value)}\nP/L ${fmtMoney(s.pnl)}  •  ${fmt(s.pnlPct)}%\nCâștigătoare ${s.winners}  •  Pierzătoare ${s.losers}\nConcentrare maximă ${fmt(s.concentration)}%  •  Risc ${s.riskLabel}")
        val trends = OracleAnalytics.trends(history).associateBy { it.ticker }
        positions.sortedByDescending { abs(it.pnlPercent) }.take(5).forEach { p ->
            val t = trends[p.ticker]
            addItem(p.ticker, "P/L ${fmtMoney(p.pnl)}  •  ${fmt(p.pnlPercent)}%  •  pondere ${fmt(p.weight)}%\nTrend ${t?.direction ?: "N/A"}")
        }
    }

    private fun renderWatchlist(positions: List<OraclePosition>, history: List<OracleHistoryPoint>) {
        host.addCard("WATCHLIST", "Poziții urmărite • status și trend local")
        if (positions.isEmpty()) return emptyState()
        val trends = OracleAnalytics.trends(history).associateBy { it.ticker }
        positions.sortedBy { it.ticker }.forEach { p ->
            val t = trends[p.ticker]
            addItem(p.ticker, "${p.status}  •  Preț ${fmtMoney(p.currentPrice)}  •  Pondere ${fmt(p.weight)}%\nTrend ${t?.direction ?: "N/A"}  •  ${t?.let { fmt(it.changePct) + "%" } ?: "fără istoric suficient"}")
        }
    }

    private fun renderKnowledge(items: List<OracleKnowledgeItem>) {
        host.addCard("KNOWLEDGE", "Biblioteca Oracle — conținut local")
        if (items.isEmpty()) return emptyState()
        items.sortedByDescending { it.publishedAt }.forEach { addItem(it.title, "${it.category}\n${it.content}") }
    }

    private fun renderActions(actions: List<OracleAction>) {
        host.addCard("ACTIONS", "Semnale calculate local pe poziții + istoric")
        if (actions.isEmpty()) return emptyState()
        actions.sortedByDescending { abs(it.score) }.forEach { addItem("${it.action} • ${it.ticker}", "Scor ${fmt(it.score)}\n${it.reason}") }
    }

    private fun addItem(title: String, body: String) {
        host.content.addView(TextView(host.root.context).apply {
            text = "$title\n$body"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(18,18,18,18); setBackgroundColor(Color.rgb(9,13,26))
        }, android.widget.LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,10) })
    }
    private fun emptyState() = host.addCard("Așteaptă date", "Nu există încă date locale suficiente pentru acest modul.")
    private fun fmt(v: Double) = "%.1f".format(v)
    private fun fmtMoney(v: Double) = "%.2f USD".format(v)
}
