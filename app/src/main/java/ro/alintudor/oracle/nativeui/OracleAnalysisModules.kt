package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import ro.alintudor.oracle.core.*

/** Shared native presentation for modules that use the local Oracle data cache. */
class OracleSimpleModule(private val host: OracleNativeModule, private val moduleTitle: String) {
    fun render(actions: List<OracleAction> = emptyList(), knowledge: List<OracleKnowledgeItem> = emptyList(), positions: List<OraclePosition> = emptyList()) {
        host.content.removeAllViews()
        when (moduleTitle) {
            "GROWTH" -> renderGrowth(positions)
            "ANALYSIS" -> renderAnalysis(positions)
            "WATCHLIST" -> renderWatchlist(positions)
            "KNOWLEDGE" -> renderKnowledge(knowledge)
            else -> renderActions(actions)
        }
    }

    private fun renderGrowth(positions: List<OraclePosition>) {
        host.addCard("GROWTH", "Top poziții după randament • date locale Oracle")
        if (positions.isEmpty()) return emptyState()
        positions.sortedByDescending { it.pnlPercent }.forEachIndexed { i, p ->
            addItem("${i + 1}. ${p.ticker}", "Randament ${fmt(p.pnlPercent)}%  •  Valoare ${fmtMoney(p.marketValue)}\n${p.company}")
        }
    }

    private fun renderAnalysis(positions: List<OraclePosition>) {
        host.addCard("ANALYSIS", "Analiză rapidă a portofoliului pe baza datelor disponibile")
        if (positions.isEmpty()) return emptyState()
        val total = positions.sumOf { it.marketValue }
        val pnl = positions.sumOf { it.pnl }
        val winners = positions.count { it.pnl > 0 }
        val losers = positions.count { it.pnl < 0 }
        host.addCard("Snapshot", "Poziții: ${positions.size}\nValoare: ${fmtMoney(total)}\nP/L: ${fmtMoney(pnl)}\nCâștigătoare: $winners  •  Pierzătoare: $losers")
        positions.sortedByDescending { kotlin.math.abs(it.pnlPercent) }.take(5).forEach { p ->
            addItem(p.ticker, "P/L ${fmtMoney(p.pnl)}  •  ${fmt(p.pnlPercent)}%  •  pondere ${fmt(p.weight)}%")
        }
    }

    private fun renderWatchlist(positions: List<OraclePosition>) {
        host.addCard("WATCHLIST", "Poziții urmărite și statusul curent")
        if (positions.isEmpty()) return emptyState()
        positions.sortedBy { it.ticker }.forEach { p ->
            addItem(p.ticker, "${p.status}  •  Preț ${fmtMoney(p.currentPrice)}  •  Pondere ${fmt(p.weight)}%")
        }
    }

    private fun renderKnowledge(items: List<OracleKnowledgeItem>) {
        host.addCard("KNOWLEDGE", "Biblioteca Oracle — conținut nativ, fără pagini WordPress în UI")
        if (items.isEmpty()) return emptyState()
        items.sortedByDescending { it.publishedAt }.forEach { addItem(it.title, "${it.category}\n${it.content}") }
    }

    private fun renderActions(actions: List<OracleAction>) {
        host.addCard(moduleTitle, "Modul Oracle nativ")
        if (actions.isEmpty()) return emptyState()
        actions.forEach { addItem("${it.action} • ${it.ticker}", "Scor ${fmt(it.score)}\n${it.reason}") }
    }

    private fun addItem(title: String, body: String) {
        host.content.addView(TextView(host.root.context).apply {
            text = "$title\n$body"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(18,18,18,18); setBackgroundColor(Color.rgb(9,13,26))
        }, android.widget.LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,10) })
    }

    private fun emptyState() = host.addCard("Așteaptă date", "Nu există încă date locale pentru acest modul. Când sincronizarea le furnizează, modulul le afișează nativ.")
    private fun fmt(v: Double) = "%.1f".format(v)
    private fun fmtMoney(v: Double) = "%.2f USD".format(v)
}
