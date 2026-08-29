package ro.alintudor.oracle.nativeui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import ro.alintudor.oracle.core.*
import kotlin.math.abs

/** Native presentation for Oracle modules, using the same premium dark visual system as the start map. */
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
        host.addCard("GROWTH", "Fundament, trend local și contribuție la portofoliu")
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
        host.addCard("ACTIONS", "Motor local de semnale — prioritizare după scor")
        if (actions.isEmpty()) return emptyState()
        val buys = actions.count { it.action.equals("BUY", true) }
        val sells = actions.count { it.action.equals("SELL", true) }
        val holds = actions.size - buys - sells
        host.addCard("SIGNAL SUMMARY", "BUY $buys  •  HOLD $holds  •  SELL $sells\nTotal semnale ${actions.size}")
        actions.sortedByDescending { abs(it.score) }.take(50).forEachIndexed { index, a ->
            val strength = when {
                abs(a.score) >= 80 -> "VERY STRONG"
                abs(a.score) >= 60 -> "STRONG"
                abs(a.score) >= 35 -> "MODERATE"
                else -> "WEAK"
            }
            addItem("${index + 1}. ${a.action} • ${a.ticker}", "Scor ${fmt(a.score)}  •  $strength\n${a.reason}")
        }
    }

    private fun addItem(title: String, body: String) {
        val card = LinearLayout(host.root.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(host.dp(16), host.dp(13), host.dp(16), host.dp(13))
            background = GradientDrawable().apply {
                setColor(Color.rgb(6, 10, 20))
                cornerRadius = host.dp(14).toFloat()
                setStroke(host.dp(1), Color.rgb(34, 43, 65))
            }
        }
        val top = LinearLayout(host.root.context).apply { gravity = Gravity.CENTER_VERTICAL }
        val marker = TextView(host.root.context).apply {
            text = "◆"
            textSize = 9f
            setTextColor(moduleAccent())
            gravity = Gravity.CENTER
        }
        top.addView(marker, LinearLayout.LayoutParams(host.dp(22), host.dp(22)))
        top.addView(TextView(host.root.context).apply {
            text = title.uppercase()
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .035f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(TextView(host.root.context).apply {
            text = "›"
            textSize = 24f
            setTextColor(moduleAccent())
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(host.dp(24), host.dp(30)))
        card.addView(top)
        card.addView(TextView(host.root.context).apply {
            text = body
            textSize = 14f
            setTextColor(Color.rgb(175, 183, 201))
            setPadding(host.dp(22), host.dp(5), 0, 0)
        })
        host.content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, host.dp(9)) })
    }

    private fun moduleAccent(): Int = when (moduleTitle) {
        "GROWTH" -> Color.rgb(145, 245, 35)
        "ANALYSIS" -> Color.rgb(25, 205, 255)
        "WATCHLIST", "KNOWLEDGE" -> Color.rgb(255, 210, 45)
        else -> Color.rgb(255, 205, 45)
    }

    private fun emptyState() = host.addCard("Așteaptă date", "Nu există încă date locale suficiente pentru acest modul. Când sincronizarea le furnizează, modulul le afișează nativ.")
    private fun fmt(v: Double) = "%.1f".format(v)
    private fun fmtMoney(v: Double) = "%.2f USD".format(v)
}
